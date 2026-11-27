# Vietnamese language

Status: Confirmed
Source: chat (2026-11-26)

## Goal
The app speaks only English and Brazilian Portuguese, so people whose device is set to Vietnamese get English.
Add Vietnamese to all three apps: when the device language is Vietnamese, the screen copy is in Vietnamese and title
content comes from TMDB in Vietnamese. Success: a user with a Vietnamese device sees every existing screen in
Vietnamese, and English and pt-BR users see no change.

## Scope
### In scope
- Vietnamese in `cmp`, `native-android` and `native-ios`, on every existing screen: Onboarding, Home (with the
  favorites section), Title details, My favorites and the remove-favorite dialog.
- Title content (names, posters, synopsis, genres, cast, recommendations) requested from TMDB in `vi-VN` and cached
  separately for Vietnamese, like the existing languages.
- Dates and runtimes formatted the Vietnamese way when the app is in Vietnamese.
- `README.md` (and `native-android/README.md`, `native-ios/README.md` where they name the languages) states that the
  app supports English, Brazilian Portuguese and Vietnamese.

### Out of scope
- An in-app language picker; the language still follows the device.
- A Vietnamese translation of the README.
- Translating TMDB content that TMDB has no Vietnamese version of.
- Changes to English or pt-BR copy or behavior.

## User flow
1. The user sets the device language to Vietnamese (any `vi` locale) and opens the app.
2. Onboarding, Home, Title details, My favorites and the remove-favorite dialog show Vietnamese copy.
3. Home rows, title details and favorites show title data that TMDB returns for `vi-VN`.
4. The user switches the device back to English or pt-BR; the next display is in that language again.

## UI states
No new states. Every existing state (loading, content, empty, error, offline) shows Vietnamese copy when the app is
in Vietnamese.

## Acceptance criteria
- **AC-1** Vietnamese copy
  - Given the device language is any locale whose language is `vi` (`vi-VN`, `vi`, `vi-US`, …)
  - When the user opens any screen or the remove-favorite dialog, in any of the three apps
  - Then every piece of app copy (titles, buttons, tabs, empty and error messages, accessibility labels) is in
    Vietnamese
- **AC-2** Vietnamese title data
  - Given the app is in Vietnamese
  - When Home lists, title details or favorites load
  - Then title data is requested from TMDB in `vi-VN` and shown from the Vietnamese cache
  - And given TMDB has no Vietnamese text for a field (for example the synopsis), then the field is handled the same
    way as a missing field in the other languages today
- **AC-3** Trailer
  - Given the app is in Vietnamese and the title has a Vietnamese YouTube trailer
  - When the user taps the trailer action
  - Then the Vietnamese trailer opens
  - And given there is none in Vietnamese but there is an English YouTube trailer, then the English one opens
  - And given there is no YouTube trailer at all, then the trailer action is not shown
- **AC-4** Favorites before Vietnamese data has loaded
  - Given the app is in Vietnamese and a favorite has no stored Vietnamese data
  - When Home's favorites section or My favorites is shown
  - Then its stored en-US data is shown, or its pt-BR data if there is no en-US data
  - And it is replaced by the Vietnamese data once that has loaded
- **AC-5** Dates and runtimes
  - Given the app is in Vietnamese
  - When a release date or runtime is shown
  - Then it uses Vietnamese formatting (for example "26 thg 11, 2026" and "2 giờ 10 phút")
- **AC-6** Existing languages unchanged
  - Given the device language is English, pt-BR, or any language other than Vietnamese and pt-BR
  - When the user uses the app
  - Then copy and title data are exactly as before (pt-BR for pt-BR, English and `en-US` otherwise)
- **AC-7** README
  - Given a reader opens `README.md`
  - Then the overview says the app works in English, Brazilian Portuguese and Vietnamese, and any mention of the
    per-language cache lists `vi-VN`

## Edge cases
- **EC-1** Device language changes to or from Vietnamese while the app is running -> the next display uses the new
  language; cached titles of another language are not shown as if they were Vietnamese (except the favorites
  fallback in AC-4).
- **EC-2** First launch in Vietnamese with no network -> the same empty or error state as the other languages, with
  Vietnamese copy.
- **EC-3** A title is cached in English or pt-BR but not in Vietnamese -> Home and Title details load it in
  Vietnamese like any uncached title; only favorites use the AC-4 fallback.
- **EC-4** Long Vietnamese copy or titles with diacritics (including with large text) -> no text is clipped and
  diacritics render correctly, following the existing large-text layout rules.
- **EC-5** Same title favorited while in different languages -> it appears once (favorites are not per language).

## Constraints
- Language follows the device locale only; matching on the `vi` language code ignores the region.
- All three apps get Vietnamese together.
- Cached content is stored per language (`en-US`, `pt-BR`, `vi-VN`).
- Must work offline the same way as the existing languages.
- English and pt-BR behavior must not change.

## Open questions
- [ ] Vietnamese copy for every string: drafted by Claude from the English copy, reviewed by the user (owner: user,
  before the UI work).

## Change log
- 2026-11-26: Initial version.
- 2026-11-27: Confirmed.
- 2026-11-27: Sliced into 4 subtasks.

## Subtasks
1. [01 - Shared core and cmp in Vietnamese](subtasks/01-shared-core-and-cmp.md)
2. [02 - native-android in Vietnamese](subtasks/02-native-android.md)
3. [03 - native-ios in Vietnamese](subtasks/03-native-ios.md)
4. [04 - README lists Vietnamese](subtasks/04-readme.md)
