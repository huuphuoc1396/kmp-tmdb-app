# 06 - native-ios: Home and Title details

Feature: [task.md](../task.md)
Design: [Home](../../home/design/home.md), [Home favorites](../../favorites/design/home-favorites.md), [Title details](../../title-details/design/title-details.md)
Depends on: 05-native-ios-tokens-and-small-screens
Status: Todo

## Goal
Home (with its favorites section and background) and Title details in `native-ios` use only design-system
tokens and stay readable at large Dynamic Type sizes. After this, `native-ios` holds no raw size, spacing or color
outside its design system.

## Scope
### In scope
- Move Home, including the favorites section, to tokens.
- Decide and apply the Home background: a gradient between two color roles or a flat surface (open question in task.md).
- Move Title details to tokens.
- Large Dynamic Type sizes on these screens.
### Out of scope (handled later)
- Cross-app comparison and docs -> 07-parity-and-docs

## Acceptance criteria
- **AC-3** No raw values in screens and components (partial: `native-ios` Home and Title details; completes AC-3)
  - Given the screen and shared component code of each app
  - When it is searched for numeric dp/sp/pt values, font sizes, corner radii and color literals
  - Then none remain; every value comes from a design-system token (color literals and token values live only in
    the design-system definition)
- **AC-4** Old values rounded to the nearest token (partial: `native-ios` Home and Title details; completes AC-4)
  - Given a value used today that is not on the scale (for example 31dp or 15sp)
  - When the screen is redone
  - Then it uses the nearest token, and the screen keeps its layout, content and behavior, with only small
    differences of a few points from the Figma design
- **AC-5** Full Material Design 3 dark color scheme with the brand colors (partial: `native-ios` Home gradient; completes AC-5)
  - Given the color scheme
  - When it is inspected
  - Then it defines every Material Design 3 color role for dark (including surface containers, outline and error),
    primary is based on `#8000FF` and secondary on `#FF1F8A`, and text stays readable on its background
- **AC-6** Text follows the system font size (partial: `native-ios` Home and Title details; completes AC-6)
  - Given the system font size set to its largest common setting (Android 200%, iOS large Dynamic Type sizes)
  - When the user opens each screen
  - Then text is larger, and no text is cut off mid-line, overlaps other content, or pushes content off screen
    with no way to scroll to it

## Edge cases
- **EC-1** A very long title name at a large font size (partial: `native-ios`; completes EC-1) -> it wraps or ends with "…" as on that screen today; the
  layout does not break.
- **EC-4** A fixed-size component (poster, rating ring) at a large font size (partial: `native-ios`; completes EC-4) -> the component keeps its size; text
  inside or beside it scales and truncates instead of overflowing.
- **EC-5** Touch targets (partial: `native-ios` Home and Title details; completes EC-5) -> every tappable element keeps at least a 48×48 (Android) / 44×44 (iOS) touch area after
  rounding.

## Layers touched
- Feature UI (Home, Title details)

## How to test
1. Run `native-ios`. Open Home: the background, lists and favorites section look as before (or with the agreed flat background) and match `cmp`.
2. Open a title's details and toggle the favorite: it looks as before.
3. Set a large Dynamic Type size: long titles wrap or truncate, posters and the rating ring keep their size, nothing overlaps.
4. Search the `native-ios` screen code for raw sizes, paddings, font sizes and color literals: none remain outside `Theme`.

## Change log
- 2026-11-24: Initial version.
