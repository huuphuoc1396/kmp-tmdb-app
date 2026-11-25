# Plan: Large text layout (native-android)

Requirement: [Large text layout](../task.md)
Status: Done

## Summary
Three local fixes in the `native-android` `:app` module, mirroring the `cmp` files one to one. Names go through a new
`NameText` that lays the text out once, finds the first line that ends inside a word, and shows the text only up to
that line with "…" (so a word is never continued on the next line). The favorites header uses a small custom layout
that keeps "View all ›" on the title line when it fits and otherwise places it on its own line at the trailing edge.
The Title details top bar takes a `surface` background while the screen's `ScrollState` is above 0.

## Decisions
- Names: Android/Compose has no option that forbids breaking inside a word, so `NameText` measures with
  `rememberTextMeasurer` inside `BoxWithConstraints` and lowers `maxLines` to the line that ends mid-word; the
  ellipsis comes from `TextOverflow.Ellipsis` on that last line — covers AC-3, AC-4, EC-3 with stock `Text`.
- A line break after whitespace or a hyphen/dash (`-`, `‐`, `–`, `—`) counts as "between words" (e.g. "Spider-Man"
  may break after the hyphen) — Android already breaks there and it does not split a word.
- The line-limit decision is a pure function `wholeWordLineCount(text, lineEnds, maxLines)` so it is unit-tested on
  the JVM (the module has no Compose UI test setup; adding Robolectric is out of proportion).
- Favorites header: a private `Layout` with two children (title + badge row, "View all ›"): one line when
  `lead.width + trailing.width <= maxWidth`, else trailing on a second line aligned to the end. `FlowRow` was
  rejected because a wrapped item goes to the start, not the trailing edge. The fit check is a pure function
  `headerFitsOnOneLine(leadWidth, trailingWidth, maxWidth)` with a unit test. "View all" text gets `maxLines = 1`
  and `softWrap = false` is not used (it would overflow); the label always fits once on its own line.
- Top bar background: `colorScheme.surface` (a token), drawn behind the status-bar inset too so no content shows
  above the controls; state `isScrolled = scroll.screen.value > 0` read through `derivedStateOf`; color animated
  with `animateColorAsState` (fade, same idea on `cmp`). Loading/error never scroll, so the bar stays transparent
  (EC-6); pull-to-refresh at the top does not change `ScrollState.value` (EC-5); `rememberScrollState` is saved, so
  rotation and return show the right state (EC-7).
- Title details name (AC-5): `NameText` with `maxLines = Int.MAX_VALUE`, so it wraps freely and is cut only at a
  single word wider than the line.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Tokens | `TmdbSpacing`, `TmdbSize`, `MaterialTheme` | `native-android/.../designsystem` |
| Poster placeholder name | `PosterTile` | `designsystem/component/PosterTile.kt` |
| Screen scroll state | `DetailsScroll.screen` (`ScrollState`) | `titledetails/TitleDetailsScreen.kt` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `native-android:app` | `designsystem/text/NameText.kt` (new) | Whole-word names with ellipsis |
| | `home/component/FavoritesSection.kt` | Wrapping header; item name via `NameText` |
| | `favorites/component/FavoriteCard.kt`, `designsystem/component/PosterTile.kt`, `titledetails/component/CastRow.kt`, `titledetails/component/DetailsHeader.kt` | Names via `NameText` |
| | `titledetails/component/DetailsTopBar.kt`, `titledetails/TitleDetailsScreen.kt` | Background on scroll |
| | tests `designsystem/text/NameTextLineCountTest.kt`, `home/component/FavoritesHeaderFitTest.kt` (new) | Pure logic |
| docs | `docs/features/home/design/home.md`, `docs/features/favorites/design/home-favorites.md`, `docs/features/title-details/design/title-details.md`, `docs/features/design-system/parity.md` | Record behavior (native-android rows only; siblings add theirs) |

