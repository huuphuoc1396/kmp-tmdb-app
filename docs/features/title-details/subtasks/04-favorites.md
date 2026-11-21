# 04 - Favorites (shared core + cmp)

Feature: [task.md](../task.md)
Design: [Title details](../design/title-details.md)
Depends on: 02-cmp-title-details-happy-path
Status: Done

## Goal
Users can mark a title as a favorite from Title details in `cmp` with the heart button, and unmark it. Favorites are stored on the device only, survive app restarts, work offline and do not depend on the language. This creates the `favorites` context in the shared core.

## Scope
### In scope
- The `favorites` context in the shared core: store, remove and observe whether a title is a favorite; local-only, not per language, kept across restarts.
- A favorites test double with its contract test.
- Making favorites available to the apps (`cmp`, `native-android` through the published core, `native-ios` through the Swift package).
- The heart button on Title details in `cmp` (off: outline, on: filled `#FF1F8A`), shown only with details.
- Screen reader toggle label "Favorite" / "Favorito".
- App: `cmp/`.
### Out of scope (handled later)
- A favorites list screen and the favorite flag on Home posters -> separate features
- Native apps -> 05-native-android-title-details, 06-native-ios-title-details

## Acceptance criteria
- **AC-3** Favorite
  - Given the details are shown
  - When the user taps the heart
  - Then the heart shows as filled and the title is stored as a favorite on the device
  - And when the user taps it again, the heart shows as empty and the title is no longer a favorite
  - And the state is kept after closing and reopening the app, works with no network, and is the same for the
    title in both languages
  - And a screen reader announces the heart as a toggle "Favorite" / "Favorito" with its on/off state
- **AC-4** Favorite only with details
  - Given the screen is in the Loading or Error state
  - When it is shown
  - Then no heart is shown; it appears once the details are shown

## Edge cases
- **EC-1** Heart tapped repeatedly or quickly -> each tap toggles once; the final state matches the last tap and
  is the one stored.
- **EC-9** Device language changes while the app is running -> the next display uses the new language; cached (partial: the favorite state is unchanged by a language change; the rest is in 03)
  details of the other language are not shown as if they were in the new one. The favorite state is unchanged.

## Layers touched
- Domain (favorites repository interface)
- Data (local storage)
- Test double
- Dependency injection
- iOS entry point
- UI and state holder (heart button)

## How to test
1. Run the `cmp` app; open a title: the outline heart shows over the artwork. Tap it: it fills pink. Tap again: it goes back to outline.
2. Mark a title, kill and reopen the app, open it again: the heart is filled.
3. Turn on airplane mode and toggle the heart on a cached title: it works.
4. Mark a title, switch the device language, open it again: the heart is still filled.
5. Tap the heart many times quickly: the final state matches the last tap, and it is the one shown after reopening.
6. Open a title never cached with airplane mode on: no heart in the Loading or Error state.
7. With TalkBack / VoiceOver, focus the heart: it is announced as the "Favorite" / "Favorito" toggle with its state.

## Change log
- 2026-11-20: Initial version.
