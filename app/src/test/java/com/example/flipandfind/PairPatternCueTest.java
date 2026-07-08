package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public final class PairPatternCueTest {
    @Test
    public void allFiftyPairIdsHaveStableDistinctCues() {
        Set<String> cues = new HashSet<>();
        Set<String> spokenNames = new HashSet<>();
        for (int pairId = 0; pairId < 50; pairId++) {
            int family = PairPatternCue.familyFor(pairId);
            int variant = PairPatternCue.variantFor(pairId);
            assertTrue(family >= 0 && family < PairPatternCue.FAMILY_COUNT);
            assertTrue(variant >= 0 && variant < PairPatternCue.VARIANT_COUNT);
            assertEquals(variant + 1, PairPatternCue.markerCountFor(pairId));
            assertTrue(cues.add(family + ":" + variant));
            String spokenName = PairPatternCue.spokenNameFor(pairId);
            assertTrue(!spokenName.isEmpty());
            assertTrue(spokenNames.add(spokenName));
        }
        assertEquals(50, cues.size());
        assertEquals(50, spokenNames.size());
    }

    @Test
    public void cueMappingWrapsSafelyForUnexpectedIds() {
        assertEquals(PairPatternCue.familyFor(0), PairPatternCue.familyFor(50));
        assertEquals(PairPatternCue.variantFor(0), PairPatternCue.variantFor(50));
        assertTrue(PairPatternCue.familyFor(-1) >= 0);
        assertTrue(PairPatternCue.variantFor(-1) >= 0);
    }
}
