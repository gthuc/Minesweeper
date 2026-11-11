package settings;

/**
 * Separates settings persistence from validation and application state.
 */
public interface ISettingsStorage {
    /**
     * Loads preferences according to the storage implementation policy.
     */
    Settings load();

    /**
     * Persists preferences according to the storage implementation failure policy.
     */
    void save(Settings settings);
}
