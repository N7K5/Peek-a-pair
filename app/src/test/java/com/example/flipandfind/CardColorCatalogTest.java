package com.example.flipandfind;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public final class CardColorCatalogTest {
    @Test
    public void validatesSupportedPairCount() {
        expectInvalidCount(3);
        expectInvalidCount(51);
        assertEquals(4, CardColorCatalog.colorsFor(4, false, 0L).length);
        assertEquals(50, CardColorCatalog.colorsFor(50, true, 0L).length);
    }

    @Test
    public void everySupportedPaletteIsOpaqueUniqueAndUsable() {
        for (boolean tricky : new boolean[] {false, true}) {
            for (int count : new int[] {4, 5, 12, 29, 30, 49, 50}) {
                for (long seed : new long[] {0L, 1L, -1L, 42L, Long.MAX_VALUE}) {
                    int[] colors = CardColorCatalog.colorsFor(count, tricky, seed);
                    Set<Integer> unique = new HashSet<>();
                    assertEquals(count, colors.length);
                    for (int color : colors) {
                        assertEquals("alpha", 0xFF, color >>> 24);
                        assertTrue("duplicate " + hex(color), unique.add(color));

                        double lightness = toLab(color)[0];
                        assertTrue("too dark " + hex(color), lightness >= 40.0);
                        assertTrue("too light " + hex(color), lightness <= 82.0);
                    }
                }
            }
        }
    }

    @Test
    public void paletteIsDeterministicAndSeedControlled() {
        for (boolean tricky : new boolean[] {false, true}) {
            int[] first = CardColorCatalog.colorsFor(50, tricky, 8675309L);
            int[] repeated = CardColorCatalog.colorsFor(50, tricky, 8675309L);
            int[] otherSeed = CardColorCatalog.colorsFor(50, tricky, 8675310L);
            assertArrayEquals(first, repeated);
            assertFalse(Arrays.equals(first, otherSeed));
        }
    }

    @Test
    public void colorBlindPaletteIsDeterministicOpaqueAndUniqueAtMaximumSize() {
        for (boolean tricky : new boolean[] {false, true}) {
            int[] first = CardColorCatalog.colorsFor(50, tricky, 314159L, true);
            int[] repeated = CardColorCatalog.colorsFor(50, tricky, 314159L, true);
            int[] standard = CardColorCatalog.colorsFor(50, tricky, 314159L, false);
            assertArrayEquals(first, repeated);
            assertArrayEquals(
                CardColorCatalog.colorsFor(50, tricky, 314159L),
                standard
            );
            Set<Integer> unique = new HashSet<>();
            for (int color : first) {
                assertEquals(0xFF, color >>> 24);
                assertTrue(unique.add(color));
            }
        }
    }

    @Test
    public void accessibleNormalPaletteKeepsVisibleSeparationAfterCardFaceTint() {
        int[] colors = CardColorCatalog.colorsFor(50, false, 271828L, true);
        int[] renderedFaces = new int[colors.length];
        for (int index = 0; index < colors.length; index++) {
            renderedFaces[index] = blendWithWhite(colors[index], 0.18);
        }
        assertTrue(minimumLabDistance(renderedFaces) >= 5.5);
    }

    @Test
    public void accessibleOddTrickyPalettesEndInASimilarTriple() {
        for (int count = 5; count <= 49; count += 2) {
            int[] colors = CardColorCatalog.colorsFor(count, true, 1618033L, true);
            int start = colors.length - 3;
            double firstSecond = labDistance(colors[start], colors[start + 1]);
            double firstThird = labDistance(colors[start], colors[start + 2]);
            double secondThird = labDistance(colors[start + 1], colors[start + 2]);
            double minimum = Math.min(firstSecond, Math.min(firstThird, secondThird));
            double maximum = Math.max(firstSecond, Math.max(firstThird, secondThird));
            assertTrue(minimum >= 3.0);
            assertTrue(maximum <= 23.0);
        }
    }

    @Test
    public void normalPalettesArePrefixStableAndFarthestFirst() {
        for (long seed : new long[] {7L, 99L, 123456789L}) {
            int[] allColors = CardColorCatalog.colorsFor(50, false, seed);
            for (int count = 4; count < allColors.length; count++) {
                assertArrayEquals(
                    Arrays.copyOf(allColors, count),
                    CardColorCatalog.colorsFor(count, false, seed)
                );
            }

            double previousNearestDistance = Double.POSITIVE_INFINITY;
            for (int index = 1; index < allColors.length; index++) {
                double nearestDistance = nearestPriorDistance(allColors, index);
                assertTrue(
                    "farthest-first distance increased at " + index,
                    nearestDistance <= previousNearestDistance + 0.01
                );
                previousNearestDistance = nearestDistance;
            }
        }
    }

    @Test
    public void normalColorsRemainPerceptuallySeparatedAtMaximumSize() {
        for (long seed = 0L; seed < 20L; seed++) {
            int[] colors = CardColorCatalog.colorsFor(50, false, seed);
            assertTrue("seed " + seed, minimumLabDistance(colors) >= 12.0);
            assertTrue("first four seed " + seed, minimumLabDistance(
                Arrays.copyOf(colors, 4)
            ) >= 55.0);
        }
    }

    @Test
    public void trickyNeighborPairsAreSimilarButVisiblyDifferent() {
        for (int count = 4; count <= 50; count += 2) {
            for (long seed : new long[] {0L, 7L, 99L, 123456L}) {
                int[] colors = CardColorCatalog.colorsFor(count, true, seed);
                for (int index = 0; index < colors.length; index += 2) {
                    double distance = labDistance(colors[index], colors[index + 1]);
                    assertTrue("pair is too alike: " + distance, distance >= 7.0);
                    assertTrue("pair is not similar: " + distance, distance <= 16.0);
                    assertTrue(
                        "pair hue diverged",
                        circularHueDistance(labHue(colors[index]), labHue(colors[index + 1]))
                            <= 7.0
                    );
                }
            }
        }
    }

    @Test
    public void oddTrickyPalettesEndInOneSimilarTriple() {
        for (int count = 5; count <= 49; count += 2) {
            for (long seed : new long[] {3L, 17L, 1001L}) {
                int[] colors = CardColorCatalog.colorsFor(count, true, seed);
                int start = colors.length - 3;
                double firstSecond = labDistance(colors[start], colors[start + 1]);
                double firstThird = labDistance(colors[start], colors[start + 2]);
                double secondThird = labDistance(colors[start + 1], colors[start + 2]);
                double minimum = Math.min(firstSecond, Math.min(firstThird, secondThird));
                double maximum = Math.max(firstSecond, Math.max(firstThird, secondThird));

                assertTrue("triple is too alike", minimum >= 5.5);
                assertTrue("triple is not similar", maximum <= 21.0);
                for (int first = start; first < colors.length; first++) {
                    for (int second = first + 1; second < colors.length; second++) {
                        assertTrue(
                            "triple hue diverged",
                            circularHueDistance(labHue(colors[first]), labHue(colors[second]))
                                <= 9.0
                        );
                    }
                }

                for (int index = 0; index < start; index += 2) {
                    double distance = labDistance(colors[index], colors[index + 1]);
                    assertTrue(distance >= 7.0 && distance <= 16.0);
                }
            }
        }
    }

    private static void expectInvalidCount(int count) {
        try {
            CardColorCatalog.colorsFor(count, false, 0L);
            fail("Expected invalid pair count " + count);
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("4"));
            assertTrue(expected.getMessage().contains("50"));
        }
    }

    private static double nearestPriorDistance(int[] colors, int index) {
        double nearest = Double.POSITIVE_INFINITY;
        for (int prior = 0; prior < index; prior++) {
            nearest = Math.min(nearest, labDistance(colors[index], colors[prior]));
        }
        return nearest;
    }

    private static double minimumLabDistance(int[] colors) {
        double minimum = Double.POSITIVE_INFINITY;
        for (int first = 0; first < colors.length; first++) {
            for (int second = first + 1; second < colors.length; second++) {
                minimum = Math.min(minimum, labDistance(colors[first], colors[second]));
            }
        }
        return minimum;
    }

    private static int blendWithWhite(int color, double whiteAmount) {
        double baseAmount = 1.0 - whiteAmount;
        int red = (int) Math.round(((color >>> 16) & 0xFF) * baseAmount + 255 * whiteAmount);
        int green = (int) Math.round(((color >>> 8) & 0xFF) * baseAmount + 255 * whiteAmount);
        int blue = (int) Math.round((color & 0xFF) * baseAmount + 255 * whiteAmount);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static double labDistance(int first, int second) {
        double[] firstLab = toLab(first);
        double[] secondLab = toLab(second);
        double lightness = firstLab[0] - secondLab[0];
        double a = firstLab[1] - secondLab[1];
        double b = firstLab[2] - secondLab[2];
        return Math.sqrt(lightness * lightness + a * a + b * b);
    }

    private static double labHue(int color) {
        double[] lab = toLab(color);
        double hue = Math.toDegrees(Math.atan2(lab[2], lab[1]));
        return hue < 0.0 ? hue + 360.0 : hue;
    }

    private static double circularHueDistance(double first, double second) {
        double distance = Math.abs(first - second);
        return Math.min(distance, 360.0 - distance);
    }

    private static double[] toLab(int argb) {
        double red = decode(((argb >>> 16) & 0xFF) / 255.0);
        double green = decode(((argb >>> 8) & 0xFF) / 255.0);
        double blue = decode((argb & 0xFF) / 255.0);
        double x = (0.4124564 * red + 0.3575761 * green + 0.1804375 * blue) / 0.95047;
        double y = 0.2126729 * red + 0.7151522 * green + 0.0721750 * blue;
        double z = (0.0193339 * red + 0.1191920 * green + 0.9503041 * blue) / 1.08883;
        double fx = pivot(x);
        double fy = pivot(y);
        double fz = pivot(z);
        return new double[] {
            116.0 * fy - 16.0,
            500.0 * (fx - fy),
            200.0 * (fy - fz),
        };
    }

    private static double decode(double component) {
        return component <= 0.04045
            ? component / 12.92
            : Math.pow((component + 0.055) / 1.055, 2.4);
    }

    private static double pivot(double component) {
        double delta = 6.0 / 29.0;
        return component > delta * delta * delta
            ? Math.cbrt(component)
            : component / (3.0 * delta * delta) + 4.0 / 29.0;
    }

    private static String hex(int color) {
        return String.format("#%08X", color);
    }
}
