# Plan: cmp Home and Title details

Requirement: [02 - cmp: Home and Title details](../subtasks/02-cmp-home-and-title-details.md)
Status: Approved

## Summary
Home (header, search field, category tabs, favorites section, rows) and Title details (screen, header, rating ring,
cast, top bar, trailer button, genre chips, artwork) move to the tokens from 01. Spacing, type, shapes and colors
come only from `TmdbSpacing` and `MaterialTheme`. Sizes that belong to one component stay in its file, written inline
when used once and rounded to the 4dp grid, following the task.md rule of 2026-11-24. `TmdbSize` gains nothing:
every new size is used by one feature only. `DetailsColors` is deleted, and the Details page moves to the same
`surface` background as the other screens. Fixed heights that hold text become minimum heights, and the rating
ring's value scales with the system font up to a cap, so it always fits inside the ring.

## Decisions
- Details page background is `surface` (`#130E1F`) — user choice over `surfaceContainer` (`#211A30`). The artwork
  fade ends in `surface` too. The other Details colors map to roles: track `#303243` -> `surfaceContainerHigh`, ring
  center `#15161D` -> `surface`, divider black 48% -> `outlineVariant`.
- Details secondary text: the date/runtime line `#BBBBBB` -> `onSurfaceVariant`; the synopsis `#CCCCCC` ->
  `onSurface` — user choice over `onSurfaceVariant` for both, because the synopsis is the main text to read.
- Section headings ("Popular", "Cast", "Genres", "Recommendations"), 18sp Bold today, use `titleLarge` (22 Bold) —
  user choice over `titleMedium` (16) from the rounding rule, to keep a clear hierarchy over buttons and body
  text. This is a deliberate exception to the rounding rule. The Home favorites section title (20sp Bold) rounds to
  `titleLarge` by the rule, so all section titles on Home and Details match.
- Rating ring value: `typography.titleMedium` (was a fixed 15dp), scaled with the system font up to
  `MAX_RING_FONT_SCALE = 1.15f` — user choice over a fixed size, or a ring that grows. The ring stays 56dp (EC-4). The
  content description already gives the full rating to screen readers.
- Home favorites count badge: `secondaryContainer` with `onSecondaryContainer` text (was pink `#FF1F8A` on
  `#381A39`, about 4.3:1, below AA for 12sp). This is a small visual change: the text becomes light pink
  (`#FFB1C8`, about 9:1).
- Sizes stay in their component's file, rounded to the nearest multiple of 4 (ties take the larger value), and are
  written inline where used once (task.md, 2026-11-24). Only the cast photo border, used three times in `CastRow`,
  stays a `private val PhotoBorder = 4.dp`. Stroke and border widths keep their value: the ring stroke (6dp) and the
  cast photo border (4dp). `RingSize`, `ArtworkHeight` and the other single-use constants (`HeaderGlowHeight`,
  `RowSpinnerSize`, `DIVIDER_ALPHA`, `PILL_ALPHA`, `NAME_MAX_LINES`, `UNSELECTED_ALPHA`, `HINT_ALPHA`) are deleted.
- The empty and error rows on Home use `PosterRowDefaults.PosterHeight` as their minimum height in place of
  `RowMinHeight` (188dp, the same value), so a message row is as tall as a poster row. `RowMinHeight` is deleted.
- `ScreenPadding` (30dp) becomes `TmdbSpacing.xxl` (32dp) in both features, and the `internal val ScreenPadding`
  in `HomeScreen.kt` is deleted. The favorites card inset is written as `TmdbSpacing.xxl - TmdbSpacing.sm`, so the
  card's content still lines up with the screen padding.
- The unselected category tab gets no horizontal padding through a conditional `Modifier`, in place of `0.dp`.
- Home's large bottom padding (79dp) rounds to `xxxl` (48dp), the largest spacing step. The scroll content
  simply ends sooner; nothing sits below it.
- Rounding examples on these screens (EC-3, ties take the larger step): 30 -> 32, 46 -> 48, 41 -> 48, 40 -> 48,
  23 -> 24, 21 -> 20, 17 -> 16, 14 -> 16, 10 -> 12, 6 -> 8, 5 -> 4, 3 -> 4; sizes 57 -> 56, 46 -> 48, 31 -> 32,
  30 -> 32, 22 -> 24, 19 -> 20, 97 -> 96, 147 -> 148, 75 -> 76, 493 -> 492, 522 -> 520; text 26 -> 28, 15 -> 16,
  13 -> 14.

