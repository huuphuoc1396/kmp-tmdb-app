# Plan: Encrypted database on Android

Requirement: [01 - Encrypted database on Android](../subtasks/01-encrypted-database-android.md)
Status: Done

## Summary
`core:security` gets `DatabaseKeys`, which gives each database a passphrase: 32 random bytes, generated once,
stored in a file in `noBackupFilesDir` and encrypted with the existing `MasterKeyCipher` (envelope key). On Android,
`DatabaseDriverFactory.create(..., passphrase)` opens the database with SQLCipher when the passphrase is not null.
If SQLite reports "file is not a database" (an unencrypted database from an earlier version, or a key that no
longer matches), the factory deletes the database and opens a new one. `TitleDataModule` passes the passphrase
from `DatabaseKeys`. On iOS, `DatabaseKeys` returns `null` until 02, so iOS keeps its plain database.

## Decisions
- **Envelope key** (from user): a random 32-byte key, encrypted with `masterKeyCipher("database")` and stored in a
  file. It reuses `MasterKeyCipher` on both platforms, so 02 only adds the iOS wiring. This is the approach SQLCipher
  recommends. Rejected: a key derived by HMAC in the Keystore (new platform code on each side), the raw key in the
  Keychain (Android has no equivalent), and the encrypted DataStore (it is `suspend` and is included in backups).
- **The `passphrase` parameter stays, and `core:security` provides the value** (from user): `DatabaseKeys` with
  `fun passphrase(name: String): ByteArray?`, bound in `core:di`, passed by each `data` module. Rejected: the
  factory managing the key itself and dropping the parameter.
- **Reset only on "file is not a database"** (from user): other open failures (disk full, I/O) are thrown as they
  are today, so a temporary error never deletes data.
- **The key file is in `noBackupFilesDir/database-keys/<name>.key`.** It is never backed up. A backup restored to
  another device therefore has no key file, gets a new key, and resets the database (EC-2).
- **One master key alias for every database: `"database"`**, separate from `"settings"`. Losing it resets the
  databases only. The settings keep their own key (AC-4).
- **A key file that cannot be decrypted is replaced by a new key.** This happens when the Keystore key was lost or
  the file is corrupt. The database then fails to open with the new key and is reset, which is the same path as
  AC-3 and AC-4.
- **`DatabaseKeys` is an interface with a platform factory function.** This is the same shape as
  `DatabaseDriverFactory`:
  - `fun DatabaseKeys(context: Context)` (androidMain)
  - `fun DatabaseKeys()` (iosMain, returns `null` from `passphrase` until 02)
  - The envelope logic is `internal class EnvelopeDatabaseKeys` in `commonMain` (okio file I/O), host-tested with a
    MockK cipher, and reused by 02.
- **Random key from `java.security.SecureRandom`** (Android), injected into `EnvelopeDatabaseKeys` as
  `generateKey: () -> ByteArray` so host tests control it.
- **A null passphrase still opens a plain database on Android.** The existing device tests keep that path, and
  AC-3's test uses it to create an "earlier version" database. The iOS factory still rejects a non-null passphrase:
  `requireNoPassphrase` moves from `commonMain` to `iosMain`, and its test moves to `iosTest`.
- **The database is opened eagerly with SQLCipher.** The factory builds the `SupportSQLiteOpenHelper` itself, calls
  `writableDatabase` inside the try/catch, and then wraps it with `AndroidSqliteDriver(openHelper)`. The default
  `AndroidSqliteDriver(schema, context, name)` opens lazily, so a wrong key would surface later, on the first query.
- **SQLCipher `net.zetetic:sqlcipher-android` 4.20.0** is the latest release on Maven Central (checked 2026-11-20,
  AAR about 4 MB). Its POM has `androidx.sqlite:sqlite` 2.7.0 at runtime scope only. The factory calls that API
  directly, so `androidx.sqlite:sqlite` 2.7.0 is added as a direct `implementation` dependency.
  `System.loadLibrary("sqlcipher")` runs once, before the first encrypted open.
