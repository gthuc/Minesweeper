package ui;
import javax.swing.JFrame;
import java.awt.BorderLayout;
import controller.GameManager;

public class GameFrame extends JFrame {
    private StatusPanel statusPanel;
    private BoardPanel boardPanel;

    public GameFrame(GameManager gm) {
        setTitle("Minesweeper - DSA Project");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 1. Sửa dòng này: Truyền gameManager vào StatusPanel
        statusPanel = new StatusPanel(gm);
        boardPanel = new BoardPanel(gm);

        // 2. Thêm dòng này: Đăng ký StatusPanel làm cảm biến thứ 2 (dưới dòng gm.registerBoardObserver(boardPanel);)
        gm.registerBoardObserver(statusPanel);

        // KẾT NỐI OBSERVER: Đăng ký giao diện với hệ thống logic
        gm.registerBoardObserver(boardPanel);

        add(statusPanel, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }
}