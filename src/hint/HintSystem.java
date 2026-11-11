package hint;

import model.CellModel;
import model.GameBoardModel;

import java.util.ArrayList;
import java.util.List;


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


        if (!board.isMinesGenerated()) {
            int mid = board.getSize() / 2;
            return annotate(board, new HintResult(mid, mid, HintType.CERTAIN_SAFE));
        }

        for (IHintStrategy strategy : strategies) {
            HintResult result = strategy.getHintPosition(board);
            if (result != null) return annotate(board, result);
        }
        return null;
    }


    private HintResult annotate(GameBoardModel board, HintResult r) {
        CellModel cell = board.getCell(r.getX(), r.getY());
        boolean flagged = cell != null && cell.isFlagged();
        return flagged == r.isOnFlaggedCell() ? r : new HintResult(r.getX(), r.getY(), r.getType(), flagged);
    }
}
