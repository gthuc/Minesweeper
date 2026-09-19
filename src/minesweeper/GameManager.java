package minesweeper;

public class GameManager {
    private static GameManager instance;

    private GameBoardModel gameBoard;
    private GameStatus gameStatus;
    private DifficultyLevel difficultyLevel;
    private int size; 
    private int mineCount;

    private GameManager() {
        gameStatus = GameStatus.NotStarted;
    }

    public static GameManager Instance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    public void StartGame(DifficultyLevel difficulty) {
        this.difficultyLevel = difficulty;
        this.gameStatus = GameStatus.Playing;

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
                size = 10;
                mineCount = 15;
                break;
        }

        gameBoard = new GameBoardModel();
        gameBoard.Initialize(size, mineCount);
        
        System.out.println("Game Started! Difficulty: " + difficulty);
    }

    public void EndGame(boolean isWin) {
        if (isWin) {
            gameStatus = GameStatus.Won;
            System.out.println("You Won!");
        } else {
            gameStatus = GameStatus.Lost;
            System.out.println("Game Over! You hit a mine.");
        }
    }

    public void HandleCellAction(ICommand command) {
        if (gameStatus != GameStatus.Playing) {
            return; 
        }
        CommandManager.Instance().ExecuteCommand(command);
        CheckWinCondition();
    }

    private void CheckWinCondition() {
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

        if (revealedMinesCount >= 3) {
            System.out.println("You have run out of lives!");
            EndGame(false);
        } 
        else if (revealedSafeCellsCount == safeCellsCount) {
            EndGame(true);
        } 
        else if (revealedMinesCount > 0 && gameStatus == GameStatus.Playing) {
            System.out.println("Watch out! You have" + (3 - revealedMinesCount) + " lives remaining.");
        }
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public DifficultyLevel getDifficultyLevel() {
        return difficultyLevel;
    }
    
    public GameBoardModel getGameBoard() {
        return gameBoard;
    }
}