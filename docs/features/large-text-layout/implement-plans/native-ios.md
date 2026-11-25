# Plan: Large text layout (native-ios)

Requirement: [Large text layout](../task.md), `native-ios` only
Status: Done

## Summary
SwiftUI wraps text between words, but falls back to splitting a word when the word alone is wider than its
line ("Rober t", "Doomsda y"). iOS 16 has no option to turn that fallback off, so names get a small
`NameText` component that computes its own lines: it measures words with the same scaled font the token uses,
breaks only between words, cuts a word wider than the line (or the last allowed line) with "…", and draws each
line as a one-line `Text`. The favorites header uses `ViewThatFits` to choose between today's one-line header
and a wrapped one with "View all ›" on its own trailing line.

## Decisions
- Names go through one component, `NameText(_:style:lineLimit:alignment:)` in `TmdbNative/Component` — the five
  name sites (poster placeholder, Home favorites item, My favorites card, Title details header, cast) share the
  rule (AC-3, AC-4), so it lives once.
- Line breaking is a pure function `NameLines.lines(of:width:lineLimit:measure:)` with an injected `measure`
  closure — it is unit-testable without rendering; the view passes a measure built from
  `UIFont.systemFont(ofSize:weight:)` at the `@ScaledMetric` size plus the style's tracking (kern), matching
  `TmdbFont`.
- Each computed line is drawn as its own `Text` with `lineLimit(1)` and `truncationMode(.tail)`, stacked with
  the scaled token line spacing — if a measurement is a fraction off, the line ends with "…" instead of
  wrapping a word again.
- A word wider than the line is cut with "…" and the name stops there, as in `cmp` `NameText` (lines before it
  stay); when the line limit is reached, the last visible line is cut at a character with "…" appended (AC-4),
  like TextKit tail truncation.
- The available width is read with a `GeometryReader` in the background of a `maxWidth: .infinity` frame and
  kept in `@State`; until it is known the view draws the plain `Text` (one frame, then the computed lines).
- The accessibility label stays the full name: the line stack is one element (`.accessibilityElement(children:
  .ignore)` + `.accessibilityLabel(name)`), and the header trait stays on the Title details name (screen-reader
  order and labels unchanged).
- Favorites header: `ViewThatFits(in: .horizontal)` with (1) today's `HStack`, where the title, badge and
  "View all ›" are `fixedSize()` so an overflow is detected, and (2) a `VStack(alignment: .leading)` of the title
  and badge row, then "View all ›" aligned trailing (AC-1, AC-2). The button keeps `minHeight:
  TmdbSize.touchTarget`; the button view is shared by both variants.
- Title details navigation bar: unchanged (out of scope for `native-ios`, AC-6/AC-7 do not apply).

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Text style size, weight, tracking, line spacing, Dynamic Type relative style | `TmdbTextStyle`, `.tmdbFont(_:)` | `TmdbNative/DesignSystem/TmdbTextStyle.swift` |
| Spacing, touch target | `TmdbSpacing`, `TmdbSize` | `TmdbNative/DesignSystem` |
| Poster placeholder name | `PosterTile` | `TmdbNative/Component/PosterTile.swift` |
| Fixed row heights scaled by Dynamic Type | `@ScaledMetric` row height in `FavoritesSection`, `CastRow` | `TmdbNative/Home/Component`, `TmdbNative/TitleDetails/Component` |
| Fits-or-stacks layout | `ViewThatFits` as in `DateLine` | `TmdbNative/TitleDetails/Component/DetailsHeader.swift` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `native-ios` app | `Component/NameLines.swift` (new), `Component/NameText.swift` (new) | Word-only line breaking for names |
| `native-ios` app | `Component/PosterTile.swift`, `Home/Component/FavoritesSection.swift`, `Favorites/Component/FavoriteCard.swift`, `TitleDetails/Component/DetailsHeader.swift`, `TitleDetails/Component/CastRow.swift` (modify) | Use `NameText`; favorites header wraps |
| `native-ios` tests | `TmdbNativeTests/NameLinesTests.swift` (new) | Line breaking rules |
| docs | `docs/features/home/design/home.md`, `docs/features/favorites/design/home-favorites.md`, `docs/features/title-details/design/title-details.md`, `docs/features/design-system/parity.md` (modify) | Record the iOS behavior (only the `native-ios` lines; the sibling sessions own theirs) |

## Steps
### Step 1: Name line breaking  (AC-3, AC-4, AC-5, EC-3, EC-4) ✅
- Files: `TmdbNative/Component/NameLines.swift` (new)
- What: `enum NameLines { static func lines(of name: String, width: CGFloat, lineLimit: Int?, measure: (String) -> CGFloat) -> [String] }`
- Pseudo-code:
  ```
  words = name split on whitespace
  lines = []; current = ""
  for word in words:
      candidate = current.isEmpty ? word : current + " " + word
      if measure(candidate) <= width { current = candidate; continue }
      if !current.isEmpty { lines.append(current); current = "" }
      if measure(word) <= width { current = word } else { lines.append(cut(word)) }
  append current if not empty
  if lineLimit, lines.count > lineLimit: lines = first lineLimit; last = cut(last, forcing "…")
  cut(text): drop trailing characters (and trailing spaces) until measure(text + "…") <= width; keep at least one character
  ```
- Unit tests: `NameLinesTests` with `measure = { CGFloat($0.count) }`: `fitsOnOneLine`, `breaksOnlyBetweenWords`,
  `cutsWordWiderThanLineWithEllipsis`, `stopsAtCutWord`, `keepsLinesBeforeCutWord`, `cutsLastLineWhenLimitReached`,
  `keepsAllLinesWithoutLimit`, `emptyNameGivesNoLines`.
