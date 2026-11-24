# Plan: native-ios Home and Title details

Requirement: [06 - native-ios: Home and Title details](../subtasks/06-native-ios-home-and-title-details.md)
Status: Done

## Summary
Home and Title details in `native-ios` move to the SwiftUI tokens from 05 (`TmdbColor`, `.tmdbFont`, `TmdbShape`,
`TmdbSpacing`, `TmdbSize`) and to `FavoriteHeart`, following the choices `cmp` made in plan 02. Then the old `Theme`
enum, `homeScreenPadding`, the old heart images and the remaining size globals are removed. After this, `native-ios`
holds no raw color, text size or corner radius outside its design system and asset catalog. All three apps then
share one design system.

## Decisions
- Every role, type role and rounding follows plan 02 (`02-cmp-home-and-title-details.md`):
  - Details page `surface`; track `#303243` → `surfaceContainerHigh`; ring center → `surface`; divider →
    `outlineVariant`.
  - Date line `onSurfaceVariant`; synopsis `onSurface`.
  - Section headings and the Home "Popular" heading `titleLarge`; Home favorites title `titleLarge`; Home title
    `headlineMedium`.
  - Home count badge `secondaryContainer`/`onSecondaryContainer`.
  - Tabs: selected `titleMedium` in `onSecondary` on `secondary`; unselected `bodyLarge` in `onSurfaceVariant`.
  - Search hint `bodyLarge` in `onSurfaceVariant`.
  - Screen padding 30 → `xxl`; spacing rounds to the nearest step, ties to the larger one.
  - Sizes used once are inline and rounded to the 4pt grid; stroke and border widths keep their value.
- Home background — user choice over a gradient between color roles or a flat surface: the gradient stays as
  artwork, with its two colors in the asset catalog, the same values as the `cmp`/Android drawable
  `home_background`.
  - `HomeBackgroundTop` `#303243`, `HomeBackgroundBottom` `#15151D`; the 0.65394 stop stays.
  - The header glow uses `TmdbColor.primary` → `TmdbColor.primary.opacity(0)`.
  - This answers the last open question in `task.md`.
- Rating ring value: the SwiftUI equivalent of the `cmp` capped, auto-sized text.
  - `.tmdbFont(.titleMedium, weight: .regular)` with the number `.bold()`.
  - `.dynamicTypeSize(...DynamicTypeSize.xLarge)` caps scaling at about 1.12×, close to `cmp`'s 1.15.
  - `.lineLimit(1)`, `.minimumScaleFactor(0.5)` and a `TmdbSpacing.xs` inset keep it inside the 56pt ring.
- Date line: the existing `ViewThatFits` (one row, or date and runtime stacked) already wraps as a group, which is
  what `cmp`'s `FlowRow` does. It is kept; only its tokens change.
- Hearts:
  - `FavoritesSection` uses `FavoriteHeart(isFavorite: true)`.
  - `FavoriteToggle` draws `FavoriteHeart(isFavorite: configuration.isOn)`, keeping its `Toggle` accessibility.
  - The `FavoriteRemove`, `FavoriteOn` and `FavoriteOff` image sets are deleted.
- Shared sizes:
  - `PosterRow.swift` gets `enum PosterRowDefaults { static let posterWidth: CGFloat = 124; static let posterHeight: CGFloat = 188 }`,
    the Swift form of `cmp`'s `PosterRowDefaults`.
  - `rowMinHeight` is replaced by `PosterRowDefaults.posterHeight`.
  - `tabsMinHeight` (44) is replaced by `TmdbSize.touchTarget` (48, the 05 decision).
  - `ratingRingSize`, `artworkHeight` and `homeScreenPadding` are removed: the first two become inline or file-private
    values, the last becomes `TmdbSpacing.xxl`.
- Text-height calculations (the favorites row height and the cast name height) use `@ScaledMetric` values built from
  `TmdbTextStyle` line heights, in place of the font size × 1.25 factor:
  - Favorites: name `titleSmall.lineHeight × 2`, meta `bodySmall.lineHeight`.
  - Cast: `labelMedium.lineHeight × 2`.

  Each is bound to the style's `relativeTo`, so they scale with the text they measure.
