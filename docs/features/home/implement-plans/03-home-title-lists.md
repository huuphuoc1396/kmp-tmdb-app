# Plan: Home title lists (shared core)

Requirement: [03 - Home title lists (shared core)](../subtasks/03-home-title-lists.md)
Status: Done

## Summary
This plan creates the `title` context: `service/title/{domain,data,fake}`. The domain exposes
`HomeListsRepository`:
- `observe(category, row, language)` reads the SQLDelight cache, which is the only source the UI reads from.
- `refresh(category, row, language, force)` fetches page 1 of the agreed TMDB endpoint and replaces that cached
  list.

A non-forced refresh does nothing while the list is younger than 6 hours. Two refreshes of the same list and
language share one request.

The shared models (`TitleId`, `Title`, `Language`) go in `core:model`. `tmdb.module` applies SQLDelight to any
`data` module that has a schema. `core:di` binds the HTTP client, the database driver factory, the clock and the
new repository. iOS reaches the repository through `SharedCore`, and `native-android` through the published
`service-title-domain` and `core-di` libraries.

## Decisions
- **`observe` only reads; `refresh(force)` is separate** (from user):
  - `observe` never starts a request.
  - The caller (the ViewModel in 04/05, the native ViewModels in 06/07) calls `refresh(…, force = false)` when a
    category is shown, and `force = true` on pull-to-refresh.
  - `refresh` returns `AppResult<Unit>`: a fresh list returns `Success` with no request; a failure returns
    `Failure` and the cache is left as it was.
  - Rejected: `observe` refreshing by itself and emitting a state with `refreshing` and `error` fields. That state is
    heavier, and harder to use from Swift.
- **SQLDelight is configured by `tmdb.module`** (from user):
  - `configureSqlDelight()` applies `app.cash.sqldelight` to a `DATA` module that has `src/commonMain/sqldelight`.
  - It creates one database, `<Context>Database` (`:service:title:data` → `TitleDatabase`), in package
    `<namespace>.generated`. Kover already excludes `*.generated.*`.
  - Module build files keep only `tmdb.module` plus dependencies, as `.claude/rules/build-logic.md` requires.
  - Rejected: a `sqldelight { }` block in each module.
- **`TitleId`, `TitleType`, `Title`, `Language` go in `core:model`**:
  - CLAUDE.md's reuse table puts these types there. `TitleId` is needed later by the details route (a `nav`
    module) and by `favorites`.
  - `docs/architecture.md` 6.1 says a model goes in `core:model` only when two or more contexts need it. CLAUDE.md
    is followed here, so `Genre` is not created yet: nothing needs it.
- **`TitleId(tmdbId: Long, type: TitleType)`, with `TitleType { MOVIE, TV }`**: TMDB IDs are only unique within a
  media type, and the details screen needs to know the type.
- **`Language { PT_BR("pt-BR"), EN_US("en-US") }`, with `Language.fromLocaleTag(tag)`**:
  - Only `pt-BR` gives `PT_BR`. The match ignores case, and `pt_BR` also counts. Every other tag gives `EN_US`
    (AC-10, EC-8).
  - The rule sits in the shared core so the Kotlin and Swift apps don't repeat it.
- **`observe` emits `List<Title>?`**: `null` means no successful refresh yet for that list and language (the
  Loading or Error state in 05). An empty list means it loaded with no titles (the Empty state, EC-3). This avoids
  a domain data class that Kover would count with no test.
- **Lists are keyed by `HomeCategory` × `HomeRow`**:
  - `HomeCategory { MOVIES, SERIES, ANIME, NOVELAS, DOCUMENTARIES }`, in tab order.
  - `HomeRow { FEATURED, POPULAR }`.
  - Each of the 10 lists is refreshed and fails on its own (EC-4, AC-8 retry per row).
