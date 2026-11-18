# Plan: Local database driver

Requirement: [02 - Local database driver](../subtasks/02-local-database-driver.md)
Status: Done

## Summary
`core:database` gets its first production code: `DatabaseDriverFactory`, which opens a SQLDelight `SqlDriver` for a
schema and a file name. Android uses `AndroidSqliteDriver`; iOS uses `NativeSqliteDriver`. The module has no tables:
each `service/*/data` brings its own schema (`docs/architecture.md` §7). The `passphrase` parameter stays in the
signature for SQLCipher later (§10), but a non-null value is rejected for now. Tests open a real database on each
platform and check that a row survives closing and reopening.

## Decisions
- **SQLDelight 2.4.1** — the latest release on Maven Central (checked 2026-11-18). Only the runtime and drivers are
  added here; the SQLDelight Gradle plugin comes with the first schema in 03.
- **A non-null passphrase is rejected** (from user): `require(passphrase == null)` with a message saying that
  encryption is not supported yet. Nobody can think the database is encrypted when it is not. Rejected: ignoring
  the value.
- **`-lsqlite3` on every iOS binary** (from user): `configureKotlinMultiplatform()` adds the linker option to all
  binaries of both iOS targets (test binaries, the `TmdbCore` XCFramework, the `ComposeApp` framework). Linking an
  unused system library costs nothing. Rejected: adding it only to the modules that need it today.
- **No Koin binding in this subtask** (from user): 03 binds the factory next to the first repository (Android passes
  `androidContext()`). This matches subtask 01.
- **`DatabaseDriverFactory` is a common interface with a platform factory function, not an `expect class`.** Android
  needs a `Context` and iOS needs nothing, so the constructors differ. `expect`/`actual` classes are still Beta and
  need a compiler flag. Shape:
  - commonMain: `interface DatabaseDriverFactory { fun create(schema, name, passphrase = null): SqlDriver }`.
  - androidMain: `fun DatabaseDriverFactory(context: Context): DatabaseDriverFactory`.
  - iosMain: `fun DatabaseDriverFactory(): DatabaseDriverFactory`.
- **Synchronous schema type**: `SqlSchema<QueryResult.Value<Unit>>`. This is the type SQLDelight generates without
  `generateAsync`, and the type both drivers accept.
- **`core:database` exposes SQLDelight with `api(libs.sqldelight.runtime)`**, because `SqlDriver` and `SqlSchema`
  are part of its public API. The drivers stay `implementation`.
- **Host tests use SQLDelight's in-memory `JdbcSqliteDriver`** (`sqldelight-sqlite-driver`), as CLAUDE.md requires.
  The factory itself needs a real `Context`, so it is tested on a device, not on the host.
- **Test schema in `commonTest`.** A small hand-written `TestSchema` (one table) is shared by the host, device and
  iOS tests. `commonTest` compiles into all three, so the schema is written once.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Module setup, targets, Detekt, publishing | `tmdb.module` | `build-logic/convention/src/main/kotlin/ModuleConventionPlugin.kt` |
| iOS target configuration | `configureKotlinMultiplatform()` | `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/KotlinMultiplatform.kt` |
| Device-test shape (real platform API, cleanup in `@AfterTest`) | `EncryptedDataStoreDeviceTest` | `core/datastore/src/androidDeviceTest/kotlin/com/banhmilabs/tmdb/core/datastore/` |
| iOS test shape (`kotlin.test`, no mocks) | `KeychainCipherTest` | `core/security/src/iosTest/kotlin/com/banhmilabs/tmdb/core/security/` |
| Platform function pattern (no `expect class`) | `masterKeyCipher(alias)` | `core/security/src/commonMain/kotlin/com/banhmilabs/tmdb/core/security/MasterKeyCipher.kt` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| root | `gradle/libs.versions.toml` (modify) | SQLDelight runtime and drivers |
| `build-logic/convention` | `buildlogic/KotlinMultiplatform.kt` (modify) | `-lsqlite3` on every iOS binary |
| `core:database` | `build.gradle.kts` (modify); `DatabaseDriverFactory.kt` (new, common), `DatabaseDriverFactory.android.kt`, `DatabaseDriverFactory.ios.kt` (new); tests (new); `DatabaseSmokeTest.kt` (delete) | The driver factory |
| docs | `CLAUDE.md` (modify) | Current state and reuse table |

`core:database` becomes publishable (`core-database`) once it has production sources. No module depends on it yet,
so no other module is affected.

