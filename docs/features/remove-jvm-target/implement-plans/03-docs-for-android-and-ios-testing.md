# Plan: Docs for Android and iOS testing

Requirement: [03 - Docs for Android and iOS testing](../subtasks/03-docs-for-android-and-ios-testing.md)
Status: Done

## Summary
Docs only. `CLAUDE.md`, `docs/architecture.md`, `.claude/rules/testing.md` and `.claude/rules/build-logic.md`
still describe a JVM target, `jvmTest` as the default, a JVM fake key and `./gradlew jvmTest`. This plan
rewrites those passages for the setup that 01 and 02 built:
- the targets are Android and iOS only;
- unit tests are Android host tests, run with `testAndroidHostTest`, with MockK and a pass-through cipher;
- the real Keystore is tested by Android device tests;
- the Keychain is tested by the `native-ios` Swift tests;
- there are no Compose UI tests in cmp.

The JVM projects (`build-logic`, `architecture-test`) and `jvmToolchain` keep their wording.

## Decisions
- **Commands:**
  - One module's unit tests: `./gradlew :<module>:testAndroidHostTest`.
  - All unit tests and the build-logic tests: `./gradlew testAndroidHostTest`.
  - CI: `./gradlew detekt testAndroidHostTest koverVerify :architecture-test:test`.
  - Compile check after a module build change: `./gradlew compileAndroidHostTest`.
- **Where tests go:**
  - Default: `src/androidHostTest/` (MockK available).
  - Real Keystore: `src/androidDeviceTest/`.
  - Kotlin iOS code without the Keychain: `src/iosTest/`.
  - `src/commonTest/`: multiplatform-only tests, which run on the Android host, on a device and on the iOS
    simulator.
- **Shared platform behavior:** the `testing.md` rule "Behavior shared by every platform's `actual` goes in one
  `commonTest` class" is replaced. Platform behavior is tested on each platform:
  - Android `actual`s in `androidDeviceTest`;
  - iOS `actual`s in `iosTest`, or in the `native-ios` Swift tests when they need the Keychain.
  This matches what 01 did with `MasterKeyCipherTest`.
- **The Compose UI test row** in architecture section 8 is replaced by "UI checked manually", as in the native
  apps.
- **Docs with no JVM-target content are left unchanged:** the "Fast feedback" goal is reworded ("on the
  developer's machine, without an emulator or Xcode"), and `architecture-test`'s "JVM only" stays (it is a JVM
  project).
- **Historical docs in `docs/features/`** (older tasks and plans) stay as they are; they record what was done at
  the time.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Real commands and test locations | Results of plans 01 and 02 | `docs/features/remove-jvm-target/implement-plans/0{1,2}-*.md` |
| Device test example | `MasterKeyCipherTest`, `EncryptedDataStoreDeviceTest` | `core/security/src/androidDeviceTest/`, `core/datastore/src/androidDeviceTest/` |
| Host test reference | `CommonSmokeTest` | `core/common/src/androidHostTest/kotlin/com/banhmilabs/tmdb/core/common/CommonSmokeTest.kt` |

## Changes overview
| Area | Files | Why |
|---|---|---|
| Project guide | `CLAUDE.md` | Setup, commands, "Before finishing", modules, conventions, do-nots |
| Architecture | `docs/architecture.md` | Goals, targets (4), module tree, sample layout, testing (8), security (10), presentation tree, tech stack, CI (13), publishing (14) |
| Rules | `.claude/rules/testing.md`, `.claude/rules/build-logic.md` | Test locations, commands, platform tests |

## Steps
### Step 1: `CLAUDE.md`  (AC-7) ✅
- **Repository layout:** "CI runs JVM checks only" becomes "CI runs on Ubuntu (static analysis, Android host
  tests, coverage, architecture tests); app builds and platform tests run locally".
- **Setup:** "lets JVM work proceed without them" becomes "lets Android work proceed without them".
- **Commands:**
  - All CI checks: `./gradlew detekt testAndroidHostTest koverVerify :architecture-test:test`.
  - One module's tests: `./gradlew :<module>:testAndroidHostTest` (the root `testAndroidHostTest` also runs the
    `build-logic` unit tests).
  - Compile every target (Android, iOS; macOS + Xcode).
- **Before finishing:**
  - The default tier uses `:<module>:testAndroidHostTest`.
  - The full tier uses the CI command.
  - Platform tests stay as they are.
- **Modules:**
  - The intro now says targets `android` (with host and device tests), `iosArm64`, `iosSimulatorArm64`.
  - `core:security` becomes "Master key (Keystore / Keychain), AES-GCM".
- **Conventions, unit tests:**
  - Unit tests go in `androidHostTest` by default (MockK available).
  - Tests that need the real Keystore go in `androidDeviceTest`.
  - `commonTest` is allowed for multiplatform-only tests (no MockK); they run on the Android host, a device and
    the iOS simulator.
  - The tools stay as they are; the in-memory `JdbcSqliteDriver` works on the Android host (JVM).
- **Conventions, encryption in host tests:** host tests never call `masterKeyCipher(…)`. They pass a MockK
  pass-through cipher.
- **Conventions, platform tests:** platform `actual` code is tested on its platform. The Keychain path is covered
  by the `native-ios` Swift tests, because Kotlin iOS tests have no app bundle.
- **Do not:**
  - "Don't use MockK or JVM-only APIs in `commonTest`; such tests go in `androidHostTest`."
  - The instrumented-test line stays.
  - Add: "Don't call the real Keystore or Keychain from a host test; use a MockK cipher, or write a device test."

### Step 2: `docs/architecture.md`  (AC-7) ✅
- **§1 Goals:** "Fast feedback. Almost every test runs on the developer's machine (Android host tests), without
  an emulator or Xcode."
