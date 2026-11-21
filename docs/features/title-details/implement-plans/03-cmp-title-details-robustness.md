# Plan: Title details robustness (cmp)

Requirement: [03 - Title details robustness (cmp)](../subtasks/03-cmp-title-details-robustness.md)
Status: Done

## Summary
Give the cmp Title details screen the same robustness Home got in its robustness subtask: the ViewModel tracks
failed loads per language and a manual refresh, and the state becomes Loading / Error / Loaded plus `isRefreshing`.
The screen draws a spinner, an error message with "Tentar novamente", and wraps the content in Material 3
`PullToRefreshBox`. Home's row message moves to `cmp/designsystem` so both screens share the retry pill. Scroll
states are hoisted and saveable for rotation and process death, and fixed heights become minimum heights for large
fonts. No change to the shared core: `TitleDetailsRepository` already shares running refreshes, skips fresh data
and keeps the cache on failure.

## Decisions
- **No open-once guard (EC-2)** (from user; rejected: drop effects unless the screen is `RESUMED`, a 1 s throttle in
  the ViewModel). Same as Home after commit 43b37d4: each tap on "Assistir trailer" or a recommended poster opens
  it. EC-2 is left not done and recorded under Out of scope.
- **Retry message in the designsystem** (from user; rejected: a copy in the details module). Home's `RowMessage`
  body moves to `cmp/designsystem/component/RetryMessage.kt` as a public `RetryMessage`. Home keeps a small
  `RowMessage` that adds its row minimum height, padding and its own "Try again" string. Strings stay in each
  feature, because the designsystem has no string resources.
- **Automatic refresh stays as it is** (from user; rejected: also on `ON_RESUME`). A non-forced refresh runs when
  the screen first gets the language and on each language change. The repository skips details younger than 24 h
  (AC-16).
- **UI state shape**, following Home's `RowState`:
  - `sealed interface DetailsLoad { Loading; Error; Loaded(details) }`.
  - `TitleDetailsUiState(load: DetailsLoad = Loading, isRefreshing: Boolean = false)`, with a `details` getter
    (`(load as? Loaded)?.details`) for the screen and `openTrailer`.
  - Cached details always win: `Loaded` whenever `observe` emits details, even if the last refresh failed (AC-15,
    AC-17). `Error` only when nothing is cached and the last refresh in that language failed (AC-14, EC-13).
- **Failures are kept per language** (`failed: Set<Language>`), so an error in one language never shows after
  switching to the other (EC-9). The new language starts as Loading until its cache or failure arrives.
- **Pull-to-refresh** (AC-17, EC-11):
  - New intent `Refresh`. It forces a refresh and sets `isRefreshing` until the refresh ends.
  - A second pull while `isRefreshing` is ignored, and the repository also shares a running refresh.
  - A failed pull leaves the details as they are, with no message.
  - It is ignored before the language is known.
  - The pull gesture exists only in the Loaded state; Loading and Error have no pull (Error has the retry pill).
  - `PullToRefreshBox` with the same custom indicator as Home: below the status-bar inset, `secondary` color.
- **Retry** (AC-14): new intent `Retry` forces a refresh in the current language. It clears the failure first, so
  the spinner shows again while it runs.
- **NotFound** (EC-13) is an ordinary failure: the Error state with the same message and retry. "Voltar" works in
  every state because the top bar is drawn in all of them.
