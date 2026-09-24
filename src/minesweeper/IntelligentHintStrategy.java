package minesweeper;

import java.util.ArrayList;
import java.util.List;

public class IntelligentHintStrategy implements IHintStrategy {

    @Override
    public HintResult GetHintPosition(GameBoardModel board) {
        int size = board.getSize();
        List<int[]> boundaryCells = new ArrayList<>();

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                CellModel cell = board.GetCell(x, y);
                if (cell != null && !cell.isRevealed() && !cell.isFlagged()) {
                    if (isBoundaryCell(board, x, y, size)) {
                        boundaryCells.add(new int[]{x, y});
                    }
                }
            }
        }

        if (boundaryCells.isEmpty()) {
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    CellModel cell = board.GetCell(x, y);
                    if (cell != null && !cell.isRevealed() && !cell.isFlagged()) {
                        boundaryCells.add(new int[]{x, y});
                    }
                }
            }
        }

        if (boundaryCells.isEmpty()) {
            return null;
        }

        int[] chosenCell = boundaryCells.get(0);
        return new HintResult(chosenCell[0], chosenCell[1], HintType.PROBABLE);
    }

    private boolean isBoundaryCell(GameBoardModel board, int x, int y, int size) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                
                int nx = x + dx;
                int ny = y + dy;
                
                if (nx >= 0 && nx < size && ny >= 0 && ny < size) {
                    CellModel neighbor = board.GetCell(nx, ny);
                    if (neighbor != null && neighbor.isRevealed() && neighbor.getAdjacentMinesCount() > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}