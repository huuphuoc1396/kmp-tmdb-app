# 02 - Remove the JVM target

Feature: [task.md](../task.md)
Depends on: 01-unit-tests-on-android-host
Status: Todo

## Goal
The KMP build targets only Android and iOS. The JVM-only production code (in-memory master key, JVM settings file
path) is gone, the published shared core has no JVM artifacts, and CI still passes.

## Scope
### In scope
- The JVM target removed from every shared and cmp module.
- The JVM-only production code deleted.
- Publishing the shared core produces only Android and iOS artifacts.
- CI confirmed to pass without the JVM target.
### Out of scope (handled later)
- Docs and rules -> 03-docs-for-android-and-ios-testing

## Acceptance criteria
- **AC-1** Only Android and iOS targets
  - Given the KMP build
  - Then every shared module and every cmp module targets only Android and iOS
  - And there is no JVM-only production code
  - And publishing the shared core produces no JVM artifacts
- **AC-3** No fake security code in production (completes 01)
  - Given the production sources
  - Then the master key cipher has only the Android Keystore and iOS Keychain implementations
- **AC-6** CI on Ubuntu (completes 01)
  - Given a pull request to `main`
  - Then CI runs static analysis, Android host tests, the coverage gate and the architecture tests on Ubuntu,
    and they pass

## Edge cases
- **EC-2** A Kotlin test on iOS would need the Keychain -> it is not written as a Kotlin iOS test (no app
  bundle there); the behavior is covered by the `native-ios` Swift tests.

## Layers touched
- Build logic (targets, publishing)
- Platform code of the security module and the settings data module
- CI workflow

## How to test
1. Build every target: Android and iOS compile; no JVM compilation runs (AC-1).
2. Search the production sources: no JVM source sets remain (AC-1, AC-3).
3. Publish the shared core: the local repository has Android and iOS artifacts and no `-jvm` ones (AC-1).
4. Rebuild `native-android/` and `native-ios/` against the new publication: both build and launch.
5. Run the CI checks locally, then open a PR: CI passes on Ubuntu (AC-6).
6. Run the `native-ios` tests on a simulator: the Keychain-backed settings test passes (EC-2).

## Change log
- 2026-11-17: Initial version.
