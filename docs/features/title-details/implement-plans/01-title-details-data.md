# Plan: Title details data (shared core)

Requirement: [01 - Title details data (shared core)](../subtasks/01-title-details-data.md)
Status: Done

## Summary
Add a `TitleDetailsRepository` to the `title` context, shaped like `TitleListsRepository`: `observe` reads the
SQLDelight cache (per title and language), `refresh` fetches from TMDB when the cache is missing, older than
24 hours or forced, and upserts it; a failure never touches the cache. One TMDB request per title
(`movie/{id}` or `tv/{id}` with `append_to_response=credits,videos,recommendations`) brings everything; the data
module picks the trailer, the first 10 cast members, up to 20 recommendations, the runtime and the rating while
mapping, and stores the result in normalized tables of `TitleDatabase`. A fake with a contract test and the iOS
entry point make it available to the apps; no screen changes.

## Decisions
- New tables go into `TitleList.sq`'s database through a new `TitleDetails.sq`, with **no migration file**; the
  schema stays at version 1 — the app is not released. (From user; rejected: `1.sqm` migration, a separate
  database file.) Installed dev builds must be uninstalled once (see Risks).
- **Normalized tables**: `titleDetails` (one row per title and language, scalar fields + `fetchedAt`),
  `titleDetailsGenre`, `titleDetailsCast`, `titleDetailsRecommendation` (ordered by `position`). (From user;
  rejected: one row with a JSON column.)
- **kotlinx-datetime** is added; the domain exposes the release date as `kotlinx.datetime.LocalDate`. (From user;
  rejected: a hand-written date type.) Version 0.7.1 (its `Instant` moved to `kotlin.time`, which the project
  already uses).
- **Series runtime**: the first value of `episode_run_time`; when empty, `last_episode_to_air.runtime`; otherwise
  unknown. (From user; rejected: `episode_run_time` only.)
- One request per title: `GET movie/{id}` or `GET tv/{id}` with `language`, `append_to_response=credits,videos,
  recommendations` and `include_video_language=<pt|en>,en` — one round trip, one atomic upsert. TMDB filters
  appended `videos` by `language` unless `include_video_language` is sent, so both languages are requested.
- **Trailer**: among videos with `site == "YouTube"` and `type == "Trailer"`, the first one whose `iso_639_1` is
  the requested language (`pt` / `en`), else the first in `en`, else none; within a language an `official`
  video comes first, then TMDB order. Only the chosen YouTube key is stored (column `trailerKey`); the domain
  exposes `Trailer(youtubeKey)` with its watch URL.
- **Rating**: `vote_count == 0` → `null` (shown as "NR"); otherwise `round(vote_average × 10)` as an `Int`
  (4.5 → 45).
- **Release date**: `release_date` (films) or `first_air_date` (series), parsed as ISO `LocalDate`; empty or
  invalid → `null`. The year is derived from it (`TitleDetails.releaseYear`), so year and date are unknown together.
- **Artwork**: `poster_path`, else `backdrop_path`, else `null`, stored as one `artworkPath`; poster at `w500`
  (same base as lists), backdrop at `w780`.
- **Cast**: `credits.cast` sorted by `order`, first 10; photos from `profile_path` at `w185`. Series use
  `credits` (current cast), not `aggregate_credits`.
- **Recommendations**: the first 20 of `recommendations.results`; entries without a name are dropped (as in
  lists); their type comes from `media_type` when present, else the parent title's type.
- **Synopsis**: blank `overview` → `null`, so the app shows the missing-synopsis text.
- **Name**: `title` (films) / `name` (series); a response without a name maps to `AppError.Unknown` and nothing
  is stored.
- **Not found**: `apiCall` already maps HTTP 404 to `AppError.NotFound`; `refresh` returns it and the cache is
  kept (EC-13; the screen decides what to show in 03).
- **Refresh sharing**: `TitleListRefreshes` becomes a generic `KeyedRefreshes<K>` (rename + type parameter), used
  by both repositories with their own instance — same behavior, no duplicated concurrency code.
