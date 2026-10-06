package storage;

import model.DifficultyLevel;

import java.util.List;
import java.util.Map;

public interface IScoreStorage {
    void saveScores(Map<DifficultyLevel, List<ScoreRecord>> scores);

    Map<DifficultyLevel, List<ScoreRecord>> loadScores();
}