### Value mapping (old -> token or constant)
| Where | Old | New |
|---|---|---|
| Home content top / bottom | 20 / 79dp | `lg` / `xxxl` |
| Header -> search, search -> tabs, tabs -> featured | 17 / 24 / 30dp | `md` / `xl` / `xxl` |
| Featured -> heading, featured -> favorites, favorites -> heading, heading -> popular | 23 / 28 / 32 / 16dp | `xl` / `xxl` / `xxl` / `md` |
| Header glow height | 522dp | `520.dp` |
| Row spinner | 24dp | `24.dp` |
| Popular heading | 18 Bold | `titleLarge` `onSurface` |
| Home title | 26 Bold | `headlineMedium` `onSurface` |
| Avatar / top offset / person icon / title gap | 40 / 7 / 24 / 8dp | `40.dp` / `xs` / `TmdbSize.iconMedium` / `xs` |
| Search field height / radius / start / end | 46 / 32 / 21 / 16dp | `48.dp` / `CircleShape` / `lg` / `md` |
| Search hint | 15 Medium, white 50% | `bodyLarge` `onSurfaceVariant` |
| Tabs min height / start / spacing | 48 / 33 / 29dp | `TmdbSize.touchTarget` / `xxl` / `xxl` |
| Tab pill radius / padding | 70 / 14×6dp | `CircleShape` / `md`×`xs` |
| Tab labels | 16 SemiBold white / 16 Regular white 40% | `titleMedium` `onSecondary` / `bodyLarge` `onSurfaceVariant` |
| Favorites card shape / colors / border | 16dp / `#211A30` / `#312A3F` 1dp | `shapes.large` / `surfaceContainer` / `surfaceContainerHigh`, `TmdbSize.borderWidth` |
| Favorites card padding top / bottom / sides / header end | 18 / 14 / 12 / 4dp | `lg` / `md` / `sm` / `xxs` |
| Title -> badge, badge min size, badge padding | 8 / 22 / 6dp | `xs` / `24.dp` / `xs` |
| View all padding, gap, chevron | 8 / 6 / 12dp | `xs` / `xs` / `12.dp` |
| Title -> subtitle, subtitle -> row, item spacing | 12 / 16 / 12dp | `sm` / `md` / `sm` |
| Item width / poster height / poster radius | 97 / 147 / 8dp | `96.dp` / `148.dp` / `shapes.small` |
| Poster -> name, name -> meta | 12 / 8dp | `sm` / `xs` |
| Heart / touch offset | 28dp outline heart | `FavoriteHeart(isFavorite = true)` / `xxs` |
| Section title / badge / view all / subtitle / name / meta | 20 Bold / 12 Bold / 14 SemiBold / 13 / 14 SemiBold / 12 | `titleLarge` / `labelMedium` / `labelLarge` / `bodyMedium` / `titleSmall` / `bodySmall` |
| Details screen padding | 30dp | `xxl` |
| Details content top | 376dp | `376.dp` |
| Header -> divider -> synopsis -> trailer | 21 / 20 / 30dp | `lg` / `lg` / `xxl` |
| To cast heading, cast heading -> row | 46 / 16dp | `xxxl` / `md` |
| To section heading, heading -> content, bottom | 41 / 20 / 40dp | `xxxl` / `lg` / `xxxl` |
| Synopsis | 14 lh 17.5 `#CCCCCC` | `bodyMedium` `onSurface` |
| Ring -> name, name -> date, date -> dot, dot -> clock, clock -> runtime | 16 / 5 / 3 / 6 / 6dp | `md` / `xxs` / `xxs` / `xs` / `xs` |
| Dot / clock | 4 / 19dp | `4.dp` / `TmdbSize.iconSmall` |
| Name / date line | 22 Bold / 16 `#BBBBBB` | `titleLarge` `onSurface` / `bodyLarge` `onSurfaceVariant` |
| Ring size / stroke / value | 57 / 6 / 15dp fixed | `56.dp` / `6.dp` / `titleMedium`, capped scale |
| Cast item width / spacing / photo / border / photo -> name / placeholder | 75 / 4 / 60 / 4 / 3 / 30dp | `76.dp` / `xxs` / `60.dp` / `private val PhotoBorder = 4.dp` / `xxs` / `32.dp` |
| Cast name | 12 Bold | `labelMedium` `onSurface` |
| Top bar height / back start / pill height / pill padding | 48 / 8 / 32 / 10dp | `TmdbSize.touchTarget` / `xs` / `32.dp` / `sm` |
| Arrow / arrow -> label / favorite end / favorite | 24 / 6 / 22dp / 32dp icon | `TmdbSize.iconMedium` / `xs` / `xl` / `FavoriteHeart` |
| Top bar label | 16 Bold | `titleMedium` `onSurface` |
| Trailer height / radius / icon / icon -> label / label | 44 / 60 / 20 / 10dp / 16 SemiBold | `44.dp` / `CircleShape` / `TmdbSize.iconSmall` / `sm` / `titleMedium` `onPrimary` |
| Chip height / radius / padding / spacing / line spacing | 31 / 34 / 17.5 / 14 / 10dp | `32.dp` / `CircleShape` / `md` / `md` / `sm` |
| Chip label | 15 Medium | `titleMedium` `onSurface` |
| Artwork height / fade | 493dp / 260 and 412 of 493 | `492.dp` / `260f / 492f`, `412f / 492f` |

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Spacing scale | `TmdbSpacing` | `cmp/designsystem/.../designsystem/TmdbSpacing.kt` |
| Shared sizes | `TmdbSize.touchTarget`, `iconSmall`, `iconMedium`, `borderWidth` | `cmp/designsystem/.../designsystem/TmdbSize.kt` |
| Poster row height | `PosterRowDefaults.PosterHeight` | `cmp/designsystem/.../component/PosterRow.kt` |
| Colors, type, shapes | `MaterialTheme.colorScheme`, `typography`, `shapes` (set by `TmdbTheme`) | `cmp/designsystem/.../designsystem/Theme.kt` |
| Poster tile, poster row, retry, dialog | `PosterTile`, `PosterRow`, `RetryMessage`, `RemoveFavoriteDialog` (already on tokens) | `cmp/designsystem/.../component/` |
| Font scale cap | `LocalDensity`, `Density(density, fontScale)` | `androidx.compose.ui` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `cmp:feature:home:ui` | `HomeScreen.kt`, `component/HomeHeader.kt`, `SearchField.kt`, `CategoryTabs.kt`, `FavoritesSection.kt`, `RowMessage.kt` (modify) | Home on tokens |
| `cmp:feature:title-details:ui` | `TitleDetailsScreen.kt`, `component/DetailsHeader.kt`, `RatingRing.kt`, `CastRow.kt`, `DetailsTopBar.kt`, `TrailerButton.kt`, `GenreChips.kt`, `DetailsArtwork.kt`, `SectionHeading.kt` (modify); `DetailsColors.kt` (delete) | Title details on tokens |
| `cmp:feature:title-details:ui` | `androidHostTest/.../component/RatingRingFontScaleTest.kt` (new) | Font scale cap |