- **TTL**: `TitleRefreshPolicy` gets `DETAILS_TTL = 24.hours`; `isStale` takes the TTL as a parameter.
- **Fake**: `FakeTitleDetailsRepository` + `TitleDetailsRepositoryContract` now, because 02's ViewModel tests need
  them.
- Design: not linked by this subtask (no UI).

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Result / error type, 404 → NotFound | `AppResult`, `AppError`, `apiCall { }` | `core/common`, `core/network/.../ApiCall.kt` |
| TMDB HTTP client (`ignoreUnknownKeys`) | `createTmdbHttpClient()` (injected `HttpClient`) | `core/network/.../TmdbHttpClient.kt` |
| Title database and driver | `TitleDatabase`, `DatabaseDriverFactory` | `service/title/data` (generated), `core/database` |
| Shared models | `TitleId`, `TitleType`, `Title`, `Language` | `core/model` |
| One refresh per key, outliving the caller | `TitleListRefreshes` → generalized to `KeyedRefreshes<K>` | `service/title/data/.../TitleListRefreshes.kt` |
| Staleness check | `TitleRefreshPolicy` | `service/title/data/.../TitleRefreshPolicy.kt` |
| Poster URL base `w500` | `POSTER_BASE_URL` in the list local data source → moved to a shared `TmdbImages` object | `service/title/data/.../local/TitleListLocalDataSource.kt` |
| Name of films/series | `TmdbTitleDto.displayName` (`title ?: name`) | `service/title/data/.../remote/TmdbTitleDto.kt` |
| Repository + data-source shape, tests | `TitleListsRepositoryImpl`, `TitleTestEnvironment`, `TitleListsRepositoryContract` | `service/title/{data,fake}` |
| Koin module / iOS entry | `titleDataModule`, `SharedCore` | `service/title/data/.../di/`, `core/ios` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| root | `gradle/libs.versions.toml` (modify) | `kotlinx-datetime` 0.7.1 |
| `service:title:domain` | `build.gradle.kts` (modify); `TitleDetails.kt`, `TitleDetailsRepository.kt` (new) | Public model and repository |
| `service:title:data` | `TitleDetails.sq` (new); `TitleDetailsKey.kt`, `TitleDetailsRepositoryImpl.kt`, `local/TitleDetailsLocalDataSource.kt`, `local/TmdbImages.kt`, `remote/TitleDetailsRemoteDataSource.kt`, `remote/TmdbTitleDetailsDto.kt` (new); `TitleListRefreshes.kt` → `KeyedRefreshes.kt`, `TitleRefreshPolicy.kt`, `TitleListsRepositoryImpl.kt`, `local/TitleListLocalDataSource.kt`, `di/TitleDataModule.kt` (modify); tests + fixtures | Fetch, map, cache |
| `service:title:fake` | `FakeTitleDetailsRepository.kt`, `TitleDetailsRepositoryContract.kt` (new); `FakeTitleDetailsRepositoryTest.kt` (new test) | Test double for 02 |
| `core:ios` | `SharedCore.kt` (modify) | `titleDetailsRepository` for Swift |
| Affected (compile only) | `core:di`, `cmp/*`, `native-*` through the published core | Domain API grows; nothing else changes |

## Steps
### Step 1: Add kotlinx-datetime  (AC-7) ✅
- Files: `gradle/libs.versions.toml` (modify), `service/title/domain/build.gradle.kts` (modify)
- What: version `kotlinx-datetime = "0.7.1"`; library `kotlinx-datetime = { module =
  "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }`; domain `commonMain`:
  `api(libs.kotlinx.datetime)` (the type is in the public API).
- Compile: `./gradlew :service:title:domain:compileAndroidMain`

### Step 2: Domain model and repository  (AC-5, AC-6, AC-7, AC-9, AC-10, AC-11, AC-12, AC-15, AC-16, AC-18) ✅
- Files: `service/title/domain/src/commonMain/kotlin/com/banhmilabs/tmdb/service/title/domain/TitleDetails.kt`,
  `.../TitleDetailsRepository.kt` (new)
