# Plan: Home on Android (native)

Requirement: [06 - Home on Android (native)](../subtasks/06-native-android-home.md)
Status: Done

## Summary
The standalone `native-android/` app gets a `home/` package that mirrors the cmp Home (`cmp/feature/home/ui`,
plans 04 and 05) in Jetpack Compose: an MVI contract with per-row `RowState`, a `HomeViewModel` on
`TitleListsRepository`, a stateful `HomeScreen` and a stateless `HomeContent`, plus `component/`, `navigation/`
and `di/`. It replaces the app's placeholder Home and adds a placeholder details screen.

The shared core is used only through the published libraries: `service-title-domain` is added next to the existing
`core-di` and `service-settings-domain`. No change to `core/` or `service/`.

## Decisions
- **Same behaviour as the cmp Home** (plans 04 and 05, including their deviations):
  - `RowState` `Loading` / `Error` / `Titles`; cached titles always win over a failed refresh (AC-7).
  - Failed refreshes are tracked per (category, list type, language) and read only for the current selection
    (EC-5, EC-7).
  - Showing a category or language refreshes both lists with `force = false` (AC-9); pull-to-refresh and retry use
    `force = true`; a second pull while one is running is ignored (EC-9).
  - The selected tab is kept in `SavedStateHandle` under `"selectedCategory"` (EC-6).
  - Row scroll: one `rememberSaveable(category, saver = LazyListState.Saver)` per row, hoisted above the row's
    `when`, so it survives details, rotation and process death; a tab switched to starts at the beginning.
  - Category spinner when both rows are `Loading`; a 24dp row spinner when only one is.
  - Custom pull-to-refresh indicator below the status bar, in `secondary`.
  - Layout values, spacing (including the 48dp tab row with 24dp / 30dp spacing), text styles, accessibility and
    copy are taken from the cmp code and design/home.md.
- **Images: Coil 3 with `coil-network-okhttp`** (from user). Rejected: `coil-network-ktor3` like cmp, which needs
  an explicit Ktor engine and a singleton `ImageLoader`. OkHttp is the Android default; the network fetcher
  registers itself through the service loader, so no `ImageLoader` setup is needed. Version: `coil = "3.6.3"`,
  as in the root catalog.
- **ViewModel tests use MockK** (from user). `FakeTitleListsRepository` is not published, and publishing it would
  change the shared core. The mock keeps one `MutableStateFlow<List<Title>?>` per (category, list type, language);
  `refresh` coAnswers by writing the configured titles or returning `AppResult.Failure`. Requests are checked
  with `coVerify`. Rejected: publishing `service-title-fake`.
- **EC-1: no guard, as in cmp** (from user). A quick double tap may open details twice. Rejected: the `RESUMED`
  check that the user removed from cmp.
- **Language**: `HomeScreen` computes `Language.fromLocaleTag(androidx.compose.ui.text.intl.Locale.current.toLanguageTag())`
  and sends `LanguageChanged` from `LaunchedEffect(language)`; Android resources pick the copy (`values` /
  `values-pt-rBR`) from the same locale (AC-10, EC-7, EC-8).
- **Package layout** follows onboarding in this app: `nativeapp/home/` with `HomeContract.kt`, `HomeViewModel.kt`,
  `HomeScreen.kt`, `component/`, `navigation/`, `di/`. `HomeRoute` moves from the app root into
  `home/navigation/`. The placeholder details screen sits in the app root (`TitleDetailsPlaceholder.kt`), as in
  cmp's shell.
- **Theme tokens** in `designsystem/Theme.kt`, as in cmp: `secondary = #FF1F8A`, `onSecondary = White`,
  `surfaceVariant = #36076B`, `onSurfaceVariant = White`.
- **Assets are copied from cmp** as Android drawables (same vector XML format): `home_background.xml`,
  `home_header_glow.xml`, `ic_search.xml`, `ic_person.xml` from `cmp/feature/home/ui/.../composeResources/drawable/`
  and `ic_arrow_back.xml` from `cmp/composeApp/.../composeResources/drawable/`.