- **Schema** (`TitleDatabase`, file `title.db`):
  - `homeListTitle(list, language, position, tmdbId, type, name, posterPath)`, primary key
    `(list, language, position)`.
  - `homeListFetch(list, language, fetchedAt)`, primary key `(list, language)`.
  - `list` is `"<CATEGORY>_<ROW>"`; `fetchedAt` is in epoch milliseconds.
  - The titles are stored once per list, not in a shared title table: Home has no other reader. Details will add
    its own table in its own feature.
- **A refresh replaces the list in one transaction**: delete the list's rows for that language, insert the new rows
  in TMDB order, and upsert `fetchedAt`. A failed request writes nothing (AC-7).
- **No duplicate requests (EC-9)**:
  - In-flight refreshes sit in a `Mutex`-guarded map keyed by `(list, language)`. They run as `async` in a
    repository `CoroutineScope` (Koin: `SupervisorJob() + io`, as in settings).
  - A second call, forced or not, awaits the running one and gets its result.
  - A refresh started from a tab the user has left still finishes and caches (EC-5).
- **Staleness**:
  - A list is stale when it has no `fetchedAt`, or when `now - fetchedAt > 6.hours`.
  - `TitleRefreshPolicy.HOME_LIST_TTL = 6.hours` is the context's refresh policy (`docs/architecture.md` 7).
  - `force = true` skips the check.
- **Time comes from an injected `kotlin.time.Clock`** (`single<Clock> { Clock.System }` in `core:di`).
  - Date filters are `(now - 7.days)` or `(now - 90.days)`, formatted as `yyyy-MM-dd` from the UTC
    `Instant.toString()`.
  - kotlinx-datetime is not added: a day in UTC is good enough for "the last 7 / 90 days".
- **Requests** (task.md "Data source (agreed)"):
  - Every request sends `language=<tag>` and `page=1`.
  - Discover requests also send `include_adult=false` and `sort_by=popularity.desc`. The trending and popular
    endpoints don't document `include_adult`, so they don't get it.
  - TMDB's Discover documentation lists `primary_release_date.gte` and `include_adult` for `/discover/movie`. This
    answers task.md's first open question. task.md itself is not edited here.
- **DTOs**: `TmdbPageDto(results: List<TmdbTitleDto>)` and `TmdbTitleDto(id, title?, name?, poster_path?)`.
  - The name is `title` for movies and `name` for series.
  - An entry with neither is dropped.
  - The type comes from the endpoint: movie lists give `MOVIE`, TV lists give `TV`.
- **Poster URL**: `https://image.tmdb.org/t/p/w500<poster_path>`, or `null` with no path (AC-11 is handled in 04).
  The tile is 124dp wide, which is 372px at 3×. `w342` would be upscaled, so `w500` is used.
- **Shared Koin bindings in `core:di`**:
  - `single { createTmdbHttpClient() }` and `single<Clock> { Clock.System }`.
  - A platform module: Android `single { DatabaseDriverFactory(androidContext()) }`, iOS
    `single { DatabaseDriverFactory() }`.
  - `titleDataModule` binds `TitleDatabase` and `HomeListsRepository`.
- **iOS link**: `cmp/iosApp` `OTHER_LDFLAGS` gains `-lsqlite3` (Debug and Release). The static `ComposeApp`
  framework doesn't carry Kotlin's linker options (follow-up from plan 02). `native-ios` uses the dynamic
  `TmdbCore`, which already links sqlite3.
