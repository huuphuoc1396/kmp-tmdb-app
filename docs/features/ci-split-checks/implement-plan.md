# Plan: Split CI checks into separate steps

Requirement: [Split CI checks into separate steps](task.md)
Status: Done

## Summary
Replace the single `Run checks` step in `.github/workflows/ci.yml` with four named steps, one per check: Detekt, JVM tests, Kover and Konsist. Each check step runs when setup succeeded and the run was not cancelled, even if an earlier check failed. This uses GitHub's built-in step status functions instead of `continue-on-error`, so a failed step still fails the job. The Gradle daemon stays alive between steps in the same job, which keeps the extra cost to one configuration per invocation.

## Decisions
- **One job, four steps** — chosen by the user in task.md over parallel jobs.
- **Step condition `if: ${{ !cancelled() && steps.gradle.outcome == 'success' }}`** on each check step.
  - `!cancelled()` replaces the default `success()`, so later checks run after a failed check (AC-2), and nothing runs after a cancel (EC-1).
  - Requiring the `gradle` setup step to succeed skips the checks when setup fails (EC-3). The user chose this over running the checks anyway.
- **No `continue-on-error`**: a failed step must stay red and fail the job (AC-3). `continue-on-error` would turn it green.
- **Coverage step may fail together with a failing test** in `core:domain` / `core:data`, because `koverVerify` re-runs those tests. The user accepted this, and AC-2 now states the exception. Running Kover with test tasks excluded was rejected as fragile.
- **Keep `--continue` in each step**, so a step reports every failing module, not only the first.
- **Step names**:
  - `Static analysis (Detekt + ktlint)`
  - `JVM tests`
  - `Coverage verification (Kover)`
  - `Architecture tests (Konsist)`
- **No change to `docs/architecture.md`**: the CI section lists the same checks, and the local command stays one line (task.md, out of scope).

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Job, triggers, permissions, concurrency, Java/Gradle setup, report upload | Current CI workflow | `.github/workflows/ci.yml` |
| Check commands | Same Gradle tasks as today | `.github/workflows/ci.yml` (`Run checks` step) |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| root | `.github/workflows/ci.yml` (modify) | Four named check steps with run conditions (AC-1 to AC-4, EC-1 to EC-3) |

## Steps

### Step 1: Split the check step  (AC-1, AC-2, AC-3, AC-4, AC-5, EC-1, EC-2, EC-3) ✅
- Files: `.github/workflows/ci.yml` (modify)
- What:
  - Give the Gradle setup step `id: gradle`.
  - Replace `Run checks` with four steps, in this order, each with `if: ${{ !cancelled() && steps.gradle.outcome == 'success' }}`:

    | Name | Command |
    |---|---|
    | `Static analysis (Detekt + ktlint)` | `./gradlew detekt --continue` |
    | `JVM tests` | `./gradlew jvmTest --continue` |
    | `Coverage verification (Kover)` | `./gradlew koverVerify --continue` |
    | `Architecture tests (Konsist)` | `./gradlew :architecture-test:test --continue` |

  - Keep `Upload reports` with `if: failure()`. It runs when any earlier step failed, including setup.
  - Job name: `Checks (JVM)`, because it no longer lists every check.
- Pseudo-code:
  ```yaml
  - id: gradle
    uses: gradle/actions/setup-gradle@v5
    with:
      cache-read-only: ${{ github.event_name == 'pull_request' }}

  - name: Static analysis (Detekt + ktlint)
    if: ${{ !cancelled() && steps.gradle.outcome == 'success' }}
    run: ./gradlew detekt --continue
  # ... three more steps with the same condition
  ```
- Unit tests: none (workflow file).
- Compile: `ruby -ryaml -e 'YAML.load_file(".github/workflows/ci.yml")'` (syntax check only).

## Traceability
| AC / EC | Step(s) | Test(s) / check |
|---|---|---|
| AC-1 | 1 | Manual: after push, the run page shows the four steps with these names, in order |
| AC-2 | 1 | Manual on a throwaway branch with a PR against `main`: introduce a Detekt violation only. The Static analysis step is red, and the JVM tests, Coverage and Architecture steps run and are green. Close the PR and delete the branch |
| AC-3 | 1 | Same manual run as AC-2: the job and the commit status are red |
| AC-4 | 1 | Same manual run as AC-2: a `reports` artifact is attached to the run |
| AC-5 | 1 | Compare job duration of the first green run against 1m52s (limit: 2m22s) |
| EC-1 | 1 | Review: `!cancelled()` on every check step. Optional manual: push twice quickly; the first run is cancelled with skipped steps |
| EC-2 | 1 | Review: no `continue-on-error`; failed step stays red and later steps run under `!cancelled()` |
| EC-3 | 1 | Review: every check step requires `steps.gradle.outcome == 'success'` |

## Out of scope
- Parallel jobs and branch protection (task.md).
- Changing the local check command in `docs/architecture.md`.

## Risks
- **Each step configures Gradle again**, and the configuration cache is not restored on a fresh runner without an encryption key. AC-5 could be missed if configuration is slow. Mitigation: the daemon is reused within the job, and configuration took a few seconds locally. Measure on the first run, and report back if the run exceeds 2m22s.
- **The AC-2 manual check needs a pull request** from a throwaway branch. That PR is public on the repository. Mitigation: `forge` asks the user before opening it, and closes it and deletes the branch afterwards.

## Verification
- `main` run [42774391418](https://github.com/huuphuoc1396/kmp-tmdb-app/actions/runs/42774391418): four named steps in order, all green; job 54s (AC-1, AC-5).
- Temporary PR huuphuoc1396/kmp-tmdb-app#1 with an unused import, run [42774421735](https://github.com/huuphuoc1396/kmp-tmdb-app/actions/runs/42774421735): Static analysis failed; JVM tests, Coverage and Architecture tests still ran and passed; job and PR check failed; `reports` artifact uploaded (AC-2, AC-3, AC-4). PR closed and branch deleted.
- EC-1, EC-2, EC-3: verified by review of the step conditions (no cancelled or failed-setup run was triggered).
