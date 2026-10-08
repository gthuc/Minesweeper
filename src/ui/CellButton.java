package ui;
import javax.swing.*;
import java.awt.*;

public class CellButton extends JButton {
    private int row;
    private int col;

    public CellButton(int row, int col) {
        this.row = row;
        this.col = col;

        // Cố định kích thước mỗi ô là hình vuông 40x40 pixel
        setPreferredSize(new Dimension(40, 40));
        setFocusPainted(false); // Bỏ viền viền focus nét đứt khi click
        setFont(new Font("Arial", Font.BOLD, 14));
    }

    // Các hàm Getter để lấy tọa độ
    public int getRow() { return row; }
    public int getCol() { return col; }
}