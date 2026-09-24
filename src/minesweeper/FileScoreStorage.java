package minesweeper;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileScoreStorage implements IScoreStorage {
    private final String FILE_PATH = "leaderboard.dat";

    @Override
    public void SaveScores(Map<DifficultyLevel, List<ScoreRecord>> scores) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(scores);
        } catch (IOException e) {
            System.out.println("Lỗi lưu điểm: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<DifficultyLevel, List<ScoreRecord>> LoadScores() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_PATH))) {
            return (Map<DifficultyLevel, List<ScoreRecord>>) ois.readObject();
        } catch (Exception e) {
            return new HashMap<>(); 
        }
    }
}