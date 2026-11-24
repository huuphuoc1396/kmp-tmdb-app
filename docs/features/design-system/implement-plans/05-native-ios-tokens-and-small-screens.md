# Plan: native-ios tokens and smaller screens

Requirement: [05 - native-ios: tokens and smaller screens](../subtasks/05-native-ios-tokens-and-small-screens.md)
Status: Done

## Summary
`native-ios` gets a SwiftUI design system with the same token names and values as `cmp` and `native-android`:
- `TmdbColor`: every Material 3 dark role.
- `TmdbTextStyle` with a `.tmdbFont(_:)` modifier: Material 3 sizes, line heights, tracking and brand weights,
  scaled with Dynamic Type through `@ScaledMetric`.
- `TmdbShape`: corner radii.
- `TmdbSpacing`.
- `TmdbSize`: shared sizes.
- `FavoriteHeart`: the shared heart for AC-9.

The shared components, Onboarding, My favorites and the app root then move to these tokens. They keep iOS controls:
the navigation bar, the custom overlay dialog and `Button` styles. Home and Title details still use the old `Theme`
enum until 06, which deletes it.

## Decisions
- Text: a `TmdbTextStyle` enum (15 Material 3 roles) and a `.tmdbFont(_ style:, weight:)` view modifier — user
  choice over mapping to iOS text styles (iOS body is 17pt, not 16, which would break AC-1).
  - Size, line height (applied as `lineSpacing = lineHeight - size`) and tracking are the Material 3 values.
  - Weights: display, headline and `titleLarge` Bold; `titleMedium`, `titleSmall` and labels SemiBold; body Regular.
  - Each role scales with Dynamic Type relative to the closest iOS text style:
    - display and `headlineLarge` → `.largeTitle`;
    - `headlineMedium`/`headlineSmall` → `.title`;
    - `titleLarge` → `.title2`;
    - `titleMedium` and `bodyLarge` → `.body`;
    - `titleSmall`, `bodyMedium` and `labelLarge` → `.subheadline`;
    - `bodySmall` and `labelMedium` → `.footnote`;
    - `labelSmall` → `.caption`.
  - The optional `weight` replaces the role's weight, for the Onboarding headline (Light).
- Onboarding artwork: the two gradient colors become Color Sets in `Assets.xcassets` — user choice over keeping them
  in code as an exception.
  - `OnboardingArtworkTop` is `#303243` and `OnboardingArtworkBottom` is `#3D458F`. They play the role of the
    Android artwork drawable.
  - The scrim color `#15151D` becomes `TmdbColor.surface`; its stops and opacities (0 → 0.85) stay as they are.
- Touch target: `TmdbSize.touchTarget = 48`, the same as Android — user choice over Apple's 44 (48 still meets it).
- Colors: `enum TmdbColor` holds every role from `cmp`'s `TmdbDarkColorScheme` with the same hex values, through a
  private `Color(hex:)` initializer in `TmdbColor.swift`. The brand pink stays `#FF1F8A` with white labels (the
  accepted contrast exception).
- Shapes: `enum TmdbShape { extraSmall 4, small 8, medium 12, large 16, extraLarge 28 }` as `CGFloat` radii, used
  with `RoundedRectangle(cornerRadius:)`. Full shapes use `Capsule()`/`Circle()`.
- Spacing and sizes:
  - `enum TmdbSpacing { xxs 4, xs 8, sm 12, md 16, lg 20, xl 24, xxl 32, xxxl 48 }`.
  - `enum TmdbSize { touchTarget 48, iconSmall 20, iconMedium 24, borderWidth 1 }`.
  - Values used in one place are written inline, as in `cmp` and `native-android`.
- `FavoriteHeart(isFavorite:)`:
  - A 32pt circle in `TmdbColor.surfaceContainerHigh.opacity(0.65)` holding a 16pt template image.
  - The image is `HeartFilled` in `TmdbColor.secondary` when the title is a favorite, or the new `HeartOutline` in
    `TmdbColor.onSurface` when it is not.
  - Both image sets use `template-rendering-intent: template`.
- The old `Theme` enum and `homeScreenPadding` stay until 06; only screens in this subtask stop using them.
  `favoritesCardColor` and `favoritesSecondaryText` are deleted (used only by My favorites).
