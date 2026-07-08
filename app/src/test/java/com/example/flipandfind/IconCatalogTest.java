package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;

public final class IconCatalogTest {
    @Test
    public void exposesAllCategoriesWithLabelsAndSamples() {
        assertEquals(14, IconCatalog.Category.values().length);

        for (IconCatalog.Category category : IconCatalog.Category.values()) {
            assertFalse(category.getDisplayName().isEmpty());
            assertFalse(category.getSample().isEmpty());
            assertEquals(category.getDisplayName(), category.toString());
        }
    }

    @Test
    public void wordAndBlankCategoriesUseVisualTileSamples() {
        assertEquals("📝", IconCatalog.Category.WORDS.getSample());
        assertEquals("🎨", IconCatalog.Category.BLANK.getSample());
        assertEquals("▦", IconCatalog.Category.RUBICS.getSample());
        assertEquals("123", IconCatalog.Category.NUMBERS.getSample());
    }

    @Test
    public void addingRubicsPreservesEveryPreviouslyShippedOrdinal() {
        assertEquals(0, IconCatalog.Category.RANDOM.ordinal());
        assertEquals(1, IconCatalog.Category.FACES.ordinal());
        assertEquals(2, IconCatalog.Category.ANIMALS.ordinal());
        assertEquals(3, IconCatalog.Category.FOOD.ordinal());
        assertEquals(4, IconCatalog.Category.NATURE.ordinal());
        assertEquals(5, IconCatalog.Category.ACTIVITIES.ordinal());
        assertEquals(6, IconCatalog.Category.TRAVEL.ordinal());
        assertEquals(7, IconCatalog.Category.OBJECTS.ordinal());
        assertEquals(8, IconCatalog.Category.FLAGS.ordinal());
        assertEquals(9, IconCatalog.Category.SYMBOLS.ordinal());
        assertEquals(10, IconCatalog.Category.WORDS.ordinal());
        assertEquals(11, IconCatalog.Category.BLANK.ordinal());
        assertEquals(12, IconCatalog.Category.RUBICS.ordinal());
        assertEquals(13, IconCatalog.Category.NUMBERS.ordinal());
    }

    @Test
    public void everyVisibleCategoryCanSupplyFiftyUniqueLabels() {
        for (IconCatalog.Category category : IconCatalog.Category.values()) {
            String[] labels = IconCatalog.iconsFor(category, 50, 42L);

            assertEquals(category.getDisplayName(), 50, labels.length);
            if (category == IconCatalog.Category.BLANK) {
                for (String label : labels) {
                    assertTrue(label.isEmpty());
                }
            } else {
                assertEquals(
                    category.getDisplayName(),
                    50,
                    new HashSet<>(Arrays.asList(labels)).size()
                );
            }
            assertTrue(IconCatalog.availableIconCount(category) >= 50);
        }
    }

    @Test
    public void theSameSeedAlwaysProducesTheSameLabelsAndOrder() {
        for (IconCatalog.Category category : IconCatalog.Category.values()) {
            assertEquals(
                category.getDisplayName(),
                Arrays.asList(IconCatalog.iconsFor(category, 24, 8675309L)),
                Arrays.asList(IconCatalog.iconsFor(category, 24, 8675309L))
            );
            assertEquals(
                category.getDisplayName(),
                Arrays.asList(IconCatalog.trickyIconsFor(category, 24, 8675309L)),
                Arrays.asList(IconCatalog.trickyIconsFor(category, 24, 8675309L))
            );
        }
    }

    @Test
    public void differentSeedsProduceDifferentSelections() {
        for (IconCatalog.Category category : IconCatalog.Category.values()) {
            if (category == IconCatalog.Category.BLANK) {
                continue;
            }
            assertNotEquals(
                category.getDisplayName(),
                Arrays.asList(IconCatalog.iconsFor(category, 20, 100L)),
                Arrays.asList(IconCatalog.iconsFor(category, 20, 200L))
            );
            assertNotEquals(
                category.getDisplayName(),
                Arrays.asList(IconCatalog.trickyIconsFor(category, 20, 100L)),
                Arrays.asList(IconCatalog.trickyIconsFor(category, 20, 200L))
            );
        }
    }

    @Test
    public void randomUsesEmojiCatalogsAndExplicitlyExcludesWords() {
        int largestEmojiPool = 0;
        for (IconCatalog.Category category : IconCatalog.Category.values()) {
            if (category != IconCatalog.Category.RANDOM
                && category != IconCatalog.Category.WORDS
                && category != IconCatalog.Category.BLANK
                && category != IconCatalog.Category.RUBICS
                && category != IconCatalog.Category.NUMBERS) {
                largestEmojiPool = Math.max(
                    largestEmojiPool,
                    IconCatalog.availableIconCount(category)
                );
            }
        }

        assertTrue(
            IconCatalog.availableIconCount(IconCatalog.Category.RANDOM)
                > largestEmojiPool
        );
        for (long seed = 0; seed < 100; seed++) {
            for (String label : IconCatalog.iconsFor(
                IconCatalog.Category.RANDOM,
                50,
                seed
            )) {
                assertFalse("RANDOM included a word: " + label, isWord(label));
                assertFalse("RANDOM included a blank label", label.isEmpty());
                assertFalse("RANDOM included a Rubics face", RubicsFaceCatalog.isToken(label));
                assertFalse("RANDOM included a number", NumberCatalog.isNumber(label));
            }
            for (String label : IconCatalog.trickyIconsFor(
                IconCatalog.Category.RANDOM,
                50,
                seed
            )) {
                assertFalse("Tricky RANDOM included a word: " + label, isWord(label));
                assertFalse("Tricky RANDOM included a blank label", label.isEmpty());
                assertFalse(
                    "Tricky RANDOM included a Rubics face",
                    RubicsFaceCatalog.isToken(label)
                );
                assertFalse("Tricky RANDOM included a number", NumberCatalog.isNumber(label));
            }
        }
    }

