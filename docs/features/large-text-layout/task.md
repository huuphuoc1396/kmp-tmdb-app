# Large text layout

Status: Confirmed
Source: chat; `docs/features/design-system/parity.md` (section Result); Out of scope sections of
`docs/features/design-system/implement-plans/02`, `04` and `06`

## Goal
At the largest system text size, three layouts break in the apps: the Home favorites header pushes its "View all"
chevron out of the card, names break in the middle of a word ("Rober t Do…", "Doomsda y"), and the floating top
bar on Title details overlaps the content while scrolling. People who read with large text should see every
control whole, every word whole, and no control drawn over content. The same behavior applies to all three apps
(`cmp`, `native-android`, `native-ios`), except where a platform difference is listed below.

## Scope
### In scope
- Home favorites section header ("Favorites", count badge, "View all ›"), all three apps.
- Title and person names on Home (featured, popular and favorites section), My favorites and Title details
  (header name, cast, recommendations), all three apps.
- Title details top bar (back pill and favorite heart) in `cmp` and `native-android`.
### Out of scope
- `native-ios` Title details navigation bar: it is a system bar that is always opaque and does not overlap
  content; it stays as it is (accepted platform difference, `docs/features/design-system/parity.md`).
- iOS accessibility Dynamic Type sizes (AX1–AX5).
- Other text (synopsis, headings, chips, buttons, date line) and other screens (onboarding).
- Changes to token values or to the layout at the default text size.

## User flow
1. The user sets the system text size to the largest (Android font scale 200%, iOS largest standard Dynamic Type)
   and opens the app in English or Portuguese (Brazil).
2. On Home with favorites, the favorites header shows "Favorites" and the count; if "View all ›" doesn't fit on
   the same line, it moves to its own line below, at the trailing edge, with its chevron.
3. Names in lists, cards and on Title details wrap only between words.
4. On Title details (`cmp`, `native-android`), the top bar is transparent over the artwork; when the user scrolls
   and content moves under it, the bar gets a solid background; scrolling back to the top makes it transparent
   again.

## UI states
| State | When | What the user sees / can do |
|---|---|---|
| Header fits | Title, badge and "View all ›" fit on one line | One line, as today |
| Header wrapped | They don't fit on one line | Title and badge on the first line; "View all ›" on the next line at the trailing edge, one line, chevron inside the card |
| Top bar at top | Title details scrolled to the top (`cmp`, `native-android`) | Transparent bar over the artwork, as today |
| Top bar scrolled | Content scrolled under the bar (`cmp`, `native-android`) | Same back pill and heart on a solid surface background; nothing new in the bar; content no longer visible behind the controls |

## Acceptance criteria
- **AC-1** "View all" stays whole
  - Given the largest text size and at least one favorite
  - When Home shows the favorites section in English or Portuguese (Brazil)
  - Then "View all" is on one line, its chevron is next to it and inside the card, and both stay tappable
- **AC-2** Header wraps only when needed
  - Given the favorites header does not fit on one line
  - When Home is shown
  - Then "View all ›" moves to its own line below the title and badge, aligned to the trailing edge
  - And when the header fits (for example at the default text size), it stays on one line as today
- **AC-3** Names never split mid-word
  - Given the largest text size
  - When a title or person name is shown on Home, My favorites or Title details (header name, cast,
    recommendations)
  - Then lines break only between words; no word is split across two lines
- **AC-4** Names that don't fit are cut with an ellipsis
  - Given a name whose line limit is reached, or a single word wider than its whole line
  - When it is shown
  - Then the visible text ends with "…" at the cut, and the cast name keeps its 2-line limit
- **AC-5** Title details name wraps freely
  - Given the largest text size and a long title name
  - When Title details is shown
  - Then the name wraps between words over as many lines as it needs (only a single word wider than the line is
    cut, per AC-4)
- **AC-6** Top bar gets a background on scroll (`cmp`, `native-android`)
  - Given Title details is open
  - When the user scrolls so that content moves under the top bar
  - Then the bar shows a solid surface background behind the back pill and heart, and no content shows through
    behind them
  - And when the user scrolls back to the top, the bar is transparent over the artwork again
- **AC-7** Top bar controls unchanged
  - Given the top bar in either state
  - When the user taps back or the heart, or uses a screen reader
  - Then they behave and are announced as today, and the bar shows no new content
- **AC-8** Default text size unchanged
  - Given the default text size
  - When Home, My favorites and Title details are shown
  - Then their layout is the same as before this task, except the top bar background on scroll (AC-6)
- **AC-9** Same behavior in all three apps
  - Given the same screen, language and largest text size
  - When it is shown in `cmp`, `native-android` and `native-ios`
  - Then AC-1 to AC-5 hold in all three, and AC-6 and AC-7 hold in `cmp` and `native-android`

## Edge cases
- **EC-1** Large favorites count (two or more digits) -> the badge stays on the title line; the header wraps per
  AC-2 if needed.
- **EC-2** Portuguese (Brazil) "Ver todos" and other longer labels -> same as AC-1 and AC-2.
- **EC-3** A one-word name wider than its cell (cast, poster card) -> cut with "…" at the end of the word's line,
  never split onto the next line.
- **EC-4** Name still loading or missing (placeholder text) -> the placeholder follows the same rule.
- **EC-5** Pull-to-refresh on Title details while at the top -> the bar stays transparent.
- **EC-6** Title details loading or error state -> the bar is transparent while nothing is scrolled under it, and
  gets the background per AC-6 if content scrolls under it.
- **EC-7** Rotation or returning to Title details already scrolled -> the bar shows the state for the current
  scroll position.

## Constraints
- Check at Android font scale 200% and iOS largest standard Dynamic Type (xxxLarge), in English and Portuguese
  (Brazil), on a narrow phone; also check the default size (AC-8).
- Design-system tokens only (spacing, text styles, shapes, colors); the top bar background is a surface color
  token.
- Touch targets stay at least the minimum touch target.
- Screen reader order and labels stay as they are.
- `native-ios` Title details navigation bar stays a system bar (out of scope).
- Design docs (`docs/features/home/design/home.md`, `docs/features/favorites/design/home-favorites.md`,
  `docs/features/title-details/design/title-details.md`) and `docs/features/design-system/parity.md` record the
  new behavior.

## Open questions
- None.

## Change log
- 2026-11-24: Initial version.
