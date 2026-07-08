package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import org.junit.Test;

public final class CardBackSelectionTest {
    @Test
    public void fixedSelectionUsesOneConcreteStyle() {
        CardBackSelection selection = CardBackSelection.fixed(CardBackStyle.WAVES, 91L);

        assertEquals(CardBackMode.FIXED, selection.getMode());
        assertEquals(91L, selection.getGameSeed());
        assertTrue(selection.isUniform());
        assertEquals(CardBackStyle.WAVES, selection.getUniformStyle());
        assertEquals("waves", selection.getSavedUniformStylePreferenceId());
        for (int cardIndex = 0; cardIndex < 100; cardIndex++) {
            assertEquals(CardBackStyle.WAVES, selection.styleForCard(cardIndex));
        }
    }

    @Test
    public void nullFixedStyleSafelyUsesClassic() {
        CardBackSelection selection = CardBackSelection.fixed(null, 0L);

        assertEquals(CardBackStyle.CLASSIC, selection.getUniformStyle());
        assertEquals(CardBackStyle.CLASSIC, selection.styleForCard(47));
    }

    @Test
    public void randomEachGameStartsClassicAndNeverImmediatelyRepeats() {
        CardBackSession session = new CardBackSession(0x1234ABCDL);
        CardBackStyle previous = null;
        Set<CardBackStyle> concreteStyles = EnumSet.allOf(CardBackStyle.class);

        for (int gameIndex = 0; gameIndex < 100; gameIndex++) {
            CardBackSelection selection = session.beginGame(
                CardBackMode.RANDOM_EACH_GAME,
                CardBackStyle.PIXEL_RAIN,
                gameIndex * 997L
            );
            CardBackStyle current = selection.getUniformStyle();

            assertEquals(CardBackMode.RANDOM_EACH_GAME, selection.getMode());
            assertTrue(selection.isUniform());
            assertTrue(concreteStyles.contains(current));
            assertEquals(current, selection.styleForCard(0));
            assertEquals(current, selection.styleForCard(99));
            if (gameIndex == 0) {
                assertEquals(CardBackStyle.CLASSIC, current);
            } else {
                assertNotEquals(previous, current);
            }
            previous = current;
        }
        assertEquals(100, session.getRandomGamesStarted());
        assertEquals(previous, session.getPreviousRandomGameStyle());
    }

    @Test
    public void randomEachGameSequenceIsDeterministicFromSeeds() {
        CardBackSession first = new CardBackSession(8421L);
        CardBackSession second = new CardBackSession(8421L);

        for (int gameIndex = 0; gameIndex < 60; gameIndex++) {
            long gameSeed = 19L + gameIndex * 101L;
            assertEquals(
                first.beginGame(
                    CardBackMode.RANDOM_EACH_GAME,
                    CardBackStyle.SUNBURST,
                    gameSeed
                ).getUniformStyle(),
                second.beginGame(
                    CardBackMode.RANDOM_EACH_GAME,
                    CardBackStyle.BOTANICAL,
                    gameSeed
                ).getUniformStyle()
            );
        }
    }

    @Test
    public void restoredSessionContinuesTheSameRandomGameSequence() {
        CardBackSession original = new CardBackSession(77112233L);
        for (int index = 0; index < 7; index++) {
            original.beginGame(
                CardBackMode.RANDOM_EACH_GAME,
                CardBackStyle.CLASSIC,
                index + 200L
            );
        }
        CardBackSession restored = CardBackSession.restore(
            original.getSessionSeed(),
            original.getRandomGamesStarted(),
            original.getPreviousRandomGameStylePreferenceId()
        );

        CardBackStyle expected = original.beginGame(
            CardBackMode.RANDOM_EACH_GAME,
            CardBackStyle.CLASSIC,
            999L
        ).getUniformStyle();
        CardBackStyle actual = restored.beginGame(
            CardBackMode.RANDOM_EACH_GAME,
            CardBackStyle.CLASSIC,
            999L
        ).getUniformStyle();

        assertEquals(expected, actual);
        assertEquals(original.getRandomGamesStarted(), restored.getRandomGamesStarted());
    }

