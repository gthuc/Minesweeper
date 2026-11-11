package storage;

import model.DifficultyLevel;

import java.util.List;
import java.util.Map;

/**
 * Abstracts persistence of score lists grouped by difficulty.
 */
public interface IScoreStorage {
    /**
     * Persists score lists grouped by difficulty.
     */
    void saveScores(Map<DifficultyLevel, List<ScoreRecord>> scores);

    /**
     * Loads stored score lists using the implementation recovery policy.
     */
    Map<DifficultyLevel, List<ScoreRecord>> loadScores();
}
