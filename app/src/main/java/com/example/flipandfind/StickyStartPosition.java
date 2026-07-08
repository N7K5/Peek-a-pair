package com.example.flipandfind;

/** Pure vertical geometry for handing a pinned setup action back to its scroll anchor. */
final class StickyStartPosition {
    private StickyStartPosition() {}

    static int targetTop(
        int naturalTop,
        int safeBottom,
        int buttonHeight,
        int bottomGap
    ) {
        long pinnedTop = (long) Math.max(0, safeBottom)
            - Math.max(0, buttonHeight)
            - Math.max(0, bottomGap);
        int clampedPinnedTop = (int) Math.max(0L, pinnedTop);
        return Math.min(naturalTop, clampedPinnedTop);
    }
}
