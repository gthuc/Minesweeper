package minesweeper;

public class RevealCommand implements ICommand {
    public int x, y;
    public boolean wasRevealed;
    public boolean wasFlagged;
    public boolean wasMine;

    private GameBoardModel board;

    public RevealCommand(GameBoardModel board, int x, int y) {
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
        board.RevealCell(x, y);
    }
    @Override 
    public void Undo()
    {
        CellModel cell = board.GetCell(x, y);
        cell.setRevealed(wasRevealed);
        cell.setFlagged(wasFlagged);
        cell.setMine(wasMine);
    }

    //sửa lại thuật toán undo để khi undo reveal cell thì nó sẽ reveal lại các ô xung quanh nếu ô đó chưa được reveal và không phải là mine
}
