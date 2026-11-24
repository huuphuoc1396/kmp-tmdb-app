# 06 - Fixed top bar on My favorites

Feature: [task.md](../task.md)
Design: [My favorites](../design/my-favorites.md)
Depends on: 03-cmp-remove-favorite, 04-native-android-favorites, 05-native-ios-favorites
Status: Done

## Goal
On My favorites, in all three apps, the row with "Voltar" and the "N títulos" badge stays fixed at the top while
the title, subtitle and cards scroll beneath it, so the user can always go back and see the count without
scrolling up.

## Scope
### In scope
- The My favorites screen in `cmp`, `native-android` and `native-ios`.
- The fixed row in the Content and Empty states.
- Keeping the existing scroll position restore (EC-7) and the Empty state layout working with the new layout.
### Out of scope (handled later)
- Home's "Favoritos" section and Title details: unchanged.
- Any divider, shadow or collapsing animation on the fixed row: not wanted (AC-14).

## Acceptance criteria
- **AC-14** My favorites fixed top bar
  - Given My favorites is shown with more favorites than fit on screen
  - When the user scrolls the list
  - Then the row with "Voltar" and the "N títulos" badge stays fixed at the top, and the title, subtitle and cards
    scroll beneath it
  - And cards pass under the row and are cut off by it cleanly; the row has the screen background, with no divider
    or shadow
  - And the same applies in `cmp`, `native-android` and `native-ios`
- **AC-10** My favorites empty (re-checked, not changed): with no favorites, the fixed row, the title, the subtitle
  and the empty message centered in the space below the subtitle are shown.

## Edge cases
- **EC-12** Largest system font size or a very long badge ("1234 títulos") -> the fixed row on My favorites stays a
  single row and still shows "Voltar" and the full badge.
- **EC-7** Rotation or process death on My favorites -> the screen and its scroll position are restored
  (re-checked, not changed).

## Layers touched
- UI of the My favorites screen in each app (no ViewModel, data or shared core changes expected)

## How to test
Run each app (`cmp` Android and iOS, `native-android`, `native-ios`) with at least 8 favorites:
1. Open Home, tap "Ver todos". My favorites shows "Voltar", the badge, the title, the subtitle and the list.
2. Scroll the list down. "Voltar" and the badge stay at the top; the title and subtitle scroll away; cards pass
   under the row with no divider or shadow.
3. While scrolled, tap "Voltar". Home is shown as it was.
4. Open My favorites again, scroll, rotate (Android) or send the app to the background and return. The scroll
   position is kept and the row is still fixed.
5. Set the largest system font size. The fixed row stays one line and shows "Voltar" and the full badge.
6. Remove every favorite from My favorites. The fixed row shows "0 títulos"; the title, subtitle and the empty
   message centered below them are shown.

## Change log
- 2026-11-24: Initial version (one subtask for all three apps, as asked by the user).
- 2026-11-24: Plan approved.
- 2026-11-24: Implemented (plan 06 done).
