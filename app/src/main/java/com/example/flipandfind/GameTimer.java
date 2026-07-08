package com.example.flipandfind;

/** Pure active-play timer driven by externally supplied elapsed-realtime values. */
public final class GameTimer {
    private long accumulatedMillis;
    private long runningSinceMillis;
    private boolean running;

    /** Starts a new timer at the supplied monotonic elapsed-realtime value. */
    public GameTimer(long nowElapsedRealtimeMillis) {
        checkClockValue(nowElapsedRealtimeMillis);
        accumulatedMillis = 0L;
        runningSinceMillis = nowElapsedRealtimeMillis;
        running = true;
    }

    private GameTimer(long accumulatedMillis, boolean running, long nowElapsedRealtimeMillis) {
        this.accumulatedMillis = accumulatedMillis;
        this.running = running;
        runningSinceMillis = nowElapsedRealtimeMillis;
    }

    /**
     * Restores a saved elapsed duration. A running timer resumes from {@code now}; time spent
     * between save and restore is intentionally not counted as active play.
     */
    public static GameTimer restore(
        long savedElapsedMillis,
        boolean wasRunning,
        long nowElapsedRealtimeMillis
    ) {
        if (savedElapsedMillis < 0L) {
            throw new IllegalArgumentException("Saved game duration cannot be negative");
        }
        checkClockValue(nowElapsedRealtimeMillis);
        return new GameTimer(savedElapsedMillis, wasRunning, nowElapsedRealtimeMillis);
    }

    public boolean isRunning() {
        return running;
    }

    /** Idempotently stops accumulating active time. */
    public void pause(long nowElapsedRealtimeMillis) {
        checkClockValue(nowElapsedRealtimeMillis);
        if (!running) {
            return;
        }
        accumulatedMillis = Math.addExact(
            accumulatedMillis,
            elapsedSinceAnchor(nowElapsedRealtimeMillis)
        );
        running = false;
    }

    /** Idempotently resumes active-time accumulation. */
    public void resume(long nowElapsedRealtimeMillis) {
        checkClockValue(nowElapsedRealtimeMillis);
        if (running) {
            // Validate monotonicity even when an accidental duplicate resume occurs.
            elapsedSinceAnchor(nowElapsedRealtimeMillis);
            return;
        }
        runningSinceMillis = nowElapsedRealtimeMillis;
        running = true;
    }

    /** Returns a saveable active duration without changing running/paused state. */
    public long getElapsedMillis(long nowElapsedRealtimeMillis) {
        checkClockValue(nowElapsedRealtimeMillis);
        if (!running) {
            return accumulatedMillis;
        }
        return Math.addExact(accumulatedMillis, elapsedSinceAnchor(nowElapsedRealtimeMillis));
    }

    private long elapsedSinceAnchor(long nowElapsedRealtimeMillis) {
        if (nowElapsedRealtimeMillis < runningSinceMillis) {
            throw new IllegalArgumentException("Elapsed realtime moved backwards");
        }
        return nowElapsedRealtimeMillis - runningSinceMillis;
    }

    private static void checkClockValue(long value) {
        if (value < 0L) {
            throw new IllegalArgumentException("Elapsed realtime cannot be negative");
        }
    }
}
