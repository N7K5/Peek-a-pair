package com.example.flipandfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public final class CardBackGeometryTest {
    @Test
    public void everySupportedCardHasStableDistinctPrismAndWaveGeometry() {
        Set<String> prismFingerprints = new HashSet<>();
        Set<String> waveFingerprints = new HashSet<>();
        Set<String> botanicalFingerprints = new HashSet<>();
        Set<String> minimumSizePrismFingerprints = new HashSet<>();
        Set<String> minimumSizeWaveFingerprints = new HashSet<>();
        Set<String> minimumSizeBotanicalFingerprints = new HashSet<>();
        for (int cardNumber = 1; cardNumber <= 102; cardNumber++) {
            CardBackGeometry first = CardBackGeometry.forCard(cardNumber);
            CardBackGeometry second = CardBackGeometry.forCard(cardNumber);
            assertEquals(prismFingerprint(first), prismFingerprint(second));
            assertEquals(waveFingerprint(first), waveFingerprint(second));
            assertEquals(botanicalFingerprint(first), botanicalFingerprint(second));
            assertTrue(prismFingerprints.add(prismFingerprint(first)));
            assertTrue(waveFingerprints.add(waveFingerprint(first)));
            assertTrue(botanicalFingerprints.add(botanicalFingerprint(first)));
            assertTrue(minimumSizePrismFingerprints.add(quantizedPrismFingerprint(first, 12f)));
            assertTrue(minimumSizeWaveFingerprints.add(quantizedWaveFingerprint(first, 12f)));
            assertTrue(
                minimumSizeBotanicalFingerprints.add(
                    quantizedBotanicalFingerprint(first, 12f)
                )
            );
        }
        assertEquals(102, prismFingerprints.size());
        assertEquals(102, waveFingerprints.size());
        assertEquals(102, botanicalFingerprints.size());
        assertEquals(102, minimumSizePrismFingerprints.size());
        assertEquals(102, minimumSizeWaveFingerprints.size());
        assertEquals(102, minimumSizeBotanicalFingerprints.size());
    }

    @Test
    public void generatedParametersStayInsideSafeOrderedRanges() {
        for (int cardNumber = -3; cardNumber <= 102; cardNumber++) {
            CardBackGeometry geometry = CardBackGeometry.forCard(cardNumber);
            assertBetween(geometry.prismApexX, -0.10f, 0.10f);
            assertBetween(geometry.prismApexY, -0.14f, -0.05f);
            assertBetween(geometry.prismBottomX, -0.10f, 0.10f);
            assertBetween(geometry.prismLeftKneeY, 0.05f, 0.15f);
            assertBetween(geometry.prismRightKneeY, 0.05f, 0.15f);
            assertTrue(geometry.prismApexY < geometry.prismLeftKneeY);
            assertTrue(geometry.prismApexY < geometry.prismRightKneeY);

            assertBetween(geometry.waveSpacing, 0.12f, 0.24f);
            assertBetween(geometry.waveAmplitude, 0.09f, 0.21f);
            assertTrue(
                geometry.waveTailBend == -0.075f
                    || geometry.waveTailBend == 0.075f
            );
            assertTrue(geometry.waveDirection == -1f || geometry.waveDirection == 1f);
            assertTrue(geometry.waveAccentBand >= 0);
            assertTrue(geometry.waveAccentBand <= 2);

            float maximumWaveY = geometry.waveSpacing
                + Math.max(geometry.waveAmplitude, Math.abs(geometry.waveTailBend));
            assertTrue(maximumWaveY <= 0.45f);

            assertTrue(geometry.botanicalSideMask >= 0);
            assertTrue(geometry.botanicalSideMask <= 0b1111);
            assertTrue(geometry.botanicalAccentLeaf >= 0);
            assertTrue(
                geometry.botanicalAccentLeaf < CardBackGeometry.BOTANICAL_LEAF_COUNT
            );
            assertTrue(
                geometry.botanicalStemDirection == -1f
                    || geometry.botanicalStemDirection == 1f
            );
            assertTrue(
                geometry.botanicalTerminalDirection == -1f
                    || geometry.botanicalTerminalDirection == 1f
            );
        }
    }

    @Test
    public void botanicalTwigsEndBeforePointedLeavesAndStayInsideTheCard() {
        for (int cardNumber = 1; cardNumber <= 102; cardNumber++) {
            CardBackGeometry geometry = CardBackGeometry.forCard(cardNumber);
            for (int leaf = 0; leaf < CardBackGeometry.BOTANICAL_LEAF_COUNT; leaf++) {
                int side = geometry.botanicalSide(leaf);
                float anchorX = geometry.botanicalStemX(leaf);
                float anchorY = geometry.botanicalAnchorY(leaf);
                float baseX = anchorX
                    + side * CardBackGeometry.BOTANICAL_LEAF_BASE_OFFSET;
                float baseY = anchorY;
                float tipX = baseX
                    + side * CardBackGeometry.BOTANICAL_LEAF_LENGTH_X;
                float tipY = baseY + CardBackGeometry.BOTANICAL_LEAF_LENGTH_Y;

                assertTrue(Math.abs(anchorX) < 0.10f);
                assertTrue(Math.abs(baseX) < 0.16f);
                assertTrue(Math.abs(tipX) < 0.31f);
                assertTrue(tipY > -0.35f && tipY < 0.35f);
                float twigBackX = anchorX - baseX;
                float twigBackY = anchorY - baseY;
                float leafForwardX = tipX - baseX;
                float leafForwardY = tipY - baseY;
                assertTrue(
                    twigBackX * leafForwardX + twigBackY * leafForwardY < 0f
                );
                assertTrue(
                    leafForwardX * leafForwardX + leafForwardY * leafForwardY
                        > CardBackGeometry.BOTANICAL_LEAF_HALF_WIDTH
                            * CardBackGeometry.BOTANICAL_LEAF_HALF_WIDTH
                );
            }

            float terminalBaseX = geometry.botanicalStemDirection
                * CardBackGeometry.BOTANICAL_STEM_CURVE;
            float terminalTipX = terminalBaseX
                + geometry.botanicalTerminalDirection
                    * CardBackGeometry.BOTANICAL_TERMINAL_LENGTH_X;
            float terminalTipY = -0.30f
                + CardBackGeometry.BOTANICAL_TERMINAL_LENGTH_Y;
            assertTrue(Math.abs(terminalTipX) < 0.21f);
            assertTrue(terminalTipY >= -0.36f);
        }
    }

    @Test
    public void prismFacetsRemainNonDegenerate() {
        for (int cardNumber = 1; cardNumber <= 102; cardNumber++) {
            CardBackGeometry geometry = CardBackGeometry.forCard(cardNumber);
            float left = -0.34f;
            float right = 0.34f;
            float top = -0.34f;
            float bottom = 0.34f;
            assertTriangle(left, top, geometry.prismApexX, geometry.prismApexY, left, geometry.prismLeftKneeY);
            assertTriangle(left, top, right, top, geometry.prismApexX, geometry.prismApexY);
            assertTriangle(right, top, right, geometry.prismRightKneeY, geometry.prismApexX, geometry.prismApexY);
            assertTriangle(left, geometry.prismLeftKneeY, geometry.prismApexX, geometry.prismApexY, geometry.prismBottomX, bottom);
            assertTriangle(geometry.prismApexX, geometry.prismApexY, right, geometry.prismRightKneeY, geometry.prismBottomX, bottom);
            assertTriangle(left, geometry.prismLeftKneeY, geometry.prismBottomX, bottom, left, bottom);
            assertTriangle(right, geometry.prismRightKneeY, right, bottom, geometry.prismBottomX, bottom);
        }
    }

    private static void assertTriangle(
        float firstX,
        float firstY,
        float secondX,
        float secondY,
        float thirdX,
        float thirdY
    ) {
        float doubledArea = Math.abs(
            firstX * (secondY - thirdY)
                + secondX * (thirdY - firstY)
                + thirdX * (firstY - secondY)
        );
        assertTrue(doubledArea > 0.005f);
    }

    private static void assertBetween(float value, float minimum, float maximum) {
        assertTrue(value >= minimum);
        assertTrue(value <= maximum);
    }

    private static String prismFingerprint(CardBackGeometry geometry) {
        return Float.floatToIntBits(geometry.prismApexX)
            + ":" + Float.floatToIntBits(geometry.prismApexY)
            + ":" + Float.floatToIntBits(geometry.prismBottomX)
            + ":" + Float.floatToIntBits(geometry.prismLeftKneeY)
            + ":" + Float.floatToIntBits(geometry.prismRightKneeY)
            + ":" + geometry.prismPaletteFlipped;
    }

    private static String waveFingerprint(CardBackGeometry geometry) {
        return Float.floatToIntBits(geometry.waveSpacing)
            + ":" + Float.floatToIntBits(geometry.waveAmplitude)
            + ":" + Float.floatToIntBits(geometry.waveTailBend)
            + ":" + Float.floatToIntBits(geometry.waveDirection)
            + ":" + geometry.waveAccentBand;
    }

    private static String botanicalFingerprint(CardBackGeometry geometry) {
        return geometry.botanicalSideMask
            + ":" + geometry.botanicalAccentLeaf
            + ":" + Float.floatToIntBits(geometry.botanicalStemDirection)
            + ":" + Float.floatToIntBits(geometry.botanicalTerminalDirection);
    }

    private static String quantizedPrismFingerprint(
        CardBackGeometry geometry,
        float cardWidth
    ) {
        return Math.round(geometry.prismApexX * cardWidth)
            + ":" + Math.round(geometry.prismApexY * cardWidth)
            + ":" + Math.round(geometry.prismBottomX * cardWidth)
            + ":" + Math.round(geometry.prismLeftKneeY * cardWidth)
            + ":" + Math.round(geometry.prismRightKneeY * cardWidth)
            + ":" + geometry.prismPaletteFlipped;
    }

    private static String quantizedWaveFingerprint(
        CardBackGeometry geometry,
        float cardWidth
    ) {
        return Math.round(geometry.waveSpacing * cardWidth)
            + ":" + Math.round(geometry.waveAmplitude * cardWidth)
            + ":" + Math.round(geometry.waveTailBend * cardWidth)
            + ":" + Float.floatToIntBits(geometry.waveDirection)
            + ":" + geometry.waveAccentBand;
    }

    private static String quantizedBotanicalFingerprint(
        CardBackGeometry geometry,
        float cardWidth
    ) {
        return geometry.botanicalSideMask
            + ":" + geometry.botanicalAccentLeaf
            + ":" + Math.round(
                geometry.botanicalStemDirection
                    * CardBackGeometry.BOTANICAL_STEM_CURVE
                    * cardWidth
            )
            + ":" + Math.round(
                geometry.botanicalTerminalDirection
                    * CardBackGeometry.BOTANICAL_TERMINAL_LENGTH_X
                    * cardWidth
            );
    }
}
