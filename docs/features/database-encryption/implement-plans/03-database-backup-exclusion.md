# Plan: Database excluded from OS backups

Requirement: [03 - Database excluded from OS backups](../subtasks/03-database-backup-exclusion.md)
Status: Done

## Summary
Android: both apps (`cmp/androidApp`, `native-android/app`) get backup rules that exclude the whole `database`
domain from cloud backup and from device-to-device transfer. The rules are `dataExtractionRules` for Android 12+ and
`fullBackupContent` for API 24–30. Everything else, including `settings.json`, keeps the default Auto Backup.

iOS: the shared core marks the SQLite directory (`Application Support/databases`) and the database key directory
(`Application Support/database-keys`) as excluded from backup (`NSURLIsExcludedFromBackupKey`). The cmp and native
iOS apps get this through the shared core, with no app changes. The settings file stays where it is, so it is
backed up as before.

## Decisions
- **Android: exclude the whole `database` domain** (from user), not only `title.db`.
  - `<exclude domain="database" path="." />` covers every SQLite database the app has now or later (e.g.
    `favorites`) and their side files (`-journal`, `-wal`, `-shm`). Every database is encrypted with a key that is
    never backed up, so a backed-up copy could never be opened.
  - Rejected: listing `title.db` and its side files (each new database must be added to both apps).
- **Android: rules per app, duplicated in both apps.** Backup rules are an `<application>` attribute that only the
  app manifest can set reliably; a library manifest can't contribute exclusions. `native-android` can't share
  files with `cmp/`, so each app has its own two XML files with the same content.
- **iOS: flag the `databases/` directory** (from user), not each file. A directory that is excluded excludes
  everything inside it, including `-wal` / `-shm` files that SQLiter creates later and a database recreated after a
  reset. The flag is set on every `DatabaseDriverFactory.create`, after the database is opened (so the directory
  exists); it is cheap and repairs a flag lost if the directory was recreated.
  - The directory is `DatabaseFileContext.databasePath(name, null)`'s parent (`databaseDirPath()` is internal in
    SQLiter 1.3.3).
  - Rejected: flagging `title.db` and each side file (must be redone after every delete or new side file).
- **iOS: also exclude `database-keys/`** (from user). The key files are encrypted with a Keychain master key that is
  `ThisDeviceOnly`, so they are useless on another device; excluding them matches Android (`noBackupFilesDir`).
- **One iOS helper in `core:security`**, `excludeFromBackup(directory: String)`: creates the directory if missing
  and sets `NSURLIsExcludedFromBackupKey`. `core:database` already depends on `core:security`, so both call sites
  share it. It takes a `String` path so `core:database` needs no okio.
- **Settings keep their current backup behavior** (from subtask). `settings.json` stays in Android `filesDir` and
  iOS `Application Support`, and is not excluded.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| iOS database file path | `DatabaseFileContext.databasePath(name, null)` (SQLiter) | already used in `core/database/src/iosTest/.../DatabaseDriverFactoryTest.kt` |
| iOS Foundation access pattern | `NSFileManager.defaultManager` with `@OptIn(ExperimentalForeignApi::class)` | `core/security/src/iosMain/.../DatabaseKeys.ios.kt` |
| Android key file outside backups | `noBackupFilesDir` (unchanged) | `core/security/src/androidMain/.../DatabaseKeys.android.kt` |
| iOS database tests | `DatabaseDriverFactoryTest` (7 cases) | `core/database/src/iosTest/...` |
| Swift check on the real app container | `testTitleDatabaseIsEncrypted` (finds `title.db` under Library) | `native-ios/TmdbNativeTests/SharedCoreApiTests.swift` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `cmp/androidApp` | `res/xml/data_extraction_rules.xml`, `res/xml/backup_rules.xml` (new); `AndroidManifest.xml` | Exclude the `database` domain |
| `native-android/app` | same three files | Same, for the native app |
| `core:security` | `iosMain/.../BackupExclusion.ios.kt` (new), `DatabaseKeys.ios.kt`; `iosTest/.../BackupExclusionTest.kt` (new) | Helper; exclude `database-keys/` |
| `core:database` | `iosMain/.../DatabaseDriverFactory.ios.kt`; `iosTest/.../DatabaseDriverFactoryTest.kt` | Exclude `databases/` |
| `native-ios` | `TmdbNativeTests/SharedCoreApiTests.swift` | Check both flags in the real app |
| docs | `docs/architecture.md` §10, `CLAUDE.md` (architecture bullet) | Describe backup behavior |

## Steps
### Step 1: Android backup rules in both apps  (AC-5) ✅
- Files: `cmp/androidApp/src/main/res/xml/data_extraction_rules.xml` (new),
  `cmp/androidApp/src/main/res/xml/backup_rules.xml` (new), `cmp/androidApp/src/main/AndroidManifest.xml` (modify);
  the same three under `native-android/app/src/main/`.
- What:
  - `data_extraction_rules.xml`: `<data-extraction-rules>` with `<cloud-backup>` and `<device-transfer>`, each
    containing `<exclude domain="database" path="." />`.
  - `backup_rules.xml`: `<full-backup-content>` with `<exclude domain="database" path="." />`.
  - Manifest `<application>`: `android:dataExtractionRules="@xml/data_extraction_rules"` and
    `android:fullBackupContent="@xml/backup_rules"`. `allowBackup` stays at its default (true), so other files keep
    their current backup.
  - A one-line XML comment in each file: databases are encrypted with keys that are never backed up.
- Unit tests: none (resource config; checked manually, see Traceability).
- Compile: `./gradlew :cmp:androidApp:assembleDebug`; in `native-android/`: `./gradlew :app:assembleDebug`.

