# Plan: Browse favorites (cmp)

Requirement: [02 - Browse favorites (cmp)](../subtasks/02-cmp-browse-favorites.md)
Status: Done

## Summary
`HomeViewModel` collects `ObserveFavoriteTitles` for the current language and adds a Favorites section to Home,
between the featured row and "Most popular": the 10 newest, the total in a badge, and "View all". A new feature
module `cmp/feature/favorites/ui` holds My favorites, with its own ViewModel on the same use case. Both draw
posters through a `PosterTile` extracted from the design system's `PosterRow`, so the placeholder is shared. The
app shell wires "View all" → My favorites and a favorite → Title details. Hearts and removing are left to 03.

## Decisions
- **My favorites in a new module `cmp/feature/favorites/ui`** (from user; rejected: inside `home/ui`). Package
  `com.banhmilabs.tmdb.cmp.feature.favorites.ui`; Home reaches it only through a callback, as `ui → ui` is forbidden.
  No `nav` module: the route has no arguments and only the app shell uses it.
- **`PosterTile` extracted to `cmp/designsystem`** (from user; rejected: a copy per module). It draws the image
  (crop) or the placeholder (`surfaceVariant` fill, name in white SemiBold 14sp, centered, 8dp padding, up to 4
  lines); size and shape come from the caller. `PosterRow` keeps its size, click and semantics and calls it.
- **EC-2: no double-tap guard, as on Home** (from user; rejected: a `RESUMED` check for favorites only, or for
  all navigation). A very quick double tap may open details or My favorites twice. EC-2 is recorded as not met.
- **pt-BR title of My favorites: "Meus favoritos"** (from user; rejected: keeping "My Favorites" from the design).
  Answers the open question in task.md.
- **Home state**: `HomeUiState` gets `favorites: List<FavoriteTitle> = emptyList()` (at most 10) and
  `favoritesCount: Int = 0`. The section is drawn only when `favoritesCount > 0` (AC-2). It does not depend on
  the selected category (AC-1). New intents `FavoriteClicked(id: TitleId)`, `SeeAllFavoritesClicked`; new effects
  `OpenFavorite(id: TitleId)`, `OpenFavorites`.
- **Navigation callbacks** take a `TitleId` for favorites (a favorite may have no data yet, so there is no `Title`
  to pass): `homeScreen(onTitleClick: (Title) -> Unit, onFavoriteClick: (TitleId) -> Unit, onSeeAllFavorites:
  () -> Unit)`, `favoritesScreen(onBack: () -> Unit, onTitleClick: (TitleId) -> Unit)`. The shell navigates with
  the existing `TitleId.toDetailsRoute()`.
- **Favorite with no data** (`summary == null`): placeholder tile with no name, no name or meta text below it;
  screen reader label "Loading title" / "Carregando título". It still opens details.
- **Meta line** "year · first genre": `metaLine(year: Int?, genre: String?)` in `cmp/designsystem` (pure function,
  shared by both modules); unknown parts are left out with their separator (AC-4).
- **Design values with no theme token** stay as private constants in the module that uses them, as
  `DetailsColors` does in title details: card `#211A30`, card border `#312A3F`, badge fill `#381A39`, page
  background of My favorites `#130E1F`, secondary text `#A6A1B5`. Pink is `MaterialTheme.colorScheme.secondary`
  (`#FF1F8A`). Sizes are the design's ≈ values.
- **Design gaps settled here**: My favorites' header (Back, badge, title, subtitle) scrolls with the list, as one
  `LazyColumn`; names in cards wrap to 2 lines then ellipsis; the Home badge is a 22dp circle that grows into a
  pill (horizontal padding 6dp) for two or more digits.
- **Icons** (Figma assets missing): the back arrow is copied from
  `cmp/feature/title-details/ui/.../drawable/ic_back_arrow.xml`; the "View all" chevron is a hand-written 12dp
  vector `ic_chevron_right.xml` in `home/ui`, tinted with `secondary`.
- **Badge text** uses Compose plurals: `favorites_count` ("%d title" / "%d titles"; "%d título" / "%d títulos").
- **Language** as on Home: the screen sends `LanguageChanged(Language.fromLocaleTag(Locale.current...))`; the
  ViewModel `flatMapLatest`s `observeFavoriteTitles(language)` (EC-9).
- **State restoration**: My favorites' list state is `rememberLazyListState()` (saveable, EC-7); the Home
  favorites row uses `rememberSaveable(saver = LazyListState.Saver)`, like the other Home rows.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Favorites with title data, newest first, language fallback | `ObserveFavoriteTitles` (bound in `titleDataModule`) | `service/title/domain` |
