package com.example.flipandfind;

/**
 * Short, distinct vibration envelopes used by {@link GameFeedback}.
 *
 * <p>The timing array alternates delay/on/off segments. API 26+ also uses the matching
 * amplitudes; Android 6 and 7 use the same timing while letting the device choose strength.</p>
 */
enum HapticPulse {
    CARD_FLIP(
        new long[] {0L, 14L},
        new int[] {0, 105}
    ),
    MATCH(
        new long[] {0L, 20L, 32L, 34L},
        new int[] {0, 150, 0, 225}
    ),
    MISS(
        new long[] {0L, 28L, 45L, 20L},
        new int[] {0, 190, 0, 110}
    );

    private final long[] timings;
    private final int[] amplitudes;

    HapticPulse(long[] timings, int[] amplitudes) {
        if (timings.length == 0 || timings.length != amplitudes.length) {
            throw new IllegalArgumentException("Haptic timings and amplitudes must align");
        }
        for (int index = 0; index < timings.length; index++) {
            if (timings[index] < 0L || amplitudes[index] < 0 || amplitudes[index] > 255) {
                throw new IllegalArgumentException("Invalid haptic envelope");
            }
        }
        this.timings = timings.clone();
        this.amplitudes = amplitudes.clone();
    }

    long[] copyTimings() {
        return timings.clone();
    }

    int[] copyAmplitudes() {
        return amplitudes.clone();
    }
}
