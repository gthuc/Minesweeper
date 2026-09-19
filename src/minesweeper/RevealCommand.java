package minesweeper;
import java.util.List;

public class RevealCommand implements ICommand {
    private int x, y;
    private GameBoardModel board;
    
    private List<int[]> affectedCoords; 

    public RevealCommand(GameBoardModel board, int x, int y) {
        this.board = board;
        this.x = x;
        this.y = y;
    }

    @Override
    public void Execute() {
        affectedCoords = board.RevealCell(x, y); 
    }

    @Override
    public void Undo() {
        if (affectedCoords != null && !affectedCoords.isEmpty()) {
            board.UndoReveal(affectedCoords);
        }
    }
}