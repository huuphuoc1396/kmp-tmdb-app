# Favorites

Status: Confirmed
Source: chat (`/spark` request, 2026-11-23); designs: Figma
[Home – Favorite section](https://www.figma.com/design/G4YiZkobsY5gJP94z8pFlh/TMDB--Community-?node-id=341-483),
[My favorites](https://www.figma.com/design/G4YiZkobsY5gJP94z8pFlh/TMDB--Community-?node-id=396-2),
[Confirm popup](https://www.figma.com/design/G4YiZkobsY5gJP94z8pFlh/TMDB--Community-?node-id=398-50)

## Goal
Users can mark a title as a favorite on Title details, but nothing shows those titles again. Favorites adds a
"Favoritos" section on Home and a "My favorites" screen that lists every favorite, so users can come back to what
they saved and remove what they no longer want. Removing a favorite always asks for confirmation, on Home, My
favorites and Title details, so a stray tap never loses a saved title. Success: from Home, users reach any saved
title in at most two taps, even offline, and never remove a favorite without confirming it.

## Scope
### In scope
- All three apps: `cmp`, `native-android` and `native-ios`. The shared core provides the data.
- A "Favoritos" section on Home, between the featured row and "Mais populares": title, a badge with the total
  number of favorites, the subtitle "Salvos para assistir quando quiser.", a "Ver todos" link and a horizontal row
  of up to 10 favorites (poster with a heart, name, "year · first genre").
- A "My favorites" screen opened from "Ver todos": "Voltar", a "N títulos" badge, title, subtitle and a vertical
  list of every favorite (poster, name, "year · first genre", heart).
- Opening a favorite's details from Home or My favorites.
- Removing a favorite from Home, My favorites and Title details, always through a confirmation popup ("Remover dos
  favoritos?", "Cancelar" / "Remover").
- Offline display of favorites.
- Copy localized for en and pt-BR. Title content comes from TMDB in the matching language.

### Out of scope
- Adding a favorite from Home or My favorites (favorites are still added only on Title details).
- Undo after removing (no snackbar).
- Sorting, filtering or searching favorites; any order other than most recently added first.
- Filtering the Home section by the selected category tab.
- Syncing favorites across devices or accounts.
- Paging the My favorites list.
- Analytics.

## User flow
1. The user favorites one or more titles on Title details (existing behavior).
2. Home shows the "Favoritos" section between the featured row and "Mais populares", with the newest favorite
   first and the total in the badge.
3. The user taps a favorite; its Title details opens. "Voltar" returns to Home as it was.
4. The user taps "Ver todos"; My favorites opens with every favorite, newest first.
5. The user taps a favorite's heart on Home, My favorites or Title details; the confirmation popup opens with the
   title's name.
6. The user taps "Remover"; the title is no longer a favorite and disappears from Home and My favorites, and its
   heart on Title details shows empty. Or the user taps "Cancelar", taps outside or goes back; nothing changes.
7. The user taps "Voltar" on My favorites; Home is shown as it was, with the section updated.

## UI states
### Home – Favoritos section
| State | When | What the user sees / can do |
|---|---|---|
| Hidden | The user has no favorites | No section; Home looks as it does today |
| Content | One or more favorites | Title, badge with the total, subtitle, "Ver todos" and up to 10 favorites; can scroll, open a title, remove, open My favorites |

### My favorites
| State | When | What the user sees / can do |
|---|---|---|
| Content | One or more favorites | Badge "N títulos" and every favorite, newest first; can scroll, open a title, remove, go back |
| Empty | No favorites (e.g. after removing the last one) | Badge "0 títulos" and a short empty message; can go back |

### Confirmation popup
| State | When | What the user sees / can do |
|---|---|---|
| Shown | The user taps the heart of a favorite on Home, My favorites or Title details | "Remover dos favoritos?", a message naming the title, "Cancelar" and "Remover" |

## Acceptance criteria
- **AC-1** Home section
  - Given the user has favorites
  - When Home is shown
  - Then the "Favoritos" section is shown between the featured row and "Mais populares", with the title, a badge
    with the total number of favorites, the subtitle, "Ver todos" and a row of up to 10 favorites, most recently
    added first, as in the design
  - And the section is the same whichever category tab is selected

- **AC-2** Home section hidden
  - Given the user has no favorites
  - When Home is shown
  - Then no "Favoritos" section is shown and the rest of Home is unchanged

- **AC-3** My favorites
  - Given the "Favoritos" section is shown
  - When the user taps "Ver todos"
  - Then My favorites opens with a "N títulos" badge ("1 título" for one) and every favorite, most recently added
    first, as in the design
  - And "Voltar" or the system back returns to Home as it was

- **AC-4** Favorite item
  - Given a favorite is shown on Home or My favorites
  - When it is drawn
  - Then it shows the poster with a filled heart, the name and the line "year · first genre" (first air year for
    series), in the current language
  - And any unknown part (year or genre) is left out together with its "·"; a title without a poster shows a
    neutral placeholder tile with its name

- **AC-5** Opening a favorite
  - Given a favorite is shown on Home or My favorites
  - When the user taps it anywhere except the heart
  - Then that title's details open
  - And going back shows the previous screen as it was

- **AC-6** Confirm before removing on Home and My favorites
  - Given a favorite is shown on Home or My favorites
  - When the user taps its heart
  - Then the confirmation popup opens with the title "Remover dos favoritos?", a message naming the title, and the
    buttons "Cancelar" and "Remover", as in the design
  - And the title stays a favorite while the popup is open

- **AC-7** Confirm before removing on Title details
  - Given Title details shows a title that is a favorite
  - When the user taps the filled heart
  - Then the same confirmation popup opens instead of removing the favorite at once
  - And given the title is not a favorite, when the user taps the empty heart, then it becomes a favorite at once
    with no popup (existing behavior)

- **AC-8** Removing
  - Given the confirmation popup is open
  - When the user taps "Remover"
  - Then the popup closes and the title is no longer a favorite
  - And it disappears from the Home section and My favorites, the badges show the new total, and its heart on
    Title details shows empty
  - And there is no undo; the user can favorite it again on Title details

- **AC-9** Cancelling
  - Given the confirmation popup is open
  - When the user taps "Cancelar", taps outside the popup or uses the system back
  - Then the popup closes and nothing changes

- **AC-10** My favorites empty
  - Given My favorites is shown
  - When the user removes the last favorite
  - Then the screen stays open with the badge "0 títulos" and a short empty message
  - And when the user goes back, Home shows no "Favoritos" section

- **AC-11** Offline
  - Given the user has favorites
  - When Home or My favorites is shown with no network
  - Then every favorite is shown with its stored name, poster, year and genre, and no error is shown
  - And removing a favorite works with no network

- **AC-12** Language
  - Given the device language is pt-BR
  - When the section, My favorites or the popup is shown
  - Then the copy is Portuguese and title data is shown in pt-BR; given any other device language, the copy is
    English and title data is in en-US
  - And given a favorite has no stored data in the current language, then its stored data in the other language is
    shown and replaced once data in the current language has loaded
  - And a favorite is never hidden because its data is missing or failed to load

- **AC-13** Accessibility
  - Given a favorite is shown on Home or My favorites
  - When a screen reader focuses it
  - Then the item is announced as a button with the title's name, and its heart as a "Remove from favorites" /
    "Remover dos favoritos" button
  - And "Ver todos", the hearts and the popup buttons meet the minimum touch target, and the popup takes the screen
    reader's focus while it is open

## Edge cases
- **EC-1** Heart or "Remover" tapped repeatedly or quickly -> only one popup opens and the title is removed once.
- **EC-2** A favorite or "Ver todos" tapped repeatedly or quickly -> the details or My favorites screen opens
  exactly once.
- **EC-3** Favorite added or removed on Title details -> on return, the Home section and My favorites already
  reflect the change (count, order, presence).
- **EC-4** More than 10 favorites -> Home shows the 10 most recent and the badge shows the full total; My
  favorites shows all.
- **EC-5** Very long title name -> it is truncated in the Home row and the list item; in the popup message it wraps
  and is shown in full.
- **EC-6** Rotation while the popup is open -> the popup stays open and nothing changes; process death -> the popup
  is closed and the title is still a favorite.
- **EC-7** Rotation or process death on My favorites -> the screen and its scroll position are restored.
- **EC-8** The same title favorited in both languages -> it appears once (favorites are not per language).
- **EC-9** Device language changes while the app is running -> the next display uses the new language (per
  AC-12); the set of favorites is unchanged.
- **EC-10** A favorite TMDB no longer has -> it is still listed with its stored data and can still be removed.
- **EC-11** Largest system font size -> section title, badge, list items and popup stay readable; nothing is
  clipped beyond the truncation in EC-5.

## Constraints
- Platforms: Android (minSdk 24) and iOS 16+, in `cmp`, `native-android` and `native-ios`.
- Offline-first: favorites show without network; errors never clear stored data.
- Favorites are local-only and not per language; title data shown for them follows the per-language cache.
- The TMDB token is never in source or committed files.
- Dark theme with purple and pink accents, as in the designs.
- Localization: en and pt-BR.
- No analytics.
- Existing Home behavior (tabs, rows, refresh, scroll positions on return) and Title details behavior other than
  the removal popup must not change.

## Designs
- [Home – Favorites section](design/home-favorites.md)
- [My favorites](design/my-favorites.md)
- [Remove favorite dialog](design/remove-favorite-dialog.md)

## Open questions
- [x] English copy for the section title and subtitle, "Ver todos", My favorites title, subtitle and badge, the
  popup title, message and buttons, and the pt-BR and en empty message for My favorites (owner: user, before the UI
  subtask). From the en screenshots and the user (2026-11-23); see the Copy table of each design file.
- [x] pt-BR title of My favorites: the design keeps "My Favorites" in English (owner: user, before the UI subtask).
  "Meus favoritos" (user, plan 02, 2026-11-23).

## Change log
- 2026-11-23: Initial version.
- 2026-11-23: Linked designs: home-favorites, my-favorites, remove-favorite-dialog. English and empty-state copy
  decided.
- 2026-11-23: Confirmed by the user.
- 2026-11-23: Split into subtasks 01–05.
- 2026-11-23: Open question answered: pt-BR title of My favorites is "Meus favoritos".
- 2026-11-23: EC-6 changed: the popup stays open on rotation and closes on process death (user, plan 03).

## Subtasks
1. [01 - Favorites list data (shared core)](subtasks/01-favorites-list-data.md)
2. [02 - Browse favorites (cmp)](subtasks/02-cmp-browse-favorites.md)
3. [03 - Remove with confirmation (cmp)](subtasks/03-cmp-remove-favorite.md)
4. [04 - Favorites on Android (native)](subtasks/04-native-android-favorites.md)
5. [05 - Favorites on iOS (native)](subtasks/05-native-ios-favorites.md)
