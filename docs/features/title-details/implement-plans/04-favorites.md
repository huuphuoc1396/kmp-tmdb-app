# Plan: Favorites (shared core + cmp)

Requirement: [04 - Favorites (shared core + cmp)](../subtasks/04-favorites.md)
Status: Done

## Summary
Create the `favorites` context: `service/favorites/{domain,data,fake}`. Its data module owns a SQLCipher
`favorites.db` with one table of title IDs and the time each was added, and it is local-only, so there is no
refresh and no language. `FavoritesRepository` exposes `observeIsFavorite(id)` and `setFavorite(id, isFavorite)`.
`core:di` binds it and `core:ios` exports it, so all three apps can use it. In cmp, `TitleDetailsViewModel` shows
the heart from the repository and keeps the last tapped value until it is stored, so quick taps never flicker or
get lost. The heart sits in the fixed top bar and is shown only in the Loaded state.

## Decisions
- **Storage: SQLDelight `favorites.db`** (from user; rejected: encrypted DataStore). Opened like `title.db`:
  `DatabaseDriverFactory.create(schema, name, passphrase = DatabaseKeys.passphrase(name))`, lazily, on the IO
  dispatcher (architecture 7, "Schema ownership").
- **Stored data: `tmdbId`, `type`, `addedAt` only** (from user; rejected: also the name and poster). A future
  favorites list reads names and posters through `title`. `addedAt` is in epoch milliseconds from the injected
  `Clock`.
- **API: `setFavorite(id, isFavorite)`** (from user; rejected: `toggle(id)`):
  ```kotlin
  interface FavoritesRepository {
      fun observeIsFavorite(id: TitleId): Flow<Boolean>
      suspend fun setFavorite(id: TitleId, isFavorite: Boolean)
  }
  ```
  - Setting `true` on a title that is already a favorite keeps its first `addedAt` (`INSERT OR IGNORE`).
  - Setting `false` on a title that is not a favorite does nothing.
  - Writes return `Unit`, not `AppResult`: a local write has no `AppError` to map, as with `SettingsRepository`.
- **Quick taps (EC-1) with `setFavorite`**: the ViewModel keeps the value of the last tap (`pendingFavorite`) and
  shows it until the database has stored it. Writes go through one `Channel` in tap order, so the last tap is
  always the last write. Each tap flips the shown value, so every tap toggles once and the final state is the last
  tap's.
- **Favorites don't depend on the language** (AC-3, EC-9): the ViewModel observes them by `TitleId` only, outside
  the language `flatMapLatest`.
- **Heart visible only when Loaded** (AC-4): `TitleDetailsUiState.isFavorite: Boolean?` is `null` until the first
  value arrives. `DetailsTopBar` gets `isFavorite: Boolean?` and draws the heart when the state is Loaded and the
  value is not `null`.
- **Heart button** (design "Favorite button"):
  - It is 32dp and sits at the right of the 48dp top-bar row. The design puts its right edge 30dp from the screen
    edge.
  - It has a 48dp touch target (`minimumInteractiveComponentSize`), so the clickable area starts 22dp from the
    edge and the visible circle stays at 30dp.
  - Semantics: `Modifier.toggleable(value = isFavorite, role = Role.Switch)` with
    `contentDescription = "Favorite" / "Favorito"`. Screen readers announce it as a toggle with on/off (AC-3).
- **Heart drawables** from `design/assets/ic_favorite_button.svg`:
  - `ic_favorite_off.xml`: the circle (`#303243` at 65%) with the outline heart, as designed.
  - `ic_favorite_on.xml`: the same circle with the heart's outer path only (the SVG's first subpath, up to
    `V10.6107Z`), filled `#FF1F8A`. This is the "on" state the user decided.
  - Both are 32×32dp VectorDrawables in `cmp/feature/title-details/ui` `composeResources/drawable/`.
- **No `domain → domain` dependency**: `title` doesn't need favorites yet. The `title → favorites` pair already in
  `DependencyRules.domainAllowlist` stays unused until Home shows the flag.
- **Native apps**: `publishSharedCore` publishes every production module with code, so
  `service-favorites-domain` reaches `build/maven-repo` with no change. `core:ios` exports
  `service:favorites:domain` (`api`) and gets `SharedCore.favoritesRepository`. The native screens use them in 05
  and 06.
