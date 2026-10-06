package model;

public abstract class CellModel {
    private boolean mine;
    private int adjacentMinesCount;
    private boolean revealed;
    private boolean flagged;

    public boolean isMine() { return mine; }
    public int getAdjacentMinesCount() { return adjacentMinesCount; }
    public boolean isRevealed() { return revealed; }
    public boolean isFlagged() { return flagged; }

    public void setMine(boolean mine) { this.mine = mine; }
    public void setAdjacentMinesCount(int count) { this.adjacentMinesCount = count; }
    public void setRevealed(boolean revealed) { this.revealed = revealed; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }

    /** Ký hiệu hiển thị của mìn theo giao diện (Classic / Theme). */
    public abstract String getMineSymbol();

    /** Ký hiệu hiển thị của cờ theo giao diện (Classic / Theme). */
    public abstract String getFlagSymbol();
}
