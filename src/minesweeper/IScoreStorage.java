package minesweeper;

import java.util.List;
import java.util.Map;

public interface IScoreStorage {
    void SaveScores(Map<DifficultyLevel, List<ScoreRecord>> scores);
    Map<DifficultyLevel, List<ScoreRecord>> LoadScores();
}