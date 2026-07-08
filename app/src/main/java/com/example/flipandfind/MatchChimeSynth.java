package com.example.flipandfind;

/** Builds the short, permission-free PCM chime used for a successful match. */
final class MatchChimeSynth {
    static final int SAMPLE_RATE_HZ = 44_100;
    static final int DURATION_MS = 460;

    private static final double TWO_PI = Math.PI * 2.0;
    private static final double MASTER_GAIN = 0.72;

    private MatchChimeSynth() {
    }

    static short[] createPcm() {
        int sampleCount = samplesForMilliseconds(DURATION_MS);
        double[] mix = new double[sampleCount];

        // A rising major arpeggio reads as a reward without resembling a system alert.
        addBellVoice(mix, 0, 250, 523.25, 0.56);    // C5
        addBellVoice(mix, 52, 270, 659.25, 0.48);   // E5
        addBellVoice(mix, 108, 285, 783.99, 0.43);  // G5
        addBellVoice(mix, 180, 270, 1046.50, 0.38); // C6 sparkle

        short[] pcm = new short[sampleCount];
        for (int index = 0; index < sampleCount; index++) {
            // Gentle saturation protects against clipping while preserving the layered harmonics.
            double sample = Math.tanh(mix[index] * MASTER_GAIN);
            pcm[index] = (short) Math.round(sample * Short.MAX_VALUE);
        }
        return pcm;
    }

    private static void addBellVoice(
        double[] mix,
        int startMs,
        int lengthMs,
        double frequencyHz,
        double gain
    ) {
        int start = samplesForMilliseconds(startMs);
        int length = Math.min(samplesForMilliseconds(lengthMs), mix.length - start);
        int attackSamples = Math.max(1, samplesForMilliseconds(7));
        for (int offset = 0; offset < length; offset++) {
            double elapsedSeconds = offset / (double) SAMPLE_RATE_HZ;
            double progress = offset / (double) Math.max(1, length - 1);
            double attack = Math.min(1.0, offset / (double) attackSamples);
            double decay = Math.pow(1.0 - progress, 2.35);
            double envelope = smoothStep(attack) * decay;

            double fundamental = Math.sin(TWO_PI * frequencyHz * elapsedSeconds);
            double brightPartial = Math.sin(TWO_PI * frequencyHz * 2.01 * elapsedSeconds + 0.35);
            double bellPartial = Math.sin(TWO_PI * frequencyHz * 3.97 * elapsedSeconds + 0.72);
            mix[start + offset] += gain
                * envelope
                * (fundamental + brightPartial * 0.24 + bellPartial * 0.08);
        }
    }

    private static double smoothStep(double value) {
        return value * value * (3.0 - 2.0 * value);
    }

    private static int samplesForMilliseconds(int milliseconds) {
        return SAMPLE_RATE_HZ * milliseconds / 1_000;
    }
}
