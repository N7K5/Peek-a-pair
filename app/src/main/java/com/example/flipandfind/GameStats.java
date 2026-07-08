package com.example.flipandfind;

/** Pure per-participant statistics for a single game. */
public final class GameStats {
    public static final int NO_PARTICIPANT = -1;

    private final int[] attempts;
    private final int[] pairs;
    private final int[] currentStreaks;
    private final int[] longestStreaks;
    private final int[] maxDeficits;

    public GameStats(int participantCount) {
        if (participantCount < 1) {
            throw new IllegalArgumentException("At least one stats participant is required");
        }
        attempts = new int[participantCount];
        pairs = new int[participantCount];
        currentStreaks = new int[participantCount];
        longestStreaks = new int[participantCount];
        maxDeficits = new int[participantCount];
    }

    private GameStats(
        int[] attempts,
        int[] pairs,
        int[] currentStreaks,
        int[] longestStreaks,
        int[] maxDeficits
    ) {
        this.attempts = attempts.clone();
        this.pairs = pairs.clone();
        this.currentStreaks = currentStreaks.clone();
        this.longestStreaks = longestStreaks.clone();
        this.maxDeficits = maxDeficits.clone();
    }

    /** Records one completed two-card attempt for a player or team participant. */
    public void recordAttempt(int participant, boolean matched) {
        checkParticipant(participant);
        attempts[participant] = Math.incrementExact(attempts[participant]);
        if (matched) {
            pairs[participant] = Math.incrementExact(pairs[participant]);
            currentStreaks[participant] = Math.incrementExact(currentStreaks[participant]);
            longestStreaks[participant] = Math.max(
                longestStreaks[participant],
                currentStreaks[participant]
            );
        } else {
            currentStreaks[participant] = 0;
        }
        updateMaxDeficits();
    }

    /**
     * Records a resolved attempt and verifies that the supplied authoritative participant
     * scores reflect exactly that resolution.
     */
    public void recordResolution(
        int participant,
        boolean matched,
        int[] participantScores
    ) {
        checkParticipant(participant);
        if (participantScores == null || participantScores.length != pairs.length) {
            throw new IllegalArgumentException("Participant scores have the wrong size");
        }
        for (int index = 0; index < participantScores.length; index++) {
            int expected = pairs[index] + (matched && index == participant ? 1 : 0);
            if (participantScores[index] != expected) {
                throw new IllegalArgumentException("Participant scores do not match the resolution");
            }
        }
        recordAttempt(participant, matched);
    }

    public int getParticipantCount() {
        return attempts.length;
    }

    public int getAttempts(int participant) {
        checkParticipant(participant);
        return attempts[participant];
    }

    public int getPairs(int participant) {
        checkParticipant(participant);
        return pairs[participant];
    }

    public int getMatches(int participant) {
        return getPairs(participant);
    }

    public int getCurrentStreak(int participant) {
        checkParticipant(participant);
        return currentStreaks[participant];
    }

    public int getLongestStreak(int participant) {
        checkParticipant(participant);
        return longestStreaks[participant];
    }

    public int getMaxDeficit(int participant) {
        checkParticipant(participant);
        return maxDeficits[participant];
    }

    /** Returns match accuracy in the inclusive range 0.0 to 1.0. */
    public double getAccuracy(int participant) {
        checkParticipant(participant);
        return attempts[participant] == 0
            ? 0d
            : pairs[participant] / (double) attempts[participant];
    }

    public int getAccuracyPercent(int participant) {
        return (int) Math.round(getAccuracy(participant) * 100d);
    }

    public int getTotalAttempts() {
        return sum(attempts);
    }

    public int getTotalPairs() {
        return sum(pairs);
    }

    public double getOverallAccuracy() {
        int totalAttempts = getTotalAttempts();
        return totalAttempts == 0 ? 0d : getTotalPairs() / (double) totalAttempts;
    }

    public int getOverallAccuracyPercent() {
        return (int) Math.round(getOverallAccuracy() * 100d);
    }

    public int getGameLongestStreak() {
        int longest = 0;
        for (int streak : longestStreaks) {
            longest = Math.max(longest, streak);
        }
        return longest;
    }

