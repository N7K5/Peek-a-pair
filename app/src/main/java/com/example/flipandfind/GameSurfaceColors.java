package com.example.flipandfind;

/** Pure color blending rules for the active player's game surround and tabletop. */
public final class GameSurfaceColors {
    private static final float DARK_TABLETOP_PLAYER_AMOUNT = 0.28f;
    private static final float LIGHT_TABLETOP_PLAYER_AMOUNT = 0.12f;
    private static final float DARK_BORDER_PLAYER_AMOUNT = 0.62f;
    private static final float LIGHT_BORDER_PLAYER_AMOUNT = 0.38f;

    private GameSurfaceColors() {}

    public static int tabletop(
        TabletopMode mode,
        int playerColor,
        int themeSurface,
        boolean darkTheme
    ) {
        if (mode == null || !mode.isPlayerTinted()) {
            return darkTheme ? 0xFF000000 : 0xFFFFFFFF;
        }
        return tabletop(playerColor, themeSurface, darkTheme);
    }

    public static int tabletopBorder(
        TabletopMode mode,
        int playerColor,
        int themeSurface,
        int neutralBorder,
        boolean darkTheme
    ) {
        if (mode == null || !mode.isPlayerTinted()) {
            return neutralBorder | 0xFF000000;
        }
        return tabletopBorder(playerColor, themeSurface, darkTheme);
    }

    public static int tabletop(int playerColor, int themeSurface, boolean darkTheme) {
        return blend(
            themeSurface,
            playerColor,
            darkTheme ? DARK_TABLETOP_PLAYER_AMOUNT : LIGHT_TABLETOP_PLAYER_AMOUNT
        );
    }

    public static int tabletopBorder(int playerColor, int themeSurface, boolean darkTheme) {
        return blend(
            themeSurface,
            playerColor,
            darkTheme ? DARK_BORDER_PLAYER_AMOUNT : LIGHT_BORDER_PLAYER_AMOUNT
        );
    }

    static int blend(int baseColor, int overlayColor, float overlayAmount) {
        float amount = Math.max(0f, Math.min(1f, overlayAmount));
        float baseAmount = 1f - amount;
        int red = Math.round(red(baseColor) * baseAmount + red(overlayColor) * amount);
        int green = Math.round(green(baseColor) * baseAmount + green(overlayColor) * amount);
        int blue = Math.round(blue(baseColor) * baseAmount + blue(overlayColor) * amount);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static int red(int color) {
        return (color >>> 16) & 0xFF;
    }

    private static int green(int color) {
        return (color >>> 8) & 0xFF;
    }

    private static int blue(int color) {
        return color & 0xFF;
    }
}
