package ui;

import controller.GameManager;
import model.GameBoardModel;
import observer.BoardEvent;
import observer.IGameBoardObserver;

import javax.swing.*;
import java.awt.*;

/**
 * Shows the flag-based mine counter, remaining lives, and elapsed time.
 * A Swing timer refreshes the clock display; GameManager owns actual timekeeping.
 */
public class StatusPanel extends JPanel implements IGameBoardObserver {
    private JLabel mineCountLabel;
    private JLabel livesLabel;
    private JLabel timerLabel;
    private GameManager gameManager;
    private Timer uiTimer;

    public StatusPanel(GameManager gameManager) {
        this.gameManager = gameManager;
        setLayout(new FlowLayout(FlowLayout.CENTER, 40, 5));

        mineCountLabel = new JLabel("💣 Mines: --");
        livesLabel = new JLabel("Lives: ❤️❤️❤️");
        timerLabel = new JLabel("⏱️ Time: 00:00");

        add(mineCountLabel);
        add(livesLabel);
        add(timerLabel);

        // This timer repaints the display; elapsed time comes from the manager clock.
        uiTimer = new Timer(500, e -> updateTimerDisplay());
        uiTimer.start();
    }

    /**
     * Formats manager-owned elapsed time; UI refresh ticks do not advance game time.
     */
    private void updateTimerDisplay() {
        if (gameManager != null) {
            long seconds = gameManager.getElapsedSeconds();
            long m = seconds / 60;
            long s = seconds % 60;
            timerLabel.setText(String.format("⏱️ Time: %02d:%02d", m, s));
        }
    }

    /**
     * Refreshes the flag-based counter and life icons from current game values.
     */
    public void updateStatus() {
        if (gameManager != null) {
            if (gameManager.getGameBoard() != null) {
                GameBoardModel board = gameManager.getGameBoard();
                int totalMines = board.getTotalMines();
                int flagCount = board.getFlagCount();
                mineCountLabel.setText("💣 Mines: " + (totalMines - flagCount));
            }

            int lives = gameManager.getLivesRemaining();
            StringBuilder hearts = new StringBuilder();

            if (lives > 0) {
                for (int i = 0; i < lives; i++) {
                    hearts.append("❤️ ");
                }
            } else {
                hearts.append("💔");
            }

            livesLabel.setText("Lives: " + hearts.toString().trim());
        }
    }

    @Override
    public void onBoardChange(BoardEvent event) {
        updateStatus();
    }
}
