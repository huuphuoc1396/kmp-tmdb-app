# Plan: Title details on iOS (native)

Requirement: [06 - Title details on iOS (native)](../subtasks/06-native-ios-title-details.md)
Status: Done

## Summary
Replace `TitleDetailsPlaceholderView` of `native-ios/` with the designed Title details screen in SwiftUI, mirroring
the cmp screen (`cmp/feature/title-details/ui`) in behaviour. A `@MainActor` `TitleDetailsViewModel`
(`ObservableObject`, MVI as Home) depends on two small Swift protocols, `TitleDetailsSource` and `Favorites`, whose
adapters wrap `SharedCore.shared.titleDetailsRepository` and `SharedCore.shared.favoritesRepository` (SKIE flows and
`async` calls) and map Kotlin types to Swift structs. The system navigation bar stays visible (back button, heart as
trailing item), the screen scrolls under it over the artwork. The navigation stack and every details screen's scroll
positions are restored with `@SceneStorage` after the app is killed. No change to `core/`, `service/`, `cmp/` or
`native-android/`.

## Decisions
- Design Title details: used (the subtask's only design).
- **Back: the system navigation bar** (from user). Details shows the bar (`.toolbar(.visible, for: .navigationBar)`,
  inline, no title, dark color scheme) with the system back button, so the edge swipe works (AC-2). The bar's
  background is transparent at the top and `#303243` once content scrolls under it
  (`.toolbarBackground(Theme.track, for: .navigationBar)`, automatic visibility), which keeps back and the heart
  readable (AC-12). No title is set, so the button reads "Back", which UIKit localizes ("Voltar" in pt-BR). Rejected:
  hidden bar with the designed "Voltar" pill plus a `UINavigationController` hack to keep the swipe; hidden bar
  without swipe. The design's back arrow asset is not used; the "Voltar" pill decision of cmp does not apply here.
- **Heart: a trailing toolbar item**, shown only when details are loaded and the stored value is known (AC-4). A
  `Toggle` with a custom `ToggleStyle` (32pt circle image in a 44pt frame), label "Favorite"/"Favorito": VoiceOver
  reads it as a switch with its on/off value on iOS 16 (`.isToggle` needs iOS 17). Off = `ic_favorite_button.svg`;
  on = a new SVG with the same circle and the heart's outer path filled `#FF1F8A` (same paths as cmp
  `ic_favorite_on.xml`). Tap → `setFavorite(id, !current)`, no pending state (as cmp).
- **Restore after the app is killed (EC-8)** (from user): stack and scroll positions.
  - `AppRootView` keeps `@SceneStorage("app.path") var savedPath: Data?`, the JSON of the path (`[DetailsRoute]`, see Step 7), written on every
    path change and decoded once when Home first shows. Rotation and back from a pushed title keep everything without
    code (the views stay alive).
  - Each details screen gets its stack index and keeps three `@SceneStorage` strings keyed by it
    (`details.<index>.scroll`, `.cast`, `.recommendations`), each prefixed with the title id (`"<id>|<value>"`) so a
    value of another title at that depth is ignored.
  - Vertical position: invisible 100pt markers (`Color.clear` with ids `0…n`) behind the scroll content; a
    `GeometryReader` preference reports the offset and the screen saves the marker index when it changes. On the first
    `Loaded` state with a saved value it calls `ScrollViewReader.scrollTo(marker, anchor: .top)` (exact to 100pt;
    iOS 16 has no API to set an offset).
  - Rows: the first visible item, restored like Home's `PosterRow` (see "Shared horizontal row").
- **Shared horizontal row.** The save/restore logic of `Home/Component/PosterRow.swift` moves into a generic
  `Component/RestoringRow.swift` (`RestoringRow<Item: Identifiable, Cell: View>` with `items`, `itemWidth`, `spacing`,
  `rowKey: String`, `savedPosition`, `cell`). `PosterRow` becomes the poster cell on top of it, keyed by
  `rowKey: String` instead of `category` (Home passes `category.name`, so the saved Home values keep their format and
  `.id(rowKey)` still resets the row on a tab switch). Details uses it for the cast and the recommendations.
