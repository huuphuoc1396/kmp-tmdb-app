# Plan: Home happy path (cmp)

Requirement: [04 - Home happy path (cmp)](../subtasks/04-cmp-home-happy-path.md)
Status: Done

## Summary
A new feature module, `cmp/feature/home/ui`, replaces the placeholder Home. It is shaped like
`cmp/feature/onboarding/ui`: an MVI contract, a ViewModel, a stateful `HomeScreen` and a stateless `HomeContent`,
plus `component/`, `navigation/` and `di/`.

The ViewModel works from the selected category and the device language:
- It observes both lists of that category from `TitleListsRepository`.
- Each time the category or language changes, it calls `refresh(force = false)` for both lists.

Posters load with Coil 3. The app shell gets a placeholder details screen that shows the title's name and has a
back button. Loading, error and empty states, pull-to-refresh and accessibility are left to 05.

## Decisions
- **New module `cmp/feature/home/ui`** (namespace `com.banhmilabs.tmdb.cmp.feature.home.ui`).
  - Its dependencies: `service:title:domain`, `cmp:designsystem` and the kernel.
  - `HomeRoute` moves from `composeApp` into it (`navigation/HomeRoute.kt`). The start destination and the
    navigation after onboarding use the new route.
  - No `nav` module: no other feature navigates to Home (`docs/architecture.md` 5.5).
- **English tab labels: Movies, Series, Anime, Telenovelas, Documentaries** (from user). "Telenovelas" matches the
  data (Latin American novelas). Rejected: "Soaps", which reads as English-language soap operas.
- **Refresh in 04** (from user):
  - When the language is first known, and on every change of category or language, the ViewModel calls
    `refresh(category, type, language, force = false)` for both list types.
  - Fresh lists are skipped by the repository.
  - Results are ignored for now: errors, the spinner, empty rows and pull-to-refresh come in 05.
  - Rejected: fetching only uncached categories.
- **Language comes from the device locale in the screen.**
  - `HomeScreen` computes `Language.fromLocaleTag(androidx.compose.ui.text.intl.Locale.current.toLanguageTag())`
    and sends `HomeIntent.LanguageChanged` from a `LaunchedEffect(language)`.
  - The ViewModel observes nothing until the language is known.
  - Compose resources pick the copy (`values` / `values-pt-rBR`), so copy and data follow the same locale
    (AC-10, EC-8).
  - Because the effect is keyed by language, a later language change already flows through (EC-7 is in 05).
- **UI state is the selected category plus two lists.**
  - `HomeUiState(selectedCategory = TitleCategory.MOVIES, featured = emptyList(), popular = emptyList())`.
  - `null` from the repository (not loaded yet) becomes an empty list. 05 adds the distinct loading, error and
    empty states.
  - Categories are `TitleCategory.entries`, already in tab order, so the state doesn't carry them.
- **Opening a title is an effect**: `HomeEffect.OpenTitle(title)`, sent from `HomeIntent.TitleClicked`. The screen
  calls `onTitleClick(title)`, and the shell navigates. EC-1 (opening exactly once on repeated taps) is in 05.
- **Placeholder details live in the app shell** (`composeApp`), like the old placeholder Home:
  - `TitleDetailsPlaceholderRoute(name: String)` with a screen that shows the name and a back arrow button.
  - The button is there because iOS has no system Back button.
  - The details feature will replace it with its own route.
- **Images: Coil 3** (`docs/architecture.md` 12):
  - Libraries: `coil-compose` plus `coil-network-ktor3`. Ktor's engines are already in the catalog: OkHttp on
    Android, Darwin on iOS.
  - The shell sets the singleton loader in `App()` with
    `setSingletonImageLoaderFactory { ImageLoader.Builder(it).components { add(KtorNetworkFetcherFactory()) }.build() }`,
    so it doesn't depend on service-loader auto-registration on iOS.
  - Version: the latest stable 3.x on Maven Central, checked in forge.
- **Poster tile**:
  - With no `posterUrl`: the placeholder tile, `surfaceVariant` fill with the name (AC-11).
  - With a URL: `AsyncImage` (crop) over a `surfaceVariant` box, so a loading or failed image shows a plain tile.
  - The whole tile is `clickable(role = Role.Button)`. Screen-reader labels come in 05.
