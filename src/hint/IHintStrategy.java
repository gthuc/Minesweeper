package hint;

import model.GameBoardModel;

public interface IHintStrategy {

    HintResult getHintPosition(GameBoardModel board);
}
