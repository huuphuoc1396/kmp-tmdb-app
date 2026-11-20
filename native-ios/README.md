# native-ios

A standalone native iOS app (SwiftUI). It uses the shared core of this repo the way an outside team uses an SDK:
through the local Swift Package `TmdbCore` in `../build/swift-package/TmdbCore`, never through the Kotlin
Multiplatform build itself.

## Prerequisites
- macOS with Xcode 16 or later.
- JDK 21, to publish the shared core.

## Workflow
1. Publish the shared core, from the repo root:
   ```bash
   ./gradlew publishSharedCore
   ```
   On macOS this writes the release XCFramework `TmdbCore` and its `Package.swift` to `build/swift-package/TmdbCore`.
2. Open `native-ios/TmdbNative.xcodeproj` in Xcode and run the `TmdbNative` scheme on a simulator, or build from
   `native-ios/`:
   ```bash
   xcodebuild -project TmdbNative.xcodeproj -scheme TmdbNative -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
   ```
3. After changing the shared core, publish again (step 1), then rebuild the app.

Posters load with [Nuke](https://github.com/kean/Nuke) (`NukeUI`), a remote Swift package pinned in
`TmdbNative.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved`. Xcode fetches it on the first build,
which needs network once.

## Tests
From `native-ios/`, on an installed iPhone simulator:
```bash
xcodebuild -project TmdbNative.xcodeproj -scheme TmdbNative -destination 'platform=iOS Simulator,name=iPhone 17' test
```
The tests run inside the app and use the real shared core through the published API.

## Troubleshooting
- Xcode reports a missing package at `../build/swift-package/TmdbCore`: the shared core was not published yet. Run
  `./gradlew publishSharedCore` at the repo root first.
- The app does not show a shared-core change: the shared core was not published again after the change. Publish,
  then rebuild. If Xcode still uses the old framework, use File > Packages > Reset Package Caches.
