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


    public static synchronized void resetInstance(ISettingsStorage storage) {
        instance = new SettingsManager(storage != null ? storage : new FileSettingsStorage());
    }


    public synchronized Settings get() {
        return current.copy();
    }





    public synchronized Settings update(Consumer<Settings> change) {
        Settings candidate = current.copy();
        change.accept(candidate);
        current = candidate;
        storage.save(current);
        return current.copy();
    }
}
