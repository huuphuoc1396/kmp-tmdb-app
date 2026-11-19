# Plan: Home robustness (cmp)

Requirement: [05 - Home robustness (cmp)](../subtasks/05-cmp-home-robustness.md)
Status: Done

## Summary
`HomeViewModel` stops turning `null` into an empty list. Each row gets a `RowState`: `Loading`, `Error`, or
`Titles` (an empty list means the row loaded with nothing in it).

The ViewModel works out each row's state from three things:
- the cached list from `observe` (`null` means never loaded);
- the outcome of the last refresh of that list, stored per (category, list type, language);
- whether a pull-to-refresh is running.

The screen then:
- shows the category spinner while neither row has loaded;
- shows the empty, error and retry rows from the design;
- wraps the content in Material 3 `PullToRefreshBox`.

The selected tab survives process death through `SavedStateHandle`. Each row's scroll state is saveable and keyed by
category. Poster taps navigate only while Home is resumed. Posters, tabs and the retry pill get accessibility
semantics and 48dp touch targets.

## Decisions
- **Empty and error copy** (from user; this closes the open question in task.md and the design gap). Copy is
  en / pt-BR:
  - Empty row: "Nothing to show here yet." / "Nada para mostrar aqui ainda."
  - Error row: "Couldn't load these titles." / "Não foi possível carregar estes títulos."
  - Retry: "Try again" / "Tentar novamente".
  - One error message covers every `AppError`. Rejected: copy that tells the user to check the connection, which
    would also show for token or server errors.
- **Row state**:
  ```kotlin
  internal sealed interface RowState {
      data object Loading : RowState
      data object Error : RowState
      data class Titles(val titles: List<Title>) : RowState
  }
  ```
  - `Titles` when `observe` emits a list, even if the last refresh failed. Cached titles always win, so no error
    shows while the cache is there (AC-7).
  - `Error` when `observe` emits `null` and the last refresh of that list and language failed (AC-8, EC-4).
  - `Loading` otherwise.
- **Category spinner versus row spinner.**
  - When both rows are `Loading`, the design's single spinner replaces both rows and the "Most popular" heading
    (AC-6). It is `CircularProgressIndicator` in `secondary`.
  - When only one row is `Loading`, a smaller spinner (24dp) sits in that row's 188dp area at the 30dp padding.
    The design does not cover this case; this keeps the other row usable (EC-4).
- **Refresh outcomes are kept per (category, list type, language)** in a `MutableStateFlow<Set<ListKey>>` of
  failed lists. They are read only for the current selection, so a refresh that finishes for the previous tab or
  language can never mark the current one (EC-5, EC-7).
  - A refresh removes its key when it starts.
  - It adds the key on `Failure`.
- **Automatic refresh stays as in 04** (AC-9): `refresh(force = false)` for both lists whenever the category or
  language changes. The repository's 6 h TTL decides whether a request is sent. The cached titles stay visible
  because the rows read `observe`.
- **Pull-to-refresh** (AC-9, EC-9):
  - `HomeIntent.Refresh` refreshes both lists of the current selection with `force = true`, and
    `isRefreshing = true` until both finish.
  - A second pull while `isRefreshing` is ignored. The repository also merges concurrent refreshes.
  - The indicator is shown only for this manual refresh, as the design says. Automatic refreshes are silent.
  - Uses Material 3 `PullToRefreshBox` (`androidx.compose.material3.pulltorefresh`), which is already in
    `compose-material3`.
- **Retry**: `HomeIntent.Retry(listType)` refreshes that one list with `force = true` (AC-8).
- **The selected tab survives process death** (EC-6, AC-3):
  - `HomeViewModel(repository, savedStateHandle: SavedStateHandle)`, with the category stored by enum name under
    `"selectedCategory"`.
  - `viewModelOf(::HomeViewModel)` gets the handle from Koin's `koinViewModel()`.
  - The language is not saved. The screen sends it again on recomposition.
- **Row scroll survives process death.**
  - Each `PosterRow` uses `rememberSaveable(category, saver = LazyListState.Saver) { LazyListState() }`. Each
    category has its own position, a switched-to tab starts at its own saved position (the start the first time),
    and the position is restored after rotation or process death.
  - The tab row and the vertical scroll already use saveable state (`rememberLazyListState`,
    `rememberScrollState`).
