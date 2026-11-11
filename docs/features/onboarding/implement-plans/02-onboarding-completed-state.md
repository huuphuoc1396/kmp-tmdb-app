# Plan: Onboarding completed state

Requirement: [02 - Onboarding completed state](../subtasks/02-onboarding-completed-state.md)
Status: Done

## Summary
This plan creates the `settings` context: `service/settings/domain` with a `SettingsRepository`
(`observeOnboardingCompleted()`, `completeOnboarding()`), and `service/settings/data` that stores
`AppSettings(onboardingCompleted)` in the encrypted DataStore from 01. It also adds `service/settings/fake`, a
`FakeSettingsRepository` plus a contract test that both the fake and the real implementation extend. The kernel
types this first service needs (`AppResult`, `AppError`, `DispatcherProvider`) are created in `core:common`, and
`core:di` gets `initKoin()` with Koin wiring, including the platform file path. There are no use cases: both
operations only forward to the repository (architecture 6.2).

## Decisions
- No use cases. — Both operations only forward to the repository; architecture 6.2 / CLAUDE.md forbid
  forwarding-only use cases. ViewModels in 03 / 05 / 06 call `SettingsRepository` directly. (The subtask's
  "Use cases (domain)" layer predates the architecture change and is satisfied by the repository interface.)
- The repository is named for its context: `SettingsRepository` in `service/settings/domain`, with only the two
  onboarding operations. — Architecture 5.1 / CLAUDE.md: the `settings` context owns language and onboarding;
  repository interfaces expose only what consumers need (6.2). Language comes later as new members.
- `service/settings/fake` is created now, with `FakeSettingsRepository` and `SettingsRepositoryContract`. —
  03, 05 and 06 all need it, and the contract is best written together with the real implementation. (from
  user; rejected: creating it in 03.)
- If reading the settings fails with an I/O error, `observeOnboardingCompleted()` emits `false` and doesn't
  fail. — Onboarding then shows again and never blocks the app, consistent with EC-1. Corrupt or undecryptable
  files already reset to defaults in 01. (from user; rejected: surfacing a read error.)
- `completeOnboarding()` returns `AppResult<Unit>`. A `CipherException` or `IOException` from the write becomes
  `AppResult.Failure(AppError.Unknown)`, and the stored value stays as before. — Architecture 7: errors map to
  `AppError` and never clear data. The UI handles the failure by entering Home anyway (EC-1, in 03 / 05 / 06).
- Kernel types, created now because this is their first use:
  - `AppResult<T>`: `Success(value)` / `Failure(error)`.
  - `AppError`: `Network`, `Unauthorized`, `NotFound`, `Unknown`, as data objects.
  - `DispatcherProvider` with only `io`, plus `DefaultDispatcherProvider`.

  Only the members needed now; more are added when first used. — "No speculative code."
- DI: Koin 4.2.2.
  - `settingsDataModule` lives in `service/settings/data` (package `…data.di`, which Kover excludes).
  - `initKoin(appDeclaration)` in `core:di` installs a core module (`DispatcherProvider`) and
    `settingsDataModule`.
  - The DataStore scope is `CoroutineScope(SupervisorJob() + dispatcherProvider.io)`.
  - The master key alias is `"settings"`.
- Settings file location, as an `internal expect fun Scope.settingsFilePath(): Path` in `…data.di`:
  - Android: `androidContext().filesDir / "settings.json"`. The app shell passes the context through `initKoin { androidContext(...) }`.
  - iOS: Application Support directory `/ "settings.json"`.
  - JVM (tests only): `SYSTEM_TEMPORARY_DIRECTORY / "kmp-tmdb" / "settings.json"`.
- Tests use the real encrypted DataStore (temporary file, JVM cipher) for the implementation. DataStore and
  the cipher aren't mocked except where a failure must be forced; that case uses a MockK `MasterKeyCipher`, a
  same-module-style collaborator, as in 01.
- The contract tests use `runTest`'s `backgroundScope` as the DataStore scope, so each store is cancelled
  automatically at the end of the test.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Encrypted settings store | `encryptedDataStore(...)` | `core/datastore/src/commonMain/kotlin/com/banhmilabs/tmdb/core/datastore/EncryptedDataStore.kt` |
