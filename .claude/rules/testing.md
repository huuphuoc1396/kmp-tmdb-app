---
paths:
  - "core/*/src/*Test/**"
  - "core/testing/**"
  - "service/*/*/src/*Test/**"
  - "service/*/fake/**"
---

# Tests (shared core and services)

## Responsibility
- Unit tests for each module live in its `src/jvmTest/kotlin/` under the module's package (`core/<module>` → `com/banhmilabs/tmdb/core/<module>/`, `service/<ctx>/<type>` → `com/banhmilabs/tmdb/service/<ctx>/<type>/`).
- `core:testing` is a normal KMP library (`commonMain`) that other modules' tests consume. It holds test dispatchers only and may depend only on the kernel (`core:common`, `core:model`).
- Strategy (`jvmTest` by default, `commonTest` allowed for multiplatform-only tests, service fakes across modules, MockK inside a module, platform code tested on its platform) is in CLAUDE.md and `docs/architecture.md` section 8; this file covers how to write the tests.

## Pattern
- One test class per subject: `<Subject>Test`, same package as the subject.
- `kotlin.test` annotations and assertions (`@Test`, `assertEquals`, `assertTrue`). The runner is JUnit 4 through `kotlin-test`; don't import `org.junit.jupiter` (that is only for `architecture-test`).
- Test function names are lowerCamelCase describing the behavior (`mockkCreatesMocks`), not backtick sentences.
- MockK (collaborators in the same module, or to `verify` calls): `mockk<T>()`, `every { ... } returns ...`, `verify { ... }`. Test-only types go in a `private` nested declaration inside the test class.
- Dependencies from another module (a ViewModel or use case using a repository): use `Fake<Repository>` from `service/<ctx>/fake` via `jvmTest` dependencies, with the real use cases on top.
- Reference: `core/common/src/jvmTest/kotlin/com/banhmilabs/tmdb/core/common/CommonSmokeTest.kt`.
- `kotlin-test` and MockK come from `tmdb.module`; don't add them per module. To use `core:testing`, add `implementation(projects.core.testing)` to the module's `jvmTest` dependencies.

## Adding tests to a module
1. Create `<Subject>Test.kt` under the module's `src/jvmTest/kotlin/...` in the subject's package.
2. Use service fakes for other modules' repositories and MockK for same-module collaborators; take dispatchers from `core:testing`. Fixture JSON for a `data` module goes in its own `src/jvmTest/resources/fixtures/`.
3. When the module gets its first real test, delete its `<Module>SmokeTest` placeholder.
4. Run `./gradlew :<module>:jvmTest`; for coverage-gated modules (`domain`, `data`) also `./gradlew :<module>:koverVerify` (80% line coverage).

## Repository fakes (`service/<ctx>/fake`)
- Create the `fake` module only when another module's tests need it.
- One `Fake<Repository>` per repository interface: in-memory state in `MutableStateFlow`, controllable from tests (for example `emit(...)`, `failNextRefresh(AppError.Network)`), sensible defaults.
- Contract test: an abstract `<Repository>Contract` class in the fake module's `commonMain` with `abstract fun createRepository(...)` and the behavior tests. `<Repository>ImplTest` in `data` (with Ktor `MockEngine` + in-memory SQLDelight) and `Fake<Repository>Test` in `fake` both extend it. A behavior change goes into the contract first.
- Fakes cover repositories only; don't fake use cases or data sources.

## Adding to core:testing
- Put shared fixtures and dispatchers in `core/testing/src/commonMain/kotlin/com/banhmilabs/tmdb/core/testing/` (package rule enforced by `PackageRulesTest`).
- Only add something here when two or more modules need it; one-off helpers stay private in the test class.

## Platform tests
- Configured in `configureKotlinMultiplatform()`: `commonTest` has `kotlin-test`; Android device tests use `sourceSetTreeName = "test"` (so `androidDeviceTest` also compiles `commonTest`) with the AndroidX test runner; `iosTest` inherits `commonTest`. No MockK in any of them.
- Behavior shared by every platform's `actual` goes in one `commonTest` class; it runs in `jvmTest`, `connectedAndroidDeviceTest` (real Keystore) and `iosSimulatorArm64Test` (real Keychain). Platform-only checks go in `src/androidDeviceTest/` or `src/iosTest/`.
- Test helpers that need platform APIs (for example deleting a stored key) are an `internal expect fun` in `commonTest` with `actual`s in `jvmTest`, `androidDeviceTest` and `iosTest`.
- Reference: `core/security/src/commonTest/kotlin/com/banhmilabs/tmdb/core/security/MasterKeyCipherTest.kt`.
- Known limitation: `iosSimulatorArm64Test` runs a standalone test binary with no app bundle, so the Keychain is unavailable (`errSecNotAvailable`, -25291) and Keychain-backed tests (`core:security`) fail there. They need an Xcode test host, which comes with `native-ios/` or an `iosApp` test target; until then they are verified on JVM and on an Android device only. Don't skip or weaken them on iOS.

## Do / Don't
- Prefer `src/jvmTest/`. Use `src/commonTest/` only for tests that need no MockK or JVM-only API and are worth running on Kotlin/Native (concurrency, `Flow` timing, formatting, parsing); they also run in `jvmTest`. `commonTest` has `kotlin-test`; add Turbine / Kotest assertions to it in `configureKotlinMultiplatform()` when a test needs them. MockK stays on `jvmTest` only.
- Don't create files under `src/androidHostTest/` (the Android host test was removed).
- Don't write ad-hoc `Fake*` classes in test code; fakes live only in `service/<ctx>/fake` with a contract test. Otherwise use MockK or a real in-memory engine (SQLDelight in-memory driver, Ktor `MockEngine`).
- Don't weaken or delete a failing test to make a check pass; fix the cause.
