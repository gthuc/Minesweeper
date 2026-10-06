package storage;

import java.io.Serializable;

public class ScoreRecord implements Comparable<ScoreRecord>, Serializable {
    private static final long serialVersionUID = 1L;

    private final String playerName;
    private final int timeInSeconds;
    private final int minesHit;
    private final boolean hintUsed;

    public ScoreRecord(String playerName, int timeInSeconds, int minesHit, boolean hintUsed) {
        this.playerName = playerName;
        this.timeInSeconds = timeInSeconds;
        this.minesHit = minesHit;
        this.hintUsed = hintUsed;
    }

    public String getPlayerName() { return playerName; }
    public int getTimeInSeconds() { return timeInSeconds; }
    public int getMinesHit() { return minesHit; }
    public boolean isHintUsed() { return hintUsed; }

    /** Ít mìn nổ hơn tốt hơn; sau đó thời gian ngắn hơn; sau đó không dùng gợi ý. */
    @Override
    public int compareTo(ScoreRecord other) {
        if (this.minesHit != other.minesHit) {
            return Integer.compare(this.minesHit, other.minesHit);
        }
        if (this.timeInSeconds != other.timeInSeconds) {
            return Integer.compare(this.timeInSeconds, other.timeInSeconds);
        }
        return Boolean.compare(this.hintUsed, other.hintUsed);
    }

    @Override
    public String toString() {
        return playerName + " - " + timeInSeconds + "s - mìn nổ: " + minesHit
                + (hintUsed ? " (có dùng gợi ý)" : "");
    }
}
