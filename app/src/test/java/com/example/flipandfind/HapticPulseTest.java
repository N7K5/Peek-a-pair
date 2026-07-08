package com.example.flipandfind;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;

public final class HapticPulseTest {
    @Test
    public void everyPulseHasAlignedValidEnvelope() {
        for (HapticPulse pulse : HapticPulse.values()) {
            long[] timings = pulse.copyTimings();
            int[] amplitudes = pulse.copyAmplitudes();

            assertTrue(timings.length > 0);
            assertTrue(timings.length == amplitudes.length);
            for (int index = 0; index < timings.length; index++) {
                assertTrue(timings[index] >= 0L);
                assertTrue(amplitudes[index] >= 0);
                assertTrue(amplitudes[index] <= 255);
            }
        }
    }

    @Test
    public void gameplayEventsRemainTactilelyDistinct() {
        assertFalse(Arrays.equals(
            HapticPulse.CARD_FLIP.copyTimings(),
            HapticPulse.MATCH.copyTimings()
        ));
        assertFalse(Arrays.equals(
            HapticPulse.MATCH.copyTimings(),
            HapticPulse.MISS.copyTimings()
        ));
        assertTrue(
            maximum(HapticPulse.MATCH.copyAmplitudes())
                > maximum(HapticPulse.CARD_FLIP.copyAmplitudes())
        );
    }

    @Test
    public void pulseArraysCannotBeMutatedByCallers() {
        long[] expectedTimings = HapticPulse.MATCH.copyTimings();
        int[] expectedAmplitudes = HapticPulse.MATCH.copyAmplitudes();
        HapticPulse.MATCH.copyTimings()[0] = 99L;
        HapticPulse.MATCH.copyAmplitudes()[0] = 255;

        assertArrayEquals(expectedTimings, HapticPulse.MATCH.copyTimings());
        assertArrayEquals(expectedAmplitudes, HapticPulse.MATCH.copyAmplitudes());
    }

    private static int maximum(int[] values) {
        int maximum = Integer.MIN_VALUE;
        for (int value : values) {
            maximum = Math.max(maximum, value);
        }
        return maximum;
    }
}
