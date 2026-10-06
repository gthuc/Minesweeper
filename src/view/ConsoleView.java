package view;

import controller.GameManager;
import model.CellModel;
import model.GameBoardModel;
import model.GameStatus;
import observer.IGameBoardObserver;

/** Giao diện dòng lệnh. Là Observer: bàn cờ đổi thì đánh dấu cần vẽ lại. */
public class ConsoleView implements IGameBoardObserver {
    private boolean dirty;

    @Override
    public void onBoardChange(Object eventData) {
        dirty = true;
    }

    public boolean consumeDirty() {
        boolean d = dirty;
        dirty = false;
        return d;
    }

    public void render(GameManager gm) {
        GameBoardModel board = gm.getGameBoard();
        if (board == null) {
            System.out.println("Chưa có ván nào. Gõ 'n' để bắt đầu.");
            return;
        }

        GameStatus status = gm.getGameStatus();
        System.out.println();
        System.out.println("[" + gm.getDifficultyLevel() + "] Trạng thái: " + status
                + " | Thời gian: " + gm.getElapsedSeconds() + "s"
                + " | Mạng: " + gm.getLivesRemaining()
                + " | Mìn còn lại: " + board.getRemainingMines());

        if (status == GameStatus.Paused) {
            System.out.println("*** TẠM DỪNG - gõ 'p' để tiếp tục ***");
            return;
        }

        boolean showMines = status == GameStatus.Lost;
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

    private String symbol(CellModel cell, boolean showMines) {
        if (cell.isRevealed()) {
            if (cell.isMine()) return cell.getMineSymbol();
            return cell.getAdjacentMinesCount() == 0 ? "." : String.valueOf(cell.getAdjacentMinesCount());
        }
        if (cell.isFlagged()) {
            return (showMines && !cell.isMine()) ? "x" : cell.getFlagSymbol(); // x = cắm cờ sai
        }
        return (showMines && cell.isMine()) ? cell.getMineSymbol() : "#";
    }
}
