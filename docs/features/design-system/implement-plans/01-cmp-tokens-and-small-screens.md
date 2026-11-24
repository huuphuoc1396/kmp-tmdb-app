# Plan: cmp tokens and smaller screens

Requirement: [01 - cmp: tokens and smaller screens](../subtasks/01-cmp-tokens-and-small-screens.md)
Status: Done

## Summary
`cmp/designsystem` gets the reference token set: a full Material Design 3 dark `ColorScheme`, a `Typography` that
keeps the Material Design 3 sizes and line heights with brand weights, the Material Design 3 `Shapes`, and two plain
objects, `TmdbSpacing` (4dp grid, t-shirt names) and `TmdbSize` (named component sizes). Colors, type and shapes are
read through `MaterialTheme`, the standard Material 3 way. Spacing and sizes never vary at runtime, so they are
plain objects with no `CompositionLocal`. The shared components, Onboarding and My favorites then replace every
private dp, sp, radius and color with these tokens, and fixed heights that hold text become minimum heights so
text can grow with the system font size. `native-android` (03) and `native-ios` (05) copy these names and values.

## Decisions
- Spacing names use t-shirt sizes — user choice over value names (`space16`) and semantic names. The scale is
  `xxs` 4, `xs` 8, `sm` 12, `md` 16, `lg` 20, `xl` 24, `xxl` 32, `xxxl` 48 (all on the 4dp grid, AC-2). There is no
  2dp step: no current spacing needs a hairline gap.
- Rounding rule (EC-2, EC-3): an old value takes the nearest step of its scale. On a tie it takes the larger step.
  Two old values can land on the same token. This applies to spacing, type sizes and corner radii. Component sizes
  keep their value when it is already a multiple of 4.
- Surfaces use the Favorites purple family — user choice over keeping Night `#15151D`. `surface`/`background`
  `#130E1F`, `surfaceContainer` `#211A30`, `surfaceContainerHigh` `#312A3F`, `onSurfaceVariant` `#A6A1B5`,
  `outline` `#655F74`. Onboarding, Home and Title details pick up a slightly purple background.
- Type: Material Design 3 default sizes, line heights and tracking (`Typography()`), with brand weights set once in
  the theme — user choice over plain Material Design 3 weights. Display, headline and `titleLarge` are Bold;
  `titleMedium`, `titleSmall` and every label role are SemiBold; body roles stay Regular. The only per-use weight
  change is the Onboarding headline (`headlineLarge.copy(fontWeight = FontWeight.Light)`), which the design
  requires (single Light weight). `FontWeight` is not a raw value under AC-3.
- Shapes: Material Design 3 defaults (`extraSmall` 4, `small` 8, `medium` 12, `large` 16, `extraLarge` 28), plus
  `CircleShape` for full/pill shapes. The dialog uses `extraLarge` (28) as Material Design 3 recommends — user
  choice. This is the one exception to the rounding rule, which would give `large` (16).
- `secondary` stays the brand pink `#FF1F8A` with white `onSecondary` labels — user choice (2026-11-24) over a
  darker pink or dark labels. White on `#FF1F8A` is about 3.6:1: it meets WCAG AA for large or bold text (3:1), not
  the 4.5:1 for normal text. This is an accepted exception to the task.md contrast constraint, limited to the
  `onSecondary`/`secondary` pair. Pink text on dark surfaces (`#FF1F8A` on `#211A30`, about 4.7:1) passes.
- The favorites count badge keeps its look: pink `secondary` text on `surfaceContainer`.
- The dialog background stays `surfaceContainer` (`#211A30`, same color as today) instead of Material Design 3's
  `surfaceContainerHigh`, so the dialog keeps its current look.
- The onboarding content max width becomes `TmdbSize.contentMaxWidth` = 320dp (was 315dp), so it sits on the grid.
- The heart icon becomes a 20dp square (`TmdbSize.iconSmall`). The 20×18 drawable keeps its aspect ratio inside it
  (`Image` uses `ContentScale.Fit` by default).
- `FavoritesColors` is deleted; its three colors are `surface`, `surfaceContainer` and `onSurfaceVariant`.
- Tokens are `internal val` in the theme file (`TmdbDarkColorScheme`, `TmdbTypography`, `TmdbShapes`), so tests in
  the same module can check them without making them public API. `TmdbSpacing` and `TmdbSize` are public objects,
  because feature modules use them.
