package com.example.flipandfind;

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
}