| Master key cipher (and `CipherException`) | `masterKeyCipher(alias)` | `core/security/src/commonMain/kotlin/com/banhmilabs/tmdb/core/security/MasterKeyCipher.kt` |
| Temporary-file DataStore test pattern | `EncryptedDataStoreTest` | `core/datastore/src/jvmTest/kotlin/com/banhmilabs/tmdb/core/datastore/EncryptedDataStoreTest.kt` |
| Module type, dependency rules, Kover for `domain` / `data`, serialization plugin | `tmdb.module` | `build-logic/convention/src/main/kotlin/ModuleConventionPlugin.kt` |
| Context layout (domain / data / fake, contract test) | Architecture 5.1 shop example; testing rules | `docs/architecture.md` 5.1, `.claude/rules/testing.md` |

Nothing exists yet for `AppResult`, `AppError`, `DispatcherProvider`, Koin, or any `service/*` module; all are
new. `core:testing` (test dispatchers) is not needed: no class under test takes a `DispatcherProvider`.

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| root | `gradle/libs.versions.toml`, `settings.gradle.kts` (modify) | Koin and Turbine; three new modules |
| `core:common` | `build.gradle.kts` (modify); `AppResult.kt`, `AppError.kt`, `DispatcherProvider.kt` (new) | Kernel types |
| `service:settings:domain` (new) | `build.gradle.kts`, `SettingsRepository.kt` | Public API of the context |
| `service:settings:fake` (new) | `build.gradle.kts`, `FakeSettingsRepository.kt`, `SettingsRepositoryContract.kt`, `FakeSettingsRepositoryTest.kt` | Test double + contract |
| `service:settings:data` (new) | `build.gradle.kts`, `AppSettings.kt`, `SettingsRepositoryImpl.kt`, `di/SettingsDataModule.kt`, `di/SettingsFilePath.*.kt`, `SettingsRepositoryImplTest.kt` | Implementation + Koin module |
| `core:di` | `build.gradle.kts` (modify); `InitKoin.kt`, `InitKoinTest.kt` (new); `DiSmokeTest.kt` (delete) | Composition root |
| docs | `CLAUDE.md` (modify) | Current state: first context exists |

## Steps

### Step 1: Catalog and module registration  (setup) ✅
- Files: `gradle/libs.versions.toml`, `settings.gradle.kts` (modify)
- What:
  - `[versions]`: `koin = "4.2.2"`, `turbine = "1.2.1"`.
  - `[libraries]`: `koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }`,
    `koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }`,
    `turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }`.
  - `settings.gradle.kts` `include(...)`: `":service:settings:domain"`, `":service:settings:data"`,
    `":service:settings:fake"` (added in steps 3–5 together with their directories, so each step configures).
- Unit tests: none.
- Compile: `./gradlew help`

### Step 2: Kernel types in `core:common`  (EC-1 partial) ✅
- Files (under `core/common/`, package `com.banhmilabs.tmdb.core.common`):
  - `build.gradle.kts` (modify): `commonMain`: `api(libs.kotlinx.coroutines.core)` (`CoroutineDispatcher` in the API).
  - `src/commonMain/.../AppResult.kt` (new):
    ```kotlin
    sealed interface AppResult<out T> {
        data class Success<out T>(val value: T) : AppResult<T>
        data class Failure(val error: AppError) : AppResult<Nothing>
    }
    ```
  - `src/commonMain/.../AppError.kt` (new):
    `sealed interface AppError { data object Network; data object Unauthorized; data object NotFound; data object Unknown }`
    (each `: AppError`).
  - `src/commonMain/.../DispatcherProvider.kt` (new):
    `interface DispatcherProvider { val io: CoroutineDispatcher }` and
    `class DefaultDispatcherProvider : DispatcherProvider { override val io = Dispatchers.IO }`
    (`kotlinx.coroutines.IO` import for common code).
- Unit tests: none (plain types; `CommonSmokeTest` stays until a real test exists).
- Compile: `./gradlew :core:common:compileKotlinJvm :core:common:compileKotlinIosSimulatorArm64`