- **Trailer**: `openURL` with `Trailer.url` (`https://www.youtube.com/watch?v=<key>`). iOS opens it in the YouTube
  app through its universal link, else in Safari (AC-5); no `LSApplicationQueriesSchemes`. If nothing accepts it,
  nothing happens (EC-3).
- **EC-2 has no guard, as Home and cmp** (from the background decisions): a quick double tap may push a title or open
  the trailer twice.
- **Language**: `HomeLanguage.from(preferredLocalizations:)` (the copy's language) on first appearance, as Home. iOS
  restarts the app when the device language changes, so EC-9 is met by reading it on appear; the ViewModel still
  handles `languageChanged` with a new language (observe the new key, refresh stale, failures per language).
- **Formats** (from the background decisions): date by app language (pt-BR `dd/MM/yyyy`, en `MM/dd/yyyy`), runtime
  `1h 53m` / `2h` / `45m`, name + non-breaking space + `(year)`.
- **Swift-side types** (the ViewModel and tests hold no Kotlin objects except the `Language` enum, as Home):
  - `TitleItem` moves from `Home/TitleLists.swift` to `TitleItem.swift` at the app root and becomes `Codable`; it
    gains `init(_ title: Title)` (made internal) and `var titleId: TitleId?`, parsing `"<movie|tv>-<tmdbId>"`, the
    inverse of the existing id format.
  - `TitleDetailsItem: Equatable` with `name`, `releaseDate: ReleaseDate?` (`year`, `month`, `day` ints), `runtimeMinutes:
    Int?`, `ratingPercent: Int?`, `synopsis: String?`, `artworkURL: URL?`, `genres: [String]`, `cast: [CastItem]`
    (`name`, `photoURL: URL?`), `trailerURL: URL?`, `recommendations: [TitleItem]`.
- **Typography and colors**: system font with `@ScaledMetric` sizes from the design (as Home; no Inter). New tokens in
  `Theme`: `track` (#303243, same value as `homeGradientTop`), `page` (#21222E), `ringCenter` (#15161D), `dateLine`
  (#BBBBBB), `synopsis` (#CCCCCC). The rating value stays at a fixed 15pt so it fits the 57pt ring (as cmp); the ring
  has an accessibility label.
- **Placeholders**: artwork = `#303243` tile (as cmp); cast = SF Symbol `person.fill` on `#303243`; recommendation =
  Home's poster placeholder (`#36076B` + name).
- **Genre chips** wrap with a small `Layout` (`FlowLayout`, iOS 16), 14pt between chips, 10pt between lines.
- **Error state** reuses `RowMessage` with a new `alignment` parameter (centered here) and the `home.retry` copy (same
  text, "Try again" / "Tentar novamente").

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Details and favorites from the shared core | `SharedCore.shared.titleDetailsRepository`, `.favoritesRepository` | `TmdbCore` package (`core:ios`) |
| Swift adapter pattern (SKIE flow → `AsyncStream`, `AppResultSuccess` check) | `SharedCoreTitleLists` | `native-ios/TmdbNative/Home/TitleLists.swift` |
| ViewModel shape (effects stream per collector, `TaskBag`, pull refresh shared) | `HomeViewModel` | `native-ios/TmdbNative/Home/HomeViewModel.swift` |
| App language | `HomeLanguage.from(preferredLocalizations:)` | `native-ios/TmdbNative/Home/HomeLanguage.swift` |
| Recommendation poster cell, row restore | `PosterRow` (generalized) | `native-ios/TmdbNative/Home/Component/PosterRow.swift` |
| Error message + retry pill | `RowMessage` | `native-ios/TmdbNative/Home/Component/RowMessage.swift` |
| Colors | `Theme` | `native-ios/TmdbNative/Theme.swift` |
| Remote images | Nuke `LazyImage` | `NukeUI` package |
| Test helpers | `eventually`, `settle`, `titles(_:)` | `native-ios/TmdbNativeTests/TitleListsStub.swift` |
| Behaviour reference | `TitleDetailsViewModel`, `TitleDetailsFormat.kt`, components | `cmp/feature/title-details/ui` |
| Icons | `ic_favorite_button.svg`, `ic_clock.svg`, `ic_play_circle.svg` | `docs/features/title-details/design/assets/` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `native-ios/TmdbNative` | `TitleItem.swift` (new), `Home/TitleLists.swift` (modify) | Shared title type, `Codable`, id ↔ `TitleId` |
| | `TitleDetails/TitleDetailsSource.swift`, `TitleDetails/Favorites.swift` (new) | Protocols, Swift models, shared-core adapters |
| | `TitleDetails/TitleDetailsFormat.swift` (new) | Date, runtime, name with year |
| | `TitleDetails/TitleDetailsContract.swift`, `TitleDetails/TitleDetailsViewModel.swift` (new) | State holder |
| | `TitleDetails/TitleDetailsView.swift`, `TitleDetails/Component/*.swift` (new) | Screen and components |
| | `Component/RestoringRow.swift` (new), `Home/Component/PosterRow.swift`, `Home/Component/RowMessage.swift`, `Home/HomeView.swift` (modify) | Shared row restore, centered message |
| | `AppRootView.swift`, `Theme.swift` (modify), `TitleDetailsPlaceholderView.swift` (delete) | Navigation, path restore, tokens |
| | `Assets.xcassets/{FavoriteOff,FavoriteOn,Clock,PlayCircle}.imageset` (new) | Icons |
| | `Localizable.xcstrings` (modify) | en / pt-BR copy |
| | `README.md` (modify) | Screens list |
| `native-ios/TmdbNativeTests` | `TitleDetailsStub.swift`, `FavoritesStub.swift`, `TitleDetailsViewModelTests.swift`, `TitleDetailsViewModelRefreshTests.swift`, `TitleDetailsFormatTests.swift`, `TitleItemTests.swift` (new); `SharedCoreApiTests.swift` (modify) | Unit tests and published-API checks |

New files are picked up by the target's file-system synchronized groups; the project file does not change.

## Steps
### Step 1: Shared title type  (AC-11, EC-7, EC-8) ✅
- Files: `TmdbNative/TitleItem.swift` (new), `TmdbNative/Home/TitleLists.swift` (modify)
- What: move `TitleItem` and its `init(_ title: Title)` (now internal) into `TitleItem.swift`; add `Codable`; add
  `var titleId: TitleId?` (`"movie-550"` → `TitleId(tmdbId: 550, type: .movie)`, `"tv-1"` → `.tv`, anything else →
  `nil`). The id format is built in one place, `static func id(for: TitleId) -> String`, used by the init.
- Unit tests: `TitleItemTests`: `testIdRoundTripsForMovie`, `testIdRoundTripsForSeries`, `testMalformedIdHasNoTitleId`,
  `testItemsRoundTripThroughJson`.
- Compile: `xcodebuild -project TmdbNative.xcodeproj -scheme TmdbNative -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build` (from `native-ios/`; same command for every step)

### Step 2: Adapters over the shared core  (AC-3, AC-5, AC-15, AC-16, AC-18) ✅
- Files: `TitleDetails/TitleDetailsSource.swift`, `TitleDetails/Favorites.swift` (new)
- What:
  - `@MainActor protocol TitleDetailsSource { func observe(_ id: TitleId, language: Language) -> AsyncStream<TitleDetailsItem?>; func refresh(_ id: TitleId, language: Language, force: Bool) async -> Bool }`
  - `TitleDetailsItem`, `ReleaseDate`, `CastItem` (see Decisions); `SharedCoreTitleDetails` maps Kotlin
    `TitleDetails` (`releaseDate` `LocalDate` → year / month number / day; `artworkUrl`, `photoUrl` → `URL`;
    `trailer?.url` → `URL`; `recommendations` → `TitleItem`; `KotlinInt?` → `Int?`).
  - `@MainActor protocol Favorites { func observeIsFavorite(_ id: TitleId) -> AsyncStream<Bool>; func setFavorite(_ id: TitleId, _ isFavorite: Bool) async }`
    and `SharedCoreFavorites` (errors of `setFavorite` are ignored: the observed value stays the truth).
- Unit tests (published API, `SharedCoreApiTests`): `testTitleDetailsAdapterEmitsWithoutWaitingForNetwork`,
  `testTitleDetailsAdapterRefreshCompletes`, `testFavoritesAdapterStoresAndClearsFavorite` (set true → observe true;
  set false → observe false).

### Step 3: Formatting  (AC-7, EC-4) ✅
- Files: `TitleDetails/TitleDetailsFormat.swift` (new)
- What: `enum TitleDetailsFormat { static func releaseDate(_: ReleaseDate, language: Language) -> String; static func runtime(minutes: Int) -> String; static func nameWithYear(_ name: String, year: Int?) -> String }`, as `TitleDetailsFormat.kt`
  (zero-padded, `\u{00A0}` before the year).
- Unit tests: `TitleDetailsFormatTests`: `testReleaseDateInPortugueseIsDayFirst`, `testReleaseDateInEnglishIsMonthFirst`,
  `testRuntimeWithHoursAndMinutes` (113 → `1h 53m`), `testRuntimeOfWholeHours` (120 → `2h`),
  `testRuntimeUnderAnHour` (45 → `45m`), `testNameWithYearKeepsYearAttached`, `testNameWithoutYear`.

### Step 4: Contract and ViewModel  (AC-3, AC-4, AC-5, AC-11, AC-13–AC-18, EC-1, EC-9, EC-11, EC-13) ✅
- Files: `TitleDetails/TitleDetailsContract.swift`, `TitleDetails/TitleDetailsViewModel.swift` (new)
- What:
  - `enum DetailsLoad: Equatable { case loading, error, loaded(TitleDetailsItem) }`
  - `struct TitleDetailsUiState: Equatable { var load: DetailsLoad = .loading; var isRefreshing = false; var isFavorite: Bool? = nil }`
  - `enum TitleDetailsIntent { case languageChanged(Language), trailerTapped, recommendationTapped(TitleItem), retry, favoriteTapped }`
    (no back intent: the system back button pops the stack).
  - `enum TitleDetailsEffect: Equatable { case openTrailer(URL), openTitle(TitleItem) }`
  - `TitleDetailsViewModel(titleId: TitleId, details: any TitleDetailsSource, favorites: any Favorites)`: `state`,
    `effects` (new stream per collector, as Home), `onIntent`, `refresh() async` (pull).
- Pseudo-code:
  ```
  init: observe favorites(titleId) for the ViewModel's life → state.isFavorite = value
  languageChanged(l): if l == language return; language = l; cancel details observation; observed = nil
      updateLoad(); observe details(titleId, l) → observed = value, updateLoad(); Task { refresh(l, force: false) }
  refresh(l, force): failed.remove(l); updateLoad(); if !await details.refresh(...) { failed.insert(l) }; updateLoad()
  updateLoad: load = observed.map(.loaded) ?? (failed.contains(language) ? .error : .loading)
  retry: Task { refresh(language, force: true) }
  pull refresh(): as HomeViewModel.refresh (one running task shared, isRefreshing only for the pull);
      does nothing before the language or when load is not .loaded
  favoriteTapped: guard let current = state.isFavorite, case .loaded = load; Task { await favorites.setFavorite(id, !current) }
  trailerTapped: guard let url = loaded details.trailerURL → yield .openTrailer(url)
  recommendationTapped(t): yield .openTitle(t)
  ```
- Unit tests (`TitleDetailsStub`, `FavoritesStub` in the test target, modelled on `TitleListsStub`; extended
  helpers):
  - `TitleDetailsViewModelTests`: `testNothingHappensBeforeLanguage`, `testLoadingUntilDetailsArrive`,
    `testCachedDetailsAreShown`, `testOpeningRefreshesWithoutForce`, `testFailedLoadWithoutCacheShowsError`,
    `testFailedRefreshKeepsCachedDetails`, `testRetryForcesRefreshAndLeavesError`,
    `testLanguageChangeObservesNewLanguage`, `testErrorIsPerLanguage`, `testFavoriteUnknownUntilStoredValue`,
    `testFavoriteTapStoresTheOppositeValue`, `testFavoriteTapsFollowStoredValue` (EC-1: two taps after each stored
    change → true then false), `testFavoriteTapIgnoredWithoutDetails`, `testTrailerTapOpensTrailer`,
    `testTrailerTapWithoutTrailerDoesNothing`, `testRecommendationTapOpensTitle`.
  - `TitleDetailsViewModelRefreshTests`: `testPullToRefreshForcesAndEndsIndicator`, `testPullWhileRefreshingIsIgnored`,
    `testPullWithoutDetailsDoesNothing`, `testFailedPullKeepsDetails`.

### Step 5: Assets, theme and copy  (AC-1, AC-3, AC-6, AC-8, AC-18, EC-10) ✅
- Files: `Assets.xcassets/FavoriteOff.imageset` (`ic_favorite_button.svg`), `FavoriteOn.imageset` (new
  `ic_favorite_on.svg`: the 65% `#303243` circle and the heart's outer path filled `#FF1F8A`),
  `Clock.imageset` (`ic_clock.svg`), `PlayCircle.imageset` (`ic_play_circle.svg`), each with
  `preserves-vector-representation` and the `Contents.json` shape of `SearchIcon.imageset`; `Theme.swift` (tokens);
  `Localizable.xcstrings`.
- Copy (en / pt-BR, `extractionState: manual`): `details.watchTrailer` Watch trailer / Assistir trailer;
  `details.cast` Main cast / Elenco principal; `details.genres` Genres / Categoria(s); `details.recommendations`
  Recommendations / Recomendações; `details.noSynopsis` Synopsis not available. / Sinopse indisponível.;
  `details.notRated` NR / NR; `details.notRatedDescription` Not rated yet / Ainda sem avaliação;
  `details.rating %lld%%` Rating %lld% / Avaliação %lld%; `details.error` Couldn't load this title. / Não foi possível
  carregar este título.; `details.favorite` Favorite / Favorito. Retry reuses `home.retry`.

### Step 6: Components  (AC-1, AC-3, AC-6, AC-7, AC-9–AC-12, EC-4–EC-6, EC-12) ✅
- Files: `Component/RestoringRow.swift` (new); `Home/Component/PosterRow.swift`, `Home/Component/RowMessage.swift`,
  `Home/HomeView.swift` (modify); `TitleDetails/Component/` `DetailsArtwork.swift`, `RatingRing.swift`,
  `DetailsHeader.swift`, `TrailerButton.swift`, `SectionHeading.swift`, `CastRow.swift`, `GenreChips.swift`,
  `FavoriteToggle.swift` (new)
- What:
  - `RestoringRow` (logic moved from `PosterRow`: visible indices from `onAppear`/`onDisappear`, saved
    `"<rowKey>|<item id>"`, `scrollTo` with the padding anchor once, `.id(rowKey)`); `PosterRow(titles:rowKey:savedPosition:onTap:)`
    on top of it; Home passes `rowKey: state.selectedCategory.name`.
  - `RowMessage` gains `alignment: HorizontalAlignment = .leading` (text alignment and frame follow it).
  - `DetailsArtwork(url:)`: full width × 493pt, `#303243`, `LazyImage` fill, fade overlay
    (clear → clear at 260/493 → `Theme.page` at 412/493 → page), `accessibilityHidden(true)`.
  - `RatingRing(percent:)`: 57pt; track circle, 45pt center, `Circle().trim(0, p/100)` stroke 6pt round caps
    rotated −90°, `Theme.accent`; "45" bold + "%" regular at 15pt, or "NR"; `accessibilityElement(children: .ignore)`
    with label `details.rating` / `details.notRatedDescription`.
  - `DetailsHeader(details:language:)`: ring, 16pt, VStack(name 22 bold with `nameWithYear`, header trait, wraps;
    5pt; date line 16 regular `#BBBBBB`: date, 3pt, 4pt dot, 6pt, clock 19pt, 6pt, runtime; missing parts and their
    separator left out; the line wraps via `ViewThatFits` fallback to a VStack at large sizes (EC-12)).
  - `TrailerButton(action:)`: full width, min height 44, `Capsule`, `LinearGradient(primary → primaryContainer,
    topLeading → bottomTrailing)`, play icon 20pt + 10pt + label 16 semibold.
  - `SectionHeading(_ key:)`: 18 bold white, header trait, `fixedSize(vertical)`.
  - `CastRow(cast:savedPosition:)`: `RestoringRow` with id `"<index>-<name>"`, column 75pt, 4pt spacing; 60pt photo
    (`LazyImage` fill or `person.fill` 30pt white 50% on track) with 4pt track border; 3pt; name 12 bold centered,
    `lineLimit(2)`, tail truncation; each item one accessibility element; no action.
  - `GenreChips(genres:)`: `FlowLayout(spacing: 14, lineSpacing: 10)`; chip min height 31, 17.5pt horizontal padding,
    `Capsule` track, label 15 medium.
  - `FavoriteToggle(isOn:onTap:)`: `Toggle(isOn: Binding(get: isOn, set: { _ in onTap() }))` with label
    `details.favorite`, `FavoriteToggleStyle` drawing `FavoriteOn`/`FavoriteOff` 32pt in a 44pt `contentShape`.
- Previews: header with long name and largest text; ring 45 % and NR; chips wrapping.
- Unit tests: none (views); covered by the manual checks in Traceability. Home's existing tests must still pass
  (row restore unchanged in behaviour).

### Step 7: Screen, navigation and restore  (AC-1, AC-2, AC-4, AC-5, AC-11, AC-13, AC-14, AC-17, EC-3, EC-5, EC-7, EC-8, EC-13) ✅
- Files: `TitleDetails/TitleDetailsView.swift` (new), `AppRootView.swift`, `TmdbNativeApp.swift` (modify),
  `TitleDetailsPlaceholderView.swift` (delete)
- What:
  - `TitleDetailsView(titleId: TitleId, stackIndex: Int, details:, favorites:, onTitleTap:)`: `@StateObject`
    ViewModel. Every `TitleItem` the app builds has a `titleId`; `AppRootView` drops items without one when it decodes
    the saved path, and the destination unwraps it with `if let`. `.task`: language →
    `languageChanged`, then collect effects (`openTrailer` → `openURL(url)`; `openTitle` → `onTitleTap`).
    Navigation bar as in Decisions; `ToolbarItem(placement: .navigationBarTrailing)` with `FavoriteToggle` when
    `state.load` is `.loaded` and `isFavorite != nil`. `@SceneStorage` positions keyed by `stackIndex`.
  - `TitleDetailsContent(state:language:onIntent:onRefresh:positions:)`: background gradient (`#303243` →
    `#15151D` at 65.394%, as Home) ignoring safe areas; `.loading` → centered large `ProgressView` tinted accent;
    `.error` → centered `RowMessage("details.error", alignment: .center) { onIntent(.retry) }`; `.loaded` →
    `ScrollViewReader` + vertical `ScrollView` (`.ignoresSafeArea(edges: .top)`, `.refreshable { await onRefresh() }`)
    with a `ZStack(alignment: .top)` of `DetailsArtwork` and the body `VStack` at top padding 376 (header, 21pt,
    divider 1pt black 48%, 20pt, synopsis 14 `#CCCCCC` line spacing 3.5 or `details.noSynopsis`, trailer (30pt
    before), cast (46pt + heading + 16pt + row), genres (41pt + heading + 20pt + chips), recommendations (41pt +
    heading + 20pt + `PosterRow`), 40pt bottom), scroll markers in the background.
  - `AppRootView`: the path becomes `[DetailsRoute]` (`struct DetailsRoute: Hashable, Codable { let title: TitleItem;
    let index: Int }`, `index` = path count when pushed), so each screen knows its depth even when the same title is
    stacked twice (EC-7); `navigationDestination(for: DetailsRoute.self)` builds `TitleDetailsView` with
    `stackIndex: route.index`; Home and details push `DetailsRoute(title:, index: path.count)`;
    `@SceneStorage("app.path")` encode on `.onChange(of: path)`, decode once when Home first shows (items without a
    `titleId` dropped). `TmdbNativeApp` passes `SharedCoreTitleDetails()` and `SharedCoreFavorites()`.
- Previews: Loaded (all data, en), Loaded (missing sections, pt-BR), Loading, Error, largest text.
- Unit tests: none new (wiring); `AppViewModelTests` and Home tests still pass.

### Step 8: Docs ✅
- Files: `native-ios/README.md` (screens: onboarding, Home, Title details; favorites through the shared core),
  `CLAUDE.md` is not touched (another session edits the repo docs; the current-state line is updated when both native
  subtasks merge). Plan and subtask statuses set to Done at the end of forge.

Verification after the last step (from the repo root, then `native-ios/`):
`./gradlew publishSharedCore`; `xcodebuild … build`; `xcodebuild -project TmdbNative.xcodeproj -scheme TmdbNative -destination 'platform=iOS Simulator,name=iPhone 17' test`.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 Layout | 5, 6, 7 | Previews; manual: side by side with design and cmp |
| AC-2 Back | 7 | Manual: back button and edge swipe return to Home with tab/rows kept, or to the previous title |
| AC-3 Favorite | 2, 4, 6 | `testFavoriteTapStoresTheOppositeValue`, `testFavoritesAdapterStoresAndClearsFavorite`; manual: relaunch, offline, both languages, VoiceOver switch "Favorite" |
| AC-4 Heart only with details | 4, 7 | `testFavoriteTapIgnoredWithoutDetails`; manual: Loading/Error show no heart |
| AC-5 Trailer | 2, 4, 7 | `testTrailerTapOpensTrailer`, `testTrailerTapWithoutTrailerDoesNothing`; manual: YouTube app / Safari (en fallback is in the shared core, plan 01) |
| AC-6 Rating | 6 | Previews (45 %, NR); manual: VoiceOver "Not rated yet" |
| AC-7 Name, year, date, runtime | 3, 6 | `TitleDetailsFormatTests`; previews with missing parts |
| AC-8 Synopsis | 5, 7 | Preview pt-BR missing sections |
| AC-9 Cast | 2, 6 | Previews; manual: placeholder photo, no tap action |
| AC-10 Genres | 6 | Preview chips wrapping |
| AC-11 Recommendations | 4, 6, 7 | `testRecommendationTapOpensTitle`; manual: opens on top, VoiceOver button with name |
| AC-12 Artwork | 2, 6 | Manual: poster / backdrop / placeholder; bar background readable when scrolled; VoiceOver skips it |
| AC-13 First load | 4, 7 | `testLoadingUntilDetailsArrive` |
| AC-14 Error and retry | 4, 7 | `testFailedLoadWithoutCacheShowsError`, `testRetryForcesRefreshAndLeavesError` |
| AC-15 Offline with cache | 4 | `testCachedDetailsAreShown`, `testFailedRefreshKeepsCachedDetails` |
| AC-16 Freshness | 4 | `testOpeningRefreshesWithoutForce` (TTL in the shared core, plan 01) |
| AC-17 Pull-to-refresh | 4, 7 | `testPullToRefreshForcesAndEndsIndicator`, `testFailedPullKeepsDetails` |
| AC-18 Language | 2, 4, 5 | `testLanguageChangeObservesNewLanguage`; manual: pt-BR copy |
| EC-1 Quick heart taps | 4 | `testFavoriteTapsFollowStoredValue` |
| EC-2 Repeated trailer/poster taps | — | Not handled (decision, as Home and cmp) |
| EC-3 Nothing can open the trailer | 7 | Manual (`openURL` not accepted → no change) |
| EC-4 Long name | 3, 6 | `testNameWithYearKeepsYearAttached`; preview long name |
| EC-5 Long synopsis | 7 | Manual / preview: full text, screen scrolls |
| EC-6 Long cast name | 6 | Manual: two lines, truncated |
| EC-7 Stacked details | 7 | Manual: each back returns one step, same title twice |
| EC-8 Rotation / process death | 1, 7 | `testItemsRoundTripThroughJson`; manual: kill in background from Xcode, relaunch |
| EC-9 Language change | 4 | `testLanguageChangeObservesNewLanguage`, `testErrorIsPerLanguage` |
| EC-10 Other language | 4 | Covered by `Language.fromLocaleTag` (shared core); manual: French device → English |
| EC-11 Pull while refreshing | 4 | `testPullWhileRefreshingIsIgnored` |
| EC-12 Largest font | 6, 7 | Preview largest text; manual |
| EC-13 Title gone on TMDB | 4 | `testFailedLoadWithoutCacheShowsError`; manual: back still works |

## Out of scope
- `native-android` (subtask 05, another session); `cmp`, `core/`, `service/` unchanged.
- EC-2 open-once guard.
- The designed "Voltar" pill and back arrow (system navigation bar chosen instead).
- `CLAUDE.md` current-state update (after both native subtasks).

## Risks
- SKIE names for Kotlin types (`LocalDate` from kotlinx-datetime, `KotlinInt?`, `Trailer.url`) may differ from what
  the plan writes — checked against the generated header after `publishSharedCore` in Step 2; the mapping stays in
  the adapter.
- The system back button text: with no title on the previous screen UIKit shows "Back" / "Voltar" from its own
  localization — checked on the simulator in both languages; whatever UIKit shows is accepted (system control).
- Vertical restore is exact only to 100pt (iOS 16 limit) — accepted with the user's choice.
- `.refreshable` on a `ScrollView` that ignores the top safe area may place the indicator under the navigation bar —
  checked on the simulator; if hidden, the scroll view keeps the safe area and the artwork extends upward with a
  negative top padding instead.
- Nuke and the shared core need network on the first build (package resolution).

## Deviations
- `TitleDetailsSource` and `Favorites` adapters rebuild the `TitleId` from its parts in a `nonisolated` helper before
  the suspend call: Swift 6 strict concurrency refuses to send the (non-`Sendable`) Kotlin object off the main actor.
- `TaskBag` (in `HomeViewModel.swift`) is now internal instead of private, so the details ViewModel reuses it.
- The month comes from `LocalDate.month.ordinal + 1`: kotlinx-datetime's `Month.number` extension is not exported to
  Swift and `monthNumber` is deprecated.
- The screen background is `Theme.page` (#21222E) in every state, as cmp, instead of Home's gradient: the artwork
  fade ends in that color, so the page below it matches.
- `FavoritesStub` lives in `TitleDetailsStub.swift` with the details stub and the shared test data, not in its own
  file; `detailsTitleId` is a computed property (a non-`Sendable` global is refused by Swift 6).
- `CastRow` takes a `rowKey` (prefixed with the title id, like the recommendations row) instead of a fixed `"cast"`,
  so a saved row position of another title at the same depth is ignored.
- `TitleItemTests.testRoutesRoundTripThroughJson` checks `[DetailsRoute]` (the saved path) instead of
  `[TitleItem]`.
- Rating ring value: `Text(verbatim:)` concatenation, so no stray keys reach the string catalog.

