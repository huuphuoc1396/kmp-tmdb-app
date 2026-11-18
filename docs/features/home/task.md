# Home

Status: Confirmed
Source: chat (`/spark` request, 2026-11-17); design: Figma
[TMDB (Community) – Home](https://www.figma.com/design/G4YiZkobsY5gJP94z8pFlh/TMDB--Community-?node-id=29-2)

## Goal
After onboarding, users land on Home and need a quick way to find something to watch. Home shows a search
field (display only for now), category tabs (Filmes, Séries, Animes, Novelas, Documentários), a featured poster row and a
"Mais populares" row for the selected category. Any poster opens that title's details. Home replaces the
current placeholder Home. Success: users see titles right after opening the app, even offline once the
category has been loaded, and reach a title's details in one tap.

## Scope
### In scope
- The Home screen in all three apps: `cmp`, `native-android` and `native-ios`. The shared core provides the data.
- Header with the title "O que você quer assistir hoje?" and a decorative, generic person avatar.
- "Buscar" search field shown as in the design, with no action.
- Five horizontally scrolling category tabs: Filmes, Séries, Animes, Novelas, Documentários.
- Two horizontal poster rows per category: a featured row and "Mais populares".
- Opening a title's details from any poster.
- Pull-to-refresh of the selected category.
- Offline display of cached titles, with automatic refresh when the data is stale.
- Copy localized for en and pt-BR. Title content comes from TMDB in the matching language.

### Out of scope
- The Search screen and any search behavior (opening Search, typing, results).
- The title details screen itself (separate feature; a placeholder showing the title's name until then).
- Profile, account or sign-in. The avatar has no action.
- Favorites.
- Pages beyond the first (no infinite scroll, no "see all").
- Mixing films and series in one tab (Animes is series only, Documentários is films only).
- Analytics.

## User flow
1. The user enters Home (after onboarding, or directly on later launches).
2. Home shows the header, avatar, "Buscar" field, the tabs with Filmes selected, and the Filmes featured and
   "Mais populares" rows.
3. The user scrolls a row sideways to browse posters.
4. The user taps another tab; both rows switch to that category.
5. The user taps a poster; the details screen for that title opens. Back returns to Home as it was.
6. ~~Or the user taps "Buscar"; the Search screen opens.~~ (removed: the search field has no action)
7. The user pulls down to refresh the selected category.

## UI states
Header, avatar, search field and tabs are always shown and usable. The states below apply to the content
area of the selected category.

| State | When | What the user sees / can do |
|---|---|---|
| Loading | Nothing cached for the category and a load is in progress | A spinner in the content area; can switch tabs |
| Content | Titles available (cached or freshly loaded) | Featured row and "Mais populares" row of posters; can scroll, tap a poster, pull to refresh |
| Refreshing | Pull-to-refresh or automatic refresh while titles are shown | Titles stay visible; the pull-to-refresh indicator shows during a manual refresh |
| Error | Nothing cached for a row and its load failed | A short error message and a "Tentar novamente" button in that row |
| Empty | The load succeeded but the row has no titles | A short message that there is nothing to show in this row |

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

## Data source (agreed)
Agreed with the user (2026-11-17): Home uses these TMDB API v3 endpoints. Every request sends
`language=pt-BR` or `language=en-US` (per AC-10), `page=1` (up to 20 titles) and, where supported,
`include_adult=false`. TMDB trending cannot be filtered by genre, so Animes, Novelas and Documentários use Discover
with a recent-date window for their featured row.

| Tab | Featured row | Mais populares |
|---|---|---|
| Filmes | `GET /trending/movie/week` | `GET /movie/popular` |
| Séries | `GET /trending/tv/week` | `GET /tv/popular` |
| Animes (series) | `GET /discover/tv` `with_genres=16`, `with_original_language=ja`, `air_date.gte=<today − 7 days>`, `sort_by=popularity.desc` | `GET /discover/tv` `with_genres=16`, `with_original_language=ja`, `sort_by=popularity.desc` |
| Novelas (Latin American telenovelas) | `GET /discover/tv` `with_genres=10766`, `with_origin_country=BR\|MX\|CO\|AR\|VE\|CL\|PE`, `air_date.gte=<today − 7 days>`, `sort_by=popularity.desc` | `GET /discover/tv` `with_genres=10766`, `with_origin_country=BR\|MX\|CO\|AR\|VE\|CL\|PE`, `sort_by=popularity.desc` |
| Documentários (films) | `GET /discover/movie` `with_genres=99`, `primary_release_date.gte=<today − 90 days>`, `sort_by=popularity.desc` | `GET /discover/movie` `with_genres=99`, `sort_by=popularity.desc` |

Genre IDs: Animation 16, Documentary 99, Soap 10766 (TV only).

## Constraints
- Platforms: Android (minSdk 24) and iOS 16+, in `cmp`, `native-android` and `native-ios`.
- Offline-first: cached titles show without network; errors never clear cached data.
- Staleness: Home lists refresh after 6 hours (`docs/architecture.md`).
- Cached titles are stored per language (`pt-BR` / `en-US`).
- The TMDB token is never in source or committed files.
- Dark theme with purple accents, as in the design.
- Localization: en and pt-BR.
- Accessibility: posters and tabs are labeled and meet the minimum touch target; the avatar is decorative.
- No analytics.
- The existing onboarding behavior (shown once, then Home) must not change.

## Designs
- [Home](design/home.md)

## Open questions
- [ ] Confirm that `/discover/movie` supports `primary_release_date.gte` and `include_adult` as expected (owner:
  developer, before blueprint of the data layer).
- [ ] Final copy for the empty-row and error messages in en and pt-BR (owner: user, before the UI subtask).

## Change log
- 2026-11-17: Initial version.
- 2026-11-17: Confirmed by the user.
- 2026-11-17: Linked designs: home.
- 2026-11-17: Search field has no action; Search screen removed from scope (AC-4 changed, user flow step 6
  removed). Confirmed by the user.

## Subtasks
1. [01 - TMDB network access](subtasks/01-tmdb-network-access.md)
2. [02 - Local database driver](subtasks/02-local-database-driver.md)
3. [03 - Home title lists (shared core)](subtasks/03-home-title-lists.md)
4. [04 - Home happy path (cmp)](subtasks/04-cmp-home-happy-path.md)
5. [05 - Home robustness (cmp)](subtasks/05-cmp-home-robustness.md)
6. [06 - Home on Android (native)](subtasks/06-native-android-home.md)
7. [07 - Home on iOS (native)](subtasks/07-native-ios-home.md)
