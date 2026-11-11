package controller;

import ui.GameFrame;

import javax.swing.*;

/**
 * Starts the Swing application and creates its window on the event dispatch thread.
 */
public class Main {
    /**
     * Selects the platform look and feel, then opens the game on the Swing thread.
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Construct and display Swing components on the event dispatch thread.
        SwingUtilities.invokeLater(() -> {
            GameManager gameManager = GameManager.getInstance();
            GameFrame frame = new GameFrame(gameManager);
            frame.setVisible(true);
        });
    }
}
