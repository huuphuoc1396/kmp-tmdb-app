# Remove comments

Status: Confirmed
Source: chat (`/spark remove toàn bộ comment trong project này`)

## Goal
Comments in the project tend to go stale or repeat what the code already says. Remove every human-written
comment so the code explains itself through names and tests, and make "no comments" a project convention so
new code stays that way. Success: no human-written comment remains in scope, nothing behaves differently, and
every check still passes.

## Scope
### In scope
- Every tracked file except `*.md`: Kotlin (`.kt`, `.kts`), Swift, SQLDelight (`.sq`), XML (backup rules,
  drawables, manifests), shell scripts, `.toml`, `.yml` (including CI workflows), `.properties`, `.gitignore`
  and similar config files.
- KDoc on the shared core's public API (`core/*`, `service/*/domain`), even though it is published to the
  native apps.
- Adding a "code has no comments" convention to the project instructions, and updating existing rules that
  ask for comments (e.g. the one-line KDoc required on each convention plugin).

### Out of scope
- `*.md` files.
- Files created or managed by tools: Xcode `project.pbxproj` and other Xcode-managed files, Gradle wrapper
  scripts (`gradlew`, `gradlew.bat`), generated code under `build/`.
- Template files `*.example` (e.g. `secrets.properties.example`).
- Comments read by tools: shebangs (`#!`), lint directives (`swiftlint:`, `ktlint-disable`), `MARK:`.
- Annotations such as `@Suppress` (not comments).
- Preserving the removed explanations: no renaming and no moving content into docs; git history is the record.
- Automated enforcement (a Detekt rule or script that fails CI on comments).

## User flow
1. A developer opens any in-scope file and sees code with no human-written comments.
2. A developer (or agent) writing new code follows the project convention and adds no comments.

## UI states
Not applicable: no user-facing change.

## Acceptance criteria
- **AC-1** No human-written comments remain
  - Given the in-scope files
  - When the repository is searched for comment syntax (`//`, `/* */`, `/** */`, `--`, `#`, `<!-- -->`)
  - Then the only matches are kept tool directives, shebangs, or comment-like text inside strings and URLs
- **AC-2** Kept items are untouched
  - Given shebangs, tool directives, tool-managed files and `*.example` files
  - When the change is applied
  - Then they are byte-for-byte unchanged
- **AC-3** No behavior change
  - Given the diff
  - When it is reviewed
  - Then it only removes comment lines and the whitespace they leave behind
- **AC-4** All checks pass
  - Given the change is applied
  - When `./gradlew detekt testAndroidHostTest koverVerify :architecture-test:test` and `./gradlew assemble` run,
    and `native-android` and `native-ios` are built against the republished shared core
  - Then all succeed
- **AC-5** The convention is written down
  - Given the project instructions (CLAUDE.md and `.claude/rules/`)
  - When a developer reads them
  - Then they state that code has no comments, and no rule asks for a comment or KDoc

## Edge cases
- **EC-1** Comment-like text inside a string or URL (`"https://…"`, `#` in a string, `--` in SQL literals) ->
  left unchanged.
- **EC-2** A file or block that held only a comment (e.g. an XML header, a KDoc-only line above a declaration)
  -> the file stays valid and parses as before.
- **EC-3** Removing a comment leaves blank lines, an empty block, or trailing whitespace -> whitespace is tidied
  so Detekt/ktlint and SwiftLint (if used) still pass.
- **EC-4** A trailing comment on a line of code (`val x = 1 // …`) -> only the comment part is removed, the code
  stays on its line.
- **EC-5** A lint directive sits next to an ordinary comment -> the directive is kept, the ordinary comment is
  removed.

## Constraints
- No functional, build-configuration, or public API signature changes.
- Must work with the existing checks as they are: Detekt + ktlint (max line length 120), Kover thresholds,
  Konsist architecture tests.
- Both standalone native apps must still build against the published shared core.

## Open questions
- [x] The saved agent memory "No spec IDs in code comments" is obsolete once no comments are allowed;
  remove it after this lands (owner: agent, after implementation).

## Change log
- 2026-11-21: Initial version.
