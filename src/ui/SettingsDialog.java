package ui;

import model.DifficultyLevel;
import settings.Settings;
import settings.SettingsManager;
import settings.Theme;

import javax.swing.*;
import java.awt.*;

public class SettingsDialog extends JDialog {
    private JTextField nameField;
    private JComboBox<DifficultyLevel> difficultyComboBox;
    private JComboBox<Theme> themeComboBox;
    private JCheckBox soundCheckBox;
    private JTextField sizeField;
    private JTextField minesField;

    public SettingsDialog(JFrame parent) {
        super(parent, "Settings - Minesweeper", true);
        setSize(400, 300);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        Settings settings = SettingsManager.getInstance().get();

        JPanel formPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Tên người chơi
        formPanel.add(new JLabel("Player Name:"));
        nameField = new JTextField(settings.getPlayerName());
        formPanel.add(nameField);

        // Độ khó mặc định
        formPanel.add(new JLabel("Default Difficulty:"));
        difficultyComboBox = new JComboBox<>(new DifficultyLevel[]{
                DifficultyLevel.EASY, DifficultyLevel.MEDIUM, DifficultyLevel.HARD
        });
        difficultyComboBox.setSelectedItem(settings.getDefaultDifficulty());
        formPanel.add(difficultyComboBox);

        // Giao diện (Theme)
        formPanel.add(new JLabel("Theme:"));
        themeComboBox = new JComboBox<>(Theme.values());
        themeComboBox.setSelectedItem(settings.getTheme());
        formPanel.add(themeComboBox);

        // Kích thước tùy chỉnh (Custom Size)
        formPanel.add(new JLabel("Custom Size (Rows/Cols):"));
        sizeField = new JTextField(String.valueOf(settings.getCustomSize()));
        formPanel.add(sizeField);

        // Số mìn tùy chỉnh (Custom Mines)
        formPanel.add(new JLabel("Custom Mines:"));
        minesField = new JTextField(String.valueOf(settings.getCustomMines()));
        formPanel.add(minesField);

        // Bật/tắt âm thanh
        formPanel.add(new JLabel("Sound Enabled:"));
        soundCheckBox = new JCheckBox();
        soundCheckBox.setSelected(settings.isSoundEnabled());
        formPanel.add(soundCheckBox);

        add(formPanel, BorderLayout.CENTER);

        // Nút Lưu và Hủy
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> saveSettings());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void saveSettings() {
        try {
            int customSize = Integer.parseInt(sizeField.getText().trim());
            int customMines = Integer.parseInt(minesField.getText().trim());
            String newName = nameField.getText().trim();
            DifficultyLevel diff = (DifficultyLevel) difficultyComboBox.getSelectedItem();
            Theme theme = (Theme) themeComboBox.getSelectedItem();
            boolean sound = soundCheckBox.isSelected();

            // Sử dụng SettingsManager để cập nhật và lưu file an toàn
            SettingsManager.getInstance().update(s -> {
                s.setPlayerName(newName);
                s.setDefaultDifficulty(diff);
                s.setTheme(theme);
                s.setCustom(customSize, customMines);
                s.setSoundEnabled(sound);
            });

            JOptionPane.showMessageDialog(this, "Settings saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Custom size and mines must be valid integers!", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}