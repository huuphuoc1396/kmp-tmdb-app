# 01 - Shared core and cmp in Vietnamese

Feature: [task.md](../task.md)
Depends on: none
Status: Done

## Goal
With the device set to Vietnamese, the `cmp` app runs fully in Vietnamese: every screen's copy, title data from TMDB
in `vi-VN` with its own cache, the trailer choice, the favorites fallback and Vietnamese dates and runtimes. The
shared core gains Vietnamese, so the native apps get the same data behavior once they pick up the republished core.

## Scope
### In scope
- Vietnamese as a supported language in the shared core: any `vi` locale maps to it; TMDB requests use `vi-VN`;
  cached content is stored per language including `vi-VN`.
- Trailer selection for Vietnamese (Vietnamese first, then English).
- Favorites fallback order when Vietnamese data is missing (en-US, then pt-BR).
- Vietnamese copy for every `cmp` screen and shared component: Onboarding, Home (with favorites section), Title
  details, My favorites, the remove-favorite dialog, accessibility labels.
- Vietnamese date and runtime formatting in `cmp`.
- Previews of the touched `cmp` screens and components include Vietnamese where the existing previews show pt-BR.
### Out of scope (handled later)
- Vietnamese in `native-android` -> 02-native-android
- Vietnamese in `native-ios` -> 03-native-ios
- README -> 04-readme

## Acceptance criteria
- **AC-1** Vietnamese copy (partial: `cmp` only)
  - Given the device language is any locale whose language is `vi` (`vi-VN`, `vi`, `vi-US`, …)
  - When the user opens any screen or the remove-favorite dialog
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
- **AC-5** Dates and runtimes (partial: `cmp` only)
  - Given the app is in Vietnamese
  - When a release date or runtime is shown
  - Then it uses Vietnamese formatting (for example "26 thg 11, 2026" and "2 giờ 10 phút")
- **AC-6** Existing languages unchanged (partial: shared core and `cmp`)
  - Given the device language is English, pt-BR, or any language other than Vietnamese and pt-BR
  - When the user uses the app
  - Then copy and title data are exactly as before (pt-BR for pt-BR, English and `en-US` otherwise)

## Edge cases
- **EC-1** Device language changes to or from Vietnamese while the app is running -> the next display uses the new
  language; cached titles of another language are not shown as if they were Vietnamese (except the favorites
  fallback in AC-4).
- **EC-2** First launch in Vietnamese with no network -> the same empty or error state as the other languages, with
  Vietnamese copy.
- **EC-3** A title is cached in English or pt-BR but not in Vietnamese -> Home and Title details load it in
  Vietnamese like any uncached title; only favorites use the AC-4 fallback.
- **EC-4** (partial: `cmp` only) Long Vietnamese copy or titles with diacritics (including with large text) -> no
  text is clipped and diacritics render correctly, following the existing large-text layout rules.
- **EC-5** Same title favorited while in different languages -> it appears once (favorites are not per language).

## Layers touched
- Shared language model
- Title data: network requests, per-language cache, trailer selection
- Favorites-with-title-data use case (fallback order)
- `cmp` UI: string resources, date and runtime formatting, previews

## How to test
1. Before starting: the Vietnamese copy (open question in task.md) is reviewed by the user.
2. Set the Android emulator (or iOS simulator) language to Tiếng Việt and run the `cmp` app on a fresh install.
3. Onboarding, Home, Title details, My favorites and the remove-favorite dialog show Vietnamese copy only.
4. Home rows and Title details show TMDB data in Vietnamese (Vietnamese genre names; synopsis in Vietnamese where TMDB
   has one).
5. Open a title with a Vietnamese trailer: it opens; open one with only an English trailer: the English one opens.
6. Release date and runtime use Vietnamese formatting.
7. Favorite a title in English, switch the device to Vietnamese with no network: My favorites shows the English data;
   reconnect and refresh: it switches to Vietnamese.
8. Switch to English and pt-BR: everything is as before. Switch to French: English.
9. Turn on the largest font size in Vietnamese: no clipped text.

## Change log
- 2026-11-27: Initial version.
