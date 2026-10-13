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

    /**
     * Hoàn tác lệnh gần nhất THỰC SỰ có tác dụng. Lệnh nào undo mà không đổi gì
     * (vd: ô đó sau đó đã nổ mìn) bị bỏ qua để người chơi không bấm undo "vô hình".
     *
     * @return true nếu có thay đổi nào được hoàn tác.
     */
    public synchronized boolean undo() {
        while (!history.isEmpty()) {
            if (history.pop().undo()) return true;
        }
        return false;
    }

    /** Gọi khi bắt đầu ván mới / về menu để Undo không tác động lên bàn cờ cũ. */
    public synchronized void clear() {
        history.clear();
    }
}
