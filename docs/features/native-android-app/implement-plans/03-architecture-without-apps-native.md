# Plan: Architecture without apps/native

Requirement: [03 - Architecture without apps/native](../subtasks/03-architecture-without-apps-native.md)
Status: Done

## Summary
The KMP build already has no `apps/native` modules: `settings.gradle.kts` includes only `apps/cmp`, and PR #11
was closed. What remains is the description and the rules:
- The architecture doc, `CLAUDE.md` and the rules still describe `apps/native` as an in-build app. They are
  rewritten so the KMP build hosts the shared core and `apps/cmp`, and native apps are standalone consumers of the
  published core: `native-android/` now, `native-ios/` planned.
- The app → other app rule existed only for a second in-build app. It is removed from `tmdb.module`
  (`DependencyRules.appViolation`) and from Konsist, with their tests. The rule that the shared core never depends
  on an app stays.

## Decisions
- **Remove the rule between apps** (from user). The KMP build has one app, so the app → other app rule in
  `DependencyRules.appViolation()` and the Konsist test `apps import no other app` are removed, together with their
  tests.
  - Kept: "the shared core (core, service) never depends on an app", in both `tmdb.module` and Konsist.
  - Kept: the module types and their table. `moduleTypeOf` stays generic (`:apps:<app>:…`).
  - The build-logic tests that used `:apps:native:…` as examples switch to `:apps:cmp:…` or are dropped when
    they only tested the cross-app rule.
  - Rejected: renaming the example app; keeping the tests unchanged.
- **iOS: a short "planned" note** (from user). `native-ios/` will consume an XCFramework published by the KMP build,
  the same way `native-android/` consumes AARs. The principles are kept as one paragraph: one Kotlin umbrella
  framework per iOS app, and SKIE for `Flow` / `suspend` / `sealed` interop. The detailed `apps/native` iOS
  structure (`shared/`, `KoinHelper`, `Features/<X>`) is removed and written when that app is built. Rejected:
  moving the details to `native-ios/` now.
- **Where `native-android/` is described**:
  - `docs/architecture.md` sections 1, 2, 9, 11, 12 and 13, plus a new section 14 "Publishing the shared core"
    with the coordinates, the command and the consumer setup.
  - `CLAUDE.md`: repository layout, the commands to build and check the standalone app, and the Do-nots.
  - `native-android/README.md` already holds the workflow (subtask 02) and does not change.
- **Native Android testing row**: JVM unit tests with MockK on the published domain API (as in
  `SharedCoreApiTest`). Compose UI tests come with the first real screen. iOS native testing becomes "planned with
  `native-ios/`".
- **Keychain note in `.claude/rules/testing.md`**: the Xcode test host "comes with `native-ios/` or an `iosApp`
  test target" instead of "comes with `apps/native`".
- **Local leftovers**: `apps/native/{designsystem,androidApp}/build` are git-ignored build outputs from PR #11.
  They are deleted, so `apps/` holds only `cmp`. This is not tracked by git.
- **`.gitignore`**: `gradle/gradle-daemon-jvm.properties` only ignores the root copy. It becomes
  `**/gradle/gradle-daemon-jvm.properties`, so the copy Gradle generates in `native-android/` is ignored too. This
  is part of the standalone setup.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Shared core → app rule (kept) | `appViolation()` first branch | `build-logic/.../buildlogic/DependencyRules.kt` |
| Konsist shared core → app rule (kept) | `` `shared core imports no app` `` | `architecture-test/.../DependencyRulesTest.kt` |
| Publish command and coordinates | `configurePublishing()`, `publishSharedCore` | `build-logic/.../buildlogic/Publishing.kt` |
| Standalone app workflow | README | `native-android/README.md` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| build-logic | `buildlogic/DependencyRules.kt`, `test/.../DependencyRulesTest.kt`, `test/.../ModuleTypeTest.kt` (modify) | Remove the app → app rule; no `apps/native` examples |
| architecture-test | `DependencyRulesTest.kt`, `ModulePaths.kt` (modify) | Remove `apps import no other app` and the now unused `appOf` |
| docs | `docs/architecture.md`, `CLAUDE.md`, `.claude/rules/build-logic.md`, `.claude/rules/architecture-test.md`, `.claude/rules/testing.md` (modify) | Describe the new setup |
| repo | `.gitignore` (modify); local `apps/native/` build outputs (deleted, untracked) | Hygiene |
| feature docs | subtask 03 | Status |

