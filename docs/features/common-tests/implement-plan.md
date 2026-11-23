# Plan: Move tier-1 tests to commonTest

Requirement: chat (rule in CLAUDE.md "Conventions": a test goes in `commonTest` when the code under test runs on iOS,
touches something Kotlin/Native can run differently, and the test needs no JVM-only API)
Status: Done

## Acceptance criteria
- **AC-1** Given the five tier-1 test classes (`EnvelopeDatabaseKeysTest`, `EncryptedDataStoreTest`,
  `EncryptedJsonSerializerTest`, `SettingsRepositoryImplTest`, `TitleDetailsFormatTest`), when the change is done,
  then each lives in its module's `src/commonTest/kotlin/` in the same package, and no copy is left in
  `src/androidHostTest/`.
- **AC-2** Given the moved tests, when `./gradlew testAndroidHostTest koverVerify` runs (CI), then they run on the
  Android host and pass, and `service:settings:data` still meets 80% line coverage.
- **AC-3** Given the moved tests, when each module's `iosSimulatorArm64Test` runs filtered to the moved classes,
  then they pass on the iOS simulator.
- **AC-4** Given the moved tests, when each module's `connectedAndroidDeviceTest` runs, then they pass on a device or
  emulator.
- **AC-5** Given `commonTest` of any module, when it needs `runTest` or `TestScope`, then `kotlinx-coroutines-test`
  is available without a per-module dependency.
- **EC-1** The temporary files the moved tests write (okio `FileSystem.SYSTEM_TEMPORARY_DIRECTORY`) work on the
  simulator and on a device and are deleted in `@AfterTest`, as on the host.
- **EC-2** `core:security`'s iOS test run also contains the Keychain tests that fail without an app host (known
  limitation in `.claude/rules/testing.md`); verification filters to the moved class and doesn't touch those tests.

## Summary
Move the five tier-1 test classes from `androidHostTest` to `commonTest` without changing their content: they already
use only multiplatform APIs (okio, DataStore, kotlinx-serialization, kotlinx-datetime, `kotlin.test`, private stubs).
The convention plugin gets `kotlinx-coroutines-test` on `commonTest`, as `.claude/rules/testing.md` says to add
multiplatform test libraries there. Dependencies a moved test needs (the settings fake) move from `androidHostTest`
to `commonTest`. CI keeps running them on the host; iOS and device runs are local checks.

## Decisions
- Scope is tier 1 only (26 tests, 5 classes) — user's choice; tier 2 (network, `ObserveFavoriteTitlesTest`, cmp
  ViewModels) is left out because it slows device tests for a moderate gain.
