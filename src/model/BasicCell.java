package model;

/** Giao diện cổ điển (Classic). */
public class BasicCell extends CellModel {
    @Override
    public String getMineSymbol() { return "*"; }

    @Override
    public String getFlagSymbol() { return "F"; }
}
