# 04 - README lists Vietnamese

Feature: [task.md](../task.md)
Depends on: 02-native-android, 03-native-ios
Status: Done

## Goal
Once all three apps speak Vietnamese, the README tells readers the app works in English, Brazilian Portuguese and
Vietnamese.

## Scope
### In scope
- `README.md`: the overview and any mention of the per-language cache.
- `native-android/README.md` and `native-ios/README.md`, where they name the supported languages.
### Out of scope (handled later)
- A Vietnamese translation of the README (out of scope for the feature).

## Acceptance criteria
- **AC-7** README
  - Given a reader opens `README.md`
  - Then the overview says the app works in English, Brazilian Portuguese and Vietnamese, and any mention of the
    per-language cache lists `vi-VN`

## Edge cases
None.

## Layers touched
- Docs

## How to test
1. Read `README.md`: the overview names English, Brazilian Portuguese and Vietnamese; the per-language cache line
   lists `en-US`, `pt-BR` and `vi-VN`.
2. Read `native-android/README.md` and `native-ios/README.md`: any list of languages includes Vietnamese.

## Change log
- 2026-11-27: Initial version.
