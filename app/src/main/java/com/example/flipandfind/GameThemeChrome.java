package com.example.flipandfind;

/**
 * Pure styling rules for the game screen's player-aware chrome.
 *
 * <p>The active player remains the strongest color cue, while the selected theme controls how
 * quickly that cue blends into the theme palette and which surface language the renderer uses.
 * Keeping these rules independent of Android drawables makes theme differences and accessibility
 * guarantees inexpensive to unit test.</p>
 */
public final class GameThemeChrome {
    /** Visual families shared by setup previews and the live game screen. */
    public enum Style {
        CLASSIC,
        FLAT,
        GLASS,
        ARCADE,
        PAPER,
        BUBBLE
    }

    /** Optional decoration a renderer can place over the base game surround. */
    public enum Decoration {
        NONE,
        HAIRLINE,
        GLASS_GLOW,
        ARCADE_GRID,
        PAPER_GRAIN,
        BUBBLES
    }

    /** Immutable colors and shape metadata for one active-player/theme combination. */
    public static final class Treatment {
        private final int surroundStart;
        private final int surroundMiddle;
        private final int surroundEnd;
        private final int playerAccentColor;
        private final int headerColor;
        private final int headerTextColor;
        private final int headerOutlineColor;
        private final int boardFrameColor;
        private final int boardFrameBorderColor;
        private final int decorationColor;
        private final Decoration decoration;
        private final float cornerScale;
        private final int panelAlpha;
        private final int strokeWidthDp;

        private Treatment(
            int surroundStart,
            int surroundMiddle,
            int surroundEnd,
            int playerAccentColor,
            int headerColor,
            int headerTextColor,
            int headerOutlineColor,
            int boardFrameColor,
            int boardFrameBorderColor,
            int decorationColor,
            Decoration decoration,
            float cornerScale,
            int panelAlpha,
            int strokeWidthDp
        ) {
            this.surroundStart = opaque(surroundStart);
            this.surroundMiddle = opaque(surroundMiddle);
            this.surroundEnd = opaque(surroundEnd);
            this.playerAccentColor = opaque(playerAccentColor);
            this.headerColor = opaque(headerColor);
            this.headerTextColor = opaque(headerTextColor);
            this.headerOutlineColor = opaque(headerOutlineColor);
            this.boardFrameColor = opaque(boardFrameColor);
            this.boardFrameBorderColor = opaque(boardFrameBorderColor);
            this.decorationColor = opaque(decorationColor);
            this.decoration = decoration;
            this.cornerScale = cornerScale;
            this.panelAlpha = panelAlpha;
            this.strokeWidthDp = strokeWidthDp;
        }

        public int getSurroundStart() {
            return surroundStart;
        }

        public int getSurroundMiddle() {
            return surroundMiddle;
        }

        public int getSurroundEnd() {
            return surroundEnd;
        }

        /** The exact active-player color, reserved for concise identity accents. */
        public int getPlayerAccentColor() {
            return playerAccentColor;
        }

        /** A defensive array ready for {@code GradientDrawable#setColors}. */
        public int[] copySurroundColors() {
            return new int[] {surroundStart, surroundMiddle, surroundEnd};
        }

        public int getHeaderColor() {
            return headerColor;
        }

        public int getHeaderTextColor() {
            return headerTextColor;
        }

        public int getHeaderOutlineColor() {
            return headerOutlineColor;
        }

        public int getBoardFrameColor() {
            return boardFrameColor;
        }

        public int getBoardFrameBorderColor() {
            return boardFrameBorderColor;
        }

        public int getDecorationColor() {
            return decorationColor;
        }

        public Decoration getDecoration() {
            return decoration;
        }

        /** Multiplier for the app's normal game-panel corner radius. */
        public float getCornerScale() {
            return cornerScale;
        }

        /** Surface opacity hint in the inclusive range 0..255. */
        public int getPanelAlpha() {
            return panelAlpha;
        }

        /** Outline width hint in density-independent pixels. */
        public int getStrokeWidthDp() {
            return strokeWidthDp;
        }
    }

    private GameThemeChrome() {}

    public static Treatment treatment(
        Style style,
        int playerColor,
        int themeBackground,
        int themeSurface,
        int themeDivider,
        boolean highContrast,
        boolean darkTheme
    ) {
        return treatment(
            style,
            playerColor,
            themeBackground,
            themeSurface,
            themeDivider,
            themeDivider,
            themeDivider,
            highContrast,
            darkTheme
        );
    }

