package hint;

import model.GameBoardModel;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Chỉ trả về gợi ý CHẮC CHẮN (an toàn / là mìn) suy ra từ các ô số đã lật. */
public class SafeHintStrategy implements IHintStrategy {

    @Override
    public HintResult getHintPosition(GameBoardModel board) {
        List<Constraint> constraints = Constraint.build(board);

        HintResult hint = level1(board, constraints);
        if (hint != null) return hint;

        return level2(board, constraints);
    }

    /** Ô số có 0 mìn còn lại -> mọi ô chưa biết đều an toàn; còn lại == số ô -> toàn mìn. */
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

    /** Quy tắc tập con: nếu A ⊂ B thì số mìn trong (B \ A) = remB - remA. */
    private HintResult level2(GameBoardModel board, List<Constraint> constraints) {
        for (Constraint a : constraints) {
            for (Constraint b : constraints) {
                if (a == b || b.cells.size() <= a.cells.size() || !b.cells.containsAll(a.cells)) continue;

                Set<Integer> diff = new LinkedHashSet<>(b.cells);
                diff.removeAll(a.cells);
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

    /** Bỏ qua ô mìn đã được cắm cờ rồi, để gợi ý không lặp lại mãi một ô. */
    private HintResult firstUnflaggedMine(GameBoardModel board, Constraint c, Set<Integer> codes) {
        for (int code : codes) {
            if (!board.getCell(c.x(code), c.y(code)).isFlagged()) {
                return new HintResult(c.x(code), c.y(code), HintType.CERTAIN_MINE);
            }
        }
        return null;
    }
}
