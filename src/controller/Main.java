package controller;

import ui.GameFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Thiết lập Look and Feel chuẩn theo hệ điều hành đang chạy (Windows / macOS)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            GameManager gameManager = GameManager.getInstance();
            GameFrame frame = new GameFrame(gameManager);
            frame.setVisible(true);
        });
    }
}