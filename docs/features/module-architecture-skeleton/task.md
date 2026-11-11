# Module architecture skeleton

Status: Confirmed
Source: chat + [docs/architecture.md](../../architecture.md) section 4

## Goal
Bring the `main` skeleton in line with the module architecture agreed in `docs/architecture.md` section 4 (Libraries / Services / Features), while there is still no production code to move. The dependency rules must be enforced by the build itself, not only described in the docs, so every later feature that creates service or feature modules is checked from its first commit.

## Scope
### In scope
- Remove the `core:domain` and `core:data` modules. No `service/*` module is created: each service context (`title`, `favorites`, `settings`) is created by the first feature that needs it.
- `core:di` and `core:testing` no longer depend on the removed modules; `core:testing` depends only on the kernel (`core:common`, `core:model`).
- One shared build setup per module that infers the module type from its path and replaces the current library and coverage setups. Every type in the architecture is known up front, including the ones that only exist on `cmp` (feature `nav` / `api` / `ui`, `core/designsystem`); Compose-specific setup is left to `cmp`.
- The dependency-rule table from `docs/architecture.md` 4.3 is enforced by the build, including the `domain → domain` allowlist. The allowlist already contains `title → favorites`; no real dependency is added.
- A shared module whose path matches no known type is rejected.
- Module (klib) names are unique per path, because leaf names repeat across contexts and features.
- Architecture tests also cover `service/`: package matches the path, and `domain` imports no frameworks. No file exists there yet.
- Update `CLAUDE.md`, `docs/architecture.md` and `.claude/rules/` to describe the new state: no "today / after the migration" notes, no `core:domain` / `core:data`, and service contexts are created together with features.

### Out of scope
- Any `service/*` module, including `fake` modules (created on demand by features).
- Production code of any kind.
- Feature modules and Compose setup (`cmp` / `native`).
- Updating the onboarding subtasks and implement plans to the new structure: a follow-up done right after this task.
- Adopting `kotlin.time.Clock`: checked when the first code needs time.
- Finished feature docs that describe the old structure (history, left as is).

## User flow
The "user" is a developer working on the repository.

1. The developer runs the CI checks locally on a clean checkout. They pass, even though no service module exists yet.
2. The developer creates the first module of a service context or feature by adding it under its path and applying the shared build setup. The module is configured according to its type with nothing else to declare.
3. The developer adds a project dependency that the architecture forbids. The build fails before any task runs, with a message naming both modules and the rule.
4. The developer writes code in a `domain` module that imports a framework or uses the wrong package. The architecture tests fail with the rule.

## UI states
Not applicable (no UI).

## Acceptance criteria
- **AC-1** Old modules removed
  - Given the repository after this task
  - When the developer reads the list of included modules
  - Then `core:domain` and `core:data` are absent, and no `service/*` module exists
- **AC-2** One build setup, configured by path
  - Given any shared (KMP) module
  - When its build file is read
  - Then it applies only the shared module setup, and the configuration matches the type inferred from its path (for example coverage verification only for `domain` and `data` types)
- **AC-3** Forbidden dependency fails the build
  - Given a temporary module that adds a dependency the table forbids (for example a `data` module depending on another context's `data`)
  - When any Gradle task is run
  - Then the build fails during configuration with a message naming the source module, the target module and the broken rule
- **AC-4** `domain → domain` limited
  - Given a temporary `domain` module that depends on another context's `domain` not in the allowlist, or a chain deeper than one level
  - When any Gradle task is run
  - Then the build fails with a message naming the modules and the rule
- **AC-5** Unknown path rejected
  - Given a temporary shared module at a path that matches no known type (for example `:core:foo:bar`)
  - When any Gradle task is run
  - Then the build fails with a message that the module type is unknown
- **AC-6** Architecture tests cover `service/`
  - Given a temporary file in a `service/<ctx>/domain` source set that imports a framework, or declares a package that does not match its path
  - When the architecture tests run
  - Then they fail with the rule
- **AC-7** CI command unchanged and green
  - Given the repository after this task, with no `domain` or `data` module
  - When `./gradlew detekt jvmTest koverVerify :architecture-test:test` runs
  - Then every step runs and passes; once a feature adds a `domain` or `data` module, the 80% line-coverage threshold applies to it without extra setup
- **AC-8** All targets still build
  - Given macOS with Xcode
  - When `./gradlew assemble` runs
  - Then Android, iOS device, iOS simulator and JVM targets build, with no duplicate module (klib) names
- **AC-9** Docs match the new state
  - Given `CLAUDE.md`, `docs/architecture.md` and `.claude/rules/`
  - When a developer reads them
  - Then they describe no `core:domain` / `core:data`, no pending migration, the shared module setup that replaces the old plugins, and that service contexts are created with the features that need them

## Edge cases
- **EC-1** A module outside the shared-module setup (`architecture-test`, the included `build-logic` build) -> not checked by the type rules and keeps working as before.
- **EC-2** A test-only dependency on a `fake` module or `core:testing` -> allowed in test source sets, rejected in production source sets.
- **EC-3** A `cmp`-only type (feature `ui`, `core/designsystem`) created on `cmp` after merging `main` -> recognized and checked without changes to the rule table.
- **EC-4** Configuration cache on, a second run with no changes -> the cache is reused; the dependency check does not break it.

## Constraints
- Build logic stays configuration-cache safe.
- The CI command stays `./gradlew detekt jvmTest koverVerify :architecture-test:test`.
- The remaining `core` modules keep their roles; only the changes listed in scope are made.
- Versions and SDK levels stay in `gradle/libs.versions.toml`.

## Open questions
- None.

## Change log
- 2026-11-11: Initial version.
- 2026-11-11: Confirmed.
