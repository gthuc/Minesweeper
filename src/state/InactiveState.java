package state;

import model.GameStatus;

/**
 * Reports an inactive status while inheriting disabled gameplay actions.
 * The manager uses this state for the menu and completed games.
 */
public class InactiveState implements IGameState {
    private final GameStatus status;

    public InactiveState(GameStatus status) {
        this.status = status;
    }

    @Override
    public GameStatus getStatus() {
        return status;
    }
}
