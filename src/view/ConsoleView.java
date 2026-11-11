package view;

import controller.GameManager;
import model.CellModel;
import model.GameBoardModel;
import model.GameStatus;
import observer.BoardEvent;
import observer.IGameBoardObserver;

/**
 * Provides an observer-driven text view without owning input processing.
 * Board events mark the view dirty; the caller decides when to render it.
 */
public class ConsoleView implements IGameBoardObserver {
    private boolean dirty;

    @Override
    public void onBoardChange(BoardEvent event) {
        dirty = true;
    }

    /**
     * Returns and clears the pending-refresh flag.
     */
    public boolean consumeDirty() {
        boolean d = dirty;
        dirty = false;
        return d;
    }

    /**
     * Prints current visible information, hiding the board while paused.
     * Mines and incorrect flags are exposed only after a loss.
     */
    public void render(GameManager gm) {
        GameBoardModel board = gm.getGameBoard();
        if (board == null) {
            System.out.println("Currently in the menu.");
            return;
        }

        GameStatus status = gm.getGameStatus();
        System.out.println();
        System.out.println("[" + gm.getDifficultyLevel() + "] Status: " + status
                + " | Time: " + gm.getElapsedSeconds() + "s"
                + " | Lives: " + gm.getLivesRemaining()
                + " | Mines remaining: " + board.getRemainingMines());

        if (status == GameStatus.PAUSED) {
            System.out.println("*** PAUSED - type 'p' to resume ***");
            return;
        }

        boolean showMines = status == GameStatus.LOST;
        int n = board.getSize();

        StringBuilder sb = new StringBuilder("    ");
        for (int j = 0; j < n; j++) sb.append(String.format("%3d", j));
        sb.append('\n');
        for (int i = 0; i < n; i++) {
            sb.append(String.format("%3d |", i));
            for (int j = 0; j < n; j++) {
                sb.append(String.format("%3s", symbol(board.getCell(i, j), showMines)));
            }
            sb.append('\n');
        }
        System.out.print(sb);
    }

    /**
     * Maps model state to a display symbol without changing the cell.
     */
    private String symbol(CellModel cell, boolean showMines) {
        if (cell.isRevealed()) {
            if (cell.isMine()) return cell.getMineSymbol();
            return cell.getAdjacentMinesCount() == 0 ? "." : String.valueOf(cell.getAdjacentMinesCount());
        }
        if (cell.isFlagged()) {
            return (showMines && !cell.isMine()) ? "x" : cell.getFlagSymbol();
        }
        return (showMines && cell.isMine()) ? cell.getMineSymbol() : "#";
    }
}
