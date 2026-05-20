package top.mrxiaom.sweet.chat.api;

public enum EnumMuteMode {
    NOT_MUTED(0),
    MUTED_TIMED(1),
    MUTED_INFINITE(2),

    ;
    private final int value;
    EnumMuteMode(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static EnumMuteMode valueOf(int value, EnumMuteMode def) {
        for (EnumMuteMode mode : values()) {
            if (mode.value == value) {
                return mode;
            }
        }
        return def;
    }
}