### Step 3: `service:settings:domain`  (AC-3 partial) ✅
- Files:
  - `settings.gradle.kts`: include `":service:settings:domain"`.
  - `service/settings/domain/build.gradle.kts` (new): `plugins { alias(libs.plugins.tmdb.module) }`;
    `commonMain`: `api(projects.core.common)`.
  - `service/settings/domain/src/commonMain/kotlin/com/banhmilabs/tmdb/service/settings/domain/SettingsRepository.kt` (new):
    ```kotlin
    /** App settings stored on the device. */
    interface SettingsRepository {
        /** `true` once onboarding was completed; `false` when nothing is stored or it can't be read. */
        fun observeOnboardingCompleted(): Flow<Boolean>

        /** Records that onboarding was completed. A failure leaves the stored value unchanged. */
        suspend fun completeOnboarding(): AppResult<Unit>
    }
    ```
- Unit tests: none (interface only; covered by the contract in step 4).
- Compile: `./gradlew :service:settings:domain:compileKotlinJvm`

### Step 4: `service:settings:fake` and the contract  (AC-3 partial, EC-2 partial) ✅
- Files (package `com.banhmilabs.tmdb.service.settings.fake`):
  - `settings.gradle.kts`: include `":service:settings:fake"`.
  - `service/settings/fake/build.gradle.kts` (new): `commonMain`: `api(projects.service.settings.domain)`,
    `implementation(libs.kotlin.test)`, `implementation(libs.kotlinx.coroutines.test)`, `implementation(libs.turbine)`
    (the contract lives in `commonMain`).
  - `src/commonMain/.../FakeSettingsRepository.kt` (new):
    ```kotlin
    class FakeSettingsRepository(onboardingCompleted: Boolean = false) : SettingsRepository {
        private val completed = MutableStateFlow(onboardingCompleted)
        private var nextCompleteError: AppError? = null

        /** The next [completeOnboarding] fails with [error] and leaves the state unchanged. */
        fun failNextComplete(error: AppError) { nextCompleteError = error }

        override fun observeOnboardingCompleted(): Flow<Boolean> = completed
        override suspend fun completeOnboarding(): AppResult<Unit> =
            nextCompleteError?.let { nextCompleteError = null; AppResult.Failure(it) }
                ?: run { completed.value = true; AppResult.Success(Unit) }
    }
    ```
  - `src/commonMain/.../SettingsRepositoryContract.kt` (new):
    `abstract class SettingsRepositoryContract { protected abstract fun TestScope.createRepository(): SettingsRepository }`
    with `@Test` cases (each `runTest { ... }`):
    `onboardingNotCompletedInitially`, `completeOnboardingReturnsSuccess`,
    `completeOnboardingNotifiesObserver` (Turbine: `false`, then `true`),
    `completeOnboardingTwiceStaysCompleted`.
  - `src/jvmTest/.../FakeSettingsRepositoryTest.kt` (new): extends the contract
    (`createRepository() = FakeSettingsRepository()`), plus
    `failNextCompleteReturnsErrorAndKeepsState`, `startsCompletedWhenConstructedSo`.
- Compile: `./gradlew :service:settings:fake:compileTestKotlinJvm`

### Step 5: `service:settings:data`  (AC-3 partial, AC-6 partial, EC-1 partial, EC-2 partial) ✅
- Files (package `com.banhmilabs.tmdb.service.settings.data`):
  - `settings.gradle.kts`: include `":service:settings:data"`.
  - `service/settings/data/build.gradle.kts` (new): `commonMain`: `implementation(projects.service.settings.domain)`,
    `implementation(projects.core.datastore)` (brings `core:security` and DataStore as `api`),
    `implementation(libs.koin.core)`; `androidMain`: `implementation(libs.koin.android)`;
    `jvmTest`: `implementation(projects.service.settings.fake)`, `implementation(libs.kotlinx.coroutines.test)`,
    `implementation(libs.turbine)`.
  - `src/commonMain/.../AppSettings.kt` (new):
    `@Serializable internal data class AppSettings(val onboardingCompleted: Boolean = false)`.
  - `src/commonMain/.../SettingsRepositoryImpl.kt` (new):
    `internal class SettingsRepositoryImpl(private val dataStore: DataStore<AppSettings>) : SettingsRepository`.
  - `src/commonMain/.../di/SettingsDataModule.kt` (new):
    ```kotlin
    val settingsDataModule = module {
        single {
            encryptedDataStore(
                serializer = AppSettings.serializer(),
                defaultValue = AppSettings(),
                path = settingsFilePath(),
                fileSystem = FileSystem.SYSTEM,
                cipher = masterKeyCipher(MASTER_KEY_ALIAS),
                scope = CoroutineScope(SupervisorJob() + get<DispatcherProvider>().io),
            )
        }
        single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    }
    private const val MASTER_KEY_ALIAS = "settings"
    internal const val SETTINGS_FILE_NAME = "settings.json"
    ```
  - `src/commonMain/.../di/SettingsFilePath.kt` (new): `internal expect fun Scope.settingsFilePath(): Path`.
  - `src/androidMain/.../di/SettingsFilePath.android.kt` (new):
    `androidContext().filesDir.toOkioPath() / SETTINGS_FILE_NAME`.
  - `src/iosMain/.../di/SettingsFilePath.ios.kt` (new):
    `NSFileManager.defaultManager.URLForDirectory(NSApplicationSupportDirectory, NSUserDomainMask, null, create = true, error = null)`
    → `path!!.toPath() / SETTINGS_FILE_NAME`; a missing URL throws `IllegalStateException("No Application Support directory")`.
  - `src/jvmMain/.../di/SettingsFilePath.jvm.kt` (new):
    `FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "kmp-tmdb" / SETTINGS_FILE_NAME`.
