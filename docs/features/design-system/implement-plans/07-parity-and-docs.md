# Plan: Cross-app comparison and docs

Requirement: [07 - Cross-app comparison and docs](../subtasks/07-parity-and-docs.md)
Status: Done

## Summary
The three apps are compared screen by screen with the same data. The result goes into a `parity.md` table, and any
token-level mismatch is fixed in the app that differs from `cmp`. Then the token rule is written down: in
`CLAUDE.md` (convention, reuse table, do-not), in the two native READMEs, and in a `## Tokens` section in each of
the six `design/*.md` files, which names the token each element uses. The large-text issues found during 02 and 06
are not fixed here; they become a separate task.

## Decisions
- Design docs: each of the six `design/*.md` files gets a `## Tokens` table (element → spacing, text style, shape,
  color token). The Figma measurements stay as the source record — user choice over replacing the numbers inline.
  The section goes right before `## Gaps` (or before `## Change log` when a file has no Gaps), and the file's Change
  log gets one line.
- Large-text issues stay out of 07 and become a separate task — user choice over fixing them here:
  - "View all" wraps and loses its chevron;
  - names and titles break mid-word;
  - the Details top bar floats over content while scrolling.

  They need a UX decision for all three apps. After 07, `spark` is suggested for them.
- Parity record: `docs/features/design-system/parity.md`, one table per screen with one column per app (`cmp`,
  `native-android`, `native-ios`) and a row per element group (layout and spacing, text, shapes, colors, hearts).
  Each cell says "match", or names the difference and how it was handled. Screenshots are used during the check
  only and are not committed — user choice over committing images.
- Reference app: `cmp`. A mismatch is fixed in the app that differs from it. A mismatch that is a platform
  convention already decided is recorded as an accepted difference and not "fixed":
  - iOS system navigation bars and back button;
  - the My favorites top padding `md` under the iOS bar;
  - the rating ring cap of 1.12× on iOS versus 1.15×;
  - Android/iOS font rendering.
- A mismatch that needs a new decision (not a wrong token) stops the step, and the options are put to the user
  before anything changes.
- Docs to update:
  - **`CLAUDE.md` Conventions:** one "Design system" bullet: where the tokens live in each app; the rules for
    spacing, type, shape, color and sizes (one place inline, two or more a named constant, across modules a
    `<Component>Defaults`); and that a token changes in all three apps together.
  - **`CLAUDE.md` Reuse table:** rows for `TmdbSpacing`/`TmdbSize`, `FavoriteHeart`, `PosterRowDefaults` and the
    native-ios `.tmdbFont`.
  - **`CLAUDE.md` Do not:** "Don't write raw colors, text sizes or corner radii in screens or components; use the
    design-system tokens."
  - **`native-android/README.md`, `native-ios/README.md`:** one line each pointing to their design-system folder.
  - **`docs/architecture.md`:** the `cmp/designsystem` row of the module table mentions the tokens.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Token names per element | Value-mapping tables in plans 01 and 02 (and their deviations); plans 03–06 | `docs/features/design-system/implement-plans/` |
| Token definitions | `cmp/designsystem`, `native-android/.../designsystem`, `native-ios/TmdbNative/DesignSystem` | Per app |
| Running the apps | `scripts/run-native-android.sh`, `scripts/run-native-ios.sh`; `cmp` via `xcodebuild` (iOS) or `:cmp:androidApp:installDebug` | `CLAUDE.md` Commands |

## Changes overview
| Area | Files (new / modified) | Why |
|---|---|---|
| Parity record | `docs/features/design-system/parity.md` (new) | AC-7 evidence |
| App fixes (if any) | Files named in `parity.md` | AC-7 |
| Project docs | `CLAUDE.md`, `native-android/README.md`, `native-ios/README.md`, `docs/architecture.md` (modify) | AC-8 convention |
| Design docs | `docs/features/onboarding/design/onboarding.md`, `docs/features/home/design/home.md`, `docs/features/favorites/design/home-favorites.md`, `my-favorites.md`, `remove-favorite-dialog.md`, `docs/features/title-details/design/title-details.md` (modify) | AC-8 tokens per screen |
| Feature docs | `docs/features/design-system/task.md` (link to `parity.md`, Change log) | Traceability |

