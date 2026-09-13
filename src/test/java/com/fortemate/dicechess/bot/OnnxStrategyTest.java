package com.fortemate.dicechess.bot;

import com.fortemate.dicechess.runtime.TurnContext;
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
    void testChooseMovesWithNullContext() {
        var moves = strategy.chooseMoves(null);
        assertTrue(moves.isEmpty(), "Should return empty list for null context");
    }

    @Test
    void testChooseMovesWithInvalidDfen() {
        var context = new TurnContext("test-game", "White", 1L, "invalid-dfen-string", null, List.of(), false);
        var moves = strategy.chooseMoves(context);
        assertTrue(moves.isEmpty(), "Should return empty list for invalid DFEN");
    }

    @Test
    void testChooseMovesWithInitialPosition() {
        // Initial DFEN position with dice pool 'p' (pawn roll) for white
        var dfen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 p";
        var context = new TurnContext("test-game", "White", 1L, dfen, null, List.of(), false);

        var moves = strategy.chooseMoves(context);
        assertFalse(moves.isEmpty(), "Should generate at least one legal move for pawn roll");
        assertEquals(1, moves.size(), "Pawn roll should produce 1 micro-move");
    }

    @Test
    void testChooseMovesWithTripleDicePool() {
        // Initial DFEN position with dice pool 'pnb' for white
        var dfen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 pnb";
        var context = new TurnContext("test-game", "White", 1L, dfen, null, List.of(), false);

        var moves = strategy.chooseMoves(context);
        assertFalse(moves.isEmpty(), "Should generate legal turn sequence for triple dice pool");
        assertTrue(moves.size() <= 3, "Turn should contain at most 3 micro-moves");
    }

    @Test
    void testOnTurnReturnsTurnAction() {
        var dfen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 p";
        var context = new TurnContext("test-game", "White", 1L, dfen, null, List.of(), false);

        var action = strategy.onTurn(context);
        assertNotNull(action, "onTurn should return non-null TurnAction");
        assertFalse(action.moves().isEmpty(), "onTurn moves should not be empty");
        assertFalse(action.offerDraw(), "onTurn offerDraw should default to false");
    }

    @Test
    void testDefaultDrawAndDoubleDecisions() {
        assertFalse(strategy.onDrawDecision(null).acceptDraw(), "Should decline draw by default");
        assertFalse(strategy.onDoubleOpportunity(null).offerDouble(), "Should roll without offering double by default");
        assertFalse(strategy.onDoubleDecision(null).acceptDouble(), "Should decline double by default");
    }

    @Test
    void testStrategyApplyMatchesChooseMoves() {
        var dfen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 p";
        var context = new TurnContext("test-game", "White", 1L, dfen, null, List.of(), false);
        assertEquals(strategy.chooseMoves(context), strategy.apply(context));
    }
}
