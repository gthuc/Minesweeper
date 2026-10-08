package controller;

import ui.GameFrame;
import javax.swing.SwingUtilities;
import model.DifficultyLevel;

public class Main {
    public static void main(String[] args) {
        // 1. Lấy phiên bản GameManager duy nhất (Singleton)
        GameManager gm = GameManager.getInstance();

        // 2. Khởi tạo dữ liệu một ván game mặc định (Mức Dễ)
        gm.startGame(DifficultyLevel.EASY);

        // 3. Khởi động giao diện đồ họa (GUI)
        SwingUtilities.invokeLater(() -> {
            GameFrame frame = new GameFrame(gm);
            frame.setVisible(true);
        });
    }
}