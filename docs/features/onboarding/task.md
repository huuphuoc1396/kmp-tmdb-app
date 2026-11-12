# Onboarding

Status: Confirmed
Source: chat (`/spark` request, 2026-11-10); product scope in `docs/architecture.md` section 1

## Goal
New users need a first impression that sets the app's tone and tells them what it is for: discovering films,
series, anime and novelas. On first launch the app shows a single cinematic welcome screen, and one tap on
"Acessar" takes the user into Home. Success: first-time users reach Home in one tap, and returning users never
see onboarding again.

## Scope
### In scope
- A single onboarding screen: full-screen artwork bundled with the app, a headline, a short value proposition,
  and a prominent primary button.
- Copy localized for en and pt-BR, including the button ("Acessar" in pt-BR).
- A persisted "onboarding completed" state that decides whether the app starts on onboarding or on Home.
- Both UI branches (`cmp` and `native`) implement the screen. The shared core provides the persisted state.

### Out of scope
- Multi-page pager, page indicators, or a Skip action.
- Sign-in, account creation, or permission prompts.
- Artwork fetched from TMDB or any other remote source.
- Analytics events.
- Entry or transition animations beyond the platform defaults.
- Showing onboarding again after an app update.
- The Home screen itself (separate feature).

## User flow
1. The user launches the app for the first time.
2. The app shows onboarding: full-screen artwork, headline, value proposition, and the "Acessar" button.
3. The user taps "Acessar".
4. The app records that onboarding is completed and replaces onboarding with Home.
5. On every later launch, the app opens directly on Home.

## UI states
| State | When | What the user sees / can do |
|---|---|---|
| Content | Onboarding not yet completed | Artwork, headline, value proposition, primary button; can tap the button or leave the app with Back |
| Navigating | Button tapped | Moves to Home; further taps have no effect |

There is no loading, empty or error state: everything on the screen is bundled with the app.

## Acceptance criteria
- **AC-1** Onboarding shown on first launch
  - Given the app is freshly installed
  - When the user launches it
  - Then the onboarding screen shows the full-screen artwork, headline, value proposition and primary button

- **AC-2** Button enters the app
  - Given the onboarding screen is shown
  - When the user taps the primary button
  - Then Home is shown

- **AC-3** Onboarding skipped after completion
  - Given the user has completed onboarding before
  - When the user launches the app again (including after closing it fully)
  - Then the app opens on Home, with no onboarding screen shown even briefly

- **AC-4** Onboarding is not in the back stack
  - Given the user tapped the primary button and is on Home
  - When the user presses system Back or swipes back
  - Then onboarding is not shown again
  - And given the user is on onboarding, when they press Back, then the app is left (as on any root screen)

- **AC-5** Localized copy
  - Given the device language is pt-BR
  - When onboarding is shown
  - Then the headline and value proposition are in Portuguese and the button reads "Acessar"
  - And given any other device language, then all copy is in English

- **AC-6** Works offline
  - Given the device has no network connection
  - When onboarding is shown and the user taps the primary button
  - Then the screen shows fully (artwork included) and Home opens, with no network request made by onboarding

- **AC-7** Accessible
  - Given a screen reader is on
  - When onboarding is shown
  - Then the artwork is skipped as decorative, the headline and value proposition are read, and the button is
    announced as a button with its label
  - And given the largest system font size, then all text stays readable without clipping
  - And the button meets the platform's minimum touch target size and text contrast guidelines

- **AC-8** Orientation and screen sizes
  - Given a phone or tablet in portrait or landscape
  - When onboarding is shown
  - Then the artwork fills the screen (cropped, not stretched or letterboxed) and the text and button are fully
    visible or reachable

## Edge cases
- **EC-1** Saving "onboarding completed" fails -> the user still enters Home; onboarding may show again on the
  next launch. No error is shown.
- **EC-2** App closed, backgrounded or killed before the button is tapped -> onboarding shows again on the next
  launch.
- **EC-3** Button tapped repeatedly or double-tapped quickly -> Home opens exactly once.
- **EC-4** Rotation or process death while on onboarding -> onboarding is restored as it was; no state is lost.
- **EC-5** Long translated copy or a very large font size -> text wraps (and scrolls if needed); the button stays
  reachable.
- **EC-6** Device language is neither en nor pt-BR -> English copy is shown.

## Constraints
- Platforms: Android (minSdk 24) and iOS 16+, on both the `cmp` and `native` branches.
- Dark theme with purple accents, consistent with the rest of the app (`docs/architecture.md` section 1).
- Must work fully offline; no TMDB token or network needed to show onboarding or enter Home.
- The "onboarding completed" state is stored locally on the device only.
- Localization: en and pt-BR.
- No analytics.

## Designs
- [Onboarding](design/onboarding.md)

## Open questions
- [x] Final headline, value proposition, and English button label, in en and pt-BR — resolved 2026-11-11: pt-BR
  as in the design; en "All about **movies**, **series**, **anime** and **much more.**" / "Stay up to date on
  movies, series, anime and much more." / "Get started" (see design/onboarding.md).
- [x] Artwork asset and its source/license — resolved 2026-11-11: the film art is not committed; a neutral
  gradient placeholder ships until licensed artwork is supplied. The logo is TMDB's official logo, used per TMDB's
  attribution guidelines.

## Change log
- 2026-11-10: Initial version.
- 2026-11-10: Linked designs: onboarding.
- 2026-11-11: Resolved open questions (English copy, placeholder artwork, official TMDB logo); added subtask
  03 (cmp app shell) and renumbered the later subtasks.
- 2026-11-12: Added subtasks 06 (native Android app shell) and 07 (native iOS app shell); renumbered the native
  onboarding subtasks to 08 and 09.

## Subtasks
1. [01 - Encrypted settings storage](subtasks/01-encrypted-settings-storage.md)
2. [02 - Onboarding completed state](subtasks/02-onboarding-completed-state.md)
3. [03 - App shell (cmp)](subtasks/03-cmp-app-shell.md)
4. [04 - Onboarding happy path (cmp)](subtasks/04-cmp-onboarding-happy-path.md)
5. [05 - Onboarding robustness (cmp)](subtasks/05-cmp-onboarding-robustness.md)
6. [06 - App shell (native Android)](subtasks/06-native-android-app-shell.md)
7. [07 - App shell (native iOS)](subtasks/07-native-ios-app-shell.md)
8. [08 - Onboarding on Android (native)](subtasks/08-native-android-onboarding.md)
9. [09 - Onboarding on iOS (native)](subtasks/09-native-ios-onboarding.md)
