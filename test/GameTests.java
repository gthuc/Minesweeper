import command.FlagCommand;
import command.RevealCommand;
import controller.GameManager;
import factory.StandardBoardFactory;
import hint.HintResult;
import hint.HintSystem;
import hint.HintType;
import model.CellModel;
import model.DifficultyLevel;
import model.GameBoardModel;
import model.GameStatus;
import storage.LeaderboardManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

/** Chạy: xem README. Không cần JUnit. */
public class GameTests {
    static int passed, failed;

    static void check(boolean ok, String name) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "PASS " : "FAIL ") + name);
    }

    static GameBoardModel board(int size, int mines, long seed) {
        GameBoardModel b = new GameBoardModel(new StandardBoardFactory(), new Random(seed));
        b.initialize(size, mines);
        return b;
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("ms-test");
        System.setProperty("minesweeper.scores", dir.resolve("scores.dat").toString());

        testFirstClickSafe();
        testCountersAndUndo();
        testFlagLimit();
        testHintsAreSound();
        testMineNotUndoable();
        testGameFlow();

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    static void testFirstClickSafe() {
        boolean ok = true;
        for (long seed = 0; seed < 200; seed++) {
            GameBoardModel b = board(9, 10, seed);
            b.revealCell(4, 4);
            ok &= !b.getCell(4, 4).isMine();
            for (int[] n : b.getNeighbors(4, 4)) ok &= !b.getCell(n[0], n[1]).isMine();
            int mines = 0;
            for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) if (b.getCell(i, j).isMine()) mines++;
            ok &= mines == 10;
        }
        check(ok, "click đầu tiên và 8 ô quanh nó không có mìn; đủ số mìn");
    }

    static void testCountersAndUndo() {
        GameBoardModel b = board(9, 10, 1);
        List<int[]> first = b.revealCell(4, 4);
        check(b.getRevealedSafeCount() == first.size() && first.size() > 1, "flood fill lật nhiều ô, bộ đếm khớp");
        b.undoReveal(first);
        boolean allHidden = true;
        for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) allHidden &= !b.getCell(i, j).isRevealed();
        check(b.getRevealedSafeCount() == 0 && allHidden, "undoReveal khôi phục ô và bộ đếm");

        for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) if (!b.getCell(i, j).isMine()) b.revealCell(i, j);
        check(b.getRevealedSafeCount() == 71 && b.getRevealedMineCount() == 0, "lật hết ô an toàn -> 71 ô");
    }

    static void testFlagLimit() {
        GameBoardModel b = board(9, 10, 2);
        boolean ok = true;
        for (int i = 0; i < 10; i++) ok &= b.toggleFlag(i / 9, i % 9);
        check(ok && b.getFlagCount() == 10, "cắm đủ 10 cờ");
        check(!b.toggleFlag(5, 5), "không cắm quá số mìn");
        check(b.toggleFlag(0, 0) && b.getFlagCount() == 9, "gỡ cờ giảm bộ đếm");
        check(!board(9, 10, 3).toggleFlag(-1, 0) && !new FlagCommand(b, 9, 9).execute(), "toạ độ ngoài biên bị bỏ qua");
    }

    /** Mô phỏng người chơi chỉ làm theo gợi ý, kể cả khi đã nổ mìn / cắm cờ sai. */
    static void testHintsAreSound() {
        int violations = 0, hintsChecked = 0;
        for (long seed = 0; seed < 300; seed++) {
            GameBoardModel b = board(16, 40, seed);
            b.revealCell(8, 8);

            if (seed % 2 == 1) { // cố tình nổ một mìn trước
                outer:
                for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
                    if (b.getCell(i, j).isMine() && !b.getCell(i, j).isRevealed()) { b.revealCell(i, j); break outer; }
                }
            }
            if (seed % 3 == 0) { // cắm cờ sai một ô an toàn
                outer2:
                for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
                    CellModel c = b.getCell(i, j);
                    if (!c.isMine() && !c.isRevealed()) { b.toggleFlag(i, j); break outer2; }
                }
            }

            for (int step = 0; step < 600; step++) {
                HintResult h = HintSystem.getInstance().getHint(b);
                if (h == null || h.getType() == HintType.PROBABLE) break;
                CellModel c = b.getCell(h.getX(), h.getY());
                hintsChecked++;
                if (h.getType() == HintType.CERTAIN_SAFE) {
                    if (c.isMine()) { violations++; break; }
                    if (c.isFlagged()) b.toggleFlag(h.getX(), h.getY());
                    b.revealCell(h.getX(), h.getY());
                } else {
                    if (!c.isMine()) { violations++; break; }
                    if (c.isFlagged() || !b.toggleFlag(h.getX(), h.getY())) break;
                }
            }
        }
        System.out.println("   (đã kiểm tra " + hintsChecked + " gợi ý chắc chắn)");
        check(hintsChecked > 0 && violations == 0, "gợi ý CERTAIN_* luôn đúng (kể cả sau khi nổ mìn / cờ sai)");

        GameBoardModel fresh = board(9, 10, 5);
        HintResult first = HintSystem.getInstance().getHint(fresh);
        check(first != null && first.getType() == HintType.CERTAIN_SAFE, "trước click đầu: gợi ý ô an toàn");
    }

    static void testMineNotUndoable() {
        GameBoardModel b = board(9, 10, 7);
        b.revealCell(4, 4);
        int mx = -1, my = -1;
        for (int i = 0; i < 9 && mx < 0; i++) for (int j = 0; j < 9; j++) if (b.getCell(i, j).isMine()) { mx = i; my = j; break; }
        RevealCommand mine = new RevealCommand(b, mx, my);
        mine.execute();
        check(!mine.isUndoable(), "lật trúng mìn -> không undo được");
    }

    static void testGameFlow() {
        GameManager gm = GameManager.getInstance();

        // Thắng + ghi bảng điểm
        gm.startGame(DifficultyLevel.EASY);
        check(gm.getGameStatus() == GameStatus.Playing, "startGame -> Playing");
        GameBoardModel b = gm.getGameBoard();
        gm.handleCellAction(new RevealCommand(b, 4, 4));

        gm.pause();
        int before = b.getRevealedSafeCount();
        gm.handleCellAction(new RevealCommand(b, 0, 0));
        check(gm.getGameStatus() == GameStatus.Paused && b.getRevealedSafeCount() == before, "Paused bỏ qua lệnh");
        gm.resume();
        check(gm.getGameStatus() == GameStatus.Playing, "resume -> Playing");

        for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) {
            if (!b.getCell(i, j).isMine()) gm.handleCellAction(new RevealCommand(b, i, j));
        }
        check(gm.getGameStatus() == GameStatus.Won, "lật hết ô an toàn -> Won");
        check(LeaderboardManager.getInstance().getTopScores(DifficultyLevel.EASY).size() == 1, "thắng -> ghi bảng điểm");
        check(gm.useHint() == null, "hết ván -> không còn gợi ý");

        // Thua sau 3 mìn; undo không trả lại mạng
        gm.startGame(DifficultyLevel.EASY);
        b = gm.getGameBoard();
        gm.handleCellAction(new RevealCommand(b, 4, 4));
        int hits = 0;
        for (int i = 0; i < 9 && hits < 3; i++) for (int j = 0; j < 9 && hits < 3; j++) {
            if (b.getCell(i, j).isMine()) {
                gm.handleCellAction(new RevealCommand(b, i, j));
                hits++;
                if (hits == 1) {
                    gm.undo();
                    check(b.getRevealedMineCount() == 1, "undo không lấy lại được mạng đã mất");
                }
            }
        }
        check(gm.getGameStatus() == GameStatus.Lost, "trúng 3 mìn -> Lost");
        check(LeaderboardManager.getInstance().getTopScores(DifficultyLevel.EASY).size() == 1, "thua -> không ghi điểm");

        boolean threw = false;
        try { gm.startGame(DifficultyLevel.CUSTOM, 5, 20); } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "CUSTOM không hợp lệ bị từ chối");
    }
}