## Steps
### Step 1: Home on tokens  (AC-3, AC-4, AC-6, EC-1, EC-5)
- Files: `cmp/feature/home/ui/src/commonMain/kotlin/com/banhmilabs/tmdb/cmp/feature/home/ui/HomeScreen.kt`,
  `component/HomeHeader.kt`, `SearchField.kt`, `CategoryTabs.kt`, `FavoritesSection.kt`, `RowMessage.kt` (modify).
- What (values from the mapping table; every `TextStyle`, `Color`, `RoundedCornerShape` and spacing literal goes):
  - `HomeScreen`: delete `internal val ScreenPadding` and every constant; header glow
    `height(520.dp)` and row spinner `size(24.dp)` inline; the loading box uses
    `heightIn(min = PosterRowDefaults.PosterHeight)`; the row loading box uses
    `height(PosterRowDefaults.PosterHeight)`; the popular heading is `Text(color = onSurface, style =
    typography.titleLarge)`; the favorites inset is `TmdbSpacing.xxl - TmdbSpacing.sm`.
  - `HomeHeader`: `headlineMedium` title in `onSurface`; avatar `size(40.dp)`; person icon
    `TmdbSize.iconMedium`; avatar top offset and gap `xs`.
  - `SearchField`: `heightIn(min = 48.dp)`, `CircleShape`, background `surfaceVariant` (unchanged),
    padding `lg`/`md`, hint `bodyLarge` in `onSurfaceVariant`, `maxLines = 1` with `TextOverflow.Ellipsis` so a long
    hint at 200% does not push the icon out (EC-1).
  - `CategoryTabs`: `heightIn(min = TmdbSize.touchTarget)`, content padding start `xxl`, spacing `xxl`. Selected tab:
    `CircleShape` + `secondary` background + `Modifier.padding(horizontal = md)`, label `titleMedium` in
    `onSecondary`. Unselected: no horizontal padding, label `bodyLarge` in `onSurfaceVariant`. Vertical padding `xs`
    on both; `UNSELECTED_ALPHA` goes. `minimumInteractiveComponentSize()` stays (EC-5).
  - `FavoritesSection`: card `shapes.large`, `surfaceContainer`,
    `border(TmdbSize.borderWidth, surfaceContainerHigh, shapes.large)`; inline sizes: badge
    `defaultMinSize(24.dp, 24.dp)`, chevron `size(12.dp)`, item `width(96.dp)`, item poster `height(148.dp)`; the heart is the shared
    `FavoriteHeart` (already in the design system); spacing tokens as mapped; badge `secondaryContainer` + `labelMedium` in `onSecondaryContainer`; view all `labelLarge` and chevron
    tint in `secondary`; subtitle `bodyMedium` and meta `bodySmall` in `onSurfaceVariant`; name `titleSmall` in
    `onSurface` (`maxLines = 2`, ellipsis stays, EC-1); heart offset `xxs`; `minimumInteractiveComponentSize()` on
    view all and the heart stays (EC-5).
  - `RowMessage`: delete `RowMinHeight`; `heightIn(min = PosterRowDefaults.PosterHeight)`, padding `xxl`.
