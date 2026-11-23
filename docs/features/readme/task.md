# Project README

Status: Confirmed
Source: chat (2026-11-23)

## Goal
The repository has no README, so a visitor on GitHub cannot tell what the project is, how it is built or how to run
it without reading `docs/architecture.md` and `CLAUDE.md`. A root `README.md` in English should explain the project,
its features, its architecture, its technology decisions and how to build and test it, self-contained, so that a
developer can understand and run the project from the README alone. It also credits the reference design and the
TMDB API.

## Scope
### In scope
- A root `README.md` in English, self-contained (it may repeat what `docs/architecture.md` says).
- Mermaid diagrams for the architecture.
- An MIT `LICENSE` file at the repository root.
- Screenshot placeholders at fixed image paths, to be filled in later by the owner.

### Out of scope
- A roadmap section.
- Taking real screenshots.
- Changes to `docs/architecture.md`, `CLAUDE.md`, `native-android/README.md` or `native-ios/README.md`.
- A translated (Vietnamese) README.

## User flow
1. A visitor opens the repository on GitHub and reads the README top to bottom.
2. Sections, in this order:
   1. **Title and badges**: project name, one-line tagline; badges for CI status, Kotlin, platforms (Android, iOS)
      and license.
   2. **Overview**: what the app is (TMDB discovery for films, series, anime and novelas; English and pt-BR), its
      features (onboarding, Home with featured and popular lists, title details, favorites), and the three apps
      that share one business core (`cmp/`, `native-android/`, `native-ios/`).
   3. **Screenshots**: one image per screen (onboarding, Home, title details, favorites), not split by
      platform, pointing to fixed paths.
   4. **Acknowledgements**: the reference design
      (https://www.figma.com/community/file/1059842558004825230/tmdb) and the TMDB API (https://www.themoviedb.org/).
   5. **Architecture**:
      - The three tiers: Libraries (`core/`), Services (`service/<context>/{domain,data,fake}`), Features
        (`cmp/feature/<x>/{nav,ui}`), what each tier holds and how they work together, plus the composition roots.
      - How each app consumes the core: KMP modules (`cmp/`), published Android libraries (`native-android/`),
        a local Swift Package with an XCFramework (`native-ios/`).
      - Dependencies between modules: what may depend on what, what is forbidden (`ui → ui`, `ui → data`,
        `data → data`, `domain → data/fake`, `fake` outside tests, apps never inside the core), and how the build
        and the architecture tests enforce it.
      - Offline-first data flow: UI reads from the local database as the single source of truth; refresh goes
        network → upsert into the database; stale-while-revalidate (6 h home lists, 24 h details); cache per
        language; favorites local only; an error never clears cached data.
   6. **Project structure**: a trimmed directory tree with one line per top-level area.
   7. **Tech stack and decisions**, each choice with the reason it was made:
      - Local: SQLDelight per context, SQLCipher encryption, DataStore for settings.
      - Network: Ktor client, kotlinx.serialization, error mapping to `AppError`, the obfuscated access token and
        why a client-side token can still be extracted.
      - Security: master key in Android Keystore / iOS Keychain, AES-GCM settings, per-database envelope keys,
        databases and keys excluded from OS backups.
      - Other: Koin, Compose Multiplatform / Jetpack Compose / SwiftUI, Coil and Nuke, SKIE, build-logic convention
        plugins.
   8. **Getting started**: prerequisites (JDK 21, Android SDK, macOS + Xcode for iOS), `secrets.properties` with the
      TMDB token, then build and run for each app: `cmp` Android and iOS, `native-android` and `native-ios`
      (publish the shared core first; each native app is opened and run as its own project in Android Studio / Xcode,
      no run scripts).
   9. **Testing and CI**: where tests run (Android host, Android device, iOS simulator), fakes instead of mocks,
      architecture tests, 80% coverage gate, the commands CI runs and what runs only locally.
   10. **License**: MIT, linking `LICENSE`.
   11. **Disclaimer**: TMDB attribution as TMDB requires it.

## UI states
Not applicable (documentation only).

## Acceptance criteria
- **AC-1** All sections present, in order
  - Given the root `README.md`
  - When a reader scans its headings
  - Then it contains the 11 sections of the user flow in that order, in English, and no roadmap section
- **AC-2** Architecture diagrams render
  - Given the Architecture section
  - When it is viewed on GitHub
  - Then it shows Mermaid diagrams for at least: the three tiers, module dependencies (allowed and how apps
    consume the core), and the offline-first data flow, each rendering without errors
- **AC-3** Architecture explained without other docs
  - Given a reader who has read only the README
  - When asked what a `domain`, `data` and `fake` module is for, which dependencies are forbidden, and where the UI
    gets its data
  - Then the README answers each of those questions
- **AC-4** Tech decisions explained
  - Given the Tech stack and decisions section
  - When a reader looks at Local, Network and Security
  - Then each listed technology has a reason it was chosen
- **AC-5** Credits and disclaimer
  - Given the README
  - When a reader looks at Acknowledgements and Disclaimer
  - Then the Figma community design link and the TMDB link are present, and the disclaimer states that the product
    uses the TMDB API but is not endorsed or certified by TMDB
- **AC-6** Commands are correct
  - Given the Getting started and Testing sections
  - When a developer runs the listed commands on a machine meeting the prerequisites
  - Then they match the commands in `CLAUDE.md` and work as written for all three apps and the checks
- **AC-7** License
  - Given the repository root
  - When a reader opens `LICENSE`
  - Then it is the MIT license with copyright holder "Phuoc Bui" and year 2026, and the README links to it
- **AC-8** Screenshot placeholders
  - Given the Screenshots section
  - When the owner adds images later
  - Then each image has a fixed, documented path per screen (not per platform), and the README shows how they are laid out

## Edge cases
- **EC-1** Image files not added yet -> the Screenshots table still reads correctly (broken images are acceptable
  until the owner adds them; paths are listed so they are easy to fill in).
- **EC-2** Mermaid not rendered (non-GitHub viewer) -> surrounding text still explains each diagram on its own.
- **EC-3** No TMDB token -> Getting started says the build warns and TMDB calls fail with Unauthorized, and how to
  fix it.
- **EC-4** No macOS / Xcode -> Getting started says which steps need it and that Android work still proceeds.

## Constraints
- English only.
- Self-contained; content must agree with `docs/architecture.md` and `CLAUDE.md` at the time of writing.
- Diagrams in Mermaid, GitHub-compatible.
- No secrets (token values) anywhere in the README.
- Follows TMDB's attribution requirements for the disclaimer.

## Open questions
- [x] Exact screenshot image folder: `docs/images/screenshots/<screen>.png`.

## Change log
- 2026-11-23: Initial version.
- 2026-11-23: Screenshot path set to docs/images/screenshots/<platform>-<screen>.png.
- 2026-11-23: Native apps run by opening their own project; the run scripts are not documented.
- 2026-11-23: Screenshots are one image per screen, not split by platform (`docs/images/screenshots/<screen>.png`).
