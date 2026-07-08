package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ContrastColorsTest {
    private static final int[] PLAYER_COLORS = {
        0xFF5B4BDB, 0xFFE84A5F, 0xFF00897B, 0xFFF59E0B, 0xFF3B82F6,
        0xFF8B5CF6, 0xFFD97706, 0xFF0F766E, 0xFFBE185D, 0xFF475569,
        0xFF0891B2, 0xFF65A30D, 0xFFC2410C, 0xFF7E22CE, 0xFF059669
    };

    @Test
    public void everyPlayerColorGetsAccessibleSmallText() {
        for (int playerColor : PLAYER_COLORS) {
            int textColor = ContrastColors.blackOrWhiteFor(playerColor);
            assertTrue(ContrastColors.contrastRatio(playerColor, textColor) >= 4.5);
        }
    }

    @Test
    public void adjustmentClearsRequestedRatioOnEveryLightBackground() {
        int[] backgrounds = {0xFFFFFFFF, 0xFFF2F4F7, 0xFFE4EAF0};
        int adjusted = ContrastColors.ensureMinimumContrast(
            0xFF8A93A2,
            ContrastColors.BLACK,
            4.5,
            backgrounds
        );

        for (int background : backgrounds) {
            assertTrue(ContrastColors.contrastRatio(adjusted, background) >= 4.5);
        }
    }

    @Test
    public void adjustmentClearsRequestedRatioOnEveryDarkBackground() {
        int[] backgrounds = {0xFF05070A, 0xFF17202A, 0xFF293746};
        int adjusted = ContrastColors.ensureMinimumContrast(
            0xFF526170,
            ContrastColors.WHITE,
            4.5,
            backgrounds
        );

        for (int background : backgrounds) {
            assertTrue(ContrastColors.contrastRatio(adjusted, background) >= 4.5);
        }
    }

    @Test
    public void alreadyAccessibleColorIsPreservedExactly() {
        int accessible = 0xFF102A43;
        assertEquals(
            accessible,
            ContrastColors.ensureMinimumContrast(
                accessible,
                ContrastColors.BLACK,
                4.5,
                0xFFFFFFFF
            )
        );
    }

    @Test
    public void everyCardBackDecorationCanReachGraphicalContrast() {
        for (CardBackStyle style : CardBackStyle.values()) {
            int fill = style.getFillColor();
            int target = ContrastColors.blackOrWhiteFor(fill);
            int pattern = ContrastColors.ensureMinimumContrast(
                style.getPatternColor(),
                target,
                3.0,
                fill
            );
            int accent = ContrastColors.ensureMinimumContrast(
                style.getAccentColor(),
                target,
                3.0,
                fill
            );
            assertTrue(ContrastColors.contrastRatio(pattern, fill) >= 3.0);
            assertTrue(ContrastColors.contrastRatio(accent, fill) >= 3.0);
        }
    }
}
