package com.example.flipandfind;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.View;

/** Small, permission-free sound and haptic effects for game events. */
public final class GameFeedback {
    private static final float MATCH_CHIME_VOLUME = 0.72f;

    private AudioTrack matchChime;
    private boolean soundEnabled;
    private boolean hapticsEnabled;
    private boolean audioUnavailable;
    private boolean released;

    /** Feedback starts disabled until the owning screen applies its persisted preferences. */
    public GameFeedback() {
    }

    public void setSoundEnabled(boolean enabled) {
        if (released) {
            return;
        }
        if (soundEnabled == enabled) {
            return;
        }
        soundEnabled = enabled;
        if (enabled) {
            // A later user action may succeed even if an earlier native allocation did not.
            audioUnavailable = false;
            // Prepare on the settings/startup path so the first match never synthesizes audio
            // during its card-flight animation.
            getOrCreateMatchChime();
        } else {
            releaseMatchChime();
        }
    }

    public void setHapticsEnabled(boolean enabled) {
        if (!released) {
            hapticsEnabled = enabled;
        }
    }

    public void onCardFlip(View source) {
        emitHaptic(source, HapticFeedbackConstants.CLOCK_TICK);
    }

    public void onMatch(View source) {
        if (!released && soundEnabled) {
            playMatchChime();
        }
        emitHaptic(source, matchHapticConstant());
    }

    public void onMiss(View source) {
        emitHaptic(source, missHapticConstant());
    }

    /** Releases native audio resources. This helper must not be reused afterwards. */
    public void release() {
        if (released) {
            return;
        }
        released = true;
        soundEnabled = false;
        hapticsEnabled = false;
        releaseMatchChime();
    }

    private void emitHaptic(View source, int hapticConstant) {
        if (released) {
            return;
        }
        if (hapticsEnabled && source != null && source.isAttachedToWindow()) {
            try {
                // No ignore-global-setting flag: always respect the device's haptic preference.
                source.performHapticFeedback(hapticConstant);
            } catch (RuntimeException ignored) {
                // A detached or vendor-specific View implementation must not interrupt the game.
            }
        }
    }

    private void playMatchChime() {
        AudioTrack track = getOrCreateMatchChime();
        if (track == null) {
            return;
        }
        try {
            if (track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                track.pause();
            }
            track.setPlaybackHeadPosition(0);
            track.play();
        } catch (RuntimeException ignored) {
            audioUnavailable = true;
            releaseMatchChime();
        }
    }

    private AudioTrack getOrCreateMatchChime() {
        if (matchChime != null || audioUnavailable) {
            return matchChime;
        }
        AudioTrack created = null;
        try {
            short[] pcm = MatchChimeSynth.createPcm();
            created = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                .setAudioFormat(new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(MatchChimeSynth.SAMPLE_RATE_HZ)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build())
                .setBufferSizeInBytes(pcm.length * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build();
            int written = created.write(pcm, 0, pcm.length, AudioTrack.WRITE_BLOCKING);
            if (written != pcm.length) {
                created.release();
                audioUnavailable = true;
                return null;
            }
            created.setVolume(MATCH_CHIME_VOLUME);
            matchChime = created;
        } catch (RuntimeException ignored) {
            if (created != null) {
                try {
                    created.release();
                } catch (RuntimeException releaseIgnored) {
                    // The partially initialized native track may already be released.
                }
            }
            audioUnavailable = true;
        }
        return matchChime;
    }

    private void releaseMatchChime() {
        AudioTrack track = matchChime;
        matchChime = null;
        if (track == null) {
            return;
        }
        try {
            track.stop();
        } catch (RuntimeException ignored) {
            // Continue to release even if playback had already completed.
        }
        try {
            track.release();
        } catch (RuntimeException ignored) {
            // Native audio teardown is best-effort during Activity destruction.
        }
    }

    private static int matchHapticConstant() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            ? HapticFeedbackConstants.CONFIRM
            : HapticFeedbackConstants.LONG_PRESS;
    }

    private static int missHapticConstant() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            ? HapticFeedbackConstants.REJECT
            : HapticFeedbackConstants.VIRTUAL_KEY;
    }
}
