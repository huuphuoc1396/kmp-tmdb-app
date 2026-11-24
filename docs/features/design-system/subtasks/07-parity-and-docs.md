# 07 - Cross-app comparison and docs

Feature: [task.md](../task.md)
Design: [Onboarding](../../onboarding/design/onboarding.md), [My favorites](../../favorites/design/my-favorites.md), [Remove favorite dialog](../../favorites/design/remove-favorite-dialog.md), [Home](../../home/design/home.md), [Home favorites](../../favorites/design/home-favorites.md), [Title details](../../title-details/design/title-details.md)
Depends on: 02-cmp-home-and-title-details, 04-native-android-home-and-title-details, 06-native-ios-home-and-title-details
Status: Done

## Goal
Every screen looks the same in the three apps, apart from platform controls, and the project docs state the
token rule so new screens follow it.

## Scope
### In scope
- Compare each screen side by side in `cmp`, `native-android` and `native-ios` and fix any spacing, text, shape or color mismatch.
- Write the "use tokens, not raw values" convention in the project docs, with where the tokens live in each app.
- Update the existing `docs/features/*/design/*.md` to name the tokens each screen uses.
### Out of scope (handled later)
- None

## Acceptance criteria
- **AC-7** The three apps match
  - Given the same screen open in the three apps with the same data
  - When they are compared side by side
  - Then spacing, text styles, corner shapes and colors match; only platform controls (navigation bar, alerts,
    sheets on iOS) differ
- **AC-8** Convention and design docs
  - Given the project docs and the `docs/features/*/design/*.md` files
  - When they are read
  - Then they state that screens use tokens, not raw values, and the design docs name the tokens each screen uses

## Edge cases
- None

## Layers touched
- Feature UI (small fixes only)
- Docs

## How to test
1. Open Onboarding, Home, My favorites and Title details with the same data in all three apps and compare screenshots: spacing, text styles, corners and colors match; only iOS controls differ.
2. Read the project docs: the token rule is stated.
3. Read each `design/*.md`: values are named by token.

## Change log
- 2026-11-24: Initial version.
