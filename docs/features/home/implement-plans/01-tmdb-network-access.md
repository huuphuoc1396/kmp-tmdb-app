# Plan: TMDB network access

Requirement: [01 - TMDB network access](../subtasks/01-tmdb-network-access.md)
Status: Done

## Summary
`core:network` gets its first production code. It provides a factory for a Ktor `HttpClient` preset for TMDB
API v3: HTTPS base URL, Bearer token, JSON, timeouts, OkHttp on Android and Darwin on iOS. It also provides
`apiCall`, which turns any failure into the app's `AppError` inside `AppResult`.

At build time, a Gradle task in `build-logic`, applied by `tmdb.module` to `:core:network` only, reads
`TMDB_ACCESS_TOKEN` from `secrets.properties` or the environment. It generates an internal Kotlin object that holds
the token XOR-ed with a random salt, as `docs/architecture.md` §10 requires. Without a token the build warns and
continues, so CI and tests need no secret.

No endpoints, no DI binding and no caching are added here. Those come with the first consumer in 03.

## Decisions
- **Missing token: warn and build anyway** (from user). Gradle logs a warning naming `secrets.properties` /
  `TMDB_ACCESS_TOKEN`, and the generated token is empty, so every TMDB call fails with `AppError.Unauthorized`.
  CI and tests run without a secret. Rejected: failing app builds only, and always failing.
- **Own generator task in build-logic, applied by `tmdb.module` to `:core:network` only** (from user). This needs
  no new plugin, and the module still applies only `tmdb.module`. Rejected: the BuildKonfig plugin, which writes
  plaintext constants and would be a second plugin on the module.
- **Credential: API Read Access Token as `Authorization: Bearer <token>`** (from user). The secret key name is
  `TMDB_ACCESS_TOKEN`, both in `secrets.properties` and as a CI environment variable. Rejected: the v3 `api_key`
  query parameter.
- **Ktor 3.6.1, OkHttp engine on Android, Darwin on iOS, no Logging plugin** (from user). The shared core has no
  debug/release flag, so leaving logging out is the safe way to never log bodies in release. 3.6.1 is the latest
  release on Maven Central (checked 2026-11-17).
- **Obfuscation: a 32-byte salt from `SecureRandom`, made each time the generator task runs.** The task runs only
  when its inputs change, and its output is not build-cacheable, so the token never goes to the build cache.
  Rerunning only on changed inputs is the incremental form of "per-build salt" in §10.
- **The token source order is `secrets.properties` first, then the environment variable.** Local developers use
  the file; CI uses its secret.
- **The data layer passes `language` per request** (`parameter("language", …)`); the client adds no default
  language. Cache keys are per language (task.md), so the caller that stores the data must choose the language.
  `core:model`'s `Language` is created in 03 when it is first needed.
- **Error mapping:**
  - `CancellationException` is rethrown.
  - HTTP 401 → `Unauthorized`.
  - HTTP 404 → `NotFound`.
  - Other 4xx/5xx → `Unknown`.
  - Timeouts and `kotlinx.io.IOException`, including engine I/O errors → `Network`.
  - Anything else, including JSON errors → `Unknown`.

  Validation uses `expectSuccess = true`.
- **Timeouts: connect 15 s, request 30 s** (`HttpTimeout`), so a hung connection surfaces as `Network`.
- **No Koin binding in this subtask.** Nothing consumes the client yet; 03 adds the binding next to the first
  repository. This follows the "no for-later hooks" rule.
- **`core:network` depends on `core:common`** for `AppResult` and `AppError`; infra → kernel is allowed. The
  existing `core:security` dependency stays as it is.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Result and error types | `AppResult`, `AppError` | `core/common/src/commonMain/kotlin/com/banhmilabs/tmdb/core/common/` |
