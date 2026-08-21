package dicechess.bot;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import dicechess.engine.domain.GameState;
import dicechess.engine.jvmapi.JvmApi;
import dicechess.engine.search.OnnxFeatures;
import dicechess.engine.search.RichFeatures;

import java.io.File;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.FloatBuffer;
import java.util.Collections;

/**
 * Evaluates board positions using an ONNX value model or falls back to engine heuristics.
 */
public class OnnxEvaluator implements AutoCloseable {

    private static final Logger logger = System.getLogger(OnnxEvaluator.class.getName());

    /** ONNX Runtime environment. */
    private final OrtEnvironment env;

    /** ONNX Runtime session for model inference. */
    private final OrtSession session;

    /** Whether an ONNX model was successfully loaded. */
    private final boolean isLoaded;

    /**
     * Constructs a new ONNX evaluator for the given model file path.
     *
     * @param modelPath filesystem path to the ONNX model file (e.g. {@code "models/baseline.onnx"}).
     *                  If {@code null}, blank, or file does not exist, the evaluator gracefully
     *                  falls back to engine heuristic evaluation.
     */
    public OnnxEvaluator(String modelPath) {
        OrtEnvironment tempEnv = null;
        OrtSession tempSession = null;
        boolean loaded = false;

        if (modelPath != null && !modelPath.isBlank()) {
            var file = new File(modelPath);
            if (file.exists() && file.isFile()) {
                try {
                    tempEnv = OrtEnvironment.getEnvironment();
                    tempSession = tempEnv.createSession(file.getAbsolutePath(), new OrtSession.SessionOptions());
                    loaded = true;
                    logger.log(Level.INFO, "Successfully loaded ONNX model from: {0}", file.getAbsolutePath());
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to load ONNX model from {0}: {1}. Falling back to engine heuristic.",
                            file.getAbsolutePath(), e.getMessage());
                }
            } else {
                logger.log(Level.INFO, "ONNX model file not found at {0}. Using engine heuristic evaluation.", modelPath);
            }
        } else {
            logger.log(Level.INFO, "No MODEL_PATH specified. Using engine heuristic evaluation.");
        }

        this.env = tempEnv;
        this.session = tempSession;
        this.isLoaded = loaded;
    }

    /**
     * Indicates whether an ONNX model file was loaded and is active for evaluation.
     *
     * @return {@code true} if ONNX model is loaded; {@code false} if using engine heuristic fallback
     */
    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * Evaluates a GameState from the perspective of the specified color.
     *
     * @param state the board state to evaluate
     * @param color active player's color (0 for White, 1 for Black)
     * @return score as a float value (higher is better for the specified color).
     *         Falls back to {@link dicechess.engine.jvmapi.JvmApi#evaluate} engine heuristic evaluation
     *         if ONNX model is not loaded.
     */
    public float evaluate(GameState state, int color) {
        if (!isLoaded || session == null || env == null) {
            // Engine heuristic fallback
            return JvmApi.evaluate(state, color);
        }

        try {
            // Determine feature count expected by ONNX model (9 for RichFeatures, 7 for OnnxFeatures)
            var inputNumFeatures = session.getInputInfo().values().iterator().next().getInfo().toString().contains("9") ? 9 : 7;
            var features = (inputNumFeatures == 9) ?
                    RichFeatures.extract(state, color) :
                    OnnxFeatures.extract(state, color);

            var shape = new long[]{1, features.length};
            var buffer = FloatBuffer.wrap(features);

            try (var inputTensor = OnnxTensor.createTensor(env, buffer, shape);
                 var result = session.run(Collections.singletonMap(session.getInputNames().iterator().next(), inputTensor))) {

                var value = result.get(0).getValue();
                return switch (value) {
                    case float[][] floatArrayArray -> floatArrayArray[0][0];
                    case float[] floatArray -> floatArray[0];
                    default -> {
                        logger.log(Level.WARNING, "Unexpected ONNX output shape: {0}", value);
                        yield 0.0f;
                    }
                };
            }
        } catch (OrtException e) {
            logger.log(Level.WARNING, "Error running ONNX inference: {0}. Falling back to engine evaluation.", e.getMessage());
            return JvmApi.evaluate(state, color);
        }
    }

    @Override
    public void close() {
        if (session != null) {
            try {
                session.close();
            } catch (OrtException e) {
                logger.log(Level.ERROR, "Error closing OrtSession: {0}", e.getMessage());
            }
        }
        if (env != null) {
            try {
                env.close();
            } catch (Exception e) {
                logger.log(Level.ERROR, "Error closing OrtEnvironment: {0}", e.getMessage());
            }
        }
    }
}