- **Theme tokens** (`cmp/designsystem/Theme.kt`):
  - `secondary = #FF1F8A` (selected tab) and `onSecondary = White`.
  - `surfaceVariant = #36076B` (search field, avatar, placeholder tile) and `onSurfaceVariant = White`.
  - The header glow uses `primary` (`#8000FF`).
  - The screen gradient colour `#303243` is used only by Home, so it stays a private constant there.
- **Typography**: the system font, as decided in onboarding plan 03 (Inter is not bundled). Sizes and weights come
  from design/home.md. Text styles stay private to the feature, as onboarding's do.
- **Inert search field and avatar (AC-4, AC-5)**: plain `Box`es with no `clickable` or focus, and
  `Modifier.clearAndSetSemantics {}` so screen readers skip them.
- **Insets**: the content gets `WindowInsets.safeDrawing` padding plus 20dp at the top, which stands in for the
  design's 64dp offset minus the 44dp status bar. The backgrounds draw edge to edge. Rows bleed to the right edge:
  each `LazyRow` keeps only start padding.
- **Back keeps the tab and scroll positions** within the back stack (AC-3, partial). The ViewModel belongs to
  Home's back-stack entry, and `rememberLazyListState()` / `rememberScrollState()` are saveable. Nothing extra is
  needed. Process death is in 05.
- **ViewModel tests need different titles per list.** `FakeTitleListsRepository` gains
  `setRemoteTitles(category, listType, titles)`, with its own fake test. It is a small extension in the fake module;
  the contract is unchanged.
- **Assets**:
  - `iconly_light_outline_search.svg` becomes a vector XML `drawable/ic_search.xml` (fill alpha 0.5 as exported).
    The screen mirrors it with `graphicsLayer(scaleX = -1f)`.
  - The person icon is a Material Symbols "person" vector (`drawable/ic_person.xml`, Apache 2.0), drawn white at
    50% alpha. This closes the design gap "Source of the generic person icon".
  - `ellipse_1.png` was only a reference and is not used.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Feature module shape (contract, ViewModel, screen, component/, navigation/, di/) | onboarding feature | `cmp/feature/onboarding/ui/` |
| Title lists | `TitleListsRepository`, `TitleCategory`, `TitleListType` | `service/title/domain` |
| Title model, language mapping | `Title`, `Language.fromLocaleTag` | `core/model` |
| Theme | `TmdbTheme` | `cmp/designsystem/.../Theme.kt` |
| Navigation entry pattern | `NavGraphBuilder.onboardingScreen(...)` | `cmp/feature/onboarding/ui/.../navigation/OnboardingNavigation.kt` |
| Koin feature module | `onboardingUiModule` with `viewModelOf` | `cmp/feature/onboarding/ui/.../di/OnboardingUiModule.kt` |
| ViewModel test base, fake | `MainDispatcherTest`, `FakeTitleListsRepository` | `core/testing`, `service/title/fake` |
| Edge-to-edge, light status bar icons | `enableEdgeToEdge(SystemBarStyle.dark(...))` | `cmp/androidApp/.../MainActivity.kt` |
| Ktor engines for Coil | `ktor-client-okhttp`, `ktor-client-darwin` | `gradle/libs.versions.toml` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| root | `gradle/libs.versions.toml`, `settings.gradle.kts` (modify) | Coil; the new module |
| `service:title:fake` | `FakeTitleListsRepository.kt`, `FakeTitleListsRepositoryTest.kt` (modify) | Titles per list for ViewModel tests |
| `cmp:designsystem` | `Theme.kt` (modify) | `secondary` and `surfaceVariant` tokens |
| `cmp:feature:home:ui` (new) | `build.gradle.kts`; `HomeContract.kt`, `HomeViewModel.kt`, `HomeScreen.kt`; `component/HomeHeader.kt`, `SearchField.kt`, `CategoryTabs.kt`, `PosterRow.kt`; `navigation/HomeRoute.kt`, `HomeNavigation.kt`; `di/HomeUiModule.kt`; `composeResources` (strings en + pt-BR, `ic_search.xml`, `ic_person.xml`); `HomeViewModelTest.kt` | The Home screen |
| `cmp:composeApp` | `build.gradle.kts`, `App.kt`, `AppNavHost.kt`, `di/CmpAppModule.kt` (modify); `TitleDetailsPlaceholder.kt`, `composeResources` (back label en + pt-BR, `ic_arrow_back.xml`) (new); `HomeRoute.kt`, `PlaceholderHomeScreen.kt` (delete) | Wiring, Coil loader, placeholder details |
| docs | `CLAUDE.md` (modify) | Current state, reuse table (Coil, home feature) |

