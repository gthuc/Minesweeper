package hint;

import model.GameBoardModel;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds certain moves using individual clues and subset differences.
 * Deductions depend on revealed information, not the correctness of player flags.
 */
public class SafeHintStrategy implements IHintStrategy {
    @Override
    public HintResult getHintPosition(GameBoardModel board) {
        List<Constraint> constraints = Constraint.build(board);

        HintResult hint = level1(board, constraints);
        if (hint != null) return hint;

        return level2(board, constraints);
    }

    /**
     * Uses single-clue rules: zero remaining mines means safe;
     * a mine count equal to the hidden-set size means every member is a mine.
     */
    private HintResult level1(GameBoardModel board, List<Constraint> constraints) {
        for (Constraint c : constraints) {
            if (c.remaining == 0) {
                int code = c.cells.iterator().next();
                return new HintResult(c.x(code), c.y(code), HintType.CERTAIN_SAFE);
            }
        }
        for (Constraint c : constraints) {
            if (c.remaining == c.cells.size()) {
                HintResult mine = firstUnflaggedMine(board, c, c.cells);
                if (mine != null) return mine;
            }
        }
        return null;
    }

    /**
     * Subtracts a strict subset constraint from a larger one to classify the difference.
     */
    private HintResult level2(GameBoardModel board, List<Constraint> constraints) {
        for (Constraint a : constraints) {
            for (Constraint b : constraints) {
                if (a == b || b.cells.size() <= a.cells.size() || !b.cells.containsAll(a.cells)) continue;

                Set<Integer> diff = new LinkedHashSet<>(b.cells);
                diff.removeAll(a.cells);
                // For A contained in B, mines(B minus A) = mines(B) minus mines(A).
                int minesInDiff = b.remaining - a.remaining;

                if (minesInDiff == 0) {
                    int code = diff.iterator().next();
                    return new HintResult(b.x(code), b.y(code), HintType.CERTAIN_SAFE);
                }
                if (minesInDiff == diff.size()) {
                    HintResult mine = firstUnflaggedMine(board, b, diff);
                    if (mine != null) return mine;
                }
            }
        }
        return null;
    }

    /**
     * Avoids recommending a mine that the player has already flagged.
     */
    private HintResult firstUnflaggedMine(GameBoardModel board, Constraint c, Set<Integer> codes) {
        for (int code : codes) {
            if (!board.getCell(c.x(code), c.y(code)).isFlagged()) {
                return new HintResult(c.x(code), c.y(code), HintType.CERTAIN_MINE);
            }
        }
        return null;
    }
}
