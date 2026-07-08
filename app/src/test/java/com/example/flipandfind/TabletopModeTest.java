package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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
}
