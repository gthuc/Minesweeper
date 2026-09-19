package minesweeper;

public interface IGameBoardSubject {
    public void RegisterObserver(IGameBoardObserver observer);

    public  void RemoveObserver(IGameBoardObserver observer);

    public void NotifyObservers(Object eventData);
}