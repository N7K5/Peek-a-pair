# Peek-a-Pair

> Remember. Match. Win together.

Peek-a-Pair is an offline, pass-the-device memory game for solo play against a bot or for 2–15 local players. Scatter up to 50 matching pairs across the tabletop, personalize the challenge, and keep a record of every round—without accounts, ads, or cloud services.

[Download the v3.10.0 debug APK](dist/Peek-a-Pair-debug.apk)

[Read the v3.10.0 release notes](RELEASE_NOTES.md)

## At a glance

| Item | Details |
|---|---|
| Players | Solo against a bot, or 2–15 people on one device |
| Board size | 4–50 matching pairs |
| New-game default | 2 people and the recommended 8 pairs |
| Android | Android 6.0 (API 23) or newer |
| Orientation | Portrait, immersive full screen |
| Connectivity | Fully offline; no network permission |
| Current version | 3.10.0 (`versionCode 31`) |

## How to play

1. Set **Number of players**, use **DETAILS** for names or photos if needed, choose **Number of card pairs**, and pick what should appear on the cards.
2. On each turn, reveal two cards. A match scores one point and earns another turn; a miss passes play to the next person.
3. When every pair has been collected, Peek-a-Pair shows the rankings, shared places for ties, player statistics, and the round duration.

Matched cards make a quick shrinking flight into the current player's score and leave the table. Choose **Play Again** to keep a running series scoreboard.

## Highlights

### Play together

- Solo mode adds an intentionally imperfect bot with Basic, Easy, Medium, and Hard memory levels.
- Local multiplayer supports 2–15 people on one phone or tablet.
- Persistent player profiles support custom names and optional camera avatars. Unnamed players use `Player A`, `Player B`, and so on.
- An optional private handoff screen hides the board until the next player taps to begin.
- The score strip follows the active player automatically when a large group does not fit on screen.
- A dedicated exit button in the setup header closes the game cleanly in fullscreen mode.
- **Random starting player** is enabled by default and fairly chooses among every participant for each new round, including the Bot in Solo. It can be disabled in Advanced Settings to always start with Player A.

### Change the challenge

- Cards are scattered into non-overlapping positions instead of a fixed grid, with a one-second opening spread animation.
- Choose Random, Faces, Animals, Food, Nature, Activities, Travel, Objects, Flags, Symbols, custom-drawn Rubics cube faces, Numbers, short Words, or color-only Blank cards.
- Rubics uses distinct 3 × 3 sticker scrambles that work without emoji support. In **Tricky mode**, neighboring faces differ by only one sticker.
- Numbers uses labels of up to three digits. Tricky pairs rearrange nearly identical digits, such as `487` and `478`.
- **Tricky mode** also uses deliberately similar symbols, words, or colors. **Moving Cards** relocates only the two cards from a missed turn.
- Pair Colors can reinforce card identity; Blank mode turns it on automatically for color-only matching.
- Board recommendations follow the player count, while the pair controls allow any size from 4 to 50.
- Advanced Settings includes a persistent 0.5–3.0 second slider for how long a wrong pair remains visible.

### Make it yours

- Themes range from system-aware minimal styles to Mixed Bubble, Aurora Glass, Neon Arcade, Paper Table, and vivid light or dark palettes. The live game uses calm, theme-first surfaces in its backdrop, header, board frame, typography, cards, and handoff screen while reserving the current player's exact color for precise identity accents.
- Fifteen card-back designs are included. Orbits, Aurora, Fireflies, Kaleido, Comet Trails, Moon Ripples, and Pixel Rain animate while visible.
- **Random each game** starts with Classic after every app restart, then avoids repeating the previous design. **Random across cards** gives each physical card a stable shuffled design for that round.
- Several card backs use stable per-card variations, making visual tracking part of the challenge.
- The game surround follows the active player's color. Sixteen concrete tabletop surfaces cover Black/white, player tint, Tiles, Mosaic, Glass tiles, Dots, Contours, Honeycomb, Glass, Reflection, Paper, Mirror, Water, Retro, Neon, and Felt.
- **Random each game** for the tabletop starts with Black/white after every app restart, then chooses a different surface for each new round without immediate repeats.
- Advanced Settings and Game History use a bordered back button that moves only vertically as the page scrolls, then stays at the top while retaining its original left inset, without an extra Done action.

### Accessibility and comfort

- High Contrast now strengthens the entire interface: solid screen backgrounds, separated surfaces, accessible text colors, heavier control borders, clearer score chips, and stronger card faces and backs.
- Scores use larger opaque chips with per-player identity, at least 4.5:1 text contrast and 3:1 boundaries; High Contrast raises score text to at least 7:1.
- Larger Cards materially increases the card footprint while preserving the fixed, non-overlapping portrait board; very dense boards still scale safely through 50 pairs.
- A color-vision-friendly palette, stable non-color pair cues, readable touch targets, and player identity shown with text as well as color.
- Reduced Motion removes card and board movement and freezes animated card backs.
- Haptics can be disabled and now use distinct short vibrations for flips, matches, and misses; turning them on plays an immediate preview. The match-only sound starts off by default.
- Older Android fonts are checked before play; unsupported emoji sets fall back to distinct symbols drawn directly by the app.

### Results and history

- Results clearly separate per-player statistics from round highlights such as comeback and pace, and include active play time for the round. Time spent paused or in the background is excluded.
- Quickest and slowest players are identified from their average time between the first and second card of each attempt.
- Tied winners and shared ranks are shown explicitly.
- Play Again tracks outright wins, ties, and current win streaks across the series.
- Game History stores lifetime totals and the latest 200 completed games, grouped into Today, Yesterday, This week, This month, and Older. Tap a game for a themed details card layered over History; swipe rows reset before it opens, while swiping left still reveals the bin action for deleting only that game.
- History can be reset from inside the app.

