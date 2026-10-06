package hint;

import model.CellModel;
import model.GameBoardModel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Ràng buộc sinh ra từ một ô số đã lật: trong tập các ô "chưa biết" xung quanh
 * có đúng {@code remaining} mìn.
 *
 * Quan trọng: mìn đã nổ (đã lật) được trừ vào số mìn của ô số, và cờ do người
 * chơi cắm KHÔNG được tin tưởng (có thể cắm sai) nên ô có cờ vẫn là "chưa biết".
 */
class Constraint {
    final Set<Integer> cells;   // mã hoá x * size + y
    final int remaining;
    private final int size;

    private Constraint(int size, Set<Integer> cells, int remaining) {
        this.size = size;
        this.cells = cells;
        this.remaining = remaining;
    }

    int x(int code) { return code / size; }
    int y(int code) { return code % size; }

    static List<Constraint> build(GameBoardModel board) {
        int size = board.getSize();
        List<Constraint> result = new ArrayList<>();

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                CellModel cell = board.getCell(x, y);
                if (!cell.isRevealed() || cell.isMine()) continue;

                Set<Integer> unknown = new LinkedHashSet<>();
                int knownMines = 0;
                for (int[] n : board.getNeighbors(x, y)) {
                    CellModel nc = board.getCell(n[0], n[1]);
                    if (nc.isRevealed()) {
                        if (nc.isMine()) knownMines++;
                    } else {
                        unknown.add(n[0] * size + n[1]);
                    }
                }
                if (!unknown.isEmpty()) {
                    result.add(new Constraint(size, unknown, cell.getAdjacentMinesCount() - knownMines));
                }
            }
        }
        return result;
    }
}
