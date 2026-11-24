# Plan: native-android tokens and smaller screens

Requirement: [03 - native-android: tokens and smaller screens](../subtasks/03-native-android-tokens-and-small-screens.md)
Status: Done

## Summary
`native-android` gets a copy of the `cmp` design system as merged in #48. The same names and values apply:
a full Material 3 dark color scheme, a `Typography` with brand weights, Material 3 `Shapes`, `TmdbSpacing`, the
shared-only `TmdbSize`, `PosterRowDefaults` and `FavoriteHeart`. Its shared components, Onboarding and My favorites
then move to tokens exactly as `cmp` did. The `native-android` files differ from the `cmp` files they were copied
from only in resource access (`R.` in place of `Res.`), so each step mirrors the final `cmp` file. The app stays
standalone: it copies the values and does not depend on `cmp`.

## Decisions
- Every value, name and rounding comes from plan 01 and its deviations (`01-cmp-tokens-and-small-screens.md`),
  including:
  - Surfaces use the favorites purple family.
  - Brand pink `#FF1F8A` stays as `secondary`, with white labels as the accepted contrast exception.
  - The dialog uses `extraLarge` (28dp).
  - `TmdbSize` holds only `touchTarget`, `iconSmall`, `iconMedium` and `borderWidth`.
  - `PosterRowDefaults` is declared at the end of `PosterRow.kt`.
  - Values used in one place are written inline.

  No new decision on values is made here (AC-1).
- `FavoriteHeart` comes in this subtask too, because My favorites uses it. It is the shared heart the `cmp` app got
  in #48: filled pink when favorite, white outline when not, on a 32dp circle. Home and Title details switch to it
  in 04. Until then, `native-android` shows the old heart on those two screens.
- Drawables:
  - `res/drawable/ic_heart_filled.xml` keeps its name, but its fill becomes white `#FFFFFF`; `FavoriteHeart` tints it.
  - New `ic_heart_outline.xml`, copied from the `cmp` design system.
  - `ic_favorite_on`, `ic_favorite_off` and `ic_favorite_remove` stay until 04 removes their last use.
- The favorites count plural stays `%d`: Android resources format it correctly. Only Compose Multiplatform
  resources needed `%1$d`.
- Tests live in `app/src/test` (JVM unit tests run by `testDebugUnitTest`), as the existing `MetaLineTest` does.
  Test names are lowerCamelCase.
- Design Home (maybe): used — only for `PosterRow`, `PosterTile` and `RetryMessage`, which appear on Home and move to
  tokens here. The Home screen itself is 04.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Reference token values and final component code | `cmp` design system after #48 | `cmp/designsystem/src/commonMain/kotlin/com/banhmilabs/tmdb/cmp/designsystem/` |
| Reference screen code | `cmp` Onboarding and My favorites after #48 | `cmp/feature/onboarding/ui`, `cmp/feature/favorites/ui` |
| Reference token tests | `TmdbSpacingTest`, `TmdbColorSchemeTest`, `TmdbTypographyTest` | `cmp/designsystem/src/androidHostTest/...` |
| Theme entry point | `TmdbTheme` | `native-android/app/src/main/kotlin/com/banhmilabs/tmdb/nativeapp/designsystem/Theme.kt` |
| Material 3 | `MaterialTheme`, `darkColorScheme`, `Typography`, `Shapes`, `minimumInteractiveComponentSize` | `androidx.compose.material3` (BOM 2026.09.00) |

## Changes overview
All paths are under `native-android/app/src/main/` unless they say otherwise. `<pkg>` is `kotlin/com/banhmilabs/tmdb/nativeapp`.

| Area | Files (new / modified) | Why |
|---|---|---|
| Design system | `<pkg>/designsystem/Theme.kt` (modify); `Color.kt`, `Type.kt`, `Shape.kt`, `TmdbSpacing.kt`, `TmdbSize.kt` (new) | Token set |
| Design system components | `<pkg>/designsystem/component/RemoveFavoriteDialog.kt`, `PosterTile.kt`, `PosterRow.kt`, `RetryMessage.kt`, `PersonIcon.kt` (modify); `FavoriteHeart.kt` (new) | Shared components on tokens |
| Resources | `res/drawable/ic_heart_filled.xml` (modify), `res/drawable/ic_heart_outline.xml` (new) | Tintable heart |
| Onboarding | `<pkg>/onboarding/OnboardingScreen.kt`, `onboarding/component/GetStartedButton.kt` (modify) | Onboarding on tokens |
| My favorites | `<pkg>/favorites/FavoritesScreen.kt`, `favorites/component/FavoriteCard.kt` (modify); `favorites/FavoritesColors.kt` (delete) | My favorites on tokens |
| Tests | `app/src/test/<pkg>/designsystem/TmdbSpacingTest.kt`, `TmdbColorSchemeTest.kt`, `TmdbTypographyTest.kt` (new) | Token checks |
| Affected, not edited | `<pkg>/home/...`, `<pkg>/titledetails/...` | Read `secondary`, `surfaceVariant` and `background`, and call `PosterRow`; their code moves in 04 |