- **§4 Targets:**
  - "All shared modules target `androidTarget`, `iosArm64` and `iosSimulatorArm64`."
  - Replace the `jvm` paragraph: Android host tests give fast feedback, and there is no JVM or desktop target.
- **§5.1 tree:** `security/` becomes "Master key: Android Keystore / iOS Keychain; AES-GCM". The sample layout
  uses `androidHostTest/` instead of `jvmTest/` (two lines).
- **§8 Testing strategy:**
  - **Table:**
    - Unit, network, database, fakes and shared ViewModels run in `androidHostTest` (default).
    - Pure multiplatform logic goes in `commonTest` (allowed): Android host, device, iOS simulator.
    - Platform code: Android device tests; iOS `kotlin.test` without the Keychain; the Keychain through the
      `native-ios` tests.
    - The Compose UI row becomes "UI checked manually on device and simulator".
  - **Rules:**
    - `androidHostTest` is the default; MockK is allowed only there.
    - Host tests use a pass-through MockK cipher.
    - Real Keystore tests are device tests.
- **§10 Security:** the encrypted settings bullet drops "and on JVM (tests) an in-memory key". Tests use a MockK
  cipher on the host and the real Keystore on a device.
- **§11 `cmp/` tree:** remove `desktopApp/ # optional`.
- **§12 tech stack:** Settings becomes "`cryptography-kotlin` on iOS"; Testing adds "Android host tests".
- **§13 CI:**
  - "on Ubuntu, for the KMP build: static analysis, Android host tests, coverage and architecture tests".
  - The command becomes `./gradlew detekt testAndroidHostTest koverVerify :architecture-test:test`.
- **§14 Publishing:** the publications are Android (AAR) and the iOS klibs. The suffix examples become
  `-android`, `-iosarm64` and `-iossimulatorarm64` (no `-jvm`).

### Step 3: Rules  (AC-7) ✅
- **`.claude/rules/testing.md`:**
  - **Responsibility:** unit tests live in `src/androidHostTest/kotlin/`. The strategy line becomes
    "`androidHostTest` by default, `commonTest` allowed, service fakes, MockK, platform code on its platform".
  - **Dependencies:** "via `androidHostTest` dependencies". The reference path changes to
    `core/common/src/androidHostTest/...`. `core:testing` is added to `androidHostTest`.
  - **Steps:**
    - `src/androidHostTest/...` for new tests; fixture JSON goes in `src/androidHostTest/resources`.
    - The run command is `./gradlew :<module>:testAndroidHostTest`.
  - **Platform tests:**
    - Configured with `withHostTest` and `withDeviceTest` in the `test` tree, so `commonTest` runs in both and on
      the iOS simulator.
    - Platform behavior goes in `src/androidDeviceTest/` (real Keystore) and `src/iosTest/`.
    - Test helpers that need platform APIs are plain `internal fun`s in those source sets.
    - The reference is `core/security/src/androidDeviceTest/.../MasterKeyCipherTest.kt`.
    - The Keychain limitation note stays as it is.
  - **Do / Don't:**
    - "Prefer `src/androidHostTest/`" replaces "Prefer `src/jvmTest/`".
    - The `commonTest` sentence ends "they also run on the Android host".
    - Add: host tests use a MockK pass-through cipher, never `masterKeyCipher(…)`.
- **`.claude/rules/build-logic.md`:**
  - Line 63 (unit test deps): `androidHostTest` has `kotlin-test` and MockK.
  - Line 64: the root `testAndroidHostTest` depends on `build-logic`'s `:convention:test`.
  - Line 67:
    - `./gradlew testAndroidHostTest` after any build-logic change;
    - `./gradlew compileAndroidHostTest` after module build changes.
  - Add a line where the KMP target setup is described: the targets are Android (host and device tests) and
    iOS, with no JVM target.

### Step 4: Check  (AC-7) ✅
- Run `grep -n -i "jvmTest\|jvm()\|JVM fake\|-jvm\|desktop\|runComposeUiTest" CLAUDE.md docs/architecture.md .claude/rules/*.md`.
  It returns nothing, apart from the JVM projects (`architecture-test`, `tmdb.detekt` for JVM modules,
  `jvmToolchain`).
- Docs only: run nothing (CLAUDE.md "Before finishing").

## Traceability
| AC / EC | Step(s) | Check |
|---|---|---|
| AC-7 Docs describe the new setup | 1, 2, 3, 4 | Step 4 search; the documented commands match the ones run in plans 01 and 02 (`testAndroidHostTest`, `compileAndroidHostTest`, `connectedAndroidDeviceTest`, `iosSimulatorArm64Test`) |

## Out of scope
- Historical feature docs in `docs/features/`.
- Any code or build change.

## Risks
- **A doc keeps an old command** that no longer exists (for example `jvmTest`). Mitigation: the step 4 search,
  and every command in the docs was run in plan 01 or 02.

## Deviations
- `CLAUDE.md` has an extra convention line. Host tests never call `masterKeyCipher(…)`; they pass a MockK cipher
  that returns its input. This states the step 1 encryption rule explicitly.
- The architecture §8 table has a new row: "Encrypted storage on the host".
- `testing.md` has a reference line for the host / device split of `core:datastore`.

## Results
- The step 4 search (`jvmTest`, `jvm()`, `JVM fake`, `-jvm`, `desktop`, `runComposeUiTest`,
  `compileTestKotlinJvm`) returns only the sentence in architecture §4 that states there is no JVM or desktop target.
- The other remaining "JVM" mentions refer to JVM projects or tools: `architecture-test`, `tmdb.detekt` for JVM
  modules, `jvmToolchain`, host tests that run on the JVM, and the native Android test row.
- Docs only: no build or test commands run (CLAUDE.md "Before finishing").
