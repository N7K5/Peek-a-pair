package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class GameSurfaceColorsTest {
    @Test
    public void staticThemeIsExactBlackOrWhiteAndIgnoresPlayer() {
        int firstPlayer = 0xFF5B4BDB;
        int secondPlayer = 0xFFF59E0B;

        assertEquals(
            0xFF000000,
            GameSurfaceColors.tabletop(
                TabletopMode.STATIC_THEME,
                firstPlayer,
                0xFF1D2130,
                true
            )
        );
        assertEquals(
            0xFF000000,
            GameSurfaceColors.tabletop(
                TabletopMode.STATIC_THEME,
                secondPlayer,
                0xFF1D2130,
                true
            )
        );
        assertEquals(
            0xFFFFFFFF,
            GameSurfaceColors.tabletop(
                TabletopMode.STATIC_THEME,
                firstPlayer,
                0xFFFFFFFF,
                false
            )
        );
    }

    @Test
    public void staticThemeUsesTheNeutralBorderExactly() {
        assertEquals(
            0xFF445566,
            GameSurfaceColors.tabletopBorder(
                TabletopMode.STATIC_THEME,
                0xFFE84A5F,
                0xFF1D2130,
                0xFF445566,
                true
            )
        );
    }

    @Test
    public void playerTintModeStillChangesWithPlayer() {
        int surface = 0xFF1D2130;
        assertNotEquals(
            GameSurfaceColors.tabletop(
                TabletopMode.PLAYER_TINT,
                0xFF5B4BDB,
                surface,
                true
            ),
            GameSurfaceColors.tabletop(
                TabletopMode.PLAYER_TINT,
                0xFFF59E0B,
                surface,
                true
            )
        );
    }

    @Test
    public void tabletopIsOpaqueAndRetainsPlayerIdentity() {
        int surface = 0xFF1D2130;
        int violet = 0xFF5B4BDB;
        int orange = 0xFFF59E0B;

        int violetTable = GameSurfaceColors.tabletop(violet, surface, true);
        int orangeTable = GameSurfaceColors.tabletop(orange, surface, true);

        assertEquals(0xFF, violetTable >>> 24);
        assertEquals(0xFF, orangeTable >>> 24);
        assertNotEquals(violetTable, orangeTable);
    }

    @Test
    public void darkTabletopStaysDarkAndLightTabletopStaysLight() {
        int player = 0xFFE84A5F;
        int darkTable = GameSurfaceColors.tabletop(player, 0xFF161E20, true);
        int lightTable = GameSurfaceColors.tabletop(player, 0xFFFFFFFF, false);

        assertTrue(luminance(darkTable) < 0.20);
        assertTrue(luminance(lightTable) > 0.70);
    }

    @Test
    public void borderUsesMorePlayerColorThanTabletop() {
        int player = 0xFF00897B;
        int surface = 0xFF161E20;
        int tabletop = GameSurfaceColors.tabletop(player, surface, true);
        int border = GameSurfaceColors.tabletopBorder(player, surface, true);

        assertTrue(colorDistance(border, player) < colorDistance(tabletop, player));
    }

    @Test
    public void blendClampsAmounts() {
        assertEquals(0xFF102030, GameSurfaceColors.blend(0xFF102030, 0xFFAABBCC, -1f));
        assertEquals(0xFFAABBCC, GameSurfaceColors.blend(0xFF102030, 0xFFAABBCC, 2f));
    }

    private static double luminance(int color) {
        return 0.2126 * channel((color >>> 16) & 0xFF)
            + 0.7152 * channel((color >>> 8) & 0xFF)
            + 0.0722 * channel(color & 0xFF);
    }

    private static double channel(int value) {
        double component = value / 255.0;
        return component <= 0.04045
            ? component / 12.92
            : Math.pow((component + 0.055) / 1.055, 2.4);
    }

    private static int colorDistance(int first, int second) {
        int red = ((first >>> 16) & 0xFF) - ((second >>> 16) & 0xFF);
        int green = ((first >>> 8) & 0xFF) - ((second >>> 8) & 0xFF);
        int blue = (first & 0xFF) - (second & 0xFF);
        return red * red + green * green + blue * blue;
    }
}
