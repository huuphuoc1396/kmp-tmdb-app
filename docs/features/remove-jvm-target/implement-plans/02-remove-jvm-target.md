# Plan: Remove the JVM target

Requirement: [02 - Remove the JVM target](../subtasks/02-remove-jvm-target.md)
Status: Done

## Summary
After 01 no test uses the JVM target any more, so `tmdb.module` stops declaring `jvm()`. The two JVM-only
production files and the `jvmMain` dependency blocks are deleted, and the catalog entry that only they used goes
with them. Publishing then produces Android and iOS artifacts only. The plan checks that every target still
compiles, the CI checks pass, the published repository has no `-jvm` artifacts, and both native apps still build
on the new publication.

## Decisions
- **`jvm()` removed from `configureKotlinMultiplatform()`, KDoc: "targeting Android and iOS (device + simulator)".**
  This is the only place that declares the target. `jvmToolchain(…)` stays: it sets the JDK for Gradle and Kotlin
  compilations, not a target.
- **Delete `core/security/src/jvmMain` (`MasterKeyCipher.jvm.kt`: `InMemoryKeyCipher`, `inMemoryKeys`) and
  `service/settings/data/src/jvmMain` (`SettingsFilePath.jvm.kt`).** The `expect` declarations keep their Android
  and iOS `actual`s.
- **Remove the `jvmMain.dependencies` blocks** in `core/security`, `core/testing` and `service/settings/fake`.
  `androidMain` keeps `kotlin-test-junit`.
- **Remove `cryptography-provider-jdk` from `gradle/libs.versions.toml`:** only the JVM cipher used it.
  `cryptography-core` and `cryptography-provider-optimal` stay for iOS.
- **No change to `publishSharedCore` or `Publishing.kt`.** The JVM publication disappears with the target.
  Artifacts left over from earlier publishes stay in the git-ignored `build/maven-repo` until it is deleted. They
  are harmless: the Gradle metadata no longer lists a JVM variant. The check in step 2 publishes into a clean
  folder.
- **CI unchanged:** after 01 it no longer runs any JVM task of the shared modules. This plan only confirms that it
  passes (AC-6).
- **The build-logic test `":cmp:desktopApp"` stays** in the list of rejected paths. It checks module paths, not
  targets.
- **Docs that mention the JVM target** (architecture, `CLAUDE.md`, rules) are left to 03. The code KDocs touched
  here are updated in place.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Target setup | `configureKotlinMultiplatform()` | `build-logic/.../buildlogic/KotlinMultiplatform.kt` |
| Real ciphers | `AndroidKeystoreCipher`, `KeychainCipher` | `core/security/src/{androidMain,iosMain}` |
| Settings file path | `settingsFilePath()` Android and iOS `actual`s | `service/settings/data/src/{androidMain,iosMain}/.../di/` |
| Publishing | `publishSharedCore` | `build-logic/.../buildlogic/Publishing.kt`, `IosSdk.kt` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| build-logic | `buildlogic/KotlinMultiplatform.kt` (modify) | Drop `jvm()`; KDoc |
| `core:security` | `src/jvmMain/.../MasterKeyCipher.jvm.kt` (delete), `build.gradle.kts` (modify) | No JVM cipher |
| `service:settings:data` | `src/jvmMain/.../SettingsFilePath.jvm.kt` (delete) | No JVM path |
| `core:testing`, `service:settings:fake` | `build.gradle.kts` (modify) | No `jvmMain` dependencies |
| catalog | `gradle/libs.versions.toml` (modify) | Drop `cryptography-provider-jdk` |