- Pseudo-code (`SettingsRepositoryImpl`):
  ```
  observeOnboardingCompleted() =
    dataStore.data
      .map { it.onboardingCompleted }
      .catch { if (it is IOException) emit(false) else throw it }   // decision: read error -> not completed
      .distinctUntilChanged()

  completeOnboarding() =
    try { dataStore.updateData { it.copy(onboardingCompleted = true) }; AppResult.Success(Unit) }
    catch (e: CipherException) { AppResult.Failure(AppError.Unknown) }
    catch (e: IOException) { AppResult.Failure(AppError.Unknown) }
  ```
- Unit tests: `src/jvmTest/.../SettingsRepositoryImplTest.kt` (new) extends `SettingsRepositoryContract`.
  `createRepository()` builds `SettingsRepositoryImpl(encryptedDataStore(..., path = <fresh temp dir>/settings.json, cipher = masterKeyCipher("SettingsRepositoryImplTest"), scope = backgroundScope))`.
  Temp directories are deleted in `@AfterTest`. Extra cases:
  - `completedSurvivesRestart` (complete with one store; a second store on the same path, created after the
    first scope is cancelled, reads `true`)
  - `writeFailureReturnsUnknownAndStaysNotCompleted` (MockK `MasterKeyCipher` whose `encrypt` throws
    `CipherException`)
  - `readIoErrorEmitsNotCompleted` (MockK `DataStore<AppSettings>` whose `data` is `flow { throw IOException() }`)
  - `corruptFileReadsNotCompleted` (random bytes written to the path first)
- Compile: `./gradlew :service:settings:data:compileTestKotlinJvm :service:settings:data:compileAndroidMain :service:settings:data:compileKotlinIosSimulatorArm64`
  (iOS needs macOS + Xcode)

### Step 6: `initKoin()` in `core:di`  (AC-3 partial) ✅
- Files (package `com.banhmilabs.tmdb.core.di`):
  - `core/di/build.gradle.kts` (modify): `commonMain`: `api(libs.koin.core)` (`KoinAppDeclaration` in the API),
    `implementation(projects.core.common)`, `implementation(projects.service.settings.data)`.
  - `src/commonMain/.../InitKoin.kt` (new):
    ```kotlin
    /** Starts Koin with every shared-core module. Platforms add their own via [appDeclaration]
     *  (Android: `androidContext(...)`). */
    fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication = startKoin {
        appDeclaration()
        modules(coreModule, settingsDataModule)
    }
    private val coreModule = module { single<DispatcherProvider> { DefaultDispatcherProvider() } }
    ```
  - `src/jvmTest/.../InitKoinTest.kt` (new): `initKoinProvidesSettingsRepository` (`initKoin()`,
    `getKoin().get<SettingsRepository>()` is not null, `stopKoin()` in `@AfterTest`). `jvmTest`:
    `implementation(projects.service.settings.domain)`.
  - Delete `src/jvmTest/.../DiSmokeTest.kt`.
- Compile: `./gradlew :core:di:compileTestKotlinJvm :core:di:compileAndroidMain :core:di:compileKotlinIosSimulatorArm64`

