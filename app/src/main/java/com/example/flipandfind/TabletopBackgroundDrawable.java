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
            case HONEYCOMB:
                drawHoneycomb(canvas, primary, secondary);
                break;
            case GLASS:
                drawGlass(canvas, primary, highlight);
                break;
            case REFLECTION:
                drawReflection(canvas, primary, secondary, highlight);
                break;
            case PAPER:
                drawPaper(canvas, primary, secondary);
                break;
            case MIRROR:
                drawMirror(canvas, primary, secondary, highlight);
                break;
            case WATER:
                drawWater(canvas, primary, secondary);
                break;
            case RETRO:
                drawRetro(canvas, primary, secondary);
                break;
            case NEON:
                drawNeon(canvas, primary, highlight);
                break;
            case FELT:
                drawFelt(canvas, primary, secondary);
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

    private void drawHoneycomb(Canvas canvas, int primary, int secondary) {
        float radius = dp(15f);
        float halfHeight = radius * 0.8660254f;
        float columnStep = radius * 1.5f;
        float rowStep = halfHeight * 2f;
        int columns = (int) Math.ceil(drawingBounds.width() / columnStep) + 3;
        int rows = (int) Math.ceil(drawingBounds.height() / rowStep) + 3;

        configurePaint(detailPaint, secondary, 27, Paint.Style.FILL);
        configurePaint(secondaryPaint, primary, 86, Paint.Style.STROKE);
        secondaryPaint.setStrokeWidth(dp(0.9f));
        secondaryPaint.setStrokeJoin(Paint.Join.ROUND);
        for (int column = -1; column < columns; column++) {
            float centerX = drawingBounds.left + column * columnStep;
            float offsetY = (column & 1) == 0 ? 0f : halfHeight;
            for (int row = -1; row < rows; row++) {
                float centerY = drawingBounds.top + offsetY + row * rowStep;
                setHexagonPath(centerX, centerY, radius, halfHeight);
                if ((stableHash(column, row) & 3) == 0) {
                    canvas.drawPath(detailPath, detailPaint);
                }
                canvas.drawPath(detailPath, secondaryPaint);
            }
        }
    }

    private void setHexagonPath(float centerX, float centerY, float radius, float halfHeight) {
        detailPath.reset();
        detailPath.moveTo(centerX + radius, centerY);
        detailPath.lineTo(centerX + radius * 0.5f, centerY + halfHeight);
        detailPath.lineTo(centerX - radius * 0.5f, centerY + halfHeight);
        detailPath.lineTo(centerX - radius, centerY);
        detailPath.lineTo(centerX - radius * 0.5f, centerY - halfHeight);
        detailPath.lineTo(centerX + radius * 0.5f, centerY - halfHeight);
        detailPath.close();
    }

    private void drawGlass(Canvas canvas, int primary, int highlight) {
        float paneWidth = dp(76f);
        float paneHeight = dp(55f);
        float horizontalGap = dp(10f);
        float verticalGap = dp(9f);
        float stepX = paneWidth + horizontalGap;
        float stepY = paneHeight + verticalGap;
        float slant = dp(9f);
        int columns = (int) Math.ceil(drawingBounds.width() / stepX) + 3;
        int rows = (int) Math.ceil(drawingBounds.height() / stepY) + 2;

        configurePaint(secondaryPaint, highlight, 92, Paint.Style.STROKE);
        secondaryPaint.setStrokeWidth(dp(1f));
        secondaryPaint.setStrokeJoin(Paint.Join.ROUND);
        for (int row = -1; row < rows; row++) {
            float rowOffset = (row & 1) == 0 ? 0f : -stepX * 0.46f;
            for (int column = -1; column < columns; column++) {
                float left = drawingBounds.left + rowOffset + column * stepX;
                float top = drawingBounds.top + row * stepY;
                detailPath.reset();
                detailPath.moveTo(left + slant, top);
                detailPath.lineTo(left + paneWidth, top);
                detailPath.lineTo(left + paneWidth - slant, top + paneHeight);
                detailPath.lineTo(left, top + paneHeight);
                detailPath.close();

                int variation = 24 + (stableHash(column, row) & 0x17);
                configurePaint(detailPaint, primary, variation, Paint.Style.FILL);
                canvas.drawPath(detailPath, detailPaint);
                canvas.drawPath(detailPath, secondaryPaint);

                configurePaint(detailPaint, highlight, 104, Paint.Style.STROKE);
                detailPaint.setStrokeWidth(dp(1.15f));
                canvas.drawLine(
                    left + slant + dp(5f),
                    top + dp(5f),
                    left + paneWidth - dp(7f),
                    top + dp(5f),
                    detailPaint
                );
                configurePaint(detailPaint, highlight, 44, Paint.Style.STROKE);
                detailPaint.setStrokeWidth(dp(3f));
                canvas.drawLine(
                    left + paneWidth - dp(13f),
                    top + dp(10f),
                    left + paneWidth - slant - dp(10f),
                    top + paneHeight - dp(8f),
                    detailPaint
                );
            }
        }
    }

    private void drawReflection(Canvas canvas, int primary, int secondary, int highlight) {
        float width = drawingBounds.width();
        float height = drawingBounds.height();
        float horizon = drawingBounds.top + height * 0.46f;
        float rayWidth = Math.max(dp(34f), width * 0.12f);
        float seedOffset = Math.floorMod(stableHash(3, 5), 37) * dp(0.6f);

        configurePaint(detailPaint, highlight, 24, Paint.Style.FILL);
        for (int ray = -2; ray < 5; ray++) {
            float startX = drawingBounds.left + ray * rayWidth * 1.9f + seedOffset;
            detailPath.reset();
            detailPath.moveTo(startX, drawingBounds.top);
            detailPath.lineTo(startX + rayWidth, drawingBounds.top);
            detailPath.lineTo(startX + rayWidth * 2.8f, drawingBounds.bottom);
            detailPath.lineTo(startX + rayWidth * 1.65f, drawingBounds.bottom);
            detailPath.close();
            canvas.drawPath(detailPath, detailPaint);
        }

        configurePaint(detailPaint, primary, 100, Paint.Style.STROKE);
        detailPaint.setStrokeWidth(dp(1.15f));
        detailPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(
            drawingBounds.left,
            horizon,
            drawingBounds.right,
            horizon,
            detailPaint
        );

        configurePaint(secondaryPaint, secondary, 68, Paint.Style.STROKE);
        secondaryPaint.setStrokeWidth(dp(1f));
        secondaryPaint.setStrokeCap(Paint.Cap.ROUND);
        float spacing = dp(16f);
        int rippleCount = (int) Math.ceil(height / (spacing * 2f)) + 1;
        for (int ripple = 1; ripple <= rippleCount; ripple++) {
            float distance = ripple * spacing;
            float inset = Math.min(width * 0.34f, distance * 0.78f);
            detailPath.reset();
            detailPath.moveTo(drawingBounds.left + inset, horizon - distance);
            detailPath.quadTo(
                drawingBounds.centerX(),
                horizon - distance - dp(5f),
                drawingBounds.right - inset,
                horizon - distance
            );
            canvas.drawPath(detailPath, secondaryPaint);
            detailPath.reset();
            detailPath.moveTo(drawingBounds.left + inset, horizon + distance);
            detailPath.quadTo(
                drawingBounds.centerX(),
                horizon + distance + dp(5f),
                drawingBounds.right - inset,
                horizon + distance
            );
            canvas.drawPath(detailPath, secondaryPaint);
        }
    }

    private void drawPaper(Canvas canvas, int primary, int secondary) {
        float ruleSpacing = dp(19f);
        int ruleCount = (int) Math.ceil(drawingBounds.height() / ruleSpacing) + 1;
        configurePaint(secondaryPaint, secondary, 18, Paint.Style.STROKE);
        secondaryPaint.setStrokeWidth(dp(0.5f));
        for (int rule = 1; rule < ruleCount; rule++) {
            float y = drawingBounds.top + rule * ruleSpacing;
            canvas.drawLine(drawingBounds.left, y, drawingBounds.right, y, secondaryPaint);
        }

        float areaDp = drawingBounds.width() * drawingBounds.height() / (density * density);
        int fiberCount = Math.max(30, Math.min(160, (int) (areaDp / 140f)));
        int widthPixels = Math.max(1, (int) drawingBounds.width());
        int heightPixels = Math.max(1, (int) drawingBounds.height());
        configurePaint(detailPaint, primary, 29, Paint.Style.STROKE);
        detailPaint.setStrokeWidth(dp(0.55f));
        detailPaint.setStrokeCap(Paint.Cap.ROUND);
        configurePaint(secondaryPaint, secondary, 20, Paint.Style.STROKE);
        secondaryPaint.setStrokeWidth(dp(0.45f));
        secondaryPaint.setStrokeCap(Paint.Cap.ROUND);
        for (int fiber = 0; fiber < fiberCount; fiber++) {
            int hash = stableHash(fiber, 0x50A);
            int otherHash = stableHash(0x137, fiber);
            float x = drawingBounds.left + Math.floorMod(hash, widthPixels);
            float y = drawingBounds.top + Math.floorMod(otherHash, heightPixels);
            float length = dp(4f + ((hash >>> 8) & 7));
            float lift = dp((((otherHash >>> 9) & 7) - 3) * 0.18f);
            canvas.drawLine(
                x,
                y,
                x + length,
                y + lift,
                (hash & 1) == 0 ? detailPaint : secondaryPaint
            );
        }
    }

    private void drawMirror(Canvas canvas, int primary, int secondary, int highlight) {
        float left = drawingBounds.left;
        float top = drawingBounds.top;
        float right = drawingBounds.right;
        float bottom = drawingBounds.bottom;
        float centerX = drawingBounds.centerX();
        float centerY = drawingBounds.centerY();

        configurePaint(detailPaint, primary, 25, Paint.Style.FILL);
        configurePaint(secondaryPaint, secondary, 19, Paint.Style.FILL);
        drawTriangle(canvas, detailPaint, left, top, centerX, top, centerX, centerY);
        drawTriangle(canvas, secondaryPaint, left, top, centerX, centerY, left, centerY);
        drawTriangle(canvas, secondaryPaint, centerX, top, right, top, centerX, centerY);
        drawTriangle(canvas, detailPaint, right, top, right, centerY, centerX, centerY);
        drawTriangle(canvas, detailPaint, left, centerY, centerX, centerY, left, bottom);
        drawTriangle(canvas, secondaryPaint, left, bottom, centerX, centerY, centerX, bottom);
        drawTriangle(canvas, secondaryPaint, centerX, centerY, right, centerY, right, bottom);
        drawTriangle(canvas, detailPaint, centerX, centerY, right, bottom, centerX, bottom);

        configurePaint(detailPaint, highlight, 22, Paint.Style.FILL);
        float bandHalfWidth = dp(7f);
        canvas.drawRect(
            centerX - bandHalfWidth,
            top,
            centerX + bandHalfWidth,
            bottom,
            detailPaint
        );
        configurePaint(detailPaint, highlight, 32, Paint.Style.STROKE);
        detailPaint.setStrokeWidth(dp(3.4f));
        detailPaint.setStrokeCap(Paint.Cap.ROUND);
        drawMirrorSeams(canvas, detailPaint, centerX, centerY);
        configurePaint(secondaryPaint, primary, 112, Paint.Style.STROKE);
        secondaryPaint.setStrokeWidth(dp(0.85f));
        secondaryPaint.setStrokeCap(Paint.Cap.ROUND);
        drawMirrorSeams(canvas, secondaryPaint, centerX, centerY);
    }

    private void drawTriangle(
        Canvas canvas,
        Paint paint,
        float firstX,
        float firstY,
        float secondX,
        float secondY,
        float thirdX,
        float thirdY
    ) {
        detailPath.reset();
        detailPath.moveTo(firstX, firstY);
        detailPath.lineTo(secondX, secondY);
        detailPath.lineTo(thirdX, thirdY);
        detailPath.close();
        canvas.drawPath(detailPath, paint);
    }

    private void drawMirrorSeams(Canvas canvas, Paint paint, float centerX, float centerY) {
        canvas.drawLine(centerX, centerY, drawingBounds.left, drawingBounds.top, paint);
        canvas.drawLine(centerX, centerY, centerX, drawingBounds.top, paint);
        canvas.drawLine(centerX, centerY, drawingBounds.right, drawingBounds.top, paint);
        canvas.drawLine(centerX, centerY, drawingBounds.right, centerY, paint);
        canvas.drawLine(centerX, centerY, drawingBounds.right, drawingBounds.bottom, paint);
        canvas.drawLine(centerX, centerY, centerX, drawingBounds.bottom, paint);
        canvas.drawLine(centerX, centerY, drawingBounds.left, drawingBounds.bottom, paint);
        canvas.drawLine(centerX, centerY, drawingBounds.left, centerY, paint);
    }

    private void drawWater(Canvas canvas, int primary, int secondary) {
        float spacing = dp(15f);
        float period = dp(44f);
        float amplitude = dp(3.8f);
        int rows = (int) Math.ceil(drawingBounds.height() / spacing) + 2;
        configurePaint(detailPaint, primary, 27, Paint.Style.STROKE);
        detailPaint.setStrokeWidth(dp(4f));
        detailPaint.setStrokeCap(Paint.Cap.ROUND);
        configurePaint(secondaryPaint, secondary, 88, Paint.Style.STROKE);
        secondaryPaint.setStrokeWidth(dp(1f));
        secondaryPaint.setStrokeCap(Paint.Cap.ROUND);
        for (int row = -1; row < rows; row++) {
            float baseline = drawingBounds.top + row * spacing;
            float phase = Math.floorMod(stableHash(row, 0xA7), (int) period);
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
            canvas.drawPath(detailPath, secondaryPaint);
        }

        float areaDp = drawingBounds.width() * drawingBounds.height() / (density * density);
        int rippleCount = Math.max(5, Math.min(16, (int) (areaDp / 14000f) + 5));
        int widthPixels = Math.max(1, (int) drawingBounds.width());
        int heightPixels = Math.max(1, (int) drawingBounds.height());
        configurePaint(detailPaint, primary, 72, Paint.Style.STROKE);
        detailPaint.setStrokeWidth(dp(0.85f));
        for (int ripple = 0; ripple < rippleCount; ripple++) {
            int hash = stableHash(ripple, 0x57A);
            int otherHash = stableHash(0x611, ripple);
            float x = drawingBounds.left + Math.floorMod(hash, widthPixels);
            float y = drawingBounds.top + Math.floorMod(otherHash, heightPixels);
            float radiusX = dp(4f + ((hash >>> 8) & 7));
            float radiusY = radiusX * 0.36f;
            scratchRect.set(x - radiusX, y - radiusY, x + radiusX, y + radiusY);
            canvas.drawOval(scratchRect, detailPaint);
            if ((hash & 3) == 0) {
                scratchRect.inset(radiusX * 0.34f, radiusY * 0.34f);
                canvas.drawOval(scratchRect, detailPaint);
            }
        }
    }

    private void drawRetro(Canvas canvas, int primary, int secondary) {
        float cell = dp(18f);
        int columns = (int) Math.ceil(drawingBounds.width() / cell) + 1;
        int rows = (int) Math.ceil(drawingBounds.height() / cell) + 1;
        configurePaint(detailPaint, primary, 42, Paint.Style.FILL);
        configurePaint(secondaryPaint, secondary, 29, Paint.Style.FILL);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                float left = drawingBounds.left + column * cell;
                float top = drawingBounds.top + row * cell;
                canvas.drawRect(
                    left,
                    top,
                    left + cell,
                    top + cell,
                    ((column + row) & 1) == 0 ? detailPaint : secondaryPaint
                );
            }
        }

        configurePaint(detailPaint, primary, 25, Paint.Style.STROKE);
        detailPaint.setStrokeWidth(dp(0.65f));
        float scanlineSpacing = dp(5f);
        int scanlineCount = (int) Math.ceil(drawingBounds.height() / scanlineSpacing);
        for (int line = 1; line < scanlineCount; line++) {
            float y = drawingBounds.top + line * scanlineSpacing;
            canvas.drawLine(drawingBounds.left, y, drawingBounds.right, y, detailPaint);
        }
    }

    private void drawNeon(Canvas canvas, int primary, int highlight) {
        float rowSpacing = dp(29f);
        float segment = dp(46f);
        int rows = (int) Math.ceil(drawingBounds.height() / rowSpacing) + 2;
        int columns = (int) Math.ceil(drawingBounds.width() / segment) + 2;
        configurePaint(detailPaint, highlight, 34, Paint.Style.STROKE);
        detailPaint.setStrokeWidth(dp(6f));
        detailPaint.setStrokeCap(Paint.Cap.ROUND);
        detailPaint.setStrokeJoin(Paint.Join.ROUND);
        configurePaint(secondaryPaint, primary, 142, Paint.Style.STROKE);
        secondaryPaint.setStrokeWidth(dp(1.25f));
        secondaryPaint.setStrokeCap(Paint.Cap.ROUND);
        secondaryPaint.setStrokeJoin(Paint.Join.ROUND);
        for (int row = -1; row < rows; row++) {
            float baseline = drawingBounds.top + row * rowSpacing;
            detailPath.reset();
            detailPath.moveTo(drawingBounds.left - segment, baseline);
            for (int column = -1; column < columns; column++) {
                float left = drawingBounds.left + column * segment;
                int hash = stableHash(column, row);
                float offset = dp(5f + ((hash >>> 7) & 3));
                if ((hash & 1) != 0) {
                    offset = -offset;
                }
                detailPath.lineTo(left + segment * 0.34f, baseline);
                detailPath.lineTo(left + segment * 0.50f, baseline + offset);
                detailPath.lineTo(left + segment * 0.76f, baseline + offset);
                detailPath.lineTo(left + segment * 0.90f, baseline);
                detailPath.lineTo(left + segment, baseline);
            }
            canvas.drawPath(detailPath, detailPaint);
            canvas.drawPath(detailPath, secondaryPaint);
        }

        configurePaint(detailPaint, highlight, 46, Paint.Style.FILL);
        configurePaint(secondaryPaint, primary, 176, Paint.Style.FILL);
        for (int row = 0; row < rows; row++) {
            float baseline = drawingBounds.top + row * rowSpacing;
            for (int column = 0; column < columns; column++) {
                int hash = stableHash(column, row);
                if ((hash & 3) != 0) {
                    continue;
                }
                float x = drawingBounds.left + column * segment + segment * 0.5f;
                float offset = dp(5f + ((hash >>> 7) & 3));
                float y = baseline + ((hash & 1) == 0 ? offset : -offset);
                canvas.drawCircle(x, y, dp(4.2f), detailPaint);
                canvas.drawCircle(x, y, dp(1.45f), secondaryPaint);
            }
        }
    }

    private void drawFelt(Canvas canvas, int primary, int secondary) {
        float areaDp = drawingBounds.width() * drawingBounds.height() / (density * density);
        int fiberCount = Math.max(70, Math.min(240, (int) (areaDp / 65f)));
        int widthPixels = Math.max(1, (int) drawingBounds.width());
        int heightPixels = Math.max(1, (int) drawingBounds.height());
        configurePaint(detailPaint, primary, 34, Paint.Style.STROKE);
        detailPaint.setStrokeWidth(dp(0.65f));
        detailPaint.setStrokeCap(Paint.Cap.ROUND);
        configurePaint(secondaryPaint, secondary, 22, Paint.Style.STROKE);
        secondaryPaint.setStrokeWidth(dp(0.5f));
        secondaryPaint.setStrokeCap(Paint.Cap.ROUND);
        for (int fiber = 0; fiber < fiberCount; fiber++) {
            int hash = stableHash(fiber, 0xFE17);
            int otherHash = stableHash(0xFA81, fiber);
            float x = drawingBounds.left + Math.floorMod(hash, widthPixels);
            float y = drawingBounds.top + Math.floorMod(otherHash, heightPixels);
            int vectorX = ((hash >>> 6) & 7) - 3;
            int vectorY = ((hash >>> 10) & 7) - 3;
            if (vectorX == 0 && vectorY == 0) {
                vectorX = 1;
            }
            float scale = dp(2f + ((otherHash >>> 8) & 3))
                / Math.max(1, Math.max(Math.abs(vectorX), Math.abs(vectorY)));
            canvas.drawLine(
                x,
                y,
                x + vectorX * scale,
                y + vectorY * scale,
                (hash & 1) == 0 ? detailPaint : secondaryPaint
            );
        }

        scratchRect.set(drawingBounds);
        scratchRect.inset(dp(8f), dp(8f));
        configurePaint(secondaryPaint, primary, 52, Paint.Style.STROKE);
        secondaryPaint.setStrokeWidth(dp(0.8f));
        canvas.drawRoundRect(
            scratchRect,
            Math.max(0f, cornerRadiusPx - dp(4f)),
            Math.max(0f, cornerRadiusPx - dp(4f)),
            secondaryPaint
        );
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
