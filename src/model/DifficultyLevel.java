package model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


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


    public int getSize() { return size; }


    public int getMines() { return mines; }

    public boolean isPreset() { return this != CUSTOM; }


    public static List<DifficultyLevel> presets() {
        return Collections.unmodifiableList(Arrays.asList(EASY, MEDIUM, HARD));
    }


    public static int maxCustomMines(int size) {
        return size * size - 9;
    }


    public static String validateCustom(int size, int mines) {
        if (size < CUSTOM_MIN_SIZE || size > CUSTOM_MAX_SIZE) {
            return "Kích thước phải từ " + CUSTOM_MIN_SIZE + " đến " + CUSTOM_MAX_SIZE + ".";
        }
        int max = maxCustomMines(size);
        if (mines < 1 || mines > max) {
            return "Số mìn phải từ 1 đến " + max + " với bàn " + size + "x" + size + ".";
        }
        return null;
    }
}