- **Loading and Error layout** (design "Loading", "Error"): `#21222E` background, the top bar, and centered content:
  - Loading: a `CircularProgressIndicator` in `secondary` (#FF1F8A).
  - Error: `RetryMessage` with "Couldn't load this title." / "Não foi possível carregar este título." and
    "Try again" / "Tentar novamente", centered horizontally, with 30dp side padding.
- **Restore after rotation or process death** (EC-8):
  - The title comes back from the saved route (`TitleDetailsRoute` arguments), so the same title is shown.
  - The vertical `ScrollState` (`rememberScrollState()`) is hoisted to the top of `TitleDetailsContent`, above the
    state branch. It is then restored even though the screen is Loading for a moment after process death.
  - The cast and recommendations rows use `rememberSaveable(saver = LazyListState.Saver) { LazyListState() }`, also
    hoisted there and passed down. `CastRow` gets a `listState` parameter; `PosterRow` already has one.
- **Large fonts** (EC-12):
  - The top bar row and the "Voltar" pill use `heightIn(min = …)` instead of fixed heights.
  - The trailer button uses `heightIn(min = 44.dp)` and its label may wrap, centered.
  - Chips already use `heightIn`; section titles and the name already wrap.
  - The rating ring keeps its 57dp size. Its value text is sized in dp (`15.dp` converted with `LocalDensity`), so
    "100%" never overflows the ring at the largest font. The value is still announced by the screen reader.
- **Long names** (EC-4): `nameWithYear` joins with a non-breaking space (`"$name ($year)"`), so the year stays
  attached when the name wraps. The name column already has `weight(1f)` next to the ring, so it can't overlap it.
- **Long synopsis and cast names** (EC-5, EC-6) are already handled in 02: the synopsis has no line limit inside the
  vertical scroll, and cast names are cut to 2 lines with an ellipsis inside the 75dp column. This plan checks
  them manually.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Robustness pattern (failed set, manual refresh, retry) | `HomeViewModel` | `cmp/feature/home/ui/.../HomeViewModel.kt` |
| Pull-to-refresh with custom indicator | `HomeContent` | `cmp/feature/home/ui/.../HomeScreen.kt` |
| Message + retry pill | `RowMessage` (moves to `RetryMessage`) | `cmp/feature/home/ui/.../component/RowMessage.kt` |
| Saveable row scroll | `rememberSaveable(saver = LazyListState.Saver)` as in Home's `TitleRow` | `HomeScreen.kt` |
| Shared refresh, staleness, cache kept on failure | `TitleDetailsRepository.refresh(id, language, force)` | `service/title/domain` |
| Repository double (`failNextRefresh`, `refreshRequests`, NotFound without data) | `FakeTitleDetailsRepository` | `service/title/fake` |
| Standard dispatcher for refresh tests | `MainDispatcherTest(StandardTestDispatcher())` as in `HomeViewModelRefreshTest` | `core:testing` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `cmp/designsystem` | `component/RetryMessage.kt` (new) | Shared message + retry pill |
| `cmp/feature/home/ui` | `component/RowMessage.kt` (modify) | Delegates to `RetryMessage`; no visible change |
| `cmp/feature/title-details/ui` | `TitleDetailsContract.kt`, `TitleDetailsViewModel.kt`, `TitleDetailsScreen.kt`, `TitleDetailsFormat.kt`, `component/{DetailsTopBar,TrailerButton,CastRow,RatingRing}.kt` (modify); `composeResources/values{,-pt-rBR}/strings.xml` (modify) | States, refresh, retry, restore, large fonts |
| `cmp/feature/title-details/ui` tests | `TitleDetailsViewModelTest.kt`, `TitleDetailsFormatTest.kt` (modify); `TitleDetailsViewModelStateTest.kt`, `TitleDetailsViewModelRefreshTest.kt` (new) | Cover the new behavior |

## Steps
### Step 1: Shared retry message  (AC-14) ✅
- Files: `cmp/designsystem/src/commonMain/kotlin/com/banhmilabs/tmdb/cmp/designsystem/component/RetryMessage.kt`
  (new), `cmp/feature/home/ui/.../component/RowMessage.kt` (modify)
- What:
  - `@Composable fun RetryMessage(message: String, retryLabel: String, onRetry: (() -> Unit)?, modifier: Modifier =
    Modifier, horizontalAlignment: Alignment.Horizontal = Alignment.Start)`.
  - It holds the body of today's `RowMessage`: a `Column` with 12dp between message and pill, the message 15sp at
    50% white, and the pill (`secondary` fill, radius 70dp, padding 14/6dp, SemiBold 16sp,
    `minimumInteractiveComponentSize`, `Role.Button`). No pill when `onRetry` is null.
  - Home's `RowMessage(message, onRetry, modifier)` keeps its signature and calls `RetryMessage` with
    `retryLabel = stringResource(Res.string.home_retry)` and
    `modifier.fillMaxWidth().heightIn(min = RowMinHeight).padding(horizontal = ScreenPadding)`. The Column's
    vertical centering stays in `RetryMessage` (`Arrangement.spacedBy(12.dp, Alignment.CenterVertically)`).
- Unit tests: none (UI only; Home's look is checked manually in How to test).
- Compile: `./gradlew :cmp:designsystem:compileAndroidMain :cmp:feature:home:ui:compileAndroidMain`

### Step 2: State, retry and pull-to-refresh in the ViewModel  (AC-13, AC-14, AC-15, AC-16, AC-17, EC-9, EC-11, EC-13) ✅
- Files: `TitleDetailsContract.kt`, `TitleDetailsViewModel.kt` (modify)
- What:
  - Contract:
    - `sealed interface DetailsLoad { data object Loading; data object Error; data class Loaded(val details:
      TitleDetails) }`.
    - `TitleDetailsUiState(val load: DetailsLoad = DetailsLoad.Loading, val isRefreshing: Boolean = false)` with
      `val details: TitleDetails? get() = (load as? DetailsLoad.Loaded)?.details`.
    - New intents `data object Refresh` and `data object Retry`.
  - ViewModel:
    - `failed = MutableStateFlow(emptySet<Language>())` and `manualRefresh = MutableStateFlow(false)`.
    - `state`: `language.filterNotNull().flatMapLatest { current -> combine(repository.observe(titleId, current),
      failed, manualRefresh) { details, failedLanguages, refreshing -> TitleDetailsUiState(load(details, current in
      failedLanguages), refreshing) } }`, `stateIn` as today.
    - `init`: on each distinct language, `launch { refresh(current, force = false) }`, as today, through the new
      `refresh`.
    - `Refresh` → `refreshManually()`, `Retry` → `retry()`.
  - Pseudo-code:
    ```kotlin
    private fun refreshManually() {
        val current = language.value ?: return
        if (manualRefresh.value) return
        manualRefresh.value = true
        viewModelScope.launch {
            try { refresh(current, force = true) } finally { manualRefresh.value = false }
        }
    }

    private fun retry() {
        val current = language.value ?: return
        viewModelScope.launch { refresh(current, force = true) }
    }

    private suspend fun refresh(current: Language, force: Boolean) {
        failed.update { it - current }
        val result = repository.refresh(titleId, current, force)
        if (result is AppResult.Failure) failed.update { it + current }
    }

    private fun load(details: TitleDetails?, failed: Boolean): DetailsLoad = when {
        details != null -> DetailsLoad.Loaded(details)
        failed -> DetailsLoad.Error
        else -> DetailsLoad.Loading
    }
    ```
  - Update the KDoc of the class (stale-only automatic refresh, forced pull and retry).
  - `TitleDetailsScreen.kt` keeps compiling by reading `state.details` (Step 3 adds the states).
- Unit tests:
  - `TitleDetailsViewModelTest` (existing): update the expected states to `TitleDetailsUiState(DetailsLoad.Loaded(...))`.
  - `TitleDetailsViewModelStateTest` (new, unconfined dispatcher):
    - `startsLoading`: before the language is known, the state is `Loading` and not refreshing.
    - `failedLoadWithoutCacheShowsError` (`failNextRefresh(AppError.Network)`).
    - `missingTitleShowsError` (no remote details → NotFound → `Error`).
    - `retryLoadsAgain`: Error, then `Retry` → `Loaded`, and the last request has `force = true`.
    - `failedRefreshKeepsCachedDetails`: loaded, then `failNextRefresh` + `Retry` → still `Loaded`.
    - `errorInOneLanguageIsNotShownInTheOther`: pt-BR fails → Error; switch to en-US → `Loaded` with en-US
      details (and never `Error` for en-US).
  - `TitleDetailsViewModelRefreshTest` (new, `MainDispatcherTest(StandardTestDispatcher())`):
    - `pullToRefreshForcesAndEndsIndicator`: one forced request, some state had `isRefreshing`, final `false`.
    - `pullWhileRefreshingIsIgnored`: two pulls → one forced request.
    - `failedPullKeepsDetails`: loaded, `failNextRefresh`, pull → still `Loaded`, `isRefreshing` false.
    - `pullBeforeLanguageDoesNothing`: no requests.
    - `loadingUntilFirstRefreshEnds`: after the language and before `advanceUntilIdle`, the state is `Loading`.
- Compile: `./gradlew :cmp:feature:title-details:ui:compileAndroidMain
  :cmp:feature:title-details:ui:compileAndroidHostTest`

### Step 3: Loading, Error, pull-to-refresh and restore on the screen  (AC-13, AC-14, AC-17, EC-8, EC-13) ✅
- Files: `TitleDetailsScreen.kt`, `component/CastRow.kt` (modify), `composeResources/values/strings.xml`,
  `composeResources/values-pt-rBR/strings.xml` (modify)
- What:
  - Strings: `details_error` ("Couldn't load this title." / "Não foi possível carregar este título."),
    `details_retry` ("Try again" / "Tentar novamente").
  - `TitleDetailsContent`:
    - Hoist `val scrollState = rememberScrollState()`, `val castState = rememberSaveable(saver =
      LazyListState.Saver) { LazyListState() }` and `val recommendationsState = rememberSaveable(...)` above the
      `when`.
    - `when (val load = state.load)`:
      - `Loading` → a `Box(fillMaxSize, Center)` with `CircularProgressIndicator(color = secondary)`.
      - `Error` → `RetryMessage(message, retryLabel, onRetry = { onIntent(Retry) }, modifier =
        Modifier.align(Center).padding(horizontal = ScreenPadding), horizontalAlignment = CenterHorizontally)`.
      - `Loaded` → `PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = { onIntent(Refresh) }, state =
        pullState, indicator = { … as Home … })` around today's scrolling `Box`, which now uses `scrollState`.
    - `DetailsTopBar` stays drawn last, in every state.
  - `DetailsBody` gets `castState` and `recommendationsState`, passed to `CastRow(listState = …)` and
    `PosterRow(listState = …)`.
  - `CastRow(cast, startPadding, modifier, listState: LazyListState = rememberLazyListState())`.
- Unit tests: none (UI). Covered by How to test 1, 2, 3 and 7.
- Compile: `./gradlew :cmp:feature:title-details:ui:compileAndroidMain`

### Step 4: Large fonts and long names  (EC-4, EC-5, EC-6, EC-12) ✅
- Files: `TitleDetailsFormat.kt`, `component/DetailsTopBar.kt`, `component/TrailerButton.kt`,
  `component/RatingRing.kt` (modify)
- What:
  - `nameWithYear`: `"$name ($year)"`; KDoc says the year stays on the name's last line.
  - `DetailsTopBar`: `.heightIn(min = BarHeight)` for the row and `.heightIn(min = PillHeight)` for the pill.
  - `TrailerButton`: `.heightIn(min = ButtonHeight)`; the label gets `textAlign = Center` and a weight-free layout
    so it wraps inside the pill.
  - `RatingRing`: the value's font size is `with(LocalDensity.current) { 15.dp.toSp() }`, so it doesn't scale.
- Unit tests: `TitleDetailsFormatTest.nameWithYearAddsYear` expects `"Imperdoável (2021)"`; new
  `nameWithYearKeepsYearAttached` asserts the separator is `' '`.
- Compile: `./gradlew :cmp:feature:title-details:ui:compileAndroidMain
  :cmp:feature:title-details:ui:compileAndroidHostTest`

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-13 First load spinner | 2, 3 | `startsLoading`, `loadingUntilFirstRefreshEnds`; manual: How to test 1 |
| AC-14 Error and retry | 1, 2, 3 | `failedLoadWithoutCacheShowsError`, `retryLoadsAgain`; manual: How to test 1 |
| AC-15 Offline with cache | 2 | `failedRefreshKeepsCachedDetails`; manual: How to test 2 |
| AC-16 Freshness | 2 | `refreshesOncePerLanguage` (non-forced refresh; staleness is the repository's, tested in 01); manual: How to test 4 |
| AC-17 Pull-to-refresh | 2, 3 | `pullToRefreshForcesAndEndsIndicator`, `failedPullKeepsDetails`; manual: How to test 2 |
| EC-2 Open once | — (not done, from user) | — |
| EC-4 Long name | 4 | `nameWithYearKeepsYearAttached`; manual: How to test 6 |
| EC-5 Long synopsis | — (already in 02) | manual: How to test 6 |
| EC-6 Long cast name | — (already in 02) | manual: How to test 6 |
| EC-8 Rotation and process death | 3 | manual: How to test 7 (Android "Don't keep activities") |
| EC-9 Language change (partial) | 2 | `languageChangeShowsOtherLanguage`, `errorInOneLanguageIsNotShownInTheOther`; manual: How to test 8 |
| EC-11 Pull while refreshing | 2 | `pullWhileRefreshingIsIgnored`; manual: How to test 3 |
| EC-12 Largest font | 4 | manual: How to test 6 |
| EC-13 Title gone from TMDB | 2, 3 | `missingTitleShowsError` |

## Out of scope
- EC-2 (open exactly once on repeated taps): not guarded, as on Home (from user). A quick double tap may open the
  trailer or a recommendation twice.
- The favorite button, and the favorite part of EC-9 -> 04-favorites.
- Native apps -> 05, 06.
- A message when a pull-to-refresh fails while details are shown (AC-17 says no error replaces them).
- Telling network errors apart from NotFound in the copy.

## Risks
- **Scroll position after process death.** The vertical `ScrollState` is restored before the content is laid out;
  if the restored value is clamped to 0 while Loading, the position is lost. Mitigation: How to test 7 on Android
  with "Don't keep activities". If it is lost, keep the scroll `Box` composed in every state and record a
  deviation.
- **`PullToRefreshBox` around a `verticalScroll` with the fixed top bar above it.** Same setup as Home, which works;
  the indicator is below the status bar and centered, so it does not cover "Voltar". Mitigation: How to test 2.
- **Rating ring text in dp.** It ignores the user's font scale inside the ring. Accepted: the value is announced
  by the screen reader, and a growing value would overflow the fixed ring.

## Deviations
- **`RetryMessage` centers its message text** when `horizontalAlignment` is `CenterHorizontally`, so a wrapped
  error message on the details screen stays centered. Home passes the default (`Start`) and looks as before.
- **`DetailsScroll`**: a small private holder for the three scroll states, passed from `TitleDetailsContent` to
  the loaded content. The Loaded state is drawn by a new private `LoadedContent`, which keeps
  `TitleDetailsContent` short.
- **Checks run**: host tests of `cmp:designsystem`, `cmp:feature:home:ui` (15) and `cmp:feature:title-details:ui`
  (29), Detekt on the three modules, `:architecture-test:test`, `:cmp:androidApp:assembleDebug`. `assemble` (iOS)
  was not run: no `expect`/`actual`, platform source set or build logic changed. Nothing was checked on a device or
  simulator (How to test 1–8 still to do).
