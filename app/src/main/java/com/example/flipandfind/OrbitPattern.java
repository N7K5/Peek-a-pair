package com.example.flipandfind;

/** Stable orbit geometry derived only from a physical card number. */
final class OrbitPattern {
    final float firstRadiusX;
    final float firstRadiusY;
    final float secondRadiusX;
    final float secondRadiusY;
    final float firstTiltDegrees;
    final float secondTiltDegrees;
    final float firstPlanetDegrees;
    final float secondPlanetDegrees;
    final float speedDegreesPerSecond;
    final float direction;

    private OrbitPattern(
        float firstRadiusX,
        float firstRadiusY,
        float secondRadiusX,
        float secondRadiusY,
        float firstTiltDegrees,
        float secondTiltDegrees,
        float firstPlanetDegrees,
        float secondPlanetDegrees,
        float speedDegreesPerSecond,
        float direction
    ) {
        this.firstRadiusX = firstRadiusX;
        this.firstRadiusY = firstRadiusY;
        this.secondRadiusX = secondRadiusX;
        this.secondRadiusY = secondRadiusY;
        this.firstTiltDegrees = firstTiltDegrees;
        this.secondTiltDegrees = secondTiltDegrees;
        this.firstPlanetDegrees = firstPlanetDegrees;
        this.secondPlanetDegrees = secondPlanetDegrees;
        this.speedDegreesPerSecond = speedDegreesPerSecond;
        this.direction = direction;
    }

    static OrbitPattern forCard(int cardNumber) {
        long seed = mix64(0x243f6a8885a308d3L ^ (long) cardNumber * 0x9e3779b97f4a7c15L);
        return new OrbitPattern(
            between(seed, 0, 0.29f, 0.36f),
            between(seed, 1, 0.09f, 0.15f),
            between(seed, 2, 0.27f, 0.35f),
            between(seed, 3, 0.10f, 0.16f),
            between(seed, 4, 14f, 44f),
            -between(seed, 5, 14f, 44f),
            between(seed, 6, 0f, 360f),
            between(seed, 7, 0f, 360f),
            between(seed, 8, 7f, 13f),
            unit(seed, 9) < 0.5f ? -1f : 1f
        );
    }

    private static float between(long seed, int channel, float minimum, float maximum) {
        return minimum + unit(seed, channel) * (maximum - minimum);
    }

    private static float unit(long seed, int channel) {
        long mixed = mix64(seed + (long) (channel + 1) * 0xbf58476d1ce4e5b9L);
        return (float) ((mixed >>> 40) & 0xFFFFFFL) / 0x1000000L;
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
