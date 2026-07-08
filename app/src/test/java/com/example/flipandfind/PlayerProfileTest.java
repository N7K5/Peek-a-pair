package com.example.flipandfind;

import static org.junit.Assert.assertEquals;

import java.util.Locale;
import org.junit.Test;

public final class PlayerProfileTest {
    @Test
    public void defaultProfileUsesLetteredNamesAndBadgesFromAToO() {
        PlayerProfile profile = new PlayerProfile(1L, "", "");

        assertEquals("Player A", profile.getDisplayName(1));
        assertEquals("A", profile.getAvatarLabel(1));
        assertEquals("A", profile.getScoreBadgeLabel(1));
        assertEquals("Player O", profile.getDisplayName(15));
        assertEquals("O", profile.getAvatarLabel(15));
        assertEquals("O", profile.getScoreBadgeLabel(15));
        assertEquals("Player O", PlayerProfile.getDefaultDisplayName(15));
    }

    @Test
    public void shortCustomNameIsUppercasedWithoutPadding() {
        PlayerProfile profile = new PlayerProfile(1L, "Al", "");

        assertEquals("AL", profile.getScoreBadgeLabel(7));
    }

    @Test
    public void longCustomNameIsLimitedToThreeCodePoints() {
        PlayerProfile profile = new PlayerProfile(1L, "Charlie", "");

        assertEquals("CHA", profile.getScoreBadgeLabel(1));
    }

    @Test
    public void unicodeNameDoesNotSplitSupplementaryCodePoint() {
        PlayerProfile profile = new PlayerProfile(1L, "😀élise", "");

        String label = profile.getScoreBadgeLabel(1);
        assertEquals("😀ÉL", label);
        assertEquals(3, label.codePointCount(0, label.length()));
    }

    @Test
    public void casingUsesLocaleRootInsteadOfDeviceLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            PlayerProfile profile = new PlayerProfile(1L, "ind", "");

            assertEquals("IND", profile.getScoreBadgeLabel(1));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void playerNumberMustBeOneBased() {
        new PlayerProfile(1L, "Ada", "").getScoreBadgeLabel(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void defaultLettersStopAtPlayerO() {
        PlayerProfile.getDefaultLabel(16);
    }
}
