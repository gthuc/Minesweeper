package ui;

import controller.GameManager;
import hint.HintResult;
import model.CellModel;
import model.GameBoardModel;
import observer.BoardEvent;
import observer.IGameBoardObserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class BoardPanel extends JPanel implements IGameBoardObserver {
    private GameManager gameManager;
    private CellButton[][] cellButtons;

    public BoardPanel(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void initializeBoard() {
        this.removeAll();

        if (gameManager != null && gameManager.getGameBoard() != null) {
            GameBoardModel board = gameManager.getGameBoard();
            int size = board.getSize();

            if (size > 0) {
                this.setLayout(new GridLayout(size, size));
                cellButtons = new CellButton[size][size];

                for (int r = 0; r < size; r++) {
                    for (int c = 0; c < size; c++) {
                        CellButton btn = new CellButton(r, c);

                        btn.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mousePressed(MouseEvent e) {
                                if (SwingUtilities.isLeftMouseButton(e)) {
                                    gameManager.reveal(btn.getRow(), btn.getCol());
                                } else if (SwingUtilities.isRightMouseButton(e)) {
                                    gameManager.toggleFlag(btn.getRow(), btn.getCol());
                                }
                            }
                        });

                        this.add(btn);
                        cellButtons[r][c] = btn;
                    }
                }
            }
        }
        this.revalidate();
        this.repaint();
    }

    @Override
    public void onBoardChange(BoardEvent event) {
        if (gameManager != null && gameManager.getGameBoard() != null && cellButtons != null) {
            GameBoardModel board = gameManager.getGameBoard();
            int size = board.getSize();

            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    CellModel cell = board.getCell(r, c);
                    CellButton btn = cellButtons[r][c];

                    if (cell != null && btn != null) {
                        btn.setText("");
                        btn.setBackground(UIManager.getColor("Button.background"));
                        btn.setFont(new Font("Arial", Font.BOLD, 14));
                        btn.setEnabled(true);

                        if (cell.isRevealed()) {
                            btn.setEnabled(false);
                            btn.setBackground(Color.LIGHT_GRAY);

                            if (cell.isMine()) {
                                btn.setText("💣");
                                btn.setFont(new Font("Dialog", Font.PLAIN, 18));
                                btn.setBackground(Color.RED);
                            } else {
                                int mines = cell.getAdjacentMinesCount();
                                if (mines > 0) {
                                    btn.setText(String.valueOf(mines));
                                    if (mines == 1) btn.setForeground(Color.BLUE);
                                    else if (mines == 2) btn.setForeground(new Color(0, 128, 0));
                                    else btn.setForeground(Color.RED);
                                }
                            }
                        } else if (cell.isFlagged()) {
                            btn.setText("🚩");
                            btn.setFont(new Font("Dialog", Font.PLAIN, 18));
                        }
                    }
                }
            }
        }
    }

    // --- HÀM MỚI: TÔ MÀU Ô ĐƯỢC GỢI Ý ---
    public void highlightHint(HintResult hint) {
        if (hint == null || cellButtons == null) return;
        int r = hint.getX();
        int c = hint.getY();

        if (r >= 0 && r < cellButtons.length && c >= 0 && c < cellButtons[0].length) {
            CellButton btn = cellButtons[r][c];

            // Tô màu dựa trên loại gợi ý
            switch (hint.getType()) {
                case CERTAIN_SAFE:
                    btn.setBackground(Color.GREEN);
                    break;
                case CERTAIN_MINE:
                    btn.setBackground(Color.ORANGE); // Dùng màu Cam để phân biệt với mìn đã nổ (Đỏ)
                    break;
                case PROBABLE:
                    btn.setBackground(Color.YELLOW);
                    break;
            }
        }
    }
}