## Steps
### Step 1: Drop the target and the JVM code  (AC-1, AC-3) ✅
- Files: as in Changes overview.
- What:
  - **`KotlinMultiplatform.kt`:**
    - Remove `jvm()`.
    - The KDoc's first sentence becomes "Kotlin Multiplatform library targeting Android and iOS (device +
      simulator), with kotlinx.serialization."
  - **`git rm`** both `jvmMain` files, and remove the empty `jvmMain` folders.
  - **Module build files:** remove the `jvmMain.dependencies { … }` block in `core/security`, `core/testing` and
    `service/settings/fake`. The comment above the fake's block, "kotlin-test picks a JUnit variant only for test
    compilations; main code needs it explicitly.", stays above `androidMain`.
  - **Catalog:** remove the `cryptography-provider-jdk` line.
- Unit tests: none new.
- Compile: `./gradlew assemble` (every target; macOS + Xcode).

### Step 2: Verify the build, publication and consumers  (AC-1, AC-6, EC-2) ✅
- What, all checks without code changes:
  - `./gradlew detekt testAndroidHostTest koverVerify :architecture-test:test --continue` passes.
  - **Clean publish:** `rm -rf build/maven-repo build/swift-package`, then `./gradlew publishSharedCore`. Then
    `find build/maven-repo -name "*-jvm*"` returns nothing, and `core-di/…/*.module` lists only Android and iOS
    variants.
  - **No JVM source sets left:**
    `find core service cmp -type d \( -name "jvmMain" -o -name "jvmTest" \) -not -path "*/build/*"` returns
    nothing.
  - **Consumers:**
    - `native-android/`: `./gradlew :app:assembleDebug :app:testDebugUnitTest`.
    - `native-ios/`: `xcodebuild … build`, then `xcodebuild … test`, on iPhone 17. The Keychain-backed settings
      test passes (EC-2).
- Compile: as above.

## Traceability
| AC / EC | Step(s) | Test(s) / check |
|---|---|---|
| AC-1 Only Android and iOS targets | 1, 2 | `./gradlew assemble` with no JVM compile tasks; no `jvmMain` / `jvmTest` folders (search); clean publish has no `-jvm` artifacts |
| AC-3 No fake cipher in production | 1 | `MasterKeyCipher.jvm.kt` deleted; only the Android and iOS `actual`s remain (search) |
| AC-6 CI passes without JVM | 2 | Local CI command passes; the PR's GitHub run |
| EC-2 Keychain covered by `native-ios` | 2 | `native-ios` `SharedCoreApiTests` pass on the simulator |

## Out of scope
- Docs and rules that still mention the JVM target or JVM unit tests: 03.
- Deleting stale artifacts from earlier publishes automatically: not required; a clean folder is used for the
  check.

## Risks
- **Kotlin may pick a different default hierarchy once JVM is gone:** for example, `expect` declarations that
  only JVM satisfied. Mitigation: the search found `actual`s for Android and iOS for both `expect`s;
  `./gradlew assemble` confirms.
- **A test or tool may still call a JVM task by name** (for example `:<module>:jvmTest` in scripts or IDE run
  configurations). Mitigation: the repository search found none outside docs; docs are fixed in 03.

## Deviations
- None.

## Results
- `./gradlew assemble`: pass. `:core:security` has no JVM tasks left.
- No `jvmMain` or `jvmTest` folders remain.
- `./gradlew detekt testAndroidHostTest koverVerify :architecture-test:test --continue`: pass.
- Clean publish (`rm -rf build/maven-repo build/swift-package`, then `./gradlew publishSharedCore`):
  - The repository has 0 `-jvm` artifacts. Each module is published for Android, `iosarm64` and
    `iossimulatorarm64`.
  - The `core-di` Gradle metadata lists only the `androidJvm`, `native` and `common` platform types.
  - `build/swift-package/TmdbCore` was written.
- `native-android`: `./gradlew :app:assembleDebug :app:testDebugUnitTest --refresh-dependencies` passes.
- `native-ios`: `xcodebuild … test` on iPhone 17 runs 11 tests with 0 failures, including the Keychain-backed
  `SharedCoreApiTests` (EC-2).
- Not run: CI on GitHub (runs on the PR).