- What:
  ```kotlin
  data class TitleDetails(
      val id: TitleId,
      val name: String,
      val releaseDate: LocalDate?,      // first air date for series
      val runtimeMinutes: Int?,         // one episode for series
      val ratingPercent: Int?,          // null = no votes
      val synopsis: String?,            // null = none in this language
      val artworkUrl: String?,          // poster, else backdrop
      val genres: List<String>,
      val cast: List<CastMember>,       // at most 10, TMDB order
      val trailer: Trailer?,
      val recommendations: List<Title>, // at most 20
  ) { val releaseYear: Int? get() = releaseDate?.year }

  data class CastMember(val name: String, val photoUrl: String?)

  data class Trailer(val youtubeKey: String) { val url: String get() = "https://www.youtube.com/watch?v=$youtubeKey" }

  interface TitleDetailsRepository {
      /** Cached details; `null` until a refresh of this title and language has succeeded. */
      fun observe(id: TitleId, language: Language): Flow<TitleDetails?>
      /** Fetches when stale (older than 24 h) or [force]d; shares a running refresh. Failures keep the cache. */
      suspend fun refresh(id: TitleId, language: Language, force: Boolean = false): AppResult<Unit>
  }
  ```
- Unit tests: `TrailerTest.urlIsYoutubeWatchUrl`; `TitleDetailsTest.releaseYearComesFromDate`,
  `releaseYearIsNullWithoutDate` (in `androidHostTest` of the domain, for coverage).
- Compile: `./gradlew :service:title:domain:compileAndroidMain :service:title:domain:compileAndroidHostTest`

### Step 3: Fake and contract  (AC-15, EC-9) ✅
- Files: `service/title/fake/src/commonMain/.../TitleDetailsRepositoryContract.kt`, `.../FakeTitleDetailsRepository.kt`
  (new); `service/title/fake/src/androidHostTest/.../FakeTitleDetailsRepositoryTest.kt` (new)
- What:
  - `FakeTitleDetailsRepository(remoteDetails: (TitleId, Language) -> TitleDetails? = { _, _ -> null })`:
    `MutableStateFlow<Map<Pair<TitleId, Language>, TitleDetails>>`; `setRemoteDetails(id, details)`;
    `failNextRefresh(error)`; `refreshRequests: List<RefreshRequest(id, language, force)>`; a refresh with no
    remote details for the id fails with `AppError.NotFound` (as TMDB would).
  - `TitleDetailsRepositoryContract`: `abstract fun TestScope.createRepository()` (remote serves
    `CONTRACT_MOVIE_DETAILS` for `CONTRACT_MOVIE_ID`), `abstract fun failNextRefresh(error)`. Tests:
    `observeIsNullBeforeRefresh`, `refreshStoresDetailsForObserver`, `failedRefreshKeepsCachedDetails`,
    `failedRefreshWithoutCacheStaysNull`, `languagesAreCachedSeparately`, `titlesAreCachedSeparately`
    (same tmdbId, other `TitleType`).
  - `CONTRACT_MOVIE_DETAILS`: a film with a date, runtime, rating, synopsis, artwork, 2 genres, 2 cast members
    (one without photo), a trailer and 2 recommendations (one without poster).
- Unit tests: `FakeTitleDetailsRepositoryTest : TitleDetailsRepositoryContract` + `refreshWithoutRemoteFailsNotFound`,
  `refreshRequestsAreRecorded`.
- Compile: `./gradlew :service:title:fake:compileAndroidMain :service:title:fake:compileAndroidHostTest`

### Step 4: Generic refreshes, TTL and image URLs  (AC-16) ✅
- Files: `service/title/data/.../TitleListRefreshes.kt` → `KeyedRefreshes.kt` (rename),
  `TitleRefreshPolicy.kt`, `TitleListsRepositoryImpl.kt`, `local/TitleListLocalDataSource.kt`,
  `local/TmdbImages.kt` (new), `di/TitleDataModule.kt`; tests `TitleListRefreshesTest` → `KeyedRefreshesTest`,
  `TitleRefreshPolicyTest`, `TitleTestEnvironment` (modify)
