# kmp-tmdb-app

Kotlin Multiplatform shared core for a TMDB discovery app (films, series, anime, novelas; en + pt-BR).
Everything lives on `main` in directories: shared core (`core/`, `service/`), the Compose Multiplatform app
`cmp/`, and the standalone native apps `native-android/` and `native-ios/`.
Full design: `docs/architecture.md`.
Per-layer rules (build logic, tests, architecture tests) are in `.claude/rules/` and load by path.

Current state: early. Production code exists in `core:common` (`AppResult`, `AppError`, `DispatcherProvider`),
`core:security` (master key cipher), `core:datastore` (encrypted DataStore), `core:network` (TMDB client,
`apiCall`), `core:di` (`initKoin()`) and the
`settings` context (`service/settings/{domain,data,fake}`, onboarding state), `core:testing` (`MainDispatcherTest`)
and the `cmp/` onboarding feature (`cmp/feature/onboarding/ui`). The other `core/*` modules are still
skeletons with a `*SmokeTest` placeholder. Contexts `title` and `favorites` are created by the first feature that
needs them. New code follows `docs/architecture.md` (sections 5–8), not the
skeleton or existing examples.

## Repository layout
- One branch (`main`); apps are directories, not branches. The KMP build holds the shared core and `cmp/` (Compose Multiplatform: shared ViewModel + UI), which has its shell (`designsystem`, `composeApp`, `androidApp`, Xcode `iosApp`), the onboarding screen and a placeholder Home.
- `native-android/` is a standalone Gradle project (own settings, wrapper, catalog, Detekt; one `:app` module, Jetpack Compose) that is not part of the KMP build. It uses the shared core only through the libraries published to `build/maven-repo` (`com.banhmilabs.tmdb:core-di`, `service-<ctx>-domain`), like an outside SDK consumer. See `native-android/README.md`.
- `native-ios/` is a standalone Xcode project (SwiftUI, iOS 16, one app target plus a test target hosted by the app) that is not part of the KMP build. It uses the shared core only through the local Swift Package `build/swift-package/TmdbCore` (`SharedCore` entry point, SKIE interop). Swift ViewModels are `ObservableObject`s (no `@Observable`, which needs iOS 17). See `native-ios/README.md`.
- `native-*` never depends on `cmp/`, and the KMP build never includes or references `native-*`.
- CI runs on Ubuntu (static analysis, Android host tests, coverage, architecture tests); app builds and platform tests run locally.

## Setup
- JDK 21 (toolchain from `jvmToolchain` in `gradle/libs.versions.toml`). Gradle wrapper only.
- Android SDK via `local.properties` (`sdk.dir`), git-ignored. compileSdk 37, targetSdk 37, minSdk 24, iOS 16.
- iOS targets need macOS + Xcode; `kotlin.native.ignoreDisabledTargets=true` lets Android work proceed without them.
- Configuration cache and build cache are on (`gradle.properties`). Keep build logic config-cache safe.
- TMDB API Read Access Token: `TMDB_ACCESS_TOKEN` in git-ignored `secrets.properties` (copy `secrets.properties.example`),
  or the `TMDB_ACCESS_TOKEN` environment variable on CI. It is injected XOR-obfuscated at build time; without it the build
  warns and TMDB calls fail with Unauthorized.

