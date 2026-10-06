package model;

import factory.ICellFactory;
import observer.IGameBoardObserver;
import observer.IGameBoardSubject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

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

    /** Cho phép truyền Random có seed để test lặp lại được. */
    public GameBoardModel(ICellFactory factory, Random random) {
        this.factory = factory;
        this.random = random;
    }

    public void initialize(int size, int totalMines) {
        if (size <= 0 || totalMines < 0 || totalMines >= size * size) {
            throw new IllegalArgumentException("Kích thước hoặc số mìn không hợp lệ");
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

    // ---------- Truy vấn ----------

    public CellModel getCell(int x, int y) {
        return isValidCoordinate(x, y) ? cells[x][y] : null;
    }

    /** Các ô lân cận hợp lệ (đã loại ô ngoài biên) dưới dạng {x, y}. */
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

    public int getSize() { return cells.length; }
    public int getTotalMines() { return totalMines; }
    public int getFlagCount() { return flagCount; }
    public int getRevealedSafeCount() { return revealedSafeCount; }
    public int getRevealedMineCount() { return revealedMineCount; }
    public boolean isMinesGenerated() { return minesGenerated; }

    /** Số mìn ước tính còn lại = tổng mìn - cờ đã cắm - mìn đã nổ. */
    public int getRemainingMines() {
        return totalMines - flagCount - revealedMineCount;
    }

    // ---------- Hành động ----------

    /**
     * Bật/tắt cờ. Trả về true nếu trạng thái thực sự thay đổi.
     * Không cho cắm quá số mìn của bàn cờ.
     */
    public boolean toggleFlag(int x, int y) {
        CellModel cell = getCell(x, y);
        if (cell == null || cell.isRevealed()) return false;
        if (!cell.isFlagged() && flagCount >= totalMines) return false;

        cell.setFlagged(!cell.isFlagged());
        flagCount += cell.isFlagged() ? 1 : -1;
        notifyObservers("FLAG");
        return true;
    }

    public List<int[]> revealCell(int x, int y) {
        List<int[]> affected = new ArrayList<>();
        CellModel cell = getCell(x, y);
        if (cell == null || cell.isRevealed() || cell.isFlagged()) {
            return affected; // không thay đổi gì, không notify
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

        notifyObservers("REVEAL");
        return affected;
    }

    public void undoReveal(List<int[]> affectedCoords) {
        if (affectedCoords == null || affectedCoords.isEmpty()) return;
        for (int[] c : affectedCoords) {
            CellModel cell = getCell(c[0], c[1]);
            if (cell != null && cell.isRevealed()) {
                cell.setRevealed(false);
                if (cell.isMine()) revealedMineCount--; else revealedSafeCount--;
            }
        }
        notifyObservers("UNDO");
    }

    // ---------- Thuật toán ----------

    private void markRevealed(CellModel cell) {
        cell.setRevealed(true);
        if (cell.isMine()) revealedMineCount++; else revealedSafeCount++;
    }

    private List<int[]> floodFill(int startX, int startY) {
        List<int[]> revealed = new ArrayList<>();
        Deque<int[]> queue = new ArrayDeque<>(getNeighbors(startX, startY));

        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            CellModel cell = cells[p[0]][p[1]];
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
     * Đặt mìn ngẫu nhiên, tránh ô click đầu và 8 ô xung quanh (nếu còn đủ chỗ).
     * Dùng danh sách ứng viên + shuffle nên không bao giờ lặp vô hạn.
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

    // ---------- IGameBoardSubject ----------

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

    @Override
    public void notifyObservers(Object eventData) {
        // Duyệt trên bản sao để observer có thể tự gỡ đăng ký khi đang được gọi.
        for (IGameBoardObserver observer : new ArrayList<>(observers)) {
            observer.onBoardChange(eventData);
        }
    }
}
