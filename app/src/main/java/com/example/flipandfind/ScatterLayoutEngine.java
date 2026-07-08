package com.example.flipandfind;

import java.util.Random;

/** Pure Java rotated-rectangle packing used by the Android tabletop and unit tests. */
final class ScatterLayoutEngine {
    static final float MAX_ROTATION_DEGREES = 10f;
    // Math.round can move one origin by half a pixel relative to another. Keeping 1.51px before
    // snapping leaves at least one physical pixel after BoardLayout applies its integer origins.
    private static final float INTEGER_POSITION_SAFETY = 1.51f;

    private static final int MAX_GROWTH_ATTEMPTS = 10;
    private static final int MAX_POSITION_ATTEMPTS = 5000;
    private static final float TARGET_DENSITY = 0.40f;

    static final class Result {
        final float[] lefts;
        final float[] tops;
        final float[] rotations;
        final float contentHeight;

        Result(float[] lefts, float[] tops, float[] rotations, float contentHeight) {
            this.lefts = lefts;
            this.tops = tops;
            this.rotations = rotations;
            this.contentHeight = contentHeight;
        }
    }

    private ScatterLayoutEngine() {
    }

    static Result generateWithin(
        int count,
        float contentWidth,
        float contentHeight,
        float cardWidth,
        float cardHeight,
        float edgeMargin,
        float spacing,
        long seed
    ) {
        return generateWithin(
            count,
            contentWidth,
            contentHeight,
            cardWidth,
            cardHeight,
            edgeMargin,
            spacing,
            seed,
            MAX_ROTATION_DEGREES
        );
    }

    static Result generateWithin(
        int count,
        float contentWidth,
        float contentHeight,
        float cardWidth,
        float cardHeight,
        float edgeMargin,
        float spacing,
        long seed,
        float maximumRotationDegrees
    ) {
        if (count <= 0) {
            return new Result(new float[0], new float[0], new float[0], contentHeight);
        }
        if (contentWidth <= 0f || contentHeight <= 0f || cardWidth <= 0f || cardHeight <= 0f) {
            return null;
        }
        float[] rotations = rotations(count, seed, maximumRotationDegrees);
        return tryRandomPacking(
            count,
            contentWidth,
            contentHeight,
            cardWidth,
            cardHeight,
            edgeMargin,
            spacing,
            seed,
            rotations
        );
    }

    static Result latticeWithin(
        int count,
        float contentWidth,
        float contentHeight,
        float cardWidth,
        float cardHeight,
        int columns,
        long seed
    ) {
        return latticeWithin(
            count,
            contentWidth,
            contentHeight,
            cardWidth,
            cardHeight,
            columns,
            seed,
            MAX_ROTATION_DEGREES
        );
    }

