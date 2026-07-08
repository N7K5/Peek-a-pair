package com.example.flipandfind;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * A small margin-aware layout that places variable-width children on successive lines.
 *
 * <p>The class is intended for programmatically-created chip groups. Spacing is specified in
 * pixels so callers can use their existing dp conversion helper. In RTL layouts, logical child
 * order starts at the right edge and start/end margins are respected.
 */
public final class FlowLayout extends ViewGroup {
    private int horizontalSpacing;
    private int verticalSpacing;

    public FlowLayout(Context context) {
        this(context, null);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public int getHorizontalSpacing() {
        return horizontalSpacing;
    }

    public void setHorizontalSpacing(int spacingPixels) {
        int spacing = Math.max(0, spacingPixels);
        if (horizontalSpacing != spacing) {
            horizontalSpacing = spacing;
            requestLayout();
        }
    }

    public int getVerticalSpacing() {
        return verticalSpacing;
    }

    public void setVerticalSpacing(int spacingPixels) {
        int spacing = Math.max(0, spacingPixels);
        if (verticalSpacing != spacing) {
            verticalSpacing = spacing;
            requestLayout();
        }
    }

    public void setSpacing(int horizontalPixels, int verticalPixels) {
        int horizontal = Math.max(0, horizontalPixels);
        int vertical = Math.max(0, verticalPixels);
        if (horizontalSpacing != horizontal || verticalSpacing != vertical) {
            horizontalSpacing = horizontal;
            verticalSpacing = vertical;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int availableWidth = widthMode == MeasureSpec.UNSPECIFIED
            ? Integer.MAX_VALUE
            : Math.max(0, widthSize - getPaddingLeft() - getPaddingRight());

        int lineWidth = 0;
        int lineHeight = 0;
        int maximumLineWidth = 0;
        int contentHeight = 0;
        boolean hasLine = false;
        int childState = 0;

        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE) {
                continue;
            }

            MarginLayoutParams params = (MarginLayoutParams) child.getLayoutParams();
            params.resolveLayoutDirection(getLayoutDirection());
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
            childState = combineMeasuredStates(childState, child.getMeasuredState());

            int childWidth = child.getMeasuredWidth()
                + params.getMarginStart()
                + params.getMarginEnd();
            int childHeight = child.getMeasuredHeight()
                + params.topMargin
                + params.bottomMargin;
            int spacingBefore = lineWidth == 0 ? 0 : horizontalSpacing;

            if (lineWidth > 0
                && availableWidth != Integer.MAX_VALUE
                && lineWidth + spacingBefore + childWidth > availableWidth) {
                maximumLineWidth = Math.max(maximumLineWidth, lineWidth);
                if (hasLine) {
                    contentHeight += verticalSpacing;
                }
                contentHeight += lineHeight;
                hasLine = true;
                lineWidth = childWidth;
                lineHeight = childHeight;
            } else {
                lineWidth += spacingBefore + childWidth;
                lineHeight = Math.max(lineHeight, childHeight);
            }
        }

        if (lineWidth > 0) {
            maximumLineWidth = Math.max(maximumLineWidth, lineWidth);
            if (hasLine) {
                contentHeight += verticalSpacing;
            }
            contentHeight += lineHeight;
        }

        int desiredWidth = getPaddingLeft() + maximumLineWidth + getPaddingRight();
        int desiredHeight = getPaddingTop() + contentHeight + getPaddingBottom();
        desiredWidth = Math.max(desiredWidth, getSuggestedMinimumWidth());
        desiredHeight = Math.max(desiredHeight, getSuggestedMinimumHeight());

        setMeasuredDimension(
            resolveSizeAndState(desiredWidth, widthMeasureSpec, childState),
            resolveSizeAndState(
                desiredHeight,
                heightMeasureSpec,
                childState << MEASURED_HEIGHT_STATE_SHIFT
            )
        );
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        int contentWidth = Math.max(
            0,
            right - left - getPaddingLeft() - getPaddingRight()
        );
        int lineWidth = 0;
        int lineHeight = 0;
        int lineTop = getPaddingTop();
        int cursor = rtl ? right - left - getPaddingRight() : getPaddingLeft();

        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE) {
                continue;
            }

            MarginLayoutParams params = (MarginLayoutParams) child.getLayoutParams();
            params.resolveLayoutDirection(getLayoutDirection());
            int startMargin = params.getMarginStart();
            int endMargin = params.getMarginEnd();
            int childWidth = child.getMeasuredWidth() + startMargin + endMargin;
            int childHeight = child.getMeasuredHeight()
                + params.topMargin
                + params.bottomMargin;
            int spacingBefore = lineWidth == 0 ? 0 : horizontalSpacing;

            if (lineWidth > 0
                && lineWidth + spacingBefore + childWidth > contentWidth) {
                lineTop += lineHeight + verticalSpacing;
                lineWidth = 0;
                lineHeight = 0;
                spacingBefore = 0;
                cursor = rtl ? right - left - getPaddingRight() : getPaddingLeft();
            }

            if (rtl) {
                cursor -= spacingBefore + startMargin;
                int childRight = cursor;
                int childLeft = childRight - child.getMeasuredWidth();
                int childTop = lineTop + params.topMargin;
                child.layout(
                    childLeft,
                    childTop,
                    childRight,
                    childTop + child.getMeasuredHeight()
                );
                cursor = childLeft - endMargin;
            } else {
                cursor += spacingBefore + startMargin;
                int childLeft = cursor;
                int childTop = lineTop + params.topMargin;
                child.layout(
                    childLeft,
                    childTop,
                    childLeft + child.getMeasuredWidth(),
                    childTop + child.getMeasuredHeight()
                );
                cursor = childLeft + child.getMeasuredWidth() + endMargin;
            }

            lineWidth += spacingBefore + childWidth;
            lineHeight = Math.max(lineHeight, childHeight);
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        );
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams params) {
        return params instanceof MarginLayoutParams
            ? new MarginLayoutParams((MarginLayoutParams) params)
            : new MarginLayoutParams(params);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams params) {
        return params instanceof MarginLayoutParams;
    }
}
