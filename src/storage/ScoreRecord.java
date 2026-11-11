package storage;

import util.PlayerNames;

import java.io.Serializable;

/**
 * Stores a serializable score with an ordering based on performance.
 * Fewer mine hits rank first, followed by shorter time and then no hint usage.
 * The timestamp records when a score was created and does not break ranking ties.
 */
public class ScoreRecord implements Comparable<ScoreRecord>, Serializable {
    private static final long serialVersionUID = 1L;

    private final String playerName;
    private final int timeInSeconds;
    private final int minesHit;
    private final boolean hintUsed;
    private final long timestamp;

    public ScoreRecord(String playerName, int timeInSeconds, int minesHit, boolean hintUsed) {
        this(playerName, timeInSeconds, minesHit, hintUsed, System.currentTimeMillis());
    }

    public ScoreRecord(String playerName, int timeInSeconds, int minesHit, boolean hintUsed, long timestamp) {
        this.playerName = playerName;
        this.timeInSeconds = timeInSeconds;
        this.minesHit = minesHit;
        this.hintUsed = hintUsed;
        this.timestamp = timestamp;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getTimeInSeconds() {
        return timeInSeconds;
    }

    public int getMinesHit() {
        return minesHit;
    }

    public boolean isHintUsed() {
        return hintUsed;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns a renamed record preserving performance and the original timestamp.
     */
    public ScoreRecord withPlayerName(String newName) {
        return new ScoreRecord(PlayerNames.normalize(newName), timeInSeconds, minesHit, hintUsed, timestamp);
    }

    /**
     * Orders records by mine hits, elapsed seconds, then hint usage (false first).
     */
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
        return playerName + " - " + timeInSeconds + "s - mines hit: " + minesHit
                + (hintUsed ? " (hint used)" : "");
    }
}
