package com.example.flipandfind;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;
import org.junit.Test;

public final class ComputerMemoryTest {
    @Test
    public void basicRetainsTheCurrentAndOneEarlierCard() {
        ComputerMemory memory = new ComputerMemory(ComputerDifficulty.BASIC, new Random(1));

        memory.remember(2, 7);
        memory.remember(4, 8);
        memory.remember(6, 9);

        assertArrayEquals(new int[] {4, 6}, memory.copyRememberedIndices());
        assertArrayEquals(new int[] {8, 9}, memory.copyRememberedPairIds());
    }

    @Test
    public void basicCanUseOneEarlierCardToCompleteAPair() {
        GameState game = new GameState(1, 4, new Random(8));
        int[] knownPair = findPair(game, 2);
        ComputerMemory memory = new ComputerMemory(
            ComputerDifficulty.BASIC,
            new NeverMistakeRandom()
        );

        int unrelated = findCardOutsidePair(game, knownPair);
        memory.remember(unrelated, game.getPairId(unrelated));
        memory.remember(knownPair[1], 2);
        assertTrue(game.flipCard(knownPair[0]));
        memory.remember(knownPair[0], 2);

        assertArrayEquals(
            new int[] {knownPair[1], knownPair[0]},
            memory.copyRememberedIndices()
        );
        assertEquals(
            knownPair[1],
            memory.chooseSecondCard(game, knownPair[0], 2)
        );
    }

    @Test
    public void difficultyPoliciesIncreaseMemoryAndAccuracyMonotonically() {
        assertPolicy(ComputerDifficulty.BASIC, 2, 30);
        assertPolicy(ComputerDifficulty.EASY, 4, 20);
        assertPolicy(ComputerDifficulty.MEDIUM, 12, 10);
        assertPolicy(ComputerDifficulty.HARD, Integer.MAX_VALUE, 5);
    }

    @Test
    public void everyDifficultyUsesItsConfiguredMistakeBoundary() {
        for (ComputerDifficulty difficulty : ComputerDifficulty.values()) {
            int threshold = difficulty.getKnownMatchMistakePercent();
            assertKnownMateChoice(difficulty, threshold - 1, false);
            assertKnownMateChoice(difficulty, threshold, true);
        }
    }

    @Test
    public void noKnownMateDoesNotConsumeAnAccuracyRoll() {
        GameState game = new GameState(1, 4, new Random(17));
        int first = 0;
        assertTrue(game.flipCard(first));
        ComputerMemory memory = new ComputerMemory(
            ComputerDifficulty.HARD,
            new NoAccuracyRollRandom()
        );
        memory.remember(first, game.getPairId(first));

        int choice = memory.chooseSecondCard(game, first, game.getPairId(first));

        assertTrue(choice >= 0);
        assertTrue(choice != first);
        assertEquals(GameState.CardState.HIDDEN, game.getCardState(choice));
    }

    @Test
    public void easyKeepsOnlyFourMostRecentCards() {
        ComputerMemory memory = new ComputerMemory(ComputerDifficulty.EASY, new Random(1));

        rememberSequentially(memory, 6);

        assertArrayEquals(new int[] {2, 3, 4, 5}, memory.copyRememberedIndices());
        assertArrayEquals(new int[] {102, 103, 104, 105}, memory.copyRememberedPairIds());
    }

    @Test
    public void observingACardAgainRefreshesItBeforeEviction() {
        ComputerMemory memory = new ComputerMemory(ComputerDifficulty.EASY, new Random(1));
        rememberSequentially(memory, 4);

        memory.remember(0, 100);
        memory.remember(4, 104);

        assertArrayEquals(new int[] {2, 3, 0, 4}, memory.copyRememberedIndices());
    }

