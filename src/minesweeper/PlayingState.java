package minesweeper;

public class PlayingState implements IGameState {
    @Override
    public void HandleCommand(GameManager context, ICommand command) {
        CommandManager.Instance().ExecuteCommand(command);
        context.CheckWinCondition();
    }

    @Override
    public HintResult UseHint(GameManager context) {
        context.setHintUsed(true);

        GameBoardModel board = context.getGameBoard();
        HintSystem hintSystem = HintSystem.Instance();
        hintSystem.setStrategy(new SafeHintStrategy());
        HintResult result = hintSystem.GetHint(board);

        if (result == null) {
            hintSystem.setStrategy(new IntelligentHintStrategy());
            result = hintSystem.GetHint(board);
        }

        return result;
}
}   