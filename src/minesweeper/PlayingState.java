package minesweeper;

public class PlayingState implements IGameState {
    @Override
    public void HandleCommand(GameManager context, ICommand command) {
        CommandManager.Instance().ExecuteCommand(command);
        context.CheckWinCondition(); // có thể chuyển sang InactiveState
    }

    @Override
    public HintResult UseHint(GameManager context) {
        HintResult result = HintSystem.Instance().GetHint(context.getGameBoard());
        if (result != null) {
            context.setHintUsed(true); // chỉ đánh dấu khi thực sự có gợi ý
        }
        return result;
    }
}