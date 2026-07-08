package com.example.flipandfind;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public final class RubicsFaceCatalogTest {
    @Test
    public void normalCatalogHasFiftyStableWellSeparatedFaces() {
        List<String> patterns = RubicsFaceCatalog.normalPatterns();

        assertEquals(50, patterns.size());
        assertEquals("RBC1:230513431", patterns.get(0));
        assertEquals("RBC1:503025533", patterns.get(patterns.size() - 1));
        assertEquals(50, new HashSet<>(patterns).size());
        for (int first = 0; first < patterns.size(); first++) {
            assertTrue(RubicsFaceCatalog.isToken(patterns.get(first)));
            assertBalancedFace(patterns.get(first));
            for (int second = first + 1; second < patterns.size(); second++) {
                assertTrue(
                    patterns.get(first) + " / " + patterns.get(second),
                    RubicsFaceCatalog.hammingDistance(
                        patterns.get(first),
                        patterns.get(second)
                    ) >= 6
                );
            }
        }
    }

    @Test
    public void trickyGroupsContainOneStickerNeighbors() {
        List<List<String>> groups = RubicsFaceCatalog.trickyGroups();
        Set<String> allPatterns = new HashSet<>();

        assertEquals(25, groups.size());
        for (List<String> group : groups) {
            assertEquals(2, group.size());
            assertEquals(1, RubicsFaceCatalog.hammingDistance(group.get(0), group.get(1)));
            assertTrue(allPatterns.add(group.get(0)));
            assertTrue(allPatterns.add(group.get(1)));
        }
        assertEquals(50, allPatterns.size());

        // Changing one cell on each of two six-away anchors still leaves a
        // clear gap between unrelated tricky groups.
        for (int first = 0; first < groups.size(); first++) {
            for (int second = first + 1; second < groups.size(); second++) {
                for (String firstPattern : groups.get(first)) {
                    for (String secondPattern : groups.get(second)) {
                        assertTrue(
                            RubicsFaceCatalog.hammingDistance(
                                firstPattern,
                                secondPattern
                            ) >= 4
                        );
                    }
                }
            }
        }
    }

    @Test
    public void trickyTripleIsDistinctAndPairwiseOneStickerApart() {
        List<String> triple = RubicsFaceCatalog.trickyTriple();

        assertEquals(3, triple.size());
        assertEquals(3, new HashSet<>(triple).size());
        for (int first = 0; first < triple.size(); first++) {
            for (int second = first + 1; second < triple.size(); second++) {
                assertEquals(
                    1,
                    RubicsFaceCatalog.hammingDistance(
                        triple.get(first),
                        triple.get(second)
                    )
                );
            }
        }
    }

    @Test
    public void everySelectionSizeIsUniqueAndDeterministic() {
        for (boolean tricky : new boolean[] {false, true}) {
            for (int count = 1; count <= RubicsFaceCatalog.MAX_PATTERN_COUNT; count++) {
                String[] first = RubicsFaceCatalog.patternsFor(count, 90210L, tricky);
                String[] second = RubicsFaceCatalog.patternsFor(count, 90210L, tricky);

                assertArrayEquals(first, second);
                assertEquals(count, first.length);
                assertEquals(count, new HashSet<>(Arrays.asList(first)).size());
                for (String pattern : first) {
                    assertTrue(RubicsFaceCatalog.isToken(pattern));
                }
            }
        }
    }

    @Test
    public void oddTrickySelectionsEndInTheConfusableTriple() {
        Set<String> expected = new HashSet<>(RubicsFaceCatalog.trickyTriple());
        for (int count : new int[] {3, 5, 29, 49}) {
            String[] patterns = RubicsFaceCatalog.patternsFor(count, 81L, true);
            Set<String> actual = new HashSet<>(Arrays.asList(
                patterns[count - 3],
                patterns[count - 2],
                patterns[count - 1]
            ));
            assertEquals(expected, actual);
        }
    }

    @Test
    public void differentSeedsChangeSelectionOrder() {
        assertFalse(Arrays.equals(
            RubicsFaceCatalog.patternsFor(20, 1L, false),
            RubicsFaceCatalog.patternsFor(20, 2L, false)
        ));
        assertFalse(Arrays.equals(
            RubicsFaceCatalog.patternsFor(20, 1L, true),
            RubicsFaceCatalog.patternsFor(20, 2L, true)
        ));
    }

    @Test
    public void tokenCodecRoundTripsAndReturnsDefensiveArrays() {
        int[] colors = {
            RubicsFaceCatalog.WHITE,
            RubicsFaceCatalog.YELLOW,
            RubicsFaceCatalog.RED,
            RubicsFaceCatalog.ORANGE,
            RubicsFaceCatalog.BLUE,
            RubicsFaceCatalog.GREEN,
            RubicsFaceCatalog.WHITE,
            RubicsFaceCatalog.RED,
            RubicsFaceCatalog.BLUE
        };
        String token = RubicsFaceCatalog.encodeColors(colors);

        assertEquals("RBC1:012345024", token);
        assertArrayEquals(colors, RubicsFaceCatalog.decodeColors(token));
        int[] decoded = RubicsFaceCatalog.decodeColors(token);
        decoded[0] = RubicsFaceCatalog.GREEN;
        assertEquals(RubicsFaceCatalog.WHITE, RubicsFaceCatalog.decodeColors(token)[0]);
    }

    @Test
    public void spokenDescriptionNamesAllRowsAndColors() {
        assertEquals(
            "Rubik's cube face. Top row: white, yellow, red. "
                + "Middle row: orange, blue, green. "
                + "Bottom row: white, red, blue.",
            RubicsFaceCatalog.spokenDescription("RBC1:012345024")
        );
    }

    @Test
    public void collectionsCannotMutateBuiltInCatalog() {
        assertUnmodifiable(RubicsFaceCatalog.normalPatterns());
        assertUnmodifiable(RubicsFaceCatalog.trickyTriple());
        assertUnmodifiable(RubicsFaceCatalog.trickyGroups());
        assertUnmodifiable(RubicsFaceCatalog.trickyGroups().get(0));
    }

    @Test
    public void rejectsMalformedTokensColorsAndCounts() {
        for (String malformed : new String[] {
            null,
            "",
            "RBC1:01234502",
            "RBC1:0123450240",
            "RBC2:012345024",
            "RBC1:012345026",
            "RBC1:01234502x"
        }) {
            assertFalse(String.valueOf(malformed), RubicsFaceCatalog.isToken(malformed));
            assertInvalidDecode(malformed);
        }

        assertInvalidEncode(new int[8]);
        assertInvalidEncode(new int[10]);
        assertInvalidEncode(new int[] {0, 1, 2, 3, 4, 5, 0, 1, -1});
        assertInvalidEncode(new int[] {0, 1, 2, 3, 4, 5, 0, 1, 6});
        assertInvalidCount(0);
        assertInvalidCount(51);
    }

    private static void assertBalancedFace(String token) {
        int[] counts = new int[6];
        for (int color : RubicsFaceCatalog.decodeColors(token)) {
            counts[color]++;
        }
        int colorsUsed = 0;
        for (int count : counts) {
            if (count > 0) {
                colorsUsed++;
            }
            assertTrue(token, count <= 3);
        }
        assertTrue(token, colorsUsed >= 4);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void assertUnmodifiable(List<?> list) {
        try {
            ((List) list).add(new Object());
            fail("Expected an immutable catalog list");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    private static void assertInvalidDecode(String token) {
        try {
            RubicsFaceCatalog.decodeColors(token);
            fail("Expected malformed token to fail");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void assertInvalidEncode(int[] colors) {
        try {
            RubicsFaceCatalog.encodeColors(colors);
            fail("Expected invalid colors to fail");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void assertInvalidCount(int count) {
        try {
            RubicsFaceCatalog.patternsFor(count, 1L, false);
            fail("Expected invalid pattern count to fail");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }
}
