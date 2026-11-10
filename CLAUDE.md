# kmp-tmdb-app

Kotlin Multiplatform shared core for a TMDB discovery app (films, series, anime, novelas; en + pt-BR).
`main` holds only the shared core: no app module, no UI. Full design: `docs/architecture.md`.
Per-layer rules (build logic, tests, architecture tests) are in `.claude/rules/` and load by path.

Current state: the `main` skeleton. All `core/*` modules exist but have **no production code yet**, only a
`*SmokeTest` placeholder each. New code follows `docs/architecture.md` (sections 4–6, 8), not existing examples.

## Branches
- `main`: shared core + build-logic + docs + CI. `cmp` (Compose Multiplatform) and `native` (Jetpack Compose + SwiftUI/SKIE) branch from it.
- Change `build-logic/` and `core/*` on `main` first, then merge into `cmp` / `native`. Never merge `cmp` ↔ `native` or back into `main`.

## Setup
- JDK 21 (toolchain from `jvmToolchain` in `gradle/libs.versions.toml`). Gradle wrapper only.
- Android SDK via `local.properties` (`sdk.dir`), git-ignored. compileSdk 36, minSdk 24, iOS 16.
- iOS targets need macOS + Xcode; `kotlin.native.ignoreDisabledTargets=true` lets JVM work proceed without them.
- Configuration cache and build cache are on (`gradle.properties`). Keep build logic config-cache safe.
- Secrets (TMDB token) go in git-ignored `secrets.properties`; not wired up yet (planned in the network task).

## Commands
- All CI checks: `./gradlew detekt jvmTest koverVerify :architecture-test:test`
- One module's tests: `./gradlew :core:<module>:jvmTest`
- Platform tests (local only): iOS `./gradlew :core:<module>:iosSimulatorArm64Test` (macOS + Xcode); Android instrumented tests are not configured yet
- Static analysis (Detekt + ktlint rules): `./gradlew detekt` or `./gradlew :core:<module>:detekt`
- Auto-format (ktlint fixes): `./gradlew :core:<module>:detekt --auto-correct`
- Coverage: `./gradlew koverVerify` (only `core:domain`, `core:data`); report: `:core:<module>:koverHtmlReport`
- Architecture rules: `./gradlew :architecture-test:test`
- Compile every target (Android, iOS, JVM; macOS + Xcode): `./gradlew assemble`

## Before finishing a code change
Applies only when source or build files changed. Docs-only changes (`*.md`): run nothing. Report results honestly.

Default (after every code change):
- `./gradlew :core:<module>:jvmTest :core:<module>:detekt` for each touched module (add `--auto-correct` first if ktlint fails).
- `./gradlew :architecture-test:test` if imports or module dependencies changed.
- `./gradlew koverVerify` if `core:domain` or `core:data` changed.

Full (when the user asks, or before opening a PR):
- `./gradlew detekt jvmTest koverVerify :architecture-test:test --continue` (same as CI).
- `./gradlew assemble` when `expect`/`actual`, platform source sets or build-logic changed (needs macOS + Xcode; say if skipped).
- Platform tests for modules whose `androidMain` / `iosMain` changed (emulator or simulator; say if skipped).

## Architecture
- Offline-first: UI ◄ ViewModel ◄ use case ◄ `Repository.observe()` ◄ SQLDelight (single source of truth). `Repository.refresh()` → Ktor → upsert into SQLDelight. The network never feeds the UI directly.
- Use cases in `core:domain` are the public API of the core (e.g. `ObserveHomeFeed`, `RefreshHomeFeed`, `ToggleFavorite`). Platforms consume the core only through them.
- Errors map to `AppError` (Network, Unauthorized, NotFound, Unknown) wrapped in `AppResult`; an error never clears cached data.
- Refresh is stale-while-revalidate: lists store `fetchedAt`; TTL 6 h for home lists, 24 h for details.
- Cached content is stored per language (`pt-BR` / `en-US`). Favorites are local-only.
- Platform code goes through `expect`/`actual` (`DatabaseDriverFactory`, master key in `core:security`).
- `DatabaseDriverFactory.create(passphrase: ByteArray?)` keeps the passphrase parameter so SQLCipher can be added later.

## Modules
All shared modules use `tmdb.kmp.library` → targets `android` (with host tests), `jvm`, `iosArm64`, `iosSimulatorArm64`.

