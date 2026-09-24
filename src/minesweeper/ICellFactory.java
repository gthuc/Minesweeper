package minesweeper;

public interface ICellFactory {
    public CellModel[][] createBoard(int size, int totalMines);
}
