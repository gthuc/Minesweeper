package minesweeper;

public class GameManager {
    // 1. Cài đặt Singleton Pattern
    private static GameManager instance;

    // Các thành phần quản lý của game
    private GameBoardModel gameBoard;
    private GameStatus gameStatus;
    private DifficultyLevel difficultyLevel;
    private int size; //cần viết hàm setter và getter ko???
    private int mineCount;

    // Private constructor để ngăn chặn tạo instance mới từ bên ngoài
    private GameManager() {
        gameStatus = GameStatus.NotStarted;
    }

    // Phương thức tĩnh để lấy instance duy nhất
    public static GameManager Instance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    // 2. Các hàm điều phối luồng trò chơi

    /**
     * Bắt đầu một ván chơi mới dựa trên độ khó
     */
    public void StartGame(DifficultyLevel difficulty) {
        this.difficultyLevel = difficulty;
        this.gameStatus = GameStatus.Playing;

        // Thiết lập kích thước và số mìn tùy theo độ khó
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

        // Khởi tạo bảng dữ liệu
        gameBoard = new GameBoardModel();
        gameBoard.Initialize(size, mineCount);
        
        System.out.println("Game Started! Difficulty: " + difficulty);
    }

    /**
     * Kết thúc trò chơi
     */
    public void EndGame(boolean isWin) {
        if (isWin) {
            gameStatus = GameStatus.Won;
            System.out.println("You Won!");
        } else {
            gameStatus = GameStatus.Lost;
            System.out.println("Game Over! You hit a mine.");
        }
    }

    /**
     * Hàm xử lý thao tác của người chơi, nhận vào một ICommand theo đúng chuẩn UML
     */
    public void HandleCellAction(ICommand command) {
        if (gameStatus != GameStatus.Playing) {
            return; // Nếu game chưa bắt đầu hoặc đã kết thúc thì không nhận lệnh nữa
        }

        // Đưa lệnh cho UndoManager thực thi và lưu lại lịch sử
        UndoManager.Instance().ExecuteCommand(command);

        // Sau mỗi lệnh, kiểm tra xem người chơi đã thắng hay thua chưa
        CheckWinCondition();
    }

    /**
     * Kiểm tra điều kiện thắng/thua sau mỗi lượt click
     */
    private void CheckWinCondition() {
        int revealedCount = 0;
        int safeCellsCount = (size * size) - mineCount;
        boolean hitMine = false;

        // Duyệt toàn bộ bảng để đếm số ô đã lật và kiểm tra có lật trúng mìn không
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                CellModel cell = gameBoard.GetCell(i, j);
                if (cell != null) {
                    if (cell.isRevealed()) {
                        if (cell.isMine()) {
                            hitMine = true;
                            break;
                        }
                        revealedCount++;
                    }
                }
            }
            if (hitMine) break;
        }

        // Nếu lật trúng mìn -> Thua
        if (hitMine) {
            EndGame(false);
        } 
        // Nếu tổng số ô đã lật bằng tổng số ô an toàn -> Thắng
        else if (revealedCount == safeCellsCount) {
            EndGame(true);
        }
    }

    // Các hàm getter
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