- **Strings** use the cmp names (`home_*`, `details_back`) in `res/values/strings.xml` and
  `res/values-pt-rBR/strings.xml`.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Behaviour, layout and components to mirror | cmp Home | `cmp/feature/home/ui/` |
| Placeholder details to mirror | `TitleDetailsPlaceholderScreen` | `cmp/composeApp/.../TitleDetailsPlaceholder.kt` |
| Title lists | `TitleListsRepository`, `TitleCategory`, `TitleListType` | published `service-title-domain` |
| Title model, language mapping | `Title`, `TitleId`, `Language.fromLocaleTag` | published `core-model` (via `service-title-domain`) |
| Result type | `AppResult`, `AppError` | published `core-common` |
| Koin start-up with the title repository | `initKoin` | published `core-di` |
| Feature shape in this app (contract, ViewModel, screen, component/, navigation/, di/) | onboarding | `native-android/app/.../nativeapp/onboarding/` |
| Theme | `TmdbTheme` | `native-android/app/.../designsystem/Theme.kt` |
| Main dispatcher in tests | `MainDispatcherRule` | `native-android/app/src/test/.../MainDispatcherRule.kt` |
| Pull-to-refresh, spinner, touch target | `PullToRefreshBox`, `CircularProgressIndicator`, `minimumInteractiveComponentSize` | `compose-material3` (already a dependency) |
| Vector assets | `home_background`, `home_header_glow`, `ic_search`, `ic_person`, `ic_arrow_back` | `cmp/**/composeResources/drawable/` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `native-android` | `gradle/libs.versions.toml`, `app/build.gradle.kts` (modify) | `service-title-domain`, Coil |
| `native-android/app` | `designsystem/Theme.kt` (modify) | `secondary`, `surfaceVariant` tokens |
| `native-android/app` | `home/HomeContract.kt`, `home/HomeViewModel.kt`, `home/di/HomeModule.kt` (new); `di/NativeAppModule.kt` (modify) | Home state holder |
| `native-android/app` | `res/drawable/{home_background,home_header_glow,ic_search,ic_person,ic_arrow_back}.xml` (new); `res/values*/strings.xml` (modify) | Assets and copy |
| `native-android/app` | `home/HomeScreen.kt`, `home/component/{HomeHeader,SearchField,CategoryTabs,PosterRow,RowMessage}.kt`, `home/navigation/{HomeRoute,HomeNavigation}.kt` (new) | Home UI |
| `native-android/app` | `TitleDetailsPlaceholder.kt` (new); `AppNavHost.kt`, `App.kt` (modify); `HomeRoute.kt`, `PlaceholderHomeScreen.kt` (delete) | Wiring, placeholder details |
| `native-android/app` tests | `home/HomeTestFixtures.kt`, `HomeViewModelTest.kt`, `HomeViewModelSelectionTest.kt`, `HomeViewModelRefreshTest.kt` (new) | ViewModel tests |
| docs | `CLAUDE.md`, `native-android/README.md` (modify) | Current state |

## Steps
Compile command for every step, from `native-android/` after `./gradlew publishSharedCore` at the repo root:
`./gradlew :app:compileDebugKotlin` (and `:app:compileDebugUnitTestKotlin` when tests change).

### Step 1: Dependencies and theme tokens ✅
- Files: `native-android/gradle/libs.versions.toml`, `native-android/app/build.gradle.kts`,
  `app/src/main/kotlin/com/banhmilabs/tmdb/nativeapp/designsystem/Theme.kt` (modify)
