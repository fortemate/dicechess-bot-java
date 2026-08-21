package dicechess.bot;

import lv.id.jc.dicechess.runtime.TurnContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OnnxStrategyTest {

    private OnnxEvaluator evaluator;
    private OnnxStrategy strategy;

    @BeforeEach
    void setUp() {
        evaluator = new OnnxEvaluator("non-existent-model.onnx"); // Will use fallback engine evaluator
        strategy = new OnnxStrategy(evaluator);
    }

    @AfterEach
    void tearDown() {
        if (evaluator != null) {
            evaluator.close();
        }
    }

    @Test
    void testChooseMovesWithEmptyDfen() {
        var context = new TurnContext("test-game", "", null, List.of());
        var moves = strategy.chooseMoves(context);
        assertTrue(moves.isEmpty(), "Should return empty list for empty DFEN");
    }

    @Test
    void testChooseMovesWithInitialPosition() {
        // Initial DFEN position with dice pool 'p' (pawn roll) for white
        var dfen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 p";
        var context = new TurnContext("test-game", dfen, null, List.of());

        var moves = strategy.chooseMoves(context);
        assertFalse(moves.isEmpty(), "Should generate at least one legal move for pawn roll");
        assertEquals(1, moves.size(), "Pawn roll should produce 1 micro-move");
    }

    @Test
    void testChooseMovesWithTripleDicePool() {
        // Initial DFEN position with dice pool 'pnb' for white
        var dfen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 pnb";
        var context = new TurnContext("test-game", dfen, null, List.of());

        var moves = strategy.chooseMoves(context);
        assertFalse(moves.isEmpty(), "Should generate legal turn sequence for triple dice pool");
        assertTrue(moves.size() <= 3, "Turn should contain at most 3 micro-moves");
    }
}
