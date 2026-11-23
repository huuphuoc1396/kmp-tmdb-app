# Plan: Favorites list data (shared core)

Requirement: [01 - Favorites list data (shared core)](../subtasks/01-favorites-list-data.md)
Status: Done

## Summary
`FavoritesRepository` gets `observeFavorites()`: every favorite ID, newest first. `TitleDetailsRepository` gets
`observeSummary(id, language)`: one light query for a title's name, release date, poster (never the backdrop) and
first genre. A use case `ObserveFavoriteTitles` in `service:title:domain` combines the two: for each favorite it
shows the summary in the current language, falls back to the other language, and refreshes the title once when
the current language is missing. A favorite with no data in either language stays in the list with a `null`
summary. The use case is bound in Koin and exported by `SharedCore` for iOS; native Android gets it from the
published `service-title-domain`.

## Decisions
- **Combine in a use case in `service:title:domain`** (from user; rejected: combining in each ViewModel, which
  would repeat the fallback and refresh logic in cmp, native-android and Swift). It combines two repositories and
  applies a rule, so it qualifies as a use case (CLAUDE.md). It uses the existing `title → favorites` pair of
  `DependencyRules.domainAllowlist`; no build-logic change.
- **Light summary query** (from user; rejected: reusing `observe()`, which runs four queries per title and
  language, including cast and recommendations). New SQL `selectSummary` over the existing tables; **no schema
  change and no migration**.
- **Refresh only when the current language is missing** (from user; rejected: also when older than 24 h). Opening
  Home or My favorites sends no request when data is present; stale data is refreshed when the user opens the
  title's details. The refresh is `refresh(id, language, force = false)`; `KeyedRefreshes` already de-duplicates
  concurrent refreshes of the same title. Its result is ignored: a failure keeps the fallback (AC-11, no error).
- **Poster only**: `TitleSummary.posterUrl` is set only when the stored artwork is a poster
  (`artworkIsPoster`); a title with only a backdrop gets `null`, and the UI draws the placeholder (task.md AC-4).
- **Models** in `service:title:domain` (used only by the `title` context and its consumers, so not `core:model`):
  ```kotlin
  data class TitleSummary(
      val id: TitleId,
      val name: String,
      val posterUrl: String?,
      val releaseYear: Int?,
      val firstGenre: String?,
  )

  data class FavoriteTitle(val id: TitleId, val summary: TitleSummary?)
  ```
  `summary == null` means no stored data in either language yet (AC-12: never hidden).
- **Order**: `ORDER BY addedAt DESC, tmdbId DESC, type DESC`, so equal timestamps still give a stable order.
- **The other language** is the one `Language` entry that is not the current one
  (`Language.entries.first { it != language }`).
- **Binding**: `factory { ObserveFavoriteTitles(favorites = get(), details = get()) }` in `titleDataModule`, the
  `title` context's Koin module; `core:di` already includes it.
- **iOS**: `SharedCore.observeFavoriteTitles` returns the use case; Swift calls `invoke(language:)` and collects
  the flow through SKIE, as it does for the repositories.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Favorite IDs and `addedAt` | `favorite` table, `FavoritesLocalDataSource` | `service/favorites/data/.../local/` |