## Steps
### Step 1: Add SQLDelight to the version catalog and link sqlite3 on iOS ✅
- Files: `gradle/libs.versions.toml` (modify), `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/KotlinMultiplatform.kt` (modify)
- What:
  - `[versions]` `sqldelight = "2.4.1"`.
  - `[libraries]` (all `version.ref = "sqldelight"`, group `app.cash.sqldelight`):
    - `sqldelight-runtime` (`runtime`)
    - `sqldelight-android-driver` (`android-driver`)
    - `sqldelight-native-driver` (`native-driver`)
    - `sqldelight-sqlite-driver` (`sqlite-driver`, JVM, host tests only)
  - `configureKotlinMultiplatform()`: for each iOS target, `target.binaries.configureEach { linkerOpts(SQLITE_LINKER_OPT) }`
    with `private const val SQLITE_LINKER_OPT = "-lsqlite3"`. `configureEach` also covers binaries created later
    (the XCFramework in `configureIosSdk()`). Update the function's KDoc in one short clause.
- Unit tests: none (the build-logic tests don't cover target setup).
- Compile: `./gradlew help`, then `./gradlew testAndroidHostTest` runs the build-logic tests.

### Step 2: `DatabaseDriverFactory`  (AC-7 partial) ✅
- Files:
  - `core/database/build.gradle.kts` (modify)
  - `core/database/src/commonMain/kotlin/com/banhmilabs/tmdb/core/database/DatabaseDriverFactory.kt` (new)
  - `core/database/src/androidMain/kotlin/com/banhmilabs/tmdb/core/database/DatabaseDriverFactory.android.kt` (new)
  - `core/database/src/iosMain/kotlin/com/banhmilabs/tmdb/core/database/DatabaseDriverFactory.ios.kt` (new)
- What:
  - Build file:
    - `commonMain`: `api(libs.sqldelight.runtime)`. Keep `implementation(projects.core.security)`.
    - `androidMain`: `implementation(libs.sqldelight.android.driver)`.
    - `iosMain`: `implementation(libs.sqldelight.native.driver)`.
    - `androidHostTest`: `implementation(libs.sqldelight.sqlite.driver)`.
  - commonMain:
    ```kotlin
    /** Opens SQLite databases; each context brings its own [SqlSchema]. */
    interface DatabaseDriverFactory {
        /** Opens (and creates or migrates) database file [name]. [passphrase] must be null until encryption exists. */
        fun create(schema: SqlSchema<QueryResult.Value<Unit>>, name: String, passphrase: ByteArray? = null): SqlDriver
    }

    internal fun requireNoPassphrase(passphrase: ByteArray?) =
        require(passphrase == null) { "Database encryption is not supported yet; pass a null passphrase." }
    ```
  - androidMain: `fun DatabaseDriverFactory(context: Context): DatabaseDriverFactory` returns an object whose
    `create` calls `requireNoPassphrase`, then `AndroidSqliteDriver(schema, context.applicationContext, name)`.
  - iosMain: `fun DatabaseDriverFactory(): DatabaseDriverFactory` returns an object whose `create` calls
    `requireNoPassphrase`, then `NativeSqliteDriver(schema, name)`.
- Unit tests: in Step 3.
- Compile: `./gradlew :core:database:compileAndroidHostTest :core:database:compileKotlinIosSimulatorArm64`

### Step 3: Tests on the host, an Android device and the iOS simulator  (AC-7 partial) ✅
- Files:
  - `core/database/src/commonTest/kotlin/com/banhmilabs/tmdb/core/database/TestSchema.kt` (new)
  - `core/database/src/commonTest/kotlin/com/banhmilabs/tmdb/core/database/RequireNoPassphraseTest.kt` (new)
  - `core/database/src/androidHostTest/kotlin/com/banhmilabs/tmdb/core/database/InMemoryDriverTest.kt` (new)
  - `core/database/src/androidDeviceTest/kotlin/com/banhmilabs/tmdb/core/database/DatabaseDriverFactoryTest.kt` (new)
  - `core/database/src/iosTest/kotlin/com/banhmilabs/tmdb/core/database/DatabaseDriverFactoryTest.kt` (new)
  - `core/database/src/androidHostTest/kotlin/com/banhmilabs/tmdb/core/database/DatabaseSmokeTest.kt` (delete)
- What:
  - `TestSchema` (`internal object : SqlSchema<QueryResult.Value<Unit>>`): version 1; `create` runs
    `CREATE TABLE item (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL)`; `migrate` does nothing. Helpers
    `internal fun SqlDriver.insertItem(id: Long, name: String)` and `internal fun SqlDriver.itemNames(): List<String>`
    (`executeQuery` reading column 0 until `cursor.next().value` is false).
  - Device and iOS tests use a unique database name per test (`test-<random>.db`) and delete it afterwards:
    Android `context.deleteDatabase(name)` with the instrumentation context; iOS
    `DatabaseFileContext.deleteDatabase(name)` from SQLiter (bundled with the native driver).
- Unit tests:
  - `RequireNoPassphraseTest` (`commonTest`, runs everywhere): `nullPassphraseIsAccepted`,
    `nonNullPassphraseIsRejected` (`assertFailsWith<IllegalArgumentException>`).
  - `InMemoryDriverTest` (`androidHostTest`): `inMemoryDriverCreatesSchemaAndStoresRows` with
    `JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)` and `TestSchema.create(driver)`. This shows the setup other
    modules' host tests use.
  - `DatabaseDriverFactoryTest` (`androidDeviceTest`, and the same in `iosTest`):
    - `createsSchemaOnFirstOpen`: a new database has the empty `item` table.
    - `persistsRowsAcrossReopen`: insert, close the driver, open the same name again, read the row.
    - `rejectsPassphrase`: `create(TestSchema, name, byteArrayOf(1))` throws `IllegalArgumentException` and creates no
      file.
- Compile: `./gradlew :core:database:compileAndroidHostTest :core:database:compileAndroidDeviceTest :core:database:compileTestKotlinIosSimulatorArm64`

### Step 4: Docs ✅
- Files: `CLAUDE.md` (modify)
- What:
  - **Current state:** add `core:database` (`DatabaseDriverFactory`).
  - **Reuse table:** add the row "SQLite driver for a context's database | `DatabaseDriverFactory(context)` /
    `DatabaseDriverFactory()` | `core:database`".
- Unit tests: none.
- Compile: none (docs).

After the last step:
- `./gradlew :core:database:testAndroidHostTest :core:database:detekt testAndroidHostTest :architecture-test:test`.
- `./gradlew :core:database:connectedAndroidDeviceTest` (emulator or device) and
  `./gradlew :core:database:iosSimulatorArm64Test`, because `androidMain` / `iosMain` changed.
- `./gradlew assemble` (macOS + Xcode), because platform source sets and build logic changed.
- `koverVerify` is not needed: `core:database` isn't coverage-gated.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-7 (partial: a database that persists across app restarts) | 2, 3 | `DatabaseDriverFactoryTest.persistsRowsAcrossReopen` (Android device, iOS simulator) |
| Scope: passphrase kept for encryption later, not used now | 2, 3 | `RequireNoPassphraseTest`, `DatabaseDriverFactoryTest.rejectsPassphrase` |
| How to test 3: an in-memory driver works in host tests | 1, 3 | `InMemoryDriverTest.inMemoryDriverCreatesSchemaAndStoresRows` |

## Out of scope
- Home tables, the SQLDelight Gradle plugin, the Koin binding of the factory, and the cache → 03-home-title-lists.
- Database encryption (SQLCipher).
- `-lsqlite3` in the `cmp/iosApp` Xcode project → 03 (see Risks).

## Risks
- **The `cmp` iOS app links `ComposeApp` as a static framework, and Kotlin linker options don't apply to static
  frameworks.** Once 03 makes `core:di` depend on a database, Xcode must link `sqlite3` itself. Mitigation: 03 adds
  `-lsqlite3` to `OTHER_LDFLAGS` in `cmp/iosApp`. `native-ios` uses the dynamic `TmdbCore` XCFramework, which links it
  through this plan's linker option.
- **The iOS test binary runs without an app bundle** (like the Keychain limitation in `testing.md`). SQLiter writes
  to the app's directories, which should exist for the simulator test process. Mitigation: if
  `iosSimulatorArm64Test` can't create the file, report it and decide with the user; don't weaken the test.
- **`DatabaseFileContext` (SQLiter) might not be on the iOS test classpath** if the native driver doesn't expose it.
  Mitigation: add `implementation(libs.sqldelight.native.driver)` to `iosTest` or delete the file through
  `NSFileManager`; record it as a deviation.

## Deviations
- iOS `rejectsPassphrase` checks that no file exists with `NSFileManager` at SQLiter's
  `DatabaseFileContext.databasePath(name, null)`. The plan only said "creates no file". SQLiter was on the iOS test
  classpath through the native driver, so no extra dependency was needed.
