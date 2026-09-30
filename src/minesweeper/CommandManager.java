package minesweeper;
import java.util.Stack;

public class CommandManager {
    private static CommandManager instance;
    private final Stack<ICommand> commandHistory = new Stack<>();

    private CommandManager() {}

    public static CommandManager Instance() {
        if (instance == null) {
            instance = new CommandManager();
        }
        return instance;
    }

    public void ExecuteCommand(ICommand command) {
        // Chỉ lưu vào history nếu lệnh thực sự có tác dụng
        if (command.Execute()) {
            commandHistory.push(command);
        }
    }

    public void Undo() {
        if (!commandHistory.isEmpty()) {
            commandHistory.pop().Undo();
        }
    }

    /** Gọi khi bắt đầu ván mới để Undo không tác động lên bàn cờ cũ. */
    public void Clear() {
        commandHistory.clear();
    }
}