- **Passphrase arrays are copied before use.** `SupportOpenHelperFactory` may clear the array it is given, and
  the factory may need the passphrase twice (open, then reopen after a reset).

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Master key (Keystore), AES-GCM | `masterKeyCipher(alias)`, `MasterKeyCipher`, `CipherException` | `core/security/src/commonMain/kotlin/com/banhmilabs/tmdb/core/security/` |
| Driver factory to extend | `DatabaseDriverFactory` | `core/database/src/{commonMain,androidMain,iosMain}/kotlin/com/banhmilabs/tmdb/core/database/` |
| Platform factory-function shape | `DatabaseDriverFactory(context)` / `DatabaseDriverFactory()` | same |
| Platform Koin bindings | `platformModule` | `core/di/src/{androidMain,iosMain}/kotlin/com/banhmilabs/tmdb/core/di/` |
| okio file I/O | `libs.okio` (already in the catalog, used by `core:datastore`) | `gradle/libs.versions.toml` |
| Host test with a MockK cipher | `EncryptedDataStoreTest` | `core/datastore/src/androidHostTest/kotlin/com/banhmilabs/tmdb/core/datastore/` |
| Device test with the real Keystore | `MasterKeyCipherTest`, `EncryptedDataStoreDeviceTest` | `core/security/src/androidDeviceTest/...`, `core/datastore/src/androidDeviceTest/...` |
| Driver device tests and test schema | `DatabaseDriverFactoryTest`, `TestSchema` | `core/database/src/androidDeviceTest/...`, `core/database/src/commonTest/...` |
| Koin wiring device test | `InitKoinTest` | `core/di/src/androidDeviceTest/kotlin/com/banhmilabs/tmdb/core/di/` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| root | `gradle/libs.versions.toml` (modify) | `sqlcipher-android`, `androidx-sqlite` |
| `core:security` | `build.gradle.kts` (modify); `DatabaseKeys.kt`, `EnvelopeDatabaseKeys.kt` (new, common); `DatabaseKeys.android.kt`, `DatabaseKeys.ios.kt` (new); tests (new) | Database passphrases |
| `core:database` | `build.gradle.kts` (modify); `DatabaseDriverFactory.kt`, `.android.kt`, `.ios.kt` (modify); `RequireNoPassphraseTest.kt` (move to `iosTest`); `DatabaseDriverFactoryTest.kt` (android device, modify) | SQLCipher open and reset |
| `core:di` | `build.gradle.kts` (modify); `PlatformModule.android.kt`, `PlatformModule.ios.kt` (modify); `InitKoinTest.kt` (modify) | Bind `DatabaseKeys` |
| `service:title:data` | `build.gradle.kts` (modify); `di/TitleDataModule.kt` (modify) | Pass the passphrase |
| docs | `CLAUDE.md`, `docs/architecture.md` (modify) | Current state, reuse table, §10 |

Affected consumers: `core:ios` (TmdbCore XCFramework), `cmp/composeApp`, and the published `core-di` used by
`native-android`. Only their transitive dependencies change; they need no code change.

## Steps
### Step 1: Version catalog  (AC-1) ✅
- Files: `gradle/libs.versions.toml` (modify)
- What:
  - `[versions]`: `sqlcipher = "4.20.0"`, `androidxSqlite = "2.7.0"`.
  - `[libraries]`: `sqlcipher-android = { module = "net.zetetic:sqlcipher-android", version.ref = "sqlcipher" }`,
    `androidx-sqlite = { module = "androidx.sqlite:sqlite", version.ref = "androidxSqlite" }`.
- Unit tests: none.
- Compile: `./gradlew help`

### Step 2: `DatabaseKeys` in `core:security`  (AC-4, EC-2) ✅
- Files:
  - `core/security/build.gradle.kts` (modify): `commonMain` gets `implementation(libs.okio)`.
  - `core/security/src/commonMain/kotlin/com/banhmilabs/tmdb/core/security/DatabaseKeys.kt` (new)
  - `core/security/src/commonMain/kotlin/com/banhmilabs/tmdb/core/security/EnvelopeDatabaseKeys.kt` (new)
  - `core/security/src/androidMain/kotlin/com/banhmilabs/tmdb/core/security/DatabaseKeys.android.kt` (new)
  - `core/security/src/iosMain/kotlin/com/banhmilabs/tmdb/core/security/DatabaseKeys.ios.kt` (new)
- What:
  ```kotlin
  /** Passphrases for local databases; the same name always gets the same passphrase on this device. */
  interface DatabaseKeys {
      /** Passphrase for database [name], created on first use; null when the platform has no encryption yet. */
      fun passphrase(name: String): ByteArray?
  }

  internal const val DATABASE_KEY_SIZE_BYTES = 32
  internal const val DATABASE_MASTER_KEY_ALIAS = "database"

  internal class EnvelopeDatabaseKeys(
      private val directory: Path,
      private val fileSystem: FileSystem,
      private val cipher: MasterKeyCipher,
      private val generateKey: () -> ByteArray,
  ) : DatabaseKeys
  ```
  - androidMain: `fun DatabaseKeys(context: Context): DatabaseKeys = EnvelopeDatabaseKeys(
    context.noBackupFilesDir.toOkioPath() / "database-keys", FileSystem.SYSTEM,
    masterKeyCipher(DATABASE_MASTER_KEY_ALIAS)) { ByteArray(DATABASE_KEY_SIZE_BYTES).also(SecureRandom()::nextBytes) }`.
  - iosMain: `fun DatabaseKeys(): DatabaseKeys = object : DatabaseKeys { override fun passphrase(name: String) = null }`,
    with KDoc saying iOS databases are not encrypted yet.
