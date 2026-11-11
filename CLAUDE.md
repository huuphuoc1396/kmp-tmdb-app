# kmp-tmdb-app

Kotlin Multiplatform shared core for a TMDB discovery app (films, series, anime, novelas; en + pt-BR).
Everything lives on `main` in directories: shared core (`core/`, `service/`) and, once built, the apps under
`apps/cmp` and `apps/native`. Full design: `docs/architecture.md`.
Per-layer rules (build logic, tests, architecture tests) are in `.claude/rules/` and load by path.

Current state: the `main` skeleton. The `core/*` modules exist but have **no production code yet**, only a
`*SmokeTest` placeholder each. No `service/*` module exists yet: each context (`title`, `favorites`, `settings`) is
created by the first feature that needs it. New code follows `docs/architecture.md` (sections 5–8), not the
skeleton or existing examples.

## Repository layout
- One branch (`main`); apps are directories, not branches. `apps/cmp` (Compose Multiplatform: shared ViewModel + UI) and `apps/native` (Jetpack Compose + SwiftUI/SKIE) both build on `core/*` and `service/*`. Neither exists yet; each is created with its first screen.
- `apps/cmp` and `apps/native` never depend on each other.
- CI runs JVM checks only; app builds and platform tests run locally.

## Setup
- JDK 21 (toolchain from `jvmToolchain` in `gradle/libs.versions.toml`). Gradle wrapper only.
- Android SDK via `local.properties` (`sdk.dir`), git-ignored. compileSdk 36, minSdk 24, iOS 16.
- iOS targets need macOS + Xcode; `kotlin.native.ignoreDisabledTargets=true` lets JVM work proceed without them.
- Configuration cache and build cache are on (`gradle.properties`). Keep build logic config-cache safe.
- Secrets (TMDB token) go in git-ignored `secrets.properties`; not wired up yet (planned in the network task).

## Commands
`<module>` is a Gradle path such as `core:common` or `service:title:data`.
- All CI checks: `./gradlew detekt jvmTest koverVerify :architecture-test:test`
- One module's tests: `./gradlew :<module>:jvmTest` (root `jvmTest` also runs the `build-logic` unit tests)
- Platform tests (local only): iOS `./gradlew :<module>:iosSimulatorArm64Test` (macOS + Xcode); Android instrumented tests are not configured yet
- Static analysis (Detekt + ktlint rules): `./gradlew detekt` or `./gradlew :<module>:detekt`
- Auto-format (ktlint fixes): `./gradlew :<module>:detekt --auto-correct`
- Coverage: `./gradlew koverVerify` (80% line on every `service/*/domain` and `service/*/data`; the root project has a rule-less `koverVerify` so the command works with none); report: `:<module>:koverHtmlReport`
- Architecture rules: `./gradlew :architecture-test:test`
- Compile every target (Android, iOS, JVM; macOS + Xcode): `./gradlew assemble`

## Before finishing a code change
Applies only when source or build files changed. Docs-only changes (`*.md`): run nothing. Report results honestly.

Default (after every code change):
- `./gradlew :<module>:jvmTest :<module>:detekt` for each touched module (add `--auto-correct` first if ktlint fails).
- `./gradlew :architecture-test:test` if imports or module dependencies changed.
- `./gradlew koverVerify` if a coverage-gated module changed (`domain` or `data`).

Full (when the user asks, or before opening a PR):
- `./gradlew detekt jvmTest koverVerify :architecture-test:test --continue` (same as CI).
- `./gradlew assemble` when `expect`/`actual`, platform source sets or build-logic changed (needs macOS + Xcode; say if skipped).
- Platform tests for modules whose `androidMain` / `iosMain` changed (emulator or simulator; say if skipped).