### Step 2: iOS backup-exclusion helper  (AC-5) ✅
- Files: `core/security/src/iosMain/kotlin/com/banhmilabs/tmdb/core/security/BackupExclusion.ios.kt` (new),
  `core/security/src/iosTest/kotlin/com/banhmilabs/tmdb/core/security/BackupExclusionTest.kt` (new).
- What: `fun excludeFromBackup(directory: String)` with a one-line KDoc.
  - Pseudo-code:
    ```
    NSFileManager.defaultManager.createDirectoryAtPath(directory, withIntermediateDirectories = true, null, null)
    val ok = NSURL.fileURLWithPath(directory, isDirectory = true)
        .setResourceValue(true, forKey = NSURLIsExcludedFromBackupKey, error = null)
    check(ok) { "Could not exclude $directory from backup" }
    ```
- Unit tests (`BackupExclusionTest`, in a fresh directory under `NSTemporaryDirectory()`):
  `marksDirectoryExcludedFromBackup` (reads `resourceValuesForKeys(listOf(NSURLIsExcludedFromBackupKey))`),
  `createsMissingDirectory`, `isIdempotent` (calling twice keeps the flag).
- Compile: `./gradlew :core:security:compileTestKotlinIosSimulatorArm64`.

### Step 3: Exclude the database and key directories  (AC-5) ✅
- Files: `core/database/src/iosMain/.../DatabaseDriverFactory.ios.kt`, `core/security/src/iosMain/.../DatabaseKeys.ios.kt`
  (modify); `core/database/src/iosTest/.../DatabaseDriverFactoryTest.kt` (modify).
- What:
  - `DatabaseDriverFactory()`: after either branch opens the driver, call
    `excludeFromBackup(DatabaseFileContext.databasePath(name, null).substringBeforeLast('/'))` (`core:database`
    has no okio). Applies to plain and encrypted databases. KDoc: the directory is
    excluded from iCloud and Finder backups.
  - `DatabaseKeys()` (iOS): compute `applicationSupportDirectory() / "database-keys"` once, call
    `excludeFromBackup(directory.toString())`, then pass it to `EnvelopeDatabaseKeys`. KDoc mentions it.
- Unit tests (`DatabaseDriverFactoryTest`): `databaseDirectoryIsExcludedFromBackup` (create an encrypted database,
  read the flag of the database's parent directory) and `plainDatabaseDirectoryIsExcludedFromBackup`.
  `DatabaseKeys()` can't run in Kotlin iOS tests (no Keychain); it is covered by Step 4.
- Compile: `./gradlew :core:database:compileTestKotlinIosSimulatorArm64 :core:security:compileTestKotlinIosSimulatorArm64`.

### Step 4: Check the real app container, update docs  (AC-5) ✅
- Files: `native-ios/TmdbNativeTests/SharedCoreApiTests.swift`, `docs/architecture.md`, `CLAUDE.md` (modify).
- What:
  - Swift test `testDatabaseFilesAreExcludedFromBackup`: after the title database is used (same setup as
    `testTitleDatabaseIsEncrypted`), find the directory that holds `title.db` and `Application Support/database-keys`,
    and assert `resourceValues(forKeys: [.isExcludedFromBackupKey]).isExcludedFromBackup == true` for both.
  - `docs/architecture.md` §10: one bullet. Databases are never in OS backups: Android apps exclude the `database`
    domain (backup and transfer rules), iOS marks `databases/` and `database-keys/` excluded; settings are backed up
    as before.
  - `CLAUDE.md` architecture bullet on `DatabaseDriverFactory`: add that the iOS database directory is excluded
    from backup and that each Android app's backup rules exclude the `database` domain (a new app must copy them).
- Compile: `./gradlew publishSharedCore`, then the `native-ios` `xcodebuild ... build` from CLAUDE.md.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-5 (Android, cmp and native) | 1 | Manual: `adb shell bmgr backupnow <package>` then restore on a clean install (subtask "How to test" 1); databases are absent and titles load from TMDB |
| AC-5 (iOS, cmp and native) | 2, 3, 4 | `BackupExclusionTest`, `DatabaseDriverFactoryTest.databaseDirectoryIsExcludedFromBackup` / `plainDatabaseDirectoryIsExcludedFromBackup`, Swift `testDatabaseFilesAreExcludedFromBackup` |
| Settings keep their backup behavior | 1, 3 (nothing excluded besides databases and keys) | Manual: subtask "How to test" 3 |

## Out of scope
- Moving `settings.json` or changing its backup behavior (its key is already not backed up; unchanged by design).
- Excluding other files (caches, image caches); not part of AC-5.

## Risks
- `path="."` for a whole domain — Android's backup rules accept `.` for the domain root, but a typo here silently
  backs up everything. Mitigation: the manual `bmgr` check in the Traceability table, on both apps.
- iOS flag lost if the directory is deleted and recreated outside our code (e.g. by SQLiter). Mitigation: the flag
  is set again on every `create`, and the key directory on every `DatabaseKeys()`.
- Android `adb backup` is deprecated and `bmgr` restore needs a transport. Mitigation: use `bmgr backupnow` with
  the local transport and `bmgr restore`, as in the subtask's How to test.

## Deviations
- `DatabaseDriverFactoryTest` (iOS) clears the directory's flag before each new case, so a flag left by an earlier
  run can't make the test pass. Each test class reads the flag through its own private helper, since `core:database`
  tests can't use `core:security` test code.
- The Swift test `testDatabaseFilesAreExcludedFromBackup` is `@MainActor`, like `testTitleDatabaseIsEncrypted`
  (`observe` is main-actor isolated).
