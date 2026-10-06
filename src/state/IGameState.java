package state;

import command.ICommand;
import controller.GameManager;
import hint.HintResult;
import model.GameStatus;

/** Mặc định mọi thao tác đều bị bỏ qua; mỗi state chỉ override những gì nó cho phép. */
public interface IGameState {
    GameStatus getStatus();

    default void handleCommand(GameManager context, ICommand command) {}

    default HintResult useHint(GameManager context) { return null; }

    default void undo(GameManager context) {}

    default void pause(GameManager context) {}

    default void resume(GameManager context) {}
}