- Unit tests: the existing `HomeViewModel*Test` classes are unchanged (no behavior change).
- Compile: `./gradlew :cmp:feature:home:ui:compileAndroidMain`

### Step 2: Title details on tokens  (AC-3, AC-4, AC-5, AC-6, EC-1, EC-4, EC-5)
- Files: `cmp/feature/title-details/ui/src/commonMain/kotlin/com/banhmilabs/tmdb/cmp/feature/titledetails/ui/`
  `TitleDetailsScreen.kt`, `component/SectionHeading.kt`, `DetailsHeader.kt`, `RatingRing.kt`, `CastRow.kt`,
  `DetailsTopBar.kt`, `TrailerButton.kt`, `GenreChips.kt`, `DetailsArtwork.kt` (modify); `DetailsColors.kt`
  (delete).
- What:
  - `TitleDetailsScreen`: background `colorScheme.surface`; content top `padding(top = 376.dp)` inline; spacing tokens as
    mapped (screen padding `xxl`); divider `color = colorScheme.outlineVariant` (`DIVIDER_ALPHA` goes); synopsis
    `Text(color = onSurface, style = typography.bodyMedium)`.
  - `SectionHeading`: `typography.titleLarge` in `onSurface`, `maxLines = 1` with `TextOverflow.Ellipsis` (EC-1).
  - `DetailsHeader`: name `titleLarge` in `onSurface` (wraps as today); date and runtime `bodyLarge` in
    `onSurfaceVariant`; dot `size(4.dp)` in `onSurfaceVariant`; clock `TmdbSize.iconSmall`; gaps as
    mapped.
  - `RatingRing`: ring `size(56.dp)` and stroke `6.dp.toPx()` inline; track `surfaceContainerHigh`,
    center `surface`, arc `secondary`; value text `typography.titleMedium` in `onSurface`, `maxLines = 1`, inside
    `CompositionLocalProvider(LocalDensity provides cappedDensity)`; the bold value and regular `%` spans stay.
  - Pseudo-code:
    ```
    internal const val MAX_RING_FONT_SCALE = 1.15f
    internal fun ringFontScale(systemFontScale: Float): Float = minOf(systemFontScale, MAX_RING_FONT_SCALE)

    val density = LocalDensity.current
    val cappedDensity = Density(density.density, ringFontScale(density.fontScale))
    ```
  - `CastRow`: item `width(76.dp)`, photo `60.dp`, placeholder icon `size(32.dp)` inline;
    `private val PhotoBorder = 4.dp` (used three times); spacing `xxs`; photo border and fill `surfaceContainerHigh`; name `labelMedium`
    in `onSurface`, centered, `maxLines = 2` and ellipsis (EC-1).
  - `DetailsTopBar`: `heightIn(min = TmdbSize.touchTarget)`; pill `heightIn(min = 32.dp)` inline; the favorite button is the shared `FavoriteHeart`; pill background `surfaceContainerHigh.copy(alpha = 0.65f)`; arrow
    `TmdbSize.iconMedium`; label `titleMedium` in `onSurface`; spacing as mapped; `minimumInteractiveComponentSize()`
    stays (EC-5).
  - `TrailerButton`: `heightIn(min = 44.dp)`; `CircleShape`; icon `TmdbSize.iconSmall`; gap `sm`; label
    `titleMedium` in `onPrimary`.
  - `GenreChips`: `heightIn(min = 32.dp)`; `CircleShape` with `surfaceContainerHigh`; padding `md`; spacing
    `md` and `sm`; label `titleMedium` in `onSurface`.
  - `DetailsArtwork`: `height(492.dp)` inline; fade stops `260f / 492f` and `412f / 492f`; the fade
    `Brush` is built inside the composable with `remember(surface)` from `colorScheme.surface`; placeholder background
    `surfaceContainerHigh`.
