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

/**
 * Loads UTF-8 properties with defaults for missing or invalid settings.
 * Saves through a sibling temporary file, requesting an atomic replacement when supported.
 */
public class FileSettingsStorage implements ISettingsStorage {
    private static final String DEFAULT_PATH = "settings.properties";

    private final Path path;

    public FileSettingsStorage() {
        this(System.getProperty("minesweeper.settings", DEFAULT_PATH));
    }

    public FileSettingsStorage(String filePath) {
        this.path = Paths.get(filePath);
    }

    /**
     * Loads fields independently so one invalid property does not discard valid fields.
     */
    @Override
    public Settings load() {
        Settings s = new Settings();
        if (!Files.exists(path)) return s;

        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Could not load settings; using defaults: " + e);
            return s;
        }

        // Parse each preference independently, retaining defaults for invalid values.
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

    /**
     * Writes UTF-8 settings via a temporary file; I/O failures are logged.
     */
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
            // Fall back to replacement on file systems that cannot perform an atomic move.
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Could not save settings: " + e.getMessage());
        }
    }
}
