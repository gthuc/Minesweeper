package minesweeper;
import java.util.Random;

public class GameBoardModel {
    private CellModel[][] cells;

    public GameBoardModel()
    {
    }

    public void Initialize(int size, int mineCount)
    {
        cells= new CellModel[size][size];
        for(int i = 0; i < size; i++)
        {
            for(int j = 0; j < size; j++)
            {
                cells[i][j] = new CellModel();
            }
        }
        GenerateMines(mineCount);
        GenerateNumbers();
    }

    public CellModel GetCell(int x, int y)
    {
        if(isValidCoordinate(x, y))
        {
            return cells[x][y];
        }
        return null;
    }

    public void RevealCell(int x, int y)
    {
        if(isValidCoordinate(x, y))
        {
            CellModel cell = cells[x][y];
            if(!cell.isRevealed() && !cell.isFlagged())
            {
                cell.setRevealed(true);
            }
        }
    }

    public void ToggleFlag(int x, int y) {
        if (isValidCoordinate(x, y)) {
            CellModel cell = cells[x][y];
            if (!cell.isRevealed()) {
                cell.setFlagged(!cell.isFlagged());
            }
        }
    }

    private void GenerateMines(int mineCount)
    {
        Random random = new Random();
        for(int i = 0; i < mineCount; i++)
        {
            int x, y;
            do
            {
                x = random.nextInt(cells.length);
                y = random.nextInt(cells[0].length);
            } while(GetCell(x, y).isMine());
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

}
