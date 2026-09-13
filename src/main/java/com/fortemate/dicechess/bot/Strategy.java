package com.fortemate.dicechess.bot;

import com.fortemate.dicechess.runtime.BotStrategy;
import com.fortemate.dicechess.runtime.TurnAction;
import com.fortemate.dicechess.runtime.TurnContext;

import java.util.List;
import java.util.function.Function;

/**
 * Common functional interface for Java bot strategies mapping a TurnContext to move notations.
 * Extends {@link BotStrategy} to provide decision-oriented runtime integration while allowing
 * simple functional implementations of {@link #chooseMoves(TurnContext)}.
 */
@FunctionalInterface
public interface Strategy extends BotStrategy, Function<TurnContext, List<String>> {

    /**
     * Choose the best list of move notations (micro-moves forming a turn) for the given TurnContext.
     *
     * @param context turn context containing DFEN, remaining time, increment, etc.
     * @return list of move notations (e.g. ["e2e4", "g1f3"]). Returns empty list if no moves are available
     *         or an error occurs.
     */
    List<String> chooseMoves(TurnContext context);

    @Override
    default TurnAction onTurn(TurnContext context) {
        return new TurnAction(chooseMoves(context));
    }

    @Override
    default List<String> apply(TurnContext context) {
        return chooseMoves(context);
    }
}