    public static Treatment treatment(
        Style style,
        int playerColor,
        int themeBackground,
        int themeSurface,
        int themeDivider,
        int themePrimary,
        int themeAccent,
        boolean highContrast,
        boolean darkTheme
    ) {
        Style safeStyle = style == null ? Style.CLASSIC : style;
        int player = opaque(playerColor);
        int background = opaque(themeBackground);
        int surface = opaque(themeSurface);
        int divider = opaque(themeDivider);
        int primary = opaque(themePrimary);
        int accent = opaque(themeAccent);

        Profile profile = Profile.forStyle(safeStyle);
        float startPlayerAmount = highContrast ? 0.20f : profile.startPlayerAmount;
        float middleAmount = highContrast ? 0.88f : profile.middleBackgroundAmount;
        float endAmount = highContrast ? 1f : profile.endBackgroundAmount;
        float headerAmount = highContrast ? 0.92f : profile.headerSurfaceAmount;
        float frameAmount = highContrast
            ? Math.max(0.30f, profile.framePlayerAmount)
            : profile.framePlayerAmount;

        int surroundStart = GameSurfaceColors.blend(
            background,
            player,
            startPlayerAmount
        );
        int surroundMiddle = GameSurfaceColors.blend(player, background, middleAmount);
        int surroundEnd = GameSurfaceColors.blend(player, background, endAmount);
        int header = GameSurfaceColors.blend(player, surface, headerAmount);
        int headerText = ContrastColors.blackOrWhiteFor(header);
        int frame = GameSurfaceColors.blend(surface, player, frameAmount);
        int contrastTarget = ContrastColors.blackOrWhiteFor(frame);
        int themeDetail = themeDetailColor(safeStyle, divider, primary, accent);
        int frameBorder = highContrast
            ? contrastTarget
            : GameSurfaceColors.blend(themeDetail, player, profile.borderPlayerAmount);
        int headerOutline = highContrast
            ? ContrastColors.blackOrWhiteFor(header)
            : GameSurfaceColors.blend(themeDetail, player, profile.borderPlayerAmount);

        int decorationBase = darkTheme ? ContrastColors.WHITE : ContrastColors.BLACK;
        int decoration = GameSurfaceColors.blend(
            GameSurfaceColors.blend(
                themeDetail,
                decorationBase,
                profile.decorationContrastAmount
            ),
            player,
            profile.decorationPlayerAmount
        );
        if (highContrast) {
            decoration = ContrastColors.ensureMinimumContrast(
                decoration,
                contrastTarget,
                3.0,
                frame
            );
        }

        return new Treatment(
            surroundStart,
            surroundMiddle,
            surroundEnd,
            player,
            header,
            headerText,
            headerOutline,
            frame,
            frameBorder,
            decoration,
            profile.decoration,
            profile.cornerScale,
            highContrast ? 255 : profile.panelAlpha,
            highContrast ? Math.max(2, profile.strokeWidthDp) : profile.strokeWidthDp
        );
    }

    private static int themeDetailColor(
        Style style,
        int divider,
        int primary,
        int accent
    ) {
        switch (style) {
            case GLASS:
                return GameSurfaceColors.blend(primary, accent, 0.28f);
            case ARCADE:
                return GameSurfaceColors.blend(primary, accent, 0.42f);
            case PAPER:
                return GameSurfaceColors.blend(primary, accent, 0.20f);
            case BUBBLE:
                return GameSurfaceColors.blend(primary, accent, 0.58f);
            case FLAT:
                return GameSurfaceColors.blend(divider, primary, 0.18f);
            case CLASSIC:
            default:
                return divider;
        }
    }

    private static int opaque(int color) {
        return color | 0xFF000000;
    }

    private enum Profile {
        CLASSIC(0.12f, 0.84f, 0.98f, 0.88f, 0.06f, 0.52f, 0.30f, 0.16f,
            Decoration.NONE, 0.58f, 255, 1),
        FLAT(0.08f, 0.92f, 1.00f, 0.96f, 0.04f, 0.46f, 0.28f, 0.18f,
            Decoration.HAIRLINE, 0.24f, 255, 1),
        GLASS(0.16f, 0.82f, 0.96f, 0.82f, 0.08f, 0.48f, 0.38f, 0.24f,
            Decoration.GLASS_GLOW, 1.28f, 188, 1),
        ARCADE(0.10f, 0.94f, 1.00f, 0.94f, 0.10f, 0.64f, 0.42f, 0.42f,
            Decoration.ARCADE_GRID, 0.18f, 248, 2),
        PAPER(0.08f, 0.90f, 1.00f, 0.90f, 0.04f, 0.42f, 0.32f, 0.16f,
            Decoration.PAPER_GRAIN, 0.38f, 255, 1),
        BUBBLE(0.18f, 0.78f, 0.94f, 0.72f, 0.12f, 0.50f, 0.34f, 0.30f,
            Decoration.BUBBLES, 1.30f, 242, 0);

        final float startPlayerAmount;
        final float middleBackgroundAmount;
        final float endBackgroundAmount;
        final float headerSurfaceAmount;
        final float framePlayerAmount;
        final float borderPlayerAmount;
        final float decorationContrastAmount;
        final float decorationPlayerAmount;
        final Decoration decoration;
        final float cornerScale;
        final int panelAlpha;
        final int strokeWidthDp;

        Profile(
            float startPlayerAmount,
            float middleBackgroundAmount,
            float endBackgroundAmount,
            float headerSurfaceAmount,
            float framePlayerAmount,
            float borderPlayerAmount,
            float decorationContrastAmount,
            float decorationPlayerAmount,
            Decoration decoration,
            float cornerScale,
            int panelAlpha,
            int strokeWidthDp
        ) {
            this.startPlayerAmount = startPlayerAmount;
            this.middleBackgroundAmount = middleBackgroundAmount;
            this.endBackgroundAmount = endBackgroundAmount;
            this.headerSurfaceAmount = headerSurfaceAmount;
            this.framePlayerAmount = framePlayerAmount;
            this.borderPlayerAmount = borderPlayerAmount;
            this.decorationContrastAmount = decorationContrastAmount;
            this.decorationPlayerAmount = decorationPlayerAmount;
            this.decoration = decoration;
            this.cornerScale = cornerScale;
            this.panelAlpha = panelAlpha;
            this.strokeWidthDp = strokeWidthDp;
        }

        static Profile forStyle(Style style) {
            return valueOf(style.name());
        }
    }
}
