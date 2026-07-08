package com.example.flipandfind;

/** Global tabletop appearance selected from Advanced Settings. Keep entries append-only. */
public enum TabletopMode {
    STATIC_THEME(
        "static_theme",
        "Theme black / white",
        "Pure black in dark themes and pure white in light themes.",
        Pattern.SOLID,
        false
    ),
    PLAYER_TINT(
        "player_tint",
        "Player tint",
        "A subtle shade of the current player's color.",
        Pattern.SOLID,
        true
    ),
    TILES(
        "tiles",
        "Tiles",
        "A tidy tiled grid shaded for the current player.",
        Pattern.TILES,
        true
    ),
    MOSAIC(
        "mosaic",
        "Mosaic",
        "Angular mosaic pieces with softly varied shades.",
        Pattern.MOSAIC,
        true
    ),
    GLASS_TILES(
        "glass_tiles",
        "Glass tiles",
        "Staggered translucent tiles with subtle highlights.",
        Pattern.GLASS_TILES,
        true
    ),
    DOTS(
        "dots",
        "Dots",
        "A calm field of offset dots in the player's color.",
        Pattern.DOTS,
        true
    ),
    CONTOURS(
        "contours",
        "Contours",
        "Flowing topographic lines across the tabletop.",
        Pattern.CONTOURS,
        true
    );

    /** The procedural drawing recipe used by {@link TabletopBackgroundDrawable}. */
    public enum Pattern {
        SOLID,
        TILES,
        MOSAIC,
        GLASS_TILES,
        DOTS,
        CONTOURS
    }

    private final String preferenceId;
    private final String displayName;
    private final String description;
    private final Pattern pattern;
    private final boolean playerTinted;

    TabletopMode(
        String preferenceId,
        String displayName,
        String description,
        Pattern pattern,
        boolean playerTinted
    ) {
        this.preferenceId = preferenceId;
        this.displayName = displayName;
        this.description = description;
        this.pattern = pattern;
        this.playerTinted = playerTinted;
    }

    public String getPreferenceId() {
        return preferenceId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public boolean hasPattern() {
        return pattern != Pattern.SOLID;
    }

    public boolean isPlayerTinted() {
        return playerTinted;
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
