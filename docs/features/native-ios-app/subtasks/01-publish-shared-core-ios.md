# 01 - Publish shared core for iOS

Feature: [task.md](../task.md)
Depends on: none
Status: Done

## Goal
The KMP build can publish the shared core for iOS with one command: a release XCFramework with the
`iosArm64` and `iosSimulatorArm64` slices, delivered as a local Swift Package in a git-ignored folder in the
repo. The framework exposes the domain API with Swift-friendly interop and one entry point that starts the core
and returns its repositories.

## Scope
### In scope
- One iOS umbrella framework of the shared core:
  - It exports the shared kernel and the `service/*/domain` APIs, with Swift-friendly interop: `Flow` as an async
    sequence, `suspend` as `async`, sealed types as enums.
  - It has one entry point: start the shared core, and accessors that return the domain's repositories. The
    dependency injection framework is not visible to Swift.
- A release build with the `iosArm64` and `iosSimulatorArm64` slices.
- A local Swift Package (the framework plus its package manifest) in a git-ignored folder in the repo, written by
  one publish command.
- Republishing replaces the previous framework.
### Out of scope (handled later)
- The `native-ios/` app that consumes the package -> 02-standalone-native-ios-app
- Architecture docs, project guide and rules about native iOS -> 03-architecture-with-native-ios

## Acceptance criteria
- **AC-1** Shared core is published for iOS
  - Given the KMP build on macOS with Xcode
  - When the developer runs the iOS publish command
  - Then a release XCFramework with the `iosArm64` and `iosSimulatorArm64` slices is in the in-repo folder,
    with a Swift Package that provides it
  - And it exposes the shared core's domain API and one entry point to start the core and get its repositories
  - And it contains no test-only code (service fakes, test utilities) and no app code

## Edge cases
- **EC-3** The shared core is published again -> the new framework replaces the old one, and the next app build
  uses it.

## Layers touched
- Build logic (framework packaging and the publish command)
- Shared core: an iOS umbrella module with the entry point
- Static analysis and architecture rules for the new module

## How to test
1. On macOS with Xcode, run the iOS publish command at the repo root: the folder holds the XCFramework (a device slice
   and an arm64 simulator slice) and a `Package.swift` (AC-1).
2. Read the framework's Swift interface (its headers or `.swiftinterface`): the settings domain API (for example
   the onboarding completed state) and the entry point are there; no fake or app type is (AC-1).
3. Change a shared module, publish again: the framework files are replaced (EC-3).
4. Run the CI checks (`./gradlew detekt jvmTest koverVerify :architecture-test:test`): they still pass.

## Change log
- 2026-11-14: Initial version.
