package com.example.flipandfind;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Theme-aware procedural tabletop background suitable for both a board and a small preview.
 * Patterns are deterministic, scale with display density, and allocate nothing while drawing.
 */
public final class TabletopBackgroundDrawable extends Drawable {
    private static final int DEFAULT_PATTERN_SEED = 0x50414552;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint detailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint secondaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clipPath = new Path();
    private final Path detailPath = new Path();
    private final RectF drawingBounds = new RectF();
    private final RectF scratchRect = new RectF();

    private final float density;
    private final float cornerRadiusPx;
    private TabletopMode mode;
    private int playerColor;
    private int themeSurface;
    private int neutralBorder;
    private boolean darkTheme;
    private int patternSeed = DEFAULT_PATTERN_SEED;
    private int drawableAlpha = 255;
    private ColorFilter colorFilter;

    public TabletopBackgroundDrawable(
        Context context,
        TabletopMode mode,
        int playerColor,
        int themeSurface,
        int neutralBorder,
        boolean darkTheme,
        float cornerRadiusDp
    ) {
        this(
            context.getResources().getDisplayMetrics().density,
            mode,
            playerColor,
            themeSurface,
            neutralBorder,
            darkTheme,
            cornerRadiusDp
        );
    }

    TabletopBackgroundDrawable(
        float density,
        TabletopMode mode,
        int playerColor,
        int themeSurface,
        int neutralBorder,
        boolean darkTheme,
        float cornerRadiusDp
    ) {
        this.density = Math.max(0.5f, density);
        this.cornerRadiusPx = Math.max(0f, cornerRadiusDp) * this.density;
        this.mode = mode == null ? TabletopMode.STATIC_THEME : mode;
        this.playerColor = playerColor;
        this.themeSurface = themeSurface;
        this.neutralBorder = neutralBorder;
        this.darkTheme = darkTheme;
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(1f));
    }

    /** Allows two boards to retain different, but stable, procedural variations. */
    public TabletopBackgroundDrawable setPatternSeed(int patternSeed) {
        if (this.patternSeed != patternSeed) {
            this.patternSeed = patternSeed;
            invalidateSelf();
        }
        return this;
    }

    public TabletopBackgroundDrawable setPatternSeed(long patternSeed) {
        return setPatternSeed((int) (patternSeed ^ (patternSeed >>> 32)));
    }

    public TabletopMode getMode() {
        return mode;
    }

    public void setMode(TabletopMode mode) {
        TabletopMode safeMode = mode == null ? TabletopMode.STATIC_THEME : mode;
        if (this.mode != safeMode) {
            this.mode = safeMode;
            invalidateSelf();
        }
    }

    /** Updates the current-player tint without replacing the drawable. */
    public void setPlayerColor(int playerColor) {
        if (this.playerColor != playerColor) {
            this.playerColor = playerColor;
            invalidateSelf();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) {
            return;
        }
        drawingBounds.set(bounds);
        int fillColor = GameSurfaceColors.tabletop(
            mode,
            playerColor,
            themeSurface,
            darkTheme
        );
        int borderColor = GameSurfaceColors.tabletopBorder(
            mode,
            playerColor,
            themeSurface,
            neutralBorder,
            darkTheme
        );
        int primary = TabletopPatternColors.primary(fillColor, playerColor, darkTheme);
        int secondary = TabletopPatternColors.secondary(fillColor, playerColor, darkTheme);
        int highlight = TabletopPatternColors.highlight(fillColor, darkTheme);

        configurePaint(fillPaint, fillColor, 255, Paint.Style.FILL);
        canvas.drawRoundRect(drawingBounds, cornerRadiusPx, cornerRadiusPx, fillPaint);

        int saveCount = canvas.save();
        clipPath.reset();
        clipPath.addRoundRect(
            drawingBounds,
            cornerRadiusPx,
            cornerRadiusPx,
            Path.Direction.CW
        );
        canvas.clipPath(clipPath);
        switch (mode.getPattern()) {
            case TILES:
                drawTiles(canvas, primary, secondary);
                break;
            case MOSAIC:
                drawMosaic(canvas, primary, secondary);
                break;
            case GLASS_TILES:
                drawGlassTiles(canvas, primary, highlight);
                break;
            case DOTS:
                drawDots(canvas, primary, secondary);
                break;
            case CONTOURS:
                drawContours(canvas, primary, secondary);
                break;
            case SOLID:
            default:
                break;
        }
        canvas.restoreToCount(saveCount);

        float inset = borderPaint.getStrokeWidth() * 0.5f;
        scratchRect.set(drawingBounds);
        scratchRect.inset(inset, inset);
        configurePaint(borderPaint, borderColor, 255, Paint.Style.STROKE);
        canvas.drawRoundRect(
            scratchRect,
            Math.max(0f, cornerRadiusPx - inset),
            Math.max(0f, cornerRadiusPx - inset),
            borderPaint
        );
    }

    private void drawTiles(Canvas canvas, int primary, int secondary) {
        float cell = dp(28f);
        int columns = (int) Math.ceil(drawingBounds.width() / cell);
        int rows = (int) Math.ceil(drawingBounds.height() / cell);
        configurePaint(detailPaint, secondary, 22, Paint.Style.FILL);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if ((stableHash(column, row) & 3) != 0) {
                    continue;
                }
                float left = drawingBounds.left + column * cell;
                float top = drawingBounds.top + row * cell;
                canvas.drawRect(left, top, left + cell, top + cell, detailPaint);
            }
        }
        configurePaint(detailPaint, primary, 70, Paint.Style.STROKE);
        detailPaint.setStrokeWidth(dp(0.8f));
        for (int column = 1; column <= columns; column++) {
            float x = drawingBounds.left + column * cell;
            canvas.drawLine(x, drawingBounds.top, x, drawingBounds.bottom, detailPaint);
        }
        for (int row = 1; row <= rows; row++) {
            float y = drawingBounds.top + row * cell;
            canvas.drawLine(drawingBounds.left, y, drawingBounds.right, y, detailPaint);
        }
    }

    private void drawMosaic(Canvas canvas, int primary, int secondary) {
        float cell = dp(34f);
        int columns = (int) Math.ceil(drawingBounds.width() / cell);
        int rows = (int) Math.ceil(drawingBounds.height() / cell);
        configurePaint(detailPaint, primary, 36, Paint.Style.FILL);
        configurePaint(secondaryPaint, secondary, 28, Paint.Style.FILL);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                float left = drawingBounds.left + column * cell;
                float top = drawingBounds.top + row * cell;
                boolean rising = (stableHash(column, row) & 1) == 0;
                detailPath.reset();
                detailPath.moveTo(left, top);
                if (rising) {
                    detailPath.lineTo(left + cell, top);
                    detailPath.lineTo(left, top + cell);
                } else {
                    detailPath.lineTo(left + cell, top + cell);
                    detailPath.lineTo(left, top + cell);
                }
                detailPath.close();
                canvas.drawPath(detailPath, detailPaint);

                detailPath.reset();
                if (rising) {
                    detailPath.moveTo(left + cell, top);
                    detailPath.lineTo(left + cell, top + cell);
                    detailPath.lineTo(left, top + cell);
                } else {
                    detailPath.moveTo(left, top);
                    detailPath.lineTo(left + cell, top);
                    detailPath.lineTo(left + cell, top + cell);
                }
                detailPath.close();
                canvas.drawPath(detailPath, secondaryPaint);
            }
        }
        configurePaint(detailPaint, primary, 64, Paint.Style.STROKE);
        detailPaint.setStrokeWidth(dp(0.75f));
        for (int row = 0; row <= rows; row++) {
            float y = drawingBounds.top + row * cell;
            canvas.drawLine(drawingBounds.left, y, drawingBounds.right, y, detailPaint);
        }
        for (int column = 0; column <= columns; column++) {
            float x = drawingBounds.left + column * cell;
            canvas.drawLine(x, drawingBounds.top, x, drawingBounds.bottom, detailPaint);
        }
    }

    private void drawGlassTiles(Canvas canvas, int primary, int highlight) {
        float tileWidth = dp(31f);
        float tileHeight = dp(24f);
        float gap = dp(5f);
        float stepX = tileWidth + gap;
        float stepY = tileHeight + gap;
        int rows = (int) Math.ceil(drawingBounds.height() / stepY) + 1;
        int columns = (int) Math.ceil(drawingBounds.width() / stepX) + 2;
        for (int row = 0; row < rows; row++) {
            float offset = (row & 1) == 0 ? 0f : -stepX * 0.5f;
            for (int column = 0; column < columns; column++) {
                float left = drawingBounds.left + offset + column * stepX;
                float top = drawingBounds.top + row * stepY;
                scratchRect.set(left, top, left + tileWidth, top + tileHeight);
                int variation = 22 + (stableHash(column, row) & 0x0F);
                configurePaint(detailPaint, primary, variation, Paint.Style.FILL);
                canvas.drawRoundRect(scratchRect, dp(4f), dp(4f), detailPaint);
                configurePaint(secondaryPaint, highlight, 70, Paint.Style.STROKE);
                secondaryPaint.setStrokeWidth(dp(0.8f));
                canvas.drawRoundRect(scratchRect, dp(4f), dp(4f), secondaryPaint);
                configurePaint(secondaryPaint, highlight, 72, Paint.Style.STROKE);
                secondaryPaint.setStrokeWidth(dp(0.7f));
                canvas.drawLine(
                    left + dp(4f),
                    top + dp(4f),
                    left + tileWidth - dp(5f),
                    top + dp(4f),
                    secondaryPaint
                );
            }
        }
    }

    private void drawDots(Canvas canvas, int primary, int secondary) {
        float spacing = dp(18f);
        int rows = (int) Math.ceil(drawingBounds.height() / spacing) + 1;
        int columns = (int) Math.ceil(drawingBounds.width() / spacing) + 1;
        for (int row = 0; row < rows; row++) {
            float offset = (row & 1) == 0 ? 0f : spacing * 0.5f;
            for (int column = 0; column < columns; column++) {
                float x = drawingBounds.left + offset + column * spacing;
                float y = drawingBounds.top + row * spacing;
                int hash = stableHash(column, row);
                float radius = dp((hash & 7) == 0 ? 2.6f : 1.65f);
                configurePaint(
                    detailPaint,
                    (hash & 3) == 0 ? secondary : primary,
                    (hash & 7) == 0 ? 100 : 76,
                    Paint.Style.FILL
                );
                canvas.drawCircle(x, y, radius, detailPaint);
            }
        }
    }

    private void drawContours(Canvas canvas, int primary, int secondary) {
        float spacing = dp(18f);
        float period = dp(50f);
        float amplitude = dp(4.5f);
        int rows = (int) Math.ceil(drawingBounds.height() / spacing) + 2;
        configurePaint(detailPaint, primary, 78, Paint.Style.STROKE);
        detailPaint.setStrokeWidth(dp(1f));
        detailPaint.setStrokeCap(Paint.Cap.ROUND);
        for (int row = -1; row < rows; row++) {
            float baseline = drawingBounds.top + row * spacing;
            float phase = Math.floorMod(stableHash(row, 0), (int) Math.max(1f, period));
            float start = drawingBounds.left - period + phase;
            detailPath.reset();
            detailPath.moveTo(start, baseline);
            for (float x = start; x < drawingBounds.right + period; x += period) {
                detailPath.quadTo(
                    x + period * 0.25f,
                    baseline - amplitude,
                    x + period * 0.5f,
                    baseline
                );
                detailPath.quadTo(
                    x + period * 0.75f,
                    baseline + amplitude,
                    x + period,
                    baseline
                );
            }
            canvas.drawPath(detailPath, detailPaint);
        }
        configurePaint(secondaryPaint, secondary, 46, Paint.Style.STROKE);
        secondaryPaint.setStrokeWidth(dp(0.75f));
        for (int row = 0; row < rows; row += 2) {
            float y = drawingBounds.top + row * spacing + spacing * 0.5f;
            canvas.drawLine(drawingBounds.left, y, drawingBounds.right, y, secondaryPaint);
        }
    }

    private void configurePaint(Paint paint, int color, int alpha, Paint.Style style) {
        paint.setColor(color);
        paint.setAlpha(Math.round(clampByte(alpha) * drawableAlpha / 255f));
        paint.setStyle(style);
        paint.setColorFilter(colorFilter);
    }

    private int stableHash(int x, int y) {
        int value = patternSeed;
        value ^= x * 0x45D9F3B;
        value = Integer.rotateLeft(value, 13);
        value ^= y * 0x119DE1F3;
        value ^= value >>> 16;
        value *= 0x7FEB352D;
        return value ^ (value >>> 15);
    }

    private float dp(float value) {
        return value * density;
    }

    @Override
    public void setAlpha(int alpha) {
        int safeAlpha = clampByte(alpha);
        if (drawableAlpha != safeAlpha) {
            drawableAlpha = safeAlpha;
            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
        return drawableAlpha;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        if (this.colorFilter != colorFilter) {
            this.colorFilter = colorFilter;
            invalidateSelf();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getOpacity() {
        // The pixels outside the rounded outline remain transparent even at full drawable alpha.
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void getOutline(Outline outline) {
        outline.setRoundRect(getBounds(), cornerRadiusPx);
    }

    private static int clampByte(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