- **No migration**: `favorites.db` is new.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Context shape (domain / data / fake, contract test) | `title` context | `service/title/*` |
| Encrypted database, lazy open on IO | `titleDataModule`, `TitleListLocalDataSource` | `service/title/data` |
| Driver and passphrase | `DatabaseDriverFactory`, `DatabaseKeys` | `core:database`, `core:security` |
| Dispatchers, time | `DispatcherProvider`, `kotlin.time.Clock` (bound in `core:di`) | `core:common`, stdlib |
| In-memory database in host tests | `JdbcSqliteDriver(IN_MEMORY)` as in `TitleDetailsTestEnvironment` | `service/title/data` tests |
| Fake build setup (contract in `commonMain`) | `service/title/fake/build.gradle.kts` | `service/title/fake` |
| Top bar with the pill style | `DetailsTopBar` | `cmp/feature/title-details/ui` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| root | `settings.gradle.kts` (modify) | Include the three new modules |
| `service/favorites/domain` (new) | `build.gradle.kts`, `FavoritesRepository.kt` | Public API |
| `service/favorites/fake` (new) | `build.gradle.kts`, `FakeFavoritesRepository.kt`, `FavoritesRepositoryContract.kt`, test `FakeFavoritesRepositoryTest.kt` | Test double and contract |
| `service/favorites/data` (new) | `build.gradle.kts`, `FavoritesRepositoryImpl.kt`, `local/FavoritesLocalDataSource.kt`, `di/FavoritesDataModule.kt`, `sqldelight/.../generated/Favorite.sq`, test `FavoritesRepositoryImplTest.kt` | Storage |
| `core:di` | `build.gradle.kts`, `InitKoin.kt` (modify); `androidDeviceTest/.../InitKoinTest.kt` (modify) | Bind the module |
| `core:ios` | `build.gradle.kts`, `SharedCore.kt` (modify); `iosTest/.../SharedCoreTest.kt` (modify) | Export to iOS |
| `cmp/feature/title-details/ui` | `build.gradle.kts`, `TitleDetailsContract.kt`, `TitleDetailsViewModel.kt`, `TitleDetailsScreen.kt`, `component/DetailsTopBar.kt`, `di/TitleDetailsUiModule.kt`, strings, `drawable/ic_favorite_{off,on}.xml` (modify / new); tests | Heart button |
| docs | `CLAUDE.md` (modify) | New context in current state, modules and reuse table |

## Steps
### Step 1: Favorites domain  (AC-3) ✅
- Files: `settings.gradle.kts` (add `:service:favorites:domain`, `:service:favorites:data`,
  `:service:favorites:fake` after the settings ones), `service/favorites/domain/build.gradle.kts` (new, copy of
  title domain without `kotlinx-datetime`),
  `service/favorites/domain/src/commonMain/kotlin/com/banhmilabs/tmdb/service/favorites/domain/FavoritesRepository.kt`
  (new).
- What: `FavoritesRepository` as in Decisions. KDoc: local-only and the same in every language; reads never fail;
  `setFavorite(true)` keeps the first time a title was added.
- Unit tests: none (interface).
- Compile: `./gradlew :service:favorites:domain:compileAndroidMain`

### Step 2: Fake and contract  (AC-3, EC-1) ✅
- Files: `service/favorites/fake/build.gradle.kts` (new, copy of title fake), `FakeFavoritesRepository.kt`,
  `FavoritesRepositoryContract.kt` (new, `commonMain`),
  `src/androidHostTest/.../FakeFavoritesRepositoryTest.kt` (new).
- What:
  - `FakeFavoritesRepository(initial: Set<TitleId> = emptySet())`: a `MutableStateFlow<Set<TitleId>>`;
    `observeIsFavorite` maps it with `distinctUntilChanged`; `setFavorite` adds or removes. It records
    `writes: List<Pair<TitleId, Boolean>>` for ViewModel tests.
  - `abstract class FavoritesRepositoryContract { protected abstract fun TestScope.createRepository():
    FavoritesRepository }`. Its tests:
    - `notFavoriteByDefault`
    - `setFavoriteTrueMakesItFavorite`
    - `setFavoriteFalseRemovesIt`
    - `favoritesAreKeptPerTitle`: another `TitleId`, and the same `tmdbId` with the other `TitleType`, stays
      `false`.
    - `settingTwiceIsIdempotent`: `true`, `true`, then `false` → `false`.
    - `observerSeesChanges`: Turbine, `false` → `true` → `false`.
  - `CONTRACT_MOVIE_ID = TitleId(550, TitleType.MOVIE)` in the contract's companion.
  - `FakeFavoritesRepositoryTest : FavoritesRepositoryContract()`.
