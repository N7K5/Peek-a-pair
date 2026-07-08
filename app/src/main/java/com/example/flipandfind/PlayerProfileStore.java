package com.example.flipandfind;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Persistent owner of the ordered human-player roster. */
public final class PlayerProfileStore {
    public static final String PREFERENCES_NAME = "flip_and_find_player_profiles";

    private static final String KEY_COUNT = "roster.count";
    private static final String KEY_NEXT_ID = "roster.next_id";
    private static final String SLOT_PREFIX = "roster.slot.";
    private static final String PROFILE_PREFIX = "profile.";

    private final SharedPreferences preferences;
    private PlayerRoster roster;

    public PlayerProfileStore(Context context) {
        this(context.getApplicationContext().getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        ));
    }

    /** Constructor useful for a custom preferences scope or instrumentation tests. */
    public PlayerProfileStore(SharedPreferences preferences) {
        if (preferences == null) {
            throw new IllegalArgumentException("SharedPreferences are required");
        }
        this.preferences = preferences;
        roster = load();
        // Also repairs malformed or partially-written preference data on construction.
        persist();
    }

    public synchronized int size() {
        return roster.size();
    }

    public synchronized List<PlayerProfile> getProfiles() {
        return roster.getProfiles();
    }

    public synchronized PlayerProfile get(int index) {
        return roster.get(index);
    }

    public synchronized PlayerProfile find(long id) {
        return roster.find(id);
    }

    public synchronized String getDisplayNameAt(int index) {
        return roster.getDisplayNameAt(index);
    }

    /** Adds default profiles until the requested count exists; never removes profiles. */
    public synchronized void ensureCount(int desiredCount) {
        int previousSize = roster.size();
        roster.ensureCount(desiredCount);
        if (roster.size() != previousSize) {
            persist();
        }
    }

    public synchronized PlayerProfile addProfile() {
        PlayerProfile added = roster.addProfile();
        persist();
        return added;
    }

    /** Exact removal by stable ID; returns false for unknown IDs or the final player. */
    public synchronized boolean removeProfile(long id) {
        if (!roster.removeProfile(id)) {
            return false;
        }
        persist();
        return true;
    }

    /** See {@link PlayerRoster#resizeUsingDefaultProfiles(int)}. */
    public synchronized boolean resizeUsingDefaultProfiles(int desiredCount) {
        int previousSize = roster.size();
        boolean resized = roster.resizeUsingDefaultProfiles(desiredCount);
        if (resized && roster.size() != previousSize) {
            persist();
        }
        return resized;
    }

    /** A blank or whitespace-only name restores the generated positional name. */
    public synchronized boolean setCustomName(long id, String customName) {
        if (!roster.setCustomName(id, customName)) {
            return false;
        }
        persist();
        return true;
    }

    /** A blank path removes the saved photo reference. */
    public synchronized boolean setPhotoPath(long id, String internalPhotoPath) {
        if (!roster.setPhotoPath(id, internalPhotoPath)) {
            return false;
        }
        persist();
        return true;
    }

    public synchronized boolean updateProfile(
        long id,
        String customName,
        String internalPhotoPath
    ) {
        if (!roster.updateProfile(id, customName, internalPhotoPath)) {
            return false;
        }
        persist();
        return true;
    }

    public synchronized boolean resetProfile(long id) {
        if (!roster.resetProfile(id)) {
            return false;
        }
        persist();
        return true;
    }

    /** Clears all custom names/photos while retaining the current roster and stable IDs. */
    public synchronized void resetAllProfiles() {
        roster.resetAllProfiles();
        persist();
    }

    public synchronized boolean isCustomized(long id) {
        return roster.isCustomized(id);
    }

    public synchronized boolean hasAnyCustomProfiles() {
        return roster.hasAnyCustomProfiles();
    }

    private PlayerRoster load() {
        if (!preferences.contains(KEY_COUNT)) {
            return PlayerRoster.createDefault();
        }

        int count = preferences.getInt(KEY_COUNT, PlayerRoster.DEFAULT_PLAYERS);
        if (count < PlayerRoster.MIN_PLAYERS || count > PlayerRoster.MAX_PLAYERS) {
            return PlayerRoster.createDefault();
        }

        ArrayList<PlayerProfile> profiles = new ArrayList<>(count);
        Set<Long> ids = new HashSet<>();
        for (int index = 0; index < count; index++) {
            long id = preferences.getLong(SLOT_PREFIX + index, -1L);
            if (id <= 0L || !ids.add(id)) {
                return PlayerRoster.createDefault();
            }
            String prefix = PROFILE_PREFIX + id + ".";
            profiles.add(new PlayerProfile(
                id,
                preferences.getString(prefix + "name", ""),
                preferences.getString(prefix + "photo", "")
            ));
        }

        try {
            return PlayerRoster.restore(
                profiles,
                preferences.getLong(KEY_NEXT_ID, 1L)
            );
        } catch (IllegalArgumentException ignored) {
            return PlayerRoster.createDefault();
        }
    }

    private void persist() {
        SharedPreferences.Editor editor = preferences.edit().clear();
        List<PlayerProfile> profiles = roster.getProfiles();
        editor.putInt(KEY_COUNT, profiles.size());
        editor.putLong(KEY_NEXT_ID, roster.getNextId());
        for (int index = 0; index < profiles.size(); index++) {
            PlayerProfile profile = profiles.get(index);
            editor.putLong(SLOT_PREFIX + index, profile.getId());
            String prefix = PROFILE_PREFIX + profile.getId() + ".";
            editor.putString(prefix + "name", profile.getCustomName());
            editor.putString(prefix + "photo", profile.getPhotoPath());
        }
        editor.apply();
    }
}
