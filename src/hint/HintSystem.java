package hint;

import model.GameBoardModel;

/**
 * Selects hints from the actual world through a Strategy.
 */
public class HintSystem {
    private static HintSystem instance;
    private final IHintStrategy strategy = new WorldHintStrategy();

    private HintSystem() {
    }

    public static synchronized HintSystem getInstance() {
        if (instance == null) {
            instance = new HintSystem();
        }
        return instance;
    }

    /**
     * Returns a suggestion without revealing, flagging, or generating mines.
     * @return a hint, or null if no candidate exists
     */
    public HintResult getHint(GameBoardModel board) {
        return board == null ? null : strategy.getHintPosition(board);
    }
}
