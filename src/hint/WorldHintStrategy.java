package hint;

import model.CellModel;
import model.GameBoardModel;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/** Reads actual mines and selects the closest hidden, unflagged cell to opened cells. */
public class WorldHintStrategy implements IHintStrategy {
    @Override
    public HintResult getHintPosition(GameBoardModel board) {
        int size = board.getSize();
        if (size <= 0) return null;
        int[][] distances = new int[size][size];
        Deque<int[]> queue = new ArrayDeque<>();
        for (int r = 0; r < size; r++) {
            Arrays.fill(distances[r], -1);
            for (int c = 0; c < size; c++) {
                if (board.getCell(r, c).isRevealed()) {
                    distances[r][c] = 0;
                    queue.addLast(new int[]{r, c});
                }
            }
        }
        // With no opened cells, guide the player toward the center.
        if (queue.isEmpty()) {
            int mid = size / 2;
            distances[mid][mid] = 0;
            queue.addLast(new int[]{mid, mid});
        }
        // Multi-source BFS measures distance in eight directions in O(size squared).
        while (!queue.isEmpty()) {
            int[] current = queue.removeFirst();
            for (int[] neighbor : board.getNeighbors(current[0], current[1])) {
                int r = neighbor[0], c = neighbor[1];
                if (distances[r][c] >= 0) continue;
                distances[r][c] = distances[current[0]][current[1]] + 1;
                queue.addLast(neighbor);
            }
        }
        HintResult best = null;
        int bestDistance = Integer.MAX_VALUE;
        int bestOpenedNeighbors = -1;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                CellModel cell = board.getCell(r, c);
                if (cell.isRevealed() || cell.isFlagged()) continue;
                int openedNeighbors = 0;
                for (int[] neighbor : board.getNeighbors(r, c)) {
                    if (board.getCell(neighbor[0], neighbor[1]).isRevealed()) openedNeighbors++;
                }
                // Before generation, the first click is protected wherever it is made.
                HintType type = board.isMinesGenerated() && cell.isMine() ? HintType.MINE : HintType.SAFE;
                int distance = distances[r][c];
                if (best == null || distance < bestDistance
                        || (distance == bestDistance && openedNeighbors > bestOpenedNeighbors)
                        || (distance == bestDistance && openedNeighbors == bestOpenedNeighbors
                            && type == HintType.SAFE && best.getType() == HintType.MINE)) {
                    best = new HintResult(r, c, type);
                    bestDistance = distance;
                    bestOpenedNeighbors = openedNeighbors;
                }
            }
        }
        return best;
    }
}
