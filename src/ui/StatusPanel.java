package ui;

import controller.GameManager;
import observer.IGameBoardObserver;
import model.GameStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StatusPanel extends JPanel implements IGameBoardObserver {
    private JLabel minesLabel;
    private JLabel timeLabel;
    private Timer timer;
    private int secondsElapsed = 0;
    private GameManager gameManager;

    public StatusPanel(GameManager gameManager) {
        this.gameManager = gameManager;
        setLayout(new GridLayout(1, 2));

        Font font = new Font("Arial", Font.BOLD, 16);

        minesLabel = new JLabel("Mìn: " + gameManager.getGameBoard().getTotalMines(), SwingConstants.CENTER);
        minesLabel.setFont(font);

        timeLabel = new JLabel("Thời gian: 00:00", SwingConstants.CENTER);
        timeLabel.setFont(font);

        add(minesLabel);
        add(timeLabel);

        // Khởi tạo bộ đếm thời gian (1 giây / lần)
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Lấy thẳng thời gian thực từ GameManager của Thục cho chính xác tuyệt đối
                long elapsed = gameManager.getElapsedSeconds();
                int minutes = (int) (elapsed / 60);
                int seconds = (int) (elapsed % 60);
                timeLabel.setText(String.format("Thời gian: %02d:%02d", minutes, seconds));
            }
        });
    }

    @Override
    public void onBoardChange(Object eventData) {
        // Cập nhật số mìn còn lại
        if (gameManager.getGameBoard() != null) {
            int remaining = gameManager.getGameBoard().getRemainingMines();
            minesLabel.setText("Mìn: " + remaining);
        }

        // Kiểm soát trạng thái đồng hồ dựa theo GameStatus của GameManager
        GameStatus status = gameManager.getGameStatus();

        if (status == GameStatus.Playing) {
            if (!timer.isRunning()) {
                timer.start();
            }
        } else {
            // Nếu không còn ở trạng thái Playing (Thua, Thắng, Paused...), dừng đồng hồ ngay lập tức!
            timer.stop();

            // Cập nhật lại chuỗi thời gian lần cuối cho chuẩn xác
            long elapsed = gameManager.getElapsedSeconds();
            int minutes = (int) (elapsed / 60);
            int seconds = (int) (elapsed % 60);
            timeLabel.setText(String.format("Thời gian: %02d:%02d", minutes, seconds));
        }
    }
}