package util;

/**
 * Centralizes trimming, blank-name rejection, and player-name length limits.
 */
public final class PlayerNames {
    public static final int MAX_LENGTH = 20;

    private PlayerNames() {
    }

    /**
     * Trims a name and validates its UTF-16 length against MAX_LENGTH.
     * @throws IllegalArgumentException if the name is null, blank, or too long
     */
    public static String normalize(String name) {
        String t = name == null ? "" : name.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("Player name must not be blank.");
        if (t.length() > MAX_LENGTH) throw new IllegalArgumentException("Player name must not exceed " + MAX_LENGTH + " characters.");
        return t;
    }
}
