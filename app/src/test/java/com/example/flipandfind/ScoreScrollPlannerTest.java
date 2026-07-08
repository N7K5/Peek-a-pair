package com.example.flipandfind;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class ScoreScrollPlannerTest {
    @Test
    public void fullyVisibleChipKeepsCurrentScroll() {
        assertEquals(100, ScoreScrollPlanner.targetX(100, 300, 150, 230, 1000));
    }

    @Test
    public void offscreenChipIsCentered() {
        assertEquals(310, ScoreScrollPlanner.targetX(0, 300, 420, 500, 1000));
    }

    @Test
    public void firstAndLastChipsClampToContentEdges() {
        assertEquals(0, ScoreScrollPlanner.targetX(500, 300, 0, 80, 1000));
        assertEquals(700, ScoreScrollPlanner.targetX(0, 300, 920, 1000, 1000));
    }

    @Test
    public void partiallyVisibleChipMovesIntoView() {
        assertEquals(450, ScoreScrollPlanner.targetX(300, 300, 550, 650, 1000));
    }

    @Test
    public void oversizedChipAlignsItsLeadingEdge() {
        assertEquals(350, ScoreScrollPlanner.targetX(0, 240, 350, 700, 1000));
    }

    @Test
    public void invalidGeometrySafelyKeepsAClampedPosition() {
        assertEquals(80, ScoreScrollPlanner.targetX(80, 0, 20, 20, 500));
        assertEquals(200, ScoreScrollPlanner.targetX(900, 300, 50, 50, 500));
    }
}
