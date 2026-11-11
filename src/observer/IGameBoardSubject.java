package observer;

/**
 * Defines observer registration and notification for board changes.
 */
public interface IGameBoardSubject {
    /**
     * Registers a listener for subsequent board changes.
     */
    void registerObserver(IGameBoardObserver observer);

    /**
     * Stops delivering board changes to a listener.
     */
    void removeObserver(IGameBoardObserver observer);

    /**
     * Delivers a board event to registered listeners.
     */
    void notifyObservers(BoardEvent event);
}
