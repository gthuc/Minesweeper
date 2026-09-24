package minesweeper;

import java.util.*;

public class LeaderboardManager {
    private static LeaderboardManager instance;
    
    private IScoreStorage storage;
    private Map<DifficultyLevel, List<ScoreRecord>> leaderboards;

    private LeaderboardManager() {
        this.storage = new FileScoreStorage();
        this.leaderboards = storage.LoadScores();
        
        if (this.leaderboards == null) {
            this.leaderboards = new HashMap<>();
        }
    }

    public static LeaderboardManager Instance() {
        if (instance == null) {
            instance = new LeaderboardManager();
        }
        return instance;
    }

    public void AddScore(DifficultyLevel level, ScoreRecord record) {
        List<ScoreRecord> scores = leaderboards.computeIfAbsent(level, k -> new ArrayList<>());
        
        scores.add(record);
        
        Collections.sort(scores); 
        
        if (scores.size() > 10) {
            scores = new ArrayList<>(scores.subList(0, 10));
            leaderboards.put(level, scores);
        }
        
        storage.SaveScores(leaderboards);
    }

    public List<ScoreRecord> getTopScores(DifficultyLevel level) {
        return leaderboards.getOrDefault(level, new ArrayList<>());
    }
}