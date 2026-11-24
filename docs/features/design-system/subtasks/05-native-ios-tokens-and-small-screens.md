# 05 - native-ios: tokens and smaller screens

Feature: [task.md](../task.md)
Design: [Onboarding](../../onboarding/design/onboarding.md), [My favorites](../../favorites/design/my-favorites.md), [Remove favorite dialog](../../favorites/design/remove-favorite-dialog.md), [Home](../../home/design/home.md) (maybe)
Depends on: 01-cmp-tokens-and-small-screens
Status: Todo

## Goal
`native-ios` has a design system with the same token names and values as `cmp`, with text styles that follow
Dynamic Type. The per-screen colors are replaced by color roles. Its shared components, Onboarding and My
favorites use only those tokens and keep iOS controls (alerts, navigation bar).

## Scope
### In scope
- Define the same token set as `cmp` in `native-ios`, with text styles that scale with Dynamic Type.
- Map the iOS-only colors (track, page, date line, synopsis, ring center) to color roles; the Home gradient is handled in 06.
- Move its shared components, Onboarding and My favorites to tokens, keeping iOS controls.
### Out of scope (handled later)
- Home and Title details in `native-ios`, Home gradient -> 06-native-ios-home-and-title-details

## Acceptance criteria
- **AC-1** One set of tokens in all three apps (partial: `native-ios` matches `cmp`; completes AC-1)
  - Given the design system of `cmp`, `native-android` and `native-ios`
  - When their spacing, type, shape, color and component-size tokens are compared
  - Then each app defines the same tokens with the same names and values (allowing for each platform's naming style)
- **AC-3** No raw values in screens and components (partial: `native-ios` shared components, Onboarding, My favorites)
  - Given the screen and shared component code of each app
  - When it is searched for numeric dp/sp/pt values, font sizes, corner radii and color literals
  - Then spacing, text styles, corner shapes and colors all come from design-system tokens (color literals and token
    values live only in the design-system definition), and no numeric size is written inline in a function: every
    other size is a named constant at the top of its component's file or in its `<Component>Defaults` object
- **AC-4** Old values rounded to the nearest token (partial: same screens)
  - Given a value used today that is not on the scale (for example 31dp or 15sp)
  - When the screen is redone
  - Then it uses the nearest token, and the screen keeps its layout, content and behavior, with only small
    differences of a few points from the Figma design
- **AC-5** Full Material Design 3 dark color scheme with the brand colors (partial: `native-ios` except the Home gradient)
  - Given the color scheme
  - When it is inspected
  - Then it defines every Material Design 3 color role for dark (including surface containers, outline and error),
    primary is based on `#8000FF` and secondary on `#FF1F8A`, and text stays readable on its background
- **AC-6** Text follows the system font size (partial: `native-ios` Onboarding and My favorites)
  - Given the system font size set to its largest common setting (Android 200%, iOS large Dynamic Type sizes)
  - When the user opens each screen
  - Then text is larger, and no text is cut off mid-line, overlaps other content, or pushes content off screen
    with no way to scroll to it

## Edge cases
- **EC-5** Touch targets (partial: `native-ios` shared components, Onboarding, My favorites) -> every tappable element keeps at least a 48×48 (Android) / 44×44 (iOS) touch area after
  rounding.

## Layers touched
- Design system (tokens, theme, shared components)
- Feature UI (Onboarding, My favorites)

## How to test
1. Run `native-ios` in the simulator. Go through Onboarding: it looks as before and matches `cmp`.
2. Open My favorites with and without favorites, and remove one: the list, empty state and alert look as before.
3. Set Dynamic Type to a large accessibility size (Settings > Accessibility > Display & Text Size > Larger Text) and repeat: text is larger, nothing overlaps or is cut mid-line, buttons stay tappable.

## Change log
- 2026-11-24: Initial version.
- 2026-11-24: AC-3 updated: component- and feature-specific sizes are named constants next to their component.
