package command;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Executes commands and stores successful, undoable actions in a LIFO history.
 * The shared history is cleared whenever a game is replaced or abandoned.
 */
public class CommandManager {
    private static CommandManager instance;
    private final Deque<ICommand> history = new ArrayDeque<>();

    private CommandManager() {
    }

    public static synchronized CommandManager getInstance() {
        if (instance == null) {
            instance = new CommandManager();
        }
        return instance;
    }

    /**
     * Executes an action once and records only effective, undoable changes.
     */
    public synchronized void executeCommand(ICommand command) {
        if (command.execute() && command.isUndoable()) {
            history.push(command);
        }
    }

    /**
     * Consumes history until an action is reversed or no entries remain.
     * @return true if one prior action was successfully reversed
     */
    public synchronized boolean undo() {
        while (!history.isEmpty()) {
            // A stale entry may no longer change the board; continue to an effective undo.
            if (history.pop().undo()) return true;
        }
        return false;
    }

    /**
     * Discards history so commands cannot affect a replacement board.
     */
    public synchronized void clear() {
        history.clear();
    }
}
