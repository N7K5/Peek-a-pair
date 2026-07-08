package com.example.flipandfind;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Allocation-free-at-draw-time renderer for the live game's themed chrome. */
public final class GameThemeChromeDrawable extends Drawable {
    public enum Region {
        SURROUND,
        HEADER,
        BOARD_FRAME
    }

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint detailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF drawingBounds = new RectF();
    private final RectF insetBounds = new RectF();
    private final float density;
    private final Region region;

    private GameThemeChrome.Treatment treatment;
    private Shader fillShader;
    private boolean shaderDirty = true;
    private int drawableAlpha = 255;
    private ColorFilter colorFilter;

    public GameThemeChromeDrawable(
        float density,
        Region region,
        GameThemeChrome.Treatment treatment
    ) {
        this.density = Math.max(0.5f, density);
        this.region = region == null ? Region.SURROUND : region;
        this.treatment = requireTreatment(treatment);
        borderPaint.setStyle(Paint.Style.STROKE);
    }

    /** Updates player/theme colors without replacing the drawable or flashing the View. */
    public void setTreatment(GameThemeChrome.Treatment treatment) {
        GameThemeChrome.Treatment safeTreatment = requireTreatment(treatment);
        if (this.treatment == safeTreatment) {
            return;
        }
        this.treatment = safeTreatment;
        shaderDirty = true;
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        shaderDirty = true;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) {
            return;
        }
        drawingBounds.set(bounds);
        ensureShader();

        float radius = region == Region.SURROUND
            ? 0f
            : dp(18f) * treatment.getCornerScale();
        configurePaint(fillPaint, Color.WHITE, panelAlpha(), Paint.Style.FILL);
        fillPaint.setShader(fillShader);
        if (region == Region.SURROUND) {
            canvas.drawRect(drawingBounds, fillPaint);
        } else {
            canvas.drawRoundRect(drawingBounds, radius, radius, fillPaint);
            if (region == Region.HEADER && radius > 0f) {
                // Keep the screen edge square while retaining theme-shaped lower corners.
                canvas.drawRect(
                    drawingBounds.left,
                    drawingBounds.top,
                    drawingBounds.right,
                    Math.min(drawingBounds.bottom, drawingBounds.top + radius),
                    fillPaint
                );
            }
        }
        fillPaint.setShader(null);