- Design Home (maybe): used — only for `PosterRow`, `PosterTile` and `RetryMessage`, which this subtask moves to
  tokens and which appear on Home. The Home screen itself is 02.

### Value mapping (old -> token)
| Where | Old | Token |
|---|---|---|
| Onboarding content start / end / bottom | 29 / 31 / 31dp | `xxl` 32 |
| Onboarding content gap | 19dp | `lg` 20 |
| Onboarding content max width | 315dp | `TmdbSize.contentMaxWidth` 320 |
| Onboarding headline | 32sp Light, lh 37.3 | `headlineLarge` (32/40) + Light |
| Onboarding body | 15sp, lh 18.75 | `bodyLarge` (16/24) |
| Get started min height / padding | 48 / 12dp | `TmdbSize.touchTarget` / `sm` |
| Get started label | 16sp SemiBold | `titleMedium` |
| Favorites screen padding / top / bottom | 20 / 8 / 24dp | `lg` / `xs` / `xl` |
| Favorites header min height, arrow, arrow gap | 48 / 24 / 8dp | `touchTarget` / `iconMedium` / `xs` |
| Badge height / padding | 28 / 12dp | `TmdbSize.badgeMinHeight` (min) / `sm` |
| Header -> title, title -> subtitle | 16 / 8dp | `md` / `xs` |
| Subtitle -> list (26dp minus card spacing) | 14dp | `sm` (total 24 = `xl`) |
| Card spacing, empty top | 12 / 48dp | `sm` / `xxxl` |
| Back label / title / subtitle / empty / badge | 16 Bold / 26 Bold / 13 / 14 / 12 Bold sp | `titleMedium` / `headlineMedium` (28) / `bodyMedium` / `bodyMedium` / `labelMedium` |
| Favorite card height / radius / padding | 120 / 12 / 12dp | `TmdbSize.favoriteCardMinHeight` (min) / `medium` / `sm` |
| Card poster, radius, poster -> text | 64×96 / 6 / 14dp | `posterSmallWidth`×`posterSmallHeight` / `small` (8) / `md` (16) |
| Name -> meta, heart, heart end padding | 8 / 20×18 / 10dp | `xs` / `iconSmall` / `sm` (12) |
| Card name / meta | 16 SemiBold / 13sp | `titleMedium` / `bodyMedium` |
| Dialog margin / radius / padding / bottom | 20 / 20 / 24 / 20dp | `lg` / `extraLarge` (28) / `xl` / `lg` |
| Dialog title -> message, message -> buttons, button gap | 20 / 24 / 12dp | `lg` / `xl` / `sm` |
| Dialog button height / radius / border | 48 / 10 / 1dp | `touchTarget` (min) / `medium` (12) / `TmdbSize.borderWidth` |
| Dialog title / message / button | 22 Bold / 14 lh20 / 16 SemiBold | `titleLarge` / `bodyMedium` / `titleMedium` |
| Dialog colors | `#211A30` / `#B0AABF` / `#655F74` / white | `surfaceContainer` / `onSurfaceVariant` / `outline` / `onSurface`, `onSecondary` |
| Poster tile padding / name | 8dp / 14 SemiBold | `xs` / `titleSmall` |
| Poster row size / radius / spacing | 124×188 / 8 / 12dp | `posterWidth`×`posterHeight` / `small` / `sm` |
| Retry gap / shape / padding | 12 / 70 / 14×6dp | `sm` / `CircleShape` / `md`×`xs` |
| Retry message / label | 15sp white 50% / 16 SemiBold | `bodyLarge` `onSurfaceVariant` / `titleMedium` `onSecondary` |

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Theme entry point | `TmdbTheme` | `cmp/designsystem/.../designsystem/Theme.kt` |
| Material 3 theme, color scheme, type, shapes | `MaterialTheme`, `darkColorScheme`, `Typography`, `Shapes` | `compose-material3` 1.9.0 (catalog) |
| 48dp touch area | `Modifier.minimumInteractiveComponentSize()` | `compose-material3` |
| Poster placeholder, poster row, retry, dialog | `PosterTile`, `PosterRow`, `RetryMessage`, `RemoveFavoriteDialog` | `cmp/designsystem/.../component/` |
| Contrast check in tests | `Color.luminance()` | `androidx.compose.ui.graphics` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `cmp:designsystem` | `Theme.kt` (modify), `Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`, `Size.kt` (new) | Token set |
| `cmp:designsystem` | `component/RemoveFavoriteDialog.kt`, `PosterTile.kt`, `PosterRow.kt`, `RetryMessage.kt` (modify) | Shared components on tokens |
| `cmp:designsystem` | `androidHostTest/.../TmdbSpacingTest.kt`, `TmdbColorSchemeTest.kt`, `TmdbTypographyTest.kt` (new) | Token checks |
| `cmp:feature:onboarding:ui` | `OnboardingScreen.kt`, `component/GetStartedButton.kt` (modify) | Onboarding on tokens |
| `cmp:feature:favorites:ui` | `FavoritesScreen.kt`, `component/FavoriteCard.kt` (modify), `FavoritesColors.kt` (delete) | My favorites on tokens |
| affected, not edited | `cmp:feature:home:ui`, `cmp:feature:title-details:ui`, `cmp:composeApp` | They read `secondary`, `surfaceVariant` and `background`, which change value; their code moves in 02 |