- Commit directly on `remove-mockk` (PR #42) — user's choice; the moved tests use the private stubs introduced there.
  (Rejected: a new branch after #42 merges.)
- `kotlinx-coroutines-test` goes on `commonTest` in `configureKotlinMultiplatform()` for every module, not per
  module — `.claude/rules/testing.md` puts multiplatform test libraries there. Redundant per-module
  `kotlinx-coroutines-test` lines are removed only in the modules this plan touches.
- Test content is not changed; only the folder (via `git mv`) — the classes compile for iOS as they are, which also
  proves AC-5's "no JVM-only API".
- iOS verification runs `iosSimulatorArm64Test --tests '<package>.<Class>'` per module (EC-2). Unfiltered runs on
  `core:security` keep failing on the Keychain tests as today.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Pass-through and failing cipher stubs | `PassThroughCipher`, `ReversingCipher` (private in the tests) | the moved test classes |
| Settings repository contract | `SettingsRepositoryContract` | `service/settings/fake/src/commonMain/.../SettingsRepositoryContract.kt` |
| Shared test dependencies | `configureKotlinMultiplatform()` | `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/KotlinMultiplatform.kt` |
| Existing `commonTest` example | `LanguageTest` | `core/model/src/commonTest/kotlin/com/banhmilabs/tmdb/core/model/LanguageTest.kt` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `build-logic/convention` | `buildlogic/KotlinMultiplatform.kt` (modify) | `kotlinx-coroutines-test` on `commonTest` |
| `core:security` | `EnvelopeDatabaseKeysTest.kt` (move) | Tier 1: key files through okio on iOS |
| `core:datastore` | `EncryptedDataStoreTest.kt`, `EncryptedJsonSerializerTest.kt` (move); `build.gradle.kts` (modify) | Tier 1: encrypted DataStore on iOS; drop redundant coroutines-test lines |
| `service:settings:data` | `SettingsRepositoryImplTest.kt` (move); `build.gradle.kts` (modify) | Tier 1: settings repository on iOS; fake dependency to `commonTest` |
| `cmp:feature:title-details:ui` | `TitleDetailsFormatTest.kt` (move) | Tier 1: date and runtime formatting on iOS |
| docs | `.claude/rules/testing.md`, `.claude/rules/build-logic.md` (modify) | `commonTest` now has `kotlinx-coroutines-test`; reference to a real `commonTest` example |

Affected by step 1: every shared module's `commonTest` classpath (one more test library; no source change).

## Steps

### Step 1: `kotlinx-coroutines-test` on `commonTest`  (AC-5) ✅
- Files: `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/KotlinMultiplatform.kt` (modify)
- What: in the `commonTest` dependencies block add
  `implementation(libs.findLibrary("kotlinx-coroutines-test").get())` after `kotlin-test`.
- Unit tests: none (build config); covered by steps 2–5 compiling.
- Compile: `./gradlew :core:model:compileTestKotlinIosSimulatorArm64 :core:model:compileAndroidHostTest`

### Step 2: Move `EnvelopeDatabaseKeysTest`  (AC-1, EC-1, EC-2) ✅
- Files: `core/security/src/androidHostTest/kotlin/com/banhmilabs/tmdb/core/security/EnvelopeDatabaseKeysTest.kt`
  → `core/security/src/commonTest/kotlin/com/banhmilabs/tmdb/core/security/EnvelopeDatabaseKeysTest.kt` (`git mv`)
- What: no content change. `EnvelopeDatabaseKeys` is `internal` in `commonMain`, visible to `commonTest` of the same
  module. `core/security/build.gradle.kts` needs nothing (okio is a `commonMain` dependency).
- Unit tests: the 5 existing cases.
- Compile: `./gradlew :core:security:compileAndroidHostTest :core:security:compileAndroidDeviceTest :core:security:compileTestKotlinIosSimulatorArm64`

### Step 3: Move the `core:datastore` tests  (AC-1, EC-1) ✅
- Files: `EncryptedDataStoreTest.kt`, `EncryptedJsonSerializerTest.kt` from
  `core/datastore/src/androidHostTest/kotlin/com/banhmilabs/tmdb/core/datastore/` to
  `core/datastore/src/commonTest/kotlin/com/banhmilabs/tmdb/core/datastore/` (`git mv`);
  `core/datastore/build.gradle.kts` (modify)
- What: no test content change. In `build.gradle.kts` remove the `androidHostTest.dependencies` and
  `androidDeviceTest.dependencies` blocks (each only held `kotlinx-coroutines-test`, now from step 1).
  `EncryptedDataStoreDeviceTest` stays in `androidDeviceTest`.
- Unit tests: the 8 existing cases.
- Compile: `./gradlew :core:datastore:compileAndroidHostTest :core:datastore:compileAndroidDeviceTest :core:datastore:compileTestKotlinIosSimulatorArm64`

### Step 4: Move `SettingsRepositoryImplTest`  (AC-1, AC-2, EC-1) ✅
- Files: `service/settings/data/src/androidHostTest/kotlin/com/banhmilabs/tmdb/service/settings/data/SettingsRepositoryImplTest.kt`
  → `service/settings/data/src/commonTest/kotlin/com/banhmilabs/tmdb/service/settings/data/SettingsRepositoryImplTest.kt`
  (`git mv`); `service/settings/data/build.gradle.kts` (modify)
- What: replace the `androidHostTest.dependencies` block with
  ```kotlin
  commonTest.dependencies {
      implementation(projects.service.settings.fake)
  }
  ```
  (`kotlinx-coroutines-test` comes from step 1; `turbine` is dropped: no test in this module imports it, and the
  contract brings its own as the fake's dependency.)
- Unit tests: the 4 own cases plus the inherited `SettingsRepositoryContract` cases.
- Compile: `./gradlew :service:settings:data:compileAndroidHostTest :service:settings:data:compileAndroidDeviceTest :service:settings:data:compileTestKotlinIosSimulatorArm64`

### Step 5: Move `TitleDetailsFormatTest`  (AC-1) ✅
- Files: `cmp/feature/title-details/ui/src/androidHostTest/kotlin/com/banhmilabs/tmdb/cmp/feature/titledetails/ui/TitleDetailsFormatTest.kt`
  → `cmp/feature/title-details/ui/src/commonTest/kotlin/com/banhmilabs/tmdb/cmp/feature/titledetails/ui/TitleDetailsFormatTest.kt`
  (`git mv`)
- What: no content change (`kotlin.test`, `kotlinx.datetime.LocalDate` and `Language` are on the main classpath).
  The other title-details tests stay in `androidHostTest`.
- Unit tests: the 9 existing cases.
- Compile: `./gradlew :cmp:feature:title-details:ui:compileAndroidHostTest :cmp:feature:title-details:ui:compileTestKotlinIosSimulatorArm64`

### Step 6: Docs  (AC-5) ✅
- Files: `.claude/rules/testing.md`, `.claude/rules/build-logic.md` (modify)
- What:
  - `testing.md`: "`commonTest` has `kotlin-test`" → "`commonTest` has `kotlin-test` and `kotlinx-coroutines-test`";
    add a reference line: `core/datastore/src/commonTest/.../EncryptedDataStoreTest.kt` as the `commonTest`
    example; the `MasterKeyCipher` reference path changes from `androidHostTest` to `commonTest`.
  - `build-logic.md`: the unit test deps sentence lists `kotlinx-coroutines-test` on `commonTest`.
- Compile: none (docs).

### Verification (after step 6)
- CI set: `./gradlew detekt testAndroidHostTest koverVerify :architecture-test:test --continue`
- iOS (macOS + Xcode), filtered per EC-2:
  - `./gradlew :core:security:iosSimulatorArm64Test --tests 'com.banhmilabs.tmdb.core.security.EnvelopeDatabaseKeysTest'`
  - `./gradlew :core:datastore:iosSimulatorArm64Test`
  - `./gradlew :service:settings:data:iosSimulatorArm64Test`
  - `./gradlew :cmp:feature:title-details:ui:iosSimulatorArm64Test`
- Device (emulator running): `./gradlew :core:security:connectedAndroidDeviceTest :core:datastore:connectedAndroidDeviceTest :service:settings:data:connectedAndroidDeviceTest`

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 | 2, 3, 4, 5 | Manual: `find . -path '*androidHostTest*' -name '<Class>.kt'` returns nothing for the five classes |
| AC-2 | 4, verification | CI set incl. `koverVerify` |
| AC-3 | 2–5, verification | The four `iosSimulatorArm64Test` runs |
| AC-4 | 2–4, verification | `connectedAndroidDeviceTest` on the three modules (`cmp` has no device test wiring needed: format test is pure) |
| AC-5 | 1, 6 | Steps 2–4 compile `runTest`/`TestScope` in `commonTest` with no per-module coroutines-test |
| EC-1 | 2–4 | `EnvelopeDatabaseKeysTest`, `EncryptedDataStoreTest`, `SettingsRepositoryImplTest` on simulator and device (`@AfterTest deleteFiles`) |
| EC-2 | verification | Filtered `core:security` iOS run |

## Out of scope
- Tier 2 tests (network, `ObserveFavoriteTitlesTest`, cmp ViewModels) and all SQLDelight `data` tests.
- A SQLDelight contract test in `iosTest` for the native driver + SQLCipher (separate follow-up).
- Removing `kotlinx-coroutines-test` lines from modules this plan doesn't touch.
- Making CI run iOS or device tests.

## Risks
- `cmp:feature:title-details:ui` has never linked an iOS test binary (Compose Multiplatform) — if linking fails,
  fall back to keeping `TitleDetailsFormatTest` in `androidHostTest` and record it here.
- Android device temp directory (`java.io.tmpdir` → app cache) must be writable from the instrumentation process —
  expected; if not, `connectedAndroidDeviceTest` shows it and the test stays host-only.
- Device and iOS runs need an emulator and macOS + Xcode; if unavailable, `forge` reports them as skipped.
- `core:security` unfiltered iOS runs still fail (Keychain without app host) — unchanged, documented.

## Deviations
- None in code. Verification: the CI set and the four filtered `iosSimulatorArm64Test` runs passed (host and iOS:
  5 + 8 + 8 + 9 tests). `connectedAndroidDeviceTest` (AC-4, EC-1 on device) was not run: `forge` skips instrumented
  tests; run it locally with an emulator.
