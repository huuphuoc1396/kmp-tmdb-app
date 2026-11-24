# Plan: Fixed top bar on My favorites

Requirement: [06 - Fixed top bar on My favorites](../subtasks/06-fixed-favorites-top-bar.md)
Status: Done

## Summary
In `cmp` and `native-android`, the "Voltar" + badge row moves out of the `LazyColumn` into a fixed row above it.
The title and subtitle become the list's first item, so they scroll with the cards. `native-ios` already keeps
back + badge fixed in the system navigation bar, so it only loses the bar's separator line (app-wide). In all three
apps the empty message is centered in the space below the subtitle. Only UI files change: no ViewModel, state,
shared-core or string changes.

## Decisions
- **cmp and native-android get the same layout change, written twice.** `native-android` can't depend on `cmp/`
  (CLAUDE.md), so no shared component.
- **Fixed row = the existing first `Row` of `FavoritesHeader`**, moved as is (back control, spacer, badge). It is
  drawn on `MaterialTheme.colorScheme.surface`, with no divider or shadow (AC-14).
- **Title and subtitle become `FavoritesIntro`**, the `LazyColumn`'s first item (key `"intro"`). The `md` gap that
  used to sit between the row and the title becomes the list's top content padding. On screen it looks the same
  until the user scrolls.
- **The `LazyListState` is hoisted** to the top of `FavoritesContent` (`rememberLazyListState()`) and passed to the
  `LazyColumn`. It is then saved and restored as it is today, and EC-7 (rotation, process death) keeps working
  when the list is swapped with the empty layout.
- **Empty layout is not a list.** With no favorites, a `Column` shows `FavoritesIntro` and then a `Box` with
  `weight(1f)` and the message centered in it (user: center it now). The fixed row stays above it.
- **EC-12: the fixed row stays one line.** The "Voltar" label and the badge text use `maxLines = 1` and
  `softWrap = false`. The badge is never weighted, so it always keeps its full width, and the flexible `Spacer`
  absorbs the slack.
- **iOS keeps the system navigation bar** (swipe-back still works). The separator line is removed app-wide with a
  `UINavigationBarAppearance` whose `shadowColor` is `.clear`, set in `TmdbNativeApp.init()` for the standard,
  compact and scroll-edge appearances (user: hide the line app-wide). Title details also loses the line, which
  fits the flat design. The per-screen `.toolbarBackground(...)` colors stay as they are.
- **iOS empty layout**: when `state.favorites.isEmpty`, `FavoritesContent` shows a `VStack` (the `header`, a
  `Spacer`, the message, a `Spacer`) in place of the `ScrollView`. The `ScrollView`'s `onAppear` already calls
  `restore(proxy)`, so the scroll restore still runs once favorites load.
- **Tests: manual checks only** (user). No state or logic changes. The existing ViewModel tests must still pass.
- **Design My favorites**: used. Its "header fixed or scrolling" gap is now answered by task.md AC-14, so the
  plan doesn't read the design file's old "Scrolling" note.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Spacing, sizes (touch target, icon) | `TmdbSpacing`, `TmdbSize` | `cmp/designsystem`; `native-android` app design system; `native-ios/TmdbNative/DesignSystem` |
| Back arrow, copy, plural badge | `ic_back_arrow`, `favorites_back`, `favorites_count`, `favorites_title`, `favorites_subtitle`, `favorites_empty` | each app's resources (unchanged) |
| Badge on iOS | `CountBadge` (trailing toolbar item) | `native-ios/TmdbNative/Favorites/FavoritesView.swift` |
| Scroll restore on iOS | `@SceneStorage("favorites.scroll")` + `restore(_:)` | same file |
| Remove popup | `RemoveFavoriteDialog` / `.removeFavoriteDialog` | unchanged |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `cmp:feature:favorites:ui` | `FavoritesScreen.kt` (modify) | Fixed row above the list; intro scrolls; centered empty message |
| `native-android` `:app` | `favorites/FavoritesScreen.kt` (modify) | Same as cmp |
| `native-ios` | `TmdbNativeApp.swift`, `Favorites/FavoritesView.swift` (modify) | No navigation bar separator line; centered empty message |

## Steps
### Step 1: cmp: fixed top bar and centered empty message  (AC-14, AC-10, EC-12, EC-7) ✅
- Files: `cmp/feature/favorites/ui/src/commonMain/kotlin/com/banhmilabs/tmdb/cmp/feature/favorites/ui/FavoritesScreen.kt` (modify)
- What:
  - `FavoritesContent` becomes:
    ```
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val listState = rememberLazyListState()
    Column(modifier.fillMaxSize().background(surface)) {
      FavoritesTopBar(count, onBack,
        Modifier.fillMaxWidth().background(surface)
          .padding(start = lg, end = lg, top = insets.top + xs))
      if (state.favorites.isEmpty()) {
        Column(Modifier.weight(1f).fillMaxWidth()
            .padding(start = lg, end = lg, top = md, bottom = insets.bottom + xl)) {
          FavoritesIntro()
          Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Center) { empty Text, centered }
        }
      } else {
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(),
          contentPadding = PaddingValues(start = lg, end = lg, top = md, bottom = insets.bottom + xl),
          verticalArrangement = spacedBy(sm)) {
          item(key = "intro") { FavoritesIntro() }
          items(favorites, key = …) { FavoriteCard(…) }
        }
      }
    }
    pendingRemoval dialog unchanged
    ```
  - `private fun FavoritesTopBar(count: Int, onBack: () -> Unit, modifier: Modifier)`: the old first `Row`.
    "Voltar" and badge `Text`s get `maxLines = 1, softWrap = false`.
  - `private fun FavoritesIntro()`: title (heading semantics) + `xs` + subtitle + `sm` bottom spacer, as today.
  - The empty `Text` drops `padding(top = xxxl)` and keeps `textAlign = Center`.
  - Delete `FavoritesHeader`.
