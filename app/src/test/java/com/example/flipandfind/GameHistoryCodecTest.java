package com.example.flipandfind;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class GameHistoryCodecTest {
    @Test
    public void roundTripPreservesUnicodeDelimitersOptionalStatsAndLifetimeTotals() {
        GameHistory history = GameHistory.restore(5, 4L, 20_000L, 16L, java.util.Collections.emptyList());
        history.record(new GameHistoryEntry(
            1_700_000_123_456L,
            5_500L,
            4,
            new String[] {"Ana • 雪", "B:2"},
            new int[] {3, 1},
            new int[] {75, 25},
            new int[] {2, 1}
        ));

        GameHistory decoded = GameHistoryCodec.decode(GameHistoryCodec.encode(history), 5);

        assertEquals(5L, decoded.getTotalGamesPlayed());
        assertEquals(25_500L, decoded.getTotalActiveDurationMillis());
        assertEquals(20L, decoded.getTotalPairsPlayed());
        assertEquals(history.getEntry(0), decoded.getEntry(0));
    }

    @Test
    public void decodingWithSmallerCurrentCapKeepsNewestRowsAndLifetimeTotals() {
        GameHistory history = GameHistory.empty(3);
        history.record(entry(1L));
        history.record(entry(2L));
        history.record(entry(3L));

        GameHistory decoded = GameHistoryCodec.decode(GameHistoryCodec.encode(history), 2);

        assertEquals(3L, decoded.getTotalGamesPlayed());
        assertEquals(2, decoded.getRetainedGameCount());
        assertEquals(3L, decoded.getEntry(0).getCompletedAtEpochMillis());
        assertEquals(2L, decoded.getEntry(1).getCompletedAtEpochMillis());
    }

    @Test(expected = IllegalArgumentException.class)
    public void malformedHexIsRejected() {
        GameHistoryCodec.decode("not history", 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void truncatedDataIsRejected() {
        String encoded = GameHistoryCodec.encode(GameHistory.empty(10));
        GameHistoryCodec.decode(encoded.substring(0, encoded.length() - 2), 10);
    }

    private static GameHistoryEntry entry(long completedAt) {
        return new GameHistoryEntry(
            completedAt,
            1_000L,
            2,
            new String[] {"A", "B"},
            new int[] {2, 0},
            null,
            null
        );
    }
}
