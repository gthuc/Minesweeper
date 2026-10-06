package hint;

import model.GameBoardModel;

public interface IHintStrategy {
    /** @return gợi ý, hoặc null nếu chiến lược này không tìm được. */
    HintResult getHintPosition(GameBoardModel board);
}