| Module setup, Detekt, publishing | `tmdb.module` (`ModuleConventionPlugin`) | `build-logic/convention/src/main/kotlin/ModuleConventionPlugin.kt` |
| Catalog access from build logic | `Project.libs`, `libs.version(...)` | `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/ProjectExtensions.kt` |
| `configureX()` shape and constants | `configureKover()` | `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/Kover.kt` |
| Build-logic unit test style (JUnit Jupiter) | `PublishingTest` | `build-logic/convention/src/test/kotlin/com/banhmilabs/tmdb/buildlogic/PublishingTest.kt` |
| `secrets.properties` git-ignored | `.gitignore` line 16 | `.gitignore` |
| Generated code kept out of Detekt | Detekt source is `src/` only | `build-logic/convention/src/main/kotlin/DetektConventionPlugin.kt` |
| Host test style | `EncryptedDataStoreTest` | `core/datastore/src/androidHostTest/kotlin/com/banhmilabs/tmdb/core/datastore/` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| root | `gradle/libs.versions.toml` (modify), `secrets.properties.example` (new) | Ktor libraries; documents the secret key |
| `build-logic/convention` | `buildlogic/TmdbSecrets.kt` (new), `ModuleConventionPlugin.kt` (modify), `src/test/.../TmdbSecretsTest.kt` (new) | Generate the obfuscated token for `:core:network` |
| `core:network` | `build.gradle.kts` (modify); `TmdbHttpClient.kt`, `ApiCall.kt`, `PlatformHttpEngine.kt` (new, common + `androidMain` / `iosMain` actuals); tests (new); `NetworkSmokeTest.kt` (delete) | The TMDB client and error mapping |
| docs | `CLAUDE.md` (modify) | Setup, current state and reuse table |

`core:network` becomes publishable once it has production sources (`hasProductionSources()`). No other module
depends on it yet, so no other module is affected.

## Steps
### Step 1: Add Ktor to the version catalog ✅
- Files: `gradle/libs.versions.toml` (modify)
- What:
  - `[versions]` `ktor = "3.6.1"`.
  - `[libraries]` (all `version.ref = "ktor"`):
    - `ktor-client-core` (`io.ktor:ktor-client-core`)
    - `ktor-client-content-negotiation`
    - `ktor-serialization-kotlinx-json`
    - `ktor-client-okhttp`
    - `ktor-client-darwin`
    - `ktor-client-mock`
- Unit tests: none.
- Compile: `./gradlew help` (the catalog parses).

### Step 2: Generate the obfuscated token for `:core:network` ✅
- Files:
  - `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/TmdbSecrets.kt` (new)
  - `build-logic/convention/src/main/kotlin/ModuleConventionPlugin.kt` (modify)
  - `build-logic/convention/src/test/kotlin/com/banhmilabs/tmdb/buildlogic/TmdbSecretsTest.kt` (new)
  - `secrets.properties.example` (new)