    @Test
    public void mediumRemembersMoreCardsThanEasy() {
        ComputerMemory easy = new ComputerMemory(ComputerDifficulty.EASY, new Random(1));
        ComputerMemory medium = new ComputerMemory(ComputerDifficulty.MEDIUM, new Random(1));

        rememberSequentially(easy, 15);
        rememberSequentially(medium, 15);

        assertEquals(4, easy.copyRememberedIndices().length);
        assertEquals(12, medium.copyRememberedIndices().length);
        assertArrayEquals(
            new int[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14},
            medium.copyRememberedIndices()
        );
    }

    @Test
    public void hardRemembersEveryObservedCardAndUsesKnownPair() {
        ComputerMemory unlimitedMemory = new ComputerMemory(
            ComputerDifficulty.HARD,
            new Random(2)
        );
        rememberSequentially(unlimitedMemory, 40);
        assertEquals(40, unlimitedMemory.copyRememberedIndices().length);

        GameState game = new GameState(1, 6, new Random(3));
        ComputerMemory memory = new ComputerMemory(
            ComputerDifficulty.HARD,
            new NeverMistakeRandom()
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
    public void hardCanRarelyOverlookAKnownMate() {
        GameState game = new GameState(1, 4, new Random(21));
        int[] pair = findPair(game, 2);
        ScriptedRandom random = new ScriptedRandom(4, 0);
        ComputerMemory memory = new ComputerMemory(ComputerDifficulty.HARD, random);
        memory.remember(pair[1], 2);
        assertTrue(game.flipCard(pair[0]));
        memory.remember(pair[0], 2);

        int choice = memory.chooseSecondCard(game, pair[0], 2);

        assertTrue(choice >= 0);
        assertTrue(choice != pair[0]);
        assertTrue(choice != pair[1]);
        assertEquals(GameState.CardState.HIDDEN, game.getCardState(choice));
        random.assertExhausted();
    }

    @Test
    public void hardUsesKnownMateAtFivePercentBoundary() {
        GameState game = new GameState(1, 4, new Random(22));
        int[] pair = findPair(game, 1);
        ScriptedRandom random = new ScriptedRandom(5);
        ComputerMemory memory = new ComputerMemory(ComputerDifficulty.HARD, random);
        memory.remember(pair[1], 1);
        assertTrue(game.flipCard(pair[0]));
        memory.remember(pair[0], 1);

        assertEquals(pair[1], memory.chooseSecondCard(game, pair[0], 1));
        random.assertExhausted();
    }

    @Test
    public void finalLegalMateIsChosenWithoutAMistakeRoll() {
        GameState original = new GameState(1, 4, new Random(23));
        int[] pair = findPair(original, 3);
        int[] states = new int[original.getCardCount()];
        Arrays.fill(states, GameState.CardState.MATCHED.ordinal());
        states[pair[0]] = GameState.CardState.FACE_UP.ordinal();
        states[pair[1]] = GameState.CardState.HIDDEN.ordinal();
        GameState game = GameState.restore(
            1,
            4,
            original.copyPairIds(),
            states,
            new int[] {0, 3},
            1,
            pair[0],
            -1,
            1,
            GameState.Phase.WAITING_SECOND.name()
        );
        ComputerMemory memory = new ComputerMemory(
            ComputerDifficulty.HARD,
            new UnexpectedRandom()
        );
        memory.remember(pair[1], 3);
        memory.remember(pair[0], 3);

        assertEquals(pair[1], memory.chooseSecondCard(game, pair[0], 3));
    }

    @Test
    public void restoreHonorsSelectedDifficultyCapacity() {
        int[] indices = {0, 1, 2, 3, 4, 5};
        int[] pairIds = {10, 11, 12, 13, 14, 15};

        ComputerMemory restored = ComputerMemory.restore(
            ComputerDifficulty.EASY,
            indices,
            pairIds
        );

        assertEquals(ComputerDifficulty.EASY, restored.getDifficulty());
        assertArrayEquals(new int[] {2, 3, 4, 5}, restored.copyRememberedIndices());
        assertArrayEquals(new int[] {12, 13, 14, 15}, restored.copyRememberedPairIds());
    }

    @Test
    public void basicRestoreKeepsOneEarlierCardBesideTheCurrentObservation() {
        ComputerMemory restored = ComputerMemory.restore(
            ComputerDifficulty.BASIC,
            new int[] {0, 1, 2, 3, 4, 5},
            new int[] {10, 11, 12, 13, 14, 15}
        );
        assertArrayEquals(new int[] {4, 5}, restored.copyRememberedIndices());

        restored.remember(6, 16);

        assertArrayEquals(new int[] {5, 6}, restored.copyRememberedIndices());
        assertArrayEquals(new int[] {15, 16}, restored.copyRememberedPairIds());
    }

    @Test
    public void legacyApisKeepUnlimitedMemoryCapacity() {
        ComputerMemory fresh = new ComputerMemory(new Random(1));
        fresh.remember(0, 4);
        assertEquals(ComputerDifficulty.HARD, fresh.getDifficulty());

        ComputerMemory restored = ComputerMemory.restore(
            new int[] {0, 1, 2, 3, 4},
            new int[] {0, 1, 2, 3, 4}
        );
        assertEquals(ComputerDifficulty.HARD, restored.getDifficulty());
        assertEquals(5, restored.copyRememberedIndices().length);
    }

    private static void rememberSequentially(ComputerMemory memory, int count) {
        for (int index = 0; index < count; index++) {
            memory.remember(index, 100 + index);
        }
    }

    private static void assertPolicy(
        ComputerDifficulty difficulty,
        int rememberedCardLimit,
        int mistakePercent
    ) {
        assertEquals(rememberedCardLimit, difficulty.getRememberedCardLimit());
        assertEquals(mistakePercent, difficulty.getKnownMatchMistakePercent());
    }

    private static void assertKnownMateChoice(
        ComputerDifficulty difficulty,
        int accuracyRoll,
        boolean expectMate
    ) {
        GameState game = new GameState(1, 4, new Random(30 + difficulty.ordinal()));
        int[] pair = findPair(game, 1);
        ScriptedRandom random = expectMate
            ? new ScriptedRandom(accuracyRoll)
            : new ScriptedRandom(accuracyRoll, 0);
        ComputerMemory memory = new ComputerMemory(difficulty, random);
        memory.remember(pair[1], 1);
        assertTrue(game.flipCard(pair[0]));
        memory.remember(pair[0], 1);

        int choice = memory.chooseSecondCard(game, pair[0], 1);

        if (expectMate) {
            assertEquals(pair[1], choice);
        } else {
            assertTrue(choice != pair[0]);
            assertTrue(choice != pair[1]);
        }
        random.assertExhausted();
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

    private static int findCardOutsidePair(GameState game, int[] pair) {
        for (int cardIndex = 0; cardIndex < game.getCardCount(); cardIndex++) {
            if (cardIndex != pair[0] && cardIndex != pair[1]) {
                return cardIndex;
            }
        }
        throw new AssertionError("Unrelated card not found");
    }

    private static final class NeverMistakeRandom extends Random {
        @Override
        public int nextInt(int bound) {
            return bound == 100 ? 99 : 0;
        }

        @Override
        public boolean nextBoolean() {
            return false;
        }
    }

    private static final class ScriptedRandom extends Random {
        private final int[] values;
        private int offset;

        ScriptedRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            if (offset >= values.length) {
                throw new AssertionError("Unexpected random choice with bound " + bound);
            }
            int value = values[offset++];
            if (value < 0 || value >= bound) {
                throw new AssertionError("Scripted random value is outside its bound");
            }
            return value;
        }

        void assertExhausted() {
            assertEquals(values.length, offset);
        }
    }

    private static final class UnexpectedRandom extends Random {
        @Override
        public int nextInt(int bound) {
            throw new AssertionError("No random choice was expected");
        }
    }

    private static final class NoAccuracyRollRandom extends Random {
        @Override
        public int nextInt(int bound) {
            if (bound == 100) {
                throw new AssertionError("No accuracy roll was expected");
            }
            return 0;
        }
    }
}
