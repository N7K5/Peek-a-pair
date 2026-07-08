package com.example.flipandfind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Stable, text-encoded 3x3 cube faces used by the Rubics card category.
 *
 * <p>A token starts with {@code RBC1:} and contains nine row-major color IDs.
 * The encoding is deliberately ASCII-only so tokens can safely travel through
 * the existing card-label and saved-state paths without depending on emoji or
 * a particular Android font.</p>
 */
public final class RubicsFaceCatalog {
    public static final int FACE_EDGE = 3;
    public static final int STICKER_COUNT = FACE_EDGE * FACE_EDGE;
    public static final int MAX_PATTERN_COUNT = 50;

    public static final int WHITE = 0;
    public static final int YELLOW = 1;
    public static final int RED = 2;
    public static final int ORANGE = 3;
    public static final int BLUE = 4;
    public static final int GREEN = 5;

    private static final String TOKEN_PREFIX = "RBC1:";
    private static final String[] COLOR_NAMES = {
        "white", "yellow", "red", "orange", "blue", "green"
    };

    /*
     * These faces are intentionally fixed rather than generated from a caller's
     * seed. Every pair of normal faces differs in at least six of nine cells.
     * That makes normal-mode faces recognizable by both color and layout, even
     * when the cards are rendered fairly small.
     */
    private static final List<String> NORMAL_PATTERNS = encodedPatterns(
        "230513431", "251420315", "315103410", "014345104", "110413052",
        "320121042", "205104155", "222405030", "505205321", "253151352",
        "552331201", "315354341", "355425101", "132444011", "050533524",
        "302512513", "254352523", "253214013", "143032043", "333551520",
        "040144512", "035111234", "021524411", "442233154", "020255143",
        "441001334", "240531055", "524550242", "535022114", "515212030",
        "430035403", "125002551", "053123240", "422350125", "105140524",
        "313243025", "012452534", "520143204", "534340515", "402111440",
        "131451402", "145323145", "405321313", "211434251", "054413135",
        "345040431", "342403322", "341250052", "423542452", "503025533"
    );

    private static final List<List<String>> TRICKY_GROUPS = createTrickyGroups();
    private static final List<String> TRICKY_TRIPLE = createTrickyTriple();

    private RubicsFaceCatalog() {}

    /** Returns the stable normal-mode pool in canonical order. */
    public static List<String> normalPatterns() {
        return NORMAL_PATTERNS;
    }

    /**
     * Returns 25 stable two-face groups. The two faces in every group differ in
     * exactly one cell, while faces belonging to different groups remain much
     * farther apart.
     */
    public static List<List<String>> trickyGroups() {
        return TRICKY_GROUPS;
    }

    /** Returns a stable three-face, one-cell cluster for odd-sized selections. */
    public static List<String> trickyTriple() {
        return TRICKY_TRIPLE;
    }

    /**
     * Returns a deterministic selection for callers that do not need to merge
     * this catalog into another icon-selection implementation.
     */
    public static String[] patternsFor(
        int patternCount,
        long seed,
        boolean tricky
    ) {
        validatePatternCount(patternCount);
        return tricky
            ? trickyPatternsFor(patternCount, seed)
            : normalPatternsFor(patternCount, seed);
    }

    /** Returns whether {@code value} is a complete, supported cube-face token. */
    public static boolean isToken(String value) {
        if (value == null
            || value.length() != TOKEN_PREFIX.length() + STICKER_COUNT
            || !value.startsWith(TOKEN_PREFIX)) {
            return false;
        }
        for (int index = TOKEN_PREFIX.length(); index < value.length(); index++) {
            char color = value.charAt(index);
            if (color < '0' || color > '5') {
                return false;
            }
        }
        return true;
    }

    /** Decodes a token to a new row-major array of color IDs from 0 through 5. */
    public static int[] decodeColors(String token) {
        requireToken(token);
        int[] colors = new int[STICKER_COUNT];
        for (int index = 0; index < colors.length; index++) {
            colors[index] = token.charAt(TOKEN_PREFIX.length() + index) - '0';
        }
        return colors;
    }

    /** Encodes exactly nine row-major color IDs into a stable ASCII token. */
    public static String encodeColors(int... colors) {
        if (colors == null || colors.length != STICKER_COUNT) {
            throw new IllegalArgumentException("A cube face requires exactly 9 colors");
        }
        StringBuilder token = new StringBuilder(
            TOKEN_PREFIX.length() + STICKER_COUNT
        );
        token.append(TOKEN_PREFIX);
        for (int color : colors) {
            if (color < WHITE || color > GREEN) {
                throw new IllegalArgumentException("Cube color IDs must be between 0 and 5");
            }
            token.append((char) ('0' + color));
        }
        return token.toString();
    }

    /** Returns the number of sticker positions whose colors differ. */
    public static int hammingDistance(String first, String second) {
        requireToken(first);
        requireToken(second);
        int distance = 0;
        for (int index = 0; index < STICKER_COUNT; index++) {
            if (first.charAt(TOKEN_PREFIX.length() + index)
                != second.charAt(TOKEN_PREFIX.length() + index)) {
                distance++;
            }
        }
        return distance;
    }

