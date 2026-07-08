package com.example.flipandfind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable record of one completed game. */
public final class GameHistoryEntry {
    public static final int MAX_PLAYERS = 15;
    public static final int MAX_PARTICIPANTS = 16;
    public static final int MAX_PAIRS = 50;

    private final long completedAtEpochMillis;
    private final long activeDurationMillis;
    private final int playerCount;
    private final int pairCount;
    private final String[] participantNames;
    private final int[] scores;
    private final int[] accuracyPercents;
    private final int[] longestStreaks;

    /** Builds the optional per-player history stats directly from the game's stats owner. */
    public static GameHistoryEntry fromGameStats(
        long completedAtEpochMillis,
        long activeDurationMillis,
        int playerCount,
        int pairCount,
        String[] participantNames,
        int[] scores,
        GameStats stats
    ) {
        if (stats == null || participantNames == null || scores == null
            || scores.length != participantNames.length
            || stats.getParticipantCount() != participantNames.length) {
            throw new IllegalArgumentException("Game stats must describe every participant");
        }
        int[] accuracyPercents = new int[participantNames.length];
        int[] longestStreaks = new int[participantNames.length];
        for (int participant = 0; participant < participantNames.length; participant++) {
            if (stats.getPairs(participant) != scores[participant]) {
                throw new IllegalArgumentException("Game stats and final scores do not match");
            }
            accuracyPercents[participant] = stats.getAccuracyPercent(participant);
            longestStreaks[participant] = stats.getLongestStreak(participant);
        }
        return new GameHistoryEntry(
            completedAtEpochMillis,
            activeDurationMillis,
            playerCount,
            pairCount,
            participantNames,
            scores,
            accuracyPercents,
            longestStreaks
        );
    }

    /**
     * Creates a history entry. {@code playerCount} is the number selected on the setup screen;
     * participant arrays may contain one extra Bot entry for a solo game. Stat arrays are optional.
     */
    public GameHistoryEntry(
        long completedAtEpochMillis,
        long activeDurationMillis,
        int playerCount,
        int pairCount,
        String[] participantNames,
        int[] scores,
        int[] accuracyPercents,
        int[] longestStreaks
    ) {
        if (completedAtEpochMillis < 0L) {
            throw new IllegalArgumentException("Completion time cannot be negative");
        }
        if (activeDurationMillis < 0L) {
            throw new IllegalArgumentException("Game duration cannot be negative");
        }
        if (playerCount < 1 || playerCount > MAX_PLAYERS) {
            throw new IllegalArgumentException("Player count must be from 1 to 15");
        }
        if (pairCount < 1 || pairCount > MAX_PAIRS) {
            throw new IllegalArgumentException("Pair count must be from 1 to 50");
        }
        if (participantNames == null || scores == null
            || participantNames.length == 0
            || participantNames.length > MAX_PARTICIPANTS
            || participantNames.length != scores.length) {
            throw new IllegalArgumentException("Names and scores must describe every participant");
        }
        if (participantNames.length < playerCount
            || participantNames.length > playerCount + 1) {
            throw new IllegalArgumentException("Participant count is inconsistent with player count");
        }
        validateOptionalStats(accuracyPercents, longestStreaks, participantNames.length);

        this.participantNames = new String[participantNames.length];
        this.scores = scores.clone();
        int capturedPairs = 0;
        for (int index = 0; index < participantNames.length; index++) {
            String name = participantNames[index] == null ? "" : participantNames[index].trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Participant names cannot be blank");
            }
            if (name.length() > 256) {
                throw new IllegalArgumentException("Participant names are too long");
            }
            if (scores[index] < 0) {
                throw new IllegalArgumentException("Scores cannot be negative");
            }
            this.participantNames[index] = name;
            capturedPairs = Math.addExact(capturedPairs, scores[index]);
        }
        if (capturedPairs != pairCount) {
            throw new IllegalArgumentException("Completed-game scores must account for every pair");
        }