- **No use case**: the ViewModels call the repository directly (`docs/architecture.md` 6.2).

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Context module shape (domain/data/fake, Koin module, contract test) | `settings` context | `service/settings/*` |
| TMDB client, error mapping | `createTmdbHttpClient()`, `apiCall { }`, `TMDB_BASE_URL` | `core/network/src/commonMain/.../TmdbHttpClient.kt`, `ApiCall.kt` |
| SQLite driver | `DatabaseDriverFactory` | `core/database/src/*Main/.../DatabaseDriverFactory*.kt` |
| Result / errors | `AppResult`, `AppError` | `core/common` |
| IO dispatcher | `DispatcherProvider` | `core/common` |
| Repository scope in Koin | `CoroutineScope(SupervisorJob() + get<DispatcherProvider>().io)` | `service/settings/data/.../di/SettingsDataModule.kt` |
| Network test engine | Ktor `MockEngine` (`ktor-client-mock`) | catalog; example `core/network/src/androidHostTest/.../TmdbHttpClientTest.kt` |
| In-memory database | `JdbcSqliteDriver` (`sqldelight-sqlite-driver`) | catalog; example `core/database/src/androidHostTest/.../InMemoryDriverTest.kt` |
| Kover exclusions (`*.di`, `*Dto`, `*.generated.*`) | `configureKover()` | `build-logic/.../buildlogic/Kover.kt` |
| Composition root, iOS entry point | `initKoin()`, `SharedCore` | `core/di`, `core/ios` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| root | `gradle/libs.versions.toml`, `build.gradle.kts`, `settings.gradle.kts` (modify) | SQLDelight plugin and coroutines extensions; the three new modules |
| `build-logic/convention` | `build.gradle.kts`, `ModuleConventionPlugin.kt` (modify); `buildlogic/SqlDelight.kt` (new); `src/test/.../SqlDelightTest.kt` (new) | SQLDelight convention |
| `core:model` | `TitleId.kt`, `Title.kt`, `Language.kt` (new); `LanguageTest.kt` (new); `ModelSmokeTest.kt` (delete) | Shared models |
| `service:title:domain` (new) | `build.gradle.kts`, `HomeCategory.kt`, `HomeRow.kt`, `HomeListsRepository.kt`; `HomeCategoryTest.kt` | Public API |
| `service:title:fake` (new) | `build.gradle.kts`, `FakeHomeListsRepository.kt`, `HomeListsRepositoryContract.kt`; `FakeHomeListsRepositoryTest.kt` | Test double and contract |
| `service:title:data` (new) | `build.gradle.kts`, `HomeList.sq`, `TmdbTitleDto.kt`, `HomeListRequest.kt`, `TitleRefreshPolicy.kt`, `HomeListsRepositoryImpl.kt`, `di/TitleDataModule.kt`; tests and fixtures | Endpoints, cache, refresh |
| `core:di` | `build.gradle.kts`, `InitKoin.kt` (modify); `PlatformModule.kt` + `.android.kt` / `.ios.kt` (new); `InitKoinTest.kt` (modify) | Bindings |
| `core:ios` | `build.gradle.kts`, `SharedCore.kt`, `SharedCoreTest.kt` (modify) | iOS access |
| `cmp/iosApp` | `iosApp.xcodeproj/project.pbxproj` (modify) | `-lsqlite3` |
| docs | `CLAUDE.md` (modify) | Current state, reuse table |

Other modules affected:
- `cmp:composeApp` and `cmp:androidApp` depend on `core:di`, so they now pull in the `title` data stack. They need
  no code changes. They compile in Steps 6 and 7.
- `native-android` needs no change until 06.

## Steps
### Step 1: SQLDelight plugin convention ✅
- Files: `gradle/libs.versions.toml`, `build.gradle.kts`, `build-logic/convention/build.gradle.kts`,
  `build-logic/convention/src/main/kotlin/ModuleConventionPlugin.kt` (modify);
  `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/SqlDelight.kt` (new);
  `build-logic/convention/src/test/kotlin/com/banhmilabs/tmdb/buildlogic/SqlDelightTest.kt` (new)