    /** Returns a complete, TalkBack-ready description of the three rows. */
    public static String spokenDescription(String token) {
        int[] colors = decodeColors(token);
        StringBuilder description = new StringBuilder("Rubik's cube face. ");
        appendSpokenRow(description, "Top", colors, 0);
        description.append(' ');
        appendSpokenRow(description, "Middle", colors, FACE_EDGE);
        description.append(' ');
        appendSpokenRow(description, "Bottom", colors, FACE_EDGE * 2);
        return description.toString();
    }

    private static String[] normalPatternsFor(int patternCount, long seed) {
        List<String> shuffled = new ArrayList<>(NORMAL_PATTERNS);
        Collections.shuffle(shuffled, new Random(seed));
        return shuffled.subList(0, patternCount).toArray(new String[0]);
    }

    private static String[] trickyPatternsFor(int patternCount, long seed) {
        Random random = new Random(seed);
        List<List<String>> groups = new ArrayList<>(TRICKY_GROUPS);
        int pairedPatternCount = patternCount;
        List<String> triple = null;
        if (patternCount >= 3 && patternCount % 2 == 1) {
            triple = new ArrayList<>(TRICKY_TRIPLE);
            pairedPatternCount -= triple.size();
            // The triple extends the first pair, so that pair must not be reused.
            groups.remove(0);
            Collections.shuffle(triple, random);
        }
        Collections.shuffle(groups, random);

        List<String> patterns = new ArrayList<>(patternCount);
        for (List<String> group : groups) {
            if (patterns.size() >= pairedPatternCount) {
                break;
            }
            int first = random.nextBoolean() ? 0 : 1;
            patterns.add(group.get(first));
            if (patterns.size() < pairedPatternCount) {
                patterns.add(group.get(1 - first));
            }
        }
        if (triple != null) {
            patterns.addAll(triple);
        }
        return patterns.toArray(new String[0]);
    }

    private static List<List<String>> createTrickyGroups() {
        List<List<String>> groups = new ArrayList<>(MAX_PATTERN_COUNT / 2);
        for (int index = 0; index < MAX_PATTERN_COUNT / 2; index++) {
            String anchor = NORMAL_PATTERNS.get(index);
            int changedCell = trickyCell(index);
            int originalColor = encodedColorAt(anchor, changedCell);
            int changedColor = (originalColor + 1 + index % 5) % COLOR_NAMES.length;
            String neighbor = replaceColor(anchor, changedCell, changedColor);
            groups.add(Collections.unmodifiableList(Arrays.asList(anchor, neighbor)));
        }
        return Collections.unmodifiableList(groups);
    }

    private static List<String> createTrickyTriple() {
        String anchor = TRICKY_GROUPS.get(0).get(0);
        String neighbor = TRICKY_GROUPS.get(0).get(1);
        int changedCell = trickyCell(0);
        int originalColor = encodedColorAt(anchor, changedCell);
        int neighborColor = encodedColorAt(neighbor, changedCell);
        int thirdColor = (neighborColor + 1) % COLOR_NAMES.length;
        if (thirdColor == originalColor) {
            thirdColor = (thirdColor + 1) % COLOR_NAMES.length;
        }
        return Collections.unmodifiableList(Arrays.asList(
            anchor,
            neighbor,
            replaceColor(anchor, changedCell, thirdColor)
        ));
    }

    private static int trickyCell(int groupIndex) {
        // A coprime step spreads the distinguishing cell around the 3x3 face.
        return (groupIndex * 5 + 2) % STICKER_COUNT;
    }

    private static int encodedColorAt(String token, int cell) {
        return token.charAt(TOKEN_PREFIX.length() + cell) - '0';
    }

    private static String replaceColor(String token, int cell, int color) {
        StringBuilder changed = new StringBuilder(token);
        changed.setCharAt(TOKEN_PREFIX.length() + cell, (char) ('0' + color));
        return changed.toString();
    }

    private static List<String> encodedPatterns(String... rows) {
        List<String> patterns = new ArrayList<>(rows.length);
        for (String rowMajorColors : rows) {
            if (rowMajorColors.length() != STICKER_COUNT) {
                throw new IllegalArgumentException("Invalid built-in cube face");
            }
            int[] colors = new int[STICKER_COUNT];
            for (int index = 0; index < colors.length; index++) {
                colors[index] = rowMajorColors.charAt(index) - '0';
            }
            patterns.add(encodeColors(colors));
        }
        return Collections.unmodifiableList(patterns);
    }

    private static void appendSpokenRow(
        StringBuilder description,
        String rowName,
        int[] colors,
        int offset
    ) {
        description.append(rowName).append(" row: ");
        for (int column = 0; column < FACE_EDGE; column++) {
            if (column > 0) {
                description.append(", ");
            }
            description.append(COLOR_NAMES[colors[offset + column]]);
        }
        description.append('.');
    }

    private static void requireToken(String token) {
        if (!isToken(token)) {
            throw new IllegalArgumentException("Invalid cube-face token");
        }
    }

    private static void validatePatternCount(int patternCount) {
        if (patternCount < 1 || patternCount > MAX_PATTERN_COUNT) {
            throw new IllegalArgumentException("Pattern count must be between 1 and 50");
        }
    }
}
