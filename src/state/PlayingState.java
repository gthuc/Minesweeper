package state;

import command.CommandManager;
import command.ICommand;
import controller.GameManager;
import hint.HintResult;
import hint.HintSystem;
import model.GameBoardModel;
import model.GameStatus;

public class PlayingState implements IGameState {
    @Override
    public GameStatus getStatus() { return GameStatus.PLAYING; }

    @Override
    public void handleCommand(GameManager context, ICommand command) {
        CommandManager.getInstance().executeCommand(command);
        context.checkWinCondition();  
    }

    @Override
    public HintResult useHint(GameManager context) {
        GameBoardModel board = context.getGameBoard();
        HintResult result = HintSystem.getInstance().getHint(board);


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
