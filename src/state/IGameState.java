package state;

import command.ICommand;
import controller.GameManager;
import hint.HintResult;
import model.GameStatus;

/**
 * State pattern contract controlling which actions are available.
 * Default implementations intentionally reject actions with no change, null, or false;
 * concrete states override only the operations they allow.
 */
public interface IGameState {
    GameStatus getStatus();

    /**
     * Ignores board commands unless a state explicitly enables them.
     */
    default void handleCommand(GameManager context, ICommand command) {
    }

    /**
     * Returns null when hints are disabled by the current state.
     */
    default HintResult useHint(GameManager context) {
        return null;
    }

    /**
     * Returns false when undo is disabled by the current state.
     */
    default boolean undo(GameManager context) {
        return false;
    }

    /**
     * Ignores pause requests unless supported by the state.
     */
    default void pause(GameManager context) {
    }

    /**
     * Ignores resume requests unless supported by the state.
     */
    default void resume(GameManager context) {
    }
}
