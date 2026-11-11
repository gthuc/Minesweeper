package hint;

import model.GameBoardModel;

/**
 * Strategy contract for selecting a hint from the current board information.
 */
public interface IHintStrategy {
    /**
     * Selects a candidate without mutating the board.
     * @return a hint, or null if this strategy cannot suggest a move
     */
    HintResult getHintPosition(GameBoardModel board);
}
