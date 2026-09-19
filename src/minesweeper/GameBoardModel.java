package minesweeper;
import java.util.Random;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class GameBoardModel implements IGameBoardSubject {
    private CellModel[][] cells;

    private boolean isFirstClick = true;
    private int totalMines;

    private List<IGameBoardObserver> observers = new ArrayList<>();

    // --- Initialization ---

    public GameBoardModel() {}

    public void Initialize(int size, int totalMines)
    {
        this.totalMines = totalMines;
        this.isFirstClick = true;
        cells= new CellModel[size][size];

        for(int i = 0; i < size; i++)
        {
            for(int j = 0; j < size; j++)
            {
                cells[i][j] = new CellModel();
            }
        }
    }

    public CellModel GetCell(int x, int y)
    {
        if(isValidCoordinate(x, y))
        {
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

    public List<int[]> RevealCell(int x, int y)
    {
        List<int[]> affectedCoords = new ArrayList<>();
        if(isValidCoordinate(x, y))
        {
            if(isFirstClick)
            {
                GenerateMines(totalMines, x, y);
                GenerateNumbers();
                isFirstClick = false;
            }
            CellModel cell = cells[x][y];
            if(!cell.isRevealed() && !cell.isFlagged())
            {
                cell.setRevealed(true);
                affectedCoords.add(new int[]{x,y});
                if(!cell.isMine() && CountAdjacentMines(x, y) == 0)
                {
                    affectedCoords.addAll(FloodFillBFS(x, y));
                }
            }
        }

        NotifyObservers(null);
        return affectedCoords;
    }

    public void UndoReveal(List<int[]> affectedCoords)
    {
        if(affectedCoords != null)
        {
            for(int[] coord : affectedCoords)
            {
                int x = coord[0];
                int y = coord[1];
                GetCell(x, y).setRevealed(false);
            }

            NotifyObservers(null);
        }
    }

    // --- Core Algorithms ---
     private List<int[]> FloodFillBFS(int startX, int startY)
     {
        List<int[]> floodRevealed = new ArrayList<>();
        Queue<int[]> queue = new LinkedList<>();

        for(int i = -1; i <= 1; i ++)
        {
            for(int j = -1; j <= 1; j++)
            {
                if(i == 0 && j == 0) continue;
                queue.offer(new int[]{startX + i, startY + j});
            }
        }
        
        while(!queue.isEmpty())
        {
            int[] currentPos = queue.poll();
            int x = currentPos[0];
            int y = currentPos[1];

            if(!isValidCoordinate(x, y)) continue;

            CellModel cell = GetCell(x, y);

            if(cell.isRevealed() || cell.isFlagged()) continue;

            cell.setRevealed(true);

            floodRevealed.add(new int[] {x, y});

            if(!cell.isMine() && cell.getAdjacentMinesCount() == 0){
                for(int i = -1; i <= 1; i ++)
                {
                    for(int j = -1; j <= 1; j ++)
                    {
                        if(i == 0 && j == 0) continue;
                        queue.offer(new int[]{x + i, y + j});
                    }
                }
            }
        }
        return floodRevealed;
     }

    // --- Helper Methods ---

    private void GenerateMines(int totalMines, int firstX, int firstY)
    {
        Random random = new Random();
        for(int i = 0; i < totalMines; i++)
        {
            int x, y;
            do
            {
                x = random.nextInt(cells.length);
                y = random.nextInt(cells[0].length);
            } while(GetCell(x, y).isMine() || (x == firstX && y == firstY));
            GetCell(x, y).setMine(true);
        }
    }

    private void GenerateNumbers()
    {
        for(int i = 0; i < cells.length; i++)
        {
            for(int j = 0; j < cells[0].length; j++)
            {
                if(GetCell(i, j).isMine())
                {
                    continue;
                }
                GetCell(i, j).setAdjacentMinesCount(CountAdjacentMines(i, j));
            }
        }
    }

    private int CountAdjacentMines(int x, int y) {
        int mineCount = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (isValidCoordinate(x + i, y + j) && GetCell(x + i, y + j).isMine()) {
                    mineCount++;
                }
            }
        }
        return mineCount;
    }

    private boolean isValidCoordinate(int x, int y) {
        return cells != null && x >= 0 && x < cells.length && y >= 0 && y < cells[0].length;
    }

    // --- IGameBoardSubject Implements ---

    @Override 
    public void RegisterObserver(IGameBoardObserver observer)
    {
        if(observer != null && !observers.contains(observer))
        {
            observers.add(observer);
        }
    }
    
    @Override 
    public void RemoveObserver(IGameBoardObserver observer)
    {
        observers.remove(observer);
    }

    @Override
    public void NotifyObservers(Object evenData)
    {
        for(IGameBoardObserver observer : observers)
        {
            observer.OnBoardChange(evenData);
        }
    }
}
