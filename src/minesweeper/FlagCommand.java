package minesweeper;

public class FlagCommand implements ICommand {
    public int x, y;
    public boolean wasRevealed;
    public boolean wasFlagged;
    public boolean wasMine;

    private GameBoardModel board;

    public FlagCommand(GameBoardModel board, int x, int y) {
        this.board = board;
        this.x = x;
        this.y = y;
        CellModel cell = board.GetCell(x, y);
        if(cell != null) 
        {
            this.wasRevealed = cell.isRevealed();
            this.wasFlagged = cell.isFlagged();
            this.wasMine = cell.isMine();
        }
    }

    @Override 
    public void Execute()
    {
        board.ToggleFlag(x, y);
    }
    @Override 
    public void Undo()
    {
        CellModel cell = board.GetCell(x, y);
        cell.setRevealed(wasRevealed);
        cell.setFlagged(wasFlagged);
        cell.setMine(wasMine);
    }
}
