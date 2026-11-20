# Plan: Title details happy path (cmp)

Requirement: [02 - Title details happy path (cmp)](../subtasks/02-cmp-title-details-happy-path.md)
Status: Done

## Summary
Add the feature module `cmp/feature/title-details/ui`, shaped like `cmp/feature/home/ui` (MVI contract, ViewModel,
stateful screen + stateless content, `component/`, `navigation/`, `di/`). The ViewModel observes
`TitleDetailsRepository` (subtask 01) for one `TitleId` in the device language and refreshes it once per language;
the content draws design/title-details.md. The app shell replaces the placeholder route with the real one, and
recommendations navigate to another details entry on the stack. Home's poster row and person icon move to
`cmp/designsystem` so both features use them (rule 5.4). Loading/error/pull-to-refresh polish is 03; the heart is 04.

## Decisions
- **Poster row → designsystem**: `PosterRow` (with its placeholder tile) moves from `home/ui/component` to
  `cmp/designsystem`; Home only changes imports. (From user; rejected: a copy in the details module.) The
  designsystem may depend on `core:model` (`DependencyRules`: `DESIGNSYSTEM -> MODEL`) and gets Coil.
- **Release date format by app language**: pt-BR `dd/MM/yyyy` ("10/12/2021"), en `MM/dd/yyyy` ("12/10/2021"),
  in common code. (From user; rejected: platform date formatters via expect/actual.)
- **Placeholders**: artwork without image → its 493dp area filled `#303243` (the fade still on top); cast member
  without photo → `#303243` circle with the white person icon at 50% (the icon moves from Home to the
  designsystem as `PersonIcon`); recommendation without poster → the poster row's existing name tile. (From user.)
- **Genre chips**: width from the label (horizontal padding 17.5dp, height 31dp), 14dp apart, wrapping to more
  lines with 10dp between lines (`FlowRow`). (From user; rejected: horizontal scroll, fixed 82dp.)
- **"Voltar" background**: a pill like the heart's circle — fill `#303243` at 65%, fully rounded, 32dp high —
  always shown, so it stays readable when content scrolls under it. (From user; rejected: a top bar that fades
  in on scroll, no background.)
- **Module path** `cmp/feature/title-details/ui` (`:cmp:feature:title-details:ui`, accessor
  `projects.cmp.feature.titleDetails.ui`); `tmdb.module` and Konsist drop the hyphen, so the package is
  `com.banhmilabs.tmdb.cmp.feature.titledetails.ui`. No `nav` module: only the app shell navigates to it
  (architecture 5.5: "until then the route lives in `ui`").
- **Route** `TitleDetailsRoute(tmdbId: Long, type: String)` with `TitleType.name` — primitive arguments only, so
  no custom `NavType`; mapped back with `TitleType.valueOf`.
- **ViewModel argument**: `TitleDetailsViewModel(titleId: TitleId, repository)`, created with Koin
  `parametersOf(titleId)` from the route; each back-stack entry has its own ViewModel, so stacked details
  (recommendations) don't share state. Avoids `SavedStateHandle.toRoute` in host tests.
- **Language**: as Home — the screen reports `Language.fromLocaleTag(Locale.current.toLanguageTag())` with an
  intent; nothing is observed or refreshed before that.
- **Refresh in this subtask**: one `refresh(id, language)` (not forced) each time the language is set, so details
  load at all; the empty state until then is the background + "Voltar" only (spinner, error and pull-to-refresh
  come in 03).
- **Navigation through effects** (MVI as Home): `BackClicked`, `TrailerClicked`, `RecommendationClicked(title)`
  intents → `NavigateBack`, `OpenTrailer(url)`, `OpenTitle(title)` effects. The screen opens the trailer with
  Compose `LocalUriHandler.openUri` (Android: `ACTION_VIEW`, handed to the YouTube app when installed; iOS:
  `openURL`, YouTube universal link), wrapped in `runCatching` so a device that can't open it does nothing (EC-3).
  The system back button uses the navigation library's default (pop).
