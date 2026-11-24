# Plan: native-android Home and Title details

Requirement: [04 - native-android: Home and Title details](../subtasks/04-native-android-home-and-title-details.md)
Status: Done

## Summary
Home and Title details in `native-android` move to tokens the way `cmp` did in #49, and switch to the shared
`FavoriteHeart` that `cmp` got in #48. Each file is generated from the final `cmp` file by the converter used in 03
(package, `R.`, Android resource and Koin imports, `internal`). The converter reproduces the current
`native-android` Home and Title details files from the pre-token `cmp` files, apart from two `native-android`
differences, which are kept. After this, `native-android` holds no raw color, text size or corner shape outside
its design system, and the old heart drawables are deleted.

## Decisions
- Every value, color role, rounding and layout choice is taken from plan 02 and its deviations
  (`02-cmp-home-and-title-details.md`), including:
  - The Details page uses `surface`; the synopsis uses `onSurface` and the date line `onSurfaceVariant`.
  - Section headings use `titleLarge`. The Home count badge uses `secondaryContainer`/`onSecondaryContainer`.
  - The rating ring value auto-sizes inside the 56dp ring, with the font scale capped at 1.15.
  - The date line is a `FlowRow`, with the runtime kept as one group.
  - Sizes used in one place are written inline; `PhotoBorder` stays a named constant (used three times).

  No new decision on values is made here.
- `native-android` differences kept from the current code:
  - `CategoryTabs`: `TitleCategory.label` stays `@StringRes Int` (Android resources), not `StringResource`.
  - `FavoritesSection`: the remove-heart label stays `stringResource(R.string.remove_favorite_heart)`, as
    `native-android` has no `removeFavoriteLabel()`.
- Hearts: Home (`FavoritesSection.RemoveButton`) and Title details (`DetailsTopBar.FavoriteButton`) use
  `FavoriteHeart`. `res/drawable/ic_favorite_on.xml`, `ic_favorite_off.xml` and `ic_favorite_remove.xml` are deleted
  once nothing references them.
- `RatingRingFontScaleTest` is copied from `cmp` into `app/src/test/.../titledetails/component/`, with the same
  three cases.
- `native-android` uses Jetpack Compose BOM 2026.09.00, which has `TextAutoSize` and `FlowRow(itemVerticalAlignment)`,
  so the `cmp` code needs no change.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Final screen and component code | `cmp` Home and Title details after #49 | `cmp/feature/home/ui`, `cmp/feature/title-details/ui` |
| Tokens and shared components | `TmdbSpacing`, `TmdbSize`, `PosterRowDefaults`, `FavoriteHeart`, `RetryMessage`, `RemoveFavoriteDialog` (from 03) | `native-android/app/src/main/kotlin/com/banhmilabs/tmdb/nativeapp/designsystem/` |
| Converter | `cmp` → `native-android` converter from 03, plus the two `native-android` fix-ups above | Forge scratch script |
| Ring font-scale test | `RatingRingFontScaleTest` | `cmp/feature/title-details/ui/src/androidHostTest/...` |

## Changes overview
All paths are under `native-android/app/src/main/`. `<pkg>` is `kotlin/com/banhmilabs/tmdb/nativeapp`.

| Area | Files (new / modified) | Why |
|---|---|---|
| Home | `<pkg>/home/HomeScreen.kt`; `home/component/HomeHeader.kt`, `SearchField.kt`, `CategoryTabs.kt`, `FavoritesSection.kt`, `RowMessage.kt` (modify) | Home on tokens, shared heart |
| Title details | `<pkg>/titledetails/TitleDetailsScreen.kt`; `titledetails/component/SectionHeading.kt`, `DetailsHeader.kt`, `RatingRing.kt`, `CastRow.kt`, `DetailsTopBar.kt`, `TrailerButton.kt`, `GenreChips.kt`, `DetailsArtwork.kt` (modify); `titledetails/DetailsColors.kt` (delete) | Title details on tokens, shared heart |
| Resources | `res/drawable/ic_favorite_on.xml`, `ic_favorite_off.xml`, `ic_favorite_remove.xml` (delete) | Replaced by `FavoriteHeart` |
| Tests | `app/src/test/<pkg>/titledetails/component/RatingRingFontScaleTest.kt` (new) | Font scale cap |

