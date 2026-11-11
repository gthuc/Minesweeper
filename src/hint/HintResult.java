package hint;

public class HintResult {
    private final int x;
    private final int y;
    private final HintType type;
    private final boolean onFlaggedCell;

    public HintResult(int x, int y, HintType type) {
        this(x, y, type, false);
    }

    public HintResult(int x, int y, HintType type, boolean onFlaggedCell) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.onFlaggedCell = onFlaggedCell;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public HintType getType() { return type; }





    public boolean isOnFlaggedCell() { return onFlaggedCell; }
}
