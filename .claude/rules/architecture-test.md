---
paths:
  - "architecture-test/**"
---

# Architecture tests (Konsist)

## Responsibility
- Enforces the dependency and package rules from `docs/architecture.md` section 4.4 by reading Kotlin source files with Konsist.
- A plain Kotlin/JVM module (`kotlin.jvm` + `tmdb.detekt`), JUnit Jupiter via the JUnit BOM, `useJUnitPlatform()`. It depends on no project module.
- `architecture-test/build.gradle.kts` declares `core/**/*.kt` as test inputs so Gradle re-runs the tests when sources change. Any new directory a rule checks (for example `feature/` or `android/` on `cmp` / `native`) must be added to those inputs, or the task stays up to date and misses violations.

## Pattern
- One test class per rule group: `DependencyRulesTest` (imports between layers), `PackageRulesTest` (package names).
- Shared helpers live in `CoreModule.kt`: `ROOT_PACKAGE`, `corePackage(module)`, `KoScope.filesInCoreModule(module)` (matches `/core/<module>/src/` in the path), `KoFileDeclaration.importsAnyOf(prefixes)`.
- Scope: `Konsist.scopeFromProduction()` for dependency rules (tests may import anything); `Konsist.scopeFromProject()` when tests must follow the rule too (package rule).
- Assert with `assertFalse` / `assertTrue(additionalMessage = "Rule: <same text as the test name>") { ... }`.
- Test names are backtick sentences stating the rule: `` `domain does not depend on implementation layers` ``.
- References: `architecture-test/src/test/kotlin/com/banhmilabs/tmdb/architecture/DependencyRulesTest.kt`, `.../CoreModule.kt`.

## Adding a rule
1. Pick the class by rule group (or add a new `<Group>RulesTest`), and the scope (production vs project).
2. Select files with the `CoreModule.kt` helpers; add a new `internal` helper there only if two tests need it.
3. Write the assertion with `additionalMessage = "Rule: ..."`.
4. If the rule comes from the architecture doc, keep `docs/architecture.md` section 4.4 in sync.
5. Check it catches a violation: temporarily add a breaking import in a core file, run the test, see it fail, revert.

## Adding a core module
- Add the module name to the list in `PackageRulesTest`, and to the layer lists in `DependencyRulesTest` where the module belongs (for example the `layers` list that `domain` must not import).

## Do / Don't
- Don't add project dependencies to this module; Konsist reads sources by path.
- Don't relax a rule to make a change pass; fix the import, or change the rule in `docs/architecture.md` first with the user's agreement.

## Testing
- `./gradlew :architecture-test:test`; failure messages start with `Rule:`.
