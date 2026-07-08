package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public final class FallbackStyleCatalogTest {
    private static final int SHAPE_COUNT = 10;
    private static final int VARIANT_COUNT = 5;
    private static final int COLOR_COUNT = 10;

    @Test
    public void normalFallbackHasFiftyUniqueStyles() {
        assertUniqueStyles(50, false);
    }

    @Test
    public void trickyFallbackStaysUniqueThroughFiftyPairs() {
        for (int pairCount = 4; pairCount <= 50; pairCount++) {
            assertUniqueStyles(pairCount, true);
        }
    }

    @Test
    public void oddTrickyBoardUsesThreeVariantsForItsFinalGroup() {
        int pairCount = 49;
        int start = pairCount - 3;
        int group = FallbackStyleCatalog.trickyGroupIndex(start, pairCount);

        assertEquals(group, FallbackStyleCatalog.trickyGroupIndex(start + 1, pairCount));
        assertEquals(group, FallbackStyleCatalog.trickyGroupIndex(start + 2, pairCount));
        for (int offset = 0; offset < 3; offset++) {
            assertEquals(offset, FallbackStyleCatalog.variantIndex(
                start + offset,
                true,
                pairCount,
                SHAPE_COUNT,
                VARIANT_COUNT
            ));
        }
    }

    private static void assertUniqueStyles(int pairCount, boolean tricky) {
        Set<Integer> styles = new HashSet<>();
        for (int pairId = 0; pairId < pairCount; pairId++) {
            assertTrue(styles.add(FallbackStyleCatalog.styleKey(
                pairId,
                tricky,
                pairCount,
                SHAPE_COUNT,
                VARIANT_COUNT,
                COLOR_COUNT
            )));
        }
        assertEquals(pairCount, styles.size());
    }
}
