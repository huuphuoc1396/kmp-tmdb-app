# Plan: JVM test strategy

Requirement: [task.md](task.md)
Status: Done

## Summary
Move the ten `*SmokeTest` placeholders from `commonTest` to `jvmTest`. Then change `KmpLibraryConventionPlugin` so test dependencies (`kotlin-test`, MockK) go to `jvmTest` instead of `commonTest`, and remove the Android host test. After that, every shared module writes unit tests the same way with no per-module setup. Platform test setup and Konsist enforcement are left out on purpose (see Decisions). The docs (`docs/architecture.md`, `CLAUDE.md`) already describe the target state; only the transitional note in `CLAUDE.md` is removed at the end.

## Decisions
- No Konsist rule for the test strategy — user's choice; documentation and review are enough. (Rejected: rules banning `src/commonTest/` files and `io.mockk` imports outside `jvmTest`.) Resolves the open question in task.md.
- Platform test setup (iOS `iosTest` dependencies, Android `withDeviceTest` + `androidx.test` runner) is deferred to the first task that adds `androidMain` / `iosMain` code — nothing to test yet, and no speculative hooks. (Rejected: configuring it now.)
- Remove `withHostTest {}` from `KmpLibraryConventionPlugin` — once `commonTest` is empty it would only run an empty `testAndroidHostTest`; unit tests already run on `jvm`. (Rejected: keeping it for future Robolectric tests.)
- `tmdb.kmp.library` adds `kotlin-test` and MockK to `jvmTest` of every shared module — one setup for all modules. (Rejected: each module declares MockK itself.)
- Keep the default JVM test runner that `kotlin-test` selects today (JUnit 4 via `kotlin-test-junit`) — moving tests between source sets should not also change the runner; `architecture-test` keeps its own JUnit Platform setup.
- MockK version: the latest stable `io.mockk:mockk` on Maven Central when `forge` runs, pinned in `gradle/libs.versions.toml` (same policy as the main skeleton). Not in the local Gradle cache, so not fixed here.
- Turbine and Kotest assertions are not added now — no production code needs them yet; add them with the first test that does.
- One MockK test is added to `CommonSmokeTest` to prove the wiring (AC-2); it is replaced together with the smoke test when `core:common` gets code.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Shared module build setup | `KmpLibraryConventionPlugin` (`tmdb.kmp.library`) | `build-logic/convention/src/main/kotlin/KmpLibraryConventionPlugin.kt` |
| Catalog lookup in convention plugins | `Project.libs`, `VersionCatalog.version()` | `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/ProjectExtensions.kt` |
| Existing placeholder tests | `*SmokeTest` (one per module) | `core/<module>/src/commonTest/kotlin/com/banhmilabs/tmdb/core/<module>/` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| all 10 `core/*` | `src/commonTest/.../<Module>SmokeTest.kt` → `src/jvmTest/.../<Module>SmokeTest.kt` (move) | AC-1 |
| `core:common` | `src/jvmTest/.../CommonSmokeTest.kt` (modify) | AC-2 wiring check |
| `gradle` | `gradle/libs.versions.toml` (modify) | MockK in the catalog |
| `build-logic/convention` | `KmpLibraryConventionPlugin.kt` (modify) | Test deps on `jvmTest`; no host test |
| docs | `CLAUDE.md`, `docs/features/jvm-test-strategy/task.md` (modify) | Drop transitional note; close the open question |

## Steps
### Step 1: Move smoke tests to jvmTest  (AC-1, AC-4) ✅
- Files: for each module in `common, model, domain, network, database, datastore, security, data, di, testing`: `git mv core/<m>/src/commonTest/kotlin/com/banhmilabs/tmdb/core/<m>/<M>SmokeTest.kt core/<m>/src/jvmTest/kotlin/com/banhmilabs/tmdb/core/<m>/<M>SmokeTest.kt`. Remove the empty `src/commonTest/` directories.
- What: path change only; package and content unchanged (package rule in `PackageRulesTest` still holds). The project still compiles because `jvmTest` inherits `kotlin-test` from `commonTest` until Step 2.
- Unit tests: the moved `*SmokeTest` classes.
- Compile: `./gradlew compileTestKotlinJvm`

### Step 2: Test dependencies on jvmTest, MockK, no host test  (AC-1, AC-2, AC-4) ✅
- Files: `gradle/libs.versions.toml` (modify), `build-logic/convention/src/main/kotlin/KmpLibraryConventionPlugin.kt` (modify), `core/common/src/jvmTest/kotlin/com/banhmilabs/tmdb/core/common/CommonSmokeTest.kt` (modify)
- What:
  - Catalog: `[versions] mockk = "<latest stable>"`; `[libraries] mockk = { module = "io.mockk:mockk", version.ref = "mockk" }`.
  - Plugin:
    - Remove `withHostTest {}` and its comment.
    - Replace the `commonTest` dependency block with `jvmTest`:
      ```kotlin
      sourceSets.getByName("jvmTest").dependencies {
          implementation(libs.findLibrary("kotlin-test").get())
          implementation(libs.findLibrary("mockk").get())
      }
      ```
    - Update the KDoc: unit tests run on `jvmTest` with `kotlin-test` + MockK.
  - `CommonSmokeTest`: add `mockkCreatesMocks()` — mock a private test-only `fun interface Greeter { fun greet(): String }`, `every { greet() } returns "hi"`, assert the result and `verify { greet() }`.