`cmp:androidApp` and `cmp/iosApp` need no changes. They host `App()` through `composeApp`.

## Steps
### Step 1: Catalog, fake titles per list, theme tokens ✅
- Files: `gradle/libs.versions.toml`; `service/title/fake/src/commonMain/.../FakeTitleListsRepository.kt`,
  `src/androidHostTest/.../FakeTitleListsRepositoryTest.kt`; `cmp/designsystem/src/commonMain/.../Theme.kt`
  (all modify)
- What:
  - Catalog: `[versions] coil = "<latest stable 3.x>"`; libraries `coil-compose`
    (`io.coil-kt.coil3:coil-compose`) and `coil-network-ktor3` (`io.coil-kt.coil3:coil-network-ktor3`).
  - Fake:
    - `fun setRemoteTitles(category: TitleCategory, listType: TitleListType, titles: List<Title>)` stores an
      override.
    - `refresh` stores that override for the list, else `remoteTitles`, for any language.
  - Theme:
    - Add `Pink = Color(0xFFFF1F8A)` and `Plum = Color(0xFF36076B)`.
    - Map them: `secondary = Pink`, `onSecondary = White`, `surfaceVariant = Plum`, `onSurfaceVariant = White`.
    - The comment says the colours come from the onboarding and home designs.
- Unit tests: `FakeTitleListsRepositoryTest.setRemoteTitlesAppliesOnlyToThatList`.
- Compile: `./gradlew :service:title:fake:compileAndroidHostTest :cmp:designsystem:compileAndroidMain`

### Step 2: Home contract and ViewModel  (AC-2, AC-3, AC-10) ✅
- Files:
  - `settings.gradle.kts` (add `:cmp:feature:home:ui`)
  - `cmp/feature/home/ui/build.gradle.kts`
  - `src/commonMain/kotlin/com/banhmilabs/tmdb/cmp/feature/home/ui/HomeContract.kt`, `HomeViewModel.kt`,
    `di/HomeUiModule.kt`
  - `src/androidHostTest/.../HomeViewModelTest.kt` (all new)
- What:
  - Build file, the same shape as onboarding's:
    - `commonMain`:
      - Projects: `service.title.domain`, `cmp.designsystem`.
      - Libraries: `androidx.navigation.compose`, `androidx.lifecycle.viewmodel.compose`,
        `androidx.lifecycle.runtime.compose`, `kotlinx.serialization.core`, `koin.compose.viewmodel`,
        `coil.compose`.
    - `androidHostTest`: `service.title.fake`, `core.testing`, `turbine`.
  - Contract:
    ```kotlin
    internal data class HomeUiState(
        val selectedCategory: TitleCategory = TitleCategory.MOVIES,
        val featured: List<Title> = emptyList(),
        val popular: List<Title> = emptyList(),
    )
    internal sealed interface HomeIntent {
        data class LanguageChanged(val language: Language) : HomeIntent
        data class CategorySelected(val category: TitleCategory) : HomeIntent
        data class TitleClicked(val title: Title) : HomeIntent
    }
    internal sealed interface HomeEffect {
        data class OpenTitle(val title: Title) : HomeEffect
    }
    ```
  - `HomeViewModel(private val repository: TitleListsRepository) : ViewModel()`:
    - `state: StateFlow<HomeUiState>`.
    - `effects: Flow<HomeEffect>` from `Channel(BUFFERED)`.
    - `onIntent(intent)`.
  - `homeUiModule = module { viewModelOf(::HomeViewModel) }`.
- Pseudo-code:
  ```
  private data class Selection(val category: TitleCategory, val language: Language)
  private val category = MutableStateFlow(TitleCategory.MOVIES)
  private val language = MutableStateFlow<Language?>(null)
  private val selection = combine(category, language.filterNotNull(), ::Selection).distinctUntilChanged()

  val state = selection.flatMapLatest { (category, language) ->
      combine(observe(category, FEATURED, language), observe(category, POPULAR, language)) { featured, popular ->
          HomeUiState(category, featured.orEmpty(), popular.orEmpty())
      }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

  init { selection.onEach { (c, l) -> TitleListType.entries.forEach { viewModelScope.launch { repository.refresh(c, it, l) } } }
             .launchIn(viewModelScope) }

  onIntent: LanguageChanged -> language.value = it; CategorySelected -> category.value = it;
            TitleClicked -> viewModelScope.launch { _effects.send(OpenTitle(title)) }
  ```
