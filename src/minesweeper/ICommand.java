package minesweeper;

public interface ICommand {
    /** @return true nếu lệnh thực sự thay đổi bàn cờ (mới được đưa vào history). */
    boolean Execute();
    void Undo();
}