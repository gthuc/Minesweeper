package minesweeper;

import java.util.ArrayList;
import java.util.List;

/** Chuỗi chiến lược: thử lần lượt cho tới khi có strategy trả về gợi ý. */
public class HintSystem {
    private static HintSystem instance;
    private final List<IHintStrategy> strategies = new ArrayList<>();

    private HintSystem() {
        strategies.add(new SafeHintStrategy());
        strategies.add(new IntelligentHintStrategy());
    }

    public static HintSystem Instance() {
        if (instance == null) {
            instance = new HintSystem();
        }
        return instance;
    }

    public HintResult GetHint(GameBoardModel board) {
        for (IHintStrategy strategy : strategies) {
            HintResult result = strategy.GetHintPosition(board);
            if (result != null) return result;
        }
        return null;
    }
}