# Minesweeper (Java)

Minesweeper dòng lệnh, dùng các pattern: Singleton, Command, State, Strategy, Observer, Factory.

## Cấu trúc

- `src/model` – ô, bàn cờ (flood fill, sinh mìn sau click đầu, bộ đếm O(1))
- `src/command` – Reveal/Flag + Undo (lật trúng mìn **không** undo được)
- `src/state` – Playing / Paused / Inactive (Not started, Won, Lost)
- `src/hint` – gợi ý chắc chắn (`SafeHintStrategy`) và ước lượng xác suất (`IntelligentHintStrategy`)
- `src/storage` – bảng điểm lưu file `leaderboard.dat`
- `src/view` – `ConsoleView` (Observer)
- `src/controller` – `GameManager` (timer, luật 3 mạng, ghi điểm) và `Main`
- `test/GameTests.java` – test không cần JUnit

## Chạy game

```bash
javac -encoding UTF-8 -d out -sourcepath src src/controller/Main.java
java -cp out controller.Main
```

Trên Windows nếu tiếng Việt bị lỗi font: `chcp 65001` trước khi chạy.

Lệnh trong game: `r <hàng> <cột>` lật, `f <hàng> <cột>` cờ, `u` undo, `h` gợi ý,
`p` tạm dừng/tiếp tục, `n [easy|medium|hard|custom <size> <mines>]` ván mới,
`t` đổi giao diện, `name <tên>`, `l` bảng điểm, `q` thoát.

## Chạy test

```bash
javac -encoding UTF-8 -d out -sourcepath src test/GameTests.java
java -cp out GameTests
```

## Luật

- 3 mạng: lật trúng mìn thứ 3 là thua; thắng khi lật hết ô an toàn.
- Bảng điểm chỉ ghi khi thắng ở EASY / MEDIUM / HARD (CUSTOM không so sánh được).
- Xếp hạng: ít mìn nổ hơn → thời gian ngắn hơn → không dùng gợi ý.
