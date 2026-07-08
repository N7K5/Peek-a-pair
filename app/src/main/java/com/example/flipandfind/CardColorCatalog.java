package com.example.flipandfind;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Deterministic color labels for the matching game.
 *
 * <p>Colors are returned as opaque {@code 0xAARRGGBB} integers, so this class
 * stays usable from ordinary JVM tests without depending on {@code android.graphics.Color}.
 */
public final class CardColorCatalog {
    public static final int MIN_PAIR_COUNT = 4;
    public static final int MAX_PAIR_COUNT = 50;

    private static final double[] CANDIDATE_LIGHTNESS = {46.0, 62.0, 76.0};
    private static final double[] CANDIDATE_CHROMA = {38.0, 52.0};
    private static final int CANDIDATE_HUE_COUNT = 36;
    private static final int[] COLOR_BLIND_BASES = {
        0xFF0072B2, // blue
        0xFFE69F00, // orange
        0xFF56B4E9, // sky blue
        0xFFD55E00, // vermilion
        0xFF009E73, // bluish green
        0xFFCC79A7, // reddish purple
        0xFFF0E442, // yellow
        0xFF4D4D4D, // neutral charcoal
        0xFF332288, // indigo
        0xFF44AA99, // teal
    };
    private static final double[] COLOR_BLIND_VARIANTS = {
        0.0, -0.58, 0.58, -0.30, 0.30
    };

    private CardColorCatalog() {}

    /**
     * Returns one unique color for each pair ID.
     *
     * <p>Normal mode greedily orders colors by their distance from every color
     * already chosen. The distance is measured in CIE L*a*b*, rather than raw
     * RGB, so early pair IDs receive especially distinct colors. Tricky mode
     * arranges IDs as similar pairs ({@code 0/1}, {@code 2/3}, and so on). An
     * odd-sized tricky palette ends with one similar group of three colors.
     *
     * @param pairCount number of matching pair IDs, from 4 through 50
     * @param tricky whether neighboring IDs should be deliberately confusable
     * @param seed controls the palette's hue rotation and deterministic order
     */
    public static int[] colorsFor(int pairCount, boolean tricky, long seed) {
        validatePairCount(pairCount);
        return tricky
            ? trickyColorsFor(pairCount, seed)
            : distinctColorsFor(pairCount, seed);
    }

    /**
     * Returns either the standard palette or a palette based on color-vision-safe anchor hues.
     * The accessible palette remains unique through shade variants; the UI also combines it
     * with a pair-specific non-color pattern cue.
     */
    public static int[] colorsFor(
        int pairCount,
        boolean tricky,
        long seed,
        boolean colorBlindSafe
    ) {
        validatePairCount(pairCount);
        if (!colorBlindSafe) {
            return colorsFor(pairCount, tricky, seed);
        }
        return colorBlindColorsFor(pairCount, tricky, seed);
    }

    private static int[] colorBlindColorsFor(int pairCount, boolean tricky, long seed) {
        int[] colors = new int[pairCount];
        int rotation = floorMod(mix64(seed ^ 0x0CB1A5AFE5EEDL), COLOR_BLIND_BASES.length);
        if (!tricky) {
            for (int index = 0; index < pairCount; index++) {
                int base = COLOR_BLIND_BASES[(rotation + index) % COLOR_BLIND_BASES.length];
                int variant = index / COLOR_BLIND_BASES.length;
                colors[index] = shade(base, COLOR_BLIND_VARIANTS[variant]);
            }
            ensureUnique(colors);
            return colors;
        }

        int output = 0;
        int tripleSize = (pairCount & 1) == 0 ? 0 : 3;
        int pairGroupCount = (pairCount - tripleSize) / 2;
        for (int group = 0; group < pairGroupCount; group++) {
            int base = COLOR_BLIND_BASES[(rotation + group) % COLOR_BLIND_BASES.length];
            double center = colorBlindGroupCenter(group);
            boolean reverse = (mix64(seed + group * 0x9E3779B97F4A7C15L) & 1L) != 0L;
            int first = shade(base, center - 0.065);
            int second = shade(base, center + 0.065);
            colors[output++] = reverse ? second : first;
            colors[output++] = reverse ? first : second;
        }
        if (tripleSize != 0) {
            int group = pairGroupCount;
            int base = COLOR_BLIND_BASES[(rotation + group) % COLOR_BLIND_BASES.length];
            double center = colorBlindGroupCenter(group);
            int[] triple = {
                shade(base, center - 0.10),
                shade(base, center),
                shade(base, center + 0.10),
            };
            int tripleRotation = floorMod(mix64(seed ^ 0x65D200CE55B19AD8L), 3);
            boolean reverse = (mix64(seed ^ 0x4F74430C22A54005L) & 1L) != 0L;
            for (int index = 0; index < triple.length; index++) {
                int source = reverse
                    ? Math.floorMod(tripleRotation - index, triple.length)
                    : (tripleRotation + index) % triple.length;
                colors[output++] = triple[source];
            }
        }
        ensureUnique(colors);
        return colors;
    }