- What:
  - Catalog:
    - Library `sqldelight-coroutines-extensions` (`app.cash.sqldelight:coroutines-extensions`).
    - Library `sqldelight-gradlePlugin` (`app.cash.sqldelight:gradle-plugin`).
    - Plugin `sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }`.
  - `build-logic/convention/build.gradle.kts`: `compileOnly(libs.sqldelight.gradlePlugin)`.
  - Root `build.gradle.kts`: `alias(libs.plugins.sqldelight) apply false`.
  - `SqlDelight.kt`:
    - `internal fun sqlDelightDatabaseName(path: String): String`, where `":service:title:data"` gives
      `"TitleDatabase"` (context with its first letter uppercased, plus `Database`).
    - `internal fun Project.configureSqlDelight()` applies `app.cash.sqldelight` and creates
      `databases.create(sqlDelightDatabaseName(path)) { packageName.set("${derivedNamespace()}.generated") }`.
    - A one-line KDoc.
  - `ModuleConventionPlugin`: after `configureKotlinMultiplatform`, add
    `if (type.kind == ModuleKind.DATA && file(SQLDELIGHT_SOURCES).isDirectory) configureSqlDelight()`, with
    `SQLDELIGHT_SOURCES = "src/commonMain/sqldelight"` in the companion.
- Unit tests: `SqlDelightTest` (JUnit Jupiter):
  - `databaseNameIsContextPlusDatabase` (`title` gives `TitleDatabase`).
  - `databaseNameOfFavorites` (gives `FavoritesDatabase`).
- Compile: `./gradlew help`, then `./gradlew testAndroidHostTest` runs the build-logic tests. No module uses the
  convention yet.

### Step 2: Shared models in `core:model`  (AC-10, EC-8) ✅
- Files: `core/model/src/commonMain/kotlin/com/banhmilabs/tmdb/core/model/` `TitleId.kt`, `Title.kt`, `Language.kt`
  (new); `core/model/src/commonTest/.../LanguageTest.kt` (new); `ModelSmokeTest.kt` (delete)
- What:
  - `enum class TitleType { MOVIE, TV }` and `data class TitleId(val tmdbId: Long, val type: TitleType)`, both in
    `TitleId.kt`.
  - `data class Title(val id: TitleId, val name: String, val posterUrl: String?)`.
  - `enum class Language(val tag: String) { PT_BR("pt-BR"), EN_US("en-US"); companion object { fun fromLocaleTag(tag: String): Language } }`.
    It returns `PT_BR` only when `tag.replace('_', '-')` equals `pt-BR`, ignoring case.
- Unit tests: `LanguageTest` in `commonTest` (string parsing, worth running on Native):
  - `ptBrMapsToPtBr`
  - `ptBrIgnoresCaseAndUnderscore`
  - `englishMapsToEnUs`
  - `otherLanguagesMapToEnUs` (`es-MX`, `pt-PT`, `pt`, empty)
  - `tagsAreTmdbLanguages`
- Compile: `./gradlew :core:model:compileAndroidMain :core:model:compileAndroidHostTest`

### Step 3: `service:title:domain`  (AC-7, AC-9, AC-10 partial) ✅
- Files: `settings.gradle.kts` (add `:service:title:domain`); `service/title/domain/build.gradle.kts`;
  `src/commonMain/kotlin/com/banhmilabs/tmdb/service/title/domain/HomeCategory.kt`, `HomeRow.kt`,
  `HomeListsRepository.kt`; `src/androidHostTest/.../HomeCategoryTest.kt` (all new)
- What:
  - Build: `api(projects.core.common)`, `api(projects.core.model)`, `api(libs.kotlinx.coroutines.core)`.
  - `enum class HomeCategory { MOVIES, SERIES, ANIME, NOVELAS, DOCUMENTARIES }`, with a KDoc saying it is in tab
    order.
  - `enum class HomeRow { FEATURED, POPULAR }`.
  - The repository interface:
    ```kotlin
    /** The Home lists, cached per language; reads never wait for the network. */
    interface HomeListsRepository {
        /** Cached titles in TMDB order; `null` until a refresh of this list and language has succeeded. */
        fun observe(category: HomeCategory, row: HomeRow, language: Language): Flow<List<Title>?>

        /** Fetches the list when stale (older than 6 h) or [force]d; shares a running refresh. Failures keep the cache. */
        suspend fun refresh(category: HomeCategory, row: HomeRow, language: Language, force: Boolean = false): AppResult<Unit>
    }
    ```
