package com.example.flipandfind;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;

public final class GameHistoryEntryTest {
    @Test
    public void soloEntrySeparatesSelectedPlayerCountFromBotParticipant() {
        GameHistoryEntry entry = new GameHistoryEntry(
            1_700_000_000_000L,
            92_500L,
            1,
            4,
            new String[] {"Maya", "Bot"},
            new int[] {3, 1},
            new int[] {75, 33},
            new int[] {2, 1}
        );

        assertEquals(1, entry.getPlayerCount());
        assertEquals(2, entry.getParticipantCount());
        assertEquals(Arrays.asList("Maya"), entry.getWinnerNames());
        assertEquals("Maya 3 • Bot 1", entry.getCompactScoreSummary());
        assertFalse(entry.isTie());
        assertEquals(75, entry.getAccuracyPercent(0));
        assertEquals(2, entry.getLongestStreak(0));
    }

    @Test
    public void tieRetainsEveryWinnerAndOptionalStatsMayBeAbsent() {
        GameHistoryEntry entry = new GameHistoryEntry(
            99L,
            1_000L,
            4,
            new String[] {"Player A", "Player B"},
            new int[] {2, 2},
            null,
            null
        );

        assertTrue(entry.isTie());
        assertArrayEquals(new int[] {0, 1}, entry.copyWinnerIndices());
        assertEquals(Arrays.asList("Player A", "Player B"), entry.getWinnerNames());
        assertFalse(entry.hasAccuracyStats());
        assertFalse(entry.hasLongestStreakStats());
    }

    @Test
    public void constructorAndCopiesAreDefensive() {
        String[] names = {"A", "B"};
        int[] scores = {3, 1};
        int[] accuracy = {60, 20};
        GameHistoryEntry entry = new GameHistoryEntry(
            10L,
            20L,
            4,
            names,
            scores,
            accuracy,
            new int[] {2, 1}
        );
        names[0] = "Changed";
        scores[0] = 99;
        accuracy[0] = 99;
        int[] copiedScores = entry.copyScores();
        copiedScores[0] = 100;

        assertEquals("A", entry.getParticipantName(0));
        assertEquals(3, entry.getScore(0));
        assertEquals(60, entry.getAccuracyPercent(0));
    }

    @Test
    public void factoryCopiesAccuracyAndStreaksFromGameStats() {
        GameStats stats = new GameStats(2);
        stats.recordAttempt(0, true);
        stats.recordAttempt(0, false);
        stats.recordAttempt(1, true);
        stats.recordAttempt(1, true);

        GameHistoryEntry entry = GameHistoryEntry.fromGameStats(
            10L,
            20L,
            2,
            3,
            new String[] {"A", "B"},
            new int[] {1, 2},
            stats
        );

        assertEquals(50, entry.getAccuracyPercent(0));
        assertEquals(100, entry.getAccuracyPercent(1));
        assertEquals(2, entry.getLongestStreak(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void completedScoresMustAccountForAllPairs() {
        new GameHistoryEntry(
            10L,
            20L,
            5,
            new String[] {"A", "B"},
            new int[] {3, 1},
            null,
            null
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void soloEntryAllowsOnlyOneBotBeyondSelectedPlayers() {
        new GameHistoryEntry(
            10L,
            20L,
            1,
            3,
            new String[] {"A", "Bot one", "Bot two"},
            new int[] {1, 1, 1},
            null,
            null
        );
    }
}
