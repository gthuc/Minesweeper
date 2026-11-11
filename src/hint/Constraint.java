package hint;

import model.CellModel;
import model.GameBoardModel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a numbered clue as a set of hidden cells and a remaining mine count.
 * Coordinates are encoded as row * boardSize + column. Flags remain unknown because
 * player guesses are not proof that a cell contains a mine.
 */
class Constraint {
    final Set<Integer> cells;
    final int remaining;
    private final int size;

    private Constraint(int size, Set<Integer> cells, int remaining) {
        this.size = size;
        this.cells = cells;
        this.remaining = remaining;
    }

    int x(int code) {
        return code / size;
    }
    int y(int code) {
        return code % size;
    }

    /**
     * Builds one constraint per revealed safe clue with hidden neighbors.
     * Only revealed mines are subtracted from the clue value.
     */
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
                    // Treat flags as unknown; otherwise an incorrect flag could create false certainty.
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
