# 04 - Favorites on Android (native)

Feature: [task.md](../task.md)
Design: [Home – Favorites section](../design/home-favorites.md), [My favorites](../design/my-favorites.md), [Remove favorite dialog](../design/remove-favorite-dialog.md)
Depends on: 01-favorites-list-data (published shared core); 03-cmp-remove-favorite as the reference behavior
Status: Todo

## Goal
The native Android app has the same favorites as the cmp app: the Home "Favoritos" section, My favorites, opening
a favorite's details, and removing with confirmation on Home, My favorites and Title details.

## Scope
### In scope
- Everything that 02 and 03 deliver, in `native-android`, using only the published shared core.
### Out of scope (handled later)
- native iOS -> 05-native-ios-favorites

## Acceptance criteria
All of these, as written in task.md and split in 02 and 03, on native Android:
- **AC-1** Home section
- **AC-2** Home section hidden
- **AC-3** My favorites
- **AC-4** Favorite item
- **AC-5** Opening a favorite
- **AC-6** Confirm before removing on Home and My favorites
- **AC-7** Confirm before removing on Title details
- **AC-8** Removing
- **AC-9** Cancelling
- **AC-10** My favorites empty
- **AC-11** Offline (screens and removing; data from 01)
- **AC-12** Language (copy; data from 01)
- **AC-13** Accessibility

## Edge cases
- **EC-1**, **EC-2**, **EC-3**, **EC-4**, **EC-5**, **EC-6**, **EC-7**, **EC-9**, **EC-11**, as in task.md.

## Layers touched
- UI (Home section, My favorites screen, shared popup, Title details), ViewModels, navigation, string resources

## How to test
Publish the shared core, run `native-android`, then repeat the "How to test" steps of
[02](02-cmp-browse-favorites.md) and [03](03-cmp-remove-favorite.md).

## Change log
- 2026-11-23: Initial version.
