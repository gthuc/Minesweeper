package util;

public final class PlayerNames {
    public static final int MAX_LENGTH = 20;

    private PlayerNames() {}

    /** @return tên đã trim; ném IllegalArgumentException nếu rỗng hoặc quá dài. */
    public static String normalize(String name) {
        String t = name == null ? "" : name.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("Tên không được để trống.");
        if (t.length() > MAX_LENGTH) throw new IllegalArgumentException("Tên tối đa " + MAX_LENGTH + " ký tự.");
        return t;
    }
}
