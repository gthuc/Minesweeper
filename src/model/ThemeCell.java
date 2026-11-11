package model;

/**
 * Provides alternate text symbols while retaining the shared cell behavior.
 */
public class ThemeCell extends CellModel {
    @Override
    public String getMineSymbol() {
        return "@";
    }

    @Override
    public String getFlagSymbol() {
        return "P";
    }
}