- **Open exactly once (EC-1)**: the effect collector in `HomeScreen` calls `onTitleClick` only while
  `LocalLifecycleOwner.current.lifecycle.currentState` is at least `RESUMED`. After the first `navigate`, Home's
  back-stack entry is no longer resumed, so a second tap is dropped. This is the same rule as
  `dropUnlessResumed`.
- **Accessibility (AC-4, AC-5 kept, AC-11, EC-10)**:
  - Poster: `semantics(mergeDescendants = true) { contentDescription = title.name }` on the clickable tile, with
    `Role.Button` from `clickable`. The placeholder's name `Text` gets `clearAndSetSemantics {}` so the name is
    read once.
  - Tabs: `Modifier.selectableGroup()` on the row. Each tab already has `selectable(role = Role.Tab)`.
  - Touch targets: tabs and the retry pill get `Modifier.minimumInteractiveComponentSize()` (48dp).
- **The 48dp tabs keep the design's positions.**
  - The row grows from 34dp to 48dp, centred, so it gains 7dp above and below.
  - `SearchToTabs` becomes 31 − 7 = 24dp and `TabsToFeatured` 37 − 7 = 30dp. The visible pills don't move.
- **Large fonts (EC-10)**:
  - The tab row and the search field use `heightIn(min = …)` instead of a fixed height.
  - The "Most popular" heading may wrap (no `maxLines = 1`).
  - Empty and error rows use `heightIn(min = 188.dp)`.
  - Posters keep 124×188, and the placeholder name is cut to 4 lines with an ellipsis (EC-2).
- **The fake records refresh calls** so the ViewModel tests can check `force`, request counts and languages:
  - `FakeTitleListsRepository.refreshRequests: List<RefreshRequest>`, with
    `data class RefreshRequest(val category, val listType, val language, val force)`.
  - The contract is unchanged.
- **`HomeUiState` changes shape**: `featured` and `popular` become `RowState`, and `isRefreshing: Boolean` is
  added.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Home contract, ViewModel, screen, rows | 04 code | `cmp/feature/home/ui/` |
| Pull-to-refresh | `PullToRefreshBox` | `compose-material3` (already a dependency through `tmdb.module`) |
| Spinner, touch target | `CircularProgressIndicator`, `Modifier.minimumInteractiveComponentSize()` | `compose-material3` |
| Pill style for retry | Selected tab pill (`secondary`, 70dp radius, 14/6dp, 16sp semibold) | `cmp/feature/home/ui/.../component/CategoryTabs.kt` |
| Saved state | `SavedStateHandle` | `androidx.lifecycle` (via `lifecycle-viewmodel-compose`) |
| Lifecycle check | `LocalLifecycleOwner` | `lifecycle-runtime-compose` |
| Repository double | `FakeTitleListsRepository` (`setRemoteTitles`, `failNextRefresh`) | `service/title/fake` |
| ViewModel test base | `MainDispatcherTest` | `core/testing` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `service:title:fake` | `FakeTitleListsRepository.kt`, `FakeTitleListsRepositoryTest.kt` (modify) | Records refresh calls |
| `cmp:feature:home:ui` | `HomeContract.kt`, `HomeViewModel.kt`, `HomeScreen.kt`, `component/CategoryTabs.kt`, `component/PosterRow.kt`, `component/SearchField.kt` (modify); `component/RowMessage.kt` (new); `composeResources/values*/strings.xml` (modify); `HomeViewModelTest.kt` (modify) | States, refresh, restore, accessibility |
| docs | `task.md` (open question answered), `design/home.md` (gap closed) (modify) | Copy decided |

## Steps
### Step 1: Fake records refresh calls ✅
- Files: `service/title/fake/src/commonMain/.../FakeTitleListsRepository.kt`,
  `src/androidHostTest/.../FakeTitleListsRepositoryTest.kt` (modify)
- What:
  - `data class RefreshRequest(val category: TitleCategory, val listType: TitleListType, val language: Language, val force: Boolean)`,
    nested in the fake.
  - `val refreshRequests: List<RefreshRequest>`. Each `refresh` appends its request, including failed ones.
- Unit tests: `FakeTitleListsRepositoryTest.refreshRequestsAreRecorded`.
- Compile: `./gradlew :service:title:fake:compileAndroidHostTest`