### Step 7: Docs ✅
- Files: `CLAUDE.md` (modify)
- What: "Current state": `core:security`, `core:datastore`, `core:common` (`AppResult`, `AppError`,
  `DispatcherProvider`), `core:di` (`initKoin`) and the `settings` context (`domain`, `data`, `fake`) have
  production code; the other modules are still skeletons. "Reuse before you create": drop the "not yet
  implemented" note for the rows that now exist, and add `encryptedDataStore` / `masterKeyCipher` and
  `FakeSettingsRepository`.
- Unit tests: none.
- Compile: none (docs only).

After the last step, run: `./gradlew :core:common:jvmTest :service:settings:domain:jvmTest :service:settings:fake:jvmTest :service:settings:data:jvmTest :core:di:jvmTest`,
`./gradlew :core:common:detekt :service:settings:domain:detekt :service:settings:fake:detekt :service:settings:data:detekt :core:di:detekt`,
`./gradlew :architecture-test:test koverVerify`, then `./gradlew assemble` (new `expect`/`actual`; macOS + Xcode).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-3 (partial: "completed" after it was recorded, including after a restart) | 3, 4, 5, 6 | `SettingsRepositoryContract.completeOnboardingNotifiesObserver` (impl + fake), `SettingsRepositoryImplTest.completedSurvivesRestart`, `InitKoinTest.initKoinProvidesSettingsRepository` |
| AC-6 (partial: no network request) | 5 | By construction: `service:settings:data` has no `core:network` dependency (checked by `tmdb.module`); manual check in review |
| EC-1 (partial: failed save returns an error, state stays "not completed") | 2, 5 | `SettingsRepositoryImplTest.writeFailureReturnsUnknownAndStaysNotCompleted`, `FakeSettingsRepositoryTest.failNextCompleteReturnsErrorAndKeepsState` |
| EC-2 (partial: nothing recorded -> "not completed") | 4, 5 | `SettingsRepositoryContract.onboardingNotCompletedInitially` (impl + fake), `SettingsRepositoryImplTest.corruptFileReadsNotCompleted` |
| Decision: read I/O error -> not completed | 5 | `SettingsRepositoryImplTest.readIoErrorEmitsNotCompleted` |

## Out of scope
- Onboarding UI, the start-screen choice and calling `initKoin` from an app shell (including
  `androidContext`) -> 03 / 05 / 06.
- An iOS-facing Koin helper for Swift (SKIE / `KoinHelper`) -> 06.
- Language settings in the `settings` context -> a later feature.
- `core:testing` test dispatchers -> when a class under test takes a `DispatcherProvider`.

## Risks
- `FileSystem.SYSTEM` may not resolve in `commonMain` for this target set — if not, add
  `internal expect val settingsFileSystem: FileSystem` next to `settingsFilePath()` with every actual
  returning `FileSystem.SYSTEM`.
- DataStore's `OkioStorage` must create the parent directory (`kmp-tmdb/` on JVM, Application Support on iOS
  is created by `create = true`) — `completedSurvivesRestart` and `InitKoinTest` cover JVM; if it doesn't,
  create the directory in `settingsFilePath()`.
- The contract in `fake`'s `commonMain` uses `kotlin-test` / `kotlinx-coroutines-test` / Turbine as main
  dependencies — allowed because `fake` is used only from test source sets (enforced by `tmdb.module`). Detekt
  may flag test-style names in main code; fix the names rather than suppress.
- Koin `single` for the DataStore must stay a singleton (one DataStore per file per process); `initKoin()` may
  only be called once per process, which is the app shell's job.
- iOS: the Keychain is unavailable to `iosSimulatorArm64Test` (see `.claude/rules/testing.md`), so no iOS run of
  the data tests; they run on JVM.

## Deviations
- `service:settings:fake` also depends on `kotlin-test-junit` in `jvmMain` / `androidMain` (new catalog entry):
  `kotlin-test` picks a JUnit variant only for test compilations, so `@Test` in the contract's `commonMain`
  didn't resolve otherwise.
- `FileSystem.SYSTEM` isn't visible in the common metadata compilation (found by `assemble`), so the planned
  mitigation was applied: `internal expect val settingsFileSystem` next to `settingsFilePath()`, every actual
  returning `FileSystem.SYSTEM`.
- `SettingsRepositoryImpl.completeOnboarding` carries `@Suppress("SwallowedException")` with a comment: mapping a
  write failure to `AppError.Unknown` (which has no cause) is the intended behavior (architecture 7).
- `SettingsRepositoryImplTest.createRepository()` passes `backgroundScope` directly as the DataStore scope.
