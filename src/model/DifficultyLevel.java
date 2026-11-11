package model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Defines preset board dimensions and bounds for custom configurations.
 * CUSTOM stores placeholder dimensions; callers must supply validated values.
 */
public enum DifficultyLevel {
    EASY(9, 10),
    MEDIUM(16, 40),
    HARD(24, 99),
    CUSTOM(0, 0);

    public static final int CUSTOM_MIN_SIZE = 5;
    public static final int CUSTOM_MAX_SIZE = 30;

    private final int size;
    private final int mines;

    DifficultyLevel(int size, int mines) {
        this.size = size;
        this.mines = mines;
    }

    public int getSize() {
        return size;
    }

    public int getMines() {
        return mines;
    }

    public boolean isPreset() {
        return this != CUSTOM;
    }

    /**
     * Returns the fixed difficulties eligible for comparable leaderboard scores.
     */
    public static List<DifficultyLevel> presets() {
        return Collections.unmodifiableList(Arrays.asList(EASY, MEDIUM, HARD));
    }

    /**
     * Reserves nine cells so any first-click neighborhood can remain mine-free.
     */
    public static int maxCustomMines(int size) {
        return size * size - 9;
    }

    /**
     * Validates custom dimensions without creating a board.
     * @return an English validation message, or null for a valid configuration
     */
    public static String validateCustom(int size, int mines) {
        if (size < CUSTOM_MIN_SIZE || size > CUSTOM_MAX_SIZE) {
            return "Board size must be between " + CUSTOM_MIN_SIZE + " and " + CUSTOM_MAX_SIZE + ".";
        }
        int max = maxCustomMines(size);
        if (mines < 1 || mines > max) {
            return "Mine count must be between 1 and " + max + " for a board of size " + size + "x" + size + ".";
        }
        return null;
    }
}