- What:
  - `internal const val TMDB_SECRETS_MODULE = ":core:network"`, plus private constants `TOKEN_KEY =
    "TMDB_ACCESS_TOKEN"`, `SECRETS_FILE = "secrets.properties"`, `SALT_SIZE = 32` and `GENERATED_PACKAGE =
    "com.banhmilabs.tmdb.core.network"`.
  - `@DisableCachingByDefault(because = "Holds a secret") abstract class GenerateTmdbSecretsTask : DefaultTask()`:
    - Input: `@get:Input abstract val token: Property<String>`.
    - Output: `@get:OutputDirectory abstract val outputDir: DirectoryProperty`.
    - `@TaskAction generate()` writes `<outputDir>/com/banhmilabs/tmdb/core/network/TmdbSecrets.kt` with
      `renderSecretsSource(obfuscate(token, salt), salt)`, using a fresh `SecureRandom` salt.
    - If the token is blank, it logs `logger.warn("TMDB_ACCESS_TOKEN is not set (secrets.properties or
      environment): TMDB requests will fail with Unauthorized.")`.
    - Blank token: `outputs.upToDateWhen { false }` while the token is blank, so the warning shows on every build.
  - `internal fun obfuscate(token: String, salt: ByteArray): ByteArray`: the UTF-8 bytes XOR
    `salt[i % salt.size]`.
  - `internal fun renderSecretsSource(data: ByteArray, salt: ByteArray): String` renders:
    ```kotlin
    // Generated by tmdb.module. Do not edit.
    package com.banhmilabs.tmdb.core.network
    internal object TmdbSecrets {
        private val salt = byteArrayOf(...)
        private val data = byteArrayOf(...)
        val accessToken: String
            get() = ByteArray(data.size) { (data[it].toInt() xor salt[it % salt.size].toInt()).toByte() }
                .decodeToString()
    }
    ```
  - `internal fun Project.configureTmdbSecrets()`:
    - The token provider is `providers.fileContents(rootProject.layout.projectDirectory.file(SECRETS_FILE))
      .asText.map { Properties().apply { load(it.reader()) }.getProperty(TOKEN_KEY).orEmpty() }`, falling back to
      `.orElse(providers.environmentVariable(TOKEN_KEY))` and then `.orElse("")`. Treat a blank value from the file
      as missing so the environment variable can still apply.
    - Register `generateTmdbSecrets` with `outputDir = layout.buildDirectory.dir("generated/tmdbSecrets/kotlin")`.
    - `kotlin.sourceSets.commonMain.kotlin.srcDir(task.flatMap { it.outputDir })`, which also wires the task
      dependency.
    - It must be configuration-cache safe: no `project` access in the task action.
  - `ModuleConventionPlugin.apply`: after `configureKotlinMultiplatform(...)`, add
    `if (path == TMDB_SECRETS_MODULE) configureTmdbSecrets()`.
  - `secrets.properties.example`: a one-line comment plus `TMDB_ACCESS_TOKEN=`.
- Pseudo-code: none beyond the above.
- Unit tests:
  - `TmdbSecretsTest` (JUnit Jupiter):
    - `obfuscateThenXorWithSaltRestoresToken`
    - `obfuscatedBytesDifferFromPlainToken`
    - `emptyTokenGivesEmptyData`
    - `renderedSourceDeclaresInternalObjectInNetworkPackage`
    - `renderedSourceDoesNotContainPlainToken`
    - `nonAsciiTokenRoundTrips`
- Compile: `./gradlew :convention:test` from `build-logic`; equivalently `./gradlew testAndroidHostTest` runs it.
  Also run `./gradlew :core:network:compileAndroidHostTest`: the generated file compiles with no token present.

### Step 3: TMDB HTTP client and error mapping  (AC-10 partial) ✅
- Files:
  - `core/network/build.gradle.kts` (modify)
  - `core/network/src/commonMain/kotlin/com/banhmilabs/tmdb/core/network/TmdbHttpClient.kt` (new)
  - `core/network/src/commonMain/kotlin/com/banhmilabs/tmdb/core/network/ApiCall.kt` (new)
  - `core/network/src/commonMain/kotlin/com/banhmilabs/tmdb/core/network/PlatformHttpEngine.kt` (new, `expect`)
  - `core/network/src/androidMain/kotlin/com/banhmilabs/tmdb/core/network/PlatformHttpEngine.android.kt` (new)
  - `core/network/src/iosMain/kotlin/com/banhmilabs/tmdb/core/network/PlatformHttpEngine.ios.kt` (new)
  - `core/network/src/androidHostTest/kotlin/com/banhmilabs/tmdb/core/network/TmdbHttpClientTest.kt` (new)
  - `core/network/src/androidHostTest/kotlin/com/banhmilabs/tmdb/core/network/ApiCallTest.kt` (new)
  - `core/network/src/commonTest/kotlin/com/banhmilabs/tmdb/core/network/PlatformHttpEngineTest.kt` (new)
  - `core/network/src/androidHostTest/kotlin/com/banhmilabs/tmdb/core/network/NetworkSmokeTest.kt` (delete)
