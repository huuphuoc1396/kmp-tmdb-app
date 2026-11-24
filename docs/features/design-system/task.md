# Design system

Status: Confirmed
Source: chat (`/spark` request, 2026-11-24): "thiết kế lại design system cho cả 3 app, chuẩn hóa và hạn chế các
margin padding specific, có thể chỉnh sửa lại cho phù hợp với tiêu chuẩn thiết kế của Material Design 3"

## Goal
The three apps (`cmp`, `native-android`, `native-ios`) each draw the same screens with their own one-off values:
paddings such as 3, 6, 10, 14, 21, 29 and 31, about 127 hard-coded font sizes, and per-screen constants and colors
on iOS. Developers have no shared vocabulary, so the apps drift apart and every new screen invents new numbers.
This redesign gives all three apps one design system that follows Material Design 3 (spacing, type, shape and
color), so screens use named tokens only. Success: no screen or component code holds a raw size, spacing or
color; the same screen looks the same in all three apps; and text follows the system font size.

## Scope
### In scope
- A spacing scale on the Material Design 3 4dp grid, used for every margin, padding and gap.
- A type scale that follows Material Design 3 roles (display, headline, title, body, label), in the system font.
- A shape scale that follows Material Design 3 (corner sizes).
- A dark color scheme with every Material Design 3 color role, keeping the brand colors: purple `#8000FF` as
  primary and pink `#FF1F8A` as secondary. Screen-specific colors (iOS track, page, date line, synopsis, home
  gradient, ring center) map to a color role.
- Sizes shared across features or set by the platform (48 touch target, icon sizes, border width) as named size
  tokens in the design system. A size that belongs to one component or one feature (poster 124×188, rating ring,
  favorite card) stays in that component's file: inline when used once, a named constant when used in two or more
  places, and in a public `<Component>Defaults` object when another module needs it.
- The same token values in all three apps. iOS keeps its own controls and gestures (navigation bar, alerts,
  sheets) while it uses the same spacing, type, shape and color values.
- Text that scales with the system setting: Android font scale, iOS Dynamic Type.
- Every existing screen and shared component in all three apps: Onboarding, Home (with its favorites section),
  My favorites, Title details, the remove-favorite dialog, poster tiles and rows, the meta line, retry messages.
- The rule "use tokens, not raw values" written in the project docs, and the existing `docs/features/*/design/*.md`
  updated to name tokens in place of raw values.

### Out of scope
- A light theme or following the system light/dark setting.
- Any change to features, flows, copy or navigation.
- Replacing current layouts with different Material Design 3 components (top app bars, cards, list items) beyond
  what the token change needs.
- An automated check (lint or CI) that rejects raw values; the rule is a documented convention.
- A custom font.

## User flow
1. A user opens any app and goes through Onboarding, Home, My favorites and Title details as today.
2. Every screen shows the same content and behaves the same, with spacing, text sizes, corners and colors taken
   from the design system.
3. A user who has raised the system font size sees larger text on every screen, and the layout still fits.

## UI states
| State | When | What the user sees / can do |
|---|---|---|
| Default font size | System font scale 100% / default Dynamic Type | Each screen as today, values rounded to the nearest token |
| Large font size | System font scale up to 200% / larger Dynamic Type sizes | Larger text; content wraps or truncates, nothing overlaps or is cut off |
| Existing screen states | Loading, empty, content, error, offline on each screen | Unchanged behavior, restyled with tokens |