- The My favorites navigation bar keeps the iOS bar; its background becomes `TmdbColor.surface` (was `Theme.track`),
  so the bar blends with the page. The app root background (`AppRootView`) becomes `TmdbColor.background`, and
  `LaunchBackground` changes to `#130E1F` so the launch screen matches it.
- Value mapping follows plan 01's table. Spacing values that differ only on iOS:
  - My favorites top padding is 16 → `md`, because there is a system navigation bar above it.
  - The `RestoringRow` trailing padding is 16 → `md`.
- Tests are XCTest in `TmdbNativeTests`, named `test…`, as `MetaLineTests` does. The Xcode project uses
  synchronized folders, so new files need no `project.pbxproj` edits.
- Design Home (maybe): used — only for `PosterTile`, which appears on Home. The Home screen is 06.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Token values and contrast cases | `cmp` `Color.kt`, `Type.kt`, `TmdbSpacing.kt`, `TmdbSize.kt`, token tests | `cmp/designsystem/src/...` |
| Final layout per component and screen | `cmp` Onboarding, My favorites, dialog, poster tile, `FavoriteHeart` | `cmp/feature/...`, `cmp/designsystem/...` |
| Heart outline path | `FavoriteOff.imageset/ic_favorite_off.svg` (second path) | `native-ios/TmdbNative/Assets.xcassets` |
| Dialog, poster tile, row | `removeFavoriteDialog`, `PosterTile`, `RestoringRow` | `native-ios/TmdbNative/Component/` |
| Test style | `MetaLineTests` | `native-ios/TmdbNativeTests/` |

## Changes overview
All paths are under `native-ios/`.

| Area | Files (new / modified) | Why |
|---|---|---|
| Design system | `TmdbNative/DesignSystem/TmdbColor.swift`, `TmdbTextStyle.swift`, `TmdbShape.swift`, `TmdbSpacing.swift`, `TmdbSize.swift` (new) | Token set |
| Shared components | `TmdbNative/Component/FavoriteHeart.swift` (new); `PosterTile.swift`, `RemoveFavoriteDialog.swift`, `RestoringRow.swift` (modify) | Components on tokens |
| Assets | `Assets.xcassets/HeartOutline.imageset` (new), `HeartFilled.imageset/Contents.json` (template intent), `OnboardingArtworkTop.colorset`, `OnboardingArtworkBottom.colorset` (new), `LaunchBackground.colorset` (modify) | Tintable hearts, artwork colors |
| App root | `TmdbNative/AppRootView.swift` (modify) | Background role |
| Onboarding | `Onboarding/OnboardingView.swift`, `Onboarding/Component/GetStartedButton.swift` (modify) | On tokens |
| My favorites | `Favorites/FavoritesView.swift`, `Favorites/Component/FavoriteCard.swift` (modify) | On tokens, shared heart |
| Tests | `TmdbNativeTests/TmdbSpacingTests.swift`, `TmdbColorTests.swift`, `TmdbTextStyleTests.swift` (new) | Token checks |

## Steps
### Step 1: Token set  (AC-1, AC-2, AC-5) ✅
- Files: the five `DesignSystem/*.swift` files.
- What:
  - `enum TmdbColor { static let primary: Color … }` with every role and hex from `cmp`, plus a private
    `extension Color { init(hex: UInt32) }`.
  - `enum TmdbTextStyle: CaseIterable` with `size`, `lineHeight`, `tracking`, `weight` and
    `relativeTo: Font.TextStyle`.
  - `struct TmdbFont: ViewModifier`:
    - Fields `@ScaledMetric var size` and `@ScaledMetric var lineSpacing`, built in `init(style:weight:)` with
      `ScaledMetric(wrappedValue:relativeTo:)`.
    - `body` applies `.font(.system(size:weight:))`, `.lineSpacing` and `.tracking`.
  - `extension View { func tmdbFont(_ style: TmdbTextStyle, weight: Font.Weight? = nil) -> some View }`.
  - `TmdbShape`, `TmdbSpacing` and `TmdbSize` as described in Decisions.
