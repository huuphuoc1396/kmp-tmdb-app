---
paths:
  - "core/*/src/*Test/**"
  - "core/testing/**"
  - "service/*/*/src/*Test/**"
  - "service/*/fake/**"
---

# Tests (shared core and services)

## Responsibility
- Unit tests for each module live in its `src/androidHostTest/kotlin/` (Android host tests) under the module's package (`core/<module>` → `com/banhmilabs/tmdb/core/<module>/`, `service/<ctx>/<type>` → `com/banhmilabs/tmdb/service/<ctx>/<type>/`).
- `core:testing` is a normal KMP library (`commonMain`) that other modules' tests consume. It holds test dispatchers (`MainDispatcherTest`, `TestDispatcherProvider`) only and may depend only on the kernel (`core:common`, `core:model`).
- Strategy (`androidHostTest` by default, `commonTest` allowed for multiplatform-only tests, service fakes across modules, no mocking library, platform code tested on its platform) is in CLAUDE.md and `docs/architecture.md` section 8; this file covers how to write the tests.

## Pattern
- One test class per subject: `<Subject>Test`, same package as the subject.
- `kotlin.test` annotations and assertions (`@Test`, `assertEquals`, `assertTrue`). The runner is JUnit 4 through `kotlin-test`; don't import `org.junit.jupiter` (that is only for `architecture-test`).
- Test function names are lowerCamelCase describing the behavior (`missingFileReadsDefault`), not backtick sentences.
- No mocking library. Use the real collaborator where it is cheap; when it is platform-bound or the test must inject a failure, write a small stub as a `private` nested class in the test (`PassThroughCipher(encryptFailure = …)`, `UnreadableDataStore`). A one-method interface can be an `object : Clock { … }`. Check commands through state or recorded calls (`writes`, `refreshRequests`), not by verifying calls.
- Dependencies from another module (a ViewModel or use case using a repository): use `Fake<Repository>` from `service/<ctx>/fake` via `androidHostTest` dependencies, with the real use cases on top.
- Reference: `core/common/src/androidHostTest/kotlin/com/banhmilabs/tmdb/core/common/CommonSmokeTest.kt`.
- Code that needs a `MasterKeyCipher` (encrypted DataStore, repositories) gets a `private class PassThroughCipher : MasterKeyCipher` that returns its input (optionally throwing a given `CipherException`); host tests never call `masterKeyCipher(…)`. Checks that need real encryption (no plaintext on disk, wrong key) are device tests. Reference: `core/datastore/src/androidHostTest/.../EncryptedDataStoreTest.kt` and `src/androidDeviceTest/.../EncryptedDataStoreDeviceTest.kt`.
- `kotlin-test` comes from `tmdb.module`; don't add it per module. To use `core:testing`, add `implementation(projects.core.testing)` to the module's `androidHostTest` dependencies.

## Adding tests to a module
1. Create `<Subject>Test.kt` under the module's `src/androidHostTest/kotlin/...` in the subject's package.
2. Use service fakes for other modules' repositories and real collaborators or private stubs inside the module; take dispatchers from `core:testing` (`TestDispatcherProvider`). Fixture JSON for a `data` module goes in its own `src/androidHostTest/resources/fixtures/`.
3. When the module gets its first real test, delete its `<Module>SmokeTest` placeholder.
4. Run `./gradlew :<module>:testAndroidHostTest`; for coverage-gated modules (`domain`, `data`) also `./gradlew :<module>:koverVerify` (80% line coverage).

## Repository fakes (`service/<ctx>/fake`)
- Create the `fake` module only when another module's tests need it.
- One `Fake<Repository>` per repository interface: in-memory state in `MutableStateFlow`, controllable from tests (for example `emit(...)`, `failNextRefresh(AppError.Network)`), sensible defaults.
- Contract test: an abstract `<Repository>Contract` class in the fake module's `commonMain` with `abstract fun createRepository(...)` and the behavior tests. `<Repository>ImplTest` in `data` (with Ktor `MockEngine` + in-memory SQLDelight) and `Fake<Repository>Test` in `fake` both extend it. A behavior change goes into the contract first.
- Fakes cover repositories only; don't fake use cases or data sources.

## Adding to core:testing
- Put shared fixtures and dispatchers in `core/testing/src/commonMain/kotlin/com/banhmilabs/tmdb/core/testing/` (package rule enforced by `PackageRulesTest`).
- Only add something here when two or more modules need it; one-off helpers stay private in the test class.

## Platform tests
- Configured in `configureKotlinMultiplatform()`: the targets are Android and iOS only (no JVM). The Android host test and device test both use `sourceSetTreeName = "test"`, so `androidHostTest` and `androidDeviceTest` also compile `commonTest`; device tests use the AndroidX test runner; `iosTest` inherits `commonTest`. `commonTest` has `kotlin-test`.
- Platform behavior is tested on each platform: Android `actual`s in `src/androidDeviceTest/` (`./gradlew :<module>:connectedAndroidDeviceTest`, real Keystore), iOS `actual`s in `src/iosTest/` (`./gradlew :<module>:iosSimulatorArm64Test`). No test doubles in either.
- Test helpers that need platform APIs (for example deleting a stored key) are plain `internal fun`s in the source set that uses them.
- Reference: `core/security/src/androidDeviceTest/kotlin/com/banhmilabs/tmdb/core/security/MasterKeyCipherTest.kt`.
- Known limitation: `iosSimulatorArm64Test` runs a standalone test binary with no app bundle, so the Keychain is unavailable (`errSecNotAvailable`, -25291) and Keychain-backed tests (`core:security`) fail there. They would need an Xcode test host, so they are verified on an Android device only. The `native-ios/` tests run in an app test host and exercise the Keychain-backed settings path end to end through the published framework, but they don't run these Kotlin tests. Don't skip or weaken them on iOS.

## Do / Don't
- Prefer `src/androidHostTest/`. Use `src/commonTest/` only for tests that need no JVM-only API and are worth running on Kotlin/Native (concurrency, `Flow` timing, formatting, parsing); they also run on the Android host. `commonTest` has `kotlin-test`; add Turbine / Kotest assertions to it in `configureKotlinMultiplatform()` when a test needs them.
- Tests that need the real Keystore go in `src/androidDeviceTest/`; don't call `masterKeyCipher(…)` or other Keystore / Keychain code from a host test.
- Don't write ad-hoc `Fake*` classes in test code; fakes live only in `service/<ctx>/fake` with a contract test. Otherwise use the real collaborator, a real in-memory engine (SQLDelight in-memory driver, Ktor `MockEngine`) or a private stub. Don't add a mocking library.
- Don't weaken or delete a failing test to make a check pass; fix the cause.
