# Plan: Home on iOS (native)

Requirement: [07 - Home on iOS (native)](../subtasks/07-native-ios-home.md)
Status: Done

## Summary
Replace the placeholder Home of `native-ios/` with the designed Home screen in SwiftUI, mirroring the cmp Home
(plans 04 and 05) in behaviour. A `@MainActor` `HomeViewModel` (`ObservableObject`, MVI as in onboarding) depends on
a small Swift protocol `TitleLists`; the adapter `SharedCoreTitleLists` implements it over
`SharedCore.shared.titleListsRepository` (SKIE flow and `async` refresh). Posters load with Nuke (disk cache), the
tab and row scroll are restored with `@SceneStorage`, and a `NavigationStack` opens a placeholder details screen. No
change to `core/` or `service/`.

## Decisions
- Design Home: used (the subtask's only design).
- **Images: Nuke 12 (`NukeUI.LazyImage`)** (from user). Added as a remote Swift package
  (`https://github.com/kean/Nuke`, up to next major from the latest 12.x, checked in forge) to the app target, with
  the product `NukeUI`. Its disk cache keeps posters that were loaded before visible offline (AC-7). The pinned
  version goes in the committed `Package.resolved`. Rejected: `AsyncImage` + a bigger `URLCache` (no dependency,
  but flashes on reuse and depends on HTTP cache headers).
- **EC-1 has no guard, as in cmp** (from user). Every tap sends `openTitle`; a quick double tap may push details
  twice. EC-1 is recorded as not handled.
- **Restoring after the app is killed in the background (EC-6)** (from user): the selected tab and each row's scroll
  position.
  - `HomeView` keeps `@SceneStorage("home.selectedCategory")` (the enum's `name`) and, per row,
    `@SceneStorage("home.featured.position")` / `@SceneStorage("home.popular.position")` holding
    `"<category name>|<title id>"` of the first visible poster.
  - On first appearance the view sends `categorySelected(saved)` **before** `languageChanged`, so only the restored
    category is observed and refreshed. It writes the category back on every change of `state.selectedCategory`.
  - A row records its first visible poster from the posters' `onAppear`/`onDisappear` (lowest index among the
    visible ones) and, when it first shows titles of the saved category, scrolls to the saved poster with
    `ScrollViewReader.scrollTo(id, anchor: .leading)`. Restoration is per poster, not per point.
  - Each row's `ScrollView` has `.id(category)`, so a switched-to tab starts at the beginning (as cmp).
  - Rotation and Back from details keep everything without extra code: the views stay alive.
- **Avatar icon: SF Symbol `person.fill`** (from user), white at 50% opacity on `#36076B`, 20pt in the 40pt circle.
  No asset needed.
- **Swift-side types**, so the ViewModel and its tests don't hold Kotlin objects:
  - `struct TitleItem: Hashable, Identifiable { let id: String; let name: String; let posterURL: URL? }`, with
    `id = "<type>-<tmdbId>"` (e.g. `movie-550`), mapped from Kotlin `Title` in the adapter.
  - `struct TitleListKey: Hashable { let category: TitleCategory; let listType: TitleListType; let language: Language }`.
  - `TitleCategory`, `TitleListType`, `Language` are SKIE's Swift enums from `TmdbCore` (`Hashable`,
    `CaseIterable`), used as is; `TitleCategory.allCases` is already in tab order.
- **`TitleLists` protocol** (`@MainActor`), like `OnboardingSettings`:
  ```swift
  func observe(_ key: TitleListKey) -> AsyncStream<[TitleItem]?>   // nil = never loaded
  func refresh(_ key: TitleListKey, force: Bool) async -> Bool     // true = success
  ```
  The adapter turns the SKIE flow into an `AsyncStream` (a `Task` iterating the flow, cancelled in
  `onTermination`) and `AppResultSuccess` / any throw or `AppResultFailure` into `true` / `false`. One error state
  covers every `AppError`, as in cmp.
