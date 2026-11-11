package ui;

import model.DifficultyLevel;
import settings.Settings;
import settings.SettingsManager;
import settings.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Edits a settings copy through form controls and submits validated changes.
 * Input errors keep the dialog open so the player can correct them.
 */
public class SettingsDialog extends JDialog {
    private JTextField nameField;
    private JComboBox<DifficultyLevel> difficultyComboBox;
    private JComboBox<Theme> themeComboBox;
    private JCheckBox soundCheckBox;
    private JTextField sizeField;
    private JTextField minesField;

    public SettingsDialog(JFrame parent) {
        super(parent, "⚙️ Settings - Minesweeper", true);
        setSize(420, 320);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        Settings settings = SettingsManager.getInstance().get();

        JPanel formPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        formPanel.add(new JLabel("👤 Player Name:"));
        nameField = new JTextField(settings.getPlayerName());
        formPanel.add(nameField);

        formPanel.add(new JLabel("🎯 Default Difficulty:"));
        difficultyComboBox = new JComboBox<>(DifficultyLevel.values());
        difficultyComboBox.setSelectedItem(settings.getDefaultDifficulty());
        formPanel.add(difficultyComboBox);

        formPanel.add(new JLabel("🎨 Theme:"));
        themeComboBox = new JComboBox<>(Theme.values());
        themeComboBox.setSelectedItem(settings.getTheme());
        formPanel.add(themeComboBox);

        formPanel.add(new JLabel("📐 Custom Size (Rows/Cols):"));
        sizeField = new JTextField(String.valueOf(settings.getCustomSize()));
        formPanel.add(sizeField);

        formPanel.add(new JLabel("💣 Custom Mines:"));
        minesField = new JTextField(String.valueOf(settings.getCustomMines()));
        formPanel.add(minesField);

        formPanel.add(new JLabel("🔊 Sound Enabled:"));
        soundCheckBox = new JCheckBox();
        soundCheckBox.setSelected(settings.isSoundEnabled());
        formPanel.add(soundCheckBox);

        add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("💾 Save");
        JButton cancelButton = new JButton("🚫 Cancel");

        saveButton.addActionListener(e -> saveSettings());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Parses form values and submits one validated settings update before closing.
     */
    private void saveSettings() {
        try {
            int customSize = Integer.parseInt(sizeField.getText().trim());
            int customMines = Integer.parseInt(minesField.getText().trim());
            String newName = nameField.getText().trim();
            DifficultyLevel diff = (DifficultyLevel) difficultyComboBox.getSelectedItem();
            Theme theme = (Theme) themeComboBox.getSelectedItem();
            boolean sound = soundCheckBox.isSelected();

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
