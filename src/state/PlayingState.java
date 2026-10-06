package state;

import command.CommandManager;
import command.ICommand;
import controller.GameManager;
import hint.HintResult;
import hint.HintSystem;
import model.GameStatus;

public class PlayingState implements IGameState {
    @Override
    public GameStatus getStatus() { return GameStatus.Playing; }

    @Override
    public void handleCommand(GameManager context, ICommand command) {
        CommandManager.getInstance().executeCommand(command);
        context.checkWinCondition(); // có thể chuyển sang InactiveState
    }

    @Override
    public HintResult useHint(GameManager context) {
        HintResult result = HintSystem.getInstance().getHint(context.getGameBoard());
        if (result != null) {
            context.setHintUsed(true); // chỉ đánh dấu khi thực sự có gợi ý
        }
        return result;
    }

    @Override
    public void undo(GameManager context) {
        CommandManager.getInstance().undo();
    }

    @Override
    public void pause(GameManager context) {
        context.transitionTo(new PausedState());
    }
}
