package com.example.flipandfind;

/** Creates a stable, card-specific constellation without any draw-time randomness. */
final class ConstellationPattern {
    static final int POINT_COUNT = 6;
    static final float MAX_ABS_X = 0.29f;
    static final float MAX_ABS_Y = 0.33f;
    static final float MIN_POINT_DISTANCE = 0.105f;

    private static final int MAX_CANDIDATE_ATTEMPTS = 96;
    private static final long CARD_SALT = 0x6a09e667f3bcc909L;
    private static final long POINT_SALT = 0x9e3779b97f4a7c15L;
    private static final long ATTEMPT_SALT = 0xbf58476d1ce4e5b9L;

    private ConstellationPattern() {
    }

    /**
     * Fills normalized point coordinates and returns the highlighted star index.
     * The only identity input is the physical card number, so matching cards do not
     * accidentally receive matching backs.
     */
    static int fill(int cardNumber, float[] xCoordinates, float[] yCoordinates) {
        if (xCoordinates == null || yCoordinates == null
            || xCoordinates.length < POINT_COUNT || yCoordinates.length < POINT_COUNT) {
            throw new IllegalArgumentException("Constellation arrays must contain six points");
        }

        long cardSeed = mix64(CARD_SALT ^ (long) cardNumber * POINT_SALT);
        float minimumDistanceSquared = MIN_POINT_DISTANCE * MIN_POINT_DISTANCE;
        for (int point = 0; point < POINT_COUNT; point++) {
            boolean placed = false;
            for (int attempt = 0; attempt < MAX_CANDIDATE_ATTEMPTS; attempt++) {
                long candidateSeed = mix64(
                    cardSeed
                        + (long) (point + 1) * POINT_SALT
                        + (long) (attempt + 1) * ATTEMPT_SALT
                );
                float x = (unitFloat(candidateSeed) * 2f - 1f) * MAX_ABS_X;
                float y = (unitFloat(mix64(candidateSeed + CARD_SALT)) * 2f - 1f)
                    * MAX_ABS_Y;
                if (isFarEnough(x, y, xCoordinates, yCoordinates, point, minimumDistanceSquared)) {
                    xCoordinates[point] = x;
                    yCoordinates[point] = y;
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                // This deterministic ring is a practically unreachable safety fallback.
                double angle = point * Math.PI * 2d / POINT_COUNT
                    + unitFloat(cardSeed) * Math.PI;
                xCoordinates[point] = (float) Math.cos(angle) * MAX_ABS_X * 0.88f;
                yCoordinates[point] = (float) Math.sin(angle) * MAX_ABS_Y * 0.88f;
            }
        }
        return (int) Math.floorMod(mix64(cardSeed + ATTEMPT_SALT), POINT_COUNT);
    }

    private static boolean isFarEnough(
        float x,
        float y,
        float[] xCoordinates,
        float[] yCoordinates,
        int placedCount,
        float minimumDistanceSquared
    ) {
        for (int index = 0; index < placedCount; index++) {
            float deltaX = x - xCoordinates[index];
            float deltaY = y - yCoordinates[index];
            if (deltaX * deltaX + deltaY * deltaY < minimumDistanceSquared) {
                return false;
            }
        }
        return true;
    }

    private static float unitFloat(long value) {
        return (float) ((value >>> 40) & 0xFFFFFFL) / 0x1000000L;
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
