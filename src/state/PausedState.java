package state;

import controller.GameManager;
import model.GameStatus;


public class PausedState implements IGameState {
    @Override
    public GameStatus getStatus() { return GameStatus.PAUSED; }

    @Override
    public void resume(GameManager context) {
        context.transitionTo(new PlayingState());
    }
}
