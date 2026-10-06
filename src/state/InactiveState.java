package state;

import model.GameStatus;

/** Chưa bắt đầu / đã thắng / đã thua: không nhận thao tác nào. */
public class InactiveState implements IGameState {
    private final GameStatus status;

    public InactiveState(GameStatus status) {
        this.status = status;
    }

    @Override
    public GameStatus getStatus() { return status; }
}
