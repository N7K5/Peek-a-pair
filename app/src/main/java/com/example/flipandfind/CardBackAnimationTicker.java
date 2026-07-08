package com.example.flipandfind;

import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/** One low-frequency frame source shared by every animated card on screen. */
final class CardBackAnimationTicker {
    private static final long FRAME_DELAY_MS = 66L;
    private static final long DISABLED_MOTION_POLL_MS = 1000L;
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static final List<WeakReference<CardTileView>> VIEWS = new ArrayList<>();
    private static boolean frameScheduled;

    private static final Runnable FRAME = () -> {
        frameScheduled = false;
        boolean hasVisibleView = false;
        boolean hasSystemSuppressedView = false;
        for (int index = VIEWS.size() - 1; index >= 0; index--) {
            CardTileView view = VIEWS.get(index).get();
            if (view == null || !view.shouldStayInBackAnimationTicker()) {
                VIEWS.remove(index);
            } else if (view.isAnimatedBackVisible()) {
                hasVisibleView = true;
                view.invalidate();
            } else if (view.isBackAnimationSuppressedBySystem()) {
                hasSystemSuppressedView = true;
            }
        }
        if (hasVisibleView) {
            scheduleFrameIfNeeded(FRAME_DELAY_MS);
        } else if (hasSystemSuppressedView) {
            scheduleFrameIfNeeded(DISABLED_MOTION_POLL_MS);
        }
    };

    private CardBackAnimationTicker() {
    }

    static void register(CardTileView view) {
        for (int index = VIEWS.size() - 1; index >= 0; index--) {
            CardTileView existing = VIEWS.get(index).get();
            if (existing == null) {
                VIEWS.remove(index);
            } else if (existing == view) {
                scheduleFrameIfNeeded(FRAME_DELAY_MS);
                return;
            }
        }
        VIEWS.add(new WeakReference<>(view));
        scheduleFrameIfNeeded(FRAME_DELAY_MS);
    }

    static void unregister(CardTileView view) {
        for (int index = VIEWS.size() - 1; index >= 0; index--) {
            CardTileView existing = VIEWS.get(index).get();
            if (existing == null || existing == view) {
                VIEWS.remove(index);
            }
        }
        if (VIEWS.isEmpty() && frameScheduled) {
            HANDLER.removeCallbacks(FRAME);
            frameScheduled = false;
        }
    }

    /** Wakes a paused ticker when scrolling or another redraw exposes an animated card. */
    static void wake() {
        scheduleFrameIfNeeded(FRAME_DELAY_MS);
    }

    private static void scheduleFrameIfNeeded(long delayMillis) {
        if (!frameScheduled && !VIEWS.isEmpty()) {
            frameScheduled = true;
            HANDLER.postDelayed(FRAME, delayMillis);
        }
    }
}
