package minesweeper;

public class InactiveState implements IGameState {
    @Override
    public void HandleCommand(GameManager context, ICommand command) 
    {
    }

    @Override
    public HintResult UseHint(GameManager context) 
    {
        return null;
    }
}