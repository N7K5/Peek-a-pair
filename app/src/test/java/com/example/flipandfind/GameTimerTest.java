package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class GameTimerTest {
    @Test
    public void excludesPausedTimeFromActiveDuration() {
        GameTimer timer = new GameTimer(1_000L);

        timer.pause(4_000L);
        assertFalse(timer.isRunning());
        assertEquals(3_000L, timer.getElapsedMillis(20_000L));
        timer.resume(30_000L);

        assertTrue(timer.isRunning());
        assertEquals(5_500L, timer.getElapsedMillis(32_500L));
    }

    @Test
    public void restoreContinuesRunningWithoutCountingRestoreGap() {
        GameTimer original = new GameTimer(10_000L);
        long savedElapsed = original.getElapsedMillis(13_000L);

        GameTimer restored = GameTimer.restore(savedElapsed, true, 50_000L);

        assertEquals(4_000L, restored.getElapsedMillis(51_000L));
    }

    @Test
    public void pausedRestoreRemainsPausedUntilResumed() {
        GameTimer restored = GameTimer.restore(7_000L, false, 50_000L);

        assertEquals(7_000L, restored.getElapsedMillis(90_000L));
        restored.resume(100_000L);
        assertEquals(8_000L, restored.getElapsedMillis(101_000L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void runningClockCannotMoveBackwards() {
        new GameTimer(10_000L).getElapsedMillis(9_999L);
    }
}
