# 02 - Encrypted database on iOS

Feature: [task.md](../task.md)
Depends on: 01-encrypted-database-android
Status: Todo

## Goal
On iOS, the local database of the shared core is encrypted at rest with the same behavior as on Android:
unreadable outside the app, a fresh database after an upgrade or a lost key, and Home and the offline cache
unchanged. It works in both the cmp iOS app and the native iOS app (through the shared core's Swift package).

## Scope
### In scope
- Encrypting every local database of the shared core on iOS, with the same key handling, upgrade and lost-key
  behavior as on Android.
- Shipping the encryption support inside what the iOS apps already consume (the cmp iOS framework and the shared
  core's Swift package), with no code change in the apps.
### Out of scope (handled later)
- Excluding the database from OS backups -> 03-database-backup-exclusion

## Acceptance criteria
- **AC-1** Database file is unreadable outside the app (partial: completes iOS; Android in 01)
  - Given the app has cached titles
  - When the database file is copied off the device and opened with a standard SQLite tool
  - Then it cannot be read (the tool reports that it is not a database) and no title data is visible
- **AC-2** App behavior is unchanged (partial: completes iOS; Android in 01)
  - Given the database is encrypted
  - When the user uses Home, including reopening the app with no network
  - Then cached titles show and refresh exactly as before encryption
- **AC-3** Upgrade from an unencrypted database (partial: completes iOS; Android in 01)
  - Given the device has the unencrypted database from an earlier version
  - When the user opens the upgraded app
  - Then the app opens normally, the old database is replaced by an encrypted one, and titles load again from TMDB
- **AC-4** Lost key resets only the database (partial: completes iOS; Android in 01)
  - Given the database cannot be opened with the device's key
  - When the user opens the app
  - Then the app does not crash and shows no error, a new encrypted database is created, and the settings
    (including the onboarding completed state) are kept

## Edge cases
- **EC-1** The database was just reset and the device is offline -> Home shows what a first launch with no network
  shows (the existing error and retry state); no new message. (partial: completes iOS; Android in 01)
- **EC-2** The database key and the database file do not match (for example, only one of them was restored) ->
  handled like AC-4: the database is deleted and created again. (partial: completes iOS; Android in 01)

## Layers touched
- Database driver (iOS)
- Native linking of the iOS binaries (shared core build, cmp iOS framework)
- Packaging of the shared core's iOS Swift package

## How to test
1. Run the current main build of the cmp iOS app on a simulator, open Home so titles are cached, then run this
   build over it: the app opens on Home (no onboarding), titles load again, and no error is shown.
2. Find the database file in the simulator's app container (`xcrun simctl get_app_container booted <bundle id> data`)
   and open it with `sqlite3`: it reports "file is not a database".
3. Turn the network off, quit and reopen the app: Home shows the cached titles.
4. Remove only the database key from the Keychain (or replace the database file with one from another install),
   then reopen the app: it does not crash, onboarding is not shown again, and titles load from TMDB.
5. Repeat step 4 with the network off: Home shows the existing error and retry state.
6. Republish the shared core and repeat steps 1–3 in the native iOS app.

## Change log
- 2026-11-20: Initial version.
