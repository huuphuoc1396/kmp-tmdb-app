# 03 - Title details robustness (cmp)

Feature: [task.md](../task.md)
Design: [Title details](../design/title-details.md)
Depends on: 02-cmp-title-details-happy-path
Status: Todo

## Goal
In `cmp`, Title details handles first load, failures, no network, stale data and pull-to-refresh, and stays correct with repeated taps, long content, rotation, process death, language changes and the largest font size.

## Scope
### In scope
- Loading state: spinner, "Voltar" only.
- Error state with "Tentar novamente" when nothing is cached.
- Showing cached details offline with no error; a failed refresh never removes them.
- Automatic refresh of details older than 24 hours while the cached ones stay visible.
- Pull-to-refresh, without duplicate requests.
- Opening the trailer or a recommendation exactly once on repeated taps.
- Long names, synopses and cast names; the largest system font size.
- Restoring the title and scroll positions after rotation or process death.
- Language change while the app is running.
- App: `cmp/`.
### Out of scope (handled later)
- The favorite button and its states -> 04-favorites
- Native apps -> 05-native-android-title-details, 06-native-ios-title-details

## Acceptance criteria
- **AC-13** First load
  - Given nothing is cached for the title in the current language
  - When the screen opens
  - Then a spinner is shown until the details arrive or the load fails
- **AC-14** Error without cache
  - Given nothing is cached for the title
  - When its load fails
  - Then an error message and a "Tentar novamente" button are shown
  - And when the user taps the button, the title loads again
- **AC-15** Offline with cache
  - Given the title was opened before in the current language
  - When the user opens it with no network
  - Then the cached details are shown immediately and no error is shown
  - And a failed refresh never removes cached details
- **AC-16** Freshness
  - Given the title's details were loaded more than 24 hours ago
  - When the screen shows the title
  - Then the details refresh automatically while the cached ones stay visible
- **AC-17** Pull-to-refresh
  - Given the details are shown
  - When the user pulls down
  - Then the title refreshes regardless of age, the details stay visible and the indicator shows until the refresh
    ends
  - And if the refresh fails, the details stay as they were and no error replaces them

## Edge cases
- **EC-2** "Assistir trailer" or a recommended poster tapped repeatedly or quickly -> the trailer or the details
  screen opens exactly once.
- **EC-4** Very long name -> it wraps onto more lines without overlapping the rating ring; the year stays attached
  to it.
- **EC-5** Very long synopsis -> shown in full; the screen scrolls.
- **EC-6** Long cast name -> wraps to two lines and is truncated inside its column.
- **EC-8** Rotation or process death on Title details -> the same title is shown with its scroll positions
  restored.
- **EC-9** Device language changes while the app is running -> the next display uses the new language; cached (partial: the favorite state staying unchanged is in 04)
  details of the other language are not shown as if they were in the new one. The favorite state is unchanged.
- **EC-11** Pull-to-refresh while a refresh is already running -> no duplicate request; the indicator ends when the
  running refresh ends.
- **EC-12** Largest system font size -> header, buttons, section titles and chips stay readable; nothing is clipped
  beyond the truncation in EC-6.
- **EC-13** Title opened from Home that TMDB no longer has -> the Error state is shown, and going back still works.

## Layers touched
- UI (states, pull-to-refresh)
- State holder

## How to test
1. Turn on airplane mode and open a title never opened before: the spinner shows, then the error message and "Tentar novamente". Turn the network back on and tap it: the details load.
2. Open a title, go back, turn on airplane mode and open it again: the details show at once with no error. Pull to refresh: the details stay, the indicator ends, no error replaces them.
3. With the network on, pull to refresh twice quickly: one refresh runs and the indicator ends with it.
4. Change the device clock (or wait) past 24 hours and reopen a cached title: it shows at once and refreshes in the background.
5. Tap "Assistir trailer" and a recommended poster several times quickly: each opens exactly once.
6. Open titles with long names, long synopses and long cast names; set the largest font size: everything wraps or truncates as specified.
7. Rotate the device, and kill the app in the background ("Don't keep activities" on Android): the same title and scroll positions come back.
8. Switch the device language while a title is open: the next display uses the new language.

## Change log
- 2026-11-20: Initial version.
