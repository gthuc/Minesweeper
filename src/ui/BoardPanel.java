package ui;

import controller.GameManager;
import hint.HintResult;
import model.CellModel;
import model.GameBoardModel;
import observer.BoardEvent;
import observer.IGameBoardObserver;
import settings.SettingsManager;
import settings.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Renders board cells and routes mouse input to the game manager.
 * Board observer callbacks refresh existing buttons; callers rebuild the grid for
 * new games and must use the Swing event dispatch thread for UI operations.
 */
public class BoardPanel extends JPanel implements IGameBoardObserver {
    private GameManager gameManager;
    private CellButton[][] cellButtons;

    private static final Font EMOJI_FONT = new Font("Segoe UI Emoji", Font.PLAIN, 16);
    private static final Font TEXT_FONT = new Font("SansSerif", Font.BOLD, 14);

    public BoardPanel(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Rebuilds the button grid for the current board and installs input listeners.
     */
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

                        btn.setOpaque(true);
                        btn.setContentAreaFilled(true);
                        btn.setFocusPainted(false);

                        // Send coordinates to the controller; the view does not mutate cell state directly.
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

    /**
     * Refreshes button appearance from model state after a board mutation.
     */
    @Override
    public void onBoardChange(BoardEvent event) {
        if (gameManager != null && gameManager.getGameBoard() != null && cellButtons != null) {
            GameBoardModel board = gameManager.getGameBoard();
            int size = board.getSize();

            Theme currentTheme = SettingsManager.getInstance().get().getTheme();

            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    CellModel cell = board.getCell(r, c);
                    CellButton btn = cellButtons[r][c];

                    if (cell != null && btn != null) {
                        btn.setText("");
                        btn.setBackground(UIManager.getColor("Button.background"));
                        btn.setFont(TEXT_FONT);
                        btn.setEnabled(true);

                        if (cell.isRevealed()) {
                            btn.setEnabled(false);

                            if (currentTheme == Theme.THEME) {
                                btn.setBackground(new Color(45, 45, 45));
                            } else {
                                btn.setBackground(Color.LIGHT_GRAY);
                            }

                            if (cell.isMine()) {
                                btn.setFont(EMOJI_FONT);
                                if (currentTheme == Theme.THEME) {
                                    btn.setText("👾");
                                    btn.setBackground(new Color(255, 140, 0));
                                } else {
                                    btn.setText("💣");
                                    btn.setBackground(Color.RED);
                                }
                            } else {
                                int mines = cell.getAdjacentMinesCount();
                                if (mines > 0) {
                                    btn.setText(String.valueOf(mines));
                                    btn.setFont(TEXT_FONT);

                                    if (currentTheme == Theme.THEME) {
                                        if (mines == 1) btn.setForeground(new Color(0, 191, 255));
                                        else if (mines == 2) btn.setForeground(new Color(50, 205, 50));
                                        else btn.setForeground(new Color(255, 20, 147));
                                    } else {
                                        if (mines == 1) btn.setForeground(Color.BLUE);
                                        else if (mines == 2) btn.setForeground(new Color(0, 128, 0));
                                        else btn.setForeground(Color.RED);
                                    }
                                }
                            }
                        } else if (cell.isFlagged()) {
                            btn.setFont(EMOJI_FONT);
                            if (currentTheme == Theme.THEME) {
                                btn.setText("🏴‍☠️");
                            } else {
                                btn.setText("🚩");
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Colors the suggested cell without revealing it or changing board state.
     */
    public void highlightHint(HintResult hint) {
        if (hint == null || cellButtons == null) return;
        int r = hint.getX();
        int c = hint.getY();

        if (r >= 0 && r < cellButtons.length && c >= 0 && c < cellButtons[0].length) {
            CellButton btn = cellButtons[r][c];

            switch (hint.getType()) {
                case CERTAIN_SAFE:
                    btn.setBackground(Color.GREEN);
                    break;
                case CERTAIN_MINE:
                    btn.setBackground(Color.ORANGE);
                    break;
                case PROBABLE:
                    btn.setBackground(Color.YELLOW);
                    break;
            }
        }
    }
}