## Steps
### Step 1: Token set in the design system  (AC-1 partial, AC-2, AC-5 partial, EC-2, EC-3) ✅
- Files: `cmp/designsystem/src/commonMain/kotlin/com/banhmilabs/tmdb/cmp/designsystem/Color.kt`, `Type.kt`,
  `Shape.kt`, `Spacing.kt`, `Size.kt` (new); `Theme.kt` (modify).
- What:
  - `Color.kt`: `internal val TmdbDarkColorScheme = darkColorScheme(...)` with every role set:
    | Role | Value | Role | Value |
    |---|---|---|---|
    | primary | `#8000FF` | onPrimary | `#FFFFFF` |
    | primaryContainer | `#540BA1` | onPrimaryContainer | `#FFFFFF` |
    | inversePrimary | `#6A00D6` | surfaceTint | `#8000FF` |
    | secondary | `#FF1F8A` | onSecondary | `#FFFFFF` |
    | secondaryContainer | `#381A39` | onSecondaryContainer | `#FFB1C8` |
    | tertiary | `#C9A7FF` | onTertiary | `#2A0059` |
    | tertiaryContainer | `#36076B` | onTertiaryContainer | `#EBDDFF` |
    | error | `#FFB4AB` | onError | `#690005` |
    | errorContainer | `#93000A` | onErrorContainer | `#FFDAD6` |
    | background | `#130E1F` | onBackground | `#FFFFFF` |
    | surface | `#130E1F` | onSurface | `#FFFFFF` |
    | surfaceVariant | `#36076B` | onSurfaceVariant | `#A6A1B5` |
    | surfaceDim | `#130E1F` | surfaceBright | `#3A3347` |
    | surfaceContainerLowest | `#0E0A17` | surfaceContainerLow | `#1A1428` |
    | surfaceContainer | `#211A30` | surfaceContainerHigh | `#312A3F` |
    | surfaceContainerHighest | `#3C3549` | outline | `#655F74` |
    | outlineVariant | `#3C3549` | scrim | `#000000` |
    | inverseSurface | `#E8E0F0` | inverseOnSurface | `#211A30` |
    `surfaceVariant` keeps the poster placeholder purple (`#36076B`) that `PosterTile` and Home use today.
  - `Type.kt`: `internal val TmdbTypography` built from `Typography()` with `copy` of the weights only:
    display*/headline*/`titleLarge` Bold, `titleMedium`/`titleSmall`/label* SemiBold, body* unchanged.
  - `Shape.kt`: `internal val TmdbShapes = Shapes()` (Material Design 3 defaults, stated explicitly).
  - `Spacing.kt`: `object TmdbSpacing { val xxs = 4.dp; xs = 8.dp; sm = 12.dp; md = 16.dp; lg = 20.dp; xl = 24.dp;
    xxl = 32.dp; xxxl = 48.dp }`.
  - `Size.kt`: `object TmdbSize { touchTarget = 48.dp; iconSmall = 20.dp; iconMedium = 24.dp; borderWidth = 1.dp;
    badgeMinHeight = 28.dp; posterWidth = 124.dp; posterHeight = 188.dp; posterSmallWidth = 64.dp;
    posterSmallHeight = 96.dp; favoriteCardMinHeight = 120.dp; contentMaxWidth = 320.dp }`.
  - `Theme.kt`: drop the private colors; `MaterialTheme(colorScheme = TmdbDarkColorScheme, typography =
    TmdbTypography, shapes = TmdbShapes, content = content)`.
