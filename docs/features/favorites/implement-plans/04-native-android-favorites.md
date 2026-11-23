# Plan: Favorites on Android (native)

Requirement: [04 - Favorites on Android (native)](../subtasks/04-native-android-favorites.md)
Status: Done

## Summary
Port the cmp work of plans 02 and 03 into `native-android/:app`, which has the same packages, contracts and
ViewModels as cmp. The app gets a `PosterTile`, `metaLine` and `RemoveFavoriteDialog` in its `designsystem`
package; Home gets the Favorites section with hearts and the confirmation; a new `favorites/` package holds
My favorites (list, hearts, popup, empty state); Title details asks to confirm before removing. Data comes from
`ObserveFavoriteTitles` (published `service-title-domain`, bound by `core-di`); tests use the published fakes.

## Decisions
- **Same behavior and shapes as cmp** (plans 02 and 03, both approved): `HomeUiState.favorites / favoritesCount /
  pendingRemoval`, `FavoritesUiState(favorites, pendingRemoval)`, `TitleDetailsUiState.isRemoveConfirmationShown`;
  the same intents and effects; the 10-item limit; popup state in the ViewModel (EC-6 as updated); no
  double-tap guard (EC-2 not met, as in cmp and native Home); pt-BR title "Meus favoritos"; empty message and
  unknown-name popup copy as in cmp. No new decisions were needed, so no questions were asked.
- **Android resources** instead of Compose Multiplatform resources: strings and plurals in
  `app/src/main/res/values/strings.xml` and `values-pt-rBR/strings.xml` (`stringResource`, `pluralStringResource`
  from `androidx.compose.ui.res`); drawables in `app/src/main/res/drawable`. The app module has one `R`, so the
  heart label is just `R.string.remove_favorite_heart` (no `removeFavoriteLabel()` helper needed).
- **Drawables** copied from cmp: `ic_chevron_right.xml`, `ic_favorite_remove.xml` (home), `ic_heart_filled.xml`
  (favorites); `ic_back_arrow.xml` already exists in the app.
- **Packages** (by screen, as in the app today): `designsystem/component/` (`PosterTile`, `RemoveFavoriteDialog`),
  `designsystem/text/MetaLine.kt`, `home/component/FavoritesSection.kt`, new `favorites/` with
  `FavoritesContract`, `FavoritesViewModel`, `FavoritesScreen`, `FavoritesColors`, `component/FavoriteCard`,
  `navigation/{FavoritesRoute, FavoritesNavigation}`, `di/FavoritesModule`.
- **Tests** follow the app's style: JUnit 4 `MainDispatcherRule`, `kotlin.test`, published fakes
  (`FakeFavoritesRepository`, `FakeTitleDetailsRepository`) with the real `ObserveFavoriteTitles`; effects are
  read with `viewModel.effects.first()` (the app has no Turbine). Removal tests are in their own classes from the
  start (Detekt `TooManyFunctions`, max 11), like cmp.
- **`MetaLineTest`** is a JVM unit test in `app/src/test` (the app is Android-only; there is no `commonTest`).

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Favorites with title data | `ObserveFavoriteTitles` | published `service-title-domain` (bound in `core-di`) |
| Remove a favorite | `FavoritesRepository.setFavorite(id, false)` | published `service-favorites-domain` |
| Reference implementation | cmp `PosterTile`, `metaLine`, `RemoveFavoriteDialog`, `FavoritesSection`, `cmp/feature/favorites/ui`, title-details confirmation | `cmp/` (copied, not depended on) |
| Poster row to refactor | `PosterRow` (private `Poster`) | `native-android/.../designsystem/component/PosterRow.kt` |
| Back arrow, favorite icons | `ic_back_arrow.xml`, `ic_favorite_on/off.xml` | `native-android/app/src/main/res/drawable` |
| Navigation to details | `TitleId.toDetailsRoute()` | `native-android/.../titledetails/navigation/TitleDetailsRoute.kt` |
| Test doubles and dispatcher | published `service-favorites-fake`, `service-title-fake`; `MainDispatcherRule` | `native-android/app/build.gradle.kts`, `app/src/test/.../MainDispatcherRule.kt` |

