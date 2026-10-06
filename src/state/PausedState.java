package state;

import controller.GameManager;
import model.GameStatus;

/** Tạm dừng: bỏ qua mọi lệnh, timer đã được GameManager đóng băng khi chuyển state. */
public class PausedState implements IGameState {
    @Override
    public GameStatus getStatus() { return GameStatus.Paused; }

    @Override
    public void resume(GameManager context) {
        context.transitionTo(new PlayingState());
    }
}
