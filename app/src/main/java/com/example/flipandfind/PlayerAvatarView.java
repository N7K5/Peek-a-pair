package com.example.flipandfind;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.io.File;

/** Circular player photo with a colored, labeled fallback for default profiles. */
public final class PlayerAvatarView extends View {
    private static final int DEFAULT_SIZE_DP = 48;
    private static final int[] DEFAULT_COLORS = {
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

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix bitmapMatrix = new Matrix();

    private String displayName = "Player A";
    private String avatarLabel = "A";
    private String photoPath = "";
    private int fallbackColor = DEFAULT_COLORS[0];
    private Bitmap photo;
    private BitmapShader photoShader;
    private String decodedPhotoPath;
    private long decodedPhotoLength = -1L;
    private long decodedPhotoLastModified = -1L;
    private int decodedPhotoTarget = -1;
    private boolean decodedPhotoWasFile;
    private boolean decodedPhotoSucceeded;

    public PlayerAvatarView(Context context) {
        this(context, null);
    }

    public PlayerAvatarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayerAvatarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(2f));
        borderPaint.setColor(Color.argb(150, 255, 255, 255));
        updateDescription();
    }

    public void setProfile(PlayerProfile profile, int oneBasedPosition) {
        setProfile(
            profile,
            oneBasedPosition,
            DEFAULT_COLORS[(oneBasedPosition - 1) % DEFAULT_COLORS.length]
        );
    }

    public void setProfile(PlayerProfile profile, int oneBasedPosition, int playerColor) {
        if (profile == null) {
            throw new IllegalArgumentException("A player profile is required");
        }
        if (oneBasedPosition < 1) {
            throw new IllegalArgumentException("Roster positions are one-based");
        }
        displayName = profile.getDisplayName(oneBasedPosition);
        avatarLabel = profile.getAvatarLabel(oneBasedPosition);
        photoPath = profile.getPhotoPath();
        fallbackColor = playerColor;
        labelPaint.setColor(contrastTextColor(playerColor));
        loadPhoto();
        updateDescription();
        invalidate();
    }

    /** Low-level convenience API for transient players such as a computer opponent. */
    public void setAvatar(
        String name,
        String label,
        String internalPhotoPath,
        int playerColor
    ) {
        displayName = clean(name, "Player");
        avatarLabel = clean(label, "?");
        photoPath = clean(internalPhotoPath, "");
        fallbackColor = playerColor;
        labelPaint.setColor(contrastTextColor(playerColor));
        loadPhoto();
        updateDescription();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int suggested = dp(DEFAULT_SIZE_DP);
        int width = resolveSize(Math.max(suggested, getSuggestedMinimumWidth()), widthMeasureSpec);
        int height = resolveSize(Math.max(suggested, getSuggestedMinimumHeight()), heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if ((width > oldWidth || height > oldHeight) && !photoPath.isEmpty()) {
            loadPhoto();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = Math.max(0f, Math.min(getWidth(), getHeight()) / 2f - dp(1f));

        if (photo != null && !photo.isRecycled()) {
            float scale = Math.max(
                getWidth() / (float) photo.getWidth(),
                getHeight() / (float) photo.getHeight()
            );
            float dx = (getWidth() - photo.getWidth() * scale) / 2f;
            float dy = (getHeight() - photo.getHeight() * scale) / 2f;
            bitmapMatrix.reset();
            bitmapMatrix.setScale(scale, scale);
            bitmapMatrix.postTranslate(dx, dy);
            photoShader.setLocalMatrix(bitmapMatrix);
            fillPaint.setShader(photoShader);
            canvas.drawCircle(centerX, centerY, radius, fillPaint);
            fillPaint.setShader(null);
        } else {
            fillPaint.setColor(fallbackColor);
            fillPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(centerX, centerY, radius, fillPaint);

            labelPaint.setTextSize(Math.min(getWidth(), getHeight()) * labelScale());
            Paint.FontMetrics metrics = labelPaint.getFontMetrics();
            float baseline = centerY - (metrics.ascent + metrics.descent) / 2f;
            canvas.drawText(avatarLabel, centerX, baseline, labelPaint);
        }

        canvas.drawCircle(centerX, centerY, radius - dp(1f), borderPaint);
    }

    private void loadPhoto() {
        int target = Math.max(dp(DEFAULT_SIZE_DP), Math.max(getWidth(), getHeight()));
        File file = photoPath.isEmpty() ? null : new File(photoPath);
        boolean isFile = file != null && file.isFile();
        long fileLength = isFile ? file.length() : -1L;
        long fileLastModified = isFile ? file.lastModified() : -1L;

        if (matchesDecodedPhoto(
            photoPath,
            isFile,
            fileLength,
            fileLastModified,
            target
        ) && (!decodedPhotoSucceeded || (photo != null && !photo.isRecycled()))) {
            return;
        }

        decodedPhotoPath = photoPath;
        decodedPhotoWasFile = isFile;
        decodedPhotoLength = fileLength;
        decodedPhotoLastModified = fileLastModified;
        decodedPhotoTarget = target;
        decodedPhotoSucceeded = false;
        photo = null;
        photoShader = null;
        if (!isFile) {
            return;
        }

        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(photoPath, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return;
            }

            int sampleSize = 1;
            while (bounds.outWidth / (sampleSize * 2) >= target
                && bounds.outHeight / (sampleSize * 2) >= target) {
                sampleSize *= 2;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            photo = BitmapFactory.decodeFile(photoPath, options);
            if (photo != null) {
                photoShader = new BitmapShader(
                    photo,
                    Shader.TileMode.CLAMP,
                    Shader.TileMode.CLAMP
                );
                decodedPhotoSucceeded = true;
            }
        } catch (RuntimeException ignored) {
            photo = null;
            photoShader = null;
            decodedPhotoSucceeded = false;
        }
    }

    private boolean matchesDecodedPhoto(
        String path,
        boolean isFile,
        long fileLength,
        long fileLastModified,
        int target
    ) {
        return decodedPhotoPath != null
            && decodedPhotoPath.equals(path)
            && decodedPhotoWasFile == isFile
            && decodedPhotoLength == fileLength
            && decodedPhotoLastModified == fileLastModified
            && decodedPhotoTarget == target;
    }

    private float labelScale() {
        int codePoints = avatarLabel.codePointCount(0, avatarLabel.length());
        return codePoints > 1 ? 0.34f : 0.44f;
    }

    private void updateDescription() {
        setContentDescription((photo == null ? "Avatar for " : "Photo of ") + displayName);
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private static int contrastTextColor(int background) {
        return ContrastColors.blackOrWhiteFor(background);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
