package ui;

import controller.GameManager;
import hint.HintResult;
import model.DifficultyLevel;
import observer.GameEvent;
import observer.IGameEventListener;
import settings.Settings;
import settings.SettingsManager;

import javax.swing.*;
import java.awt.*;

/**
 * Composes the board, status display, menus, and gameplay controls.
 * Lifecycle events schedule dialogs and feedback on the Swing event dispatch thread.
 */
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

        JButton undoButton = new JButton("↩️ Undo");
        undoButton.addActionListener(e -> gameManager.undo());

        JButton hintButton = new JButton("💡 Hint");
        hintButton.addActionListener(e -> {
            HintResult hint = gameManager.useHint();
            boardPanel.highlightHint(hint);
            if (hint == null) {
                JOptionPane.showMessageDialog(this,
                        "No hints available at the moment!",
                        "Hint", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        bottomPanel.add(undoButton);
        bottomPanel.add(hintButton);

        add(topPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(boardPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);

        gameManager.registerBoardObserver(statusPanel);
        gameManager.registerBoardObserver(boardPanel);

        startNewGame(DifficultyLevel.EASY);
    }

    /**
     * Connects menu actions to game startup, preferences, scores, and application exit.
     */
    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("🎮 Game");

        JMenuItem easyItem = new JMenuItem("🟢 New Game (EASY 9x9)");
        easyItem.addActionListener(e -> startNewGame(DifficultyLevel.EASY));

        JMenuItem mediumItem = new JMenuItem("🟡 New Game (MEDIUM 16x16)");
        mediumItem.addActionListener(e -> startNewGame(DifficultyLevel.MEDIUM));

        JMenuItem hardItem = new JMenuItem("🔴 New Game (HARD 24x24)");
        hardItem.addActionListener(e -> startNewGame(DifficultyLevel.HARD));

        JMenuItem customItem = new JMenuItem("⚙️ New Game (CUSTOM)");
        customItem.addActionListener(e -> startCustomGame());

        JMenuItem settingsItem = new JMenuItem("⚙️ Settings");
        settingsItem.addActionListener(e -> {
            SettingsDialog settingsDialog = new SettingsDialog(this);
            settingsDialog.setVisible(true);
        });

        JMenuItem leaderboardItem = new JMenuItem("🏆 Leaderboard");
        leaderboardItem.addActionListener(e -> {
            LeaderboardDialog dialog = new LeaderboardDialog(this);
            dialog.setVisible(true);
        });

        JMenuItem exitItem = new JMenuItem("🚪 Exit");
        exitItem.addActionListener(e -> System.exit(0));

        gameMenu.add(easyItem);
        gameMenu.add(mediumItem);
        gameMenu.add(hardItem);
        gameMenu.add(customItem);
        gameMenu.addSeparator();
        gameMenu.add(settingsItem);
        gameMenu.add(leaderboardItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);

        menuBar.add(gameMenu);
        setJMenuBar(menuBar);
    }

    /**
     * Starts a preset game, rebuilds the grid, and sizes the window to its contents.
     */
    private void startNewGame(DifficultyLevel difficulty) {
        if (difficulty == DifficultyLevel.CUSTOM) {
            startCustomGame();
            return;
        }
        gameManager.startGame(difficulty);
        boardPanel.initializeBoard();
        statusPanel.updateStatus();

        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Uses saved custom dimensions and displays validation errors to the player.
     */
    private void startCustomGame() {
        Settings s = SettingsManager.getInstance().get();
        try {
            gameManager.startGame(DifficultyLevel.CUSTOM, s.getCustomSize(), s.getCustomMines());
            boardPanel.initializeBoard();
            statusPanel.updateStatus();

            pack();
            setLocationRelativeTo(null);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Custom Game Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Animates brief window movement with a Swing timer, then restores its location.
     */
    public void shakeScreen() {
        Point originalLocation = getLocation();
        int shakeDistance = 8;
        int delay = 25;
        int totalDuration = 250;
        long startTime = System.currentTimeMillis();

        Timer timer = new Timer(delay, null);
        timer.addActionListener(e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= totalDuration) {
                setLocation(originalLocation);
                timer.stop();
            } else {
                int offsetX = (int) ((Math.random() - 0.5) * 2 * shakeDistance);
                int offsetY = (int) ((Math.random() - 0.5) * 2 * shakeDistance);
                setLocation(originalLocation.x + offsetX, originalLocation.y + offsetY);
            }
        });
        timer.start();
    }

    /**
     * Schedules lifecycle feedback on the Swing thread, including dialogs and optional sound.
     */
    @Override
    public void onGameEvent(GameEvent event, String message) {
        SwingUtilities.invokeLater(() -> {
            boolean isSoundOn = SettingsManager.getInstance().get().isSoundEnabled();

            if (event == GameEvent.LIFE_LOST) {
                if (isSoundOn) {
                    Toolkit.getDefaultToolkit().beep();
                }
                shakeScreen();
            } else if (event == GameEvent.WON || event == GameEvent.LOST) {
                if (isSoundOn) {
                    Toolkit.getDefaultToolkit().beep();
                }
                if (event == GameEvent.LOST) {
                    shakeScreen();
                }
                String title = (event == GameEvent.WON) ? "Victory!" : "Game Over";
                int messageType = (event == GameEvent.LOST) ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE;

                JOptionPane.showMessageDialog(this, message, title, messageType);
            }
        });
    }
}