| Favorites test double | `FakeFavoritesRepository`, `FakeTitleDetailsRepository` (real use case on top) | `service/favorites/fake`, `service/title/fake` |
| Poster image / placeholder | `PosterRow`'s private `Poster` → extracted as `PosterTile` | `cmp/designsystem/.../component/PosterRow.kt` |
| Theme colors (pink, plum placeholder) | `TmdbTheme` (`secondary`, `surfaceVariant`) | `cmp/designsystem/.../Theme.kt` |
| MVI shape, language intent, effects channel | `HomeViewModel`, `HomeScreen` | `cmp/feature/home/ui` |
| Back arrow drawable | `ic_back_arrow.xml` | `cmp/feature/title-details/ui/src/commonMain/composeResources/drawable` |
| Details navigation | `TitleId.toDetailsRoute()` | `cmp/feature/title-details/ui/.../navigation/TitleDetailsRoute.kt` |
| Main dispatcher in ViewModel tests | `MainDispatcherTest` | `core/testing` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `cmp/designsystem` | `component/PosterTile.kt` (new), `component/PosterRow.kt` (modify), `text/MetaLine.kt` (new), `src/androidHostTest/.../text/MetaLineTest.kt` (new) | Shared poster and meta line |
| `cmp/feature/home/ui` | `HomeContract.kt`, `HomeViewModel.kt`, `HomeScreen.kt`, `navigation/HomeNavigation.kt`, `build.gradle.kts` (modify); `component/FavoritesSection.kt`, `drawable/ic_chevron_right.xml` (new); strings en + pt-BR (modify); tests (modify + new `HomeViewModelFavoritesTest.kt`) | Home section |
| `cmp/feature/favorites/ui` (new) | `build.gradle.kts`, `FavoritesContract.kt`, `FavoritesViewModel.kt`, `FavoritesScreen.kt`, `component/FavoriteCard.kt`, `FavoritesColors.kt`, `navigation/FavoritesRoute.kt`, `navigation/FavoritesNavigation.kt`, `di/FavoritesUiModule.kt`, strings en + pt-BR, `drawable/ic_back_arrow.xml`, `FavoritesViewModelTest.kt` | My favorites |
| `settings.gradle.kts` | add `:cmp:feature:favorites:ui` | New module |
| `cmp/composeApp` | `build.gradle.kts`, `AppNavHost.kt`, `di/CmpAppModule.kt` (modify) | Wiring (Step 4) |
| Docs | `CLAUDE.md`; `docs/features/favorites/task.md`, `design/my-favorites.md` (pt-BR title) | Current state, reuse, copy |

## Steps
### Step 1: `PosterTile` and `metaLine` in the design system  (AC-4) ✅
- Files: `cmp/designsystem/src/commonMain/kotlin/com/banhmilabs/tmdb/cmp/designsystem/component/PosterTile.kt`
  (new), `component/PosterRow.kt` (modify), `text/MetaLine.kt` (new),
  `cmp/designsystem/src/androidHostTest/kotlin/com/banhmilabs/tmdb/cmp/designsystem/text/MetaLineTest.kt` (new)
- What:
  - `@Composable fun PosterTile(name: String?, posterUrl: String?, modifier: Modifier = Modifier)`: `Box` with
    `surfaceVariant` background, `contentAlignment = Center`; `AsyncImage` (crop, `contentDescription = null`)
    when `posterUrl != null`, else the placeholder `Text` (when `name != null`) with `clearAndSetSemantics {}`.
    Size, clip shape, click and semantics come from `modifier`.
  - `PosterRow`'s private `Poster` keeps size, shape, semantics and click and calls `PosterTile` inside.
  - `fun metaLine(year: Int?, genre: String?): String = listOfNotNull(year?.toString(), genre).joinToString(" · ")`.
- Unit tests: `MetaLineTest`: `yearAndGenreAreJoinedWithDot`, `missingYearLeavesGenreOnly`,
  `missingGenreLeavesYearOnly`, `bothMissingIsEmpty`.
- Compile: `./gradlew :cmp:designsystem:compileAndroidMain :cmp:designsystem:compileAndroidHostTest :cmp:feature:home:ui:compileAndroidMain :cmp:feature:title-details:ui:compileAndroidMain`

