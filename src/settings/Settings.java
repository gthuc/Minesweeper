package settings;

import model.DifficultyLevel;
import util.Json;
import util.PlayerNames;

/** Cài đặt người chơi. Setter tự kiểm tra hợp lệ và ném IllegalArgumentException nếu sai. */
public class Settings {
    private Theme theme = Theme.CLASSIC;
    private String playerName = "Player";
    private DifficultyLevel defaultDifficulty = DifficultyLevel.EASY;
    private int customSize = 10;
    private int customMines = 15;
    private boolean soundEnabled = true;

    public Theme getTheme() { return theme; }
    public String getPlayerName() { return playerName; }
    public DifficultyLevel getDefaultDifficulty() { return defaultDifficulty; }
    public int getCustomSize() { return customSize; }
    public int getCustomMines() { return customMines; }
    public boolean isSoundEnabled() { return soundEnabled; }

    public void setTheme(Theme theme) {
        if (theme == null) throw new IllegalArgumentException("Chưa chọn giao diện.");
        this.theme = theme;
    }

    public void setPlayerName(String name) {
        this.playerName = PlayerNames.normalize(name);
    }

    public void setDefaultDifficulty(DifficultyLevel level) {
        if (level == null) throw new IllegalArgumentException("Chưa chọn độ khó.");
        this.defaultDifficulty = level;
    }

    public void setCustom(int size, int mines) {
        String error = DifficultyLevel.validateCustom(size, mines);
        if (error != null) throw new IllegalArgumentException(error);
        this.customSize = size;
        this.customMines = mines;
    }

    public void setSoundEnabled(boolean soundEnabled) { this.soundEnabled = soundEnabled; }

    public Settings copy() {
        Settings c = new Settings();
        c.theme = theme;
        c.playerName = playerName;
        c.defaultDifficulty = defaultDifficulty;
        c.customSize = customSize;
        c.customMines = customMines;
        c.soundEnabled = soundEnabled;
        return c;
    }

    public String toJson() {
        return "{\"theme\":" + Json.quote(theme.name())
                + ",\"playerName\":" + Json.quote(playerName)
                + ",\"defaultDifficulty\":" + Json.quote(defaultDifficulty.name())
                + ",\"customSize\":" + customSize
                + ",\"customMines\":" + customMines
                + ",\"soundEnabled\":" + soundEnabled + "}";
    }
}