- Navigation bar background on Title details: `TmdbColor.surfaceContainerHigh` (was `Theme.track`).
- `Theme.swift` is deleted at the end, once nothing references it.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Tokens, text modifier | `TmdbColor`, `.tmdbFont`, `TmdbShape`, `TmdbSpacing`, `TmdbSize` | `native-ios/TmdbNative/DesignSystem/` |
| Heart | `FavoriteHeart` | `native-ios/TmdbNative/Component/FavoriteHeart.swift` |
| Final values per component | `cmp` Home and Title details after #49 (plan 02 mapping table) | `cmp/feature/home/ui`, `cmp/feature/title-details/ui` |
| Asset colors | Color Set format of `OnboardingArtworkTop` (05) | `native-ios/TmdbNative/Assets.xcassets` |

## Changes overview
All paths are under `native-ios/TmdbNative/`.

| Area | Files (new / modified / deleted) | Why |
|---|---|---|
| Home | `Home/HomeView.swift`; `Home/Component/HomeHeader.swift`, `SearchField.swift`, `CategoryTabs.swift`, `FavoritesSection.swift`, `RowMessage.swift`, `PosterRow.swift` (modify) | Home on tokens |
| Title details | `TitleDetails/TitleDetailsView.swift`; `TitleDetails/Component/DetailsHeader.swift`, `RatingRing.swift`, `CastRow.swift`, `TrailerButton.swift`, `GenreChips.swift`, `SectionHeading.swift`, `DetailsArtwork.swift`, `FavoriteToggle.swift` (modify) | Title details on tokens |
| Assets | `HomeBackgroundTop.colorset`, `HomeBackgroundBottom.colorset` (new); `FavoriteRemove`, `FavoriteOn`, `FavoriteOff` imagesets (delete) | Home artwork, old hearts |
| Design system | `Theme.swift` (delete) | Replaced by `TmdbColor` |

## Steps
### Step 1: Home on tokens  (AC-3, AC-4, AC-5, AC-6, AC-9, EC-1, EC-5) ✅
- Files: the seven Home files; the two Home Color Sets.
- What:
  - **`HomeView`:**
    - Background: the asset gradient and the `primary` glow, `height: 520` inline.
    - Spacing: top `lg`, bottom `xxxl`, header→search `md`, search→tabs `xl`, tabs→featured `xxl`,
      featured→heading `xl`, featured→favorites `xxl`, favorites→heading `xxl`, heading→popular `md`.
    - Favorites inset: `TmdbSpacing.xxl - TmdbSpacing.sm`.
    - Heading `.tmdbFont(.titleLarge)` in `onSurface`; spinners tint `secondary`; row min height
      `PosterRowDefaults.posterHeight`.
  - **`HomeHeader`:**
    - Title `.headlineMedium` in `onSurface`; spacing and top offset `xs`.
    - Avatar `40` inline on `surfaceVariant`; person symbol `font(.system(size: TmdbSize.iconMedium))` in
      `onSurface.opacity(0.5)`.
  - **`SearchField`:** hint `.bodyLarge` in `onSurfaceVariant` with `lineLimit(1)`; padding `lg`/`md`;
    `minHeight: 48`; `surfaceVariant` capsule.
  - **`CategoryTabs`:**
    - Spacing `xxl`, leading `xxl`, trailing `md`.
    - Labels as in Decisions; selected padding `md`, vertical `xs`; `minHeight: TmdbSize.touchTarget`.
  - **`RowMessage`:**
    - Spacing `sm`; message `.bodyLarge` in `onSurfaceVariant`.
    - Retry: `.titleMedium` in `onSecondary` on a `secondary` capsule, padding `md` × `xs`,
      `minHeight: TmdbSize.touchTarget`.
    - Padding `xxl`; min height `PosterRowDefaults.posterHeight`.
  - **`PosterRow`:** `PosterRowDefaults`, corner `TmdbShape.small`, spacing `sm`.
  - **`FavoritesSection`:**
    - Card: `surfaceContainer`, corner `TmdbShape.large`, `surfaceContainerHigh` stroke at `TmdbSize.borderWidth`.
    - Spacing: header top `lg` (with the existing touch-target offset), bottom `md`, sides `sm`, header end `xxs`,
      gaps `xs`/`sm`/`md`.
    - Badge: `private let badgeSize: CGFloat = 24` (used three times), `.labelMedium` in `onSecondaryContainer` on
      `secondaryContainer`, padding `xs`.
    - View all: `.labelLarge` in `secondary`, chevron `12` inline.
    - Items: width `96` (constant, used twice); poster height `148`, corner `TmdbShape.small`; name `.titleSmall`;
      meta `.bodySmall` in `onSurfaceVariant`.
    - Heart: `FavoriteHeart(isFavorite: true)` in a `TmdbSize.touchTarget` frame, inset `xxs`.
    - Row height: from scaled `TmdbTextStyle` line heights.
