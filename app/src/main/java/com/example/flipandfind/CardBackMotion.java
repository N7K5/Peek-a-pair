package com.example.flipandfind;

/** Pure timing helpers shared by Canvas-only animated card backs. */
final class CardBackMotion {
    private CardBackMotion() {
    }

    static float phase(long uptimeMillis, long periodMillis) {
        if (periodMillis <= 0L) {
            return 0f;
        }
        return (float) Math.floorMod(uptimeMillis, periodMillis) / periodMillis;
    }

    static float sine(float phase) {
        return (float) Math.sin(phase * Math.PI * 2d);
    }

    static float pulse(float phase) {
        return 0.5f + sine(phase) * 0.5f;
    }

    /** Wraps any finite phase into the half-open drawing interval [0, 1). */
    static float wrap(float value) {
        return value - (float) Math.floor(value);
    }

    /** Stable, allocation-free variation for decorative elements on a physical card. */
    static float stableUnit(int cardNumber, int elementIndex, int salt) {
        int mixed = cardNumber * 0x45D9F3B;
        mixed ^= elementIndex * 0x119DE1F3;
        mixed ^= salt * 0x27D4EB2D;
        mixed = (mixed ^ (mixed >>> 16)) * 0x45D9F3B;
        mixed ^= mixed >>> 16;
        return (mixed & 0x7FFFFFFF) / 2147483648f;
    }
}
