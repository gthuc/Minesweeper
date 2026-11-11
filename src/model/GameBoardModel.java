package model;

import factory.ICellFactory;
import observer.BoardEvent;
import observer.IGameBoardObserver;
import observer.IGameBoardSubject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Owns the square cell grid, mine generation, reveal expansion, and board counters.
 * Coordinates use x as row and y as column. Mutations notify registered observers
 * synchronously; this class does not dispatch callbacks to the Swing thread.
 */
public class GameBoardModel implements IGameBoardSubject {
    private CellModel[][] cells;
    private boolean minesGenerated;
    private int totalMines;
    private int flagCount;
    private int revealedSafeCount;
    private int revealedMineCount;

    private final ICellFactory factory;
    private final Random random;
    private final List<IGameBoardObserver> observers = new ArrayList<>();

    public GameBoardModel(ICellFactory factory) {
        this(factory, new Random());
    }

    /**
     * Accepts an injectable random source for repeatable mine layouts in tests.
     */
    public GameBoardModel(ICellFactory factory, Random random) {
        this.factory = factory;
        this.random = random;
    }

    /**
     * Creates empty cells and resets counters; mine placement waits for a valid reveal.
     * @throws IllegalArgumentException if size or mine count is invalid
     */
    public void initialize(int size, int totalMines) {
        if (size <= 0 || totalMines < 0 || totalMines >= size * size) {
            throw new IllegalArgumentException("Invalid board size or mine count");
        }
        this.totalMines = totalMines;
        this.minesGenerated = false;
        this.flagCount = 0;
        this.revealedSafeCount = 0;
        this.revealedMineCount = 0;
        this.cells = new CellModel[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                cells[i][j] = factory.createCell();
            }
        }
    }

    /**
     * Returns the cell at zero-based row x and column y, or null outside the board.
     */
    public CellModel getCell(int x, int y) {
        return isValidCoordinate(x, y) ? cells[x][y] : null;
    }

    /**
     * Returns up to eight valid neighboring coordinates, excluding the cell itself.
     */
    public List<int[]> getNeighbors(int x, int y) {
        List<int[]> result = new ArrayList<>(8);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                if (isValidCoordinate(x + dx, y + dy)) {
                    result.add(new int[]{x + dx, y + dy});
                }
            }
        }
        return result;
    }

    public int getSize() {
        return cells == null ? 0 : cells.length;
    }

    public int getTotalMines() {
        return totalMines;
    }

    public int getFlagCount() {
        return flagCount;
    }

    public int getRevealedSafeCount() {
        return revealedSafeCount;
    }

    public int getRevealedMineCount() {
        return revealedMineCount;
    }

    public boolean isMinesGenerated() {
        return minesGenerated;
    }

    /**
     * Returns a nonnegative estimate after subtracting flags and already revealed mines.
     * Flags may be incorrect, so this is not a count of proven hidden mines.
     */
    public int getRemainingMines() {
        return Math.max(0, totalMines - flagCount - revealedMineCount);
    }

    /**
     * Toggles a hidden cell flag while respecting the remaining flag budget.
     * @return true if the flag state changed
     */
    public boolean toggleFlag(int x, int y) {
        CellModel cell = getCell(x, y);
        if (cell == null || cell.isRevealed()) return false;
        if (!cell.isFlagged() && flagCount + revealedMineCount >= totalMines) return false;

        cell.setFlagged(!cell.isFlagged());
        flagCount += cell.isFlagged() ? 1 : -1;
        notifyObservers(BoardEvent.FLAG);
        return true;
    }

    /**
     * Reveals a valid unflagged cell, generating mines on the first reveal.
     * @return all newly revealed coordinates, or an empty list for an ineffective action
     */
    public List<int[]> revealCell(int x, int y) {
        List<int[]> affected = new ArrayList<>();
        CellModel cell = getCell(x, y);
        if (cell == null || cell.isRevealed() || cell.isFlagged()) {
            return affected;
        }

        if (!minesGenerated) {
            generateMines(totalMines, x, y);
            generateNumbers();
            minesGenerated = true;
        }

        markRevealed(cell);
        affected.add(new int[]{x, y});
        if (!cell.isMine() && cell.getAdjacentMinesCount() == 0) {
            affected.addAll(floodFill(x, y));
        }

        notifyObservers(BoardEvent.REVEAL);
        return affected;
    }

    /**
     * Hides supplied revealed cells and restores counters.
     * Mine-hit exclusion is enforced by CommandManager, not by this low-level method.
     * @return true if at least one cell changed
     */
    public boolean undoReveal(List<int[]> affectedCoords) {
        if (affectedCoords == null || affectedCoords.isEmpty()) return false;
        boolean changed = false;
        for (int[] c : affectedCoords) {
            CellModel cell = getCell(c[0], c[1]);
            if (cell != null && cell.isRevealed()) {
                cell.setRevealed(false);
                if (cell.isMine()) revealedMineCount--; else revealedSafeCount--;
                changed = true;
            }
        }
        if (changed) notifyObservers(BoardEvent.UNDO);
        return changed;
    }

    private void markRevealed(CellModel cell) {
        cell.setRevealed(true);
        if (cell.isMine()) revealedMineCount++; else revealedSafeCount++;
    }

    /**
     * Expands zero-valued regions with a queue (BFS), including numbered boundary cells.
     * Already revealed cells act as the visited set; flagged cells remain hidden.
     */
    private List<int[]> floodFill(int startX, int startY) {
        List<int[]> revealed = new ArrayList<>();
        Deque<int[]> queue = new ArrayDeque<>(getNeighbors(startX, startY));

        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            CellModel cell = cells[p[0]][p[1]];
            // Queue entries may repeat; revealing on first visit prevents repeated expansion.
            if (cell.isRevealed() || cell.isFlagged()) continue;

            markRevealed(cell);
            revealed.add(p);
            if (!cell.isMine() && cell.getAdjacentMinesCount() == 0) {
                queue.addAll(getNeighbors(p[0], p[1]));
            }
        }
        return revealed;
    }

    /**
     * Shuffles candidate coordinates and selects the requested number without duplicates.
     * The first cell is always excluded; its neighbors are excluded when capacity permits.
     */
    private void generateMines(int mines, int firstX, int firstY) {
        List<int[]> candidates = new ArrayList<>();
        List<int[]> nearFirst = new ArrayList<>();

        for (int x = 0; x < cells.length; x++) {
            for (int y = 0; y < cells.length; y++) {
                if (x == firstX && y == firstY) continue;
                boolean near = Math.abs(x - firstX) <= 1 && Math.abs(y - firstY) <= 1;
                (near ? nearFirst : candidates).add(new int[]{x, y});
            }
        }

        // Very dense direct board configurations relax neighbor protection, never first-cell safety.
        if (candidates.size() < mines) {
            candidates.addAll(nearFirst);
        }

        Collections.shuffle(candidates, random);
        for (int i = 0; i < mines; i++) {
            int[] c = candidates.get(i);
            cells[c[0]][c[1]].setMine(true);
        }
    }

    private void generateNumbers() {
        for (int i = 0; i < cells.length; i++) {
            for (int j = 0; j < cells.length; j++) {
                if (!cells[i][j].isMine()) {
                    cells[i][j].setAdjacentMinesCount(countAdjacentMines(i, j));
                }
            }
        }
    }

    private int countAdjacentMines(int x, int y) {
        int count = 0;
        for (int[] n : getNeighbors(x, y)) {
            if (cells[n[0]][n[1]].isMine()) count++;
        }
        return count;
    }

    private boolean isValidCoordinate(int x, int y) {
        return cells != null && x >= 0 && x < cells.length && y >= 0 && y < cells.length;
    }

    @Override
    public void registerObserver(IGameBoardObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(IGameBoardObserver observer) {
        observers.remove(observer);
    }

    /**
     * Invokes a copy of the observer list so callbacks may unregister safely.
     */
    @Override
    public void notifyObservers(BoardEvent event) {
        for (IGameBoardObserver observer : new ArrayList<>(observers)) {
            observer.onBoardChange(event);
        }
    }
}