## Steps
### Step 1: Remove the app → app rule  (AC-6) ✅
- Files: `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/DependencyRules.kt`,
  `build-logic/convention/src/test/kotlin/com/banhmilabs/tmdb/buildlogic/DependencyRulesTest.kt`,
  `.../ModuleTypeTest.kt`, `architecture-test/src/test/kotlin/com/banhmilabs/tmdb/architecture/DependencyRulesTest.kt`,
  `.../ModulePaths.kt`
- What:
  - `DependencyRules`:
    - `appViolation` keeps only the shared-core branch: `if (from.app == null && to.app != null) "the shared core
      (core, service) never depends on an app" else null`.
    - KDoc: "The shared core never depends on an app."
    - `ruleFor(UI)` becomes "ui depends only on nav/api, the designsystem, service domains and the kernel".
  - build-logic `DependencyRulesTest`:
    - Remove `uiUsesOnlyItsOwnAppFeaturesAndDesignsystem`, `appsNeverDependOnEachOther` and
      `appShellMayNotDependOnOtherApp`. The allowed cases they also held are already in `featureTypesFollowTable`
      and `appShellMayDependOnSharedCoreAndOwnApp`.
    - Keep `sharedCoreNeverDependsOnAnApp`.
  - `ModuleTypeTest`:
    - `:apps:native:feature:cart:api` → `:apps:cmp:feature:cart:api`.
    - Drop the `:apps:native:designsystem` assertion (cmp is already covered).
    - `:apps:native:android:feature:home:ui` → `:apps:cmp:android:feature:home:ui` in `unknownPathsReturnNull`.
  - Konsist: remove `` `apps import no other app` `` and `appOf` (no other user).
- Unit tests: the remaining build-logic and Konsist tests.
- Compile: `./gradlew -p build-logic :convention:compileTestKotlin :convention:test`,
  `./gradlew :architecture-test:compileTestKotlin`

### Step 2: Architecture doc  (AC-6) ✅
- Files: `docs/architecture.md`
- What:
  - **Intro and 1. Goals**:
    - The table rows become `apps/cmp` (unchanged) and `native-android/` (shared core through published AARs;
      ViewModel and UI with Android ViewModel / Jetpack Compose), plus a note that `native-ios/` follows the same
      way later.
    - The goal "two presentation styles" is reworded: one core, consumed in-build by `apps/cmp` and as a published
      SDK by standalone native apps.
  - **2. Repository layout**:
    - The tree gets `apps/cmp/` only under `apps/`, plus `native-android/` (standalone Gradle project, own
      settings/wrapper/catalog/Detekt, consumes `build/maven-repo`) and `native-ios/` (planned).
    - Bullets: the KMP build never includes `native-*`; native apps reach the core only through published
      artifacts and see only `domain` + `core:di` (plus transitive runtime). They pick up a core change after a
      republish.
    - The "never depend on each other" rule becomes: `native-*` never depends on `apps/cmp`, and the KMP build
      never references `native-*`.
  - **5.3**:
    - The forbidden list drops "anything from `apps/cmp` to `apps/native` or back".
    - Add: "the shared core never depends on an app."
  - **6.2**: The reason for optional use cases now refers to the native apps, where the ViewModel exists per
    platform (Kotlin on Android, Swift on iOS).
  - **8. Testing**:
    - The `apps/native` rows become "Native Android (`native-android/`): JVM unit tests with MockK against the
      published API; Compose UI tests with the first real screen".
    - Plus "Native iOS (`native-ios/`): planned".
  - **9. Enforcement**:
    - The cross-app sentence becomes: "`ui` uses only `nav` / `api` / designsystem; the shared core never depends
      on an app."
    - The status note drops `apps/native`. Standalone apps are checked by their own Detekt and tests, not by
      `tmdb.module` or Konsist.
  - **11. Presentation**:
    - `### apps/native` is replaced by `### native-android/: standalone Android app`: one `:app` module
      (theme in `designsystem` package, Activity, NavHost, `Application` calling `initKoin`), modules split out
      when a second feature needs them, dependencies `com.banhmilabs.tmdb:core-di` and
      `service-<ctx>-domain`.
    - Then `### native-ios/ (planned)`: one paragraph with XCFramework, one umbrella framework and SKIE.
    - The Localization rows become `native-android/` Android resources, `native-ios/` String Catalog (planned).
  - **12. Tech stack**: UI row `Jetpack Compose (native-android/)`, SwiftUI planned; iOS interop SKIE (planned,
    `native-ios/`).
  - **13. CI**:
    - CI covers only the KMP build.
    - `native-android/` is built and checked locally: `./gradlew publishSharedCore`, then
      `cd native-android && ./gradlew :app:assembleDebug :app:detekt :app:testDebugUnitTest`.
  - **New 14. Publishing the shared core**:
    - What is published: production modules with code; never `fake`, `core:testing` or apps.
    - Coordinates `com.banhmilabs.tmdb:<path-with-dashes>:0.1.0-SNAPSHOT`, published to `build/maven-repo` by
      `./gradlew publishSharedCore`.
    - A consumer declares the repository restricted to the group and depends on root coordinates; Gradle
      metadata picks the platform variant.
    - SNAPSHOT republishing replaces the artifacts.