- Unit tests: `HomeCategoryTest.categoriesAreInTabOrder`. It also keeps the module's Kover line coverage above
  80%, because the enums count as lines.
- Compile: `./gradlew :service:title:domain:compileAndroidMain :service:title:domain:compileAndroidHostTest`

### Step 4: `service:title:fake` and the contract  (AC-7, EC-7) ✅
- Files: `settings.gradle.kts` (add `:service:title:fake`); `service/title/fake/build.gradle.kts` (same shape as
  `service/settings/fake/build.gradle.kts`); `src/commonMain/.../fake/FakeHomeListsRepository.kt`,
  `HomeListsRepositoryContract.kt`; `src/androidHostTest/.../fake/FakeHomeListsRepositoryTest.kt` (all new)
- What:
  - `class FakeHomeListsRepository(private val remoteTitles: List<Title> = emptyList()) : HomeListsRepository`:
    - Holds a `MutableStateFlow<Map<Key, List<Title>>>` keyed by `(category, row, language)`.
    - `observe` maps the state to the entry, or `null`.
    - `refresh` stores `remoteTitles` unless `failNextRefresh(error)` was set; in that case it returns `Failure`
      once and leaves the state alone.
    - The fake has no TTL. It refreshes on every call: freshness is a `data` concern, tested there.
  - `abstract class HomeListsRepositoryContract`:
    - `protected abstract fun TestScope.createRepository(): HomeListsRepository`, whose remote serves
      `CONTRACT_TITLES` for every list.
    - `protected abstract fun failNextRefresh(error: AppError)`.
    - `CONTRACT_TITLES` is a `companion` `val` with two `MOVIE` titles, one of them with no poster. The tests use
      only `MOVIES` lists, so the types match the real implementation.
- Unit tests:
  - Contract:
    - `observeIsNullBeforeRefresh`
    - `refreshStoresTitlesForObserver`
    - `failedRefreshKeepsCachedTitles` (AC-7)
    - `failedRefreshWithoutCacheStaysNull`
    - `languagesAreCachedSeparately` (EC-7)
    - `listsAreCachedSeparately` (featured vs popular)
  - `FakeHomeListsRepositoryTest` extends the contract and adds `failNextRefreshFailsOnlyOnce`.
- Compile: `./gradlew :service:title:fake:compileAndroidMain :service:title:fake:compileAndroidHostTest`

### Step 5: `service:title:data`  (AC-7, AC-9, AC-10, EC-7, EC-9) ✅
- Files: `settings.gradle.kts` (add `:service:title:data`); `service/title/data/build.gradle.kts`;
  `src/commonMain/sqldelight/com/banhmilabs/tmdb/service/title/data/generated/HomeList.sq`;
  `src/commonMain/kotlin/com/banhmilabs/tmdb/service/title/data/` `TmdbTitleDto.kt`, `HomeListRequest.kt`,
  `TitleRefreshPolicy.kt`, `HomeListsRepositoryImpl.kt`, `di/TitleDataModule.kt`; tests and
  `src/androidHostTest/resources/fixtures/movie_list.json`, `tv_list.json` (all new)
