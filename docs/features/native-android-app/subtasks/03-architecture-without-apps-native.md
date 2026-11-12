# 03 - Architecture without apps/native

Feature: [task.md](../task.md)
Depends on: 02-standalone-native-android-app
Status: Todo

## Goal
The KMP build and its docs describe the new setup:
- No native Android modules live in the KMP build.
- The native Android app is the standalone `native-android/` project that consumes the published shared core.
- A standalone `native-ios/` follows later.

The CI checks still pass without touching `native-android/`.

## Scope
### In scope
- The architecture doc, the project guide and the build and architecture rules:
  - Remove `apps/native` as part of the KMP build.
  - Describe `native-android/` and the publish-then-build workflow.
  - Note that the iOS native app will follow the same approach later.
- Any build-logic or module-rule handling that exists only for `apps/native` in the KMP build is removed.
### Out of scope (handled later)
- The standalone native iOS app (not in this feature)
- Retargeting the onboarding feature's native subtasks (done in the onboarding feature)

## Acceptance criteria
- **AC-6** KMP build is free of native Android modules
  - Given the KMP build
  - Then it contains no native Android app modules
  - And the CI checks (`detekt jvmTest koverVerify :architecture-test:test`) pass without building
    `native-android/`

## Edge cases
None from task.md; EC-1–EC-3 are handled in 01 / 02.

## Layers touched
- Docs (architecture, project guide, rules)
- Build logic and module rules (only where `apps/native` is handled)

## How to test
1. List the KMP build's projects: none is under `apps/native` (AC-6).
2. Run the CI checks (`./gradlew detekt jvmTest koverVerify :architecture-test:test`): they pass, and no task of
   `native-android/` runs (AC-6).
3. Read the architecture doc and the project guide: they describe `native-android/`, how it gets the shared core,
   and the commands to publish and build it.

## Change log
- 2026-11-12: Initial version.
