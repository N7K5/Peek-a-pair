package com.example.flipandfind;

/**
 * Process/session-scoped chooser for card-back modes.
 *
 * <p>Create a new instance on a fresh app launch. RANDOM_EACH_GAME then deliberately starts
 * with Classic and avoids repeating the previous game on every later choice.
 */
public final class CardBackSession {
    private static final long GAME_INCREMENT = 0xD1B54A32D192ED03L;

    private final long sessionSeed;
    private int randomGamesStarted;
    private CardBackStyle previousRandomGameStyle;

    public CardBackSession(long sessionSeed) {
        this(sessionSeed, 0, CardBackStyle.CLASSIC);
    }

    private CardBackSession(
        long sessionSeed,
        int randomGamesStarted,
        CardBackStyle previousRandomGameStyle
    ) {
        this.sessionSeed = sessionSeed;
        this.randomGamesStarted = Math.max(0, randomGamesStarted);
        this.previousRandomGameStyle = this.randomGamesStarted == 0
            ? CardBackStyle.CLASSIC
            : safeStyle(previousRandomGameStyle);
    }

    /**
     * Resolves the chosen mode for a newly created game.
     *
     * @param fixedStyle the user's persisted concrete design, used only by FIXED
     * @param gameSeed a seed that is saved with the active game
     */
    public CardBackSelection beginGame(
        CardBackMode mode,
        CardBackStyle fixedStyle,
        long gameSeed
    ) {
        CardBackMode safeMode = mode == null ? CardBackMode.FIXED : mode;
        if (safeMode == CardBackMode.RANDOM_EACH_CARD) {
            return CardBackSelection.randomEachCard(gameSeed);
        }
        if (safeMode == CardBackMode.FIXED) {
            return CardBackSelection.fixed(safeStyle(fixedStyle), gameSeed);
        }

        CardBackStyle chosen;
        if (randomGamesStarted == 0) {
            chosen = CardBackStyle.CLASSIC;
        } else {
            chosen = chooseDifferentStyle(
                sessionSeed,
                gameSeed,
                randomGamesStarted,
                previousRandomGameStyle
            );
        }
        randomGamesStarted++;
        previousRandomGameStyle = chosen;
        return CardBackSelection.uniform(CardBackMode.RANDOM_EACH_GAME, chosen, gameSeed);
    }

    /** Restores session continuity after an activity recreation, not a fresh root launch. */
    public static CardBackSession restore(
        long sessionSeed,
        int randomGamesStarted,
        String previousStylePreferenceId
    ) {
        return new CardBackSession(
            sessionSeed,
            randomGamesStarted,
            CardBackStyle.fromPreference(previousStylePreferenceId)
        );
    }

    public long getSessionSeed() {
        return sessionSeed;
    }

    public int getRandomGamesStarted() {
        return randomGamesStarted;
    }

    public CardBackStyle getPreviousRandomGameStyle() {
        return previousRandomGameStyle;
    }

    public String getPreviousRandomGameStylePreferenceId() {
        return previousRandomGameStyle.getPreferenceId();
    }

    private static CardBackStyle chooseDifferentStyle(
        long sessionSeed,
        long gameSeed,
        int gameNumber,
        CardBackStyle previousStyle
    ) {
        CardBackStyle[] styles = CardBackSelection.concreteStylesCopy();
        int previousIndex = indexOf(styles, safeStyle(previousStyle));
        long entropy = sessionSeed
            ^ Long.rotateLeft(gameSeed, 21)
            ^ (GAME_INCREMENT * gameNumber);
        int candidate = CardBackSelection.boundedIndex(
            CardBackSelection.mix64(entropy),
            styles.length - 1
        );
        if (candidate >= previousIndex) {
            candidate++;
        }
        return styles[candidate];
    }

    private static int indexOf(CardBackStyle[] styles, CardBackStyle target) {
        for (int index = 0; index < styles.length; index++) {
            if (styles[index] == target) {
                return index;
            }
        }
        return 0;
    }

    private static CardBackStyle safeStyle(CardBackStyle style) {
        return style == null ? CardBackStyle.CLASSIC : style;
    }
}
