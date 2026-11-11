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
- `core:testing` is a normal KMP library (`commonMain`) that other modules' tests consume. Target: test dispatchers only, depending on kernel modules. Today it still depends on `core:domain` as `api` until the `service/*` migration.
- Strategy (jvmTest only, service fakes across modules, MockK inside a module, platform code tested on its platform) is in CLAUDE.md and `docs/architecture.md` section 8; this file covers how to write the tests.

## Pattern
- One test class per subject: `<Subject>Test`, same package as the subject.
- `kotlin.test` annotations and assertions (`@Test`, `assertEquals`, `assertTrue`). The runner is JUnit 4 through `kotlin-test`; don't import `org.junit.jupiter` (that is only for `architecture-test`).
- Test function names are lowerCamelCase describing the behavior (`mockkCreatesMocks`), not backtick sentences.
- MockK (collaborators in the same module, or to `verify` calls): `mockk<T>()`, `every { ... } returns ...`, `verify { ... }`. Test-only types go in a `private` nested declaration inside the test class.
- Dependencies from another module (a ViewModel or use case using a repository): use `Fake<Repository>` from `service/<ctx>/fake` via `jvmTest` dependencies, with the real use cases on top.
- Reference: `core/common/src/jvmTest/kotlin/com/banhmilabs/tmdb/core/common/CommonSmokeTest.kt`.
- `kotlin-test` and MockK come from `tmdb.kmp.library`; don't add them per module. To use `core:testing`, add `implementation(projects.core.testing)` to the module's `jvmTest` dependencies.

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

## Platform tests (not configured yet)
- `iosTest` and Android device tests have no dependencies set up. The first task that adds `androidMain` / `iosMain` code configures them in `KmpLibraryConventionPlugin` (`kotlin-test` for `iosTest`; no MockK in either).

## Do / Don't
- Don't create files under `src/commonTest/` or `src/androidHostTest/` (the Android host test was removed).
- Don't write ad-hoc `Fake*` classes in test code; fakes live only in `service/<ctx>/fake` with a contract test. Otherwise use MockK or a real in-memory engine (SQLDelight in-memory driver, Ktor `MockEngine`).
- Don't weaken or delete a failing test to make a check pass; fix the cause.
