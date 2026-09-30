package minesweeper;

public class FlagCommand implements ICommand {
    private final int x, y;
    private final GameBoardModel board;

    public FlagCommand(GameBoardModel board, int x, int y) {
        this.board = board;
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean Execute() {
        CellModel cell = board.GetCell(x, y);
        if (cell == null || cell.isRevealed()) {
            return false; // không có tác dụng -> không vào history
        }
        board.ToggleFlag(x, y);
        return true;
    }

    @Override
    public void Undo() {
        // Toggle lại: đảm bảo observer được thông báo, không đụng tới isMine
        board.ToggleFlag(x, y);
    }
}