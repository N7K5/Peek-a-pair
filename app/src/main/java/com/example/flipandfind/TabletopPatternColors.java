package com.example.flipandfind;

/** Pure, theme-aware colors shared by tabletop board backgrounds and selector previews. */
public final class TabletopPatternColors {
    private TabletopPatternColors() {}

    public static int primary(int tabletopColor, int playerColor, boolean darkTheme) {
        int contrast = darkTheme ? 0xFFFFFFFF : 0xFF000000;
        int lifted = GameSurfaceColors.blend(
            tabletopColor,
            contrast,
            darkTheme ? 0.32f : 0.20f
        );
        return GameSurfaceColors.blend(lifted, playerColor, darkTheme ? 0.30f : 0.24f);
    }

    public static int secondary(int tabletopColor, int playerColor, boolean darkTheme) {
        int contrast = darkTheme ? 0xFFFFFFFF : 0xFF000000;
        int playerShade = GameSurfaceColors.blend(
            tabletopColor,
            playerColor,
            darkTheme ? 0.52f : 0.34f
        );
        return GameSurfaceColors.blend(
            playerShade,
            contrast,
            darkTheme ? 0.12f : 0.08f
        );
    }

    public static int highlight(int tabletopColor, boolean darkTheme) {
        return GameSurfaceColors.blend(
            tabletopColor,
            darkTheme ? 0xFFFFFFFF : 0xFF000000,
            darkTheme ? 0.55f : 0.32f
        );
    }
}
