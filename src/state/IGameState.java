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

    /** @return true nếu có thay đổi được hoàn tác. */
    default boolean undo(GameManager context) { return false; }

    default void pause(GameManager context) {}

    default void resume(GameManager context) {}
}
