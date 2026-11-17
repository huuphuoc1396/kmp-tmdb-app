# Plan: Unit tests on the Android host

Requirement: [01 - Unit tests on the Android host](../subtasks/01-unit-tests-on-android-host.md)
Status: Done

## Summary
`tmdb.module` turns on the Android host test again (`withHostTest`, source set tree `test`, so `commonTest` runs
there too), and moves `kotlin-test` and MockK from `jvmTest` to `androidHostTest`. Every `jvmTest` file moves to
`androidHostTest`, except four groups:
- **Pass-through MockK cipher:** tests that used the in-memory JVM key now use a MockK cipher that passes data
  through unchanged.
- **Tests that need real encryption** (plaintext on disk, wrong key) and **the Koin start-up test:** they become
  Android device tests.
- **The Keystore test:** it leaves `commonTest` and becomes an Android device test.
- **Other platform-specific tests:** the iOS entry point test moves to `iosTest`, and the cmp Compose UI tests are
  deleted.

A root `testAndroidHostTest` task also runs the build-logic tests. CI and Kover use the host tests. The JVM target
itself stays until 02.

## Decisions
- **Pass-through MockK cipher** (from user, matches task.md AC-3).
  - `every { encrypt(any()) } answers { firstArg() }` and the same for `decrypt`, as a private helper in each
    test class that needs it.
  - The tests that need a real transform move to Android device tests with the real Keystore cipher:
    `fileOnDiskDoesNotContainPlaintext`, `keyMismatchResetsToDefault` and `writtenBytesDoNotContainPlaintext`.
  - Rejected: a MockK cipher with a reversible XOR transform, which would keep everything on the host.
- **`undecryptableBytesThrowCorruptionException` uses a MockK cipher whose `decrypt` throws `CipherException`.**
  With a pass-through cipher, random bytes would fail as invalid JSON instead, and the test would no longer check
  that the serializer maps cipher errors to `CorruptionException`.
- **`MasterKeyCipherTest` moves from `commonTest` to `androidDeviceTest` only** (from user).
  - On the host it would call the real Android Keystore.
  - iOS is covered by the `native-ios` Swift tests (task EC-2).
  - The `expect fun deleteMasterKeyForTest` and its JVM `actual` are removed. The Android and iOS versions become
    plain `internal fun`s in their test source sets. `KeychainCipherTest` (iOS) stays as it is.
- **`InitKoinTest` becomes an Android device test** (task.md default). It uses
  `initKoin { androidContext(targetContext) }`, because `settingsFilePath()` needs an Android `Context`.
- **`SharedCoreTest` moves to `iosTest` of `core:ios`** (task.md default). It only resolves objects:
  `KeychainCipher` creates its key lazily, so the test never touches the Keychain.
- **cmp Compose UI tests deleted** (task AC-4):
  - The deleted tests are `OnboardingContentTest` and `AppNavHostTest`.
  - `configureComposeMultiplatform()` stops adding `compose-ui-test` and the desktop runtime to `jvmTest`.
  - `AppViewModelTest` and `OnboardingViewModelTest` move to `androidHostTest`.
- **One command:** `./gradlew testAndroidHostTest` (Gradle runs the task in every module).
  - The root project registers its own `testAndroidHostTest` that runs the build-logic tests, like today's root
    `jvmTest`. It replaces the root `jvmTest` task.
  - The per-module `jvmTest` tasks still exist until 02, but they are empty.
- **CI:** the "JVM tests" step becomes "Android host tests" (`./gradlew testAndroidHostTest --continue`). Ubuntu
  runners ship the Android SDK.
- **The stale do-not in `.claude/rules/testing.md`** ("Don't create files under `src/androidHostTest/`") is
  replaced in this subtask with "Unit tests go in `src/androidHostTest/`", so the rules don't contradict the code.
  The rest of the docs is in 03.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Host test switch | `withHostTest {}` (removed in commit 6d0888b) | AGP KMP library DSL in `buildlogic/KotlinMultiplatform.kt` |
