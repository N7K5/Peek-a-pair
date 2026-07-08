package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TabletopModeTest {
    @Test
    public void missingAndUnknownPreferencesUseStaticTheme() {
        assertEquals(TabletopMode.STATIC_THEME, TabletopMode.fromPreference(null));
        assertEquals(TabletopMode.STATIC_THEME, TabletopMode.fromPreference(""));
        assertEquals(TabletopMode.STATIC_THEME, TabletopMode.fromPreference("future_mode"));
    }

    @Test
    public void everyStableIdRoundTripsAndIsUnique() {
        for (TabletopMode mode : TabletopMode.values()) {
            assertEquals(mode, TabletopMode.fromPreference(mode.getPreferenceId()));
            for (TabletopMode other : TabletopMode.values()) {
                if (mode != other) {
                    assertNotEquals(mode.getPreferenceId(), other.getPreferenceId());
                }
            }
        }
    }

    @Test
    public void originalModesKeepTheirStableOrderAndIds() {
        assertEquals(0, TabletopMode.STATIC_THEME.ordinal());
        assertEquals("static_theme", TabletopMode.STATIC_THEME.getPreferenceId());
        assertEquals(1, TabletopMode.PLAYER_TINT.ordinal());
        assertEquals("player_tint", TabletopMode.PLAYER_TINT.getPreferenceId());
    }

    @Test
    public void newPatternModesAreAppendOnlyAndHaveSelectorMetadata() {
        assertEquals("tiles", TabletopMode.TILES.getPreferenceId());
        assertEquals("mosaic", TabletopMode.MOSAIC.getPreferenceId());
        assertEquals("glass_tiles", TabletopMode.GLASS_TILES.getPreferenceId());
        assertEquals("dots", TabletopMode.DOTS.getPreferenceId());
        assertEquals("contours", TabletopMode.CONTOURS.getPreferenceId());

        assertFalse(TabletopMode.STATIC_THEME.hasPattern());
        assertFalse(TabletopMode.PLAYER_TINT.hasPattern());
        for (TabletopMode mode : TabletopMode.values()) {
            assertFalse(mode.getDisplayName().isEmpty());
            assertFalse(mode.getDescription().isEmpty());
            if (mode.ordinal() >= TabletopMode.TILES.ordinal()) {
                assertTrue(mode.hasPattern());
                assertTrue(mode.isPlayerTinted());
            }
        }
    }
}