- Unit tests: none (docs).
- Compile: none.

### Step 3: Project guide, rules and repo hygiene  (AC-6) ✅
- Files: `CLAUDE.md`, `.claude/rules/build-logic.md`, `.claude/rules/architecture-test.md`,
  `.claude/rules/testing.md`, `.gitignore`; delete the local `apps/native/` build outputs
- What:
  - **`CLAUDE.md`**:
    - Intro: the apps are `apps/cmp` (in the KMP build) and the standalone `native-android/` (and later
      `native-ios/`).
    - **Repository layout**: replace the `apps/native` bullets with `native-android/`, a standalone Gradle project
      consuming `build/maven-repo`. It is not part of this build, and `native-*` never depends on `apps/cmp`.
    - **Commands**: add "Native Android app (standalone): `./gradlew publishSharedCore`, then
      `cd native-android && ./gradlew :app:installDebug`; checks `./gradlew :app:detekt :app:testDebugUnitTest`".
    - **Architecture / Modules**:
      - Drop "apps never depend on each other" from the `tmdb.module` sentence.
      - Drop "a file in one app imports nothing from another app" from the Konsist list.
    - **Do not**: "Don't make `native-android/` depend on `apps/cmp` or reference the KMP build (no
      `includeBuild`, `project(...)`, source paths); it uses only the published core. Don't put UI in `core/` or
      `service/`."
  - **`build-logic.md`**: `appViolation()` rejects only shared core → app.
  - **`architecture-test.md`**:
    - Remove `appOf` from the helper list.
    - Change the last "Current rules" item to "`core/` and `service/` import nothing from `apps.*`".
  - **`testing.md`**: the Keychain note (Decisions).
  - **`.gitignore`**: `gradle/gradle-daemon-jvm.properties` → `**/gradle/gradle-daemon-jvm.properties`.
  - Delete the untracked `apps/native/` (build outputs only) with `rm -rf apps/native`.
- Unit tests: none.
- Compile: none.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-6 KMP build has no native Android modules | 1, 3 | Manual: `./gradlew projects` lists nothing under `apps/native`; `apps/` holds only `cmp` (subtask "How to test" 1) |
| AC-6 CI checks pass without `native-android/` | 1 | `./gradlew detekt jvmTest koverVerify :architecture-test:test --continue` passes; no `native-android` task in its output (subtask "How to test" 2) |
| AC-6 docs describe the new setup | 2, 3 | Review: `docs/architecture.md` and `CLAUDE.md` describe `native-android/`, the publish command and how it gets the core; `grep -rn "apps/native" CLAUDE.md docs/architecture.md .claude/rules` is empty (subtask "How to test" 3) |

## Out of scope
- The standalone `native-ios/` app and its XCFramework publishing (not in this feature).
- The onboarding feature's native subtasks (already retargeted in the onboarding docs).
- Restricting `moduleTypeOf` to `:apps:cmp:` only; it stays generic.
- `docs/features/**` history that mentions `apps/native` (records of past decisions, not changed).

## Risks
- **A second in-build app could depend on `apps/cmp`** once the app → app rule is gone. Mitigation: the docs state
  that new apps are standalone consumers, and a new `apps/<x>` would be a reviewed architecture change (the
  rule can come back then).
- **Doc drift between `docs/architecture.md`, `CLAUDE.md` and the rules**. Mitigation: the `grep` check in
  Traceability, plus reading the three together in review.