    /**
     * Returns the unique final winner who recovered from the largest positive deficit.
     * Returns {@link #NO_PARTICIPANT} when nobody trailed or the comeback is tied.
     */
    public int getComebackParticipant() {
        int winningPairs = maximum(pairs);
        int candidate = NO_PARTICIPANT;
        int candidateDeficit = 0;
        boolean tied = false;
        for (int participant = 0; participant < pairs.length; participant++) {
            if (pairs[participant] != winningPairs || maxDeficits[participant] <= 0) {
                continue;
            }
            if (maxDeficits[participant] > candidateDeficit) {
                candidate = participant;
                candidateDeficit = maxDeficits[participant];
                tied = false;
            } else if (maxDeficits[participant] == candidateDeficit) {
                tied = true;
            }
        }
        return candidateDeficit > 0 && !tied ? candidate : NO_PARTICIPANT;
    }

    public int[] copyAttempts() {
        return attempts.clone();
    }

    public int[] copyPairs() {
        return pairs.clone();
    }

    public int[] copyMatches() {
        return copyPairs();
    }

    public int[] copyCurrentStreaks() {
        return currentStreaks.clone();
    }

    public int[] copyLongestStreaks() {
        return longestStreaks.clone();
    }

    public int[] copyMaxDeficits() {
        return maxDeficits.clone();
    }

    /** Restores a defensive, validated stats snapshot. */
    public static GameStats restore(
        int[] attempts,
        int[] pairs,
        int[] currentStreaks,
        int[] longestStreaks,
        int[] maxDeficits
    ) {
        validateSnapshot(attempts, pairs, currentStreaks, longestStreaks, maxDeficits);
        return new GameStats(attempts, pairs, currentStreaks, longestStreaks, maxDeficits);
    }

    /** Restores a snapshot while also checking the participant count saved by its owner. */
    public static GameStats restore(
        int participantCount,
        int[] attempts,
        int[] pairs,
        int[] currentStreaks,
        int[] longestStreaks,
        int[] maxDeficits
    ) {
        if (participantCount < 1 || attempts == null || attempts.length != participantCount) {
            throw new IllegalArgumentException("Stats participant count is inconsistent");
        }
        return restore(attempts, pairs, currentStreaks, longestStreaks, maxDeficits);
    }

    private void updateMaxDeficits() {
        int leader = maximum(pairs);
        for (int participant = 0; participant < pairs.length; participant++) {
            maxDeficits[participant] = Math.max(
                maxDeficits[participant],
                leader - pairs[participant]
            );
        }
    }

    private void checkParticipant(int participant) {
        if (participant < 0 || participant >= attempts.length) {
            throw new IllegalArgumentException("Stats participant is out of range");
        }
    }

    private static void validateSnapshot(
        int[] attempts,
        int[] pairs,
        int[] currentStreaks,
        int[] longestStreaks,
        int[] maxDeficits
    ) {
        if (attempts == null || pairs == null || currentStreaks == null
            || longestStreaks == null || maxDeficits == null) {
            throw new IllegalArgumentException("Every stats array is required");
        }
        int count = attempts.length;
        if (count == 0 || pairs.length != count || currentStreaks.length != count
            || longestStreaks.length != count || maxDeficits.length != count) {
            throw new IllegalArgumentException("Stats arrays must have one consistent size");
        }
        int leader = maximum(pairs);
        for (int participant = 0; participant < count; participant++) {
            if (attempts[participant] < 0 || pairs[participant] < 0
                || currentStreaks[participant] < 0 || longestStreaks[participant] < 0
                || maxDeficits[participant] < 0) {
                throw new IllegalArgumentException("Stats values cannot be negative");
            }
            if (pairs[participant] > attempts[participant]) {
                throw new IllegalArgumentException("Pairs cannot exceed attempts");
            }
            if (currentStreaks[participant] > longestStreaks[participant]
                || longestStreaks[participant] > pairs[participant]) {
                throw new IllegalArgumentException("Saved streaks are inconsistent");
            }
            if (maxDeficits[participant] < leader - pairs[participant]) {
                throw new IllegalArgumentException("Saved deficit is inconsistent");
            }
        }
    }

    private static int maximum(int[] values) {
        int result = 0;
        for (int value : values) {
            result = Math.max(result, value);
        }
        return result;
    }

    private static int sum(int[] values) {
        int result = 0;
        for (int value : values) {
            result = Math.addExact(result, value);
        }
        return result;
    }
}
