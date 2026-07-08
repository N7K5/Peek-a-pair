package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.Map;
import org.junit.Test;

public final class GameHistoryTest {
    @Test
    public void capDropsOldRowsButPreservesLifetimeTotals() {
        GameHistory history = GameHistory.empty(2);
        history.record(entry(1L, 1_000L, new int[] {3, 1}));
        history.record(entry(2L, 2_000L, new int[] {2, 2}));
        history.record(entry(3L, 3_000L, new int[] {1, 3}));

        assertEquals(3L, history.getTotalGamesPlayed());
        assertEquals(6_000L, history.getTotalActiveDurationMillis());
        assertEquals(12L, history.getTotalPairsPlayed());
        assertEquals(2, history.getRetainedGameCount());
        assertEquals(3L, history.getEntry(0).getCompletedAtEpochMillis());
        assertEquals(2L, history.getEntry(1).getCompletedAtEpochMillis());
    }

    @Test
    public void retainedWinnerCountsCreditBothNamesInTie() {
        GameHistory history = GameHistory.empty(5);
        history.record(entry(1L, 1_000L, new int[] {3, 1}));
        history.record(entry(2L, 1_000L, new int[] {2, 2}));
        Map<String, Integer> winnerCounts = history.getRetainedWinnerCounts();

        assertEquals(1, history.getRetainedTieCount());
        assertEquals(Integer.valueOf(2), winnerCounts.get("A"));
        assertEquals(Integer.valueOf(1), winnerCounts.get("B"));
    }

    @Test
    public void deletingRetainedGameSubtractsOnlyThatGameFromLifetimeTotals() {
        GameHistory history = GameHistory.empty(2);
        GameHistoryEntry oldest = entry(1L, 1_000L, new int[] {3, 1});
        GameHistoryEntry deleted = entry(2L, 2_000L, new int[] {2, 2});
        GameHistoryEntry newest = entry(3L, 3_000L, new int[] {1, 3});
        history.record(oldest);
        history.record(deleted);
        history.record(newest);

        assertSame(deleted, history.removeRetainedGame(1));

        assertEquals(2L, history.getTotalGamesPlayed());
        assertEquals(4_000L, history.getTotalActiveDurationMillis());
        assertEquals(8L, history.getTotalPairsPlayed());
        assertEquals(1, history.getRetainedGameCount());
        assertSame(newest, history.getEntry(0));
    }

    @Test
    public void deletingInvalidRetainedIndexDoesNotInventADeletion() {
        GameHistory history = GameHistory.empty(2);
        history.record(entry(1L, 1_000L, new int[] {3, 1}));
        try {
            history.removeRetainedGame(1);
            fail("Expected invalid retained index to be rejected");
        } catch (IndexOutOfBoundsException expected) {
            assertEquals(1L, history.getTotalGamesPlayed());
            assertEquals(1_000L, history.getTotalActiveDurationMillis());
            assertEquals(4L, history.getTotalPairsPlayed());
            assertEquals(1, history.getRetainedGameCount());
        }
    }

    private static GameHistoryEntry entry(long completedAt, long duration, int[] scores) {
        return new GameHistoryEntry(
            completedAt,
            duration,
            4,
            new String[] {"A", "B"},
            scores,
            null,
            null
        );
    }
}
