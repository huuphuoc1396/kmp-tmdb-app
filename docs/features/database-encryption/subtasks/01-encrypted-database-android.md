# 01 - Encrypted database on Android

Feature: [task.md](../task.md)
Depends on: none
Status: Done

## Goal
On Android, the local database of the shared core is encrypted at rest with a key unique to the device. Users
upgrading from a version with an unencrypted database, or whose key was lost, get a fresh database instead of a
crash. Home and the offline cache behave exactly as before, in both the cmp and native Android apps.

## Scope
### In scope
- Encrypting every local database of the shared core on Android.
- A database key unique to each device, protected by the device key that already protects the app settings.
- Deleting an unencrypted database left by an earlier version and creating an encrypted one.
- Deleting and recreating the database when it cannot be opened with the device's key, without a crash or an
  error shown, and without touching the settings.
- No code change in the cmp and native Android apps.
### Out of scope (handled later)
- Encryption on iOS -> 02-encrypted-database-ios
- Excluding the database from OS backups -> 03-database-backup-exclusion

## Acceptance criteria
- **AC-1** Database file is unreadable outside the app (partial: Android; iOS in 02)
  - Given the app has cached titles
  - When the database file is copied off the device and opened with a standard SQLite tool
  - Then it cannot be read (the tool reports that it is not a database) and no title data is visible
- **AC-2** App behavior is unchanged (partial: Android; iOS in 02)
  - Given the database is encrypted
  - When the user uses Home, including reopening the app with no network
  - Then cached titles show and refresh exactly as before encryption
- **AC-3** Upgrade from an unencrypted database (partial: Android; iOS in 02)
  - Given the device has the unencrypted database from an earlier version
  - When the user opens the upgraded app
  - Then the app opens normally, the old database is replaced by an encrypted one, and titles load again from TMDB
- **AC-4** Lost key resets only the database (partial: Android; iOS in 02)
  - Given the database cannot be opened with the device's key
  - When the user opens the app
  - Then the app does not crash and shows no error, a new encrypted database is created, and the settings
    (including the onboarding completed state) are kept

## Edge cases
- **EC-1** The database was just reset and the device is offline -> Home shows what a first launch with no network
  shows (the existing error and retry state); no new message. (partial: Android; iOS in 02)
- **EC-2** The database key and the database file do not match (for example, only one of them was restored) ->
  handled like AC-4: the database is deleted and created again. (partial: Android; iOS in 02)

## Layers touched
- Database driver (Android)
- Database key storage (protected by the device master key)
- Dependency injection wiring of the database

## How to test
1. Install the current main build on an emulator, open Home so titles are cached, then install this build over it:
   the app opens on Home (no onboarding), titles load again, and no error is shown.
2. With `adb`, pull the database file from the app's databases directory and open it with `sqlite3`: it reports
   "file is not a database".
3. Turn the network off, force-stop and reopen the app: Home shows the cached titles.
4. Clear only the database key (or replace the database file with one from another install), then reopen the app:
   it does not crash, onboarding is not shown again, and titles load from TMDB.
5. Repeat step 4 with the network off: Home shows the existing error and retry state.
6. Repeat steps 1–3 in the native Android app.

## Change log
- 2026-11-20: Initial version.