- Unit tests: `HomeViewModelTest : MainDispatcherTest()`. Its fake has different titles for each list.
  - `startsWithMoviesSelectedAndNoTitles`
  - `languageKnownShowsMoviesFeaturedAndPopular`
  - `selectingCategoryShowsItsLists` (AC-2)
  - `listsAreLoadedInTheGivenLanguage`: after `LanguageChanged(PT_BR)`, the fake has `PT_BR` lists and none for
    `EN_US` (AC-10).
  - `titleClickedOpensThatTitle` (AC-3)
- Compile: `./gradlew :cmp:feature:home:ui:compileAndroidMain :cmp:feature:home:ui:compileAndroidHostTest`

### Step 3: Resources and assets  (AC-10, EC-8) ✅
- Files: `cmp/feature/home/ui/src/commonMain/composeResources/values/strings.xml`,
  `values-pt-rBR/strings.xml`, `drawable/ic_search.xml`, `drawable/ic_person.xml` (all new)
- What:
  - Strings, as `name`: en / pt-BR:
    - `home_title`: "What do you want to watch today?" / "O que você quer assistir hoje?"
    - `home_search_hint`: "Search" / "Buscar"
    - `home_popular_heading`: "Most popular" / "Mais populares"
    - `home_category_movies`: "Movies" / "Filmes"
    - `home_category_series`: "Series" / "Séries"
    - `home_category_anime`: "Anime" / "Animes"
    - `home_category_novelas`: "Telenovelas" / "Novelas"
    - `home_category_documentaries`: "Documentaries" / "Documentários"
  - `ic_search.xml`: Android vector with `viewportWidth="20.9704"`, `viewportHeight="21.4434"`, size
    20.97×21.44dp, the SVG's `pathData`, `fillColor="#FFFFFF"`, `fillAlpha="0.5"`, `fillType="evenOdd"`.
  - `ic_person.xml`: Material Symbols "person" (24dp, filled, white).
- Unit tests: none (resources).
- Compile: `./gradlew :cmp:feature:home:ui:compileAndroidMain` (generates `Res`)

### Step 4: Home screen and components  (AC-1, AC-2, AC-4, AC-5, AC-11) ✅
- Files:
  - `cmp/feature/home/ui/src/commonMain/kotlin/com/banhmilabs/tmdb/cmp/feature/home/ui/HomeScreen.kt`
  - `component/HomeHeader.kt`, `component/SearchField.kt`, `component/CategoryTabs.kt`, `component/PosterRow.kt`
  - `navigation/HomeRoute.kt`, `navigation/HomeNavigation.kt` (all new)