- Unit tests (`cmp/designsystem/src/androidHostTest/kotlin/com/banhmilabs/tmdb/cmp/designsystem/`):
  - `TmdbSpacingTest`: `every step is a multiple of four`, `steps grow from xxs to xxxl`.
  - `TmdbColorSchemeTest`: `primary is the brand purple`, `secondary is the brand pink`,
    `surfaces use the favorites purple family`, `labels on the brand pink reach 3 to 1`,
    `secondary text on surface containers reaches 4.5 to 1`, `text roles reach 4.5 to 1 on their backgrounds` (pairs:
    onPrimary/primary, onSurface/surface, onSurface/surfaceContainer,
    onSurfaceVariant/surface, onSurfaceVariant/surfaceContainer, onSecondaryContainer/secondaryContainer,
    onPrimaryContainer/primaryContainer, onSurface/surfaceVariant; ratio from a private `contrast(a, b)` using
    `luminance()`).
  - `TmdbTypographyTest`: `sizes and line heights match material defaults`,
    `headline and title large are bold`, `title medium, title small and labels are semibold`,
    `body roles keep the regular weight`.
- Compile: `./gradlew :cmp:designsystem:compileAndroidMain`

### Step 2: Shared components on tokens  (AC-3 partial, AC-4 partial, AC-6 partial, EC-5 partial) ✅
- Files: `cmp/designsystem/.../component/RemoveFavoriteDialog.kt`, `PosterTile.kt`, `PosterRow.kt`,
  `RetryMessage.kt` (modify).
- What: remove every private dp, sp, radius, `TextStyle` and `Color`; use the mapping table above:
  - `RemoveFavoriteDialog`: `MaterialTheme.shapes.extraLarge`, background `colorScheme.surfaceContainer`;
    `typography.titleLarge` / `bodyMedium` (color `onSurfaceVariant`) / `titleMedium`; cancel border
    `BorderStroke(TmdbSize.borderWidth, colorScheme.outline)`; confirm background `colorScheme.secondary` with
    `onSecondary` label, cancel label `onSurface`; button `heightIn(min = TmdbSize.touchTarget)`, so a label at a
    large font size grows the button instead of clipping; `shapes.medium`. The `verticalScroll` stays.
  - `PosterTile`: padding `TmdbSpacing.xs`, `typography.titleSmall` with `color = colorScheme.onSurface` and
    `textAlign = TextAlign.Center`; background stays `surfaceVariant`.
  - `PosterRow`: `TmdbSize.posterWidth`/`posterHeight`, `shapes.small`, `TmdbSpacing.sm`. The row height stays
    fixed to the poster height (EC-4: posters keep their size).
  - `RetryMessage`: gap `TmdbSpacing.sm`; message `typography.bodyLarge` in `onSurfaceVariant` (replaces white at
    50%, which is also why `MESSAGE_ALPHA` goes); label `typography.titleMedium` in `onSecondary` on `secondary`,
    `CircleShape`, padding `md` × `xs`. `minimumInteractiveComponentSize()` stays (EC-5).
- Unit tests: none new (no logic); covered by the manual checks.
- Compile: `./gradlew :cmp:designsystem:compileAndroidMain :cmp:feature:home:ui:compileAndroidMain
  :cmp:feature:title-details:ui:compileAndroidMain :cmp:feature:favorites:ui:compileAndroidMain`

