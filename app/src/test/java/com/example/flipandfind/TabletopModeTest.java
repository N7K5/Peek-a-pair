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
        assertEquals(2, TabletopMode.TILES.ordinal());
        assertEquals(3, TabletopMode.MOSAIC.ordinal());
        assertEquals(4, TabletopMode.GLASS_TILES.ordinal());
        assertEquals(5, TabletopMode.DOTS.ordinal());
        assertEquals(6, TabletopMode.CONTOURS.ordinal());
    }

    @Test
    public void newPatternModesAreAppendOnlyAndHaveSelectorMetadata() {
        assertEquals(17, TabletopMode.values().length);
        assertEquals("tiles", TabletopMode.TILES.getPreferenceId());
        assertEquals("mosaic", TabletopMode.MOSAIC.getPreferenceId());
        assertEquals("glass_tiles", TabletopMode.GLASS_TILES.getPreferenceId());
        assertEquals("dots", TabletopMode.DOTS.getPreferenceId());
        assertEquals("contours", TabletopMode.CONTOURS.getPreferenceId());
        assertEquals(7, TabletopMode.HONEYCOMB.ordinal());
        assertEquals("honeycomb", TabletopMode.HONEYCOMB.getPreferenceId());
        assertEquals(8, TabletopMode.GLASS.ordinal());
        assertEquals("glass", TabletopMode.GLASS.getPreferenceId());
        assertEquals(9, TabletopMode.REFLECTION.ordinal());
        assertEquals("reflection", TabletopMode.REFLECTION.getPreferenceId());
        assertEquals(TabletopMode.Pattern.HONEYCOMB, TabletopMode.HONEYCOMB.getPattern());
        assertEquals(TabletopMode.Pattern.GLASS, TabletopMode.GLASS.getPattern());
        assertEquals(TabletopMode.Pattern.REFLECTION, TabletopMode.REFLECTION.getPattern());
        assertEquals(10, TabletopMode.PAPER.ordinal());
        assertEquals("paper", TabletopMode.PAPER.getPreferenceId());
        assertEquals(TabletopMode.Pattern.PAPER, TabletopMode.PAPER.getPattern());
        assertEquals(11, TabletopMode.MIRROR.ordinal());
        assertEquals("mirror", TabletopMode.MIRROR.getPreferenceId());
        assertEquals(TabletopMode.Pattern.MIRROR, TabletopMode.MIRROR.getPattern());
        assertEquals(12, TabletopMode.WATER.ordinal());
        assertEquals("water", TabletopMode.WATER.getPreferenceId());
        assertEquals(TabletopMode.Pattern.WATER, TabletopMode.WATER.getPattern());
        assertEquals(13, TabletopMode.RETRO.ordinal());
        assertEquals("retro", TabletopMode.RETRO.getPreferenceId());
        assertEquals(TabletopMode.Pattern.RETRO, TabletopMode.RETRO.getPattern());
        assertEquals(14, TabletopMode.NEON.ordinal());
        assertEquals("neon", TabletopMode.NEON.getPreferenceId());
        assertEquals(TabletopMode.Pattern.NEON, TabletopMode.NEON.getPattern());
        assertEquals(15, TabletopMode.FELT.ordinal());
        assertEquals("felt", TabletopMode.FELT.getPreferenceId());
        assertEquals(TabletopMode.Pattern.FELT, TabletopMode.FELT.getPattern());
        assertEquals(16, TabletopMode.RANDOM_EACH_GAME.ordinal());
        assertEquals("random_each_game", TabletopMode.RANDOM_EACH_GAME.getPreferenceId());

        assertFalse(TabletopMode.STATIC_THEME.hasPattern());
        assertFalse(TabletopMode.PLAYER_TINT.hasPattern());
        for (TabletopMode mode : TabletopMode.values()) {
            assertFalse(mode.getDisplayName().isEmpty());
            assertFalse(mode.getDescription().isEmpty());
            if (mode.isConcrete() && mode.ordinal() >= TabletopMode.TILES.ordinal()) {
                assertTrue(mode.hasPattern());
                assertTrue(mode.isPlayerTinted());
            }
        }
    }

    @Test
    public void selectionMetadataSeparatesConcreteAndRandomModes() {
        TabletopMode[] concreteModes = TabletopMode.concreteModesCopy();

        assertEquals(16, concreteModes.length);
        for (TabletopMode mode : concreteModes) {
            assertTrue(mode.isConcrete());
            assertFalse(mode.isRandomEachGame());
            assertEquals(TabletopMode.SelectionType.CONCRETE, mode.getSelectionType());
            assertNotEquals(TabletopMode.RANDOM_EACH_GAME, mode);
        }
        assertFalse(TabletopMode.RANDOM_EACH_GAME.isConcrete());
        assertTrue(TabletopMode.RANDOM_EACH_GAME.isRandomEachGame());
        assertEquals(
            TabletopMode.SelectionType.RANDOM_EACH_GAME,
            TabletopMode.RANDOM_EACH_GAME.getSelectionType()
        );

        concreteModes[0] = TabletopMode.RANDOM_EACH_GAME;
        assertEquals(TabletopMode.STATIC_THEME, TabletopMode.concreteModesCopy()[0]);
    }
}
