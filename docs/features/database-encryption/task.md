# Database encryption

Status: Confirmed
Source: chat (2026-11-20)

## Goal
The data the app caches on the device (title lists today, favorites later) is stored in plain form. Anyone who
copies the database file off the device (a rooted or jailbroken device, adb, an OS backup) can read it. The local
database must be encrypted at rest so that a copied file is unreadable, while the app behaves exactly as before
for the user. It applies to every app that uses the shared core (cmp, native Android, native iOS).

## Scope
### In scope
- Encrypting every local database of the shared core, on Android and iOS, for all three apps, with no code change
  in the apps.
- A database key unique to each device, protected by the device key that already protects the app settings.
- Upgrade from a version with an unencrypted database: the old database is deleted and the cache is fetched again.
- A lost or mismatched key (backup restored to another device, device reset, Keychain cleared): the database is
  deleted and created again, without a crash or an error shown.
- Excluding the database file from OS backups (Android Auto Backup, iCloud / iTunes backup).
### Out of scope
- Room or any other database library.
- Encrypting individual columns.
- Rotating the database key.
- Any new UI or copy.
- Changes to the encrypted settings store or the onboarding state.

## User flow
1. The user opens the app (fresh install, upgrade, or later launch).
2. The app opens its encrypted local database; if it is missing, unencrypted from an older version, or cannot be
   opened with the device's key, the app deletes it and creates a new empty one.
3. Home shows cached titles when there are any and refreshes them as it does today; after a reset it loads them
   from TMDB like a first launch.

## UI states
No new UI states. Home keeps its existing loading, content, error and retry states; after a database reset it
behaves like a first launch.

## Acceptance criteria
- **AC-1** Database file is unreadable outside the app
  - Given the app has cached titles
  - When the database file is copied off the device and opened with a standard SQLite tool
  - Then it cannot be read (the tool reports that it is not a database) and no title data is visible

- **AC-2** App behavior is unchanged
  - Given the database is encrypted
  - When the user uses Home, including reopening the app with no network
  - Then cached titles show and refresh exactly as before encryption

- **AC-3** Upgrade from an unencrypted database
  - Given the device has the unencrypted database from an earlier version
  - When the user opens the upgraded app
  - Then the app opens normally, the old database is replaced by an encrypted one, and titles load again from TMDB

- **AC-4** Lost key resets only the database
  - Given the database cannot be opened with the device's key
  - When the user opens the app
  - Then the app does not crash and shows no error, a new encrypted database is created, and the settings
    (including the onboarding completed state) are kept

- **AC-5** Database is not in OS backups
  - Given the app has a database on the device
  - When an OS backup of the app is made
  - Then the database file is not part of it

## Edge cases
- **EC-1** The database was just reset and the device is offline -> Home shows what a first launch with no network
  shows (the existing error and retry state); no new message.
- **EC-2** The database key and the database file do not match (for example, only one of them was restored) ->
  handled like AC-4: the database is deleted and created again.

## Constraints
- Android and iOS (minSdk 24, iOS 16), for the cmp, native Android and native iOS apps.
- The database key never appears in source, committed files or logs.
- An app size increase of a few MB is acceptable. No specific performance threshold.
- Existing offline-first behavior (cached titles shown first, a failed refresh never removes them) must not change.

## Open questions
None.

## Change log
- 2026-11-20: Initial version.

## Subtasks
1. [01 - Encrypted database on Android](subtasks/01-encrypted-database-android.md)
2. [02 - Encrypted database on iOS](subtasks/02-encrypted-database-ios.md)
3. [03 - Database excluded from OS backups](subtasks/03-database-backup-exclusion.md)
