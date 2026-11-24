# 05 - Favorites on iOS (native)

Feature: [task.md](../task.md)
Design: [Home – Favorites section](../design/home-favorites.md), [My favorites](../design/my-favorites.md), [Remove favorite dialog](../design/remove-favorite-dialog.md)
Depends on: 01-favorites-list-data (Swift package); 03-cmp-remove-favorite as the reference behavior
Status: Done

## Goal
The native iOS app has the same favorites as the cmp app: the Home "Favoritos" section, My favorites, opening
a favorite's details, and removing with confirmation on Home, My favorites and Title details.

## Scope
### In scope
- Everything that 02 and 03 deliver, in `native-ios`, using only the shared core Swift package.
- Anything the iOS entry point still lacks for the favorites list, if 01 left a gap.
### Out of scope (handled later)
- Nothing; this is the last subtask.

## Acceptance criteria
All of these, as written in task.md and split in 02 and 03, on native iOS:
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
- SwiftUI views (Home section, My favorites, shared popup, Title details), ViewModels, navigation, localized
  strings; the iOS entry point only if needed

## How to test
Publish the shared core, run `native-ios` on the simulator, then repeat the "How to test" steps of
[02](02-cmp-browse-favorites.md) and [03](03-cmp-remove-favorite.md).

## Change log
- 2026-11-23: Initial version.
- 2026-11-23: Plan approved.
- 2026-11-23: Implemented (plan 05 done).