- Pseudo-code (`EnvelopeDatabaseKeys.passphrase`):
  ```
  file = directory / "$name.key"
  if fileSystem.exists(file):
      try return cipher.decrypt(fileSystem.read(file) { readByteArray() })
      catch CipherException -> fall through (Keystore key lost or file corrupt)
  key = generateKey()
  fileSystem.createDirectories(directory)
  write cipher.encrypt(key) to "$name.key.tmp", then fileSystem.atomicMove(tmp, file)
  return key
  ```
  `passphrase` is not synchronized: Koin creates each database once. KDoc states that a new key means the
  existing database can no longer be opened.
- Unit tests:
  - `core/security/src/androidHostTest/kotlin/com/banhmilabs/tmdb/core/security/EnvelopeDatabaseKeysTest.kt` (new).
    Uses a MockK cipher that reverses its input (so stored bytes differ from the key) and a fresh directory under
    `FileSystem.SYSTEM_TEMPORARY_DIRECTORY`, deleted in `@AfterTest`. Cases:
    - `createsKeyOnFirstCall`: returns the generated key.
    - `returnsSameKeyOnLaterCalls`: a second instance on the same directory returns the same key; `generateKey` is
      called once.
    - `storesOnlyEncryptedKey`: the file content equals `cipher.encrypt(key)` and not the key.
    - `replacesKeyThatCannotBeDecrypted`: when `decrypt` throws `CipherException`, it returns a new key and
      overwrites the file.
    - `keepsSeparateKeysPerDatabase`: `"a.db"` and `"b.db"` get different keys and files.
  - `core/security/src/androidDeviceTest/kotlin/com/banhmilabs/tmdb/core/security/DatabaseKeysDeviceTest.kt` (new).
    Uses the real Keystore through `DatabaseKeys(context)` and a unique database name; deletes the key file and
    the `"database"` Keystore entry in `@AfterTest` (reuse the helper in `MasterKeyTestSupport.android.kt`). Cases:
    - `passphraseIs32Bytes`
    - `passphraseIsStableAcrossInstances`
    - `keyFileDoesNotContainPassphrase`
    - `lostMasterKeyGivesNewPassphrase`: delete the Keystore entry, then a new instance returns a different
      passphrase.
- Compile: `./gradlew :core:security:compileAndroidHostTest :core:security:compileKotlinIosSimulatorArm64`

### Step 3: SQLCipher in the Android driver factory  (AC-1, AC-3, AC-4, EC-2) ✅
- Files:
  - `core/database/build.gradle.kts` (modify): `androidMain` gets `implementation(libs.sqlcipher.android)` and
    `implementation(libs.androidx.sqlite)`.
  - `core/database/src/commonMain/.../DatabaseDriverFactory.kt` (modify): KDoc of `create` becomes "[passphrase]
    encrypts the database; a database that cannot be opened with it is deleted and created again. Platforms that
    don't support encryption reject a non-null value." Remove `requireNoPassphrase` from here.
  - `core/database/src/androidMain/.../DatabaseDriverFactory.android.kt` (modify)
  - `core/database/src/iosMain/.../DatabaseDriverFactory.ios.kt` (modify): holds `internal fun requireNoPassphrase`
    (same body and message), called as today.
  - `core/database/src/commonTest/.../RequireNoPassphraseTest.kt` → `core/database/src/iosTest/.../RequireNoPassphraseTest.kt` (move).
- What (Android):
  ```kotlin
  fun DatabaseDriverFactory(context: Context): DatabaseDriverFactory = object : DatabaseDriverFactory {
      override fun create(schema, name, passphrase): SqlDriver =
          if (passphrase == null) AndroidSqliteDriver(schema, context.applicationContext, name)
          else openEncrypted(context.applicationContext, schema, name, passphrase)
  }

  private val sqlCipherLoaded by lazy { System.loadLibrary("sqlcipher") }
  ```
