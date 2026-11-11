package controller;

import ui.GameFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {

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