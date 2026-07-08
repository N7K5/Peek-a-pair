package com.example.flipandfind;

/** Global tabletop appearance selected from Advanced Settings. Keep entries append-only. */
public enum TabletopMode {
    STATIC_THEME(
        "static_theme",
        "Black / white",
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
    ),
    HONEYCOMB(
        "honeycomb",
        "Honeycomb",
        "A crisp field of interlocking hexagons in the current player's color.",
        Pattern.HONEYCOMB,
        true
    ),
    GLASS(
        "glass",
        "Glass",
        "Large translucent panes with angled highlights and softly varied depth.",
        Pattern.GLASS,
        true
    ),
    REFLECTION(
        "reflection",
        "Reflection",
        "Broad rays and mirrored ripples create a polished reflective surface.",
        Pattern.REFLECTION,
        true
    ),
    PAPER(
        "paper",
        "Paper",
        "Fine fibers and faint ruled grain create a soft paper surface.",
        Pattern.PAPER,
        true
    ),
    MIRROR(
        "mirror",
        "Mirror",
        "Symmetric beveled facets meet in a bright mirrored center.",
        Pattern.MIRROR,
        true
    ),
    WATER(
        "water",
        "Water",
        "Layered waves and scattered ripple rings flow across the table.",
        Pattern.WATER,
        true
    ),
    RETRO(
        "retro",
        "Retro",
        "Bold pixel checks and scanlines give the table an arcade-era finish.",
        Pattern.RETRO,
        true
    ),
    NEON(
        "neon",
        "Neon",
        "Glowing circuit traces and bright nodes run through the surface.",
        Pattern.NEON,
        true
    ),
    FELT(
        "felt",
        "Felt",
        "Dense multidirectional fibers create a tactile game-table texture.",
        Pattern.FELT,
        true
    ),
    RANDOM_EACH_GAME(
        "random_each_game",
        "Random each game",
        "Starts with Black / white after each app restart, then changes every game.",
        Pattern.SOLID,
        false,
        SelectionType.RANDOM_EACH_GAME
    );

    /** Whether this value is a drawable surface or a rule that resolves to one. */
    public enum SelectionType {
        CONCRETE,
        RANDOM_EACH_GAME
    }

    /** The procedural drawing recipe used by {@link TabletopBackgroundDrawable}. */
    public enum Pattern {
        SOLID,
        TILES,
        MOSAIC,
        GLASS_TILES,
        DOTS,
        CONTOURS,
        HONEYCOMB,
        GLASS,
        REFLECTION,
        PAPER,
        MIRROR,
        WATER,
        RETRO,
        NEON,
        FELT
    }

    private final String preferenceId;
    private final String displayName;
    private final String description;
    private final Pattern pattern;
    private final boolean playerTinted;
    private final SelectionType selectionType;

    TabletopMode(
        String preferenceId,
        String displayName,
        String description,
        Pattern pattern,
        boolean playerTinted
    ) {
        this(
            preferenceId,
            displayName,
            description,
            pattern,
            playerTinted,
            SelectionType.CONCRETE
        );
    }

    TabletopMode(
        String preferenceId,
        String displayName,
        String description,
        Pattern pattern,
        boolean playerTinted,
        SelectionType selectionType
    ) {
        this.preferenceId = preferenceId;
        this.displayName = displayName;
        this.description = description;
        this.pattern = pattern;
        this.playerTinted = playerTinted;
        this.selectionType = selectionType;
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

    public SelectionType getSelectionType() {
        return selectionType;
    }

    public boolean isConcrete() {
        return selectionType == SelectionType.CONCRETE;
    }

    public boolean isRandomEachGame() {
        return selectionType == SelectionType.RANDOM_EACH_GAME;
    }

    /** Returns a defensive copy of every mode that can be drawn directly. */
    public static TabletopMode[] concreteModesCopy() {
        TabletopMode[] modes = values();
        int concreteCount = 0;
        for (TabletopMode mode : modes) {
            if (mode.isConcrete()) {
                concreteCount++;
            }
        }
        TabletopMode[] concreteModes = new TabletopMode[concreteCount];
        int concreteIndex = 0;
        for (TabletopMode mode : modes) {
            if (mode.isConcrete()) {
                concreteModes[concreteIndex++] = mode;
            }
        }
        return concreteModes;
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
