package com.example.flipandfind;

/** Controls how much the bot remembers and how reliably it uses a known match. */
public enum ComputerDifficulty {
    BASIC("Basic", 2, 30),
    EASY("Easy", 4, 20),
    MEDIUM("Medium", 12, 10),
    HARD("Hard", Integer.MAX_VALUE, 5);

    private final String displayName;
    private final int rememberedCardLimit;
    private final int knownMatchMistakePercent;

    ComputerDifficulty(
        String displayName,
        int rememberedCardLimit,
        int knownMatchMistakePercent
    ) {
        this.displayName = displayName;
        this.rememberedCardLimit = rememberedCardLimit;
        this.knownMatchMistakePercent = knownMatchMistakePercent;
    }

    public String getDisplayName() {
        return displayName;
    }

    int getRememberedCardLimit() {
        return rememberedCardLimit;
    }

    int getKnownMatchMistakePercent() {
        return knownMatchMistakePercent;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
