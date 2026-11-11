package settings;

public interface ISettingsStorage {

    Settings load();

    void save(Settings settings);
}
