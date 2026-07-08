package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.Test;

public final class GameStateTest {
    @Test
    public void deckContainsExactlyTwoOfEveryPair() {
        GameState game = new GameState(10, 50, new Random(7));
        Map<Integer, Integer> counts = new HashMap<>();

        for (int index = 0; index < game.getCardCount(); index++) {
            int pairId = game.getPairId(index);
            Integer previous = counts.get(pairId);
            counts.put(pairId, previous == null ? 1 : previous + 1);
        }

        assertEquals(50, counts.size());
        for (int count : counts.values()) {
            assertEquals(2, count);
        }
    }

    @Test
    public void matchAddsPointAndKeepsTheTurn() {
        GameState game = new GameState(2, 4, new Random(2));
        int[] pair = findPair(game, 0);

        assertTrue(game.flipCard(pair[0]));
        assertTrue(game.flipCard(pair[1]));
        GameState.Resolution resolution = game.resolveTurn();

        assertTrue(resolution.isMatch());
        assertEquals(0, resolution.getScoringPlayer());
        assertEquals(0, resolution.getNextPlayer());
        assertEquals(1, game.getScore(0));
        assertEquals(0, game.getCurrentPlayer());
        assertEquals(GameState.CardState.MATCHED, game.getCardState(pair[0]));
        assertEquals(GameState.Phase.WAITING_FIRST, game.getPhase());
    }

    @Test
    public void mismatchHidesCardsAndAdvancesTheTurn() {
        GameState game = new GameState(3, 4, new Random(3));
        int first = 0;
        int second = findDifferentCard(game, first);

        assertTrue(game.flipCard(first));
        assertFalse(game.flipCard(first));
        assertTrue(game.flipCard(second));
        GameState.Resolution resolution = game.resolveTurn();

        assertFalse(resolution.isMatch());
        assertEquals(0, resolution.getScoringPlayer());
        assertEquals(1, resolution.getNextPlayer());
        assertEquals(0, game.getScore(0));
        assertEquals(GameState.CardState.HIDDEN, game.getCardState(first));
        assertEquals(GameState.CardState.HIDDEN, game.getCardState(second));
        assertEquals(1, game.getCurrentPlayer());
    }

    @Test
    public void finalPairFinishesTheGameAndScoresSumToPairCount() {
        GameState game = new GameState(2, 4, new Random(4));
        for (int pairId = 0; pairId < 4; pairId++) {
            int[] pair = findPair(game, pairId);
            game.flipCard(pair[0]);
            game.flipCard(pair[1]);
            game.resolveTurn();
        }

        assertEquals(GameState.Phase.FINISHED, game.getPhase());
        assertEquals(4, game.getScore(0) + game.getScore(1));
        assertEquals(0, game.getRemainingPairs());
    }

    @Test
    public void botUsesOnlyRememberedMatch() {
        GameState game = new GameState(1, 4, new Random(5));
        ComputerMemory memory = new ComputerMemory(
            ComputerDifficulty.HARD,
            new NoMistakeRandom()
        );
        int[] knownPair = findPair(game, 2);
        memory.remember(knownPair[0], 2);
        memory.remember(knownPair[1], 2);

        int first = memory.chooseFirstCard(game);
        assertTrue(first == knownPair[0] || first == knownPair[1]);
        assertTrue(game.flipCard(first));
        int second = memory.chooseSecondCard(game, first, game.getPairId(first));

        assertTrue(second == knownPair[0] || second == knownPair[1]);
        assertTrue(second != first);
    }

    @Test
    public void savedResolvingTurnCanBeRestoredAndCompleted() {
        GameState original = new GameState(3, 6, new Random(8));
        int first = 0;
        int second = findDifferentCard(original, first);
        original.flipCard(first);
        original.flipCard(second);

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

        assertEquals(GameState.Phase.RESOLVING, restored.getPhase());
        restored.resolveTurn();
        assertEquals(GameState.Phase.WAITING_FIRST, restored.getPhase());
        assertEquals(1, restored.getCurrentPlayer());
    }

    @Test
    public void recommendedBoardsStayWithinLimits() {
        int previous = GameState.MIN_PAIRS;
        for (int players = 1; players <= GameState.MAX_HUMAN_PLAYERS; players++) {
            int recommended = GameState.recommendedPairsForPlayers(players);
            assertTrue(recommended >= GameState.MIN_PAIRS);
            assertTrue(recommended <= GameState.MAX_PAIRS);
            assertTrue(recommended >= previous);
            previous = recommended;
        }
    }

    @Test
    public void fifteenHumanPlayersAreSupported() {
        GameState game = new GameState(15, GameState.MIN_PAIRS, new Random(15));

        assertEquals(15, game.getHumanPlayerCount());
        assertEquals(15, game.getTotalPlayerCount());
        assertEquals(15, game.copyScores().length);
        assertEquals(50, GameState.recommendedPairsForPlayers(15));
    }

    @Test
    public void fifteenPlayerMismatchCycleVisitsEveryScoreTargetAndWraps() {
        GameState game = new GameState(15, GameState.MIN_PAIRS, new Random(25));
        int first = 0;
        int second = findDifferentCard(game, first);

        for (int player = 0; player < 15; player++) {
            assertEquals(player, game.getCurrentPlayer());
            assertTrue(game.flipCard(first));
            assertTrue(game.flipCard(second));
            GameState.Resolution resolution = game.resolveTurn();
            assertFalse(resolution.isMatch());
            assertEquals(player, resolution.getScoringPlayer());
            assertEquals((player + 1) % 15, resolution.getNextPlayer());
        }

        assertEquals(0, game.getCurrentPlayer());
        for (int score : game.copyScores()) {
            assertEquals(0, score);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void sixteenHumanPlayersAreRejected() {
        new GameState(16, GameState.MIN_PAIRS, new Random(16));
    }

    @Test(expected = IllegalArgumentException.class)
    public void fiftyOnePairsAreRejected() {
        new GameState(2, 51, new Random(51));
    }

    private static final class NoMistakeRandom extends Random {
        @Override
        public int nextInt(int bound) {
            return bound == 100 ? 99 : 0;
        }

        @Override
        public boolean nextBoolean() {
            return false;
        }
    }

    private static int[] findPair(GameState game, int pairId) {
        int first = -1;
        for (int index = 0; index < game.getCardCount(); index++) {
            if (game.getPairId(index) == pairId) {
                if (first < 0) {
                    first = index;
                } else {
                    return new int[] {first, index};
                }
            }
        }
        throw new AssertionError("Pair not found");
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
