package factory;

import model.BasicCell;
import model.CellModel;

/**
 * Creates classic cells through the cell factory abstraction.
 */
public class StandardBoardFactory implements ICellFactory {
    @Override
    public CellModel createCell() {
        return new BasicCell();
    }
}
