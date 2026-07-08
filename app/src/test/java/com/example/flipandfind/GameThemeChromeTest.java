package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public final class GameThemeChromeTest {
    private static final int DARK_BACKGROUND = 0xFF0C1617;
    private static final int DARK_SURFACE = 0xFF162627;
    private static final int DARK_DIVIDER = 0xFF314949;

    @Test
    public void everyStyleRetainsPlayerIdentityAcrossTheGameChrome() {
        int violet = 0xFF5B4BDB;
        int orange = 0xFFF59E0B;

        for (GameThemeChrome.Style style : GameThemeChrome.Style.values()) {
            GameThemeChrome.Treatment first = dark(style, violet, false);
            GameThemeChrome.Treatment second = dark(style, orange, false);

            assertEquals(violet, first.getPlayerAccentColor());
            assertEquals(orange, second.getPlayerAccentColor());
            assertNotEquals(first.getSurroundStart(), second.getSurroundStart());
            assertNotEquals(first.getHeaderColor(), second.getHeaderColor());
            assertNotEquals(first.getBoardFrameColor(), second.getBoardFrameColor());
            assertNotEquals(first.getDecorationColor(), second.getDecorationColor());
        }
    }

    @Test
    public void styleFamiliesExposeMateriallyDifferentSurfaceLanguage() {
        Set<GameThemeChrome.Decoration> decorations = EnumSet.noneOf(
            GameThemeChrome.Decoration.class
        );
        Set<String> shapeSignatures = new HashSet<>();

        for (GameThemeChrome.Style style : GameThemeChrome.Style.values()) {
            GameThemeChrome.Treatment treatment = dark(style, 0xFF00897B, false);
            decorations.add(treatment.getDecoration());
            shapeSignatures.add(
                treatment.getCornerScale() + ":"
                    + treatment.getPanelAlpha() + ":"
                    + treatment.getStrokeWidthDp()
            );
        }

        assertEquals(GameThemeChrome.Style.values().length, decorations.size());
        assertEquals(GameThemeChrome.Style.values().length, shapeSignatures.size());
    }

    @Test
    public void allPublishedColorsAreOpaqueAndHeaderTextIsAccessible() {
        for (GameThemeChrome.Style style : GameThemeChrome.Style.values()) {
            GameThemeChrome.Treatment treatment = dark(style, 0xFFE84A5F, false);
            for (int color : treatment.copySurroundColors()) {
                assertEquals(0xFF, color >>> 24);
            }
            assertEquals(0xFF, treatment.getHeaderColor() >>> 24);
            assertEquals(0xFF, treatment.getHeaderTextColor() >>> 24);
            assertEquals(0xFF, treatment.getHeaderOutlineColor() >>> 24);
            assertEquals(0xFF, treatment.getBoardFrameColor() >>> 24);
            assertEquals(0xFF, treatment.getBoardFrameBorderColor() >>> 24);
            assertEquals(0xFF, treatment.getDecorationColor() >>> 24);
            assertTrue(
                ContrastColors.contrastRatio(
                    treatment.getHeaderTextColor(),
                    treatment.getHeaderColor()
                ) >= 4.5
            );
        }
    }

    @Test
    public void highContrastUsesStrongOutlinesAndOpaquePanels() {
        for (GameThemeChrome.Style style : GameThemeChrome.Style.values()) {
            GameThemeChrome.Treatment treatment = dark(style, 0xFF5B4BDB, true);

            assertTrue(treatment.getStrokeWidthDp() >= 2);
            assertEquals(255, treatment.getPanelAlpha());
            assertTrue(
                ContrastColors.contrastRatio(
                    treatment.getBoardFrameBorderColor(),
                    treatment.getBoardFrameColor()
                ) >= 3.0
            );
            assertTrue(
                ContrastColors.contrastRatio(
                    treatment.getDecorationColor(),
                    treatment.getBoardFrameColor()
                ) >= 3.0
            );
            assertTrue(
                ContrastColors.contrastRatio(
                    treatment.getHeaderOutlineColor(),
                    treatment.getHeaderColor()
                ) >= 4.5
            );
        }
    }

    @Test
    public void highContrastStillKeepsThemeDecorationAndPlayerCue() {
        for (GameThemeChrome.Style style : GameThemeChrome.Style.values()) {
            GameThemeChrome.Treatment normal = dark(style, 0xFF00897B, false);
            GameThemeChrome.Treatment contrast = dark(style, 0xFF00897B, true);

            assertEquals(normal.getDecoration(), contrast.getDecoration());
            assertEquals(0xFF00897B, contrast.getPlayerAccentColor());
            assertNotEquals(contrast.getSurroundStart(), contrast.getSurroundEnd());
        }
    }

    @Test
    public void lightAndDarkPalettesProduceDifferentThemeSurfaces() {
        GameThemeChrome.Treatment dark = dark(
            GameThemeChrome.Style.PAPER,
            0xFFC2410C,
            false
        );
        GameThemeChrome.Treatment light = GameThemeChrome.treatment(
            GameThemeChrome.Style.PAPER,
            0xFFC2410C,
            0xFFF2E1BD,
            0xFFFFF9E8,
            0xFFC7A981,
            false,
            false
        );

        assertNotEquals(dark.getSurroundEnd(), light.getSurroundEnd());
        assertNotEquals(dark.getBoardFrameColor(), light.getBoardFrameColor());
        assertNotEquals(dark.getDecorationColor(), light.getDecorationColor());
    }

    @Test
    public void classicColorThemesStillAffectLiveGameChrome() {
        int player = 0xFF5B4BDB;
        GameThemeChrome.Treatment teal = GameThemeChrome.treatment(
            GameThemeChrome.Style.CLASSIC,
            player,
            0xFF0C1617,
            0xFF162627,
            0xFF314949,
            false,
            true
        );
        GameThemeChrome.Treatment orange = GameThemeChrome.treatment(
            GameThemeChrome.Style.CLASSIC,
            player,
            0xFF180801,
            0xFF2E1205,
            0xFF692E0C,
            false,
            true
        );

        assertEquals(player, teal.getPlayerAccentColor());
        assertEquals(player, orange.getPlayerAccentColor());
        assertNotEquals(teal.getSurroundEnd(), orange.getSurroundEnd());
        assertNotEquals(teal.getHeaderColor(), orange.getHeaderColor());
        assertNotEquals(teal.getBoardFrameColor(), orange.getBoardFrameColor());
    }

    @Test
    public void defaultClassicChromeIsThemeFirstWithAnExactPlayerAccent() {
        int player = 0xFF5B4BDB;
        GameThemeChrome.Treatment treatment = GameThemeChrome.treatment(
            GameThemeChrome.Style.CLASSIC,
            player,
            DARK_BACKGROUND,
            DARK_SURFACE,
            DARK_DIVIDER,
            0xFF26BEAD,
            0xFFFFBE4E,
            false,
            true
        );

        assertEquals(player, treatment.getPlayerAccentColor());
        assertTrue(
            colorDistance(treatment.getHeaderColor(), DARK_SURFACE)
                < colorDistance(treatment.getHeaderColor(), player)
        );
        assertTrue(
            colorDistance(treatment.getSurroundEnd(), DARK_BACKGROUND)
                < colorDistance(treatment.getSurroundEnd(), player)
        );
    }

    @Test
    public void liveDecorationUsesThemePrimaryAndAccent() {
        GameThemeChrome.Treatment teal = GameThemeChrome.treatment(
            GameThemeChrome.Style.ARCADE,
            0xFF5B4BDB,
            DARK_BACKGROUND,
            DARK_SURFACE,
            DARK_DIVIDER,
            0xFF00F5D4,
            0xFFFF3DA5,
            false,
            true
        );
        GameThemeChrome.Treatment amber = GameThemeChrome.treatment(
            GameThemeChrome.Style.ARCADE,
            0xFF5B4BDB,
            DARK_BACKGROUND,
            DARK_SURFACE,
            DARK_DIVIDER,
            0xFFFFB000,
            0xFF6EE7FF,
            false,
            true
        );

        assertNotEquals(teal.getDecorationColor(), amber.getDecorationColor());
        assertNotEquals(teal.getHeaderOutlineColor(), amber.getHeaderOutlineColor());
    }

    @Test
    public void nullStyleFallsBackToClassic() {
        GameThemeChrome.Treatment fallback = GameThemeChrome.treatment(
            null,
            0xFF00897B,
            DARK_BACKGROUND,
            DARK_SURFACE,
            DARK_DIVIDER,
            false,
            true
        );
        GameThemeChrome.Treatment classic = dark(
            GameThemeChrome.Style.CLASSIC,
            0xFF00897B,
            false
        );

        assertEquals(classic.getHeaderColor(), fallback.getHeaderColor());
        assertEquals(classic.getDecoration(), fallback.getDecoration());
        assertEquals(classic.getCornerScale(), fallback.getCornerScale(), 0f);
    }

    @Test
    public void surroundArrayIsDefensivelyCopied() {
        GameThemeChrome.Treatment treatment = dark(
            GameThemeChrome.Style.GLASS,
            0xFF3B82F6,
            false
        );
        int[] first = treatment.copySurroundColors();
        int[] second = treatment.copySurroundColors();

        assertNotSame(first, second);
        first[0] = 0;
        assertEquals(treatment.getSurroundStart(), second[0]);
        assertEquals(0xFF3B82F6, treatment.getPlayerAccentColor());
    }

    private static GameThemeChrome.Treatment dark(
        GameThemeChrome.Style style,
        int player,
        boolean highContrast
    ) {
        return GameThemeChrome.treatment(
            style,
            player,
            DARK_BACKGROUND,
            DARK_SURFACE,
            DARK_DIVIDER,
            highContrast,
            true
        );
    }

    private static int colorDistance(int first, int second) {
        int red = ((first >>> 16) & 0xFF) - ((second >>> 16) & 0xFF);
        int green = ((first >>> 8) & 0xFF) - ((second >>> 8) & 0xFF);
        int blue = (first & 0xFF) - (second & 0xFF);
        return red * red + green * green + blue * blue;
    }
}