- What (values from design/home.md):
  - `HomeScreen(onTitleClick: (Title) -> Unit, viewModel: HomeViewModel = koinViewModel())`:
    - Computes the language and sends it from `LaunchedEffect(language)`.
    - Collects the state, and collects effects to call `onTitleClick`.
    - Draws `HomeContent(state, onIntent)`.
  - `HomeContent`:
    - A full-size `Box`. Its background is a vertical gradient: `#303243` at 0, `background` (`#15151D`) at
      0.654, then solid.
    - Inside, a `Column` with `verticalScroll`. Its first layer is the 522dp header glow, a vertical gradient from
      `primary` to transparent that scrolls with the content.
    - Content with start padding 30dp:
      - `HomeHeader`.
      - 17dp, then `SearchField`.
      - 31dp, then `CategoryTabs`.
      - 37dp, then the featured `PosterRow`.
      - 23dp, then the heading "Most popular" (18sp bold).
      - 16dp, then the popular `PosterRow`.
      - Then 79dp of bottom padding.
  - `HomeHeader`:
    - A `Row`. The title takes the weight: 26sp bold white, max width 288dp, wraps.
    - The avatar is 40dp, 30dp from the end and 7dp below the title top: a `CircleShape` with `surfaceVariant`
      fill and `ic_person` at 50% alpha, using `clearAndSetSemantics {}` (AC-5).
  - `SearchField`:
    - 46dp high, filling the width minus 30dp of end padding, `RoundedCornerShape(32.dp)`, `surfaceVariant`.
    - The hint is 15sp medium white at 50%, starting 21dp in.
    - `ic_search` is mirrored and sits 16dp from the end.
    - No `clickable` or focus, and `clearAndSetSemantics {}` (AC-4).
  - `CategoryTabs(selected, onSelect)`: a `LazyRow` with start padding 33dp and 29dp spacing.
    - Selected: a pill with `secondary` fill, 70dp radius, 14/6dp padding, 16sp semibold white.
    - Unselected: 16sp regular, white at 40%, with the same padding so the labels line up.
    - Each tab is `clickable(role = Role.Tab)` and sends `CategorySelected`.
    - Labels come from a private `TitleCategory.label()` mapping to the string resources.
  - `PosterRow(titles, onTitleClick)`: a `LazyRow`, keyed by `title.id`, with start padding 30dp and 12dp
    spacing.
    - Each `Poster` is 124×188dp, clipped to an 8dp radius, and `clickable(role = Role.Button)`.
    - With no URL: the placeholder tile, `surfaceVariant` fill with the name in 14sp semibold white, centred,
      8dp padding, max 4 lines, ellipsis (AC-11).
    - With a URL: `AsyncImage(contentScale = Crop)` over a `surfaceVariant` box.
  - `@Serializable data object HomeRoute`.
  - `fun NavGraphBuilder.homeScreen(onTitleClick: (Title) -> Unit)`.
- Unit tests: none. Compose UI is checked manually (`docs/architecture.md` 8). See How to test.
- Compile: `./gradlew :cmp:feature:home:ui:compileAndroidMain :cmp:feature:home:ui:compileKotlinIosSimulatorArm64`

### Step 5: Wire Home and placeholder details into the app shell  (AC-1, AC-3) ✅
- Files:
  - `cmp/composeApp/build.gradle.kts`, `App.kt`, `AppNavHost.kt`, `di/CmpAppModule.kt` (modify)
  - `TitleDetailsPlaceholder.kt`, `composeResources/values/strings.xml`,
    `composeResources/values-pt-rBR/strings.xml`, `composeResources/drawable/ic_arrow_back.xml` (new)
  - `HomeRoute.kt`, `PlaceholderHomeScreen.kt` (delete)
- What:
  - Build:
    - `commonMain`: `projects.cmp.feature.home.ui`, `libs.coil.compose`, `libs.coil.network.ktor3`.
    - `androidMain`: `libs.ktor.client.okhttp`.
    - `iosMain`: `libs.ktor.client.darwin`.
  - `App()`: `setSingletonImageLoaderFactory { context -> ImageLoader.Builder(context).components { add(KtorNetworkFetcherFactory()) }.build() }`
    before `TmdbTheme`. `HomeRoute` is imported from the home module.
  - `AppNavHost`:
    - Uses `homeScreen(onTitleClick = { navController.navigate(TitleDetailsPlaceholderRoute(it.name)) })`.
    - Adds `composable<TitleDetailsPlaceholderRoute> { TitleDetailsPlaceholderScreen(name, onBack = navController::navigateUp) }`.
  - `TitleDetailsPlaceholder.kt`:
    - `@Serializable internal data class TitleDetailsPlaceholderRoute(val name: String)`.
    - `TitleDetailsPlaceholderScreen`:
      - The background colour fills the screen, with safe-drawing insets.
      - An `IconButton` with `ic_arrow_back` and the content description `details_back` ("Back" / "Voltar").
      - The name, centred, 26sp bold.
      - Its KDoc says the details feature replaces it.
  - `cmpAppModule` includes `homeUiModule`.
- Unit tests: `AppViewModelTest` is unchanged and must still pass.
- Compile:
  - `./gradlew :cmp:composeApp:compileAndroidMain :cmp:androidApp:compileDebugKotlin :cmp:composeApp:compileKotlinIosSimulatorArm64`
  - Then `xcodebuild -project cmp/iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`

### Step 6: Docs ✅
- Files: `CLAUDE.md` (modify)
- What:
  - Current state: the `cmp/` Home feature (`cmp/feature/home/ui`) and the placeholder details screen in the shell.
  - Reuse table:
    - Add "Remote images | Coil 3 `AsyncImage`; loader set in `App()` | `cmp/*`".
    - Add `FakeTitleListsRepository.setRemoteTitles`.
  - The repository layout line: "a placeholder Home" becomes the Home screen and placeholder details.
