package com.example.flipandfind;

/** Global tabletop appearance selected from Advanced Settings. */
public enum TabletopMode {
    STATIC_THEME("static_theme", "Theme black / white"),
    PLAYER_TINT("player_tint", "Player tint");

    private final String preferenceId;
    private final String displayName;

    TabletopMode(String preferenceId, String displayName) {
        this.preferenceId = preferenceId;
        this.displayName = displayName;
    }

    public String getPreferenceId() {
        return preferenceId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TabletopMode fromPreference(String rawValue) {
        if (rawValue != null) {
            for (TabletopMode mode : values()) {
                if (mode.preferenceId.equals(rawValue)) {
                    return mode;
                }
            }
        }
        return STATIC_THEME;
    }
}
