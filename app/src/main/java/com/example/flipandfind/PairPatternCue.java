package com.example.flipandfind;

/** Pure, stable color-blind pattern identity for the fifty supported pair IDs. */
final class PairPatternCue {
    static final int FAMILY_COUNT = 10;
    static final int VARIANT_COUNT = 5;
    static final int CUE_COUNT = FAMILY_COUNT * VARIANT_COUNT;

    private static final String[] FAMILY_NAMES = {
        "dots", "rings", "squares", "diamonds", "triangles",
        "pluses", "crosses", "bars", "posts", "chevrons"
    };

    private PairPatternCue() {
    }

    static int familyFor(int pairId) {
        return normalizedId(pairId) % FAMILY_COUNT;
    }

    static int variantFor(int pairId) {
        return normalizedId(pairId) / FAMILY_COUNT;
    }

    static int markerCountFor(int pairId) {
        return variantFor(pairId) + 1;
    }

    static String spokenNameFor(int pairId) {
        return FAMILY_NAMES[familyFor(pairId)] + " " + markerCountFor(pairId);
    }

    private static int normalizedId(int pairId) {
        return Math.floorMod(pairId, CUE_COUNT);
    }
}
