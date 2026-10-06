package observer;

public interface IGameBoardObserver {
    /** @param eventData mô tả thay đổi: "REVEAL", "FLAG" hoặc "UNDO". */
    void onBoardChange(Object eventData);
}