- What:
  - Catalog: `shared-core-title-domain = { module = "com.banhmilabs.tmdb:service-title-domain", version.ref = "shared-core" }`;
    `coil = "3.6.3"`; `coil-compose` (`io.coil-kt.coil3:coil-compose`), `coil-network-okhttp`
    (`io.coil-kt.coil3:coil-network-okhttp`).
  - App: `implementation(libs.shared.core.title.domain)`, `implementation(libs.coil.compose)`,
    `implementation(libs.coil.network.okhttp)`.
  - Theme: `Pink = Color(0xFFFF1F8A)`, `Plum = Color(0xFF36076B)`; `secondary = Pink`, `onSecondary = White`,
    `surfaceVariant = Plum`, `onSurfaceVariant = White`.
- Unit tests: none.
- Compile: `./gradlew :app:compileDebugKotlin`

### Step 2: Contract, ViewModel, DI  (AC-2, AC-3, AC-6, AC-7, AC-8, AC-9, AC-10, EC-3, EC-4, EC-5, EC-6, EC-7, EC-9) ✅
- Files: `app/src/main/kotlin/com/banhmilabs/tmdb/nativeapp/home/HomeContract.kt`, `HomeViewModel.kt`,
  `home/di/HomeModule.kt` (new); `di/NativeAppModule.kt` (modify); tests under
  `app/src/test/kotlin/com/banhmilabs/tmdb/nativeapp/home/` (new)
- What:
  - Contract, the same as cmp: `RowState` (`Loading`, `Error`, `Titles(titles)`), `HomeUiState(selectedCategory =
    MOVIES, featured = Loading, popular = Loading, isRefreshing = false)`, `HomeIntent` (`LanguageChanged`,
    `CategorySelected`, `TitleClicked`, `Refresh`, `Retry(listType)`), `HomeEffect.OpenTitle(title)`.
  - `HomeViewModel(repository: TitleListsRepository, savedStateHandle: SavedStateHandle)`: the cmp ViewModel's
    logic (selection flow, `failed` set of list keys, `manualRefresh`, refresh on selection, `refreshAll`, `retry`,
    `SELECTED_CATEGORY_KEY`).
  - `homeModule = module { viewModelOf(::HomeViewModel) }`, included in `nativeAppModule`.
- Unit tests (MockK repository, `MainDispatcherRule`):
  - `HomeTestFixtures.kt`: test titles (Movies and Anime lists distinct); `HomeTestRepository` wrapper holding the
    MockK `TitleListsRepository`, per-key `MutableStateFlow`s, `setRemoteTitles(category, listType, titles)`,
    `failNextRefresh()`/failing keys; `TestScope.start(viewModel, language)`.
  - `HomeViewModelTest`: `startsLoadingWithMoviesSelected`, `rowsShowTitlesOnceLoaded`, `emptyListShowsEmptyRow`,
    `failedLoadWithoutCacheShowsError`, `oneRowFailsWhileOtherShowsTitles`, `failedRefreshWithCacheKeepsTitles`,
    `retryReloadsTheFailedRow`, `titleClickedOpensThatTitle`.
  - `HomeViewModelSelectionTest`: `tabSwitchRefreshesWithoutForce`, `failureForPreviousTabDoesNotMarkNewTab`,
    `languageChangeShowsNewLanguageOnly`, `listsAreLoadedInTheGivenLanguage`,
    `selectedCategoryIsRestoredFromSavedState`, `selectingCategorySavesIt`.
  - `HomeViewModelRefreshTest`: `pullToRefreshForcesBothListsAndEndsIndicator`, `pullWhileRefreshingIsIgnored`
    (a refresh that suspends until the test releases it).
- Compile: `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`

### Step 3: Resources and assets  (AC-1, AC-8, AC-10, EC-3, EC-8) ✅
- Files: `app/src/main/res/drawable/home_background.xml`, `home_header_glow.xml`, `ic_search.xml`, `ic_person.xml`,
  `ic_arrow_back.xml` (new, copied from cmp); `res/values/strings.xml`, `res/values-pt-rBR/strings.xml` (modify)
