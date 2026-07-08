package com.example.flipandfind;

/** Pure WCAG contrast helpers for app palettes, controls, player colors, and card artwork. */
public final class ContrastColors {
    public static final int BLACK = 0xFF000000;
    public static final int WHITE = 0xFFFFFFFF;

    private ContrastColors() {}

    public static int blackOrWhiteFor(int backgroundColor) {
        double blackContrast = contrastRatio(backgroundColor, BLACK);
        double whiteContrast = contrastRatio(backgroundColor, WHITE);
        return blackContrast >= whiteContrast ? BLACK : WHITE;
    }

    /**
     * Moves a foreground toward a known contrast target until it clears the requested ratio on
     * every supplied background. The result is always opaque and the original hue is retained as
     * far as the contrast requirement allows.
     */
    static int ensureMinimumContrast(
        int foreground,
        int contrastTarget,
        double minimumRatio,
        int... backgrounds
    ) {
        int adjusted = opaque(foreground);
        if (backgrounds == null || backgrounds.length == 0 || minimumRatio <= 1d) {
            return adjusted;
        }
        int opaqueTarget = opaque(contrastTarget);
        for (int pass = 0; pass < 16; pass++) {
            if (meetsContrastOnEveryBackground(adjusted, minimumRatio, backgrounds)) {
                return adjusted;
            }
            adjusted = blend(adjusted, opaqueTarget, 0.20d);
        }
        return opaqueTarget;
    }

    private static boolean meetsContrastOnEveryBackground(
        int foreground,
        double minimumRatio,
        int[] backgrounds
    ) {
        for (int background : backgrounds) {
            if (contrastRatio(foreground, background) < minimumRatio) {
                return false;
            }
        }
        return true;
    }

    private static int blend(int base, int overlay, double overlayAmount) {
        double amount = Math.max(0d, Math.min(1d, overlayAmount));
        double baseAmount = 1d - amount;
        int red = (int) Math.round(component(base, 16) * baseAmount
            + component(overlay, 16) * amount);
        int green = (int) Math.round(component(base, 8) * baseAmount
            + component(overlay, 8) * amount);
        int blue = (int) Math.round(component(base, 0) * baseAmount
            + component(overlay, 0) * amount);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static int opaque(int color) {
        return color | 0xFF000000;
    }

    private static int component(int color, int shift) {
        return (color >>> shift) & 0xFF;
    }

    static double contrastRatio(int first, int second) {
        double firstLuminance = relativeLuminance(first);
        double secondLuminance = relativeLuminance(second);
        return (Math.max(firstLuminance, secondLuminance) + 0.05)
            / (Math.min(firstLuminance, secondLuminance) + 0.05);
    }

    private static double relativeLuminance(int color) {
        return 0.2126 * linear((color >>> 16) & 0xFF)
            + 0.7152 * linear((color >>> 8) & 0xFF)
            + 0.0722 * linear(color & 0xFF);
    }

    private static double linear(int component) {
        double value = component / 255.0;
        return value <= 0.04045
            ? value / 12.92
            : Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
