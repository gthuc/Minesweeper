package hint;

import model.GameBoardModel;

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

    public static synchronized HintSystem getInstance() {
        if (instance == null) {
            instance = new HintSystem();
        }
        return instance;
    }

    public HintResult getHint(GameBoardModel board) {
        if (board == null) return null;

        // Chưa click lần đầu: mìn chưa được sinh, và ô đầu tiên luôn an toàn.
        if (!board.isMinesGenerated()) {
            int mid = board.getSize() / 2;
            return new HintResult(mid, mid, HintType.CERTAIN_SAFE);
        }

        for (IHintStrategy strategy : strategies) {
            HintResult result = strategy.getHintPosition(board);
            if (result != null) return result;
        }
        return null;
    }
}