## Architecture
- Three tiers: **Libraries** (`core/`), **Services** (`service/<context>/{domain,data,fake}`), **Features** (`apps/<app>/feature/<x>/{nav,api,ui}`). `core:di` and each app shell are the composition roots.
- Offline-first: UI ◄ ViewModel ◄ (use case) ◄ `Repository.observe()` ◄ SQLDelight (single source of truth). `Repository.refresh()` → Ktor → upsert into SQLDelight. The network never feeds the UI directly.
- The `service/*/domain` modules (repository interfaces, use cases, their models) are the public API of the core. Platforms never see `data`, `network` or `database`.
- Use cases are optional: create one only when it combines repositories, applies business rules, shapes data for a screen, or is reused by two features. Otherwise the ViewModel calls the repository directly. Never write a use case that only forwards.
- Errors map to `AppError` (Network, Unauthorized, NotFound, Unknown) wrapped in `AppResult`; an error never clears cached data.
- Refresh is stale-while-revalidate: lists store `fetchedAt`; TTL 6 h for home lists, 24 h for details.
- Cached content is stored per language (`pt-BR` / `en-US`). Favorites are local-only.
- Platform code goes through `expect`/`actual` (`DatabaseDriverFactory`, master key in `core:security`).
- `DatabaseDriverFactory.create(passphrase: ByteArray?)` keeps the passphrase parameter so SQLCipher can be added later.

## Modules
All shared modules apply only `tmdb.module`, which reads the module type from the path (`docs/architecture.md` 5.2)
and sets targets `android` (with host tests), `jvm`, `iosArm64`, `iosSimulatorArm64`, Detekt, and Kover for
`domain`/`data`. Modules marked *on demand* are created only when first needed (5.5).

| Module | Purpose | May depend on |
|---|---|---|
| `core:common` | `AppResult`, `AppError`, `DispatcherProvider` | — |
| `core:model` | Models shared by ≥ 2 contexts or a `nav` module (`TitleId`, `Title`, `Genre`, `Language`) | — |
| `core:security` | Master key (Keystore / Keychain / JVM fake), AES-GCM | `common`, `model` |
| `core:network` | Ktor client factory, auth, cert pinning, error mapping (no endpoints) | `security`, kernel |
| `core:database` | `DatabaseDriverFactory` (no tables; each `service/*/data` owns its own SQLDelight database) | `security`, kernel |
| `core:datastore` | DataStore KMP + encrypted serializer | `security`, kernel |
| `core:testing` | Test dispatchers | kernel |
| `core:di` | `initKoin()`, collects every `service/*/data` Koin module | everything |
| `service/<ctx>/domain` (on demand) | Context models, repository interfaces, use cases | kernel; another context's `domain` (one level, registered in `DependencyRules.domainAllowlist`) |
| `service/<ctx>/data` (on demand) | Repository impls, endpoints + DTOs, mappers, its own SQLDelight database, refresh policy, Koin module | own `domain`, kernel, `network`/`database`/`datastore` |
| `service/<ctx>/fake` (on demand) | In-memory repository fakes + repository contract test | own `domain`, kernel |
| `apps/<app>/feature/<x>/nav` (on demand) | Route, Args, Result; renamed `api` when it must expose an entry interface | `core:model` |
| `apps/<app>/feature/<x>/ui` | ViewModel + screen | `nav`/`api` (same app), `service/*/domain`, the app's `designsystem`, kernel |
| `architecture-test` | Konsist rules (JVM only) | — |

Contexts: `title` (home lists, details, cast, videos, genres, search), `favorites`, `settings` (language, onboarding).
Kernel = `core:common` + `core:model`. Test source sets may also use `service/*/fake` and `core:testing`.

Forbidden: `ui → ui`, `ui → data`, `data → data`, `fake → data`, `domain → data`/`fake`, `fake` outside tests.
Feature A needing something from feature B: service `domain` → the app's `designsystem` → a `@Composable` slot passed by the
app shell → an entry interface in B's `api` bound with Koin (5.4).

Enforced by `tmdb.module` (`build-logic/convention/.../buildlogic/DependencyRules.kt`): an unknown module path or a
forbidden `project(...)` dependency fails configuration with `Rule: <from> -> <to>: …`; the `domain → domain`
allowlist is `DependencyRules.domainAllowlist` (one level, checked). Enforced by Konsist (`architecture-test/`, over
`core/`, `service/`, `feature/`): `core:model`, `service/*/domain`, `feature/*/nav` import no frameworks (Ktor,
SQLDelight, `androidx.`, Compose, Koin, `android.`, `platform.`); `domain` imports no `data`/`fake`/infrastructure/`di`;
`network`/`database`/`datastore` import no `service`/`di`; feature `ui` imports no other `ui` and no `data`; every
file uses the package of its path (`service/title/domain` → `com.banhmilabs.tmdb.service.title.domain`).