| Stored title data per language | `titleDetails`, `titleDetailsGenre` tables | `service/title/data/src/commonMain/sqldelight/.../TitleDetails.sq` |
| Refreshing one title (with de-duplication) | `TitleDetailsRepository.refresh`, `KeyedRefreshes` | `service/title/domain`, `service/title/data` |
| Poster URL from a path | `TmdbImages.poster` | `service/title/data/.../TmdbImages.kt` |
| Language | `Language` | `core/model` |
| Repository fakes and contracts | `FakeFavoritesRepository` + `FavoritesRepositoryContract`, `FakeTitleDetailsRepository` + `TitleDetailsRepositoryContract` | `service/favorites/fake`, `service/title/fake` |
| In-memory database in tests | `JdbcSqliteDriver` setup in `TitleDetailsTestEnvironment`, `FavoritesRepositoryImplTest` | `service/title/data`, `service/favorites/data` `androidHostTest` |
| iOS entry point | `SharedCore` | `core/ios` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `service:favorites:domain` | `FavoritesRepository.kt` (modify) | `observeFavorites()` |
| `service:favorites:data` | `Favorite.sq` (modify), `FavoritesLocalDataSource.kt`, `FavoritesRepositoryImpl.kt` (modify) | Newest-first query |
| `service:favorites:fake` | `FakeFavoritesRepository.kt`, `FavoritesRepositoryContract.kt` (modify) | Ordered fake and contract cases |
| `service:title:domain` | `TitleSummary.kt`, `FavoriteTitle.kt`, `ObserveFavoriteTitles.kt` (new); `TitleDetailsRepository.kt` (modify); `build.gradle.kts` (modify) | Summary API and the use case |
| `service:title:data` | `TitleDetails.sq`, `TitleDetailsLocalDataSource.kt`, `TitleDetailsRepositoryImpl.kt`, `di/TitleDataModule.kt` (modify) | Summary query, binding |
| `service:title:fake` | `FakeTitleDetailsRepository.kt`, `TitleDetailsRepositoryContract.kt` (modify) | Summary in fake and contract |
| `core:ios` | `SharedCore.kt` (modify), `SharedCoreTest.kt` (modify) | Export the use case |
| Affected (interfaces changed) | `cmp/feature/home/ui`, `cmp/feature/title-details/ui`, `core:di` compile against the new interfaces; no code change | Recompiled |
| Docs | `CLAUDE.md` | Reuse table and current state |

## Steps
### Step 1: Favorites in order  (AC-11, EC-8) ✅
- Files: `service/favorites/domain/.../FavoritesRepository.kt`, `service/favorites/data/src/commonMain/sqldelight/.../Favorite.sq`
  (the file holding the `favorite` table), `.../local/FavoritesLocalDataSource.kt`, `.../FavoritesRepositoryImpl.kt`
  (modify)
- What:
  - `FavoritesRepository`: add `fun observeFavorites(): Flow<List<TitleId>>`.
  - SQL `selectAllNewestFirst: SELECT tmdbId, type FROM favorite ORDER BY addedAt DESC, tmdbId DESC, type DESC;`
  - `FavoritesLocalDataSource.observeAll(): Flow<List<TitleId>>` (`asFlow().mapToList(io)`, mapper to
    `TitleId(tmdbId, TitleType.valueOf(type))`, `distinctUntilChanged`, `flowOn(io)`, same shape as `observe`).
  - `FavoritesRepositoryImpl.observeFavorites() = local.observeAll()`.
- Unit tests: covered by the contract in Step 2 (`FavoritesRepositoryImplTest` extends it).
- Compile: `./gradlew :service:favorites:domain:compileAndroidMain :service:favorites:data:compileAndroidMain`

### Step 2: Favorites fake and contract  (AC-11, EC-8) ✅
- Files: `service/favorites/fake/.../FakeFavoritesRepository.kt`, `FavoritesRepositoryContract.kt` (modify);
  `service/favorites/data/src/androidHostTest/.../FavoritesRepositoryImplTest.kt` (modify)
- What:
  - Fake: state becomes `MutableStateFlow<List<TitleId>>`, newest first; `setFavorite(true)` prepends when absent
    (keeps position when present, like `INSERT OR IGNORE`), `setFavorite(false)` removes. The constructor keeps
    `initial: Set<TitleId> = emptySet()`, read in iteration order as newest first, so the existing cmp tests
    (`FakeFavoritesRepository(setOf(...))` in `cmp/feature/title-details/ui`) compile unchanged.
  - Contract cases: `observeFavoritesIsEmptyByDefault`, `observeFavoritesListsNewestFirst`,
    `refavoritingKeepsPosition`, `removedFavoriteLeavesList`, `observeFavoritesSeesChanges` (Turbine).
  - `FavoritesRepositoryImplTest`: the MockK clock advances 1 ms on every `now()` so insert order is strictly
    increasing.
