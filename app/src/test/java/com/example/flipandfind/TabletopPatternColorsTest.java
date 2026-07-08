package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public final class TabletopPatternColorsTest {
    @Test
    public void allPatternColorsAreOpaque() {
        int tabletop = 0xFF172526;
        int player = 0xFF00A896;

        assertEquals(0xFF, TabletopPatternColors.primary(tabletop, player, true) >>> 24);
        assertEquals(0xFF, TabletopPatternColors.secondary(tabletop, player, true) >>> 24);
        assertEquals(0xFF, TabletopPatternColors.highlight(tabletop, true) >>> 24);
    }

    @Test
    public void patternLayersRemainVisuallyDistinct() {
        int tabletop = 0xFF172526;
        int player = 0xFF00A896;

        int primary = TabletopPatternColors.primary(tabletop, player, true);
        int secondary = TabletopPatternColors.secondary(tabletop, player, true);
        int highlight = TabletopPatternColors.highlight(tabletop, true);

        assertNotEquals(tabletop, primary);
        assertNotEquals(primary, secondary);
        assertNotEquals(secondary, highlight);
    }

    @Test
    public void colorsRespondToThemeAndPlayer() {
        int darkTabletop = 0xFF172526;
        int lightTabletop = 0xFFF4F9F8;

        assertNotEquals(
            TabletopPatternColors.primary(darkTabletop, 0xFF5B4BDB, true),
            TabletopPatternColors.primary(darkTabletop, 0xFFF59E0B, true)
        );
        assertNotEquals(
            TabletopPatternColors.highlight(darkTabletop, true),
            TabletopPatternColors.highlight(lightTabletop, false)
        );
    }
}
