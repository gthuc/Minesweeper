package minesweeper;

public class StandardBoardFactory implements ICellFactory {
    @Override
    public CellModel createCell() {
        return new BasicCell();
    }
}