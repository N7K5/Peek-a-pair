package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public final class PlayerRosterTest {
    @Test
    public void defaultRosterHasTwoStablePlayersAndPositionalNames() {
        PlayerRoster roster = PlayerRoster.createDefault();

        assertEquals(2, roster.size());
        assertEquals("Player A", roster.getDisplayNameAt(0));
        assertEquals("Player B", roster.getDisplayNameAt(1));
        assertNotEquals(roster.get(0).getId(), roster.get(1).getId());
    }

    @Test
    public void customNameSurvivesPositionChangeWhileDefaultNameFollowsPosition() {
        PlayerRoster roster = PlayerRoster.createDefault();
        roster.ensureCount(3);
        long firstId = roster.get(0).getId();
        long secondId = roster.get(1).getId();
        roster.setCustomName(secondId, "Maya");

        assertTrue(roster.removeProfile(firstId));

        assertEquals(secondId, roster.get(0).getId());
        assertEquals("Maya", roster.getDisplayNameAt(0));
        assertEquals("Player B", roster.getDisplayNameAt(1));
    }

    @Test
    public void newIdsAreNeverReusedAfterRemoval() {
        PlayerRoster roster = PlayerRoster.createDefault();
        PlayerProfile third = roster.addProfile();
        assertTrue(roster.removeProfile(third.getId()));

        PlayerProfile replacement = roster.addProfile();

        assertTrue(replacement.getId() > third.getId());
    }

    @Test
    public void safeResizeDoesNotDiscardCustomizedProfiles() {
        PlayerRoster roster = PlayerRoster.createDefault();
        roster.setCustomName(roster.get(0).getId(), "A");
        roster.setPhotoPath(roster.get(1).getId(), "/private/player.jpg");

        assertFalse(roster.resizeUsingDefaultProfiles(1));
        assertEquals(2, roster.size());
        assertTrue(roster.get(0).isCustomized());
        assertTrue(roster.get(1).isCustomized());
    }

    @Test
    public void safeResizePrefersUntouchedPlayersFromTheEnd() {
        PlayerRoster roster = PlayerRoster.createDefault();
        roster.ensureCount(4);
        long customId = roster.get(3).getId();
        roster.setCustomName(customId, "Last");

        assertTrue(roster.resizeUsingDefaultProfiles(2));

        assertEquals(2, roster.size());
        assertEquals(customId, roster.get(1).getId());
        assertEquals("Last", roster.getDisplayNameAt(1));
    }

    @Test
    public void resetOneAndResetAllKeepIdsAndRosterOrder() {
        PlayerRoster roster = PlayerRoster.createDefault();
        roster.ensureCount(3);
        List<Long> ids = ids(roster);
        roster.updateProfile(roster.get(0).getId(), "Ana", "/one.jpg");
        roster.updateProfile(roster.get(1).getId(), "Ben", "/two.jpg");

        assertTrue(roster.resetProfile(roster.get(0).getId()));
        assertFalse(roster.get(0).isCustomized());
        assertTrue(roster.get(1).isCustomized());

        roster.resetAllProfiles();
        assertFalse(roster.hasAnyCustomProfiles());
        assertEquals(ids, ids(roster));
        assertEquals(3, roster.size());
    }

    @Test
    public void restoredRosterContinuesAfterHighestExistingId() {
        ArrayList<PlayerProfile> saved = new ArrayList<>();
        saved.add(new PlayerProfile(12L, "One", ""));
        saved.add(new PlayerProfile(40L, "", "/photo.jpg"));
        PlayerRoster roster = PlayerRoster.restore(saved, 7L);

        PlayerProfile added = roster.addProfile();

        assertEquals(41L, added.getId());
    }

    @Test
    public void rosterSupportsFifteenStablePlayers() {
        PlayerRoster roster = PlayerRoster.createDefault();

        roster.ensureCount(15);

        assertEquals(15, roster.size());
        assertEquals("Player O", roster.getDisplayNameAt(14));
        assertEquals(15, ids(roster).stream().distinct().count());
    }

    @Test
    public void restoringDefaultCountAfterSoloPreservesCustomizedPlayer() {
        PlayerRoster roster = PlayerRoster.createDefault();
        long firstId = roster.get(0).getId();
        long secondId = roster.get(1).getId();
        roster.updateProfile(firstId, "Asha", "/private/asha.jpg");
        assertTrue(roster.removeProfile(secondId));

        roster.ensureCount(PlayerRoster.DEFAULT_PLAYERS);

        assertEquals(2, roster.size());
        assertEquals(firstId, roster.get(0).getId());
        assertEquals("Asha", roster.get(0).getCustomName());
        assertEquals("/private/asha.jpg", roster.get(0).getPhotoPath());
        assertFalse(roster.get(1).isCustomized());
        assertNotEquals(secondId, roster.get(1).getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void countAboveMaximumIsRejected() {
        PlayerRoster.createDefault().ensureCount(16);
    }

    private static List<Long> ids(PlayerRoster roster) {
        ArrayList<Long> ids = new ArrayList<>();
        for (PlayerProfile profile : roster.getProfiles()) {
            ids.add(profile.getId());
        }
        return ids;
    }
}
