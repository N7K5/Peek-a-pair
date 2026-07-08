package com.example.flipandfind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pure, ordered player-roster rules used by {@link PlayerProfileStore}. */
public final class PlayerRoster {
    public static final int MIN_PLAYERS = 1;
    public static final int MAX_PLAYERS = 15;
    public static final int DEFAULT_PLAYERS = 2;

    private final ArrayList<PlayerProfile> profiles;
    private long nextId;

    public static PlayerRoster createDefault() {
        ArrayList<PlayerProfile> defaults = new ArrayList<>();
        defaults.add(new PlayerProfile(1L, "", ""));
        defaults.add(new PlayerProfile(2L, "", ""));
        return new PlayerRoster(defaults, 3L);
    }

    static PlayerRoster restore(List<PlayerProfile> savedProfiles, long savedNextId) {
        return new PlayerRoster(savedProfiles, savedNextId);
    }

    private PlayerRoster(List<PlayerProfile> initialProfiles, long initialNextId) {
        if (initialProfiles == null
            || initialProfiles.size() < MIN_PLAYERS
            || initialProfiles.size() > MAX_PLAYERS) {
            throw new IllegalArgumentException("A roster must contain 1 to 15 players");
        }

        profiles = new ArrayList<>(initialProfiles.size());
        Set<Long> seenIds = new HashSet<>();
        long highestId = 0L;
        for (PlayerProfile profile : initialProfiles) {
            if (profile == null || !seenIds.add(profile.getId())) {
                throw new IllegalArgumentException("Player IDs must be unique");
            }
            profiles.add(profile);
            highestId = Math.max(highestId, profile.getId());
        }
        nextId = Math.max(Math.max(1L, initialNextId), highestId + 1L);
    }

    public int size() {
        return profiles.size();
    }

    public List<PlayerProfile> getProfiles() {
        return Collections.unmodifiableList(new ArrayList<>(profiles));
    }

    public PlayerProfile get(int index) {
        return profiles.get(index);
    }

    public PlayerProfile find(long id) {
        int index = indexOf(id);
        return index < 0 ? null : profiles.get(index);
    }

    public int indexOf(long id) {
        for (int index = 0; index < profiles.size(); index++) {
            if (profiles.get(index).getId() == id) {
                return index;
            }
        }
        return -1;
    }

    public String getDisplayNameAt(int index) {
        return get(index).getDisplayName(index + 1);
    }

    /** Ensures at least this many profiles, without ever deleting an existing profile. */
    public void ensureCount(int desiredCount) {
        checkCount(desiredCount);
        while (profiles.size() < desiredCount) {
            addProfile();
        }
    }

    public PlayerProfile addProfile() {
        if (profiles.size() >= MAX_PLAYERS) {
            throw new IllegalStateException("The roster already has 15 players");
        }
        PlayerProfile profile = new PlayerProfile(nextId++, "", "");
        profiles.add(profile);
        return profile;
    }

    /** Removes the requested stable ID, preserving the order of all remaining players. */
    public boolean removeProfile(long id) {
        if (profiles.size() <= MIN_PLAYERS) {
            return false;
        }
        int index = indexOf(id);
        if (index < 0) {
            return false;
        }
        profiles.remove(index);
        return true;
    }

    /**
     * Resizes without discarding custom data. When shrinking, untouched profiles are removed
     * from the end first. Returns false, without changing anything, if a customized player
     * would need to be selected for removal.
     */
    public boolean resizeUsingDefaultProfiles(int desiredCount) {
        checkCount(desiredCount);
        if (desiredCount >= profiles.size()) {
            ensureCount(desiredCount);
            return true;
        }

        int removalCount = profiles.size() - desiredCount;
        ArrayList<Integer> removable = new ArrayList<>(removalCount);
        for (int index = profiles.size() - 1; index >= 0; index--) {
            if (!profiles.get(index).isCustomized()) {
                removable.add(index);
                if (removable.size() == removalCount) {
                    break;
                }
            }
        }
        if (removable.size() < removalCount) {
            return false;
        }
        for (int index : removable) {
            profiles.remove(index);
        }
        return true;
    }

    public boolean setCustomName(long id, String customName) {
        int index = indexOf(id);
        if (index < 0) {
            return false;
        }
        profiles.set(index, profiles.get(index).withCustomName(customName));
        return true;
    }

    public boolean setPhotoPath(long id, String photoPath) {
        int index = indexOf(id);
        if (index < 0) {
            return false;
        }
        profiles.set(index, profiles.get(index).withPhotoPath(photoPath));
        return true;
    }

    public boolean updateProfile(long id, String customName, String photoPath) {
        int index = indexOf(id);
        if (index < 0) {
            return false;
        }
        profiles.set(index, profiles.get(index).withDetails(customName, photoPath));
        return true;
    }

    public boolean resetProfile(long id) {
        int index = indexOf(id);
        if (index < 0) {
            return false;
        }
        profiles.set(index, profiles.get(index).reset());
        return true;
    }

    /** Clears every custom name and photo while retaining roster order and stable IDs. */
    public void resetAllProfiles() {
        for (int index = 0; index < profiles.size(); index++) {
            profiles.set(index, profiles.get(index).reset());
        }
    }

    public boolean isCustomized(long id) {
        PlayerProfile profile = find(id);
        return profile != null && profile.isCustomized();
    }

    public boolean hasAnyCustomProfiles() {
        for (PlayerProfile profile : profiles) {
            if (profile.isCustomized()) {
                return true;
            }
        }
        return false;
    }

    long getNextId() {
        return nextId;
    }

    private static void checkCount(int count) {
        if (count < MIN_PLAYERS || count > MAX_PLAYERS) {
            throw new IllegalArgumentException("Player count must be from 1 to 15");
        }
    }
}