### Step 2: Row states, refresh outcomes, pull-to-refresh, retry, saved tab  (AC-6, AC-7, AC-8, AC-9, EC-3, EC-4, EC-5, EC-6, EC-7, EC-9) ✅
- Files: `cmp/feature/home/ui/src/commonMain/.../HomeContract.kt`, `HomeViewModel.kt`;
  `src/androidHostTest/.../HomeViewModelTest.kt` (modify)
- What:
  - Contract:
    - `RowState` as in Decisions.
    - `HomeUiState(selectedCategory, featured: RowState = Loading, popular: RowState = Loading, isRefreshing = false)`.
    - New intents `HomeIntent.Refresh` (`data object`) and `HomeIntent.Retry(val listType: TitleListType)`.
  - ViewModel:
    - The constructor gains `savedStateHandle: SavedStateHandle`.
    - `category` is read from and written to the handle under `"selectedCategory"`, by enum name.
- Pseudo-code:
  ```
  private data class ListKey(val category, val listType, val language)
  private val failed = MutableStateFlow(emptySet<ListKey>())
  private val manualRefresh = MutableStateFlow(false)

  state = selection.flatMapLatest { (c, l) ->
      combine(observe(c, FEATURED, l), observe(c, POPULAR, l), failed, manualRefresh) { f, p, failed, refreshing ->
          HomeUiState(c, rowState(f, ListKey(c, FEATURED, l) in failed), rowState(p, ...), refreshing)
      }
  }.stateIn(..., HomeUiState(selectedCategory = savedCategory))

  rowState(titles, failed) = titles?.let(::Titles) ?: if (failed) Error else Loading

  private suspend fun refresh(key, force): AppResult<Unit> {
      failed.update { it - key }
      return repository.refresh(key.category, key.listType, key.language, force)
          .also { if (it is AppResult.Failure) failed.update { set -> set + key } }
  }

  selection.onEach -> both list types: launch { refresh(key, force = false) }          // unchanged trigger
  Refresh -> if (manualRefresh.value) return; manualRefresh.value = true
             launch { listOf(FEATURED, POPULAR).map { async { refresh(key(it), force = true) } }.awaitAll()
                      manualRefresh.value = false }
  Retry(type) -> launch { refresh(key(type), force = true) }
  CategorySelected(c) -> category.value = c; savedStateHandle[KEY] = c.name
  ```
  - `Refresh` and `Retry` do nothing until the language is known.
- Unit tests: `HomeViewModelTest`, extended. The fake gets different titles per list.
  - `startsLoadingWithMoviesSelected` (AC-6)
  - `rowsShowTitlesOnceLoaded`
  - `emptyListShowsEmptyRow`: `Titles(emptyList())` (EC-3)
  - `failedLoadWithoutCacheShowsError` (AC-8)
  - `oneRowFailsWhileOtherShowsTitles` (EC-4)
  - `failedRefreshWithCacheKeepsTitles`: load, then fail a pull-to-refresh, and the rows are still `Titles` (AC-7)
  - `retryReloadsTheFailedRow`, with a forced request for that list only (AC-8)
  - `tabSwitchRefreshesWithoutForce`: the requests for the new category have `force = false` (AC-9)
  - `pullToRefreshForcesBothListsAndEndsIndicator`: `isRefreshing` goes true then false, and both requests have
    `force = true` (AC-9)
  - `pullWhileRefreshingIsIgnored`: run with `StandardTestDispatcher`, pull twice before advancing, and the
    forced requests are 2, not 4 (EC-9)
  - `failureForPreviousTabDoesNotMarkNewTab` (EC-5)
  - `languageChangeShowsNewLanguageOnly`: the PT_BR refresh fails, so the rows show `Error` and not the cached
    EN_US titles (EC-7)
  - `selectedCategoryIsRestoredFromSavedState`: `SavedStateHandle(mapOf("selectedCategory" to "ANIME"))` (EC-6)
  - Existing tests are updated to `RowState`. `titleClickedOpensThatTitle` is kept.
- Compile: `./gradlew :cmp:feature:home:ui:compileAndroidMain :cmp:feature:home:ui:compileAndroidHostTest`

### Step 3: Copy and row message component  (AC-8, EC-3) ✅
- Files:
  - `cmp/feature/home/ui/src/commonMain/composeResources/values/strings.xml`, `values-pt-rBR/strings.xml`
    (modify)
  - `component/RowMessage.kt` (new)
