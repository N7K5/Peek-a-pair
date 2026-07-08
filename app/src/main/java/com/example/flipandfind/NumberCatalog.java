package com.example.flipandfind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Stable, locale-independent number labels used by the Numbers card category. */
public final class NumberCatalog {
    public static final int MAX_NUMBER_COUNT = 50;

    /*
     * These are a length-three decimal code with a minimum Hamming distance of
     * two. In normal mode, no two labels can be confused by overlooking only a
     * single digit position.
     */
    private static final List<String> NORMAL_NUMBERS = numberPool(
        "101", "112", "123", "134", "145", "156", "167", "178", "189", "190",
        "202", "213", "224", "235", "246", "257", "268", "279", "280", "291",
        "303", "314", "325", "336", "347", "358", "369", "370", "381", "392",
        "404", "415", "426", "437", "448", "459", "460", "471", "482", "493",
        "505", "516", "527", "538", "549", "550", "561", "572", "583", "594"
    );

    /* Every neighbor is formed by swapping exactly two distinct digits. */
    private static final List<List<String>> TRICKY_GROUPS = trickyPairs(
        "487", "478", "236", "263", "519", "591", "742", "724", "860", "806",
        "351", "315", "927", "972", "164", "146", "583", "538", "709", "790",
        "425", "452", "618", "681", "273", "237", "946", "964", "130", "103",
        "857", "875", "294", "249", "761", "716", "508", "580", "342", "324",
        "695", "659", "817", "871", "204", "240", "953", "935", "126", "162"
    );

    /* Repeated digits make all three permutations one-swap neighbors. */
    private static final List<String> TRICKY_TRIPLE = trickyTriple(
        "112", "121", "211"
    );

    private NumberCatalog() {}

    /** Returns the stable normal-mode pool in canonical order. */
    public static List<String> normalNumbers() {
        return NORMAL_NUMBERS;
    }

    /** Returns 25 stable pairs whose members differ by one digit swap. */
    public static List<List<String>> trickyGroups() {
        return TRICKY_GROUPS;
    }

    /** Returns a stable three-label cluster for odd-sized tricky selections. */
    public static List<String> trickyTriple() {
        return TRICKY_TRIPLE;
    }

    /** Returns a deterministic, seed-controlled selection of distinct labels. */
    public static String[] numbersFor(int numberCount, long seed, boolean tricky) {
        validateNumberCount(numberCount);
        return tricky
            ? trickyNumbersFor(numberCount, seed)
            : normalNumbersFor(numberCount, seed);
    }