- What: strings `home_title`, `home_search_hint`, `home_popular_heading`, `home_category_{movies,series,anime,novelas,documentaries}`,
  `home_row_empty`, `home_row_error`, `home_retry`, `details_back`, with the en / pt-BR copy of design/home.md
  (the apostrophe in "Couldn't" escaped for Android resources).
- Unit tests: none.
- Compile: `./gradlew :app:compileDebugKotlin`

### Step 4: Home screen and components  (AC-1, AC-2, AC-4, AC-5, AC-6, AC-8, AC-9, AC-11, EC-2, EC-6, EC-10) ✅
- Files: `home/HomeScreen.kt`, `home/component/HomeHeader.kt`, `SearchField.kt`, `CategoryTabs.kt`, `PosterRow.kt`,
  `RowMessage.kt`, `home/navigation/HomeRoute.kt`, `HomeNavigation.kt` (new)
- What: a port of the cmp files with `R.string` / `R.drawable` through `stringResource` / `painterResource`
  (`androidx.compose.ui.res`) and `koinViewModel()` from `org.koin.androidx.compose`:
  - `HomeScreen(onTitleClick, viewModel = koinViewModel())`: language effect, effect collector, `HomeContent`.
  - `HomeContent`: background image, `PullToRefreshBox` with the custom indicator, vertical scroll with the header
    glow, header, search field, tabs, `HomeRows` (category spinner, `TitleRow` per row with hoisted saveable
    `LazyListState`).
  - Components as in cmp: inert avatar and search field with `clearAndSetSemantics {}`; tabs with
    `selectableGroup`, `selectable(role = Role.Tab)`, 48dp minimum; poster with merged semantics,
    `Role.Button`, placeholder name 4 lines + ellipsis, `AsyncImage` crop; `RowMessage` with the retry pill.
  - `@Serializable internal data object HomeRoute`; `NavGraphBuilder.homeScreen(onTitleClick: (Title) -> Unit)`.
- Unit tests: none (Compose UI; manual, How to test in the subtask).
- Compile: `./gradlew :app:compileDebugKotlin`

### Step 5: Placeholder details and app wiring  (AC-1, AC-3) ✅
- Files: `TitleDetailsPlaceholder.kt` (new); `AppNavHost.kt`, `App.kt` (modify); `HomeRoute.kt`,
  `PlaceholderHomeScreen.kt` (delete)
- What:
  - `@Serializable internal data class TitleDetailsPlaceholderRoute(val name: String)` and
    `TitleDetailsPlaceholderScreen(name, onBack)`, as in cmp (back `IconButton` with `ic_arrow_back` and
    `details_back`, the name centred 26sp bold, safe-drawing insets).
  - `AppNavHost`: `homeScreen(onTitleClick = { navController.navigate(TitleDetailsPlaceholderRoute(it.name)) })`,
    `composable<TitleDetailsPlaceholderRoute> { TitleDetailsPlaceholderScreen(it.toRoute<…>().name, navController::navigateUp) }`.
  - `App.kt` imports `HomeRoute` from `home.navigation`.
- Unit tests: `AppViewModelTest`, `OnboardingViewModelTest` unchanged and still pass.
- Compile: `./gradlew :app:compileDebugKotlin`

### Step 6: Docs ✅
- Files: `CLAUDE.md`, `native-android/README.md` (modify)
- What: current state mentions the native-android Home; the app uses `service-title-domain` and Coil
  (`coil-network-okhttp`).
