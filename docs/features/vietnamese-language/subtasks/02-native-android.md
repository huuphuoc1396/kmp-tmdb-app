# 02 - native-android in Vietnamese

Feature: [task.md](../task.md)
Depends on: 01-shared-core-and-cmp
Status: Todo

## Goal
With the device set to Vietnamese, the `native-android` app shows every screen in Vietnamese, with Vietnamese dates
and runtimes, and gets Vietnamese title data, trailers and the favorites fallback from the shared core published
after 01.

## Scope
### In scope
- Vietnamese copy for every `native-android` screen and component: Onboarding, Home (with favorites section), Title
  details, My favorites, the remove-favorite dialog, accessibility labels.
- Vietnamese date and runtime formatting in `native-android`.
- Previews of the touched screens and components include Vietnamese where the existing previews show pt-BR.
- Checking that the data behavior from 01 (AC-2, AC-3, AC-4) works in this app.
### Out of scope (handled later)
- Vietnamese in `native-ios` -> 03-native-ios
- README -> 04-readme

## Acceptance criteria
- **AC-1** Vietnamese copy (partial: `native-android` only)
  - Given the device language is any locale whose language is `vi` (`vi-VN`, `vi`, `vi-US`, …)
  - When the user opens any screen or the remove-favorite dialog
  - Then every piece of app copy (titles, buttons, tabs, empty and error messages, accessibility labels) is in
    Vietnamese
- **AC-5** Dates and runtimes (partial: `native-android` only)
  - Given the app is in Vietnamese
  - When a release date or runtime is shown
  - Then it uses Vietnamese formatting (for example "26 thg 11, 2026" and "2 giờ 10 phút")
- **AC-6** Existing languages unchanged (partial: `native-android` only)
  - Given the device language is English, pt-BR, or any language other than Vietnamese and pt-BR
  - When the user uses the app
  - Then copy and title data are exactly as before (pt-BR for pt-BR, English and `en-US` otherwise)

## Edge cases
- **EC-4** (partial: `native-android` only) Long Vietnamese copy or titles with diacritics (including with large
  text) -> no text is clipped and diacritics render correctly, following the existing large-text layout rules.

## Layers touched
- `native-android` UI: string resources, date and runtime formatting, previews

## How to test
1. Publish the shared core, set the emulator language to Tiếng Việt and install `native-android` fresh.
2. Onboarding, Home, Title details, My favorites and the remove-favorite dialog show Vietnamese copy only.
3. Title data is in Vietnamese; the trailer and favorites fallback behave as in 01 steps 5 and 7.
4. Release date and runtime use Vietnamese formatting.
5. Switch to English and pt-BR: everything is as before. Switch to French: English.
6. Turn on the largest font size in Vietnamese: no clipped text.

## Change log
- 2026-11-27: Initial version.
