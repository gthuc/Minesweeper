package hint;

public class HintResult {
    private final int x;
    private final int y;
    private final HintType type;

    public HintResult(int x, int y, HintType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public HintType getType() { return type; }
}
