# 06 - Title details on iOS (native)

Feature: [task.md](../task.md)
Design: [Title details](../design/title-details.md)
Depends on: 01-title-details-data, 04-favorites
Status: Todo

## Goal
In the standalone `native-ios` app, tapping a poster on Home opens the full Title details screen as designed, with the favorite button, trailer, recommendations, loading, error, offline and pull-to-refresh behavior of the `cmp` app, using only the published shared core.

## Scope
### In scope
- The whole Title details screen in `native-ios`, replacing its placeholder: every state in design/title-details.md.
- Favorites through the shared core.
- Opening the trailer in the YouTube app or the browser; opening recommendations on top.
- en and pt-BR copy; screen reader labels.
- Every acceptance criterion and edge case below, for this app.
- App: `native-ios/` only, using the shared core as published as the `TmdbCore` Swift package.
### Out of scope (handled later)
- The other native app -> 05-native-android-title-details

## Acceptance criteria
- **AC-1** Layout
  - Given the user opens a title that has every piece of data
  - When the details are shown
  - Then the screen shows, from top to bottom: the artwork with "Voltar" and the heart over it, the rating ring
    next to "Name (year)" and the date and runtime line, the synopsis, "Assistir trailer", "Elenco principal",
    "Categoria(s)" and "Recomendações", as in the design
- **AC-2** Back navigation
  - Given Title details is shown
  - When the user taps "Voltar" or uses the system back
  - Then the previous screen is shown as it was (Home with the same tab and row scroll positions, or the previous
    title's details)
- **AC-3** Favorite
  - Given the details are shown
  - When the user taps the heart
  - Then the heart shows as filled and the title is stored as a favorite on the device
  - And when the user taps it again, the heart shows as empty and the title is no longer a favorite
  - And the state is kept after closing and reopening the app, works with no network, and is the same for the
    title in both languages
  - And a screen reader announces the heart as a toggle "Favorite" / "Favorito" with its on/off state
- **AC-4** Favorite only with details
  - Given the screen is in the Loading or Error state
  - When it is shown
  - Then no heart is shown; it appears once the details are shown
- **AC-5** Trailer
  - Given the title has a YouTube trailer in the current language
  - When the user taps "Assistir trailer"
  - Then that trailer opens in the YouTube app, or in the browser if the YouTube app is not installed
  - And given there is none in the current language but there is an English YouTube trailer, then that one opens
  - And given the title has no YouTube trailer at all, then "Assistir trailer" is not shown
- **AC-6** Rating
  - Given the title has votes
  - When the details are shown
  - Then the ring shows the TMDB score as a whole percentage (e.g. 4.5 → "45%") and fills to that percentage
  - And given the title has no votes, then the ring is empty and shows "NR", and a screen reader announces
    "Not rated yet" / "Ainda sem avaliação"
- **AC-7** Name, year, date and runtime
  - Given the details are shown
  - When the header is drawn
  - Then the name is followed by the release year in parentheses (first air year for series)
  - And the line below shows the release date (first air date for series) in the device's date format, with no
    country code, then "•" and the runtime as hours and minutes (e.g. "1h 53m", "45m"); for series, the runtime is
    that of one episode
  - And any part that is unknown (year, date or runtime) is left out, together with its separator
- **AC-8** Synopsis
  - Given the title has no synopsis in the current language
  - When the details are shown
  - Then "Synopsis not available." / "Sinopse indisponível." is shown in its place
- **AC-9** Main cast
  - Given the title has cast members
  - When the details are shown
  - Then "Elenco principal" shows up to the first 10 cast members in TMDB order, each as a round photo with the
    name below, in a row that scrolls sideways
  - And a cast member without a photo shows a neutral placeholder photo
  - And tapping a cast member does nothing
  - And given the title has no cast, then the "Elenco principal" section is not shown
- **AC-10** Genres
  - Given the title has genres
  - When the details are shown
  - Then "Categoria(s)" shows one chip per genre in the current language, and tapping a chip does nothing
  - And given the title has no genres, then the section is not shown
- **AC-11** Recommendations
  - Given TMDB has recommendations for the title
  - When the details are shown
  - Then "Recomendações" shows up to 20 posters in a row that scrolls sideways; a poster without an image shows a
    neutral placeholder tile with the title's name
  - And when the user taps a poster, that title's details open on top, and "Voltar" returns to the previous title
  - And every poster is announced by a screen reader with the title's name as a button
  - And given there are no recommendations, then the section is not shown
- **AC-12** Artwork
  - Given the title has a poster image
  - When the details are shown
  - Then the poster fills the top of the screen as in the design, with "Voltar" and the heart readable over it
  - And given it has no poster but has a backdrop, then the backdrop is shown there instead
  - And given it has neither, then a neutral placeholder is shown in its place
  - And the artwork is decorative for screen readers
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
- **AC-18** Language
  - Given the device language is pt-BR
  - When the screen is shown
  - Then the screen copy is Portuguese and title data (name, synopsis, genres, cast, recommendations) is requested
    in pt-BR
  - And given any other device language, then the copy is English ("Back", "Watch trailer", "Main cast",
    "Genres", "Recommendations") and title data is requested in en-US

## Edge cases
- **EC-1** Heart tapped repeatedly or quickly -> each tap toggles once; the final state matches the last tap and
  is the one stored.
- **EC-2** "Assistir trailer" or a recommended poster tapped repeatedly or quickly -> the trailer or the details
  screen opens exactly once.
- **EC-3** Neither the YouTube app nor a browser can open the trailer -> nothing crashes; the screen stays as it
  was.
- **EC-4** Very long name -> it wraps onto more lines without overlapping the rating ring; the year stays attached
  to it.
- **EC-5** Very long synopsis -> shown in full; the screen scrolls.
- **EC-6** Long cast name -> wraps to two lines and is truncated inside its column.
- **EC-7** Many details opened on top of each other through recommendations -> each "Voltar" returns one step;
  the same title may appear more than once in the stack.
- **EC-8** Rotation or process death on Title details -> the same title is shown with its scroll positions
  restored.
- **EC-9** Device language changes while the app is running -> the next display uses the new language; cached
  details of the other language are not shown as if they were in the new one. The favorite state is unchanged.
- **EC-10** Device language is neither en nor pt-BR -> English copy and en-US title data.
- **EC-11** Pull-to-refresh while a refresh is already running -> no duplicate request; the indicator ends when the
  running refresh ends.
- **EC-12** Largest system font size -> header, buttons, section titles and chips stay readable; nothing is clipped
  beyond the truncation in EC-6.
- **EC-13** Title opened from Home that TMDB no longer has -> the Error state is shown, and going back still works.

## Layers touched
- UI (screen, components, states, copy)
- State holder
- Navigation (from Home and between details)

## How to test
1. Publish the shared core and run `native-ios` (`scripts/run-native-ios.sh`); repeat the "How to test" steps of 02, 03 and 04 in this app.
2. Check the screen against design/title-details.md side by side with the `cmp` app.

## Change log
- 2026-11-20: Initial version.