- **ViewModel contract** (`Home/HomeContract.swift`), the cmp shape in Swift:
  ```swift
  enum RowState: Equatable { case loading, error, titles([TitleItem]) }
  struct HomeUiState: Equatable {
      var selectedCategory: TitleCategory = .movies
      var featured: RowState = .loading
      var popular: RowState = .loading
      var isRefreshing = false
  }
  enum HomeIntent { case languageChanged(Language), categorySelected(TitleCategory), titleTapped(TitleItem),
                    retry(TitleListType) }
  enum HomeEffect: Equatable { case openTitle(TitleItem) }
  ```
  - Row rules as in cmp 05: `titles` whenever the observed value is non-nil (cache wins, AC-7); `error` when nil and
    the last refresh of that key failed; `loading` otherwise.
  - Failed refreshes are a `Set<TitleListKey>`, read only for the current selection (EC-5, EC-7). A refresh removes
    its key when it starts and adds it on failure.
  - Nothing is observed or refreshed until the language is known. Each change of category or language cancels the
    previous observation tasks (like `flatMapLatest`), resets both rows to their new observed values and starts
    `refresh(force: false)` for both lists; refresh tasks are not cancelled, so a late result is cached but only
    marks its own key.
- **Pull-to-refresh uses SwiftUI `.refreshable`** on the vertical `ScrollView` (iOS 16). It awaits
  `HomeViewModel.refresh() async`, a separate entry point because `.refreshable` needs to await; the indicator ends
  when it returns. It forces both lists of the current selection in parallel and sets `isRefreshing` until both end.
  A pull while `isRefreshing` awaits the running refresh instead of starting another (EC-9). Retry forces one list
  (AC-8). The system refresh control's colour is left as is (it is light on the dark theme).
- **Language** (AC-10, EC-7, EC-8): `HomeView` computes
  `Language.companion.fromLocaleTag(tag: Bundle.main.preferredLocalizations.first ?? "en")`, the same localization
  the String Catalog uses for the copy, so copy and data always match. iOS relaunches apps when the system or
  per-app language changes, so EC-7 is covered by the next launch; the ViewModel still handles a language change
  (tested).
- **Effects**: an `AsyncStream<HomeEffect>` as in `OnboardingViewModel`; `HomeView` collects it in `.task` and calls
  `onTitleTap(title)`.
- **Navigation**: `AppRootView`'s `.home` case becomes `NavigationStack(path: $path)` with
  `@State var path: [TitleItem]`, `HomeView` as root and
  `.navigationDestination(for: TitleItem.self) { TitleDetailsPlaceholderView(title: $0) }`. Home hides the navigation
  bar (`.toolbar(.hidden, for: .navigationBar)`); the placeholder shows the system back button and the title's name
  centred, on `Theme.night`. Back keeps Home's view, so tab and scroll stay (AC-3).
