# Split CI checks into separate steps

Status: Confirmed
Source: chat; follows [Main branch skeleton](../main-skeleton/task.md) (AC-7)

## Goal
CI on `main` currently runs every check (static analysis, JVM tests, coverage, architecture tests) as a single step, so a failure shows only "Run checks" and the reader has to open the log to find what failed. A developer looking at a CI run on GitHub should see at once which check failed, and one push should still report every failure.

## Scope
### In scope
- Each of the four checks appears as its own named step in the existing CI job, in this order: static analysis, JVM tests, coverage verification, architecture tests.
- When one check fails, the remaining checks still run and report their own result.
- The job fails if any check fails.
- Reports are still uploaded when any check fails.

### Out of scope
- Splitting the checks into separate parallel jobs (worth revisiting when `cmp` / `native` add heavy Android and macOS jobs).
- Branch protection or required status checks on `main`.
- The local check command documented in `docs/architecture.md` (stays one command).

## User flow
1. The developer pushes to `main` or opens a pull request against `main`.
2. CI runs the four checks one after another in the same job.
3. On the run page, the developer sees each check as a separate step with its own pass/fail status and duration.
4. If a check fails, the developer opens that step, or downloads the uploaded reports, to see the cause.

## UI states
| State | When | What the user sees / can do |
|---|---|---|
| All passed | Every check passes | Four green steps; job and commit marked as passed |
| Some failed | One or more checks fail | Failed steps marked red, passing steps green; job and commit marked as failed; reports artifact available |
| Cancelled | A newer run replaces this one | Remaining steps do not run; run marked as cancelled |

## Acceptance criteria
- **AC-1** Each check is a named step
  - Given a CI run on `main` or on a pull request against `main`
  - When the developer opens the run on GitHub
  - Then static analysis, JVM tests, coverage verification and architecture tests each appear as a separate step with a descriptive name, in that order
- **AC-2** A failing check does not hide the others
  - Given a change that breaks exactly one check
  - When CI runs
  - Then only that check's step is marked as failed, and every later step still runs and shows its own result
  - Exception: coverage is measured from the unit tests, so when a test fails in a module with a coverage threshold, the coverage step also fails
- **AC-3** Any failure fails the job
  - Given at least one check fails
  - When CI finishes
  - Then the job and the commit status are marked as failed
- **AC-4** Reports are kept on failure
  - Given at least one check fails
  - When CI finishes
  - Then the build reports are available as a downloadable artifact of the run
- **AC-5** No significant slowdown
  - Given all checks pass
  - When CI finishes
  - Then the job takes no more than 30 seconds longer than the single-step version (about 1m52s on the first run)

## Edge cases
- **EC-1** The run is cancelled (for example, replaced by a newer push) -> the remaining steps do not run.
- **EC-2** A step fails for an infrastructure reason (for example, a dependency download error) -> later steps still run and report their own result; the job is marked as failed, never green.
- **EC-3** Setup fails before any check (checkout, Java or Gradle setup) -> none of the check steps run; the job is marked as failed with the setup error.

## Constraints
- Stays on a single Ubuntu job, JVM only, with no repository secrets (fork pull requests must still work).
- Must not change what each check verifies or its thresholds.

## Open questions
None.

## Change log
- 2026-11-10: Initial version.
- 2026-11-10: Confirmed.
- 2026-11-10: AC-2 allows the coverage step to fail with a failing test; added EC-3 (setup failure). Back to Draft for re-approval.
- 2026-11-10: Re-confirmed.
