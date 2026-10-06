package controller;

import command.FlagCommand;
import command.RevealCommand;
import factory.StandardBoardFactory;
import factory.ThemeBoardFactory;
import hint.HintResult;
import model.DifficultyLevel;
import model.GameStatus;
import storage.LeaderboardManager;
import storage.ScoreRecord;
import view.ConsoleView;

import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        GameManager gm = GameManager.getInstance();
        ConsoleView view = new ConsoleView();
        gm.registerBoardObserver(view);

        boolean theme = false;
        printHelp();
        gm.startGame(DifficultyLevel.EASY);
        view.render(gm);

        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            if (!in.hasNextLine()) break;
            String line = in.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] t = line.split("\\s+");
            String cmd = t[0].toLowerCase();
            boolean redraw = true;

            try {
                switch (cmd) {
                    case "r":
                    case "f":
                        requirePlaying(gm);
                        if (t.length < 3) throw new IllegalArgumentException("Cú pháp: " + cmd + " <hàng> <cột>");
                        int x = Integer.parseInt(t[1]);
                        int y = Integer.parseInt(t[2]);
                        gm.handleCellAction(cmd.equals("r")
                                ? new RevealCommand(gm.getGameBoard(), x, y)
                                : new FlagCommand(gm.getGameBoard(), x, y));
                        break;
                    case "u":
                        gm.undo();
                        break;
                    case "h":
                        printHint(gm.useHint());
                        redraw = false;
                        break;
                    case "p":
                        if (gm.getGameStatus() == GameStatus.Paused) gm.resume(); else gm.pause();
                        break;
                    case "n":
                        gm.setCellFactory(theme ? new ThemeBoardFactory() : new StandardBoardFactory());
                        startNewGame(gm, t);
                        break;
                    case "t":
                        theme = !theme;
                        System.out.println("Giao diện " + (theme ? "Theme" : "Classic") + " sẽ áp dụng từ ván mới.");
                        redraw = false;
                        break;
                    case "name":
                        gm.setPlayerName(line.substring(4));
                        System.out.println("Tên người chơi: " + gm.getPlayerName());
                        redraw = false;
                        break;
                    case "l":
                        printLeaderboard();
                        redraw = false;
                        break;
                    case "q":
                        return;
                    default:
                        printHelp();
                        redraw = false;
                }
            } catch (NumberFormatException e) {
                System.out.println("Tọa độ phải là số nguyên.");
                redraw = false;
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println(e.getMessage());
                redraw = false;
            }

            if (redraw || view.consumeDirty()) {
                view.consumeDirty();
                view.render(gm);
            }
        }
    }

    private static void requirePlaying(GameManager gm) {
        if (gm.getGameStatus() != GameStatus.Playing) {
            throw new IllegalStateException("Ván chơi không ở trạng thái đang chơi (gõ 'n' để ván mới, 'p' để tiếp tục).");
        }
    }

    private static void startNewGame(GameManager gm, String[] t) {
        String level = t.length > 1 ? t[1].toUpperCase() : "EASY";
        DifficultyLevel d;
        try {
            d = DifficultyLevel.valueOf(level);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Độ khó phải là easy | medium | hard | custom <size> <mines>");
        }
        if (d == DifficultyLevel.CUSTOM) {
            if (t.length < 4) throw new IllegalArgumentException("Cú pháp: n custom <size 5-30> <mines>");
            gm.startGame(d, Integer.parseInt(t[2]), Integer.parseInt(t[3]));
        } else {
            gm.startGame(d);
        }
    }

    private static void printHint(HintResult h) {
        if (h == null) {
            System.out.println("Không có gợi ý (ván chưa chơi hoặc đã kết thúc).");
            return;
        }
        String meaning;
        switch (h.getType()) {
            case CERTAIN_SAFE: meaning = "CHẮC CHẮN AN TOÀN"; break;
            case CERTAIN_MINE: meaning = "CHẮC CHẮN LÀ MÌN"; break;
            default:           meaning = "ít rủi ro nhất (chỉ là ước lượng)";
        }
        System.out.println("Gợi ý: ô (" + h.getX() + ", " + h.getY() + ") " + meaning);
    }

    private static void printLeaderboard() {
        for (DifficultyLevel level : DifficultyLevel.values()) {
            if (level == DifficultyLevel.CUSTOM) continue;
            List<ScoreRecord> top = LeaderboardManager.getInstance().getTopScores(level);
            System.out.println("== " + level + " ==");
            if (top.isEmpty()) System.out.println("  (trống)");
            for (int i = 0; i < top.size(); i++) System.out.println("  " + (i + 1) + ". " + top.get(i));
        }
    }

    private static void printHelp() {
        System.out.println("Lệnh: r <hàng> <cột> (lật) | f <hàng> <cột> (cờ) | u (undo) | h (gợi ý) | p (tạm dừng/tiếp tục)");
        System.out.println("      n [easy|medium|hard|custom <size> <mines>] (ván mới) | t (đổi giao diện)");
        System.out.println("      name <tên> | l (bảng điểm) | q (thoát)");
    }
}
