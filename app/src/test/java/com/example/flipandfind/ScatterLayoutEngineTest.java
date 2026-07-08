package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ScatterLayoutEngineTest {
    @Test
    public void layoutsAreStableAndNonOverlappingAcrossSizes() {
        int[] counts = {8, 16, 30, 60, 102};
        float[] widths = {280f, 330f, 600f};
        long[] seeds = {41L, 12345L};

        for (float width : widths) {
            for (int count : counts) {
                for (long seed : seeds) {
                    float cardWidth = Math.max(48f, width * cardFraction(count));
                    float cardHeight = cardWidth * 1.22f;
                    float edge = Math.max(8f, cardWidth * 0.16f);
                    float spacing = Math.max(7f, cardWidth * 0.17f);
                    ScatterLayoutEngine.Result result = ScatterLayoutEngine.generate(
                        count,
                        width,
                        cardWidth,
                        cardHeight,
                        edge,
                        spacing,
                        seed
                    );

                    assertLayoutIsValid(result, count, width, cardWidth, cardHeight, spacing);

                    ScatterLayoutEngine.Result repeated = ScatterLayoutEngine.generate(
                        count,
                        width,
                        cardWidth,
                        cardHeight,
                        edge,
                        spacing,
                        seed
                    );
                    assertEquals(result.contentHeight, repeated.contentHeight, 0f);
                    for (int index = 0; index < count; index++) {
                        assertEquals(result.lefts[index], repeated.lefts[index], 0f);
                        assertEquals(result.tops[index], repeated.tops[index], 0f);
                        assertEquals(result.rotations[index], repeated.rotations[index], 0f);
                    }
                }
            }
        }
    }

    @Test
    public void changingTheSeedChangesTheScatter() {
        ScatterLayoutEngine.Result first = ScatterLayoutEngine.generate(
            20, 330f, 62f, 76f, 10f, 10f, 1L
        );
        ScatterLayoutEngine.Result second = ScatterLayoutEngine.generate(
            20, 330f, 62f, 76f, 10f, 10f, 2L
        );

        boolean anyPositionChanged = false;
        for (int index = 0; index < 20; index++) {
            if (first.lefts[index] != second.lefts[index]
                || first.tops[index] != second.tops[index]) {
                anyPositionChanged = true;
                break;
            }
        }
        assertTrue(anyPositionChanged);
    }

    @Test
    public void fixedViewportsFitWithoutScrolling() {
        int[] counts = {8, 16, 30, 60, 102};
        float[][] viewports = {
            {280f, 500f},
            {330f, 420f},
            {600f, 180f},
            {1000f, 600f}
        };
        long[] seeds = {7L, 99L, 987654L};

        for (float[] viewport : viewports) {
            for (int count : counts) {
                for (long seed : seeds) {
                    float cardWidth = (float) Math.sqrt(
                        viewport[0] * viewport[1] * 0.30f / (count * 1.22f)
                    );
                    ScatterLayoutEngine.Result result = null;
                    float spacing = 0f;
                    for (int attempt = 0; attempt < 12 && result == null; attempt++) {
                        float cardHeight = cardWidth * 1.22f;
                        float edge = Math.max(2f, cardWidth * 0.10f);
                        spacing = Math.max(1f, cardWidth * 0.14f);
                        result = ScatterLayoutEngine.generateWithin(
                            count,
                            viewport[0],
                            viewport[1],
                            cardWidth,
                            cardHeight,
                            edge,
                            spacing,
                            seed
                        );
                        if (result == null) {
                            cardWidth *= 0.92f;
                        }
                    }

                    assertTrue(result != null);
                    assertEquals(viewport[1], result.contentHeight, 0f);
                    assertLayoutIsValid(
                        result,
                        count,
                        viewport[0],
                        cardWidth,
                        cardWidth * 1.22f,
                        spacing
                    );
                }
            }
        }
    }

    @Test
    public void impossibleRandomPackingReturnsNullInsteadOfOverlapping() {
        ScatterLayoutEngine.Result result = ScatterLayoutEngine.generateWithin(
            60, 20f, 5f, 10f, 12f, 1f, 1f, 3L
        );
        assertTrue(result == null);
    }

    @Test
    public void scatteredCardsUseTheStrongerTenDegreeTilt() {
        assertEquals(10f, ScatterLayoutEngine.MAX_ROTATION_DEGREES, 0f);
        ScatterLayoutEngine.Result result = ScatterLayoutEngine.latticeWithin(
            102,
            1000f,
            900f,
            38f,
            47f,
            12,
            20260707L
        );
        boolean hasStrongClockwiseTilt = false;
        boolean hasStrongCounterClockwiseTilt = false;
        for (float rotation : result.rotations) {
            assertTrue(rotation >= -10f);
            assertTrue(rotation <= 10f);
            hasStrongClockwiseTilt |= rotation > 7f;
            hasStrongCounterClockwiseTilt |= rotation < -7f;
        }
        assertTrue(hasStrongClockwiseTilt);
        assertTrue(hasStrongCounterClockwiseTilt);
    }

    private static void assertLayoutIsValid(
        ScatterLayoutEngine.Result result,
        int count,
        float width,
        float cardWidth,
        float cardHeight,
        float spacing
    ) {
        for (int index = 0; index < count; index++) {
            assertTrue(result.lefts[index] >= 0f);
            assertTrue(result.tops[index] >= 0f);
            assertTrue(result.lefts[index] + cardWidth <= width + 0.01f);
            assertTrue(result.tops[index] + cardHeight <= result.contentHeight + 0.01f);

            RotatedBounds first = rotatedBounds(
                result.lefts[index],
                result.tops[index],
                cardWidth,
                cardHeight,
                result.rotations[index]
            );
            assertTrue(first.left >= -0.01f);
            assertTrue(first.right <= width + 0.01f);
            assertTrue(first.top >= -0.01f);
            assertTrue(first.bottom <= result.contentHeight + 0.01f);

            for (int other = index + 1; other < count; other++) {
                assertFalse(rectanglesOverlapWithSpacing(
                    result.lefts[index],
                    result.tops[index],
                    result.lefts[other],
                    result.tops[other],
                    cardWidth,
                    cardHeight,
                    spacing - 0.01f
                ));

                RotatedBounds second = rotatedBounds(
                    result.lefts[other],
                    result.tops[other],
                    cardWidth,
                    cardHeight,
                    result.rotations[other]
                );
                assertFalse(first.overlaps(second));
            }
        }
    }

    private static boolean rectanglesOverlapWithSpacing(
        float firstLeft,
        float firstTop,
        float secondLeft,
        float secondTop,
        float width,
        float height,
        float spacing
    ) {
        return firstLeft - spacing < secondLeft + width
            && firstLeft + width + spacing > secondLeft
            && firstTop - spacing < secondTop + height
            && firstTop + height + spacing > secondTop;
    }

    private static RotatedBounds rotatedBounds(
        float left,
        float top,
        float width,
        float height,
        float rotationDegrees
    ) {
        double radians = Math.toRadians(rotationDegrees);
        float rotatedWidth = (float) (
            Math.abs(width * Math.cos(radians)) + Math.abs(height * Math.sin(radians))
        );
        float rotatedHeight = (float) (
            Math.abs(width * Math.sin(radians)) + Math.abs(height * Math.cos(radians))
        );
        float centerX = left + width / 2f;
        float centerY = top + height / 2f;
        return new RotatedBounds(
            centerX - rotatedWidth / 2f,
            centerY - rotatedHeight / 2f,
            centerX + rotatedWidth / 2f,
            centerY + rotatedHeight / 2f
        );
    }

    private static float cardFraction(int count) {
        if (count <= 12) {
            return 0.22f;
        }
        if (count <= 24) {
            return 0.19f;
        }
        if (count <= 40) {
            return 0.175f;
        }
        return 0.16f;
    }

    private static final class RotatedBounds {
        final float left;
        final float top;
        final float right;
        final float bottom;

        RotatedBounds(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        boolean overlaps(RotatedBounds other) {
            return left < other.right
                && right > other.left
                && top < other.bottom
                && bottom > other.top;
        }
    }
}