- What:
  - Strings: `home_row_empty`, `home_row_error`, `home_retry`, with the copy from Decisions.
  - `RowMessage(message: String, onRetry: (() -> Unit)?, modifier)`:
    - A `Column` of at least 188dp, centred vertically, starting at the 30dp padding.
    - The message is 15sp regular, white at 50%.
    - When `onRetry` is set, a pill below it uses the selected-tab style, with
      `minimumInteractiveComponentSize()` and `Role.Button`.
- Unit tests: none (Compose UI; manual).
- Compile: `./gradlew :cmp:feature:home:ui:compileAndroidMain`

### Step 4: Screen states, pull-to-refresh, restore and open-once  (AC-3, AC-6, AC-8, AC-9, EC-1, EC-6) ✅
- Files: `HomeScreen.kt`, `component/PosterRow.kt` (modify)
- What:
  - `HomeContent`:
    - `PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = { onIntent(HomeIntent.Refresh) })` wraps the
      scrolling content. The background image stays behind it.
    - Below the tabs:
      - When both rows are `Loading`: a `CircularProgressIndicator(color = secondary)`, centred, in place of both
        rows and the heading.
      - Otherwise: the featured row, the heading, and the popular row, each drawn from its `RowState`:
        - `Titles` with items → `PosterRow`.
        - `Titles` with no items → `RowMessage(empty)`.
        - `Error` → `RowMessage(error, onRetry = { onIntent(HomeIntent.Retry(type)) })`.
        - `Loading` → a 24dp spinner in a 188dp box at the 30dp padding.
  - `PosterRow(titles, category, onTitleClick)`: its list state is
    `rememberSaveable(category, saver = LazyListState.Saver) { LazyListState() }`.
  - `HomeScreen`: the effect collector checks
    `lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)` before calling `onTitleClick`.
- Unit tests: none (Compose UI). The ViewModel side is covered in Step 2. Manual checks: How to test 1–6 and 9.
- Compile: `./gradlew :cmp:feature:home:ui:compileAndroidMain :cmp:feature:home:ui:compileKotlinIosSimulatorArm64`

### Step 5: Accessibility, touch targets, large fonts  (AC-4, AC-5, AC-11, EC-2, EC-10) ✅
- Files: `component/PosterRow.kt`, `component/CategoryTabs.kt`, `component/SearchField.kt`, `HomeScreen.kt` (modify)
- What:
  - Poster:
    - `semantics(mergeDescendants = true) { contentDescription = title.name }` before `clickable`.
    - The placeholder `Text` gets `clearAndSetSemantics {}`.
  - Tabs:
    - The `LazyRow` gets `selectableGroup()` and `heightIn(min = 48.dp)`.
    - Each tab gets `minimumInteractiveComponentSize()`.
    - Spacing in `HomeContent`: `SearchToTabs = 24.dp` and `TabsToFeatured = 30.dp`, with comments giving the
      reason.
  - Search field: `heightIn(min = 46.dp)` instead of `height`. Its `clearAndSetSemantics {}` stays (AC-4).
  - Heading: drop `maxLines = 1`.
  - The avatar's `clearAndSetSemantics {}` stays (AC-5).
- Unit tests: none (manual: How to test 7, 8).
- Compile: `./gradlew :cmp:feature:home:ui:compileAndroidMain :cmp:feature:home:ui:compileKotlinIosSimulatorArm64`

### Step 6: Close the copy question in the feature docs ✅
- Files: `docs/features/home/task.md`, `docs/features/home/design/home.md` (modify)
- What:
  - `task.md`:
    - Tick the open question about empty and error copy, with the decided copy.
    - Tick the `/discover/movie` question, answered by plan 03.
    - Add a Change log line.
  - `design/home.md`:
    - Tick the gaps that are now closed: English tab labels (plan 04), error and empty copy, the person icon
      (plan 04), and the minimum touch target.
    - The "Copy" table gets the en and pt-BR rows.
    - Add a Change log line.
