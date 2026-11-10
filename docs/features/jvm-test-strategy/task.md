# JVM test strategy

Status: Confirmed
Source: chat + [docs/architecture.md](../../architecture.md)

## Goal
Unit tests for shared logic should be fast to run and easy to write. Today they are planned in `commonTest`, which runs on every target and rules out MockK because MockK does not support Kotlin/Native. This change moves unit tests to the JVM only, makes MockK the mocking library, and tests platform-specific code separately on its own platform. The audience is developers working on `main`, `cmp` and `native`; success means every unit test runs with one JVM command and the docs describe one clear way to write tests.

## Scope
### In scope
- The testing rules for the shared core on `main`, shared ViewModels on `cmp`, and Android tests on `native`.
- The role of the shared test module (fixtures and test dispatchers, no fakes).
- How platform-specific code (`actual` implementations) is tested.
- Updating `docs/architecture.md` and `CLAUDE.md` to describe the new strategy.

### Out of scope
- Making the change in code: moving existing smoke tests, adding MockK to the version catalog, changing convention plugins. Planned separately (`blueprint`, then `forge`).
- CI jobs for platform tests (emulator, macOS simulator).
- iOS XCTest on `native` (Swift ViewModel tests stay as they are).

## User flow
The "user" is a developer on the repository.

1. The developer writes a unit test for shared logic in the JVM test source set, using MockK for test doubles and the shared fixtures and test dispatchers.
2. The developer runs one JVM test command locally; CI on `main` runs the same command and coverage verification.
3. When the developer changes platform-specific code, they write or run a test on that platform (Android emulator/device or iOS simulator) locally.

## UI states
Not applicable (no UI).

## Acceptance criteria
- **AC-1** Shared logic is unit-tested on the JVM only
  - Given a shared module
  - When a developer adds a unit test for its logic
  - Then the test lives in `jvmTest`, and `commonTest` contains no tests
- **AC-2** MockK is the only mocking library for Kotlin tests
  - Given any Kotlin test that needs a test double
  - When the double is created
  - Then it is a MockK mock; Mokkery is not used and no hand-written fakes exist
- **AC-3** The shared test module holds no fakes
  - Given the shared test module
  - When its contents are listed
  - Then it holds only fixture JSON, test dispatchers and shared test helpers that are not fakes
- **AC-4** One JVM command runs every unit test
  - Given the repository on `main`
  - When `./gradlew jvmTest` runs
  - Then every unit test runs, and the CI command on `main` is unchanged (`detekt`, `jvmTest`, `koverVerify`, `:architecture-test:test`)
- **AC-5** Coverage threshold is unchanged
  - Given `core:domain` and `core:data`
  - When `koverVerify` runs
  - Then it still requires at least 80% line coverage, measured on JVM tests
- **AC-6** Platform code is tested on its platform, locally
  - Given an `actual` implementation (for example Keystore, Keychain, SQLite driver, Ktor engine, certificate pinning)
  - When it is tested
  - Then Android code is tested with instrumented tests on an emulator or device, iOS code is tested on the simulator with `kotlin.test` and no mocks, and neither runs on `main` CI
- **AC-7** `cmp` follows the same rule
  - Given the `cmp` branch
  - When shared ViewModels and Compose UI are tested
  - Then ViewModel tests run on the JVM with MockK and Turbine, and Compose UI tests run on the JVM only
- **AC-8** `native` Android tests use MockK
  - Given the `native` branch
  - When Android unit tests need test doubles
  - Then they use MockK
- **AC-9** Docs match the strategy
  - Given `docs/architecture.md` and `CLAUDE.md`
  - When a developer reads the testing sections
  - Then they describe the rules above, and the statement that MockK is not used in `commonTest` is gone

## Edge cases
- **EC-1** A behavior differs between Kotlin/Native and the JVM (number formatting, regex, time zones, exceptions) -> not caught by unit tests; accepted trade-off, covered by platform tests and iOS XCTest on `native`.
- **EC-2** A test needs network responses -> uses Ktor `MockEngine` with fixture JSON (a test engine, not a hand-written fake).
- **EC-3** A test needs the database -> uses the real SQLDelight in-memory driver on the JVM, not a mock.
- **EC-4** A platform test needs a test double -> no MockK (not available on iOS, not used in instrumented tests); test against the real platform API.
- **EC-5** A developer has no emulator or macOS/Xcode -> platform tests are skipped and the skip is reported; JVM tests and CI are unaffected.

## Constraints
- MockK runs only on the JVM; it must never be used in `commonTest` or iOS test source sets.
- `main` CI stays JVM-only on Ubuntu.
- Android and iOS targets keep compiling; only where tests run changes.

## Open questions
- [x] Add a Konsist rule that fails when a test file is placed in `commonTest`, so AC-1 is checked automatically? (owner: user, before `blueprint`) — No Konsist rule; documentation and review are enough.

## Change log
- 2026-11-10: Initial version.
- 2026-11-10: Open question resolved: no Konsist rule.
