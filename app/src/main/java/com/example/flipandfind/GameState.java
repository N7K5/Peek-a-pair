package com.example.flipandfind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Pure Java game rules. Android views never decide scoring or turn order. */
public final class GameState {
    public static final int MIN_HUMAN_PLAYERS = 1;
    public static final int MAX_HUMAN_PLAYERS = 15;
    public static final int MIN_PAIRS = 4;
    public static final int MAX_PAIRS = 50;

    public enum Phase {
        WAITING_FIRST,
        WAITING_SECOND,
        RESOLVING,
        FINISHED
    }

    public enum CardState {
        HIDDEN,
        FACE_UP,
        MATCHED
    }

    public static final class Resolution {
        private final boolean match;
        private final boolean gameComplete;
        private final int scoringPlayer;
        private final int nextPlayer;
        private final int firstCard;
        private final int secondCard;

        private Resolution(
            boolean match,
            boolean gameComplete,
            int scoringPlayer,
            int nextPlayer,
            int firstCard,
            int secondCard
        ) {
            this.match = match;
            this.gameComplete = gameComplete;
            this.scoringPlayer = scoringPlayer;
            this.nextPlayer = nextPlayer;
            this.firstCard = firstCard;
            this.secondCard = secondCard;
        }

        public boolean isMatch() {
            return match;
        }

        public boolean isGameComplete() {
            return gameComplete;
        }

        public int getScoringPlayer() {
            return scoringPlayer;
        }

        public int getNextPlayer() {
            return nextPlayer;
        }

        public int getFirstCard() {
            return firstCard;
        }

        public int getSecondCard() {
            return secondCard;
        }
    }

    private final int humanPlayerCount;
    private final int totalPlayerCount;
    private final int pairCount;
    private final int[] pairIds;
    private final CardState[] cardStates;
    private final int[] scores;

    private int currentPlayer;
    private int firstCard = -1;
    private int secondCard = -1;
    private int remainingPairs;
    private Phase phase = Phase.WAITING_FIRST;

    public GameState(int humanPlayerCount, int pairCount) {
        this(humanPlayerCount, pairCount, new Random());
    }

    GameState(int humanPlayerCount, int pairCount, Random random) {
        validateConfiguration(humanPlayerCount, pairCount);
        this.humanPlayerCount = humanPlayerCount;
        this.totalPlayerCount = humanPlayerCount == 1 ? 2 : humanPlayerCount;
        this.pairCount = pairCount;
        this.remainingPairs = pairCount;
        this.pairIds = shuffledDeck(pairCount, random);
        this.cardStates = new CardState[pairCount * 2];
        this.scores = new int[totalPlayerCount];
        for (int index = 0; index < cardStates.length; index++) {
            cardStates[index] = CardState.HIDDEN;
        }
    }

    private GameState(
        int humanPlayerCount,
        int pairCount,
        int[] pairIds,
        CardState[] cardStates,
        int[] scores,
        int currentPlayer,
        int firstCard,
        int secondCard,
        int remainingPairs,
        Phase phase
    ) {
        validateConfiguration(humanPlayerCount, pairCount);
        int totalPlayers = humanPlayerCount == 1 ? 2 : humanPlayerCount;
        if (pairIds.length != pairCount * 2 || cardStates.length != pairIds.length) {
            throw new IllegalArgumentException("Saved deck has the wrong size");
        }
        if (scores.length != totalPlayers) {
            throw new IllegalArgumentException("Saved score list has the wrong size");
        }
        if (currentPlayer < 0 || currentPlayer >= totalPlayers) {
            throw new IllegalArgumentException("Saved current player is invalid");
        }

        this.humanPlayerCount = humanPlayerCount;
        this.totalPlayerCount = totalPlayers;
        this.pairCount = pairCount;
        this.pairIds = pairIds.clone();
        this.cardStates = cardStates.clone();
        this.scores = scores.clone();
        this.currentPlayer = currentPlayer;
        this.firstCard = firstCard;
        this.secondCard = secondCard;
        this.remainingPairs = remainingPairs;
        this.phase = phase;
    }

    public static GameState restore(
        int humanPlayerCount,
        int pairCount,
        int[] pairIds,
        int[] cardStateOrdinals,
        int[] scores,
        int currentPlayer,
        int firstCard,
        int secondCard,
        int remainingPairs,
        String phaseName
    ) {
        if (cardStateOrdinals == null) {
            throw new IllegalArgumentException("Saved card states are missing");
        }
        CardState[] restoredStates = new CardState[cardStateOrdinals.length];
        CardState[] values = CardState.values();
        for (int index = 0; index < cardStateOrdinals.length; index++) {
            int ordinal = cardStateOrdinals[index];
            if (ordinal < 0 || ordinal >= values.length) {
                throw new IllegalArgumentException("Saved card state is invalid");
            }
            restoredStates[index] = values[ordinal];
        }
        return new GameState(
            humanPlayerCount,
            pairCount,
            pairIds,
            restoredStates,
            scores,
            currentPlayer,
            firstCard,
            secondCard,
            remainingPairs,
            Phase.valueOf(phaseName)
        );
    }

