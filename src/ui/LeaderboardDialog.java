package ui;

import model.DifficultyLevel;
import storage.LeaderboardManager;
import storage.ScoreRecord;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class LeaderboardDialog extends JDialog {

    public LeaderboardDialog(JFrame parent) {
        super(parent, "Leaderboard - Minesweeper", true);
        setSize(550, 400);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        // Sử dụng TabbedPane để chia bảng xếp hạng theo từng mức độ khó
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Easy (9x9)", createTablePanel(DifficultyLevel.EASY));
        tabbedPane.addTab("Medium (16x16)", createTablePanel(DifficultyLevel.MEDIUM));
        tabbedPane.addTab("Hard (24x24)", createTablePanel(DifficultyLevel.HARD));

        add(tabbedPane, BorderLayout.CENTER);

        // Nút đóng cửa sổ
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createTablePanel(DifficultyLevel level) {
        JPanel panel = new JPanel(new BorderLayout());

        // Cột hiển thị: Hạng, Tên, Thời gian (giây), Số mìn nổ, Gợi ý, Ngày giờ
        String[] columnNames = {"Rank", "Player", "Time (s)", "Mines Hit", "Hint Used", "Date"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho phép chỉnh sửa trực tiếp trên bảng
            }
        };

        LeaderboardManager manager = LeaderboardManager.getInstance();
        List<ScoreRecord> scores = manager.getTopScores(level);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        for (int i = 0; i < scores.size(); i++) {
            ScoreRecord record = scores.get(i);
            String dateStr = record.getTimestamp() > 0 ? dateFormat.format(new Date(record.getTimestamp())) : "N/A";

            Object[] rowData = {
                    (i + 1),
                    record.getPlayerName(),
                    record.getTimeInSeconds(),
                    record.getMinesHit(),
                    record.isHintUsed() ? "Yes" : "No",
                    dateStr
            };
            tableModel.addRow(rowData);
        }

        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);

        // Căn chỉnh kích thước cột cho đẹp mắt
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(4).setPreferredWidth(70);
        table.getColumnModel().getColumn(5).setPreferredWidth(110);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }
}