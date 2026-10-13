package observer;

/**
 * Sự kiện cấp ván chơi (thắng/thua/mất mạng...). Được gọi khi GameManager đang giữ lock,
 * nên listener phải nhanh và không gọi ngược lại các thao tác chậm.
 */
public interface IGameEventListener {
    void onGameEvent(GameEvent event, String message);
}
