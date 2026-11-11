package state;

import command.ICommand;
import controller.GameManager;
import hint.HintResult;
import model.GameStatus;


public interface IGameState {
    GameStatus getStatus();

    default void handleCommand(GameManager context, ICommand command) {}

    default HintResult useHint(GameManager context) { return null; }


    default boolean undo(GameManager context) { return false; }

    default void pause(GameManager context) {}

    default void resume(GameManager context) {}
}