## Commands
`<module>` is a Gradle path such as `core:common` or `service:title:data`.
- All CI checks: `./gradlew detekt testAndroidHostTest koverVerify :architecture-test:test`
- One module's tests: `./gradlew :<module>:testAndroidHostTest` (root `testAndroidHostTest` also runs the `build-logic` unit tests)
- Platform tests (local only): iOS `./gradlew :<module>:iosSimulatorArm64Test` (macOS + Xcode); Android `./gradlew :<module>:connectedAndroidDeviceTest` (emulator/device). Both also run `commonTest`
- Static analysis (Detekt + ktlint rules): `./gradlew detekt` or `./gradlew :<module>:detekt`
- Auto-format (ktlint fixes): `./gradlew :<module>:detekt --auto-correct`
- Coverage: `./gradlew koverVerify` (80% line on every `service/*/domain` and `service/*/data`; the root project has a rule-less `koverVerify` so the command works with none); report: `:<module>:koverHtmlReport`
- Architecture rules: `./gradlew :architecture-test:test`
- Compile every target (Android, iOS; macOS + Xcode): `./gradlew assemble`
- Publish the shared core: `./gradlew publishSharedCore` writes every production module of `core/` and `service/` that has code (not `fake`, `core:testing` or app modules) to `build/maven-repo`, as `com.banhmilabs.tmdb:<path-with-dashes>:0.1.0-SNAPSHOT` (e.g. `core-di`, `service-settings-domain`). All targets; iOS klibs need macOS + Xcode. Republishing replaces the snapshot. On macOS it also writes the iOS Swift Package `build/swift-package/TmdbCore` (`Package.swift` + a dynamic release XCFramework `TmdbCore`, bundle ID `com.banhmilabs.tmdb.TmdbCore`, slices `iosArm64` and `iosSimulatorArm64`, with SKIE) from `core:ios`, which is not on Maven.
- Native Android app (standalone; publish first, then from `native-android/`): `./gradlew publishSharedCore`, then `cd native-android && ./gradlew :app:installDebug`. Checks: `./gradlew :app:detekt :app:testDebugUnitTest` (in `native-android/`). Not run by CI.
- Native iOS app (standalone; macOS + Xcode; publish first, then from `native-ios/`): `./gradlew publishSharedCore`, then `xcodebuild -project TmdbNative.xcodeproj -scheme TmdbNative -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`. Tests: `xcodebuild -project TmdbNative.xcodeproj -scheme TmdbNative -destination 'platform=iOS Simulator,name=iPhone 17' test`. Not run by CI.
- Run the cmp app: Android `./gradlew :cmp:androidApp:installDebug` (emulator/device); iOS: open `cmp/iosApp/iosApp.xcodeproj` in Xcode, or build with `xcodebuild -project cmp/iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`. The Xcode project repeats the iOS deployment target (16.0) because it can't read the version catalog.

## Before finishing a code change
Applies only when source or build files changed. Docs-only changes (`*.md`): run nothing. Report results honestly.

Default (after every code change):
- `./gradlew :<module>:testAndroidHostTest :<module>:detekt` for each touched module (add `--auto-correct` first if ktlint fails).
- `./gradlew :architecture-test:test` if imports or module dependencies changed.
- `./gradlew koverVerify` if a coverage-gated module changed (`domain` or `data`).

Full (when the user asks, or before opening a PR):
- `./gradlew detekt testAndroidHostTest koverVerify :architecture-test:test --continue` (same as CI).
- `./gradlew assemble` when `expect`/`actual`, platform source sets or build-logic changed (needs macOS + Xcode; say if skipped).
- Platform tests for modules whose `androidMain` / `iosMain` changed (emulator or simulator; say if skipped).

## Architecture
- Three tiers: **Libraries** (`core/`), **Services** (`service/<context>/{domain,data,fake}`), **Features** (`cmp/feature/<x>/{nav,api,ui}`). `core:di` and each app shell are the composition roots.
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
and sets targets `android` (with host and device tests), `iosArm64`, `iosSimulatorArm64` (no JVM target), Detekt, and Kover for
`domain`/`data`. Modules marked *on demand* are created only when first needed (5.5).