- Compile: `./gradlew :service:favorites:fake:compileAndroidMain :service:favorites:data:compileAndroidHostTest`

### Step 3: Title summary API and query  (AC-11, AC-12, EC-10) ✅
- Files: `service/title/domain/.../TitleSummary.kt` (new), `TitleDetailsRepository.kt` (modify);
  `service/title/data/.../TitleDetails.sq`, `details/local/TitleDetailsLocalDataSource.kt`,
  `details/TitleDetailsRepositoryImpl.kt` (modify)
- What:
  - `TitleDetailsRepository`: add `fun observeSummary(id: TitleId, language: Language): Flow<TitleSummary?>`.
  - SQL:
    ```sql
    selectSummary:
    SELECT d.name, d.releaseDate, d.artworkPath, d.artworkIsPoster,
        (SELECT g.name FROM titleDetailsGenre g
         WHERE g.tmdbId = d.tmdbId AND g.type = d.type AND g.language = d.language
         ORDER BY g.position LIMIT 1) AS firstGenre
    FROM titleDetails d
    WHERE d.tmdbId = ? AND d.type = ? AND d.language = ?;
    ```
  - `TitleDetailsLocalDataSource.observeSummary(key: TitleDetailsKey): Flow<TitleSummary?>`
    (`mapToOneOrNull(io)`, `distinctUntilChanged`, `flowOn(io)`); mapping: `posterUrl = artworkPath` through
    `TmdbImages.poster` only when `artworkIsPoster != 0`, `releaseYear = releaseDate?.let(LocalDate::parse)?.year`.
  - `TitleDetailsRepositoryImpl.observeSummary` delegates to the local data source.
- Unit tests: `TitleDetailsLocalDataSourceTest`: `summaryIsNullWhenNotStored`, `summaryHasNameYearPosterAndFirstGenre`,
  `summaryHasNoPosterWhenArtworkIsBackdrop`, `summaryHasNoGenreWhenNoneStored`, `summaryIsPerLanguage`,
  `summaryUpdatesAfterUpsert`.
- Compile: `./gradlew :service:title:domain:compileAndroidMain :service:title:data:compileAndroidMain :service:title:data:compileAndroidHostTest`

### Step 4: Title fake and contract  (AC-12) ✅
- Files: `service/title/fake/.../FakeTitleDetailsRepository.kt`, `TitleDetailsRepositoryContract.kt` (modify)
- What:
  - Fake `observeSummary` maps its cache: `TitleSummary(id, name, posterUrl = artworkUrl, releaseYear,
    firstGenre = genres.firstOrNull())`. (The fake has no poster/backdrop distinction; that rule is tested on the
    real data source in Step 3.)
  - Contract cases: `observeSummaryIsNullBeforeRefresh`, `refreshStoresSummaryForObserver` (name, year, first
    genre of `CONTRACT_MOVIE_DETAILS`), `summaryIsKeptPerLanguage`.
- Compile: `./gradlew :service:title:fake:compileAndroidMain :service:title:fake:compileAndroidHostTest :service:title:data:compileAndroidHostTest`

### Step 5: `ObserveFavoriteTitles` use case  (AC-11, AC-12, EC-8, EC-10) ✅
- Files: `service/title/domain/.../FavoriteTitle.kt`, `ObserveFavoriteTitles.kt` (new);
  `service/title/domain/build.gradle.kts` (modify)
