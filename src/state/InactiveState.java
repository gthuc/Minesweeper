package state;

import model.GameStatus;


public class InactiveState implements IGameState {
    private final GameStatus status;

    public InactiveState(GameStatus status) {
        this.status = status;
    }

    @Override
    public GameStatus getStatus() { return status; }
}
