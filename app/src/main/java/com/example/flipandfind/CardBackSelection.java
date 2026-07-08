package com.example.flipandfind;

/**
 * Immutable card-back choices for one game.
 *
 * <p>The random-across-cards permutation is derived only from the saved game seed, so the
 * design at a card index remains stable through redraws and activity recreation.
 */
public final class CardBackSelection {
    private static final long SHUFFLE_INCREMENT = 0x9E3779B97F4A7C15L;

    // Keep pseudo-modes out of CardBackStyle so every resolved value is a drawable design.
    private static final CardBackStyle[] CONCRETE_STYLES = {
        CardBackStyle.CLASSIC,
        CardBackStyle.CONSTELLATION,
        CardBackStyle.SUNBURST,
        CardBackStyle.WAVES,
        CardBackStyle.HARLEQUIN,
        CardBackStyle.ORBITS,
        CardBackStyle.PRISM,
        CardBackStyle.BOTANICAL,
        CardBackStyle.WEAVE,
        CardBackStyle.AURORA,
        CardBackStyle.FIREFLIES,
        CardBackStyle.KALEIDO,
        CardBackStyle.COMET_TRAILS,
        CardBackStyle.MOON_RIPPLES,
        CardBackStyle.PIXEL_RAIN
    };

    private final CardBackMode mode;
    private final CardBackStyle uniformStyle;
    private final long gameSeed;
    private final CardBackStyle[] perCardCycle;

    private CardBackSelection(
        CardBackMode mode,
        CardBackStyle uniformStyle,
        long gameSeed,
        CardBackStyle[] perCardCycle
    ) {
        this.mode = mode;
        this.uniformStyle = uniformStyle;
        this.gameSeed = gameSeed;
        this.perCardCycle = perCardCycle;
    }

    /** Creates the backward-compatible fixed-style selection. */
    public static CardBackSelection fixed(CardBackStyle style, long gameSeed) {
        return uniform(CardBackMode.FIXED, style, gameSeed);
    }

    /** Creates a stable shuffled cycle of the concrete designs for one table. */
    public static CardBackSelection randomEachCard(long gameSeed) {
        return new CardBackSelection(
            CardBackMode.RANDOM_EACH_CARD,
            null,
            gameSeed,
            shuffledStyles(gameSeed)
        );
    }

    static CardBackSelection uniform(
        CardBackMode mode,
        CardBackStyle style,
        long gameSeed
    ) {
        CardBackMode safeMode = mode == null ? CardBackMode.FIXED : mode;
        if (safeMode == CardBackMode.RANDOM_EACH_CARD) {
            throw new IllegalArgumentException("Random-each-card selections are not uniform");
        }
        return new CardBackSelection(
            safeMode,
            style == null ? CardBackStyle.CLASSIC : style,
            gameSeed,
            null
        );
    }

    /** Restores the exact selection saved for an active game. */
    public static CardBackSelection restore(
        String modePreferenceId,
        String uniformStylePreferenceId,
        long gameSeed
    ) {
        CardBackMode restoredMode = CardBackMode.fromPreference(modePreferenceId);
        if (restoredMode == CardBackMode.RANDOM_EACH_CARD) {
            return randomEachCard(gameSeed);
        }
        return uniform(
            restoredMode,
            CardBackStyle.fromPreference(uniformStylePreferenceId),
            gameSeed
        );
    }

    public CardBackMode getMode() {
        return mode;
    }

    public long getGameSeed() {
        return gameSeed;
    }

    public boolean isUniform() {
        return perCardCycle == null;
    }

    /** Returns the one style used by FIXED and RANDOM_EACH_GAME selections. */
    public CardBackStyle getUniformStyle() {
        if (!isUniform()) {
            throw new IllegalStateException("This selection varies by card");
        }
        return uniformStyle;
    }

    /**
     * Value suitable for an active-game Bundle. Random-across-cards needs only its mode and
     * seed, so Classic is emitted as a harmless fallback value.
     */
    public String getSavedUniformStylePreferenceId() {
        return isUniform()
            ? uniformStyle.getPreferenceId()
            : CardBackStyle.CLASSIC.getPreferenceId();
    }

    /** Resolves a concrete drawable design for a zero-based card index. */
    public CardBackStyle styleForCard(int cardIndex) {
        if (cardIndex < 0) {
            throw new IllegalArgumentException("Card index cannot be negative");
        }
        if (isUniform()) {
            return uniformStyle;
        }
        return perCardCycle[cardIndex % perCardCycle.length];
    }

    static CardBackStyle[] concreteStylesCopy() {
        return CONCRETE_STYLES.clone();
    }

    private static CardBackStyle[] shuffledStyles(long seed) {
        CardBackStyle[] styles = CONCRETE_STYLES.clone();
        long state = seed;
        for (int index = styles.length - 1; index > 0; index--) {
            state += SHUFFLE_INCREMENT;
            int swapIndex = boundedIndex(mix64(state), index + 1);
            CardBackStyle old = styles[index];
            styles[index] = styles[swapIndex];
            styles[swapIndex] = old;
        }
        return styles;
    }

    static int boundedIndex(long value, int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("Bound must be positive");
        }
        return (int) ((value & Long.MAX_VALUE) % bound);
    }

    static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
