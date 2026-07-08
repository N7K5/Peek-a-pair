package com.example.flipandfind;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MatchChimeSynthTest {
    @Test
    public void generatedPcmHasExpectedDurationAndIsDeterministic() {
        short[] first = MatchChimeSynth.createPcm();
        short[] second = MatchChimeSynth.createPcm();

        assertEquals(
            MatchChimeSynth.SAMPLE_RATE_HZ * MatchChimeSynth.DURATION_MS / 1_000,
            first.length
        );
        assertArrayEquals(first, second);
    }

    @Test
    public void generatedPcmHasAudibleEnergyWithoutClipping() {
        short[] pcm = MatchChimeSynth.createPcm();
        int peak = 0;
        double squareSum = 0.0;
        for (short sample : pcm) {
            int magnitude = Math.abs((int) sample);
            peak = Math.max(peak, magnitude);
            squareSum += (double) sample * sample;
        }
        double rms = Math.sqrt(squareSum / pcm.length);

        assertTrue("The chime should be clearly audible", rms > 1_500.0);
        assertTrue("The mix should leave PCM headroom", peak < 32_700);
    }

    @Test
    public void envelopeStartsAndEndsQuietly() {
        short[] pcm = MatchChimeSynth.createPcm();
        int edgeWindow = MatchChimeSynth.SAMPLE_RATE_HZ / 100;

        assertEquals(0, pcm[0]);
        assertTrue(averageMagnitude(pcm, 0, edgeWindow) < 7_000.0);
        assertTrue(
            averageMagnitude(pcm, pcm.length - edgeWindow, pcm.length) < 120.0
        );
    }

    @Test
    public void layeredVoicesProduceRichWaveform() {
        short[] pcm = MatchChimeSynth.createPcm();
        int signChanges = 0;
        for (int index = 1; index < pcm.length; index++) {
            if ((pcm[index - 1] < 0 && pcm[index] >= 0)
                || (pcm[index - 1] > 0 && pcm[index] <= 0)) {
                signChanges++;
            }
        }

        assertTrue("The chime should contain substantial harmonic motion", signChanges > 600);
    }

    private static double averageMagnitude(short[] pcm, int start, int end) {
        long sum = 0;
        for (int index = start; index < end; index++) {
            sum += Math.abs((int) pcm[index]);
        }
        return sum / (double) (end - start);
    }
}