    static Result latticeWithin(
        int count,
        float contentWidth,
        float contentHeight,
        float cardWidth,
        float cardHeight,
        int columns,
        long seed,
        float maximumRotationDegrees
    ) {
        if (count <= 0) {
            return new Result(new float[0], new float[0], new float[0], contentHeight);
        }
        columns = Math.max(1, Math.min(count, columns));
        int rows = (count + columns - 1) / columns;
        float cellWidth = contentWidth / columns;
        float cellHeight = contentHeight / rows;
        int capacity = columns * rows;
        int[] slots = new int[capacity];
        for (int index = 0; index < capacity; index++) {
            slots[index] = index;
        }
        Random random = new Random(seed ^ 0x3c6ef372fe94f82bL);
        for (int index = capacity - 1; index > 0; index--) {
            int swapWith = random.nextInt(index + 1);
            int value = slots[index];
            slots[index] = slots[swapWith];
            slots[swapWith] = value;
        }

        float[] rotations = rotations(count, seed, maximumRotationDegrees);
        float[] lefts = new float[count];
        float[] tops = new float[count];
        for (int card = 0; card < count; card++) {
            int slot = slots[card];
            int row = slot / columns;
            int column = slot % columns;
            float aabbWidth = rotatedWidth(cardWidth, cardHeight, rotations[card]);
            float aabbHeight = rotatedHeight(cardWidth, cardHeight, rotations[card]);
            float halfWidth = aabbWidth / 2f;
            float halfHeight = aabbHeight / 2f;
            float cellLeft = column * cellWidth;
            float cellTop = row * cellHeight;
            float minCenterX = cellLeft + halfWidth + INTEGER_POSITION_SAFETY;
            float maxCenterX = cellLeft + cellWidth - halfWidth - INTEGER_POSITION_SAFETY;
            float minCenterY = cellTop + halfHeight + INTEGER_POSITION_SAFETY;
            float maxCenterY = cellTop + cellHeight - halfHeight - INTEGER_POSITION_SAFETY;
            float centerX = minCenterX <= maxCenterX
                ? minCenterX + random.nextFloat() * (maxCenterX - minCenterX)
                : cellLeft + cellWidth / 2f;
            float centerY = minCenterY <= maxCenterY
                ? minCenterY + random.nextFloat() * (maxCenterY - minCenterY)
                : cellTop + cellHeight / 2f;
            centerX = centerForRoundedOrigin(centerX, cardWidth);
            centerY = centerForRoundedOrigin(centerY, cardHeight);
            lefts[card] = centerX - cardWidth / 2f;
            tops[card] = centerY - cardHeight / 2f;
        }
        relaxLatticePositions(
            lefts,
            tops,
            rotations,
            contentWidth,
            contentHeight,
            cardWidth,
            cardHeight,
            cellWidth,
            cellHeight,
            random,
            90,
            0.72f
        );
        return new Result(lefts, tops, rotations, contentHeight);
    }

    /**
     * Starts from a guaranteed packed arrangement, then performs a much deeper seeded relaxation
     * which lets cards cross their original cells whenever free space permits. This retains the
     * fixed-board guarantee without leaving ordinary games visibly aligned to a grid.
     */
    static Result compactScatterWithin(
        int count,
        float contentWidth,
        float contentHeight,
        float cardWidth,
        float cardHeight,
        int columns,
        long seed,
        float maximumRotationDegrees
    ) {
        Result result = latticeWithin(
            count,
            contentWidth,
            contentHeight,
            cardWidth,
            cardHeight,
            columns,
            seed,
            maximumRotationDegrees
        );
        int rows = Math.max(1, (count + Math.max(1, columns) - 1) / Math.max(1, columns));
        relaxLatticePositions(
            result.lefts,
            result.tops,
            result.rotations,
            contentWidth,
            contentHeight,
            cardWidth,
            cardHeight,
            contentWidth / Math.max(1, columns),
            contentHeight / rows,
            new Random(seed ^ 0xa54ff53a5f1d36f1L),
            320,
            1.35f
        );
        return result;
    }

