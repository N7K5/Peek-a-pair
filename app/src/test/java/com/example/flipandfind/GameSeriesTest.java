package com.example.flipandfind;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class GameSeriesTest {
    @Test
    public void tracksOutrightWinsAndConsecutiveWinner() {
        GameSeries series = new GameSeries(new String[] {"A", "B"});

        series.recordGame(new int[] {4, 2});
        series.recordGame(new int[] {5, 1});

        assertEquals(2, series.getGameCount());
        assertEquals(2, series.getWinCount(0));
        assertEquals(2, series.getCurrentWinStreak(0));
        assertEquals(2, series.getLongestWinStreak(0));
        assertEquals(0, series.getWinCount(1));
        assertEquals(0, series.getTiedGameCount());
        assertArrayEquals(new int[] {0}, series.copySeriesLeaderIndices());
    }

    @Test
    public void tieIsRecordedSeparatelyAndBreaksOutrightWinStreak() {
        GameSeries series = new GameSeries(new String[] {"A", "B"});
        series.recordGame(new int[] {3, 1});

        int[] winners = series.recordGame(new int[] {2, 2});

        assertArrayEquals(new int[] {0, 1}, winners);
        assertEquals(2, series.getGameCount());
        assertEquals(1, series.getTiedGameCount());
        assertEquals(1, series.getWinCount(0));
        assertEquals(0, series.getWinCount(1));
        assertEquals(0, series.getCurrentWinStreak(0));
        assertEquals(GameSeries.NO_PARTICIPANT, series.getCurrentStreakParticipant());
    }

    @Test
    public void restoreIsDefensiveAndContinuesTheSeries() {
        String[] names = {"A", "B"};
        int[] wins = {2, 1};
        GameSeries series = GameSeries.restore(
            names,
            4,
            1,
            wins,
            new int[] {0, 1},
            new int[] {2, 1}
        );
        names[0] = "Changed";
        wins[0] = 99;

        series.recordGame(new int[] {1, 4});

        assertEquals("A", series.getParticipantName(0));
        assertArrayEquals(new int[] {2, 2}, series.copyWins());
        assertEquals(2, series.getCurrentWinStreak(1));
        assertArrayEquals(new int[] {0, 1}, series.copySeriesLeaderIndices());
    }

    @Test(expected = IllegalArgumentException.class)
    public void restoreRejectsWinsThatDoNotAccountForNonTieGames() {
        GameSeries.restore(
            new String[] {"A", "B"},
            3,
            1,
            new int[] {1, 0},
            new int[] {1, 0},
            new int[] {1, 0}
        );
    }
}
