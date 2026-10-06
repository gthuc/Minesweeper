package command;

import model.CellModel;
import model.GameBoardModel;

import java.util.List;

public class RevealCommand implements ICommand {
    private final int x, y;
    private final GameBoardModel board;
    private List<int[]> affectedCoords;
    private boolean hitMine;

    public RevealCommand(GameBoardModel board, int x, int y) {
        this.board = board;
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean execute() {
        affectedCoords = board.revealCell(x, y);
        CellModel cell = board.getCell(x, y);
        hitMine = !affectedCoords.isEmpty() && cell != null && cell.isMine();
        return !affectedCoords.isEmpty();
    }

    @Override
    public void undo() {
        if (affectedCoords != null && !affectedCoords.isEmpty()) {
            board.undoReveal(affectedCoords);
        }
    }

    /** Trúng mìn là mất mạng vĩnh viễn, không được undo để "lấy lại mạng". */
    @Override
    public boolean isUndoable() {
        return !hitMine;
    }
}
