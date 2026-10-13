import command.FlagCommand;
import command.RevealCommand;
import controller.GameManager;
import controller.GameSnapshot;
import factory.StandardBoardFactory;
import hint.HintResult;
import hint.HintSystem;
import hint.HintType;
import model.CellModel;
import model.DifficultyLevel;
import model.GameBoardModel;
import model.GameStatus;
import observer.GameEvent;
import observer.IGameEventListener;
import settings.FileSettingsStorage;
import settings.Settings;
import settings.SettingsManager;
import settings.Theme;
import storage.FileScoreStorage;
import storage.LeaderboardManager;
import storage.ScoreRecord;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Chạy: xem README. Không cần JUnit. */
public class GameTests {
    static int passed, failed;
    static Path dir;

    static void check(boolean ok, String name) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "PASS " : "FAIL ") + name);
    }

    static GameBoardModel board(int size, int mines, long seed) {
        GameBoardModel b = new GameBoardModel(new StandardBoardFactory(), new Random(seed));
        b.initialize(size, mines);
        return b;
    }

    /** Cô lập singleton giữa các test: mỗi test dùng file riêng trong thư mục tạm. */
    static void freshManagers(String tag) {
        LeaderboardManager.resetInstance(new FileScoreStorage(dir.resolve(tag + "-scores.dat").toString()));
        SettingsManager.resetInstance(new FileSettingsStorage(dir.resolve(tag + "-settings.properties").toString()));
    }

    public static void main(String[] args) throws Exception {
        dir = Files.createTempDirectory("ms-test");
        System.setProperty("minesweeper.scores", dir.resolve("scores.dat").toString());
        System.setProperty("minesweeper.settings", dir.resolve("settings.properties").toString());

        testFirstClickSafe();
        testCountersAndUndo();
        testFlagLimit();
        testFlagVsMinesLeft();
        testHintsAreSound();
        testHintFlaggedCells();
        testMineNotUndoable();
        testUndoSkipsNoOps();
        testDifficultyConfig();
        testSettings();
        testLeaderboard();
        testMenuFlow();
        testHintPreClick();
        testTimerFreezesWhenPaused();
        testGameFlow();
        testEventsAndSnapshot();

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
        check(b.undoReveal(first), "undoReveal báo có thay đổi");
        boolean allHidden = true;
        for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) allHidden &= !b.getCell(i, j).isRevealed();
        check(b.getRevealedSafeCount() == 0 && allHidden, "undoReveal khôi phục ô và bộ đếm");
        check(!b.undoReveal(first), "undoReveal lần 2 không còn thay đổi gì");

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

    static void testFlagVsMinesLeft() {
        GameBoardModel b = board(9, 10, 4);
        b.revealCell(4, 4);
        int hit = 0;
        for (int i = 0; i < 9 && hit < 2; i++) for (int j = 0; j < 9 && hit < 2; j++) {
            if (b.getCell(i, j).isMine()) { b.revealCell(i, j); hit++; }
        }
        int flags = 0;
        for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) if (b.toggleFlag(i, j)) flags++;
        check(flags == 8, "đã nổ 2 mìn thì chỉ cắm được tối đa 8 cờ");
        check(b.getRemainingMines() == 0, "mìn còn lại không bao giờ âm");
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

    /** Khi mọi ô chưa lật đều có cờ (nghĩa là có cờ sai) vẫn phải có gợi ý, đánh dấu là ô đang có cờ. */
    static void testHintFlaggedCells() {
        boolean tested = false, ok = false;
        for (long seed = 0; seed < 50 && !tested; seed++) {
            GameBoardModel b = board(5, 1, seed);
            b.revealCell(2, 2);
            if (25 - b.getRevealedSafeCount() != 1) continue; // chỉ còn đúng 1 ô chưa lật (chính là mìn)
            for (int i = 0; i < 5; i++) for (int j = 0; j < 5; j++) {
                if (!b.getCell(i, j).isRevealed()) b.toggleFlag(i, j);
            }
            HintResult h = HintSystem.getInstance().getHint(b);
            ok = h != null && h.isOnFlaggedCell();
            tested = true;
        }
        check(tested && ok, "mọi ô chưa lật đều có cờ -> vẫn có gợi ý, đánh dấu ô đang có cờ");
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

    /** gỡ cờ ô M rồi lật trúng M: lệnh cờ của M undo không còn tác dụng, phải bị bỏ qua. */
    static void testUndoSkipsNoOps() {
        freshManagers("undo");
        GameManager gm = GameManager.getInstance();
        gm.startGame(DifficultyLevel.EASY);
        GameBoardModel b = gm.getGameBoard();
        gm.reveal(4, 4);
        int mx = -1, my = -1;
        for (int i = 0; i < 9 && mx < 0; i++) for (int j = 0; j < 9; j++) if (b.getCell(i, j).isMine()) { mx = i; my = j; break; }
        gm.toggleFlag(mx, my);
        gm.toggleFlag(mx, my);
        gm.reveal(mx, my); // trúng mìn
        boolean undone = gm.undo();
        check(undone && b.getRevealedSafeCount() == 0 && b.getRevealedMineCount() == 1,
                "undo bỏ qua lệnh vô hiệu, hoàn tác lệnh lật thật sự; mạng đã mất không lấy lại");
        check(!gm.undo(), "hết lệnh để hoàn tác -> false");
    }

    static void testDifficultyConfig() {
        check(DifficultyLevel.EASY.getSize() == 9 && DifficultyLevel.EASY.getMines() == 10
                && DifficultyLevel.MEDIUM.getSize() == 16 && DifficultyLevel.MEDIUM.getMines() == 40
                && DifficultyLevel.HARD.getSize() == 24 && DifficultyLevel.HARD.getMines() == 99,
                "cấu hình EASY/MEDIUM/HARD");
        check(DifficultyLevel.presets().size() == 3 && !DifficultyLevel.presets().contains(DifficultyLevel.CUSTOM),
                "presets() không chứa CUSTOM");
        check(DifficultyLevel.validateCustom(10, 15) == null
                        && DifficultyLevel.validateCustom(4, 1) != null
                        && DifficultyLevel.validateCustom(31, 5) != null
                        && DifficultyLevel.validateCustom(10, 0) != null
                        && DifficultyLevel.validateCustom(5, 17) != null
                        && DifficultyLevel.validateCustom(5, 16) == null,
                "validateCustom: biên kích thước và số mìn");
    }

    static void testSettings() throws Exception {
        Path f = dir.resolve("settings-roundtrip.properties");
        FileSettingsStorage st = new FileSettingsStorage(f.toString());
        Settings s = new Settings();
        s.setTheme(Theme.THEME);
        s.setPlayerName("Nguyễn Văn A");
        s.setDefaultDifficulty(DifficultyLevel.CUSTOM);
        s.setCustom(12, 20);
        s.setSoundEnabled(false);
        st.save(s);
        Settings r = st.load();
        check(r.getTheme() == Theme.THEME && r.getPlayerName().equals("Nguyễn Văn A")
                        && r.getDefaultDifficulty() == DifficultyLevel.CUSTOM
                        && r.getCustomSize() == 12 && r.getCustomMines() == 20 && !r.isSoundEnabled(),
                "settings lưu rồi đọc lại đủ (kể cả tên tiếng Việt)");

        Path bad = dir.resolve("settings-bad.properties");
        Files.write(bad, "theme=XYZ\ncustomSize=abc\nplayerName=\nsoundEnabled=false\n".getBytes(StandardCharsets.UTF_8));
        Settings d = new FileSettingsStorage(bad.toString()).load();
        check(d.getTheme() == Theme.CLASSIC && d.getCustomSize() == 10 && d.getPlayerName().equals("Player")
                        && !d.isSoundEnabled(),
                "giá trị hỏng rơi về mặc định, giá trị tốt vẫn giữ");
        check(new FileSettingsStorage(dir.resolve("none.properties").toString()).load().getTheme() == Theme.CLASSIC,
                "chưa có file -> mặc định");

        Path f2 = dir.resolve("settings-manager.properties");
        SettingsManager.resetInstance(new FileSettingsStorage(f2.toString()));
        SettingsManager sm = SettingsManager.getInstance();
        boolean threw = false;
        try { sm.update(x -> x.setCustom(5, 99)); } catch (IllegalArgumentException e) { threw = true; }
        check(threw && sm.get().getCustomMines() == 15, "update không hợp lệ bị từ chối, cài đặt cũ giữ nguyên");
        threw = false;
        try { sm.update(x -> x.setPlayerName("   ")); } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "tên rỗng bị từ chối");
        sm.update(x -> x.setTheme(Theme.THEME));
        check(new FileSettingsStorage(f2.toString()).load().getTheme() == Theme.THEME, "update được ghi xuống file");
        Settings copy = sm.get();
        copy.setTheme(Theme.CLASSIC);
        check(sm.get().getTheme() == Theme.THEME, "get() trả bản sao, sửa bên ngoài không ảnh hưởng");
    }

    static void testLeaderboard() throws Exception {
        Path f = dir.resolve("lb-test.dat");
        LeaderboardManager.resetInstance(new FileScoreStorage(f.toString()));
        LeaderboardManager lb = LeaderboardManager.getInstance();

        for (int i = 0; i < 12; i++) lb.addScore(DifficultyLevel.MEDIUM, new ScoreRecord("p" + i, 100 + i, 0, false));
        check(lb.getTopScores(DifficultyLevel.MEDIUM).size() == 10
                        && lb.getTopScores(DifficultyLevel.MEDIUM).get(0).getTimeInSeconds() == 100,
                "bảng điểm giữ tối đa 10, sắp xếp đúng");
        check(lb.addScore(DifficultyLevel.MEDIUM, new ScoreRecord("fast", 50, 0, false)) == 1, "addScore trả về hạng 1");
        check(lb.addScore(DifficultyLevel.MEDIUM, new ScoreRecord("slow", 999, 2, false)) == 0, "không lọt top 10 -> hạng 0");
        check(lb.addScore(DifficultyLevel.CUSTOM, new ScoreRecord("c", 1, 0, false)) == 0
                && lb.getTopScores(DifficultyLevel.CUSTOM).isEmpty(), "CUSTOM không ghi bảng điểm");

        lb.addScore(DifficultyLevel.EASY, new ScoreRecord("a", 10, 1, false));
        lb.addScore(DifficultyLevel.EASY, new ScoreRecord("b", 300, 0, false));
        check(lb.getTopScores(DifficultyLevel.EASY).get(0).getPlayerName().equals("b"),
                "ít mìn nổ hơn xếp trên dù chậm hơn");
        check(lb.getTopScores(DifficultyLevel.EASY).get(0).getTimestamp() > 0, "bản ghi có ngày giờ");

        ScoreRecord r = new ScoreRecord("old", 1, 0, false);
        lb.addScore(DifficultyLevel.HARD, r);
        ScoreRecord renamed = lb.renameScore(DifficultyLevel.HARD, r, "  Bob ");
        check(renamed != null && lb.getTopScores(DifficultyLevel.HARD).get(0).getPlayerName().equals("Bob"),
                "renameScore đổi tên (đã trim)");
        boolean threw = false;
        try { lb.renameScore(DifficultyLevel.HARD, renamed, " "); } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "renameScore tên rỗng bị từ chối");
        check(lb.renameScore(DifficultyLevel.HARD, r, "X") == null, "bản ghi không còn trong bảng -> null");

        LeaderboardManager.resetInstance(new FileScoreStorage(f.toString()));
        check(LeaderboardManager.getInstance().getTopScores(DifficultyLevel.HARD).get(0).getPlayerName().equals("Bob"),
                "bảng điểm được lưu và đọc lại từ file");
        LeaderboardManager.getInstance().clear(DifficultyLevel.HARD);
        check(LeaderboardManager.getInstance().getTopScores(DifficultyLevel.HARD).isEmpty(), "clear(level)");
        LeaderboardManager.getInstance().clearAll();
        check(LeaderboardManager.getInstance().getTopScores(DifficultyLevel.MEDIUM).isEmpty(), "clearAll()");

        Path corrupt = dir.resolve("lb-corrupt.dat");
        Files.write(corrupt, "không phải dữ liệu hợp lệ".getBytes(StandardCharsets.UTF_8));
        check(new FileScoreStorage(corrupt.toString()).loadScores().isEmpty()
                        && Files.exists(dir.resolve("lb-corrupt.dat.corrupt")),
                "file điểm hỏng -> bảng trống và giữ bản sao .corrupt");
    }

    static void testMenuFlow() {
        freshManagers("menu");
        GameManager gm = GameManager.getInstance();
        gm.exitToMenu();
        check(gm.getGameStatus() == GameStatus.NOT_STARTED && gm.getGameBoard() == null,
                "exitToMenu -> NOT_STARTED, không còn bàn cờ");
        gm.reveal(0, 0); // ở menu: bỏ qua, không lỗi
        check(gm.getGameBoard() == null, "ở menu thì reveal bị bỏ qua");

        gm.startGame(DifficultyLevel.EASY);
        gm.reveal(4, 4);
        gm.exitToMenu();
        check(gm.getGameStatus() == GameStatus.NOT_STARTED && !gm.undo(), "bỏ ván giữa chừng, undo không tác động");

        gm.startGame(DifficultyLevel.CUSTOM, 6, 5);
        gm.reveal(2, 2);
        gm.restart();
        check(gm.getBoardSize() == 6 && gm.getMineCount() == 5 && !gm.getGameBoard().isMinesGenerated()
                        && gm.getGameStatus() == GameStatus.PLAYING,
                "restart giữ cấu hình, bàn cờ mới");
        gm.exitToMenu();
        gm.restart();
        check(gm.getGameStatus() == GameStatus.PLAYING && gm.getBoardSize() == 6, "restart được cả sau khi về menu");

        boolean threw = false;
        try { gm.startGame(DifficultyLevel.CUSTOM); } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "startGame(CUSTOM) thiếu tham số bị từ chối");

        SettingsManager.getInstance().update(s -> { s.setDefaultDifficulty(DifficultyLevel.MEDIUM); s.setTheme(Theme.THEME); });
        gm.startConfiguredGame();
        check(gm.getBoardSize() == 16 && gm.getMineCount() == 40
                        && gm.getGameBoard().getCell(0, 0).getMineSymbol().equals("@"),
                "Play theo Settings: độ khó và giao diện");
        SettingsManager.getInstance().update(s -> { s.setDefaultDifficulty(DifficultyLevel.CUSTOM); s.setCustom(7, 8); });
        gm.startConfiguredGame();
        check(gm.getBoardSize() == 7 && gm.getMineCount() == 8, "Play với CUSTOM lấy size/mines từ Settings");
        gm.exitToMenu();
    }

    static void testHintPreClick() {
        freshManagers("hint");
        GameManager gm = GameManager.getInstance();
        gm.startGame(DifficultyLevel.EASY);
        HintResult h = gm.useHint();
        check(h != null && !gm.isHintUsed(), "gợi ý trước click đầu không bị tính là đã dùng gợi ý");
        gm.toggleFlag(4, 4);
        HintResult flagged = gm.useHint();
        check(flagged != null && flagged.isOnFlaggedCell(), "gợi ý trỏ vào ô đang cắm cờ thì được đánh dấu");
        gm.toggleFlag(4, 4);
        gm.reveal(4, 4);
        check(gm.useHint() != null && gm.isHintUsed(), "gợi ý sau click đầu được tính");
        gm.exitToMenu();
    }

    static void testTimerFreezesWhenPaused() {
        freshManagers("timer");
        GameManager gm = GameManager.getInstance();
        long[] now = {0};
        gm.exitToMenu();
        gm.setClock(() -> now[0]);
        gm.startGame(DifficultyLevel.EASY);
        now[0] = 4_000_000_000L;
        check(gm.getElapsedSeconds() == 0, "timer chưa chạy trước click đầu");
        gm.reveal(4, 4);
        now[0] += 5_000_000_000L;
        check(gm.getElapsedSeconds() == 5, "timer chạy sau click đầu");
        gm.pause();
        now[0] += 10_000_000_000L;
        check(gm.getElapsedSeconds() == 5, "tạm dừng đóng băng timer");
        gm.resume();
        now[0] += 3_000_000_000L;
        check(gm.getElapsedSeconds() == 8, "tiếp tục thì timer chạy tiếp");
        gm.exitToMenu();
        gm.setClock(System::nanoTime);
    }

    static void testGameFlow() {
        freshManagers("flow");
        GameManager gm = GameManager.getInstance();

        // Thắng + ghi bảng điểm
        gm.startGame(DifficultyLevel.EASY);
        check(gm.getGameStatus() == GameStatus.PLAYING, "startGame -> PLAYING");
        GameBoardModel b = gm.getGameBoard();
        gm.handleCellAction(new RevealCommand(b, 4, 4));

        gm.pause();
        int before = b.getRevealedSafeCount();
        gm.handleCellAction(new RevealCommand(b, 0, 0));
        check(gm.getGameStatus() == GameStatus.PAUSED && b.getRevealedSafeCount() == before, "PAUSED bỏ qua lệnh");
        gm.resume();
        check(gm.getGameStatus() == GameStatus.PLAYING, "resume -> PLAYING");

        for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) {
            if (!b.getCell(i, j).isMine()) gm.handleCellAction(new RevealCommand(b, i, j));
        }
        check(gm.getGameStatus() == GameStatus.WON, "lật hết ô an toàn -> WON");
        check(LeaderboardManager.getInstance().getTopScores(DifficultyLevel.EASY).size() == 1, "thắng -> ghi bảng điểm");
        check(gm.getLastRank() == 1 && gm.getLastRecord() != null, "biết hạng của ván vừa thắng");
        check(gm.renameLastScore("Tester")
                        && LeaderboardManager.getInstance().getTopScores(DifficultyLevel.EASY).get(0).getPlayerName().equals("Tester"),
                "đổi tên bản ghi vừa thắng");
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
        check(gm.getGameStatus() == GameStatus.LOST, "trúng 3 mìn -> LOST");
        check(LeaderboardManager.getInstance().getTopScores(DifficultyLevel.EASY).size() == 1, "thua -> không ghi điểm");

        boolean threw = false;
        try { gm.startGame(DifficultyLevel.CUSTOM, 5, 20); } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "CUSTOM không hợp lệ bị từ chối");
        check(gm.getGameStatus() == GameStatus.LOST, "startGame lỗi không làm hỏng ván hiện tại");

        // CUSTOM thắng không ghi điểm
        gm.startGame(DifficultyLevel.CUSTOM, 5, 3);
        GameBoardModel cb = gm.getGameBoard();
        gm.reveal(2, 2);
        for (int i = 0; i < 5; i++) for (int j = 0; j < 5; j++) if (!cb.getCell(i, j).isMine()) gm.reveal(i, j);
        check(gm.getGameStatus() == GameStatus.WON && gm.getLastRank() == 0
                        && LeaderboardManager.getInstance().getTopScores(DifficultyLevel.EASY).size() == 1,
                "thắng CUSTOM -> không ghi bảng điểm");
        gm.exitToMenu();
    }

    static void testEventsAndSnapshot() {
        freshManagers("events");
        GameManager gm = GameManager.getInstance();
        List<GameEvent> events = new ArrayList<>();
        IGameEventListener l = (e, m) -> events.add(e);
        gm.registerEventListener(l);

        gm.startGame(DifficultyLevel.EASY);
        gm.reveal(4, 4);
        GameSnapshot snap = gm.getSnapshot();
        boolean leak = false;
        for (String row : snap.rows) if (row.indexOf('*') >= 0) leak = true;
        check(snap.inGame && snap.rows.size() == 9 && snap.rows.get(0).length() == 9 && !leak,
                "snapshot đang chơi: đủ lưới, không lộ mìn");
        check(snap.toJson().contains("\"status\":\"PLAYING\"") && snap.toJson().contains("\"rows\":["),
                "snapshot.toJson");

        gm.pause();
        check(gm.getSnapshot().rows.isEmpty() && events.contains(GameEvent.PAUSED), "tạm dừng: giấu bàn cờ + sự kiện PAUSED");
        gm.resume();
        check(events.contains(GameEvent.RESUMED), "sự kiện RESUMED");

        GameBoardModel b = gm.getGameBoard();
        int hits = 0;
        for (int i = 0; i < 9 && hits < 3; i++) for (int j = 0; j < 9 && hits < 3; j++) {
            if (b.getCell(i, j).isMine()) { gm.reveal(i, j); hits++; }
        }
        check(events.contains(GameEvent.GAME_STARTED) && events.contains(GameEvent.LIFE_LOST)
                        && events.contains(GameEvent.LOST),
                "sự kiện GAME_STARTED / LIFE_LOST / LOST");

        GameSnapshot lost = gm.getSnapshot();
        int stars = 0;
        for (String row : lost.rows) for (char c : row.toCharArray()) if (c == '*') stars++;
        check(lost.status == GameStatus.LOST && stars == 10, "sau khi thua snapshot hiện toàn bộ 10 mìn");

        gm.removeEventListener(l);
        gm.exitToMenu();
        GameSnapshot menu = gm.getSnapshot();
        check(!menu.inGame && menu.rows.isEmpty() && menu.status == GameStatus.NOT_STARTED
                        && menu.toJson().contains("\"inGame\":false"),
                "snapshot ở menu");
        check(!events.contains(GameEvent.RETURNED_TO_MENU), "listener đã gỡ thì không nhận sự kiện nữa");
    }
}
