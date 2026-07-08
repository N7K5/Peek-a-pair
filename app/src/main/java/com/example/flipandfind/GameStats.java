package com.example.flipandfind;

import java.math.BigInteger;

/** Pure per-participant statistics for a single game. */
public final class GameStats {
    public static final int NO_PARTICIPANT = -1;
    public static final long NO_DECISION_TIME = -1L;

    private final int[] attempts;
    private final int[] pairs;
    private final int[] currentStreaks;
    private final int[] longestStreaks;
    private final int[] maxDeficits;
    private final long[] decisionTimeTotalsMillis;
    private final int[] timedAttempts;

    public GameStats(int participantCount) {
        if (participantCount < 1) {
            throw new IllegalArgumentException("At least one stats participant is required");
        }
        attempts = new int[participantCount];
        pairs = new int[participantCount];
        currentStreaks = new int[participantCount];
        longestStreaks = new int[participantCount];
        maxDeficits = new int[participantCount];
        decisionTimeTotalsMillis = new long[participantCount];
        timedAttempts = new int[participantCount];
    }

    private GameStats(
        int[] attempts,
        int[] pairs,
        int[] currentStreaks,
        int[] longestStreaks,
        int[] maxDeficits,
        long[] decisionTimeTotalsMillis,
        int[] timedAttempts
    ) {
        this.attempts = attempts.clone();
        this.pairs = pairs.clone();
        this.currentStreaks = currentStreaks.clone();
        this.longestStreaks = longestStreaks.clone();
        this.maxDeficits = maxDeficits.clone();
        this.decisionTimeTotalsMillis = decisionTimeTotalsMillis.clone();
        this.timedAttempts = timedAttempts.clone();
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
        validateResolutionScores(participant, matched, participantScores);
        recordAttempt(participant, matched);
    }

    /**
     * Records a resolved attempt together with the active decision time between its first and
     * second card reveals.
     */
    public void recordResolution(
        int participant,
        boolean matched,
        int[] participantScores,
        long decisionTimeMillis
    ) {
        if (decisionTimeMillis < 0L) {
            throw new IllegalArgumentException("Decision time cannot be negative");
        }
        validateResolutionScores(participant, matched, participantScores);
        long updatedTotal = Math.addExact(
            decisionTimeTotalsMillis[participant],
            decisionTimeMillis
        );
        int updatedTimedAttempts = Math.incrementExact(timedAttempts[participant]);
        recordAttempt(participant, matched);
        decisionTimeTotalsMillis[participant] = updatedTotal;
        timedAttempts[participant] = updatedTimedAttempts;
    }

