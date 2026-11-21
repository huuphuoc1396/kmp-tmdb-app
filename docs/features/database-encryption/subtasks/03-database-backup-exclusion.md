# 03 - Database excluded from OS backups

Feature: [task.md](../task.md)
Depends on: 02-encrypted-database-ios
Status: Done

## Goal
The local database file is never part of an OS backup (Android Auto Backup and device-to-device transfer, iCloud
and Finder / iTunes backups), in the cmp, native Android and native iOS apps. A restored device starts with no
database and loads titles from TMDB, instead of carrying a file its key cannot open.

## Scope
### In scope
- Excluding the database file from Android cloud backup and device-to-device transfer, in the cmp and native
  Android apps.
- Excluding the database file from iCloud and Finder / iTunes backups, in the cmp and native iOS apps.
- The settings keep their current backup behavior.
### Out of scope (handled later)
- None.

## Acceptance criteria
- **AC-5** Database is not in OS backups
  - Given the app has a database on the device
  - When an OS backup of the app is made
  - Then the database file is not part of it

## Edge cases
None from task.md in this subtask.

## Layers touched
- Backup configuration of the Android apps (cmp, native Android)
- Database file attributes on iOS

## How to test
1. Android (cmp and native): open Home so titles are cached, run `adb shell bmgr backupnow <package>`, then inspect
   the backup (or run a local `adb backup` / `bmgr` restore on a clean install): the database file is not included,
   and after restore the app opens and loads titles from TMDB.
2. iOS (cmp and native): open Home so titles are cached, then check the database file's "excluded from backup"
   resource value in the simulator's app container (for example with a debug log or `xattr`): it is set.
3. On both platforms, the onboarding completed state is restored as before.

## Change log
- 2026-11-20: Initial version.
