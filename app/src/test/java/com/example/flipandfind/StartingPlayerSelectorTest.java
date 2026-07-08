package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import org.junit.Test;

public final class StartingPlayerSelectorTest {
    @Test
    public void disabledRandomizationAlwaysStartsWithPlayerA() {
        assertEquals(0, StartingPlayerSelector.select(1, false, Long.MIN_VALUE));
        assertEquals(0, StartingPlayerSelector.select(2, false, 42L));
        assertEquals(0, StartingPlayerSelector.select(15, false, Long.MAX_VALUE));
    }

    @Test
    public void soloRandomizationIncludesTheHumanAndBot() {
        boolean selectedHuman = false;
        boolean selectedBot = false;

        for (long seed = 0; seed < 100; seed++) {
            int selected = StartingPlayerSelector.select(1, true, seed);
            assertTrue(selected == 0 || selected == 1);
            selectedHuman |= selected == 0;
            selectedBot |= selected == 1;
        }

        assertTrue(selectedHuman);
        assertTrue(selectedBot);
    }

    @Test
    public void multiplayerRandomizationCanSelectEveryParticipant() {
        boolean[] selected = new boolean[15];
        for (long seed = 0; seed < 2_000; seed++) {
            int player = StartingPlayerSelector.select(15, true, seed);
            assertTrue(player >= 0 && player < selected.length);
            selected[player] = true;
        }

        for (boolean wasSelected : selected) {
            assertTrue(wasSelected);
        }
    }

    @Test
    public void selectionIsDeterministicForARoundSeed() {
        int selected = StartingPlayerSelector.select(7, true, 0x1234ABCDL);

        assertEquals(selected, StartingPlayerSelector.select(7, true, 0x1234ABCDL));
        assertFalse(selected < 0 || selected >= 7);
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroPlayersAreRejected() {
        StartingPlayerSelector.select(0, true, 1L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooManyPlayersAreRejected() {
        StartingPlayerSelector.select(16, false, 1L);
    }

    @Test
    public void chosenBotStarterIsAppliedToGameRules() {
        GameState game = new GameState(1, 4, 1, new Random(91L));
        int first = 0;
        int second = findDifferentCard(game, first);

        assertEquals(1, game.getCurrentPlayer());
        assertTrue(game.isComputerTurn());
        assertTrue(game.flipCard(first));
        assertTrue(game.flipCard(second));
        GameState.Resolution resolution = game.resolveTurn();

        assertFalse(resolution.isMatch());
        assertEquals(1, resolution.getScoringPlayer());
        assertEquals(0, resolution.getNextPlayer());
        assertFalse(game.isComputerTurn());
    }

    @Test(expected = IllegalArgumentException.class)
    public void gameRejectsStarterOutsideParticipantList() {
        new GameState(2, 4, 2, new Random(17L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void gameRejectsNegativeStarter() {
        new GameState(2, 4, -1, new Random(17L));
    }

    @Test
    public void restoreKeepsANonzeroStartingPlayer() {
        GameState original = new GameState(3, 4, 2, new Random(44L));
        GameState restored = GameState.restore(
            original.getHumanPlayerCount(),
            original.getPairCount(),
            original.copyPairIds(),
            original.copyCardStateOrdinals(),
            original.copyScores(),
            original.getCurrentPlayer(),
            original.getFirstCard(),
            original.getSecondCard(),
            original.getRemainingPairs(),
            original.getPhase().name()
        );

        assertEquals(2, restored.getCurrentPlayer());
    }

    private static int findDifferentCard(GameState game, int first) {
        for (int index = 0; index < game.getCardCount(); index++) {
            if (game.getPairId(index) != game.getPairId(first)) {
                return index;
            }
        }
        throw new AssertionError("Different card not found");
    }
}