### Step 3: Onboarding on tokens  (AC-3 partial, AC-4 partial, AC-6 partial, EC-5 partial) ✅
- Files: `cmp/feature/onboarding/ui/.../OnboardingScreen.kt`, `component/GetStartedButton.kt` (modify).
- What:
  - `OnboardingScreen`: padding start/end/bottom `TmdbSpacing.xxl`, gap `TmdbSpacing.lg`, max width
    `TmdbSize.contentMaxWidth`; headline `typography.headlineLarge.copy(fontWeight = FontWeight.Light)` and body
    `typography.bodyLarge`, both with `color = colorScheme.onSurface` (the artwork scrim stays). The
    `verticalScroll` and `heightIn(min = maxHeight)` stay, so large text scrolls instead of being cut (AC-6).
  - `GetStartedButton`: `heightIn(min = TmdbSize.touchTarget)`, padding `TmdbSpacing.sm`, label
    `typography.titleMedium` in `colorScheme.onPrimary`, centered.
- Unit tests: `OnboardingViewModelTest` unchanged (behavior unchanged).
- Compile: `./gradlew :cmp:feature:onboarding:ui:compileAndroidMain`

### Step 4: My favorites on tokens  (AC-3 partial, AC-4 partial, AC-5 partial, AC-6 partial, EC-5 partial) ✅
- Files: `cmp/feature/favorites/ui/.../FavoritesScreen.kt`, `component/FavoriteCard.kt` (modify);
  `FavoritesColors.kt` (delete).
- What:
  - `FavoritesScreen`: background `colorScheme.surface`; content padding `lg` / `xs` / `xl`, item spacing `sm`;
    header `heightIn(min = TmdbSize.touchTarget)`, arrow `TmdbSize.iconMedium`, gap `xs`, back label
    `typography.titleMedium` in `onSurface`; badge `heightIn(min = TmdbSize.badgeMinHeight)`, `CircleShape`,
    background `surfaceContainer`, padding `sm`, text `typography.labelMedium` in `secondary`;
    title `typography.headlineMedium` in `onSurface`; subtitle `typography.bodyMedium` in `onSurfaceVariant`;
    spacers `md`, `xs`, `sm`; empty text `typography.bodyMedium` in `onSurfaceVariant`, centered, top padding
    `xxxl`.
  - `FavoriteCard`: outer `Row` uses `heightIn(min = TmdbSize.favoriteCardMinHeight)` plus
    `height(IntrinsicSize.Min)`, so the inner clickable `Row` can still `fillMaxHeight()` and the card grows
    with large text (AC-6, EC-1: the name keeps `maxLines = 2` and ellipsis); `shapes.medium`,
    `surfaceContainer`, padding `sm`; poster `TmdbSize.posterSmallWidth` × `posterSmallHeight`, `shapes.small`;
    gaps `md`, `xs`; name `typography.titleMedium` in `onSurface`; meta `typography.bodyMedium` in
    `onSurfaceVariant`; heart `Modifier.size(TmdbSize.iconSmall)`, end padding `sm`;
    `minimumInteractiveComponentSize()` stays (EC-5).
- Unit tests: `FavoritesViewModelTest`, `FavoritesViewModelRemoveTest` unchanged.
- Compile: `./gradlew :cmp:feature:favorites:ui:compileAndroidMain`

### Step 5: Run checks ✅
- Commands: `./gradlew :cmp:designsystem:testAndroidHostTest :cmp:designsystem:detekt
  :cmp:feature:onboarding:ui:testAndroidHostTest :cmp:feature:onboarding:ui:detekt
  :cmp:feature:favorites:ui:testAndroidHostTest :cmp:feature:favorites:ui:detekt`, then
  `./gradlew :architecture-test:test`. `./gradlew assemble` is not needed (no `expect`/`actual` or
  platform source set changes), but the iOS build is part of the manual check.
- Raw-value check for the touched files (AC-3): `grep -nE '[0-9]+(\.[0-9]+)?\.(dp|sp)|Color\(0x|Color\.White|
  RoundedCornerShape\(' ` over `cmp/designsystem/src/commonMain/**/component`,
  `cmp/feature/onboarding/ui/src/commonMain`, `cmp/feature/favorites/ui/src/commonMain` finds nothing. Token
  values live only in `Color.kt`, `Spacing.kt` and `Size.kt`.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 (partial: reference set in `cmp`) | 1 | `TmdbSpacingTest`, `TmdbColorSchemeTest`, `TmdbTypographyTest`; values compared in 03/05 |