- Unit tests (`TmdbNativeTests/`):
  - `TmdbSpacingTests`: `testEveryStepIsAMultipleOfFour`, `testStepsGrow`.
  - `TmdbColorTests`:
    - `testPrimaryIsTheBrandPurple`, `testSecondaryIsTheBrandPink`, `testSurfacesUseTheFavoritesPurpleFamily`:
      RGB read through `UIColor(color).getRed`.
    - `testLabelsOnTheBrandPinkReachThreeToOne`, `testSecondaryTextOnSurfaceContainerReachesAa`,
      `testTextRolesReachAaOnTheirBackgrounds`: the same pairs as `cmp`, with relative luminance computed in a
      private helper.
  - `TmdbTextStyleTests`: `testSizesAndLineHeightsMatchMaterial` (the 15 roles' size and line height),
    `testDisplayHeadlineAndTitleLargeAreBold`, `testTitleMediumTitleSmallAndLabelsAreSemibold`,
    `testBodyRolesAreRegular`.
- Compile: `xcodebuild -project TmdbNative.xcodeproj -scheme TmdbNative -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`
  (in `native-ios/`; the shared core package is already published in `build/swift-package/TmdbCore`).

### Step 2: Assets and shared components  (AC-3, AC-4, AC-6, AC-9, EC-5) ✅
- Files: `Component/FavoriteHeart.swift` (new); `PosterTile.swift`, `RemoveFavoriteDialog.swift`,
  `RestoringRow.swift` (modify); the asset sets above.
- What:
  - **Assets:**
    - `HeartOutline.imageset/heartOutline.svg`: the outline path, viewBox `8.6 9.4 15.6 14.04`, fill white.
    - `HeartFilled` and `HeartOutline` set `"template-rendering-intent" : "template"`.
    - Two artwork Color Sets.
    - `LaunchBackground` becomes `0x13 0x0E 0x1F`.
  - **`FavoriteHeart`:** as in Decisions; the image is `.resizable().scaledToFit()`.
  - **`PosterTile`:**
    - Background `TmdbColor.surfaceVariant`.
    - Name `.tmdbFont(.titleSmall)` in `TmdbColor.onSurface`, padding `TmdbSpacing.xs`.
    - The `@ScaledMetric` goes.
  - **`RemoveFavoriteDialog`:**
    - Background `TmdbColor.surfaceContainer` with radius `TmdbShape.extraLarge`.
    - Margin `lg`; padding `xl` (top, sides) and `lg` (bottom); gaps `lg`, `xl`, `sm`.
    - Text: title `.titleLarge` in `onSurface`; message `.bodyMedium` in `onSurfaceVariant`; buttons `.titleMedium`.
    - Buttons: `minHeight: TmdbSize.touchTarget`, radius `TmdbShape.medium`. Cancel has a stroke in `outline` with
      `TmdbSize.borderWidth`; confirm is `secondary` with `onSecondary` text.
    - Scrim: `TmdbColor.scrim.opacity(0.68)`.
    - The private colors, constants and `@ScaledMetric`s go.
  - **`RestoringRow`:** trailing padding `TmdbSpacing.md`. `homeScreenPadding` stays the default until 06.
- Compile: same as Step 1.

### Step 3: App root and Onboarding  (AC-3, AC-4, AC-6, EC-5) ✅
- Files: `AppRootView.swift`, `Onboarding/OnboardingView.swift`, `Onboarding/Component/GetStartedButton.swift`.
- What:
  - **`AppRootView`:** background `TmdbColor.background`.
  - **`OnboardingView` colors:** artwork gradient `Color("OnboardingArtworkTop")` → `Color("OnboardingArtworkBottom")`;
    scrim from `TmdbColor.surface`.
  - **`OnboardingView` layout:**
    - Stack spacing `TmdbSpacing.lg`, padding `TmdbSpacing.xxl` (leading, trailing, bottom).
    - `frame(maxWidth: 320)` inline; logo `frame(height: 15)` inline.
  - **`OnboardingView` text:** headline `.tmdbFont(.headlineLarge, weight: .light)`, body `.tmdbFont(.bodyLarge)`,
    both in `TmdbColor.onSurface`. The four `@ScaledMetric`s go.
  - **`GetStartedButton`:**
    - Label `.tmdbFont(.titleMedium)` in `TmdbColor.onPrimary`.
    - `minHeight: TmdbSize.touchTarget`, padding `TmdbSpacing.sm`.
    - Gradient `TmdbColor.primary` → `TmdbColor.primaryContainer`.
- Compile: same as Step 1.

### Step 4: My favorites  (AC-3, AC-4, AC-5, AC-6, AC-9, EC-1, EC-5) ✅
- Files: `Favorites/FavoritesView.swift`, `Favorites/Component/FavoriteCard.swift`.
- What:
  - **`FavoritesView`:**
    - Page `TmdbColor.surface`; navigation bar background `TmdbColor.surface`.
    - Padding `lg`, top `md`, bottom `xl`; spacing `sm`; subtitle → list `sm`; empty top `xxxl`.
    - Text: title `.headlineMedium` in `onSurface`; subtitle and empty text `.bodyMedium` in `onSurfaceVariant`.
    - `CountBadge`: `.labelMedium` in `secondary` on `surfaceContainer`, `minHeight: 28` inline, padding `sm`.
  - **`FavoriteCard`:**
    - `minHeight: 120` inline, radius `TmdbShape.medium`, `surfaceContainer`.
    - Padding `sm` (leading, vertical, trailing).
    - Thumbnail `64 × 96` inline with radius `TmdbShape.small`; thumbnail → text `md`; name → meta `xs`.
    - Text: name `.titleMedium` in `onSurface` (2 lines); meta `.bodyMedium` in `onSurfaceVariant`.
    - Remove button: `FavoriteHeart(isFavorite: true)` inside a `TmdbSize.touchTarget` frame.
    - `favoritesCardColor`, `favoritesSecondaryText`, the constants and the `@ScaledMetric`s go.
- Compile: same as Step 1.

### Step 5: Run checks ✅
- Tests: `xcodebuild -project TmdbNative.xcodeproj -scheme TmdbNative -destination 'platform=iOS Simulator,name=iPhone 17' test`
  (in `native-ios/`; use the available simulator name if iPhone 17 is missing).
- Raw-value grep (AC-3) over `Component/`, `Onboarding/`, `Favorites/` and `AppRootView.swift`:
  `grep -nE 'Color\(red:|Color\.(white|black)|\.foregroundStyle\(\.white\)|\.system\(size:|@ScaledMetric|cornerRadius: [0-9]'`
  finds nothing.
- Manual (iOS simulator):
  - Onboarding and My favorites at the default text size and at the largest Dynamic Type size; then reset it.
  - A favorite shows the filled heart on My favorites.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 (completes AC-1: `native-ios` matches `cmp`) | 1 | `TmdbColorTests`, `TmdbTextStyleTests`, `TmdbSpacingTests` with the same expected values as `cmp` |
| AC-2 | 1 | `TmdbSpacingTests.testEveryStepIsAMultipleOfFour` |
| AC-3 (partial: shared components, Onboarding, My favorites, app root) | 2, 3, 4 | Step 5 raw-value grep |
| AC-4 (partial: same screens) | 2, 3, 4 | Manual: the screens match `cmp` |
| AC-5 (partial: `native-ios` except Home and Details colors) | 1 | `TmdbColorTests` |
| AC-6 (partial: Onboarding, My favorites) | 1, 3, 4 | Manual: largest Dynamic Type size |
| AC-9 (partial: shared heart, My favorites) | 2, 4 | Manual: a favorite shows the filled heart on My favorites |
| EC-5 (partial) | 2, 3, 4 | Manual: buttons and the heart are still tappable at the largest size |

## Out of scope
- Home and Title details: `Theme`, `homeScreenPadding`, the old heart images (`FavoriteRemove`, `FavoriteOn`,
  `FavoriteOff`) and the Home gradient -> 06.
- Design docs and the project convention -> 07.

## Risks
- `UIColor(Color).getRed` in tests reads the color in the display color space. Mitigation: colors are built in
  sRGB from hex, and the tests compare with a tolerance of 1/255.
- `.tracking` adds the Material 3 letter spacing that iOS did not have, so text looks slightly wider. Mitigation:
  this is the parity AC-1 asks for; the manual check compares with `cmp`.
- `@ScaledMetric` inside a `ViewModifier` is evaluated per view. Mitigation: this is the same mechanism the views
  use today, just in one place.

## Deviations
- `HeartFilled.svg` keeps its pink fill: with `template-rendering-intent: template` only its shape is used, and
  `FavoriteHeart` tints it.
- `RemoveFavoriteDialog`: the label color is set on each button (`onSurface` for cancel, `onSecondary` for confirm)
  in place of one `.foregroundStyle(.white)` on the button row.
- Tests ran on the booted iPhone 13 simulator (the iPhone 17 named in CLAUDE.md is not installed): 111 tests, 0
  failures.
- At the largest Dynamic Type size the Material 3 line heights scale with the text, so multi-line Onboarding text is
  widely spaced. This is the same ratio as on Android and `cmp`.
