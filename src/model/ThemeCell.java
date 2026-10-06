package model;

/** Giao diện chủ đề (ví dụ: cờ hải tặc, bom). */
public class ThemeCell extends CellModel {
    @Override
    public String getMineSymbol() { return "@"; }

    @Override
    public String getFlagSymbol() { return "P"; }
}
