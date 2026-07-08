package com.example.flipandfind;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;

/** A small dependency-free row which reveals an end action when dragged to the left. */
public final class SwipeRevealLayout extends FrameLayout {
    private static final int ACTION_REVEAL_DELETE = 0x01020001;
    private final int touchSlop;
    private View foreground;
    private View action;
    private int actionWidth;
    private float downX;
    private float downY;
    private float startTranslation;
    private boolean dragging;
    private OnClickListener contentClickListener;

    public SwipeRevealLayout(Context context) {
        this(context, null);
    }

    public SwipeRevealLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setClipChildren(true);
    }

    public void setActionView(View actionView, int widthPixels) {
        if (actionView == null || widthPixels <= 0) {
            throw new IllegalArgumentException("A positive-width action view is required");
        }
        if (action != null) {
            removeView(action);
        }
        action = actionView;
        actionWidth = widthPixels;
        LayoutParams params = new LayoutParams(widthPixels, LayoutParams.MATCH_PARENT);
        params.gravity = android.view.Gravity.END;
        addView(action, 0, params);
        updateActionAccessibility(false);
    }

    public void setContentView(View contentView) {
        if (contentView == null) {
            throw new IllegalArgumentException("A content view is required");
        }
        if (foreground != null) {
            removeView(foreground);
        }
        foreground = contentView;
        addView(
            foreground,
            new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        );
        foreground.setOnClickListener(view -> {
            if (isOpen()) {
                close(true);
            } else if (contentClickListener != null) {
                contentClickListener.onClick(view);
            }
        });
        foreground.setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(
                View host,
                AccessibilityNodeInfo info
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(
                    ACTION_REVEAL_DELETE,
                    "Reveal delete button"
                ));
            }

            @Override
            public boolean performAccessibilityAction(View host, int actionId, Bundle args) {
                if (actionId == ACTION_REVEAL_DELETE) {
                    settleTo(-actionWidth, true);
                    return true;
                }
                return super.performAccessibilityAction(host, actionId, args);
            }
        });
    }

    public void setOnContentClickListener(OnClickListener listener) {
        contentClickListener = listener;
        if (foreground != null) {
            foreground.setClickable(true);
        }
    }

    public boolean isOpen() {
        return foreground != null && foreground.getTranslationX() < -1f;
    }

    public void close(boolean animate) {
        settleTo(0f, animate);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (foreground == null || actionWidth <= 0) {
            return super.onInterceptTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                startTranslation = foreground.getTranslationX();
                dragging = false;
                return false;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                    dragging = true;
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (foreground == null || actionWidth <= 0) {
            return super.onTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                startTranslation = foreground.getTranslationX();
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (!dragging && Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                    dragging = true;
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                if (dragging) {
                    foreground.animate().cancel();
                    foreground.setTranslationX(clamp(startTranslation + dx));
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (dragging) {
                    boolean reveal = foreground.getTranslationX() < -actionWidth * 0.4f;
                    settleTo(reveal ? -actionWidth : 0f, true);
                    performClick();
                    return true;
                }
                return super.onTouchEvent(event);
            case MotionEvent.ACTION_CANCEL:
                settleTo(
                    foreground.getTranslationX() < -actionWidth * 0.5f ? -actionWidth : 0f,
                    true
                );
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private float clamp(float translation) {
        return Math.max(-actionWidth, Math.min(0f, translation));
    }

    private void settleTo(float translation, boolean animate) {
        if (foreground == null) {
            return;
        }
        foreground.animate().cancel();
        if (!animate) {
            foreground.setTranslationX(translation);
            updateActionAccessibility(translation < 0f);
            return;
        }
        updateActionAccessibility(translation < 0f);
        foreground.animate()
            .translationX(translation)
            .setDuration(160L)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    updateActionAccessibility(foreground.getTranslationX() < 0f);
                }
            })
            .start();
    }

    private void updateActionAccessibility(boolean revealed) {
        if (action != null) {
            action.setImportantForAccessibility(
                revealed
                    ? View.IMPORTANT_FOR_ACCESSIBILITY_YES
                    : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            );
        }
    }
}
