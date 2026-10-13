package storage;

import model.DifficultyLevel;
import util.PlayerNames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaderboardManager {
    public static final int MAX_ENTRIES = 10;
    private static LeaderboardManager instance;

    private final IScoreStorage storage;
    private final Map<DifficultyLevel, List<ScoreRecord>> leaderboards;

    private LeaderboardManager(IScoreStorage storage) {
        this.storage = storage;
        Map<DifficultyLevel, List<ScoreRecord>> loaded = storage.loadScores();
        this.leaderboards = loaded != null ? loaded : new HashMap<>();
    }

    public static synchronized LeaderboardManager getInstance() {
        if (instance == null) {
            instance = new LeaderboardManager(new FileScoreStorage());
        }
        return instance;
    }

    /** Dùng cho test / tiêm storage: thay singleton bằng bản mới (null = file mặc định). */
    public static synchronized void resetInstance(IScoreStorage storage) {
        instance = new LeaderboardManager(storage != null ? storage : new FileScoreStorage());
    }

    /**
     * Thêm điểm. CUSTOM không được ghi (kích thước khác nhau, không so sánh được).
     *
     * @return hạng 1..MAX_ENTRIES của bản ghi vừa thêm, hoặc 0 nếu không được ghi / không lọt top.
     */
    public synchronized int addScore(DifficultyLevel level, ScoreRecord record) {
        if (level == null || level == DifficultyLevel.CUSTOM || record == null) return 0;

        List<ScoreRecord> scores = leaderboards.computeIfAbsent(level, k -> new ArrayList<>());
        scores.add(record);
        Collections.sort(scores);
        while (scores.size() > MAX_ENTRIES) {
            scores.remove(scores.size() - 1);
        }
        storage.saveScores(leaderboards);
        return indexOfIdentity(scores, record) + 1; // -1 + 1 = 0 nếu bị loại khỏi top
    }

    /**
     * Đổi tên người chơi của một bản ghi đã có trong bảng (so khớp theo đối tượng).
     *
     * @return bản ghi mới, hoặc null nếu bản ghi không còn trong bảng.
     * @throws IllegalArgumentException nếu tên rỗng hoặc quá dài.
     */
    public synchronized ScoreRecord renameScore(DifficultyLevel level, ScoreRecord target, String newName) {
        String name = PlayerNames.normalize(newName);
        List<ScoreRecord> scores = leaderboards.get(level);
        if (scores == null || target == null) return null;
        int idx = indexOfIdentity(scores, target);
        if (idx < 0) return null;

        ScoreRecord renamed = target.withPlayerName(name);
        scores.set(idx, renamed); // cùng khoá xếp hạng nên thứ tự không đổi
        storage.saveScores(leaderboards);
        return renamed;
    }

    public synchronized void clear(DifficultyLevel level) {
        if (leaderboards.remove(level) != null) {
            storage.saveScores(leaderboards);
        }
    }

    public synchronized void clearAll() {
        leaderboards.clear();
        storage.saveScores(leaderboards);
    }

    /** Trả về bản sao chỉ đọc, bên ngoài không sửa được dữ liệu nội bộ. */
    public synchronized List<ScoreRecord> getTopScores(DifficultyLevel level) {
        List<ScoreRecord> scores = leaderboards.get(level);
        if (scores == null) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(scores));
    }

    private static int indexOfIdentity(List<ScoreRecord> list, ScoreRecord target) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == target) return i;
        }
        return -1;
    }
}