### Step 2: Home favorites state  (AC-1, AC-2, AC-3, AC-5, AC-11, AC-12, EC-3, EC-4, EC-9) ✅
- Files: `cmp/feature/home/ui/.../HomeContract.kt`, `HomeViewModel.kt`, `build.gradle.kts` (modify); tests
  `HomeViewModelTest.kt`, `HomeViewModelRefreshTest.kt`, `HomeViewModelSelectionTest.kt`, `HomeTestFixtures.kt`
  (modify), `HomeViewModelFavoritesTest.kt` (new)
- What:
  - `build.gradle.kts`: `androidHostTest` adds `implementation(projects.service.favorites.fake)`.
  - Contract: `HomeUiState.favorites: List<FavoriteTitle> = emptyList()`, `favoritesCount: Int = 0`;
    `HomeIntent.FavoriteClicked(val id: TitleId)`, `HomeIntent.SeeAllFavoritesClicked`;
    `HomeEffect.OpenFavorite(val id: TitleId)`, `HomeEffect.OpenFavorites`.
  - `HomeViewModel(repository, observeFavoriteTitles: ObserveFavoriteTitles, savedStateHandle)`; Koin
    `viewModelOf(::HomeViewModel)` resolves the use case.
  - `HOME_FAVORITES_LIMIT = 10`.
- Pseudo-code:
  ```kotlin
  private val favorites = language.filterNotNull().flatMapLatest { observeFavoriteTitles(it) }
      .onStart { emit(emptyList()) }

  val state = combine(listsState, favorites) { lists, all ->
      lists.copy(favorites = all.take(HOME_FAVORITES_LIMIT), favoritesCount = all.size)
  }.stateIn(...)
  ```
  where `listsState` is today's `selection.flatMapLatest { ... }` flow.
- Fixtures: `homeViewModel(repository, savedState, favorites = FakeFavoritesRepository(), details =
  FakeTitleDetailsRepository())` builds the ViewModel with the real `ObserveFavoriteTitles`; existing tests use it.
- Unit tests: `HomeViewModelFavoritesTest`: `noFavoritesGivesEmptySection`,
  `showsTenNewestFavoritesWithTotalCount` (12 favorites → 10 newest, count 12), `sectionIsTheSameForEveryCategory`,
  `favoritesUpdateWhenOneIsAddedOrRemoved`, `favoritesFollowLanguageChange` (pt-BR summary, then en-US),
  `favoriteClickOpensFavorite`, `seeAllOpensFavorites`.
- Compile: `./gradlew :cmp:feature:home:ui:compileAndroidMain :cmp:feature:home:ui:compileAndroidHostTest`

### Step 3: My favorites module  (AC-3, AC-4, AC-5, AC-11, AC-12, EC-3, EC-5, EC-7, EC-9) ✅
- Files: new module `cmp/feature/favorites/ui/`: `build.gradle.kts`;
  `src/commonMain/kotlin/com/banhmilabs/tmdb/cmp/feature/favorites/ui/{FavoritesContract.kt, FavoritesViewModel.kt,
  FavoritesScreen.kt, FavoritesColors.kt}`, `component/FavoriteCard.kt`, `navigation/{FavoritesRoute.kt,
  FavoritesNavigation.kt}`, `di/FavoritesUiModule.kt`; `composeResources/{values,values-pt-rBR}/strings.xml`,
  `composeResources/drawable/ic_back_arrow.xml`; `src/androidHostTest/.../FavoritesViewModelTest.kt`;
  `settings.gradle.kts` (modify)
