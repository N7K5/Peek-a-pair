package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public final class CardBackModeTest {
    @Test
    public void stablePreferenceIdsRoundTrip() {
        assertMode(CardBackMode.FIXED, "fixed", "Choose one design");
        assertMode(
            CardBackMode.RANDOM_EACH_GAME,
            "random_each_game",
            "Random each game"
        );
        assertMode(
            CardBackMode.RANDOM_EACH_CARD,
            "random_each_card",
            "Random across cards"
        );
    }

    @Test
    public void modeIdsAreUnique() {
        for (CardBackMode mode : CardBackMode.values()) {
            for (CardBackMode other : CardBackMode.values()) {
                if (mode != other) {
                    assertNotEquals(mode.getPreferenceId(), other.getPreferenceId());
                }
            }
        }
    }

    @Test
    public void missingAndUnknownPreferencesKeepExistingFixedBehavior() {
        assertEquals(CardBackMode.FIXED, CardBackMode.fromPreference(null));
        assertEquals(CardBackMode.FIXED, CardBackMode.fromPreference(""));
        assertEquals(CardBackMode.FIXED, CardBackMode.fromPreference("future_mode"));
    }

    private static void assertMode(
        CardBackMode mode,
        String preferenceId,
        String displayName
    ) {
        assertEquals(preferenceId, mode.getPreferenceId());
        assertEquals(displayName, mode.getDisplayName());
        assertEquals(mode, CardBackMode.fromPreference(preferenceId));
    }
}