- Unit tests: none new; `FavoritesViewModelTest`, `FavoritesViewModelRemoveTest` must still pass.
- Compile: `./gradlew :cmp:androidApp:assembleDebug`

### Step 2: native-android: same layout  (AC-14, AC-10, EC-12, EC-7) ✅
- Files: `native-android/app/src/main/kotlin/com/banhmilabs/tmdb/nativeapp/favorites/FavoritesScreen.kt` (modify)
- What: the same changes as Step 1, with `R.string` / `R.plurals` / `R.drawable` and the app's own
  `TmdbSpacing` / `TmdbSize`.
- Unit tests: none new; the app's `FavoritesViewModelTest`, `FavoritesViewModelRemoveTest` must still pass.
- Compile: `cd native-android && ./gradlew :app:compileDebugKotlin` (needs the shared core already published to
  `build/maven-repo`; run `./gradlew publishSharedCore` at the root first if it's missing)

### Step 3: native-ios: no bar separator, centered empty message  (AC-14, AC-10, EC-12, EC-7) ✅
- Files: `native-ios/TmdbNative/TmdbNativeApp.swift`, `native-ios/TmdbNative/Favorites/FavoritesView.swift` (modify)
- What:
  - `TmdbNativeApp.init()`, after `SharedCore.shared.start()`:
    ```
    let appearance = UINavigationBarAppearance()
    appearance.configureWithDefaultBackground()
    appearance.shadowColor = .clear
    UINavigationBar.appearance().standardAppearance = appearance
    UINavigationBar.appearance().compactAppearance = appearance
    UINavigationBar.appearance().scrollEdgeAppearance = appearance
    ```
    with `import UIKit`. Done in a private static func `hideNavigationBarSeparator()`, called from `init()`.
  - `FavoritesContent.body`: inside the `ZStack`, `if state.favorites.isEmpty { emptyContent } else
    { ScrollViewReader { ScrollView { … } } }`. The `ScrollView` loses the empty `Text` and the
    `.onChange(of: state.favorites.isEmpty)`; its `.onAppear { restore(proxy) }` stays.
  - `private var emptyContent: some View`: `VStack(alignment: .leading, spacing: 0) { header; Spacer(minLength:
    TmdbSpacing.xl); Text("favorites.empty") …centered, frame(maxWidth: .infinity); Spacer(minLength:
    TmdbSpacing.xl) }` with the same horizontal/top/bottom padding as the list.
- Unit tests: none new; `FavoritesViewModelTests` must still pass.
- Compile: `cd native-ios && xcodebuild -project TmdbNative.xcodeproj -scheme TmdbNative -destination
  'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`

### After the last step
- `./gradlew :cmp:feature:favorites:ui:testAndroidHostTest :cmp:feature:favorites:ui:detekt`
- `cd native-android && ./gradlew :app:detekt :app:testDebugUnitTest`
- `cd native-ios && xcodebuild -project TmdbNative.xcodeproj -scheme TmdbNative -destination
  'platform=iOS Simulator,name=iPhone 17' test`
- Manual: the subtask's "How to test" steps on cmp (Android), native-android and native-ios.
  cmp iOS gets the same Compose code, so check it if Xcode time allows, and say so if skipped.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-14 fixed row, intro scrolls, no divider, all apps | 1, 2, 3 | Manual: How to test 1–3 per app |
| AC-10 empty (re-check; message centered) | 1, 2, 3 | Manual: How to test 6; existing ViewModel empty-state tests |
| EC-12 one-line row at largest font / long badge | 1, 2 (iOS: system bar) | Manual: How to test 5 |
| EC-7 scroll position restored | 1, 2 (hoisted `LazyListState`), 3 (unchanged restore) | Manual: How to test 4 |

## Out of scope
- Home "Favoritos" section and Title details layout (apart from iOS Title details losing its bar line).
- Updating `design/my-favorites.md` (its "Scrolling" note and the header gap): run `mold` if the design file should
  match.
- UI test infrastructure (user: manual checks only).

## Risks
- SwiftUI's `.toolbarBackground` may rebuild the bar appearance and bring the separator back on My favorites. If
  it shows on the simulator, also apply `.toolbarBackground(.visible, for: .navigationBar)` on that screen; if it
  still shows, stop and ask (don't switch to a custom bar on your own).
- At the largest font, "Voltar" plus a 4-digit badge may get close to the screen width on small phones
  (≈ 320dp). `softWrap = false` keeps one line. If they overlap, tell the user before shrinking text.
- The cmp/native-android empty layout swaps the `LazyColumn` out. The hoisted `LazyListState` keeps EC-7, but check
  rotation right after removing the last favorite and after favorites first load.

## Deviations
- cmp and native-android: the empty layout and the list are extracted into `private` composables
  `FavoritesEmpty(bottomPadding, modifier)` and `FavoritesList(favorites, listState, bottomPadding, onIntent,
  modifier)`, so that `FavoritesContent` stays under Detekt's `LongMethod` limit (60 lines).
- native-ios: the `ScrollView`'s `.onChange(of: state.favorites.isEmpty)` is removed, not just moved. The
  `ScrollView` now appears only when there are favorites, and its `onAppear` runs the restore.
