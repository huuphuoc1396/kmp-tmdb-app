# 03 - Architecture with native-ios

Feature: [task.md](../task.md)
Depends on: 02-standalone-native-ios-app
Status: Done

## Goal
The KMP build's docs and rules describe the standalone `native-ios/` app and how the shared core is published for
iOS, the same way they describe `native-android/`. The CI checks still pass without touching `native-ios/`.

## Scope
### In scope
- The architecture doc, the project guide and the rules:
  - Replace the "planned" notes about `native-ios/` with the real setup: the XCFramework, the local Swift Package,
    the entry point, and Swift-friendly interop.
  - Describe the iOS publish command and the commands to build and test `native-ios/`.
  - The testing strategy row for native iOS.
- Any leftover native iOS app code in the KMP build is removed (none is expected).
### Out of scope (handled later)
- The onboarding screen on iOS -> onboarding subtask 07

## Acceptance criteria
- **AC-6** KMP build is free of native iOS app code
  - Given the KMP build
  - Then it contains no native iOS app code
  - And the CI checks (`detekt jvmTest koverVerify :architecture-test:test`) pass without building `native-ios/`

## Edge cases
None from task.md; EC-1–EC-3 are handled in 01 / 02.

## Layers touched
- Docs (architecture, project guide, rules)

## How to test
1. The KMP build's projects contain no native iOS app module or Swift sources outside `native-ios/` and the cmp
   `iosApp` (AC-6).
2. Run the CI checks (`./gradlew detekt jvmTest koverVerify :architecture-test:test`): they pass, and nothing in
   `native-ios/` is built (AC-6).
3. Read the architecture doc and the project guide: they describe `native-ios/`, how it gets the shared core, and
   the commands to publish, build and test it.

## Change log
- 2026-11-14: Initial version.