- Compile: `./gradlew :service:favorites:fake:compileAndroidMain :service:favorites:fake:compileAndroidHostTest`

### Step 3: Data module with `favorites.db`  (AC-3) ✅
- Files:
  - `service/favorites/data/build.gradle.kts` (new). Dependencies as title data, without network or
    serialization: domain, `core:database`, `core:security`, sqldelight coroutines, koin core. Host tests: fake,
    sqlite driver, coroutines test, turbine.
  - `src/commonMain/sqldelight/com/banhmilabs/tmdb/service/favorites/data/generated/Favorite.sq` (new):
    ```sql
    -- Titles the user marked as favorites; local only, the same in every language.
    CREATE TABLE favorite (
        tmdbId INTEGER NOT NULL,
        type TEXT NOT NULL,
        addedAt INTEGER NOT NULL,
        PRIMARY KEY (tmdbId, type)
    );

    isFavorite:
    SELECT EXISTS(SELECT 1 FROM favorite WHERE tmdbId = ? AND type = ?);

    insert:
    INSERT OR IGNORE INTO favorite (tmdbId, type, addedAt) VALUES (?, ?, ?);

    delete:
    DELETE FROM favorite WHERE tmdbId = ? AND type = ?;
    ```
  - `local/FavoritesLocalDataSource.kt` (new, internal):
    - `(database: Lazy<FavoritesDatabase>, dispatchers: DispatcherProvider)`.
    - `observe(id): Flow<Boolean>` reads `isFavorite` with `asFlow().mapToOne(io)`, wrapped in `flow { emitAll(…) }`
      and `.flowOn(io)` so the database opens off the main thread.
    - `insert(id, addedAt: Instant)` and `delete(id)` run in `withContext(io)`.
  - `FavoritesRepositoryImpl.kt` (new, internal): `(local, clock)`. `setFavorite(true)` calls
    `insert(id, clock.now())` and `false` calls `delete(id)`.
  - `di/FavoritesDataModule.kt` (new): `FAVORITES_DATABASE_NAME = "favorites.db"`, a `single` for
    `FavoritesDatabase` as in `titleDataModule`, and `single<FavoritesRepository>`.
  - Tests in `src/androidHostTest/.../FavoritesRepositoryImplTest.kt` (new):
    - It extends `FavoritesRepositoryContract`, over an in-memory `JdbcSqliteDriver`, an `UnconfinedTestDispatcher`
      `DispatcherProvider` (MockK, as in title's environments) and a MockK `Clock`.
    - Extra test `addedAtIsKeptWhenSetAgain`, which reads the `favorite` row through the generated queries.
- Compile: `./gradlew :service:favorites:data:compileAndroidMain :service:favorites:data:compileAndroidHostTest`

### Step 4: Bind in `core:di` and export in `core:ios`  (AC-3) ✅
- Files:
  - `core/di/build.gradle.kts`: add `implementation(projects.service.favorites.data)`, plus the domain in the
    device-test dependencies next to the others.
  - `InitKoin.kt`: add `favoritesDataModule` to `modules(...)`.
  - `InitKoinTest.kt` (device): add `initKoinProvidesFavoritesRepository` and `favoritesDatabaseIsEncrypted`, as
    the title ones.
  - `core/ios/build.gradle.kts`: `api(projects.service.favorites.domain)`.
  - `SharedCore.kt`: `val favoritesRepository: FavoritesRepository`.
  - `SharedCoreTest.kt` (iOS): `startProvidesFavoritesRepository`.
- Compile: `./gradlew :core:di:compileAndroidMain :core:di:compileAndroidDeviceTest :core:ios:compileKotlinIosSimulatorArm64`

### Step 5: Heart state in the ViewModel  (AC-3, AC-4, EC-1, EC-9) ✅
- Files: `cmp/feature/title-details/ui/build.gradle.kts` (add `projects.service.favorites.domain`, and in host tests
  `projects.service.favorites.fake`), `TitleDetailsContract.kt`, `TitleDetailsViewModel.kt`,
  `di/TitleDetailsUiModule.kt` (modify).
- What:
  - `TitleDetailsUiState` gets `isFavorite: Boolean? = null` ("`null` until the stored value is known").
  - New intent `data object FavoriteClicked`.
  - The constructor becomes `(titleId, repository: TitleDetailsRepository, favorites: FavoritesRepository)`, and
    Koin uses `favorites = get()`.
  - Pseudo-code:
    ```kotlin
    /** The last tapped value, shown until the database has stored every tap. */
    private val pendingFavorite = MutableStateFlow<Boolean?>(null)
    private val queuedFavoriteWrites = MutableStateFlow(0)
    private val favoriteWrites = Channel<Boolean>(Channel.UNLIMITED)

    private val isFavorite: Flow<Boolean> = combine(
        favorites.observeIsFavorite(titleId), pendingFavorite, queuedFavoriteWrites,
    ) { stored, pending, queued ->
        if (pending != null && (queued > 0 || stored != pending)) pending else stored
    }.distinctUntilChanged()

    // state: combine(the language-based details flow, isFavorite) { details, favorite ->
    //     details.copy(isFavorite = favorite) }; the initial value stays TitleDetailsUiState().

    init {
        viewModelScope.launch {
            for (value in favoriteWrites) {
                favorites.setFavorite(titleId, value)
                queuedFavoriteWrites.update { it - 1 }
            }
        }
    }

    private fun toggleFavorite() {
        val current = state.value.isFavorite ?: return
        pendingFavorite.value = !current
        queuedFavoriteWrites.update { it + 1 }
        favoriteWrites.trySend(!current)
    }
    ```
    The pending value is shown while writes are queued, and until the stored value matches it. This avoids a
    one-frame flash of the old value between the write and the database's next emission. Once they match, the
    stored value is shown again, so a second details entry of the same title (EC-7) shows the truth.
  - Detekt `TooManyFunctions` (11): the ViewModel stays under the limit. If not, move the favorite logic into a
    private class in the same file and record it as a deviation.
- Unit tests in `TitleDetailsViewModelFavoriteTest` (new), with `FakeFavoritesRepository`:
  - `favoriteIsShownFromStorage`: initial set has the title → `isFavorite == true`.
  - `favoriteClickStoresTrue`: `false` → tap → `true`, and `writes == [(id, true)]`.
  - `secondClickRemovesFavorite`: two taps → `false`, writes `[true, false]`.
  - `quickTapsEndOnLastTap`: three taps without advancing on a `StandardTestDispatcher`. The shown value is `true`
    after each odd tap; after `advanceUntilIdle` the stored and shown value is `true` and the writes are
    `[true, false, true]`.
  - `favoriteUnchangedByLanguage`: marked, then the language switches → still `true`, and no write happens.
  - `clickBeforeFavoriteKnownDoesNothing`: tap before the state is subscribed → no write.
  - The existing ViewModel tests get `FakeFavoritesRepository()` in their constructor call.
- Compile: `./gradlew :cmp:feature:title-details:ui:compileAndroidMain :cmp:feature:title-details:ui:compileAndroidHostTest :cmp:composeApp:compileAndroidMain`

### Step 6: Heart button on the screen  (AC-3, AC-4) ✅
- Files: `component/DetailsTopBar.kt`, `TitleDetailsScreen.kt` (modify), `composeResources/drawable/ic_favorite_off.xml`,
  `ic_favorite_on.xml` (new), `values/strings.xml`, `values-pt-rBR/strings.xml` (modify).
- What:
  - Strings: `details_favorite` "Favorite" / "Favorito".
  - Drawables as in Decisions: viewport 32×32. The circle is a path for `cx 16 cy 16 r 16`, with
    `fillColor="#303243"` and `fillAlpha="0.65"`. The heart path comes from the SVG: off uses the full path in
    white, on uses the first subpath in `#FF1F8A`.
  - `DetailsTopBar(onBack, isFavorite: Boolean?, onFavoriteClick: () -> Unit, modifier)`:
    - When `isFavorite != null`, an `Image` of `ic_favorite_on` or `ic_favorite_off` at 32dp, aligned
      `CenterEnd`.
    - Modifiers: `.padding(end = 22.dp)`, `.minimumInteractiveComponentSize()`, `.clip(CircleShape)`, and
      `.toggleable(value = isFavorite, role = Role.Switch, onValueChange = { onFavoriteClick() })`.
    - Semantics: `contentDescription` set to the string, through `semantics` on the toggleable.
  - `TitleDetailsContent` passes `isFavorite = state.isFavorite.takeIf { state.load is DetailsLoad.Loaded }` and
    `onFavoriteClick = { onIntent(TitleDetailsIntent.FavoriteClicked) }`.
- Unit tests: none (UI). Covered by How to test 1, 6 and 7.
- Compile: `./gradlew :cmp:feature:title-details:ui:compileAndroidMain`

### Step 7: Docs ✅
- Files: `CLAUDE.md`.
- What:
  - Current state: add the `favorites` context (`service/favorites/{domain,data,fake}`, local favorites in
    SQLDelight), and drop "The `favorites` context is created by the first feature that needs it".
  - Reuse table: add `FavoritesRepository` (observe and set a title's favorite) in `service:favorites:domain`, and
    `FakeFavoritesRepository`.
- Compile: none (docs).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-3 Favorite on / off, stored, offline, both languages | 1, 2, 3, 5, 6 | Contract tests (fake and real), `favoriteClickStoresTrue`, `secondClickRemovesFavorite`, `favoriteUnchangedByLanguage`; manual: How to test 1–4 |
| AC-3 Screen reader toggle | 6 | manual: How to test 7 |
| AC-3 Available to every app | 4 | `initKoinProvidesFavoritesRepository` (device), `startProvidesFavoritesRepository` (iOS) |
| AC-4 Heart only with details | 5, 6 | `clickBeforeFavoriteKnownDoesNothing`; manual: How to test 6 |
| EC-1 Quick taps | 5 | `quickTapsEndOnLastTap`; manual: How to test 5 |
| EC-9 Favorite unchanged by language (partial, rest in 03) | 5 | `favoriteUnchangedByLanguage`; manual: How to test 4 |

## Out of scope
- A favorites list screen and the favorite flag on Home posters (separate features; they will use the
  `title → favorites` allowlist pair).
- Native apps -> 05, 06 (they get the repository from the published core and `SharedCore`).
- Syncing favorites to a TMDB account.

## Risks
- **Kover 80% on `service:favorites:data`.** The DI module is excluded, and the contract covers the rest.
  Mitigation: `./gradlew koverVerify` in Finish.
- **Device and iOS tests not run in the loop.** `InitKoinTest` and `SharedCoreTest` gain tests that only compile
  here. Mitigation: run `:core:di:connectedAndroidDeviceTest` and `:core:ios:iosSimulatorArm64Test` before the PR
  if a device or simulator is available, and say if they are skipped.
- **The flicker guard depends on emission order.** If the database emits the stored value before the write
  counter drops, the guard still holds, because the pending value stays until the stored value matches. Covered
  by `quickTapsEndOnLastTap`.

## Deviations
- **Extra query `selectAddedAt`** in `Favorite.sq`, used only by `addedAtIsKeptWhenSetAgain` to read the stored time.
- **`FavoriteButton`** is a private composable in `DetailsTopBar.kt`, so the top bar stays short.
- **`quickTapsEndOnLastTap`** runs the scheduler (`runCurrent`) after each tap, because the state is a `stateIn`
  flow and updates only on dispatch. The fake writes at once, so the test checks that each tap flips the shown
  value and that the writes keep tap order. It does not hold writes in the queue.
- **`CLAUDE.md`** also says title details are cached (that was missing), next to the new favorites context.
- **Checks run**:
  - Host tests passed: `service:favorites:fake` (6), `service:favorites:data` (7), `cmp:feature:title-details:ui`
    (35).
  - Detekt passed on every touched module.
  - `:architecture-test:test` and `koverVerify` passed; `service:favorites:data` has 100% line coverage.
  - `:cmp:androidApp:assembleDebug` passed.
  - `:core:ios:iosSimulatorArm64Test` passed: `SharedCoreTest`, 5 tests.
  - `:core:di:connectedAndroidDeviceTest` passed on a Pixel 8 emulator (API 36) and a Galaxy S23 (API 37), 5 tests
    each.
  - Not run: `./gradlew assemble` for every target. The iOS code of `core:ios` was compiled and tested on the
    simulator.
  - Not checked by hand: How to test 1–7 (heart on a device, restart, airplane mode, TalkBack / VoiceOver).
- **Simpler heart state** (from user, after the PR was opened):
  - Removed: `pendingFavorite`, `queuedFavoriteWrites` and the write `Channel`.
  - The heart now shows `observeIsFavorite(titleId)` as stored.
  - A tap launches `setFavorite(titleId, !current)`.
  - The trade-off for EC-1: two taps faster than a database write can both read the same value, so they may store
    the same state instead of toggling twice. `quickTapsEndOnLastTap` still passes, because the fake writes at
    once.
