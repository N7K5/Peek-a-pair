package com.example.flipandfind;

/**
 * Process/session-scoped resolver for tabletop selections.
 *
 * <p>Create a new instance on a fresh app launch. RANDOM_EACH_GAME deliberately starts with
 * Black / white and avoids repeating the previous random-game surface on later choices.
 */
public final class TabletopSession {
    private static final long GAME_INCREMENT = 0xD1B54A32D192ED03L;

    private final long sessionSeed;
    private int randomGamesStarted;
    private TabletopMode previousRandomGameMode;

    public TabletopSession(long sessionSeed) {
        this(sessionSeed, 0, TabletopMode.STATIC_THEME);
    }

    private TabletopSession(
        long sessionSeed,
        int randomGamesStarted,
        TabletopMode previousRandomGameMode
    ) {
        this.sessionSeed = sessionSeed;
        this.randomGamesStarted = Math.max(0, randomGamesStarted);
        this.previousRandomGameMode = this.randomGamesStarted == 0
            ? TabletopMode.STATIC_THEME
            : safeConcreteMode(previousRandomGameMode);
    }

    /**
     * Resolves the persisted selection to the concrete surface for a newly created game.
     * Concrete selections are returned unchanged and do not consume the random sequence.
     */
    public TabletopMode beginGame(TabletopMode selectedMode, long gameSeed) {
        TabletopMode safeSelection = selectedMode == null
            ? TabletopMode.STATIC_THEME
            : selectedMode;
        if (safeSelection.isConcrete()) {
            return safeSelection;
        }

        TabletopMode chosen;
        if (randomGamesStarted == 0) {
            chosen = TabletopMode.STATIC_THEME;
        } else {
            chosen = chooseDifferentMode(
                sessionSeed,
                gameSeed,
                randomGamesStarted,
                previousRandomGameMode
            );
        }
        randomGamesStarted++;
        previousRandomGameMode = chosen;
        return chosen;
    }

    /** Restores random-sequence continuity after an activity recreation. */
    public static TabletopSession restore(
        long sessionSeed,
        int randomGamesStarted,
        String previousModePreferenceId
    ) {
        return new TabletopSession(
            sessionSeed,
            randomGamesStarted,
            TabletopMode.fromPreference(previousModePreferenceId)
        );
    }

    public long getSessionSeed() {
        return sessionSeed;
    }

    public int getRandomGamesStarted() {
        return randomGamesStarted;
    }

    public TabletopMode getPreviousRandomGameMode() {
        return previousRandomGameMode;
    }

    public String getPreviousRandomGameModePreferenceId() {
        return previousRandomGameMode.getPreferenceId();
    }

    private static TabletopMode chooseDifferentMode(
        long sessionSeed,
        long gameSeed,
        int gameNumber,
        TabletopMode previousMode
    ) {
        TabletopMode[] modes = TabletopMode.concreteModesCopy();
        int previousIndex = indexOf(modes, safeConcreteMode(previousMode));
        long entropy = sessionSeed
            ^ Long.rotateLeft(gameSeed, 21)
            ^ (GAME_INCREMENT * gameNumber);
        int candidate = boundedIndex(mix64(entropy), modes.length - 1);
        if (candidate >= previousIndex) {
            candidate++;
        }
        return modes[candidate];
    }

    private static int indexOf(TabletopMode[] modes, TabletopMode target) {
        for (int index = 0; index < modes.length; index++) {
            if (modes[index] == target) {
                return index;
            }
        }
        return 0;
    }

    private static TabletopMode safeConcreteMode(TabletopMode mode) {
        return mode != null && mode.isConcrete() ? mode : TabletopMode.STATIC_THEME;
    }

    private static int boundedIndex(long value, int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("Bound must be positive");
        }
        return (int) ((value & Long.MAX_VALUE) % bound);
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
