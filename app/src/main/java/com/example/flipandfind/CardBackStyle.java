package com.example.flipandfind;

/** Stable, global card-back designs. Palette values are opaque ARGB colors. */
public enum CardBackStyle {
    CLASSIC("classic", "Classic", 0xFF252A45, 0xFF49507D, 0xFFB8B2FF, 0xFFFFB74D),
    CONSTELLATION("constellation", "Constellation", 0xFF13233F, 0xFF345B8C, 0xFF8FD3FF, 0xFFFFD166),
    SUNBURST("sunburst", "Sunburst", 0xFF4B1830, 0xFF85385C, 0xFFFF91B8, 0xFFFFC857),
    WAVES("waves", "Waves", 0xFF073B3A, 0xFF147D78, 0xFF7DE2D1, 0xFFF4D35E),
    HARLEQUIN("harlequin", "Harlequin", 0xFF38271E, 0xFF77503A, 0xFFE6C59A, 0xFFF2A65A),
    ORBITS(
        "orbits",
        "Orbits",
        0xFF241747,
        0xFF5C4B8A,
        0xFFC3B7FF,
        0xFFFFCB69,
        MotionKind.ORBIT
    ),
    PRISM("prism", "Prism", 0xFF19223F, 0xFF465B8C, 0xFF8BCDF4, 0xFFFF91C8),
    BOTANICAL("botanical", "Botanical", 0xFF153528, 0xFF416C53, 0xFFA8D5A2, 0xFFF2C14E),
    WEAVE("weave", "Weave", 0xFF2D302B, 0xFF666A5E, 0xFFD8D1B4, 0xFFE89A6A),
    AURORA(
        "aurora",
        "Aurora",
        0xFF101A38,
        0xFF3B4F83,
        0xFF6FDDD0,
        0xFFC2A6FF,
        MotionKind.AURORA_DRIFT
    ),
    FIREFLIES(
        "fireflies",
        "Fireflies",
        0xFF122B25,
        0xFF3D6254,
        0xFF8FD19E,
        0xFFFFD166,
        MotionKind.FIREFLY_TWINKLE
    ),
    KALEIDO(
        "kaleido",
        "Kaleido",
        0xFF30183C,
        0xFF704779,
        0xFFD9A7E8,
        0xFFFFB45C,
        MotionKind.KALEIDO_BREATHE
    ),
    COMET_TRAILS(
        "comet_trails",
        "Comet Trails",
        0xFF0B1739,
        0xFF31578E,
        0xFF84D9FF,
        0xFFFFD166,
        MotionKind.COMET_GLIDE
    ),
    MOON_RIPPLES(
        "moon_ripples",
        "Moon Ripples",
        0xFF062E45,
        0xFF167188,
        0xFF65D6CE,
        0xFFF6C177,
        MotionKind.RIPPLE_EXPAND
    ),
    PIXEL_RAIN(
        "pixel_rain",
        "Pixel Rain",
        0xFF102A24,
        0xFF315F4C,
        0xFF66F2A3,
        0xFFC4FF4D,
        MotionKind.PIXEL_FALL
    );

    enum MotionKind {
        NONE,
        ORBIT,
        AURORA_DRIFT,
        FIREFLY_TWINKLE,
        KALEIDO_BREATHE,
        COMET_GLIDE,
        RIPPLE_EXPAND,
        PIXEL_FALL
    }

    private final String preferenceId;
    private final String displayName;
    private final int fillColor;
    private final int borderColor;
    private final int patternColor;
    private final int accentColor;
    private final MotionKind motionKind;

    CardBackStyle(
        String preferenceId,
        String displayName,
        int fillColor,
        int borderColor,
        int patternColor,
        int accentColor
    ) {
        this(
            preferenceId,
            displayName,
            fillColor,
            borderColor,
            patternColor,
            accentColor,
            MotionKind.NONE
        );
    }

    CardBackStyle(
        String preferenceId,
        String displayName,
        int fillColor,
        int borderColor,
        int patternColor,
        int accentColor,
        MotionKind motionKind
    ) {
        this.preferenceId = preferenceId;
        this.displayName = displayName;
        this.fillColor = fillColor;
        this.borderColor = borderColor;
        this.patternColor = patternColor;
        this.accentColor = accentColor;
        this.motionKind = motionKind;
    }

    public String getPreferenceId() {
        return preferenceId;
    }

    public String getDisplayName() {
        return displayName;
    }

    int getFillColor() {
        return fillColor;
    }

    int getBorderColor() {
        return borderColor;
    }

    int getPatternColor() {
        return patternColor;
    }

    int getAccentColor() {
        return accentColor;
    }

    MotionKind getMotionKind() {
        return motionKind;
    }

    public boolean isAnimated() {
        return motionKind != MotionKind.NONE;
    }

    public static CardBackStyle fromPreference(String rawValue) {
        if (rawValue != null) {
            for (CardBackStyle style : values()) {
                if (style.preferenceId.equals(rawValue)) {
                    return style;
                }
            }
        }
        return CLASSIC;
    }
}
