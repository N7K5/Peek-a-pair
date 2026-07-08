package com.example.flipandfind;

import java.util.Random;

/** Pure-Java rule for choosing who takes the first turn of a newly created round. */
public final class StartingPlayerSelector {
    private StartingPlayerSelector() {
    }

    /**
     * Returns the zero-based participant index that should start a round.
     *
     * <p>A one-player setup has two participants: the human at index 0 and the bot at index 1.
     * When randomization is disabled, Player A always starts. Supplying the round seed keeps the
     * choice independent from deck shuffling and makes the rule deterministic in tests.
     */
    public static int select(int humanPlayerCount, boolean randomize, long roundSeed) {
        if (humanPlayerCount < GameState.MIN_HUMAN_PLAYERS
            || humanPlayerCount > GameState.MAX_HUMAN_PLAYERS) {
            throw new IllegalArgumentException("Players must be between 1 and 15");
        }
        if (!randomize) {
            return 0;
        }

        int participantCount = humanPlayerCount == 1 ? 2 : humanPlayerCount;
        return new Random(mix64(roundSeed)).nextInt(participantCount);
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
