# Design system parity

Feature: [task.md](task.md) · Plan: [07](implement-plans/07-parity-and-docs.md)
Checked: 2026-11-24, after #53, with the final code of every app.

How it was checked:
- **Screenshots** (same data: Iron Man as a favorite, English, default text size; largest system size where
  noted):
  - `cmp` on the iOS simulator (subtasks 02 and the heart fix);
  - `native-android` on the Pixel_8 emulator (03, 04);
  - `native-ios` on the iOS simulator (05, 06).
- **Code comparison:** the token each element uses, per app.

`cmp` is the reference. "Match" means the same spacing, text style, shape and color token.

## Accepted platform differences
| Difference | Apps | Reason |
|---|---|---|
| System navigation bar with an iOS back button on My favorites and Title details | native-ios | iOS controls stay (task.md scope) |
| My favorites top padding `md` (others `xs`) | native-ios | Content sits under a system navigation bar |
| My favorites count badge in the navigation bar | native-ios | Toolbar item of the system bar |
| Rating ring value cap 1.12× (others 1.15×) | native-ios | Closest Dynamic Type step (`xLarge`) |
| Home favorites row reserves two name lines | native-ios | A SwiftUI horizontal `ScrollView` needs an explicit height; Compose sizes the row to its tallest item |
| Font rendering and system font | all | Roboto on Android, SF on iOS; sizes, weights and line heights are the same tokens |
| Remove dialog presentation | native-ios | Custom overlay; Compose uses `Dialog` |

## Onboarding
| Element | cmp | native-android | native-ios |
|---|---|---|---|
| Layout and spacing (`xxl` sides and bottom, `lg` gap, 320 max width) | reference | match | match |
| Text (`headlineLarge` Light, `bodyLarge`) | reference | match | match |
| Shapes (button `CircleShape`/`Capsule`) | reference | match | match |
| Colors (`onSurface` text, `primary`→`primaryContainer` button, artwork asset colors) | reference | match | match |

## Home
| Element | cmp | native-android | native-ios |
|---|---|---|---|
| Layout and spacing (`xxl` sides, `lg`/`xxxl` top/bottom, `md`/`xl`/`xxl` gaps) | reference | match | match |
| Header and search (`headlineMedium`, `bodyLarge` hint, 48 min height) | reference | match | match |
| Tabs (`titleMedium`/`bodyLarge`, `secondary` pill, `xxl` spacing) | reference | match | match |
| Poster rows (`PosterRowDefaults` 124×188, `small` corners, `sm` spacing) | reference | match | match |
| "Popular" heading (`titleLarge`) | reference | match | match |
| Background (artwork gradient + `primary` glow) | reference | match | match (asset colors) |

## Home favorites section
| Element | cmp | native-android | native-ios |
|---|---|---|---|
| Card (`large` corners, `surfaceContainer`, `surfaceContainerHigh` border) | reference | match | match |
| Title, badge, view all (`titleLarge`, `labelMedium` on `secondaryContainer`, `labelLarge` `secondary`) | reference | match | match |
| Items (96×148 poster, `titleSmall`/`bodySmall`, `sm`/`xs` gaps) | reference | match | match; row height see accepted differences |
| Heart (`FavoriteHeart`, filled) | reference | match | match |

## My favorites
| Element | cmp | native-android | native-ios |
|---|---|---|---|
| Page and header (`surface`, `headlineMedium`, `bodyMedium` subtitle) | reference | match | match; bar and padding see accepted differences |
| Badge (`labelMedium` `secondary` on `surfaceContainer`) | reference | match | match (in the navigation bar) |
| Card (`medium` corners, `surfaceContainer`, 120 min height, 64×96 thumbnail, `titleMedium`/`bodyMedium`) | reference | match | match |
| Heart (`FavoriteHeart`, filled) | reference | match | match |

## Remove favorite dialog
| Element | cmp | native-android | native-ios |
|---|---|---|---|
| Container (`extraLarge` corners, `surfaceContainer`, `xl`/`lg` padding) | reference | match (same Compose code) | match (code) |
| Text (`titleLarge`, `bodyMedium` `onSurfaceVariant`) | reference | match | match (code) |
| Buttons (`medium` corners, 48 min height, `outline` cancel, `secondary` confirm) | reference | match | match (code) |

The dialog was checked on screen in `cmp` (iOS) during 01. On `native-android` it is the same Compose code. On
`native-ios` it was compared in code only: simulator screenshots were unreliable during the 07 run.

## Title details
| Element | cmp | native-android | native-ios |
|---|---|---|---|
| Page (`surface`, artwork fade to `surface`, divider `outlineVariant`) | reference | match | match |
| Header (`titleLarge` name, `bodyLarge` `onSurfaceVariant` date line, wraps as a group) | reference | match | match |
| Rating ring (56, `surfaceContainerHigh` track, `secondary` arc, capped auto-sized value) | reference | match | match; cap see accepted differences |
| Synopsis (`bodyMedium` `onSurface`) | reference | match | match |
| Trailer button (`titleMedium` `onPrimary`, gradient, 44 min height) | reference | match | match |
| Section headings (`titleLarge`) | reference | match | match |
| Cast (76 wide, 60 photo, `labelMedium`, `surfaceContainerHigh`) | reference | match | match |
| Genres (`titleMedium`, `surfaceContainerHigh` capsule, 32 min height, `md`/`sm` spacing) | reference | match | match |
| Favorite heart (`FavoriteHeart`, outline ↔ filled) | reference | match | match |

## Result
No token-level mismatch was found. Every difference is an accepted platform difference listed above.

Large-text issues found while checking, for a separate task:
- "View all" wraps and loses its chevron.
- Names and long words break mid-word.
- The floating Details top bar overlaps content while scrolling.

Large text (`docs/features/large-text-layout/task.md`), `native-ios`: names use `NameText` (breaks only between
words, "…" at the cut), and the favorites header moves "View all ›" to its own line when it doesn't fit. The Title
details navigation bar stays the system bar (accepted platform difference).
