# Plan: Title details on Android (native)

Requirement: [05 - Title details on Android (native)](../subtasks/05-native-android-title-details.md)
Status: Done

## Summary
Replace the placeholder title details screen of the standalone `native-android` app with the full screen. Port the
cmp screen (`cmp/feature/title-details/ui`), which is the behavioral reference, to Jetpack Compose with Android
resources. A `TitleDetailsViewModel` observes `TitleDetailsRepository` and `FavoritesRepository` from the published
shared core. Its contract is the same as in cmp: `DetailsLoad` Loading / Error / Loaded, `isRefreshing`,
`isFavorite`, intents and effects. The Home poster row, the retry message and the person icon move to
`designsystem/component`, as in cmp, so Home and details share them. No change to the shared core: favorites and
details are already published (`service-favorites-domain`, `service-title-domain`) and bound by `core-di`.

## Decisions
- Design Title details: used. It is linked without `(maybe)`, and every gap is settled by the cmp decisions below.
- **Same behavior as cmp, decided earlier with the user, kept as is:**
  - Release date by app language: `dd/MM/yyyy` in pt-BR and `MM/dd/yyyy` in en.
  - Runtime as "1h 53m" / "2h" / "45m".
  - Artwork: the poster, else the backdrop (`TitleDetails.artworkUrl`), else a `#303243` tile.
  - Cast: up to 10. A missing photo shows a `#303243` circle with the person icon.
  - Genre chips are as wide as their label and wrap (`FlowRow`).
  - "Voltar" sits on a `#303243` 65% pill, like the heart circle.
  - Loading: a centered `#FF1F8A` spinner. Error: the message and a "Tentar novamente" pill, centered.
  - Pull-to-refresh is offered only while details are shown, and is ignored while one is running.
  - A failed refresh keeps the cached details. Failures are tracked per language.
  - Automatic refresh runs only on open and on language change.
  - Heart:
    - off is `ic_favorite_button.svg`; on is the heart's outer path filled `#FF1F8A`.
    - 48dp touch target, toggle role, labelled "Favorite" / "Favorito".
    - The ViewModel shows the stored value. A tap calls `setFavorite(id, !current)`, with no pending state.
- **Shared UI moves to `designsystem/component`** (from user; rejected: import from `home/`, or a copy for details).
  The change mirrors cmp:
  - `PosterRow` takes `listState` and `contentPadding`. It no longer reads `home.ScreenPadding`.
  - `RetryMessage(message, retryLabel, onRetry, modifier, horizontalAlignment)` has the body of Home's
    `RowMessage`. Home keeps a small `RowMessage` that adds the row minimum height, the padding and its own string.
  - `PersonIcon(modifier)` is `ic_person` in white at 50%, used by Home's avatar and the cast placeholder.
- **Trailer: `ACTION_VIEW` on the `https://www.youtube.com/watch?v=<key>` URL** (from user; rejected: a
  `vnd.youtube:` intent first, then a fallback). The screen calls `LocalUriHandler.current.openUri(url)`, as cmp
  does. Android opens the YouTube app when it is installed, since that app handles youtube.com links, and the
  browser otherwise. `runCatching` around the call covers a device that can open neither (EC-3).
- **No back or open-once guard** (from user; rejected: `dropUnlessResumed`). "Voltar" and system back call
  `navController.navigateUp()`. Trailer and recommendation taps send one effect each, as in cmp and Home. EC-2 is
  not done, as on Home and in cmp; see Out of scope.
- **Route `TitleDetailsRoute(tmdbId: Long, type: String)`**, with `titleId` and `TitleId.toDetailsRoute()`, as in
  cmp. It has only primitive arguments, so the back stack survives process death. Home and recommendations both
  `navigate(title.id.toDetailsRoute())`, so the same title can be stacked more than once (EC-7).
- **ViewModel argument through Koin `parametersOf(titleId)`**, as in cmp: `viewModel { (id: TitleId) -> … }`. The
  title-details module is added to `nativeAppModule`. The alternative, reading the route from `SavedStateHandle`,
  needs Android `Bundle` code in unit tests.
- **Package `titledetails/`**, by screen, like `home/`: contract, ViewModel, screen and format at its root, then
  `component/`, `di/` and `navigation/`. `TitleDetailsPlaceholder.kt` and `res/drawable/ic_arrow_back.xml` (used
  only by the placeholder) are deleted.
