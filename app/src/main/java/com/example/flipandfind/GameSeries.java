package com.example.flipandfind;

import java.util.Arrays;

/** Pure Play Again series scoreboard for one unchanged participant lineup. */
public final class GameSeries {
    public static final int NO_PARTICIPANT = -1;

    private final String[] participantNames;
    private final int[] wins;
    private final int[] currentWinStreaks;
    private final int[] longestWinStreaks;
    private int gameCount;
    private int tiedGameCount;

    public GameSeries(String[] participantNames) {
        this(
            validateAndCopyNames(participantNames),
            0,
            0,
            new int[participantNames == null ? 0 : participantNames.length],
            new int[participantNames == null ? 0 : participantNames.length],
            new int[participantNames == null ? 0 : participantNames.length]
        );
    }

    private GameSeries(
        String[] participantNames,
        int gameCount,
        int tiedGameCount,
        int[] wins,
        int[] currentWinStreaks,
        int[] longestWinStreaks
    ) {
        this.participantNames = participantNames;
        this.gameCount = gameCount;
        this.tiedGameCount = tiedGameCount;
        this.wins = wins;
        this.currentWinStreaks = currentWinStreaks;
        this.longestWinStreaks = longestWinStreaks;
    }

    /**
     * Restores the arrays written by the copy methods. Ties count as series results, do not
     * award an outright win, and break every consecutive outright-win streak.
     */
    public static GameSeries restore(
        String[] participantNames,
        int gameCount,
        int tiedGameCount,
        int[] wins,
        int[] currentWinStreaks,
        int[] longestWinStreaks
    ) {
        String[] names = validateAndCopyNames(participantNames);
        int participantCount = names.length;
        if (gameCount < 0 || tiedGameCount < 0 || tiedGameCount > gameCount) {
            throw new IllegalArgumentException("Series game counts are inconsistent");
        }
        validateArray(wins, participantCount, "wins");
        validateArray(currentWinStreaks, participantCount, "current streaks");
        validateArray(longestWinStreaks, participantCount, "longest streaks");

        int totalWins = 0;
        int activeStreaks = 0;
        for (int participant = 0; participant < participantCount; participant++) {
            if (wins[participant] < 0
                || currentWinStreaks[participant] < 0
                || longestWinStreaks[participant] < 0) {
                throw new IllegalArgumentException("Series values cannot be negative");
            }
            if (currentWinStreaks[participant] > longestWinStreaks[participant]
                || longestWinStreaks[participant] > wins[participant]) {
                throw new IllegalArgumentException("Series streaks are inconsistent with wins");
            }
            totalWins = Math.addExact(totalWins, wins[participant]);
            if (currentWinStreaks[participant] > 0) {
                activeStreaks++;
            }
        }
        if (totalWins != gameCount - tiedGameCount || activeStreaks > 1) {
            throw new IllegalArgumentException("Series wins are inconsistent with game count");
        }

        return new GameSeries(
            names,
            gameCount,
            tiedGameCount,
            wins.clone(),
            currentWinStreaks.clone(),
            longestWinStreaks.clone()
        );
    }

    /** Records final scores and returns every winner index (multiple indices for a tie). */
    public int[] recordGame(int[] finalScores) {
        if (finalScores == null || finalScores.length != participantNames.length) {
            throw new IllegalArgumentException("Final scores must describe every participant");
        }
        int winningScore = -1;
        for (int score : finalScores) {
            if (score < 0) {
                throw new IllegalArgumentException("Final scores cannot be negative");
            }
            winningScore = Math.max(winningScore, score);
        }
        if (winningScore <= 0) {
            throw new IllegalArgumentException("A completed game must contain a captured pair");
        }

        int winnerCount = 0;
        for (int score : finalScores) {
            if (score == winningScore) {
                winnerCount++;
            }
        }
        int[] winnerIndices = new int[winnerCount];
        int winnerOutput = 0;
        for (int participant = 0; participant < finalScores.length; participant++) {
            if (finalScores[participant] == winningScore) {
                winnerIndices[winnerOutput++] = participant;
            }
        }

        gameCount = Math.incrementExact(gameCount);
        if (winnerCount > 1) {
            tiedGameCount = Math.incrementExact(tiedGameCount);
            Arrays.fill(currentWinStreaks, 0);
            return winnerIndices;
        }

        int winner = winnerIndices[0];
        wins[winner] = Math.incrementExact(wins[winner]);
        for (int participant = 0; participant < currentWinStreaks.length; participant++) {
            if (participant == winner) {
                currentWinStreaks[participant] = Math.incrementExact(
                    currentWinStreaks[participant]
                );
                longestWinStreaks[participant] = Math.max(
                    longestWinStreaks[participant],
                    currentWinStreaks[participant]
                );
            } else {
                currentWinStreaks[participant] = 0;
            }
        }
        return winnerIndices;
    }

    public int getParticipantCount() {
        return participantNames.length;
    }

    public String getParticipantName(int participant) {
        checkParticipant(participant);
        return participantNames[participant];
    }

    public int getGameCount() {
        return gameCount;
    }

    public int getTiedGameCount() {
        return tiedGameCount;
    }

    public int getWinCount(int participant) {
        checkParticipant(participant);
        return wins[participant];
    }

    public int getCurrentWinStreak(int participant) {
        checkParticipant(participant);
        return currentWinStreaks[participant];
    }

    public int getLongestWinStreak(int participant) {
        checkParticipant(participant);
        return longestWinStreaks[participant];
    }

    public int getCurrentStreakParticipant() {
        for (int participant = 0; participant < currentWinStreaks.length; participant++) {
            if (currentWinStreaks[participant] > 0) {
                return participant;
            }
        }
        return NO_PARTICIPANT;
    }

    /** Returns every participant tied for the most outright wins. Empty before game one. */
    public int[] copySeriesLeaderIndices() {
        if (gameCount == 0) {
            return new int[0];
        }
        int leadingWins = 0;
        for (int winCount : wins) {
            leadingWins = Math.max(leadingWins, winCount);
        }
        if (leadingWins == 0) {
            return new int[0];
        }
        int count = 0;
        for (int winCount : wins) {
            if (winCount == leadingWins) {
                count++;
            }
        }
        int[] leaders = new int[count];
        int output = 0;
        for (int participant = 0; participant < wins.length; participant++) {
            if (wins[participant] == leadingWins) {
                leaders[output++] = participant;
            }
        }
        return leaders;
    }

    public String[] copyParticipantNames() {
        return participantNames.clone();
    }

    public int[] copyWins() {
        return wins.clone();
    }

    public int[] copyCurrentWinStreaks() {
        return currentWinStreaks.clone();
    }

    public int[] copyLongestWinStreaks() {
        return longestWinStreaks.clone();
    }

    private void checkParticipant(int participant) {
        if (participant < 0 || participant >= participantNames.length) {
            throw new IllegalArgumentException("Series participant is out of range");
        }
    }

    private static String[] validateAndCopyNames(String[] names) {
        if (names == null || names.length < 1
            || names.length > GameHistoryEntry.MAX_PARTICIPANTS) {
            throw new IllegalArgumentException("A series requires 1 to 16 participants");
        }
        String[] result = new String[names.length];
        for (int index = 0; index < names.length; index++) {
            String name = names[index] == null ? "" : names[index].trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Series participant names cannot be blank");
            }
            result[index] = name;
        }
        return result;
    }

    private static void validateArray(int[] values, int expectedSize, String label) {
        if (values == null || values.length != expectedSize) {
            throw new IllegalArgumentException("Series " + label + " have the wrong size");
        }
    }
}
