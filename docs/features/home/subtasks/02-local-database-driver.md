# 02 - Local database driver

Feature: [task.md](../task.md)
Depends on: none
Status: Done

## Goal
The shared core can open a local SQLite database on Android and iOS, so a context can keep its own cached data on
the device. Nothing changes for the user yet; this is the foundation for showing Home offline.

## Scope
### In scope
- A database driver factory for Android and iOS, with no tables of its own (each context owns its own database).
- The driver keeps a passphrase parameter so encryption can be added later, without using it now.
- Platform behavior tested on each platform (Android device test, iOS simulator test).
### Out of scope (handled later)
- Home tables, cache and refresh -> 03-home-title-lists
- Encrypting the database

## Acceptance criteria
- **AC-7** Offline with cache (partial: foundation only: a database that persists across app restarts; the cache and offline behavior are in 03-home-title-lists and 05-cmp-home-robustness)
  - Given the category was loaded before
  - When the user opens it with no network
  - Then the cached titles are shown immediately and no error is shown
  - And a failed refresh never removes cached titles

## Edge cases
None from task.md in this subtask.

## Layers touched
- Database (driver factory, platform code)

## How to test
1. Run the Android device test: a database opened with the driver persists a row across closing and reopening.
2. Run the iOS simulator test: the same, on iOS.
3. Run the host tests: an in-memory driver works for tests in other modules.

## Change log
- 2026-11-17: Initial version.
