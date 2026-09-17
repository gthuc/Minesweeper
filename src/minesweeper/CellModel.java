package minesweeper;

public class CellModel 
{
    private boolean IsMine;
    private int AdjacentMinesCount;
    private boolean IsRevealed;
    private boolean IsFlagged;
    //constructor
    public CellModel()
    {
        IsMine = false;
        AdjacentMinesCount = 0;
        IsRevealed = false;
        IsFlagged = false;
    }
    //getters
    public boolean isMine() {
        return IsMine;
    }
    public int getAdjacentMinesCount() {
        return AdjacentMinesCount;
    }
    public boolean isRevealed() {
        return IsRevealed;
    }
    public boolean isFlagged() {
        return IsFlagged;
    }
    //setters
    public void setMine(boolean mine) {
        IsMine = mine;
    }
    public void setAdjacentMinesCount(int adjacentMinesCount) {
        AdjacentMinesCount = adjacentMinesCount;
    }
    public void setRevealed(boolean revealed) {
        IsRevealed = revealed;
    }
    public void setFlagged(boolean flagged) {
        IsFlagged = flagged;
    }
}