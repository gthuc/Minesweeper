package controller;

import command.CommandManager;
import command.FlagCommand;
import command.ICommand;
import command.RevealCommand;
import hint.HintResult;
import model.CellModel;
import model.DifficultyLevel;
import model.GameBoardModel;
import model.GameStatus;
import observer.GameEvent;
import observer.IGameBoardObserver;
import observer.IGameEventListener;
import settings.Settings;
import settings.SettingsManager;
import settings.Theme;
import state.IGameState;
import state.InactiveState;
import state.PlayingState;
import storage.LeaderboardManager;
import storage.ScoreRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * Single entry point for the backend. All public methods are synchronized for thread safety.
 * UI receives notifications via {@link IGameEventListener} and reads state via {@link #getSnapshot()}.
 */
public class GameManager {
    public static final int MAX_MINES_HIT = 3;

    private static GameManager instance;

    private GameBoardModel gameBoard;       // null = at menu
    private DifficultyLevel difficultyLevel; // last game configuration (for restart)
    private int size;
    private int mineCount;
    private Theme activeTheme = Theme.CLASSIC;
    private IGameState currentState = new InactiveState(GameStatus.NOT_STARTED); // single source of truth for state
    private final List<IGameBoardObserver> boardObservers = new ArrayList<>();
    private final List<IGameEventListener> eventListeners = new ArrayList<>();

    private boolean hintUsed;
    private int lastReportedMinesHit;
    private ScoreRecord lastRecord;
    private int lastRank;

    // Timer: runs only during PLAYING state after first click. Uses monotonic clock (nanoTime).
    private LongSupplier clock = System::nanoTime;
    private boolean timerStarted;
    private boolean running;
    private long runningSince;
    private long accumulatedNanos;

    private GameManager() {}

    public static synchronized GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    // ---------- Menu / Start / End ----------

    /** "Play" button: starts a game based on difficulty and settings. */
    public synchronized void startConfiguredGame() {
        Settings s = SettingsManager.getInstance().get();
        DifficultyLevel d = s.getDefaultDifficulty();
        if (d == DifficultyLevel.CUSTOM) {
            startGame(d, s.getCustomSize(), s.getCustomMines());
        } else {
            startGame(d);
        }
    }

    /** Start a game with fixed difficulty (EASY/MEDIUM/HARD). CUSTOM must use the 3-parameter version. */
    public synchronized void startGame(DifficultyLevel difficulty) {
        if (difficulty == DifficultyLevel.CUSTOM) {
            throw new IllegalArgumentException("CUSTOM difficulty requires size and mine count.");
        }
        startGame(difficulty, 0, 0);
    }

    public synchronized void startGame(DifficultyLevel difficulty, int customSize, int customMines) {
        if (difficulty == null) {
            throw new IllegalArgumentException("Difficulty not selected");
        }
        if (difficulty == DifficultyLevel.CUSTOM) {
            String error = DifficultyLevel.validateCustom(customSize, customMines);
            if (error != null) throw new IllegalArgumentException(error);
            beginGame(difficulty, customSize, customMines);
        } else {
            beginGame(difficulty, difficulty.getSize(), difficulty.getMines());
        }
    }

    /** Restart using the configuration of the last game. */
    public synchronized void restart() {
        if (difficultyLevel == null) {
            throw new IllegalStateException("No previous game to restart.");
        }
        beginGame(difficultyLevel, size, mineCount);
    }

    /** Abandon the current game (if any) and return to menu. Does not record score. */
    public synchronized void exitToMenu() {
        stopTimer();
        CommandManager.getInstance().clear();
        gameBoard = null;
        timerStarted = false;
        accumulatedNanos = 0;
        hintUsed = false;
        lastReportedMinesHit = 0;
        lastRecord = null;
        lastRank = 0;
        transitionTo(new InactiveState(GameStatus.NOT_STARTED));
        fire(GameEvent.RETURNED_TO_MENU, "Returned to menu.");
    }

    private void beginGame(DifficultyLevel difficulty, int newSize, int newMines) {
        // Create the board first: if it fails, current state is untouched.
        Theme theme = SettingsManager.getInstance().get().getTheme();
        GameBoardModel board = new GameBoardModel(theme.createFactory());
        board.initialize(newSize, newMines);

        this.size = newSize;
        this.mineCount = newMines;
        this.difficultyLevel = difficulty;
        this.activeTheme = theme;
        this.hintUsed = false;
        this.lastReportedMinesHit = 0;
        this.lastRecord = null;
        this.lastRank = 0;
        this.timerStarted = false;
        this.running = false;
        this.accumulatedNanos = 0;
        CommandManager.getInstance().clear();

        this.gameBoard = board;
        for (IGameBoardObserver observer : boardObservers) {
            gameBoard.registerObserver(observer);
        }

        transitionTo(new PlayingState());
        fire(GameEvent.GAME_STARTED, "New game started: " + difficulty + " (" + size + "x" + size
                + ", " + mineCount + " mines).");
    }

    /** Internal use only (via checkWinCondition) so external callers cannot force a win. */
    private void endGame(boolean isWin) {
        if (getGameStatus() != GameStatus.PLAYING) return; // prevent double end/score recording
        transitionTo(new InactiveState(isWin ? GameStatus.WON : GameStatus.LOST));
        if (isWin) {
            recordScore();
            String rank = lastRank > 0 ? " Rank #" + lastRank + " on the leaderboard." : "";
            fire(GameEvent.WON, "You won! Time: " + getElapsedSeconds() + "s." + rank);
        } else {
            fire(GameEvent.LOST, "Out of lives! You lost.");
        }
    }

    /** Only record scores for fixed difficulties; CUSTOM sizes vary too much to compare. */
    private void recordScore() {
        lastRecord = null;
        lastRank = 0;
        if (difficultyLevel == DifficultyLevel.CUSTOM) return;

        ScoreRecord record = new ScoreRecord(SettingsManager.getInstance().get().getPlayerName(),
                (int) getElapsedSeconds(), gameBoard.getRevealedMineCount(), hintUsed);
        int rank = LeaderboardManager.getInstance().addScore(difficultyLevel, record);
        if (rank > 0) {
            lastRecord = record;
            lastRank = rank;
        }
    }

    /** Rename the record of the last win (e.g., player enters name after winning). */
    public synchronized boolean renameLastScore(String newName) {
        if (lastRecord == null || difficultyLevel == null) return false;
        ScoreRecord renamed = LeaderboardManager.getInstance().renameScore(difficultyLevel, lastRecord, newName);
        if (renamed == null) return false;
        lastRecord = renamed;
        return true;
    }

    // ---------- Actions (delegated to State) ----------

    /** Reveal a cell. Ignored if at menu. Better to use this than creating a RevealCommand externally. */
    public synchronized void reveal(int x, int y) {
        if (gameBoard == null) return;
        handleCellAction(new RevealCommand(gameBoard, x, y));
    }

    /** Toggle flag. Ignored if at menu. */
    public synchronized void toggleFlag(int x, int y) {
        if (gameBoard == null) return;
        handleCellAction(new FlagCommand(gameBoard, x, y));
    }

    public synchronized void handleCellAction(ICommand command) {
        if (command == null) return;
        currentState.handleCommand(this, command);

        // Timer starts from the first click
        if (!timerStarted && gameBoard != null && gameBoard.isMinesGenerated()
                && getGameStatus() == GameStatus.PLAYING) {
            timerStarted = true;
            startTimer();
        }
    }

    public synchronized HintResult useHint() {
        return currentState.useHint(this);
    }

    /** @return true if an actual change was undone. */
    public synchronized boolean undo() {
        return currentState.undo(this);
    }

    public synchronized void pause() {
        GameStatus before = getGameStatus();
        currentState.pause(this);
        if (before != GameStatus.PAUSED && getGameStatus() == GameStatus.PAUSED) {
            fire(GameEvent.PAUSED, "Game paused.");
        }
    }

    public synchronized void resume() {
        GameStatus before = getGameStatus();
        currentState.resume(this);
        if (before == GameStatus.PAUSED && getGameStatus() == GameStatus.PLAYING) {
            fire(GameEvent.RESUMED, "Game resumed.");
        }
    }

    /** Called by PlayingState after every command. */
    public synchronized void checkWinCondition() {
        if (getGameStatus() != GameStatus.PLAYING || gameBoard == null) return;

        int minesHit = gameBoard.getRevealedMineCount();
        int safeCells = size * size - mineCount;

        if (minesHit >= MAX_MINES_HIT) {
            endGame(false);
        } else if (gameBoard.getRevealedSafeCount() == safeCells) {
            endGame(true);
        } else if (minesHit > lastReportedMinesHit) { // Only notify when losing a life
            lastReportedMinesHit = minesHit;
            fire(GameEvent.LIFE_LOST, "Careful! " + (MAX_MINES_HIT - minesHit) + " lives remaining.");
        }
    }

    /** For States: automatically stops/restarts timer according to the new state. */
    public synchronized void transitionTo(IGameState newState) {
        stopTimer();
        this.currentState = newState;
        if (newState.getStatus() == GameStatus.PLAYING && timerStarted) {
            startTimer();
        }
    }

    // ---------- Timer ----------

    private void startTimer() {
        if (!running) {
            running = true;
            runningSince = clock.getAsLong();
        }
    }

    private void stopTimer() {
        if (running) {
            accumulatedNanos += clock.getAsLong() - runningSince;
            running = false;
        }
    }

    public synchronized long getElapsedSeconds() {
        long total = accumulatedNanos;
        if (running) total += clock.getAsLong() - runningSince;
        return total / 1_000_000_000L;
    }

    /** For testing: inject a custom clock (in nanoseconds). Should be called when no game is running. */
    public synchronized void setClock(LongSupplier nanoClock) {
        if (nanoClock != null) this.clock = nanoClock;
    }

    // ---------- Snapshot ----------

    public synchronized GameSnapshot getSnapshot() {
        GameStatus status = getGameStatus();
        if (gameBoard == null) {
            return new GameSnapshot(status, false, difficultyLevel, 0, 0, 0, MAX_MINES_HIT, MAX_MINES_HIT,
                    0, false, activeTheme.name(), "", "", Collections.<String>emptyList(), 0);
        }

        List<String> rows = new ArrayList<>();
        if (status != GameStatus.PAUSED) { // Hide board if paused
            boolean showMines = status == GameStatus.LOST;
            for (int i = 0; i < size; i++) {
                StringBuilder sb = new StringBuilder(size);
                for (int j = 0; j < size; j++) {
                    sb.append(cellCode(gameBoard.getCell(i, j), showMines));
                }
                rows.add(sb.toString());
            }
        }
        CellModel sample = gameBoard.getCell(0, 0);
        return new GameSnapshot(status, true, difficultyLevel, size, mineCount,
                gameBoard.getRemainingMines(), getLivesRemaining(), MAX_MINES_HIT,
                getElapsedSeconds(), hintUsed, activeTheme.name(),
                sample.getMineSymbol(), sample.getFlagSymbol(), rows, lastRank);
    }

    private static char cellCode(CellModel c, boolean showMines) {
        if (c.isRevealed()) {
            if (c.isMine()) return '*';
            int n = c.getAdjacentMinesCount();
            return n == 0 ? '.' : (char) ('0' + n);
        }
        if (c.isFlagged()) return (showMines && !c.isMine()) ? 'X' : 'F';
        return (showMines && c.isMine()) ? '*' : '#';
    }

    // ---------- Observer / Listener ----------

    /** Board observers are automatically re-registered at the start of a new game. */
    public synchronized void registerBoardObserver(IGameBoardObserver observer) {
        if (observer == null || boardObservers.contains(observer)) return;
        boardObservers.add(observer);
        if (gameBoard != null) gameBoard.registerObserver(observer);
    }

    public synchronized void registerEventListener(IGameEventListener listener) {
        if (listener != null && !eventListeners.contains(listener)) eventListeners.add(listener);
    }

    public synchronized void removeEventListener(IGameEventListener listener) {
        eventListeners.remove(listener);
    }

    private void fire(GameEvent event, String message) {
        for (IGameEventListener l : new ArrayList<>(eventListeners)) {
            l.onGameEvent(event, message);
        }
    }

    // ---------- Getters & Setters ----------

    public synchronized void setHintUsed(boolean used) { this.hintUsed = used; }
    public synchronized boolean isHintUsed() { return hintUsed; }
    public synchronized GameStatus getGameStatus() { return currentState.getStatus(); }
    public synchronized DifficultyLevel getDifficultyLevel() { return difficultyLevel; }
    public synchronized GameBoardModel getGameBoard() { return gameBoard; }
    public synchronized int getBoardSize() { return size; }
    public synchronized int getMineCount() { return mineCount; }
    public synchronized int getLastRank() { return lastRank; }
    public synchronized ScoreRecord getLastRecord() { return lastRecord; }

    public synchronized int getLivesRemaining() {
        return gameBoard == null ? MAX_MINES_HIT : MAX_MINES_HIT - gameBoard.getRevealedMineCount();
    }
}