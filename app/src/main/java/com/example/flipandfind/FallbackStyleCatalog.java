package com.example.flipandfind;

/** Pure-Java indexing for device-safe fallback card symbols. */
final class FallbackStyleCatalog {
    private FallbackStyleCatalog() {
    }

    static int shapeIndex(
        int pairId,
        boolean tricky,
        int pairCount,
        int shapeCount
    ) {
        return tricky
            ? trickyGroupIndex(pairId, pairCount) % shapeCount
            : pairId % shapeCount;
    }

    static int variantIndex(
        int pairId,
        boolean tricky,
        int pairCount,
        int shapeCount,
        int variantCount
    ) {
        if (!tricky) {
            return (pairId / shapeCount) % variantCount;
        }
        if (hasFinalTrickyTriple(pairCount) && pairId >= pairCount - 3) {
            return pairId - (pairCount - 3);
        }
        return pairId % 2;
    }

    static int trickyGroupIndex(int pairId, int pairCount) {
        if (hasFinalTrickyTriple(pairCount) && pairId >= pairCount - 3) {
            return (pairCount - 3) / 2;
        }
        return pairId / 2;
    }

    static int trickyColorIndex(int group, int shapeCount, int colorCount) {
        return (group / shapeCount) % colorCount;
    }

    static int styleKey(
        int pairId,
        boolean tricky,
        int pairCount,
        int shapeCount,
        int variantCount,
        int colorCount
    ) {
        int shape = shapeIndex(pairId, tricky, pairCount, shapeCount);
        int variant = variantIndex(
            pairId,
            tricky,
            pairCount,
            shapeCount,
            variantCount
        );
        int color = tricky
            ? trickyColorIndex(
                trickyGroupIndex(pairId, pairCount),
                shapeCount,
                colorCount
            )
            : 0;
        return (color * shapeCount + shape) * variantCount + variant;
    }

    private static boolean hasFinalTrickyTriple(int pairCount) {
        return pairCount >= 3 && pairCount % 2 == 1;
    }
}
