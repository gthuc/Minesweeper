
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


        setOpaque(true);  
        setContentAreaFilled(true);  
        setBorder(BorderFactory.createLineBorder(Color.GRAY));  

    }

    public int getRow() { return row; }
    public int getCol() { return col; }
}