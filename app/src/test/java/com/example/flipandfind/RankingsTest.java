package com.example.flipandfind;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

public final class RankingsTest {
    @Test
    public void tiesShareADenseRank() {
        List<Rankings.Group> groups = Rankings.fromScores(new int[] {4, 7, 7, 2});

        assertEquals(3, groups.size());
        assertEquals(1, groups.get(0).getRank());
        assertEquals(7, groups.get(0).getScore());
        assertEquals(2, groups.get(0).getPlayerIndices().size());
        assertEquals(2, groups.get(1).getRank());
        assertEquals(4, groups.get(1).getScore());
        assertEquals(3, groups.get(2).getRank());
    }
}
