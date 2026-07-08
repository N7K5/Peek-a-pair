package com.example.flipandfind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure history archive with newest-first retained games and lifetime totals. */
public final class GameHistory {
    private final int maxEntries;
    private final ArrayList<GameHistoryEntry> entries;
    private long totalGamesPlayed;
    private long totalActiveDurationMillis;
    private long totalPairsPlayed;

    public static GameHistory empty(int maxEntries) {
        return new GameHistory(maxEntries, 0L, 0L, 0L, Collections.emptyList());
    }

    /** Restores validated lifetime totals and newest-first retained entries. */
    public static GameHistory restore(
        int maxEntries,
        long totalGamesPlayed,
        long totalActiveDurationMillis,
        long totalPairsPlayed,
        List<GameHistoryEntry> newestFirstEntries
    ) {
        return new GameHistory(
            maxEntries,
            totalGamesPlayed,
            totalActiveDurationMillis,
            totalPairsPlayed,
            newestFirstEntries
        );
    }

    private GameHistory(
        int maxEntries,
        long totalGamesPlayed,
        long totalActiveDurationMillis,
        long totalPairsPlayed,
        List<GameHistoryEntry> newestFirstEntries
    ) {
        if (maxEntries < 1) {
            throw new IllegalArgumentException("History must retain at least one game");
        }
        if (totalGamesPlayed < 0L || totalActiveDurationMillis < 0L || totalPairsPlayed < 0L) {
            throw new IllegalArgumentException("History totals cannot be negative");
        }
        if (newestFirstEntries == null) {
            throw new IllegalArgumentException("History entries are required");
        }

        long retainedDuration = 0L;
        long retainedPairs = 0L;
        for (GameHistoryEntry entry : newestFirstEntries) {
            if (entry == null) {
                throw new IllegalArgumentException("History entries cannot be null");
            }
            retainedDuration = Math.addExact(retainedDuration, entry.getActiveDurationMillis());
            retainedPairs = Math.addExact(retainedPairs, entry.getPairCount());
        }
        if (totalGamesPlayed < newestFirstEntries.size()
            || totalActiveDurationMillis < retainedDuration
            || totalPairsPlayed < retainedPairs) {
            throw new IllegalArgumentException("Lifetime totals cannot be below retained totals");
        }

        this.maxEntries = maxEntries;
        this.totalGamesPlayed = totalGamesPlayed;
        this.totalActiveDurationMillis = totalActiveDurationMillis;
        this.totalPairsPlayed = totalPairsPlayed;
        entries = new ArrayList<>(Math.min(maxEntries, newestFirstEntries.size()));
        for (int index = 0; index < newestFirstEntries.size() && index < maxEntries; index++) {
            entries.add(newestFirstEntries.get(index));
        }
    }

    /** Adds a completed game to the front while preserving lifetime totals beyond the cap. */
    public void record(GameHistoryEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("A completed game is required");
        }
        totalGamesPlayed = Math.addExact(totalGamesPlayed, 1L);
        totalActiveDurationMillis = Math.addExact(
            totalActiveDurationMillis,
            entry.getActiveDurationMillis()
        );
        totalPairsPlayed = Math.addExact(totalPairsPlayed, entry.getPairCount());
        entries.add(0, entry);
        if (entries.size() > maxEntries) {
            entries.remove(entries.size() - 1);
        }
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public int getRetainedGameCount() {
        return entries.size();
    }

    public long getTotalGamesPlayed() {
        return totalGamesPlayed;
    }

    public long getTotalActiveDurationMillis() {
        return totalActiveDurationMillis;
    }

    public long getTotalPairsPlayed() {
        return totalPairsPlayed;
    }

    public long getAverageActiveDurationMillis() {
        return totalGamesPlayed == 0L ? 0L : totalActiveDurationMillis / totalGamesPlayed;
    }

    public List<GameHistoryEntry> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public GameHistoryEntry getEntry(int newestFirstIndex) {
        return entries.get(newestFirstIndex);
    }

    /** Number of retained tied games; lifetime tie totals are not inferred past the cap. */
    public int getRetainedTieCount() {
        int ties = 0;
        for (GameHistoryEntry entry : entries) {
            if (entry.isTie()) {
                ties++;
            }
        }
        return ties;
    }

    /**
     * Winner credits for retained games. Every named co-winner receives one credit for a tie.
     * Linked insertion order follows first appearance in newest-first history.
     */
    public Map<String, Integer> getRetainedWinnerCounts() {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        for (GameHistoryEntry entry : entries) {
            for (String winner : entry.getWinnerNames()) {
                Integer current = counts.get(winner);
                counts.put(winner, (current == null ? 0 : current) + 1);
            }
        }
        return Collections.unmodifiableMap(counts);
    }
}
