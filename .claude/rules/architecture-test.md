---
paths:
  - "architecture-test/**"
---

# Architecture tests (Konsist)

## Responsibility
- Enforces the import and package rules from `docs/architecture.md` sections 5.3 and 9 by reading Kotlin source files with Konsist. Allowed module dependencies (`project(...)`) are checked by `tmdb.module` in `build-logic`, not here.
- A plain Kotlin/JVM module (`kotlin.jvm` + `tmdb.detekt`), JUnit Jupiter via the JUnit BOM, `useJUnitPlatform()`. It depends on no project module.
- `architecture-test/build.gradle.kts` declares `core/`, `service/` and `feature/` `**/*.kt` as test inputs so Gradle re-runs the tests when sources change. Any new directory a rule checks (for example `apps/` once an app exists) must be added to those inputs, or the task stays up to date and misses violations.

## Pattern
- One test class per rule group: `DependencyRulesTest` (imports between layers), `PackageRulesTest` (package names).
- Shared helpers live in `ModulePaths.kt`: `ROOT_PACKAGE`, `KoFileDeclaration.modulePath()` (`core/<m>`, `service/<ctx>/<type>`, `feature/<x>/<type>` from the file path), `packageOf(modulePath)`, `KoScope.filesMatching(regex)` (by module path), `importsAnyOf(prefixes)`, `importsMatching(regex)`.
- Scope: `Konsist.scopeFromProduction()` for dependency rules (tests may import anything); `Konsist.scopeFromProject()` when tests must follow the rule too (package rule).
- Assert with `assertFalse` / `assertTrue(additionalMessage = "Rule: <same text as the test name>") { ... }`.
- Test names are backtick sentences stating the rule: `` `domain imports no data, fake or infrastructure` ``.
- Konsist assertions are non-strict: an empty file list passes, so rules for `service/` and `feature/` hold before those modules exist.
- References: `architecture-test/src/test/kotlin/com/banhmilabs/tmdb/architecture/DependencyRulesTest.kt`, `.../ModulePaths.kt`.

## Adding a rule
1. Pick the class by rule group (or add a new `<Group>RulesTest`), and the scope (production vs project).
2. Select files with the `ModulePaths.kt` helpers; add a new `internal` helper there only if two tests need it.
3. Write the assertion with `additionalMessage = "Rule: ..."`.
4. If the rule comes from the architecture doc, keep `docs/architecture.md` sections 5.3 and 9 in sync.
5. Check it catches a violation: temporarily add a breaking import (or a file under a not-yet-existing module path, e.g. `service/x/domain/src/commonMain/kotlin/...`), run the test, see it fail, revert.

## Adding a module
- Nothing to do: `PackageRulesTest` covers every file under a recognised module path, and the rules select files by path pattern. Only a new module *type* needs its pattern added to the rules that apply to it.

## Current rules
- Packages: every file uses the package of its module path (`service/title/domain` → `com.banhmilabs.tmdb.service.title.domain`).
- No frameworks in `core/model`, `service/*/domain`, `feature/*/nav` (`feature/*/api` may use the Compose runtime).
- `service/*/domain` imports no `service.*.data|fake` and no `core.network|database|datastore|security|di`.
- `core/model` imports nothing else in the app; `core/network|database|datastore` import no `service.*` and no `core.di`.
- `feature/*/ui` imports no other feature's `ui` and no `service.*.data`.

## Do / Don't
- Don't add project dependencies to this module; Konsist reads sources by path.
- Don't relax a rule to make a change pass; fix the import, or change the rule in `docs/architecture.md` first with the user's agreement.

## Testing
- `./gradlew :architecture-test:test`; failure messages start with `Rule:`.
