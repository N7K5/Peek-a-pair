package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class BoardCardSizingTest {
    private static final float REQUIRED_INTEGER_BUFFER = 0.98f;
    private static final int[][] PORTRAIT_VIEWPORTS = {
        {306, 430},
        {346, 560},
        {397, 700}
    };
    private static final int[] SLOT_COUNTS = {4, 10, 22, 42, 62, 102};

    @Test
    public void largerModeProducesMateriallyLargerCardsAcrossPortraitBoards() {
        for (int[] viewport : PORTRAIT_VIEWPORTS) {
            for (int slotCount : SLOT_COUNTS) {
                BoardCardSizing.Fit normal = BoardCardSizing.fit(
                    slotCount, viewport[0], viewport[1], 1f, false, 20260708L
                );
                BoardCardSizing.Fit larger = BoardCardSizing.fit(
                    slotCount, viewport[0], viewport[1], 1f, true, 20260708L
                );
                float ratio = larger.cardWidth / (float) normal.cardWidth;
                assertTrue(
                    "Larger mode should be at least one-third wider for " + viewport[0] + "x"
                        + viewport[1] + " with " + slotCount + " slots; normal="
                        + normal.cardWidth + ", larger=" + larger.cardWidth,
                    ratio >= 1.33f
                );
            }
        }
    }

    @Test
    public void bothModesFitAndDoNotOverlapThroughFiftyPairs() {
        long[] seeds = {1L, 16L, 41L, 987654321L};
        for (int[] viewport : PORTRAIT_VIEWPORTS) {
            for (int slotCount : SLOT_COUNTS) {
                for (long seed : seeds) {
                    assertValid(BoardCardSizing.fit(
                        slotCount, viewport[0], viewport[1], 1f, false, seed
                    ), slotCount, viewport[0], viewport[1]);
                    assertValid(BoardCardSizing.fit(
                        slotCount, viewport[0], viewport[1], 1f, true, seed
                    ), slotCount, viewport[0], viewport[1]);
                }
            }
        }
    }

    @Test
    public void reportedRoundingRegressionsHavePixelSafeAndroidOrigins() {
        BoardCardSizing.Fit compact = BoardCardSizing.fit(
            10, 306, 430, 1f, true, 16L
        );
        BoardCardSizing.Fit smallBoard = BoardCardSizing.fit(
            4, 240, 320, 1f, true, 1L
        );

        assertValid(compact, 10, 306, 430);
        assertValid(smallBoard, 4, 240, 320);
        assertTrue(BoardCardSizing.isRoundedLayoutSafe(
            compact.scatter, 306, 430, compact.cardWidth, compact.cardHeight
        ));
        assertTrue(BoardCardSizing.isRoundedLayoutSafe(
            smallBoard.scatter, 240, 320, smallBoard.cardWidth, smallBoard.cardHeight
        ));
    }

    @Test
    public void ordinaryLargerGamesAvoidDenseLatticeFallback() {
        int[] ordinarySlots = {10, 18, 30, 46}; // 4, 8, 14, and 22 pairs plus two vacancies.
        long[] seeds = {7L, 41L, 20260708L};
        for (int[] viewport : PORTRAIT_VIEWPORTS) {
            for (int slotCount : ordinarySlots) {
                for (long seed : seeds) {
                    BoardCardSizing.Fit fit = BoardCardSizing.fit(
                        slotCount,
                        viewport[0],
                        viewport[1],
                        1f,
                        true,
                        seed
                    );
                    assertTrue(
                        "Ordinary game should use random or compact scatter, not dense lattice",
                        fit.packingStrategy != BoardCardSizing.PackingStrategy.DENSE_LATTICE
                    );
                }
            }
        }
    }

    @Test
    public void densityOnlyChangesDpCapsNotGeometryDrivenSizes() {
        BoardCardSizing.Fit oneX = BoardCardSizing.fit(
            4, 306, 430, 1f, true, 17L
        );
        BoardCardSizing.Fit threeX = BoardCardSizing.fit(
            4, 918, 1290, 3f, true, 17L
        );

        assertEquals(oneX.cardWidth, threeX.cardWidth / 3f, oneX.cardWidth * 0.10f);
        assertEquals(oneX.cardHeight, threeX.cardHeight / 3f, oneX.cardHeight * 0.10f);
    }

    @Test
    public void largerPackedScatterRemainsSeededAndVisuallyVaried() {
        BoardCardSizing.Fit first = BoardCardSizing.fit(
            46, 346, 560, 1f, true, 11L
        );
        BoardCardSizing.Fit repeated = BoardCardSizing.fit(
            46, 346, 560, 1f, true, 11L
        );
        BoardCardSizing.Fit different = BoardCardSizing.fit(
            46, 346, 560, 1f, true, 12L
        );

        assertEquals(BoardCardSizing.PackingStrategy.COMPACT_SCATTER, first.packingStrategy);
        boolean changedWithSeed = false;
        boolean clockwise = false;
        boolean counterClockwise = false;
        for (int card = 0; card < 46; card++) {
            assertEquals(first.scatter.lefts[card], repeated.scatter.lefts[card], 0f);
            assertEquals(first.scatter.tops[card], repeated.scatter.tops[card], 0f);
            assertEquals(first.scatter.rotations[card], repeated.scatter.rotations[card], 0f);
            assertTrue(first.scatter.rotations[card] >= -6f);
            assertTrue(first.scatter.rotations[card] <= 6f);
            clockwise |= first.scatter.rotations[card] > 3f;
            counterClockwise |= first.scatter.rotations[card] < -3f;
            changedWithSeed |= first.scatter.lefts[card] != different.scatter.lefts[card]
                || first.scatter.tops[card] != different.scatter.tops[card];
        }
        assertTrue(changedWithSeed);
        assertTrue(clockwise);
        assertTrue(counterClockwise);
    }

    @Test
    public void emptyBoardHasNoCardsAndKeepsViewportHeight() {
        BoardCardSizing.Fit fit = BoardCardSizing.fit(0, 320, 500, 2f, true, 1L);

        assertEquals(0, fit.cardWidth);
        assertEquals(0, fit.cardHeight);
        assertEquals(0, fit.scatter.lefts.length);
        assertEquals(500f, fit.scatter.contentHeight, 0f);
    }

    private static void assertValid(
        BoardCardSizing.Fit fit,
        int slotCount,
        float width,
        float height
    ) {
        assertEquals(slotCount, fit.scatter.lefts.length);
        assertEquals(slotCount, fit.scatter.tops.length);
        assertEquals(slotCount, fit.scatter.rotations.length);
        assertEquals(height, fit.scatter.contentHeight, 0f);
        assertTrue(BoardCardSizing.isRoundedLayoutSafe(
            fit.scatter,
            Math.round(width),
            Math.round(height),
            fit.cardWidth,
            fit.cardHeight
        ));
        for (int first = 0; first < slotCount; first++) {
            assertEquals(Math.round(fit.scatter.lefts[first]), fit.scatter.lefts[first], 0f);
            assertEquals(Math.round(fit.scatter.tops[first]), fit.scatter.tops[first], 0f);
            Bounds firstBounds = roundedBounds(fit, first);
            assertTrue(firstBounds.left >= REQUIRED_INTEGER_BUFFER);
            assertTrue(firstBounds.top >= REQUIRED_INTEGER_BUFFER);
            assertTrue(firstBounds.right <= width - REQUIRED_INTEGER_BUFFER);
            assertTrue(firstBounds.bottom <= height - REQUIRED_INTEGER_BUFFER);
            for (int second = first + 1; second < slotCount; second++) {
                assertFalse(firstBounds.overlapsWithGap(
                    roundedBounds(fit, second),
                    REQUIRED_INTEGER_BUFFER
                ));
            }
        }
    }

    private static Bounds roundedBounds(BoardCardSizing.Fit fit, int card) {
        float rotatedWidth = ScatterLayoutEngine.rotatedWidth(
            fit.cardWidth,
            fit.cardHeight,
            fit.scatter.rotations[card]
        );
        float rotatedHeight = ScatterLayoutEngine.rotatedHeight(
            fit.cardWidth,
            fit.cardHeight,
            fit.scatter.rotations[card]
        );
        float centerX = Math.round(fit.scatter.lefts[card]) + fit.cardWidth / 2f;
        float centerY = Math.round(fit.scatter.tops[card]) + fit.cardHeight / 2f;
        return new Bounds(
            centerX - rotatedWidth / 2f,
            centerY - rotatedHeight / 2f,
            centerX + rotatedWidth / 2f,
            centerY + rotatedHeight / 2f
        );
    }

    private static final class Bounds {
        final float left;
        final float top;
        final float right;
        final float bottom;

        Bounds(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        boolean overlapsWithGap(Bounds other, float gap) {
            return left - gap < other.right
                && right + gap > other.left
                && top - gap < other.bottom
                && bottom + gap > other.top;
        }
    }
}