## Changes overview
| Area (`native-android/app/src/`) | Files (new / modified) | Why |
|---|---|---|
| `main/.../designsystem` | `component/PosterTile.kt`, `component/RemoveFavoriteDialog.kt`, `text/MetaLine.kt` (new); `component/PosterRow.kt` (modify) | Shared poster, meta line, popup |
| `main/.../home` | `HomeContract.kt`, `HomeViewModel.kt`, `HomeScreen.kt`, `navigation/HomeNavigation.kt` (modify); `component/FavoritesSection.kt` (new) | Home section, hearts, popup |
| `main/.../favorites` (new) | contract, ViewModel, screen, colors, card, route, navigation, Koin module | My favorites |
| `main/.../titledetails` | `TitleDetailsContract.kt`, `TitleDetailsViewModel.kt`, `TitleDetailsScreen.kt` (modify) | Confirm before removing |
| `main/...` | `AppNavHost.kt`, `di/NativeAppModule.kt` (modify) | Wiring |
| `main/res` | `values/strings.xml`, `values-pt-rBR/strings.xml` (modify); `drawable/ic_chevron_right.xml`, `ic_favorite_remove.xml`, `ic_heart_filled.xml` (new) | Copy and icons |
| `test/...` | `designsystem/text/MetaLineTest.kt`, `home/HomeViewModelFavoritesTest.kt`, `home/HomeViewModelRemoveFavoriteTest.kt`, `favorites/FavoritesViewModelTest.kt`, `favorites/FavoritesViewModelRemoveTest.kt` (new); `home/HomeTestFixtures.kt`, `home/HomeViewModel*Test.kt`, `titledetails/TitleDetailsViewModelFavoriteTest.kt`, `titledetails/TitleDetailsTestFixtures.kt` if it builds the ViewModel (modify) | Tests |
| Docs | `native-android/README.md`, `CLAUDE.md` (native-android screens) | Screens list |

## Steps
Before Step 1 (and whenever the shared core changes): `./gradlew publishSharedCore` at the repo root, so the app
compiles against the published `ObserveFavoriteTitles` and fakes.

### Step 1: Design system pieces  (AC-4, AC-6, AC-9, AC-12, AC-13, EC-5, EC-11) ✅
- Files: `designsystem/component/PosterTile.kt`, `designsystem/component/RemoveFavoriteDialog.kt`,
  `designsystem/text/MetaLine.kt` (new); `designsystem/component/PosterRow.kt` (modify); strings
  `remove_favorite_*` (en, pt-BR); `test/.../designsystem/text/MetaLineTest.kt` (new)
- What: ports of the cmp files (plan 02 Step 1, plan 03 Step 1) with `R` resources; `PosterRow`'s `Poster` calls
  `PosterTile`.
- Unit tests: `MetaLineTest` (4 cases as in cmp).
- Compile: `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` (in `native-android/`)

### Step 2: My favorites  (AC-3, AC-4, AC-5, AC-6, AC-8, AC-9, AC-10, AC-11, AC-12, AC-13, EC-1, EC-3, EC-5, EC-6, EC-7, EC-9) ✅
- Files: new `favorites/` package (contract, ViewModel, screen, colors, card, route, navigation,
  `di/FavoritesModule.kt`); strings `favorites_*` and plurals `favorites_count` (en, pt-BR);
  `drawable/ic_heart_filled.xml`; `test/.../favorites/FavoritesViewModelTest.kt` (5),
  `FavoritesViewModelRemoveTest.kt` (6)
- What: port of `cmp/feature/favorites/ui` after plan 03, with `R` resources and `koinViewModel()`.
- Unit tests: same names as cmp (`listsAllFavoritesNewestFirst`, `emptyWithoutFavorites`,
  `updatesWhenFavoriteRemoved`, `followsLanguageChange`, `titleClickOpensTitle`; removal: the 6 cmp cases ending
  with `removingLastFavoriteLeavesEmptyList`).
- Compile: `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` (the package compiles on its own;
  its route is wired in Step 3)

### Step 3: Home favorites (state and UI), Title details confirmation and wiring  (AC-1 – AC-9, AC-11, AC-12, AC-13, EC-1, EC-3, EC-4, EC-5, EC-6, EC-9) ✅
- Files: `home/HomeContract.kt`, `home/HomeViewModel.kt` (modify); `test/.../home/HomeTestFixtures.kt`,
  `HomeViewModelTest.kt`, `HomeViewModelRefreshTest.kt`, `HomeViewModelSelectionTest.kt` (modify);
  `HomeViewModelFavoritesTest.kt`, `HomeViewModelRemoveFavoriteTest.kt` (new)
- What: `HomeViewModel(repository, observeFavoriteTitles, favoritesRepository, savedStateHandle)` with the cmp
  logic (plans 02 Step 2 and 03 Step 2); fixture `homeViewModel(repository, savedState, favorites, details)`.
- Unit tests: as in cmp — `HomeViewModelFavoritesTest` (7: `noFavoritesGivesEmptySection`,
  `showsTenNewestFavoritesWithTotalCount`, `sectionIsTheSameForEveryCategory`,
  `favoritesUpdateWhenOneIsAddedOrRemoved`, `favoritesFollowLanguageChange`, `favoriteClickOpensFavorite`,
  `seeAllOpensFavorites`) and `HomeViewModelRemoveFavoriteTest` (6: `removeClickAsksToConfirm`,
  `secondRemoveClickKeepsFirstRequest`, `confirmRemovesFavoriteAndCloses`, `confirmTwiceWritesOnce`,
  `dismissKeepsFavorite`, `removingLastFavoriteHidesSection`).
- Also files: `home/component/FavoritesSection.kt` (new), `home/HomeScreen.kt`, `home/navigation/HomeNavigation.kt`,
  `titledetails/TitleDetailsContract.kt`, `TitleDetailsViewModel.kt`, `TitleDetailsScreen.kt`, `AppNavHost.kt`,
  `di/NativeAppModule.kt` (modify); strings `home_favorites_*`; `drawable/ic_chevron_right.xml`,
  `ic_favorite_remove.xml`; `test/.../titledetails/TitleDetailsViewModelFavoriteTest.kt` (modify)