## Steps
### Step 1: `NameText`  (AC-3, AC-4, AC-5, EC-3, EC-4) ✅
- Files: `designsystem/text/NameText.kt` (new)
- What:
  - `internal fun wholeWordLineCount(text: String, lineEnds: List<Int>, maxLines: Int): Int` — first line index `i`
    (not the text's last line) whose end `e` has `text[e - 1]` and `text[e]` both non-breaking word characters
    (not whitespace, not a hyphen/dash at `e - 1`) → returns `min(i + 1, maxLines)`; none → `maxLines`.
  - `@Composable internal fun NameText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE, textAlign: TextAlign? = null)`: `BoxWithConstraints(modifier)` +
    `rememberTextMeasurer()`; `remember(text, style, maxWidth, maxLines)` measures with
    `Constraints(maxWidth = constraints.maxWidth)`, `maxLines`, collects `getLineEnd(i, visibleEnd = false)`, then
    `Text(text, color, style, textAlign, maxLines = limit, overflow = TextOverflow.Ellipsis)`.
- Unit tests: `NameTextLineCountTest` — `breakBetweenWordsKeepsMaxLines`, `breakInsideWordCutsAtThatLine`,
  `singleWordWiderThanLineIsOneLine`, `breakAfterHyphenIsBetweenWords`, `unlimitedLinesCutOnlyAtSplitWord`,
  `limitNeverExceedsMaxLines`.
- Compile: `./gradlew :app:compileDebugKotlin` (in `native-android/`)

### Step 2: Names use `NameText`  (AC-3, AC-4, AC-5, EC-3, EC-4) ✅
- Files: `home/component/FavoritesSection.kt` (item name, 2 lines), `favorites/component/FavoriteCard.kt` (2 lines),
  `designsystem/component/PosterTile.kt` (placeholder name, 4 lines, keeps padding and `clearAndSetSemantics`),
  `titledetails/component/CastRow.kt` (2 lines, centered), `titledetails/component/DetailsHeader.kt` (name with
  year, unlimited lines, keeps `heading()` semantics).
- What: replace the `Text` for the name with `NameText`, same style, color, alignment and line limit; semantics
  modifiers move onto `NameText`'s `modifier`.
- Compile: `./gradlew :app:compileDebugKotlin`

### Step 3: Favorites header wraps  (AC-1, AC-2, EC-1, EC-2) ✅
- Files: `home/component/FavoritesSection.kt`, test `home/component/FavoritesHeaderFitTest.kt` (new)
- What: `internal fun headerFitsOnOneLine(leadWidth: Int, trailingWidth: Int, maxWidth: Int): Boolean`;
  private `FavoritesHeader(count, onSeeAll)` = `Layout(contents = [Row { title, xs spacer, CountBadge }, ViewAll])`:
  measure both with `constraints.copy(minWidth = 0)`; fits → height `max`, lead at start and trailing at end, both
  centered vertically; else height `lead + trailing`, trailing at `x = width - trailing.width` on the second line.
  `ViewAll` label gets `maxLines = 1`. Paddings, tokens and the default one-line look unchanged (AC-8).
- Unit tests: `FavoritesHeaderFitTest` — `fitsWhenSumEqualsWidth`, `wrapsWhenSumExceedsWidth`.
- Compile: `./gradlew :app:compileDebugKotlin`

### Step 4: Top bar background on scroll  (AC-6, AC-7, EC-5, EC-6, EC-7) ✅
- Files: `titledetails/component/DetailsTopBar.kt`, `titledetails/TitleDetailsScreen.kt`
- What: `DetailsTopBar(isScrolled: Boolean, …)`; `val background by animateColorAsState(if (isScrolled)
  colorScheme.surface else Color.Transparent)`; `.background(background)` before `windowInsetsPadding`. In
  `TitleDetailsContent`: `val isScrolled by remember { derivedStateOf { scroll.screen.value > 0 } }`, passed to the
  bar. Controls, semantics and paddings unchanged.
- Compile: `./gradlew :app:compileDebugKotlin`

### Step 5: Docs (dropped, see Deviations)
- Files: the four design docs listed above — add the large-text behavior for `native-android` (header wrap, whole-word
  names, top bar background) next to the existing notes, without editing the other apps' rows.

### Checks
- `./gradlew :app:detekt :app:testDebugUnitTest` in `native-android/`.
- Manual on `Pixel_8` emulator (`ANDROID_SERIAL=emulator-5554`), font scale 1.0 and 2.0, en and pt-BR: Home
  favorites header, Home/favorites/details names, details top bar at top / scrolled / back to top / pull to refresh.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 | 3 | manual 200% en, pt-BR |
| AC-2 | 3 | `FavoritesHeaderFitTest`; manual 100% and 200% |
| AC-3 | 1, 2 | `NameTextLineCountTest.breakInsideWordCutsAtThatLine`; manual 200% |
| AC-4 | 1, 2 | `NameTextLineCountTest.singleWordWiderThanLineIsOneLine`, `limitNeverExceedsMaxLines`; manual cast |
| AC-5 | 1, 2 | `NameTextLineCountTest.unlimitedLinesCutOnlyAtSplitWord`; manual details |
| AC-6 | 4 | manual scroll down / up |
| AC-7 | 4 | manual back, heart, TalkBack labels unchanged (no semantics change in diff) |
| AC-8 | 2, 3, 4 | manual 100% screenshots vs main |
| AC-9 | all | native-android part; `cmp`/`native-ios` in sibling PRs |
| EC-1 | 3 | manual with ≥10 favorites if available, else `FavoritesHeaderFitTest` |
| EC-2 | 3 | manual pt-BR |
| EC-3 | 1 | `singleWordWiderThanLineIsOneLine` |
| EC-4 | 2 | `PosterTile` placeholder uses `NameText`; manual |
| EC-5 | 4 | manual pull to refresh |
| EC-6 | 4 | manual loading/error (bar transparent) |
| EC-7 | 4 | manual rotate while scrolled |

## Out of scope
- `cmp` and `native-ios` (sibling sessions and PRs).
- Other text (synopsis, headings, chips, buttons, date line), onboarding, token values.

## Risks
- `BoxWithConstraints` + measuring per name adds a subcomposition per item in lazy rows — names are short and
  measured once per width; acceptable.
- If the `cmp` session picks a different technique, the two apps diverge in code (not in behavior) — the approach is
  recorded in the shared notes so `cmp` can match it.

## Deviations
- `NameText` mirrors `cmp` (PR #55): a `Spacer` with `layout` + `drawBehind` drawing a `TextMeasurer` result, and the
  pure function `wordSafeLineCount` in `WordSafeLineCount.kt` (test `WordSafeLineCountTest`, copied from `cmp`).
  The planned `BoxWithConstraints` version crashed My favorites, whose card row asks for intrinsic sizes
  (`IntrinsicSize`), which subcomposition does not support.
- A break after a hyphen counts as a word split (as in `cmp`), not as a break between words.
- Added to `cmp`'s approach: when a name splits a word and contains a no-break space (`nameWithYear` glues the year
  with ` `), it is measured again with a normal space. "Avengers: Doomsday (2026)" at 200% shows
  "Doomsday / (2026)" instead of "Doomsda…" (AC-5: only a single word wider than the line is cut). At the default size
  nothing changes (AC-8).
- Favorites header: the fit check is inline in `FavoritesHeader` (as in `cmp`); `headerFitsOnOneLine` and its test
  were dropped. "View all" label gets `weight(1f, fill = false)` and an ellipsis (as in `cmp`).
- Step 5 dropped: the `cmp` PR updates the four design docs for all three apps; editing them here would conflict.
