package ui;

import controller.GameManager;
import model.GameBoardModel;
import observer.BoardEvent;
import observer.IGameBoardObserver;

import javax.swing.*;
import java.awt.*;

public class StatusPanel extends JPanel implements IGameBoardObserver {
    private JLabel mineCountLabel;
    private JLabel timerLabel;
    private GameManager gameManager;
    private Timer uiTimer; // UI timer for the clock

    public StatusPanel(GameManager gameManager) {
        this.gameManager = gameManager;
        setLayout(new FlowLayout(FlowLayout.CENTER, 50, 5));

        mineCountLabel = new JLabel("Mines: --");
        timerLabel = new JLabel("Time: 00:00");

        add(mineCountLabel);
        add(timerLabel);

        // Initialize Timer to run every 500ms (0.5s) to update the clock
        uiTimer = new Timer(500, e -> updateTimerDisplay());
        uiTimer.start();
    }

    private void updateTimerDisplay() {
        if (gameManager != null) {
            long seconds = gameManager.getElapsedSeconds();
            long m = seconds / 60;
            long s = seconds % 60;
            timerLabel.setText(String.format("Time: %02d:%02d", m, s));
        }
    }

    public void updateStatus() {
        if (gameManager != null && gameManager.getGameBoard() != null) {
            GameBoardModel board = gameManager.getGameBoard();
            int totalMines = board.getTotalMines();
            int flagCount = board.getFlagCount();
            mineCountLabel.setText("Mines: " + (totalMines - flagCount));
        }
    }

    @Override
    public void onBoardChange(BoardEvent event) {
        updateStatus();
    }
}