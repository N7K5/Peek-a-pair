# Peek-a-Pair 3.4.0 — Shuffle the Table

Peek-a-Pair 3.4.0 makes customization faster to navigate and gives every tabletop—and even every card—more personality.

## Tabletop backgrounds

- Added five patterned surfaces: **Tiles**, **Mosaic**, **Glass tiles**, **Dots**, and **Contours**.
- Kept the existing Theme black/white and Player tint choices for a total of seven tabletop options.
- Every option now includes a small live preview on the left of its selector.
- Pattern colors remain subtle, theme-aware, and tied to the current player without reducing card readability.
- Procedural patterns are deterministic and allocation-free while drawing.

## Card-back shuffling

- Added **Random each game**. Its first game after every app restart uses Classic; later games choose another design and never immediately repeat the previous one.
- Added **Random across cards**, which shuffles all 15 designs across the cards on one table.
- A card keeps its assigned design when it flips, moves, or survives an Activity recreation.
- Mixed animated card backs continue to share the existing battery-conscious frame ticker and respect Reduced Motion.
- Existing fixed card-back choices and saved preferences remain compatible.

## Navigation polish

- Removed the unnecessary Done buttons from Advanced Settings and Game History.
- Their top-left back buttons now remain visible while the page scrolls.
- Sticky navigation stays inside display-cutout safe areas and retains a 48 dp accessible touch target.

## Compatibility

- Android 6.0 (API 23) or newer
- Version `3.4.0` (`versionCode 25`)
- Existing player profiles, settings, and game history are preserved when updating from a build signed with the same key.

## Verification

- Debug unit tests: 221 passed
- Release unit tests: 221 passed
- Android lint: no issues found
- Debug and unsigned release APK builds: successful
