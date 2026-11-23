# 01 - Favorites list data (shared core)

Feature: [task.md](../task.md)
Depends on: none
Status: Done

## Goal
The shared core gives every app a live list of the user's favorites, most recently added first, each with its
name, poster, year and first genre in the current language. The list works offline, falls back to the other
language's stored data when the current one is missing and fills it in once loaded, and never drops a favorite
because its data is missing.

## Scope
### In scope
- Observing all favorites in order of when they were added, newest first, together with the title data a list
  item shows (name, poster, year or first air year, first genre).
- Title data in the current language; when missing, the stored data of the other language, then a refresh that
  replaces it.
- A favorite with no stored data in any language still appears (with its ID only until data loads).
- The total number of favorites.
- Exposing the list to the iOS entry point and the published shared core, so native apps can use it.
### Out of scope (handled later)
- Any screen or UI -> 02-cmp-browse-favorites, 04-native-android-favorites, 05-native-ios-favorites
- The confirmation popup; removing a favorite itself already exists -> 03-cmp-remove-favorite

## Acceptance criteria
- **AC-11** Offline (partial: data; the screens are in 02)
  - Given the user has favorites
  - When Home or My favorites is shown with no network
  - Then every favorite is shown with its stored name, poster, year and genre, and no error is shown
  - And removing a favorite works with no network
- **AC-12** Language (partial: title data and fallback; screen copy is in 02 and 03)
  - Given the device language is pt-BR
  - When the section, My favorites or the popup is shown
  - Then title data is shown in pt-BR; given any other device language, title data is in en-US
  - And given a favorite has no stored data in the current language, then its stored data in the other language is
    shown and replaced once data in the current language has loaded
  - And a favorite is never hidden because its data is missing or failed to load

## Edge cases
- **EC-8** The same title favorited in both languages -> it appears once (favorites are not per language).
- **EC-10** A favorite TMDB no longer has (partial: data) -> it stays in the list with its stored data and can
  still be removed.

## Layers touched
- Favorites and title repositories, their local databases, the title network refresh
- A combining step across the favorites and title contexts
- Dependency injection, the iOS entry point, the published shared core

## How to test
No screen uses the list yet; verify through tests and, optionally, a debug log in the cmp app:
1. Favorite three titles on Title details; the list reports them newest first with name, poster, year and genre.
2. Turn the network off and relaunch; the same three come back with their data.
3. Switch the device language; titles opened only in the other language come back with that language's data,
   then with the new language's once the network is back.
4. Remove one on Title details; the list and its count update.

## Change log
- 2026-11-23: Initial version.
- 2026-11-23: Plan approved.
- 2026-11-23: Implemented (plan 01 done).
