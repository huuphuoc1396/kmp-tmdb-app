# Plan: Encrypted database on iOS

Requirement: [02 - Encrypted database on iOS](../subtasks/02-encrypted-database-ios.md)
Status: Done

## Summary
iOS binaries stop linking the system `sqlite3` and link Zetetic's official `SQLCipher.xcframework` 4.20.0, the
same SQLCipher release as Android. Gradle fetches it as a dependency from the GitHub release, checks its checksum,
and passes `-framework SQLCipher` to every iOS binary. The apps get the framework through Swift Package Manager:
the `TmdbCore` package for `native-ios`, and a package reference in `cmp/iosApp`.

On iOS, `DatabaseKeys()` becomes the envelope key from 01, backed by the Keychain master key.
`DatabaseDriverFactory()` opens a database with SQLiter's encryption config when given a passphrase, and recreates
a file that is "not a database" for that key. Both title local data sources now open the database lazily on the IO
dispatcher. This keeps the Keychain and SQLCipher's key derivation off the main thread and out of Koin start-up, on
both platforms.

## Decisions
- **Official `SQLCipher.xcframework` 4.20.0, a dynamic framework** (from user). Checked 2026-11-20:
  - `ios-arm64` and `ios-arm64_x86_64-simulator` slices, each `@rpath/SQLCipher.framework/SQLCipher`.
  - It exports the `sqlite3_*` API and depends only on `Security`/`CoreFoundation`.
  - Zip SHA-256: `1505174fe7f8a0fe81d6ebf87a64b17701b500c12a445ef6b68b9159f012d747`.
  - Rejected: building SQLCipher from source as a static library (more build logic, and SQLCipher upgrades become ours).
- **Gradle resolves the zip as a dependency.** An `ivy` repository in `settings.gradle.kts` points at
  `https://github.com/sqlcipher/SQLCipher.swift/releases/download/[revision]/[artifact].[ext]`. It is declared with
  `exclusiveContent` for group `net.zetetic.ios`, so no other dependency is looked up on GitHub.
  - Gradle downloads the file once into its cache, and a CI host without iOS targets never resolves it.
  - Rejected: a custom download task (it re-implements caching and offline mode).
- **The checksum lives in the version catalog** as `sqlcipher-ios-checksum`, next to `sqlcipher`. Two places use it:
  - the unpack task verifies it before extracting, so a changed asset fails the build;
  - the `TmdbCore` `Package.swift` uses it as the SPM checksum (same value: SPM's checksum is the zip's SHA-256).
- **Every iOS binary links SQLCipher instead of `-lsqlite3`.** SQLiter's cinterop only declares the `sqlite3_*`
  symbols. Linking both libraries would bind them to whichever comes first, so `-lsqlite3` is removed everywhere,
  including `cmp/iosApp`'s `OTHER_LDFLAGS`.
  - Binary linker options: `-F <slice dir> -framework SQLCipher`.
  - Test executables also get `-rpath <slice dir>`, so `iosSimulatorArm64Test` loads it from the build directory.
- **The apps embed SQLCipher through SPM.**
  - `native-ios`: the generated `Package.swift` adds a second binary target `SQLCipher` (release URL + checksum)
    to the `TmdbCore` product. The `native-ios` project itself does not change.
  - `cmp/iosApp`: gets an `XCRemoteSwiftPackageReference` to `https://github.com/sqlcipher/SQLCipher.swift`
    (exact version 4.20.0) with the `SQLCipher` product on the `iosApp` target, copying the Nuke reference shape in
    `native-ios/TmdbNative.xcodeproj`. The project repeats the version because Xcode can't read the catalog, like
    the deployment target.
