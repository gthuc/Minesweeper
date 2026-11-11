package command;

public interface ICommand {

    boolean execute();


    boolean undo();


    default boolean isUndoable() {
        return true;
    }
}
