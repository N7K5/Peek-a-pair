package com.example.flipandfind;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class GameStatsTest {
    @Test
    public void recordsAttemptsAccuracyPairsAndStreaksPerParticipant() {
        GameStats stats = new GameStats(3);

        stats.recordAttempt(0, true);
        stats.recordAttempt(0, true);
        stats.recordAttempt(0, false);
        stats.recordAttempt(1, true);

        assertEquals(3, stats.getAttempts(0));
        assertEquals(2, stats.getPairs(0));
        assertEquals(2, stats.getMatches(0));
        assertEquals(0, stats.getCurrentStreak(0));
        assertEquals(2, stats.getLongestStreak(0));
        assertEquals(67, stats.getAccuracyPercent(0));
        assertEquals(1, stats.getAttempts(1));
        assertEquals(1, stats.getCurrentStreak(1));
        assertEquals(4, stats.getTotalAttempts());
        assertEquals(3, stats.getTotalPairs());
        assertEquals(75, stats.getOverallAccuracyPercent());
        assertEquals(2, stats.getGameLongestStreak());
    }

    @Test
    public void finalWinnerWhoRecoveredFromLargestDeficitIsTheComebackParticipant() {
        GameStats stats = new GameStats(2);
        stats.recordAttempt(0, true);
        stats.recordAttempt(0, true);
        stats.recordAttempt(0, false);
        stats.recordAttempt(1, true);
        stats.recordAttempt(1, true);
        stats.recordAttempt(1, true);

        assertEquals(2, stats.getMaxDeficit(1));
        assertEquals(1, stats.getComebackParticipant());
    }

    @Test
    public void losingRecoveryIsNotReportedAsTheComebackWinner() {
        GameStats stats = new GameStats(2);
        stats.recordAttempt(0, true);
        stats.recordAttempt(0, true);
        stats.recordAttempt(0, true);
        stats.recordAttempt(1, true);
        stats.recordAttempt(1, true);

        assertEquals(3, stats.getMaxDeficit(1));
        assertEquals(GameStats.NO_PARTICIPANT, stats.getComebackParticipant());
    }

    @Test
    public void tiedComebackCandidatesDoNotSelectAnArbitraryParticipant() {
        GameStats stats = new GameStats(2);
        stats.recordAttempt(0, true);
        stats.recordAttempt(1, true);
        stats.recordAttempt(1, true);
        stats.recordAttempt(0, true);

        assertEquals(1, stats.getMaxDeficit(0));
        assertEquals(1, stats.getMaxDeficit(1));
        assertEquals(GameStats.NO_PARTICIPANT, stats.getComebackParticipant());
    }

    @Test
    public void snapshotsAreDefensiveAndRestoreEveryTrackedValue() {
        GameStats original = new GameStats(2);
        original.recordAttempt(0, true);
        original.recordAttempt(1, false);
        int[] attempts = original.copyAttempts();
        int[] pairs = original.copyPairs();
        int[] current = original.copyCurrentStreaks();
        int[] longest = original.copyLongestStreaks();
        int[] deficits = original.copyMaxDeficits();

        GameStats restored = GameStats.restore(attempts, pairs, current, longest, deficits);
        attempts[0] = 99;
        pairs[0] = 99;

        assertArrayEquals(new int[] {1, 1}, restored.copyAttempts());
        assertArrayEquals(new int[] {1, 0}, restored.copyPairs());
        assertArrayEquals(new int[] {1, 0}, restored.copyCurrentStreaks());
        assertArrayEquals(new int[] {1, 0}, restored.copyLongestStreaks());
        assertArrayEquals(new int[] {0, 1}, restored.copyMaxDeficits());
    }

    @Test
    public void recordResolutionChecksAuthoritativeScoresBeforeMutatingStats() {
        GameStats stats = new GameStats(2);

        stats.recordResolution(0, true, new int[] {1, 0});
        stats.recordResolution(0, false, new int[] {1, 0});

        assertArrayEquals(new int[] {2, 0}, stats.copyAttempts());
        assertArrayEquals(new int[] {1, 0}, stats.copyPairs());
        assertEquals(0, stats.getCurrentStreak(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void recordResolutionRejectsScoresThatDoNotMatchTheResolution() {
        new GameStats(2).recordResolution(0, true, new int[] {0, 0});
    }

    @Test(expected = IllegalArgumentException.class)
    public void restoreRejectsDifferentArrayLengths() {
        GameStats.restore(
            new int[] {1, 1},
            new int[] {1},
            new int[] {1, 0},
            new int[] {1, 0},
            new int[] {0, 1}
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void restoreRejectsMismatchedParticipantCount() {
        GameStats.restore(
            3,
            new int[] {1, 1},
            new int[] {1, 0},
            new int[] {1, 0},
            new int[] {1, 0},
            new int[] {0, 1}
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void restoreRejectsPairsBeyondAttempts() {
        GameStats.restore(
            new int[] {1},
            new int[] {2},
            new int[] {0},
            new int[] {0},
            new int[] {0}
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void restoreRejectsImpossibleStreaks() {
        GameStats.restore(
            new int[] {2},
            new int[] {1},
            new int[] {2},
            new int[] {1},
            new int[] {0}
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void restoreRejectsDeficitBelowTheCurrentScoreGap() {
        GameStats.restore(
            new int[] {2, 1},
            new int[] {2, 0},
            new int[] {2, 0},
            new int[] {2, 0},
            new int[] {0, 1}
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void recordRejectsUnknownParticipants() {
        new GameStats(2).recordAttempt(2, true);
    }
}
