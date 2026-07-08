package com.example.flipandfind;

import java.util.Locale;

/** Immutable player identity data. Default names are derived from roster position. */
public final class PlayerProfile {
    private static final int MAX_DEFAULT_POSITION = 15;

    private final long id;
    private final String customName;
    private final String photoPath;

    PlayerProfile(long id, String customName, String photoPath) {
        if (id <= 0L) {
            throw new IllegalArgumentException("Player IDs must be positive");
        }
        this.id = id;
        this.customName = clean(customName);
        this.photoPath = clean(photoPath);
    }

    public long getId() {
        return id;
    }

    /** Returns an empty string when the player is using their generated default name. */
    public String getCustomName() {
        return customName;
    }

    /** Returns an empty string when the player has no saved photo. */
    public String getPhotoPath() {
        return photoPath;
    }

    public boolean hasCustomName() {
        return !customName.isEmpty();
    }

    public boolean hasPhoto() {
        return !photoPath.isEmpty();
    }

    public boolean isCustomized() {
        return hasCustomName() || hasPhoto();
    }

    /**
     * Returns the custom name, or a generated name based on the current one-based roster
     * position. The generated name intentionally is not persisted, so it follows reordering.
     */
    public String getDisplayName(int oneBasedPosition) {
        validatePosition(oneBasedPosition);
        return hasCustomName()
            ? customName
            : "Player " + getDefaultLabel(oneBasedPosition);
    }

    /** Returns up to two initials for custom names, or the player letter for defaults. */
    public String getAvatarLabel(int oneBasedPosition) {
        validatePosition(oneBasedPosition);
        if (!hasCustomName()) {
            return getDefaultLabel(oneBasedPosition);
        }

        String[] words = customName.split("\\s+");
        String first = firstCodePoint(words[0]);
        String second = words.length > 1 ? firstCodePoint(words[words.length - 1]) : "";
        return (first + second).toUpperCase(Locale.getDefault());
    }

    /**
     * Returns a compact, locale-neutral score label. Custom names are uppercased first and
     * limited to three Unicode code points; default profiles use their supplied player letter.
     */
    public String getScoreBadgeLabel(int oneBasedPosition) {
        validatePosition(oneBasedPosition);
        if (!hasCustomName()) {
            return getDefaultLabel(oneBasedPosition);
        }

        String uppercaseName = customName.toUpperCase(Locale.ROOT);
        int length = Math.min(3, uppercaseName.codePointCount(0, uppercaseName.length()));
        int end = uppercaseName.offsetByCodePoints(0, length);
        return uppercaseName.substring(0, end);
    }

    /** Returns the stable positional fallback label A through O. */
    public static String getDefaultLabel(int oneBasedPosition) {
        validatePosition(oneBasedPosition);
        return String.valueOf((char) ('A' + oneBasedPosition - 1));
    }

    /** Returns the generated positional display name, from Player A through Player O. */
    public static String getDefaultDisplayName(int oneBasedPosition) {
        return "Player " + getDefaultLabel(oneBasedPosition);
    }

    PlayerProfile withCustomName(String name) {
        return new PlayerProfile(id, name, photoPath);
    }

    PlayerProfile withPhotoPath(String path) {
        return new PlayerProfile(id, customName, path);
    }

    PlayerProfile withDetails(String name, String path) {
        return new PlayerProfile(id, name, path);
    }

    PlayerProfile reset() {
        return new PlayerProfile(id, "", "");
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String firstCodePoint(String value) {
        if (value.isEmpty()) {
            return "";
        }
        int end = value.offsetByCodePoints(0, 1);
        return value.substring(0, end);
    }

    private static void validatePosition(int oneBasedPosition) {
        if (oneBasedPosition < 1 || oneBasedPosition > MAX_DEFAULT_POSITION) {
            throw new IllegalArgumentException("Roster positions must be from 1 to 15");
        }
    }
}