- What:
  - Build:
    - `commonMain`:
      - `implementation`: `projects.service.title.domain`, `projects.core.network`, `projects.core.database`,
        `libs.sqldelight.coroutines.extensions`, `libs.kotlinx.serialization.core`, `libs.koin.core`.
    - `androidHostTest`:
      - `projects.service.title.fake`
      - `libs.ktor.client.mock`
      - `libs.sqldelight.sqlite.driver`
      - `libs.kotlinx.coroutines.test`
      - `libs.turbine`
  - `HomeList.sq`: the two tables from Decisions, plus these queries:
    - `selectTitles(list, language)`, ordered by `position`.
    - `selectFetchedAt(list, language)`.
    - `deleteTitles(list, language)`.
    - `insertTitle(...)`.
    - `upsertFetchedAt(list, language, fetchedAt)`, using `INSERT OR REPLACE`.
  - `TmdbTitleDto.kt`: `@Serializable internal data class TmdbPageDto(val results: List<TmdbTitleDto>)`, and
    `TmdbTitleDto(id: Long, title: String? = null, name: String? = null, @SerialName("poster_path") posterPath: String? = null)`.
  - `HomeListRequest.kt`:
    - `internal data class HomeListRequest(val path: String, val parameters: Map<String, String>, val type: TitleType)`.
    - `internal fun homeListRequest(category: HomeCategory, row: HomeRow, now: Instant): HomeListRequest`. It
      encodes the table in task.md "Data source (agreed)" (paths are relative to `TMDB_BASE_URL`, e.g.
      `trending/movie/week`).
    - `internal fun Instant.minusDaysAsDate(days: Int): String = (this - days.days).toString().substringBefore('T')`.
  - `TitleRefreshPolicy.kt`: `internal object TitleRefreshPolicy { val HOME_LIST_TTL = 6.hours; fun isStale(fetchedAtMillis: Long?, now: Instant): Boolean }`.
  - `HomeListsRepositoryImpl`:
    - Constructor: `(database: TitleDatabase, httpClient: HttpClient, clock: Clock, dispatchers: DispatcherProvider, scope: CoroutineScope)`.
    - `observe`: `combine(selectTitles.asFlow().mapToList(io), selectFetchedAt.asFlow().mapToOneOrNull(io))`.
      With no `fetchedAt` it emits `null`; otherwise it emits the rows mapped to `Title`, with the poster URL
      built by `posterUrl(path)`. Then `distinctUntilChanged()`.
    - `refresh`:
      ```
      key = list name + language
      deferred = mutex.withLock {
          inFlight[key] ?: run {
              if (!force && !isStale(fetchedAt(key), clock.now())) return Success(Unit)
              scope.async { try { fetchAndStore(…) } finally { mutex.withLock { inFlight.remove(key) } } }
                  .also { inFlight[key] = it }
          }
      }
      return deferred.await()
      ```
    - `fetchAndStore`: `apiCall { httpClient.get(request.path) { parameter("language", tag); parameter("page", 1); request.parameters… }.body<TmdbPageDto>() }`.
      On `Success`, it runs a `withContext(io)` transaction that deletes the list, inserts the titles (dropping DTOs
      with no name) and sets `fetchedAt = clock.now()`. It returns `Success(Unit)` or the `Failure` as it is.
  - `di/TitleDataModule.kt`:
    - `internal const val TITLE_DATABASE_NAME = "title.db"`.
    - `val titleDataModule = module { single { TitleDatabase(get<DatabaseDriverFactory>().create(TitleDatabase.Schema, TITLE_DATABASE_NAME)) }; single<HomeListsRepository> { HomeListsRepositoryImpl(get(), get(), get(), get(), CoroutineScope(SupervisorJob() + get<DispatcherProvider>().io)) } }`.