- **Drawables copied from cmp:** `ic_back_arrow`, `ic_clock`, `ic_play_circle`, `ic_favorite_off` and
  `ic_favorite_on` are already VectorDrawable XML converted from `design/assets/`. They go to `res/drawable/`
  unchanged. The rating ring, divider, dot and fade are drawn in code, as in cmp.
- **Colors:** a `designsystem/DetailsColors` object would make one-off tokens public to Home. So, as in cmp, an
  `internal object DetailsColors` stays in `titledetails/`: Page `#21222E`, Track `#303243`, RingCenter
  `#15161D`, DateLine `#BBBBBB`, Synopsis `#CCCCCC`. Pink, purple and deep purple come from the theme
  (`secondary`, `primary`, `primaryContainer`).
- **Tests use MockK**, like Home's tests in this app: the `service/*/fake` modules are not published. A small
  `TitleDetailsTestRepositories` fixture holds `MutableStateFlow`s per language and records refreshes, in the same
  way as `HomeTestRepository`.
- **`kotlinx-datetime` comes from `service-title-domain`**, which exposes `LocalDate` as `api`. The app uses only
  `LocalDate.day`, `month.number` and `year` in the formatter. No new catalog entry.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Details data (observe, refresh) | `TitleDetailsRepository`, `TitleDetails`, `CastMember`, `Trailer` | `service-title-domain` (Maven) |
