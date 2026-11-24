# 01 - cmp: tokens and smaller screens

Feature: [task.md](../task.md)
Design: [Onboarding](../../onboarding/design/onboarding.md), [My favorites](../../favorites/design/my-favorites.md), [Remove favorite dialog](../../favorites/design/remove-favorite-dialog.md), [Home](../../home/design/home.md) (maybe)
Depends on: none
Status: Done

## Goal
The `cmp` app has a full Material Design 3 design system: spacing, type, shape, dark color scheme and shared
size tokens. Its shared components, Onboarding and My favorites use only those tokens and scale with the
system font size, while looking and behaving as before.

## Scope
### In scope
- Define the token set for `cmp`: spacing scale on the 4dp grid, Material Design 3 type scale in the system font, shape scale, full dark color scheme with the brand colors, shared size tokens (touch target, icon sizes, border width). These names and values are the reference the other two apps copy.
- Settle the rounding rule for values not on the scale and apply it.
- Move the shared components (remove-favorite dialog, poster tile and row, meta line, retry message, person icon) to tokens.
- Move Onboarding and My favorites to tokens.
- Text on these screens and components scales with the Android / iOS system font size.
### Out of scope (handled later)
- Home and Title details in `cmp` -> 02-cmp-home-and-title-details
- `native-android` tokens -> 03-native-android-tokens-and-small-screens
- `native-ios` tokens -> 05-native-ios-tokens-and-small-screens
- Cross-app comparison and docs -> 07-parity-and-docs

## Acceptance criteria
- **AC-1** One set of tokens in all three apps (partial: defines the reference token set in `cmp`; the other apps follow in 03 and 05)
  - Given the design system of `cmp`, `native-android` and `native-ios`
  - When their spacing, type, shape, color and component-size tokens are compared
  - Then each app defines the same tokens with the same names and values (allowing for each platform's naming style)
- **AC-2** Spacing on the 4dp grid
  - Given the spacing scale
  - When its values are listed
  - Then every value is a multiple of 4 (with 0 and 2 allowed for hairline gaps only if kept)
- **AC-3** No raw values in screens and components (partial: `cmp` shared components, Onboarding, My favorites)
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
- **AC-5** Full Material Design 3 dark color scheme with the brand colors (partial: `cmp`)
  - Given the color scheme
  - When it is inspected
  - Then it defines every Material Design 3 color role for dark (including surface containers, outline and error),
    primary is based on `#8000FF` and secondary on `#FF1F8A`, and text stays readable on its background
- **AC-6** Text follows the system font size (partial: `cmp` Onboarding and My favorites)
  - Given the system font size set to its largest common setting (Android 200%, iOS large Dynamic Type sizes)
  - When the user opens each screen
  - Then text is larger, and no text is cut off mid-line, overlaps other content, or pushes content off screen
    with no way to scroll to it

## Edge cases
- **EC-2** Two different old values round to the same token -> accepted; both use that token.
- **EC-3** A value exactly between two tokens -> it rounds to the larger token unless that breaks the layout.
- **EC-5** Touch targets (partial: `cmp` shared components, Onboarding, My favorites) -> every tappable element keeps at least a 48×48 (Android) / 44×44 (iOS) touch area after
  rounding.

## Layers touched
- Design system (tokens, theme, shared components)
- Feature UI (Onboarding, My favorites)

## How to test
1. Run the `cmp` app on Android. Go through Onboarding: layout, text and colors match the current app, with differences of a few points at most.
2. Open My favorites with some favorites and with none, and remove one: the confirm dialog, the list and the empty state look as before.
3. Set the system font size to 200% and repeat: text is larger, nothing overlaps or is cut mid-line, every button stays tappable.
4. Repeat steps 1–2 on the iOS build of `cmp`.

## Change log
- 2026-11-24: Initial version.
- 2026-11-24: AC-3 updated: component- and feature-specific sizes are named constants next to their component.
- 2026-11-24: AC-3 updated: single-use sizes are written inline.
