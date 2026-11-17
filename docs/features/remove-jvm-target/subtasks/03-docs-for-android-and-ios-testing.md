# 03 - Docs for Android and iOS testing

Feature: [task.md](../task.md)
Depends on: 02-remove-jvm-target
Status: Todo

## Goal
The architecture doc, the project guide and the rules describe the Android and iOS targets, where each kind of
test runs, and the commands to run them, with no mention of a JVM target or JVM unit tests for shared modules.

## Scope
### In scope
- Architecture doc: targets, testing strategy, enforcement, CI and publishing sections.
- Project guide: commands, "before finishing a code change", conventions and do-nots about tests.
- Rules for build logic, tests and architecture tests.
### Out of scope (handled later)
- None.

## Acceptance criteria
- **AC-7** Docs describe the new setup
  - Given the architecture doc, the project guide and the rules
  - Then they describe the Android and iOS targets, where each kind of test runs, and the commands to run them
  - And they no longer mention a JVM target or JVM unit tests for shared modules

## Edge cases
None from task.md; EC-1–EC-3 are handled in 01 / 02.

## Layers touched
- Docs (architecture, project guide, rules)

## How to test
1. Search the docs and rules for the JVM target and JVM unit tests: only build-logic and the architecture tests
   (JVM projects) are mentioned (AC-7).
2. Follow the documented commands: the unit test, coverage, CI and platform test commands run as described
   (AC-7).

## Change log
- 2026-11-17: Initial version.
