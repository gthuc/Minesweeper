package minesweeper;

public class ThemeBoardFactory implements ICellFactory {
    
    @Override
    public CellModel[][] createBoard(int size, int totalMines) {
        CellModel[][] board = new CellModel[size][size];
        
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                board[i][j] = new ThemeCell();
            }
        }
        
        return board;
    }
}