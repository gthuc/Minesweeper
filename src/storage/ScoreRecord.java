package storage;

import util.PlayerNames;

import java.io.Serializable;

public class ScoreRecord implements Comparable<ScoreRecord>, Serializable {
    // Giữ nguyên 1L: thêm field mới (timestamp) vẫn đọc được file leaderboard.dat cũ
    // (bản ghi cũ có timestamp = 0). Đổi số này sẽ làm file cũ bị coi là hỏng.
    private static final long serialVersionUID = 1L;

    private final String playerName;
    private final int timeInSeconds;
    private final int minesHit;
    private final boolean hintUsed;
    private final long timestamp; // epoch millis; 0 = bản ghi cũ không có ngày

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

    public String getPlayerName() { return playerName; }
    public int getTimeInSeconds() { return timeInSeconds; }
    public int getMinesHit() { return minesHit; }
    public boolean isHintUsed() { return hintUsed; }
    public long getTimestamp() { return timestamp; }

    /** Bản sao với tên mới (đã kiểm tra hợp lệ); ScoreRecord là bất biến. */
    public ScoreRecord withPlayerName(String newName) {
        return new ScoreRecord(PlayerNames.normalize(newName), timeInSeconds, minesHit, hintUsed, timestamp);
    }

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
