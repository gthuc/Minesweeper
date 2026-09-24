package minesweeper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SafeHintStrategy implements IHintStrategy {

    @Override
    public HintResult GetHintPosition(GameBoardModel board) {
        HintResult level1 = getLevel1Hint(board);
        if (level1 != null) return level1;
    
        HintResult level2 = getLevel2Hint(board);
        if (level2 != null) return level2;

        return null;
    }

    private HintResult getLevel1Hint(GameBoardModel board) 
    {
        int size = board.getSize();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                CellModel cell = board.GetCell(x, y);
                if (cell == null || !cell.isRevealed() || cell.getAdjacentMinesCount() == 0) continue;

                List<int[]> hiddenCellsCoords = getHiddenNeighborsCoords(board, x, y);
                int flaggedCount = getFlaggedNeighborsCount(board, x, y);
                int requiredMines = cell.getAdjacentMinesCount();

                if (flaggedCount == requiredMines && !hiddenCellsCoords.isEmpty()) {
                    int[] safeCoords = hiddenCellsCoords.get(0);
                    return new HintResult(safeCoords[0], safeCoords[1], HintType.CERTAIN_SAFE);
                }

                if (hiddenCellsCoords.size() == (requiredMines - flaggedCount) && !hiddenCellsCoords.isEmpty()) {
                    int[] mineCoords = hiddenCellsCoords.get(0);
                    return new HintResult(mineCoords[0], mineCoords[1], HintType.CERTAIN_MINE);
                }
            }
        }
        return null;
    }

    private HintResult getLevel2Hint(GameBoardModel board) 
    {
        int size = board.getSize();
        
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                CellModel cellA = board.GetCell(x, y);
                if (cellA == null || !cellA.isRevealed() || cellA.getAdjacentMinesCount() == 0) continue;

                Set<String> setA = getHiddenNeighborsCoordsSet(board, x, y);
                if (setA.isEmpty()) continue;

                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) continue;
                        
                        int bx = x + dx;
                        int by = y + dy;
                        
                        if (bx < 0 || bx >= size || by < 0 || by >= size) continue;
                        
                        CellModel cellB = board.GetCell(bx, by);
                        
                        if (cellB != null && cellB.isRevealed() && cellB.getAdjacentMinesCount() > 0) {
                            
                            Set<String> setB = getHiddenNeighborsCoordsSet(board, bx, by);
                            
                            if (!setB.isEmpty() && setB.containsAll(setA) && setB.size() > setA.size()) {
                                
                                int remainingMinesA = cellA.getAdjacentMinesCount() - getFlaggedNeighborsCount(board, x, y);
                                int remainingMinesB = cellB.getAdjacentMinesCount() - getFlaggedNeighborsCount(board, bx, by);
                                
                                Set<String> differenceBminusA = new HashSet<>(setB);
                                differenceBminusA.removeAll(setA);

                                int minesInDifference = remainingMinesB - remainingMinesA;

                                if (minesInDifference == differenceBminusA.size() && !differenceBminusA.isEmpty()) {
                                    String[] coords = differenceBminusA.iterator().next().split(",");
                                    int targetX = Integer.parseInt(coords[0]);
                                    int targetY = Integer.parseInt(coords[1]);
                                    
                                    return new HintResult(targetX, targetY, HintType.CERTAIN_MINE);
                                }
                                
                                if (minesInDifference == 0 && !differenceBminusA.isEmpty()) {
                                    String[] coords = differenceBminusA.iterator().next().split(",");
                                    int targetX = Integer.parseInt(coords[0]);
                                    int targetY = Integer.parseInt(coords[1]);
                                    
                                    return new HintResult(targetX, targetY, HintType.CERTAIN_SAFE);
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private List<int[]> getHiddenNeighborsCoords(GameBoardModel board, int x, int y) 
    {
        List<int[]> hiddenCoords = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                
                int nx = x + dx;
                int ny = y + dy;
                CellModel neighbor = board.GetCell(nx, ny);
                
                if (neighbor != null && !neighbor.isRevealed() && !neighbor.isFlagged()) {
                    hiddenCoords.add(new int[]{nx, ny}); 
                }
            }
        }
        return hiddenCoords;
    }

    private Set<String> getHiddenNeighborsCoordsSet(GameBoardModel board, int x, int y) 
    {
        Set<String> hidden = new HashSet<>();
        int size = board.getSize();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && nx < size && ny >= 0 && ny < size) {
                    CellModel neighbor = board.GetCell(nx, ny);
                    if (neighbor != null && !neighbor.isRevealed() && !neighbor.isFlagged()) {
                        hidden.add(nx + "," + ny);
                    }
                }
            }
        }
        return hidden;
    }

    private int getFlaggedNeighborsCount(GameBoardModel board, int x, int y) 
    {
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                CellModel neighbor = board.GetCell(x + dx, y + dy);
                if (neighbor != null && neighbor.isFlagged()) {
                    count++;
                }
            }
        }
        return count;
    }
}