- **The database opens lazily on the IO dispatcher** (from user). `TitleListLocalDataSource` and
  `TitleDetailsLocalDataSource` take `Lazy<TitleDatabase>`. Their queries are touched only inside
  `withContext(dispatchers.io)` or a flow that runs `flowOn(dispatchers.io)`. Koin passes `lazy { get<TitleDatabase>() }`.
  - Getting a repository from Koin no longer opens the database. The Keychain-less `core:ios` simulator test keeps
    passing, and SQLCipher's PBKDF2 (about 100–300 ms) leaves the main thread on Android too.
  - Rejected: keeping the eager open and moving `SharedCoreTest`'s title check to Swift.
- **Hex passphrase** (from user): the 32 key bytes become a 64-character lowercase hex string, passed as
  `DatabaseConfiguration.Encryption(key = hex)`. SQLiter runs `PRAGMA key`, and SQLCipher derives the key with
  PBKDF2, the standard path. Rejected: a raw `x'…'` key (it fights SQLiter's quoting).
- **"Not a database" on iOS is `SQLiteExceptionErrorCode` with `errorType == SqliteErrorType.SQLITE_NOTADB`**
  (both are in SQLiter 1.3.3). The factory runs one query right after opening, so a wrong key fails in `create`,
  as on Android. Then `DatabaseFileContext.deleteDatabase(name)` and a second open. Other errors are rethrown.
- **iOS key files go in `<Application Support>/database-keys/`** (created on first use).
  - Random bytes come from `CryptographyRandom` (cryptography-kotlin, already an iOS dependency of `core:security`).
  - Excluding the key file and the database from iCloud backups is 03. Until then, a restored key file can't be
    decrypted on another device (the Keychain item is `ThisDeviceOnly`), so the database is reset, which matches EC-2.
- **A null passphrase still opens a plain database on iOS** (same as Android). `requireNoPassphrase` and
  `RequireNoPassphraseTest` are deleted.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Envelope key logic | `EnvelopeDatabaseKeys`, `DATABASE_MASTER_KEY_ALIAS`, `DATABASE_KEY_SIZE_BYTES` | `core/security/src/commonMain/kotlin/com/banhmilabs/tmdb/core/security/` |
| Keychain master key | `masterKeyCipher(alias)` (iOS actual) | `core/security/src/iosMain/kotlin/com/banhmilabs/tmdb/core/security/MasterKeyCipher.ios.kt` |
| Secure random on iOS | `CryptographyRandom` (cryptography-kotlin 0.6.0) | `libs.cryptography.core` |
| SQLCipher open-and-reset shape | `openEncrypted` (Android) | `core/database/src/androidMain/kotlin/com/banhmilabs/tmdb/core/database/DatabaseDriverFactory.android.kt` |
| Encrypted driver test cases | `DatabaseDriverFactoryTest` (Android device) | `core/database/src/androidDeviceTest/kotlin/com/banhmilabs/tmdb/core/database/` |
| iOS driver test shape | `DatabaseDriverFactoryTest` (iOS) | `core/database/src/iosTest/kotlin/com/banhmilabs/tmdb/core/database/` |
| iOS binary configuration | `configureKotlinMultiplatform()` | `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/KotlinMultiplatform.kt` |
| `TmdbCore` Swift package | `swiftPackageManifest`, `IosSdkTest` | `build-logic/convention/src/main/kotlin/.../IosSdk.kt`, `build-logic/convention/src/test/kotlin/.../IosSdkTest.kt` |
| Remote SPM reference in a pbxproj | Nuke in `native-ios` | `native-ios/TmdbNative.xcodeproj/project.pbxproj` |
| Swift tests through the published core | `SharedCoreApiTests` | `native-ios/TmdbNativeTests/SharedCoreApiTests.swift` |
| Title data host-test setup | `TitleListTestEnvironment`, `TitleDetailsTestEnvironment` | `service/title/data/src/androidHostTest/kotlin/com/banhmilabs/tmdb/service/title/data/{list,details}/` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| root | `settings.gradle.kts`, `gradle/libs.versions.toml` (modify) | SQLCipher xcframework repository, library, checksum |
| `build-logic/convention` | `buildlogic/SqlCipher.kt` (new); `buildlogic/KotlinMultiplatform.kt`, `buildlogic/IosSdk.kt` (modify); `IosSdkTest.kt` (modify) | Link SQLCipher; `TmdbCore` package ships it |
| `cmp/iosApp` | `iosApp.xcodeproj/project.pbxproj` (modify) | SQLCipher package; drop `-lsqlite3` |
| `core:security` | `DatabaseKeys.ios.kt` (modify) | Envelope key on iOS |
| `core:database` | `DatabaseDriverFactory.ios.kt` (modify); `iosTest/.../DatabaseDriverFactoryTest.kt` (modify); `iosTest/.../RequireNoPassphraseTest.kt` (delete) | SQLCipher open and reset on iOS |
| `service:title:data` | `list/local/TitleListLocalDataSource.kt`, `details/local/TitleDetailsLocalDataSource.kt`, `di/TitleDataModule.kt` (modify); test environments (modify); `TitleListLocalDataSourceTest.kt`, `TitleDetailsLocalDataSourceTest.kt` (new) | Lazy open on IO |
| `native-ios` | `TmdbNativeTests/SharedCoreApiTests.swift` (modify) | Title database is encrypted end to end |
| docs | `CLAUDE.md`, `docs/architecture.md`, `native-ios/README.md` (modify) | iOS encryption, SQLCipher linking |

Affected: every KMP module's iOS binaries (test executables, `ComposeApp`, `TmdbCore`) through
`configureKotlinMultiplatform()`.

## Steps
### Step 1: Lazy database in the title local data sources  (AC-2) ✅
- Files: `service/title/data/src/commonMain/.../list/local/TitleListLocalDataSource.kt`,
  `.../details/local/TitleDetailsLocalDataSource.kt`, `.../di/TitleDataModule.kt` (modify);
  `src/androidHostTest/.../list/TitleListTestEnvironment.kt`, `.../details/TitleDetailsTestEnvironment.kt` (modify);
  `src/androidHostTest/.../list/local/TitleListLocalDataSourceTest.kt`,
  `src/androidHostTest/.../details/local/TitleDetailsLocalDataSourceTest.kt` (new)
- What:
  - Both constructors take `database: Lazy<TitleDatabase>`. `queries` becomes `private val queries by lazy {
    database.value.titleListQueries }` (and `titleDetailsQueries`), and transactions use `database.value.transaction`.
  - `observe(key)` builds its flows inside `flow { emitAll(…) }.flowOn(dispatchers.io)`, so the first query (and the
    open) runs on IO when collected, not when called. `fetchedAt`, `replace` and `upsert` already run in
    `withContext(dispatchers.io)`.
  - `TitleDataModule`: `TitleListLocalDataSource(database = lazy { get<TitleDatabase>() }, dispatchers = get())`, same
    for details. `TitleDatabase` stays a `single`.
  - Test environments pass `lazyOf(TitleDatabase(driver))`.
  - KDoc on each class: "The database opens on first use, on the IO dispatcher."
- Unit tests (`androidHostTest`, in-memory `JdbcSqliteDriver`, IO = a `newSingleThreadContext("io")` dispatcher
  closed in `@AfterTest`):
  - `TitleListLocalDataSourceTest.databaseOpensOnFirstQueryOnIo`: the `Lazy` records the thread name. After
    `observe(key)` it is not initialized. After `first()` it was initialized on thread `"io"`.
  - `TitleDetailsLocalDataSourceTest.databaseOpensOnFirstQueryOnIo`: same.
- Compile: `./gradlew :service:title:data:compileAndroidHostTest :service:title:data:compileKotlinIosSimulatorArm64`

### Step 2: Catalog and SQLCipher repository  (AC-1) ✅
- Files: `settings.gradle.kts`, `gradle/libs.versions.toml` (modify)
- What:
  - Catalog:
    - `[versions]`: `sqlcipher-ios-checksum = "1505174fe7f8a0fe81d6ebf87a64b17701b500c12a445ef6b68b9159f012d747"`.
    - `[libraries]`: `sqlcipher-ios = { module = "net.zetetic.ios:SQLCipher.xcframework", version.ref = "sqlcipher" }`.
  - `settings.gradle.kts` `dependencyResolutionManagement.repositories`:
    ```kotlin
    exclusiveContent {
        forRepository {
            ivy("https://github.com/sqlcipher/SQLCipher.swift/releases/download") {
                name = "SQLCipherReleases"
                patternLayout { artifact("[revision]/[artifact].[ext]") }
                metadataSources { artifact() }
            }
        }
        filter { includeGroup("net.zetetic.ios") }
    }
    ```
- Unit tests: none.
- Compile: `./gradlew help`

### Step 3: Link SQLCipher into every iOS binary  (AC-1) ✅
- Files: `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/SqlCipher.kt` (new),
  `.../KotlinMultiplatform.kt` (modify)
- What (`SqlCipher.kt`):
  - `internal fun Project.registerSqlCipherUnpack(): TaskProvider<Sync>`:
    - Creates a non-consumable, non-transitive configuration `sqlCipherXcframework`. Its dependency is
      `libs.findLibrary("sqlcipher-ios")` with artifact `type = "zip"`, `extension = "zip"`.
    - Registers `unpackSqlCipher` (`Sync`):
      - `from({ zipTree(configuration.singleFile) })`, including only `SQLCipher.xcframework/ios-arm64/**` and
        `SQLCipher.xcframework/ios-arm64_x86_64-simulator/**`;
      - `into(layout.buildDirectory.dir("sqlcipher"))`;
      - `inputs.property("checksum", libs.version("sqlcipher-ios-checksum"))`;
      - `doFirst` compares the zip's SHA-256 (`MessageDigest`) with the checksum and fails with
        `"SQLCipher.xcframework.zip checksum mismatch: expected …, got …"`.
    - Configuration-cache safe: the action captures only the `FileCollection` and the string.
  - `internal fun sqlCipherSlice(target: KonanTarget): String`: `IOS_ARM64` → `ios-arm64`,
    `IOS_SIMULATOR_ARM64` → `ios-arm64_x86_64-simulator`.
- What (`KotlinMultiplatform.kt`):
  - Replace `SQLITE_LINKER_OPT` with:
    ```kotlin
    val unpack = registerSqlCipherUnpack()
    target.binaries.configureEach {
        val sliceDir = layout.buildDirectory.dir("sqlcipher/SQLCipher.xcframework/${sqlCipherSlice(target.konanTarget)}")
        linkTaskProvider.configure { dependsOn(unpack) }
        linkerOpts("-F", sliceDir.get().asFile.path, "-framework", "SQLCipher")
        if (this is TestExecutable) linkerOpts("-rpath", sliceDir.get().asFile.path)
    }
    ```
  - KDoc: "iOS binaries link SQLCipher (in place of the system sqlite3) for SQLDelight."
- Unit tests: `build-logic/convention/src/test/.../SqlCipherTest.kt` (new, JUnit Jupiter): `slicesMatchIosTargets`.
- Compile: `./gradlew testAndroidHostTest` (runs the build-logic tests),
  `./gradlew :core:database:linkDebugTestIosSimulatorArm64 :core:ios:linkDebugFrameworkIosArm64`

### Step 4: `TmdbCore` package and `cmp/iosApp` ship SQLCipher  (AC-1, AC-2) ✅
- Files: `build-logic/convention/src/main/kotlin/.../IosSdk.kt`, `build-logic/convention/src/test/.../IosSdkTest.kt`,
  `cmp/iosApp/iosApp.xcodeproj/project.pbxproj` (modify)
- What:
  - `swiftPackageManifest(iosMajor: Int, sqlCipherVersion: String, sqlCipherChecksum: String)`:
    ```swift
    products: [.library(name: "TmdbCore", targets: ["TmdbCore", "SQLCipher"])],
    targets: [
        .binaryTarget(name: "TmdbCore", path: "TmdbCore.xcframework"),
        .binaryTarget(
            name: "SQLCipher",
            url: "https://github.com/sqlcipher/SQLCipher.swift/releases/download/<version>/SQLCipher.xcframework.zip",
            checksum: "<checksum>"
        )
    ]
    ```
    `configureIosSdk()` passes `libs.version("sqlcipher")` and `libs.version("sqlcipher-ios-checksum")`.
  - `cmp/iosApp` `project.pbxproj`:
    - Remove both `"-lsqlite3",` entries from `OTHER_LDFLAGS`.
    - Add an `XCRemoteSwiftPackageReference "SQLCipher.swift"` (`repositoryURL =
      "https://github.com/sqlcipher/SQLCipher.swift"`, `requirement = { kind = exactVersion; version = 4.20.0; }`), an
      `XCSwiftPackageProductDependency` `SQLCipher`, the project's `packageReferences`, the `iosApp` target's
      `packageProductDependencies`, and a `PBXBuildFile` in its Frameworks phase. Shape and IDs follow the Nuke
      entries in `native-ios`.
- Unit tests: `IosSdkTest.manifestDeclaresBinaryTargetAndPlatform` (update) also asserts:
  - the `SQLCipher` binary target with the given URL and checksum;
  - `targets: ["TmdbCore", "SQLCipher"]`.
- Compile: `./gradlew testAndroidHostTest`; `./gradlew publishSharedCore`; `xcodebuild -project
  cmp/iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`

### Step 5: iOS `DatabaseKeys` and encrypted driver  (AC-1, AC-3, AC-4, EC-2) ✅
- Files: `core/security/src/iosMain/.../DatabaseKeys.ios.kt`, `core/database/src/iosMain/.../DatabaseDriverFactory.ios.kt`
  (modify); `core/database/src/iosTest/.../DatabaseDriverFactoryTest.kt` (modify), `.../RequireNoPassphraseTest.kt`
  (delete)
- What:
  - `fun DatabaseKeys(): DatabaseKeys = EnvelopeDatabaseKeys(directory = applicationSupportDirectory() /
    "database-keys", fileSystem = FileSystem.SYSTEM, cipher = masterKeyCipher(DATABASE_MASTER_KEY_ALIAS), generateKey =
    { CryptographyRandom.nextBytes(DATABASE_KEY_SIZE_BYTES) })`.
    - `applicationSupportDirectory()` uses `NSFileManager.defaultManager.URLForDirectory(NSApplicationSupportDirectory,
      NSUserDomainMask, null, create = true, error = null)` and converts it to an okio `Path`.
    - KDoc: "iOS: keys in Application Support, encrypted with the Keychain master key."
  - iOS factory:
    ```kotlin
    fun DatabaseDriverFactory(): DatabaseDriverFactory = object : DatabaseDriverFactory {
        override fun create(schema, name, passphrase): SqlDriver =
            if (passphrase == null) NativeSqliteDriver(schema, name) else openEncrypted(schema, name, passphrase.toHex())
    }
    ```
- Pseudo-code (`openEncrypted`):
  ```
  fun open(): SqlDriver = NativeSqliteDriver(schema, name, onConfiguration = {
      it.copy(encryptionConfig = DatabaseConfiguration.Encryption(key = hexKey))
  }).also { it.executeQuery(null, "SELECT count(*) FROM sqlite_master", { c -> QueryResult.Value(c.next().value) }, 0) }
  return try open() catch (e: SQLiteExceptionErrorCode) {
      if (e.errorType != SqliteErrorType.SQLITE_NOTADB) throw e
      DatabaseFileContext.deleteDatabase(name)
      open()
  }
  ```
  `toHex()`: lowercase, two characters per byte (`private fun ByteArray.toHex()`). If the error surfaces from the
  driver constructor instead of the query, the same `try` covers it.
- Unit tests (`iosTest`, real SQLCipher through the linked framework, file bytes read with
  `NSFileManager.contentsAtPath(DatabaseFileContext.databasePath(name, null))`). Keep `createsSchemaOnFirstOpen` and
  `persistsRowsAcrossReopen`, and add the Android device cases:
  - `encryptedDatabaseHasNoPlainHeader`
  - `encryptedDatabasePersistsRowsAcrossReopen`
  - `plainDatabaseIsReplacedByEncryptedOne`
  - `databaseIsResetWhenPassphraseChanges`
  - `passphraseArrayIsNotRequiredAfterCreate`
  - Replace `rejectsPassphrase`.
  - `DatabaseKeys()` itself is not tested in Kotlin, because the simulator test binary has no Keychain (see
    `.claude/rules/testing.md`). Step 6 covers it in Swift.
- Compile: `./gradlew :core:security:compileKotlinIosSimulatorArm64 :core:database:compileTestKotlinIosSimulatorArm64 :core:di:compileKotlinIosSimulatorArm64`

### Step 6: End-to-end check in `native-ios`  (AC-1, AC-2) ✅
- Files: `native-ios/TmdbNativeTests/SharedCoreApiTests.swift` (modify)
- What: `testTitleDatabaseIsEncrypted` (`@MainActor`, async):
  - Collect the first value of `SharedCoreTitleLists().observe(listKey(.movies, .featured, .enUs))`, which opens the
    database through `DatabaseKeys` (Keychain) and SQLCipher.
  - Enumerate the app's `Library` directory for a file named `title.db`.
  - Assert it exists and its first 16 bytes are not `"SQLite format 3\0"`.
- Compile: `./gradlew publishSharedCore`, then (in `native-ios/`) `xcodebuild -project TmdbNative.xcodeproj -scheme
  TmdbNative -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`

### Step 7: Docs ✅
- Files: `CLAUDE.md`, `docs/architecture.md`, `native-ios/README.md` (modify)
- What:
  - CLAUDE.md:
    - Current state and the Architecture bullet: SQLCipher on both platforms, the database opens lazily on IO.
    - Setup: iOS binaries link `SQLCipher.xcframework` (resolved by Gradle from the SQLCipher.swift release, checksum
      in the catalog). `cmp/iosApp` repeats the SQLCipher version in its package reference.
    - `publishSharedCore` line: the `TmdbCore` package also carries the `SQLCipher` binary target.
  - `docs/architecture.md` §10: iOS is no longer "planned"; add the iOS key directory, the lazy open, and the dynamic
    `SQLCipher` framework in place of `sqlite3`.
  - `native-ios/README.md`: the `TmdbCore` package brings `SQLCipher`, and the first build fetches it.
- Compile: none (docs).

Final checks (forge, after the last step; macOS + Xcode, simulator):
- `./gradlew :service:title:data:testAndroidHostTest :service:title:data:detekt :service:title:data:koverVerify :core:security:detekt :core:database:detekt testAndroidHostTest`
- `./gradlew :architecture-test:test`
- `./gradlew :core:database:iosSimulatorArm64Test :core:ios:iosSimulatorArm64Test :service:title:data:iosSimulatorArm64Test`
- `./gradlew assemble`
- `./gradlew publishSharedCore`, then the `native-ios` `xcodebuild … test` command from CLAUDE.md (iPhone 17)
- `cmp/iosApp` `xcodebuild … build` (CLAUDE.md command)
- `./gradlew :core:database:connectedAndroidDeviceTest :core:di:connectedAndroidDeviceTest`, because the Android open
  path is now lazy

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 Database file is unreadable outside the app (iOS) | 2, 3, 4, 5, 6 | iOS `DatabaseDriverFactoryTest.encryptedDatabaseHasNoPlainHeader`; Swift `SharedCoreApiTests.testTitleDatabaseIsEncrypted`; manual: subtask How to test step 2 |
| AC-2 App behavior is unchanged (iOS) | 1, 4, 5, 6 | `TitleListLocalDataSourceTest`, `TitleDetailsLocalDataSourceTest`, existing title data host tests; iOS `encryptedDatabasePersistsRowsAcrossReopen`; native-ios suite; manual: How to test steps 3, 6 |
| AC-3 Upgrade from an unencrypted database (iOS) | 5 | iOS `DatabaseDriverFactoryTest.plainDatabaseIsReplacedByEncryptedOne`; manual: How to test step 1 |
| AC-4 Lost key resets only the database (iOS) | 5 | iOS `DatabaseDriverFactoryTest.databaseIsResetWhenPassphraseChanges`; `EnvelopeDatabaseKeysTest.replacesKeyThatCannotBeDecrypted` (01); manual: How to test step 4 |
| EC-1 Reset while offline (iOS) | none (existing Home behavior) | Manual: How to test step 5 |
| EC-2 Key and database file do not match (iOS) | 5 | iOS `DatabaseDriverFactoryTest.databaseIsResetWhenPassphraseChanges` |

## Out of scope
- Excluding the database and key files from iCloud / Finder backups and Android backups -> 03.
- Key rotation; a raw (non-PBKDF2) key.

## Risks
- **`exclusiveContent` ivy resolution of a GitHub release asset** may need `artifact` type tweaks, and GitHub
  redirects to its CDN. Mitigation: Step 2's compile plus Step 3's link are the check. If the ivy resolution fails,
  stop and report rather than switching approach.
- **Simulator test executables loading a dynamic framework from the build directory through `-rpath`.** Mitigation:
  `:core:database:iosSimulatorArm64Test` in the final checks. If it fails, try `DYLD_FRAMEWORK_PATH` on the test
  task as a small deviation.
- **Where SQLiter throws `SQLITE_NOTADB`** (driver constructor or first query). Mitigation: one `try` around both,
  confirmed by `plainDatabaseIsReplacedByEncryptedOne` and `databaseIsResetWhenPassphraseChanges` on the simulator.
- **The cmp `ComposeApp` static framework leaves `sqlite3_*` unresolved until the app links `SQLCipher`.**
  Mitigation: the `cmp/iosApp` `xcodebuild` build in the final checks.
- **Lazy open changes the Android path from 01.** Mitigation: the Android device tests in the final checks, and
  `InitKoinTest.titleDatabaseIsEncrypted` still gets `TitleDatabase` directly.
- **The app gains a dynamic framework (about 1.5 MB on device).** Accepted in task.md.

## Deviations
- **`sqlCipherSlice` takes the Kotlin target name** (`iosArm64`, `iosSimulatorArm64`) instead of a `KonanTarget`, so
  build-logic needs no Kotlin/Native utility types. `sqlCipherFrameworkDir(targetName)` wraps the path.
- **The SQLCipher dependency is created with `dependencies.create(...)` and added to the configuration.**
  `dependencies.add` with a catalog `Provider` returns `null`, so the zip artifact could not be configured on it.
- **Extra build-logic tests:** `SqlCipherTest.unknownTargetHasNoSlice`, and `IosSdkTest.manifestShipsOfficialSqlCipher`
  as a separate case for the `SQLCipher` binary target.
- **The lazy-open tests strip the coroutine debug suffix** (`io @coroutine#N`) from the thread name before comparing.
- **Detekt:** `toHex` uses named constants (`BYTE_MASK`, `HEX_RADIX`, `HEX_DIGITS_PER_BYTE`).
- **No ivy or rpath fallback was needed.** The release zip resolves through the `exclusiveContent` ivy repository,
  and the simulator test executables load `SQLCipher.framework` through `-rpath`. On iOS, `SQLITE_NOTADB` is caught
  by the single `try` around the driver and its first query.
- **The emulator needed a reboot** before the Android device tests (system server error: `IActivityManager` null).
  This was an environment problem; the tests then passed unchanged.

## Change log
- 2026-11-20: Initial version.
