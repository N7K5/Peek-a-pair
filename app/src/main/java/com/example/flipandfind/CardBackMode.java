package com.example.flipandfind;

/** Persisted behavior for choosing concrete card-back designs. */
public enum CardBackMode {
    FIXED("fixed", "Choose one design"),
    RANDOM_EACH_GAME("random_each_game", "Random each game"),
    RANDOM_EACH_CARD("random_each_card", "Random across cards");

    private final String preferenceId;
    private final String displayName;

    CardBackMode(String preferenceId, String displayName) {
        this.preferenceId = preferenceId;
        this.displayName = displayName;
    }

    public String getPreferenceId() {
        return preferenceId;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Older installs have no mode preference and therefore keep their fixed design. */
    public static CardBackMode fromPreference(String rawValue) {
        if (rawValue != null) {
            for (CardBackMode mode : values()) {
                if (mode.preferenceId.equals(rawValue)) {
                    return mode;
                }
            }
        }
        return FIXED;
    }
}
