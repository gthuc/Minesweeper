package command;

import model.GameBoardModel;

/**
 * Encapsulates a flag toggle for the Command pattern.
 * Undo applies the same toggle, subject to the board rules.
 */
public class FlagCommand implements ICommand {
    private final int x, y;
    private final GameBoardModel board;

    public FlagCommand(GameBoardModel board, int x, int y) {
        this.board = board;
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean execute() {
        return board.toggleFlag(x, y);
    }

    @Override
    public boolean undo() {
        return board.toggleFlag(x, y);
    }
}
