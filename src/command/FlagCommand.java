package command;

import model.GameBoardModel;

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
        return board.toggleFlag(x, y); // false -> không vào history
    }

    @Override
    public void undo() {
        board.toggleFlag(x, y);
    }
}
