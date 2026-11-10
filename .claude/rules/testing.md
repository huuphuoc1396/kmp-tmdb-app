---
paths:
  - "core/*/src/*Test/**"
  - "core/testing/**"
---

# Tests (shared core)

## Responsibility
- Unit tests for each `core/<module>` live in `core/<module>/src/jvmTest/kotlin/com/banhmilabs/tmdb/core/<module>/`.
- `core:testing` is a normal KMP library (`commonMain`) that other modules' tests consume. It holds fixture JSON and test dispatchers only; it depends on `core:domain` as `api`.
- Strategy (jvmTest only, MockK only, no fakes, platform code tested on its platform) is in CLAUDE.md and `docs/architecture.md` section 8; this file covers how to write the tests.

## Pattern
- One test class per subject: `<Subject>Test`, same package as the subject.
- `kotlin.test` annotations and assertions (`@Test`, `assertEquals`, `assertTrue`). The runner is JUnit 4 through `kotlin-test`; don't import `org.junit.jupiter` (that is only for `architecture-test`).
- Test function names are lowerCamelCase describing the behavior (`mockkCreatesMocks`), not backtick sentences.
- MockK: `mockk<T>()`, `every { ... } returns ...`, `verify { ... }`. Test-only types go in a `private` nested declaration inside the test class.
- Reference: `core/common/src/jvmTest/kotlin/com/banhmilabs/tmdb/core/common/CommonSmokeTest.kt`.
- `kotlin-test` and MockK come from `tmdb.kmp.library`; don't add them per module. To use `core:testing`, add `implementation(projects.core.testing)` to the module's `jvmTest` dependencies.

## Adding tests to a module
1. Create `<Subject>Test.kt` under the module's `src/jvmTest/kotlin/...` in the subject's package.
2. Mock collaborators with MockK; load fixtures and dispatchers from `core:testing`.
3. When the module gets its first real test, delete its `<Module>SmokeTest` placeholder.
4. Run `./gradlew :core:<module>:jvmTest`; for `core:domain` / `core:data` also `./gradlew :core:<module>:koverVerify` (80% line coverage).

## Adding to core:testing
- Put shared fixtures and dispatchers in `core/testing/src/commonMain/kotlin/com/banhmilabs/tmdb/core/testing/` (package rule enforced by `PackageRulesTest`).
- Only add something here when two or more modules need it; one-off helpers stay private in the test class.

## Platform tests (not configured yet)
- `iosTest` and Android device tests have no dependencies set up. The first task that adds `androidMain` / `iosMain` code configures them in `KmpLibraryConventionPlugin` (`kotlin-test` for `iosTest`; no MockK in either).

## Do / Don't
- Don't create files under `src/commonTest/` or `src/androidHostTest/` (the Android host test was removed).
- Don't write `Fake*` classes; use a MockK mock or a real in-memory implementation (SQLDelight in-memory driver, Ktor `MockEngine`).
- Don't weaken or delete a failing test to make a check pass; fix the cause.