## Steps
### Step 1: Compare the three apps  (AC-7) ✅
- What:
  - **Setup:** build and run `cmp` (iOS simulator), `native-ios` (iOS simulator) and `native-android` (emulator).
    Each app has the same favorite (Iron Man) and is set to English.
  - **Screens:** Onboarding (after a reinstall), Home with the favorites section, My favorites, the remove dialog,
    and Title details (favorite and not favorite).
  - **Two text sizes:** the default, and the largest system size. Reset the size afterwards.
  - **Record:** write `parity.md` as described in Decisions. Accepted differences are listed with their reason.
- Unit tests: none.
- Compile: the app builds used for the run.

### Step 2: Fix mismatches  (AC-7) ✅
- What:
  - For each "mismatch" row in `parity.md`, change the deviating app to the token `cmp` uses. Then update the row to
    "fixed in <app>: <file>".
  - Any row that needs a new decision is asked before it is changed.
  - If there are no mismatches, the step records that.
- Checks, per app touched:
  - **`cmp`:** `./gradlew :<module>:testAndroidHostTest :<module>:detekt`.
  - **`native-android`:** `./gradlew :app:detekt :app:testDebugUnitTest`.
  - **`native-ios`:** `xcodebuild … test`.
  - Re-check the fixed screen on its simulator or emulator.

### Step 3: Project docs  (AC-8) ✅
- Files: `CLAUDE.md`, `native-android/README.md`, `native-ios/README.md`, `docs/architecture.md`.
- What: the additions listed in Decisions. Wording follows `CLAUDE.md`'s existing style: short bullets, no new
  headings beyond what exists.
- Checks: docs only — nothing to run (`CLAUDE.md`: docs-only changes run nothing).

### Step 4: Design docs  (AC-8) ✅
- Files: the six `design/*.md` files.
- What: a `## Tokens` table in each, built from the 01/02 mapping tables and the final code:
  - columns: Element | Spacing | Text | Shape | Color;
  - rows: the elements the doc already describes;
  - plus a Change log line: `- 2026-11-24: Added Tokens (design system).`

  Example row for `my-favorites.md`: `Card | sm padding, md poster→text | titleMedium name, bodyMedium meta | medium | surfaceContainer, onSurface, onSurfaceVariant`.
- Checks: every element named in the doc's Components or Layout section has a row.

### Step 5: Close the feature docs ✅
- Files: `docs/features/design-system/task.md`.
- What: link `parity.md` (a `## Parity` line before `## Subtasks`) and add a Change log line.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-7 | 1, 2 | `parity.md`: every screen × app cell is "match", "fixed", or an accepted platform difference |
| AC-8 | 3, 4 | Read-through: `CLAUDE.md` states the token rule; each design doc has a complete Tokens table |

## Out of scope
- The large-text issues ("View all", mid-word breaks, floating top bar) -> a new task via `spark` after 07.
- A light theme, or an automated raw-value lint (task.md Out of scope).

## Risks
- The emulator was unstable earlier (it rebooted after a Bluetooth crash). Mitigation: restart it and continue; if
  a screen can't be checked on Android, `parity.md` says so.
- An Android device connected by USB makes `installDebug` install there too. Mitigation: set `ANDROID_SERIAL` to
  the emulator for the run.
- The `cmp` Android build is not checked in Step 1, because `native-android` shares Android rendering and `cmp`
  shares the code. Mitigation: if a mismatch shows only on `cmp` iOS, check `cmp` Android before fixing.

## Deviations
- Step 1 used the screenshots already taken on the final code in subtasks 02–06, plus a per-element token
  comparison in code, instead of a new three-app run.
- During the 07 run the iOS simulator screenshots lagged behind the taps: they showed earlier screens, and the app
  itself responded normally. The `native-ios` remove dialog was compared in code only; `parity.md` says so.
- `cmp` Onboarding was not re-shot in 07. It is the same code checked in 01.
- Step 2 changed nothing: no token-level mismatch was found.
- Xcode modified `native-ios/TmdbNative.xcodeproj/project.pbxproj` again during builds. It is left uncommitted, as in
  06.