| Device test runner and tree | `withDeviceTestBuilder { sourceSetTreeName = "test" }` | `buildlogic/KotlinMultiplatform.kt` |
| Keystore cleanup in device tests | `deleteMasterKeyForTest` (Android `actual`) | `core/security/src/androidDeviceTest/.../MasterKeyTestSupport.android.kt` |
| Main dispatcher in ViewModel tests | `MainDispatcherTest` | `core:testing` |
| Repository double | `FakeSettingsRepository` | `service:settings:fake` |
| Coverage gate | `configureKover()` | `buildlogic/Kover.kt` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| build-logic | `buildlogic/KotlinMultiplatform.kt`, `buildlogic/Compose.kt` (modify) | Host test on; test deps move from `jvmTest` to `androidHostTest`; no Compose UI test deps |
| root | `build.gradle.kts` (modify) | Root `testAndroidHostTest` runs the build-logic tests |
| `core:common`, `core:database`, `core:model`, `core:network` | smoke tests (move `jvmTest` → `androidHostTest`) | Host tests |
| `core:security` | `MasterKeyCipherTest` (move `commonTest` → `androidDeviceTest`); `MasterKeyTestSupport.kt` and `.jvm.kt` (delete); `.android.kt` and `.ios.kt` (plain fun) | Keystore is device-only |
| `core:datastore` | `EncryptedDataStoreTest`, `EncryptedJsonSerializerTest` (move to `androidHostTest`, MockK cipher); `EncryptedDataStoreDeviceTest.kt` (new, `androidDeviceTest`); `build.gradle.kts` | Host + device split |
| `core:di` | `InitKoinTest` (move to `androidDeviceTest`); `build.gradle.kts` | Needs an Android `Context` |
| `core:ios` | `SharedCoreTest` (move to `iosTest`) | iOS entry point |
| `service:settings:data` | `SettingsRepositoryImplTest` (move to `androidHostTest`, MockK cipher); `build.gradle.kts` | Host test |
| `service:settings:fake` | `FakeSettingsRepositoryTest` (move to `androidHostTest`) | Host test |
| `cmp:composeApp` | `AppViewModelTest` (move); `AppNavHostTest` (delete); `build.gradle.kts` | VM on the host; UI test removed |
| `cmp:feature:onboarding:ui` | `OnboardingViewModelTest` (move); `OnboardingContentTest` (delete); `build.gradle.kts` | Same |
| CI | `.github/workflows/ci.yml` (modify) | Host tests instead of JVM tests |
| rules | `.claude/rules/testing.md` (one line) | Remove the contradicting do-not |

## Steps
### Step 1: Host test in the convention plugin  (AC-2, EC-3) ✅
- Files: `build-logic/.../buildlogic/KotlinMultiplatform.kt`, `build-logic/.../buildlogic/Compose.kt`,
  `build.gradle.kts` (modify).
- What:
  - **Android target:** add `withHostTestBuilder { sourceSetTreeName = "test" }.configure {}`, so `commonTest`
    also runs on the host.
  - **Test dependencies:** `kotlin-test` and `mockk` go on `androidHostTest`; the `jvmTest` block is removed.
  - **KDoc:** "Unit tests default to `androidHostTest` (`kotlin-test`, MockK). `commonTest` (`kotlin-test`, no
    MockK) also runs on the Android host, in Android device tests and on the iOS simulator."
  - **`Compose.kt`:** remove the `jvmTest` block (`compose-ui-test`, desktop runtime).
  - **Root build:** `tasks.register("testAndroidHostTest") { dependsOn(gradle.includedBuild("build-logic").task(":convention:test")) }`
    replaces the root `jvmTest` task.
  - **Module build files:** every `jvmTest.dependencies { … }` block becomes `androidHostTest.dependencies { … }`
    with the same entries (cmp composeApp and onboarding ui, `core:datastore`, `service:settings:data`). In
    `core:di`, `androidDeviceTest.dependencies { implementation(projects.service.settings.domain); implementation(libs.koin.android) }`
    replaces the `jvmTest` block. `core:datastore` adds
    `androidDeviceTest.dependencies { implementation(libs.kotlinx.coroutines.test) }`.
- Unit tests: none new (moved in step 2).
- Compile: `./gradlew :build-logic:convention:test` (the build-logic tests in `build-logic`) and
  `./gradlew help` (configuration).

