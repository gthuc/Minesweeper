package controller;

import model.DifficultyLevel;
import model.GameStatus;
import util.Json;

import java.util.Collections;
import java.util.List;

/**
 * Carries a view-oriented representation of game state without exposing cell objects.
 * Rows use compact cell codes and are exposed through an unmodifiable list.
 * The constructor wraps the supplied list, so its creator must not modify that list afterward.
 */
public final class GameSnapshot {
    public final GameStatus status;
    public final boolean inGame;
    public final DifficultyLevel difficulty;
    public final int size;
    public final int totalMines;
    public final int remainingMines;
    public final int lives;
    public final int maxLives;
    public final long elapsedSeconds;
    public final boolean hintUsed;
    public final String theme;
    public final String mineSymbol;
    public final String flagSymbol;
    public final List<String> rows;
    public final int lastRank;

    GameSnapshot(GameStatus status, boolean inGame, DifficultyLevel difficulty, int size, int totalMines,
                 int remainingMines, int lives, int maxLives, long elapsedSeconds, boolean hintUsed,
                 String theme, String mineSymbol, String flagSymbol, List<String> rows, int lastRank) {
        this.status = status;
        this.inGame = inGame;
        this.difficulty = difficulty;
        this.size = size;
        this.totalMines = totalMines;
        this.remainingMines = remainingMines;
        this.lives = lives;
        this.maxLives = maxLives;
        this.elapsedSeconds = elapsedSeconds;
        this.hintUsed = hintUsed;
        this.theme = theme;
        this.mineSymbol = mineSymbol;
        this.flagSymbol = flagSymbol;
        this.rows = Collections.unmodifiableList(rows);
        this.lastRank = lastRank;
    }

    /**
     * Serializes snapshot fields and row codes into a JSON object.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"status\":").append(Json.quote(status.name()));
        sb.append(",\"inGame\":").append(inGame);
        sb.append(",\"difficulty\":").append(difficulty == null ? "null" : Json.quote(difficulty.name()));
        sb.append(",\"size\":").append(size);
        sb.append(",\"totalMines\":").append(totalMines);
        sb.append(",\"remainingMines\":").append(remainingMines);
        sb.append(",\"lives\":").append(lives);
        sb.append(",\"maxLives\":").append(maxLives);
        sb.append(",\"elapsedSeconds\":").append(elapsedSeconds);
        sb.append(",\"hintUsed\":").append(hintUsed);
        sb.append(",\"theme\":").append(Json.quote(theme));
        sb.append(",\"mineSymbol\":").append(Json.quote(mineSymbol));
        sb.append(",\"flagSymbol\":").append(Json.quote(flagSymbol));
        sb.append(",\"lastRank\":").append(lastRank);
        sb.append(",\"rows\":[");
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(Json.quote(rows.get(i)));
        }
        return sb.append("]}").toString();
    }
}
