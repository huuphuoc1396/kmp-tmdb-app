# 02 - Title details happy path (cmp)

Feature: [task.md](../task.md)
Design: [Title details](../design/title-details.md)
Depends on: 01-title-details-data
Status: Done

## Goal
In the Compose Multiplatform app (Android and iOS), tapping a poster on Home opens the real Title details screen as designed, with real TMDB data. Users can go back, open the trailer on YouTube and open a recommended title's details.

## Scope
### In scope
- Title details as in design/title-details.md (content state): artwork with its fade, "Voltar", rating ring, name with year, date and runtime line, divider, synopsis, "Assistir trailer", "Elenco principal", "Categoria(s)", "Recomendações"; whole screen scrolls; "Voltar" stays fixed; cast and recommendations rows scroll sideways.
- Leaving out sections without data, and the missing-synopsis text.
- Back with "Voltar" and the system back; Home keeps its tab and scroll positions.
- Opening the trailer in the YouTube app or the browser.
- Opening a recommended title's details on top; back returns to the previous title.
- Poster, cast photo and artwork images from TMDB, with placeholders when missing.
- en and pt-BR copy; screen reader labels for the rating, recommended posters and the decorative artwork.
- Replacing the placeholder details screen in `cmp`.
- App: `cmp/`.
### Out of scope (handled later)
- Spinner, error and retry, offline display, automatic refresh, pull-to-refresh, repeated taps, long content, rotation and process death, large fonts -> 03-cmp-title-details-robustness
- The favorite button -> 04-favorites
- Native apps -> 05-native-android-title-details, 06-native-ios-title-details

## Acceptance criteria
- **AC-1** Layout (partial: everything but the heart, which is added in 04)
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
- **AC-11** Recommendations (partial: opening exactly once on repeated taps is in 03 (EC-2))
  - Given TMDB has recommendations for the title
  - When the details are shown
  - Then "Recomendações" shows up to 20 posters in a row that scrolls sideways; a poster without an image shows a
    neutral placeholder tile with the title's name
  - And when the user taps a poster, that title's details open on top, and "Voltar" returns to the previous title
  - And every poster is announced by a screen reader with the title's name as a button
  - And given there are no recommendations, then the section is not shown
- **AC-12** Artwork (partial: "Voltar" readable over the artwork; the heart is added in 04)
  - Given the title has a poster image
  - When the details are shown
  - Then the poster fills the top of the screen as in the design, with "Voltar" and the heart readable over it
  - And given it has no poster but has a backdrop, then the backdrop is shown there instead
  - And given it has neither, then a neutral placeholder is shown in its place
  - And the artwork is decorative for screen readers
- **AC-18** Language
  - Given the device language is pt-BR
  - When the screen is shown
  - Then the screen copy is Portuguese and title data (name, synopsis, genres, cast, recommendations) is requested
    in pt-BR
  - And given any other device language, then the copy is English ("Back", "Watch trailer", "Main cast",
    "Genres", "Recommendations") and title data is requested in en-US

## Edge cases
- **EC-3** Neither the YouTube app nor a browser can open the trailer -> nothing crashes; the screen stays as it
  was.
- **EC-7** Many details opened on top of each other through recommendations -> each "Voltar" returns one step;
  the same title may appear more than once in the stack.
- **EC-10** Device language is neither en nor pt-BR -> English copy and en-US title data.

## Layers touched
- UI (screen, components, copy)
- State holder
- Navigation (from Home and between details)

## How to test
1. Run the `cmp` app on Android and on iOS; on Home tap a film poster. The details screen opens as in the design, with that film's data.
2. Check the rating ring percentage, "Name (year)", the date and runtime line, synopsis, cast (at most 10), genre chips and recommendations (at most 20).
3. Tap "Assistir trailer": the trailer opens in YouTube or the browser. Open a title with no trailer: the button is not shown.
4. Tap a recommended poster: its details open. Tap "Voltar": the previous title is shown. Tap "Voltar" again: Home shows the same tab and scroll positions.
5. Open a series from the "Séries" tab: the first air year and date and the episode runtime are shown.
6. Find titles with no votes ("NR"), no synopsis, no cast, no recommendations or no poster, and check each matches the design and task.md.
7. Switch the device language to English and to another language: the copy is English and the data is en-US.
8. Uninstall YouTube (or on a simulator without it): the trailer opens in the browser.

## Change log
- 2026-11-20: Initial version.
- 2026-11-20: Plan approved (implement-plans/02-cmp-title-details-happy-path.md).
- 2026-11-20: Implemented (plan 02 done).