- What:
  - `internal class KeyedRefreshes<K : Any>(scope: CoroutineScope)` with the same `joinOrLaunch(key: K, needed,
    refresh)`; lists use `KeyedRefreshes<TitleListKey>`.
  - `TitleRefreshPolicy`: `LIST_TTL = 6.hours`, `DETAILS_TTL = 24.hours`,
    `isStale(fetchedAt: Instant?, now: Instant, ttl: Duration): Boolean`; the list repository passes `LIST_TTL`.
  - `internal object TmdbImages { fun poster(path) = "https://image.tmdb.org/t/p/w500$path"; fun backdrop(path) =
    ".../w780$path"; fun profile(path) = ".../w185$path" }`; the list data source uses `poster`.
- Unit tests: existing tests renamed/adjusted; add `TitleRefreshPolicyTest.detailsAreFreshFor24Hours`,
  `detailsAreStaleAfter24Hours`.
- Compile: `./gradlew :service:title:data:compileAndroidMain :service:title:data:compileAndroidHostTest`

### Step 5: Tables  (AC-15, EC-9) ✅
- Files: `service/title/data/src/commonMain/sqldelight/com/banhmilabs/tmdb/service/title/data/generated/TitleDetails.sq` (new)
- What: every table is keyed by `(tmdbId, type, language)`; child rows add `position`.
  ```sql
  CREATE TABLE titleDetails (
      tmdbId INTEGER NOT NULL, type TEXT NOT NULL, language TEXT NOT NULL,
      name TEXT NOT NULL, releaseDate TEXT, runtimeMinutes INTEGER, ratingPercent INTEGER,
      synopsis TEXT, artworkPath TEXT, artworkIsPoster INTEGER NOT NULL, trailerKey TEXT,
      fetchedAt INTEGER NOT NULL,
      PRIMARY KEY (tmdbId, type, language)
  );
  CREATE TABLE titleDetailsGenre (..., position INTEGER NOT NULL, name TEXT NOT NULL, PRIMARY KEY (tmdbId, type, language, position));
  CREATE TABLE titleDetailsCast (..., position, name TEXT NOT NULL, profilePath TEXT, PRIMARY KEY (...));
  CREATE TABLE titleDetailsRecommendation (..., position, recTmdbId INTEGER NOT NULL, recType TEXT NOT NULL,
      name TEXT NOT NULL, posterPath TEXT, PRIMARY KEY (...));
  ```
  Queries: `selectDetails`, `selectGenres`, `selectCast`, `selectRecommendations` (ordered by `position`),
  `selectFetchedAt`, `upsertDetails` (`INSERT OR REPLACE`), `deleteGenres`, `deleteCast`,
  `deleteRecommendations`, `insertGenre`, `insertCast`, `insertRecommendation`. No `.sqm` file: schema version
  stays 1.
- Compile: `./gradlew :service:title:data:compileAndroidMain`

### Step 6: Remote: request, DTOs, mapping  (AC-5, AC-6, AC-7, AC-9, AC-10, AC-11, AC-12, AC-18, EC-10, EC-13) ✅
- Files: `service/title/data/.../TitleDetailsKey.kt`, `remote/TmdbTitleDetailsDto.kt`,
  `remote/TitleDetailsRemoteDataSource.kt` (new); fixtures `movie_details.json`, `tv_details.json`
  (new, in `src/androidHostTest/resources/fixtures/`)
