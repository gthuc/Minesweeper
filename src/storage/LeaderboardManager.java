package storage;

import model.DifficultyLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaderboardManager {
    private static final int MAX_ENTRIES = 10;
    private static LeaderboardManager instance;

    private final IScoreStorage storage;
    private Map<DifficultyLevel, List<ScoreRecord>> leaderboards;

    private LeaderboardManager() {
        this.storage = new FileScoreStorage();
        this.leaderboards = storage.loadScores();
        if (this.leaderboards == null) {
            this.leaderboards = new HashMap<>();
        }
    }

    public static synchronized LeaderboardManager getInstance() {
        if (instance == null) {
            instance = new LeaderboardManager();
        }
        return instance;
    }

    public synchronized void addScore(DifficultyLevel level, ScoreRecord record) {
        if (level == null || record == null) return;

        List<ScoreRecord> scores = leaderboards.computeIfAbsent(level, k -> new ArrayList<>());
        scores.add(record);
        Collections.sort(scores);
        while (scores.size() > MAX_ENTRIES) {
            scores.remove(scores.size() - 1);
        }
        storage.saveScores(leaderboards);
    }

    /** Trả về bản sao chỉ đọc, bên ngoài không sửa được dữ liệu nội bộ. */
    public synchronized List<ScoreRecord> getTopScores(DifficultyLevel level) {
        List<ScoreRecord> scores = leaderboards.get(level);
        if (scores == null) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(scores));
    }
}