    @Test
    public void wordsHaveAtMostFiveLetters() {
        Set<String> words = collectEntireCategory(IconCatalog.Category.WORDS);
        assertEquals(
            IconCatalog.availableIconCount(IconCatalog.Category.WORDS),
            words.size()
        );
        for (String word : words) {
            assertTrue(word, isWord(word));
            assertTrue(word, word.length() <= 5);
        }

        for (String word : IconCatalog.trickyIconsFor(
            IconCatalog.Category.WORDS,
            50,
            17L
        )) {
            assertTrue(word, isWord(word));
            assertTrue(word, word.length() <= 5);
        }
    }

    @Test
    public void trickyWordsAreReturnedAsConfusableNeighbors() {
        Set<Set<String>> expectedPairs = new HashSet<>();
        addPair(expectedPairs, "DEER", "DEAR");
        addPair(expectedPairs, "BEAR", "BARE");
        addPair(expectedPairs, "PAIR", "PEAR");
        addPair(expectedPairs, "MAIL", "MALE");
        addPair(expectedPairs, "SEA", "SEE");
        addPair(expectedPairs, "SUN", "SON");
        addPair(expectedPairs, "ONE", "WON");
        addPair(expectedPairs, "TWO", "TOO");
        addPair(expectedPairs, "BLUE", "BLEW");
        addPair(expectedPairs, "READ", "REED");
        addPair(expectedPairs, "MEET", "MEAT");
        addPair(expectedPairs, "ROAD", "RODE");
        addPair(expectedPairs, "TAIL", "TALE");
        addPair(expectedPairs, "HERE", "HEAR");
        addPair(expectedPairs, "RIGHT", "WRITE");
        addPair(expectedPairs, "PEACE", "PIECE");
        addPair(expectedPairs, "WEEK", "WEAK");
        addPair(expectedPairs, "SALE", "SAIL");
        addPair(expectedPairs, "PLAIN", "PLANE");
        addPair(expectedPairs, "HOLE", "WHOLE");
        addPair(expectedPairs, "BREAK", "BRAKE");
        addPair(expectedPairs, "FAIR", "FARE");
        addPair(expectedPairs, "CELL", "SELL");
        addPair(expectedPairs, "DYE", "DIE");
        addPair(expectedPairs, "NEW", "KNEW");

        String[] labels = IconCatalog.trickyIconsFor(
            IconCatalog.Category.WORDS,
            50,
            1234L
        );
        assertEquals(50, new HashSet<>(Arrays.asList(labels)).size());
        for (int i = 0; i < labels.length; i += 2) {
            assertTrue(
                labels[i] + " / " + labels[i + 1],
                expectedPairs.contains(pairOf(labels[i], labels[i + 1]))
            );
        }
    }

    @Test
    public void oddTrickyWordBoardsEndWithAConfusableTriple() {
        Set<String> expectedTriple = new HashSet<>(Arrays.asList("TO", "TWO", "TOO"));
        for (int count : new int[] {5, 29, 49}) {
            String[] labels = IconCatalog.trickyIconsFor(
                IconCatalog.Category.WORDS,
                count,
                321L
            );
            Set<String> actualTriple = new HashSet<>(Arrays.asList(
                labels[count - 3],
                labels[count - 2],
                labels[count - 1]
            ));
            assertEquals(expectedTriple, actualTriple);
        }
    }

    @Test
    public void trickyRubicsFacesStayPairedAsOneStickerNeighbors() {
        String[] labels = IconCatalog.trickyIconsFor(
            IconCatalog.Category.RUBICS,
            50,
            1234L
        );

        assertEquals(50, new HashSet<>(Arrays.asList(labels)).size());
        for (int index = 0; index < labels.length; index += 2) {
            assertTrue(RubicsFaceCatalog.isToken(labels[index]));
            assertTrue(RubicsFaceCatalog.isToken(labels[index + 1]));
            assertEquals(
                1,
                RubicsFaceCatalog.hammingDistance(labels[index], labels[index + 1])
            );
        }
    }