## Privacy

Peek-a-Pair uses only Android platform APIs and declares one normal permission, `VIBRATE`, solely for optional game feedback. It requires no runtime permission prompt and has no analytics, accounts, ads, downloads, or network access; its optional match sound is synthesized locally.

Player profiles, game settings, and history stay on the device. Optional avatars are captured through the system camera and stored in the app's private internal storage. Clearing the app's data or uninstalling it removes this local information.

## Install the ready-built APK

The APK in [`dist/Peek-a-Pair-debug.apk`](dist/Peek-a-Pair-debug.apk) is a development build intended for local testing.

With USB debugging enabled and a device visible in `adb devices`, run:

```sh
adb install -r dist/Peek-a-Pair-debug.apk
adb shell am start -n com.example.flipandfind/.MainActivity
```

The `-r` flag updates an existing copy when it was signed with the same debug key. If Android reports a signature mismatch, uninstall the older copy first; uninstalling also removes its local profiles and history.

## Build from source

### Requirements

- JDK 17
- Android SDK Platform 35
- Android SDK Build Tools 35.0.0

Android Studio is optional; the Gradle wrapper is included.

On macOS or Linux:

```sh
./gradlew clean test lintDebug assembleDebug
```

On Windows:

```bat
gradlew.bat clean test lintDebug assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

To install that build on a connected device:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

For the complete debug and release verification used during development:

```sh
./gradlew clean test lintDebug assembleDebug assembleRelease
```

The release task produces `app/build/outputs/apk/release/app-release-unsigned.apk`; it must be signed before distribution.

The [Build APK workflow](.github/workflows/build-apk.yml) can also be run manually in GitHub Actions and runs automatically on pushes to `main`. It uploads the generated debug APK as a workflow artifact.

## Project guide

Peek-a-Pair is a small native Java Android app with a single activity, programmatic screens, custom View/Canvas rendering, no XML layout files, and no AndroidX or third-party runtime libraries. The installed label is **Peek-a-Pair**; the Gradle project remains `FlipAndFind`, and the package ID is `com.example.flipandfind`.

The project uses Gradle 8.12, Android Gradle Plugin 8.7.3, and JUnit 4 for its pure-Java unit tests.

| Area | Main files |
|---|---|
| Screens, navigation, settings, and themes | [`MainActivity.java`](app/src/main/java/com/example/flipandfind/MainActivity.java), [`GameThemeChrome.java`](app/src/main/java/com/example/flipandfind/GameThemeChrome.java), [`GameThemeChromeDrawable.java`](app/src/main/java/com/example/flipandfind/GameThemeChromeDrawable.java), [`ScoreChipColors.java`](app/src/main/java/com/example/flipandfind/ScoreChipColors.java) |
| Game rules and ranking | [`GameState.java`](app/src/main/java/com/example/flipandfind/GameState.java), [`StartingPlayerSelector.java`](app/src/main/java/com/example/flipandfind/StartingPlayerSelector.java), [`Rankings.java`](app/src/main/java/com/example/flipandfind/Rankings.java) |
| Scattered board and tabletop surfaces | [`BoardLayout.java`](app/src/main/java/com/example/flipandfind/BoardLayout.java), [`ScatterLayoutEngine.java`](app/src/main/java/com/example/flipandfind/ScatterLayoutEngine.java), [`TabletopMode.java`](app/src/main/java/com/example/flipandfind/TabletopMode.java), [`TabletopSession.java`](app/src/main/java/com/example/flipandfind/TabletopSession.java), [`TabletopBackgroundDrawable.java`](app/src/main/java/com/example/flipandfind/TabletopBackgroundDrawable.java) |
| Card rendering and animated backs | [`CardTileView.java`](app/src/main/java/com/example/flipandfind/CardTileView.java), [`RubicsFaceCatalog.java`](app/src/main/java/com/example/flipandfind/RubicsFaceCatalog.java), [`NumberCatalog.java`](app/src/main/java/com/example/flipandfind/NumberCatalog.java), [`CardBackStyle.java`](app/src/main/java/com/example/flipandfind/CardBackStyle.java), [`CardBackMode.java`](app/src/main/java/com/example/flipandfind/CardBackMode.java), [`CardBackSelection.java`](app/src/main/java/com/example/flipandfind/CardBackSelection.java), [`CardBackAnimationTicker.java`](app/src/main/java/com/example/flipandfind/CardBackAnimationTicker.java) |
| Bot behavior | [`ComputerMemory.java`](app/src/main/java/com/example/flipandfind/ComputerMemory.java), [`ComputerDifficulty.java`](app/src/main/java/com/example/flipandfind/ComputerDifficulty.java) |
| Sound and haptics | [`GameFeedback.java`](app/src/main/java/com/example/flipandfind/GameFeedback.java), [`HapticPulse.java`](app/src/main/java/com/example/flipandfind/HapticPulse.java), [`MatchChimeSynth.java`](app/src/main/java/com/example/flipandfind/MatchChimeSynth.java) |
| Statistics, series, and local history | [`GameStats.java`](app/src/main/java/com/example/flipandfind/GameStats.java), [`GameSeries.java`](app/src/main/java/com/example/flipandfind/GameSeries.java), [`GameHistory.java`](app/src/main/java/com/example/flipandfind/GameHistory.java) |
| JVM unit tests | [`app/src/test`](app/src/test/java/com/example/flipandfind) |