- Compile: `xcodebuild -project TmdbNative.xcodeproj -scheme TmdbNative -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`
  (in `native-ios/`).

### Step 2: Title details on tokens  (AC-3, AC-4, AC-5, AC-6, AC-9, EC-1, EC-4, EC-5) ✅
- Files: the nine Title details files.
- What:
  - **`TitleDetailsView`:**
    - Page `surface`; navigation bar `surfaceContainerHigh`; spinner tint `secondary`.
    - Spacing: header top `376` inline; gaps `lg`, `lg`, `xxl`, `xxxl`, `md`, `xxxl`, `lg`; bottom `xxxl`;
      horizontal padding `xxl`.
    - Divider: `outlineVariant`, height `TmdbSize.borderWidth`.
    - Synopsis: `.bodyMedium` in `onSurface`.
  - **`DetailsHeader`:**
    - Ring→name `md`, name→date `xxs`; name `.titleLarge` in `onSurface`.
    - Date line: `.bodyLarge` in `onSurfaceVariant`; dot `4` inline with paddings `xxs`/`xs`; stacked spacing
      `xxs`; clock `TmdbSize.iconSmall`; clock→runtime `xs`.
  - **`RatingRing`:** `56` and stroke `6` inline; `surfaceContainerHigh` track, `surface` center, `secondary` arc;
    the value as in Decisions.
  - **`CastRow`:**
    - Sizes: width `76`; photo `60`; `private let photoBorder: CGFloat = 4` (used several times).
    - Spacing `xxs`; photo→name `xxs`.
    - Name `.labelMedium` in `onSurface`; name height from the scaled `labelMedium` line height × 2.
    - Placeholder symbol `32` inline in `onSurface.opacity(0.5)`; fills `surfaceContainerHigh`.
  - **`TrailerButton`:** gradient `primary` → `primaryContainer`; icon `TmdbSize.iconSmall`; spacing `sm`; label
    `.titleMedium` in `onPrimary`; padding `sm`; `minHeight: 44` inline.
  - **`GenreChips`:** flow spacing `md`, line spacing `sm`; label `.titleMedium` in `onSurface`; padding `md`;
    `minHeight: 32` inline; `surfaceContainerHigh` capsule. The preview background becomes `TmdbColor.surface`.
  - **`SectionHeading`:** `.titleLarge` in `onSurface`, `lineLimit(1)`.
  - **`DetailsArtwork`:**
    - `private let artworkHeight: CGFloat = 492`; fade stops `260/492` and `412/492`.
    - Fade to `TmdbColor.surface`; placeholder `surfaceContainerHigh`.
  - **`FavoriteToggle`:** the label is `FavoriteHeart(isFavorite: configuration.isOn)` with `contentShape(Circle())`.
- Compile: same as Step 1.

