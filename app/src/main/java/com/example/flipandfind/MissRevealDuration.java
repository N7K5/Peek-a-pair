package com.example.flipandfind;

/** Pure conversions for the configurable time that a missed pair remains visible. */
public final class MissRevealDuration {
    public static final int MIN_TENTHS = 5;
    public static final int MAX_TENTHS = 30;
    public static final int DEFAULT_TENTHS = 9;

    private MissRevealDuration() {
    }

    public static int clampTenths(int tenths) {
        return Math.max(MIN_TENTHS, Math.min(MAX_TENTHS, tenths));
    }

    public static int maxProgress() {
        return MAX_TENTHS - MIN_TENTHS;
    }

    public static int progressForTenths(int tenths) {
        return clampTenths(tenths) - MIN_TENTHS;
    }

    public static int tenthsForProgress(int progress) {
        int clampedProgress = Math.max(0, Math.min(maxProgress(), progress));
        return MIN_TENTHS + clampedProgress;
    }

    public static long millisForTenths(int tenths) {
        return clampTenths(tenths) * 100L;
    }

    public static String displayText(int tenths) {
        int safeTenths = clampTenths(tenths);
        return (safeTenths / 10) + "." + (safeTenths % 10) + " seconds";
    }
}
