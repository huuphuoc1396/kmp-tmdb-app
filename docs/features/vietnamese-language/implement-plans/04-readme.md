# Plan: README lists Vietnamese

Requirement: [04 - README lists Vietnamese](../subtasks/04-readme.md)
Status: Done

## Summary
Docs-only: name Vietnamese wherever the READMEs list the supported languages or the per-language cache keys.

## Decisions
- Also update `CLAUDE.md` (project summary, per-language cache line, preview locales) (auto) — it states the same
  facts and would otherwise contradict the code; rejected: leaving it stale.

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| docs | `README.md`, `native-android/README.md`, `CLAUDE.md` (mod) | List Vietnamese |

## Steps
### Step 1: README and CLAUDE.md  (AC-7) ✅
- `README.md`: overview "in English, Brazilian Portuguese or Vietnamese"; cache line `en-US` / `pt-BR` / `vi-VN`.
- `native-android/README.md`: preview locales include Vietnamese. `native-ios/README.md` names no languages.
- `CLAUDE.md`: `en + pt-BR + vi`, cache `pt-BR` / `en-US` / `vi-VN`, previews English, pt-BR, Vietnamese.
- Compile: none (docs).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-7 | 1 | Manual: read `README.md` |
