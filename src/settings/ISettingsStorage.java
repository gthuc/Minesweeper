package settings;

public interface ISettingsStorage {
    /** Luôn trả về Settings hợp lệ (giá trị mặc định nếu chưa có file / file hỏng). */
    Settings load();

    void save(Settings settings);
}
