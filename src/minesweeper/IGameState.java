package minesweeper;

public interface IGameState {
    public void HandleCommand(GameManager context, ICommand command);
    public HintResult UseHint (GameManager context);
}