- Unit tests (`androidHostTest`):
  - `HomeListRequestTest`, one case per list (10). Each checks the path, the parameters and the type, with
    `now = 2026-11-18T12:00:00Z`:
    - `moviesFeaturedIsTrendingMovieWeek`
    - `moviesPopularIsMoviePopular`
    - `seriesFeaturedIsTrendingTvWeek`
    - `seriesPopularIsTvPopular`
    - `animeFeaturedAiredInLast7Days` (`air_date.gte=2026-11-11`)
    - `animePopular`
    - `novelasFeaturedAiredInLast7Days`
    - `novelasPopular` (`with_origin_country=BR|MX|CO|AR|VE|CL|PE`)
    - `documentariesFeaturedReleasedInLast90Days` (`primary_release_date.gte=2026-08-20`)
    - `documentariesPopular`
  - `TitleRefreshPolicyTest`:
    - `neverFetchedIsStale`
    - `sevenHoursOldIsStale`
    - `oneHourOldIsFresh`
  - `HomeListsRepositoryImplTest`, which extends `HomeListsRepositoryContract`. It uses an in-memory
    `TitleDatabase` (`JdbcSqliteDriver(IN_MEMORY)` plus `Schema.create`), a `MockEngine` passed to
    `createTmdbHttpClient(engine, "token")`, a MockK `Clock`, a test dispatcher, and `backgroundScope`. Its extra
    cases:
    - `refreshSendsLanguageAndPage` (AC-10)
    - `tvListStoresTvTitles`
    - `posterPathBecomesW500Url`
    - `titleWithoutPosterHasNullUrl`
    - `entryWithoutNameIsDropped`
    - `refreshReplacesPreviousTitlesInOrder`
    - `freshListIsNotRefetched` (1 h; no request)
    - `staleListIsRefetched` (7 h) (AC-9)
    - `forcedRefreshIgnoresAge` (AC-9)
    - `concurrentRefreshesSendOneRequest`: the handler suspends on a `CompletableDeferred`, and both calls get
      the same result (EC-9).
    - `networkErrorWithCacheKeepsTitles` (AC-7)
    - `unauthorizedIsReported`
    - The JSON fixtures come from `src/androidHostTest/resources/fixtures/`. The contract's remote JSON is built
      from `CONTRACT_TITLES`.
- Compile: `./gradlew :service:title:data:compileAndroidMain :service:title:data:compileAndroidHostTest`

### Step 6: Bindings in `core:di` and access from iOS  (subtask scope: reachable from the apps) ✅
- Files: `core/di/build.gradle.kts`, `core/di/src/commonMain/.../InitKoin.kt`,
  `core/di/src/androidDeviceTest/.../InitKoinTest.kt` (modify); `core/di/src/commonMain/.../PlatformModule.kt`,
  `src/androidMain/.../PlatformModule.android.kt`, `src/iosMain/.../PlatformModule.ios.kt` (new);
  `core/ios/build.gradle.kts`, `core/ios/src/commonMain/.../SharedCore.kt`, `core/ios/src/iosTest/.../SharedCoreTest.kt` (modify)
- What:
  - `core:di` build:
    - `implementation`: `projects.core.network`, `projects.core.database`, `projects.service.title.data`.
    - `androidMain`: `implementation(libs.koin.android)`.
    - `androidDeviceTest`: `implementation(projects.service.title.domain)`.
  - `PlatformModule.kt`: `internal expect val platformModule: Module`.
    - Android: `module { single { DatabaseDriverFactory(androidContext()) } }`.
    - iOS: `module { single { DatabaseDriverFactory() } }`.
  - `InitKoin.kt`:
    - `coreModule` adds `single { createTmdbHttpClient() }` and `single<Clock> { Clock.System }`.
    - `modules(coreModule, platformModule, settingsDataModule, titleDataModule)`.
  - `core:ios`: `api(projects.core.model)`, `api(projects.service.title.domain)`;
    `val homeListsRepository: HomeListsRepository`, the same getter shape as `settingsRepository`.
- Unit tests:
  - `InitKoinTest.initKoinProvidesHomeListsRepository` (device).
  - `SharedCoreTest.startProvidesHomeListsRepository` (iOS).
- Compile: `./gradlew :core:di:compileAndroidMain :core:ios:compileKotlinIosSimulatorArm64 :cmp:composeApp:compileAndroidMain :cmp:androidApp:compileDebugKotlin`

