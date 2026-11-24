# Plan: Large text layout (cmp)

Requirement: [Large text layout](../task.md)
Status: Done

## Summary
Three small, local fixes in the Compose Multiplatform app. The Home favorites header gets a two-child custom
`Layout` that keeps one line when the title + badge and "View all ›" fit, and otherwise puts "View all ›" on its
own trailing line. Names are drawn by a new design-system `NameText` that never shows a word split across lines:
it lays the text out with a `TextMeasurer`, finds the first line that ends inside a word, and stops there with
"…". The Title details top bar takes an `isScrolled` flag and paints `colorScheme.surface` behind itself once the
screen's `ScrollState` has moved.

## Decisions
- Word-safe names are a new `NameText` in `cmp/designsystem` (`text/NameText.kt`) built on `Modifier.layout` +
  `TextMeasurer` + `drawText`, not `BoxWithConstraints` or an `onTextLayout` state loop — `FavoriteCard` sizes its
  row with `IntrinsicSize.Min`, which a subcomposition can't answer, and a state loop draws the split word for one
  frame. Compose has no line-break option that refuses to split a word wider than the line (`LineBreak`,
  `Hyphens` and `WordBreak.Phrase` don't cover it), so the rule is applied after layout.
- The rule: lay the text out with the caller's `maxLines` and clipping; the shown line count is the smaller of
  `maxLines` and the index (+1) of the first line whose end falls between two non-whitespace characters; then lay
  out again with that count and `TextOverflow.Ellipsis`. A break after a hyphen counts as inside a word (cut, not
  split). This is the pure function `wordSafeLineCount(text, lineEnds, maxLines)`, unit-tested.
- `NameText` exposes the text to accessibility with `semantics { text = AnnotatedString(text) }`, so screen
  readers read the full name as `Text` did (AC-7 style: no change in what is announced). Callers keep their own
  modifiers (`heading()` on Title details, `clearAndSetSemantics` on cards and `PosterTile`).
- Favorites header: custom `Layout` (`FavoritesHeader`) with two children (title + badge row, `ViewAll`); fits when
  `title.width + viewAll.width <= maxWidth`; no minimum gap, same as today's weighted spacer. When wrapped,
  "View all ›" is placed at `x = maxWidth - viewAll.width` on the line below. `ViewAll`'s label gets `maxLines = 1`,
  ellipsis and `weight(1f, fill = false)` so the chevron always stays inside the card, even if the label alone is
  wider than the card.
- Top bar background: shown when `scroll.screen.value > 0` (anything scrolled moves under the bar, the artwork
  first), read through `derivedStateOf`; color `MaterialTheme.colorScheme.surface`, faded with
  `animateColorAsState`, drawn before the window-insets padding so it also covers the status bar area. Loading and
  error states have no scroll and stay transparent (EC-6); pull-to-refresh doesn't move `ScrollState` (EC-5);
  `rememberScrollState` is saveable, so rotation and returning restore the state (EC-7).
- Docs: this PR (cmp is the reference) records the behavior in the three design docs and `parity.md`, worded for
  all three apps; the native sessions don't edit those files. Left as a note in the shared notes folder.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Poster placeholder name | `PosterTile` (switch its `Text` to `NameText`) | `cmp/designsystem/.../component/PosterTile.kt` |
| Tokens | `TmdbSpacing`, `TmdbSize`, `MaterialTheme` | `cmp/designsystem` |
| Pure text helper next to `metaLine` | package `designsystem.text` | `cmp/designsystem/.../text/MetaLine.kt` |
| Screen scroll state | `DetailsScroll.screen` (`ScrollState`) | `TitleDetailsScreen.kt` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `cmp:designsystem` | `text/NameText.kt` (new), `text/WordSafeLineCount.kt` (new), `component/PosterTile.kt` (modify), `androidHostTest/.../text/WordSafeLineCountTest.kt` (new) | AC-3, AC-4, EC-3, EC-4 |
| `cmp:feature:home:ui` | `component/FavoritesSection.kt` (modify) | AC-1, AC-2, AC-3, EC-1, EC-2 |
| `cmp:feature:favorites:ui` | `component/FavoriteCard.kt` (modify) | AC-3, AC-4 |
| `cmp:feature:title-details:ui` | `component/DetailsHeader.kt`, `component/CastRow.kt`, `component/DetailsTopBar.kt`, `TitleDetailsScreen.kt` (modify) | AC-3–AC-7 |
| docs | `home/design/home.md`, `favorites/design/home-favorites.md`, `title-details/design/title-details.md`, `design-system/parity.md` | Constraint: docs record the behavior |

## Steps
### Step 1: Word-safe `NameText`  (AC-3, AC-4, EC-3, EC-4) ✅
- Files: `cmp/designsystem/src/commonMain/.../designsystem/text/WordSafeLineCount.kt` (new),
  `.../designsystem/text/NameText.kt` (new), `.../designsystem/component/PosterTile.kt` (modify),
  `cmp/designsystem/src/androidHostTest/.../designsystem/text/WordSafeLineCountTest.kt` (new)
- What:
  - `internal fun wordSafeLineCount(text: String, lineEnds: List<Int>, maxLines: Int): Int`
  - `@Composable fun NameText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE, textAlign: TextAlign = TextAlign.Start)`
  - `PosterTile`: its `Text` becomes `NameText(..., maxLines = 4, textAlign = TextAlign.Center)`, keeping
    padding and `clearAndSetSemantics {}`.
- Pseudo-code:
  ```
  wordSafeLineCount:
    for i in lineEnds.indices:
      end = lineEnds[i]
      if end in 1 until text.length && !text[end-1].isWhitespace() && !text[end].isWhitespace(): return min(i+1, maxLines)
    return min(lineEnds.size, maxLines)

  NameText:
    measurer = rememberTextMeasurer(); merged = style.merge(color = color, textAlign = textAlign)
    holder = remember { LayoutHolder() }   // plain var, written in measure, read in draw
    Spacer(modifier
      .semantics { this.text = AnnotatedString(text) }
      .layout { m, c ->
         box = Constraints(maxWidth = c.maxWidth)
         probe = measurer.measure(text, merged, TextOverflow.Clip, maxLines = maxLines, constraints = box)
         lines = wordSafeLineCount(text, (0 until probe.lineCount).map(probe::getLineEnd), maxLines)
         result = measurer.measure(text, merged, TextOverflow.Ellipsis, maxLines = lines, constraints = box)
         holder.result = result
         w = result.size.width.coerceIn(c.minWidth, c.maxWidth); h = result.size.height.coerceIn(c.minHeight, c.maxHeight)
         p = m.measure(Constraints.fixed(w, h)); layout(w, h) { p.place(0, 0) } }
      .drawBehind { holder.result?.let { drawText(it) } })
  ```
- Unit tests (`WordSafeLineCountTest`): `linesEndingBetweenWordsKeepTheLimit`, `wordSplitAcrossLinesStopsAtItsFirstLine`,
  `longWordAfterAShortOneKeepsTheFirstLine`, `breakAfterHyphenCountsAsInsideAWord`, `fewerLinesThanLimitKeepsAll`,
  `limitIsNeverExceeded`, `singleLineIsKept`, `emptyTextKeepsOneLine`.
- Compile: `./gradlew :cmp:designsystem:compileAndroidMain`

### Step 2: Names on Home, My favorites and Title details  (AC-3, AC-4, AC-5) ✅
- Files: `FavoritesSection.kt` (`FavoriteItem` name, 2 lines), `FavoriteCard.kt` (name, 2 lines),
  `CastRow.kt` (name, 2 lines, centered), `DetailsHeader.kt` (name with year, no line limit, keeps `heading()`)
- What: replace each name `Text(...)` with `NameText(...)` with the same style, color, alignment and line limit.
  The featured, popular and recommendations rows get the change through `PosterTile` (Step 1). Meta lines, headings
  and other text stay `Text`.
- Unit tests: none new (pure rule covered in Step 1; layout checked on the simulator, see Traceability).
- Compile: `./gradlew :cmp:feature:home:ui:compileAndroidMain :cmp:feature:favorites:ui:compileAndroidMain :cmp:feature:title-details:ui:compileAndroidMain`

### Step 3: Favorites header that wraps "View all ›"  (AC-1, AC-2, EC-1, EC-2) ✅
- Files: `cmp/feature/home/ui/.../component/FavoritesSection.kt`
- What:
  - `@Composable private fun FavoritesHeader(count: Int, onSeeAll: () -> Unit, modifier: Modifier)`: `Layout`
    with children `Row(title Text + xs spacer + CountBadge)` and `ViewAll(onSeeAll)`; replaces the current header
    `Row` (same padding modifier: `start = sm, end = xxs`).
  - Measure both with `constraints.copy(minWidth = 0, minHeight = 0)`. One line: height = max of both, each
    vertically centered, `ViewAll` at the trailing edge. Wrapped: title row at (0, 0), `ViewAll` at
    (`maxWidth - width`, `titleHeight`); height = sum. Width = `constraints.maxWidth`. RTL handled by
    `placeRelative`.
  - `ViewAll`: label `Text` gets `maxLines = 1`, `overflow = TextOverflow.Ellipsis`,
    `Modifier.weight(1f, fill = false)`. Click target, role and padding unchanged.
- Unit tests: none (layout arithmetic only; checked on the simulator).
- Compile: `./gradlew :cmp:feature:home:ui:compileAndroidMain`

### Step 4: Title details top bar background on scroll  (AC-6, AC-7, EC-5, EC-6, EC-7) ✅
- Files: `DetailsTopBar.kt`, `TitleDetailsScreen.kt`
- What:
  - `DetailsTopBar(onBack, isFavorite, onFavoriteClick, isScrolled: Boolean, modifier)`: outer `Box` gets
    `.background(animateColorAsState(if (isScrolled) colorScheme.surface else Color.Transparent).value)` before
    `windowInsetsPadding`. Pill, heart, semantics unchanged.
  - `TitleDetailsContent`: `val isScrolled by remember(scroll.screen) { derivedStateOf { scroll.screen.value > 0 } }`
    and pass it to `DetailsTopBar`.
- Unit tests: none (UI-only; checked on the simulator).
- Compile: `./gradlew :cmp:feature:title-details:ui:compileAndroidMain`

### Step 5: Docs ✅
- Files: `docs/features/home/design/home.md` (poster placeholder name: word-safe), `docs/features/favorites/design/home-favorites.md`
  (header wrap rule; name word-safe), `docs/features/title-details/design/title-details.md` (top bar background on
  scroll; name, cast word-safe), `docs/features/design-system/parity.md` (large text: behavior shared by all three
  apps; iOS nav bar stays system), and this plan's status.
- Compile: none (docs).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 | 3 | Manual: iOS simulator at xxxLarge, en + pt-BR, Home with favorites |
| AC-2 | 3 | Manual: same, and default size (one line) |
| AC-3 | 1, 2 | `WordSafeLineCountTest`; manual: Home, My favorites, Title details at xxxLarge |
| AC-4 | 1, 2 | `wordSplitAcrossLinesStopsAtItsFirstLine`, `limitIsNeverExceeded`; manual: cast names |
| AC-5 | 2 | `fewerLinesThanLimitKeepsAll`; manual: long title name on Title details |
| AC-6 | 4 | Manual: scroll Title details down and back |
| AC-7 | 4 | Manual: back and heart work in both states; no new content in the bar |
| AC-8 | 1–4 | Manual: default size screenshots match today except AC-6 |
| AC-9 | — | Sibling PRs (native-android, native-ios) follow this behavior |
| EC-1 | 3 | Manual: badge stays with the title (same child) |
| EC-2 | 3 | Manual: pt-BR "Ver todos" |
| EC-3 | 1 | `longWordAfterAShortOneKeepsTheFirstLine`, `wordSplitAcrossLinesStopsAtItsFirstLine` |
| EC-4 | 1, 2 | Covered by the same rule (`PosterTile` placeholder uses `NameText`; loading cards show no name text) |
| EC-5 | 4 | Manual: pull to refresh at the top keeps the bar transparent |
| EC-6 | 4 | Manual: loading/error show a transparent bar (no scroll) |
| EC-7 | 4 | `rememberScrollState` is saveable; manual: back from a recommendation restores the state |

## Out of scope
- `native-android` and `native-ios` (sibling sessions, own PRs).
- Other text (synopsis, headings, chips, buttons, date line), onboarding, token values.

## Risks
- `NameText` draws text itself: selection and text-to-speech of a `Text` node aren't needed for names, and the
  semantics text keeps screen readers working — check with VoiceOver on one card.
- Skia (iOS) and Android may place line ends differently; the rule only reads line ends, so both are covered —
  checked on the iOS simulator; Android checked only by host tests (no emulator run in this task).

## Deviations
- Step 1: `wordSafeLineCount` also keeps at least one line (`emptyTextKeepsOneLine`), so empty text never asks
  the measurer for zero lines.
- Step 1: the compile check ran as `:cmp:designsystem:testAndroidHostTest` (there is no separate host-test compile task name to call).