- Unit tests: none.
- Compile: none (docs).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 Home layout | 4, 5 | Manual: How to test 1 |
| AC-2 Switching category | 2, 4 | `selectingCategoryShowsItsLists`; manual: How to test 2 |
| AC-3 Opening a title (partial) | 2, 5 | `titleClickedOpensThatTitle`; manual: How to test 3 (Back keeps the tab and scroll) |
| AC-4 Search field has no action | 4 | Manual: How to test 4 (TalkBack/VoiceOver skips it) |
| AC-5 Avatar has no action | 4 | Manual: How to test 4 |
| AC-10 Language (partial: copy) | 2, 3, 4 | `listsAreLoadedInTheGivenLanguage`; manual: How to test 5 |
| AC-11 Poster without image (partial) | 4 | Manual: How to test 6 |
| EC-8 Other device language | 3, 4 | `LanguageTest.otherLanguagesMapToEnUs` (core:model); manual: How to test 5 with e.g. es |

## Out of scope
- Spinner, per-row error and retry, empty rows, pull-to-refresh, offline display, restoring the tab and scroll
  after process death, screen-reader labels for posters and tabs, minimum touch targets, large fonts, opening
  exactly once on repeated taps (EC-1), language change while running (EC-7) → 05-cmp-home-robustness.
- Press feedback design (a design gap): the default Compose indication is used for now.
- Native apps → 06, 07. The real details screen and Search → separate features.
- The Inter font → later, as decided in onboarding plan 03.

## Risks
- **Coil 3 on iOS needs a network fetcher.** Mitigation: it is registered explicitly in `App()`, and How to test 1
  on the iOS simulator confirms posters load.
- **Coil and Compose Multiplatform versions.** The latest Coil 3.x might need a newer Compose than 1.12.2.
  Mitigation: check Coil's release notes in forge, pick the newest version compatible with Compose 1.12.x, and
  record it as a deviation if it is not the latest.
- **20dp top offset is a guess.** It is the design's 64dp minus a 44dp status bar. Mitigation: compare the
  simulator against `home.png` in How to test 1, and adjust as a small deviation.
- **Real data without a token.** Without `TMDB_ACCESS_TOKEN`, the rows stay empty with no error (errors come in
  05). Mitigation: How to test needs `secrets.properties` with a valid token.

## Deviations
- **Coil 3.6.3**, the latest on Maven Central (checked 2026-11-19). It requires Compose 1.12.0 and Kotlin 2.4.10,
  both covered by the project's versions.
- **`cmp:feature:home:ui` declares `api(projects.core.model)`**: `homeScreen(onTitleClick: (Title) -> Unit)` exposes
  `Title` to the shell.
- **The person icon is Material Icons "person"** (Apache 2.0) rather than Material Symbols. The shape is the same
  and the path is simpler.
- **Unselected tabs have no horizontal padding.** Labels sit 29dp apart, and the selected pill's edge is 29dp from
  the next label, as in the design. As a result, the tabs shift slightly when the selection changes. The plan said
  every tab would get the same padding.
- **ViewModel test setup**:
  - `HomeViewModelTest` creates the ViewModel lazily, because its `init` starts collecting, which must happen after
    `MainDispatcherTest` sets Main.
  - It runs `runTest(testDispatcher)`, as onboarding does.
- **Manual check on the Pixel 8 emulator** (Android, English), with a real token:
  - Home matches the design.
  - Movies posters load.
  - Switching to Anime changes both rows.
  - Tapping a poster opens the placeholder showing its name.
  - Back returns to Anime with the same rows.
- **Backgrounds are vector drawables, not `Brush` code** (from user, after review). This matches onboarding's
  `onboarding_scrim.xml`.
  - `drawable/home_background.xml`: 375×812, `#303243` → `#15151D` at 0.65394. It is drawn behind the screen with
    `ContentScale.FillBounds`.
  - `drawable/home_header_glow.xml`: 375×522, `#8000FF` → transparent. It is drawn at the top of the scrolling
    content.
  - The glow colour is now fixed in the vector rather than taken from `primary`.
  - Checked on the iPhone 17 simulator: it looks the same as before. This was also the first run of Home on iOS.
