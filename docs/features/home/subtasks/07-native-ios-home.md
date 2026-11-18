# 07 - Home on iOS (native)

Feature: [task.md](../task.md)
Design: [Home](../design/home.md)
Depends on: 03-home-title-lists
Status: Todo

## Goal
In the native iOS app (SwiftUI), Home works end to end as in the cmp app: the designed layout, five tabs, featured and
"Mais populares" rows with TMDB posters, a placeholder details screen, offline cache, loading, error, empty and
refresh states, state restoration and accessibility, in en / pt-BR.

## Scope
### In scope
- Everything in task.md for Home, in this app: layout as in design/home.md, inert search field and avatar, tabs,
  rows, poster placeholder tiles, placeholder details screen, all UI states, pull-to-refresh, offline display,
  restoring tab and scroll, accessibility, en and pt-BR copy.
- Replacing this app's placeholder Home.
- App: the standalone `native-ios/` project, using the shared core through the Swift Package `TmdbCore` (`SharedCore` entry point); Swift ViewModels are `ObservableObject`s (iOS 16).
### Out of scope (handled later)
- The other native app -> 06-native-android-home
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
- **AC-3** Opening a title
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
- **AC-6** First load
  - Given nothing is cached for the selected category
  - When Home loads it
  - Then a spinner is shown in the content area until titles arrive or the load fails
- **AC-7** Offline with cache
  - Given the category was loaded before
  - When the user opens it with no network
  - Then the cached titles are shown immediately and no error is shown
  - And a failed refresh never removes cached titles
- **AC-8** Error without cache
  - Given nothing is cached for a row
  - When its load fails
  - Then the row shows an error message and a "Tentar novamente" button
  - And when the user taps the button, the row loads again
- **AC-9** Freshness and refresh
  - Given a category's titles were loaded more than 6 hours ago
  - When Home shows that category (on launch or on switching tabs)
  - Then the titles refresh automatically while the cached ones stay visible
  - And when the user pulls to refresh, the selected category refreshes regardless of age
- **AC-10** Language
  - Given the device language is pt-BR
  - When Home is shown
  - Then the screen copy is Portuguese and title data (names, posters) is requested in pt-BR
  - And given any other device language, then the copy is English ("What do you want to watch today?",
    "Search", "Most popular") and title data is requested in en-US
- **AC-11** Poster without image
  - Given a title has no poster image
  - When it appears in a row
  - Then a neutral placeholder tile with the title's name is shown, and it can be tapped like any poster
  - And every poster is announced by a screen reader with the title's name as a button

## Edge cases
- **EC-1** Poster tapped repeatedly or quickly -> the details screen opens exactly once.
- **EC-2** Very long title name on a placeholder tile -> the name wraps and is truncated inside the tile.
- **EC-3** A row returns no titles (e.g. no Latin American novela aired in the last 7 days) -> that row shows the
  empty message; the other row is unaffected.
- **EC-4** One row loads and the other fails -> the loaded row shows titles; the failed row follows AC-8 (or keeps
  its cache, per AC-7).
- **EC-5** User switches tabs while a load is in progress -> the newly selected category is shown; the earlier
  load may complete in the background and is cached, but never shows under the wrong tab.
- **EC-6** Rotation or process death on Home -> the selected tab and row scroll positions are restored.
- **EC-7** Device language changes while the app is running -> the next Home display uses the new language;
  cached titles of the other language are not shown as if they were in the new one.
- **EC-8** Device language is neither en nor pt-BR -> English copy and en-US title data.
- **EC-9** Pull-to-refresh while a refresh is already running -> no duplicate request; the indicator ends when the
  running refresh ends.
- **EC-10** Largest system font size -> header, tabs and row titles stay readable; tabs scroll; nothing is clipped
  beyond the truncation in EC-2.

## Layers touched
- UI (SwiftUI screens, poster row, tabs, states, placeholder details screen)
- State holder (ViewModel)
- Navigation
- Image loading
- Resources (strings en + pt-BR, search icon, person icon)

## How to test
1. Publish the shared core, build and run the app past onboarding: Home matches design/home.md with TMDB posters.
2. Tap each tab: both rows switch; tap a poster: the details placeholder shows the title's name; Back returns with
   the same tab and scroll.
3. Tap "Buscar" and the avatar: nothing happens.
4. Repeat the robustness checks of 05-cmp-home-robustness (spinner, offline errors and retry, offline cache,
   pull-to-refresh, stale refresh on tab switch, rotation and process death, screen reader, largest font, double
   tap) in this app.
5. Switch the device language between pt-BR and English: the copy and title data follow.

## Change log
- 2026-11-17: Initial version.
