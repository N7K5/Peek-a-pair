package com.example.flipandfind;

/** Pure card sizing and fixed-viewport packing policy used by {@link BoardLayout}. */
final class BoardCardSizing {
    static final float CARD_ASPECT = 1.22f;

    private static final float NORMAL_AREA_SHARE = 0.30f;
    private static final float LARGER_AREA_SHARE = 0.72f;
    private static final float NORMAL_SHRINK_FACTOR = 0.92f;
    private static final float LARGER_SHRINK_FACTOR = 0.97f;
    private static final int MAX_RANDOM_ATTEMPTS = 16;
    private static final int LARGER_RANDOM_ATTEMPTS = 5;
    private static final float LARGER_MAX_ROTATION_DEGREES = 6f;
    private static final int MAX_COMPACT_SCATTER_SLOTS = 46;
    private static final float INTEGER_LAYOUT_SAFETY = 0.99f;
    private static final float LATTICE_CELL_SAFETY = 1.51f;

    enum PackingStrategy {
        RANDOM,
        COMPACT_SCATTER,
        DENSE_LATTICE
    }

    static final class Fit {
        final int cardWidth;
        final int cardHeight;
        final float spacing;
        final ScatterLayoutEngine.Result scatter;
        final PackingStrategy packingStrategy;

        Fit(
            int cardWidth,
            int cardHeight,
            float spacing,
            ScatterLayoutEngine.Result scatter,
            PackingStrategy packingStrategy
        ) {
            this.cardWidth = cardWidth;
            this.cardHeight = cardHeight;
            this.spacing = spacing;
            this.scatter = scatter;
            this.packingStrategy = packingStrategy;
        }
    }

    private BoardCardSizing() {
    }

    static Fit fit(
        int slotCount,
        int contentWidth,
        int contentHeight,
        float density,
        boolean largerCards,
        long seed
    ) {
        if (slotCount < 0) {
            throw new IllegalArgumentException("Slot count cannot be negative");
        }
        if (contentWidth <= 0 || contentHeight <= 0) {
            throw new IllegalArgumentException("Board dimensions must be positive");
        }
        density = Math.max(0.1f, density);
        if (slotCount == 0) {
            return new Fit(
                0,
                0,
                0f,
                new ScatterLayoutEngine.Result(
                    new float[0],
                    new float[0],
                    new float[0],
                    contentHeight
                ),
                PackingStrategy.RANDOM
            );
        }

        float usableArea = contentWidth * (float) contentHeight;
        float targetAreaShare = largerCards ? LARGER_AREA_SHARE : NORMAL_AREA_SHARE;
        int maximumCardWidth = pixels(largerCards ? 126f : 84f, density);
        float estimatedWidth = (float) Math.sqrt(
            usableArea * targetAreaShare / (slotCount * CARD_ASPECT)
        );
        int candidateWidth = Math.min(
            maximumCardWidth,
            Math.max(pixels(18f, density), Math.round(estimatedWidth))
        );
        float shrinkFactor = largerCards ? LARGER_SHRINK_FACTOR : NORMAL_SHRINK_FACTOR;

        int randomAttempts = largerCards ? LARGER_RANDOM_ATTEMPTS : MAX_RANDOM_ATTEMPTS;
        float maximumRotation = largerCards
            ? LARGER_MAX_ROTATION_DEGREES
            : ScatterLayoutEngine.MAX_ROTATION_DEGREES;
        for (int attempt = 0;
            attempt < randomAttempts && candidateWidth >= pixels(12f, density);
            attempt++) {
            int candidateHeight = Math.max(1, Math.round(candidateWidth * CARD_ASPECT));
            float edgeMargin = largerCards
                ? Math.max(pixels(1.5f, density), candidateWidth * 0.04f)
                : Math.max(pixels(3f, density), candidateWidth * 0.10f);
            float spacing = largerCards
                ? Math.max(pixels(1f, density), candidateWidth * 0.055f)
                : Math.max(pixels(2f, density), candidateWidth * 0.14f);
            ScatterLayoutEngine.Result scatter = ScatterLayoutEngine.generateWithin(
                slotCount,
                contentWidth,
                contentHeight,
                candidateWidth,
                candidateHeight,
                edgeMargin,
                spacing,
                seed,
                maximumRotation
            );
            if (scatter != null && isRoundedLayoutSafe(
                scatter,
                contentWidth,
                contentHeight,
                candidateWidth,
                candidateHeight
            )) {
                return new Fit(
                    candidateWidth,
                    candidateHeight,
                    spacing,
                    scatter,
                    PackingStrategy.RANDOM
                );
            }
            int nextWidth = Math.round(candidateWidth * shrinkFactor);
            candidateWidth = Math.max(pixels(11f, density), nextWidth);
        }

        return latticeFit(
            slotCount,
            contentWidth,
            contentHeight,
            maximumCardWidth,
            largerCards,
            seed
        );
    }