- Pseudo-code (`openEncrypted`):
  ```
  sqlCipherLoaded
  fun open(): SupportSQLiteOpenHelper =
      SupportOpenHelperFactory(passphrase.copyOf()).create(
          SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
              .callback(AndroidSqliteDriver.Callback(schema)).build(),
      ).also { it.writableDatabase }          // opens now, so a wrong key fails here
  helper = try open() catch (e: SQLiteException) {
      if (!e.isNotADatabase()) throw e
      context.deleteDatabase(name)            // also removes -journal / -wal / -shm
      open()
  }
  return AndroidSqliteDriver(helper)

  isNotADatabase(): this is SQLiteDatabaseCorruptException || message contains "file is not a database"
  ```
  Forge narrows `isNotADatabase` to the exception type SQLCipher actually throws, as observed in the device tests
  below; both reset cases (plain file, wrong key) must take the reset path.
- Unit tests: `core/database/src/androidDeviceTest/.../DatabaseDriverFactoryTest.kt` (modify). Keep
  `createsSchemaOnFirstOpen` and `persistsRowsAcrossReopen` (null passphrase); replace `rejectsPassphrase` with:
  - `encryptedDatabaseHasNoPlainHeader`: after creating with a passphrase and inserting a row, the first 16 bytes
    of `getDatabasePath(name)` are not `"SQLite format 3\u0000"`, and the file does not contain `"Dune"` as UTF-8.
  - `encryptedDatabasePersistsRowsAcrossReopen`
  - `plainDatabaseIsReplacedByEncryptedOne`: create with a null passphrase and insert a row; then create with a
    passphrase: no rows, and the header is not plain.
  - `databaseIsResetWhenPassphraseChanges`: create with passphrase A and insert; then create with passphrase B: no
    rows, and later inserts persist under B.
  - `passphraseArrayIsNotRequiredAfterCreate`: zero the caller's array after `create`, then a write and a reopen
    with the original value still work.
- Compile: `./gradlew :core:database:compileAndroidDeviceTest :core:database:compileKotlinIosSimulatorArm64 :core:database:compileTestKotlinIosSimulatorArm64`

### Step 4: Wire `DatabaseKeys` and pass the passphrase  (AC-1, AC-2) ✅
- Files:
  - `core/di/build.gradle.kts` (modify): `commonMain` gets `implementation(projects.core.security)`.
  - `core/di/src/androidMain/.../PlatformModule.android.kt` (modify): add `single { DatabaseKeys(androidContext()) }`.
  - `core/di/src/iosMain/.../PlatformModule.ios.kt` (modify): add `single { DatabaseKeys() }`.
  - `service/title/data/build.gradle.kts` (modify): `commonMain` gets `implementation(projects.core.security)`.
  - `service/title/data/src/commonMain/.../di/TitleDataModule.kt` (modify):
    ```kotlin
    single {
        val driver = get<DatabaseDriverFactory>().create(
            schema = TitleDatabase.Schema,
            name = TITLE_DATABASE_NAME,
            passphrase = get<DatabaseKeys>().passphrase(TITLE_DATABASE_NAME),
        )
        TitleDatabase(driver)
    }
    ```
- Unit tests: `core/di/src/androidDeviceTest/.../InitKoinTest.kt` (modify), adding
  `titleDatabaseIsEncrypted`: start Koin, resolve `TitleListsRepository` and then `TitleDatabase`. The file
  `getDatabasePath("title.db")` exists and its header is not `"SQLite format 3\u0000"`. `@AfterTest` deletes
  `title.db` and the key file. `TitleDatabase` is reachable because `core:di` already depends on `service:title:data`.
- Compile: `./gradlew :core:di:compileAndroidDeviceTest :core:di:compileKotlinIosSimulatorArm64 :service:title:data:compileAndroidHostTest :core:ios:compileKotlinIosSimulatorArm64 :cmp:composeApp:compileAndroidMain`

### Step 5: Docs ✅
- Files: `CLAUDE.md`, `docs/architecture.md` (modify)
- What:
  - CLAUDE.md:
    - Current state: `core:security` also has `DatabaseKeys`, and the Android database is encrypted with SQLCipher.
    - Architecture bullet: replace "keeps the passphrase parameter so SQLCipher can be added later" with "Android
      opens it with SQLCipher when given a passphrase from `DatabaseKeys` (`core:security`); a database that cannot
      be opened with it is deleted and recreated; iOS is not encrypted yet."
    - Modules table: `core:security` purpose adds "database passphrases".
    - Reuse table: add a row for database passphrases (`DatabaseKeys`, `core:security`), passed to
      `DatabaseDriverFactory.create`.
  - `docs/architecture.md` §10: "Database encryption" describes the envelope key (random key, encrypted with the
    `"database"` master key, kept in `noBackupFilesDir`), SQLCipher on Android, the reset on "not a database", and
    iOS as planned.
