package minesweeper;

import java.io.Serializable;

public class ScoreRecord implements Comparable<ScoreRecord>, Serializable {
    private String playerName;
    private int timeInSeconds;
    private int minesHit;

    public ScoreRecord(String playerName, int timeInSeconds, int minesHit) {
        this.playerName = playerName;
        this.timeInSeconds = timeInSeconds;
        this.minesHit = minesHit;
    }

    public String getPlayerName() { return playerName; }
    public int getTimeInSeconds() { return timeInSeconds; }
    public int getMinesHit() { return minesHit; }

    @Override
    public int compareTo(ScoreRecord other) {
        if (this.minesHit != other.minesHit) {
            return Integer.compare(this.minesHit, other.minesHit);
        }
        return Integer.compare(this.timeInSeconds, other.timeInSeconds);
    }
}