    private static Fit latticeFit(
        int slotCount,
        int contentWidth,
        int contentHeight,
        int maximumCardWidth,
        boolean largerCards,
        long seed
    ) {
        int bestColumns = 1;
        float bestWidth = 1f;
        float maximumRotation = largerCards
            ? LARGER_MAX_ROTATION_DEGREES
            : ScatterLayoutEngine.MAX_ROTATION_DEGREES;
        double angle = Math.toRadians(maximumRotation);
        float horizontalFactor = (float) (
            Math.cos(angle) + CARD_ASPECT * Math.sin(angle)
        );
        float verticalFactor = (float) (
            Math.sin(angle) + CARD_ASPECT * Math.cos(angle)
        );
        for (int columns = 1; columns <= slotCount; columns++) {
            int rows = (slotCount + columns - 1) / columns;
            float widthForColumns = Math.max(
                0f,
                contentWidth / (float) columns - LATTICE_CELL_SAFETY * 2f
            ) / horizontalFactor;
            float widthForRows = Math.max(
                0f,
                contentHeight / (float) rows - LATTICE_CELL_SAFETY * 2f
            ) / verticalFactor;
            float fittedWidth = Math.min(widthForColumns, widthForRows)
                * (largerCards ? 0.99f : 0.90f);
            if (fittedWidth > bestWidth) {
                bestWidth = fittedWidth;
                bestColumns = columns;
            }
        }
        int cardWidth = Math.max(
            1,
            Math.min(maximumCardWidth, (int) Math.floor(bestWidth))
        );
        boolean compactScatter = largerCards && slotCount <= MAX_COMPACT_SCATTER_SLOTS;
        while (cardWidth > 0) {
            int cardHeight = Math.max(1, Math.round(cardWidth * CARD_ASPECT));
            ScatterLayoutEngine.Result scatter = compactScatter
                ? ScatterLayoutEngine.compactScatterWithin(
                    slotCount,
                    contentWidth,
                    contentHeight,
                    cardWidth,
                    cardHeight,
                    bestColumns,
                    seed,
                    maximumRotation
                )
                : ScatterLayoutEngine.latticeWithin(
                    slotCount,
                    contentWidth,
                    contentHeight,
                    cardWidth,
                    cardHeight,
                    bestColumns,
                    seed,
                    maximumRotation
                );
            if (isRoundedLayoutSafe(
                scatter,
                contentWidth,
                contentHeight,
                cardWidth,
                cardHeight
            )) {
                return new Fit(
                    cardWidth,
                    cardHeight,
                    0f,
                    scatter,
                    compactScatter
                        ? PackingStrategy.COMPACT_SCATTER
                        : PackingStrategy.DENSE_LATTICE
                );
            }
            cardWidth--;
        }
        throw new IllegalStateException("Could not fit cards inside the board");
    }

    /** Validates the exact integer origins used by BoardLayout, including a one-pixel buffer. */
    static boolean isRoundedLayoutSafe(
        ScatterLayoutEngine.Result scatter,
        int contentWidth,
        int contentHeight,
        int cardWidth,
        int cardHeight
    ) {
        int count = scatter.lefts.length;
        float[] centerX = new float[count];
        float[] centerY = new float[count];
        float[] halfWidth = new float[count];
        float[] halfHeight = new float[count];
        for (int card = 0; card < count; card++) {
            int roundedLeft = Math.round(scatter.lefts[card]);
            int roundedTop = Math.round(scatter.tops[card]);
            centerX[card] = roundedLeft + cardWidth / 2f;
            centerY[card] = roundedTop + cardHeight / 2f;
            halfWidth[card] = ScatterLayoutEngine.rotatedWidth(
                cardWidth,
                cardHeight,
                scatter.rotations[card]
            ) / 2f;
            halfHeight[card] = ScatterLayoutEngine.rotatedHeight(
                cardWidth,
                cardHeight,
                scatter.rotations[card]
            ) / 2f;
            if (centerX[card] - halfWidth[card] < INTEGER_LAYOUT_SAFETY
                || centerY[card] - halfHeight[card] < INTEGER_LAYOUT_SAFETY
                || centerX[card] + halfWidth[card]
                    > contentWidth - INTEGER_LAYOUT_SAFETY
                || centerY[card] + halfHeight[card]
                    > contentHeight - INTEGER_LAYOUT_SAFETY) {
                return false;
            }
        }
        for (int first = 0; first < count; first++) {
            for (int second = first + 1; second < count; second++) {
                if (Math.abs(centerX[first] - centerX[second])
                        < halfWidth[first] + halfWidth[second] + INTEGER_LAYOUT_SAFETY
                    && Math.abs(centerY[first] - centerY[second])
                        < halfHeight[first] + halfHeight[second] + INTEGER_LAYOUT_SAFETY) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int pixels(float dp, float density) {
        return Math.max(1, Math.round(dp * density));
    }
}