- What:
  - Build file dependencies:
    - `commonMain`:
      - `api(projects.core.common)`, because `AppResult` appears in the public `apiCall`.
      - `api(libs.ktor.client.core)`, because the public API exposes `HttpClient` and `HttpClientEngine`.
      - `implementation` of `ktor.client.content.negotiation`, `ktor.serialization.kotlinx.json`,
        `kotlinx.serialization.json` and `kotlinx.coroutines.core`.
      - Keep `implementation(projects.core.security)`.
    - `androidMain`: `implementation(libs.ktor.client.okhttp)`.
    - `iosMain`: `implementation(libs.ktor.client.darwin)`.
    - `androidHostTest`: `implementation(libs.ktor.client.mock)` and `implementation(libs.kotlinx.coroutines.test)`.
  - `TmdbHttpClient.kt`:
    - `const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"`.
    - `fun createTmdbHttpClient(engine: HttpClientEngine = platformHttpEngine(), accessToken: String =
      TmdbSecrets.accessToken): HttpClient` installs:
      - `expectSuccess = true`
      - `defaultRequest { url(TMDB_BASE_URL); bearerAuth(accessToken); accept(ContentType.Application.Json) }`
      - `ContentNegotiation { json(tmdbJson) }`
      - `HttpTimeout { connectTimeoutMillis = 15_000; requestTimeoutMillis = 30_000 }`, with private constants
        for both values.
    - `internal val tmdbJson = Json { ignoreUnknownKeys = true; explicitNulls = false }`.
    - KDoc says endpoints live in each `service/*/data`, and that callers add `parameter("language", tag)`.
  - `ApiCall.kt`: `suspend fun <T> apiCall(block: suspend () -> T): AppResult<T>` (KDoc: wrap every TMDB request).
  - `PlatformHttpEngine`: `internal expect fun platformHttpEngine(): HttpClientEngine`. The Android actual is
    `OkHttp.create()` and the iOS actual is `Darwin.create()`.
- Pseudo-code:
  ```kotlin
  try { AppResult.Success(block()) }
  catch (e: CancellationException) { throw e }
  catch (e: ResponseException) {
      AppResult.Failure(when (e.response.status) {
          HttpStatusCode.Unauthorized -> AppError.Unauthorized
          HttpStatusCode.NotFound -> AppError.NotFound
          else -> AppError.Unknown
      })
  }
  catch (e: HttpRequestTimeoutException) { AppResult.Failure(AppError.Network) }   // also covers connect/socket timeouts
  catch (e: IOException) { AppResult.Failure(AppError.Network) }                     // kotlinx.io.IOException
  catch (e: Exception) { AppResult.Failure(AppError.Unknown) }                        // serialization etc.
  ```
  Detekt's `TooGenericExceptionCaught` applies to the last catch. Suppress it there with a one-line reason:
  every failure must become an `AppError`.
- Unit tests:
  - `TmdbHttpClientTest` (`MockEngine`):
    - `sendsBearerToken`
    - `requestsAreHttpsToTmdbBaseUrl` (relative path `movie/popular` resolves to
      `https://api.themoviedb.org/3/movie/popular`)
    - `sendsLanguageParameterGivenByCaller` (`pt-BR` and `en-US`)
    - `acceptsJson`
    - `decodesJsonIgnoringUnknownKeys` (a private `@Serializable` test DTO)
  - `ApiCallTest` (`MockEngine` + the client):
    - `successReturnsValue`
    - `status401MapsToUnauthorized`
    - `status404MapsToNotFound`
    - `status500MapsToUnknown`
    - `status429MapsToUnknown`
    - `ioExceptionMapsToNetwork`
    - `timeoutMapsToNetwork`
    - `malformedJsonMapsToUnknown`
    - `cancellationIsRethrown`
    - `emptyTokenRequestReturnsUnauthorized`: a 401 from the mock while the client was built with `""` maps to
      `Unauthorized`.
  - `PlatformHttpEngineTest` (`commonTest`): `createsEngine`. It builds and closes the engine, makes no network
    call, and runs on the host, on a device and on the iOS simulator.
- Compile: `./gradlew :core:network:compileAndroidHostTest`

