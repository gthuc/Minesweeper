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

        if (command.execute() && command.isUndoable()) {
            history.push(command);
        }
    }







    public synchronized boolean undo() {
        while (!history.isEmpty()) {
            if (history.pop().undo()) return true;
        }
        return false;
    }


    public synchronized void clear() {
        history.clear();
    }
}
