# 01 - TMDB network access

Feature: [task.md](../task.md)
Depends on: none
Status: Done

## Goal
The shared core can call the TMDB API v3 securely: requests carry the TMDB token from local secrets (never from
committed files) and the requested language, and failures come back as the app's standard errors. Nothing changes
for the user yet; this is the foundation for the Home lists.

## Scope
### In scope
- A TMDB client in the shared core, usable by any context, with no endpoints of its own.
- The TMDB token read from the git-ignored `secrets.properties` (and from CI secrets), never from source or committed
  files. A missing token fails clearly at build or start-up for developers, not silently.
- Every request can carry `language` (`pt-BR` / `en-US`).
- Failures map to `AppError` (Network, Unauthorized, NotFound, Unknown).
- No request or response bodies logged in release builds.
### Out of scope (handled later)
- Home endpoints and caching -> 03-home-title-lists
- Certificate pinning (not required by this feature)

## Acceptance criteria
- **AC-10** Language (partial: only that requests can carry pt-BR / en-US; choosing the language and the copy are in 03-home-title-lists and 04-cmp-home-happy-path)
  - Given the device language is pt-BR
  - When Home is shown
  - Then the screen copy is Portuguese and title data (names, posters) is requested in pt-BR
  - And given any other device language, then the copy is English ("What do you want to watch today?",
    "Search", "Most popular") and title data is requested in en-US

## Edge cases
None from task.md in this subtask.

## Constraints (from task.md)
- The TMDB token is never in source or committed files.

## Layers touched
- Network (shared HTTP client, auth, error mapping)
- Build configuration (secrets)

## How to test
1. Without `secrets.properties`, build the app: the missing token is reported clearly.
2. Add a valid token and run the shared-core tests: a request against a mocked TMDB response succeeds and sends the
   bearer token and `language`.
3. Mocked 401, 404, timeout and no-network responses come back as Unauthorized, NotFound and Network errors.
4. `git status` shows no token in tracked files.

## Change log
- 2026-11-17: Initial version.