- **Layout and tokens** (design/home.md):
  - New `Theme` tokens: `accent = #FF1F8A` (selected tab, retry pill, spinner), `surfaceVariant = #36076B` (search
    field, avatar, poster tile), `homeGradientTop = #303243`. `primary` (`#8000FF`) is the header glow,
    `night` (`#15151D`) the gradient end.
  - System font with `@ScaledMetric` sizes as in onboarding (Inter is not bundled): title 26 bold, search hint 15
    medium, tabs 16 semibold / regular, heading 18 bold, placeholder name 14 semibold, row message 15 regular.
  - Screen background: fixed gradient `#303243` 0 → `#15151D` 0.65394, edge to edge. Header glow: 522pt gradient
    `#8000FF` → clear at the top of the scroll content, drawn behind the status bar and scrolling with the content.
  - Content top: safe area + 20pt (the design's 64pt less the 44pt status bar, as cmp). Start padding 30pt; tabs and
    rows bleed to the right edge (leading padding only). Bottom padding 79pt.
  - Tabs get a 44pt minimum touch height (iOS minimum), so the 34pt row grows by 5pt each side: search → tabs
    31 − 5 = 26pt, tabs → featured 37 − 5 = 32pt; the visible pills don't move.
  - Search field: `minHeight` 46, `Capsule` fill `surfaceVariant`, hint 21pt inside, icon 16pt from the end,
    mirrored with `scaleEffect(x: -1)`.
  - Posters 124×188, radius 8, 12pt gap, image `.fill` cropped; placeholder: name centred, 8pt padding, 4 lines,
    tail truncation (AC-11, EC-2). The tile is filled with `surfaceVariant`, so a loading or failed image shows a
    plain tile.
  - Spinners: when both rows are `loading`, one `ProgressView` tinted `accent` replaces both rows and the heading
    (AC-6); a single loading row shows a small spinner in its 188pt area at the 30pt padding (as cmp).
  - Empty / error rows: `minHeight` 188, message at 50% white, retry pill (`accent`, capsule, 14/6 padding, 44pt
    minimum height) 12pt below.
- **Accessibility** (AC-4, AC-5, AC-11, EC-10):
  - Search field and avatar: `.accessibilityHidden(true)`; no `Button`, gesture or `TextField`, so they can't be
    focused or typed into.
  - Posters: `Button` with `.accessibilityLabel(title.name)` (button trait from `Button`); the placeholder text is
    inside the label, so the name is read once.
  - Tabs: `Button`s with `.isSelected` on the selected one. Title and "Most popular" heading: `.isHeader`.
  - Large fonts: title and heading wrap; tabs scroll; search field, tabs and rows use minimum heights, posters stay
    124×188.
- **Copy** in `Localizable.xcstrings` (en / pt-BR), keys `home.title`, `home.searchHint`, `home.popularHeading`,
  `home.category.movies|series|anime|novelas|documentaries`, `home.rowEmpty`, `home.rowError`, `home.retry`, with the
  cmp texts (English tabs: Movies, Series, Anime, Telenovelas, Documentaries).
- **Asset**: `docs/features/home/design/assets/iconly_light_outline_search.svg` becomes the image set
  `SearchIcon.imageset` (SVG, "Preserve Vector Data"), as `TmdbLogo` is. Its 50% white is baked into the SVG.
- **Folders**: `TmdbNative/Home/` (contract, ViewModel, view, `TitleLists.swift` with protocol, types and adapter)
  and `TmdbNative/Home/Component/` (header, search field, tabs, poster row, row message), as `Onboarding/`.
  `TitleDetailsPlaceholderView.swift` sits at the app root, like `PlaceholderHomeView.swift`, which is deleted.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Title lists (observe, refresh, 6 h TTL, per-language cache) | `TitleListsRepository` via `SharedCore.shared.titleListsRepository` | `core/ios`, `service/title/domain` |
| Device language → TMDB language | `Language.companion.fromLocaleTag(tag:)` | `core/model` |
| MVI ViewModel shape, effects stream, `@StateObject` wiring | `OnboardingViewModel`, `OnboardingView` | `native-ios/TmdbNative/Onboarding/` |
| Protocol + adapter over `SharedCore`, Swift stub in tests | `OnboardingSettings`, `SharedCoreOnboardingSettings`, `OnboardingSettingsStub` | `native-ios/TmdbNative/Onboarding/`, `TmdbNativeTests/` |
| Adapter tests against the real core | `SharedCoreApiTests` | `native-ios/TmdbNativeTests/` |
| Colours | `Theme.night`, `Theme.primary` | `native-ios/TmdbNative/Theme.swift` |
| Strings | String Catalog `Localizable.xcstrings` | `native-ios/TmdbNative/` |
| SVG in the asset catalog | `TmdbLogo.imageset` | `native-ios/TmdbNative/Assets.xcassets/` |
| Behaviour reference | `HomeViewModel`, `HomeContract`, `HomeScreen`, components | `cmp/feature/home/ui` |
| Build, install, launch on a simulator | `scripts/run-native-ios.sh` | `scripts/` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `native-ios` project | `TmdbNative.xcodeproj/project.pbxproj` (modify), `.../project.xcworkspace/xcshareddata/swiftpm/Package.resolved` (new) | Nuke package and `NukeUI` product |
| `native-ios/TmdbNative` | `Theme.swift`, `Localizable.xcstrings`, `AppRootView.swift`, `TmdbNativeApp.swift` (modify); `TitleDetailsPlaceholderView.swift`, `Assets.xcassets/SearchIcon.imageset/*` (new); `PlaceholderHomeView.swift` (delete) | Tokens, copy, navigation, wiring, asset |
| `native-ios/TmdbNative/Home` | `TitleLists.swift`, `HomeContract.swift`, `HomeViewModel.swift`, `HomeView.swift`, `HomeLanguage.swift` (new) | Home feature |
| `native-ios/TmdbNative/Home/Component` | `HomeHeader.swift`, `SearchField.swift`, `CategoryTabs.swift`, `PosterRow.swift`, `RowMessage.swift` (new) | Home components |
| `native-ios/TmdbNativeTests` | `TitleListsStub.swift`, `HomeViewModelTests.swift`, `HomeViewModelRefreshTests.swift`, `HomeLanguageTests.swift` (new); `SharedCoreApiTests.swift` (modify) | Tests |
| docs | `native-ios/README.md` (modify: Nuke is fetched by SwiftPM on first build), this plan, subtask status | Docs |

The shared scheme `TmdbNative.xcodeproj/xcshareddata/xcschemes/TmdbNative.xcscheme` must stay in every commit.

Build command used below (from `native-ios/`, after `./gradlew publishSharedCore` at the worktree root):
`xcodebuild -project TmdbNative.xcodeproj -scheme TmdbNative -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`

## Steps
### Step 1: Nuke package, tokens, copy and search icon  (AC-1, AC-10) ✅
- Files: `project.pbxproj`, `Package.resolved` (new), `Theme.swift`, `Localizable.xcstrings`,
  `Assets.xcassets/SearchIcon.imageset/{Contents.json, iconly_light_outline_search.svg}` (new).
- What:
  - `XCRemoteSwiftPackageReference` for `https://github.com/kean/Nuke` (`upToNextMajorVersion`, latest 12.x),
    `XCSwiftPackageProductDependency` `NukeUI` in the app target's `packageProductDependencies` and frameworks phase.
    Resolve with `xcodebuild -resolvePackageDependencies` so `Package.resolved` is written.
  - `Theme.accent`, `Theme.surfaceVariant`, `Theme.homeGradientTop`.
  - The 11 `home.*` keys in en and pt-BR.
  - `SearchIcon` image set with `preserves-vector-representation`.
- Unit tests: none (resources).
- Compile: build command.

### Step 2: `TitleLists` protocol, types and adapter  (AC-7, AC-9, AC-10) ✅
- Files: `Home/TitleLists.swift` (new), `Home/HomeLanguage.swift` (new).
- What:
  - `TitleItem`, `TitleListKey`, `@MainActor protocol TitleLists` as in Decisions.
  - `@MainActor struct SharedCoreTitleLists: TitleLists` over `SharedCore.shared.titleListsRepository`:
    `observe` maps each emission (`nil` stays `nil`; `Title` → `TitleItem`, `id = "\(type.name.lowercased())-\(tmdbId)"`,
    `posterURL = posterUrl.flatMap(URL.init(string:))`); `refresh` returns `result is AppResultSuccess<…>` and
    `false` on a throw.
  - `enum HomeLanguage { static func from(preferredLocalizations: [String]) -> Language }` using
    `Language.companion.fromLocaleTag(tag: preferredLocalizations.first ?? "en")`.
- Unit tests: `HomeLanguageTests` — `testPortugueseBrazilMapsToPtBr`, `testEnglishMapsToEnUs`,
  `testOtherLanguageMapsToEnUs`, `testNoLocalizationMapsToEnUs`. `SharedCoreApiTests` —
  `testSharedCoreProvidesTitleListsRepository`, `testTitleListsAdapterEmitsWithoutWaitingForNetwork` (first value
  arrives, `nil` or a list), `testTitleListsAdapterRefreshCompletes` (returns `true` or `false` without throwing; the
  result depends on network and token).
- Compile: build command.

### Step 3: Contract and ViewModel  (AC-2, AC-3, AC-6–AC-10, EC-3–EC-7, EC-9) ✅
- Files: `Home/HomeContract.swift`, `Home/HomeViewModel.swift` (new).
- What:
  - Contract types as in Decisions.
  - `@MainActor final class HomeViewModel: ObservableObject` with `@Published private(set) var state`,
    `let effects: AsyncStream<HomeEffect>`, `init(titleLists: any TitleLists)`, `func onIntent(_:)`,
    `func refresh() async`.
  - Private: `category`, `language: Language?`, `observed: [TitleListType: [TitleItem]?]`, `failed: Set<TitleListKey>`,
    `observeTasks: [Task<Void, Never>]`, `pullRefresh: Task<Void, Never>?`.
  - Tasks capture `self` weakly; all stored tasks are cancelled when the ViewModel goes away (a small
    `final class TaskBag: @unchecked Sendable` whose `deinit` cancels them, since a `@MainActor` `deinit` can't
    touch isolated state in Swift 6).
- Pseudo-code:
  ```swift
  func select(category: TitleCategory?, language: Language?) {
      // update fields; if unchanged or language == nil → only state.selectedCategory changes
      cancel observeTasks; observed = [:]; recompute()
      for type in TitleListType.allCases {
          let key = TitleListKey(category, type, language)
          observeTasks += Task { for await v in titleLists.observe(key) { observed[type] = v; recompute() } }
          Task { await refreshList(key, force: false) }
      }
  }
  func refreshList(_ key, force) async {
      failed.remove(key); recompute()
      if !(await titleLists.refresh(key, force: force)) { failed.insert(key) }
      recompute()
  }
  func refresh() async {               // pull-to-refresh
      guard let language else { return }
      if let running = pullRefresh { await running.value; return }
      state.isRefreshing = true
      let task = Task { await withTaskGroup { both lists of the current selection, force: true } }
      pullRefresh = task; await task.value; pullRefresh = nil; state.isRefreshing = false
  }
  func recompute() {
      row(type) = observed[type] ?? nil → .titles(list); else failed.contains(key) ? .error : .loading
  }
  ```
  `recompute` reads `observed` only for the current selection's tasks (cancelled ones stop writing because the
  tasks check `Task.isCancelled` / the stream ends).
