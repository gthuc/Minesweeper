# Minesweeper (Java)

Command-line Minesweeper using the Singleton, Command, State, Strategy, Observer, and Factory patterns.

## Project structure

- `src/model` - Cells and board (flood fill, mine generation after the first click, O(1) counters)
- `src/command` - Reveal/Flag and Undo (revealing a mine **cannot** be undone)
- `src/state` - Playing / Paused / Inactive (Not started, Won, Lost)
- `src/hint` - World-based hints (`WorldHintStrategy`): Safe (green) or Mine (red), closest to opened cells; ties prefer more opened neighbors, then Safe. Flagged cells are skipped.
- `src/storage` - Leaderboard stored in `data/leaderboard.dat` (the directory is created automatically when saving)
- `src/view` - `ConsoleView` (Observer)
- `src/controller` - `GameManager` (timer, three-life rule, score recording) and `Main`
- `test/GameTests.java` - Tests without JUnit

## Run the game

```bash
javac -encoding UTF-8 -d out -sourcepath src src/controller/Main.java
java -cp out controller.Main
```

On Windows, run `chcp 65001` first if Unicode text is displayed incorrectly.

Game commands: `r <row> <col>` reveal, `f <row> <col>` flag, `u` undo, `h` hint,
`p` pause/resume, `n [easy|medium|hard|custom <size> <mines>]` new game,
`t` switch theme, `name <name>`, `l` leaderboard, `q` quit.

## Run the tests

```bash
javac -encoding UTF-8 -d out -sourcepath src test/GameTests.java
java -cp out GameTests
```

## Rules

- Three lives: hitting a third mine ends the game; reveal all safe cells to win.
- Only wins on EASY / MEDIUM / HARD are recorded on the leaderboard (CUSTOM games are not comparable).
- Ranking: fewer mine hits, then shorter time, then no hints used.
