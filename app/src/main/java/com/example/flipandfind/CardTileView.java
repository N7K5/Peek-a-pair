package com.example.flipandfind;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** Draws card faces, including system emoji, words, numbers, and Rubics patterns. */
public final class CardTileView extends View {
    private static final int FACE = Color.WHITE;
    private static final int MATCHED_FACE = Color.rgb(232, 248, 239);
    private static final int MATCHED_BORDER = Color.rgb(46, 157, 101);

    private static final int[] SYMBOL_COLORS = {
        Color.rgb(91, 75, 219),
        Color.rgb(232, 74, 95),
        Color.rgb(0, 137, 123),
        Color.rgb(245, 158, 11),
        Color.rgb(59, 130, 246),
        Color.rgb(139, 92, 246),
        Color.rgb(217, 119, 6),
        Color.rgb(15, 118, 110),
        Color.rgb(190, 24, 93),
        Color.rgb(71, 85, 105)
    };
    private static final int[] RUBICS_STICKER_COLORS = {
        Color.rgb(246, 247, 249), // White
        Color.rgb(255, 213, 0),   // Yellow
        Color.rgb(210, 38, 48),   // Red
        Color.rgb(255, 122, 0),   // Orange
        Color.rgb(0, 91, 187),    // Blue
        Color.rgb(0, 155, 72)     // Green
    };

    private static final String[] SHAPE_NAMES = {
        "circle", "square", "triangle", "diamond", "star",
        "heart", "hexagon", "plus", "lightning bolt", "flower"
    };
    private static final String[] VARIANT_NAMES = {
        "solid", "outline", "dotted", "striped", "ringed"
    };
    private static final float[] FIREFLY_X = {
        -0.28f, -0.08f, 0.19f, 0.29f, 0.03f, -0.24f, 0.23f
    };
    private static final float[] FIREFLY_Y = {
        -0.23f, -0.31f, -0.17f, 0.08f, 0.29f, 0.22f, 0.31f
    };
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shapePath = new Path();
    private final Path checkPath = new Path();
    private final RectF bounds = new RectF();
    private final RectF patternBounds = new RectF();
    private final Rect animationVisibleBounds = new Rect();
    private final DecelerateInterpolator flipInterpolator = new DecelerateInterpolator();
    private final float[] constellationX = new float[ConstellationPattern.POINT_COUNT];
    private final float[] constellationY = new float[ConstellationPattern.POINT_COUNT];
    private final int[] rubicsStickerIds = new int[9];

    private AnimatorSet activeStateAnimator;
    private Animator activeMidpointAnimator;
    private Animator.AnimatorListener activeMidpointListener;
    private Runnable activeStateFinalizer;
    private long stateAnimationGeneration;

    private int cardNumber;
    private int constellationAccentIndex;
    private OrbitPattern orbitPattern = OrbitPattern.forCard(0);
    private CardBackGeometry cardBackGeometry = CardBackGeometry.forCard(0);
    private int pairId;
    private String iconText = "";
    private String rubicsSpokenDescription = "";
    private boolean wordMode;
    private boolean fallbackToShape;
    private boolean trickyFallback;
    private boolean rubicsMode;
    private boolean numberMode;
    private int pairCount;
    private boolean usePairColor;
    private boolean reducedMotion;
    private boolean highContrast;
    private boolean colorBlindPatterns;
    private float cornerRadiusFraction = 0.13f;
    private int pairColor = Color.rgb(91, 75, 219);
    private CardBackStyle cardBackStyle = CardBackStyle.CLASSIC;
    private GameState.CardState displayedState = GameState.CardState.HIDDEN;

