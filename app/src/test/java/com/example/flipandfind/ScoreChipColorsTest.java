package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ScoreChipColorsTest {
    private static final int[] PLAYER_COLORS = {
        0xFF5B4BDB, 0xFFE84A5F, 0xFF00897B, 0xFFF59E0B, 0xFF3B82F6,
        0xFF8B5CF6, 0xFFD97706, 0xFF0F766E, 0xFFBE185D, 0xFF475569,
        0xFF0891B2, 0xFF65A30D, 0xFFC2410C, 0xFF7E22CE, 0xFF059669
    };

    private static final Palette[] PALETTES = {
        // Default dark teal, minimal dark, glass, arcade, light teal, paper, minimal light.
        new Palette(0xFF1A5450, 0xFF162627, true),
        new Palette(0xFF303133, 0xFF191B1D, true),
        new Palette(0xFF31566E, 0xFF1D3048, true),
        new Palette(0xFF073C3D, 0xFF0D1422, true),
        new Palette(0xFF42A69D, 0xFFFFFFFF, false),
        new Palette(0xFFE5B36C, 0xFFFFF9E8, false),
        new Palette(0xFFD9DADC, 0xFFFFFFFF, false)
    };

    @Test
    public void everyChipUsesReadableTextAndVisibleHeaderBoundary() {
        for (Palette palette : PALETTES) {
            for (int player : PLAYER_COLORS) {
                for (boolean active : new boolean[] {false, true}) {
                    ScoreChipColors.Treatment treatment = ScoreChipColors.treatment(
                        player,
                        palette.header,
                        palette.surface,
                        active,
                        false,
                        palette.dark
                    );

                    assertTrue(ContrastColors.contrastRatio(
                        treatment.getTextColor(),
                        treatment.getFillColor()
                    ) >= 4.5d);
                    assertTrue(ContrastColors.contrastRatio(
                        treatment.getOutlineColor(),
                        palette.header
                    ) >= 3.0d);
                }
            }
        }
    }

    @Test
    public void highContrastReachesEnhancedTextContrast() {
        for (Palette palette : PALETTES) {
            for (int player : PLAYER_COLORS) {
                for (boolean active : new boolean[] {false, true}) {
                    ScoreChipColors.Treatment treatment = ScoreChipColors.treatment(
                        player,
                        palette.header,
                        palette.surface,
                        active,
                        true,
                        palette.dark
                    );

                    assertTrue(ContrastColors.contrastRatio(
                        treatment.getTextColor(),
                        treatment.getFillColor()
                    ) >= 7.0d);
                    assertTrue(ContrastColors.contrastRatio(
                        treatment.getOutlineColor(),
                        palette.header
                    ) >= 4.5d);
                    assertTrue(treatment.getStrokeWidthDp() >= 3);
                }
            }
        }
    }

    @Test
    public void normalActiveFillKeepsExactPlayerColor() {
        for (Palette palette : PALETTES) {
            for (int player : PLAYER_COLORS) {
                ScoreChipColors.Treatment treatment = ScoreChipColors.treatment(
                    player,
                    palette.header,
                    palette.surface,
                    true,
                    false,
                    palette.dark
                );
                assertEquals(player, treatment.getFillColor());
                assertEquals(2, treatment.getStrokeWidthDp());
            }
        }
    }

    @Test
    public void inactiveFillIsQuietOpaqueAndStillPlayerSpecific() {
        Palette palette = PALETTES[0];
        ScoreChipColors.Treatment violet = ScoreChipColors.treatment(
            PLAYER_COLORS[0], palette.header, palette.surface, false, false, true
        );
        ScoreChipColors.Treatment orange = ScoreChipColors.treatment(
            PLAYER_COLORS[3], palette.header, palette.surface, false, false, true
        );

        assertEquals(0xFF, violet.getFillColor() >>> 24);
        assertEquals(0xFF, orange.getFillColor() >>> 24);
        assertNotEquals(violet.getFillColor(), orange.getFillColor());
        assertNotEquals(PLAYER_COLORS[0], violet.getFillColor());
        assertEquals(1, violet.getStrokeWidthDp());
    }

    @Test
    public void activeAndInactiveTreatmentsStayVisuallyDistinct() {
        for (Palette palette : PALETTES) {
            for (int player : PLAYER_COLORS) {
                ScoreChipColors.Treatment active = ScoreChipColors.treatment(
                    player,
                    palette.header,
                    palette.surface,
                    true,
                    false,
                    palette.dark
                );
                ScoreChipColors.Treatment inactive = ScoreChipColors.treatment(
                    player,
                    palette.header,
                    palette.surface,
                    false,
                    false,
                    palette.dark
                );

                assertNotEquals(active.getFillColor(), inactive.getFillColor());
                assertTrue(active.getStrokeWidthDp() > inactive.getStrokeWidthDp());
            }
        }
    }

    @Test
    public void allPublishedColorsAreOpaqueEvenForTransparentInputs() {
        ScoreChipColors.Treatment treatment = ScoreChipColors.treatment(
            0x005B4BDB,
            0x00162526,
            0x001D3048,
            true,
            false,
            true
        );

        assertEquals(0xFF, treatment.getFillColor() >>> 24);
        assertEquals(0xFF, treatment.getTextColor() >>> 24);
        assertEquals(0xFF, treatment.getOutlineColor() >>> 24);
    }

    private static final class Palette {
        final int header;
        final int surface;
        final boolean dark;

        Palette(int header, int surface, boolean dark) {
            this.header = header;
            this.surface = surface;
            this.dark = dark;
        }
    }
}