    private static double colorBlindGroupCenter(int group) {
        int tier = group / COLOR_BLIND_BASES.length;
        return tier == 0 ? 0.0 : tier == 1 ? -0.30 : 0.30;
    }

    private static int shade(int argb, double amount) {
        double clamped = Math.max(-0.75, Math.min(0.75, amount));
        int target = clamped >= 0.0 ? 255 : 0;
        double weight = Math.abs(clamped);
        int red = (int) Math.round(((argb >>> 16) & 0xFF) * (1.0 - weight) + target * weight);
        int green = (int) Math.round(((argb >>> 8) & 0xFF) * (1.0 - weight) + target * weight);
        int blue = (int) Math.round((argb & 0xFF) * (1.0 - weight) + target * weight);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static int[] distinctColorsFor(int pairCount, long seed) {
        List<ColorCandidate> candidates = createCandidates(seed);
        int[] colors = new int[pairCount];
        boolean[] selected = new boolean[candidates.size()];

        int firstIndex = floorMod(mix64(seed ^ 0x31B8A5106F24B7A3L), candidates.size());
        selected[firstIndex] = true;
        colors[0] = candidates.get(firstIndex).argb;

        for (int outputIndex = 1; outputIndex < pairCount; outputIndex++) {
            int bestIndex = -1;
            double bestMinimumDistance = -1.0;
            long bestTieBreaker = 0L;

            for (int candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
                if (selected[candidateIndex]) {
                    continue;
                }

                ColorCandidate candidate = candidates.get(candidateIndex);
                double minimumDistance = Double.POSITIVE_INFINITY;
                for (int priorIndex = 0; priorIndex < candidates.size(); priorIndex++) {
                    if (selected[priorIndex]) {
                        minimumDistance = Math.min(
                            minimumDistance,
                            labDistanceSquared(candidate, candidates.get(priorIndex))
                        );
                    }
                }

                long tieBreaker = mix64(
                    seed
                        ^ ((long) outputIndex * 0x9E3779B97F4A7C15L)
                        ^ ((long) candidateIndex * 0xD1B54A32D192ED03L)
                );
                if (minimumDistance > bestMinimumDistance + 1e-9
                    || (Math.abs(minimumDistance - bestMinimumDistance) <= 1e-9
                        && Long.compareUnsigned(tieBreaker, bestTieBreaker) > 0)) {
                    bestIndex = candidateIndex;
                    bestMinimumDistance = minimumDistance;
                    bestTieBreaker = tieBreaker;
                }
            }

            selected[bestIndex] = true;
            colors[outputIndex] = candidates.get(bestIndex).argb;
        }
        return colors;
    }

    private static int[] trickyColorsFor(int pairCount, long seed) {
        int tripleSize = pairCount % 2 == 0 ? 0 : 3;
        int pairGroupCount = (pairCount - tripleSize) / 2;
        int groupCount = pairGroupCount + (tripleSize == 0 ? 0 : 1);
        double[] groupHues = spreadHues(groupCount, seed);
        int[] colors = new int[pairCount];
        int outputIndex = 0;

        for (int groupIndex = 0; groupIndex < pairGroupCount; groupIndex++) {
            double hue = groupHues[groupIndex];
            int darker = labToArgb(57.0, 40.0, hue - 1.5);
            int lighter = labToArgb(67.0, 40.0, hue + 1.5);
            boolean reverse = (mix64(seed + groupIndex * 0x632BE59BD9B4E019L) & 1L) != 0L;
            colors[outputIndex++] = reverse ? lighter : darker;
            colors[outputIndex++] = reverse ? darker : lighter;
        }

        if (tripleSize != 0) {
            double hue = groupHues[groupCount - 1];
            int[] triple = {
                labToArgb(54.0, 37.0, hue - 2.0),
                labToArgb(62.0, 37.0, hue),
                labToArgb(70.0, 37.0, hue + 2.0),
            };
            int rotation = floorMod(mix64(seed ^ 0x7A4BA6C1D392E85FL), triple.length);
            boolean reverse = (mix64(seed ^ 0xCC3D4A27EB4F1659L) & 1L) != 0L;
            for (int index = 0; index < triple.length; index++) {
                int sourceIndex = reverse
                    ? floorMod(rotation - index, triple.length)
                    : (rotation + index) % triple.length;
                colors[outputIndex++] = triple[sourceIndex];
            }
        }

        ensureUnique(colors);
        return colors;
    }

    private static List<ColorCandidate> createCandidates(long seed) {
        double hueOffset = unitInterval(mix64(seed ^ 0xA0761D6478BD642FL)) * 360.0;
        List<ColorCandidate> candidates = new ArrayList<>();
        Set<Integer> seenColors = new HashSet<>();

        for (double lightness : CANDIDATE_LIGHTNESS) {
            for (double chroma : CANDIDATE_CHROMA) {
                for (int hueIndex = 0; hueIndex < CANDIDATE_HUE_COUNT; hueIndex++) {
                    double hue = hueOffset + hueIndex * (360.0 / CANDIDATE_HUE_COUNT);
                    int argb = labToArgb(lightness, chroma, hue);
                    if (seenColors.add(argb)) {
                        double[] actualLab = argbToLab(argb);
                        candidates.add(
                            new ColorCandidate(argb, actualLab[0], actualLab[1], actualLab[2])
                        );
                    }
                }
            }
        }
        return candidates;
    }

    /** Returns evenly spaced hues in a farthest-first order. */
    private static double[] spreadHues(int count, long seed) {
        double[] available = new double[count];
        double hueOffset = unitInterval(mix64(seed ^ 0xE7037ED1A0B428DBL)) * 360.0;
        for (int index = 0; index < count; index++) {
            available[index] = normalizeHue(hueOffset + index * (360.0 / count));
        }

        double[] ordered = new double[count];
        boolean[] selected = new boolean[count];
        int first = floorMod(mix64(seed ^ 0x8EBC6AF09C88C6E3L), count);
        selected[first] = true;
        ordered[0] = available[first];

        for (int outputIndex = 1; outputIndex < count; outputIndex++) {
            int bestIndex = -1;
            double bestDistance = -1.0;
            long bestTieBreaker = 0L;
            for (int candidateIndex = 0; candidateIndex < count; candidateIndex++) {
                if (selected[candidateIndex]) {
                    continue;
                }
                double minimumDistance = 180.0;
                for (int priorIndex = 0; priorIndex < outputIndex; priorIndex++) {
                    minimumDistance = Math.min(
                        minimumDistance,
                        circularHueDistance(available[candidateIndex], ordered[priorIndex])
                    );
                }
                long tieBreaker = mix64(
                    seed
                        ^ ((long) outputIndex * 0x94D049BB133111EBL)
                        ^ candidateIndex
                );
                if (minimumDistance > bestDistance + 1e-9
                    || (Math.abs(minimumDistance - bestDistance) <= 1e-9
                        && Long.compareUnsigned(tieBreaker, bestTieBreaker) > 0)) {
                    bestIndex = candidateIndex;
                    bestDistance = minimumDistance;
                    bestTieBreaker = tieBreaker;
                }
            }
            selected[bestIndex] = true;
            ordered[outputIndex] = available[bestIndex];
        }
        return ordered;
    }

    private static int labToArgb(double lightness, double chroma, double hueDegrees) {
        double hueRadians = Math.toRadians(normalizeHue(hueDegrees));
        double a = chroma * Math.cos(hueRadians);
        double b = chroma * Math.sin(hueRadians);

        double fy = (lightness + 16.0) / 116.0;
        double fx = fy + a / 500.0;
        double fz = fy - b / 200.0;
        double x = 0.95047 * labPivotInverse(fx);
        double y = labPivotInverse(fy);
        double z = 1.08883 * labPivotInverse(fz);

        double redLinear = 3.2404542 * x - 1.5371385 * y - 0.4985314 * z;
        double greenLinear = -0.9692660 * x + 1.8760108 * y + 0.0415560 * z;
        double blueLinear = 0.0556434 * x - 0.2040259 * y + 1.0572252 * z;
        int red = toByte(srgbEncode(redLinear));
        int green = toByte(srgbEncode(greenLinear));
        int blue = toByte(srgbEncode(blueLinear));
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static double[] argbToLab(int argb) {
        double red = srgbDecode(((argb >>> 16) & 0xFF) / 255.0);
        double green = srgbDecode(((argb >>> 8) & 0xFF) / 255.0);
        double blue = srgbDecode((argb & 0xFF) / 255.0);
        double x = (0.4124564 * red + 0.3575761 * green + 0.1804375 * blue) / 0.95047;
        double y = 0.2126729 * red + 0.7151522 * green + 0.0721750 * blue;
        double z = (0.0193339 * red + 0.1191920 * green + 0.9503041 * blue) / 1.08883;
        double fx = labPivot(x);
        double fy = labPivot(y);
        double fz = labPivot(z);
        return new double[] {
            116.0 * fy - 16.0,
            500.0 * (fx - fy),
            200.0 * (fy - fz),
        };
    }

    private static double labDistanceSquared(ColorCandidate first, ColorCandidate second) {
        double lightness = first.lightness - second.lightness;
        double a = first.a - second.a;
        double b = first.b - second.b;
        return lightness * lightness + a * a + b * b;
    }

    private static double labPivot(double component) {
        double delta = 6.0 / 29.0;
        return component > delta * delta * delta
            ? Math.cbrt(component)
            : component / (3.0 * delta * delta) + 4.0 / 29.0;
    }

    private static double labPivotInverse(double component) {
        double delta = 6.0 / 29.0;
        return component > delta
            ? component * component * component
            : 3.0 * delta * delta * (component - 4.0 / 29.0);
    }

    private static double srgbEncode(double component) {
        return component <= 0.0031308
            ? 12.92 * component
            : 1.055 * Math.pow(component, 1.0 / 2.4) - 0.055;
    }

    private static double srgbDecode(double component) {
        return component <= 0.04045
            ? component / 12.92
            : Math.pow((component + 0.055) / 1.055, 2.4);
    }

    private static int toByte(double component) {
        return (int) Math.round(Math.max(0.0, Math.min(1.0, component)) * 255.0);
    }

    private static double circularHueDistance(double first, double second) {
        double distance = Math.abs(normalizeHue(first) - normalizeHue(second));
        return Math.min(distance, 360.0 - distance);
    }

    private static double normalizeHue(double hue) {
        double normalized = hue % 360.0;
        return normalized < 0.0 ? normalized + 360.0 : normalized;
    }

    private static double unitInterval(long value) {
        return (value >>> 11) * 0x1.0p-53;
    }

    private static int floorMod(long value, int divisor) {
        return (int) Math.floorMod(value, (long) divisor);
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static void validatePairCount(int pairCount) {
        if (pairCount < MIN_PAIR_COUNT || pairCount > MAX_PAIR_COUNT) {
            throw new IllegalArgumentException("Pair count must be between 4 and 50");
        }
    }

    private static void ensureUnique(int[] colors) {
        Set<Integer> seen = new HashSet<>();
        for (int color : colors) {
            if (!seen.add(color)) {
                throw new IllegalStateException("Generated card colors must be unique");
            }
        }
    }

    private static final class ColorCandidate {
        final int argb;
        final double lightness;
        final double a;
        final double b;

        ColorCandidate(int argb, double lightness, double a, double b) {
            this.argb = argb;
            this.lightness = lightness;
            this.a = a;
            this.b = b;
        }
    }
}
