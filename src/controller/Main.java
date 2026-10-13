// File: src/controller/Main.java
package controller;

import ui.GameFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Sử dụng EDT (Event Dispatch Thread) để khởi tạo giao diện an toàn
        SwingUtilities.invokeLater(() -> {
            GameManager manager = GameManager.getInstance();
            GameFrame frame = new GameFrame(manager);
            frame.setVisible(true);
        });
    }
}