    private void validateResolutionScores(
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

    public int getTimedAttempts(int participant) {
        checkParticipant(participant);
        return timedAttempts[participant];
    }

    public long getDecisionTimeTotalMillis(int participant) {
        checkParticipant(participant);
        return decisionTimeTotalsMillis[participant];
    }

    /** Returns the rounded average decision time, or {@link #NO_DECISION_TIME} without data. */
    public long getAverageDecisionTimeMillis(int participant) {
        checkParticipant(participant);
        int count = timedAttempts[participant];
        if (count == 0) {
            return NO_DECISION_TIME;
        }
        long total = decisionTimeTotalsMillis[participant];
        long quotient = total / count;
        long remainder = total % count;
        return quotient + (remainder >= (count + 1L) / 2L ? 1L : 0L);
    }

    /** Returns all participants sharing the lowest exact average decision time. */
    public int[] getQuickestParticipants() {
        return participantsAtDecisionTimeExtreme(true);
    }

    /** Returns all participants sharing the highest exact average decision time. */
    public int[] getSlowestParticipants() {
        return participantsAtDecisionTimeExtreme(false);
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

    public long[] copyDecisionTimeTotalsMillis() {
        return decisionTimeTotalsMillis.clone();
    }

    public int[] copyTimedAttempts() {
        return timedAttempts.clone();
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
        return new GameStats(
            attempts,
            pairs,
            currentStreaks,
            longestStreaks,
            maxDeficits,
            new long[attempts.length],
            new int[attempts.length]
        );
    }

    /** Restores a defensive, validated stats snapshot including decision-time data. */
    public static GameStats restore(
        int[] attempts,
        int[] pairs,
        int[] currentStreaks,
        int[] longestStreaks,
        int[] maxDeficits,
        long[] decisionTimeTotalsMillis,
        int[] timedAttempts
    ) {
        validateSnapshot(attempts, pairs, currentStreaks, longestStreaks, maxDeficits);
        validateDecisionTimeSnapshot(attempts, decisionTimeTotalsMillis, timedAttempts);
        return new GameStats(
            attempts,
            pairs,
            currentStreaks,
            longestStreaks,
            maxDeficits,
            decisionTimeTotalsMillis,
            timedAttempts
        );
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

    /** Restores all tracked values while checking the participant count saved by its owner. */
    public static GameStats restore(
        int participantCount,
        int[] attempts,
        int[] pairs,
        int[] currentStreaks,
        int[] longestStreaks,
        int[] maxDeficits,
        long[] decisionTimeTotalsMillis,
        int[] timedAttempts
    ) {
        if (participantCount < 1 || attempts == null || attempts.length != participantCount) {
            throw new IllegalArgumentException("Stats participant count is inconsistent");
        }
        return restore(
            attempts,
            pairs,
            currentStreaks,
            longestStreaks,
            maxDeficits,
            decisionTimeTotalsMillis,
            timedAttempts
        );
    }

    private int[] participantsAtDecisionTimeExtreme(boolean quickest) {
        int[] candidates = new int[timedAttempts.length];
        int candidateCount = 0;
        int best = NO_PARTICIPANT;
        for (int participant = 0; participant < timedAttempts.length; participant++) {
            if (timedAttempts[participant] == 0) {
                continue;
            }
            int comparison = best == NO_PARTICIPANT
                ? 0
                : compareDecisionAverages(participant, best);
            boolean better = best == NO_PARTICIPANT
                || (quickest ? comparison < 0 : comparison > 0);
            if (better) {
                best = participant;
                candidates[0] = participant;
                candidateCount = 1;
            } else if (comparison == 0) {
                candidates[candidateCount++] = participant;
            }
        }
        int[] result = new int[candidateCount];
        System.arraycopy(candidates, 0, result, 0, candidateCount);
        return result;
    }

    private int compareDecisionAverages(int left, int right) {
        BigInteger leftScaled = BigInteger.valueOf(decisionTimeTotalsMillis[left])
            .multiply(BigInteger.valueOf(timedAttempts[right]));
        BigInteger rightScaled = BigInteger.valueOf(decisionTimeTotalsMillis[right])
            .multiply(BigInteger.valueOf(timedAttempts[left]));
        return leftScaled.compareTo(rightScaled);
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

    private static void validateDecisionTimeSnapshot(
        int[] attempts,
        long[] decisionTimeTotalsMillis,
        int[] timedAttempts
    ) {
        if (decisionTimeTotalsMillis == null || timedAttempts == null
            || decisionTimeTotalsMillis.length != attempts.length
            || timedAttempts.length != attempts.length) {
            throw new IllegalArgumentException("Decision-time arrays have the wrong size");
        }
        for (int participant = 0; participant < attempts.length; participant++) {
            if (decisionTimeTotalsMillis[participant] < 0L
                || timedAttempts[participant] < 0
                || timedAttempts[participant] > attempts[participant]) {
                throw new IllegalArgumentException("Saved decision-time values are inconsistent");
            }
            if (timedAttempts[participant] == 0
                && decisionTimeTotalsMillis[participant] != 0L) {
                throw new IllegalArgumentException("Decision time requires a timed attempt");
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