### Step 2: Move the unit tests  (AC-2, AC-3, AC-4, EC-1) ✅
- Files: as in Changes overview.
- What:
  - **Pure moves (`git mv`)** from `src/jvmTest/kotlin/...` to `src/androidHostTest/kotlin/...`, same package:
    - smoke tests of `core:common`, `core:database`, `core:model` and `core:network`;
    - `FakeSettingsRepositoryTest`, `AppViewModelTest` and `OnboardingViewModelTest`.
  - **`core:security`:**
    - `git mv` `commonTest/MasterKeyCipherTest.kt` to `androidDeviceTest/`, and update its KDoc to "Runs on an
      Android device (Keystore)".
    - Delete `commonTest/MasterKeyTestSupport.kt` and `jvmTest/MasterKeyTestSupport.jvm.kt`.
    - Make `deleteMasterKeyForTest` in `.android.kt` and `.ios.kt` a plain `internal fun` (drop `actual`).
  - **`core:datastore`:**
    - Move both tests to `androidHostTest`.
    - The default cipher becomes `private fun passThroughCipher(): MasterKeyCipher = mockk { every { encrypt(any()) } answers { firstArg() }; every { decrypt(any()) } answers { firstArg() } }`.
    - `writeFailurePropagatesAndKeepsPreviousValue` keeps its failing mock, whose decrypt passes through.
    - `undecryptableBytesThrowCorruptionException` builds its serializer with a mock that throws
      `CipherException` on decrypt.
    - Remove `fileOnDiskDoesNotContainPlaintext`, `keyMismatchResetsToDefault` and
      `writtenBytesDoNotContainPlaintext` from the host classes.
    - New `androidDeviceTest/EncryptedDataStoreDeviceTest.kt` holds those three tests:
      - real `masterKeyCipher(alias)`, temp directory `FileSystem.SYSTEM_TEMPORARY_DIRECTORY`;
      - `@AfterTest` deletes the files, and deletes the keys with
        `KeyStore.getInstance("AndroidKeyStore").deleteEntry`.
  - **`core:di`:**
    - `git mv` `InitKoinTest` to `androidDeviceTest`.
    - It calls `initKoin { androidContext(InstrumentationRegistry.getInstrumentation().targetContext) }`.
  - **`core:ios`:** `git mv` `SharedCoreTest` to `iosTest`, unchanged.
  - **`service:settings:data`:**
    - Move `SettingsRepositoryImplTest` to `androidHostTest`.
    - The default cipher becomes the same pass-through MockK helper.
    - The other tests are unchanged: `corruptFileReadsNotCompleted` still fails JSON parsing with a pass-through
      cipher.
  - **cmp:** delete `AppNavHostTest.kt` and `OnboardingContentTest.kt`.
  - **Leftovers:** the empty `src/jvmTest` folders are removed.
- Unit tests: as moved; new `EncryptedDataStoreDeviceTest` with `fileOnDiskDoesNotContainPlaintext`,
  `keyMismatchResetsToDefault` and `writtenBytesDoNotContainPlaintext`.
- Compile:
  - `./gradlew compileAndroidHostTest` (the host test compile task per module, discovered in forge);
  - `:core:security:compileAndroidDeviceTestKotlinAndroid`, `:core:datastore:…` and `:core:di:…` (device test
    compile);
  - `:core:ios:compileTestKotlinIosSimulatorArm64` and `:core:security:compileTestKotlinIosSimulatorArm64`.

### Step 3: Coverage and CI  (AC-5, AC-6) ✅
- Files: `buildlogic/Kover.kt` (only if needed), `.github/workflows/ci.yml`, `.claude/rules/testing.md`
  (modify).
- What:
  - **Kover:** confirm that `koverVerify` on `:service:settings:data` and `:service:settings:domain` measures the
    Android host tests. Kover 0.9.11 takes the Android variant of the AGP KMP library. If the total report stays
    empty, add `currentProject { … }` variant selection for the Android host test in `configureKover()`.
  - **CI:** the "JVM tests" step becomes
    `name: Android host tests` / `run: ./gradlew testAndroidHostTest --continue`. The job name becomes
    `Checks (Android host)`.
  - **`testing.md`:** replace "Don't create files under `src/androidHostTest/` …" with "Unit tests go in
    `src/androidHostTest/` (MockK available); tests that need the real Keystore go in `src/androidDeviceTest/`."