    /** Breaks visible row/column alignment while retaining the lattice's overlap guarantee. */
    private static void relaxLatticePositions(
        float[] lefts,
        float[] tops,
        float[] rotations,
        float contentWidth,
        float contentHeight,
        float cardWidth,
        float cardHeight,
        float cellWidth,
        float cellHeight,
        Random random,
        int attemptsPerCard,
        float movementScale
    ) {
        int count = lefts.length;
        float[] centersX = new float[count];
        float[] centersY = new float[count];
        float[] halfWidths = new float[count];
        float[] halfHeights = new float[count];
        for (int card = 0; card < count; card++) {
            centersX[card] = lefts[card] + cardWidth / 2f;
            centersY[card] = tops[card] + cardHeight / 2f;
            halfWidths[card] = rotatedWidth(cardWidth, cardHeight, rotations[card]) / 2f;
            halfHeights[card] = rotatedHeight(cardWidth, cardHeight, rotations[card]) / 2f;
        }

        int attempts = Math.max(80, count * Math.max(1, attemptsPerCard));
        for (int attempt = 0; attempt < attempts; attempt++) {
            int card = random.nextInt(count);
            float proposedX = centersX[card]
                + (random.nextFloat() - 0.5f) * cellWidth * movementScale;
            float proposedY = centersY[card]
                + (random.nextFloat() - 0.5f) * cellHeight * movementScale;
            proposedX = Math.max(
                halfWidths[card] + INTEGER_POSITION_SAFETY,
                Math.min(
                    contentWidth - halfWidths[card] - INTEGER_POSITION_SAFETY,
                    proposedX
                )
            );
            proposedY = Math.max(
                halfHeights[card] + INTEGER_POSITION_SAFETY,
                Math.min(
                    contentHeight - halfHeights[card] - INTEGER_POSITION_SAFETY,
                    proposedY
                )
            );
            proposedX = centerForRoundedOrigin(proposedX, cardWidth);
            proposedY = centerForRoundedOrigin(proposedY, cardHeight);
            if (overlapsAnyExcept(
                proposedX,
                proposedY,
                halfWidths[card],
                halfHeights[card],
                centersX,
                centersY,
                halfWidths,
                halfHeights,
                card,
                INTEGER_POSITION_SAFETY
            )) {
                continue;
            }
            centersX[card] = proposedX;
            centersY[card] = proposedY;
            lefts[card] = proposedX - cardWidth / 2f;
            tops[card] = proposedY - cardHeight / 2f;
        }
    }

    private static boolean overlapsAnyExcept(
        float centerX,
        float centerY,
        float halfWidth,
        float halfHeight,
        float[] centersX,
        float[] centersY,
        float[] halfWidths,
        float[] halfHeights,
        int excludedCard,
        float separation
    ) {
        for (int card = 0; card < centersX.length; card++) {
            if (card == excludedCard) {
                continue;
            }
            if (Math.abs(centerX - centersX[card])
                    < halfWidth + halfWidths[card] + separation
                && Math.abs(centerY - centersY[card])
                    < halfHeight + halfHeights[card] + separation) {
                return true;
            }
        }
        return false;
    }

    static Result generate(
        int count,
        float contentWidth,
        float cardWidth,
        float cardHeight,
        float edgeMargin,
        float spacing,
        long seed
    ) {
        if (count <= 0) {
            return new Result(new float[0], new float[0], new float[0], 0f);
        }
        float expandedCardArea = (cardWidth + spacing * 2f) * (cardHeight + spacing * 2f);
        float contentHeight = Math.max(
            cardHeight + edgeMargin * 2f,
            expandedCardArea * count / (Math.max(1f, contentWidth) * TARGET_DENSITY)
        );

        for (int growth = 0; growth < MAX_GROWTH_ATTEMPTS; growth++) {
            Result result = generateWithin(
                count,
                contentWidth,
                contentHeight,
                cardWidth,
                cardHeight,
                edgeMargin,
                spacing,
                seed
            );
            if (result != null) {
                return result;
            }
            contentHeight = contentHeight * 1.14f + cardHeight;
        }

        int columns = Math.max(1, (int) Math.sqrt(count * contentWidth / Math.max(1f, contentHeight)));
        int rows = (count + columns - 1) / columns;
        float minimumHeight = rows * maxRotatedHeight(cardWidth, cardHeight) * 1.08f;
        contentHeight = Math.max(contentHeight, minimumHeight);
        return latticeWithin(count, contentWidth, contentHeight, cardWidth, cardHeight, columns, seed);
    }

