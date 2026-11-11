package observer;

/**
 * Receives synchronous notifications after board mutations.
 */
public interface IGameBoardObserver {
    /**
     * Called synchronously after a board mutation; the event identifies its category.
     */
    void onBoardChange(BoardEvent event);
}