### Step 7: Link sqlite3 in `cmp/iosApp` and update docs ✅
- Files: `cmp/iosApp/iosApp.xcodeproj/project.pbxproj`, `CLAUDE.md` (modify)
- What:
  - `OTHER_LDFLAGS` in Debug and Release: add `"-lsqlite3",` after `ComposeApp,`.
  - `CLAUDE.md`:
    - Current state: the `title` context (`service/title/{domain,data,fake}`, Home lists) and the `core:model`
      types.
    - Reuse table: `Title`/`TitleId`/`Language` are marked as existing; add rows for `FakeHomeListsRepository`
      and for "SQLDelight database in a context" (add `src/commonMain/sqldelight`; `tmdb.module` names it
      `<Context>Database`).
    - Drop the "planned" note for `core:model` and `Clock`.
- Unit tests: none.
- Compile: `xcodebuild -project cmp/iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build` (macOS + Xcode)

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-7 (data part) | 4, 5 | `failedRefreshKeepsCachedTitles` (contract), `networkErrorWithCacheKeepsTitles` |
| AC-9 (data part) | 5 | `staleListIsRefetched`, `freshListIsNotRefetched`, `forcedRefreshIgnoresAge`, `TitleRefreshPolicyTest` |
| AC-10 (data part) | 2, 5 | `LanguageTest`, `refreshSendsLanguageAndPage` |
| EC-7 (data part) | 4, 5 | `languagesAreCachedSeparately` (contract, runs against impl and fake) |
| EC-8 (data part) | 2 | `LanguageTest.otherLanguagesMapToEnUs` |
| EC-9 (data part) | 5 | `concurrentRefreshesSendOneRequest` |
| Ten lists use the agreed endpoints | 5 | `HomeListRequestTest` (10 cases) |
| Reachable from the apps | 6, 7 | `InitKoinTest`, `SharedCoreTest`; manual: `./gradlew publishSharedCore` lists `service-title-domain` in `build/maven-repo` and `HomeListsRepository` in the `TmdbCore` headers; `cmp/iosApp` builds |

## Out of scope
- UI, the ViewModel, when refresh is called, Loading/Error/Empty → 04, 05.
- Using the repository from `native-android` / `native-ios` → 06, 07.
- Title details and their cache → the details feature.
- Pages after the first.
- Clearing old cache rows (lists are replaced, so they don't grow).

## Risks
- **SQLDelight plugin with AGP 9's `com.android.kotlin.multiplatform.library`**: if 2.4.1 doesn't generate code for
  the new Android KMP plugin, Step 5 fails to compile. Mitigation: SQLDelight generates into `commonMain`, so check
  the generated sources in Step 5 first; if they are missing, stop and report it (a large deviation).
- **`kotlin.time.Clock` / `Instant` may still need `@OptIn(ExperimentalTime::class)` on Kotlin 2.4.21**. Mitigation:
  add the opt-in where needed and record it as a small deviation.
- **Kover counts enums and generated code**: the domain module has only enums plus an interface, covered by
  `HomeCategoryTest`. The generated SQLDelight code is in `*.generated.*`, which is excluded.
- **Real TMDB results may differ from the fixtures** (e.g. novelas with no recent airing return an empty list,
  EC-3). This is expected: an empty list is cached as loaded. A manual check with a real token is in 04.

## Deviations
- The extra tests of `HomeListsRepositoryImplTest` are split in two because of Detekt `TooManyFunctions` (max 11):
  - The mapping cases move to `HomeListMappingTest`: TV type, w500 URL, no poster, no name, replace in order.
  - The engine, database and clock setup is shared in a test-only `TitleTestEnvironment`. It builds the real
    implementation, so it is not a fake.
- One extra test: `HomeListsRepositoryImplTest.discoverRequestSendsFilters`. It checks that Discover filters
  reach the URL.
- `kotlin.time.Clock` / `Instant` needed no opt-in on Kotlin 2.4.21, and SQLDelight 2.4.1 generated code with the
  AGP 9 KMP library plugin, so both risks are cleared.
