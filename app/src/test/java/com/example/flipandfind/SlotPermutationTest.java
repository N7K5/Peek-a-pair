package com.example.flipandfind;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SlotPermutationTest {
    @Test
    public void identityMapsEveryChildToItsOwnSlot() {
        assertArrayEquals(new int[] {0, 1, 2, 3}, SlotPermutation.identity(4));
        assertArrayEquals(new int[0], SlotPermutation.identity(0));
    }

    @Test
    public void swappingTwiceRestoresTheOriginalMapping() {
        int[] permutation = SlotPermutation.identity(6);

        SlotPermutation.swap(permutation, 1, 4);
        assertArrayEquals(new int[] {0, 4, 2, 3, 1, 5}, permutation);
        assertTrue(SlotPermutation.isValid(permutation, 6));

        SlotPermutation.swap(permutation, 1, 4);
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5}, permutation);
    }

    @Test
    public void repeatedSwapsRemainABijection() {
        int[] permutation = SlotPermutation.identity(8);
        int[][] swaps = {{0, 7}, {2, 5}, {7, 3}, {1, 6}, {5, 0}};

        for (int[] swap : swaps) {
            SlotPermutation.swap(permutation, swap[0], swap[1]);
            assertTrue(SlotPermutation.isValid(permutation, 8));
        }
    }

    @Test
    public void validationRejectsMissingDuplicateAndOutOfRangeSlots() {
        assertFalse(SlotPermutation.isValid(null, 2));
        assertFalse(SlotPermutation.isValid(new int[] {0}, 2));
        assertFalse(SlotPermutation.isValid(new int[] {0, 0}, 2));
        assertFalse(SlotPermutation.isValid(new int[] {0, 2}, 2));
        assertFalse(SlotPermutation.isValid(new int[] {-1, 0}, 2));
        assertTrue(SlotPermutation.isValid(new int[] {1, 0}, 2));
    }

    @Test
    public void copiedStateDoesNotExposeTheLivePermutation() {
        int[] original = {2, 0, 1};
        int[] copy = SlotPermutation.copy(original);

        copy[0] = 0;

        assertArrayEquals(new int[] {2, 0, 1}, original);
    }

    @Test
    public void injectiveValidationAcceptsLegacyAndVacantSlotMappings() {
        assertTrue(SlotPermutation.isValid(new int[] {0, 1, 2, 3}, 4, 6));
        assertTrue(SlotPermutation.isValid(new int[] {0, 5, 2, 4}, 4, 6));
        assertFalse(SlotPermutation.isValid(new int[] {0, 5, 2}, 4, 6));
        assertFalse(SlotPermutation.isValid(new int[] {0, 5, 5, 2}, 4, 6));
        assertFalse(SlotPermutation.isValid(new int[] {0, 6, 2, 3}, 4, 6));
        assertFalse(SlotPermutation.isValid(new int[] {0, 1, 2, 3}, 4, 3));
    }

    @Test
    public void relocationMovesOnlySelectedChildrenIntoVacancies() {
        int[] mapping = SlotPermutation.identity(8);
        int[] before = SlotPermutation.copy(mapping);

        assertTrue(SlotPermutation.relocatePairToVacancies(mapping, 10, 1, 6, 72L));

        for (int child = 0; child < mapping.length; child++) {
            if (child == 1 || child == 6) {
                assertTrue(mapping[child] == 8 || mapping[child] == 9);
            } else {
                assertEquals(before[child], mapping[child]);
            }
        }
        assertTrue(mapping[1] != mapping[6]);
        assertTrue(SlotPermutation.isValid(mapping, 8, 10));
    }

    @Test
    public void oldSelectedSlotsBecomeTheNextVacancies() {
        int[] mapping = SlotPermutation.identity(6);
        assertTrue(SlotPermutation.relocatePairToVacancies(mapping, 8, 1, 4, 19L));

        int[] afterFirstMove = SlotPermutation.copy(mapping);
        assertTrue(SlotPermutation.relocatePairToVacancies(mapping, 8, 0, 5, 20L));

        assertTrue(mapping[0] == 1 || mapping[0] == 4);
        assertTrue(mapping[5] == 1 || mapping[5] == 4);
        for (int child = 1; child < 5; child++) {
            assertEquals(afterFirstMove[child], mapping[child]);
        }
        assertTrue(SlotPermutation.isValid(mapping, 6, 8));
    }

    @Test
    public void relocationIsDeterministicForTheSameSeedAndStartingState() {
        int[] first = {0, 7, 2, 3, 6, 5};
        int[] second = SlotPermutation.copy(first);

        assertTrue(SlotPermutation.relocatePairToVacancies(first, 8, 0, 4, 9981L));
        assertTrue(SlotPermutation.relocatePairToVacancies(second, 8, 0, 4, 9981L));

        assertArrayEquals(first, second);
    }

    @Test
    public void twoChildBoardCanRelocateWithoutMovingAnythingElse() {
        int[] mapping = SlotPermutation.identity(2);

        assertTrue(SlotPermutation.relocatePairToVacancies(mapping, 4, 0, 1, 4L));

        assertTrue(mapping[0] == 2 || mapping[0] == 3);
        assertTrue(mapping[1] == 2 || mapping[1] == 3);
        assertTrue(mapping[0] != mapping[1]);
        assertTrue(SlotPermutation.isValid(mapping, 2, 4));
    }

    @Test
    public void repeatedRelocationsOfOneHundredCardsRemainInjective() {
        int[] mapping = SlotPermutation.identity(100);
        for (long seed = 0; seed < 100; seed++) {
            int firstChild = (int) (seed % mapping.length);
            int secondChild = (firstChild + 37) % mapping.length;
            int[] before = SlotPermutation.copy(mapping);

            assertTrue(SlotPermutation.relocatePairToVacancies(
                mapping,
                102,
                firstChild,
                secondChild,
                seed
            ));

            int changed = 0;
            for (int index = 0; index < mapping.length; index++) {
                if (before[index] != mapping[index]) {
                    changed++;
                }
            }
            assertEquals(2, changed);
            assertTrue(SlotPermutation.isValid(mapping, 100, 102));
        }
    }

    @Test
    public void relocationRejectsImpossibleAndInvalidRequestsWithoutMutation() {
        int[] mapping = SlotPermutation.identity(4);
        int[] before = SlotPermutation.copy(mapping);
        assertFalse(SlotPermutation.relocatePairToVacancies(mapping, 5, 0, 1, 1L));
        assertFalse(SlotPermutation.relocatePairToVacancies(mapping, 6, -1, 1, 1L));
        assertFalse(SlotPermutation.relocatePairToVacancies(mapping, 6, 1, 1, 1L));
        assertFalse(SlotPermutation.relocatePairToVacancies(mapping, 6, 0, 4, 1L));
        assertArrayEquals(before, mapping);
    }
}
