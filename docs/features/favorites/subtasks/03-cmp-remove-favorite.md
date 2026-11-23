# 03 - Remove with confirmation (cmp)

Feature: [task.md](../task.md)
Design: [Remove favorite dialog](../design/remove-favorite-dialog.md), [Home – Favorites section](../design/home-favorites.md), [My favorites](../design/my-favorites.md)
Depends on: 02-cmp-browse-favorites
Status: Done

## Goal
In the cmp app, users remove a favorite from Home, My favorites or Title details, always after confirming in the
popup. Cancelling leaves everything as it was, and My favorites shows its empty state after the last removal.

## Scope
### In scope
- The heart button on Home section items and My favorites cards.
- The confirmation popup on Home, My favorites and Title details (filled heart only).
- Removing, cancelling (button, outside tap, system back).
- The My favorites empty state.
- Screen reader labels for items, hearts and the popup; popup copy in en and pt-BR.
### Out of scope (handled later)
- native apps -> 04-native-android-favorites, 05-native-ios-favorites

## Acceptance criteria
- **AC-4** Favorite item (partial: the heart; the rest in 02)
  - Given a favorite is shown on Home or My favorites
  - When it is drawn
  - Then its poster shows the heart as in the design
- **AC-6** Confirm before removing on Home and My favorites
  - Given a favorite is shown on Home or My favorites
  - When the user taps its heart
  - Then the confirmation popup opens with the title "Remover dos favoritos?", a message naming the title, and the
    buttons "Cancelar" and "Remover", as in the design
  - And the title stays a favorite while the popup is open
- **AC-7** Confirm before removing on Title details
  - Given Title details shows a title that is a favorite
  - When the user taps the filled heart
  - Then the same confirmation popup opens instead of removing the favorite at once
  - And given the title is not a favorite, when the user taps the empty heart, then it becomes a favorite at once
    with no popup (existing behavior)
- **AC-8** Removing
  - Given the confirmation popup is open
  - When the user taps "Remover"
  - Then the popup closes and the title is no longer a favorite
  - And it disappears from the Home section and My favorites, the badges show the new total, and its heart on
    Title details shows empty
  - And there is no undo; the user can favorite it again on Title details
- **AC-9** Cancelling
  - Given the confirmation popup is open
  - When the user taps "Cancelar", taps outside the popup or uses the system back
  - Then the popup closes and nothing changes
- **AC-10** My favorites empty
  - Given My favorites is shown
  - When the user removes the last favorite
  - Then the screen stays open with the badge "0 títulos" and a short empty message
  - And when the user goes back, Home shows no "Favoritos" section
- **AC-11** Offline (partial: removing; the rest in 01 and 02)
  - And removing a favorite works with no network
- **AC-12** Language (partial: popup and empty-message copy)
  - Given the device language is pt-BR
  - When the popup or the empty state is shown
  - Then the copy is Portuguese; given any other device language, the copy is English
- **AC-13** Accessibility
  - Given a favorite is shown on Home or My favorites
  - When a screen reader focuses it
  - Then the item is announced as a button with the title's name, and its heart as a "Remove from favorites" /
    "Remover dos favoritos" button
  - And "Ver todos", the hearts and the popup buttons meet the minimum touch target, and the popup takes the screen
    reader's focus while it is open

## Edge cases
- **EC-1** Heart or "Remover" tapped repeatedly or quickly -> only one popup opens and the title is removed once.
- **EC-5** Very long title name (partial: popup; truncation in 02) -> in the popup message it wraps and is shown
  in full.
- **EC-6** Rotation while the popup is open -> the popup stays open and nothing changes; process death -> the popup
  is closed and the title is still a favorite.
- **EC-11** Largest system font size -> section title, badge, list items and popup stay readable; nothing is
  clipped beyond the truncation in EC-5.

## Layers touched
- UI (shared popup, Home section, My favorites, Title details), ViewModels, copy resources

## How to test
1. On Home, tap a favorite's heart: the popup names the title; tap "Cancelar", tap outside, press back: each time
   nothing changes.
2. Tap "Remover": the item leaves the section and the badge drops by one.
3. On My favorites, remove every favorite: the badge reads "0 títulos" and the empty message shows; back to Home,
   no section.
4. On Title details of a favorite, tap the heart: the popup opens; "Remover" empties the heart. Tap it again: it
   becomes a favorite with no popup.
5. Double-tap "Remover" quickly: removed once, no crash.
6. Rotate with the popup open: the popup is gone and the title is still a favorite.
7. With airplane mode on, remove a favorite: it works.
8. With TalkBack / VoiceOver and the largest font size, go through the section, list and popup.

## Change log
- 2026-11-23: Initial version.
- 2026-11-23: Plan approved.
- 2026-11-23: EC-6 changed: the popup stays open on rotation (user, plan 03).
- 2026-11-23: Implemented (plan 03 done).
