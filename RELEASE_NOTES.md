# Peek-a-Pair 3.10.0 — Cleaner Tables, Clearer Scores

Peek-a-Pair 3.10.0 gives the game screen a calmer visual hierarchy and makes every player's score immediately readable.

## Themes, refined

- Reworked the default theme so its deep teal surfaces remain visible instead of being replaced by a full-screen player color.
- Every theme is now **theme-first**: the selected palette controls the backdrop, header, frame, outlines, and decoration.
- The current player's exact color is reserved for concise identity cues—the header rule, avatar, active score, board border, and a subtle surrounding tint.
- Regular dark and light palettes now remain visually distinct during play, not only in their selector previews.
- Glass, Arcade, Paper, Bubble, Flat, and Classic decoration now incorporates each theme's primary and accent colors.
- Simplified the default header shape and removed its heavy capsule-like outline.

## Scores you can scan

- Increased score labels from 12sp to 14sp with larger 42dp-high, 72dp-wide touch-safe chips.
- Every inactive player now receives an opaque, lightly player-tinted surface instead of disappearing into the header.
- The active score keeps the player's exact color with a thicker contrasting outline and subtle elevation.
- Score text maintains at least **4.5:1** contrast and chip boundaries at least **3:1** across all 15 player colors and representative light/dark themes.
- High Contrast mode raises score text to at least **7:1** and uses 3dp boundaries.
- The existing active-player auto-scroll continues to keep the current score visible in large groups.

## Compatibility

- Android 6.0 (API 23) or newer
- Version `3.10.0` (`versionCode 31`)
- Existing profiles, preferences, series data, and game history remain compatible when updating from a build signed with the same key.

## Verification

- Debug unit tests: 275 passed
- Release unit tests: 275 passed
- Android lint: no issues found
- Debug and unsigned release APK builds: successful
