package minesweeper;

public interface IHintStrategy {
    public HintResult GetHintPosition(GameBoardModel board);
}