- What:
  - `build.gradle.kts` as `home/ui`: `tmdb.module`; `commonMain`: `api(projects.core.model)`,
    `projects.service.title.domain`, `projects.cmp.designsystem`, navigation-compose, lifecycle viewmodel/runtime
    compose, serialization core, koin compose viewmodel, coil compose; `androidHostTest`:
    `projects.service.title.fake`, `projects.service.favorites.fake`, `projects.core.testing`, turbine.
  - Contract: `FavoritesUiState(val favorites: List<FavoriteTitle> = emptyList())` (count = size);
    `FavoritesIntent { LanguageChanged(language), TitleClicked(id: TitleId) }`; `FavoritesEffect { OpenTitle(id) }`.
  - `FavoritesViewModel(observeFavoriteTitles)`: `language.filterNotNull().flatMapLatest { observe(it) }` →
    `stateIn(WhileSubscribed(5_000))`; effects via buffered `Channel`.
  - `FavoritesScreen(onBack, onTitleClick, viewModel)` / `FavoritesContent(state, onBack, onIntent)`: background
    `#130E1F`; one `LazyColumn` (safe-drawing insets, side padding 20dp): header row (Back: arrow + "Back" bold
    16sp, `clickable(role = Button)`; badge pill `#211A30` with `favorites_count` in `secondary` bold 12sp), title
    bold 26sp (`heading()`), subtitle 13sp `#A6A1B5`, then `items(key = id)` of `FavoriteCard` spaced 12dp.
  - `FavoriteCard(favorite, onClick)`: 120dp high, radius 12dp, `#211A30`, padding 12dp; `PosterTile` 64×96dp
    radius 6dp; 14dp gap; name SemiBold 16sp (2 lines, ellipsis), meta 13sp `#A6A1B5`; whole card
    `clickable(role = Button)` labelled with the name (or "Loading title"). No heart (03).
  - `@Serializable data object FavoritesRoute`; `fun NavGraphBuilder.favoritesScreen(onBack, onTitleClick)`.
  - `val favoritesUiModule = module { viewModelOf(::FavoritesViewModel) }`.
  - Strings: `favorites_back` Back / Voltar, `favorites_title` My Favorites / Meus favoritos, `favorites_subtitle`
    All your saved titles, in one place. / Todos os seus títulos salvos, em um só lugar., `favorites_count`
    plurals, `favorites_loading` Loading title / Carregando título.
  - `settings.gradle.kts`: `":cmp:feature:favorites:ui"` after `":cmp:feature:home:ui"`.
- Unit tests: `FavoritesViewModelTest` (extends `MainDispatcherTest`, real use case on the two fakes):
  `listsAllFavoritesNewestFirst`, `emptyWithoutFavorites`, `updatesWhenFavoriteRemoved`,
  `followsLanguageChange`, `titleClickOpensTitle`.
- Compile: `./gradlew :cmp:feature:favorites:ui:compileAndroidMain :cmp:feature:favorites:ui:compileAndroidHostTest`

### Step 4: Home favorites section UI and app shell wiring  (AC-1, AC-2, AC-3, AC-4, AC-5, AC-12, EC-4, EC-5) ✅
- Files: `cmp/feature/home/ui/.../component/FavoritesSection.kt` (new), `HomeScreen.kt`,
  `navigation/HomeNavigation.kt` (modify), `composeResources/drawable/ic_chevron_right.xml` (new),
  `composeResources/values/strings.xml`, `values-pt-rBR/strings.xml` (modify); `cmp/composeApp/build.gradle.kts`,
  `AppNavHost.kt`, `di/CmpAppModule.kt` (modify)
