# 01 - Publish shared core

Feature: [task.md](../task.md)
Depends on: none
Status: Done

## Goal
One command in the KMP build publishes the shared core as Android libraries into a Maven repository inside the
repo, so an independent Android project can depend on it like any SDK.

## Scope
### In scope
- Publishing every production module of `core/` and `service/` as an Android library with a POM that lists its
  dependencies.
- The in-repo Maven repository (git-ignored) and the fixed version `0.1.0-SNAPSHOT`.
- Leaving test-only modules (`service/*/fake`, `core:testing`) unpublished.
- One documented command that publishes everything.
### Out of scope (handled later)
- The standalone Android project that consumes the libraries -> 02-standalone-native-android-app
- Architecture docs for the new setup -> 03-architecture-without-apps-native
- Publishing to a remote repository, and iOS frameworks (not in this feature)

## Acceptance criteria
- **AC-1** Shared core is published
  - Given the KMP build
  - When the developer runs the publish command
  - Then every production module of `core/` and `service/` is in the in-repo Maven repository as an Android
    library with a POM listing its dependencies, at version `0.1.0-SNAPSHOT`
  - And test-only modules (`service/*/fake`, `core:testing`) are not published

## Edge cases
- **EC-3** The shared core is published again at the same `0.1.0-SNAPSHOT` version -> the new libraries replace
  the old ones, and the next app build uses them.

## Layers touched
- Build logic (publishing setup for shared modules)
- Repository config (in-repo Maven repository, git-ignore)

## How to test
1. Run the publish command: it succeeds, and the in-repo Maven repository contains an Android library and a POM for
   each production module of `core/` and `service/`, at `0.1.0-SNAPSHOT` (AC-1).
2. Open the POM of a `data` module: it lists its `domain` module and libraries such as Koin as dependencies (AC-1).
3. The repository contains nothing for `service/*/fake` or `core:testing` (AC-1).
4. Change a shared module and publish again: the files are replaced at the same version (EC-3).
5. The CI checks (`./gradlew detekt jvmTest koverVerify :architecture-test:test`) still pass, and the repository
   folder is git-ignored.

## Change log
- 2026-11-12: Initial version.
