package com.example.flipandfind;

/** Stable per-card geometry for the Prism and Waves card backs. */
final class CardBackGeometry {
    private static final float[] PRISM_APEX_X_LEVELS = {
        -0.10f, 0f, 0.10f
    };
    private static final float[] PRISM_APEX_Y_LEVELS = {-0.14f, -0.05f};
    private static final float[] PRISM_BOTTOM_X_LEVELS = {
        -0.10f, 0f, 0.10f
    };
    private static final float[] WAVE_SPACING_LEVELS = {0.12f, 0.18f, 0.24f};
    private static final float[] WAVE_AMPLITUDE_LEVELS = {0.09f, 0.15f, 0.21f};
    private static final int[] BOTANICAL_SIDE_MASKS = {
        0b0101, 0b1010, 0b0011, 0b1100,
        0b0110, 0b1001, 0b0001, 0b1110
    };
    private static final float[] BOTANICAL_ANCHOR_T = {0.26f, 0.44f, 0.62f, 0.80f};

    static final int BOTANICAL_LEAF_COUNT = 4;
    static final float BOTANICAL_STEM_CURVE = 0.08f;
    static final float BOTANICAL_LEAF_BASE_OFFSET = 0.045f;
    static final float BOTANICAL_LEAF_LENGTH_X = 0.135f;
    static final float BOTANICAL_LEAF_LENGTH_Y = -0.055f;
    static final float BOTANICAL_LEAF_HALF_WIDTH = 0.038f;
    static final float BOTANICAL_TERMINAL_LENGTH_X = 0.11f;
    static final float BOTANICAL_TERMINAL_LENGTH_Y = -0.06f;

    final float prismApexX;
    final float prismApexY;
    final float prismBottomX;
    final float prismLeftKneeY;
    final float prismRightKneeY;
    final boolean prismPaletteFlipped;

    final float waveSpacing;
    final float waveAmplitude;
    final float waveTailBend;
    final float waveDirection;
    final int waveAccentBand;

    final int botanicalSideMask;
    final int botanicalAccentLeaf;
    final float botanicalStemDirection;
    final float botanicalTerminalDirection;

    private CardBackGeometry(
        float prismApexX,
        float prismApexY,
        float prismBottomX,
        float prismLeftKneeY,
        float prismRightKneeY,
        boolean prismPaletteFlipped,
        float waveSpacing,
        float waveAmplitude,
        float waveTailBend,
        float waveDirection,
        int waveAccentBand,
        int botanicalSideMask,
        int botanicalAccentLeaf,
        float botanicalStemDirection,
        float botanicalTerminalDirection
    ) {
        this.prismApexX = prismApexX;
        this.prismApexY = prismApexY;
        this.prismBottomX = prismBottomX;
        this.prismLeftKneeY = prismLeftKneeY;
        this.prismRightKneeY = prismRightKneeY;
        this.prismPaletteFlipped = prismPaletteFlipped;
        this.waveSpacing = waveSpacing;
        this.waveAmplitude = waveAmplitude;
        this.waveTailBend = waveTailBend;
        this.waveDirection = waveDirection;
        this.waveAccentBand = waveAccentBand;
        this.botanicalSideMask = botanicalSideMask;
        this.botanicalAccentLeaf = botanicalAccentLeaf;
        this.botanicalStemDirection = botanicalStemDirection;
        this.botanicalTerminalDirection = botanicalTerminalDirection;
    }

    static CardBackGeometry forCard(int cardNumber) {
        int prismIdentity = (int) Math.floorMod((long) cardNumber - 1L, 108L);
        boolean prismPaletteFlipped = prismIdentity % 2 != 0;
        prismIdentity /= 2;
        int prismApexLevel = prismIdentity % 3;
        prismIdentity /= 3;
        int prismBottomLevel = prismIdentity % 3;
        prismIdentity /= 3;
        int prismKneeLayout = prismIdentity % 3;
        prismIdentity /= 3;
        int prismApexYLevel = prismIdentity % 2;

        float prismLeftKneeY;
        float prismRightKneeY;
        if (prismKneeLayout == 0) {
            prismLeftKneeY = 0.05f;
            prismRightKneeY = 0.15f;
        } else if (prismKneeLayout == 1) {
            prismLeftKneeY = 0.15f;
            prismRightKneeY = 0.05f;
        } else {
            prismLeftKneeY = 0.10f;
            prismRightKneeY = 0.10f;
        }

        int waveIdentity = (int) Math.floorMod((long) cardNumber - 1L, 108L);
        float waveDirection = waveIdentity % 2 == 0 ? -1f : 1f;
        waveIdentity /= 2;
        int waveAccentBand = waveIdentity % 3;
        waveIdentity /= 3;
        int waveSpacingLevel = waveIdentity % 3;
        waveIdentity /= 3;
        int waveAmplitudeLevel = waveIdentity % 3;
        waveIdentity /= 3;
        float waveTailBend = waveIdentity % 2 == 0 ? -0.075f : 0.075f;

        int botanicalIdentity = (int) Math.floorMod((long) cardNumber - 1L, 128L);
        botanicalIdentity = (botanicalIdentity * 73 + 19) & 127;
        int botanicalSideMask = BOTANICAL_SIDE_MASKS[botanicalIdentity % 8];
        botanicalIdentity /= 8;
        int botanicalAccentLeaf = botanicalIdentity % BOTANICAL_LEAF_COUNT;
        botanicalIdentity /= BOTANICAL_LEAF_COUNT;
        float botanicalStemDirection = botanicalIdentity % 2 == 0 ? -1f : 1f;
        botanicalIdentity /= 2;
        float botanicalTerminalDirection = botanicalIdentity % 2 == 0 ? -1f : 1f;

        return new CardBackGeometry(
            PRISM_APEX_X_LEVELS[prismApexLevel],
            PRISM_APEX_Y_LEVELS[prismApexYLevel],
            PRISM_BOTTOM_X_LEVELS[prismBottomLevel],
            prismLeftKneeY,
            prismRightKneeY,
            prismPaletteFlipped,
            WAVE_SPACING_LEVELS[waveSpacingLevel],
            WAVE_AMPLITUDE_LEVELS[waveAmplitudeLevel],
            waveTailBend,
            waveDirection,
            waveAccentBand,
            botanicalSideMask,
            botanicalAccentLeaf,
            botanicalStemDirection,
            botanicalTerminalDirection
        );
    }

    int botanicalSide(int leafIndex) {
        if (leafIndex < 0 || leafIndex >= BOTANICAL_LEAF_COUNT) {
            throw new IllegalArgumentException("Botanical leaf index is out of range");
        }
        return ((botanicalSideMask >>> leafIndex) & 1) == 0 ? -1 : 1;
    }

    float botanicalAnchorY(int leafIndex) {
        float t = BOTANICAL_ANCHOR_T[leafIndex];
        return cubicPoint(t, 0.34f, 0.13f, -0.12f, -0.30f);
    }

    float botanicalStemX(int leafIndex) {
        float curve = botanicalStemDirection * BOTANICAL_STEM_CURVE;
        float t = BOTANICAL_ANCHOR_T[leafIndex];
        return cubicPoint(t, -curve, -curve * 0.45f, curve * 0.35f, curve);
    }

    private static float cubicPoint(
        float t,
        float start,
        float firstControl,
        float secondControl,
        float end
    ) {
        float inverse = 1f - t;
        return inverse * inverse * inverse * start
            + 3f * inverse * inverse * t * firstControl
            + 3f * inverse * t * t * secondControl
            + t * t * t * end;
    }
}