- Unit tests (`cmp/feature/title-details/ui/src/androidHostTest/kotlin/com/banhmilabs/tmdb/cmp/feature/titledetails/ui/component/RatingRingFontScaleTest.kt`):
  `smallScalesPassThrough` (0.85 -> 0.85, 1.0 -> 1.0), `largeScalesAreCappedAtTheMaximum` (2.0 -> 1.15),
  `theMaximumItselfPassesThrough`. The existing `TitleDetailsViewModel*Test`, `TitleDetailsFormatTest` and
  `TitleDetailsRouteTest` are unchanged.
- Compile: `./gradlew :cmp:feature:title-details:ui:compileAndroidMain :cmp:feature:title-details:ui:compileAndroidHostTest`

### Step 3: Run checks
- `./gradlew :cmp:feature:home:ui:testAndroidHostTest :cmp:feature:home:ui:detekt
  :cmp:feature:title-details:ui:testAndroidHostTest :cmp:feature:title-details:ui:detekt` and
  `./gradlew :architecture-test:test`.
- Raw-value check (AC-3): over `cmp/feature/home/ui/src/commonMain` and `cmp/feature/title-details/ui/src/commonMain`,
  `grep -nE 'Color\(0x|Color\.(White|Black)|RoundedCornerShape\(|\.sp\b'` finds nothing, and every `n.dp` left is
  is a size from the mapping table (no spacing value: every gap and padding uses `TmdbSpacing`).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-3 (completes `cmp`) | 1, 2 | Step 3 raw-value grep |
| AC-4 (completes `cmp`) | 1, 2 | Manual: Home and Details match the current app within a few points (subtask How to test 1–2) |
| AC-5 (Details and Home favorites colors) | 1, 2 | `TmdbColorSchemeTest` (01) covers the roles; manual check of the screens |
| AC-6 (completes `cmp`) | 1, 2 | Manual: system font 200% (How to test 3); `RatingRingFontScaleTest` |
| EC-1 | 1, 2 | Manual: a long title name at 200% on Home and Details (How to test 3) |
| EC-4 | 2 | `RatingRingFontScaleTest`; manual: poster and ring keep their size at 200% |
| EC-5 | 1, 2 | Manual: tabs, view all, heart, back, favorite, trailer still tappable at 200% |

## Out of scope
- `native-android` and `native-ios` -> 03–06.
- Design docs and the project convention -> 07.
- The Home background images (`home_background`, `home_header_glow`) stay as drawables; they are assets, not color
  literals.

## Risks
- "100%" at `titleMedium` × 1.15 is about 40dp wide, close to the ring's 44dp inner width. Mitigation: `maxLines = 1`
  keeps it on one line; check a 100% (or 99%) rating at 200% font scale on the device, and lower
  `MAX_RING_FONT_SCALE` to 1.1 if it touches the stroke.
- Section headings get larger (18 -> 22): a long localized heading (pt-BR "Recomendações") may hit the end of the
  line. Mitigation: `maxLines = 1` with ellipsis on `SectionHeading`; check pt-BR on a narrow screen.
- Home bottom padding drops from 79dp to 48dp. Mitigation: nothing overlaps the end of the scroll today; check on a
  device with gesture navigation that the last row is still clear of the system bar (the safe-drawing insets stay).
- Darker Details page (`#21222E` -> `#130E1F`) with the purple surface family. Mitigation: agreed by the user; the
  artwork fade uses the same color, so there is no visible seam.
