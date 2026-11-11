package observer;

/**
 * Receives a lifecycle event and its human-readable message.
 * UI implementations must arrange Swing updates on the event dispatch thread.
 */
public interface IGameEventListener {
    /**
     * Handles a lifecycle notification with its display message.
     */
    void onGameEvent(GameEvent event, String message);
}
