package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public final class CardBackStyleTest {
    @Test
    public void fifteenStableStylesRoundTrip() {
        assertEquals(15, CardBackStyle.values().length);
        Set<String> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (CardBackStyle style : CardBackStyle.values()) {
            assertTrue(ids.add(style.getPreferenceId()));
            assertTrue(names.add(style.getDisplayName()));
            assertEquals(style, CardBackStyle.fromPreference(style.getPreferenceId()));
        }
    }

    @Test
    public void persistedIdsNamesAndMotionKindsStayBackwardCompatible() {
        assertStyle(CardBackStyle.CLASSIC, "classic", "Classic", CardBackStyle.MotionKind.NONE);
        assertStyle(CardBackStyle.CONSTELLATION, "constellation", "Constellation", CardBackStyle.MotionKind.NONE);
        assertStyle(CardBackStyle.SUNBURST, "sunburst", "Sunburst", CardBackStyle.MotionKind.NONE);
        assertStyle(CardBackStyle.WAVES, "waves", "Waves", CardBackStyle.MotionKind.NONE);
        assertStyle(CardBackStyle.HARLEQUIN, "harlequin", "Harlequin", CardBackStyle.MotionKind.NONE);
        assertStyle(CardBackStyle.ORBITS, "orbits", "Orbits", CardBackStyle.MotionKind.ORBIT);
        assertStyle(CardBackStyle.PRISM, "prism", "Prism", CardBackStyle.MotionKind.NONE);
        assertStyle(CardBackStyle.BOTANICAL, "botanical", "Botanical", CardBackStyle.MotionKind.NONE);
        assertStyle(CardBackStyle.WEAVE, "weave", "Weave", CardBackStyle.MotionKind.NONE);
        assertStyle(CardBackStyle.AURORA, "aurora", "Aurora", CardBackStyle.MotionKind.AURORA_DRIFT);
        assertStyle(CardBackStyle.FIREFLIES, "fireflies", "Fireflies", CardBackStyle.MotionKind.FIREFLY_TWINKLE);
        assertStyle(CardBackStyle.KALEIDO, "kaleido", "Kaleido", CardBackStyle.MotionKind.KALEIDO_BREATHE);
        assertStyle(CardBackStyle.COMET_TRAILS, "comet_trails", "Comet Trails", CardBackStyle.MotionKind.COMET_GLIDE);
        assertStyle(CardBackStyle.MOON_RIPPLES, "moon_ripples", "Moon Ripples", CardBackStyle.MotionKind.RIPPLE_EXPAND);
        assertStyle(CardBackStyle.PIXEL_RAIN, "pixel_rain", "Pixel Rain", CardBackStyle.MotionKind.PIXEL_FALL);
    }

    @Test
    public void motionStylesAreExplicitAndLimitedToTheAnimatedDesigns() {
        Set<CardBackStyle> animated = EnumSet.noneOf(CardBackStyle.class);
        for (CardBackStyle style : CardBackStyle.values()) {
            assertEquals(
                style.getMotionKind() != CardBackStyle.MotionKind.NONE,
                style.isAnimated()
            );
            if (style.isAnimated()) {
                animated.add(style);
            }
        }
        assertEquals(
            EnumSet.of(
                CardBackStyle.ORBITS,
                CardBackStyle.AURORA,
                CardBackStyle.FIREFLIES,
                CardBackStyle.KALEIDO,
                CardBackStyle.COMET_TRAILS,
                CardBackStyle.MOON_RIPPLES,
                CardBackStyle.PIXEL_RAIN
            ),
            animated
        );
    }

    @Test
    public void missingAndUnknownPreferencesUseClassic() {
        assertEquals(CardBackStyle.CLASSIC, CardBackStyle.fromPreference(null));
        assertEquals(CardBackStyle.CLASSIC, CardBackStyle.fromPreference(""));
        assertEquals(CardBackStyle.CLASSIC, CardBackStyle.fromPreference("future_style"));
    }

    @Test
    public void everyStyleHasDistinctVisiblePaletteParts() {
        Set<Integer> fills = new HashSet<>();
        for (CardBackStyle style : CardBackStyle.values()) {
            assertTrue(fills.add(style.getFillColor()));
            assertNotEquals(style.getFillColor(), style.getBorderColor());
            assertNotEquals(style.getFillColor(), style.getPatternColor());
            assertNotEquals(style.getPatternColor(), style.getAccentColor());
            assertEquals(0xFF, style.getFillColor() >>> 24);
            assertEquals(0xFF, style.getBorderColor() >>> 24);
            assertEquals(0xFF, style.getPatternColor() >>> 24);
            assertEquals(0xFF, style.getAccentColor() >>> 24);
        }
    }

    private static void assertStyle(
        CardBackStyle style,
        String preferenceId,
        String displayName,
        CardBackStyle.MotionKind motionKind
    ) {
        assertEquals(preferenceId, style.getPreferenceId());
        assertEquals(displayName, style.getDisplayName());
        assertEquals(motionKind, style.getMotionKind());
        assertEquals(style, CardBackStyle.fromPreference(preferenceId));
    }
}