## Steps
### Step 1: Token set  (AC-1, AC-5) ✅
- Files: `designsystem/Color.kt`, `Type.kt`, `Shape.kt`, `TmdbSpacing.kt`, `TmdbSize.kt` (new); `Theme.kt` (modify).
- What: the same content as the `cmp` files of the same name, with the `native-android` package
  (`com.banhmilabs.tmdb.nativeapp.designsystem`):
  - `internal val TmdbDarkColorScheme` with every role.
  - `internal val TmdbTypography`.
  - `internal val TmdbShapes = Shapes()`.
  - `object TmdbSpacing` (`xxs` 4 … `xxxl` 48).
  - `object TmdbSize` (`touchTarget` 48, `iconSmall` 20, `iconMedium` 24, `borderWidth` 1).
  - `TmdbTheme` passes `colorScheme`, `typography` and `shapes`.
- Unit tests (`app/src/test/kotlin/com/banhmilabs/tmdb/nativeapp/designsystem/`): copies of the `cmp` tests,
  same cases:
  - `TmdbSpacingTest`: `everyStepIsAMultipleOfFour`, `stepsGrowFromXxsToXxxl`.
  - `TmdbColorSchemeTest`: `primaryIsTheBrandPurple`, `secondaryIsTheBrandPink`,
    `surfacesUseTheFavoritesPurpleFamily`, `labelsOnTheBrandPinkReachThreeToOne`,
    `secondaryTextOnSurfaceContainersReachesAa`, `textRolesReachAaOnTheirBackgrounds`.
  - `TmdbTypographyTest`: `sizesAndLineHeightsMatchMaterialDefaults`, `displayHeadlineAndTitleLargeAreBold`,
    `titleMediumTitleSmallAndLabelsAreSemiBold`, `bodyRolesKeepTheMaterialWeight`.
- Compile: `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` (in `native-android/`)

### Step 2: Shared components and FavoriteHeart  (AC-3, AC-4, AC-6, EC-5) ✅
- Files: `designsystem/component/RemoveFavoriteDialog.kt`, `PosterTile.kt`, `PosterRow.kt`, `RetryMessage.kt`,
  `PersonIcon.kt` (modify); `FavoriteHeart.kt` (new); `res/drawable/ic_heart_filled.xml` (modify),
  `res/drawable/ic_heart_outline.xml` (new).
- What: mirror the final `cmp` component files, with `R.string` and `R.drawable` in place of `Res`:
  - `RemoveFavoriteDialog`: `shapes.extraLarge`, `surfaceContainer`, `titleLarge`/`bodyMedium`/`titleMedium`,
    `outline` border with `TmdbSize.borderWidth`, buttons `heightIn(min = TmdbSize.touchTarget)` with `shapes.medium`.
  - `PosterTile`: `TmdbSpacing.xs`, `titleSmall` in `onSurface`, `maxLines = 4` inline.
  - `PosterRow`: `PosterRowDefaults.PosterWidth`/`PosterHeight`, `shapes.small`, `TmdbSpacing.sm`.
    `object PosterRowDefaults { val PosterWidth = 124.dp; val PosterHeight = 188.dp }` goes at the end of the file.
  - `RetryMessage`: `bodyLarge` in `onSurfaceVariant`, label `titleMedium` in `onSecondary` on `secondary`,
    `CircleShape`, padding `md` × `xs`.
  - `PersonIcon`: `alpha = 0.5f` inline.
  - `FavoriteHeart(isFavorite: Boolean, modifier: Modifier = Modifier)`: a 32dp circle in
    `surfaceContainerHigh.copy(alpha = 0.65f)` holding a 16dp heart, tinted `secondary` (filled) or `onSurface`
    (outline).
  - Drawables: `ic_heart_filled.xml` changes `fillColor` to `#FFFFFF`; `ic_heart_outline.xml` is the outline path
    from `cmp/designsystem/src/commonMain/composeResources/drawable/ic_heart_outline.xml`.
- Unit tests: none new (no logic).
- Compile: `./gradlew :app:compileDebugKotlin`

### Step 3: Onboarding on tokens  (AC-3, AC-4, AC-6, EC-5) ✅
- Files: `onboarding/OnboardingScreen.kt`, `onboarding/component/GetStartedButton.kt` (modify).
- What: mirror the `cmp` files:
  - Padding: `xxl` start/end/bottom, gap `lg`, `widthIn(max = 320.dp)` inline.
  - Text: headline `headlineLarge.copy(fontWeight = FontWeight.Light)`, body `bodyLarge`, both in `onSurface`.
  - Button: `heightIn(min = TmdbSize.touchTarget)`, padding `sm`, label `titleMedium` in `onPrimary`.
