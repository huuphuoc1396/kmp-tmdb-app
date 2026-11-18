# 05 - Home robustness (cmp)

Feature: [task.md](../task.md)
Design: [Home](../design/home.md)
Depends on: 04-cmp-home-happy-path
Status: Todo

## Goal
Home in the cmp app handles first loads, failures, empty rows, offline use, refreshing, interruptions and
accessibility as specified, so it is ready to ship.

## Scope
### In scope
- Loading spinner, per-row empty message, per-row error with "Tentar novamente" (design/home.md States).
- Pull-to-refresh of the selected category; automatic refresh of stale data on launch and on tab switch.
- Offline display of cached titles with no error.
- Restoring the selected tab and row scroll positions after details, rotation and process death.
- Screen-reader labels, minimum touch targets, largest font size.
- Repeated taps, tab switches during loads, language changes while running.
- App: `cmp/`.
### Out of scope (handled later)
- Native apps -> 06-native-android-home, 07-native-ios-home

## Acceptance criteria
- **AC-3** Opening a title (partial: completes it: same tab and scroll positions after returning from details, including after process death)
  - Given posters are shown
  - When the user taps a poster
  - Then the details screen for that title opens
  - And when the user goes back, Home shows the same tab and row scroll positions as before
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
- **AC-11** Poster without image (partial: completes it: every poster announced as a button with the title's name)
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
- UI (states, pull-to-refresh, accessibility, adaptive layout)
- State holder (Home ViewModel: state restoration, refresh triggers)

## How to test
1. Fresh install with network: the spinner shows, then the rows.
2. Fresh install in airplane mode: each row shows the error and "Tentar novamente"; turn the network on and tap it:
   the row loads.
3. Load all tabs, then go offline and relaunch: cached titles show with no error.
4. Pull down: the selected category refreshes; pull again while refreshing: no duplicate request.
5. Set the device clock 7 hours ahead (or use a short TTL in a debug build), switch tabs: the data refreshes while the
   old titles stay visible.
6. Pick a tab and scroll a row, rotate, then kill the app in the background and return: the same tab and scroll
   positions.
7. TalkBack / VoiceOver: avatar and search field are skipped; tabs and posters are announced with their names.
8. Largest font size: nothing is clipped beyond the poster-name truncation.
9. Double-tap a poster: one details screen opens.

## Change log
- 2026-11-17: Initial version.