- **Edge-to-edge as Home**: the artwork draws behind the status bar; "Voltar" and the (04) heart sit in a 48dp
  row right below the status bar inset (the design's 36dp top already falls inside a 44dp status bar).
- **Touch targets**: the "Voltar" pill is centered in a 48dp-high clickable area; the trailer button is 44dp
  high plus 4dp of minimum interactive padding (`minimumInteractiveComponentSize`).
- **Page color**: the design's fade layer makes the page `#21222E` below the artwork; the screen background is
  `#21222E` and the artwork gets a vertical gradient from transparent (at 260/493) to `#21222E` (at 412/493).
- **One-off colors** stay private to the module (`DetailsColors`): `#21222E` page, `#303243` track/chip/pill,
  `#15161D` ring center, `#BBBBBB` date line, `#CCCCCC` synopsis. Theme tokens are used where they match:
  `secondary` (#FF1F8A) for the ring arc, `primary` (#8000FF) → `primaryContainer` (#540BA1) for the trailer
  gradient.
- **Rating ring semantics**: announced as "Rating 45%" / "Avaliação 45%", or "Not rated yet" / "Ainda sem
  avaliação" when `ratingPercent` is null (AC-6).
- **Runtime text**: `1h 53m`, `2h` (no minutes), `45m` (under an hour), the same in both languages.
- **Recommendations row scroll state**: `rememberLazyListState()` here; saving it across process death is 03 (EC-8).
- Design title-details: used (sure link).

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Details data (observe, refresh) | `TitleDetailsRepository`, `FakeTitleDetailsRepository` | `service/title/{domain,fake}` |
| Language from the device | `Language.fromLocaleTag`, `Locale.current` (as `HomeScreen`) | `core:model`, `cmp/feature/home/ui/.../HomeScreen.kt` |
| MVI shape, effects channel, `stateIn` | `HomeViewModel`, `HomeContract` | `cmp/feature/home/ui` |
| Route + `NavGraphBuilder` extension | `HomeRoute`, `homeScreen` | `cmp/feature/home/ui/.../navigation/` |
| Koin feature module | `homeUiModule`, `cmpAppModule.includes(...)` | `.../home/ui/di/`, `cmp/composeApp/.../di/CmpAppModule.kt` |
| Theme colors | `TmdbTheme` (`primary`, `primaryContainer`, `secondary`) | `cmp/designsystem/.../Theme.kt` |
| Poster row + name placeholder tile | `PosterRow` → moved to designsystem | `cmp/feature/home/ui/.../component/PosterRow.kt` |
| Person icon | `ic_person.xml` (Material "person") → moved to designsystem as `PersonIcon` | `cmp/feature/home/ui/composeResources/drawable/` |
| Remote images | Coil `AsyncImage`; loader set in `App()` | `cmp/composeApp/.../App.kt` |
| ViewModel test Main dispatcher | `MainDispatcherTest` | `core:testing` |
| Placeholder screen to replace | `TitleDetailsPlaceholderRoute`, `TitleDetailsPlaceholderScreen` | `cmp/composeApp/.../TitleDetailsPlaceholder.kt` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `cmp:designsystem` | `build.gradle.kts` (modify); `component/PosterRow.kt`, `component/PersonIcon.kt`, `composeResources/drawable/ic_person.xml` (moved in) | Shared stateless components |
| `cmp:feature:home:ui` | `component/PosterRow.kt`, `drawable/ic_person.xml` (deleted); `HomeScreen.kt`, `component/HomeHeader.kt` (modify) | Use the designsystem versions |
| `cmp:feature:title-details:ui` (new) | `build.gradle.kts`; `TitleDetailsContract.kt`, `TitleDetailsViewModel.kt`, `TitleDetailsScreen.kt`, `TitleDetailsFormat.kt`, `DetailsColors.kt`; `component/{DetailsTopBar,DetailsArtwork,DetailsHeader,RatingRing,TrailerButton,CastRow,GenreChips,SectionHeading}.kt`; `navigation/{TitleDetailsRoute,TitleDetailsNavigation}.kt`; `di/TitleDetailsUiModule.kt`; `composeResources/values{,-pt-rBR}/strings.xml`, `drawable/{ic_back_arrow,ic_clock,ic_play_circle}.xml`; tests | The screen |
| root | `settings.gradle.kts` (modify) | Include the module |
| `cmp:composeApp` | `build.gradle.kts`, `AppNavHost.kt`, `di/CmpAppModule.kt` (modify); `TitleDetailsPlaceholder.kt`, `drawable/ic_arrow_back.xml`, `values{,-pt-rBR}/strings.xml` (deleted) | Real route instead of the placeholder |

## Steps
### Step 1: Shared poster row and person icon in the designsystem  (AC-11) ✅
- Files: `cmp/designsystem/build.gradle.kts` (modify); `cmp/designsystem/src/commonMain/kotlin/com/banhmilabs/tmdb/cmp/designsystem/component/PosterRow.kt`
  (moved from home, made public), `.../component/PersonIcon.kt` (new), `cmp/designsystem/src/commonMain/composeResources/drawable/ic_person.xml`
  (moved); `cmp/feature/home/ui/.../HomeScreen.kt`, `.../component/HomeHeader.kt` (modify); home's `PosterRow.kt`
  and `ic_person.xml` (deleted)
- What:
  - designsystem `commonMain`: `api(projects.core.model)`, `implementation(libs.coil.compose)`.
  - `fun PosterRow(titles: List<Title>, onTitleClick: (Title) -> Unit, modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(), contentPadding: PaddingValues = PaddingValues())` — same
    look and semantics as today; the start padding becomes a parameter (Home passes `ScreenPadding`).
  - `fun PersonIcon(modifier: Modifier = Modifier)` — the person vector, white at 50% (the alpha Home uses).
  - Home: `PosterRow(..., contentPadding = PaddingValues(start = ScreenPadding))`; `HomeHeader` draws `PersonIcon`.
- Unit tests: none (stateless UI, unchanged behavior; Home's ViewModel tests still run).
- Compile: `./gradlew :cmp:designsystem:compileAndroidMain :cmp:feature:home:ui:compileAndroidMain :cmp:feature:home:ui:compileAndroidHostTest`

### Step 2: Module, contract, ViewModel, formatting  (AC-2, AC-5, AC-7, AC-11, AC-18, EC-7, EC-10) ✅
- Files: `settings.gradle.kts` (modify); `cmp/feature/title-details/ui/build.gradle.kts`; under
  `cmp/feature/title-details/ui/src/commonMain/kotlin/com/banhmilabs/tmdb/cmp/feature/titledetails/ui/`:
  `TitleDetailsContract.kt`, `TitleDetailsViewModel.kt`, `TitleDetailsFormat.kt`, `di/TitleDetailsUiModule.kt`,
  `navigation/TitleDetailsRoute.kt` (new)
- What:
  - Build: like `home/ui` — `implementation(projects.service.title.domain)`, `projects.cmp.designsystem`,
    navigation-compose, lifecycle viewmodel/runtime compose, serialization core, koin-compose-viewmodel, coil
    compose; `androidHostTest`: `projects.service.title.fake`, `projects.core.testing`, turbine.
  - Contract:
    ```kotlin
    internal data class TitleDetailsUiState(val details: TitleDetails? = null)
    internal sealed interface TitleDetailsIntent {
        data class LanguageChanged(val language: Language) : TitleDetailsIntent
        data object BackClicked : TitleDetailsIntent
        data object TrailerClicked : TitleDetailsIntent
        data class RecommendationClicked(val title: Title) : TitleDetailsIntent
    }
    internal sealed interface TitleDetailsEffect {
        data object NavigateBack : TitleDetailsEffect
        data class OpenTrailer(val url: String) : TitleDetailsEffect
        data class OpenTitle(val title: Title) : TitleDetailsEffect
    }
    ```
  - `TitleDetailsViewModel(titleId: TitleId, repository: TitleDetailsRepository) : ViewModel`:
    `language = MutableStateFlow<Language?>(null)`; `state = language.filterNotNull().flatMapLatest {
    repository.observe(titleId, it) }.map(::TitleDetailsUiState).stateIn(viewModelScope, WhileSubscribed(5_000),
    TitleDetailsUiState())`; `init`: `language.filterNotNull().distinctUntilChanged().onEach { launch {
    repository.refresh(titleId, it) } }.launchIn(viewModelScope)`; `effects` from a buffered `Channel`;
    `TrailerClicked` sends `OpenTrailer(details.trailer.url)` only when the current details have a trailer.
  - `TitleDetailsFormat.kt`: `internal fun formatReleaseDate(date: LocalDate, language: Language): String`
    (zero-padded `dd/MM/yyyy` or `MM/dd/yyyy`), `internal fun formatRuntime(minutes: Int): String`,
    `internal fun nameWithYear(name: String, year: Int?): String` (`"Name (2021)"` or `"Name"`).
  - Route: `@Serializable data class TitleDetailsRoute(val tmdbId: Long, val type: String) { val titleId get() =
    TitleId(tmdbId, TitleType.valueOf(type)) }`; `fun TitleId.toDetailsRoute() = TitleDetailsRoute(tmdbId, type.name)`.
  - DI: `val titleDetailsUiModule = module { viewModel { (id: TitleId) -> TitleDetailsViewModel(id, get()) } }`.
- Unit tests (`androidHostTest`):
  - `TitleDetailsViewModelTest : MainDispatcherTest` with `FakeTitleDetailsRepository`: `nothingObservedBeforeLanguage`,
    `showsDetailsAfterLanguage`, `refreshesOncePerLanguage` (`refreshRequests`), `languageChangeShowsOtherLanguage`,
    `trailerClickOpensTrailerUrl`, `trailerClickWithoutTrailerDoesNothing`, `recommendationClickOpensTitle`,
    `backClickNavigatesBack`.
  - `TitleDetailsFormatTest`: `portugueseDateIsDayMonthYear`, `englishDateIsMonthDayYear`, `datePadsDayAndMonth`,
    `runtimeWithHoursAndMinutes`, `runtimeWholeHours`, `runtimeUnderAnHour`, `nameWithYear`, `nameWithoutYear`.
  - `TitleDetailsRouteTest`: `routeRoundTripsTitleId` (movie and series).
- Compile: `./gradlew :cmp:feature:title-details:ui:compileAndroidMain :cmp:feature:title-details:ui:compileAndroidHostTest`

### Step 3: Resources  (AC-1, AC-6, AC-8, AC-18) ✅
- Files: `cmp/feature/title-details/ui/src/commonMain/composeResources/values/strings.xml`,
  `values-pt-rBR/strings.xml`, `drawable/ic_back_arrow.xml`, `drawable/ic_clock.xml`, `drawable/ic_play_circle.xml` (new)
- What:
  - Strings (en / pt-BR): `details_back` Back / Voltar; `details_watch_trailer` Watch trailer / Assistir trailer;
    `details_cast` Main cast / Elenco principal; `details_genres` Genres / Categoria(s); `details_recommendations`
    Recommendations / Recomendações; `details_no_synopsis` Synopsis not available. / Sinopse indisponível.;
    `details_not_rated` NR / NR; `details_not_rated_description` Not rated yet / Ainda sem avaliação;
    `details_rating_description` Rating %1$d%% / Avaliação %1$d%%.
  - Vectors converted from `design/assets/` (same paths and colors): `ic_back_arrow.svg` (24×24, white),
    `ic_clock.svg` (19×19, `#BBBBBB`), `ic_play_circle.svg` (20×20, white at 90%). `dot_separator`, the ring and
    the divider are drawn in code; the other SVGs are reference only (design.md assets table).
- Compile: `./gradlew :cmp:feature:title-details:ui:compileAndroidMain` (generates `Res`)

### Step 4: Components  (AC-1, AC-6, AC-7, AC-8, AC-9, AC-10, AC-11, AC-12) ✅
- Files: `.../titledetails/ui/DetailsColors.kt`, `.../component/DetailsTopBar.kt`, `DetailsArtwork.kt`,
  `DetailsHeader.kt`, `RatingRing.kt`, `TrailerButton.kt`, `CastRow.kt`, `GenreChips.kt`, `SectionHeading.kt` (new)
- What (values from design/title-details.md):
  - `DetailsColors`: `Page #21222E`, `Track #303243`, `RingCenter #15161D`, `DateLine #BBBBBB`, `Synopsis #CCCCCC`.
  - `DetailsTopBar(onBack)`: 48dp row below the status-bar inset; "Voltar" pill (start 8dp, height 32dp, fill
    Track at 65%, `CircleShape`, padding 10dp horizontal; arrow 24dp, 6dp gap, label Inter Bold 16sp white);
    clickable `Role.Button`, merged semantics with the label. The heart slot is left for 04 (not added now).
  - `DetailsArtwork(url)`: 493dp high, `AsyncImage` crop, `contentDescription = null`; background Track; overlay
    `Brush.verticalGradient(0f to Transparent, 260/493f to Transparent, 412/493f to Page, 1f to Page)`.
  - `RatingRing(percent: Int?)`: 57dp `Canvas` — Track circle, RingCenter circle 45dp, arc stroke 6dp round cap from
    -90° sweeping `percent × 3.6°` in `secondary`; text "45" Bold 15sp + "%" Regular 15sp white, or "NR" Bold 15sp;
    `clearAndSetSemantics { contentDescription = rating / not-rated string }`.
  - `DetailsHeader(details, language)`: `Row(top)`: ring, 16dp, `Column(weight 1)`: name+year Inter Bold 22sp
    white (wraps, EC-4 handled in 03), 5dp, date line `Row(centerVertically)`: date Regular 16sp DateLine, 3dp,
    4dp dot (DateLine), 6dp, clock 19dp, 6dp, runtime; parts without a value are left out with their separator.
  - `TrailerButton(onClick)`: full width, height 44dp, `RoundedCornerShape(60.dp)`, `Brush.linearGradient`
    (`primary` → `primaryContainer`, at 148.66° as the design), play icon 20dp + 10dp + label SemiBold 16sp white.
  - `SectionHeading(text)`: Inter Bold 18sp white, `semantics { heading() }`.
  - `CastRow(cast)`: `LazyRow`, start padding 30dp, items 75dp wide with 4dp spacing; photo 60dp circle (`AsyncImage`
    crop, or Track circle + `PersonIcon` 30dp) with a 4dp Track border; name 3dp below, Bold 12sp white centered,
    max 2 lines, ellipsis; not clickable.
  - `GenreChips(genres)`: `FlowRow` (14dp horizontal, 10dp vertical spacing); chip height 31dp, `RoundedCornerShape(34.dp)`,
    fill Track, label Medium 15sp white, horizontal padding 17.5dp; not clickable.
- Unit tests: none (stateless UI; checked manually, see Traceability).
- Compile: `./gradlew :cmp:feature:title-details:ui:compileAndroidMain`

### Step 5: Screen and content  (AC-1, AC-2, AC-5, AC-8, AC-9, AC-10, AC-11, AC-12, AC-18, EC-3) ✅
- Files: `.../titledetails/ui/TitleDetailsScreen.kt`, `.../navigation/TitleDetailsNavigation.kt` (new)
- What:
  - `TitleDetailsScreen(titleId, onBack, onTitleClick, viewModel = koinViewModel { parametersOf(titleId) })`:
    reports the language as Home does; collects effects: `NavigateBack` → `onBack()`, `OpenTitle` →
    `onTitleClick(title)`, `OpenTrailer` → `runCatching { uriHandler.openUri(url) }`.
  - `TitleDetailsContent(state, language, onIntent)`: `Box(fillMaxSize, background Page)`; `details == null` →
    only the top bar. Otherwise a `Column(verticalScroll)`: `Box` { `DetailsArtwork`; `Column(padding(top = 376.dp))`
    { header (horizontal 30dp), 21dp, divider (1dp, black 48%), 20dp, synopsis Regular 14sp line height 17.5sp
    Synopsis color (or `details_no_synopsis`), trailer [30dp + `TrailerButton`] if `trailer != null`, cast
    [46dp + heading + 16dp + `CastRow`] if not empty, genres [41dp + heading + 20dp + `GenreChips`] if not empty,
    recommendations [41dp + heading + 20dp + `PosterRow(contentPadding start 30dp)`] if not empty, then 40dp +
    navigation-bar inset } }; `DetailsTopBar` drawn last, fixed on top.
  - `fun NavGraphBuilder.titleDetailsScreen(onBack: () -> Unit, onTitleClick: (Title) -> Unit)`:
    `composable<TitleDetailsRoute> { entry -> TitleDetailsScreen(entry.toRoute<TitleDetailsRoute>().titleId, onBack,
    onTitleClick) }`.
- Unit tests: none beyond step 2 (the ViewModel holds the logic).
- Compile: `./gradlew :cmp:feature:title-details:ui:compileAndroidMain :cmp:feature:title-details:ui:compileAndroidHostTest`

### Step 6: Wire into the app, remove the placeholder  (AC-2, AC-11, EC-7) ✅
- Files: `cmp/composeApp/build.gradle.kts`, `AppNavHost.kt`, `di/CmpAppModule.kt` (modify); `TitleDetailsPlaceholder.kt`,
  `composeResources/drawable/ic_arrow_back.xml`, `composeResources/values/strings.xml`,
  `composeResources/values-pt-rBR/strings.xml` (delete)
- What:
  - `implementation(projects.cmp.feature.titleDetails.ui)`; `cmpAppModule.includes(..., titleDetailsUiModule)`.
  - `AppNavHost`: Home's `onTitleClick = { navController.navigate(it.id.toDetailsRoute()) }`;
    `titleDetailsScreen(onBack = navController::navigateUp, onTitleClick = { navController.navigate(it.id.toDetailsRoute()) })`.
- Compile: `./gradlew :cmp:composeApp:compileAndroidMain :cmp:androidApp:assembleDebug`, then `./gradlew assemble`
  (macOS + Xcode; iOS framework).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 layout (no heart) | 3, 4, 5 | Manual: compare with design/title-details.md on Android and iOS |
| AC-2 back | 2, 5, 6 | `backClickNavigatesBack`; manual: "Voltar" and system back to Home (same tab/scroll) and to the previous title |
| AC-5 trailer | 2, 4, 5 | `trailerClickOpensTrailerUrl`, `trailerClickWithoutTrailerDoesNothing`; manual: YouTube app / browser; no button without trailer |
| AC-6 rating, NR | 3, 4 | Manual: "45%" ring, "NR" title; TalkBack/VoiceOver labels |
| AC-7 year, date, runtime | 2, 4 | `TitleDetailsFormatTest` (all cases); manual: series shows first air date and episode runtime |
| AC-8 missing synopsis | 3, 5 | Manual: title without pt-BR synopsis |
| AC-9 cast | 4, 5 | Manual: ≤10 members, placeholder photo, no tap action, hidden when empty |
| AC-10 genres | 4, 5 | Manual: chips wrap, no tap action, hidden when empty |
| AC-11 recommendations | 1, 2, 5, 6 | `recommendationClickOpensTitle`; manual: ≤20, name tile, TalkBack button with name, hidden when empty |
| AC-12 artwork | 4 | Manual: poster/backdrop/placeholder; "Voltar" readable; artwork not announced |
| AC-18 language | 2, 3, 5 | `showsDetailsAfterLanguage`, `languageChangeShowsOtherLanguage`; manual: en and pt-BR copy |
| EC-3 no app to open trailer | 5 | Manual: simulator without YouTube/Safari restrictions — no crash (`runCatching`) |
| EC-7 stacked details | 2, 6 | `recommendationClickOpensTitle`; manual: open 3 recommendations, back one step at a time |
| EC-10 other device language | 2 | `Language.fromLocaleTag` (existing `core:model` tests); manual: e.g. French device → English |

Checks after the last step: `./gradlew :cmp:designsystem:detekt :cmp:feature:home:ui:testAndroidHostTest
:cmp:feature:home:ui:detekt :cmp:feature:title-details:ui:testAndroidHostTest :cmp:feature:title-details:ui:detekt
:cmp:composeApp:testAndroidHostTest :cmp:composeApp:detekt :architecture-test:test`, and `./gradlew assemble`.

## Out of scope
- Spinner, error + retry, offline/no-error display, automatic refresh after 24 h, pull-to-refresh, repeated taps,
  long content, saved scroll positions, rotation/process death, largest font → 03-cmp-title-details-robustness.
- The heart button and favorites → 04-favorites.
- Native apps → 05, 06.

## Risks
- Uninstall dev builds before testing: `title.db` has no migration (subtask 01).
- `LocalUriHandler.openUri` on iOS opens Safari when the YouTube app is not installed; on Android it throws when no
  app handles the URL — covered by `runCatching`, checked manually.
- Moving `PosterRow` to the designsystem changes Home's imports only; Home's tests and a manual pass over Home cover it.
- Inter font: the design uses Inter; the app uses the platform default font today (Home and onboarding as well) —
  kept consistent, not changed here.

## Deviations
- Step 1: `PosterRow`'s parameters were reordered (`listState` and `contentPadding` optional, after `modifier`) so
  the details screen can call it with defaults.
- Step 4: `CastRow` takes a `startPadding` parameter (the screen's 30dp) instead of reading a shared constant;
  each cast item merges its semantics so the name is announced once.
- Step 4: the trailer gradient runs from the top-left to the bottom-right corner of the pill
  (`Offset.Zero` → `Offset.Infinite`), which is how a 148.66° gradient lands on a 315×44dp pill.
