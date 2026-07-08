package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public final class OrbitPatternTest {
    @Test
    public void everySupportedCardHasAStableDistinctOrbitFingerprint() {
        Set<String> fingerprints = new HashSet<>();
        for (int cardNumber = 1; cardNumber <= 102; cardNumber++) {
            OrbitPattern first = OrbitPattern.forCard(cardNumber);
            OrbitPattern second = OrbitPattern.forCard(cardNumber);
            assertEquals(fingerprint(first), fingerprint(second));
            assertTrue(fingerprints.add(fingerprint(first)));
        }
        assertEquals(102, fingerprints.size());
    }

    @Test
    public void generatedOrbitGeometryStaysInsideSafeBounds() {
        for (int cardNumber = -3; cardNumber <= 102; cardNumber++) {
            OrbitPattern pattern = OrbitPattern.forCard(cardNumber);
            assertBetween(pattern.firstRadiusX, 0.29f, 0.36f);
            assertBetween(pattern.firstRadiusY, 0.09f, 0.15f);
            assertBetween(pattern.secondRadiusX, 0.27f, 0.35f);
            assertBetween(pattern.secondRadiusY, 0.10f, 0.16f);
            assertBetween(pattern.firstTiltDegrees, 14f, 44f);
            assertBetween(pattern.secondTiltDegrees, -44f, -14f);
            assertBetween(pattern.firstPlanetDegrees, 0f, 360f);
            assertBetween(pattern.secondPlanetDegrees, 0f, 360f);
            assertBetween(pattern.speedDegreesPerSecond, 7f, 13f);
            assertTrue(pattern.direction == -1f || pattern.direction == 1f);

            assertOrbitEnvelope(
                pattern.firstRadiusX,
                pattern.firstRadiusY,
                pattern.firstTiltDegrees
            );
            assertOrbitEnvelope(
                pattern.secondRadiusX,
                pattern.secondRadiusY,
                pattern.secondTiltDegrees
            );
        }
    }

    private static void assertOrbitEnvelope(float radiusX, float radiusY, float tiltDegrees) {
        double tilt = Math.toRadians(tiltDegrees);
        for (int angleDegrees = 0; angleDegrees < 360; angleDegrees += 5) {
            double angle = Math.toRadians(angleDegrees);
            double localX = Math.cos(angle) * radiusX;
            double localY = Math.sin(angle) * radiusY;
            double x = localX * Math.cos(tilt) - localY * Math.sin(tilt);
            double y = localX * Math.sin(tilt) + localY * Math.cos(tilt);
            // Includes the largest rendered planet radius (0.035 of card width).
            assertTrue(Math.abs(x) + 0.035f < 0.45f);
            assertTrue(Math.abs(y) + 0.035f < 0.45f);
        }
    }

    private static void assertBetween(float value, float minimum, float maximum) {
        assertTrue(value >= minimum);
        assertTrue(value <= maximum);
    }

    private static String fingerprint(OrbitPattern pattern) {
        return Float.floatToIntBits(pattern.firstRadiusX)
            + ":" + Float.floatToIntBits(pattern.firstRadiusY)
            + ":" + Float.floatToIntBits(pattern.secondRadiusX)
            + ":" + Float.floatToIntBits(pattern.secondRadiusY)
            + ":" + Float.floatToIntBits(pattern.firstTiltDegrees)
            + ":" + Float.floatToIntBits(pattern.secondTiltDegrees)
            + ":" + Float.floatToIntBits(pattern.firstPlanetDegrees)
            + ":" + Float.floatToIntBits(pattern.secondPlanetDegrees)
            + ":" + Float.floatToIntBits(pattern.speedDegreesPerSecond)
            + ":" + Float.floatToIntBits(pattern.direction);
    }
}
