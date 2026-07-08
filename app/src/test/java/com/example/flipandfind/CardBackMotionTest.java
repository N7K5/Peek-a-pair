package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class CardBackMotionTest {
    @Test
    public void phaseIsBoundedPeriodicAndHandlesInvalidPeriods() {
        assertEquals(0f, CardBackMotion.phase(50L, 0L), 0f);
        assertEquals(0.25f, CardBackMotion.phase(250L, 1000L), 0f);
        assertEquals(
            CardBackMotion.phase(375L, 1000L),
            CardBackMotion.phase(3375L, 1000L),
            0f
        );
        for (long time = -2500L; time <= 2500L; time += 37L) {
            float phase = CardBackMotion.phase(time, 913L);
            assertTrue(phase >= 0f);
            assertTrue(phase < 1f);
        }
    }

    @Test
    public void sineAndPulseStayInsideTheirDrawingRanges() {
        for (int step = 0; step <= 1000; step++) {
            float phase = step / 1000f;
            assertTrue(CardBackMotion.sine(phase) >= -1.00001f);
            assertTrue(CardBackMotion.sine(phase) <= 1.00001f);
            assertTrue(CardBackMotion.pulse(phase) >= 0f);
            assertTrue(CardBackMotion.pulse(phase) <= 1f);
        }
    }

    @Test
    public void wrapKeepsStaggeredAnimationPhasesBounded() {
        assertEquals(0.25f, CardBackMotion.wrap(2.25f), 0.00001f);
        assertEquals(0.75f, CardBackMotion.wrap(-0.25f), 0.00001f);
        for (int step = -1000; step <= 1000; step++) {
            float wrapped = CardBackMotion.wrap(step / 37f);
            assertTrue(wrapped >= 0f);
            assertTrue(wrapped < 1f);
        }
    }

    @Test
    public void stableUnitIsDeterministicBoundedAndVariesAcrossCards() {
        float first = CardBackMotion.stableUnit(7, 2, 41);
        assertEquals(first, CardBackMotion.stableUnit(7, 2, 41), 0f);
        boolean foundDifferentCard = false;
        boolean foundDifferentElement = false;
        for (int card = 0; card < 80; card++) {
            for (int element = 0; element < 8; element++) {
                float value = CardBackMotion.stableUnit(card, element, 41);
                assertTrue(value >= 0f);
                assertTrue(value < 1f);
                foundDifferentCard |= value != CardBackMotion.stableUnit(card + 1, element, 41);
                foundDifferentElement |= value != CardBackMotion.stableUnit(card, element + 1, 41);
            }
        }
        assertTrue(foundDifferentCard);
        assertTrue(foundDifferentElement);
    }
}