- Compile: none (docs).

Final checks (forge, after the last step):
- `./gradlew :core:security:testAndroidHostTest :core:security:detekt :core:database:testAndroidHostTest :core:database:detekt :core:di:detekt :service:title:data:testAndroidHostTest :service:title:data:detekt`
- `./gradlew :architecture-test:test` (module dependencies changed)
- `./gradlew :service:title:data:koverVerify`
- Platform tests (emulator):
  `./gradlew :core:security:connectedAndroidDeviceTest :core:database:connectedAndroidDeviceTest :core:di:connectedAndroidDeviceTest`
- `./gradlew :core:database:iosSimulatorArm64Test` (moved `RequireNoPassphraseTest`) and `./gradlew assemble`
  (platform source sets changed; macOS + Xcode).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 Database file is unreadable outside the app (Android) | 1, 3, 4 | `DatabaseDriverFactoryTest.encryptedDatabaseHasNoPlainHeader`, `InitKoinTest.titleDatabaseIsEncrypted`; manual: subtask How to test step 2 |
| AC-2 App behavior is unchanged (Android) | 3, 4 | `DatabaseDriverFactoryTest.encryptedDatabasePersistsRowsAcrossReopen`, `passphraseArrayIsNotRequiredAfterCreate`; existing title data host tests; manual: How to test steps 3, 6 |
| AC-3 Upgrade from an unencrypted database (Android) | 3 | `DatabaseDriverFactoryTest.plainDatabaseIsReplacedByEncryptedOne`; manual: How to test step 1 |
| AC-4 Lost key resets only the database (Android) | 2, 3 | `EnvelopeDatabaseKeysTest.replacesKeyThatCannotBeDecrypted`, `DatabaseKeysDeviceTest.lostMasterKeyGivesNewPassphrase`, `DatabaseDriverFactoryTest.databaseIsResetWhenPassphraseChanges`; settings kept by design (separate alias and file); manual: How to test step 4 |
| EC-1 Reset while offline (Android) | none (existing Home behavior) | Manual: How to test step 5 |
| EC-2 Key and database file do not match (Android) | 2, 3 | `DatabaseDriverFactoryTest.databaseIsResetWhenPassphraseChanges`, `EnvelopeDatabaseKeysTest.replacesKeyThatCannotBeDecrypted` |

## Out of scope
- Encryption on iOS (`DatabaseKeys()` returns null; the iOS factory still rejects a passphrase) -> 02.
- Excluding the database file from OS backups -> 03. The key file is already outside backups because it lives in
  `noBackupFilesDir`.
- Key rotation.

## Risks
- **The exception for "not a database"** may differ from `SQLiteDatabaseCorruptException`. SQLCipher may also
  delete the file itself through `Callback.onCorruption`. Mitigation: the device tests for the plain-file and
  wrong-key cases must both end with an empty, encrypted database; forge adjusts `isNotADatabase` to what they
  observe.
- **Native library loading or ABI packaging** (`libsqlcipher.so`) could fail on some devices. Mitigation: the device
  tests run the real library, and How to test covers both apps. No app module minifies today, so no R8 rules are
  needed now.
- **App size grows by about 4 MB** (SQLCipher AAR, all ABIs). Accepted in task.md.
- **`native-android` gets SQLCipher transitively from the published `core-database`.** Mitigation: How to test
  step 6, after `publishSharedCore`.

## Deviations
- **No direct `androidx.sqlite` dependency.** SQLDelight `android-driver` already exposes `androidx.sqlite:sqlite`
  2.7.1 at compile scope, so the planned 2.7.0 entry would have been redundant and older. The catalog only gains
  `sqlcipher-android`.
- **`isNotADatabase` is a catch of `net.zetetic.database.sqlcipher.SQLiteNotADatabaseException`.** SQLCipher 4.20.0
  ships this exception (a subclass of `android.database.sqlite.SQLiteException`), so the factory catches it instead
  of checking for `SQLiteDatabaseCorruptException` or the message. The device tests
  `plainDatabaseIsReplacedByEncryptedOne` and `databaseIsResetWhenPassphraseChanges` pass on an API 36 emulator.

## Change log
- 2026-11-20: Initial version.