    @Test
    public void fixedAndPerCardModesDoNotConsumeRandomGameSequence() {
        CardBackSession session = new CardBackSession(44L);
        session.beginGame(CardBackMode.FIXED, CardBackStyle.AURORA, 1L);
        session.beginGame(CardBackMode.RANDOM_EACH_CARD, CardBackStyle.AURORA, 2L);

        CardBackSelection firstRandomGame = session.beginGame(
            CardBackMode.RANDOM_EACH_GAME,
            CardBackStyle.AURORA,
            3L
        );
        assertEquals(CardBackStyle.CLASSIC, firstRandomGame.getUniformStyle());
        assertEquals(1, session.getRandomGamesStarted());
    }

    @Test
    public void randomEachCardUsesAllConcreteStylesBeforeRepeating() {
        CardBackSelection selection = CardBackSelection.randomEachCard(0xCAFEBABEL);
        Set<CardBackStyle> firstCycle = EnumSet.noneOf(CardBackStyle.class);

        assertEquals(CardBackMode.RANDOM_EACH_CARD, selection.getMode());
        assertFalse(selection.isUniform());
        assertEquals("classic", selection.getSavedUniformStylePreferenceId());
        for (int cardIndex = 0; cardIndex < CardBackStyle.values().length; cardIndex++) {
            CardBackStyle style = selection.styleForCard(cardIndex);
            assertTrue(firstCycle.add(style));
            assertEquals(
                style,
                selection.styleForCard(cardIndex + CardBackStyle.values().length)
            );
        }
        assertEquals(EnumSet.allOf(CardBackStyle.class), firstCycle);
    }

    @Test
    public void randomEachCardIsStableFromItsGameSeed() {
        CardBackSelection first = CardBackSelection.randomEachCard(123456789L);
        CardBackSelection sameSeed = CardBackSelection.randomEachCard(123456789L);
        CardBackSelection otherSeed = CardBackSelection.randomEachCard(987654321L);
        boolean foundDifference = false;

        for (int cardIndex = 0; cardIndex < 100; cardIndex++) {
            assertEquals(first.styleForCard(cardIndex), first.styleForCard(cardIndex));
            assertEquals(first.styleForCard(cardIndex), sameSeed.styleForCard(cardIndex));
            if (first.styleForCard(cardIndex) != otherSeed.styleForCard(cardIndex)) {
                foundDifference = true;
            }
        }
        assertTrue(foundDifference);
    }

    @Test
    public void savedActiveSelectionsRestoreExactly() {
        CardBackSelection uniform = CardBackSelection.restore(
            "random_each_game",
            "moon_ripples",
            90210L
        );
        assertEquals(CardBackMode.RANDOM_EACH_GAME, uniform.getMode());
        assertEquals(CardBackStyle.MOON_RIPPLES, uniform.getUniformStyle());

        CardBackSelection perCard = CardBackSelection.randomEachCard(78123L);
        CardBackSelection restoredPerCard = CardBackSelection.restore(
            perCard.getMode().getPreferenceId(),
            perCard.getSavedUniformStylePreferenceId(),
            perCard.getGameSeed()
        );
        for (int cardIndex = 0; cardIndex < 100; cardIndex++) {
            assertEquals(perCard.styleForCard(cardIndex), restoredPerCard.styleForCard(cardIndex));
        }
    }

    @Test
    public void oldOrUnknownModePreferenceRestoresAsFixed() {
        CardBackSelection missingMode = CardBackSelection.restore(null, "prism", 4L);
        CardBackSelection unknownMode = CardBackSelection.restore("future", "weave", 5L);

        assertEquals(CardBackMode.FIXED, missingMode.getMode());
        assertEquals(CardBackStyle.PRISM, missingMode.getUniformStyle());
        assertEquals(CardBackMode.FIXED, unknownMode.getMode());
        assertEquals(CardBackStyle.WEAVE, unknownMode.getUniformStyle());
    }

    @Test(expected = IllegalStateException.class)
    public void randomEachCardHasNoUniformStyle() {
        CardBackSelection.randomEachCard(1L).getUniformStyle();
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeCardIndexIsRejected() {
        CardBackSelection.randomEachCard(1L).styleForCard(-1);
    }
}