- Compile: `xcodebuild -project TmdbNative.xcodeproj -scheme TmdbNative -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`

### Step 2: NameText view  (AC-3, AC-4, AC-8) ✅
- Files: `TmdbNative/Component/NameText.swift` (new)
- What: `struct NameText: View { init(_ name: String, style: TmdbTextStyle, lineLimit: Int?, alignment: TextAlignment = .leading) }`
  - `@ScaledMetric` size and line spacing relative to `style.relativeTo` (as `TmdbFont`); `@State width: CGFloat?`.
  - Body: `VStack(alignment: horizontal(alignment), spacing: lineSpacing)` of `Text(verbatim: line).tmdbFont(style).lineLimit(1).truncationMode(.tail)`,
    `.frame(maxWidth: .infinity, alignment:)`, background `GeometryReader` writing the width; before the width
    is known, `Text(verbatim: name).tmdbFont(style).lineLimit(lineLimit)`.
  - Measure: `NSAttributedString(string:, attributes: [.font: UIFont.systemFont(ofSize: size, weight: style.uiWeight), .kern: style.tracking]).size().width`
    (`Font.Weight` → `UIFont.Weight` mapping as a private extension in this file).
  - Accessibility: one element, label = name. Foreground color stays with the caller.
- Compile: as Step 1.

### Step 3: Use NameText for every name  (AC-3, AC-4, AC-5, EC-3, EC-4, AC-8) ✅
- Files: `PosterTile.swift` (`lineLimit: 4`, centered), `FavoritesSection.swift` `FavoriteItem` (`titleSmall`, 2),
  `FavoriteCard.swift` (`titleMedium`, 2), `DetailsHeader.swift` (`titleLarge`, `nil`, keeps
  `.accessibilityAddTraits(.isHeader)`), `CastRow.swift` `CastCell` (`labelMedium`, 2, centered, keeps
  the fixed `nameHeight` frame with top alignment).
- What: replace the `Text(name)` + `.lineLimit` / `.truncationMode` / `.multilineTextAlignment` lines with
  `NameText`; colors and paddings unchanged.
- Compile: as Step 1.

### Step 4: Favorites header wraps when needed  (AC-1, AC-2, EC-1, EC-2, AC-8) ✅
- Files: `Home/Component/FavoritesSection.swift`
- What: extract `title`, `badge` and `viewAllButton` as private computed views; header =
  `ViewThatFits(in: .horizontal) { HStack(spacing: xs) { title.fixedSize(); badge; Spacer(minLength: 0); viewAllButton.fixedSize() } ; VStack(alignment: .leading, spacing: 0) { HStack(spacing: xs) { title; badge }; viewAllButton.frame(maxWidth: .infinity, alignment: .trailing) } }`;
  the badge keeps `fixedSize()` so a two-digit count stays on the title line. Paddings unchanged.
- Compile: as Step 1.

### Step 5: Docs  (constraints) ✅
- Files: the four design docs above.
- What: add the iOS lines: names break only between words via `NameText`; favorites header wraps "View all ›"
  to its own trailing line; Title details navigation bar unchanged on iOS.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 | 4 | Manual: simulator xxxLarge, en + pt-BR, iPhone SE |
| AC-2 | 4 | Manual: xxxLarge wraps, default size one line |
| AC-3 | 1, 2, 3 | `breaksOnlyBetweenWords`; manual cast/favorites at xxxLarge |
| AC-4 | 1, 3 | `cutsWordWiderThanLineWithEllipsis`, `cutsLastLineWhenLimitReached` |
| AC-5 | 1, 3 | `keepsAllLinesWithoutLimit`; manual Title details |
| AC-6, AC-7 | — | Not for `native-ios` (system navigation bar) |
| AC-8 | 2, 3, 4 | Manual: default size screenshots compared with `main` |
| AC-9 | all | Manual (same screens as the sibling apps) |
| EC-1 | 4 | Manual: badge on the title line |
| EC-2 | 4 | Manual: pt-BR "Ver todos" |
| EC-3 | 1 | `cutsWordWiderThanLineWithEllipsis`, `stopsAtCutWord`, `keepsLinesBeforeCutWord` |
| EC-4 | 3 | Placeholder (`PosterTile` name) uses `NameText`; "loading" texts are not names |
| EC-5–EC-7 | — | Top bar, not for `native-ios` |

## Out of scope
- `native-ios` Title details navigation bar (system bar).
- `cmp` and `native-android` (sibling sessions).
- AX1–AX5 Dynamic Type sizes.

## Risks
- Measured widths may differ by a fraction from SwiftUI's rendering — each line is one `Text` with tail
  truncation, so the worst case is an early "…", never a split word.
- One frame with the unmeasured `Text` before the width is known — cells are fixed-size and the frame is not
  visible in practice; checked on the simulator.

## Deviations
- The wrapped header's title row gets `minHeight: TmdbSize.touchTarget`, so the card's top padding (which offsets
  the touch target) stays the same in both header variants.
- Simulator check ran on iPhone 17 (the iPhone SE was not available to drive): at xxxLarge the English and pt-BR
  headers still fit on one line there; the wrapped header was checked at an accessibility size (AX3).
- After merging `main`: a word wider than its line now ends the name (was: later words continued on the next
  lines), to match `cmp` `NameText` and the "Large text" sections of the design docs (AC-9).
- The design-doc and `parity.md` edits were dropped in the merge: `main` already records the behavior for all
  three apps.
