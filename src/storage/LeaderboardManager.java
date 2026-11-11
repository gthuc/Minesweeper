package storage;

import model.DifficultyLevel;
import util.PlayerNames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintains at most twenty ranked records per preset difficulty.
 * Custom games are excluded. Records are located by object identity when renamed
 * or when determining whether a newly added entry survived trimming.
 */
public class LeaderboardManager {
    public static final int MAX_ENTRIES = 20;
    private static LeaderboardManager instance;

    private final IScoreStorage storage;
    private final Map<DifficultyLevel, List<ScoreRecord>> leaderboards;

    private LeaderboardManager(IScoreStorage storage) {
        this.storage = storage;
        Map<DifficultyLevel, List<ScoreRecord>> loaded = storage.loadScores();
        this.leaderboards = loaded != null ? loaded : new HashMap<>();
    }

    public static synchronized LeaderboardManager getInstance() {
        if (instance == null) {
            instance = new LeaderboardManager(new FileScoreStorage());
        }
        return instance;
    }

    /**
     * Replaces the singleton with injectable storage for isolated tests.
     */
    public static synchronized void resetInstance(IScoreStorage storage) {
        instance = new LeaderboardManager(storage != null ? storage : new FileScoreStorage());
    }

    /**
     * Sorts and trims a preset leaderboard, then requests persistence.
     * @return the retained one-based rank, or zero if excluded or outside the entry limit
     */
    public synchronized int addScore(DifficultyLevel level, ScoreRecord record) {
        if (level == null || level == DifficultyLevel.CUSTOM || record == null) return 0;

        List<ScoreRecord> scores = leaderboards.computeIfAbsent(level, k -> new ArrayList<>());
        scores.add(record);
        Collections.sort(scores);
        while (scores.size() > MAX_ENTRIES) {
            scores.remove(scores.size() - 1);
        }
        storage.saveScores(leaderboards);
        return indexOfIdentity(scores, record) + 1;
    }

    /**
     * Replaces the exact retained record with a renamed copy, preserving performance.
     * @return the replacement, or null if the target is not retained
     */
    public synchronized ScoreRecord renameScore(DifficultyLevel level, ScoreRecord target, String newName) {
        String name = PlayerNames.normalize(newName);
        List<ScoreRecord> scores = leaderboards.get(level);
        if (scores == null || target == null) return null;
        int idx = indexOfIdentity(scores, target);
        if (idx < 0) return null;

        ScoreRecord renamed = target.withPlayerName(name);
        scores.set(idx, renamed);
        storage.saveScores(leaderboards);
        return renamed;
    }

    public synchronized void clear(DifficultyLevel level) {
        if (leaderboards.remove(level) != null) {
            storage.saveScores(leaderboards);
        }
    }

    public synchronized void clearAll() {
        leaderboards.clear();
        storage.saveScores(leaderboards);
    }

    /**
     * Returns an unmodifiable copy so callers cannot reorder stored scores.
     */
    public synchronized List<ScoreRecord> getTopScores(DifficultyLevel level) {
        List<ScoreRecord> scores = leaderboards.get(level);
        if (scores == null) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(scores));
    }

    /**
     * Matches the original entry by reference, not by equal performance values.
     */
    private static int indexOfIdentity(List<ScoreRecord> list, ScoreRecord target) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == target) return i;
        }
        return -1;
    }
}
