# Plan: native-android in Vietnamese

Requirement: [02 - native-android in Vietnamese](../subtasks/02-native-android.md)
Status: Done

## Summary
Mirror 01 in `native-android`: republish the shared core (which now has `Language.VI_VN`, the Vietnamese trailer
language and the favorites fallback order), add the Vietnamese branch to the app's release date and runtime
formatting, add `values-vi/strings.xml` with every key, and a Vietnamese entry in the preview annotations.

## Decisions
- Same formats, copy and preview entries as 01 (auto) — CLAUDE.md: a value changes in all apps together; the copy
  matches `cmp` word for word so the user reviews one set.
- Resource qualifier `values-vi` (auto) — any `vi` region resolves to it; rejected `values-vi-rVN`.
- `favorites_count` uses `%d` like the existing app strings, only the `other` item (auto) — Vietnamese has no
  singular plural form.
- No locale config / per-app language (auto) — the language follows the device, as for pt-BR today.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Language from the device locale | `Language.fromLocaleTag(Locale.current.toLanguageTag())` | `HomeScreen.kt`, `FavoritesScreen.kt`, `TitleDetailsScreen.kt` |
| Date / runtime formatting | `formatReleaseDate`, `formatRuntime` | `app/.../titledetails/TitleDetailsFormat.kt` |
| Previews | `TmdbPreviews`, `TmdbComponentPreviews` | `app/.../designsystem/preview/TmdbPreviews.kt` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `native-android :app` | `TitleDetailsFormat.kt`, `DetailsHeader.kt`, `TmdbPreviews.kt`, `TitleDetailsFormatTest.kt` (mod); `res/values-vi/strings.xml` (new) | Vietnamese formatting, copy, previews |

## Steps
### Step 1: Republish the core and format dates and runtimes in Vietnamese  (AC-5, AC-6) ✅
- Files: `app/src/main/kotlin/.../titledetails/TitleDetailsFormat.kt`, `.../titledetails/component/DetailsHeader.kt`
  (modify)
- What: `./gradlew publishSharedCore` (repo root); `formatReleaseDate` gets
  `Language.VI_VN -> "${date.day} thg ${date.month.number}, $year"`; `formatRuntime(minutes: Int, language: Language)`
  with ` giờ` / ` phút` for `VI_VN`, `h` / `m` otherwise; `DetailsHeader` passes `language`.
- Unit tests (`TitleDetailsFormatTest`): `vietnameseDateIsDayThgMonthYear`, `vietnameseRuntime`,
  `portugueseRuntimeIsUnchanged`; existing runtime tests pass `Language.EN_US`.
- Compile: `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` (in `native-android/`)

### Step 2: Vietnamese copy and previews  (AC-1, EC-4) ✅
- Files: `app/src/main/res/values-vi/strings.xml` (new) with the same keys and Vietnamese text as 01 Step 3;
  `TmdbPreviews.kt` (modify): `@Preview(name = "Tiếng Việt", locale = "vi", …)` after pt-BR in both annotations.
- Compile: `./gradlew :app:compileDebugKotlin`

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 | 2 | Manual: How to test 2; Vietnamese previews |
| AC-5 | 1 | `TitleDetailsFormatTest.vietnameseDateIsDayThgMonthYear`, `vietnameseRuntime` |
| AC-6 | 1 | `portugueseRuntimeIsUnchanged`, existing format and ViewModel tests |
| EC-4 | 2 | Manual: largest font in Vietnamese, large-text preview |

## Out of scope
- `native-ios` -> 03; README -> 04.

## Risks
- `publishSharedCore` also writes the iOS Swift package, which `native-ios` builds against — 03 updates that app next.
