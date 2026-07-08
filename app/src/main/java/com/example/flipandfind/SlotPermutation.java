package com.example.flipandfind;

import java.util.Arrays;
import java.util.Random;

/** Pure-Java helpers for mapping board children to the tabletop's packed slots. */
final class SlotPermutation {
    private SlotPermutation() {
    }

    static int[] identity(int count) {
        int[] permutation = new int[Math.max(0, count)];
        for (int index = 0; index < permutation.length; index++) {
            permutation[index] = index;
        }
        return permutation;
    }

    /** Legacy full-permutation validation retained for callers with no vacant slots. */
    static boolean isValid(int[] permutation, int count) {
        return isValid(permutation, count, count);
    }

    /** Validates an injective child-to-slot mapping with optional unoccupied slots. */
    static boolean isValid(int[] permutation, int childCount, int slotCount) {
        if (permutation == null
            || childCount < 0
            || slotCount < childCount
            || permutation.length != childCount) {
            return false;
        }
        boolean[] seen = new boolean[slotCount];
        for (int slot : permutation) {
            if (slot < 0 || slot >= slotCount || seen[slot]) {
                return false;
            }
            seen[slot] = true;
        }
        return true;
    }

    static int[] copy(int[] permutation) {
        return Arrays.copyOf(permutation, permutation.length);
    }

    static void swap(int[] permutation, int firstChildIndex, int secondChildIndex) {
        int slot = permutation[firstChildIndex];
        permutation[firstChildIndex] = permutation[secondChildIndex];
        permutation[secondChildIndex] = slot;
    }

    /**
     * Moves only the two selected children into two currently vacant slots. Their former slots
     * become the next vacancies. The supplied seed makes vacancy assignment repeatable.
     */
    static boolean relocatePairToVacancies(
        int[] permutation,
        int slotCount,
        int firstChildIndex,
        int secondChildIndex,
        long randomSeed
    ) {
        int count = permutation == null ? 0 : permutation.length;
        if (count < 2
            || slotCount - count < 2
            || !isValid(permutation, count, slotCount)
            || firstChildIndex < 0
            || firstChildIndex >= count
            || secondChildIndex < 0
            || secondChildIndex >= count
            || firstChildIndex == secondChildIndex) {
            return false;
        }

        boolean[] occupied = new boolean[slotCount];
        for (int slot : permutation) {
            occupied[slot] = true;
        }
        int[] vacantSlots = new int[slotCount - count];
        int vacancyCount = 0;
        for (int slot = 0; slot < count; slot++) {
            if (!occupied[slot]) {
                vacantSlots[vacancyCount++] = slot;
            }
        }
        for (int slot = count; slot < slotCount; slot++) {
            if (!occupied[slot]) {
                vacantSlots[vacancyCount++] = slot;
            }
        }

        Random random = new Random(randomSeed);
        int firstPick = random.nextInt(vacancyCount);
        permutation[firstChildIndex] = vacantSlots[firstPick];
        vacantSlots[firstPick] = vacantSlots[vacancyCount - 1];
        vacancyCount--;
        permutation[secondChildIndex] = vacantSlots[random.nextInt(vacancyCount)];
        return true;
    }
}