| Favorite state | `FavoritesRepository` | `service-favorites-domain` (Maven) |
| Koin bindings of both repositories | `initKoin` | `core-di` (Maven), started in `TmdbApplication` |
| Result type | `AppResult`, `AppError` | `core-common` (transitive) |
| Ids, title model, language | `TitleId`, `TitleType`, `Title`, `Language.fromLocaleTag` | `core-model` (transitive) |
| MVI shape, language intent, pull-to-refresh, effects | `HomeViewModel`, `HomeScreen`, `HomeContract` | `native-android/.../home/` |
| Poster row with placeholder tile and button semantics | `PosterRow` (moves to the designsystem) | `native-android/.../home/component/PosterRow.kt` |
| Retry pill | `RowMessage` body (moves as `RetryMessage`) | `native-android/.../home/component/RowMessage.kt` |
| Person icon | `ic_person` drawable | `native-android/app/src/main/res/drawable/ic_person.xml` |
| Theme colors (pink, purple) | `TmdbTheme` | `native-android/.../designsystem/Theme.kt` |
| Images | Coil 3 `AsyncImage` + `coil-network-okhttp` | already in `app/build.gradle.kts` |
| Main dispatcher in tests | `MainDispatcherRule` | `native-android/app/src/test/.../MainDispatcherRule.kt` |
| Screen to port (logic, layout, values) | `TitleDetailsViewModel`, `TitleDetailsScreen`, `component/*`, `TitleDetailsFormat` | `cmp/feature/title-details/ui` (copied, never depended on) |
| Vector drawables | `ic_back_arrow`, `ic_clock`, `ic_play_circle`, `ic_favorite_off`, `ic_favorite_on` | `cmp/feature/title-details/ui/src/commonMain/composeResources/drawable/` (copied) |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `native-android` build | `gradle/libs.versions.toml`, `app/build.gradle.kts` (modify) | Add `shared-core-favorites-domain` |
| `native-android/app` designsystem | `designsystem/component/PosterRow.kt`, `RetryMessage.kt`, `PersonIcon.kt` (new) | Shared by Home and details |
| `native-android/app` home | `home/HomeScreen.kt`, `home/component/RowMessage.kt`, `home/component/HomeHeader.kt` (modify); `home/component/PosterRow.kt` (delete) | Use the moved components |
| `native-android/app` titledetails | `titledetails/TitleDetailsContract.kt`, `TitleDetailsViewModel.kt`, `TitleDetailsFormat.kt`, `DetailsColors.kt`, `TitleDetailsScreen.kt`, `component/{DetailsTopBar,DetailsArtwork,DetailsHeader,RatingRing,TrailerButton,SectionHeading,CastRow,GenreChips}.kt`, `di/TitleDetailsModule.kt`, `navigation/{TitleDetailsRoute,TitleDetailsNavigation}.kt` (new) | The screen |
| `native-android/app` shell | `AppNavHost.kt`, `di/NativeAppModule.kt` (modify); `TitleDetailsPlaceholder.kt` (delete) | Wire the route; drop the placeholder |
| `native-android/app` res | `drawable/ic_back_arrow.xml`, `ic_clock.xml`, `ic_play_circle.xml`, `ic_favorite_off.xml`, `ic_favorite_on.xml` (new); `drawable/ic_arrow_back.xml` (delete); `values/strings.xml`, `values-pt-rBR/strings.xml` (modify) | Icons and copy |
| `native-android/app` tests | `test/.../titledetails/TitleDetailsTestRepositories.kt`, `TitleDetailsViewModelTest.kt`, `TitleDetailsViewModelStateTest.kt`, `TitleDetailsViewModelRefreshTest.kt`, `TitleDetailsViewModelFavoriteTest.kt`, `TitleDetailsFormatTest.kt`, `navigation/TitleDetailsRouteTest.kt` (new) | Unit tests |
| `native-android/README.md` | modify | The details screen is no longer a placeholder |
| Root `CLAUDE.md` | — | Not touched (out of this task's scope); the line "plus a placeholder title details screen" for native-android becomes stale and is listed as a follow-up |

## Steps
### Step 1: Favorites dependency and shared components  (AC-11, AC-14) ✅
- Files:
  - `gradle/libs.versions.toml` (modify): add
    `shared-core-favorites-domain = { module = "com.banhmilabs.tmdb:service-favorites-domain", version.ref = "shared-core" }`.
  - `app/build.gradle.kts` (modify): `implementation(libs.shared.core.favorites.domain)`.
  - `designsystem/component/PosterRow.kt` (new), moved from `home/component/PosterRow.kt` (deleted). The signature
    becomes `internal fun PosterRow(titles: List<Title>, onTitleClick: (Title) -> Unit, modifier: Modifier =
    Modifier, listState: LazyListState = rememberLazyListState(), contentPadding: PaddingValues = PaddingValues())`.
    Same poster, placeholder tile and semantics.
  - `designsystem/component/RetryMessage.kt` (new): `internal fun RetryMessage(message: String, retryLabel:
    String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier, horizontalAlignment: Alignment.Horizontal =
    Alignment.Start)`. It holds the message and pill styles of `RowMessage`, and center-aligns the text when
    centered.
  - `designsystem/component/PersonIcon.kt` (new): `internal fun PersonIcon(modifier: Modifier = Modifier)`. It
    draws `R.drawable.ic_person`, alpha 0.5, with no description.
  - `home/component/RowMessage.kt` (modify): keeps `RowMinHeight` and calls `RetryMessage(..., retryLabel =
    stringResource(R.string.home_retry))` inside `fillMaxWidth().heightIn(min = RowMinHeight).padding(horizontal =
    ScreenPadding)`.
  - `home/HomeScreen.kt` (modify): `PosterRow(..., listState = listState, contentPadding = PaddingValues(start =
    ScreenPadding))`.
  - `home/component/HomeHeader.kt` (modify): the avatar uses `PersonIcon`.
- Unit tests: none (no logic change; Home's ViewModel tests still cover Home).
- Compile: `cd native-android && ./gradlew :app:compileDebugKotlin` (after `./gradlew publishSharedCore` at the
  repo root).

### Step 2: Contract, format helpers, route  (AC-6, AC-7, AC-18, EC-4, EC-7, EC-8) ✅
- Files (all new, package `com.banhmilabs.tmdb.nativeapp.titledetails`):
  - `TitleDetailsContract.kt`, as in cmp:
    - `DetailsLoad { Loading; Error; Loaded(details) }`.
    - `TitleDetailsUiState(load = Loading, isRefreshing = false, isFavorite: Boolean? = null)`, with a `details`
      getter.
    - `TitleDetailsIntent`: `LanguageChanged(language)`, `BackClicked`, `TrailerClicked`,
      `RecommendationClicked(title)`, `Refresh`, `Retry`, `FavoriteClicked`.
    - `TitleDetailsEffect`: `NavigateBack`, `OpenTrailer(url)`, `OpenTitle(title)`.
  - `TitleDetailsFormat.kt`:
    - `formatReleaseDate(date: LocalDate, language: Language): String`.
    - `formatRuntime(minutes: Int): String`.
    - `nameWithYear(name: String, year: Int?): String`, with a non-breaking space before "(year)" (EC-4).
  - `navigation/TitleDetailsRoute.kt`: `@Serializable internal data class TitleDetailsRoute(val tmdbId: Long, val
    type: String) { val titleId: TitleId }` and `internal fun TitleId.toDetailsRoute()`.
- Unit tests:
  - `TitleDetailsFormatTest`: `portugueseDateIsDayMonthYear`, `englishDateIsMonthDayYear`, `datePadsDayAndMonth`,
    `runtimeWithHoursAndMinutes`, `runtimeWholeHours`, `runtimeUnderAnHour`, `nameWithYearAddsYear`,
    `nameWithYearKeepsYearAttached`, `nameWithoutYearIsName`.
  - `navigation/TitleDetailsRouteTest`: `routeRoundTripsTitleId` (movie and TV).
- Compile: `cd native-android && ./gradlew :app:compileDebugKotlin`

### Step 3: ViewModel and Koin  (AC-3, AC-4, AC-5, AC-11, AC-13–AC-18, EC-1, EC-9, EC-11, EC-13) ✅
- Files:
  - `TitleDetailsViewModel.kt` (new): a port of cmp's `TitleDetailsViewModel(titleId, repository: TitleDetailsRepository, favorites: FavoritesRepository)`.
    - `language: MutableStateFlow<Language?>`, `failed: MutableStateFlow<Set<Language>>`,
      `manualRefresh: MutableStateFlow<Boolean>`.
    - `state`: `language.filterNotNull().flatMapLatest { combine(observe(id, it), failed, manualRefresh) }`, then
      `.combine(favorites.observeIsFavorite(id))`, then `stateIn(WhileSubscribed(5_000))`.
    - `init`: one non-forced refresh per distinct language.
    - `onIntent`:
      - `Refresh` is ignored while a manual refresh runs (EC-11). `Retry` forces a refresh.
      - `TrailerClicked` sends `OpenTrailer(trailer.url)` only when there is a trailer.
      - `RecommendationClicked` sends `OpenTitle`. `BackClicked` sends `NavigateBack`.
      - `FavoriteClicked` launches `setFavorite(id, !current)` when `isFavorite` is known.
    - Effects come from a `Channel(BUFFERED)` exposed with `receiveAsFlow()`.
  - `di/TitleDetailsModule.kt` (new): `internal val titleDetailsModule = module { viewModel { (titleId: TitleId) ->
    TitleDetailsViewModel(titleId, get(), get()) } }`.
  - `di/NativeAppModule.kt` (modify): `includes(onboardingModule, homeModule, titleDetailsModule)`.
- Unit tests (`test/.../titledetails/`, `MainDispatcherRule`, MockK):
  - `TitleDetailsTestRepositories`: MockK `TitleDetailsRepository` with a `MutableStateFlow<TitleDetails?>` per
    language. Refresh stores the remote details unless `failNextRefresh(error)` was called, and can be made to
    suspend so a running refresh can be tested. Every `(language, force)` call is recorded. A MockK
    `FavoritesRepository` is backed by a `MutableStateFlow<Boolean>` and records the values of `setFavorite`. It
    also holds a `testDetails(...)` builder and a `start(viewModel, language)` helper.
  - `TitleDetailsViewModelTest`: `nothingObservedBeforeLanguage`, `showsDetailsAfterLanguage`,
    `refreshesOncePerLanguage`, `languageChangeShowsOtherLanguage`, `trailerClickOpensTrailerUrl`,
    `trailerClickWithoutTrailerDoesNothing`, `recommendationClickOpensTitle`, `backClickNavigatesBack`.
  - `TitleDetailsViewModelStateTest`: `startsLoading`, `failedLoadWithoutCacheShowsError`,
    `missingTitleShowsError` (NotFound), `retryLoadsAgain`, `failedRefreshKeepsCachedDetails`,
    `errorInOneLanguageIsNotShownInTheOther`.
  - `TitleDetailsViewModelRefreshTest`: `loadingUntilFirstRefreshEnds`, `pullToRefreshForcesAndEndsIndicator`,
    `pullWhileRefreshingIsIgnored`, `failedPullKeepsDetails`, `pullBeforeLanguageDoesNothing`.
  - `TitleDetailsViewModelFavoriteTest`: `favoriteIsShownFromStorage`, `favoriteClickStoresTrue`,
    `secondClickRemovesFavorite`, `quickTapsEndOnLastTap`, `favoriteUnchangedByLanguage`,
    `clickBeforeFavoriteKnownDoesNothing`.
- Compile: `cd native-android && ./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`

### Step 4: Drawables and strings  (AC-1, AC-3, AC-5, AC-6, AC-8, AC-14, AC-18) ✅
- Files:
  - Copy into `app/src/main/res/drawable/`, unchanged: `ic_back_arrow.xml`, `ic_clock.xml`, `ic_play_circle.xml`,
    `ic_favorite_off.xml` and `ic_favorite_on.xml` from `cmp/feature/title-details/ui/.../composeResources/drawable/`.
    They are VectorDrawables made from `design/assets/*.svg`; `ic_favorite_on` is the heart's outer path filled
    `#FF1F8A`.
  - `values/strings.xml`, `values-pt-rBR/strings.xml` (modify): keep `details_back`. Add `details_watch_trailer`,
    `details_cast`, `details_genres`, `details_recommendations`, `details_no_synopsis`, `details_not_rated`,
    `details_not_rated_description`, `details_rating_description` (`%1$d%%`), `details_error`, `details_retry` and
    `details_favorite`, with the cmp copy in en and pt-BR.
- Unit tests: none.
- Compile: `cd native-android && ./gradlew :app:compileDebugKotlin`

### Step 5: Components  (AC-1, AC-3, AC-5–AC-10, AC-12, EC-4–EC-6, EC-12) ✅
- Files (new, `titledetails/component/`), ported from cmp with `R.drawable` / `R.string` in place of `Res`:
  - `DetailsTopBar(onBack, isFavorite: Boolean?, onFavoriteClick)`:
    - The bar is fixed below the status bar (safe-drawing top + horizontal), with a minimum height of 48dp.
    - "Voltar" is a pill: 8dp start, 10dp padding, 24dp arrow, 6dp gap, Inter Bold 16sp label, `#303243` 65%, role
      Button.
    - The heart is shown only when `isFavorite != null`. It is 32dp with a 48dp target, 22dp from the end, and
      uses `toggleable(role = Switch)` with the `details_favorite` description.
  - `DetailsArtwork(url: String?)`: 493dp high, `#303243` behind a cropped `AsyncImage`, no description. The fade
    is transparent until 260/493, then reaches `#21222E` at 412/493.
  - `RatingRing(percent: Int?)`:
    - A 57dp Canvas: `#303243` track, `#15161D` 45dp center, and a `#FF1F8A` 6dp round-cap arc from -90°, 3.6° per
      percent.
    - The value is drawn at 15dp (so it doesn't grow with font size): bold value plus regular "%", or "NR".
    - `clearAndSetSemantics` with the description "Rating N%" or "Not rated yet".
  - `DetailsHeader(details, language)`:
    - Ring, a 16dp gap, then a column with the name and year (Inter Bold 22sp, heading semantics) and the date
      line 5dp below.
    - The date line: date, 3dp, a 4dp `#BBBBBB` dot, 6dp, a 19dp clock, 6dp, runtime (16sp `#BBBBBB`). A part
      that is missing is left out with its separator.
  - `TrailerButton(onClick)`: full width, at least 44dp high, 60dp pill, linear gradient from `primary` to
    `primaryContainer` (top-left to bottom-right), 20dp play icon, 10dp gap, SemiBold 16sp label, role Button.
  - `SectionHeading(text)`: Bold 18sp, white, one line, heading semantics.
  - `CastRow(cast, startPadding, listState)`:
    - A `LazyRow` keyed by `"$index-$name"`, 4dp spacing.
    - Each item is a 75dp column: a 60dp photo with a 4dp `#303243` border; `PersonIcon` at 30dp when there is no
      photo; the name 3dp below, Bold 12sp, centered, at most 2 lines with ellipsis.
    - Descendants are merged, and the item is not clickable.
  - `GenreChips(genres)`: a `FlowRow` with 14dp / 10dp spacing. Each chip is at least 31dp high, a 34dp pill,
    `#303243`, with 17.5dp horizontal padding and a Medium 15sp label.
  - `DetailsColors.kt` (new, `titledetails/`): the colors listed in Decisions.
- Unit tests: none (UI; checked manually).
- Compile: `cd native-android && ./gradlew :app:compileDebugKotlin`

### Step 6: Screen and navigation  (AC-1, AC-2, AC-4, AC-5, AC-11, AC-13–AC-18, EC-3, EC-5, EC-7, EC-8, EC-9, EC-13) ✅
- Files:
  - `TitleDetailsScreen.kt` (new):
    - `TitleDetailsScreen(titleId, onBack, onTitleClick, viewModel = koinViewModel { parametersOf(titleId) })`:
      - It sends `LanguageChanged(Language.fromLocaleTag(Locale.current.toLanguageTag()))` on each language.
      - It collects effects. `NavigateBack` calls `onBack`. `OpenTitle` calls `onTitleClick`. `OpenTrailer`
        calls `runCatching { uriHandler.openUri(url) }` (EC-3).
    - `TitleDetailsContent(state, language, onIntent)`:
      - Scroll states are hoisted and saveable above the states: `rememberScrollState()`, and the cast and
        recommendations `LazyListState` with `rememberSaveable(saver = LazyListState.Saver)` (EC-8).
      - The background is `#21222E`.
      - Loading shows a centered `secondary` spinner. Error shows a centered `RetryMessage` that sends `Retry`.
      - Loaded shows a `PullToRefreshBox` with a top-inset `secondary` indicator around a `verticalScroll` `Box`.
        The box holds `DetailsArtwork`, then a column starting 376dp from the top (safe-drawing horizontal +
        bottom inset, 40dp bottom padding).
      - `DetailsTopBar` is drawn last. It gets `isFavorite` only when Loaded (AC-4).
    - Body:
      - Header, 21dp, divider (black 48%), 20dp, then the synopsis (14sp, 17.5sp line height, `#CCCCCC`) or
        `details_no_synopsis`.
      - When there is a trailer: 30dp, then the trailer button.
      - When there is cast: 46dp, heading, 16dp, cast row.
      - When there are genres: 41dp, heading, 20dp, chips.
      - When there are recommendations: 41dp, heading, 20dp, then `PosterRow` with a 30dp start padding.
  - `navigation/TitleDetailsNavigation.kt` (new): `internal fun NavGraphBuilder.titleDetailsScreen(onBack: () ->
    Unit, onTitleClick: (Title) -> Unit)` uses `composable<TitleDetailsRoute>` and `toRoute()`.
  - `AppNavHost.kt` (modify):
    - Define `val openTitle: (Title) -> Unit = { navController.navigate(it.id.toDetailsRoute()) }`.
    - Pass it to `homeScreen(onTitleClick = openTitle)`, and add
      `titleDetailsScreen(onBack = navController::navigateUp, onTitleClick = openTitle)`.
  - Delete `TitleDetailsPlaceholder.kt` and `res/drawable/ic_arrow_back.xml`.
  - `native-android/README.md` (modify): mention the screens (onboarding, Home, Title details) and that favorites
    come from `service-favorites-domain`.
- Unit tests: none new (the ViewModel and route tests cover the logic).
- Compile: `cd native-android && ./gradlew :app:compileDebugKotlin`
- Final checks: at the repo root `./gradlew publishSharedCore`, then in `native-android/` run
  `./gradlew :app:detekt :app:testDebugUnitTest` (add `--auto-correct` to detekt first if ktlint fails).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 Layout | 4, 5, 6 | Manual: compare with the design and the cmp app |
| AC-2 Back | 3, 6 | `backClickNavigatesBack`; manual: Home tab and row positions kept, system back |
| AC-3 Favorite | 3, 4, 5 | `favoriteIsShownFromStorage`, `favoriteClickStoresTrue`, `secondClickRemovesFavorite`, `favoriteUnchangedByLanguage`; manual: restart, offline, TalkBack toggle |
| AC-4 Heart only with details | 3, 6 | `clickBeforeFavoriteKnownDoesNothing`; manual: no heart in Loading / Error |
| AC-5 Trailer | 3, 6 | `trailerClickOpensTrailerUrl`, `trailerClickWithoutTrailerDoesNothing`; manual: YouTube app / browser (English fallback chosen by the shared core) |
| AC-6 Rating | 5 | Manual: "45%" ring, "NR", TalkBack "Not rated yet" |
| AC-7 Name, year, date, runtime | 2, 5 | `TitleDetailsFormatTest` (all cases); manual: missing parts and separators |
| AC-8 Synopsis | 4, 6 | Manual: missing synopsis text |
| AC-9 Cast | 5, 6 | Manual: up to 10, placeholder, not clickable, section hidden when empty |
| AC-10 Genres | 5, 6 | Manual: chips, not clickable, section hidden when empty |
| AC-11 Recommendations | 1, 3, 6 | `recommendationClickOpensTitle`; manual: placeholder tile, TalkBack button, section hidden |
| AC-12 Artwork | 5 | Manual: poster / backdrop / placeholder, decorative |
| AC-13 First load | 3, 6 | `startsLoading`, `loadingUntilFirstRefreshEnds` |
| AC-14 Error without cache | 1, 3, 6 | `failedLoadWithoutCacheShowsError`, `retryLoadsAgain` |
| AC-15 Offline with cache | 3 | `failedRefreshKeepsCachedDetails`, `failedPullKeepsDetails` |
| AC-16 Freshness | 3 | `refreshesOncePerLanguage` (non-forced refresh on open; the 24 h TTL is in the shared core) |
| AC-17 Pull-to-refresh | 3, 6 | `pullToRefreshForcesAndEndsIndicator`, `failedPullKeepsDetails`, `pullBeforeLanguageDoesNothing` |
| AC-18 Language | 2, 3, 4 | `languageChangeShowsOtherLanguage`, `showsDetailsAfterLanguage`; manual: pt-BR / en copy |
| EC-1 Quick heart taps | 3 | `quickTapsEndOnLastTap` |
| EC-2 Open once | — | Not done (see Out of scope) |
| EC-3 Nothing opens the trailer | 6 | Manual (`runCatching` around `openUri`) |
| EC-4 Long name | 2, 5 | `nameWithYearKeepsYearAttached`; manual |
| EC-5 Long synopsis | 6 | Manual: full text, screen scrolls |
| EC-6 Long cast name | 5 | Manual: 2 lines, ellipsis |
| EC-7 Stacked details | 2, 6 | `routeRoundTripsTitleId`; manual: each "Voltar" pops one |
| EC-8 Rotation / process death | 2, 6 | `routeRoundTripsTitleId`; manual: rotate, "Don't keep activities" |
| EC-9 Language change | 3 | `languageChangeShowsOtherLanguage`, `errorInOneLanguageIsNotShownInTheOther`, `favoriteUnchangedByLanguage` |
| EC-10 Other language | 2, 3 | Covered by `Language.fromLocaleTag` (shared core); manual: French device |
| EC-11 Pull while refreshing | 3 | `pullWhileRefreshingIsIgnored` |
| EC-12 Largest font | 5, 6 | Manual: minimum heights, ring value in dp |
| EC-13 Title no longer on TMDB | 3, 6 | `missingTitleShowsError`; manual: back works |

## Out of scope
- **EC-2 (open once on repeated taps)**: not done, as on Home and in cmp (user's choice). Each tap on "Assistir
  trailer" or a recommendation opens it. The same goes for a quick double tap on "Voltar".
- **native-ios** (subtask 06, another session).
- **Shared core changes**: none needed. `core/`, `service/` and `cmp/` are untouched.
- **Root `CLAUDE.md`**: it still says native-android has a placeholder title details screen. It is outside this
  task's allowed files and is left for a follow-up.
- **Compose UI tests or screenshot tests**: the app has none; the UI is checked manually.

## Risks
- **The published core is older than `main`** — `build/maven-repo` is missing in this worktree. Run
  `./gradlew publishSharedCore` at the repo root before the first compile; it builds every target, so iOS klibs
  need Xcode.
- **The transitive `kotlinx-datetime`** — it reaches the app only through `service-title-domain`'s `api`. If it
  doesn't resolve, add it to the catalog with the version the shared core uses.
- **The YouTube app may not open `https` links** on some devices, if the user turned off its link handling. The
  browser then opens the trailer, which AC-5 allows.
- **Manual checks not run here**: device behavior (layout, TalkBack, rotation, process death, offline, fonts,
  YouTube) needs an emulator; it is reported as not verified.

## Deviations
- `values/strings.xml`: `details_error` uses `\'` instead of cmp's `&apos;`, which Android resources reject
  for an apostrophe.
- Tests collect effects into a list in `backgroundScope` instead of using Turbine, which this app doesn't have.
- `TitleDetailsTestRepository` fails a refresh with `NotFound` when `remoteDetails` is `null`, which covers
  `missingTitleShowsError` without a second repository.
