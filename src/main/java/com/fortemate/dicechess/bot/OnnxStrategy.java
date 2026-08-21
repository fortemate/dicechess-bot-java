package com.fortemate.dicechess.bot;

import dicechess.engine.domain.GameState;
import dicechess.engine.jvmapi.JvmApi;

import lv.id.jc.dicechess.runtime.TurnContext;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collections;
import java.util.List;

/**
 * Strategy implementation using an ONNX value model (or fallback engine evaluation) over every legal full turn
 * available from the current position, via the engine's {@link JvmApi} facade.
 *
 * <p>{@link TurnContext#legalMoves()} is not consulted here: scoring a candidate turn needs the resulting board
 * position, not just its UCI tokens, so the engine's own enumeration ({@link JvmApi#legalTurns(GameState)}) has to
 * run regardless of whether the platform's inline tree was present.</p>
 */
public class OnnxStrategy implements Strategy {

    private static final Logger logger = System.getLogger(OnnxStrategy.class.getName());

    /** The evaluator used to score candidate board positions. */
    private final OnnxEvaluator evaluator;

    /**
     * Constructs a new ONNX strategy instance backed by the given evaluator.
     *
     * @param evaluator position evaluator instance (ONNX-backed or engine fallback)
     */
    public OnnxStrategy(OnnxEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    /**
     * Choose the best list of move notations (micro-moves forming a turn) for the given TurnContext.
     *
     * @param context turn context containing DFEN, remaining time, increment, etc.
     * @return list of move notations (e.g. ["e2e4", "g1f3"]), or empty list if no legal moves are available,
     *         DFEN parsing fails, the game is already over, or an error occurs
     */
    public List<String> chooseMoves(TurnContext context) {
        if (context == null || context.dfen() == null || context.dfen().isBlank()) {
            logger.log(Level.WARNING, "Received empty or null DFEN context");
            return Collections.emptyList();
        }

        GameState initialState;
        try {
            initialState = JvmApi.parseDfen(context.dfen());
        } catch (IllegalArgumentException e) {
            logger.log(Level.ERROR, "Failed to parse DFEN ''{0}'': {1}", context.dfen(), e.getMessage());
            return Collections.emptyList();
        }

        if (JvmApi.isGameOver(initialState)) {
            logger.log(Level.INFO, "Game is already over for DFEN: {0}", context.dfen());
            return Collections.emptyList();
        }

        var activeColor = JvmApi.activeColor(initialState);
        var turns = JvmApi.legalTurns(initialState);

        if (turns.isEmpty()) {
            logger.log(Level.INFO, "No legal turn paths available for DFEN: {0}", context.dfen());
            return Collections.emptyList();
        }

        JvmApi.Turn bestTurn = null;
        float bestScore = -Float.MAX_VALUE;

        for (var turn : turns) {
            var score = evaluator.evaluate(turn.finalState(), activeColor);
            if (bestTurn == null || score > bestScore) {
                bestScore = score;
                bestTurn = turn;
            }
        }

        logger.log(Level.DEBUG, "Chosen turn path: {0} with score {1}", bestTurn.uci(), bestScore);
        return bestTurn.uci();
    }
}