- Unit tests (with `TitleListsStub`, `@MainActor`, waiting on `viewModel.$state.values` for the expected state with
  a timeout helper):
  - `HomeViewModelTests`: `testStartsLoadingWithMoviesSelected`, `testNothingIsObservedBeforeLanguage`,
    `testListsAreLoadedInTheGivenLanguage`, `testCachedTitlesAreShownPerRow`,
    `testSelectingCategoryShowsItsListsAndRefreshesWithoutForce`, `testFailedLoadWithoutCacheShowsError`,
    `testFailedRefreshWithCacheKeepsTitles`, `testRetryForcesOnlyThatList`, `testEmptyListShowsEmptyRow`,
    `testOneRowFailsWhileOtherShowsTitles`, `testFailureForPreviousTabDoesNotMarkNewTab`,
    `testLanguageChangeShowsNewLanguageOnly`, `testCategoryRestoredBeforeLanguageLoadsOnlyThatCategory`,
    `testTitleTappedOpensThatTitle`.
  - `HomeViewModelRefreshTests`: `testPullToRefreshForcesBothListsAndEndsIndicator`,
    `testPullWhileRefreshingIsIgnored` (stub holds refreshes until released),
    `testPullBeforeLanguageDoesNothing`.
  - `TitleListsStub` (`@MainActor final class`, test target): `send(_ titles: [TitleItem]?, for key:)` (buffers the
    latest value per key, replayed to new observers, `nil` by default), `refreshResults: [TitleListKey: Bool]`
    (default `true`), `refreshRequests: [(key: TitleListKey, force: Bool)]`, `observedKeys: [TitleListKey]`,
    `holdRefreshes` + `releaseRefreshes()`. It is a Swift test double for the Swift protocol, as
    `OnboardingSettingsStub`.