    private static void validateConfiguration(int humanPlayers, int pairs) {
        if (humanPlayers < MIN_HUMAN_PLAYERS || humanPlayers > MAX_HUMAN_PLAYERS) {
            throw new IllegalArgumentException("Players must be between 1 and 15");
        }
        if (pairs < MIN_PAIRS || pairs > MAX_PAIRS) {
            throw new IllegalArgumentException("Pairs must be between 4 and 50");
        }
    }

    private static int[] shuffledDeck(int pairCount, Random random) {
        List<Integer> deck = new ArrayList<>(pairCount * 2);
        for (int pairId = 0; pairId < pairCount; pairId++) {
            deck.add(pairId);
            deck.add(pairId);
        }
        Collections.shuffle(deck, random);
        int[] result = new int[deck.size()];
        for (int index = 0; index < deck.size(); index++) {
            result[index] = deck.get(index);
        }
        return result;
    }

    /**
     * Flips a hidden card. Returns false when the tap is not currently legal.
     * Resolution is deliberately separate so the UI can leave two cards visible.
     */
    public boolean flipCard(int cardIndex) {
        if (phase != Phase.WAITING_FIRST && phase != Phase.WAITING_SECOND) {
            return false;
        }
        if (cardIndex < 0 || cardIndex >= cardStates.length) {
            return false;
        }
        if (cardStates[cardIndex] != CardState.HIDDEN) {
            return false;
        }

        cardStates[cardIndex] = CardState.FACE_UP;
        if (phase == Phase.WAITING_FIRST) {
            firstCard = cardIndex;
            phase = Phase.WAITING_SECOND;
        } else {
            secondCard = cardIndex;
            phase = Phase.RESOLVING;
        }
        return true;
    }

    public Resolution resolveTurn() {
        if (phase != Phase.RESOLVING || firstCard < 0 || secondCard < 0) {
            throw new IllegalStateException("There is no turn to resolve");
        }

        int resolvedFirst = firstCard;
        int resolvedSecond = secondCard;
        int scoringPlayer = currentPlayer;
        boolean match = pairIds[firstCard] == pairIds[secondCard];

        if (match) {
            cardStates[firstCard] = CardState.MATCHED;
            cardStates[secondCard] = CardState.MATCHED;
            scores[currentPlayer]++;
            remainingPairs--;
            phase = remainingPairs == 0 ? Phase.FINISHED : Phase.WAITING_FIRST;
        } else {
            cardStates[firstCard] = CardState.HIDDEN;
            cardStates[secondCard] = CardState.HIDDEN;
            currentPlayer = (currentPlayer + 1) % totalPlayerCount;
            phase = Phase.WAITING_FIRST;
        }

        firstCard = -1;
        secondCard = -1;
        return new Resolution(
            match,
            phase == Phase.FINISHED,
            scoringPlayer,
            currentPlayer,
            resolvedFirst,
            resolvedSecond
        );
    }

    public int getHumanPlayerCount() {
        return humanPlayerCount;
    }

    public int getTotalPlayerCount() {
        return totalPlayerCount;
    }

    public int getPairCount() {
        return pairCount;
    }

    public int getCardCount() {
        return pairIds.length;
    }

    public int getPairId(int cardIndex) {
        return pairIds[cardIndex];
    }

    public CardState getCardState(int cardIndex) {
        return cardStates[cardIndex];
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public int getFirstCard() {
        return firstCard;
    }

    public int getSecondCard() {
        return secondCard;
    }

    public int getRemainingPairs() {
        return remainingPairs;
    }

    public int getMatchedPairs() {
        return pairCount - remainingPairs;
    }

    public int getScore(int playerIndex) {
        return scores[playerIndex];
    }

    public Phase getPhase() {
        return phase;
    }

    public boolean isComputerMode() {
        return humanPlayerCount == 1;
    }

    public boolean isComputerTurn() {
        return isComputerMode() && currentPlayer == 1 && phase != Phase.FINISHED;
    }

    public List<Integer> getHiddenCardIndices() {
        List<Integer> hidden = new ArrayList<>();
        for (int index = 0; index < cardStates.length; index++) {
            if (cardStates[index] == CardState.HIDDEN) {
                hidden.add(index);
            }
        }
        return hidden;
    }

    public int[] copyPairIds() {
        return pairIds.clone();
    }

    public int[] copyCardStateOrdinals() {
        int[] ordinals = new int[cardStates.length];
        for (int index = 0; index < cardStates.length; index++) {
            ordinals[index] = cardStates[index].ordinal();
        }
        return ordinals;
    }

    public int[] copyScores() {
        return scores.clone();
    }

    public static int recommendedPairsForPlayers(int humanPlayerCount) {
        int[] recommendations = {
            8, 8, 10, 12, 15, 18, 21, 24, 27, 30, 34, 38, 42, 46, 50
        };
        int index = Math.max(1, Math.min(MAX_HUMAN_PLAYERS, humanPlayerCount)) - 1;
        return recommendations[index];
    }
}
