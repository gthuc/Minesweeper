// File: src/ui/LeaderboardDialog.java
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

    private JTabbedPane tabbedPane;

    public LeaderboardDialog(JFrame parent) {
        super(parent, "🏆 Leaderboard - Minesweeper", true);
        setSize(550, 420);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();
        refreshTabs();

        add(tabbedPane, BorderLayout.CENTER);

        JButton clearButton = new JButton("🗑️ Clear Scores");
        JButton closeButton = new JButton("❌ Close");

        clearButton.addActionListener(e -> clearSelectedLeaderboard());
        closeButton.addActionListener(e -> dispose());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(clearButton);
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void refreshTabs() {
        int currentIndex = tabbedPane.getSelectedIndex();
        tabbedPane.removeAll();

        tabbedPane.addTab("🟢 Easy (9x9)", createTablePanel(DifficultyLevel.EASY));
        tabbedPane.addTab("🟡 Medium (16x16)", createTablePanel(DifficultyLevel.MEDIUM));
        tabbedPane.addTab("🔴 Hard (24x24)", createTablePanel(DifficultyLevel.HARD));

        if (currentIndex >= 0 && currentIndex < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(currentIndex);
        }
    }

    private void clearSelectedLeaderboard() {
        int index = tabbedPane.getSelectedIndex();
        DifficultyLevel level;

        switch (index) {
            case 0: level = DifficultyLevel.EASY; break;
            case 1: level = DifficultyLevel.MEDIUM; break;
            case 2: level = DifficultyLevel.HARD; break;
            default: return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to clear the leaderboard for " + level + "?",
                "Confirm Clear",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            LeaderboardManager.getInstance().clear(level);
            refreshTabs();
        }
    }

    private JPanel createTablePanel(DifficultyLevel level) {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columnNames = {"Rank", "Player", "Time (s)", "Mines Hit", "Hint Used", "Date"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
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
        table.getTableHeader().setReorderingAllowed(false);

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