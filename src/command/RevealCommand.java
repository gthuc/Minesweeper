package command;

import model.GameBoardModel;

import java.util.List;

/**
 * Records all cells revealed by one action, including flood-fill expansion.
 * Undo hides the reveal group and restores counters, including a lost life on mine hits.
 */
public class RevealCommand implements ICommand {
    private final int x, y;
    private final GameBoardModel board;
    private List<int[]> affectedCoords;

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
        return !affectedCoords.isEmpty();
    }

    @Override
    public boolean undo() {
        return affectedCoords != null && !affectedCoords.isEmpty() && board.undoReveal(affectedCoords);
    }

}