| AC-2 | 1 | `TmdbSpacingTest.every step is a multiple of four` |
| AC-3 (partial: shared components, Onboarding, My favorites) | 2, 3, 4 | Step 5 raw-value grep |
| AC-4 (partial: same screens) | 2, 3, 4 | Manual: screens match the current app within a few points (subtask How to test 1–2) |
| AC-5 (partial: `cmp`) | 1, 4 | `TmdbColorSchemeTest` (brand colors, surfaces, contrast pairs) |
| AC-6 (partial: Onboarding, My favorites) | 2, 3, 4 | Manual: system font 200% (How to test 3) |
| EC-2 | Decisions (rounding rule) | Mapping table; manual |
| EC-3 | Decisions (ties take the larger step) | Mapping table (15sp -> 16, 13sp -> 14, 14dp -> 16, 10dp -> 12, 26sp -> 28) |
| EC-5 (partial) | 2, 3, 4 | Manual: every button is still tappable at 200% (How to test 3) |

## Out of scope
- Home and Title details code, including their own color objects (`DetailsColors`, Home favorites colors) -> 02.
- `native-android` and `native-ios` tokens -> 03 and 05.
- Updating `docs/features/*/design/*.md` and the project convention -> 07.
- A light color scheme, an automated raw-value lint.

## Risks
- White labels on the brand pink stay at about 3.6:1, below AA for normal text — accepted by the user. The test
  pins it at 3:1 or more, so a later palette change cannot make it worse. The task.md constraint should name this
  exception (a spark update, not part of this plan).
- Home and Title details change color before 02: the app background becomes `#130E1F` (was `#15151D`).
  Mitigation: a small shift; 02 maps their own colors to roles. The change shows in the How to test run.
- Detekt `MagicNumber` could flag `dp` literals inside `object` property declarations. Mitigation: if it does, use
  top-level `private val` constants inside `Spacing.kt`/`Size.kt` exposed through the object, the same form
  existing files already pass with.
- `height(IntrinsicSize.Min)` with a `LazyColumn` item and `AsyncImage`: if intrinsic measurement fails for the
  poster, the clickable area may not fill the card. Mitigation: the poster has a fixed size, so its intrinsic
  height is known; check it on the device at 100% and 200% font scale.
- Some hex values for roles nobody uses yet (tertiary, error, inverse) are chosen here, not taken from Figma.
  Mitigation: they follow the Material Design 3 baseline dark tones, and 07 compares them.

## Deviations
- `Spacing.kt` / `Size.kt` are named `TmdbSpacing.kt` / `TmdbSize.kt`: Detekt `MatchingDeclarationName` requires the
  file to carry the single declaration's name.
- Test function names are lowerCamelCase (`everyStepIsAMultipleOfFour`), as `.claude/rules/testing.md` requires, not
  the sentence names in Step 1.
- `TmdbColorSchemeTest` adds `secondaryTextOnSurfaceContainersReachesAa` for the pink badge text; it was in the
  Decisions but not in the test list.
- `TmdbTypographyTest.bodyRolesKeepTheMaterialWeight` compares body weights with `Typography()` instead of asserting
  `Normal`, since Material's body styles leave the weight to the font default.
- The dialog's inner padding is written on one line so `RemoveFavoriteDialog` stays within Detekt's 60-line
  `LongMethod` limit.
- Detekt `MagicNumber` did not flag the `dp` literals inside the token objects, so the Risks mitigation was not needed.
- After review (task.md change of 2026-11-24), `TmdbSize` keeps only shared sizes: `touchTarget`, `iconSmall`,
  `iconMedium`, `borderWidth`. Component- and feature-specific sizes moved next to their component: poster 124×188 to
  `PosterRowDefaults` (own file `PosterRowDefaults.kt`, as Detekt `MatchingDeclarationName` requires), card min height
  and small poster to private constants in `FavoriteCard.kt`, badge min height to `FavoritesScreen.kt`, content max
  width to `OnboardingScreen.kt`. The value-mapping table keeps the old `TmdbSize.*` names for these rows.