- What:
  - `FavoritesSection(favorites, count, onFavoriteClick: (TitleId) -> Unit, onSeeAll: () -> Unit, modifier)`:
    card ≈18dp from the screen edges (`ScreenPadding - 12.dp`), radius 16dp, fill `#211A30`, 1dp border `#312A3F`,
    inner padding 12dp; header row (title Bold 20sp, badge, "View all ›" SemiBold 14sp `secondary`, `heading()` on
    the title), subtitle 13sp `#A6A1B5`, `LazyRow` of items 97×147dp spaced 12dp, with name (SemiBold 14sp, 2
    lines, ellipsis) and `metaLine` (12sp `#A6A1B5`); positions per `design/home-favorites.md`.
  - Item: `PosterTile` with `RoundedCornerShape(8.dp)`; whole item `clickable(role = Button)` with
    `contentDescription` = name (or "Loading title"); `summary == null` per Decisions.
  - "View all" row is one `clickable(role = Button)` with `minimumInteractiveComponentSize()`.
  - `HomeScreen(onTitleClick, onFavoriteClick, onSeeAllFavorites, viewModel)` collects the two new effects.
  - `HomeRows`: after the featured `TitleRow`, when `favoritesCount > 0`: spacer ≈28dp, `FavoritesSection`, spacer
    ≈32dp before "Most popular"; otherwise unchanged (`FeaturedToHeading`). While both rows load (the full
    spinner), the section is drawn below the spinner.
  - `homeScreen(onTitleClick, onFavoriteClick, onSeeAllFavorites)`.
  - Strings: `home_favorites_title` Favorites / Favoritos, `home_favorites_subtitle` Saved to watch whenever you
    like. / Salvos para assistir quando quiser., `home_favorites_view_all` View all / Ver todos,
    `home_favorites_loading` Loading title / Carregando título.
  - App shell (changed in the same step so `homeScreen`'s new parameters compile):
    - `implementation(projects.cmp.feature.favorites.ui)`.
    - `cmpAppModule` includes `favoritesUiModule`.
    - `AppNavHost`: `val openTitleId: (TitleId) -> Unit = { navController.navigate(it.toDetailsRoute()) }`;
      `homeScreen(onTitleClick = openTitle, onFavoriteClick = openTitleId, onSeeAllFavorites = {
      navController.navigate(FavoritesRoute) })`; `favoritesScreen(onBack = navController::navigateUp,
      onTitleClick = openTitleId)`.
- Unit tests: none (composables; covered by Step 2 state and the manual checks).
- Compile: `./gradlew :cmp:feature:home:ui:compileAndroidMain :cmp:composeApp:compileAndroidMain :cmp:composeApp:compileKotlinIosSimulatorArm64`

### Step 5: Docs ✅
- Files: `CLAUDE.md`; `docs/features/favorites/task.md`; `docs/features/favorites/design/my-favorites.md`
- What: `CLAUDE.md` current state and module list mention `cmp/feature/favorites/ui` and the Home favorites
  section; reuse table gets `PosterTile` and `metaLine` (`cmp/designsystem`). task.md: tick the pt-BR title open
  question ("Meus favoritos", 2026-11-23) with a Change log line; `my-favorites.md` Copy table and Gaps updated
  the same way.
- Compile: none.

After the last step: `./gradlew :cmp:designsystem:testAndroidHostTest :cmp:feature:home:ui:testAndroidHostTest :cmp:feature:favorites:ui:testAndroidHostTest :cmp:composeApp:testAndroidHostTest`
and `detekt` on the same modules, `./gradlew :architecture-test:test` (new module, new imports).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 | 2, 4 | `HomeViewModelFavoritesTest.showsTenNewestFavoritesWithTotalCount`, `sectionIsTheSameForEveryCategory`; manual: section between the rows |
| AC-2 | 2, 4 | `HomeViewModelFavoritesTest.noFavoritesGivesEmptySection`; manual: no section |
| AC-3 | 2, 3, 4 | `HomeViewModelFavoritesTest.seeAllOpensFavorites`; `FavoritesViewModelTest.listsAllFavoritesNewestFirst`; manual: badge plurals, back |
| AC-4 (without heart) | 1, 3, 4 | `MetaLineTest`; manual: placeholder tile |
| AC-5 | 2, 3, 4 | `HomeViewModelFavoritesTest.favoriteClickOpensFavorite`; `FavoritesViewModelTest.titleClickOpensTitle`; manual: back keeps position |
| AC-11 (screens) | 2, 3 | Data covered in 01; manual: airplane mode |
| AC-12 (copy) | 3, 4 | `favoritesFollowLanguageChange`, `followsLanguageChange`; manual: en / pt-BR copy |
| EC-2 | — | Not met by decision (no guard, as on Home) |
| EC-3 | 2, 3 | `favoritesUpdateWhenOneIsAddedOrRemoved`, `updatesWhenFavoriteRemoved` |
| EC-4 | 2, 4 | `showsTenNewestFavoritesWithTotalCount` |
| EC-5 (truncation) | 3, 4 | Manual: long name truncated |
| EC-7 | 3 | Manual: rotate on My favorites |
| EC-9 | 2, 3 | `favoritesFollowLanguageChange`, `followsLanguageChange` |

## Out of scope
- Hearts, the confirmation popup, removing, the My favorites empty message, screen reader labels for hearts → 03.
- Native apps → 04, 05.
- A double-tap guard (EC-2), by decision.

## Risks
- Heart, chevron and exact Figma values are missing (Figma MCP rate limit); sizes are the ≈ values. The chevron is
  hand-drawn; replace it when the Figma asset is available (owner: user, after the limit resets).
- Home's `HomeRows` spacing changes only when favorites exist; check the existing Home layout without favorites is
  pixel-identical (manual).
- Each favorite runs two summary queries (see plan 01 Risks); Home and My favorites can both be subscribed while
  My favorites is on top of Home. Fine for tens of favorites.

## Deviations
- Step 2 did not compile on its own: `HomeScreen`'s `when` over effects needs the two new effects handled, which
  is Step 4 work. Step 3 was done next, then Steps 2 and 4 were compiled together.
- `cmp/feature/favorites/ui` doesn't depend on Coil: `PosterTile` (in `cmp/designsystem`) draws the images.
- `HomeScreen` gets a private `Favorites(state, onIntent)` helper that draws the spacer and `FavoritesSection`
  only when `favoritesCount > 0`; used both after the featured row and below the full spinner.
- `CLAUDE.md`'s repository-layout line now lists the real title details screen instead of "a placeholder title
  details screen", in the same sentence that adds My favorites.