## Acceptance criteria
- **AC-1** One set of tokens in all three apps
  - Given the design system of `cmp`, `native-android` and `native-ios`
  - When their spacing, type, shape, color and component-size tokens are compared
  - Then each app defines the same tokens with the same names and values (allowing for each platform's naming style)
- **AC-2** Spacing on the 4dp grid
  - Given the spacing scale
  - When its values are listed
  - Then every value is a multiple of 4 (with 0 and 2 allowed for hairline gaps only if kept)
- **AC-3** No raw values in screens and components
  - Given the screen and shared component code of each app
  - When it is searched for numeric dp/sp/pt values, font sizes, corner radii and color literals
  - Then spacing, text styles, corner shapes and colors all come from design-system tokens (color literals and token
    values live only in the design-system definition); any other size used in one place is written inline where it
    is used, and a size used in two or more places is a named constant in its file, or in its `<Component>Defaults`
    object when another module needs it
- **AC-4** Old values rounded to the nearest token
  - Given a value used today that is not on the scale (for example 31dp or 15sp)
  - When the screen is redone
  - Then it uses the nearest token, and the screen keeps its layout, content and behavior, with only small
    differences of a few points from the Figma design
- **AC-5** Full Material Design 3 dark color scheme with the brand colors
  - Given the color scheme
  - When it is inspected
  - Then it defines every Material Design 3 color role for dark (including surface containers, outline and error),
    primary is based on `#8000FF` and secondary on `#FF1F8A`, and text stays readable on its background
- **AC-6** Text follows the system font size
  - Given the system font size set to its largest common setting (Android 200%, iOS large Dynamic Type sizes)
  - When the user opens each screen
  - Then text is larger, and no text is cut off mid-line, overlaps other content, or pushes content off screen
    with no way to scroll to it
- **AC-7** The three apps match
  - Given the same screen open in the three apps with the same data
  - When they are compared side by side
  - Then spacing, text styles, corner shapes and colors match; only platform controls (navigation bar, alerts,
    sheets on iOS) differ
- **AC-8** Convention and design docs
  - Given the project docs and the `docs/features/*/design/*.md` files
  - When they are read
  - Then they state that screens use tokens, not raw values, and the design docs name the tokens each screen uses

- **AC-9** Favorite heart is the same on every screen
  - Given a title that is a favorite
  - When it appears on Home (favorites section), My favorites or Title details, in any of the three apps
  - Then it shows the same filled heart as My favorites (filled pink heart on a round background); a title that is
    not a favorite shows the outline heart (Title details only)

## Edge cases
- **EC-1** A very long title name at a large font size -> it wraps or ends with "…" as on that screen today; the
  layout does not break.
- **EC-2** Two different old values round to the same token -> accepted; both use that token.
- **EC-3** A value exactly between two tokens -> it rounds to the larger token unless that breaks the layout.
- **EC-4** A fixed-size component (poster, rating ring) at a large font size -> the component keeps its size; text
  inside or beside it scales and truncates instead of overflowing.
- **EC-5** Touch targets -> every tappable element keeps at least a 48×48 (Android) / 44×44 (iOS) touch area after
  rounding.

## Constraints
- Platforms stay as today: Android minSdk 24, iOS 16 (no iOS 17-only APIs).
- Dark theme only.
- Features, copy, navigation, localization (en, pt-BR) and offline behavior must not change.
- `native-android` and `native-ios` stay standalone and do not depend on `cmp`; each defines its own copy of the
  tokens with the same values.
- Text contrast meets Material Design 3 / WCAG AA guidance for the roles used, with one accepted exception: white
  labels on the brand pink `#FF1F8A` (about 3.6:1, AA for large or bold text only).

## Open questions
- [ ] Exact token values and names (spacing steps, type roles in use, shape sizes, component sizes) (owner: dev,
  needed before blueprint)
- [ ] Whether the Home background gradient on iOS stays a gradient between two color roles or becomes a flat
  surface (owner: user, needed before blueprint)

## Change log
- 2026-11-24: Initial version.
- 2026-11-24: Component- and feature-specific sizes stay next to their component (`<Component>Defaults` when shared),
  not in the design system; AC-3 and the size scope updated. Added the white-on-pink contrast exception.

## Subtasks
1. [01 - cmp: tokens and smaller screens](subtasks/01-cmp-tokens-and-small-screens.md)
2. [02 - cmp: Home and Title details](subtasks/02-cmp-home-and-title-details.md)
3. [03 - native-android: tokens and smaller screens](subtasks/03-native-android-tokens-and-small-screens.md)
4. [04 - native-android: Home and Title details](subtasks/04-native-android-home-and-title-details.md)
5. [05 - native-ios: tokens and smaller screens](subtasks/05-native-ios-tokens-and-small-screens.md)
6. [06 - native-ios: Home and Title details](subtasks/06-native-ios-home-and-title-details.md)
7. [07 - Cross-app comparison and docs](subtasks/07-parity-and-docs.md)
- 2026-11-24: Sizes and other values used in one place are written inline; named constants only for values
  used in two or more places.
- 2026-11-24: Added AC-9: a favorite shows the same filled heart on Home, My favorites and Title details in all three
  apps.