- What:
  - `build.gradle.kts`: `commonMain` adds `api(projects.service.favorites.domain)` (the use case's constructor
    exposes `FavoritesRepository`); `androidHostTest` adds `projects.service.title.fake`,
    `projects.service.favorites.fake`, `libs.kotlinx.coroutines.test`, `libs.turbine`.
  - `class ObserveFavoriteTitles(favorites: FavoritesRepository, details: TitleDetailsRepository)` with
    `operator fun invoke(language: Language): Flow<List<FavoriteTitle>>`.
- Pseudo-code:
  ```kotlin
  operator fun invoke(language: Language) = favorites.observeFavorites().flatMapLatest { ids ->
      if (ids.isEmpty()) flowOf(emptyList())
      else combine(ids.map { observeOne(it, language) }) { it.toList() }
  }

  private fun observeOne(id: TitleId, language: Language) = channelFlow {
      val other = Language.entries.first { it != language }
      launch { if (details.observeSummary(id, language).first() == null) details.refresh(id, language) }
      combine(details.observeSummary(id, language), details.observeSummary(id, other)) { current, fallback ->
          FavoriteTitle(id, current ?: fallback)
      }.collect { send(it) }
  }.distinctUntilChanged()
  ```
- Unit tests: `ObserveFavoriteTitlesTest` (fakes from both contexts, Turbine):
  `emitsEmptyListWithoutFavorites`, `listsFavoritesNewestFirst`, `usesCurrentLanguageSummary`,
  `fallsBackToOtherLanguageThenReplacesAfterRefresh`, `keepsFavoriteWithNullSummaryWhenNoData`,
  `keepsFallbackWhenRefreshFails`, `refreshesOnlyTitlesMissingCurrentLanguage` (checks
  `FakeTitleDetailsRepository.refreshRequests`, `force = false`), `removedFavoriteLeavesList`,
  `sameTitleAppearsOnce`.
- Compile: `./gradlew :service:title:domain:compileAndroidMain :service:title:domain:compileAndroidHostTest`

### Step 6: Bind and export  (AC-11, AC-12) ✅
- Files: `service/title/data/.../di/TitleDataModule.kt`, `core/ios/.../SharedCore.kt`,
  `core/ios/src/iosTest/.../SharedCoreTest.kt` (modify)
- What:
  - `titleDataModule`: `factory { ObserveFavoriteTitles(favorites = get(), details = get()) }`.
  - `SharedCore`: `val observeFavoriteTitles: ObserveFavoriteTitles` with the same `checkNotNull(koin)` getter
    as the repositories.
  - `SharedCoreTest`: `observeFavoriteTitlesIsAvailableAfterStart`.
- Compile: `./gradlew :service:title:data:compileAndroidMain :core:di:compileAndroidMain :core:ios:compileKotlinIosSimulatorArm64 :cmp:feature:home:ui:compileAndroidMain :cmp:feature:title-details:ui:compileAndroidMain`

### Step 7: Docs ✅
- Files: `CLAUDE.md` (modify)
- What: "Reuse before you create" gets a row "Favorites with title data (newest first, language fallback) |
  `ObserveFavoriteTitles` | `service:title:domain`"; the `FavoritesRepository` row mentions `observeFavorites`;
  "Current state" mentions the favorites list use case.
- Compile: none (docs).

