package com.example.flipandfind;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A fair bot player: it chooses only from card identities it has observed.
 * Difficulty changes how many observations remain and how reliably known matches are used.
 */
public final class ComputerMemory {
    private final LinkedHashMap<Integer, Integer> rememberedCards = new LinkedHashMap<>();
    private final ComputerDifficulty difficulty;
    private final Random random;

    public ComputerMemory() {
        this(ComputerDifficulty.HARD);
    }

    public ComputerMemory(ComputerDifficulty difficulty) {
        this(difficulty, new Random());
    }

    ComputerMemory(Random random) {
        this(ComputerDifficulty.HARD, random);
    }

    ComputerMemory(ComputerDifficulty difficulty, Random random) {
        if (difficulty == null) {
            throw new IllegalArgumentException("Bot difficulty is required");
        }
        if (random == null) {
            throw new IllegalArgumentException("Random source is required");
        }
        this.difficulty = difficulty;
        this.random = random;
    }

    public void remember(int cardIndex, int pairId) {
        int rememberedCardLimit = difficulty.getRememberedCardLimit();
        if (rememberedCardLimit == 0) {
            return;
        }
        rememberedCards.remove(cardIndex);
        rememberedCards.put(cardIndex, pairId);
        while (rememberedCards.size() > rememberedCardLimit) {
            Iterator<Integer> oldest = rememberedCards.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
    }

    public void forgetMatchedCards(GameState game) {
        Iterator<Map.Entry<Integer, Integer>> iterator = rememberedCards.entrySet().iterator();
        while (iterator.hasNext()) {
            int cardIndex = iterator.next().getKey();
            if (game.getCardState(cardIndex) == GameState.CardState.MATCHED) {
                iterator.remove();
            }
        }
    }

    public int chooseFirstCard(GameState game) {
        LinkedHashMap<Integer, Integer> firstCardByPair = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> entry : rememberedCards.entrySet()) {
            int cardIndex = entry.getKey();
            if (game.getCardState(cardIndex) != GameState.CardState.HIDDEN) {
                continue;
            }
            int pairId = entry.getValue();
            Integer matchingIndex = firstCardByPair.get(pairId);
            if (matchingIndex != null) {
                return random.nextBoolean() ? matchingIndex : cardIndex;
            }
            firstCardByPair.put(pairId, cardIndex);
        }

        List<Integer> hidden = game.getHiddenCardIndices();
        List<Integer> unseen = new ArrayList<>();
        for (int cardIndex : hidden) {
            if (!rememberedCards.containsKey(cardIndex)) {
                unseen.add(cardIndex);
            }
        }
        return randomChoice(unseen.isEmpty() ? hidden : unseen);
    }

    public int chooseSecondCard(GameState game, int firstCard, int revealedPairId) {
        int rememberedMate = -1;
        for (Map.Entry<Integer, Integer> entry : rememberedCards.entrySet()) {
            int cardIndex = entry.getKey();
            if (cardIndex != firstCard
                && entry.getValue() == revealedPairId
                && game.getCardState(cardIndex) == GameState.CardState.HIDDEN) {
                rememberedMate = cardIndex;
                break;
            }
        }

        List<Integer> hidden = game.getHiddenCardIndices();
        List<Integer> legalChoices = hiddenCardsExcept(hidden, firstCard);
        if (rememberedMate >= 0
            && (legalChoices.size() == 1 || !overlooksKnownMatch())) {
            return rememberedMate;
        }

        List<Integer> unseen = new ArrayList<>();
        for (int cardIndex : hidden) {
            if (cardIndex != firstCard && !rememberedCards.containsKey(cardIndex)) {
                unseen.add(cardIndex);
            }
        }
        if (!unseen.isEmpty()) {
            return randomChoice(unseen);
        }

        return randomChoice(legalChoices);
    }

    private boolean overlooksKnownMatch() {
        return random.nextInt(100) < difficulty.getKnownMatchMistakePercent();
    }

    private static List<Integer> hiddenCardsExcept(List<Integer> hidden, int excludedCard) {
        List<Integer> alternatives = new ArrayList<>();
        for (int cardIndex : hidden) {
            if (cardIndex != excludedCard) {
                alternatives.add(cardIndex);
            }
        }
        return alternatives;
    }

    private int randomChoice(List<Integer> choices) {
        if (choices.isEmpty()) {
            return -1;
        }
        return choices.get(random.nextInt(choices.size()));
    }

    public int[] copyRememberedIndices() {
        int[] indices = new int[rememberedCards.size()];
        int offset = 0;
        for (int cardIndex : rememberedCards.keySet()) {
            indices[offset++] = cardIndex;
        }
        return indices;
    }

    public int[] copyRememberedPairIds() {
        int[] pairIds = new int[rememberedCards.size()];
        int offset = 0;
        for (int pairId : rememberedCards.values()) {
            pairIds[offset++] = pairId;
        }
        return pairIds;
    }

    public ComputerDifficulty getDifficulty() {
        return difficulty;
    }

    public static ComputerMemory restore(int[] indices, int[] pairIds) {
        return restore(ComputerDifficulty.HARD, indices, pairIds);
    }

    public static ComputerMemory restore(
        ComputerDifficulty difficulty,
        int[] indices,
        int[] pairIds
    ) {
        ComputerMemory memory = new ComputerMemory(difficulty);
        if (indices == null || pairIds == null || indices.length != pairIds.length) {
            return memory;
        }
        for (int index = 0; index < indices.length; index++) {
            memory.remember(indices[index], pairIds[index]);
        }
        return memory;
    }
}