## Conventions
- New shared module: create it at its architecture path, add it to `settings.gradle.kts`, and apply only `alias(libs.plugins.tmdb.module)`. Type, namespace (`:service:title:domain` → `com.banhmilabs.tmdb.service.title.domain`), Kover and the dependency check come from the path; don't set them by hand. Konsist picks it up automatically.
- Leaf names repeat (`domain`, `data`, `ui`); `tmdb.module` keeps klib names unique by setting `group` from the path.
- A model goes in `core:model` only when two or more contexts (or a `nav` module) need it; otherwise it stays in its context's `domain`.
- A use case is a class with `operator fun invoke`, no interface. Repository interfaces expose only what consumers need; `upsert`-style operations stay in `data`.
- Coverage (80% line, DI/`*Dto`/generated excluded) is applied by `tmdb.module` to `domain` and `data` modules only.
- Module dependencies use type-safe accessors: `implementation(projects.service.title.domain)`.
- Every dependency and plugin version goes in `gradle/libs.versions.toml`; shared build config goes in a convention plugin in `build-logic/convention`.
- Write unit tests in `jvmTest` by default (fast, MockK available). `commonTest` is allowed for tests that use only multiplatform libraries (no MockK) and are worth running on Kotlin/Native too (concurrency, `Flow` timing, formatting, parsing); they also run in `jvmTest`. Tools: `kotlin.test`, Turbine, Kotest assertions; Ktor `MockEngine` + fixture JSON for network; real SQLDelight in-memory `JdbcSqliteDriver` for database.
- Test doubles: across modules use the repository fakes from `service/<ctx>/fake` (each kept honest by a contract test that both `data` and `fake` extend); MockK for collaborators in the same module and for `verify`. No other hand-written fakes, no Mokkery.
- Platform `actual` code is tested on its platform, locally: Android instrumented tests; iOS `iosTest` with `kotlin.test` and no mocks.
- Inject dispatchers via `DispatcherProvider` (`core:common`) and time via `kotlin.time.Clock`.
- Max line length 120 (`config/detekt/detekt.yml`).
- Commits: short imperative subject, no prefix (`Split CI checks into separate steps`).
- Feature work is specced in `docs/features/<feature>/task.md` with an `implement-plan.md` alongside.

## Reuse before you create
| Need | Use | Module |
|---|---|---|
| Result / error type | `AppResult`, `AppError` | `core:common` |
| Coroutine dispatchers | `DispatcherProvider` | `core:common` |
| Current time | `kotlin.time.Clock` (injected) | stdlib |
| Shared IDs and models | `TitleId`, `Title`, `Genre`, `Language` | `core:model` |
| Test dispatchers | `core:testing` | `core:testing` |
| Repository test double | `Fake<Repository>` | `service/<ctx>/fake` |
| Module build setup | `tmdb.module` (shared modules), `tmdb.detekt` (JVM modules) | `build-logic/convention` |

(Planned in `docs/architecture.md`; not yet implemented — create them in these modules when first needed.)

## Do not
- Don't use MockK or JVM-only APIs in `commonTest`; such tests go in `jvmTest`.
- Don't write ad-hoc `Fake*` classes in test code; add the fake to `service/<ctx>/fake` with its contract test, or use MockK.
- Don't use MockK in iOS or Android instrumented tests (JVM-only); test against the real platform API.
- Don't mock the database or HTTP client; use the in-memory SQLDelight driver and Ktor `MockEngine`.
- Don't let a feature `ui` depend on another feature's `ui` or on any `service/*/data`.
- Don't add a `domain → domain` dependency without adding the pair to `DependencyRules.domainAllowlist` (one level only) with its reason.
- Don't create a use case that only forwards to a repository.
- Don't feed network results to callers directly; upsert into SQLDelight and read through `observe()`.
- Don't use `EncryptedSharedPreferences` (deprecated); use `core:datastore` with the AES-GCM serializer.
- Don't put the TMDB token in source or committed files; use `secrets.properties` / CI secrets.
- Don't log request/response bodies in release builds.
- Don't make `apps/cmp` and `apps/native` depend on each other, or put UI in `core/` or `service/`.
- Don't hardcode versions or SDK levels in module build files; read them from `libs.versions.toml`.
- Don't add `GlobalScope` or hardcoded `Dispatchers.*`; inject `DispatcherProvider`.
