package com.fortemate.dicechess.bot;

import dicechess.engine.jvmapi.JvmApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OnnxEvaluatorFallbackTest {

    @Test
    void testFallbackForInvalidOrMissingPaths() {
        var invalidPaths = List.of("", "   ", "no/such/model.onnx");
        for (var path : invalidPaths) {
            assertDoesNotThrow(() -> {
                try (var evaluator = new OnnxEvaluator(path)) {
                    assertFalse(evaluator.isLoaded(), "isLoaded() should be false for path: " + path);
                }
            });
        }

        assertDoesNotThrow(() -> {
            try (var evaluator = new OnnxEvaluator(null)) {
                assertFalse(evaluator.isLoaded(), "isLoaded() should be false for null path");
            }
        });
    }

    @Test
    void testFallbackForNonOnnxFile(@TempDir Path tempDir) throws IOException {
        var invalidModelFile = tempDir.resolve("invalid_model.onnx");
        Files.writeString(invalidModelFile, "this is not a valid onnx model file");

        assertDoesNotThrow(() -> {
            try (var evaluator = new OnnxEvaluator(invalidModelFile.toString())) {
                assertFalse(evaluator.isLoaded(), "isLoaded() should be false for non-ONNX file");
            }
        });
    }

    @Test
    void testEvaluateMatchesEngineHeuristicFallback() {
        var dfen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 p";
        var state = JvmApi.parseDfen(dfen);

        try (var evaluator = new OnnxEvaluator(null)) {
            assertFalse(evaluator.isLoaded());
            assertEquals(JvmApi.evaluate(state, 0), evaluator.evaluate(state, 0), "White evaluation should match JvmApi.evaluate");
            assertEquals(JvmApi.evaluate(state, 1), evaluator.evaluate(state, 1), "Black evaluation should match JvmApi.evaluate");
        }
    }
}
