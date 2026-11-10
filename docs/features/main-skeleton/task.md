# Main branch skeleton

Status: Confirmed
Source: chat + [docs/architecture.md](../../architecture.md)

## Goal
Set up the build foundation of the `main` branch as described in the architecture document. All later work (network, database, settings, the `cmp` and `native` branches) should only need to add logic to modules that already exist. From the first commit, every change is checked for style, tests, coverage and architecture rules, both locally and on CI.

## Scope
### In scope
- A shared build setup: one version catalog plus convention plugins, so all modules are configured the same way.
- The ten shared core modules from the architecture document (`common`, `model`, `domain`, `network`, `database`, `datastore`, `security`, `data`, `di`, `testing`), created empty. Each compiles for Android, iOS device, iOS simulator and JVM.
- Root package `com.banhmilabs.tmdb`.
- Minimum platforms: Android API 24, iOS 16.
- One sample test per module.
- Static analysis with Detekt and ktlint formatting rules.
- Coverage verification: at least 80% line coverage for `domain` and `data`, excluding dependency injection, DTOs and generated code.
- Architecture tests for the dependency rules in section 4.4 of the architecture document.
- CI on GitHub Actions (Ubuntu, JVM only): static analysis, JVM tests and coverage verification.
- An update to the architecture document so its CI section matches this task: `main` CI runs on the JVM only.

### Out of scope
- Hiding the API key (`secrets.properties` and the obfuscated key): moved to the network task.
- Real logic: HTTP client, database schema, Keystore/Keychain, repositories, use cases.
- App modules, UI, and the `cmp` and `native` branches.
- Android and iOS jobs on CI.
- Database encryption (SQLCipher).

## User flow
The "user" here is a developer working on the repository.

1. The developer clones the repository and runs the checks locally (static analysis, JVM tests, coverage verification, architecture tests). All of them pass.
2. The developer adds code to a core module. If it breaks a style rule, the coverage threshold or a dependency rule, a local check fails with a message that points to the cause.
3. The developer adds a new shared module by applying the shared build configuration and declaring its namespace. Nothing else is needed.
4. The developer pushes or opens a pull request against `main`. CI runs the same checks and reports pass or fail on the commit.

## UI states
Not applicable (no UI).

## Acceptance criteria
- **AC-1** All core modules compile for every target
  - Given a macOS machine with the Android SDK and Xcode installed
  - When the developer compiles the project
  - Then all ten core modules compile for Android, iOS arm64, iOS simulator arm64 and JVM without errors
- **AC-2** JVM tests run and pass
  - Given the skeleton
  - When the developer runs the JVM tests
  - Then every core module runs at least one test and all tests pass
- **AC-3** Single place for versions and shared configuration
  - Given a developer needs to change a dependency version or add a module
  - When they look for where to do it
  - Then every version is declared in one version catalog, and a new shared module only needs to apply the shared configuration and declare its namespace
- **AC-4** Style and static analysis are enforced
  - Given code that breaks a Detekt or ktlint formatting rule
  - When the developer runs the check
  - Then the check fails and names the file and rule
- **AC-5** Coverage threshold is enforced on business logic
  - Given `domain` and `data` with less than 80% line coverage (excluding DI, DTOs and generated code)
  - When coverage verification runs
  - Then it fails; on the empty skeleton it passes
- **AC-6** Architecture rules are enforced
  - Given code that breaks a dependency rule (for example, `domain` importing from `data`, or `model`/`domain` importing Ktor, SQLDelight or Compose)
  - When the architecture tests run
  - Then they fail and name the offending file and the broken rule
- **AC-7** CI checks every change to `main`
  - Given a push to `main` or a pull request targeting `main`
  - When CI runs on Ubuntu
  - Then it runs static analysis, JVM tests and coverage verification, and marks the commit as failed if any of them fails
- **AC-8** Architecture document matches the CI setup
  - Given the architecture document
  - When a reader checks the CI section
  - Then it says that `main` CI runs JVM jobs only

## Edge cases
- **EC-1** Machine without Xcode (or not macOS) -> JVM compilation, tests and checks still run. iOS targets are skipped with a warning, not an error.
- **EC-2** No `secrets.properties` file -> the build works, because the skeleton does not read any key.
- **EC-3** A module without production code -> coverage verification does not fail because of it.
- **EC-4** CI run for a pull request from a fork -> the checks run without needing repository secrets.

## Constraints
- Must follow the module list, package layout and dependency rules in `docs/architecture.md`.
- Root package: `com.banhmilabs.tmdb`.
- Minimum Android API 24, minimum iOS 16.
- CI is JVM-only, so it uses no macOS runners.
- Use current stable versions of the toolchain and libraries.

## Open questions
- [ ] Swift ViewModels: the architecture document says `@Observable`, which requires iOS 17, but the minimum is iOS 16. Choose between `ObservableObject` and raising the minimum to iOS 17. (owner: Alex, needed before the `native` branch)

## Change log
- 2026-11-10: Initial version.
- 2026-11-10: Confirmed.
