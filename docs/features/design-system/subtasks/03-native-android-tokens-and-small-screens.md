# 03 - native-android: tokens and smaller screens

Feature: [task.md](../task.md)
Design: [Onboarding](../../onboarding/design/onboarding.md), [My favorites](../../favorites/design/my-favorites.md), [Remove favorite dialog](../../favorites/design/remove-favorite-dialog.md), [Home](../../home/design/home.md) (maybe)
Depends on: 01-cmp-tokens-and-small-screens
Status: Done

## Goal
`native-android` has its own copy of the design system, with the same token names and values as `cmp`. Its
shared components, Onboarding and My favorites use only those tokens and scale with the system font size.

## Scope
### In scope
- Define the same token set as `cmp` in `native-android`, without depending on `cmp`.
- Move its shared components, Onboarding and My favorites to tokens.
- Text on these screens scales with the system font size.
### Out of scope (handled later)
- Home and Title details in `native-android` -> 04-native-android-home-and-title-details
- `native-ios` -> 05-native-ios-tokens-and-small-screens

## Acceptance criteria
- **AC-1** One set of tokens in all three apps (partial: `native-android` matches `cmp`)
  - Given the design system of `cmp`, `native-android` and `native-ios`
  - When their spacing, type, shape, color and component-size tokens are compared
  - Then each app defines the same tokens with the same names and values (allowing for each platform's naming style)
- **AC-3** No raw values in screens and components (partial: `native-android` shared components, Onboarding, My favorites)
  - Given the screen and shared component code of each app
  - When it is searched for numeric dp/sp/pt values, font sizes, corner radii and color literals
  - Then spacing, text styles, corner shapes and colors all come from design-system tokens (color literals and token
    values live only in the design-system definition); any other size used in one place is written inline where it
    is used, and a size used in two or more places is a named constant in its file, or in its `<Component>Defaults`
    object when another module needs it
- **AC-4** Old values rounded to the nearest token (partial: same screens)
  - Given a value used today that is not on the scale (for example 31dp or 15sp)
  - When the screen is redone
  - Then it uses the nearest token, and the screen keeps its layout, content and behavior, with only small
    differences of a few points from the Figma design
- **AC-5** Full Material Design 3 dark color scheme with the brand colors (partial: `native-android`)
  - Given the color scheme
  - When it is inspected
  - Then it defines every Material Design 3 color role for dark (including surface containers, outline and error),
    primary is based on `#8000FF` and secondary on `#FF1F8A`, and text stays readable on its background
- **AC-6** Text follows the system font size (partial: `native-android` Onboarding and My favorites)
  - Given the system font size set to its largest common setting (Android 200%, iOS large Dynamic Type sizes)
  - When the user opens each screen
  - Then text is larger, and no text is cut off mid-line, overlaps other content, or pushes content off screen
    with no way to scroll to it

## Edge cases
- **EC-5** Touch targets (partial: `native-android` shared components, Onboarding, My favorites) -> every tappable element keeps at least a 48×48 (Android) / 44×44 (iOS) touch area after
  rounding.

## Layers touched
- Design system (tokens, theme, shared components)
- Feature UI (Onboarding, My favorites)

## How to test
1. Run `native-android`. Go through Onboarding: it looks as before and matches `cmp`.
2. Open My favorites with and without favorites, and remove one: the list, empty state and confirm dialog look as before.
3. Set the system font size to 200% and repeat: nothing overlaps or is cut mid-line, every button stays tappable.

## Change log
- 2026-11-24: Initial version.
- 2026-11-24: AC-3 updated: component- and feature-specific sizes are named constants next to their component.
- 2026-11-24: AC-3 updated: single-use sizes are written inline.