- Unit tests: none.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 Home layout | 3, 4, 5 | Manual: How to test 1 |
| AC-2 Switching category | 2, 4 | `tabSwitchRefreshesWithoutForce`, `rowsShowTitlesOnceLoaded`; manual 2 |
| AC-3 Opening a title, back keeps tab and scroll | 2, 4, 5 | `titleClickedOpensThatTitle`, `selectedCategoryIsRestoredFromSavedState`; manual 2 |
| AC-4 Search field inert | 4 | Manual 3 (TalkBack skips it) |
| AC-5 Avatar inert | 4 | Manual 3 |
| AC-6 First load spinner | 2, 4 | `startsLoadingWithMoviesSelected`; manual 4 |
| AC-7 Offline with cache | 2 | `failedRefreshWithCacheKeepsTitles`; manual 4 |
| AC-8 Error and retry | 2, 3, 4 | `failedLoadWithoutCacheShowsError`, `retryReloadsTheFailedRow`; manual 4 |
| AC-9 Freshness and pull-to-refresh | 2, 4 | `tabSwitchRefreshesWithoutForce`, `pullToRefreshForcesBothListsAndEndsIndicator`; manual 4 |
| AC-10 Language | 2, 3, 4 | `listsAreLoadedInTheGivenLanguage`; manual 5 |
| AC-11 Poster without image, announced as button | 4 | Manual 4 (TalkBack) |
| EC-1 Repeated taps | — (no guard, as in cmp; from user) | — |
| EC-2 Long name truncated | 4 | Manual |
| EC-3 Empty row | 2, 3, 4 | `emptyListShowsEmptyRow` |
| EC-4 One row fails | 2, 4 | `oneRowFailsWhileOtherShowsTitles` |
| EC-5 Tab switch during load | 2 | `failureForPreviousTabDoesNotMarkNewTab` |
| EC-6 Rotation and process death | 2, 4 | `selectedCategoryIsRestoredFromSavedState`, `selectingCategorySavesIt`; manual ("Don't keep activities") |
| EC-7 Language change while running | 2 | `languageChangeShowsNewLanguageOnly` |
| EC-8 Other language | 3, 4 | `Language.fromLocaleTag` (core:model tests); manual 5 |
| EC-9 Pull while refreshing | 2 | `pullWhileRefreshingIsIgnored` |
| EC-10 Largest font | 4 | Manual |

## Out of scope
- The iOS native app → 07-native-ios-home (in progress in another session).
- The real details screen and Search → separate features.
- A guard against double taps (EC-1), as in cmp.
- Sharing Compose UI between `cmp/` and `native-android/` (forbidden: `native-*` never depends on `cmp/`).

## Risks
- **Coil 3.6.3 and the app's Compose BOM 2026.09.00 / OkHttp version.** Mitigation: compile in Step 1; if Coil's
  OkHttp clashes with the one from `core-network`, Gradle picks the higher; record a deviation if a different
  version is needed.
- **`service-title-domain` not published in this worktree.** Mitigation: run `./gradlew publishSharedCore` at the
  repo root before Step 1's compile.
- **Real data needs a token.** Without `TMDB_ACCESS_TOKEN` in `secrets.properties` at publish time, rows show the
  error state. Mitigation: publish with the token for the manual check.

## Deviations
- **Test repository is a small wrapper around the MockK mock** (`HomeTestRepository` in `HomeTestFixtures.kt`): it
  holds a `MutableStateFlow` per list and language, records refresh calls in `refreshRequests` and has
  `failNextRefresh(error)`, so the test bodies match cmp's. The Movies and Anime titles are fixed in it rather than
  set with `setRemoteTitles`.
- **Tests use `MainDispatcherRule(UnconfinedTestDispatcher())`** in place of cmp's `MainDispatcherTest` (not
  published), and `HomeViewModelRefreshTest` uses a `StandardTestDispatcher`. Turbine is not in this app, so
  `titleClickedOpensThatTitle` reads `effects.first()`, as `OnboardingViewModelTest` does.
- **`HomeViewModelSelectionTest` has one extra test**, `listsAreLoadedInTheGivenLanguage` (6 in all).
- **`native-android/README.md` is unchanged**: its workflow and checks did not change; `CLAUDE.md` describes the
  new screens.
- **Not checked by hand**: no emulator was running, so the app was built (`:app:assembleDebug`) but not installed
  or run. The manual How to test steps are still open.
