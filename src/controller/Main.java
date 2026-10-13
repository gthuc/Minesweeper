package controller;

import hint.HintResult;
import model.DifficultyLevel;
import model.GameStatus;
import settings.Settings;
import settings.SettingsManager;
import storage.LeaderboardManager;
import storage.ScoreRecord;
import view.ConsoleView;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/** Console: mở đầu bằng MENU (Play / Difficulty / Leaderboard / Settings / Quit). */
public class Main {
    public static void main(String[] args) {
        GameManager gm = GameManager.getInstance();
        ConsoleView view = new ConsoleView();
        gm.registerBoardObserver(view);
        gm.registerEventListener((event, message) -> System.out.println(message));

        System.out.println("=== MINESWEEPER ===");
        printMenu();

        Scanner in = new Scanner(System.in);
        while (true) {
            boolean inGame = gm.getGameBoard() != null;
            System.out.print(inGame ? "game> " : "menu> ");
            if (!in.hasNextLine()) break;
            String line = in.nextLine().trim();
            if (line.isEmpty()) continue;

            try {
                boolean quit = inGame ? handleGameCommand(gm, view, in, line) : handleMenuCommand(gm, view, line);
                if (quit) return;
            } catch (NumberFormatException e) {
                System.out.println("Giá trị phải là số nguyên.");
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // ---------- Menu ----------

    private static boolean handleMenuCommand(GameManager gm, ConsoleView view, String line) {
        String[] t = line.split("\\s+");
        switch (t[0].toLowerCase()) {
            case "1":
            case "play":
                gm.startConfiguredGame();
                view.consumeDirty();
                view.render(gm);
                return false;
            case "2":
            case "difficulty":
                chooseDifficulty(t);
                return false;
            case "3":
            case "leaderboard":
                printLeaderboard();
                return false;
            case "4":
            case "settings":
                handleSettings(t);
                return false;
            case "5":
            case "q":
            case "quit":
                return true;
            default:
                printMenu();
                return false;
        }
    }

    private static void chooseDifficulty(String[] t) {
        SettingsManager sm = SettingsManager.getInstance();
        if (t.length < 2) {
            Settings s = sm.get();
            System.out.println("Độ khó hiện tại: " + describe(s));
            System.out.println("Đổi: 2 easy | 2 medium | 2 hard | 2 custom <size " + DifficultyLevel.CUSTOM_MIN_SIZE
                    + "-" + DifficultyLevel.CUSTOM_MAX_SIZE + "> <mines>");
            return;
        }
        final DifficultyLevel level;
        try {
            level = DifficultyLevel.valueOf(t[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Độ khó phải là easy | medium | hard | custom <size> <mines>");
        }
        if (level == DifficultyLevel.CUSTOM) {
            if (t.length < 4) throw new IllegalArgumentException("Cú pháp: 2 custom <size> <mines>");
            final int size = Integer.parseInt(t[2]);
            final int mines = Integer.parseInt(t[3]);
            sm.update(s -> {
                s.setCustom(size, mines);
                s.setDefaultDifficulty(DifficultyLevel.CUSTOM);
            });
        } else {
            sm.update(s -> s.setDefaultDifficulty(level));
        }
        System.out.println("Độ khó: " + describe(sm.get()));
    }

    private static void handleSettings(String[] t) {
        SettingsManager sm = SettingsManager.getInstance();
        if (t.length >= 2) {
            switch (t[1].toLowerCase()) {
                case "theme":
                    sm.update(s -> s.setTheme(s.getTheme().toggled()));
                    System.out.println("Giao diện sẽ áp dụng từ ván mới.");
                    break;
                case "name":
                    final String name = String.join(" ", Arrays.copyOfRange(t, 2, t.length));
                    sm.update(s -> s.setPlayerName(name));
                    break;
                case "sound":
                    sm.update(s -> s.setSoundEnabled(!s.isSoundEnabled()));
                    break;
                default:
                    throw new IllegalArgumentException("Cú pháp: 4 theme | 4 name <tên> | 4 sound");
            }
        }
        Settings s = sm.get();
        System.out.println("Cài đặt: giao diện=" + s.getTheme() + ", tên=" + s.getPlayerName()
                + ", âm thanh=" + (s.isSoundEnabled() ? "bật" : "tắt") + ", độ khó=" + describe(s));
        System.out.println("Đổi: 4 theme | 4 name <tên> | 4 sound");
    }

    private static String describe(Settings s) {
        DifficultyLevel d = s.getDefaultDifficulty();
        if (d == DifficultyLevel.CUSTOM) {
            return "CUSTOM (" + s.getCustomSize() + "x" + s.getCustomSize() + ", " + s.getCustomMines() + " mìn)";
        }
        return d + " (" + d.getSize() + "x" + d.getSize() + ", " + d.getMines() + " mìn)";
    }

    // ---------- Trong ván ----------

    private static boolean handleGameCommand(GameManager gm, ConsoleView view, Scanner in, String line) {
        String[] t = line.split("\\s+");
        String cmd = t[0].toLowerCase();
        GameStatus before = gm.getGameStatus();
        boolean redraw = true;

        switch (cmd) {
            case "r":
            case "f":
                requirePlaying(gm);
                if (t.length < 3) throw new IllegalArgumentException("Cú pháp: " + cmd + " <hàng> <cột>");
                int x = Integer.parseInt(t[1]);
                int y = Integer.parseInt(t[2]);
                if (cmd.equals("r")) gm.reveal(x, y); else gm.toggleFlag(x, y);
                break;
            case "u":
                if (!gm.undo()) System.out.println("Không có gì để hoàn tác.");
                break;
            case "h":
                printHint(gm, gm.useHint());
                redraw = false;
                break;
            case "p":
                if (gm.getGameStatus() == GameStatus.PAUSED) gm.resume(); else gm.pause();
                break;
            case "n":
                gm.restart();
                break;
            case "m":
                gm.exitToMenu();
                printMenu();
                return false;
            case "l":
                printLeaderboard();
                redraw = false;
                break;
            case "q":
                return true;
            default:
                printHelp();
                redraw = false;
        }

        if (redraw || view.consumeDirty()) {
            view.consumeDirty();
            view.render(gm);
        }
        if (before == GameStatus.PLAYING && gm.getGameStatus() == GameStatus.WON && gm.getLastRecord() != null) {
            promptName(gm, in);
        }
        return false;
    }

    private static void promptName(GameManager gm, Scanner in) {
        System.out.print("Nhập tên để ghi bảng điểm (Enter để giữ '" + gm.getLastRecord().getPlayerName() + "'): ");
        if (!in.hasNextLine()) return;
        String name = in.nextLine().trim();
        if (name.isEmpty()) return;
        try {
            gm.renameLastScore(name);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage() + " Giữ tên cũ.");
        }
    }

    private static void requirePlaying(GameManager gm) {
        if (gm.getGameStatus() != GameStatus.PLAYING) {
            throw new IllegalStateException("Ván chơi không ở trạng thái đang chơi (gõ 'p' để tiếp tục, 'n' chơi lại, 'm' về menu).");
        }
    }

    private static void printHint(GameManager gm, HintResult h) {
        if (h == null) {
            System.out.println(gm.getGameStatus() == GameStatus.PLAYING
                    ? "Không có gợi ý nào lúc này."
                    : "Không có gợi ý (ván chưa chơi hoặc đã kết thúc).");
            return;
        }
        String meaning;
        switch (h.getType()) {
            case CERTAIN_SAFE: meaning = "CHẮC CHẮN AN TOÀN"; break;
            case CERTAIN_MINE: meaning = "CHẮC CHẮN LÀ MÌN"; break;
            default:           meaning = "ít rủi ro nhất (chỉ là ước lượng)";
        }
        System.out.println("Gợi ý: ô (" + h.getX() + ", " + h.getY() + ") " + meaning
                + (h.isOnFlaggedCell() ? " - ô đang cắm cờ, gỡ cờ (f) trước khi lật" : ""));
    }

    // ---------- Hiển thị chung ----------

    private static void printLeaderboard() {
        for (DifficultyLevel level : DifficultyLevel.presets()) {
            List<ScoreRecord> top = LeaderboardManager.getInstance().getTopScores(level);
            System.out.println("== " + level + " ==");
            if (top.isEmpty()) System.out.println("  (trống)");
            for (int i = 0; i < top.size(); i++) System.out.println("  " + (i + 1) + ". " + top.get(i));
        }
        System.out.println("(CUSTOM không ghi bảng điểm)");
    }

    private static void printMenu() {
        System.out.println("  1. Play         (chơi với độ khó hiện tại)");
        System.out.println("  2. Difficulty   (2 easy | medium | hard | custom <size> <mines>)");
        System.out.println("  3. Leaderboard");
        System.out.println("  4. Settings     (4 theme | 4 name <tên> | 4 sound)");
        System.out.println("  5. Quit");
    }

    private static void printHelp() {
        System.out.println("Lệnh: r <hàng> <cột> (lật) | f <hàng> <cột> (cờ) | u (undo) | h (gợi ý) | p (tạm dừng/tiếp tục)");
        System.out.println("      n (chơi lại) | m (về menu) | l (bảng điểm) | q (thoát)");
    }
}
