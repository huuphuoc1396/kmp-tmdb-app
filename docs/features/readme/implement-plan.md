# Plan: Project README

Requirement: [task.md](task.md)
Status: Done

## Summary
Write a self-contained English `README.md` at the repository root with the 11 sections of task.md, four Mermaid
diagrams (tiers and app consumption, module dependencies, offline-first data flow, key handling), and add an MIT
`LICENSE`. All facts come from `docs/architecture.md`, `CLAUDE.md`, the native app READMEs and
`.github/workflows/ci.yml`, checked against the code where they disagree. Docs only: nothing to compile or test;
verification is a manual review of the rendered page and of the commands against `CLAUDE.md`.

## Decisions
- Favorites are listed as a full feature (mark/unmark on Title details and a My favorites list), with a fourth
  screenshot row — user's choice over "list only what works today" (see Risks: the list screen is not built yet).
- Screenshots live in `docs/images/screenshots/<screen>.webp`, screen
  `onboarding` | `home` | `title-details` (3 files, not split by platform; favorites show inside Home and details) — user's choice; resolves task.md's open question.
- TMDB disclaimer is text plus a link to https://www.themoviedb.org/, no logo file — user's choice over committing or
  hotlinking the official logo.
- No library versions in the README text (they drift); the tech table names libraries and points to
  `gradle/libs.versions.toml` for versions. Badges are version-free for the same reason.
- Badges: CI status from `.github/workflows/ci.yml` (GitHub Actions badge for `huuphuoc1396/kmp-tmdb-app`), static
  shields.io badges for Kotlin Multiplatform, platforms Android | iOS, and license MIT.
- Where `docs/architecture.md` / `CLAUDE.md` are stale, the README follows the code: SQLCipher is in use (architecture
  12 still says "planned"); Title details is a real screen in all three apps (CLAUDE.md says placeholder). The stale
  docs are not edited here (out of scope in task.md).
- The module-dependency diagram uses the project's real modules (`title`, `favorites`, `settings` contexts,
  `cmp/feature/{onboarding,home,title-details}/ui`) rather than the generic shop example, plus a short table of
  forbidden edges.
- No empty `docs/images/screenshots/` folder is created (git cannot track it); the README lists the expected file
  names (EC-1).

## Reuse
| Need | Existing source | Location |
|---|---|---|
| Goals, three apps and how each consumes the core | Section 1 | `docs/architecture.md` |
| Tiers, module types, dependency rules + Mermaid base | Sections 5.1–5.3 | `docs/architecture.md` |
| Offline-first data flow | Section 7 | `docs/architecture.md` |
| Testing strategy | Section 8 | `docs/architecture.md` |
| Enforcement (build + Konsist) | Section 9 | `docs/architecture.md` |
| Security (token, Keystore/Keychain, SQLCipher, backups) | Section 10 | `docs/architecture.md` |
| Tech stack | Section 12 + version catalog | `docs/architecture.md`, `gradle/libs.versions.toml` |
| Commands, setup, TTLs, `domain → domain` allowlist | Setup, Commands, Architecture | `CLAUDE.md` |
| Native app workflows | READMEs | `native-android/README.md`, `native-ios/README.md` |
| CI steps | Workflow | `.github/workflows/ci.yml` |
| Token template | Example file | `secrets.properties.example` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| repo root | `LICENSE` (new) | AC-7 |
| repo root | `README.md` (new) | AC-1 – AC-8 |
| docs | `docs/features/readme/task.md` (modify) | Close the screenshot-path open question, change log line |

## Steps

### Step 1: MIT license  (AC-7) ✅
- Files: `LICENSE` (new)
- What: standard MIT license text, `Copyright (c) 2026 Phuoc Bui`.
- Unit tests: none (docs). Manual check: GitHub detects "MIT License" in the repo sidebar after push.
- Compile: none.

