# 05 - Onboarding robustness (cmp)

Feature: [task.md](../task.md)
Design: [Onboarding](../design/onboarding.md)
Depends on: 04-cmp-onboarding-happy-path
Status: Todo

## Goal
On the Compose Multiplatform app, onboarding is usable with a screen reader and large fonts, adapts to landscape and tablets, and survives rotation and process death.

## Scope
### In scope
- Screen-reader semantics: artwork, scrim and logo decorative; headline read as one sentence; button announced with its label.
- Large fonts and long copy: the content block scrolls when it doesn't fit; the button stays reachable.
- Landscape and tablet: artwork keeps a cover crop; content block bottom-anchored, centered, max width 315dp (design).
- State restored after rotation and process death.
- App: `apps/cmp` (on `main`).
### Out of scope (handled later)
- None within this feature.

## Acceptance criteria
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
- **EC-4** Rotation or process death while on onboarding -> onboarding is restored as it was; no state is lost.
- **EC-5** Long translated copy or a very large font size -> text wraps (and scrolls if needed); the button stays
  reachable.

## Layers touched
- UI
- State holder (ViewModel)

## How to test
1. Screen reader on: artwork and logo are skipped, headline and body are read, the button is announced as a
   button with its label (AC-7).
2. Largest system font size: text wraps, the content scrolls if needed, the button stays reachable (AC-7, EC-5).
3. Phone and tablet, portrait and landscape: artwork fills the screen with a cover crop; text and button are
   visible or reachable (AC-8).
4. Rotate on onboarding, and trigger process death (Android: "Don't keep activities"; iOS: kill in background
   after state save): onboarding is restored as it was (EC-4).

Run on Android (TalkBack) and on the iOS simulator (VoiceOver via Accessibility Inspector).

## Change log
- 2026-11-10: Initial version.
- 2026-11-11: Renumbered from 04; apps are directories on `main`.
