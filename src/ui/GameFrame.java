package ui;

import controller.GameManager;
import hint.HintResult;
import model.DifficultyLevel;
import observer.GameEvent;
import observer.IGameEventListener;

import javax.swing.*;
import java.awt.*;

public class GameFrame extends JFrame implements IGameEventListener {
    private GameManager gameManager;
    private BoardPanel boardPanel;
    private StatusPanel statusPanel;

    public GameFrame(GameManager gameManager) {
        this.gameManager = gameManager;
        setTitle("Minesweeper");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        gameManager.registerEventListener(this);

        setupMenuBar();

        statusPanel = new StatusPanel(gameManager);
        boardPanel = new BoardPanel(gameManager);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(statusPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout());

        JButton undoButton = new JButton("Undo");
        undoButton.addActionListener(e -> gameManager.undo());

        // --- SỬA LẠI LOGIC NÚT HINT ---
        JButton hintButton = new JButton("Hint");
        hintButton.addActionListener(e -> {
            HintResult hint = gameManager.useHint();
            if (hint != null) {
                boardPanel.highlightHint(hint);
                // Nếu ô đó đang bị cắm cờ, hiện cảnh báo
                if (hint.isOnFlaggedCell()) {
                    JOptionPane.showMessageDialog(this,
                            "The hint points to a flagged cell. Please unflag it first!",
                            "Warning", JOptionPane.WARNING_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "No hints available at the moment!",
                        "Hint", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        // ------------------------------

        bottomPanel.add(undoButton);
        bottomPanel.add(hintButton);

        add(topPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(boardPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); // Bỏ viền mặc định
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);  // Cuộn mượt hơn
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        gameManager.registerBoardObserver(statusPanel);
        gameManager.registerBoardObserver(boardPanel);

        startNewGame(DifficultyLevel.EASY);
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");

        JMenuItem easyItem = new JMenuItem("New Game (EASY 9x9)");
        easyItem.addActionListener(e -> startNewGame(DifficultyLevel.EASY));

        JMenuItem mediumItem = new JMenuItem("New Game (MEDIUM 16x16)");
        mediumItem.addActionListener(e -> startNewGame(DifficultyLevel.MEDIUM));

        JMenuItem hardItem = new JMenuItem("New Game (HARD 24x24)");
        hardItem.addActionListener(e -> startNewGame(DifficultyLevel.HARD));

        // --- THÊM MỤC SETTINGS VÀO MENU ---
        JMenuItem settingsItem = new JMenuItem("Settings");
        settingsItem.addActionListener(e -> {
            SettingsDialog settingsDialog = new SettingsDialog(this);
            settingsDialog.setVisible(true);
        });
        // ----------------------------------

        JMenuItem leaderboardItem = new JMenuItem("Leaderboard");
        leaderboardItem.addActionListener(e -> {
            LeaderboardDialog dialog = new LeaderboardDialog(this);
            dialog.setVisible(true);
        });

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));

        gameMenu.add(easyItem);
        gameMenu.add(mediumItem);
        gameMenu.add(hardItem);
        gameMenu.addSeparator();
        gameMenu.add(settingsItem);     // Thêm Settings vào Menu
        gameMenu.add(leaderboardItem);  // Thêm Leaderboard vào Menu
        gameMenu.addSeparator();
        gameMenu.add(exitItem);

        menuBar.add(gameMenu);
        setJMenuBar(menuBar);
    }

    private void startNewGame(DifficultyLevel difficulty) {
        gameManager.startGame(difficulty);
        boardPanel.initializeBoard();
        statusPanel.updateStatus();

        pack();
        setLocationRelativeTo(null);
    }

    @Override
    public void onGameEvent(GameEvent event, String message) {
        SwingUtilities.invokeLater(() -> {
            if (event == GameEvent.WON || event == GameEvent.LOST || event == GameEvent.LIFE_LOST) {
                String title = (event == GameEvent.WON) ? "Victory!" : "Notification";
                int messageType = (event == GameEvent.LOST) ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE;

                JOptionPane.showMessageDialog(this, message, title, messageType);
            }
        });
    }
}