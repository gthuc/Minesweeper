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

/**
 * Runs regression checks without JUnit using seeded boards and temporary storage.
 * The suite covers board invariants, hints, undo, persistence, lifecycle, and snapshots.
 */
public class GameTests {
    static int passed, failed;
    static Path dir;

    /**
     * Records an assertion result and prints its descriptive label.
     */
    static void check(boolean ok, String name) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "PASS " : "FAIL ") + name);
    }

    /**
     * Creates a board with a reproducible random sequence.
     */
    static GameBoardModel board(int size, int mines, long seed) {
        GameBoardModel b = new GameBoardModel(new StandardBoardFactory(), new Random(seed));
        b.initialize(size, mines);
        return b;
    }

    /**
     * Isolates settings and scores in distinct files for each test group.
     */
    static void freshManagers(String tag) {
        LeaderboardManager.resetInstance(new FileScoreStorage(dir.resolve(tag + "-scores.dat").toString()));
        SettingsManager.resetInstance(new FileSettingsStorage(dir.resolve(tag + "-settings.properties").toString()));
    }

    /**
     * Runs all regression groups using temporary files rather than player data.
     */
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
        check(ok, "first click and its eight neighbors are mine-free; total mine count is correct");
    }

    static void testCountersAndUndo() {
        GameBoardModel b = board(9, 10, 1);
        List<int[]> first = b.revealCell(4, 4);
        check(b.getRevealedSafeCount() == first.size() && first.size() > 1, "flood fill reveals multiple cells and updates the counter");
        check(b.undoReveal(first), "undoReveal reports a change");
        boolean allHidden = true;
        for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) allHidden &= !b.getCell(i, j).isRevealed();
        check(b.getRevealedSafeCount() == 0 && allHidden, "undoReveal restores cells and the counter");
        check(!b.undoReveal(first), "a second undoReveal makes no changes");

        for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) if (!b.getCell(i, j).isMine()) b.revealCell(i, j);
        check(b.getRevealedSafeCount() == 71 && b.getRevealedMineCount() == 0, "revealing all safe cells reveals 71 cells");
    }

    static void testFlagLimit() {
        GameBoardModel b = board(9, 10, 2);
        boolean ok = true;
        for (int i = 0; i < 10; i++) ok &= b.toggleFlag(i / 9, i % 9);
        check(ok && b.getFlagCount() == 10, "all 10 flags can be placed");
        check(!b.toggleFlag(5, 5), "flag count cannot exceed mine count");
        check(b.toggleFlag(0, 0) && b.getFlagCount() == 9, "removing a flag decreases the counter");
        check(!board(9, 10, 3).toggleFlag(-1, 0) && !new FlagCommand(b, 9, 9).execute(), "out-of-bounds coordinates are ignored");
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
        check(flags == 8, "after hitting two mines, at most eight flags can be placed");
        check(b.getRemainingMines() == 0, "remaining mine count is never negative");
    }

    /**
     * Checks certain deductions across seeded games, including incorrect player flags.
     */
    static void testHintsAreSound() {
        int violations = 0, hintsChecked = 0;
        for (long seed = 0; seed < 300; seed++) {
            GameBoardModel b = board(16, 40, seed);
            b.revealCell(8, 8);

            if (seed % 2 == 1) {
                outer:
                for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
                    if (b.getCell(i, j).isMine() && !b.getCell(i, j).isRevealed()) { b.revealCell(i, j); break outer; }
                }
            }
            if (seed % 3 == 0) {
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
        System.out.println("   (checked " + hintsChecked + " certain hints)");
        check(hintsChecked > 0 && violations == 0, "CERTAIN_* hints are always correct, even after mine hits or incorrect flags");

        GameBoardModel fresh = board(9, 10, 5);
        HintResult first = HintSystem.getInstance().getHint(fresh);
        check(first != null && first.getType() == HintType.CERTAIN_SAFE, "before the first click, a safe cell is suggested");
    }

    static void testHintFlaggedCells() {
        boolean tested = false, ok = false;
        for (long seed = 0; seed < 50 && !tested; seed++) {
            GameBoardModel b = board(5, 1, seed);
            b.revealCell(2, 2);
            if (25 - b.getRevealedSafeCount() != 1) continue;
            for (int i = 0; i < 5; i++) for (int j = 0; j < 5; j++) {
                if (!b.getCell(i, j).isRevealed()) b.toggleFlag(i, j);
            }
            HintResult h = HintSystem.getInstance().getHint(b);
            ok = h != null && h.isOnFlaggedCell();
            tested = true;
        }
        check(tested && ok, "hints remain available when all hidden cells are flagged and identify flagged cells");
    }

    static void testMineNotUndoable() {
        GameBoardModel b = board(9, 10, 7);
        b.revealCell(4, 4);
        int mx = -1, my = -1;
        for (int i = 0; i < 9 && mx < 0; i++) for (int j = 0; j < 9; j++) if (b.getCell(i, j).isMine()) { mx = i; my = j; break; }
        RevealCommand mine = new RevealCommand(b, mx, my);
        mine.execute();
        check(!mine.isUndoable(), "revealing a mine cannot be undone");
    }

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
        gm.reveal(mx, my);
        boolean undone = gm.undo();
        check(undone && b.getRevealedSafeCount() == 0 && b.getRevealedMineCount() == 1,
                "undo skips ineffective commands and reverses a reveal without restoring lost lives");
        check(!gm.undo(), "undo returns false when no commands remain");
    }

    static void testDifficultyConfig() {
        check(DifficultyLevel.EASY.getSize() == 9 && DifficultyLevel.EASY.getMines() == 10
                && DifficultyLevel.MEDIUM.getSize() == 16 && DifficultyLevel.MEDIUM.getMines() == 40
                && DifficultyLevel.HARD.getSize() == 24 && DifficultyLevel.HARD.getMines() == 99,
                "EASY/MEDIUM/HARD configuration");
        check(DifficultyLevel.presets().size() == 3 && !DifficultyLevel.presets().contains(DifficultyLevel.CUSTOM),
                "presets() excludes CUSTOM");
        check(DifficultyLevel.validateCustom(10, 15) == null
                        && DifficultyLevel.validateCustom(4, 1) != null
                        && DifficultyLevel.validateCustom(31, 5) != null
                        && DifficultyLevel.validateCustom(10, 0) != null
                        && DifficultyLevel.validateCustom(5, 17) != null
                        && DifficultyLevel.validateCustom(5, 16) == null,
                "validateCustom checks board size and mine count boundaries");
    }

    static void testSettings() throws Exception {
        Path f = dir.resolve("settings-roundtrip.properties");
        FileSettingsStorage st = new FileSettingsStorage(f.toString());
        Settings s = new Settings();
        s.setTheme(Theme.THEME);
        s.setPlayerName("Jos\u00e9");
        s.setDefaultDifficulty(DifficultyLevel.CUSTOM);
        s.setCustom(12, 20);
        s.setSoundEnabled(false);
        st.save(s);
        Settings r = st.load();
        check(r.getTheme() == Theme.THEME && r.getPlayerName().equals("Jos\u00e9")
                        && r.getDefaultDifficulty() == DifficultyLevel.CUSTOM
                        && r.getCustomSize() == 12 && r.getCustomMines() == 20 && !r.isSoundEnabled(),
                "settings round-trip preserves all values, including Unicode player names");

        Path bad = dir.resolve("settings-bad.properties");
        Files.write(bad, "theme=XYZ\ncustomSize=abc\nplayerName=\nsoundEnabled=false\n".getBytes(StandardCharsets.UTF_8));
        Settings d = new FileSettingsStorage(bad.toString()).load();
        check(d.getTheme() == Theme.CLASSIC && d.getCustomSize() == 10 && d.getPlayerName().equals("Player")
                        && !d.isSoundEnabled(),
                "invalid values fall back to defaults while valid values are preserved");
        check(new FileSettingsStorage(dir.resolve("none.properties").toString()).load().getTheme() == Theme.CLASSIC,
                "missing settings file uses defaults");

        Path f2 = dir.resolve("settings-manager.properties");
        SettingsManager.resetInstance(new FileSettingsStorage(f2.toString()));
        SettingsManager sm = SettingsManager.getInstance();
        boolean threw = false;
        try { sm.update(x -> x.setCustom(5, 99)); } catch (IllegalArgumentException e) { threw = true; }
        check(threw && sm.get().getCustomMines() == 15, "invalid updates are rejected and previous settings are preserved");
        threw = false;
        try { sm.update(x -> x.setPlayerName("   ")); } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "blank player names are rejected");
        sm.update(x -> x.setTheme(Theme.THEME));
        check(new FileSettingsStorage(f2.toString()).load().getTheme() == Theme.THEME, "settings updates are written to disk");
        Settings copy = sm.get();
        copy.setTheme(Theme.CLASSIC);
        check(sm.get().getTheme() == Theme.THEME, "get() returns a copy that cannot modify stored settings");
    }

    static void testLeaderboard() throws Exception {
        Path f = dir.resolve("lb-test.dat");
        LeaderboardManager.resetInstance(new FileScoreStorage(f.toString()));
        LeaderboardManager lb = LeaderboardManager.getInstance();

        for (int i = 0; i < 12; i++) lb.addScore(DifficultyLevel.MEDIUM, new ScoreRecord("p" + i, 100 + i, 0, false));
        check(lb.getTopScores(DifficultyLevel.MEDIUM).size() == 10
                        && lb.getTopScores(DifficultyLevel.MEDIUM).get(0).getTimeInSeconds() == 100,
                "leaderboard keeps at most 10 entries in the correct order");
        check(lb.addScore(DifficultyLevel.MEDIUM, new ScoreRecord("fast", 50, 0, false)) == 1, "addScore returns rank 1 for the top score");
        check(lb.addScore(DifficultyLevel.MEDIUM, new ScoreRecord("slow", 999, 2, false)) == 0, "scores outside the top 10 return rank 0");
        check(lb.addScore(DifficultyLevel.CUSTOM, new ScoreRecord("c", 1, 0, false)) == 0
                && lb.getTopScores(DifficultyLevel.CUSTOM).isEmpty(), "CUSTOM scores are not recorded");

        lb.addScore(DifficultyLevel.EASY, new ScoreRecord("a", 10, 1, false));
        lb.addScore(DifficultyLevel.EASY, new ScoreRecord("b", 300, 0, false));
        check(lb.getTopScores(DifficultyLevel.EASY).get(0).getPlayerName().equals("b"),
                "fewer mine hits rank higher even with a slower time");
        check(lb.getTopScores(DifficultyLevel.EASY).get(0).getTimestamp() > 0, "score records have a timestamp");

        ScoreRecord r = new ScoreRecord("old", 1, 0, false);
        lb.addScore(DifficultyLevel.HARD, r);
        ScoreRecord renamed = lb.renameScore(DifficultyLevel.HARD, r, "  Bob ");
        check(renamed != null && lb.getTopScores(DifficultyLevel.HARD).get(0).getPlayerName().equals("Bob"),
                "renameScore trims and updates the player name");
        boolean threw = false;
        try { lb.renameScore(DifficultyLevel.HARD, renamed, " "); } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "renameScore rejects blank names");
        check(lb.renameScore(DifficultyLevel.HARD, r, "X") == null, "renameScore returns null for a record no longer on the leaderboard");

        LeaderboardManager.resetInstance(new FileScoreStorage(f.toString()));
        check(LeaderboardManager.getInstance().getTopScores(DifficultyLevel.HARD).get(0).getPlayerName().equals("Bob"),
                "leaderboard can be saved and loaded");
        LeaderboardManager.getInstance().clear(DifficultyLevel.HARD);
        check(LeaderboardManager.getInstance().getTopScores(DifficultyLevel.HARD).isEmpty(), "clear(level)");
        LeaderboardManager.getInstance().clearAll();
        check(LeaderboardManager.getInstance().getTopScores(DifficultyLevel.MEDIUM).isEmpty(), "clearAll()");

        Path corrupt = dir.resolve("lb-corrupt.dat");
        Files.write(corrupt, "invalid score data".getBytes(StandardCharsets.UTF_8));
        check(new FileScoreStorage(corrupt.toString()).loadScores().isEmpty()
                        && Files.exists(dir.resolve("lb-corrupt.dat.corrupt")),
                "corrupt score files produce an empty leaderboard and a .corrupt backup");
    }

    static void testMenuFlow() {
        freshManagers("menu");
        GameManager gm = GameManager.getInstance();
        gm.exitToMenu();
        check(gm.getGameStatus() == GameStatus.NOT_STARTED && gm.getGameBoard() == null,
                "exitToMenu sets NOT_STARTED and removes the board");
        gm.reveal(0, 0);
        check(gm.getGameBoard() == null, "reveal is ignored while in the menu");

        gm.startGame(DifficultyLevel.EASY);
        gm.reveal(4, 4);
        gm.exitToMenu();
        check(gm.getGameStatus() == GameStatus.NOT_STARTED && !gm.undo(), "abandoning a game prevents undo");

        gm.startGame(DifficultyLevel.CUSTOM, 6, 5);
        gm.reveal(2, 2);
        gm.restart();
        check(gm.getBoardSize() == 6 && gm.getMineCount() == 5 && !gm.getGameBoard().isMinesGenerated()
                        && gm.getGameStatus() == GameStatus.PLAYING,
                "restart preserves configuration and creates a new board");
        gm.exitToMenu();
        gm.restart();
        check(gm.getGameStatus() == GameStatus.PLAYING && gm.getBoardSize() == 6, "restart also works after returning to the menu");

        boolean threw = false;
        try { gm.startGame(DifficultyLevel.CUSTOM); } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "startGame(CUSTOM) without parameters is rejected");

        SettingsManager.getInstance().update(s -> { s.setDefaultDifficulty(DifficultyLevel.MEDIUM); s.setTheme(Theme.THEME); });
        gm.startConfiguredGame();
        check(gm.getBoardSize() == 16 && gm.getMineCount() == 40
                        && gm.getGameBoard().getCell(0, 0).getMineSymbol().equals("@"),
                "configured games use the difficulty and theme from settings");
        SettingsManager.getInstance().update(s -> { s.setDefaultDifficulty(DifficultyLevel.CUSTOM); s.setCustom(7, 8); });
        gm.startConfiguredGame();
        check(gm.getBoardSize() == 7 && gm.getMineCount() == 8, "CUSTOM games use the board size and mine count from settings");
        gm.exitToMenu();
    }

    static void testHintPreClick() {
        freshManagers("hint");
        GameManager gm = GameManager.getInstance();
        gm.startGame(DifficultyLevel.EASY);
        HintResult h = gm.useHint();
        check(h != null && !gm.isHintUsed(), "hints before the first click do not mark hints as used");
        gm.toggleFlag(4, 4);
        HintResult flagged = gm.useHint();
        check(flagged != null && flagged.isOnFlaggedCell(), "hints targeting flagged cells are marked accordingly");
        gm.toggleFlag(4, 4);
        gm.reveal(4, 4);
        check(gm.useHint() != null && gm.isHintUsed(), "hints after the first click are counted as used");
        gm.exitToMenu();
    }

    /**
     * Advances an injected clock to verify active time without real waiting.
     */
    static void testTimerFreezesWhenPaused() {
        freshManagers("timer");
        GameManager gm = GameManager.getInstance();
        long[] now = {0};
        gm.exitToMenu();
        gm.setClock(() -> now[0]);
        gm.startGame(DifficultyLevel.EASY);
        now[0] = 4_000_000_000L;
        check(gm.getElapsedSeconds() == 0, "timer does not run before the first click");
        gm.reveal(4, 4);
        now[0] += 5_000_000_000L;
        check(gm.getElapsedSeconds() == 5, "timer starts after the first click");
        gm.pause();
        now[0] += 10_000_000_000L;
        check(gm.getElapsedSeconds() == 5, "pausing freezes the timer");
        gm.resume();
        now[0] += 3_000_000_000L;
        check(gm.getElapsedSeconds() == 8, "resuming continues the timer");
        gm.exitToMenu();
        gm.setClock(System::nanoTime);
    }

    static void testGameFlow() {
        freshManagers("flow");
        GameManager gm = GameManager.getInstance();

        gm.startGame(DifficultyLevel.EASY);
        check(gm.getGameStatus() == GameStatus.PLAYING, "startGame -> PLAYING");
        GameBoardModel b = gm.getGameBoard();
        gm.handleCellAction(new RevealCommand(b, 4, 4));

        gm.pause();
        int before = b.getRevealedSafeCount();
        gm.handleCellAction(new RevealCommand(b, 0, 0));
        check(gm.getGameStatus() == GameStatus.PAUSED && b.getRevealedSafeCount() == before, "commands are ignored while PAUSED");
        gm.resume();
        check(gm.getGameStatus() == GameStatus.PLAYING, "resume -> PLAYING");

        for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) {
            if (!b.getCell(i, j).isMine()) gm.handleCellAction(new RevealCommand(b, i, j));
        }
        check(gm.getGameStatus() == GameStatus.WON, "revealing all safe cells sets WON");
        check(LeaderboardManager.getInstance().getTopScores(DifficultyLevel.EASY).size() == 1, "winning records a leaderboard entry");
        check(gm.getLastRank() == 1 && gm.getLastRecord() != null, "the rank of the winning game is available");
        check(gm.renameLastScore("Tester")
                        && LeaderboardManager.getInstance().getTopScores(DifficultyLevel.EASY).get(0).getPlayerName().equals("Tester"),
                "the winning score record can be renamed");
        check(gm.useHint() == null, "hints are unavailable after the game ends");

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
                    check(b.getRevealedMineCount() == 1, "undo does not restore lost lives");
                }
            }
        }
        check(gm.getGameStatus() == GameStatus.LOST, "hitting three mines sets LOST");
        check(LeaderboardManager.getInstance().getTopScores(DifficultyLevel.EASY).size() == 1, "losing does not record a score");

        boolean threw = false;
        try { gm.startGame(DifficultyLevel.CUSTOM, 5, 20); } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "invalid CUSTOM configuration is rejected");
        check(gm.getGameStatus() == GameStatus.LOST, "a failed startGame preserves the current game");

        gm.startGame(DifficultyLevel.CUSTOM, 5, 3);
        GameBoardModel cb = gm.getGameBoard();
        gm.reveal(2, 2);
        for (int i = 0; i < 5; i++) for (int j = 0; j < 5; j++) if (!cb.getCell(i, j).isMine()) gm.reveal(i, j);
        check(gm.getGameStatus() == GameStatus.WON && gm.getLastRank() == 0
                        && LeaderboardManager.getInstance().getTopScores(DifficultyLevel.EASY).size() == 1,
                "winning a CUSTOM game does not record a score");
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
                "an active snapshot contains the full grid without exposing mines");
        check(snap.toJson().contains("\"status\":\"PLAYING\"") && snap.toJson().contains("\"rows\":["),
                "snapshot.toJson");

        gm.pause();
        check(gm.getSnapshot().rows.isEmpty() && events.contains(GameEvent.PAUSED), "pausing hides the board and emits PAUSED");
        gm.resume();
        check(events.contains(GameEvent.RESUMED), "RESUMED event is emitted");

        GameBoardModel b = gm.getGameBoard();
        int hits = 0;
        for (int i = 0; i < 9 && hits < 3; i++) for (int j = 0; j < 9 && hits < 3; j++) {
            if (b.getCell(i, j).isMine()) { gm.reveal(i, j); hits++; }
        }
        check(events.contains(GameEvent.GAME_STARTED) && events.contains(GameEvent.LIFE_LOST)
                        && events.contains(GameEvent.LOST),
                "GAME_STARTED, LIFE_LOST, and LOST events are emitted");

        GameSnapshot lost = gm.getSnapshot();
        int stars = 0;
        for (String row : lost.rows) for (char c : row.toCharArray()) if (c == '*') stars++;
        check(lost.status == GameStatus.LOST && stars == 10, "the snapshot exposes all 10 mines after losing");

        gm.removeEventListener(l);
        gm.exitToMenu();
        GameSnapshot menu = gm.getSnapshot();
        check(!menu.inGame && menu.rows.isEmpty() && menu.status == GameStatus.NOT_STARTED
                        && menu.toJson().contains("\"inGame\":false"),
                "menu snapshot");
        check(!events.contains(GameEvent.RETURNED_TO_MENU), "removed listeners no longer receive events");
    }
}
