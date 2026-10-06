package factory;

import model.BasicCell;
import model.CellModel;

public class StandardBoardFactory implements ICellFactory {
    @Override
    public CellModel createCell() {
        return new BasicCell();
    }
}
