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

public final class NumberCatalogTest {
    @Test
    public void normalCatalogHasFiftyStableWellSeparatedNumbers() {
        List<String> numbers = NumberCatalog.normalNumbers();

        assertEquals(50, numbers.size());
        assertEquals("101", numbers.get(0));
        assertEquals("594", numbers.get(numbers.size() - 1));
        assertEquals(50, new HashSet<>(numbers).size());
        for (int first = 0; first < numbers.size(); first++) {
            String number = numbers.get(first);
            assertTrue(number, NumberCatalog.isNumber(number));
            assertEquals(3, number.length());
            for (int second = first + 1; second < numbers.size(); second++) {
                assertTrue(
                    number + " / " + numbers.get(second),
                    NumberCatalog.hammingDistance(number, numbers.get(second)) >= 2
                );
            }
        }
    }

    @Test
    public void trickyGroupsAreDistinctSingleSwapNeighbors() {
        List<List<String>> groups = NumberCatalog.trickyGroups();
        Set<String> allNumbers = new HashSet<>();

        assertEquals(25, groups.size());
        assertEquals(Arrays.asList("487", "478"), groups.get(0));
        for (List<String> group : groups) {
            assertEquals(2, group.size());
            assertTrue(NumberCatalog.isSingleDigitSwap(group.get(0), group.get(1)));
            assertEquals(2, NumberCatalog.hammingDistance(group.get(0), group.get(1)));
            assertTrue(allNumbers.add(group.get(0)));
            assertTrue(allNumbers.add(group.get(1)));
        }
        assertEquals(50, allNumbers.size());
    }

    @Test
    public void trickyTripleIsPairwiseOneSwapApart() {
        List<String> triple = NumberCatalog.trickyTriple();

        assertEquals(Arrays.asList("112", "121", "211"), triple);
        assertEquals(3, new HashSet<>(triple).size());
        for (int first = 0; first < triple.size(); first++) {
            for (int second = first + 1; second < triple.size(); second++) {
                assertTrue(
                    NumberCatalog.isSingleDigitSwap(
                        triple.get(first),
                        triple.get(second)
                    )
                );
                assertEquals(
                    2,
                    NumberCatalog.hammingDistance(
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
            for (int count = 1; count <= NumberCatalog.MAX_NUMBER_COUNT; count++) {
                String[] first = NumberCatalog.numbersFor(count, 90210L, tricky);
                String[] second = NumberCatalog.numbersFor(count, 90210L, tricky);

                assertArrayEquals(first, second);
                assertEquals(count, first.length);
                assertEquals(count, new HashSet<>(Arrays.asList(first)).size());
                for (String number : first) {
                    assertTrue(NumberCatalog.isNumber(number));
                    assertTrue(number.length() <= 3);
                }
            }
        }
    }

    @Test
    public void oddTrickySelectionsEndInConfusableTriple() {
        Set<String> expected = new HashSet<>(NumberCatalog.trickyTriple());
        for (int count : new int[] {3, 5, 29, 49}) {
            String[] numbers = NumberCatalog.numbersFor(count, 81L, true);
            Set<String> actual = new HashSet<>(Arrays.asList(
                numbers[count - 3],
                numbers[count - 2],
                numbers[count - 1]
            ));
            assertEquals(expected, actual);
        }
    }

    @Test
    public void differentSeedsChangeSelectionOrder() {
        assertFalse(Arrays.equals(
            NumberCatalog.numbersFor(20, 1L, false),
            NumberCatalog.numbersFor(20, 2L, false)
        ));
        assertFalse(Arrays.equals(
            NumberCatalog.numbersFor(20, 1L, true),
            NumberCatalog.numbersFor(20, 2L, true)
        ));
    }

    @Test
    public void recognizesAndParsesOneToThreeAsciiDigits() {
        assertTrue(NumberCatalog.isNumber("0"));
        assertTrue(NumberCatalog.isNumber("07"));
        assertTrue(NumberCatalog.isNumber("487"));
        assertEquals(0, NumberCatalog.parseNumber("0"));
        assertEquals(7, NumberCatalog.parseNumber("07"));
        assertEquals(487, NumberCatalog.parseNumber("487"));

        for (String invalid : new String[] {
            null, "", "1234", "-1", "+1", "1.0", " 1", "1 ", "１２", "١٢"
        }) {
            assertFalse(String.valueOf(invalid), NumberCatalog.isNumber(invalid));
            assertInvalidParse(invalid);
        }
    }

    @Test
    public void distanceAndSwapHelpersRejectLookalikesThatAreNotSwaps() {
        assertEquals(0, NumberCatalog.hammingDistance("487", "487"));
        assertEquals(1, NumberCatalog.hammingDistance("487", "486"));
        assertEquals(2, NumberCatalog.hammingDistance("487", "478"));
        assertEquals(3, NumberCatalog.hammingDistance("487", "210"));

        assertTrue(NumberCatalog.isSingleDigitSwap("487", "478"));
        assertFalse(NumberCatalog.isSingleDigitSwap("487", "487"));
        assertFalse(NumberCatalog.isSingleDigitSwap("487", "486"));
        assertFalse(NumberCatalog.isSingleDigitSwap("487", "748"));
        assertFalse(NumberCatalog.isSingleDigitSwap("12", "102"));
        assertFalse(NumberCatalog.isSingleDigitSwap(null, "12"));

        assertInvalidDistance("1", "01");
        assertInvalidDistance("one", "two");
    }

    @Test
    public void catalogCollectionsAreImmutable() {
        assertUnmodifiable(NumberCatalog.normalNumbers());
        assertUnmodifiable(NumberCatalog.trickyGroups());
        assertUnmodifiable(NumberCatalog.trickyGroups().get(0));
        assertUnmodifiable(NumberCatalog.trickyTriple());
    }

    @Test
    public void rejectsSelectionCountsOutsideSupportedRange() {
        assertInvalidCount(0);
        assertInvalidCount(NumberCatalog.MAX_NUMBER_COUNT + 1);
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

    private static void assertInvalidParse(String value) {
        try {
            NumberCatalog.parseNumber(value);
            fail("Expected invalid number to fail");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void assertInvalidDistance(String first, String second) {
        try {
            NumberCatalog.hammingDistance(first, second);
            fail("Expected incompatible number labels to fail");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void assertInvalidCount(int count) {
        try {
            NumberCatalog.numbersFor(count, 1L, false);
            fail("Expected invalid number count to fail");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }
}
