package minesweeper;

public class ThemeBoardFactory implements ICellFactory {
    @Override
    public CellModel createCell() {
        return new ThemeCell();
    }
}