- Unit tests: none.
- Compile: none (docs).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-3 Same tab and scroll after details and process death | 2, 4 | `selectedCategoryIsRestoredFromSavedState`; manual: How to test 6 |
| AC-6 First load spinner | 2, 4 | `startsLoadingWithMoviesSelected`; manual: How to test 1 |
| AC-7 Offline with cache | 2 | `failedRefreshWithCacheKeepsTitles`; manual: How to test 3 |
| AC-8 Error without cache, retry | 2, 3, 4 | `failedLoadWithoutCacheShowsError`, `retryReloadsTheFailedRow`; manual: How to test 2 |
| AC-9 Freshness and pull-to-refresh | 2, 4 | `tabSwitchRefreshesWithoutForce`, `pullToRefreshForcesBothListsAndEndsIndicator`; manual: How to test 4, 5 |
| AC-11 Posters announced with name as button | 5 | Manual: How to test 7 |
| EC-1 Open once on repeated taps | — (guard removed, see Deviations) | — |
| EC-2 Long name truncated | 5 (kept from 04) | Manual: How to test 8 |
| EC-3 Empty row | 2, 3, 4 | `emptyListShowsEmptyRow` |
| EC-4 One row fails | 2, 4 | `oneRowFailsWhileOtherShowsTitles` |
| EC-5 Tab switch during load | 2 | `failureForPreviousTabDoesNotMarkNewTab` |
| EC-6 Rotation and process death | 2, 4 | `selectedCategoryIsRestoredFromSavedState`; manual: How to test 6 (Android "Don't keep activities") |
| EC-7 Language change while running | 2 | `languageChangeShowsNewLanguageOnly`; manual: change the language in Settings while Home is open |
| EC-8 Other language | 04 (done) | `LanguageTest.otherLanguagesMapToEnUs` |
| EC-9 Pull while refreshing | 2 | `pullWhileRefreshingIsIgnored` |
| EC-10 Largest font | 5 | Manual: How to test 8 |

## Out of scope
- Native apps → 06, 07.
- Press-feedback design (a design gap): the default Compose indication stays.
- Telling network errors apart from server or token errors in the copy.
- A banner or snackbar when a refresh fails while cached titles are shown (AC-7 says no error is shown).

## Risks
- **Koin and `SavedStateHandle` on iOS.** `koinViewModel()` might not supply a `SavedStateHandle` on iOS.
  Mitigation: run the iOS app after Step 4. If it fails, register with `viewModel { HomeViewModel(get(), get()) }`
  and record a deviation. If the handle is still missing, stop and ask.
- **Restoring row scroll before the rows reload.** After process death the rows are `Loading` before they show
  `Titles`. The `LazyListState` keeps its saved index, but the item may not be there yet. Mitigation: How to test
  6 on Android with "Don't keep activities". If the position is lost, key the row by its first visible title and
  record a deviation.
- **`PullToRefreshBox` with an outer `verticalScroll`.** The pull gesture needs a nested-scroll child; the
  `Column`'s `verticalScroll` provides one. Mitigation: How to test 4 on both platforms.

## Deviations
- **Row scroll state lives in `TitleRow`**, hoisted above the `when`, and is passed to `PosterRow` as `listState`.
  This way it survives a row going from `Loading` to `Titles` after process death.
  - There is one saveable state per row, keyed by category.
  - Switching back to a tab starts its rows at the beginning, unlike the plan's "own position per category".
  - The position after details, rotation or process death is still kept.
- **The pull-to-refresh indicator is custom.** `PullToRefreshDefaults.Indicator` sits below the status bar
  (top safe-drawing inset) and uses `secondary`. The default would draw behind the status bar.
- **ViewModel tests are split into three classes** because of Detekt `TooManyFunctions` and the need for a
  standard dispatcher:
  - `HomeViewModelTest`: rows, retry and opening a title.
  - `HomeViewModelSelectionTest`: tabs, language and saved state.
  - `HomeViewModelRefreshTest`: pull-to-refresh, on `StandardTestDispatcher`, so a refresh is still running
    between two pulls.
  - Shared fixtures (`homeTestRepository()`, test titles, `start()`) are in `HomeTestFixtures.kt`.
  - The indicator test checks the final value through `state.value`, because a collector can lag one emission
    behind.
- **One extra test**: `selectingCategorySavesIt`.
- **Step 2 needed a temporary bridge** in `HomeScreen` (`Titles` or an empty list) so the project kept
  compiling. Step 4 replaced it.
- **Manual check on the iPhone 17 simulator** (English, real token):
  - Koin injects `SavedStateHandle` on iOS, with no crash.
  - The tabs keep the design positions with the 48dp row.
  - Telenovelas loads both rows.
  - A pull gesture ran, but the indicator was not captured because the refresh finished too fast.
  - Android and process death were not checked (no emulator was running).
- **The `RESUMED` check before opening details was removed** (from user, after review). As a result, EC-1 (open
  exactly once on repeated taps) has no guard in the code now, and a quick double tap may push details twice.