    @Test
    public void oddTrickyRubicsBoardsEndWithTheOneStickerTriple() {
        Set<String> expectedTriple = new HashSet<>(RubicsFaceCatalog.trickyTriple());
        for (int count : new int[] {5, 29, 49}) {
            String[] labels = IconCatalog.trickyIconsFor(
                IconCatalog.Category.RUBICS,
                count,
                321L
            );
            Set<String> actualTriple = new HashSet<>(Arrays.asList(
                labels[count - 3],
                labels[count - 2],
                labels[count - 1]
            ));
            assertEquals(expectedTriple, actualTriple);
        }
    }

    @Test
    public void trickyNumbersStayPairedAsDigitSwaps() {
        String[] labels = IconCatalog.trickyIconsFor(
            IconCatalog.Category.NUMBERS,
            50,
            1234L
        );

        assertEquals(50, new HashSet<>(Arrays.asList(labels)).size());
        for (int index = 0; index < labels.length; index += 2) {
            assertTrue(NumberCatalog.isNumber(labels[index]));
            assertTrue(NumberCatalog.isNumber(labels[index + 1]));
            assertTrue(NumberCatalog.isSingleDigitSwap(labels[index], labels[index + 1]));
        }
    }

    @Test
    public void oddTrickyNumberBoardsEndWithTheSwapTriple() {
        Set<String> expectedTriple = new HashSet<>(NumberCatalog.trickyTriple());
        for (int count : new int[] {5, 29, 49}) {
            String[] labels = IconCatalog.trickyIconsFor(
                IconCatalog.Category.NUMBERS,
                count,
                321L
            );
            Set<String> actualTriple = new HashSet<>(Arrays.asList(
                labels[count - 3],
                labels[count - 2],
                labels[count - 1]
            ));
            assertEquals(expectedTriple, actualTriple);
        }
    }

    @Test
    public void everyTrickyCategorySupportsOneThroughFiftyLabels() {
        for (IconCatalog.Category category : IconCatalog.Category.values()) {
            for (int count : new int[] {1, 3, 4, 5, 12, 29, 30, 49, 50}) {
                String[] labels = IconCatalog.iconsFor(category, count, 9981L, true);
                assertEquals(category.getDisplayName(), count, labels.length);
                if (category == IconCatalog.Category.BLANK) {
                    for (String label : labels) {
                        assertTrue(label.isEmpty());
                    }
                } else {
                    assertEquals(
                        category.getDisplayName(),
                        count,
                        new HashSet<>(Arrays.asList(labels)).size()
                    );
                }
            }
        }
    }

    @Test
    public void emojiCatalogAvoidsModernCompositionSequences() {
        for (IconCatalog.Category category : IconCatalog.Category.values()) {
            if (category == IconCatalog.Category.WORDS
                || category == IconCatalog.Category.BLANK
                || category == IconCatalog.Category.RUBICS
                || category == IconCatalog.Category.NUMBERS) {
                continue;
            }
            Set<String> labels = collectEntireCategory(category);
            for (String label : labels) {
                assertFalse("ZWJ sequence: " + label, label.contains("\u200D"));
                assertFalse("Variation sequence: " + label, label.contains("\uFE0F"));
                for (int offset = 0; offset < label.length();) {
                    int codePoint = label.codePointAt(offset);
                    assertFalse(
                        "Skin-tone composition: " + label,
                        codePoint >= 0x1F3FB && codePoint <= 0x1F3FF
                    );
                    offset += Character.charCount(codePoint);
                }
            }
        }
    }

    @Test
    public void returnedSelectionDoesNotExposeCatalogStorage() {
        String[] labels = IconCatalog.iconsFor(IconCatalog.Category.FACES, 4, 1L);
        labels[0] = "test";

        assertNotEquals(
            "test",
            IconCatalog.iconsFor(IconCatalog.Category.FACES, 4, 1L)[0]
        );
    }

    @Test
    public void rejectsInvalidRequests() {
        assertInvalid(null, 4, false);
        assertInvalid(IconCatalog.Category.FACES, 0, false);
        assertInvalid(IconCatalog.Category.FACES, 51, false);
        assertInvalid(null, 4, true);
        assertInvalid(IconCatalog.Category.FACES, 0, true);
        assertInvalid(IconCatalog.Category.FACES, 51, true);
    }

    private static Set<String> collectEntireCategory(IconCatalog.Category category) {
        Set<String> labels = new LinkedHashSet<>();
        int expected = IconCatalog.availableIconCount(category);
        for (long seed = 0; seed < 2000 && labels.size() < expected; seed++) {
            labels.addAll(Arrays.asList(IconCatalog.iconsFor(category, 50, seed)));
        }
        assertEquals(category.getDisplayName(), expected, labels.size());
        return labels;
    }

    private static boolean isWord(String label) {
        return label.matches("[A-Z]{1,5}");
    }

    private static void addPair(Set<Set<String>> pairs, String first, String second) {
        pairs.add(pairOf(first, second));
    }

    private static Set<String> pairOf(String first, String second) {
        return new HashSet<>(Arrays.asList(first, second));
    }

    private static void assertInvalid(
        IconCatalog.Category category,
        int pairCount,
        boolean tricky
    ) {
        try {
            IconCatalog.iconsFor(category, pairCount, 1L, tricky);
            fail("Expected an invalid icon request to fail");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }
}
