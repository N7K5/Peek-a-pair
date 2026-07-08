package com.example.flipandfind;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import java.util.ArrayList;
import java.util.List;

/** A deterministic random tabletop constrained to exactly the available screen area. */
public final class BoardLayout extends ViewGroup {
    private static final int VACANT_SLOT_COUNT = 2;

    private long layoutSeed = 1L;
    private boolean largerCards;
    private int tileWidth;
    private int tileHeight;
    private ScatterLayoutEngine.Result scatter;
    private int[] slotPermutation = new int[0];

    private AnimatorSet activeBoardMotion;
    private long motionGeneration;
    private long pendingSpreadDurationMs = -1L;
    private Runnable pendingSpreadEndAction;
    private boolean spreadStartPosted;

    private long cachedSeed = Long.MIN_VALUE;
    private int cachedCount = -1;
    private int cachedContentWidth = -1;
    private int cachedContentHeight = -1;

    public BoardLayout(Context context) {
        this(context, null);
    }

    public BoardLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClipChildren(false);
        setClipToPadding(false);
    }

    public void setLayoutSeed(long layoutSeed) {
        cancelBoardMotion(true);
        this.layoutSeed = layoutSeed;
        cachedSeed = Long.MIN_VALUE;
        requestLayout();
    }

    /**
     * Gives card size priority over decorative whitespace while retaining the same fixed,
     * non-scrolling, overlap-free board contract. Extremely dense boards still shrink as needed.
     */
    public void setLargerCards(boolean largerCards) {
        if (this.largerCards == largerCards) {
            return;
        }
        cancelBoardMotion(true);
        this.largerCards = largerCards;
        cachedSeed = Long.MIN_VALUE;
        scatter = null;
        requestLayout();
    }

    /**
     * Returns a defensive child-to-slot mapping suitable for an Activity saved-state bundle.
     * Each array index is a child index and each value is its packed tabletop slot. Two packed
     * slots are intentionally absent from the array and therefore vacant.
     */
    public int[] copySlotPermutation() {
        ensureSlotPermutation();
        return SlotPermutation.copy(slotPermutation);
    }

    /**
     * Restores an injective child-to-slot mapping whose length matches the current child count.
     * Legacy mappings containing only slots {@code 0..childCount-1} remain valid; the two new
     * trailing slots simply begin vacant. Invalid input is rejected without changing state.
     */
    public boolean setSlotPermutation(int[] permutation) {
        int count = getChildCount();
        if (!SlotPermutation.isValid(permutation, count, slotCountForChildren(count))) {
            return false;
        }
        cancelBoardMotion(true);
        slotPermutation = SlotPermutation.copy(permutation);
        requestLayout();
        return true;
    }

    /**
     * Starts every visible card near the board center and moves it to its existing packed slot.
     * If called before the first layout, the animation starts immediately after layout completes.
     */
    public void animateSpread(long durationMs) {
        animateSpread(durationMs, null);
    }

    public void animateSpread(long durationMs, Runnable endAction) {
        cancelBoardMotion(true);
        pendingSpreadDurationMs = Math.max(0L, durationMs);
        pendingSpreadEndAction = endAction;
        requestLayout();
        postSpreadStartIfReady();
    }

    /** Snaps any active motion to its already-committed final slots. */
    public void finishBoardMotion() {
        cancelBoardMotion(true);
        if (hasUsableScatter(getChildCount())) {
            ensureSlotPermutation();
            for (int index = 0; index < getChildCount(); index++) {
                layoutChildInSlot(getChildAt(index), slotPermutation[index]);
            }
        }
    }

    /**
     * Swaps two children's packed slots and animates only those visible children between them.
     * The permutation is updated before animation begins, so saving mid-animation preserves the
     * final destination. Returns false for invalid/equal indices or before a usable layout exists.
     */
    public boolean swapCardSlots(
        int firstChildIndex,
        int secondChildIndex,
        long durationMs,
        Runnable endAction
    ) {
        int count = getChildCount();
        if (firstChildIndex < 0 || firstChildIndex >= count
            || secondChildIndex < 0 || secondChildIndex >= count
            || firstChildIndex == secondChildIndex
            || !hasUsableScatter(count)) {
            return false;
        }

        cancelBoardMotion(true);
        ensureSlotPermutation();
        int[] oldPermutation = SlotPermutation.copy(slotPermutation);
        SlotPermutation.swap(slotPermutation, firstChildIndex, secondChildIndex);
        animatePermutationChange(oldPermutation, durationMs, endAction);
        return true;
    }

    /**
     * Relocates only the two selected children into the two random vacant packed slots. Their old
     * slots become vacant, so no third card moves. {@code randomSeed} makes assignment deterministic.
     */
    public boolean relocateCardSlots(
        int firstChildIndex,
        int secondChildIndex,
        long randomSeed,
        long durationMs,
        Runnable endAction
    ) {
        int count = getChildCount();
        if (count < 2
            || firstChildIndex < 0
            || firstChildIndex >= count
            || secondChildIndex < 0
            || secondChildIndex >= count
            || firstChildIndex == secondChildIndex
            || !hasUsableScatter(count)) {
            return false;
        }

        cancelBoardMotion(true);
        ensureSlotPermutation();
        int[] oldPermutation = SlotPermutation.copy(slotPermutation);
        if (!SlotPermutation.relocatePairToVacancies(
            slotPermutation,
            slotCountForChildren(count),
            firstChildIndex,
            secondChildIndex,
            randomSeed
        )) {
            return false;
        }
        animatePermutationChange(oldPermutation, durationMs, endAction);
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            measuredWidth = getResources().getDisplayMetrics().widthPixels;
        }
        int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            measuredHeight = Math.round(getResources().getDisplayMetrics().heightPixels * 0.58f);
        }

        int contentWidth = Math.max(1, measuredWidth - getPaddingLeft() - getPaddingRight());
        int contentHeight = Math.max(1, measuredHeight - getPaddingTop() - getPaddingBottom());
        int count = getChildCount();
        ensureSlotPermutation();

        if (measuredWidth <= getPaddingLeft() + getPaddingRight()
            || measuredHeight <= getPaddingTop() + getPaddingBottom()) {
            tileWidth = 0;
            tileHeight = 0;
            scatter = null;
            int zeroSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY);
            for (int index = 0; index < count; index++) {
                getChildAt(index).measure(zeroSpec, zeroSpec);
            }
            setMeasuredDimension(
                resolveSize(measuredWidth, widthMeasureSpec),
                resolveSize(measuredHeight, heightMeasureSpec)
            );
            return;
        }

        if (scatter == null
            || cachedSeed != layoutSeed
            || cachedCount != count
            || cachedContentWidth != contentWidth
            || cachedContentHeight != contentHeight) {
            fitScatter(slotCountForChildren(count), contentWidth, contentHeight);
            cachedSeed = layoutSeed;
            cachedCount = count;
            cachedContentWidth = contentWidth;
            cachedContentHeight = contentHeight;
        }

        int childWidthSpec = MeasureSpec.makeMeasureSpec(tileWidth, MeasureSpec.EXACTLY);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(tileHeight, MeasureSpec.EXACTLY);
        for (int index = 0; index < count; index++) {
            getChildAt(index).measure(childWidthSpec, childHeightSpec);
        }

        setMeasuredDimension(
            resolveSize(measuredWidth, widthMeasureSpec),
            resolveSize(measuredHeight, heightMeasureSpec)
        );
    }

    private void fitScatter(int count, int contentWidth, int contentHeight) {
        BoardCardSizing.Fit fit = BoardCardSizing.fit(
            count,
            contentWidth,
            contentHeight,
            getResources().getDisplayMetrics().density,
            largerCards,
            layoutSeed
        );
        tileWidth = fit.cardWidth;
        tileHeight = fit.cardHeight;
        scatter = fit.scatter;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (scatter == null) {
            return;
        }
        ensureSlotPermutation();
        for (int index = 0; index < getChildCount(); index++) {
            layoutChildInSlot(getChildAt(index), slotPermutation[index]);
        }
        postSpreadStartIfReady();
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelBoardMotion(true);
        super.onDetachedFromWindow();
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams params) {
        return new LayoutParams(params);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    private void ensureSlotPermutation() {
        int count = getChildCount();
        if (!SlotPermutation.isValid(
            slotPermutation,
            count,
            slotCountForChildren(count)
        )) {
            slotPermutation = SlotPermutation.identity(count);
        }
    }

    private boolean hasUsableScatter(int count) {
        int slotCount = slotCountForChildren(count);
        return count > 0
            && scatter != null
            && scatter.lefts.length == slotCount
            && scatter.tops.length == slotCount
            && scatter.rotations.length == slotCount
            && tileWidth > 0
            && tileHeight > 0
            && getWidth() > 0
            && getHeight() > 0;
    }

    private static int slotCountForChildren(int childCount) {
        return childCount <= 0 ? 0 : childCount + VACANT_SLOT_COUNT;
    }

    private int slotLeft(int slot) {
        return getPaddingLeft() + Math.round(scatter.lefts[slot]);
    }

    private int slotTop(int slot) {
        return getPaddingTop() + Math.round(scatter.tops[slot]);
    }

    private void layoutChildInSlot(View child, int slot) {
        int childLeft = slotLeft(slot);
        int childTop = slotTop(slot);
        child.layout(childLeft, childTop, childLeft + tileWidth, childTop + tileHeight);
        child.setPivotX(tileWidth / 2f);
        child.setPivotY(tileHeight / 2f);
        // Rotation belongs to the slot, not the child. Moving a child into an occupied slot thus
        // preserves the exact packed rectangle and cannot introduce a final-position overlap.
        // Keep an ObjectAnimator's current value if Android relays out the board mid-swap.
        if (activeBoardMotion == null) {
            child.setRotation(scatter.rotations[slot]);
        }
    }

    private void postSpreadStartIfReady() {
        int count = getChildCount();
        if (pendingSpreadDurationMs < 0L
            || spreadStartPosted
            || !hasUsableScatter(count)) {
            return;
        }
        long durationMs = pendingSpreadDurationMs;
        Runnable endAction = pendingSpreadEndAction;
        pendingSpreadDurationMs = -1L;
        pendingSpreadEndAction = null;
        // Reduced-motion callers use duration zero. Do not briefly translate the cards into the
        // center stack before the posted completion snaps them back to their final slots.
        List<Animator> animators = durationMs == 0L
            ? new ArrayList<>()
            : prepareSpreadAnimators();
        long expectedGeneration = motionGeneration;
        spreadStartPosted = true;
        post(() -> {
            spreadStartPosted = false;
            if (expectedGeneration != motionGeneration) {
                // A newer spread request may have arrived while this start was queued.
                postSpreadStartIfReady();
                return;
            }
            startBoardMotion(
                animators,
                durationMs,
                new DecelerateInterpolator(1.7f),
                endAction
            );
        });
    }

    private List<Animator> prepareSpreadAnimators() {
        ensureSlotPermutation();
        float centerX = getPaddingLeft()
            + (getWidth() - getPaddingLeft() - getPaddingRight()) / 2f;
        float centerY = getPaddingTop()
            + (getHeight() - getPaddingTop() - getPaddingBottom()) / 2f;
        List<Animator> animators = new ArrayList<>(getChildCount() * 2);
        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);
            if (child.getVisibility() != VISIBLE) {
                child.setTranslationX(0f);
                child.setTranslationY(0f);
                continue;
            }
            float stackOffsetX = ((index % 3) - 1) * dp(1.25f);
            float stackOffsetY = (((index / 3) % 3) - 1) * dp(1.25f);
            child.setTranslationX(
                centerX + stackOffsetX - (child.getLeft() + child.getWidth() / 2f)
            );
            child.setTranslationY(
                centerY + stackOffsetY - (child.getTop() + child.getHeight() / 2f)
            );
            addTranslationAnimators(animators, child);
        }
        return animators;
    }

    private static void addTranslationAnimators(List<Animator> animators, View child) {
        if (child.getVisibility() != VISIBLE) {
            child.setTranslationX(0f);
            child.setTranslationY(0f);
            return;
        }
        animators.add(ObjectAnimator.ofFloat(child, View.TRANSLATION_X, 0f));
        animators.add(ObjectAnimator.ofFloat(child, View.TRANSLATION_Y, 0f));
    }

    /** Lays changed children into committed destinations, then animates from their old slots. */
    private void animatePermutationChange(
        int[] oldPermutation,
        long durationMs,
        Runnable endAction
    ) {
        List<Animator> animators = new ArrayList<>(getChildCount() * 3);
        for (int childIndex = 0; childIndex < getChildCount(); childIndex++) {
            int oldSlot = oldPermutation[childIndex];
            int destinationSlot = slotPermutation[childIndex];
            if (oldSlot == destinationSlot) {
                continue;
            }

            View child = getChildAt(childIndex);
            float destinationRotation = scatter.rotations[destinationSlot];
            layoutChildInSlot(child, destinationSlot);
            child.setTranslationX(slotLeft(oldSlot) - slotLeft(destinationSlot));
            child.setTranslationY(slotTop(oldSlot) - slotTop(destinationSlot));
            if (child.getVisibility() == VISIBLE) {
                child.setRotation(scatter.rotations[oldSlot]);
                addTranslationAnimators(animators, child);
                animators.add(ObjectAnimator.ofFloat(
                    child,
                    View.ROTATION,
                    destinationRotation
                ));
            } else {
                child.setTranslationX(0f);
                child.setTranslationY(0f);
                child.setRotation(destinationRotation);
            }
        }
        startBoardMotion(
            animators,
            Math.max(0L, durationMs),
            new AccelerateDecelerateInterpolator(),
            endAction
        );
    }

    private void startBoardMotion(
        List<Animator> animators,
        long durationMs,
        android.animation.TimeInterpolator interpolator,
        Runnable endAction
    ) {
        long generation = ++motionGeneration;
        if (animators.isEmpty() || durationMs == 0L) {
            resetTranslations();
            snapSlotRotations();
            if (endAction != null) {
                post(() -> {
                    if (generation == motionGeneration) {
                        endAction.run();
                    }
                });
            }
            return;
        }

        AnimatorSet motion = new AnimatorSet();
        activeBoardMotion = motion;
        motion.playTogether(animators);
        motion.setDuration(durationMs);
        motion.setInterpolator(interpolator);
        motion.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (generation != motionGeneration || activeBoardMotion != motion) {
                    return;
                }
                activeBoardMotion = null;
                resetTranslations();
                snapSlotRotations();
                if (endAction != null) {
                    endAction.run();
                }
            }
        });
        motion.start();
    }

    private void cancelBoardMotion(boolean clearPendingSpread) {
        motionGeneration++;
        AnimatorSet motion = activeBoardMotion;
        activeBoardMotion = null;
        if (motion != null) {
            motion.removeAllListeners();
            motion.cancel();
        }
        resetTranslations();
        snapSlotRotations();
        if (clearPendingSpread) {
            pendingSpreadDurationMs = -1L;
            pendingSpreadEndAction = null;
        }
    }

    private void resetTranslations() {
        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);
            child.setTranslationX(0f);
            child.setTranslationY(0f);
        }
    }

    private void snapSlotRotations() {
        if (!hasUsableScatter(getChildCount())) {
            return;
        }
        ensureSlotPermutation();
        for (int index = 0; index < getChildCount(); index++) {
            getChildAt(index).setRotation(scatter.rotations[slotPermutation[index]]);
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
