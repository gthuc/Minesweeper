package minesweeper;

public class GameManager {
    private static final int MAX_MINES_HIT = 3;

    private static GameManager instance;

    private GameBoardModel gameBoard;
    private GameStatus gameStatus;
    private DifficultyLevel difficultyLevel;
    private IGameState currentState;
    private boolean isHintUsed;
    private int size;
    private int mineCount;

    private GameManager() {
        gameStatus = GameStatus.NotStarted;
        currentState = new InactiveState();
    }

    public static GameManager Instance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    public void StartGame(DifficultyLevel difficulty) {
        StartGame(difficulty, 10, 15); // giá trị mặc định cho CUSTOM
    }

    public void StartGame(DifficultyLevel difficulty, int customSize, int customMines) {
        switch (difficulty) {
            case EASY:
                size = 9;
                mineCount = 10;
                break;
            case MEDIUM:
                size = 16;
                mineCount = 40;
                break;
            case HARD:
                size = 24;
                mineCount = 99;
                break;
            case CUSTOM:
                if (customSize < 5 || customSize > 30
                        || customMines < 1 || customMines > customSize * customSize - 9) {
                    throw new IllegalArgumentException("Tham số CUSTOM không hợp lệ");
                }
                size = customSize;
                mineCount = customMines;
                break;
        }

        this.difficultyLevel = difficulty;
        this.isHintUsed = false;
        CommandManager.Instance().Clear();

        ICellFactory factory = new StandardBoardFactory();
        gameBoard = new GameBoardModel(factory);
        gameBoard.Initialize(size, mineCount);

        this.gameStatus = GameStatus.Playing;
        this.currentState = new PlayingState();

        System.out.println("Game Started! Difficulty: " + difficulty);
    }

    public void EndGame(boolean isWin) {
        currentState = new InactiveState();
        if (isWin) {
            gameStatus = GameStatus.Won;
            System.out.println("You Won!");
        } else {
            gameStatus = GameStatus.Lost;
            System.out.println("Game Over! You hit a mine.");
        }
    }

    public void HandleCellAction(ICommand command) {
        currentState.HandleCommand(this, command);
    }

    public HintResult UseHint() {
        return currentState.UseHint(this);
    }

    public void Undo() {
        if (gameStatus == GameStatus.Playing) {
            CommandManager.Instance().Undo();
        }
    }

    public void CheckWinCondition() {
        if (gameStatus != GameStatus.Playing) return;

        int revealedSafeCellsCount = 0;
        int revealedMinesCount = 0;
        int safeCellsCount = (size * size) - mineCount;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                CellModel cell = gameBoard.GetCell(i, j);
                if (cell != null && cell.isRevealed()) {
                    if (cell.isMine()) {
                        revealedMinesCount++;
                    } else {
                        revealedSafeCellsCount++;
                    }
                }
            }
        }

        if (revealedMinesCount >= MAX_MINES_HIT) {
            System.out.println("You have run out of lives!");
            EndGame(false);
        } else if (revealedSafeCellsCount == safeCellsCount) {
            EndGame(true);
        } else if (revealedMinesCount > 0) {
            System.out.println("Watch out! You have "
                    + (MAX_MINES_HIT - revealedMinesCount) + " lives remaining.");
        }
    }

    public void setHintUsed(boolean used) { this.isHintUsed = used; }
    public boolean isHintUsed() { return isHintUsed; }
    public GameStatus getGameStatus() { return gameStatus; }
    public DifficultyLevel getDifficultyLevel() { return difficultyLevel; }
    public GameBoardModel getGameBoard() { return gameBoard; }
}