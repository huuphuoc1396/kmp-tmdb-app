# 03 - Home title lists (shared core)

Feature: [task.md](../task.md)
Depends on: 01-tmdb-network-access, 02-local-database-driver
Status: Done

## Goal
The shared core provides the ten Home lists (featured and "Mais populares" for each of the five categories) as
defined in task.md "Data source (agreed)". The lists are cached on the device per language, readable offline, and
refreshed automatically when older than 6 hours. Apps can observe and refresh them, but no screen shows them yet.

## Scope
### In scope
- The `title` context, created by this feature: title models, the Home lists repository (observe, refresh), its
  fake and contract test.
- The ten lists with exactly the endpoints and filters of task.md "Data source (agreed)", page 1 only; dates for
  `air_date.gte` / `primary_release_date.gte` computed from the current time (today − 7 days / − 90 days).
- Cache per list and per language (`pt-BR` / `en-US`) with the time it was fetched; the network only refreshes the
  cache, and readers always read the cache.
- Staleness: a list older than 6 hours is refreshed when it is observed; a forced refresh ignores the age.
- A failed refresh keeps cached titles and reports the error.
- Concurrent refreshes of the same list are not sent twice.
- Poster image address per title (or none when TMDB has no poster).
- The repository is reachable from the apps (DI, published domain, iOS entry point).
### Out of scope (handled later)
- Any UI -> 04-cmp-home-happy-path, 06-native-android-home, 07-native-ios-home
- Showing loading, error and empty states -> 05-cmp-home-robustness

## Acceptance criteria
- **AC-7** Offline with cache (partial: data: cached titles are readable with no network, and a failed refresh never removes them; display in 05-cmp-home-robustness and the native subtasks)
  - Given the category was loaded before
  - When the user opens it with no network
  - Then the cached titles are shown immediately and no error is shown
  - And a failed refresh never removes cached titles
- **AC-9** Freshness and refresh (partial: data: refresh when older than 6 hours on observe, forced refresh ignores the age; triggers from the UI in 05-cmp-home-robustness and the native subtasks)
  - Given a category's titles were loaded more than 6 hours ago
  - When Home shows that category (on launch or on switching tabs)
  - Then the titles refresh automatically while the cached ones stay visible
  - And when the user pulls to refresh, the selected category refreshes regardless of age
- **AC-10** Language (partial: data: title data is requested and cached in pt-BR or en-US; the screen copy is in 04-cmp-home-happy-path)
  - Given the device language is pt-BR
  - When Home is shown
  - Then the screen copy is Portuguese and title data (names, posters) is requested in pt-BR
  - And given any other device language, then the copy is English ("What do you want to watch today?",
    "Search", "Most popular") and title data is requested in en-US

## Edge cases
- **EC-7** Device language changes while the app is running -> the next Home display uses the new language; (partial: data: cached titles of one language are never returned for the other; reacting on screen in 05-cmp-home-robustness)
  cached titles of the other language are not shown as if they were in the new one.
- **EC-8** Device language is neither en nor pt-BR -> English copy and en-US title data. (partial: data: any language other than pt-BR maps to en-US)
- **EC-9** Pull-to-refresh while a refresh is already running -> no duplicate request; the indicator ends when the (partial: data: no duplicate request for a list already refreshing; the indicator in 05-cmp-home-robustness)
  running refresh ends.

## Layers touched
- Domain (title models, Home lists repository interface)
- Data (TMDB endpoints and DTOs, local database tables, refresh policy, DI)
- Fake (in-memory repository and contract test)
- iOS entry point and published shared core (exposing the repository)

## How to test
1. Host tests with mocked TMDB responses: each of the ten lists calls the agreed endpoint and filters and stores the
   titles; observing returns them.
2. Observe a list cached 7 hours ago: it refreshes; one cached 1 hour ago does not; a forced refresh always does.
3. Fail the network: cached titles stay and the error is reported; with no cache, the error is reported and the
   list stays empty.
4. Cache in pt-BR, then observe in en-US: the pt-BR titles are not returned.
5. `./gradlew publishSharedCore` exposes the repository to `native-android/` and to the iOS Swift Package.

## Change log
- 2026-11-17: Initial version.