| Module | Purpose | May depend on |
|---|---|---|
| `core:common` | `AppResult`, `AppError`, `DispatcherProvider` | — |
| `core:model` | Models shared by ≥ 2 contexts or a `nav` module (`TitleId`, `Title`, `Genre`, `Language`) | — |
| `core:security` | Master key (Keystore / Keychain), AES-GCM | `common`, `model` |
| `core:network` | Ktor client factory, auth, cert pinning, error mapping (no endpoints) | `security`, kernel |
| `core:database` | `DatabaseDriverFactory` (no tables; each `service/*/data` owns its own SQLDelight database) | `security`, kernel |
| `core:datastore` | DataStore KMP + encrypted serializer | `security`, kernel |
| `core:testing` | Test dispatchers | kernel |
| `core:di` | `initKoin()`, collects every `service/*/data` Koin module | everything |
| `core:ios` | iOS entry point `SharedCore` (start, repositories); exports its `api` dependencies (kernel, service domains) in the `TmdbCore` XCFramework, with SKIE | the shared core; nothing depends on it |
| `service/<ctx>/domain` (on demand) | Context models, repository interfaces, use cases | kernel; another context's `domain` (one level, registered in `DependencyRules.domainAllowlist`) |
| `service/<ctx>/data` (on demand) | Repository impls, endpoints + DTOs, mappers, its own SQLDelight database, refresh policy, Koin module | own `domain`, kernel, `network`/`database`/`datastore` |
| `service/<ctx>/fake` (on demand) | In-memory repository fakes + repository contract test | own `domain`, kernel |
| `cmp/composeApp` | `App()`, navigation graph; builds the iOS framework `ComposeApp` (root of the app) | its own app, the shared core |
| `cmp/androidApp` | Android application: `Application` (starts `initKoin`), `Activity` (root of the app) | its own app, the shared core |
| `cmp/feature/<x>/nav` (on demand) | Route, Args, Result; renamed `api` when it must expose an entry interface | `core:model` |
| `cmp/feature/<x>/ui` | ViewModel + screen | `nav`/`api` (same app), `service/*/domain`, the app's `designsystem`, kernel |
| `architecture-test` | Konsist rules (JVM only) | — |

Contexts: `title` (home lists, details, cast, videos, genres, search), `favorites`, `settings` (language, onboarding).
Kernel = `core:common` + `core:model`. Test source sets may also use `service/*/fake` and `core:testing`.

Forbidden: `ui → ui`, `ui → data`, `data → data`, `fake → data`, `domain → data`/`fake`, `fake` outside tests.
Feature A needing something from feature B: service `domain` → the app's `designsystem` → a `@Composable` slot passed by the
app shell → an entry interface in B's `api` bound with Koin (5.4).

Enforced by `tmdb.module` (`build-logic/convention/.../buildlogic/DependencyRules.kt`): an unknown module path or a
forbidden `project(...)` dependency fails configuration with `Rule: <from> -> <to>: …`; the `domain → domain`
allowlist is `DependencyRules.domainAllowlist` (one level, checked); the shared core never depends on an app. Enforced by Konsist (`architecture-test/`, over
`core/`, `service/`, `cmp/`): `core:model`, `service/*/domain`, `cmp/feature/*/nav` import no frameworks (Ktor,
SQLDelight, `androidx.`, Compose, Koin, `android.`, `platform.`); `domain` imports no `data`/`fake`/infrastructure/`di`;
`network`/`database`/`datastore` import no `service`/`di`; feature `ui` imports no other `ui` and no `data`; every
file uses the package of its path (`service/title/domain` → `com.banhmilabs.tmdb.service.title.domain`).

