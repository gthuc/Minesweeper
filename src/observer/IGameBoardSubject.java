package observer;

public interface IGameBoardSubject {
    void registerObserver(IGameBoardObserver observer);

    void removeObserver(IGameBoardObserver observer);

    void notifyObservers(Object eventData);
}