- What:
  - `internal data class TitleDetailsKey(val id: TitleId, val language: Language)`.
  - `internal data class TitleDetailsEntry(name, releaseDate: LocalDate?, runtimeMinutes: Int?, ratingPercent:
    Int?, synopsis: String?, artworkPath: String?, artworkIsPoster: Boolean, trailerKey: String?, genres:
    List<String>, cast: List<CastEntry(name, profilePath)>, recommendations: List<TitleListEntry>)` — reuses
    `TitleListEntry` for recommendations.
  - DTOs (`@Serializable`, all optional with defaults): `TmdbTitleDetailsDto(title, name, overview, release_date,
    first_air_date, runtime, episode_run_time: List<Int>, last_episode_to_air: TmdbEpisodeDto?(runtime),
    vote_average: Double, vote_count: Int, poster_path, backdrop_path, genres: List<TmdbGenreDto(name)>,
    credits: TmdbCreditsDto(cast: List<TmdbCastDto(name, profile_path, order)>), videos:
    TmdbVideosDto(results: List<TmdbVideoDto(key, site, type, iso_639_1, official)>), recommendations:
    TmdbRecommendationsDto(results: List<TmdbRecommendationDto(id, title, name, poster_path, media_type)>))`.
  - `TitleDetailsRemoteDataSource.fetch(key): AppResult<TitleDetailsEntry>`: `GET movie/{id}` / `tv/{id}` with
    `language=<tag>`, `append_to_response=credits,videos,recommendations`, `include_video_language=<pt|en>,en`;
    then `toEntry(key)`; a missing name → `AppResult.Failure(AppError.Unknown)`.
  - Pseudo-code:
    ```
    trailer = videos.filter { site == "YouTube" && type == "Trailer" }
        .let { v -> v.pick(lang) ?: v.pick("en") }   // pick(l) = filter iso_639_1 == l, official first, then first
    runtime = if MOVIE: runtime else episode_run_time.firstOrNull() ?: last_episode_to_air?.runtime
    runtime = runtime?.takeIf { it > 0 }
    rating  = if (vote_count == 0) null else round(vote_average * 10).toInt()
    date    = (release_date ?: first_air_date)?.takeIf(isNotBlank)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    artwork = poster_path → (path, true) ?: backdrop_path → (path, false) ?: (null, false)
    cast    = credits.cast.sortedBy { order }.take(10)
    recs    = recommendations.results.mapNotNull { toEntry(type = media_type?.toTitleType() ?: key.type) }.take(20)
    ```
    (`lang` = `pt` for `PT_BR`, `en` for `EN_US`.)
- Unit tests: `TitleDetailsMappingTest` (MockK-free, on DTOs): `movieMapsAllFields`, `seriesUsesFirstAirDateAndEpisodeRuntime`,
  `seriesFallsBackToLastEpisodeRuntime`, `runtimeUnknownWhenMissing`, `trailerPrefersRequestedLanguage`,
  `trailerFallsBackToEnglish`, `trailerPrefersOfficial`, `noYoutubeTrailerGivesNull`, `ratingIsWholePercent`,
  `noVotesGivesNullRating`, `posterIsArtwork`, `backdropWhenNoPoster`, `noArtwork`, `blankSynopsisIsNull`,
  `invalidDateIsNull`, `castIsFirstTenByOrder`, `recommendationsAreFirstTwentyWithNames`,
  `recommendationTypeFromMediaType`, `missingNameFails`.
- Compile: `./gradlew :service:title:data:compileAndroidMain :service:title:data:compileAndroidHostTest`

### Step 7: Local data source  (AC-9, AC-11, AC-12, AC-15, EC-9) ✅
- Files: `service/title/data/.../local/TitleDetailsLocalDataSource.kt` (new)
- What:
  - `fun observe(key): Flow<TitleDetails?>` — combines the four `select*` query flows (`asFlow().mapToOneOrNull` /
    `mapToList` on `dispatchers.io`); `null` while no `titleDetails` row; maps paths to URLs with `TmdbImages`
    (artwork: `poster` or `backdrop` by `artworkIsPoster`; cast: `profile`; recommendations: `poster`), date via
    `LocalDate.parse`, `trailerKey` → `Trailer`; `distinctUntilChanged()`.
  - `suspend fun fetchedAt(key): Instant?`.
  - `suspend fun upsert(key, entry, fetchedAt)` — one `database.transaction`: `upsertDetails`, then delete and
    reinsert genres, cast and recommendations with their positions.
- Unit tests: covered through the repository test (step 8).
- Compile: `./gradlew :service:title:data:compileAndroidMain`

