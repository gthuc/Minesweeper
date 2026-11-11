package model;


public class BasicCell extends CellModel {
    @Override
    public String getMineSymbol() { return "*"; }

    @Override
    public String getFlagSymbol() { return "F"; }
}