    /** Returns true for one through three ASCII decimal digits. */
    public static boolean isNumber(String value) {
        if (value == null || value.isEmpty() || value.length() > 3) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char digit = value.charAt(index);
            if (digit < '0' || digit > '9') {
                return false;
            }
        }
        return true;
    }

    /** Parses a valid label without relying on locale-sensitive number APIs. */
    public static int parseNumber(String value) {
        requireNumber(value);
        int parsed = 0;
        for (int index = 0; index < value.length(); index++) {
            parsed = parsed * 10 + value.charAt(index) - '0';
        }
        return parsed;
    }

    /** Returns the number of differing positions in two equally long labels. */
    public static int hammingDistance(String first, String second) {
        requireNumber(first);
        requireNumber(second);
        if (first.length() != second.length()) {
            throw new IllegalArgumentException("Number labels must have equal length");
        }
        int distance = 0;
        for (int index = 0; index < first.length(); index++) {
            if (first.charAt(index) != second.charAt(index)) {
                distance++;
            }
        }
        return distance;
    }

    /** Returns whether the second label is made by swapping two unlike digits. */
    public static boolean isSingleDigitSwap(String first, String second) {
        if (!isNumber(first)
            || !isNumber(second)
            || first.length() != second.length()) {
            return false;
        }
        int firstDifference = -1;
        int secondDifference = -1;
        for (int index = 0; index < first.length(); index++) {
            if (first.charAt(index) == second.charAt(index)) {
                continue;
            }
            if (firstDifference < 0) {
                firstDifference = index;
            } else if (secondDifference < 0) {
                secondDifference = index;
            } else {
                return false;
            }
        }
        return firstDifference >= 0
            && secondDifference >= 0
            && first.charAt(firstDifference) == second.charAt(secondDifference)
            && first.charAt(secondDifference) == second.charAt(firstDifference);
    }

    private static String[] normalNumbersFor(int numberCount, long seed) {
        List<String> shuffled = new ArrayList<>(NORMAL_NUMBERS);
        Collections.shuffle(shuffled, new Random(seed));
        return shuffled.subList(0, numberCount).toArray(new String[0]);
    }

    private static String[] trickyNumbersFor(int numberCount, long seed) {
        Random random = new Random(seed);
        List<List<String>> groups = new ArrayList<>(TRICKY_GROUPS);
        List<String> triple = null;
        int pairedNumberCount = numberCount;
        if (numberCount >= 3 && numberCount % 2 == 1) {
            triple = new ArrayList<>(TRICKY_TRIPLE);
            pairedNumberCount -= triple.size();
            Collections.shuffle(triple, random);
        }
        Collections.shuffle(groups, random);

        List<String> numbers = new ArrayList<>(numberCount);
        for (List<String> group : groups) {
            if (numbers.size() >= pairedNumberCount) {
                break;
            }
            int first = random.nextBoolean() ? 0 : 1;
            numbers.add(group.get(first));
            if (numbers.size() < pairedNumberCount) {
                numbers.add(group.get(1 - first));
            }
        }
        if (triple != null) {
            numbers.addAll(triple);
        }
        return numbers.toArray(new String[0]);
    }

    private static List<String> numberPool(String... labels) {
        if (labels.length < MAX_NUMBER_COUNT) {
            throw new IllegalStateException("Numbers needs at least 50 labels");
        }
        Set<String> distinct = new LinkedHashSet<>(Arrays.asList(labels));
        if (distinct.size() != labels.length) {
            throw new IllegalStateException("Numbers contains a duplicate label");
        }
        for (String label : labels) {
            if (!isNumber(label)) {
                throw new IllegalStateException("Invalid built-in number label");
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(distinct));
    }

    private static List<List<String>> trickyPairs(String... labels) {
        if (labels.length != MAX_NUMBER_COUNT || labels.length % 2 != 0) {
            throw new IllegalStateException("Numbers needs exactly 25 tricky pairs");
        }
        Set<String> distinct = new LinkedHashSet<>(Arrays.asList(labels));
        if (distinct.size() != labels.length) {
            throw new IllegalStateException("Tricky numbers contains a duplicate label");
        }
        List<List<String>> groups = new ArrayList<>(labels.length / 2);
        for (int index = 0; index < labels.length; index += 2) {
            String first = labels[index];
            String second = labels[index + 1];
            if (!isSingleDigitSwap(first, second)) {
                throw new IllegalStateException("Tricky numbers must differ by one digit swap");
            }
            groups.add(Collections.unmodifiableList(Arrays.asList(first, second)));
        }
        return Collections.unmodifiableList(groups);
    }

    private static List<String> trickyTriple(String first, String second, String third) {
        List<String> triple = Arrays.asList(first, second, third);
        if (new LinkedHashSet<>(triple).size() != triple.size()
            || !isSingleDigitSwap(first, second)
            || !isSingleDigitSwap(first, third)
            || !isSingleDigitSwap(second, third)) {
            throw new IllegalStateException("Invalid tricky number triple");
        }
        return Collections.unmodifiableList(new ArrayList<>(triple));
    }

    private static void requireNumber(String value) {
        if (!isNumber(value)) {
            throw new IllegalArgumentException("Number must contain 1 to 3 ASCII digits");
        }
    }

    private static void validateNumberCount(int numberCount) {
        if (numberCount < 1 || numberCount > MAX_NUMBER_COUNT) {
            throw new IllegalArgumentException("Number count must be between 1 and 50");
        }
    }
}
