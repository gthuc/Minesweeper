package minesweeper;
import java.util.Stack;

public class CommandManager {
    private static CommandManager instance;
    private Stack<ICommand> commandHistory;

    private CommandManager() {
        commandHistory = new Stack<>();
    }

    public static CommandManager Instance() {
        if (instance == null) {
            instance = new CommandManager();
        }
        return instance;
    }

    public void ExecuteCommand(ICommand command) {
        command.Execute();
        commandHistory.push(command);
    }

    public void Undo() {
        if (!commandHistory.isEmpty()) {
            ICommand command = commandHistory.pop();
            command.Undo();
        }
    }
}