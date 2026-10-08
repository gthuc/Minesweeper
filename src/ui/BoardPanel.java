package ui;

import command.FlagCommand;
import command.RevealCommand;
import controller.GameManager;
import observer.IGameBoardObserver;
import model.GameStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class BoardPanel extends JPanel implements IGameBoardObserver {
    private GameManager gameManager;
    private CellButton[][] cellButtons;
    private int rows = 9;
    private int cols = 9;
    private boolean isGameOverHandled = false;

    public BoardPanel(GameManager gameManager) {
        this.gameManager = gameManager;
        setLayout(new GridLayout(rows, cols));
        cellButtons = new CellButton[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                cellButtons[i][j] = new CellButton(i, j);
                final int r = i;
                final int c = j;

                cellButtons[i][j].addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        // 1. Nếu game đã kết thúc thì chặn click
                        if (gameManager.getGameStatus() != GameStatus.Playing) {
                            return;
                        }

                        if (SwingUtilities.isLeftMouseButton(e)) {
                            gameManager.handleCellAction(new RevealCommand(gameManager.getGameBoard(), r, c));
                        } else if (SwingUtilities.isRightMouseButton(e)) {
                            gameManager.handleCellAction(new FlagCommand(gameManager.getGameBoard(), r, c));
                        }

                        // 2. NGAY SAU KHI CLICK: Kiểm tra xem game đã thua/thắng chưa để xử lý hiển thị lập tức
                        checkAndUpdateGameState();
                    }
                });

                add(cellButtons[i][j]);
            }
        }
    }

    // Hàm kiểm tra và bật popup / khóa bàn cờ
    private void checkAndUpdateGameState() {
        GameStatus status = gameManager.getGameStatus();

        if (status == GameStatus.Playing) {
            isGameOverHandled = false;
        }

        if (!isGameOverHandled && (status == GameStatus.Lost || status == GameStatus.Won)) {
            isGameOverHandled = true;

            // Gọi đồng bộ hóa lại giao diện lần cuối để vẽ nốt các ô mìn
            onBoardChange(null);

            SwingUtilities.invokeLater(() -> {
                if (status == GameStatus.Lost) {
                    JOptionPane.showMessageDialog(this,
                            "Game Over! Bạn đã hết mạng (chạm quá 3 quả mìn).",
                            "Thất bại",
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Xuất sắc! Bạn đã quét sạch bãi mìn thành công!",
                            "Chiến thắng",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            });
        }
    }

    @Override
    public void onBoardChange(Object eventData) {
        var board = gameManager.getGameBoard();
        if (board == null) return;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                var cell = board.getCell(i, j);
                CellButton btn = cellButtons[i][j];

                if (cell.isRevealed()) {
                    btn.setEnabled(false);
                    if (cell.isMine()) {
                        btn.setText(cell.getMineSymbol());
                        btn.setBackground(Color.RED);
                        btn.setOpaque(true);
                    } else {
                        int mines = cell.getAdjacentMinesCount();
                        btn.setText(mines > 0 ? String.valueOf(mines) : "");

                        switch (mines) {
                            case 1: btn.setForeground(Color.BLUE); break;
                            case 2: btn.setForeground(new Color(0, 128, 0)); break;
                            case 3: btn.setForeground(Color.RED); break;
                            case 4: btn.setForeground(new Color(0, 0, 128)); break;
                            default: btn.setForeground(Color.BLACK); break;
                        }

                        btn.setBackground(Color.LIGHT_GRAY);
                        btn.setOpaque(true);
                    }
                } else if (cell.isFlagged()) {
                    btn.setText(cell.getFlagSymbol());
                    btn.setForeground(Color.RED);
                    btn.setBackground(null);
                    btn.setOpaque(false);
                } else {
                    btn.setText("");
                    btn.setEnabled(true);
                    btn.setBackground(null);
                    btn.setOpaque(false);
                }
            }
        }
    }
}