## Conventions
- New shared module: create it at its architecture path, add it to `settings.gradle.kts`, and apply only `alias(libs.plugins.tmdb.module)`. Type, namespace (`:service:title:domain` → `com.banhmilabs.tmdb.service.title.domain`), Kover and the dependency check come from the path; don't set them by hand. Konsist picks it up automatically.
- Leaf names repeat (`domain`, `data`, `ui`); `tmdb.module` keeps klib names unique by setting `group` from the path.
- A model goes in `core:model` only when two or more contexts (or a `nav` module) need it; otherwise it stays in its context's `domain`.
- `cmp/` ViewModels follow MVI (`docs/architecture.md` 11): `<Feature>UiState` data class as `StateFlow`, `<Feature>Intent` sealed interface handled by `onIntent`, `<Feature>Effect` sealed interface as a `Flow` from a buffered `Channel`; stateful `<Feature>Screen` (gets the ViewModel, collects state and effects) draws stateless `<Feature>Content(state, onIntent)`; navigation only calls the screen. Packages by screen, not by layer: `navigation/` and `di/` fixed; contract, ViewModel and screen at the module root with `component/`; one package per screen once there are two. Reference: `cmp/feature/onboarding/ui`.
- A use case is a class with `operator fun invoke`, no interface. Repository interfaces expose only what consumers need; `upsert`-style operations stay in `data`.
- Coverage (80% line, DI/`*Dto`/generated excluded) is applied by `tmdb.module` to `domain` and `data` modules only.
- Module dependencies use type-safe accessors: `implementation(projects.service.title.domain)`.
- Every dependency and plugin version goes in `gradle/libs.versions.toml`; shared build config goes in a convention plugin in `build-logic/convention`.
- Write unit tests in `androidHostTest` by default (Android host tests on the JVM: fast, MockK available). Tests that need the real Keystore go in `androidDeviceTest`. `commonTest` is allowed for tests that use only multiplatform libraries (no MockK) and are worth running on Kotlin/Native too (concurrency, `Flow` timing, formatting, parsing); they run on the Android host, on a device and on the iOS simulator. Tools: `kotlin.test`, Turbine, Kotest assertions; Ktor `MockEngine` + fixture JSON for network; real SQLDelight in-memory `JdbcSqliteDriver` for database.
- Test doubles: across modules use the repository fakes from `service/<ctx>/fake` (each kept honest by a contract test that both `data` and `fake` extend); MockK for collaborators in the same module and for `verify`. No other hand-written fakes, no Mokkery.
- Host tests never call `masterKeyCipher(…)`; they pass a MockK cipher that returns its input (encryption itself is tested on a device).
- Platform `actual` code is tested on its platform, locally: Android device tests (`androidDeviceTest`); iOS `iosTest` with `kotlin.test` and no mocks. Kotlin iOS tests have no app bundle, so the Keychain path is covered by the `native-ios` Swift tests.
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
| Encrypted local settings | `encryptedDataStore(...)`, `masterKeyCipher(alias)` | `core:datastore`, `core:security` |
| TMDB HTTP client / error mapping | `createTmdbHttpClient()`, `apiCall { }` | `core:network` |
| Koin start-up | `initKoin(appDeclaration)` | `core:di` |
| Test Main dispatcher (ViewModel tests) | extend `MainDispatcherTest` | `core:testing` |
| Compose resources (strings, drawables) | `composeResources/` in the module; `Res` is in `<namespace>.generated.resources` | `cmp/*` (set by `tmdb.module`) |
| Repository test double | `Fake<Repository>` (e.g. `FakeSettingsRepository`) | `service/<ctx>/fake` |
| Module build setup | `tmdb.module` (shared modules), `tmdb.detekt` (JVM modules) | `build-logic/convention` |

(`core:model` types and `kotlin.time.Clock` injection are planned but not implemented yet —
create them in these modules when first needed.)

## Do not
- Don't use MockK or JVM-only APIs in `commonTest`; such tests go in `androidHostTest`.
- Don't call the real Keystore or Keychain from a host test; use a MockK cipher, or write a device test.
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
- Don't make `native-android/` depend on `cmp/` or reference the KMP build (no `includeBuild`, `project(...)` or source paths); it uses only the published shared core.
- Don't put UI in `core/` or `service/`.
- Don't hardcode versions or SDK levels in module build files; read them from `libs.versions.toml`.
- Don't add `GlobalScope` or hardcoded `Dispatchers.*`; inject `DispatcherProvider`.