- Compile: build command.

### Step 4: Components  (AC-1, AC-4, AC-5, AC-8, AC-11, EC-2, EC-3, EC-10) ✅
- Files: `Home/Component/HomeHeader.swift`, `SearchField.swift`, `CategoryTabs.swift`, `PosterRow.swift`,
  `RowMessage.swift` (new).
- What:
  - `HomeHeader`: title (`home.title`, `.isHeader`, wraps) + 8pt + avatar (40pt `Circle` `surfaceVariant`,
    `person.fill` 20pt white 50%, 7pt top offset, hidden from accessibility).
  - `SearchField`: hint + mirrored `SearchIcon`, capsule, `minHeight: 46`, hidden from accessibility, no hit testing.
  - `CategoryTabs(selected:onSelect:)`: horizontal `ScrollView` with `HStack(spacing: 29)`, leading 33pt; each tab a
    `Button` (plain style), label `Text` 16pt, selected = semibold on `accent` capsule (14/6 padding), unselected =
    regular at 40% white; `minHeight: 44`; `.isSelected` trait on the selected tab.
  - `PosterRow(titles:category:savedPosition: Binding<String>, onTap:)`: horizontal `ScrollView` + `ScrollViewReader`
    + `LazyHStack(spacing: 12)`, leading 30pt, height 188; `.id(category)`; poster = `Button` with a tile
    (`surfaceVariant` fill, `LazyImage(url:)` `.aspectRatio(contentMode: .fill)` or placeholder name), 124×188,
    `RoundedRectangle(cornerRadius: 8)` clip, `.accessibilityLabel(name)`; first-visible tracking and restore as in
    Decisions.
  - `RowMessage(message:onRetry:)`: message + optional retry pill, `minHeight: 188`, leading 30pt.
  - `#Preview`s for English, Portuguese and the largest text size on `HomeContent` (step 5).
