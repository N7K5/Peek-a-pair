package com.example.flipandfind;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class MissRevealDurationTest {
    @Test
    public void clampKeepsStoredValuesInsideSupportedRange() {
        assertEquals(5, MissRevealDuration.clampTenths(-100));
        assertEquals(5, MissRevealDuration.clampTenths(5));
        assertEquals(9, MissRevealDuration.clampTenths(9));
        assertEquals(30, MissRevealDuration.clampTenths(30));
        assertEquals(30, MissRevealDuration.clampTenths(100));
    }

    @Test
    public void progressMappingCoversEveryTenthFromHalfToThreeSeconds() {
        assertEquals(25, MissRevealDuration.maxProgress());
        for (int tenths = 5; tenths <= 30; tenths++) {
            int progress = MissRevealDuration.progressForTenths(tenths);
            assertEquals(tenths - 5, progress);
            assertEquals(tenths, MissRevealDuration.tenthsForProgress(progress));
        }
        assertEquals(5, MissRevealDuration.tenthsForProgress(-1));
        assertEquals(30, MissRevealDuration.tenthsForProgress(26));
    }

    @Test
    public void millisecondsAndLabelsUseOneTenthSecondSteps() {
        assertEquals(500L, MissRevealDuration.millisForTenths(5));
        assertEquals(900L, MissRevealDuration.millisForTenths(
            MissRevealDuration.DEFAULT_TENTHS
        ));
        assertEquals(3000L, MissRevealDuration.millisForTenths(30));
        assertEquals("0.5 seconds", MissRevealDuration.displayText(5));
        assertEquals("0.9 seconds", MissRevealDuration.displayText(9));
        assertEquals("1.0 seconds", MissRevealDuration.displayText(10));
        assertEquals("3.0 seconds", MissRevealDuration.displayText(30));
    }
}
