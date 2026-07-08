# Peek-a-Pair 3.3.0 — Think Fast, Look Twice

Peek-a-Pair 3.3.0 adds two new visual challenges, more control over missed turns, and a clearer look at everyone’s pace after the game.

## What's new

- Added **Rubics** cards with custom-drawn 3 × 3 cube-face scrambles.
- Added **Numbers** cards with labels of up to three digits that stay crisp on older Android devices.
- Normal Numbers boards use clearly separated labels. Tricky mode introduces easy-to-confuse rearrangements such as `487` and `478`.
- Rubics and Blank are now clearly marked as hard choices in the card selector.
- Added a fullscreen exit button to the top-right of the setup screen. The in-game leave button remains available during a round.

## Timing and results

- Added a persistent **Wrong-pair reveal time** slider in Advanced Settings.
- Missed pairs can remain visible for 0.5–3.0 seconds in 0.1-second steps; the default remains 0.9 seconds.
- Match timing is unchanged, so successful pairs still clear quickly.
- Results now show every player’s average decision time and identify all tied quickest and slowest players.
- Decision timing measures active time between revealing the first and second card, excluding time spent in the background or at the leave-game dialog.

## Accessibility and compatibility

- Number labels and cube faces are rendered directly by the app and do not depend on emoji availability.
- Rubics supports stronger high-contrast outlines, stable non-color sticker cues, and descriptive TalkBack labels.
- Timing controls include a live value label, keyboard increments, and an updating accessibility description.
- Existing profiles, preferences, series data, and game history remain compatible.
- Android 6.0 (API 23) or newer is supported.

## Version

- Version name: `3.3.0`
- Version code: `24`


## Verification

- Debug unit tests: 200 passed
- Release unit tests: 200 passed
- Android lint: no issues found
- Debug and unsigned release APK builds: successful