### Step 8: Repository, DI  (AC-15, AC-16, EC-9, EC-13) ✅
- Files: `service/title/data/.../TitleDetailsRepositoryImpl.kt` (new), `di/TitleDataModule.kt` (modify); tests
  `TitleDetailsRepositoryImplTest.kt`, `TitleDetailsTestEnvironment.kt` (new, modelled on `TitleTestEnvironment`)
- What:
  - `internal class TitleDetailsRepositoryImpl(remote, local, refreshes: KeyedRefreshes<TitleDetailsKey>, clock)`:
    `observe` → `local.observe(key)`; `refresh` → `refreshes.joinOrLaunch(key, needed = { force ||
    isStale(local.fetchedAt(key), clock.now(), DETAILS_TTL) }) { remote.fetch(key) → on success
    local.upsert(key, entry, clock.now()) }`.
  - Koin: `single { TitleDetailsRemoteDataSource(get()) }`, `single { TitleDetailsLocalDataSource(get(), get()) }`,
    `single<TitleDetailsRepository> { TitleDetailsRepositoryImpl(remote = get(), local = get(), refreshes =
    KeyedRefreshes(CoroutineScope(SupervisorJob() + get<DispatcherProvider>().io)), clock = get()) }`.
- Unit tests: `TitleDetailsRepositoryImplTest : TitleDetailsRepositoryContract` + `movieRequestSendsPathAndParameters`
  (`/3/movie/550`, `language=pt-BR`, `append_to_response`, `include_video_language=pt,en`),
  `seriesRequestUsesTvPath`, `fixtureMovieIsStoredAndObserved` (fixture → observed `TitleDetails` with URLs),
  `refreshReplacesChildRows` (fewer cast after refresh), `freshDetailsAreNotRefetched` (+23 h),
  `staleDetailsAreRefetched` (+25 h), `forceRefetchesFreshDetails`, `concurrentRefreshesShareOneRequest`,
  `notFoundKeepsCacheAndReturnsNotFound`.
- Compile: `./gradlew :service:title:data:compileAndroidMain :service:title:data:compileAndroidHostTest :core:di:compileAndroidMain`

### Step 9: iOS entry point  (AC-18) ✅
- Files: `core/ios/src/commonMain/kotlin/com/banhmilabs/tmdb/core/ios/SharedCore.kt` (modify)
- What: `val titleDetailsRepository: TitleDetailsRepository get() = checkNotNull(koin) { ... }.get()`.
- Unit tests: none (same shape as the existing getters).
- Compile: `./gradlew :core:ios:compileAndroidMain`; `./gradlew assemble` at the end (macOS + Xcode) since the
  XCFramework's API changes.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-5 (trailer choice) | 2, 6 | `TitleDetailsMappingTest.trailer*`, `noYoutubeTrailerGivesNull`; `TrailerTest` |
| AC-6 (percentage, no votes) | 6 | `ratingIsWholePercent`, `noVotesGivesNullRating` |
| AC-7 (year, date, runtime) | 1, 2, 6 | `movieMapsAllFields`, `seriesUsesFirstAirDateAndEpisodeRuntime`, `seriesFallsBackToLastEpisodeRuntime`, `runtimeUnknownWhenMissing`, `invalidDateIsNull`; `TitleDetailsTest` |
| AC-9 (first 10 cast) | 6, 7 | `castIsFirstTenByOrder`, `fixtureMovieIsStoredAndObserved` |
| AC-10 (genres, language) | 6, 7 | `movieMapsAllFields`, `fixtureMovieIsStoredAndObserved` |
| AC-11 (up to 20 recommendations) | 6, 7 | `recommendationsAreFirstTwentyWithNames`, `recommendationTypeFromMediaType` |
| AC-12 (poster, else backdrop) | 6, 7 | `posterIsArtwork`, `backdropWhenNoPoster`, `noArtwork` |
| AC-15 (cache kept, offline read) | 3, 5, 7, 8 | contract `failedRefreshKeepsCachedDetails`, `failedRefreshWithoutCacheStaysNull`; `refreshReplacesChildRows` |
| AC-16 (24 h) | 4, 8 | `TitleRefreshPolicyTest.details*`, `freshDetailsAreNotRefetched`, `staleDetailsAreRefetched`, `forceRefetchesFreshDetails` |
| AC-18 (language of data) | 6, 8 | `movieRequestSendsPathAndParameters` |
| AC-8 (synopsis, data side) | 6 | `blankSynopsisIsNull` |
| EC-9 (per-language cache) | 3, 5, 8 | contract `languagesAreCachedSeparately` |
| EC-10 (en-US data) | 6, 8 | `movieRequestSendsPathAndParameters` (EN_US case) |
| EC-13 (not found) | 8 | `notFoundKeepsCacheAndReturnsNotFound`; fake `refreshWithoutRemoteFailsNotFound` |
| Concurrency (shared refresh) | 4, 8 | `KeyedRefreshesTest`, `concurrentRefreshesShareOneRequest` |

