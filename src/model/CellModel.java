package model;

/**
 * Stores mine placement, adjacent-mine count, reveal state, and flag state.
 * Board operations maintain game counters and notifications; direct setters only
 * change this cell and should be used with that responsibility in mind.
 */
public abstract class CellModel {
    private boolean mine;
    private int adjacentMinesCount;
    private boolean revealed;
    private boolean flagged;

    public boolean isMine() {
        return mine;
    }

    public int getAdjacentMinesCount() {
        return adjacentMinesCount;
    }

    public boolean isRevealed() {
        return revealed;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setMine(boolean mine) {
        this.mine = mine;
    }

    public void setAdjacentMinesCount(int count) {
        this.adjacentMinesCount = count;
    }

    public void setRevealed(boolean revealed) {
        this.revealed = revealed;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    /**
     * Returns the theme-specific text symbol for a revealed mine.
     */
    public abstract String getMineSymbol();

    /**
     * Returns the theme-specific text symbol for a player flag.
     */
    public abstract String getFlagSymbol();
}
