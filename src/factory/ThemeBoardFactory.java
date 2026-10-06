package factory;

import model.CellModel;
import model.ThemeCell;

public class ThemeBoardFactory implements ICellFactory {
    @Override
    public CellModel createCell() {
        return new ThemeCell();
    }
}
