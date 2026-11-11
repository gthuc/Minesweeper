package settings;

import java.util.function.Consumer;

/**
 * Owns the current settings and applies changes to a candidate copy.
 * A validation failure leaves the previous settings intact. Persistence is delegated
 * to the configured storage, whose failure policy determines how save errors surface.
 */
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

    /**
     * Replaces the singleton and storage, allowing isolated persistence tests.
     */
    public static synchronized void resetInstance(ISettingsStorage storage) {
        instance = new SettingsManager(storage != null ? storage : new FileSettingsStorage());
    }

    /**
     * Returns a defensive copy rather than the stored mutable settings.
     */
    public synchronized Settings get() {
        return current.copy();
    }

    /**
     * Applies a change to a copy, commits it if validation succeeds, then requests a save.
     * Exceptions from the change callback leave the current settings untouched.
     * @return a copy of the committed settings
     */
    public synchronized Settings update(Consumer<Settings> change) {
        Settings candidate = current.copy();
        // Commit only after every requested setter has validated its input.
        change.accept(candidate);
        current = candidate;
        storage.save(current);
        return current.copy();
    }
}