        drawDecoration(canvas);
        if (region != Region.SURROUND) {
            drawOutline(canvas, radius);
        }
        if (region == Region.HEADER) {
            configurePaint(
                detailPaint,
                treatment.getPlayerAccentColor(),
                255,
                Paint.Style.FILL
            );
            float ruleHeight = dp(treatment.getDecoration()
                == GameThemeChrome.Decoration.BUBBLES ? 5f : 3f);
            canvas.drawRect(
                drawingBounds.left,
                drawingBounds.bottom - ruleHeight,
                drawingBounds.right,
                drawingBounds.bottom,
                detailPaint
            );
        }
    }

    private void ensureShader() {
        if (!shaderDirty && fillShader != null) {
            return;
        }
        shaderDirty = false;
        if (region == Region.SURROUND) {
            fillShader = new LinearGradient(
                drawingBounds.left,
                drawingBounds.top,
                drawingBounds.right,
                drawingBounds.bottom,
                treatment.copySurroundColors(),
                new float[] {0f, 0.38f, 1f},
                Shader.TileMode.CLAMP
            );
            return;
        }
        int base = region == Region.HEADER
            ? treatment.getHeaderColor()
            : treatment.getBoardFrameColor();
        float blendAmount;
        switch (treatment.getDecoration()) {
            case GLASS_GLOW:
                blendAmount = 0.22f;
                break;
            case BUBBLES:
                blendAmount = 0.14f;
                break;
            case ARCADE_GRID:
                blendAmount = 0.10f;
                break;
            default:
                blendAmount = 0.04f;
                break;
        }
        int finish = GameSurfaceColors.blend(
            base,
            treatment.getSurroundEnd(),
            blendAmount
        );
        fillShader = new LinearGradient(
            drawingBounds.left,
            drawingBounds.top,
            drawingBounds.right,
            drawingBounds.bottom,
            base,
            finish,
            Shader.TileMode.CLAMP
        );
    }

    private void drawDecoration(Canvas canvas) {
        switch (treatment.getDecoration()) {
            case HAIRLINE:
                drawHairlines(canvas);
                break;
            case GLASS_GLOW:
                drawGlassGlow(canvas);
                break;
            case ARCADE_GRID:
                drawArcadeGrid(canvas);
                break;
            case PAPER_GRAIN:
                drawPaperGrain(canvas);
                break;
            case BUBBLES:
                drawBubbles(canvas);
                break;
            case NONE:
            default:
                break;
        }
    }

    private void drawHairlines(Canvas canvas) {
        configurePaint(
            detailPaint,
            treatment.getDecorationColor(),
            region == Region.SURROUND ? 46 : 78,
            Paint.Style.STROKE
        );
        detailPaint.setStrokeWidth(dp(1f));
        float step = Math.max(dp(28f), drawingBounds.height() / 5f);
        for (float y = drawingBounds.top + step; y < drawingBounds.bottom; y += step) {
            canvas.drawLine(drawingBounds.left, y, drawingBounds.right, y, detailPaint);
        }
    }

    private void drawGlassGlow(Canvas canvas) {
        configurePaint(
            detailPaint,
            treatment.getDecorationColor(),
            region == Region.SURROUND ? 46 : 66,
            Paint.Style.FILL
        );
        float width = drawingBounds.width();
        float height = drawingBounds.height();
        canvas.drawCircle(
            drawingBounds.left + width * 0.16f,
            drawingBounds.top + height * 0.20f,
            Math.min(width, height) * 0.24f,
            detailPaint
        );
        configurePaint(detailPaint, Color.WHITE, 42, Paint.Style.STROKE);
        detailPaint.setStrokeWidth(dp(2f));
        canvas.drawLine(
            drawingBounds.left + width * 0.10f,
            drawingBounds.top + height * 0.12f,
            drawingBounds.left + width * 0.72f,
            drawingBounds.top + height * 0.88f,
            detailPaint
        );
    }

    private void drawArcadeGrid(Canvas canvas) {
        configurePaint(
            detailPaint,
            treatment.getDecorationColor(),
            region == Region.SURROUND ? 54 : 82,
            Paint.Style.STROKE
        );
        detailPaint.setStrokeWidth(dp(1f));
        float step = dp(region == Region.SURROUND ? 34f : 18f);
        for (float x = drawingBounds.left + step; x < drawingBounds.right; x += step) {
            canvas.drawLine(x, drawingBounds.top, x, drawingBounds.bottom, detailPaint);
        }
        for (float y = drawingBounds.top + step; y < drawingBounds.bottom; y += step) {
            canvas.drawLine(drawingBounds.left, y, drawingBounds.right, y, detailPaint);
        }
    }

    private void drawPaperGrain(Canvas canvas) {
        configurePaint(
            detailPaint,
            treatment.getDecorationColor(),
            region == Region.SURROUND ? 38 : 58,
            Paint.Style.STROKE
        );
        detailPaint.setStrokeWidth(dp(0.8f));
        float step = dp(13f);
        int line = 0;
        for (float y = drawingBounds.top + step; y < drawingBounds.bottom; y += step) {
            float inset = dp((line++ % 3) * 5f);
            canvas.drawLine(
                drawingBounds.left + inset,
                y,
                drawingBounds.right - inset,
                y + ((line & 1) == 0 ? dp(0.7f) : 0f),
                detailPaint
            );
        }
    }

    private void drawBubbles(Canvas canvas) {
        configurePaint(
            detailPaint,
            treatment.getDecorationColor(),
            region == Region.SURROUND ? 48 : 68,
            Paint.Style.STROKE
        );
        detailPaint.setStrokeWidth(dp(1.4f));
        float width = drawingBounds.width();
        float height = drawingBounds.height();
        float base = Math.max(dp(7f), Math.min(width, height) * 0.055f);
        for (int index = 0; index < 8; index++) {
            float x = drawingBounds.left + width * (((index * 37 + 11) % 91) / 100f);
            float y = drawingBounds.top + height * (((index * 53 + 17) % 89) / 100f);
            float radius = base * (0.70f + (index % 3) * 0.35f);
            canvas.drawCircle(x, y, radius, detailPaint);
        }
    }

    private void drawOutline(Canvas canvas, float radius) {
        if (region == Region.HEADER
            && (treatment.getDecoration() == GameThemeChrome.Decoration.NONE
                || treatment.getDecoration() == GameThemeChrome.Decoration.HAIRLINE)) {
            return;
        }
        int borderColor = region == Region.HEADER
            ? treatment.getHeaderOutlineColor()
            : treatment.getBoardFrameBorderColor();
        configurePaint(borderPaint, borderColor, 255, Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(Math.max(1, treatment.getStrokeWidthDp())));
        float inset = borderPaint.getStrokeWidth() * 0.5f;
        insetBounds.set(drawingBounds);
        insetBounds.inset(inset, inset);
        canvas.drawRoundRect(
            insetBounds,
            Math.max(0f, radius - inset),
            Math.max(0f, radius - inset),
            borderPaint
        );
    }

    private int panelAlpha() {
        return region == Region.BOARD_FRAME ? treatment.getPanelAlpha() : 255;
    }

    private void configurePaint(Paint paint, int color, int sourceAlpha, Paint.Style style) {
        paint.setShader(null);
        paint.setColor(color);
        paint.setAlpha(Math.round(sourceAlpha * (drawableAlpha / 255f)));
        paint.setStyle(style);
        paint.setColorFilter(colorFilter);
    }

    private float dp(float value) {
        return value * density;
    }

    private static GameThemeChrome.Treatment requireTreatment(
        GameThemeChrome.Treatment treatment
    ) {
        if (treatment == null) {
            throw new IllegalArgumentException("A game theme treatment is required");
        }
        return treatment;
    }

    @Override
    public void setAlpha(int alpha) {
        int safeAlpha = Math.max(0, Math.min(255, alpha));
        if (drawableAlpha != safeAlpha) {
            drawableAlpha = safeAlpha;
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        if (this.colorFilter != colorFilter) {
            this.colorFilter = colorFilter;
            invalidateSelf();
        }
    }

    @Override
    public int getOpacity() {
        return region == Region.SURROUND && drawableAlpha == 255
            ? PixelFormat.OPAQUE
            : PixelFormat.TRANSLUCENT;
    }

    @Override
    public void getOutline(Outline outline) {
        Rect bounds = getBounds();
        if (region == Region.SURROUND) {
            outline.setRect(bounds);
            return;
        }
        float radius = dp(18f) * treatment.getCornerScale();
        outline.setRoundRect(bounds, radius);
    }
}
