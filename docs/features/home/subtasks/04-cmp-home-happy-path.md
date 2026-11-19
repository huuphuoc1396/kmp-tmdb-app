# 04 - Home happy path (cmp)

Feature: [task.md](../task.md)
Design: [Home](../design/home.md)
Depends on: 03-home-title-lists
Status: Done

## Goal
In the Compose Multiplatform app (Android and iOS), the placeholder Home is replaced by the real Home as designed:
header, inert search field, avatar, five category tabs, and the featured and "Mais populares" rows with real TMDB
posters. Tapping a poster opens a placeholder details screen for that title.

## Scope
### In scope
- Home as in design/home.md: background and header glow, title, generic avatar, "Buscar" field with no action, tabs
  (Filmes selected), featured row, "Mais populares" heading and row, edge-to-edge, whole screen scrolls vertically.
- Switching tabs shows that category in both rows.
- Poster images loaded from TMDB; a placeholder tile with the title name when there is no image.
- A placeholder details screen showing the title's name; Back returns to Home.
- en and pt-BR copy for the title, search hint, tabs and heading.
- App: `cmp/` (Compose Multiplatform).
### Out of scope (handled later)
- Spinner, error and retry, empty rows, pull-to-refresh, offline display, refresh on tab switch, restoring tab and
  scroll, screen-reader labels, large fonts -> 05-cmp-home-robustness
- Native apps -> 06-native-android-home, 07-native-ios-home
- The real details screen and Search -> separate features

## Acceptance criteria
- **AC-1** Home layout
  - Given the user enters Home
  - When the screen is shown
  - Then it shows the title, the avatar, the "Buscar" field, the five tabs with Filmes selected, a featured
    poster row and a "Mais populares" poster row
- **AC-2** Switching category
  - Given Home is shown
  - When the user taps a category tab
  - Then that tab is shown as selected and both rows show that category's titles, as defined in Data source
- **AC-3** Opening a title (partial: Back returns to Home; keeping the same tab and scroll positions across process death is in 05-cmp-home-robustness)
  - Given posters are shown
  - When the user taps a poster
  - Then the details screen for that title opens
  - And when the user goes back, Home shows the same tab and row scroll positions as before
- **AC-4** Search field has no action
  - Given Home is shown
  - When the user taps the "Buscar" field
  - Then nothing happens: no screen opens, no keyboard appears, and the field cannot be focused or typed into
  - And a screen reader skips the field
- **AC-5** Avatar has no action
  - Given Home is shown
  - When the user taps the avatar
  - Then nothing happens, and a screen reader skips the avatar
- **AC-10** Language (partial: screen copy in both languages; title data language comes from 03-home-title-lists)
  - Given the device language is pt-BR
  - When Home is shown
  - Then the screen copy is Portuguese and title data (names, posters) is requested in pt-BR
  - And given any other device language, then the copy is English ("What do you want to watch today?",
    "Search", "Most popular") and title data is requested in en-US
- **AC-11** Poster without image (partial: placeholder tile with the title name, tappable; screen-reader announcement in 05-cmp-home-robustness)
  - Given a title has no poster image
  - When it appears in a row
  - Then a neutral placeholder tile with the title's name is shown, and it can be tapped like any poster
  - And every poster is announced by a screen reader with the title's name as a button

## Edge cases
- **EC-8** Device language is neither en nor pt-BR -> English copy and en-US title data. (partial: screen copy)

## Layers touched
- UI (Home screen, poster row, tabs, placeholder details screen, design-system pieces it needs)
- State holder (Home ViewModel)
- Navigation (Home as the start destination after onboarding, details route)
- Image loading
- Resources (strings en + pt-BR, search icon, person icon)

## How to test
1. Run the cmp app on Android and on iOS past onboarding: Home matches design/home.md, Filmes is selected, and both
   rows show TMDB posters.
2. Tap each tab: both rows switch to that category.
3. Tap a poster: the details placeholder shows the title's name; Back returns to Home.
4. Tap "Buscar" and the avatar: nothing happens and no keyboard appears.
5. Set the device to pt-BR, then to English: the copy and title names follow.
6. Find a title without a poster (or use a mocked list): it shows the placeholder tile with its name.

## Change log
- 2026-11-17: Initial version.