Checks after the last step: `./gradlew :service:title:domain:testAndroidHostTest :service:title:data:testAndroidHostTest
:service:title:fake:testAndroidHostTest :service:title:domain:detekt :service:title:data:detekt :service:title:fake:detekt
:core:ios:detekt :core:di:detekt :architecture-test:test koverVerify`, and `./gradlew assemble` (macOS + Xcode).

## Out of scope
- The details screen, trailer opening and navigation in `cmp` -> 02-cmp-title-details-happy-path.
- Loading, error, offline display and pull-to-refresh on screen -> 03-cmp-title-details-robustness.
- Favorites -> 04-favorites.
- Native apps -> 05, 06.
- A database migration and evicting old details from the cache (not required).

## Risks
- No migration: a dev build that already has `title.db` (version 1, without the new tables) crashes on the first
  details query — uninstall the `cmp`, `native-android` and `native-ios` apps once after this lands. Any later
  schema change after a release needs a real migration.
- The cache grows by one set of rows per title and language opened, with no eviction — acceptable for now; revisit
  if storage becomes an issue.
- `kotlinx-datetime` in the public domain API is exported to the `TmdbCore` XCFramework through SKIE; Swift sees
  `Kotlinx_datetimeLocalDate` (read `year`, `month`, `day`). If the export breaks the XCFramework build, export the
  library explicitly in `core:ios` — checked by `./gradlew assemble` / `publishSharedCore` on macOS.
- TMDB's `include_video_language` and `append_to_response` behavior is relied on; the request test pins the
  parameters, and a manual check against the live API is done in 02.

## Deviations
- Step 2: `TrailerTest` merged into `TitleDetailsTest` (`trailerUrlIsYoutubeWatchUrl`) — one small test class per
  file of the domain.
- Step 3: `FakeTitleDetailsRepository` takes `remoteDetails: Map<TitleId, TitleDetails>` instead of a lambda —
  simpler, same control through `setRemoteDetails`.
- Step 4: the Koin module no longer binds `KeyedRefreshes` as a `single` (two generic instances would clash by
  type); each repository gets its own through a private `Scope.refreshScope()` helper.
- Step 6/8: mapping tests split by Detekt's `TooManyFunctions` (11 per class) into `TitleDetailsMappingTest`,
  `TitleDetailsArtworkMappingTest`, `TrailerSelectionTest` with shared helpers in `DetailsDtos`; storage tests
  (`seriesIsStoredAndObserved`, `refreshReplacesChildRows`, `responseWithoutNameIsUnknownAndNotStored`) moved to
  `TitleDetailsStorageTest`. `fixtureMovieIsStoredAndObserved` is covered by the contract test
  `refreshStoresDetailsForObserver` (the default response is `movie_details.json`, which maps to
  `CONTRACT_MOVIE_DETAILS`). Added `englishRequestAsksForEnglishVideos` and `runtimeUnknownWhenMissingOrZero`.
- Step 6: a runtime of `0` is treated as unknown (TMDB sends 0 for unknown runtimes).