## Steps
### Step 1: Home on tokens  (AC-3, AC-4, AC-6, AC-9, EC-1, EC-5) ✅
- Files: the six Home files above.
- What: generate each file from its `cmp/feature/home/ui` counterpart with the converter. Then apply the two
  `native-android` fix-ups: `@StringRes Int` labels in `CategoryTabs`, and the `stringResource` remove label in
  `FavoritesSection`. In the result:
  - `ScreenPadding` and `RowMinHeight` are gone; rows use `PosterRowDefaults.PosterHeight`.
  - The count badge uses `secondaryContainer`; the heart is `FavoriteHeart(isFavorite = true)`.
- Compile: `./gradlew :app:compileDebugKotlin` (in `native-android/`)

### Step 2: Title details on tokens  (AC-3, AC-4, AC-5, AC-6, AC-9, EC-1, EC-4, EC-5) ✅
- Files: the nine Title details files above; delete `DetailsColors.kt`.
- What: generate each file from its `cmp/feature/title-details/ui` counterpart with the converter. In the result:
  - The page uses `surface`, the divider `outlineVariant`, and section headings `titleLarge`.
  - The date line is a `FlowRow`.
  - `RatingRing` has `MAX_RING_FONT_SCALE`, `ringFontScale` and an auto-sized `BasicText`.
  - `CastRow` keeps `private val PhotoBorder = 4.dp`.
  - The favorite button is `FavoriteHeart(isFavorite)` with `toggleable`.
- Unit tests: `app/src/test/kotlin/com/banhmilabs/tmdb/nativeapp/titledetails/component/RatingRingFontScaleTest.kt`:
  `smallScalesPassThrough`, `largeScalesAreCappedAtTheMaximum`, `theMaximumItselfPassesThrough`.
- Compile: `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`

### Step 3: Remove the old hearts  (AC-3) ✅
- Files: delete `res/drawable/ic_favorite_on.xml`, `ic_favorite_off.xml` and `ic_favorite_remove.xml`.
- What: `grep -rn 'ic_favorite_' native-android/app/src` finds nothing before the files are deleted.
- Compile: `./gradlew :app:compileDebugKotlin`

### Step 4: Run checks ✅
- In `native-android/`: `./gradlew :app:detekt :app:testDebugUnitTest` (`--auto-correct` first if ktlint fails).
- Raw-value grep (AC-3) over `home` and `titledetails` under `native-android/app/src/main/kotlin`:
  `grep -nE 'Color\(0x|Color\.(White|Black)|RoundedCornerShape\(|[0-9]\.sp\b|TextStyle\('` finds nothing. The
  `n.dp` values left are the ones in plan 02's mapping table.
- Manual (Pixel_8 emulator): Home and Title details at 100% and at 200% font scale, then reset the font scale to 1.0.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-3 (completes `native-android`) | 1, 2, 3 | Step 4 raw-value grep |
| AC-4 (completes `native-android`) | 1, 2 | Manual: Home and Details match `cmp` (subtask How to test 1–2) |
| AC-6 (completes `native-android`) | 1, 2 | Manual: 200% font scale (How to test 3); `RatingRingFontScaleTest` |
| AC-9 (partial: `native-android`) | 1, 2 | Manual: a favorite shows the filled heart on Home, My favorites and Title details; a non-favorite shows the outline heart on Title details |
| EC-1 (`native-android`) | 1, 2 | Manual: a long title name at 200% wraps or ends with "…" |
| EC-4 (`native-android`) | 2 | `RatingRingFontScaleTest`; manual: posters and the ring keep their size |
| EC-5 (`native-android` Home and Details) | 1, 2 | Manual: tabs, view all, hearts, back and trailer stay tappable |

## Out of scope
- `native-ios` -> 05, 06.
- The issues found in `cmp` at the largest text size: "View all" wrapping, cast names breaking mid-word, and the
  floating top bar. `native-android` mirrors `cmp`, so any fix belongs to both apps, after 07.
- Design docs and the project convention -> 07.

## Risks
- This plan builds on 03 (PR #50). Mitigation: forge starts after #50 is merged, or on top of its branch.
- Android may render `TextAutoSize` or `FlowRow` slightly differently from Compose Multiplatform on iOS.
  Mitigation: the manual check at 200% on the emulator covers the ring and the date line.

## Deviations
- None in the code: every file was generated from the `cmp` file with the 03 converter plus the two planned
  `native-android` fix-ups.
- Manual check: Home and Title details were checked on the Pixel_8 emulator at 100% font scale. The check at 200%
  was not completed, because the emulator switched to another app during the run. The 200% behavior is the same
  code `cmp` was checked with on iOS (#49).
