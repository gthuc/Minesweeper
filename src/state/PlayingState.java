package state;

import command.CommandManager;
import command.ICommand;
import controller.GameManager;
import hint.HintResult;
import hint.HintSystem;
import model.GameBoardModel;
import model.GameStatus;

/**
 * Enables commands, hints, undo, and pausing during active play.
 * Delegates board mutations to the command manager and checks outcomes after execution.
 */
public class PlayingState implements IGameState {
    @Override
    public GameStatus getStatus() {
        return GameStatus.PLAYING;
    }

    /**
     * Executes the action before evaluating victory and life loss.
     */
    @Override
    public void handleCommand(GameManager context, ICommand command) {
        CommandManager.getInstance().executeCommand(command);
        context.checkWinCondition();
    }

    /**
     * Marks hint usage only after mine generation; first-click guidance is free.
     */
    @Override
    public HintResult useHint(GameManager context) {
        GameBoardModel board = context.getGameBoard();
        HintResult result = HintSystem.getInstance().getHint(board);

        // Opening guidance does not count as using a hint for score ranking.
        if (result != null && board != null && board.isMinesGenerated()) {
            context.setHintUsed(true);
        }
        return result;
    }

    @Override
    public boolean undo(GameManager context) {
        return CommandManager.getInstance().undo();
    }

    @Override
    public void pause(GameManager context) {
        context.transitionTo(new PausedState());
    }
}
