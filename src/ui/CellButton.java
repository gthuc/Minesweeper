// File: src/ui/CellButton.java
package ui;
import javax.swing.*;
import java.awt.*;

public class CellButton extends JButton {
    private int row;
    private int col;

    public CellButton(int row, int col) {
        this.row = row;
        this.col = col;

        setPreferredSize(new Dimension(40, 40));
        setFocusPainted(false);
        setFont(new Font("Arial", Font.BOLD, 14));

        // --- FIX LỖI TÀNG HÌNH MÀU NỀN TRÊN MACOS ---
        setOpaque(true); // Cho phép nút được tô màu toàn bộ
        setContentAreaFilled(true); // Đổ màu vào phần ruột nút
        setBorder(BorderFactory.createLineBorder(Color.GRAY)); // Bo viền vuông vắn cho giống ô mìn
        // --------------------------------------------
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
}