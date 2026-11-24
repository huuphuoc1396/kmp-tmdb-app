# Plan: Favorites on iOS (native)

Requirement: [05 - Favorites on iOS (native)](../subtasks/05-native-ios-favorites.md)
Status: Done

## Summary
Bring the cmp / native-android favorites to `native-ios` (SwiftUI, iOS 16) with the app's own patterns: a Swift
protocol `FavoriteTitles` and its `SharedCoreFavoriteTitles` adapter over `SharedCore.observeFavoriteTitles`
(already exported by `core:ios`, plan 01), Swift value types for the UI, `ObservableObject` ViewModels with
`HomeUiState`-style structs, Swift stubs in tests, SVG image sets and `Localizable.xcstrings`. Home gets the
Favorites section, a new `Favorites/` folder holds My favorites, Title details asks before removing, and the
`NavigationStack` path gains a My favorites route. No shared-core change.

## Decisions
- **Popup: a custom overlay drawn to the design** (from user; rejected: system `.alert`, which doesn't match the
  design and can't be dismissed by tapping outside). `RemoveFavoriteDialog` is a view modifier
  `.removeFavoriteDialog(titleName: String??, onConfirm:, onDismiss:)` (`nil` = hidden; `.some(nil)` = no name):
  full-screen scrim `Color.black.opacity(0.68)` (tap → dismiss, AC-9), centered card per
  `design/remove-favorite-dialog.md` (≈ values, as in cmp). The card has `.accessibilityAddTraits(.isModal)` so
  VoiceOver stays inside it and `.accessibilityAction(.escape)` (two-finger scrub) dismisses (AC-9, AC-13). Message
  wraps in full (EC-5); content scrolls at the largest Dynamic Type size (EC-11).
- **Same behavior and state shape as cmp / Android** (plans 02–04): 10 newest on Home with the total in the badge;
  `pendingRemoval` / `isRemoveConfirmationShown` in each ViewModel's state, so the popup stays open on rotation
  and is gone after the app is killed (EC-6 as updated); second heart tap ignored while pending, confirm twice
  writes once (EC-1); no double-tap guard on navigation (EC-2 not met, as elsewhere); pt-BR "Meus favoritos";
  empty message and unknown-name copy as in cmp.
- **Data**: `protocol FavoriteTitles { func observe(_ language: Language) -> AsyncStream<[FavoriteTitleItem]> }`
  with `SharedCoreFavoriteTitles` (same `AsyncStream` bridge as `SharedCoreTitleLists`);
  `struct FavoriteTitleItem: Hashable, Identifiable { id: String (TitleItem id format), name: String?, posterURL: URL?,
  releaseYear: Int?, firstGenre: String? }` mapped from `FavoriteTitle` / `TitleSummary` (`releaseYear` from the
  boxed `KotlinInt`). `titleItem` (name or "") feeds the existing `DetailsRoute`. Removing uses the existing
  `Favorites.setFavorite(_:_:)`.
- **Navigation**: the stack path becomes `[AppRoute]`, `enum AppRoute: Hashable, Codable { case details(DetailsRoute);
  case favorites }`, saved in the same `@SceneStorage("app.path")`; a saved path from the old format fails to
  decode and is ignored (starts at Home), as today for bad data. Home gets `onSeeAllFavorites`; My favorites
  gets `onTitleTap` (→ details) and back via the stack.
- **My favorites header uses the system navigation bar**, like Title details on iOS (`.toolbar(.visible)`,
  `Theme.track` background, dark scheme): the system back button (label "Back" / "Voltar" from the platform)
  replaces the design's custom "Voltar" control, and the "N titles" badge is the trailing toolbar item. Title and
  subtitle stay at the top of the scroll content. No `favorites.back` string and no back-arrow asset.
- **Scroll restoration**: Home's favorites row uses the existing `RestoringRow` with
  `@SceneStorage("home.favorites.position")`; My favorites saves the first visible card id in
  `@SceneStorage("favorites.scroll")` and scrolls back with `ScrollViewReader`, like Title details (EC-7).
- **Icons**: SVG image sets built from the same paths as cmp / Android — `ChevronRight`, `FavoriteRemove` (28pt
  dark circle + pink outline heart), `HeartFilled` (20×18 pink heart).
- **Strings** in `Localizable.xcstrings` (en + pt-BR), keys following the file's style: `favorites.section.title`,
  `favorites.section.subtitle`, `favorites.section.viewAll`, `favorites.loading`, `favorites.title`,
  `favorites.subtitle`, `favorites.empty`, `favorites.count %lld` (plural variations one / other),
  `removeFavorite.title`, `removeFavorite.message %@`, `removeFavorite.messageUnknown`, `removeFavorite.cancel`,
  `removeFavorite.confirm`, `removeFavorite.heart`.
- **Tests** (XCTest, app-hosted): Swift stubs as today — new `FavoriteTitlesStub` (`send(_:for:)`,
  `observedLanguages`); `FavoritesStub` gains `writtenIds: [TitleId]` so removals are checked by id. One
  adapter test in `SharedCoreApiTests` exercises the real `SharedCoreFavoriteTitles` through the published API.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Favorites list with title data | `SharedCore.shared.observeFavoriteTitles` (`invoke(language:)`) | `TmdbCore` (plan 01) |
| Remove a favorite | `Favorites` / `SharedCoreFavorites.setFavorite` | `native-ios/TmdbNative/TitleDetails/Favorites.swift` |
| Flow → `AsyncStream` bridge | `SharedCoreTitleLists.observe` | `native-ios/TmdbNative/Home/TitleLists.swift` |
| Title value type, details route | `TitleItem`, `DetailsRoute` | `native-ios/TmdbNative/TitleItem.swift`, `AppRootView.swift` |
| Poster tile look | `PosterRow`'s private `Poster` (image or name placeholder, `LazyImage`) | `native-ios/TmdbNative/Home/Component/PosterRow.swift` |
| Horizontal row with restored position | `RestoringRow` | `native-ios/TmdbNative/Component/RestoringRow.swift` |
| Vertical scroll restoration | `TitleDetailsView` (`@SceneStorage`, `ScrollViewReader`) | `native-ios/TmdbNative/TitleDetails/TitleDetailsView.swift` |
| Task cancellation helper | `TaskBag` | `native-ios/TmdbNative/Home/HomeViewModel.swift` |
| Colors | `Theme` | `native-ios/TmdbNative/Theme.swift` |
| Test stubs | `TitleListsStub`, `TitleDetailsStub`, `FavoritesStub` | `native-ios/TmdbNativeTests/` |

## Changes overview
| Area (`native-ios/`) | Files (new / modified) | Why |
|---|---|---|
| `TmdbNative/Component` | `PosterTile.swift`, `MetaLine.swift`, `RemoveFavoriteDialog.swift` (new) | Shared UI |
| `TmdbNative/Home` | `HomeContract.swift`, `HomeViewModel.swift`, `HomeView.swift`, `Component/PosterRow.swift` (modify); `Component/FavoritesSection.swift` (new) | Home section |
| `TmdbNative/Favorites` (new) | `FavoriteTitles.swift`, `FavoritesContract.swift`, `FavoritesViewModel.swift`, `FavoritesView.swift`, `Component/FavoriteCard.swift` | Data adapter, My favorites |
| `TmdbNative/TitleDetails` | `TitleDetailsContract.swift`, `TitleDetailsViewModel.swift`, `TitleDetailsView.swift` (modify) | Confirm before removing |
| `TmdbNative` | `AppRootView.swift`, `TmdbNativeApp.swift`, `Localizable.xcstrings`, `Assets.xcassets/{ChevronRight,FavoriteRemove,HeartFilled}.imageset` (new) | Wiring, copy, icons |
| `TmdbNativeTests` | `FavoriteTitlesStub.swift`, `MetaLineTests.swift`, `HomeViewModelFavoritesTests.swift`, `FavoritesViewModelTests.swift` (new); `TitleDetailsStub.swift`, `TitleDetailsViewModelTests.swift`, `HomeViewModelTests.swift`, `HomeViewModelRefreshTests.swift`, `SharedCoreApiTests.swift` (modify) | Tests |
| Docs | `native-ios/README.md`, `CLAUDE.md` (native-ios line) | Screens |

The Xcode project uses synchronized folders (`PBXFileSystemSynchronizedRootGroup`), so new files need no
`project.pbxproj` edit.

## Steps
Before Step 1: `./gradlew publishSharedCore` at the repo root (macOS), so `TmdbCore` has `observeFavoriteTitles`.
Build command for every step (from `native-ios/`):
`xcodebuild -project TmdbNative.xcodeproj -scheme TmdbNative -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`
and, when tests changed, `build-for-testing` with `-destination 'platform=iOS Simulator,name=iPhone 17'`.

### Step 1: Shared UI pieces and icons  (AC-4, AC-6, AC-9, AC-12, AC-13, EC-5, EC-11) ✅
- Files: `Component/PosterTile.swift`, `Component/MetaLine.swift`, `Component/RemoveFavoriteDialog.swift` (new);
  `Home/Component/PosterRow.swift` (modify); `Assets.xcassets/{ChevronRight,FavoriteRemove,HeartFilled}.imageset`
  (new); `Localizable.xcstrings` (`removeFavorite.*`); `TmdbNativeTests/MetaLineTests.swift` (new)
- What: `PosterTile(name: String?, posterURL: URL?)` (the `Poster` body without button / size); `PosterRow` uses it;
  `func metaLine(year: Int?, genre: String?) -> String`; the dialog modifier per Decisions.
- Unit tests: `MetaLineTests` — `testYearAndGenreAreJoinedWithDot`, `testMissingYearLeavesGenreOnly`,
  `testMissingGenreLeavesYearOnly`, `testBothMissingIsEmpty`.
- Compile: build + build-for-testing.

### Step 2: Favorites data adapter  (AC-11, AC-12, EC-8, EC-10) ✅
- Files: `Favorites/FavoriteTitles.swift` (new); `TmdbNativeTests/FavoriteTitlesStub.swift` (new),
  `TitleDetailsStub.swift` (`FavoritesStub.writtenIds`), `SharedCoreApiTests.swift` (modify)
- What: protocol, `FavoriteTitleItem` (+ `init(_ favorite: FavoriteTitle)`, `titleItem`, `meta`), adapter.
- Unit tests: `SharedCoreApiTests.testFavoriteTitlesAdapterEmitsStoredFavorites` (favorite a title through
  `favoritesRepository`, the adapter emits it with a `nil`-or-filled summary without waiting for network).
- Compile: build + build-for-testing.

### Step 3: My favorites  (AC-3, AC-4, AC-5, AC-6, AC-8, AC-9, AC-10, AC-11, AC-12, AC-13, EC-1, EC-3, EC-5, EC-6, EC-7, EC-9) ✅
- Files: `Favorites/FavoritesContract.swift`, `FavoritesViewModel.swift`, `FavoritesView.swift`,
  `Component/FavoriteCard.swift` (new); `Localizable.xcstrings` (`favorites.*`);
  `TmdbNativeTests/FavoritesViewModelTests.swift` (new)
- What: `FavoritesUiState { favorites: [FavoriteTitleItem]; pendingRemoval: FavoriteTitleItem? }`;
  `FavoritesIntent { languageChanged, titleTapped(FavoriteTitleItem), removeTapped(FavoriteTitleItem),
  removeConfirmed, removeDismissed }`; `FavoritesEffect.openTitle(TitleItem)`;
  `FavoritesViewModel(favoriteTitles:, favorites:)`; the view (system nav bar with the badge, title, subtitle,
  cards, empty message, dialog) per `design/my-favorites.md`; the card's heart is a separate button labelled
  `removeFavorite.heart`.
- Unit tests (`FavoritesViewModelTests`): `testListsFavoritesInOrder`, `testEmptyWithoutFavorites`,
  `testFollowsLanguageChange`, `testTitleTapOpensTitle`, `testRemoveTapAsksToConfirm`,
  `testSecondRemoveTapKeepsFirstRequest`, `testConfirmRemovesFavoriteAndCloses`, `testConfirmTwiceWritesOnce`,
  `testDismissKeepsFavorite`.
- Compile: build + build-for-testing (the view isn't reachable yet; wired in Step 5).

### Step 4: Home favorites  (AC-1, AC-2, AC-3, AC-4, AC-5, AC-6, AC-8, AC-9, AC-11, AC-12, AC-13, EC-1, EC-3, EC-4, EC-5, EC-6, EC-9) ✅
- Files: `Home/HomeContract.swift`, `HomeViewModel.swift`, `HomeView.swift` (modify);
  `Home/Component/FavoritesSection.swift` (new); `Localizable.xcstrings` (`favorites.section.*`);
  `TmdbNativeTests/HomeViewModelTests.swift`, `HomeViewModelRefreshTests.swift` (constructor),
  `HomeViewModelFavoritesTests.swift` (new)
- What: `HomeUiState.favorites` (≤ 10), `favoritesCount`, `pendingRemoval`; intents `favoriteTapped`,
  `seeAllFavoritesTapped`, `removeFavoriteTapped`, `removeFavoriteConfirmed`, `removeFavoriteDismissed`; effect
  `openFavorites` (a favorite opens with the existing `openTitle`); `HomeViewModel(titleLists:, favoriteTitles:,
  favorites:)` observes favorites for the current language; `HomeView` draws the section between the featured
  row and "Mais populares" only when `favoritesCount > 0`, and the dialog; `HomeView(titleLists:, favoriteTitles:,
  favorites:, onTitleTap:, onSeeAllFavorites:)`.
- Unit tests (`HomeViewModelFavoritesTests`): `testNoFavoritesGivesEmptySection`,
  `testShowsTenNewestFavoritesWithTotalCount`, `testSectionIsTheSameForEveryCategory`,
  `testFavoritesFollowLanguageChange`, `testFavoriteTapOpensTitle`, `testSeeAllOpensFavorites`,
  `testRemoveTapAsksToConfirm`, `testConfirmRemovesFavoriteAndCloses`, `testConfirmTwiceWritesOnce`,
  `testDismissKeepsFavorite`.
- Compile: build fails until `AppRootView` passes the new arguments — `AppRootView` / `TmdbNativeApp` are updated
  in this step too (route enum, `favoriteTitles` injection, `FavoritesView` destination), so the step builds.

### Step 5: Title details confirmation  (AC-7, AC-8, AC-9, EC-1, EC-6) ✅
- Files: `TitleDetails/TitleDetailsContract.swift`, `TitleDetailsViewModel.swift`, `TitleDetailsView.swift` (modify);
  `TmdbNativeTests/TitleDetailsViewModelTests.swift` (modify)
- What: `isRemoveConfirmationShown`; `favoriteTapped` adds when off, asks when on; intents
  `removeFavoriteConfirmed`, `removeFavoriteDismissed`; the view shows the dialog with `details.name`.
- Unit tests: replace `testFavoriteTapStoresTheOppositeValue` / `testFavoriteTapsFollowStoredValue` with
  `testFavoriteTapAddsWhenOff`, `testFavoriteTapAsksToConfirmWhenOn`, `testConfirmRemovesFavorite`,
  `testConfirmTwiceWritesOnce`, `testDismissKeepsFavorite`; keep `testFavoriteTapIgnoredWithoutDetails`.
- Compile: build + build-for-testing.

### Step 6: Docs ✅
- Files: `native-ios/README.md` (Screens paragraph: Home favorites section, My favorites, removal confirmation;
  `FavoriteTitles` adapter in the protocol list), `CLAUDE.md` (native-ios line: "onboarding and Home" → also
  title details and My favorites).
- Compile: none.

After the last step (from `native-ios/`):
`xcodebuild -project TmdbNative.xcodeproj -scheme TmdbNative -destination 'platform=iOS Simulator,name=iPhone 17' test`.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1, AC-2 | 4 | `testShowsTenNewest…`, `testSectionIsTheSame…`, `testNoFavorites…`; manual: section position |
| AC-3 | 3, 4 | `testSeeAllOpensFavorites`, `testListsFavoritesInOrder`; manual: badge plurals, back |
| AC-4 | 1, 3, 4 | `MetaLineTests`; manual: placeholder tile, hearts |
| AC-5 | 3, 4 | `testTitleTapOpensTitle`, `testFavoriteTapOpensTitle` |
| AC-6 | 1, 3, 4 | `testRemoveTapAsksToConfirm` (Home, My favorites) |
| AC-7 | 5 | `testFavoriteTapAddsWhenOff`, `testFavoriteTapAsksToConfirmWhenOn` |
| AC-8 | 3, 4, 5 | `testConfirmRemovesFavoriteAndCloses` (both), `testConfirmRemovesFavorite` |
| AC-9 | 1, 3, 4, 5 | `testDismissKeepsFavorite` (all three); manual: tap outside, VoiceOver escape |
| AC-10 | 3 | `testEmptyWithoutFavorites`; manual: empty message after last removal |
| AC-11 | 2, 3, 4 | `testFavoriteTitlesAdapterEmitsStoredFavorites`; manual: airplane mode |
| AC-12 | 1, 2, 3, 4 | `testFollowsLanguageChange`, `testFavoritesFollowLanguageChange`; manual: en / pt-BR copy |
| AC-13 | 1, 3, 4 | Manual: VoiceOver (item / heart labels, modal focus), touch targets |
| EC-1 | 3, 4, 5 | `testSecondRemoveTapKeepsFirstRequest`, `testConfirmTwiceWritesOnce` (×3) |
| EC-2 | — | Not met by decision (as cmp / Android) |
| EC-3 | 3, 4 | Stub updates in `testConfirmRemoves…`; manual: favorite on details then back |
| EC-4 | 4 | `testShowsTenNewestFavoritesWithTotalCount` |
| EC-5 | 1, 3, 4 | Manual: truncation and full name in popup |
| EC-6 | 3, 4, 5 | Manual: rotate with popup open (stays), kill the app (closed) |
| EC-7 | 3 | Manual: background-kill on My favorites, scroll restored |
| EC-8, EC-10 | 2 | Shared core (plan 01); adapter test |
| EC-9 | 3, 4 | `testFollowsLanguageChange`, `testFavoritesFollowLanguageChange` |
| EC-11 | 1, 3, 4 | Manual: largest Dynamic Type |

## Out of scope
- Any shared-core, cmp or native-android change.
- Exact Figma values / assets (rate limit; ≈ values as in cmp).

## Risks
- `native-ios` builds only against a fresh `publishSharedCore` (XCFramework with `observeFavoriteTitles`).
- Changing the path type drops a saved navigation path from before the update (starts at Home once).
- VoiceOver focus in a custom overlay depends on `.isModal`; verify by hand.
- Tests and builds need macOS + Xcode and the `iPhone 17` simulator; not run by CI.

## Deviations
- The dialog modifier is `.removeFavoriteDialog(isPresented: Bool, titleName: String?, onConfirm:, onDismiss:)`
  instead of a `String??` parameter; same behavior, clearer call sites.
- The dialog's fallback for large text is `ViewThatFits(in: .vertical) { content; ScrollView { content } }`
  (`scrollBounceBehavior` needs iOS 16.4).
- `RestoringRow` got a `leadingPadding` parameter (default `homeScreenPadding`, so Home's rows are unchanged) so the
  favorites row inside the card starts at the card's 12pt padding; the row's height is computed from the
  Dynamic Type sizes of the name and meta lines.
- The Home section's "remove" tests are merged into `testRemoveTapAsksToConfirm` (also checks the second tap is
  ignored); the planned `testListsFavoritesInOrder` etc. names are kept elsewhere.
- Tests need a signed build (the shared core reads the Keychain at launch): run the README test command without
  `CODE_SIGNING_ALLOWED=NO`; with it the test host crashes on Keychain error -34018.
- `CLAUDE.md`'s native-ios line now lists onboarding, Home, My favorites and Title details (it still listed only
  onboarding and Home).
