package minesweeper;
import java.util.Stack;

public class UndoManager {
    private static UndoManager instance;
    private Stack<ICommand> commandHistory;

    private UndoManager() {
        commandHistory = new Stack<>();
    }

    public static UndoManager Instance() {
        if (instance == null) {
            instance = new UndoManager();
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