### Step 4: Docs ✅
- Files: `CLAUDE.md` (modify)
- What:
  - **Setup:** the secrets line becomes: "TMDB API Read Access Token: `TMDB_ACCESS_TOKEN` in git-ignored
    `secrets.properties` (copy `secrets.properties.example`), or the `TMDB_ACCESS_TOKEN` environment variable on
    CI. It is injected XOR-obfuscated at build time; without it the build warns and TMDB calls fail with
    Unauthorized."
  - **Current state:** add `core:network` (TMDB client, `apiCall`).
  - **Reuse table:** add the row "TMDB HTTP client / error mapping | `createTmdbHttpClient()`, `apiCall { }` |
    `core:network`".
- Unit tests: none.
- Compile: none (docs).

After the last step: `./gradlew :core:network:testAndroidHostTest :core:network:detekt testAndroidHostTest
:architecture-test:test`. Also run `./gradlew :core:network:iosSimulatorArm64Test` and `./gradlew assemble`
(macOS + Xcode), because `expect`/`actual` and build-logic changed. `koverVerify` is not needed: `core:network`
isn't coverage-gated.

## Traceability
| AC / EC / constraint | Step(s) | Test(s) |
|---|---|---|
| AC-10 (partial: requests carry `pt-BR` / `en-US`) | 3 | `TmdbHttpClientTest.sendsLanguageParameterGivenByCaller` |
| Token never in source or committed files | 2, 4 | `TmdbSecretsTest.renderedSourceDoesNotContainPlainToken`; manual: `git status` and `git grep TMDB_ACCESS_TOKEN` show only the example file and docs |
| Missing token reported clearly, not silently | 2 | Manual: build without `secrets.properties`, see the warning; `ApiCallTest.emptyTokenRequestReturnsUnauthorized` |
| Failures map to `AppError` | 3 | `ApiCallTest` (all cases) |
| No bodies logged in release | 3 | Manual review: no Logging plugin installed |
| HTTPS only | 3 | `TmdbHttpClientTest.requestsAreHttpsToTmdbBaseUrl` |

## Out of scope
- Home endpoints, DTOs, caching, the Koin binding of the client and `core:model`'s `Language` →
  03-home-title-lists.
- Certificate pinning (third-party host; §10 advises against pinning TMDB's leaf certificate).
- A backend proxy to keep the token server-side (§10 notes it is the real fix; not in this feature).

## Risks
- **The token can still be extracted from the app binary or the published artifacts** (XOR only deters casual
  inspection). Mitigation: §10 accepts this for now. Use a read-only TMDB token, and keep `build/maven-repo`
  git-ignored (it is, under `build/`).
- **The token value is a task input, so Gradle's local task history and configuration cache store it on the
  developer machine.** Mitigation: caching is disabled for the task, so nothing goes to a shared build cache;
  local `.gradle/` is git-ignored.
- **Darwin engine errors might not extend `kotlinx.io.IOException`**, which would map no-network to `Unknown` on
  iOS. Mitigation: check in Step 3 against Ktor 3.6.1 sources (`DarwinHttpRequestException`). If it doesn't extend
  it, add `internal expect fun Throwable.isNetworkError(): Boolean` (Android: `false`; iOS: `this is
  DarwinHttpRequestException`) and catch it as `Network` before the generic catch.
- **A blank token makes `generateTmdbSecrets` rerun and `core:network` recompile on every build** (CI and
  token-less local builds). This is accepted: it is cheap, and it keeps the warning visible.

## Deviations
- `ApiCallTest`: the `respondJson` helper is a private top-level function instead of a class member, to stay
  within Detekt's `TooManyFunctions` limit (11 per class).
- `PlatformHttpEngineTest.createsEngine` also builds a client on the engine (`createTmdbHttpClient(engine, "")`)
  and checks it uses that engine, so the test asserts something beyond "no exception".
- Darwin risk checked: in Ktor, `DarwinHttpRequestException` extends `kotlinx.io.IOException`, so iOS I/O errors
  already map to `Network`; no `isNetworkError` expect/actual was added.
- The KDoc of `createTmdbHttpClient` says "each service's `data` module" instead of `service/*/data`,
  because `/*` inside a KDoc opens a nested comment.