- Also: ports of the cmp Home section (with hearts and the dialog), Title details confirmation (plan 03 Step 4) and
  shell wiring (`homeScreen(onTitleClick, onFavoriteClick, onSeeAllFavorites)`, `favoritesScreen(onBack,
  onTitleClick)`, `nativeAppModule` includes `favoritesModule`).
- Unit tests, Title details (`TitleDetailsViewModelFavoriteTest`): replace `secondClickRemovesFavorite` / `quickTapsEndOnLastTap`
  (or their native names) with `clickOnFavoriteAsksToConfirm`, `confirmRemovesFavorite`, `confirmTwiceWritesOnce`,
  `dismissKeepsFavorite`, `quickTapsOnFavoriteKeepOneConfirmation`.
- Compile: `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`
  (Home's state and screen change in the same step, so the new effects are handled and it compiles.)

### Step 4: Docs ✅
- Files: `native-android/README.md`, `CLAUDE.md`
- What: native-android screens now include My favorites and the Home favorites section (README "Screens" line,
  CLAUDE.md repository-layout line, which still says "a placeholder title details screen").
- Compile: none.

After the last step (in `native-android/`): `./gradlew :app:detekt :app:testDebugUnitTest`. No KMP module changes,
so no root checks are needed.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1, AC-2 | 3 | `HomeViewModelFavoritesTest` (`showsTenNewest…`, `sectionIsTheSame…`, `noFavorites…`); manual |
| AC-3 | 2, 3 | `seeAllOpensFavorites`, `listsAllFavoritesNewestFirst`; manual: badge plurals, back |
| AC-4 | 1, 2, 3 | `MetaLineTest`; manual: placeholder tile, hearts |
| AC-5 | 2, 3 | `favoriteClickOpensFavorite`, `titleClickOpensTitle`; manual |
| AC-6 | 1, 2, 3 | `removeClickAsksToConfirm` (Home, My favorites) |
| AC-7 | 3 | `clickOnFavoriteAsksToConfirm`, `favoriteClickStoresTrue` |
| AC-8 | 2, 3 | `confirmRemovesFavoriteAndCloses` (both), `confirmRemovesFavorite`, `removingLastFavoriteHidesSection` |
| AC-9 | 1, 2, 3 | `dismissKeepsFavorite` (all three); manual: outside tap, back |
| AC-10 | 2 | `removingLastFavoriteLeavesEmptyList`; manual: empty message |
| AC-11 | 2, 3 | Stored data via fakes; manual: airplane mode |
| AC-12 | 1, 2, 3 | `favoritesFollowLanguageChange`, `followsLanguageChange`; manual: en / pt-BR copy |
| AC-13 | 1, 2, 3 | Manual: TalkBack, touch targets |
| EC-1 | 2, 3 | `secondRemoveClickKeepsFirstRequest`, `confirmTwiceWritesOnce`, `quickTapsOnFavoriteKeepOneConfirmation` |
| EC-2 | — | Not met by decision (no guard, as in cmp) |
| EC-3 | 2, 3 | `favoritesUpdateWhenOneIsAddedOrRemoved`, `updatesWhenFavoriteRemoved` |
| EC-4 | 3 | `showsTenNewestFavoritesWithTotalCount` |
| EC-5 | 1, 2, 3 | Manual: truncation in row and card, full name in popup |
| EC-6 | 2, 3 | Manual: rotate with popup open (stays), process death (closed) |
| EC-7 | 2 | Manual: rotate on My favorites |
| EC-9 | 2, 3 | `favoritesFollowLanguageChange`, `followsLanguageChange` |
| EC-11 | 1, 2, 3 | Manual: largest font |

## Out of scope
- Native iOS → 05.
- Any shared-core or cmp change.

## Risks
- The app compiles only against a fresh `publishSharedCore`; a stale `build/maven-repo` misses
  `ObserveFavoriteTitles` and the fakes' new methods. Mitigation: publish before Step 1.
- Step 3 is the largest (Home and Title details together); it is one step so that every step compiles.
- Exact Figma sizes and assets are still pending (rate limit); values are the ≈ ones used in cmp.

## Deviations
- Ported mechanically from the merged cmp code: new files copied with packages and resources converted
  (`Res.*` → `R.*`, Compose Multiplatform resource functions → `androidx.compose.ui.res`, Koin
  `org.koin.androidx.compose.koinViewModel`), existing native files patched with the cmp diffs of plans 02 and 03;
  two rejected hunks (an import in `HomeScreen.kt`, `HomeNavigation.kt`) were applied by hand.
- `CLAUDE.md`'s native-android line now lists Home with a favorites section, My favorites and title details (it
  still said "a placeholder title details screen").
- `native-android/local.properties` (git-ignored) was copied into the worktree to build; not part of the change.