- Unit tests: none (UI; manual checks below).
- Compile: build command.

### Step 5: Home screen, details placeholder and navigation  (AC-1–AC-3, AC-6, AC-9, AC-10, EC-6) ✅
- Files: `Home/HomeView.swift` (new), `TitleDetailsPlaceholderView.swift` (new), `AppRootView.swift`,
  `TmdbNativeApp.swift` (modify), `PlaceholderHomeView.swift` (delete).
- What:
  - `HomeView(titleLists:onTitleTap:)`: `@StateObject` ViewModel, `@SceneStorage` values, `.task` sends the restored
    category then `languageChanged(HomeLanguage.from(preferredLocalizations: Bundle.main.preferredLocalizations))`,
    then collects effects; `.onChange(of: viewModel.state.selectedCategory)` saves it. Draws
    `HomeContent(state:onIntent:onRefresh:positions)`.
  - `HomeContent`: `ZStack` of the fixed background gradient and a vertical `ScrollView` with header glow, header,
    search, tabs and rows (layout as in Decisions), `.refreshable { await onRefresh() }`.
  - Rows: both `loading` → one spinner; otherwise featured row, "Most popular" heading, popular row; each row by
    `RowState` (`loading` small spinner, `error` `RowMessage` with retry, `titles([])` `RowMessage` empty,
    `titles` `PosterRow`).
  - `TitleDetailsPlaceholderView(title:)`: name centred, white, on `Theme.night`, system back button.
  - `AppRootView(settings:titleLists:)`: `.home` → `NavigationStack(path:)` as in Decisions; `TmdbNativeApp` passes
    `SharedCoreTitleLists()`.
- Unit tests: existing `AppViewModelTests` stay green (AppViewModel unchanged).
- Compile: build command, then the test command:
  `xcodebuild -project TmdbNative.xcodeproj -scheme TmdbNative -destination 'platform=iOS Simulator,name=iPhone 17' test`.

### Step 6: Docs ✅
- Files: `native-ios/README.md` (Nuke is a SwiftPM dependency, resolved on the first build; needs network once),
  `CLAUDE.md` (current state: native-ios has Home; reuse table: Nuke `LazyImage` for posters in `native-ios`),
  subtask `07` status.
- Compile: none (docs).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 Home layout | 1, 4, 5 | Manual: How to test 1 (compare with design/home.png) |
| AC-2 Switching category | 3, 4, 5 | `testSelectingCategoryShowsItsListsAndRefreshesWithoutForce`, `testCachedTitlesAreShownPerRow`; manual 2 |
| AC-3 Opening a title, back keeps tab and scroll | 3, 5 | `testTitleTappedOpensThatTitle`; manual 2 |
| AC-4 Search field inert | 4 | Manual 3 (tap, VoiceOver skips it) |
| AC-5 Avatar inert | 4 | Manual 3 |
| AC-6 First load spinner | 3, 5 | `testStartsLoadingWithMoviesSelected`; manual 4 (fresh install) |
| AC-7 Offline with cache | 2, 3 | `testFailedRefreshWithCacheKeepsTitles`; manual 4 (airplane mode via Network Link Conditioner / Mac offline) |
| AC-8 Error without cache, retry | 3, 4, 5 | `testFailedLoadWithoutCacheShowsError`, `testRetryForcesOnlyThatList`; manual 4 |
| AC-9 Freshness, pull-to-refresh | 3, 5 | `testSelectingCategoryShowsItsListsAndRefreshesWithoutForce`, `testPullToRefreshForcesBothListsAndEndsIndicator`; manual 4 |
| AC-10 Language | 1, 2, 5 | `testListsAreLoadedInTheGivenLanguage`, `HomeLanguageTests`; manual 5 |
| AC-11 Poster without image, announced as button | 4 | Manual 4 (VoiceOver / Accessibility Inspector) |
| EC-1 Open once on repeated taps | — (not handled, from user, as cmp) | — |
| EC-2 Long name truncated | 4 | Manual (preview with a long name) |
| EC-3 Empty row | 3, 4 | `testEmptyListShowsEmptyRow` |
| EC-4 One row fails | 3 | `testOneRowFailsWhileOtherShowsTitles` |
| EC-5 Tab switch during load | 3 | `testFailureForPreviousTabDoesNotMarkNewTab` |
| EC-6 Rotation, app killed in background | 3, 4, 5 | `testCategoryRestoredBeforeLanguageLoadsOnlyThatCategory`; manual (rotate; background, stop from Xcode, relaunch) |
| EC-7 Language change while running | 3 | `testLanguageChangeShowsNewLanguageOnly`; manual 5 |
| EC-8 Other language | 2 | `testOtherLanguageMapsToEnUs` |
| EC-9 Pull while refreshing | 3 | `testPullWhileRefreshingIsIgnored` |
| EC-10 Largest font | 4 | Manual (Dynamic Type accessibility 5, preview) |