| Module | Purpose | May depend on |
|---|---|---|
| `core:common` | `AppResult`, `AppError`, `DispatcherProvider`, `Clock` | — |
| `core:model` | Pure Kotlin models (`Title`, `TitleDetails`, `Cast`, `Genre`, `Category`, `Video`) | nothing in the app |
| `core:domain` | Repository interfaces + use cases | `model`, `common` (as `api`) |
| `core:security` | Master key (Keystore / Keychain / JVM fake), AES-GCM | — |
| `core:network` | Ktor client, DTOs, TMDB API, auth, cert pinning | `security` |
| `core:database` | SQLDelight schema, `DatabaseDriverFactory` | `security` |
| `core:datastore` | DataStore KMP + encrypted serializer | `security` |
| `core:data` | Offline-first repository impls, mappers, refresh policy | `domain`, `network`, `database`, `datastore` |
| `core:di` | Koin modules + `initKoin()` | `data` |
| `core:testing` | Fixture JSON, test dispatchers (no fakes) | `domain` (as `api`) |
| `architecture-test` | Konsist rules (JVM only) | — |

Enforced by Konsist (`architecture-test/`): `model`/`domain` import no frameworks (Ktor, SQLDelight, `androidx.`, Compose, Koin, `android.`, `platform.`); `domain` imports no `data`/`network`/`database`/`datastore`/`security`/`di`; `network`/`database`/`datastore` import no `data`/`di`; every file in `core/<module>` uses package `com.banhmilabs.tmdb.core.<module>`.

## Conventions
- New shared module: add to `settings.gradle.kts`, apply `alias(libs.plugins.tmdb.kmp.library)`. Namespace is derived from the path (`:core:foo` → `com.banhmilabs.tmdb.core.foo`); don't set it by hand. Add the module name to `PackageRulesTest`.
- Apply `alias(libs.plugins.tmdb.kover)` only where coverage is enforced (80% line, DI/`*Dto`/generated excluded).
- Module dependencies use type-safe accessors: `implementation(projects.core.domain)`.
- Every dependency and plugin version goes in `gradle/libs.versions.toml`; shared build config goes in a convention plugin in `build-logic/convention`.
- Write unit tests in `jvmTest` only, never `commonTest`: `kotlin.test`, MockK, Turbine, Kotest assertions; Ktor `MockEngine` + fixture JSON for network; real SQLDelight in-memory `JdbcSqliteDriver` for database.
- Test doubles: MockK only. No hand-written fakes, no Mokkery. Fixtures and test dispatchers come from `core:testing`.
- Platform `actual` code is tested on its platform, locally: Android instrumented tests; iOS `iosTest` with `kotlin.test` and no mocks.
- Inject dispatchers via `DispatcherProvider` and time via `Clock` from `core:common`.
- Max line length 120 (`config/detekt/detekt.yml`).
- Commits: short imperative subject, no prefix (`Split CI checks into separate steps`).
- Feature work is specced in `docs/features/<feature>/task.md` with an `implement-plan.md` alongside.

## Reuse before you create
| Need | Use | Module |
|---|---|---|
| Result / error type | `AppResult`, `AppError` | `core:common` |
| Coroutine dispatchers, current time | `DispatcherProvider`, `Clock` | `core:common` |
| Fixture JSON, test dispatchers | `core:testing` | `core:testing` |
| Module build setup | `tmdb.kmp.library`, `tmdb.kover`, `tmdb.detekt` | `build-logic/convention` |

(Planned in `docs/architecture.md`; not yet implemented — create them in these modules when first needed.)

## Do not
- Don't put tests in `commonTest` or write hand-written fakes; write them in `jvmTest` with MockK.
- Don't use MockK in iOS or Android instrumented tests (JVM-only); test against the real platform API.
- Don't mock the database or HTTP client; use the in-memory SQLDelight driver and Ktor `MockEngine`.
- Don't feed network results to callers directly; upsert into SQLDelight and read through `observe()`.
- Don't use `EncryptedSharedPreferences` (deprecated); use `core:datastore` with the AES-GCM serializer.
- Don't put the TMDB token in source or committed files; use `secrets.properties` / CI secrets.
- Don't log request/response bodies in release builds.
- Don't add an app module or UI on `main`; those belong on `cmp` / `native`.
- Don't hardcode versions or SDK levels in module build files; read them from `libs.versions.toml`.
- Don't add `GlobalScope` or hardcoded `Dispatchers.*`; inject `DispatcherProvider`.
