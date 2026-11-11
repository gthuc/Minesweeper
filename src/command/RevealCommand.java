package command;

import model.CellModel;
import model.GameBoardModel;

import java.util.List;

/**
 * Records all cells revealed by one action, including flood-fill expansion.
 * Mine hits are excluded from normal undo history so lost lives cannot be recovered.
 */
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

    /**
     * Captures the complete reveal result for a later undo, if allowed.
     */
    @Override
    public boolean execute() {
        // Store the whole reveal group so flood-fill cells are undone together.
        affectedCoords = board.revealCell(x, y);
        CellModel cell = board.getCell(x, y);
        hitMine = !affectedCoords.isEmpty() && cell != null && cell.isMine();
        return !affectedCoords.isEmpty();
    }

    @Override
    public boolean undo() {
        return affectedCoords != null && !affectedCoords.isEmpty() && board.undoReveal(affectedCoords);
    }

    /**
     * Excludes mine hits from history; a life loss must remain permanent.
     */
    @Override
    public boolean isUndoable() {
        return !hitMine;
    }
}
