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

/**
 * Persists leaderboard maps using Java object serialization.
 * Writes through a temporary file and preserves unreadable input as a .corrupt backup
 * when possible, returning an empty leaderboard on supported load failures.
 */
public class FileScoreStorage implements IScoreStorage {
    private static final String DEFAULT_PATH = "resources/data/leaderboard.dat";

    private final Path path;

    public FileScoreStorage() {
        this(System.getProperty("minesweeper.scores", DEFAULT_PATH));
    }

    public FileScoreStorage(String filePath) {
        this.path = Paths.get(filePath);
    }

    /**
     * Serializes scores before replacing the destination; I/O failures are logged.
     */
    @Override
    public void saveScores(Map<DifficultyLevel, List<ScoreRecord>> scores) {
        Path tmp = Paths.get(path.toString() + ".tmp");
        try {
            Path parent = path.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(tmp))) {
                oos.writeObject(scores);
            }
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Could not save scores: " + e.getMessage());
        }
    }

    /**
     * Loads serialized scores, backing up unreadable input when possible.
     * @return loaded scores, or an empty map if missing or unreadable
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<DifficultyLevel, List<ScoreRecord>> loadScores() {
        if (!Files.exists(path)) {
            return new HashMap<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(path))) {
            return (Map<DifficultyLevel, List<ScoreRecord>>) ois.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            // Preserve the unreadable file for diagnosis instead of silently overwriting it.
            Path backup = Paths.get(path.toString() + ".corrupt");
            try {
                Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
                System.err.println("Could not load leaderboard (" + e + "). Backed up to " + backup);
            } catch (IOException moveError) {
                System.err.println("Could not load leaderboard: " + e);
            }
            return new HashMap<>();
        }
    }
}