- Compile: `./gradlew koverVerify`.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-2 Unit tests on the host | 1, 2 | `./gradlew testAndroidHostTest` runs all moved tests, plus the build-logic tests through the root task; `:architecture-test:test` |
| AC-3 Test side of encryption | 2 | Host: `EncryptedDataStoreTest`, `EncryptedJsonSerializerTest`, `SettingsRepositoryImplTest` with the MockK cipher. Device: `MasterKeyCipherTest`, `EncryptedDataStoreDeviceTest` (manual run on an emulator). iOS: `native-ios` `SharedCoreApiTests` (manual) |
| AC-4 No cmp Compose UI tests | 1, 2 | Search: no `runComposeUiTest` in `cmp/`; `AppViewModelTest` and `OnboardingViewModelTest` run on the host |
| AC-5 Coverage gate | 3 | `./gradlew koverVerify` passes; a temporary drop below 80% fails (manual check in forge) |
| AC-6 CI (partial) | 3 | The PR's CI run: four steps pass on Ubuntu |
| EC-1 Keystore only in device tests | 2 | `MasterKeyCipherTest`, `EncryptedDataStoreDeviceTest` and `InitKoinTest` in `androidDeviceTest`; no host test calls `masterKeyCipher(…)` (search) |
| EC-3 `commonTest` runs on the host and iOS | 1 | Host test tree `test` includes `commonTest` (configuration check in forge); no `commonTest` files remain after step 2 |

## Out of scope
- Removing the JVM target, `jvmMain` code, and the empty per-module `jvmTest` tasks: 02.
- Docs (architecture, `CLAUDE.md`, the other rules): 03.

## Risks
- **Android host tests stub `android.jar`.** A test that reaches an Android API (for example `Log`, `Looper`)
  throws "Method … not mocked". Mitigation: the moved tests use none. If one does, set
  `isReturnDefaultValues = true` on the host test and record it as a deviation.
- **Kover and the AGP KMP library plugin:** the total report may not include Android host tests in Kover 0.9.11.
  Mitigation: step 3 checks `koverVerify` with a deliberate coverage drop; fall back to explicit variant
  selection, or stop and ask if neither works.
- **CI SDK platform:** the Ubuntu image may lack `platforms;android-37`. Mitigation: AGP downloads missing
  platforms when licenses are accepted (they are on GitHub images). If the PR run fails, add an `sdkmanager` step.
- **Device tests are not on CI:** the Keystore paths are verified only by running the device tests locally on an
  emulator. This is stated in the report.

## Deviations
- The `compose-ui-test` entry was removed from `gradle/libs.versions.toml`, because nothing uses it any more.
  The matching phrase in `.claude/rules/build-logic.md` was removed too.
- `config/detekt/detekt.yml` sets the `MagicNumber` excludes. Detekt's defaults cover `jvmTest` but not
  `androidHostTest` or `androidDeviceTest`. The new list is `test`, `androidHostTest`, `androidDeviceTest`,
  `commonTest`, `iosTest` and `*.kts`.
- `Kover.kt` is unchanged: `koverVerify` already measures the Android host tests.
- Steps 1 and 2 were compiled together, because the Compose UI tests could not compile once their `jvmTest`
  dependencies were removed in step 1.

## Results
- `./gradlew compileAndroidHostTest`, the device-test compiles (`:core:security`, `:core:datastore`, `:core:di`)
  and the iOS test compiles (`:core:ios`, `:core:security`): pass.
- `./gradlew testAndroidHostTest --continue`: 10 classes and 34 tests pass. The root task also ran the
  build-logic tests.
- The coverage gate works on host tests. Without `SettingsRepositoryImplTest`, `:service:settings:data:koverVerify`
  fails with 0% against a minimum of 80%. With the test back, it passes at 100%.
- `./gradlew detekt testAndroidHostTest koverVerify :architecture-test:test --continue`: pass.
- `./gradlew assemble`: pass.
- Android device tests on the Pixel_8 emulator (Android 16), with
  `./gradlew :core:security:connectedAndroidDeviceTest :core:datastore:connectedAndroidDeviceTest :core:di:connectedAndroidDeviceTest`:
  - `MasterKeyCipherTest`: 7 tests pass.
  - `EncryptedDataStoreDeviceTest`: 3 tests pass.
  - `InitKoinTest`: 1 test passes.
- `./gradlew :core:ios:iosSimulatorArm64Test`: `SharedCoreTest`, 3 tests pass.
- Not run:
  - The `native-ios` tests: this subtask does not change them.
  - CI on GitHub: it runs on the PR.
