package settings;

import java.util.function.Consumer;

public class SettingsManager {
    private static SettingsManager instance;

    private final ISettingsStorage storage;
    private Settings current;

    private SettingsManager(ISettingsStorage storage) {
        this.storage = storage;
        Settings loaded = storage.load();
        this.current = loaded != null ? loaded : new Settings();
    }

    public static synchronized SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager(new FileSettingsStorage());
        }
        return instance;
    }

    /** Dùng cho test: thay singleton bằng bản mới với storage chỉ định (null = file mặc định). */
    public static synchronized void resetInstance(ISettingsStorage storage) {
        instance = new SettingsManager(storage != null ? storage : new FileSettingsStorage());
    }

    /** Trả về bản sao: sửa bản sao không ảnh hưởng cài đặt thật. */
    public synchronized Settings get() {
        return current.copy();
    }

    /**
     * Áp dụng thay đổi trên bản sao; chỉ khi tất cả hợp lệ mới thay thế và lưu file.
     * Nếu {@code change} ném IllegalArgumentException thì cài đặt cũ giữ nguyên.
     */
    public synchronized Settings update(Consumer<Settings> change) {
        Settings candidate = current.copy();
        change.accept(candidate);
        current = candidate;
        storage.save(current);
        return current.copy();
    }
}
