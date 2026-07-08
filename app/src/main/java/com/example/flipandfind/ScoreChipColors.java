package com.example.flipandfind;

/**
 * Pure color rules for the live game's compact score chips.
 *
 * <p>Score chips sit on a player-tinted header, so borrowing the header's text color is unsafe:
 * an inactive surface-colored chip can be on the opposite side of the luminance range. This
 * treatment keeps every fill opaque, chooses text against that actual fill, and gives the chip a
 * boundary that remains visible against the current header.</p>
 */
public final class ScoreChipColors {
    private static final double NORMAL_TEXT_CONTRAST = 4.5d;
    private static final double HIGH_TEXT_CONTRAST = 7.0d;
    private static final double GRAPHICAL_CONTRAST = 3.0d;

    /** Immutable rendering colors for one score chip. */
    public static final class Treatment {
        private final int fillColor;
        private final int textColor;
        private final int outlineColor;
        private final int strokeWidthDp;

        private Treatment(
            int fillColor,
            int textColor,
            int outlineColor,
            int strokeWidthDp
        ) {
            this.fillColor = opaque(fillColor);
            this.textColor = opaque(textColor);
            this.outlineColor = opaque(outlineColor);
            this.strokeWidthDp = strokeWidthDp;
        }

        public int getFillColor() {
            return fillColor;
        }

        public int getTextColor() {
            return textColor;
        }

        public int getOutlineColor() {
            return outlineColor;
        }

        public int getStrokeWidthDp() {
            return strokeWidthDp;
        }
    }

    private ScoreChipColors() {}

    /**
     * Returns a readable score-chip treatment for the current game header.
     *
     * @param playerColor the color assigned to the player represented by this chip
     * @param headerColor the opaque base color immediately behind the chip
     * @param themeSurface the theme's normal panel/surface color
     * @param active whether this player currently has the turn
     * @param highContrast whether enhanced contrast is enabled
     * @param darkTheme whether the selected theme uses dark surfaces
     */
    public static Treatment treatment(
        int playerColor,
        int headerColor,
        int themeSurface,
        boolean active,
        boolean highContrast,
        boolean darkTheme
    ) {
        int player = opaque(playerColor);
        int header = opaque(headerColor);
        int surface = opaque(themeSurface);

        int fill;
        if (active) {
            // The current player remains the strongest and most immediate color cue.
            fill = player;
        } else {
            // A quiet player tint preserves identity without competing with the active score.
            float playerAmount = highContrast ? 0.18f : darkTheme ? 0.16f : 0.11f;
            fill = GameSurfaceColors.blend(surface, player, playerAmount);
        }

        int text = ContrastColors.blackOrWhiteFor(fill);
        if (highContrast) {
            int fillTarget = text == ContrastColors.WHITE
                ? ContrastColors.BLACK
                : ContrastColors.WHITE;
            fill = ContrastColors.ensureMinimumContrast(
                fill,
                fillTarget,
                HIGH_TEXT_CONTRAST,
                text
            );
            text = ContrastColors.blackOrWhiteFor(fill);
        }

        // The outline is judged against the surrounding header rather than the chip fill: its
        // purpose is to stop a similarly colored active chip from disappearing into the header.
        int outlineTarget = ContrastColors.blackOrWhiteFor(header);
        int outline = highContrast
            ? outlineTarget
            : ContrastColors.ensureMinimumContrast(
                player,
                outlineTarget,
                GRAPHICAL_CONTRAST,
                header
            );

        // blackOrWhiteFor guarantees at least the WCAG small-text threshold. Keep this defensive
        // adjustment in case the fill-generation rules evolve independently later.
        text = ContrastColors.ensureMinimumContrast(
            text,
            text,
            highContrast ? HIGH_TEXT_CONTRAST : NORMAL_TEXT_CONTRAST,
            fill
        );

        return new Treatment(
            fill,
            text,
            outline,
            highContrast ? 3 : active ? 2 : 1
        );
    }

    private static int opaque(int color) {
        return color | 0xFF000000;
    }
}
