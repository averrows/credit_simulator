# AGENTS.md — bcadigital

> Instructions for AI coding agents working in this repo. Follow these rules for every task.

## Project Overview
- Project: `bcadigital`
- Current state: greenfield / early setup. Update this section as the stack is decided (language, framework, monorepo vs single package, etc.).
- Source of truth for agent behavior is this file. If repo docs conflict, this file wins.

## Working Conventions
1. Keep changes small and focused — one unit of work per task.
2. Prefer reading existing files (`read`) over guessing. Check for related code/tests before adding new things.
3. Use `edit` for targeted changes, `write` only for new files or full rewrites.
4. Use `bash` for `ls`, `rg`, `find`, running tests/linters — do not `cat`/`sed` to view files.
5. Don't commit secrets, `.env` contents, or large generated artifacts. Respect `.gitignore`.
6. Update docs/tests alongside code when behavior changes.

## Definition of Done (per unit of work)
- Code change is complete and self-consistent.
- Tests/lint/typecheck pass if they exist in the repo (`npm test`, `npm run lint`, `tsc --noEmit`, or equivalent).
- No leftover debug logs, TODOs, or commented-out code unless intentional and noted.
- Docs updated if the change affects usage.

## Git Workflow — REQUIRED

**Rule: After finishing a unit of work, you MUST commit.**

- One logical unit = one commit. Don't batch unrelated changes.
- Don't wait to be asked. Don't leave changes uncommitted at the end of a task.
- Verify with `git status` / `git diff --stat` before committing.
- Only commit when working tree state is intentional (no scratch files).

### Commit Message Format (Conventional Commits)

```
<type>: <short imperative description>
```

- Keep subject ≤ 72 chars, lowercase type, no trailing period.
- Examples: `feat: add user login api`, `fix: handle null token on refresh`, `test: cover checkout edge cases`.

Allowed types:

| Type | Use for |
|------|---------|
| `feat` | New feature / capability |
| `fix` | Bug fix |
| `test` | Tests only (add/update) |
| `refactor` | Code restructuring, no behavior change |
| `docs` | Documentation only (including this file) |
| `chore` | Tooling, config, deps, repo maintenance |
| `style` | Formatting, no logic change |
| `perf` | Performance improvement |

- For larger commits, add a blank line + bullet body explaining what/why:
  ```
  feat: add jwt auth middleware

  - validate bearer token on protected routes
  - return 401 on expired token
  ```
- Scope is optional but allowed: `feat(auth): add refresh token flow`.
- Breaking changes: append `!` → `feat!: drop node 16 support` and explain in body.

### Commit Commands
```bash
git status --short
git diff --stat
git add <intentional files>   # never `git add -A` blindly with unrelated junk
git commit -m "<type>: <description>"
```