    public CardTileView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);
        setElevation(dp(2));
        setForeground(new RippleDrawable(
            ColorStateList.valueOf(Color.argb(55, 255, 255, 255)),
            null,
            null
        ));
        updateConstellationPattern();
        updateContentDescription();
    }

    public void setCardNumber(int cardNumber) {
        this.cardNumber = cardNumber;
        updateConstellationPattern();
        orbitPattern = OrbitPattern.forCard(cardNumber);
        cardBackGeometry = CardBackGeometry.forCard(cardNumber);
        invalidate();
        updateContentDescription();
    }

    private void updateConstellationPattern() {
        constellationAccentIndex = ConstellationPattern.fill(
            cardNumber,
            constellationX,
            constellationY
        );
    }

    public void setCardBackStyle(CardBackStyle style) {
        cardBackStyle = style == null ? CardBackStyle.CLASSIC : style;
        syncBackAnimationTicker();
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        syncBackAnimationTicker();
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelStateAnimation(true);
        CardBackAnimationTicker.unregister(this);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        syncBackAnimationTicker();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        syncBackAnimationTicker();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        syncBackAnimationTicker();
    }

    boolean shouldStayInBackAnimationTicker() {
        return isAttachedToWindow()
            && getWindowVisibility() == VISIBLE
            && getVisibility() == VISIBLE
            && isShown()
            && !reducedMotion
            && displayedState == GameState.CardState.HIDDEN
            && cardBackStyle != null
            && cardBackStyle.isAnimated();
    }

    boolean isAnimatedBackVisible() {
        return shouldStayInBackAnimationTicker()
            && hasWindowFocus()
            && systemAnimationsEnabled()
            && getGlobalVisibleRect(animationVisibleBounds)
            && !animationVisibleBounds.isEmpty();
    }

    boolean isBackAnimationSuppressedBySystem() {
        return shouldStayInBackAnimationTicker()
            && hasWindowFocus()
            && !systemAnimationsEnabled()
            && getGlobalVisibleRect(animationVisibleBounds)
            && !animationVisibleBounds.isEmpty();
    }

    private void syncBackAnimationTicker() {
        if (shouldStayInBackAnimationTicker()) {
            CardBackAnimationTicker.register(this);
        } else {
            CardBackAnimationTicker.unregister(this);
        }
    }

    private boolean systemAnimationsEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return ValueAnimator.areAnimatorsEnabled();
        }
        try {
            return Settings.Global.getFloat(
                getContext().getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) > 0f;
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    private long backAnimationTimeMillis() {
        return !reducedMotion && systemAnimationsEnabled() ? SystemClock.uptimeMillis() : 0L;
    }

    public void setIcon(String iconText) {
        setContent(iconText, false, false);
    }

    public void setContent(String contentText, boolean wordMode, boolean fallbackToShape) {
        setContent(contentText, wordMode, fallbackToShape, false, 0);
    }

    public void setContent(
        String contentText,
        boolean wordMode,
        boolean fallbackToShape,
        boolean trickyFallback,
        int pairCount
    ) {
        this.iconText = contentText == null ? "" : contentText;
        this.wordMode = wordMode;
        this.fallbackToShape = fallbackToShape;
        this.trickyFallback = fallbackToShape && trickyFallback;
        this.pairCount = Math.max(0, pairCount);
        rubicsMode = RubicsFaceCatalog.isToken(this.iconText);
        numberMode = NumberCatalog.isNumber(this.iconText);
        rubicsSpokenDescription = "";
        if (rubicsMode) {
            int[] decodedColors = RubicsFaceCatalog.decodeColors(this.iconText);
            System.arraycopy(decodedColors, 0, rubicsStickerIds, 0, rubicsStickerIds.length);
            rubicsSpokenDescription = RubicsFaceCatalog.spokenDescription(this.iconText);
        }
        invalidate();
        updateContentDescription();
    }

    /** API 23-safe glyph check used to avoid identical missing-glyph boxes. */
    public static boolean allGlyphsSupported(String[] labels) {
        if (labels == null) {
            return false;
        }
        Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glyphPaint.setTypeface(Typeface.DEFAULT);
        for (String label : labels) {
            if (label == null) {
                return false;
            }
            // Empty labels and custom-drawn Rubics faces do not depend on a font glyph.
            if (!label.isEmpty()
                && !RubicsFaceCatalog.isToken(label)
                && !NumberCatalog.isNumber(label)
                && !glyphPaint.hasGlyph(label)) {
                return false;
            }
        }
        return true;
    }

    public void setPairColor(int pairColor, boolean enabled) {
        this.pairColor = pairColor;
        this.usePairColor = enabled;
        invalidate();
        updateContentDescription();
    }

    /** Suppresses decorative and state-transition motion while retaining immediate state changes. */
    public void setReducedMotion(boolean reducedMotion) {
        if (this.reducedMotion == reducedMotion) {
            return;
        }
        this.reducedMotion = reducedMotion;
        syncBackAnimationTicker();
        invalidate();
    }

    /** Strengthens face separation, borders, and symbol outlines without changing game identity. */
    public void setHighContrast(boolean highContrast) {
        if (this.highContrast == highContrast) {
            return;
        }
        this.highContrast = highContrast;
        invalidate();
    }

    /** Adds a stable non-color cue for every supported pair ID. */
    public void setColorBlindPatterns(boolean colorBlindPatterns) {
        if (this.colorBlindPatterns == colorBlindPatterns) {
            return;
        }
        this.colorBlindPatterns = colorBlindPatterns;
        invalidate();
        updateContentDescription();
    }

    /** Lets the selected app theme alter card silhouette without changing card identity. */
    public void setCornerRadiusFraction(float fraction) {
        float clamped = Math.max(0.03f, Math.min(0.26f, fraction));
        if (cornerRadiusFraction == clamped) {
            return;
        }
        cornerRadiusFraction = clamped;
        invalidate();
    }

    public void showState(int pairId, GameState.CardState state, boolean animate) {
        boolean wasVisiblyOnTable = getVisibility() == VISIBLE && getAlpha() > 0f;
        cancelStateAnimation(false);
        setVisibility(VISIBLE);
        setAlpha(1f);
        setScaleY(1f);
        setEnabled(true);
        setClickable(true);
        setFocusable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        if (!animate || reducedMotion) {
            setScaleX(1f);
            applyDisplayedState(pairId, state);
            return;
        }

        if (!wasVisiblyOnTable) {
            setScaleX(1f);
        }
        float startingScaleX = Math.max(0.06f, getScaleX());
        ObjectAnimator collapse = ObjectAnimator.ofFloat(
            this,
            View.SCALE_X,
            startingScaleX,
            0.06f
        );
        collapse.setDuration(110L);
        collapse.setInterpolator(flipInterpolator);
        ObjectAnimator expand = ObjectAnimator.ofFloat(
            this,
            View.SCALE_X,
            0.06f,
            1f
        );
        expand.setDuration(150L);
        expand.setInterpolator(flipInterpolator);

        AnimatorSet transition = new AnimatorSet();
        transition.playSequentially(collapse, expand);
        long generation = ++stateAnimationGeneration;
        Runnable finalizer = () -> {
            setVisibility(VISIBLE);
            setAlpha(1f);
            setScaleX(1f);
            setScaleY(1f);
            applyDisplayedState(pairId, state);
        };
        AnimatorListenerAdapter midpointListener = new AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelled
                    && generation == stateAnimationGeneration
                    && activeStateAnimator == transition) {
                    applyDisplayedState(pairId, state);
                }
            }
        };
        collapse.addListener(midpointListener);
        transition.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (generation != stateAnimationGeneration
                    || activeStateAnimator != transition) {
                    return;
                }
                clearActiveStateAnimation(transition);
                finalizer.run();
            }
        });
        activeStateAnimator = transition;
        activeMidpointAnimator = collapse;
        activeMidpointListener = midpointListener;
        activeStateFinalizer = finalizer;
        transition.start();
    }

    public void removeFromTable(int pairId, boolean animateRemoval) {
        cancelStateAnimation(false);
        this.pairId = pairId;
        displayedState = GameState.CardState.MATCHED;
        syncBackAnimationTicker();
        setClickable(false);
        setFocusable(false);
        setEnabled(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        updateContentDescription();

        if (!animateRemoval || reducedMotion) {
            setAlpha(0f);
            setScaleX(0.72f);
            setScaleY(0.72f);
            setVisibility(INVISIBLE);
            return;
        }

        float destinationScaleX = Math.min(getScaleX(), 0.62f);
        float destinationScaleY = Math.min(getScaleY(), 0.62f);
        ObjectAnimator fade = ObjectAnimator.ofFloat(this, View.ALPHA, getAlpha(), 0f);
        ObjectAnimator shrinkX = ObjectAnimator.ofFloat(
            this,
            View.SCALE_X,
            getScaleX(),
            destinationScaleX
        );
        ObjectAnimator shrinkY = ObjectAnimator.ofFloat(
            this,
            View.SCALE_Y,
            getScaleY(),
            destinationScaleY
        );
        AnimatorSet removal = new AnimatorSet();
        removal.playTogether(fade, shrinkX, shrinkY);
        removal.setDuration(380L);
        removal.setInterpolator(flipInterpolator);
        long generation = ++stateAnimationGeneration;
        Runnable finalizer = () -> {
            setAlpha(0f);
            setScaleX(destinationScaleX);
            setScaleY(destinationScaleY);
            setVisibility(INVISIBLE);
        };
        removal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (generation != stateAnimationGeneration
                    || activeStateAnimator != removal) {
                    return;
                }
                clearActiveStateAnimation(removal);
                finalizer.run();
            }
        });
        activeStateAnimator = removal;
        activeStateFinalizer = finalizer;
        removal.start();
    }

    private void applyDisplayedState(int pairId, GameState.CardState state) {
        this.pairId = pairId;
        displayedState = state;
        syncBackAnimationTicker();
        invalidate();
        updateContentDescription();
    }

    private void cancelStateAnimation(boolean settleToDestination) {
        stateAnimationGeneration++;
        AnimatorSet animator = activeStateAnimator;
        Animator midpoint = activeMidpointAnimator;
        Animator.AnimatorListener midpointListener = activeMidpointListener;
        Runnable finalizer = activeStateFinalizer;
        activeStateAnimator = null;
        activeMidpointAnimator = null;
        activeMidpointListener = null;
        activeStateFinalizer = null;

        if (midpoint != null && midpointListener != null) {
            midpoint.removeListener(midpointListener);
        }
        if (animator != null) {
            animator.removeAllListeners();
            animator.cancel();
        }
        if (settleToDestination && finalizer != null) {
            finalizer.run();
        }
    }

    private void clearActiveStateAnimation(AnimatorSet animator) {
        if (activeStateAnimator != animator) {
            return;
        }
        if (activeMidpointAnimator != null && activeMidpointListener != null) {
            activeMidpointAnimator.removeListener(activeMidpointListener);
        }
        activeStateAnimator = null;
        activeMidpointAnimator = null;
        activeMidpointListener = null;
        activeStateFinalizer = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.reset();
        paint.setAntiAlias(true);
        float inset = dp(1.5f);
        bounds.set(inset, inset, getWidth() - inset, getHeight() - inset);
        float radius = Math.min(getWidth(), getHeight()) * cornerRadiusFraction;

        if (displayedState == GameState.CardState.HIDDEN) {
            drawCardBack(canvas, radius);
            if (!reducedMotion && cardBackStyle != null && cardBackStyle.isAnimated()) {
                CardBackAnimationTicker.wake();
            }
            return;
        }

        boolean matched = displayedState == GameState.CardState.MATCHED;
        int faceColor = displayedFaceColor(matched);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(faceColor);
        canvas.drawRoundRect(bounds, radius, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(
            matched ? (highContrast ? 3.5f : 2.5f)
                : highContrast ? 4f
                : (usePairColor ? 3.5f : 2f)
        ));
        paint.setColor(
            matched ? MATCHED_BORDER
                : highContrast ? contrastTextColor(faceColor)
                : faceBorderColor()
        );
        canvas.drawRoundRect(bounds, radius, radius, paint);
        if (colorBlindPatterns) {
            drawPairPatternCue(canvas, faceColor);
        }
        drawSymbol(canvas, faceColor);

        if (matched) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(MATCHED_BORDER);
            canvas.drawCircle(getWidth() - dp(10), dp(10), dp(6), paint);
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(dp(1.8f));
            paint.setStyle(Paint.Style.STROKE);
            checkPath.reset();
            checkPath.moveTo(getWidth() - dp(13), dp(10));
            checkPath.lineTo(getWidth() - dp(10.5f), dp(12.5f));
            checkPath.lineTo(getWidth() - dp(7), dp(7.5f));
            canvas.drawPath(checkPath, paint);
        }
    }

    private void drawCardBack(Canvas canvas, float cornerRadius) {
        CardBackStyle style = cardBackStyle == null
            ? CardBackStyle.CLASSIC
            : cardBackStyle;
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float minimum = Math.min(getWidth(), getHeight());
        float size = minimum * 0.22f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(style.getFillColor());
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, paint);

        int patternSaveCount = canvas.save();
        shapePath.reset();
        shapePath.addRoundRect(
            bounds,
            cornerRadius,
            cornerRadius,
            Path.Direction.CW
        );
        canvas.clipPath(shapePath);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        switch (style) {
            case CONSTELLATION:
                drawConstellationBack(canvas, centerX, centerY, minimum, style);
                break;
            case SUNBURST:
                drawSunburstBack(canvas, centerX, centerY, minimum, style);
                break;
            case WAVES:
                drawWavesBack(canvas, centerX, centerY, minimum, style);
                break;
            case HARLEQUIN:
                drawHarlequinBack(canvas, centerX, centerY, minimum, style);
                break;
            case ORBITS:
                drawOrbitsBack(canvas, centerX, centerY, minimum, style);
                break;
            case PRISM:
                drawPrismBack(canvas, centerX, centerY, minimum, style);
                break;
            case BOTANICAL:
                drawBotanicalBack(canvas, centerX, centerY, minimum, style);
                break;
            case WEAVE:
                drawWeaveBack(canvas, centerX, centerY, minimum, style);
                break;
            case AURORA:
                drawAuroraBack(canvas, centerX, centerY, minimum, style);
                break;
            case FIREFLIES:
                drawFirefliesBack(canvas, centerX, centerY, minimum, style);
                break;
            case KALEIDO:
                drawKaleidoBack(canvas, centerX, centerY, minimum, style);
                break;
            case COMET_TRAILS:
                drawCometTrailsBack(canvas, centerX, centerY, minimum, style);
                break;
            case MOON_RIPPLES:
                drawMoonRipplesBack(canvas, centerX, centerY, minimum, style);
                break;
            case PIXEL_RAIN:
                drawPixelRainBack(canvas, centerX, centerY, minimum, style);
                break;
            case CLASSIC:
            default:
                drawClassicBack(canvas, centerX, centerY, size, style);
                break;
        }
        canvas.restoreToCount(patternSaveCount);

        // The border is last so even dense or dp-clamped patterns stay visually contained.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(dp(1f), minimum * 0.025f));
        paint.setColor(style.getBorderColor());
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, paint);
    }

    private void drawClassicBack(
        Canvas canvas,
        float centerX,
        float centerY,
        float size,
        CardBackStyle style
    ) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(dp(1.3f), size * 0.10f));
        paint.setColor(style.getPatternColor());
        shapePath.reset();
        shapePath.moveTo(centerX, centerY - size);
        shapePath.lineTo(centerX + size, centerY);
        shapePath.lineTo(centerX, centerY + size);
        shapePath.lineTo(centerX - size, centerY);
        shapePath.close();
        canvas.drawPath(shapePath, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(style.getAccentColor());
        canvas.drawCircle(centerX, centerY, Math.max(dp(1.6f), size * 0.16f), paint);
    }

    private void drawConstellationBack(
        Canvas canvas,
        float centerX,
        float centerY,
        float minimum,
        CardBackStyle style
    ) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(dp(0.8f), minimum * 0.018f));
        paint.setColor(style.getPatternColor());
        shapePath.reset();
        for (int index = 0; index < constellationX.length; index++) {
            float x = centerX + constellationX[index] * minimum;
            float y = centerY + constellationY[index] * minimum;
            if (index == 0) {
                shapePath.moveTo(x, y);
            } else {
                shapePath.lineTo(x, y);
            }
        }
        canvas.drawPath(shapePath, paint);
        paint.setStyle(Paint.Style.FILL);
        for (int index = 0; index < constellationX.length; index++) {
            boolean accented = index == constellationAccentIndex;
            paint.setColor(accented ? style.getAccentColor() : style.getPatternColor());
            float radius = minimum * (accented ? 0.045f : 0.027f);
            canvas.drawCircle(
                centerX + constellationX[index] * minimum,
                centerY + constellationY[index] * minimum,
                Math.max(dp(1f), radius),
                paint
            );
        }
    }

    private void drawSunburstBack(
        Canvas canvas,
        float centerX,
        float centerY,
        float minimum,
        CardBackStyle style
    ) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(dp(1f), minimum * 0.025f));
        paint.setColor(style.getPatternColor());
        for (int ray = 0; ray < 12; ray++) {
            double angle = Math.toRadians(ray * 30f - 90f);
            float inner = minimum * 0.10f;
            float outer = minimum * (ray % 2 == 0 ? 0.36f : 0.29f);
            canvas.drawLine(
                centerX + (float) Math.cos(angle) * inner,
                centerY + (float) Math.sin(angle) * inner,
                centerX + (float) Math.cos(angle) * outer,
                centerY + (float) Math.sin(angle) * outer,
                paint
            );
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(style.getAccentColor());
        canvas.drawCircle(centerX, centerY, Math.max(dp(2f), minimum * 0.09f), paint);
    }

    private void drawWavesBack(
        Canvas canvas,
        float centerX,
        float centerY,
        float minimum,
        CardBackStyle style
    ) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(dp(1.1f), minimum * 0.03f));
        for (int wave = -1; wave <= 1; wave++) {
            paint.setColor(
                wave + 1 == cardBackGeometry.waveAccentBand
                    ? style.getAccentColor()
                    : style.getPatternColor()
            );
            float y = centerY + minimum * wave * cardBackGeometry.waveSpacing;
            float amplitude = minimum
                * cardBackGeometry.waveAmplitude
                * cardBackGeometry.waveDirection;
            float tailBend = minimum * cardBackGeometry.waveTailBend;
            shapePath.reset();
            shapePath.moveTo(centerX - minimum * 0.36f, y);
            shapePath.cubicTo(
                centerX - minimum * 0.18f,
                y - amplitude,
                centerX + minimum * 0.02f,
                y + amplitude,
                centerX + minimum * 0.18f,
                y
            );
            shapePath.cubicTo(
                centerX + minimum * 0.25f,
                y - tailBend,
                centerX + minimum * 0.31f,
                y - tailBend,
                centerX + minimum * 0.36f,
                y + tailBend * 0.30f
            );
            canvas.drawPath(shapePath, paint);
        }
    }

    private void drawHarlequinBack(
        Canvas canvas,
        float centerX,
        float centerY,
        float minimum,
        CardBackStyle style
    ) {
        float half = minimum * 0.075f;
        float horizontalStep = minimum * 0.19f;
        float verticalStep = minimum * 0.19f;
        paint.setStyle(Paint.Style.FILL);
        for (int row = -2; row <= 2; row++) {
            for (int column = -1; column <= 1; column++) {
                float x = centerX + column * horizontalStep;
                float y = centerY + row * verticalStep;
                shapePath.reset();
                shapePath.moveTo(x, y - half);
                shapePath.lineTo(x + half, y);
                shapePath.lineTo(x, y + half);
                shapePath.lineTo(x - half, y);
                shapePath.close();
                paint.setColor(
                    (row + column) % 2 == 0
                        ? style.getPatternColor()
                        : style.getAccentColor()
                );
                canvas.drawPath(shapePath, paint);
            }
        }
    }

    private void drawOrbitsBack(
        Canvas canvas,
        float centerX,
        float centerY,
        float minimum,
        CardBackStyle style
    ) {
        drawOrbit(
            canvas,
            centerX,
            centerY,
            minimum * orbitPattern.firstRadiusX,
            minimum * orbitPattern.firstRadiusY,
            orbitPattern.firstTiltDegrees,
            style.getPatternColor()
        );
        drawOrbit(
            canvas,
            centerX,
            centerY,
            minimum * orbitPattern.secondRadiusX,
            minimum * orbitPattern.secondRadiusY,
            orbitPattern.secondTiltDegrees,
            style.getPatternColor()
        );

        long uptimeMillis = backAnimationTimeMillis();
        float motion = (float) (
            (uptimeMillis / 1000d
                * orbitPattern.speedDegreesPerSecond
                * orbitPattern.direction) % 360d
        );
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(style.getPatternColor());
        drawOrbitPlanet(
            canvas,
            centerX,
            centerY,
            minimum * orbitPattern.firstRadiusX,
            minimum * orbitPattern.firstRadiusY,
            orbitPattern.firstTiltDegrees,
            orbitPattern.firstPlanetDegrees + motion,
            Math.max(dp(1f), minimum * 0.035f)
        );
        drawOrbitPlanet(
            canvas,
            centerX,
            centerY,
            minimum * orbitPattern.secondRadiusX,
            minimum * orbitPattern.secondRadiusY,
            orbitPattern.secondTiltDegrees,
            orbitPattern.secondPlanetDegrees - motion * 0.82f,
            Math.max(dp(1f), minimum * 0.028f)
        );

        paint.setColor(style.getAccentColor());
        canvas.drawCircle(centerX, centerY, Math.max(dp(2f), minimum * 0.075f), paint);
    }

    private void drawOrbit(
        Canvas canvas,
        float centerX,
        float centerY,
        float radiusX,
        float radiusY,
        float tiltDegrees,
        int color
    ) {
        patternBounds.set(
            centerX - radiusX,
            centerY - radiusY,
            centerX + radiusX,
            centerY + radiusY
        );
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(dp(1f), Math.min(radiusX, radiusY) * 0.17f));
        paint.setColor(color);
        int saveCount = canvas.save();
        canvas.rotate(tiltDegrees, centerX, centerY);
        canvas.drawOval(patternBounds, paint);
        canvas.restoreToCount(saveCount);
    }

    private void drawOrbitPlanet(
        Canvas canvas,
        float centerX,
        float centerY,
        float radiusX,
        float radiusY,
        float tiltDegrees,
        float planetDegrees,
        float planetRadius
    ) {
        double planetRadians = Math.toRadians(planetDegrees);
        float localX = (float) Math.cos(planetRadians) * radiusX;
        float localY = (float) Math.sin(planetRadians) * radiusY;
        double tiltRadians = Math.toRadians(tiltDegrees);
        float rotatedX = (float) (
            localX * Math.cos(tiltRadians) - localY * Math.sin(tiltRadians)
        );
        float rotatedY = (float) (
            localX * Math.sin(tiltRadians) + localY * Math.cos(tiltRadians)
        );
        canvas.drawCircle(
            centerX + rotatedX,
            centerY + rotatedY,
            planetRadius,
            paint
        );
    }

    private void drawPrismBack(
        Canvas canvas,
        float centerX,
        float centerY,
        float minimum,
        CardBackStyle style
    ) {
        float left = centerX - minimum * 0.34f;
        float right = centerX + minimum * 0.34f;
        float top = centerY - minimum * 0.34f;
        float bottom = centerY + minimum * 0.34f;
        float apexX = centerX + minimum * cardBackGeometry.prismApexX;
        float apexY = centerY + minimum * cardBackGeometry.prismApexY;
        float bottomX = centerX + minimum * cardBackGeometry.prismBottomX;
        float leftKneeY = centerY + minimum * cardBackGeometry.prismLeftKneeY;
        float rightKneeY = centerY + minimum * cardBackGeometry.prismRightKneeY;
        int firstColor = cardBackGeometry.prismPaletteFlipped
            ? style.getAccentColor()
            : style.getPatternColor();
        int secondColor = cardBackGeometry.prismPaletteFlipped
            ? style.getPatternColor()
            : style.getAccentColor();
        drawFacet(canvas, firstColor, left, top, apexX, apexY, left, leftKneeY);
        drawFacet(canvas, secondColor, left, top, right, top, apexX, apexY);
        drawFacet(canvas, firstColor, right, top, right, rightKneeY, apexX, apexY);
        drawFacet(canvas, secondColor, left, leftKneeY, apexX, apexY, bottomX, bottom);
        drawFacet(canvas, firstColor, apexX, apexY, right, rightKneeY, bottomX, bottom);
        drawFacet(canvas, firstColor, left, leftKneeY, bottomX, bottom, left, bottom);
        drawFacet(canvas, secondColor, right, rightKneeY, right, bottom, bottomX, bottom);
    }

    private void drawFacet(
        Canvas canvas,
        int color,
        float firstX,
        float firstY,
        float secondX,
        float secondY,
        float thirdX,
        float thirdY
    ) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(colorWithAlpha(color, 218));
        shapePath.reset();
        shapePath.moveTo(firstX, firstY);
        shapePath.lineTo(secondX, secondY);
        shapePath.lineTo(thirdX, thirdY);
        shapePath.close();
        canvas.drawPath(shapePath, paint);
    }

    private void drawBotanicalBack(
        Canvas canvas,
        float centerX,
        float centerY,
        float minimum,
        CardBackStyle style
    ) {
        float curve = minimum
            * CardBackGeometry.BOTANICAL_STEM_CURVE
            * cardBackGeometry.botanicalStemDirection;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(dp(1f), minimum * 0.025f));
        paint.setColor(style.getPatternColor());
        shapePath.reset();
        shapePath.moveTo(centerX - curve, centerY + minimum * 0.34f);
        shapePath.cubicTo(
            centerX - curve * 0.45f,
            centerY + minimum * 0.13f,
            centerX + curve * 0.35f,
            centerY - minimum * 0.12f,
            centerX + curve,
            centerY - minimum * 0.30f
        );
        canvas.drawPath(shapePath, paint);

        // Twigs stop at each leaf base, so no line is visible through a leaf.
        for (int leaf = 0; leaf < CardBackGeometry.BOTANICAL_LEAF_COUNT; leaf++) {
            int side = cardBackGeometry.botanicalSide(leaf);
            float anchorX = centerX + minimum * cardBackGeometry.botanicalStemX(leaf);
            float anchorY = centerY + minimum * cardBackGeometry.botanicalAnchorY(leaf);
            float baseX = anchorX
                + side * minimum * CardBackGeometry.BOTANICAL_LEAF_BASE_OFFSET;
            canvas.drawLine(anchorX, anchorY, baseX, anchorY, paint);
        }

        for (int leaf = 0; leaf < CardBackGeometry.BOTANICAL_LEAF_COUNT; leaf++) {
            int side = cardBackGeometry.botanicalSide(leaf);
            float baseX = centerX
                + minimum * cardBackGeometry.botanicalStemX(leaf)
                + side * minimum * CardBackGeometry.BOTANICAL_LEAF_BASE_OFFSET;
            float baseY = centerY + minimum * cardBackGeometry.botanicalAnchorY(leaf);
            drawPointedLeaf(
                canvas,
                baseX,
                baseY,
                baseX + side * minimum * CardBackGeometry.BOTANICAL_LEAF_LENGTH_X,
                baseY + minimum * CardBackGeometry.BOTANICAL_LEAF_LENGTH_Y,
                minimum * CardBackGeometry.BOTANICAL_LEAF_HALF_WIDTH,
                leaf == cardBackGeometry.botanicalAccentLeaf
                    ? style.getAccentColor()
                    : style.getPatternColor()
            );
        }

        float terminalBaseX = centerX + curve;
        float terminalBaseY = centerY - minimum * 0.30f;
        drawPointedLeaf(
            canvas,
            terminalBaseX,
            terminalBaseY,
            terminalBaseX
                + cardBackGeometry.botanicalTerminalDirection
                * minimum
                * CardBackGeometry.BOTANICAL_TERMINAL_LENGTH_X,
            terminalBaseY
                + minimum * CardBackGeometry.BOTANICAL_TERMINAL_LENGTH_Y,
            minimum * CardBackGeometry.BOTANICAL_LEAF_HALF_WIDTH,
            style.getPatternColor()
        );
    }

    private void drawPointedLeaf(
        Canvas canvas,
        float baseX,
        float baseY,
        float tipX,
        float tipY,
        float halfWidth,
        int color
    ) {
        float deltaX = tipX - baseX;
        float deltaY = tipY - baseY;
        float length = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        float perpendicularX = -deltaY / length * halfWidth;
        float perpendicularY = deltaX / length * halfWidth;
        float firstX = baseX + deltaX * 0.36f;
        float firstY = baseY + deltaY * 0.36f;
        float secondX = baseX + deltaX * 0.74f;
        float secondY = baseY + deltaY * 0.74f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        shapePath.reset();
        shapePath.moveTo(baseX, baseY);
        shapePath.cubicTo(
            firstX + perpendicularX,
            firstY + perpendicularY,
            secondX + perpendicularX,
            secondY + perpendicularY,
            tipX,
            tipY
        );
        shapePath.cubicTo(
            secondX - perpendicularX,
            secondY - perpendicularY,
            firstX - perpendicularX,
            firstY - perpendicularY,
            baseX,
            baseY
        );
        shapePath.close();
        canvas.drawPath(shapePath, paint);
    }

    private void drawWeaveBack(
        Canvas canvas,
        float centerX,
        float centerY,
        float minimum,
        CardBackStyle style
    ) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Math.max(dp(3f), minimum * 0.12f));
        paint.setColor(style.getPatternColor());
        shapePath.reset();
        shapePath.moveTo(centerX - minimum * 0.31f, centerY - minimum * 0.29f);
        shapePath.cubicTo(
            centerX + minimum * 0.31f,
            centerY - minimum * 0.18f,
            centerX - minimum * 0.31f,
            centerY + minimum * 0.18f,
            centerX + minimum * 0.31f,
            centerY + minimum * 0.29f
        );
        canvas.drawPath(shapePath, paint);

        shapePath.reset();
        shapePath.moveTo(centerX + minimum * 0.29f, centerY - minimum * 0.31f);
        shapePath.cubicTo(
            centerX + minimum * 0.18f,
            centerY + minimum * 0.31f,
            centerX - minimum * 0.18f,
            centerY - minimum * 0.31f,
            centerX - minimum * 0.29f,
            centerY + minimum * 0.31f
        );
        paint.setStrokeWidth(Math.max(dp(4f), minimum * 0.17f));
        paint.setColor(style.getFillColor());
        canvas.drawPath(shapePath, paint);
        paint.setStrokeWidth(Math.max(dp(3f), minimum * 0.10f));
        paint.setColor(style.getAccentColor());
        canvas.drawPath(shapePath, paint);
    }

    private void drawAuroraBack(
        Canvas canvas,
        float centerX,
        float centerY,
        float minimum,
        CardBackStyle style
    ) {
        float phase = CardBackMotion.phase(backAnimationTimeMillis(), 8000L);
        float drift = CardBackMotion.sine(phase) * minimum * 0.055f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int ribbon = 0; ribbon < 2; ribbon++) {
            float direction = ribbon == 0 ? 1f : -1f;
            float startY = centerY + direction * minimum * 0.29f;
            float endY = centerY - direction * minimum * 0.29f;
            shapePath.reset();
            shapePath.moveTo(
                centerX - minimum * 0.34f,
                startY + drift * direction
            );
            shapePath.cubicTo(
                centerX - minimum * 0.12f,
                centerY - direction * minimum * 0.17f - drift,
                centerX + minimum * 0.12f,
                centerY + direction * minimum * 0.17f + drift,
                centerX + minimum * 0.36f,
                endY - drift * direction
            );
            paint.setStrokeWidth(Math.max(dp(2f), minimum * (ribbon == 0 ? 0.085f : 0.065f)));
            paint.setColor(colorWithAlpha(
                ribbon == 0 ? style.getAccentColor() : style.getPatternColor(),
                ribbon == 0 ? 225 : 190
            ));
            canvas.drawPath(shapePath, paint);
        }
    }

    private void drawFirefliesBack(
        Canvas canvas,
        float centerX,
        float centerY,
        float minimum,
        CardBackStyle style
    ) {
        float phase = CardBackMotion.phase(backAnimationTimeMillis(), 4800L);
        paint.setStyle(Paint.Style.FILL);
        for (int index = 0; index < FIREFLY_X.length; index++) {
            float localPhase = phase + (float) index / FIREFLY_X.length;
            float glow = CardBackMotion.pulse(localPhase);
            paint.setColor(colorWithAlpha(
                index % 3 == 0 ? style.getAccentColor() : style.getPatternColor(),
                Math.round(80f + glow * 175f)
            ));
            canvas.drawCircle(
                centerX + FIREFLY_X[index] * minimum,
                centerY + FIREFLY_Y[index] * minimum,
                Math.max(dp(1f), minimum * (0.018f + glow * 0.027f)),
                paint
            );
        }
    }

    private void drawKaleidoBack(
        Canvas canvas,
        float centerX,
        float centerY,
        float minimum,
        CardBackStyle style
    ) {
        float phase = CardBackMotion.phase(backAnimationTimeMillis(), 6000L);
        float breathing = 0.92f + CardBackMotion.pulse(phase) * 0.08f;
        float rocking = CardBackMotion.sine(phase) * 5f;
        paint.setStyle(Paint.Style.FILL);
        for (int petal = 0; petal < 6; petal++) {
            patternBounds.set(
                centerX - minimum * 0.065f,
                centerY - minimum * 0.31f * breathing,
                centerX + minimum * 0.065f,
                centerY - minimum * 0.075f
            );
            paint.setColor(
                petal % 2 == 0 ? style.getPatternColor() : style.getAccentColor()
            );
            int saveCount = canvas.save();
            canvas.rotate(rocking + petal * 60f, centerX, centerY);
            canvas.drawOval(patternBounds, paint);
            canvas.restoreToCount(saveCount);
        }
        paint.setColor(style.getAccentColor());
        canvas.drawCircle(
            centerX,
            centerY,
            minimum * (0.055f + CardBackMotion.pulse(phase) * 0.018f),
            paint
        );
    }

    private void drawCometTrailsBack(
        Canvas canvas,
        float centerX,
        float centerY,
        float minimum,
        CardBackStyle style
    ) {
        long uptimeMillis = backAnimationTimeMillis();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int comet = 0; comet < 4; comet++) {
            long periodMillis = 4800L + Math.round(
                CardBackMotion.stableUnit(cardNumber, comet, 101) * 2600f
            );
            float progress = CardBackMotion.wrap(
                CardBackMotion.phase(uptimeMillis, periodMillis)
                    + CardBackMotion.stableUnit(cardNumber, comet, 102)
            );
            float lane = CardBackMotion.stableUnit(cardNumber, comet, 103) - 0.5f;
            float headX = centerX + minimum * (-0.53f + progress * 1.06f);
            float headY = centerY
                + minimum * (-0.39f + progress * 0.78f + lane * 0.24f);
            float trail = minimum * (
                0.10f + CardBackMotion.stableUnit(cardNumber, comet, 104) * 0.07f
            );
            int color = comet % 3 == 0
                ? style.getAccentColor()
                : style.getPatternColor();
            paint.setColor(colorWithAlpha(color, comet % 2 == 0 ? 205 : 160));
            paint.setStrokeWidth(Math.max(dp(1f), minimum * 0.022f));
            canvas.drawLine(
                headX - trail,
                headY - trail * 0.72f,
                headX,
                headY,
                paint
            );
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawCircle(
                headX,
                headY,
                Math.max(dp(1f), minimum * (comet % 3 == 0 ? 0.036f : 0.027f)),
                paint
            );
            paint.setStyle(Paint.Style.STROKE);
        }
    }

    private void drawMoonRipplesBack(
        Canvas canvas,
        float centerX,
        float centerY,
        float minimum,
        CardBackStyle style
    ) {
        long uptimeMillis = backAnimationTimeMillis();
        float sourceX = centerX
            + minimum * (CardBackMotion.stableUnit(cardNumber, 0, 201) - 0.5f) * 0.13f;
        float sourceY = centerY
            + minimum * (CardBackMotion.stableUnit(cardNumber, 0, 202) - 0.5f) * 0.13f;
        float basePhase = CardBackMotion.phase(uptimeMillis, 5600L);
        paint.setStyle(Paint.Style.STROKE);
        for (int ripple = 0; ripple < 4; ripple++) {
            float progress = CardBackMotion.wrap(
                basePhase
                    + ripple * 0.25f
                    + CardBackMotion.stableUnit(cardNumber, ripple, 203) * 0.08f
            );
            float remaining = 1f - progress;
            float radius = minimum * (0.045f + progress * 0.40f);
            paint.setStrokeWidth(Math.max(dp(0.8f), minimum * (0.012f + remaining * 0.018f)));
            paint.setColor(colorWithAlpha(
                ripple % 3 == 0 ? style.getAccentColor() : style.getPatternColor(),
                Math.round(32f + remaining * remaining * 190f)
            ));
            canvas.drawCircle(sourceX, sourceY, radius, paint);
        }

        float glow = CardBackMotion.pulse(basePhase);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(style.getAccentColor());
        canvas.drawCircle(
            sourceX,
            sourceY,
            Math.max(dp(1.5f), minimum * (0.045f + glow * 0.014f)),
            paint
        );
    }

    private void drawPixelRainBack(
        Canvas canvas,
        float centerX,
        float centerY,
        float minimum,
        CardBackStyle style
    ) {
        long uptimeMillis = backAnimationTimeMillis();
        float halfPixel = Math.max(dp(1f), minimum * 0.028f);
        for (int column = 0; column < 6; column++) {
            float jitter = CardBackMotion.stableUnit(cardNumber, column, 301) - 0.5f;
            float x = centerX + minimum * (-0.35f + column * 0.14f + jitter * 0.035f);
            long periodMillis = 3000L + Math.round(
                CardBackMotion.stableUnit(cardNumber, column, 302) * 2100f
            );
            float headProgress = CardBackMotion.wrap(
                CardBackMotion.phase(uptimeMillis, periodMillis)
                    + CardBackMotion.stableUnit(cardNumber, column, 303)
            );
            for (int trail = 0; trail < 4; trail++) {
                float progress = CardBackMotion.wrap(headProgress - trail * 0.075f);
                float y = centerY + minimum * (-0.48f + progress * 0.96f);
                int color = trail == 0 && column % 2 == 0
                    ? style.getAccentColor()
                    : style.getPatternColor();
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(colorWithAlpha(color, 235 - trail * 48));
                patternBounds.set(
                    x - halfPixel,
                    y - halfPixel * 0.72f,
                    x + halfPixel,
                    y + halfPixel * 0.72f
                );
                canvas.drawRoundRect(
                    patternBounds,
                    halfPixel * 0.28f,
                    halfPixel * 0.28f,
                    paint
                );
            }
        }
    }

    private void drawPairPatternCue(Canvas canvas, int faceColor) {
        int family = PairPatternCue.familyFor(pairId);
        int markerCount = PairPatternCue.markerCountFor(pairId);
        float minimum = Math.min(getWidth(), getHeight());
        float startX = getWidth() / 2f - minimum * 0.30f;
        float endX = getWidth() / 2f + minimum * 0.30f;
        float topY = getHeight() / 2f - minimum * 0.40f;
        float bottomY = getHeight() / 2f + minimum * 0.40f;
        float markerRadius = Math.max(
            dp(0.65f),
            minimum * (markerCount >= 4 ? 0.018f : 0.024f)
        );
        int cueColor = contrastTextColor(faceColor);
        // These markers carry pair identity, so keep them fully opaque even when the rest of the
        // card uses softer decoration.
        paint.setColor(cueColor);
        paint.setStrokeWidth(Math.max(dp(0.8f), minimum * 0.018f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        for (int marker = 0; marker < markerCount; marker++) {
            float fraction = markerCount == 1 ? 0.5f : marker / (float) (markerCount - 1);
            float x = startX + (endX - startX) * fraction;
            drawPairPatternMarker(canvas, family, x, topY, markerRadius);
            drawPairPatternMarker(canvas, family, x, bottomY, markerRadius);
        }
    }

    private void drawPairPatternMarker(
        Canvas canvas,
        int family,
        float centerX,
        float centerY,
        float radius
    ) {
        switch (family) {
            case 0:
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(centerX, centerY, radius, paint);
                break;
            case 1:
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(centerX, centerY, radius, paint);
                break;
            case 2:
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRect(
                    centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius,
                    paint
                );
                break;
            case 3:
                paint.setStyle(Paint.Style.FILL);
                shapePath.reset();
                shapePath.moveTo(centerX, centerY - radius);
                shapePath.lineTo(centerX + radius, centerY);
                shapePath.lineTo(centerX, centerY + radius);
                shapePath.lineTo(centerX - radius, centerY);
                shapePath.close();
                canvas.drawPath(shapePath, paint);
                break;
            case 4:
                paint.setStyle(Paint.Style.FILL);
                shapePath.reset();
                shapePath.moveTo(centerX, centerY - radius);
                shapePath.lineTo(centerX + radius, centerY + radius);
                shapePath.lineTo(centerX - radius, centerY + radius);
                shapePath.close();
                canvas.drawPath(shapePath, paint);
                break;
            case 5:
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, paint);
                canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, paint);
                break;
            case 6:
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(
                    centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius,
                    paint
                );
                canvas.drawLine(
                    centerX - radius,
                    centerY + radius,
                    centerX + radius,
                    centerY - radius,
                    paint
                );
                break;
            case 7:
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, paint);
                break;
            case 8:
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, paint);
                break;
            default:
                paint.setStyle(Paint.Style.STROKE);
                shapePath.reset();
                shapePath.moveTo(centerX - radius, centerY - radius * 0.55f);
                shapePath.lineTo(centerX, centerY + radius * 0.55f);
                shapePath.lineTo(centerX + radius, centerY - radius * 0.55f);
                canvas.drawPath(shapePath, paint);
                break;
        }
    }

    private void drawHighContrastSymbolPlate(Canvas canvas) {
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) * 0.31f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(centerX, centerY, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(dp(1.5f), radius * 0.08f));
        paint.setColor(Color.rgb(20, 23, 32));
        canvas.drawCircle(centerX, centerY, radius, paint);
    }

    private void drawSymbol(Canvas canvas, int faceColor) {
        if (rubicsMode) {
            drawRubicsFace(canvas);
            return;
        }
        if (numberMode) {
            drawNumber(canvas, faceColor);
            return;
        }
        if (!fallbackToShape) {
            if (iconText.isEmpty()) {
                return;
            }
            if (wordMode) {
                drawWord(canvas, faceColor);
            } else {
                if (highContrast) {
                    drawHighContrastSymbolPlate(canvas);
                }
                drawEmoji(canvas);
            }
            return;
        }
        int shape = fallbackShapeIndex();
        int variant = fallbackVariantIndex();
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) * 0.26f;
        createShapePath(shape, centerX, centerY, radius);

        if (highContrast) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(dp(3f), radius * 0.20f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setColor(contrastTextColor(faceColor));
            canvas.drawPath(shapePath, paint);
        }

        paint.setColor(displayedSymbolColor());
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        if (variant == 1) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(dp(2.5f), radius * 0.14f));
            canvas.drawPath(shapePath, paint);
        } else {
            paint.setStyle(Paint.Style.FILL);
            canvas.drawPath(shapePath, paint);
        }

        if (variant == 2) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(faceColor == MATCHED_FACE ? Color.WHITE : faceColor);
            float dotRadius = Math.max(dp(1.7f), radius * 0.09f);
            canvas.drawCircle(centerX - radius * 0.34f, centerY, dotRadius, paint);
            canvas.drawCircle(centerX, centerY, dotRadius, paint);
            canvas.drawCircle(centerX + radius * 0.34f, centerY, dotRadius, paint);
        } else if (variant == 3) {
            int saveCount = canvas.save();
            canvas.clipPath(shapePath);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(dp(1.4f), radius * 0.09f));
            paint.setColor(faceColor == MATCHED_FACE ? Color.WHITE : faceColor);
            float step = Math.max(dp(4f), radius * 0.34f);
            for (float offset = -getHeight(); offset < getWidth(); offset += step) {
                canvas.drawLine(offset, getHeight(), offset + getHeight(), 0f, paint);
            }
            canvas.restoreToCount(saveCount);
        } else if (variant == 4) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(faceColor == MATCHED_FACE ? Color.WHITE : faceColor);
            canvas.drawCircle(centerX, centerY, Math.max(dp(2f), radius * 0.42f), paint);
        }
    }

    /** Draws one cube face without relying on fonts or emoji support. */
    private void drawRubicsFace(Canvas canvas) {
        float minimum = Math.min(getWidth(), getHeight());
        if (minimum <= 0f) {
            return;
        }
        float side = minimum * (highContrast ? 0.72f : 0.68f);
        float left = (getWidth() - side) / 2f;
        float top = (getHeight() - side) / 2f;
        float outerRadius = Math.min(side * 0.16f, Math.max(dp(2f), side * 0.075f));
        float edge = Math.min(
            side * 0.09f,
            Math.max(dp(highContrast ? 1.5f : 1f), side * 0.035f)
        );
        float gap = Math.min(
            side * 0.08f,
            Math.max(dp(highContrast ? 1.15f : 0.8f), side * 0.027f)
        );
        float cell = (side - edge * 2f - gap * 2f) / 3f;
        int frameColor = highContrast ? Color.BLACK : Color.rgb(25, 29, 38);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(frameColor);
        canvas.drawRoundRect(
            left,
            top,
            left + side,
            top + side,
            outerRadius,
            outerRadius,
            paint
        );

        float stickerRadius = Math.max(dp(0.6f), cell * 0.09f);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int stickerIndex = row * 3 + column;
                int colorId = rubicsStickerIds[stickerIndex];
                float stickerLeft = left + edge + column * (cell + gap);
                float stickerTop = top + edge + row * (cell + gap);
                float stickerRight = stickerLeft + cell;
                float stickerBottom = stickerTop + cell;

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(rubicsStickerColor(colorId));
                canvas.drawRoundRect(
                    stickerLeft,
                    stickerTop,
                    stickerRight,
                    stickerBottom,
                    stickerRadius,
                    stickerRadius,
                    paint
                );

                if (colorBlindPatterns) {
                    drawRubicsStickerCue(
                        canvas,
                        colorId,
                        stickerLeft,
                        stickerTop,
                        stickerRight,
                        stickerBottom
                    );
                }

                if (highContrast) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(Math.max(dp(0.65f), cell * 0.055f));
                    paint.setColor(frameColor);
                    canvas.drawRoundRect(
                        stickerLeft,
                        stickerTop,
                        stickerRight,
                        stickerBottom,
                        stickerRadius,
                        stickerRadius,
                        paint
                    );
                }
            }
        }
    }

    /** Adds one compact, repeatable non-color mark for each sticker color. */
    private void drawRubicsStickerCue(
        Canvas canvas,
        int colorId,
        float left,
        float top,
        float right,
        float bottom
    ) {
        float centerX = (left + right) / 2f;
        float centerY = (top + bottom) / 2f;
        float halfSpan = Math.min(right - left, bottom - top) * 0.23f;
        int stickerColor = rubicsStickerColor(colorId);
        paint.setColor(colorWithAlpha(contrastTextColor(stickerColor), 190));
        paint.setStrokeWidth(Math.max(dp(0.55f), halfSpan * 0.22f));
        paint.setStrokeCap(Paint.Cap.ROUND);

        switch (colorId) {
            case 0:
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(centerX, centerY, Math.max(dp(0.55f), halfSpan * 0.35f), paint);
                break;
            case 1:
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(centerX, centerY, Math.max(dp(0.75f), halfSpan * 0.65f), paint);
                break;
            case 2:
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(centerX - halfSpan, centerY, centerX + halfSpan, centerY, paint);
                break;
            case 3:
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(centerX, centerY - halfSpan, centerX, centerY + halfSpan, paint);
                break;
            case 4:
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(
                    centerX - halfSpan,
                    centerY + halfSpan,
                    centerX + halfSpan,
                    centerY - halfSpan,
                    paint
                );
                break;
            case 5:
            default:
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(
                    centerX - halfSpan,
                    centerY - halfSpan,
                    centerX + halfSpan,
                    centerY + halfSpan,
                    paint
                );
                break;
        }
    }

    private static int rubicsStickerColor(int colorId) {
        if (colorId < 0 || colorId >= RUBICS_STICKER_COLORS.length) {
            return RUBICS_STICKER_COLORS[0];
        }
        return RUBICS_STICKER_COLORS[colorId];
    }

    private void drawEmoji(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(36, 39, 55));
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Math.min(getWidth(), getHeight()) * 0.55f);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = getHeight() / 2f - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(iconText, getWidth() / 2f, baseline, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawWord(Canvas canvas, int faceColor) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(contrastTextColor(faceColor));
        paint.setTypeface(Typeface.create("sans", Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        float textSize = highContrast
            ? Math.min(getHeight() * 0.32f, getWidth() * 0.27f)
            : Math.min(getHeight() * 0.29f, getWidth() * 0.24f);
        paint.setTextSize(Math.max(dp(highContrast ? 8f : 7f), textSize));
        float maxWidth = getWidth() * 0.80f;
        float measured = paint.measureText(iconText);
        if (measured > maxWidth && measured > 0f) {
            paint.setTextSize(paint.getTextSize() * maxWidth / measured);
        }
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = getHeight() / 2f - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(iconText, getWidth() / 2f, baseline, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawNumber(Canvas canvas, int faceColor) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(contrastTextColor(faceColor));
        paint.setTypeface(Typeface.create("sans", Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        float textSize = Math.min(getHeight() * 0.50f, getWidth() * 0.42f);
        paint.setTextSize(Math.max(dp(10f), textSize));
        float maxWidth = getWidth() * 0.80f;
        float measured = paint.measureText(iconText);
        if (measured > maxWidth && measured > 0f) {
            paint.setTextSize(paint.getTextSize() * maxWidth / measured);
        }
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = getHeight() / 2f - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(iconText, getWidth() / 2f, baseline, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void createShapePath(int shape, float cx, float cy, float radius) {
        shapePath.reset();
        switch (shape) {
            case 0:
                shapePath.addCircle(cx, cy, radius, Path.Direction.CW);
                break;
            case 1:
                shapePath.addRoundRect(
                    new RectF(cx - radius, cy - radius, cx + radius, cy + radius),
                    radius * 0.2f,
                    radius * 0.2f,
                    Path.Direction.CW
                );
                break;
            case 2:
                polygon(shapePath, cx, cy, radius, 3, -90f);
                break;
            case 3:
                polygon(shapePath, cx, cy, radius, 4, -90f);
                break;
            case 4:
                star(shapePath, cx, cy, radius, radius * 0.44f, 5);
                break;
            case 5:
                heart(shapePath, cx, cy, radius);
                break;
            case 6:
                polygon(shapePath, cx, cy, radius, 6, -90f);
                break;
            case 7:
                plus(shapePath, cx, cy, radius);
                break;
            case 8:
                lightning(shapePath, cx, cy, radius);
                break;
            default:
                flower(shapePath, cx, cy, radius);
                break;
        }
    }

    private static void polygon(Path path, float cx, float cy, float radius, int sides, float startDegrees) {
        for (int index = 0; index < sides; index++) {
            double angle = Math.toRadians(startDegrees + index * 360f / sides);
            float x = cx + (float) Math.cos(angle) * radius;
            float y = cy + (float) Math.sin(angle) * radius;
            if (index == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
    }

    private static void star(Path path, float cx, float cy, float outer, float inner, int points) {
        for (int index = 0; index < points * 2; index++) {
            float radius = index % 2 == 0 ? outer : inner;
            double angle = Math.toRadians(-90f + index * 180f / points);
            float x = cx + (float) Math.cos(angle) * radius;
            float y = cy + (float) Math.sin(angle) * radius;
            if (index == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
    }

    private static void heart(Path path, float cx, float cy, float radius) {
        path.moveTo(cx, cy + radius * 0.9f);
        path.cubicTo(
            cx - radius * 1.25f,
            cy + radius * 0.15f,
            cx - radius * 1.05f,
            cy - radius * 0.85f,
            cx - radius * 0.42f,
            cy - radius * 0.65f
        );
        path.cubicTo(cx - radius * 0.15f, cy - radius * 1.05f, cx, cy - radius * 0.72f, cx, cy - radius * 0.48f);
        path.cubicTo(cx, cy - radius * 0.72f, cx + radius * 0.15f, cy - radius * 1.05f, cx + radius * 0.42f, cy - radius * 0.65f);
        path.cubicTo(
            cx + radius * 1.05f,
            cy - radius * 0.85f,
            cx + radius * 1.25f,
            cy + radius * 0.15f,
            cx,
            cy + radius * 0.9f
        );
        path.close();
    }

    private static void plus(Path path, float cx, float cy, float radius) {
        float narrow = radius * 0.36f;
        path.moveTo(cx - narrow, cy - radius);
        path.lineTo(cx + narrow, cy - radius);
        path.lineTo(cx + narrow, cy - narrow);
        path.lineTo(cx + radius, cy - narrow);
        path.lineTo(cx + radius, cy + narrow);
        path.lineTo(cx + narrow, cy + narrow);
        path.lineTo(cx + narrow, cy + radius);
        path.lineTo(cx - narrow, cy + radius);
        path.lineTo(cx - narrow, cy + narrow);
        path.lineTo(cx - radius, cy + narrow);
        path.lineTo(cx - radius, cy - narrow);
        path.lineTo(cx - narrow, cy - narrow);
        path.close();
    }

    private static void lightning(Path path, float cx, float cy, float radius) {
        path.moveTo(cx + radius * 0.15f, cy - radius);
        path.lineTo(cx - radius * 0.72f, cy + radius * 0.05f);
        path.lineTo(cx - radius * 0.08f, cy + radius * 0.02f);
        path.lineTo(cx - radius * 0.28f, cy + radius);
        path.lineTo(cx + radius * 0.75f, cy - radius * 0.22f);
        path.lineTo(cx + radius * 0.13f, cy - radius * 0.15f);
        path.close();
    }

    private static void flower(Path path, float cx, float cy, float radius) {
        float petalRadius = radius * 0.46f;
        float orbit = radius * 0.52f;
        for (int index = 0; index < 6; index++) {
            double angle = Math.toRadians(index * 60f);
            path.addCircle(
                cx + (float) Math.cos(angle) * orbit,
                cy + (float) Math.sin(angle) * orbit,
                petalRadius,
                Path.Direction.CW
            );
        }
        path.addCircle(cx, cy, radius * 0.35f, Path.Direction.CW);
    }

    private static int symbolColor(int pairId) {
        return SYMBOL_COLORS[pairId % SYMBOL_COLORS.length];
    }

    private int displayedFaceColor(boolean matched) {
        if (matched) {
            return highContrast ? Color.rgb(218, 255, 229) : MATCHED_FACE;
        }
        if (!usePairColor) {
            return FACE;
        }
        return blendColors(
            pairColor,
            Color.WHITE,
            highContrast || colorBlindPatterns ? 0.18f : 0.48f
        );
    }

    private int faceBorderColor() {
        if (usePairColor) {
            return pairColor;
        }
        return fallbackToShape ? displayedSymbolColor() : Color.rgb(194, 198, 211);
    }

    private int displayedSymbolColor() {
        if (usePairColor) {
            return pairColor;
        }
        if (!trickyFallback) {
            return symbolColor(pairId);
        }
        int group = trickyGroupIndex();
        return SYMBOL_COLORS[FallbackStyleCatalog.trickyColorIndex(
            group,
            SHAPE_NAMES.length,
            SYMBOL_COLORS.length
        )];
    }

    private int fallbackShapeIndex() {
        return FallbackStyleCatalog.shapeIndex(
            pairId,
            trickyFallback,
            pairCount,
            SHAPE_NAMES.length
        );
    }

    private int fallbackVariantIndex() {
        return FallbackStyleCatalog.variantIndex(
            pairId,
            trickyFallback,
            pairCount,
            SHAPE_NAMES.length,
            VARIANT_NAMES.length
        );
    }

    private int trickyGroupIndex() {
        return FallbackStyleCatalog.trickyGroupIndex(pairId, pairCount);
    }

    private static int blendColors(int base, int overlay, float overlayAmount) {
        float amount = Math.max(0f, Math.min(1f, overlayAmount));
        float baseAmount = 1f - amount;
        return Color.rgb(
            Math.round(Color.red(base) * baseAmount + Color.red(overlay) * amount),
            Math.round(Color.green(base) * baseAmount + Color.green(overlay) * amount),
            Math.round(Color.blue(base) * baseAmount + Color.blue(overlay) * amount)
        );
    }

    private static int colorWithAlpha(int color, int alpha) {
        return Color.argb(
            Math.max(0, Math.min(255, alpha)),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        );
    }

    private static int contrastTextColor(int background) {
        return ContrastColors.blackOrWhiteFor(background);
    }

    private void updateContentDescription() {
        String prefix = "Card " + cardNumber + ", ";
        if (displayedState == GameState.CardState.HIDDEN) {
            setContentDescription(prefix + "face down");
            return;
        }
        String description;
        if (rubicsMode) {
            description = rubicsSpokenDescription.isEmpty()
                ? "Rubics cube face pattern " + (pairId + 1)
                : rubicsSpokenDescription;
        } else if (numberMode) {
            description = "number " + iconText;
        } else if (!fallbackToShape) {
            if (iconText.isEmpty()) {
                // Blank mode relies entirely on color. A stable spoken label gives TalkBack users
                // the same ability to recognize that two revealed cards carry the same cue.
                description = usePairColor ? "color " + (pairId + 1) : "blank face";
            } else {
                description = wordMode ? "word " + iconText : "symbol " + iconText;
            }
        } else {
            int shape = fallbackShapeIndex();
            int variant = fallbackVariantIndex();
            description = VARIANT_NAMES[variant] + " " + SHAPE_NAMES[shape];
        }
        if (usePairColor && !iconText.isEmpty()) {
            description += ", colored pair";
        }
        if (colorBlindPatterns) {
            description += ", " + PairPatternCue.spokenNameFor(pairId) + " pattern";
        }
        setContentDescription(prefix + (displayedState == GameState.CardState.MATCHED ? "matched " : "") + description);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
