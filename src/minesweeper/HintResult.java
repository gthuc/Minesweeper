package minesweeper;

public class HintResult {
    private int x;
    private int y;
    private HintType type;

    public HintResult(int x, int y, HintType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public HintType getType() { return type; }
}