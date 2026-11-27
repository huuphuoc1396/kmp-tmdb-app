# Plan: native-ios in Vietnamese

Requirement: [03 - native-ios in Vietnamese](../subtasks/03-native-ios.md)
Status: Done

## Summary
The Swift package from `publishSharedCore` (run in 02) already exposes `Language.viVn`. Add the Vietnamese case to
`TitleDetailsFormat` (date and a language-aware runtime), add a `vi` localization for every key in
`Localizable.xcstrings`, and add `vi` to the project's `knownRegions` so `Bundle.main.preferredLocalizations`
reports `vi` on a Vietnamese device and `HomeLanguage` maps it to `.viVn`.

## Decisions
- Same date / runtime formats and copy as 01 (auto) — values change in all three apps together.
- `vi` localization in the string catalog plus `knownRegions` (auto) — iOS only reports a language the bundle
  declares; rejected: `CFBundleLocalizations` in `Info.plist`, which this app does not use (it relies on the catalog).
- `favorites.count %lld` gets only the `other` plural variation for `vi` (auto) — Vietnamese has no singular form.
- The string catalog is rewritten with Xcode's JSON layout (2-space indent, `" : "`, sorted keys) (auto) — keeps the
  diff to the added `vi` entries.
- No new SwiftUI preview (auto) — the subtask does not ask for one; the language-switch previews stay as they are.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Device language → `Language` | `HomeLanguage.from(preferredLocalizations:)` | `TmdbNative/Home/HomeLanguage.swift` |
| Date / runtime formatting | `TitleDetailsFormat` | `TmdbNative/TitleDetails/TitleDetailsFormat.swift` |
| Copy | `Localizable.xcstrings` | `TmdbNative/` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `native-ios` | `TitleDetailsFormat.swift`, `Component/DetailsHeader.swift`, `Localizable.xcstrings`, `project.pbxproj`, `TitleDetailsFormatTests.swift`, `HomeLanguageTests.swift` (mod) | Vietnamese formatting, copy, language declaration |

## Steps
### Step 1: Vietnamese date and runtime  (AC-5, AC-6) ✅
- Files: `TitleDetailsFormat.swift`, `TitleDetails/Component/DetailsHeader.swift` (modify)
- What: `case .viVn: return "\(date.day) thg \(date.month), \(date.year)"`;
  `static func runtime(minutes: Int, language: Language) -> String` with ` giờ` / ` phút` for `.viVn`, `h` / `m`
  otherwise; `DetailsHeader` passes `language`.
- Unit tests (`TitleDetailsFormatTests`): `testReleaseDateInVietnamese` (`5 thg 12, 2021`),
  `testRuntimeInVietnamese` (`2 giờ 10 phút`, `2 giờ`, `45 phút`), `testRuntimeInPortugueseIsUnchanged`;
  existing runtime tests pass `.enUs`.
- Compile: `xcodebuild -project TmdbNative.xcodeproj -scheme TmdbNative -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`

### Step 2: Vietnamese copy and language declaration  (AC-1, EC-4) ✅
- Files: `Localizable.xcstrings`, `TmdbNative.xcodeproj/project.pbxproj` (modify), `HomeLanguageTests.swift` (modify)
- What: a `vi` localization (`state: translated`) for every key, with the 01 copy (`%@`, `%lld` placeholders kept);
  `vi` added to `knownRegions`.
- Unit tests (`HomeLanguageTests`): `testVietnameseMapsToViVn` (`["vi"]`, `["vi-VN"]`).
- Compile: same as Step 1.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 | 2 | `HomeLanguageTests.testVietnameseMapsToViVn`; manual How to test 2 |
| AC-5 | 1 | `TitleDetailsFormatTests.testReleaseDateInVietnamese`, `testRuntimeInVietnamese` |
| AC-6 | 1 | `testRuntimeInPortugueseIsUnchanged`, existing format and language tests |
| EC-4 | 2 | Manual: largest Dynamic Type in Vietnamese |

## Out of scope
- README -> 04.

## Risks
- `knownRegions` is in the Xcode-managed project file — a one-line addition; Xcode keeps it on the next save.
