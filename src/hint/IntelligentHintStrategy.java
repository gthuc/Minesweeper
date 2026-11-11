package hint;

import model.CellModel;
import model.GameBoardModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;









public class IntelligentHintStrategy implements IHintStrategy {

    @Override
    public HintResult getHintPosition(GameBoardModel board) {
        int size = board.getSize();
        List<Constraint> constraints = Constraint.build(board);

        Map<Integer, double[]> sums = new HashMap<>();  
        for (Constraint c : constraints) {
            double p = (double) c.remaining / c.cells.size();
            for (int code : c.cells) {
                double[] s = sums.computeIfAbsent(code, k -> new double[2]);
                s[0] += p;
                s[1] += 1;
            }
        }

        int hidden = size * size - board.getRevealedSafeCount() - board.getRevealedMineCount();
        int minesLeft = board.getTotalMines() - board.getRevealedMineCount();
        double globalDensity = hidden > 0 ? Math.max(0, minesLeft) / (double) hidden : 1.0;

        HintResult best = null;
        double bestProb = Double.MAX_VALUE;
        HintResult bestFlagged = null;
        double bestFlaggedProb = Double.MAX_VALUE;

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                CellModel cell = board.getCell(x, y);
                if (cell.isRevealed()) continue;

                double[] s = sums.get(x * size + y);
                double prob = (s == null) ? globalDensity : s[0] / s[1];
                if (cell.isFlagged()) {
                    if (prob < bestFlaggedProb) {
                        bestFlaggedProb = prob;
                        bestFlagged = new HintResult(x, y, HintType.PROBABLE, true);
                    }
                } else if (prob < bestProb) {
                    bestProb = prob;
                    best = new HintResult(x, y, HintType.PROBABLE);
                }
            }
        }
        return best != null ? best : bestFlagged;
    }
}
