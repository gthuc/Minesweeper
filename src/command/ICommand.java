package command;

/**
 * Defines a board action and its optional reversal.
 * A successful action reports an actual change, rather than merely an attempted input.
 */
public interface ICommand {
    /**
     * Applies the action.
     * @return true if the board changed
     */
    boolean execute();

    /**
     * Attempts to reverse the recorded action.
     * @return true if the board changed
     */
    boolean undo();

    /**
     * Reports whether a successful execution may enter undo history.
     * @return true if the command supports normal undo
     */
    default boolean isUndoable() {
        return true;
    }
}