- Unit tests: `OnboardingViewModelTest` unchanged.
- Compile: `./gradlew :app:compileDebugKotlin`

### Step 4: My favorites on tokens  (AC-3, AC-4, AC-5, AC-6, EC-1, EC-5) ✅
- Files: `favorites/FavoritesScreen.kt`, `favorites/component/FavoriteCard.kt` (modify); `favorites/FavoritesColors.kt`
  (delete).
- What: mirror the `cmp` files:
  - `FavoritesScreen`:
    - Background `surface`; padding `lg`/`xs`/`xl`; spacing `sm`.
    - Header: `heightIn(min = TmdbSize.touchTarget)`, arrow `TmdbSize.iconMedium`, back label `titleMedium`.
    - Badge: `heightIn(min = 28.dp)`, `surfaceContainer`, `labelMedium` in `secondary`.
    - Title `headlineMedium`; subtitle and empty text `bodyMedium` in `onSurfaceVariant`.
  - `FavoriteCard`:
    - `heightIn(min = 120.dp)` plus `height(IntrinsicSize.Min)`, `shapes.medium`, `surfaceContainer`.
    - Thumbnail `size(width = 64.dp, height = 96.dp)` with `shapes.small`.
    - Name `titleMedium` with `maxLines = 2`; meta `bodyMedium` in `onSurfaceVariant`.
    - Remove button: `FavoriteHeart(isFavorite = true)` with `minimumInteractiveComponentSize()`,
      `clip(CircleShape)`, `clickable` and the remove label.
- Unit tests: `FavoritesViewModelTest` and `FavoritesViewModelRemoveTest` unchanged.
- Compile: `./gradlew :app:compileDebugKotlin`

### Step 5: Run checks ✅
- In `native-android/`: `./gradlew :app:detekt :app:testDebugUnitTest`. Use `./gradlew :app:detekt --auto-correct`
  first if ktlint fails. The app is outside the KMP build, so the KMP `architecture-test` and `koverVerify` do not
  apply. It also needs no `publishSharedCore`, because no shared-core module changes.
- Raw-value grep (AC-3) over `designsystem/component`, `onboarding` and `favorites` under
  `native-android/app/src/main/kotlin`: `grep -nE 'Color\(0x|Color\.(White|Black)|RoundedCornerShape\(|[0-9]\.sp\b|TextStyle\('`
  finds nothing.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 (partial: `native-android` matches `cmp`) | 1 | Token tests with the same expected values as `cmp`; diff of the token files against `cmp` |
| AC-3 (partial: shared components, Onboarding, My favorites) | 2, 3, 4 | Step 5 raw-value grep |
| AC-4 (partial: same screens) | 2, 3, 4 | Manual: the screens match `cmp` (subtask How to test 1–2) |
| AC-5 (partial: `native-android`) | 1 | `TmdbColorSchemeTest` |
| AC-6 (partial: Onboarding, My favorites) | 2, 3, 4 | Manual: system font 200% (How to test 3) |
| EC-5 (partial) | 2, 3, 4 | Manual: buttons and the heart are still tappable at 200% |

## Out of scope
- Home and Title details in `native-android`, including their switch to `FavoriteHeart` and the removal of
  `ic_favorite_on`, `ic_favorite_off` and `ic_favorite_remove` -> 04.
- `native-ios` -> 05, 06.
- Design docs and the project convention -> 07.

## Risks
- Home and Title details in `native-android` change color before 04: the background becomes `#130E1F`, and they
  still use their own private colors. Mitigation: this is the same short-lived state `cmp` had between #48 and #49;
  04 follows directly.
- `Typography()` and `darkColorScheme()` in JVM unit tests: these are the same Material 3 APIs the `cmp` Android
  host tests already ran with. Mitigation: if a test needs Android framework classes, it moves to a
  `ComposeTestRule`-free check of the values only (sizes and weights), as `cmp` does.
- The manual check needs a device or emulator (`scripts/run-native-android.sh`), which publishes the shared core
  first. Mitigation: run it in Finish; if no emulator is available, say so in the report.

## Deviations
- The files were generated from the final `cmp` files by a converter. It changes the package, swaps `Res.` for
  `R.`, moves resource imports to `androidx.compose.ui.res` and Koin to `org.koin.androidx.compose`, and marks
  top-level functions and objects `internal`, as `native-android` does. The converter reproduced the existing
  `native-android` files from the pre-token `cmp` files, apart from two `native-android` differences that are kept:
  - no test tags in Onboarding;
  - `stringResource(R.string.remove_favorite_heart)` in place of `removeFavoriteLabel()`.
- `TmdbTheme` stays public, as before.
