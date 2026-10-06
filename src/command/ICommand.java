package command;

public interface ICommand {
    /** @return true nếu lệnh thực sự thay đổi bàn cờ. */
    boolean execute();

    void undo();

    /** Lệnh không undo được (vd: lật trúng mìn) sẽ không được đưa vào history. */
    default boolean isUndoable() {
        return true;
    }
}
