package com.example.flipandfind;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class StickyStartPositionTest {
    private static final int SAFE_BOTTOM = 800;
    private static final int BUTTON_HEIGHT = 58;
    private static final int BOTTOM_GAP = 14;
    private static final int PINNED_TOP = 728;

    @Test
    public void anchorBelowPinnedPositionUsesPinnedTop() {
        assertEquals(
            PINNED_TOP,
            StickyStartPosition.targetTop(900, SAFE_BOTTOM, BUTTON_HEIGHT, BOTTOM_GAP)
        );
    }

    @Test
    public void anchorAtPinnedPositionHandsOffWithoutAJump() {
        assertEquals(
            PINNED_TOP,
            StickyStartPosition.targetTop(
                PINNED_TOP,
                SAFE_BOTTOM,
                BUTTON_HEIGHT,
                BOTTOM_GAP
            )
        );
    }

    @Test
    public void anchorAbovePinnedPositionUsesNaturalTop() {
        assertEquals(
            600,
            StickyStartPosition.targetTop(600, SAFE_BOTTOM, BUTTON_HEIGHT, BOTTOM_GAP)
        );
    }

    @Test
    public void hostShorterThanButtonAndGapClampsPinnedTopToZero() {
        assertEquals(
            0,
            StickyStartPosition.targetTop(300, 50, BUTTON_HEIGHT, BOTTOM_GAP)
        );
    }
}
