package controller;

import command.CommandManager;
import command.ICommand;
import factory.ICellFactory;
import factory.StandardBoardFactory;
import hint.HintResult;
import model.DifficultyLevel;
import model.GameBoardModel;
import model.GameStatus;
import observer.IGameBoardObserver;
import state.IGameState;
import state.InactiveState;
import state.PlayingState;
import storage.LeaderboardManager;
import storage.ScoreRecord;

import java.util.ArrayList;
import java.util.List;

public class GameManager {
    public static final int MAX_MINES_HIT = 3;

    private static GameManager instance;

    private GameBoardModel gameBoard;
    private DifficultyLevel difficultyLevel;
    private IGameState currentState = new InactiveState(GameStatus.NotStarted); // nguồn trạng thái duy nhất
    private ICellFactory cellFactory = new StandardBoardFactory();
    private final List<IGameBoardObserver> boardObservers = new ArrayList<>();

    private boolean hintUsed;
    private String playerName = "Player";
    private int size;
    private int mineCount;
    private int lastReportedMinesHit;

    // Timer: chỉ chạy khi đang Playing và đã click lần đầu
    private boolean timerStarted;
    private long accumulatedMillis;
    private long runningSince = -1;

    private GameManager() {}

    public static synchronized GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    // ---------- Bắt đầu / kết thúc ----------

    public void startGame(DifficultyLevel difficulty) {
        startGame(difficulty, 10, 15); // giá trị mặc định cho CUSTOM
    }

    public void startGame(DifficultyLevel difficulty, int customSize, int customMines) {
        if (difficulty == null) {
            throw new IllegalArgumentException("Chưa chọn độ khó");
        }

        int newSize;
        int newMines;
        switch (difficulty) {
            case EASY:
                newSize = 9;
                newMines = 10;
                break;
            case MEDIUM:
                newSize = 16;
                newMines = 40;
                break;
            case HARD:
                newSize = 24;
                newMines = 99;
                break;
            case CUSTOM:
                if (customSize < 5 || customSize > 30
                        || customMines < 1 || customMines > customSize * customSize - 9) {
                    throw new IllegalArgumentException("Tham số CUSTOM không hợp lệ");
                }
                newSize = customSize;
                newMines = customMines;
                break;
            default:
                throw new IllegalArgumentException("Độ khó không được hỗ trợ: " + difficulty);
        }

        this.size = newSize;
        this.mineCount = newMines;
        this.difficultyLevel = difficulty;
        this.hintUsed = false;
        this.lastReportedMinesHit = 0;
        this.timerStarted = false;
        this.accumulatedMillis = 0;
        this.runningSince = -1;
        CommandManager.getInstance().clear();

        gameBoard = new GameBoardModel(cellFactory);
        gameBoard.initialize(size, mineCount);
        for (IGameBoardObserver observer : boardObservers) {
            gameBoard.registerObserver(observer);
        }

        transitionTo(new PlayingState());
        System.out.println("Game Started! Difficulty: " + difficulty);
    }

    public void endGame(boolean isWin) {
        transitionTo(new InactiveState(isWin ? GameStatus.Won : GameStatus.Lost));
        if (isWin) {
            System.out.println("You Won! Thời gian: " + getElapsedSeconds() + "s");
            recordScore();
        } else {
            System.out.println("Game Over! You hit too many mines.");
        }
    }

    /** Chỉ ghi điểm các mức cố định; CUSTOM có kích thước khác nhau nên không so sánh được. */
    private void recordScore() {
        if (difficultyLevel == DifficultyLevel.CUSTOM) return;
        LeaderboardManager.getInstance().addScore(difficultyLevel,
                new ScoreRecord(playerName, (int) getElapsedSeconds(),
                        gameBoard.getRevealedMineCount(), hintUsed));
    }

    // ---------- Thao tác (uỷ quyền cho State) ----------

    public void handleCellAction(ICommand command) {
        if (command == null) return;
        currentState.handleCommand(this, command);

        // Timer bắt đầu từ lần click đầu tiên
        if (!timerStarted && gameBoard != null && gameBoard.isMinesGenerated()
                && getGameStatus() == GameStatus.Playing) {
            timerStarted = true;
            startTimer();
        }
    }

    public HintResult useHint() {
        return currentState.useHint(this);
    }

    public void undo() {
        currentState.undo(this);
    }

    public void pause() {
        currentState.pause(this);
    }

    public void resume() {
        currentState.resume(this);
    }

    /** Được PlayingState gọi sau mỗi lệnh. */
    public void checkWinCondition() {
        if (getGameStatus() != GameStatus.Playing) return;

        int minesHit = gameBoard.getRevealedMineCount();
        int safeCells = size * size - mineCount;

        if (minesHit >= MAX_MINES_HIT) {
            System.out.println("You have run out of lives!");
            endGame(false);
        } else if (gameBoard.getRevealedSafeCount() == safeCells) {
            endGame(true);
        } else if (minesHit > lastReportedMinesHit) { // chỉ báo khi vừa mất thêm mạng
            lastReportedMinesHit = minesHit;
            System.out.println("Watch out! You have " + (MAX_MINES_HIT - minesHit) + " lives remaining.");
        }
    }

    /** Dành cho các State; tự đóng băng/khởi động lại timer theo trạng thái mới. */
    public void transitionTo(IGameState newState) {
        stopTimer();
        this.currentState = newState;
        if (newState.getStatus() == GameStatus.Playing && timerStarted) {
            startTimer();
        }
    }

    // ---------- Timer ----------

    private void startTimer() {
        if (runningSince < 0) runningSince = System.currentTimeMillis();
    }

    private void stopTimer() {
        if (runningSince >= 0) {
            accumulatedMillis += System.currentTimeMillis() - runningSince;
            runningSince = -1;
        }
    }

    public long getElapsedSeconds() {
        long total = accumulatedMillis;
        if (runningSince >= 0) total += System.currentTimeMillis() - runningSince;
        return total / 1000;
    }

    // ---------- Cấu hình / Getter ----------

    public void setCellFactory(ICellFactory factory) {
        if (factory != null) this.cellFactory = factory;
    }

    /** Observer được đăng ký lại tự động mỗi khi bắt đầu ván mới. */
    public void registerBoardObserver(IGameBoardObserver observer) {
        if (observer == null || boardObservers.contains(observer)) return;
        boardObservers.add(observer);
        if (gameBoard != null) gameBoard.registerObserver(observer);
    }

    public void setPlayerName(String name) {
        if (name != null && !name.trim().isEmpty()) this.playerName = name.trim();
    }

    public void setHintUsed(boolean used) { this.hintUsed = used; }
    public boolean isHintUsed() { return hintUsed; }
    public GameStatus getGameStatus() { return currentState.getStatus(); }
    public DifficultyLevel getDifficultyLevel() { return difficultyLevel; }
    public GameBoardModel getGameBoard() { return gameBoard; }
    public String getPlayerName() { return playerName; }

    public int getLivesRemaining() {
        return gameBoard == null ? MAX_MINES_HIT : MAX_MINES_HIT - gameBoard.getRevealedMineCount();
    }
}
