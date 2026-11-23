# 02 - Browse favorites (cmp)

Feature: [task.md](../task.md)
Design: [Home – Favorites section](../design/home-favorites.md), [My favorites](../design/my-favorites.md)
Depends on: 01-favorites-list-data
Status: Done

## Goal
In the cmp app, Home shows the "Favoritos" section between the featured row and "Mais populares", and "Ver todos"
opens My favorites with every favorite. Tapping a favorite opens its details.

## Scope
### In scope
- The Home "Favoritos" section: title, count badge, subtitle, "Ver todos", up to 10 items, hidden with no
  favorites.
- The My favorites screen with "Voltar", the "N títulos" badge, title, subtitle and the full list.
- Navigation: Home → My favorites, Home or My favorites → Title details, back.
- Poster placeholder, "year · first genre" line, truncation of long names.
- Copy in en and pt-BR.
### Out of scope (handled later)
- The heart button on items, the confirmation popup, removing, the My favorites empty state, the popup on Title
  details -> 03-cmp-remove-favorite
- Screen reader labels for hearts and the popup -> 03-cmp-remove-favorite
- native apps -> 04-native-android-favorites, 05-native-ios-favorites

The heart buttons are not drawn in this subtask, so nothing visible lacks an action; 03 adds them.

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
- **AC-4** Favorite item (partial: without the heart, added in 03)
  - Given a favorite is shown on Home or My favorites
  - When it is drawn
  - Then it shows the poster, the name and the line "year · first genre" (first air year for series), in the
    current language
  - And any unknown part (year or genre) is left out together with its "·"; a title without a poster shows a
    neutral placeholder tile with its name
- **AC-5** Opening a favorite
  - Given a favorite is shown on Home or My favorites
  - When the user taps it anywhere except the heart
  - Then that title's details open
  - And going back shows the previous screen as it was
- **AC-11** Offline (partial: screens; data in 01)
  - Given the user has favorites
  - When Home or My favorites is shown with no network
  - Then every favorite is shown with its stored name, poster, year and genre, and no error is shown
- **AC-12** Language (partial: section and My favorites copy; data in 01, popup copy in 03)
  - Given the device language is pt-BR
  - When the section or My favorites is shown
  - Then the copy is Portuguese; given any other device language, the copy is English

## Edge cases
- **EC-2** A favorite or "Ver todos" tapped repeatedly or quickly -> the details or My favorites screen opens
  exactly once.
- **EC-3** Favorite added or removed on Title details -> on return, the Home section and My favorites already
  reflect the change (count, order, presence).
- **EC-4** More than 10 favorites -> Home shows the 10 most recent and the badge shows the full total; My
  favorites shows all.
- **EC-5** Very long title name (partial: Home row and list item; popup in 03) -> it is truncated.
- **EC-7** Rotation or process death on My favorites -> the screen and its scroll position are restored.
- **EC-9** Device language changes while the app is running -> the next display uses the new language; the set of
  favorites is unchanged.

## Layers touched
- UI (Home section, My favorites screen), ViewModels, navigation graph, copy resources

## How to test
1. With no favorites, open Home: no "Favoritos" section; Home looks as before.
2. Favorite three titles on Title details, go back to Home: the section appears between the featured row and
   "Mais populares", newest first, badge "3"; switch tabs, the section stays the same.
3. Favorite 12 titles: Home shows 10, badge "12"; "Ver todos" shows all 12.
4. Tap a favorite on Home and on My favorites: details open; back returns to the same scroll position.
5. Turn the network off and relaunch: favorites still show their data.
6. Switch the device to English: copy reads "Favorites", "View all", "My Favorites", "3 titles".
7. Rotate on My favorites: the list keeps its scroll position.

## Change log
- 2026-11-23: Initial version.
- 2026-11-23: Plan approved.
- 2026-11-23: Implemented (plan 02 done).
