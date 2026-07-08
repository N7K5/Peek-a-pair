package com.example.flipandfind;

/** Pure horizontal-scroll geometry for keeping the active score chip visible. */
public final class ScoreScrollPlanner {
    private ScoreScrollPlanner() {}

    public static int targetX(
        int currentX,
        int viewportWidth,
        int childLeft,
        int childRight,
        int contentWidth
    ) {
        int safeViewport = Math.max(0, viewportWidth);
        int safeContent = Math.max(0, contentWidth);
        int maxScroll = Math.max(0, safeContent - safeViewport);
        int clampedCurrent = clamp(currentX, 0, maxScroll);
        if (safeViewport == 0 || childRight <= childLeft) {
            return clampedCurrent;
        }

        int viewportRight = clampedCurrent + safeViewport;
        if (childLeft >= clampedCurrent && childRight <= viewportRight) {
            return clampedCurrent;
        }

        int childWidth = childRight - childLeft;
        long desired = childWidth >= safeViewport
            ? childLeft
            : (long) childLeft - (safeViewport - childWidth) / 2L;
        return clamp(desired, 0, maxScroll);
    }

    private static int clamp(long value, int minimum, int maximum) {
        return (int) Math.max(minimum, Math.min(maximum, value));
    }
}