## Out of scope
- `native-android/` → 06 (another session).
- Real details screen and Search → separate features.
- Changes to `core/` or `service/`: none needed; `observe` and `refresh` are usable from Swift as published.
- Press-feedback design (design gap): the default `Button` highlight.
- EC-1 guard (from user).
- Tinting the system refresh control.

## Risks
- **SKIE flow type for `Flow<List<Title>?>`** (expected `SkieSwiftOptionalFlow<[Title]>`) — checked against the
  published `.swiftinterface` in step 2; if the protocol method is not bridged, iterate the raw flow through SKIE's
  `SkieSwiftOptionalFlow(_:)` initializer. If neither works, stop and ask before changing the shared core.
- **Swift 6 strict concurrency with Kotlin types** (not `Sendable`) — everything that touches them is `@MainActor`
  (adapter, ViewModel), as the onboarding adapter is.
- **Nuke needs network for the first package resolution** — documented in the README; `Package.resolved` pins it.
- **Scroll restore is approximate** (per poster, through lazy `onAppear`) — acceptable per the user's choice;
  checked by hand.
- **Adapter tests share the app's real database and network** — they assert only what holds with or without
  network and cache.

## Deviations
- **Nuke 13 instead of 12.** The latest major is 13 (13.2.3, Swift 6 tools, iOS 15+); the plan assumed 12.x was the
  latest. Pinned `upToNextMajorVersion` from 13.2.3 in `Package.resolved`. The API used (`LazyImage`) is the same.
- **`effects` is a computed property** that returns a new stream per collector and finishes the previous one. The root
  view of a `NavigationStack` loses its `.task` when details are pushed, and a cancelled `AsyncStream` can't be
  iterated again, so a single stream would stop opening details after the first Back. Extra test:
  `testEffectsReachTheLatestCollector`.
- **Scroll restore anchor**: `scrollTo(id, anchor: .leading)` put the poster at the row's edge instead of the 30pt
  padding. The row reads its width with a `GeometryReader` and scrolls with
  `UnitPoint(x: 30 / (rowWidth − 124), y: 0)`; the first poster is never scrolled to.
- **Shared test helpers** `listKey(...)`, `titles(...)`, `eventually { }` and `settle()` live in
  `TitleListsStub.swift`.
- `homeScreenPadding`, `rowMinHeight` and `tabsMinHeight` are module-level constants shared by `HomeView` and the
  components (the cmp `ScreenPadding` / `RowMinHeight` equivalents).
- **Manual check on the iPhone 17 simulator (iOS 26.4, real token)**:
  - English: layout as in design/home.md, TMDB posters, Anime tab switches both rows, a poster opens the
    placeholder details, Back keeps the tab and row scroll, a second poster tap after Back opens details again.
  - App killed in the background (`simctl terminate` after Home): Anime and the featured row's position were restored
    at the 30pt padding.
  - pt-BR (`-AppleLanguages (pt-BR)`): Portuguese copy and Portuguese posters and names.
  - Largest accessibility text size: title, search hint, tabs and heading readable and wrapping; tabs scroll.
  - Not checked by hand: the pull-to-refresh indicator (the gesture ran, the refresh ended too fast to see it),
    the first-load spinner, error/retry and offline display (no way to cut the network in the simulator here),
    VoiceOver announcements, the search field and avatar with VoiceOver, rotation.
