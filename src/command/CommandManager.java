package command;

import java.util.ArrayDeque;
import java.util.Deque;

public class CommandManager {
    private static CommandManager instance;
    private final Deque<ICommand> history = new ArrayDeque<>();

    private CommandManager() {}

    public static synchronized CommandManager getInstance() {
        if (instance == null) {
            instance = new CommandManager();
        }
        return instance;
    }

    public synchronized void executeCommand(ICommand command) {
        // Chỉ lưu vào history nếu lệnh có tác dụng VÀ undo được
        if (command.execute() && command.isUndoable()) {
            history.push(command);
        }
    }

    /** @return true nếu có lệnh được hoàn tác. */
    public synchronized boolean undo() {
        if (history.isEmpty()) return false;
        history.pop().undo();
        return true;
    }

    /** Gọi khi bắt đầu ván mới để Undo không tác động lên bàn cờ cũ. */
    public synchronized void clear() {
        history.clear();
    }
}
