package settings;

import model.DifficultyLevel;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Lưu dạng .properties (UTF-8) cho dễ đọc/sửa tay; giá trị sai sẽ rơi về mặc định. */
public class FileSettingsStorage implements ISettingsStorage {
    private static final String DEFAULT_PATH = "settings.properties";

    private final Path path;

    /** Đường dẫn lấy từ system property "minesweeper.settings" nếu có (tiện cho test). */
    public FileSettingsStorage() {
        this(System.getProperty("minesweeper.settings", DEFAULT_PATH));
    }

    public FileSettingsStorage(String filePath) {
        this.path = Paths.get(filePath);
    }

    @Override
    public Settings load() {
        Settings s = new Settings();
        if (!Files.exists(path)) return s;

        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Không đọc được cài đặt, dùng mặc định: " + e);
            return s;
        }

        // Từng khoá độc lập: một giá trị hỏng không làm mất các giá trị còn lại.
        try { s.setTheme(Theme.valueOf(p.getProperty("theme").trim().toUpperCase())); } catch (RuntimeException ignored) {}
        try { s.setPlayerName(p.getProperty("playerName")); } catch (RuntimeException ignored) {}
        try { s.setDefaultDifficulty(DifficultyLevel.valueOf(p.getProperty("defaultDifficulty").trim().toUpperCase())); } catch (RuntimeException ignored) {}
        try {
            s.setCustom(Integer.parseInt(p.getProperty("customSize").trim()),
                    Integer.parseInt(p.getProperty("customMines").trim()));
        } catch (RuntimeException ignored) {}
        try { s.setSoundEnabled(Boolean.parseBoolean(p.getProperty("soundEnabled").trim())); } catch (RuntimeException ignored) {}
        return s;
    }

    @Override
    public void save(Settings s) {
        Properties p = new Properties();
        p.setProperty("theme", s.getTheme().name());
        p.setProperty("playerName", s.getPlayerName());
        p.setProperty("defaultDifficulty", s.getDefaultDifficulty().name());
        p.setProperty("customSize", String.valueOf(s.getCustomSize()));
        p.setProperty("customMines", String.valueOf(s.getCustomMines()));
        p.setProperty("soundEnabled", String.valueOf(s.isSoundEnabled()));

        Path tmp = Paths.get(path.toString() + ".tmp");
        try {
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                p.store(w, "Minesweeper settings");
            }
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Lỗi lưu cài đặt: " + e.getMessage());
        }
    }
}