- Unit tests: `CommonSmokeTest.testSourceSetRuns`, `CommonSmokeTest.mockkCreatesMocks`; the other nine smoke tests.
- Compile: `./gradlew compileTestKotlinJvm`, and on macOS + Xcode `./gradlew assemble` (all targets still compile without `commonTest` deps and host test).

### Step 3: Docs  (AC-9) ✅
- Files: `CLAUDE.md` (modify), `docs/features/jvm-test-strategy/task.md` (modify)
- What:
  - `CLAUDE.md`: remove the two sentences that start "Test strategy is moving to JVM-only unit tests…" and "Write new tests the new way"; keep the `docs/architecture.md (sections 4–6, 8)` pointer.
  - `task.md`: tick the open question with the answer "No Konsist rule (documentation and review)"; add a Change log line `- <date>: Open question resolved: no Konsist rule.`
- Unit tests: none (docs).
- Compile: none.

### Final verification (after Step 3)
- `./gradlew detekt jvmTest koverVerify :architecture-test:test --continue`
- `./gradlew :core:common:tasks --all | grep -c testAndroidHostTest` prints `0`.
- `find core -path '*/src/commonTest/*' -type f` prints nothing.
- `./gradlew assemble` on macOS + Xcode (report if skipped).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 jvmTest only | 1, 2 | Moved `*SmokeTest`; `find` check finds no `commonTest` files |
| AC-2 MockK only | 2 | `CommonSmokeTest.mockkCreatesMocks`; manual: no Mokkery in catalog |
| AC-3 no fakes in `core:testing` | — (already true) | Manual: `core/testing` holds only `TestingSmokeTest` |
| AC-4 one JVM command | 1, 2 | `./gradlew jvmTest` runs all 10 smoke tests; `.github/workflows/ci.yml` unchanged |
| AC-5 coverage unchanged | 2 | `./gradlew koverVerify` passes; `KoverConventionPlugin` unchanged |
| AC-6 platform tests local | — (deferred, see Decisions) | Manual: documented in `docs/architecture.md` §8 and `CLAUDE.md` |
| AC-7 `cmp` rule | — (branch not started) | Manual: documented in `docs/architecture.md` §8 |
| AC-8 `native` Android MockK | — (branch not started) | Manual: documented in `docs/architecture.md` §8 |
| AC-9 docs match | 3 (+ done during spark) | Manual review of `docs/architecture.md` §3, 4.1, 8–10 and `CLAUDE.md` |
| EC-1 Native/JVM differences | — | Accepted trade-off, documented in §8 |
| EC-2 network via `MockEngine` | — | Documented; applied when `core:network` gets code |
| EC-3 real in-memory database | — | Documented; applied when `core:database` gets code |
| EC-4 no MockK in platform tests | 2 (MockK only on `jvmTest`) | Manual: `mockk` appears only in the `jvmTest` block of the plugin |
| EC-5 no emulator / Xcode | — | Manual: CI stays JVM-only; `assemble` skip is reported |

## Out of scope
- Platform test setup (`iosTest` dependencies, Android `withDeviceTest`): first task that adds `androidMain` / `iosMain` code.
- Turbine and Kotest assertions: first test that needs them.
- Konsist enforcement of the test strategy: rejected (see Decisions).
- `cmp` and `native` branches: they follow `docs/architecture.md` §8 when created.

## Risks
- The latest MockK may need a newer Kotlin or JDK than the project uses (Kotlin 2.4.21, JDK 21) — `forge` checks MockK's release notes and picks the newest version that compiles; record the version in the plan.
- `kotlin.test` runner inference: with `kotlin-test` declared only on `jvmTest`, KGP must still pick the JUnit 4 variant — Step 2 compile plus `jvmTest` show it; if no tests are found, add `kotlin-test-junit` explicitly.
- Removing `withHostTest` changes the Android variant's tasks — `./gradlew assemble` in Step 2 confirms the Android target still builds.

## Deviations
- None in scope. MockK pinned to `1.14.12` (latest stable on Maven Central on 2026-11-10); it compiles with Kotlin 2.4.21 / JDK 21, and `kotlin-test` on `jvmTest` still resolves the JUnit 4 runner (all 11 tests ran).