### Step 3: Remove what is left of the old design  (AC-3) ✅
- Files: delete `Theme.swift` and the `FavoriteRemove`, `FavoriteOn` and `FavoriteOff` image sets.
- What: before deleting, `grep -rn 'Theme\.\|homeScreenPadding\|FavoriteRemove\|FavoriteOn\|FavoriteOff' native-ios/TmdbNative`
  finds no reference. Previews that used `Theme` use `TmdbColor`.
- Compile: same as Step 1.

### Step 4: Run checks ✅
- Tests: `xcodebuild -project TmdbNative.xcodeproj -scheme TmdbNative -destination 'platform=iOS Simulator,name=<available iPhone>' test`.
- Raw-value grep (AC-3) over all of `native-ios/TmdbNative` except `DesignSystem/`:
  `grep -rnE 'Color\(red:|Color\.(white|black)|\.(white|black)\.opacity|\.foregroundStyle\(\.white\)|\.system\(size: [0-9]|cornerRadius: [0-9]'`
  finds nothing.
- Manual (iOS simulator), at the default and the largest Dynamic Type size:
  - Home: background, tabs, favorites section with the filled heart, popular row.
  - Title details: ring value inside the ring, date line wrapping, outline/filled heart toggling, cast and genres.

  Then reset the text size.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-3 (completes AC-3) | 1, 2, 3 | Step 4 raw-value grep |
| AC-4 (completes AC-4) | 1, 2 | Manual: Home and Details match `cmp` |
| AC-5 (completes AC-5: Home artwork in assets, Details roles) | 1, 2 | `TmdbColorTests` (05) for roles; manual |
| AC-6 (completes AC-6) | 1, 2 | Manual: largest Dynamic Type size |
| AC-9 (completes AC-9) | 1, 2, 3 | Manual: filled heart on Home and My favorites; Details toggles outline/filled |
| EC-1 (completes EC-1) | 1, 2 | Manual: long title names wrap or end with "…" |
| EC-4 (completes EC-4) | 1, 2 | Manual: posters and the ring keep their size; the ring value stays inside |
| EC-5 (completes EC-5) | 1, 2 | Manual: tabs, view all, hearts, retry and trailer stay tappable |

## Out of scope
- The large-text issues seen in `cmp` ("View all" wrapping, cast names breaking mid-word, the floating top bar) —
  to be decided for all three apps after 07.
- Design docs and the project convention -> 07.

## Risks
- The navigation bar on Title details is a system bar, while `cmp` and Android draw their own top bar. Mitigation:
  iOS controls stay, per task.md; only the color is aligned.
- `.dynamicTypeSize(...xLarge)` caps at about 1.12×, slightly below `cmp`'s 1.15×. Mitigation: within the agreed
  "capped growth"; the ring value stays readable.
- This builds on 05 (PR #52). Mitigation: forge starts after #52 is merged, or on top of its branch.

## Deviations
- Steps 1 and 2 were compiled together: removing `homeScreenPadding` in Step 1 broke `TitleDetailsView` until
  Step 2 replaced it.
- `.tmdbFont` line spacing fix (05 code): SwiftUI adds `lineSpacing` on top of the font's natural line height (about
  1.19 × size), so `lineHeight - size` overshot the Material 3 line height. Two-line cast names were cut to one
  line, and large Onboarding text was widely spaced.
  - `TmdbTextStyle.lineSpacing` is now `max(0, lineHeight - UIFont.systemFont(ofSize: size).lineHeight)`.
  - New test: `TmdbTextStyleTests.testLineSpacingFillsTheMaterialLineHeight`.
- `SectionHeading` drops its old `fixedSize` and uses `lineLimit(1)` with tail truncation, as in `cmp`.
- The raw-value grep leaves one match, `CastRow`'s `.font(.system(size: 32))`. It sizes the placeholder SF Symbol,
  used once and written inline as planned; it is not a text size.
- `project.pbxproj` and `Localizable.xcstrings` had changes made by Xcode (reordered build settings,
  auto-extracted strings). They are not part of this subtask and are left uncommitted.
- Tests: 112, 0 failures (iPhone 13 simulator).
