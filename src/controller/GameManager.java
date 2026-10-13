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
 * Điểm vào duy nhất của backend. Mọi method public đều synchronized nên có thể gọi
 * an toàn từ nhiều luồng (vd HttpServer). Không in ra màn hình: UI nhận thông báo
 * qua {@link IGameEventListener} và đọc trạng thái qua {@link #getSnapshot()}.
 */
public class GameManager {
    public static final int MAX_MINES_HIT = 3;

    private static GameManager instance;

    private GameBoardModel gameBoard;       // null = đang ở menu
    private DifficultyLevel difficultyLevel; // cấu hình ván gần nhất (để restart)
    private int size;
    private int mineCount;
    private Theme activeTheme = Theme.CLASSIC;
    private IGameState currentState = new InactiveState(GameStatus.NOT_STARTED); // nguồn trạng thái duy nhất
    private final List<IGameBoardObserver> boardObservers = new ArrayList<>();
    private final List<IGameEventListener> eventListeners = new ArrayList<>();

    private boolean hintUsed;
    private int lastReportedMinesHit;
    private ScoreRecord lastRecord;
    private int lastRank;

    // Timer: chỉ chạy khi đang PLAYING và đã click lần đầu. Dùng đồng hồ đơn điệu (nanoTime).
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

    // ---------- Menu / bắt đầu / kết thúc ----------

    /** Nút "Play": bắt đầu ván theo độ khó và giao diện trong Settings. */
    public synchronized void startConfiguredGame() {
        Settings s = SettingsManager.getInstance().get();
        DifficultyLevel d = s.getDefaultDifficulty();
        if (d == DifficultyLevel.CUSTOM) {
            startGame(d, s.getCustomSize(), s.getCustomMines());
        } else {
            startGame(d);
        }
    }

    /** Bắt đầu ván ở mức cố định (EASY/MEDIUM/HARD). CUSTOM phải dùng bản 3 tham số. */
    public synchronized void startGame(DifficultyLevel difficulty) {
        if (difficulty == DifficultyLevel.CUSTOM) {
            throw new IllegalArgumentException("Độ khó CUSTOM cần kích thước và số mìn.");
        }
        startGame(difficulty, 0, 0);
    }

    public synchronized void startGame(DifficultyLevel difficulty, int customSize, int customMines) {
        if (difficulty == null) {
            throw new IllegalArgumentException("Chưa chọn độ khó");
        }
        if (difficulty == DifficultyLevel.CUSTOM) {
            String error = DifficultyLevel.validateCustom(customSize, customMines);
            if (error != null) throw new IllegalArgumentException(error);
            beginGame(difficulty, customSize, customMines);
        } else {
            beginGame(difficulty, difficulty.getSize(), difficulty.getMines());
        }
    }

    /** Chơi lại đúng cấu hình của ván gần nhất (kể cả khi đã về menu). */
    public synchronized void restart() {
        if (difficultyLevel == null) {
            throw new IllegalStateException("Chưa có ván nào để chơi lại.");
        }
        beginGame(difficultyLevel, size, mineCount);
    }

    /** Bỏ ván hiện tại (nếu có) và về menu. Không ghi điểm. */
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
        fire(GameEvent.RETURNED_TO_MENU, "Về menu.");
    }

    private void beginGame(DifficultyLevel difficulty, int newSize, int newMines) {
        // Tạo bàn cờ trước: nếu lỗi thì trạng thái hiện tại không bị đụng tới.
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
        fire(GameEvent.GAME_STARTED, "Bắt đầu ván mới: " + difficulty + " (" + size + "x" + size
                + ", " + mineCount + " mìn).");
    }

    /** Chỉ nội bộ gọi (qua checkWinCondition) nên bên ngoài không thể "thắng giả". */
    private void endGame(boolean isWin) {
        if (getGameStatus() != GameStatus.PLAYING) return; // chống kết thúc/ghi điểm hai lần
        transitionTo(new InactiveState(isWin ? GameStatus.WON : GameStatus.LOST));
        if (isWin) {
            recordScore();
            String rank = lastRank > 0 ? " Hạng #" + lastRank + " trên bảng điểm." : "";
            fire(GameEvent.WON, "Bạn thắng! Thời gian: " + getElapsedSeconds() + "s." + rank);
        } else {
            fire(GameEvent.LOST, "Hết mạng! Bạn đã thua.");
        }
    }

    /** Chỉ ghi điểm các mức cố định; CUSTOM có kích thước khác nhau nên không so sánh được. */
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

    /** Đổi tên trên bản ghi vừa thắng (vd người chơi nhập tên sau khi thắng). */
    public synchronized boolean renameLastScore(String newName) {
        if (lastRecord == null || difficultyLevel == null) return false;
        ScoreRecord renamed = LeaderboardManager.getInstance().renameScore(difficultyLevel, lastRecord, newName);
        if (renamed == null) return false;
        lastRecord = renamed;
        return true;
    }

    // ---------- Thao tác (uỷ quyền cho State) ----------

    /** Lật ô. Bỏ qua nếu đang ở menu. Nên dùng cái này thay vì tự tạo RevealCommand bên ngoài. */
    public synchronized void reveal(int x, int y) {
        if (gameBoard == null) return;
        handleCellAction(new RevealCommand(gameBoard, x, y));
    }

    /** Bật/tắt cờ. Bỏ qua nếu đang ở menu. */
    public synchronized void toggleFlag(int x, int y) {
        if (gameBoard == null) return;
        handleCellAction(new FlagCommand(gameBoard, x, y));
    }

    public synchronized void handleCellAction(ICommand command) {
        if (command == null) return;
        currentState.handleCommand(this, command);

        // Timer bắt đầu từ lần click đầu tiên
        if (!timerStarted && gameBoard != null && gameBoard.isMinesGenerated()
                && getGameStatus() == GameStatus.PLAYING) {
            timerStarted = true;
            startTimer();
        }
    }

    public synchronized HintResult useHint() {
        return currentState.useHint(this);
    }

    /** @return true nếu có thay đổi thực sự được hoàn tác. */
    public synchronized boolean undo() {
        return currentState.undo(this);
    }

    public synchronized void pause() {
        GameStatus before = getGameStatus();
        currentState.pause(this);
        if (before != GameStatus.PAUSED && getGameStatus() == GameStatus.PAUSED) {
            fire(GameEvent.PAUSED, "Tạm dừng.");
        }
    }

    public synchronized void resume() {
        GameStatus before = getGameStatus();
        currentState.resume(this);
        if (before == GameStatus.PAUSED && getGameStatus() == GameStatus.PLAYING) {
            fire(GameEvent.RESUMED, "Tiếp tục.");
        }
    }

    /** Được PlayingState gọi sau mỗi lệnh. */
    public synchronized void checkWinCondition() {
        if (getGameStatus() != GameStatus.PLAYING || gameBoard == null) return;

        int minesHit = gameBoard.getRevealedMineCount();
        int safeCells = size * size - mineCount;

        if (minesHit >= MAX_MINES_HIT) {
            endGame(false);
        } else if (gameBoard.getRevealedSafeCount() == safeCells) {
            endGame(true);
        } else if (minesHit > lastReportedMinesHit) { // chỉ báo khi vừa mất thêm mạng
            lastReportedMinesHit = minesHit;
            fire(GameEvent.LIFE_LOST, "Cẩn thận! Còn " + (MAX_MINES_HIT - minesHit) + " mạng.");
        }
    }

    /** Dành cho các State; tự đóng băng/khởi động lại timer theo trạng thái mới. */
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

    /** Dùng cho test: thay đồng hồ (đơn vị nano giây). Nên gọi khi không có ván đang chạy. */
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
        if (status != GameStatus.PAUSED) { // tạm dừng thì giấu bàn cờ
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

    /** Observer bàn cờ được đăng ký lại tự động mỗi khi bắt đầu ván mới. */
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

    // ---------- Getter ----------

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
