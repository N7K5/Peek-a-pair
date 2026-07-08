package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import org.junit.Test;

public final class TabletopSessionTest {
    @Test
    public void randomEachGameStartsStaticAndNeverImmediatelyRepeats() {
        TabletopSession session = new TabletopSession(0x1234ABCDL);
        TabletopMode previous = null;
        Set<TabletopMode> concreteModes = EnumSet.noneOf(TabletopMode.class);
        for (TabletopMode mode : TabletopMode.concreteModesCopy()) {
            concreteModes.add(mode);
        }

        for (int gameIndex = 0; gameIndex < 100; gameIndex++) {
            TabletopMode current = session.beginGame(
                TabletopMode.RANDOM_EACH_GAME,
                gameIndex * 997L
            );
            assertTrue(current.isConcrete());
            assertTrue(concreteModes.contains(current));
            if (gameIndex == 0) {
                assertEquals(TabletopMode.STATIC_THEME, current);
            } else {
                assertNotEquals(previous, current);
            }
            previous = current;
        }
        assertEquals(100, session.getRandomGamesStarted());
        assertEquals(previous, session.getPreviousRandomGameMode());
    }

    @Test
    public void randomSequenceIsDeterministicFromSeeds() {
        TabletopSession first = new TabletopSession(8421L);
        TabletopSession second = new TabletopSession(8421L);

        for (int gameIndex = 0; gameIndex < 60; gameIndex++) {
            long gameSeed = 19L + gameIndex * 101L;
            assertEquals(
                first.beginGame(TabletopMode.RANDOM_EACH_GAME, gameSeed),
                second.beginGame(TabletopMode.RANDOM_EACH_GAME, gameSeed)
            );
        }
    }

    @Test
    public void restoredSessionContinuesTheSameSequence() {
        TabletopSession original = new TabletopSession(77112233L);
        for (int index = 0; index < 7; index++) {
            original.beginGame(TabletopMode.RANDOM_EACH_GAME, index + 200L);
        }
        TabletopSession restored = TabletopSession.restore(
            original.getSessionSeed(),
            original.getRandomGamesStarted(),
            original.getPreviousRandomGameModePreferenceId()
        );

        TabletopMode expected = original.beginGame(TabletopMode.RANDOM_EACH_GAME, 999L);
        TabletopMode actual = restored.beginGame(TabletopMode.RANDOM_EACH_GAME, 999L);

        assertEquals(expected, actual);
        assertEquals(original.getRandomGamesStarted(), restored.getRandomGamesStarted());
        assertEquals(
            original.getPreviousRandomGameMode(),
            restored.getPreviousRandomGameMode()
        );
    }

    @Test
    public void concreteModesDoNotConsumeRandomSequence() {
        TabletopSession session = new TabletopSession(44L);
        assertEquals(TabletopMode.WATER, session.beginGame(TabletopMode.WATER, 1L));
        assertEquals(TabletopMode.STATIC_THEME, session.beginGame(null, 2L));

        assertEquals(0, session.getRandomGamesStarted());
        assertEquals(
            TabletopMode.STATIC_THEME,
            session.beginGame(TabletopMode.RANDOM_EACH_GAME, 3L)
        );
        assertEquals(1, session.getRandomGamesStarted());
    }

    @Test
    public void invalidRestoredStateUsesSafeConcreteFallbacks() {
        TabletopSession negativeCount = TabletopSession.restore(
            9L,
            -3,
            TabletopMode.NEON.getPreferenceId()
        );
        assertEquals(0, negativeCount.getRandomGamesStarted());
        assertEquals(TabletopMode.STATIC_THEME, negativeCount.getPreviousRandomGameMode());

        TabletopSession randomPrevious = TabletopSession.restore(
            9L,
            3,
            TabletopMode.RANDOM_EACH_GAME.getPreferenceId()
        );
        assertTrue(randomPrevious.getPreviousRandomGameMode().isConcrete());
        assertEquals(TabletopMode.STATIC_THEME, randomPrevious.getPreviousRandomGameMode());

        TabletopSession unknownPrevious = TabletopSession.restore(9L, 3, "future_mode");
        assertEquals(TabletopMode.STATIC_THEME, unknownPrevious.getPreviousRandomGameMode());
        assertFalse(unknownPrevious.getPreviousRandomGameMode().isRandomEachGame());
    }
}