    private static Result tryRandomPacking(
        int count,
        float contentWidth,
        float contentHeight,
        float cardWidth,
        float cardHeight,
        float edgeMargin,
        float spacing,
        long seed,
        float[] rotations
    ) {
        float[] lefts = new float[count];
        float[] tops = new float[count];
        float[] centersX = new float[count];
        float[] centersY = new float[count];
        float[] halfWidths = new float[count];
        float[] halfHeights = new float[count];
        Random random = new Random(seed);

        for (int card = 0; card < count; card++) {
            halfWidths[card] = rotatedWidth(cardWidth, cardHeight, rotations[card]) / 2f;
            halfHeights[card] = rotatedHeight(cardWidth, cardHeight, rotations[card]) / 2f;
            float minCenterX = edgeMargin + halfWidths[card];
            float maxCenterX = contentWidth - edgeMargin - halfWidths[card];
            float minCenterY = edgeMargin + halfHeights[card];
            float maxCenterY = contentHeight - edgeMargin - halfHeights[card];
            if (minCenterX > maxCenterX || minCenterY > maxCenterY) {
                return null;
            }

            boolean placed = false;
            for (int attempt = 0; attempt < MAX_POSITION_ATTEMPTS; attempt++) {
                float centerX = minCenterX + random.nextFloat() * (maxCenterX - minCenterX);
                float centerY = minCenterY + random.nextFloat() * (maxCenterY - minCenterY);
                centerX = centerForRoundedOrigin(centerX, cardWidth);
                centerY = centerForRoundedOrigin(centerY, cardHeight);
                if (!overlapsAny(
                    centerX,
                    centerY,
                    halfWidths[card],
                    halfHeights[card],
                    spacing,
                    centersX,
                    centersY,
                    halfWidths,
                    halfHeights,
                    card
                )) {
                    centersX[card] = centerX;
                    centersY[card] = centerY;
                    lefts[card] = centerX - cardWidth / 2f;
                    tops[card] = centerY - cardHeight / 2f;
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                return null;
            }
        }

        return new Result(lefts, tops, rotations, contentHeight);
    }

    private static boolean overlapsAny(
        float centerX,
        float centerY,
        float halfWidth,
        float halfHeight,
        float spacing,
        float[] placedCentersX,
        float[] placedCentersY,
        float[] placedHalfWidths,
        float[] placedHalfHeights,
        int placedCount
    ) {
        for (int index = 0; index < placedCount; index++) {
            if (Math.abs(centerX - placedCentersX[index])
                    < halfWidth + placedHalfWidths[index] + spacing
                && Math.abs(centerY - placedCentersY[index])
                    < halfHeight + placedHalfHeights[index] + spacing) {
                return true;
            }
        }
        return false;
    }

    /** Matches BoardLayout's Math.round(left/top) while keeping center-based packing arithmetic. */
    private static float centerForRoundedOrigin(float center, float cardExtent) {
        return Math.round(center - cardExtent / 2f) + cardExtent / 2f;
    }

    static float rotatedWidth(float width, float height, float degrees) {
        double radians = Math.toRadians(degrees);
        return (float) (Math.abs(width * Math.cos(radians)) + Math.abs(height * Math.sin(radians)));
    }

    static float rotatedHeight(float width, float height, float degrees) {
        double radians = Math.toRadians(degrees);
        return (float) (Math.abs(width * Math.sin(radians)) + Math.abs(height * Math.cos(radians)));
    }

    static float maxRotatedWidth(float width, float height) {
        return rotatedWidth(width, height, MAX_ROTATION_DEGREES);
    }

    static float maxRotatedHeight(float width, float height) {
        return rotatedHeight(width, height, MAX_ROTATION_DEGREES);
    }

    private static float[] rotations(int count, long seed, float maximumRotationDegrees) {
        maximumRotationDegrees = Math.max(0f, Math.min(45f, maximumRotationDegrees));
        float[] rotations = new float[count];
        Random random = new Random(seed ^ 0xbb67ae8584caa73bL);
        for (int index = 0; index < count; index++) {
            rotations[index] = -maximumRotationDegrees
                + random.nextFloat() * maximumRotationDegrees * 2f;
        }
        return rotations;
    }
}
