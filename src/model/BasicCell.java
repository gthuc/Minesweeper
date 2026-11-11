package model;

/**
 * Provides classic text symbols for the shared cell model.
 */
public class BasicCell extends CellModel {
    @Override
    public String getMineSymbol() {
        return "*";
    }

    @Override
    public String getFlagSymbol() {
        return "F";
    }
}
