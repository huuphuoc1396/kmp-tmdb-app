# 04 - native-android: Home and Title details

Feature: [task.md](../task.md)
Design: [Home](../../home/design/home.md), [Home favorites](../../favorites/design/home-favorites.md), [Title details](../../title-details/design/title-details.md)
Depends on: 03-native-android-tokens-and-small-screens
Status: Todo

## Goal
Home (with its favorites section) and Title details in `native-android` use only design-system tokens and stay
readable at large font sizes. After this, `native-android` holds no raw size, spacing or color outside its design
system.

## Scope
### In scope
- Move Home, including the favorites section, to tokens.
- Move Title details to tokens.
- Large font sizes on these screens.
### Out of scope (handled later)
- Cross-app comparison and docs -> 07-parity-and-docs

## Acceptance criteria
- **AC-3** No raw values in screens and components (partial: `native-android` Home and Title details; completes `native-android`)
  - Given the screen and shared component code of each app
  - When it is searched for numeric dp/sp/pt values, font sizes, corner radii and color literals
  - Then spacing, text styles, corner shapes and colors all come from design-system tokens (color literals and token
    values live only in the design-system definition); any other size used in one place is written inline where it
    is used, and a size used in two or more places is a named constant in its file, or in its `<Component>Defaults`
    object when another module needs it
- **AC-4** Old values rounded to the nearest token (partial: `native-android` Home and Title details; completes `native-android`)
  - Given a value used today that is not on the scale (for example 31dp or 15sp)
  - When the screen is redone
  - Then it uses the nearest token, and the screen keeps its layout, content and behavior, with only small
    differences of a few points from the Figma design
- **AC-6** Text follows the system font size (partial: `native-android` Home and Title details; completes `native-android`)
  - Given the system font size set to its largest common setting (Android 200%, iOS large Dynamic Type sizes)
  - When the user opens each screen
  - Then text is larger, and no text is cut off mid-line, overlaps other content, or pushes content off screen
    with no way to scroll to it

## Edge cases
- **EC-1** A very long title name at a large font size (partial: `native-android`) -> it wraps or ends with "…" as on that screen today; the
  layout does not break.
- **EC-4** A fixed-size component (poster, rating ring) at a large font size (partial: `native-android`) -> the component keeps its size; text
  inside or beside it scales and truncates instead of overflowing.
- **EC-5** Touch targets (partial: `native-android` Home and Title details) -> every tappable element keeps at least a 48×48 (Android) / 44×44 (iOS) touch area after
  rounding.

## Layers touched
- Feature UI (Home, Title details)

## How to test
1. Run `native-android`. Open Home and a title's details: they look as before and match `cmp`.
2. Toggle a favorite from details and see it in the Home favorites section.
3. Set the system font size to 200%: long titles wrap or truncate, posters and the rating ring keep their size, nothing overlaps.
4. Search the `native-android` screen code for raw dp/sp values and color literals: none remain outside the design system.

## Change log
- 2026-11-24: Initial version.
- 2026-11-24: AC-3 updated: component- and feature-specific sizes are named constants next to their component.
- 2026-11-24: AC-3 updated: single-use sizes are written inline.
