package minesweeper;

public class HintSystem {
    private static HintSystem instance;
    private IHintStrategy currentStrategy;

    private HintSystem() {
        // Gắn mặc định một chiến lược khi khởi tạo
        this.currentStrategy = new SafeHintStrategy(); 
    }

    public static HintSystem Instance() {
        if (instance == null) {
            instance = new HintSystem();
        }
        return instance;
    }

    // Hàm cho phép đổi thuật toán linh hoạt ngay trong lúc chơi
    public void setStrategy(IHintStrategy strategy) {
        this.currentStrategy = strategy;
    }

    // Hàm này sẽ được GameManager hoặc PlayingState gọi
    public HintResult GetHint(GameBoardModel board) {
        if (currentStrategy == null) return null;
        return currentStrategy.GetHintPosition(board);
    }
}