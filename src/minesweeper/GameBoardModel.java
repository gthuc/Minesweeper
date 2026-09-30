package minesweeper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class GameBoardModel implements IGameBoardSubject {

    // --- Fields & Properties ---

    private CellModel[][] cells;
    private boolean isFirstClick = true;
    private int totalMines;
    private final ICellFactory factory;
    private final Random random = new Random();

    private final List<IGameBoardObserver> observers = new ArrayList<>();

    // --- Initialization ---

    public GameBoardModel(ICellFactory factory) {
        this.factory = factory;
    }

    public void Initialize(int size, int totalMines) {
        if (size <= 0 || totalMines < 0 || totalMines >= size * size) {
            throw new IllegalArgumentException("Kích thước hoặc số mìn không hợp lệ");
        }
        this.totalMines = totalMines;
        this.isFirstClick = true;
        cells = new CellModel[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                cells[i][j] = factory.createCell(); // dùng Factory
            }
        }
    }

    public CellModel GetCell(int x, int y) {
        if (isValidCoordinate(x, y)) {
            return cells[x][y];
        }
        return null;
    }

    // --- Core Game Actions ---

    public void ToggleFlag(int x, int y) {
        if (isValidCoordinate(x, y)) {
            CellModel cell = cells[x][y];
            if (!cell.isRevealed()) {
                cell.setFlagged(!cell.isFlagged());
            }
            NotifyObservers(null);
        }
    }

    public List<int[]> RevealCell(int x, int y) {
        List<int[]> affectedCoords = new ArrayList<>();
        if (!isValidCoordinate(x, y)) {
            return affectedCoords;
        }

        CellModel cell = cells[x][y];
        if (cell.isRevealed() || cell.isFlagged()) {
            return affectedCoords; // không có gì thay đổi, không cần notify
        }

        if (isFirstClick) {
            GenerateMines(totalMines, x, y);
            GenerateNumbers();
            isFirstClick = false;
        }

        cell.setRevealed(true);
        affectedCoords.add(new int[]{x, y});
        if (!cell.isMine() && cell.getAdjacentMinesCount() == 0) {
            affectedCoords.addAll(FloodFillBFS(x, y));
        }

        NotifyObservers(null);
        return affectedCoords;
    }

    public void UndoReveal(List<int[]> affectedCoords) {
        if (affectedCoords != null) {
            for (int[] coord : affectedCoords) {
                GetCell(coord[0], coord[1]).setRevealed(false);
            }
            NotifyObservers(null);
        }
    }

    // --- Core Algorithms ---

    private List<int[]> FloodFillBFS(int startX, int startY) {
        List<int[]> floodRevealed = new ArrayList<>();
        Queue<int[]> queue = new LinkedList<>();
        enqueueNeighbors(queue, startX, startY);

        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            int x = pos[0];
            int y = pos[1];

            if (!isValidCoordinate(x, y)) continue;

            CellModel cell = GetCell(x, y);
            if (cell.isRevealed() || cell.isFlagged()) continue;

            cell.setRevealed(true);
            floodRevealed.add(new int[]{x, y});

            if (!cell.isMine() && cell.getAdjacentMinesCount() == 0) {
                enqueueNeighbors(queue, x, y);
            }
        }
        return floodRevealed;
    }

    private void enqueueNeighbors(Queue<int[]> queue, int x, int y) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;
                queue.offer(new int[]{x + i, y + j});
            }
        }
    }

    // --- Helper Methods ---

    /**
     * Đặt mìn ngẫu nhiên, tránh ô click đầu và 8 ô xung quanh (nếu còn đủ chỗ).
     * Dùng danh sách ứng viên + shuffle nên không bao giờ lặp vô hạn.
     */
    private void GenerateMines(int totalMines, int firstX, int firstY) {
        int size = cells.length;
        List<int[]> candidates = new ArrayList<>();
        List<int[]> nearFirst = new ArrayList<>();

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (x == firstX && y == firstY) continue;
                boolean isNear = Math.abs(x - firstX) <= 1 && Math.abs(y - firstY) <= 1;
                (isNear ? nearFirst : candidates).add(new int[]{x, y});
            }
        }

        // Nếu vùng an toàn quá lớn so với số mìn thì mới cần dùng lại các ô kề
        if (candidates.size() < totalMines) {
            candidates.addAll(nearFirst);
        }

        Collections.shuffle(candidates, random);
        for (int i = 0; i < totalMines; i++) {
            int[] c = candidates.get(i);
            cells[c[0]][c[1]].setMine(true);
        }
    }

    private void GenerateNumbers() {
        for (int i = 0; i < cells.length; i++) {
            for (int j = 0; j < cells[0].length; j++) {
                if (!cells[i][j].isMine()) {
                    cells[i][j].setAdjacentMinesCount(CountAdjacentMines(i, j));
                }
            }
        }
    }

    private int CountAdjacentMines(int x, int y) {
        int count = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;
                CellModel n = GetCell(x + i, y + j);
                if (n != null && n.isMine()) count++;
            }
        }
        return count;
    }

    private boolean isValidCoordinate(int x, int y) {
        return cells != null && x >= 0 && x < cells.length && y >= 0 && y < cells[0].length;
    }

    // --- Getter ---

    public int getSize() {
        return cells.length;
    }

    // --- IGameBoardSubject Implements ---

    @Override
    public void RegisterObserver(IGameBoardObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void RemoveObserver(IGameBoardObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void NotifyObservers(Object eventData) {
        for (IGameBoardObserver observer : observers) {
            observer.OnBoardChange(eventData);
        }
    }
}