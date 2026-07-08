package com.example.flipandfind;

import android.content.Context;
import android.content.SharedPreferences;

/** Durable SharedPreferences owner for the capped game-history archive. */
public final class GameHistoryStore {
    public static final String PREFERENCES_NAME = "peek_a_pair_game_history";
    public static final int DEFAULT_MAX_ENTRIES = 200;

    private static final String KEY_ARCHIVE = "history.archive.v1";

    private final SharedPreferences preferences;
    private final int maxEntries;
    private GameHistory history;

    public GameHistoryStore(Context context) {
        this(
            context.getApplicationContext().getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            ),
            DEFAULT_MAX_ENTRIES
        );
    }

    /** Constructor useful for a custom preference scope or instrumentation tests. */
    public GameHistoryStore(SharedPreferences preferences) {
        this(preferences, DEFAULT_MAX_ENTRIES);
    }

    public GameHistoryStore(SharedPreferences preferences, int maxEntries) {
        if (preferences == null) {
            throw new IllegalArgumentException("SharedPreferences are required");
        }
        if (maxEntries < 1) {
            throw new IllegalArgumentException("History must retain at least one game");
        }
        this.preferences = preferences;
        this.maxEntries = maxEntries;
        history = load();
    }

    /** Returns a detached snapshot; mutating it cannot bypass persistence. */
    public synchronized GameHistory getHistory() {
        return GameHistory.restore(
            maxEntries,
            history.getTotalGamesPlayed(),
            history.getTotalActiveDurationMillis(),
            history.getTotalPairsPlayed(),
            history.getEntries()
        );
    }

    public synchronized void recordGame(GameHistoryEntry completedGame) {
        history.record(completedGame);
        persist();
    }

    /** Clears retained games and every lifetime history total. */
    public synchronized void reset() {
        history = GameHistory.empty(maxEntries);
        persist();
    }

    private GameHistory load() {
        try {
            return GameHistoryCodec.decode(preferences.getString(KEY_ARCHIVE, ""), maxEntries);
        } catch (IllegalArgumentException | ArithmeticException | ClassCastException ignored) {
            GameHistory repaired = GameHistory.empty(maxEntries);
            preferences.edit().putString(KEY_ARCHIVE, GameHistoryCodec.encode(repaired)).apply();
            return repaired;
        }
    }

    private void persist() {
        preferences.edit().putString(KEY_ARCHIVE, GameHistoryCodec.encode(history)).apply();
    }
}