After the last step: `./gradlew :service:favorites:data:testAndroidHostTest :service:favorites:fake:testAndroidHostTest :service:title:domain:testAndroidHostTest :service:title:data:testAndroidHostTest :service:title:fake:testAndroidHostTest`,
`detekt` on the same modules plus `:core:ios`, `./gradlew :architecture-test:test`, `./gradlew koverVerify`, and
`./gradlew :core:ios:iosSimulatorArm64Test` (macOS).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-11 (data: offline, stored data) | 1, 3, 5 | `FavoritesRepositoryContract` cases; `TitleDetailsLocalDataSourceTest.summaryHasNameYearPosterAndFirstGenre`; `ObserveFavoriteTitlesTest.keepsFallbackWhenRefreshFails` |
| AC-11 (removing offline) | 1, 2 | `FavoritesRepositoryContract.removedFavoriteLeavesList` (local only, no network) |
| AC-12 (data in current language, fallback, never hidden) | 3, 4, 5, 6 | `ObserveFavoriteTitlesTest.usesCurrentLanguageSummary`, `fallsBackToOtherLanguageThenReplacesAfterRefresh`, `keepsFavoriteWithNullSummaryWhenNoData`, `refreshesOnlyTitlesMissingCurrentLanguage`; `TitleDetailsLocalDataSourceTest.summaryIsPerLanguage`; `SharedCoreTest.observeFavoriteTitlesIsAvailableAfterStart` |
| EC-8 | 1, 5 | `FavoritesRepositoryContract.refavoritingKeepsPosition`; `ObserveFavoriteTitlesTest.sameTitleAppearsOnce` |
| EC-10 (data) | 3, 5 | `ObserveFavoriteTitlesTest.keepsFallbackWhenRefreshFails` (refresh returns `NotFound`, stored data stays) |

## Out of scope
- Screens, the 10-item limit on Home, the count badge -> 02-cmp-browse-favorites (the count is the list size).
- The confirmation popup -> 03-cmp-remove-favorite.
- Native apps -> 04, 05; this plan only makes the use case available to them.
- Refreshing stale (older than 24 h) favorites from the list; done by Title details as today.

## Risks
- `title:domain`'s host tests depend on `title:fake`, which depends on `title:domain` — a test-scope loop at the
  project level (no task cycle). If Gradle or `tmdb.module` rejects it, move `ObserveFavoriteTitlesTest` to
  `service:title:fake`'s `androidHostTest` and note it as a deviation.
- Each favorite holds two summary flows; SQLDelight re-runs every summary query on any write to `titleDetails` or
  `titleDetailsGenre`. Fine for tens of favorites; if it shows up in profiling, batch the summaries in one query.
- Every resubscription (e.g. the ID list changes) re-attempts the refresh of titles still missing the current
  language; offline this is a failed request each time. `KeyedRefreshes` prevents parallel duplicates.
- `native-android/app/src/test/.../titledetails/TitleDetailsTestRepositories.kt` builds its own
  `FavoritesRepository` and `TitleDetailsRepository` objects. After `publishSharedCore`, they miss
  `observeFavorites` / `observeSummary` and native-android's unit tests stop compiling until 04 adds them. This
  plan doesn't touch `native-android/`; run 04 before republishing for native-android work, or add the two
  overrides there as the first step of 04.
- Swift sees the use case as `invoke(language:)`; if SKIE exposes it differently, 05 adapts the call (no shared-core
  change expected).

## Deviations
- `selectSummary` reads the first genre with `LEFT JOIN titleDetailsGenre ... AND g.position = 0` instead of a
  scalar subquery: SQLDelight typed the subquery column as non-null, so a title without genres crashed
  (`summaryHasNoGenreOrYearWhenNoneStored`). Genres are stored from position 0, so the result is the same.
- `FavoritesRepositoryContract`: `observeFavoritesIsEmptyByDefault` folded into `observeFavoritesSeesChanges`
  (its first item is the empty list), to stay within Detekt's `TooManyFunctions` limit of 11.
- `FavoritesRepositoryImplTest.addedAtIsKeptWhenSetAgain` no longer moves the clock by hand; the clock now
  advances 1 ms on every `now()`, which covers the same case.
- `TitleDetailsLocalDataSourceTest.summaryHasNoGenreWhenNoneStored` is `summaryHasNoGenreOrYearWhenNoneStored`
  (also checks a missing year); `SharedCoreTest` case is `startProvidesObserveFavoriteTitles`, matching the
  existing names.
- The test-scope loop `title:domain` tests → `title:fake` → `title:domain` (Risks) compiled and ran without change.
