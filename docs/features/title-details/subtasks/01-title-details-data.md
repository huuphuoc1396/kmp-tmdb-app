# 01 - Title details data (shared core)

Feature: [task.md](../task.md)
Depends on: none
Status: Done

## Goal
The shared core can load the details of any film or series opened from Home: name, year, dates, runtime, rating, synopsis, poster and backdrop, genres, up to 10 cast members, the chosen trailer and up to 20 recommendations. They are cached per language, readable offline and refreshed when older than 24 hours. Nothing on screen changes yet; the apps keep the placeholder.

## Scope
### In scope
- Loading a title's details from TMDB for films and series, in pt-BR or en-US.
- Cast (first 10 in TMDB order), genres, recommendations (up to 20) and videos as part of the details.
- Choosing the trailer: a YouTube trailer in the current language, else an English YouTube trailer, else none.
- Rating as a whole percentage, and "no votes" as its own case.
- Runtime for films, and the runtime of one episode for series; first air date and year for series.
- Poster, with the backdrop as the artwork when there is no poster.
- Caching per language: observe the cached details, refresh from TMDB, keep the cache when a refresh fails.
- Staleness: details older than 24 hours count as stale.
- Making the details available to the apps (`cmp`, `native-android` through the published core, `native-ios` through the Swift package).
### Out of scope (handled later)
- The details screen in `cmp` -> 02-cmp-title-details-happy-path
- Loading, error, pull-to-refresh and offline display on screen -> 03-cmp-title-details-robustness
- Favorites -> 04-favorites
- Native apps -> 05-native-android-title-details, 06-native-ios-title-details

## Acceptance criteria
- **AC-5** Trailer (partial: choosing the trailer; opening it is in 02)
  - Given the title has a YouTube trailer in the current language
  - When the user taps "Assistir trailer"
  - Then that trailer opens in the YouTube app, or in the browser if the YouTube app is not installed
  - And given there is none in the current language but there is an English YouTube trailer, then that one opens
  - And given the title has no YouTube trailer at all, then "Assistir trailer" is not shown
- **AC-6** Rating (partial: the percentage and the no-votes case; the ring is drawn in 02)
  - Given the title has votes
  - When the details are shown
  - Then the ring shows the TMDB score as a whole percentage (e.g. 4.5 → "45%") and fills to that percentage
  - And given the title has no votes, then the ring is empty and shows "NR", and a screen reader announces
    "Not rated yet" / "Ainda sem avaliação"
- **AC-7** Name, year, date and runtime (partial: year, date and runtime values; their display is in 02)
  - Given the details are shown
  - When the header is drawn
  - Then the name is followed by the release year in parentheses (first air year for series)
  - And the line below shows the release date (first air date for series) in the device's date format, with no
    country code, then "•" and the runtime as hours and minutes (e.g. "1h 53m", "45m"); for series, the runtime is
    that of one episode
  - And any part that is unknown (year, date or runtime) is left out, together with its separator
- **AC-9** Main cast (partial: the first 10 cast members in TMDB order; their display is in 02)
  - Given the title has cast members
  - When the details are shown
  - Then "Elenco principal" shows up to the first 10 cast members in TMDB order, each as a round photo with the
    name below, in a row that scrolls sideways
  - And a cast member without a photo shows a neutral placeholder photo
  - And tapping a cast member does nothing
  - And given the title has no cast, then the "Elenco principal" section is not shown
- **AC-10** Genres (partial: genre names in the current language; their display is in 02)
  - Given the title has genres
  - When the details are shown
  - Then "Categoria(s)" shows one chip per genre in the current language, and tapping a chip does nothing
  - And given the title has no genres, then the section is not shown
- **AC-11** Recommendations (partial: up to 20 recommendations; their display and navigation are in 02)
  - Given TMDB has recommendations for the title
  - When the details are shown
  - Then "Recomendações" shows up to 20 posters in a row that scrolls sideways; a poster without an image shows a
    neutral placeholder tile with the title's name
  - And when the user taps a poster, that title's details open on top, and "Voltar" returns to the previous title
  - And every poster is announced by a screen reader with the title's name as a button
  - And given there are no recommendations, then the section is not shown
- **AC-12** Artwork (partial: poster, else backdrop, else none; the display is in 02)
  - Given the title has a poster image
  - When the details are shown
  - Then the poster fills the top of the screen as in the design, with "Voltar" and the heart readable over it
  - And given it has no poster but has a backdrop, then the backdrop is shown there instead
  - And given it has neither, then a neutral placeholder is shown in its place
  - And the artwork is decorative for screen readers
- **AC-15** Offline with cache (partial: cached details are kept and readable offline; the screen behavior is in 03)
  - Given the title was opened before in the current language
  - When the user opens it with no network
  - Then the cached details are shown immediately and no error is shown
  - And a failed refresh never removes cached details
- **AC-16** Freshness (partial: details older than 24 hours are stale and refresh; the screen behavior is in 03)
  - Given the title's details were loaded more than 24 hours ago
  - When the screen shows the title
  - Then the details refresh automatically while the cached ones stay visible
- **AC-18** Language (partial: title data is requested in pt-BR or en-US; the screen copy is in 02)
  - Given the device language is pt-BR
  - When the screen is shown
  - Then the screen copy is Portuguese and title data (name, synopsis, genres, cast, recommendations) is requested
    in pt-BR
  - And given any other device language, then the copy is English ("Back", "Watch trailer", "Main cast",
    "Genres", "Recommendations") and title data is requested in en-US

## Edge cases
- **EC-9** Device language changes while the app is running -> the next display uses the new language; cached (partial: details are cached per language, so one language's cache is never returned for the other; the screen behavior is in 03)
  details of the other language are not shown as if they were in the new one. The favorite state is unchanged.
- **EC-10** Device language is neither en nor pt-BR -> English copy and en-US title data. (partial: en-US title data; the English copy is in 02)
- **EC-13** Title opened from Home that TMDB no longer has -> the Error state is shown, and going back still works. (partial: a title TMDB no longer has is reported as not found; the Error state is in 03)

## Layers touched
- Domain (details model, repository interface)
- Data (TMDB endpoints, local cache, refresh policy)
- Dependency injection
- iOS entry point

## How to test
1. Automated tests cover the details of a film and of a series, the trailer choice (current language, English fallback, none), the rating percentage and the no-votes case, the 10-cast and 20-recommendation limits, poster/backdrop fallback, per-language caching, staleness after 24 hours, and a failed refresh keeping the cache.
2. Run the `cmp` app: Home still opens the placeholder details for a poster; nothing on screen has changed.
3. Publish the shared core and build `native-android` and `native-ios`: both still build and run with the placeholder.

## Change log
- 2026-11-20: Initial version.
- 2026-11-20: Plan approved (implement-plans/01-title-details-data.md).
- 2026-11-20: Implemented (plan 01 done).
