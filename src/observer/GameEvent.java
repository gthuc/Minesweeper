package observer;

/**
 * Describes lifecycle changes independently of low-level board mutations.
 */
public enum GameEvent {
    GAME_STARTED,
    LIFE_LOST,
    WON,
    LOST,
    PAUSED,
    RESUMED,
    RETURNED_TO_MENU
}
