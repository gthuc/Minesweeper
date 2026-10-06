package storage;

import model.DifficultyLevel;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileScoreStorage implements IScoreStorage {
    private static final String DEFAULT_PATH = "leaderboard.dat";

    private final Path path;

    /** Đường dẫn lấy từ system property "minesweeper.scores" nếu có (tiện cho test). */
    public FileScoreStorage() {
        this(System.getProperty("minesweeper.scores", DEFAULT_PATH));
    }

    public FileScoreStorage(String filePath) {
        this.path = Paths.get(filePath);
    }

    @Override
    public void saveScores(Map<DifficultyLevel, List<ScoreRecord>> scores) {
        // Ghi ra file tạm rồi đổi tên: nếu lỗi giữa chừng thì file cũ không bị hỏng.
        Path tmp = Paths.get(path.toString() + ".tmp");
        try {
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(tmp))) {
                oos.writeObject(scores);
            }
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Lỗi lưu điểm: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<DifficultyLevel, List<ScoreRecord>> loadScores() {
        if (!Files.exists(path)) {
            return new HashMap<>(); // lần chạy đầu tiên: bình thường
        }
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(path))) {
            return (Map<DifficultyLevel, List<ScoreRecord>>) ois.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            // File hỏng: giữ lại bản sao thay vì lặng lẽ ghi đè mất dữ liệu cũ.
            Path backup = Paths.get(path.toString() + ".corrupt");
            try {
                Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
                System.err.println("Không đọc được bảng điểm (" + e + "). Đã sao lưu sang " + backup);
            } catch (IOException moveError) {
                System.err.println("Không đọc được bảng điểm: " + e);
            }
            return new HashMap<>();
        }
    }
}
