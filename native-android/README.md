# native-android

A standalone native Android app (Jetpack Compose). It uses the shared core of this repo the way an outside team
uses an SDK: through the published Android libraries in `../build/maven-repo`, never through the Kotlin
Multiplatform build itself.

## Prerequisites
- JDK 21.
- Android SDK: `native-android/local.properties` with `sdk.dir=...` (git-ignored).

## Workflow
1. Publish the shared core, from the repo root:
   ```bash
   ./gradlew publishSharedCore
   ```
2. Build and install the app, from `native-android/`:
   ```bash
   ./gradlew :app:installDebug
   ```
3. After changing the shared core, publish again (step 1), then rebuild the app. The version stays
   `0.1.0-SNAPSHOT`; the new libraries replace the old ones.

## Checks
```bash
./gradlew :app:detekt :app:testDebugUnitTest
```

## Troubleshooting
- `Could not find com.banhmilabs.tmdb:core-di:0.1.0-SNAPSHOT`: the shared core was not published yet. Run
  `./gradlew publishSharedCore` at the repo root first.
- The app does not show a shared-core change: the shared core was not published again after the change. Publish,
  then rebuild.
