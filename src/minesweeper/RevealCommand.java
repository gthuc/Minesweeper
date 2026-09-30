package minesweeper;
import java.util.List;

public class RevealCommand implements ICommand {
    private final int x, y;
    private final GameBoardModel board;
    private List<int[]> affectedCoords;

    public RevealCommand(GameBoardModel board, int x, int y) {
        this.board = board;
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean Execute() {
        affectedCoords = board.RevealCell(x, y);
        return !affectedCoords.isEmpty();
    }

    @Override
    public void Undo() {
        if (affectedCoords != null && !affectedCoords.isEmpty()) {
            board.UndoReveal(affectedCoords);
        }
    }
}