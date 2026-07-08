package com.example.flipandfind;

/** Pure WCAG contrast helpers used for text drawn over player colors. */
public final class ContrastColors {
    public static final int BLACK = 0xFF000000;
    public static final int WHITE = 0xFFFFFFFF;

    private ContrastColors() {}

    public static int blackOrWhiteFor(int backgroundColor) {
        double blackContrast = contrastRatio(backgroundColor, BLACK);
        double whiteContrast = contrastRatio(backgroundColor, WHITE);
        return blackContrast >= whiteContrast ? BLACK : WHITE;
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
