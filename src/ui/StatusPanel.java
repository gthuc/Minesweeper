package ui;

import controller.GameManager;
import model.GameBoardModel;
import observer.BoardEvent;
import observer.IGameBoardObserver;

import javax.swing.*;
import java.awt.*;

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
        livesLabel = new JLabel("Lives: ❤️❤️❤️"); // Đã bỏ trái tim thừa phía trước chữ Lives
        timerLabel = new JLabel("⏱️ Time: 00:00");

        add(mineCountLabel);
        add(livesLabel);
        add(timerLabel);

        uiTimer = new Timer(500, e -> updateTimerDisplay());
        uiTimer.start();
    }

    private void updateTimerDisplay() {
        if (gameManager != null) {
            long seconds = gameManager.getElapsedSeconds();
            long m = seconds / 60;
            long s = seconds % 60;
            timerLabel.setText(String.format("⏱️ Time: %02d:%02d", m, s));
        }
    }

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

            // Đã sửa: Chuỗi hiển thị chuẩn Lives: ❤️❤️❤️
            livesLabel.setText("Lives: " + hearts.toString().trim());
        }
    }

    @Override
    public void onBoardChange(BoardEvent event) {
        updateStatus();
    }
}