        this.completedAtEpochMillis = completedAtEpochMillis;
        this.activeDurationMillis = activeDurationMillis;
        this.playerCount = playerCount;
        this.pairCount = pairCount;
        this.accuracyPercents = accuracyPercents == null ? null : accuracyPercents.clone();
        this.longestStreaks = longestStreaks == null ? null : longestStreaks.clone();
    }

    /** Convenience overload for local games where every participant is a selected player. */
    public GameHistoryEntry(
        long completedAtEpochMillis,
        long activeDurationMillis,
        int pairCount,
        String[] participantNames,
        int[] scores,
        int[] accuracyPercents,
        int[] longestStreaks
    ) {
        this(
            completedAtEpochMillis,
            activeDurationMillis,
            participantNames == null ? 0 : participantNames.length,
            pairCount,
            participantNames,
            scores,
            accuracyPercents,
            longestStreaks
        );
    }

    public long getCompletedAtEpochMillis() {
        return completedAtEpochMillis;
    }

    public long getActiveDurationMillis() {
        return activeDurationMillis;
    }

    /** Number of human players selected on the setup screen; solo is one. */
    public int getPlayerCount() {
        return playerCount;
    }

    /** Number of scored participants, including the Bot in a solo game. */
    public int getParticipantCount() {
        return participantNames.length;
    }

    public int getPairCount() {
        return pairCount;
    }

    public String getParticipantName(int participant) {
        checkParticipant(participant);
        return participantNames[participant];
    }

    public int getScore(int participant) {
        checkParticipant(participant);
        return scores[participant];
    }

    public String[] copyParticipantNames() {
        return participantNames.clone();
    }

    public int[] copyScores() {
        return scores.clone();
    }

    public boolean hasAccuracyStats() {
        return accuracyPercents != null;
    }

    public int getAccuracyPercent(int participant) {
        checkParticipant(participant);
        if (accuracyPercents == null) {
            throw new IllegalStateException("Accuracy was not recorded for this game");
        }
        return accuracyPercents[participant];
    }

    public int[] copyAccuracyPercents() {
        return accuracyPercents == null ? null : accuracyPercents.clone();
    }

    public boolean hasLongestStreakStats() {
        return longestStreaks != null;
    }

    public int getLongestStreak(int participant) {
        checkParticipant(participant);
        if (longestStreaks == null) {
            throw new IllegalStateException("Streaks were not recorded for this game");
        }
        return longestStreaks[participant];
    }

    public int[] copyLongestStreaks() {
        return longestStreaks == null ? null : longestStreaks.clone();
    }

    public int[] copyWinnerIndices() {
        int winningScore = 0;
        for (int score : scores) {
            winningScore = Math.max(winningScore, score);
        }
        int count = 0;
        for (int score : scores) {
            if (score == winningScore) {
                count++;
            }
        }
        int[] winners = new int[count];
        int output = 0;
        for (int participant = 0; participant < scores.length; participant++) {
            if (scores[participant] == winningScore) {
                winners[output++] = participant;
            }
        }
        return winners;
    }

    public List<String> getWinnerNames() {
        int[] winnerIndices = copyWinnerIndices();
        ArrayList<String> names = new ArrayList<>(winnerIndices.length);
        for (int winner : winnerIndices) {
            names.add(participantNames[winner]);
        }
        return Collections.unmodifiableList(names);
    }

    public boolean isTie() {
        return copyWinnerIndices().length > 1;
    }

    /** Compact, display-ready score such as "Maya 7 • Player B 5". */
    public String getCompactScoreSummary() {
        StringBuilder summary = new StringBuilder();
        for (int participant = 0; participant < participantNames.length; participant++) {
            if (participant > 0) {
                summary.append(" • ");
            }
            summary.append(participantNames[participant]).append(' ').append(scores[participant]);
        }
        return summary.toString();
    }

    @Override
    public boolean equals(Object value) {
        if (this == value) {
            return true;
        }
        if (!(value instanceof GameHistoryEntry)) {
            return false;
        }
        GameHistoryEntry other = (GameHistoryEntry) value;
        return completedAtEpochMillis == other.completedAtEpochMillis
            && activeDurationMillis == other.activeDurationMillis
            && playerCount == other.playerCount
            && pairCount == other.pairCount
            && Arrays.equals(participantNames, other.participantNames)
            && Arrays.equals(scores, other.scores)
            && Arrays.equals(accuracyPercents, other.accuracyPercents)
            && Arrays.equals(longestStreaks, other.longestStreaks);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(
            completedAtEpochMillis,
            activeDurationMillis,
            playerCount,
            pairCount
        );
        result = 31 * result + Arrays.hashCode(participantNames);
        result = 31 * result + Arrays.hashCode(scores);
        result = 31 * result + Arrays.hashCode(accuracyPercents);
        result = 31 * result + Arrays.hashCode(longestStreaks);
        return result;
    }

    private void checkParticipant(int participant) {
        if (participant < 0 || participant >= participantNames.length) {
            throw new IllegalArgumentException("Participant is out of range");
        }
    }

    private static void validateOptionalStats(
        int[] accuracyPercents,
        int[] longestStreaks,
        int participantCount
    ) {
        if (accuracyPercents != null && accuracyPercents.length != participantCount) {
            throw new IllegalArgumentException("Accuracy must describe every participant");
        }
        if (longestStreaks != null && longestStreaks.length != participantCount) {
            throw new IllegalArgumentException("Streaks must describe every participant");
        }
        if (accuracyPercents != null) {
            for (int accuracy : accuracyPercents) {
                if (accuracy < 0 || accuracy > 100) {
                    throw new IllegalArgumentException("Accuracy must be from 0 to 100");
                }
            }
        }
        if (longestStreaks != null) {
            for (int streak : longestStreaks) {
                if (streak < 0) {
                    throw new IllegalArgumentException("Streaks cannot be negative");
                }
            }
        }
    }
}