### Step 2: README header, overview, screenshots, acknowledgements  (AC-1, AC-5, AC-8, EC-1) ✅
- Files: `README.md` (new)
- What:
  - Title `# kmp-tmdb-app`, then a one-line tagline: Kotlin Multiplatform TMDB discovery app with one shared core and
    three apps.
  - Badges row: CI (`https://github.com/huuphuoc1396/kmp-tmdb-app/actions/workflows/ci.yml/badge.svg`, linking to
    the workflow), Kotlin Multiplatform, Android | iOS, License MIT (links to `LICENSE`).
  - `## Overview`: films, series, anime, novelas; English and Brazilian Portuguese with cached content per language;
    features: onboarding, Home (featured carousel + popular list), Title details, Favorites (mark/unmark and My
    favorites list). Table of the three apps: `cmp/` (Compose Multiplatform, shared ViewModel + UI),
    `native-android/` (Jetpack Compose, core as published Android libraries), `native-ios/` (SwiftUI, core as a
    local Swift Package). One sentence: same business core, different amounts of shared presentation.
  - `## Screenshots`: table, columns Onboarding | Home | Title details, one row, each cell
    `<img src="docs/images/screenshots/<screen>.webp" width="250">`; one line under it listing the folder
    and naming pattern so the owner can drop files in.
  - `## Acknowledgements`: design based on the TMDB Figma community file
    (https://www.figma.com/community/file/1059842558004825230/tmdb); data from the TMDB API
    (https://developer.themoviedb.org/).
- Compile: none.

### Step 3: Architecture  (AC-1, AC-2, AC-3, EC-2) ✅
- Files: `README.md` (modify)
- What, `## Architecture` with subsections:
  - `### Goals`: 4 bullets condensed from architecture 1 (one core / two ways to consume it, scales with the team,
    rules checked by the build, offline-first, fast feedback).
  - `### Three tiers`: table Libraries / Services / Features (directory, contains), the module list of `core/`
    (common, model, security, network, database, datastore, testing, di, ios) one line each, what `domain`, `data`,
    `fake` hold, what `nav`/`api`/`ui` hold, composition roots (`core:di`, app shells). Contexts: `title`,
    `favorites`, `settings`.
    Diagram A (Mermaid `flowchart LR`): subgraphs Libraries, Services, Features/apps; arrows from `cmp/` (Gradle
    modules), `native-android/` (Maven AARs from `build/maven-repo`), `native-ios/` (`TmdbCore` XCFramework via
    `core:ios` + SKIE) into `service/*/domain`.
  - `### Module dependencies`: Diagram B (Mermaid `graph TD`) with real modules: app shells + `core:di` →
    `cmp/feature/*/ui` and `service/*/data`; ui → `service/*/domain`, `cmp/designsystem`; `title/domain` →
    `favorites/domain` (dotted, "allowlisted"); data → own domain + `core:network`/`database`/`datastore` →
    `core:security`; domain/fake → kernel. Then a table of forbidden edges (`ui → ui`, `ui → data`, `data → data`,
    `fake → data`, `domain → data/fake`, `fake` outside tests, shared core → app, `native-*` → `cmp/`) and how
    each is enforced: `tmdb.module` (`DependencyRules`, fails configuration with `Rule: <from> -> <to>`), Konsist
    (no framework imports in `domain`/`core:model`/`nav`, package matches path), and the published API boundary for
    `native-*`. Short "feature A needs feature B" order (domain → designsystem → slot → `api` entry).
  - `### Offline-first`: Diagram C (Mermaid `sequenceDiagram`): UI ← ViewModel ← Repository.observe() ← SQLDelight;
    refresh path Repository → Ktor → TMDB → upsert → Flow emits. Bullets: database is the single source of truth;
    stale-while-revalidate with `fetchedAt`, TTL 6 h home lists, 24 h details; cache keyed by language
    (`en-US`/`pt-BR`); favorites local only; errors map to `AppError` (Network, Unauthorized, NotFound, Unknown) in
    `AppResult` and never clear the cache; use cases only when they add logic (`ObserveFavoriteTitles` example).
  - Each diagram has 1–3 sentences before it that state the same thing in words (EC-2).
- Compile: none.

### Step 4: Project structure, tech stack and decisions  (AC-1, AC-4) ✅
- Files: `README.md` (modify)
- What:
  - `## Project structure`: trimmed tree: `build-logic/`, `core/`, `service/{title,favorites,settings}/`,
    `cmp/{designsystem,composeApp,androidApp,iosApp,feature}/`, `native-android/`, `native-ios/`,
    `architecture-test/`, `docs/` (`architecture.md`, `features/`), one comment per line.
  - `## Tech stack and decisions`, one table per area, columns Choice | Why:
    - Local: SQLDelight (one database per context owns its schema, typed queries, `Flow` observation, runs on
      Android and iOS); SQLCipher 4 (database encryption on both platforms; official `SQLCipher.xcframework` on
      iOS); DataStore KMP + AES-GCM serializer (typed settings; `EncryptedSharedPreferences` is deprecated); lazy
      open on the IO dispatcher.
    - Network: Ktor client (multiplatform, OkHttp / Darwin engines, `MockEngine` for tests); kotlinx.serialization;
      `apiCall {}` mapping to `AppError`; token from `secrets.properties` / CI secret, XOR-obfuscated at build time,
      with the honest note that a client-side token can be extracted and a backend proxy is the real fix; no body
      logging in release; HTTPS only; no third-party leaf pinning (reason from architecture 10).
    - Security: master key in Android Keystore (non-exportable) / iOS Keychain (`AfterFirstUnlockThisDeviceOnly`,
      `cryptography-kotlin`); per-database 32-byte passphrase encrypted with the master key (envelope key); a
      database that can't be opened with its key is recreated; databases and keys excluded from OS backups.
      Diagram D (Mermaid `flowchart LR`): master key → encrypts passphrase → SQLCipher database; master key →
      AES-GCM → DataStore.
    - Other: Koin (DI, composition roots), Compose Multiplatform / Jetpack Compose / SwiftUI, Navigation Compose /
      `NavigationStack`, Coil 3 / Nuke, SKIE, Gradle version catalog + `build-logic` convention plugin
      `tmdb.module`, Detekt + ktlint, Kover, Konsist.
  - One line: versions in `gradle/libs.versions.toml`.
- Compile: none.

### Step 5: Getting started, testing and CI, license, disclaimer  (AC-1, AC-5, AC-6, AC-7, EC-3, EC-4) ✅
- Files: `README.md` (modify), `docs/features/readme/task.md` (modify)
- What:
  - `## Getting started`:
    - Prerequisites: JDK 21; Android SDK with `sdk.dir` in `local.properties`; macOS + Xcode 16+ for iOS (Android
      work proceeds without it thanks to `kotlin.native.ignoreDisabledTargets=true`, EC-4).
    - TMDB token: `cp secrets.properties.example secrets.properties`, set `TMDB_ACCESS_TOKEN` (API Read Access
      Token from TMDB settings); env var on CI; without it the build warns and calls fail with Unauthorized (EC-3).
    - `cmp/`: Android `./gradlew :cmp:androidApp:installDebug`; iOS open `cmp/iosApp/iosApp.xcodeproj` or the
      `xcodebuild … -scheme iosApp …` command from CLAUDE.md.
    - `native-android/`: `./gradlew publishSharedCore`, then `cd native-android && ./gradlew :app:installDebug`, or open
      `native-android/` in Android Studio; link `native-android/README.md`.
    - `native-ios/`: `./gradlew publishSharedCore` (macOS), then open the Xcode project or the `xcodebuild`
      build command; link `native-ios/README.md`.
    - Every command copied verbatim from `CLAUDE.md` Commands, each in its own `bash` block.
  - `## Testing and CI`: table of test levels (condensed architecture 8: host tests default, `commonTest` when
    Kotlin/Native behaviour matters, device tests for Keystore, iOS simulator tests, native app tests); principles
    (service fakes + contract tests, no mocking library, in-memory SQLDelight, Ktor `MockEngine`); commands: all CI
    checks, one module, platform tests, detekt/auto-correct, Kover report, architecture tests, `assemble`; CI runs
    on Ubuntu for push/PR to `main` (Detekt, host tests, Kover 80% on `domain`/`data`, Konsist) and what stays
    local (app builds, device/simulator tests, native apps).
  - `## License`: "MIT — see [LICENSE](LICENSE)."
  - `## Disclaimer`: "This product uses the TMDB API but is not endorsed or certified by TMDB." with a link to
    https://www.themoviedb.org/.
  - task.md: tick the open question with the chosen path, add change log line
    `- 2026-11-23: Screenshot path set to docs/images/screenshots/<platform>-<screen>.png.`
- Compile: none.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 | 2, 3, 4, 5 | Manual: heading list of README matches the 11 sections in order; no Roadmap |
| AC-2 | 3, 4 | Manual: open README on GitHub (or a Mermaid live preview) — diagrams A–D render |
| AC-3 | 3 | Manual: README answers "what is domain/data/fake", "forbidden deps", "where UI data comes from" |
| AC-4 | 4 | Manual: every row in Local / Network / Security has a Why |
| AC-5 | 2, 5 | Manual: Figma link, TMDB link and disclaimer sentence present |
| AC-6 | 5 | Manual: `diff` each command block against `CLAUDE.md` Commands; spot-run `./gradlew :architecture-test:test` |
| AC-7 | 1, 5 | Manual: `LICENSE` is MIT, 2026, Phuoc Bui; README links it |
| AC-8 | 2 | Manual: 3 images in `docs/images/screenshots/<screen>.webp` show in the README |
| EC-1 | 2 | Manual: table readable with images missing; naming line present |
| EC-2 | 3, 4 | Manual: text before each diagram explains it alone |
| EC-3 | 5 | Manual: missing-token behaviour described |
| EC-4 | 5 | Manual: macOS/Xcode-only steps marked |

## Out of scope
- Real screenshots (owner adds them to `docs/images/screenshots/`).
- Fixing stale lines in `docs/architecture.md` (SQLCipher "planned") and `CLAUDE.md` (Title details "placeholder").
- Building the My favorites list screen (favorites subtasks 02–05, merged on main before this PR).
- Roadmap, translated README, TMDB logo.

## Risks
- Facts drift from `docs/architecture.md` because the README is self-contained. Mitigation: README links
  `docs/architecture.md` as the source of truth for details; versions are not repeated.
- Mermaid syntax errors only show after push. Mitigation: check diagrams in the GitHub preview (or mermaid.live)
  before merging.
- Screenshots are missing until the owner adds them (broken images on GitHub meanwhile) — accepted in EC-1.

## Deviations
- Native iOS tests and native Android checks are also listed under Testing and CI (from `CLAUDE.md` Commands), so every
  test command in `CLAUDE.md` appears in the README.
- The Offline-first "per language" bullet says each language has its own cache and refresh, not that the other
  language is never shown: `ObserveFavoriteTitles` falls back to the other language's title data.
- Run scripts (`scripts/run-native-*.sh`) removed from the README at the user's request: native apps are opened and
  run as their own projects; `scripts/` dropped from the project tree.
- README condensed at the user's request (477 → ~230 lines): same 11 sections and 4 diagrams; Getting started and
  Testing keep only the main commands and link the native app READMEs for the rest.
- Screenshots changed to one row of 4 images, not split by platform, at the user's request.
- Favorites screenshot column dropped: favorites show inside the Home and Title details screenshots.
- Screenshots added as WebP (`onboarding`, `home`, `title-details`), so the README shows them directly and the
  placeholder line under the table is gone.
- Screenshots retaken on the iPhone 17 simulator (native-ios) and stored as 600 px wide JPEGs in two rows of three:
  onboarding, home, home-scrolled / title-details, title-details-scrolled, my-favorites. The WebP files are removed.
- Screenshots replaced by a single banner image `docs/images/screenshots.webp` (2000×1111) supplied by the user; the
  six JPEGs are removed.
