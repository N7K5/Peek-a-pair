package com.example.flipandfind;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public final class ConstellationPatternTest {
    @Test
    public void samePhysicalCardAlwaysGetsTheSamePattern() {
        float[] firstX = new float[ConstellationPattern.POINT_COUNT];
        float[] firstY = new float[ConstellationPattern.POINT_COUNT];
        float[] secondX = new float[ConstellationPattern.POINT_COUNT];
        float[] secondY = new float[ConstellationPattern.POINT_COUNT];

        int firstAccent = ConstellationPattern.fill(37, firstX, firstY);
        int secondAccent = ConstellationPattern.fill(37, secondX, secondY);

        assertEquals(firstAccent, secondAccent);
        assertArrayEquals(firstX, secondX, 0f);
        assertArrayEquals(firstY, secondY, 0f);
    }

    @Test
    public void everySupportedPhysicalCardGetsADistinctFingerprint() {
        Set<String> fingerprints = new HashSet<>();
        for (int cardNumber = 1; cardNumber <= 102; cardNumber++) {
            float[] x = new float[ConstellationPattern.POINT_COUNT];
            float[] y = new float[ConstellationPattern.POINT_COUNT];
            int accent = ConstellationPattern.fill(cardNumber, x, y);
            StringBuilder fingerprint = new StringBuilder().append(accent);
            for (int index = 0; index < ConstellationPattern.POINT_COUNT; index++) {
                fingerprint.append(':').append(Float.floatToIntBits(x[index]));
                fingerprint.append(':').append(Float.floatToIntBits(y[index]));
            }
            assertTrue(fingerprints.add(fingerprint.toString()));
        }
        assertEquals(102, fingerprints.size());
    }

    @Test
    public void pointsStayInsideTheCardAndApartFromEachOther() {
        float minimumDistanceSquared = ConstellationPattern.MIN_POINT_DISTANCE
            * ConstellationPattern.MIN_POINT_DISTANCE;
        for (int cardNumber = -3; cardNumber <= 102; cardNumber++) {
            float[] x = new float[ConstellationPattern.POINT_COUNT];
            float[] y = new float[ConstellationPattern.POINT_COUNT];
            int accent = ConstellationPattern.fill(cardNumber, x, y);

            assertTrue(accent >= 0 && accent < ConstellationPattern.POINT_COUNT);
            for (int first = 0; first < ConstellationPattern.POINT_COUNT; first++) {
                assertTrue(Float.isFinite(x[first]));
                assertTrue(Float.isFinite(y[first]));
                assertTrue(Math.abs(x[first]) <= ConstellationPattern.MAX_ABS_X);
                assertTrue(Math.abs(y[first]) <= ConstellationPattern.MAX_ABS_Y);
                for (int second = first + 1;
                    second < ConstellationPattern.POINT_COUNT;
                    second++) {
                    float deltaX = x[first] - x[second];
                    float deltaY = y[first] - y[second];
                    assertTrue(
                        deltaX * deltaX + deltaY * deltaY
                            >= minimumDistanceSquared - 0.000001f
                    );
                }
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsCoordinateArraysThatCannotHoldThePattern() {
        ConstellationPattern.fill(1, new float[5], new float[6]);
    }
}
