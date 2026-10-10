# AGENTS.md — bcadigital

> Instructions for AI coding agents working in this repo. Follow these rules for every task.

## Project Overview
- Project: `bcadigital`
- Stack: **Java 17 (LTS, Corretto) + Maven 3.9.x**. Standard Maven layout (`src/main/java`, `src/test/java`).
- Build/test: `mvn verify`, fast feedback: `mvn -q -DskipTests package` / `mvn -q test -Dtest=...`.
- Source of truth for agent behavior is this file. If repo docs conflict, this file wins.

## Working Conventions
1. **Talk first before implementing (REQUIRED).** Every time you want to implement something (feature, refactor, fix, structure change), present the why + proposed structure/plan to the user FIRST and wait for approval. Let the user grill the reasoning. Do NOT write/edit code until they say go. Exception: trivial doc/comment fixes in the file you were explicitly asked to touch.
2. Keep changes small and focused — one unit of work per task.
2. Prefer reading existing files (`read`) over guessing. Check for related code/tests before adding new things.
3. Use `edit` for targeted changes, `write` only for new files or full rewrites.
4. Use `bash` for `ls`, `rg`, `find`, running tests/linters — do not `cat`/`sed` to view files.
5. Don't commit secrets, `.env` contents, or large generated artifacts. Respect `.gitignore`.
6. Update docs/tests alongside code when behavior changes.

## Definition of Done (per unit of work)
- Code change is complete and self-consistent.
- `mvn verify` passes (or `mvn test` + `mvn -q package -DskipTests` if full verify is too heavy). New behavior has JUnit 5 tests.
- No `System.out.println`, leftover TODOs, or commented-out code unless intentional and noted.
- Public APIs have Javadoc describing contract, not implementation.
- Docs updated if the change affects usage.

## Java + Maven Conventions
- Java 17, no wildcard imports, `final` where sensible, prefer immutability (`List.of`, records where fitting).
- Maven layout only: production in `src/main/java/...`, tests in `src/test/java/...` mirroring packages. One public top-level class per file.
- Naming: packages lowercase, classes `PascalCase`, methods/vars `camelCase`, constants `UPPER_SNAKE`, tests `*Test` with `shouldX_whenY` method names.
- Exceptions: prefer unchecked domain exceptions at boundaries; never swallow (`catch` must handle/log/rethrow). No `throws Exception` leakage.
- Tests: JUnit 5 + AssertJ/Mockito when available. Each fix gets a regression test; each feat gets happy-path + edge-case tests.

## Design Philosophy — John Ousterhout, *A Philosophy of Software Design* (REQUIRED)

> Goal: minimize complexity (cognitive load). Complexity = change amplification + cognitive load + unknown unknowns. Every decision must reduce it.

### 1. Deep modules over shallow modules (core rule)
- A module is **deep** if its interface is small relative to the work it does. **Deep = good. Shallow = bad.**
- Shallow module smell: many thin wrappers / pass-through methods that add little value but enlarge the interface (e.g. `UserService.createUser(name, email)` that only calls `userRepo.save(new User(name, email))`).
- Rules:
  - Prefer a few powerful methods over many trivial getters/setters/delegates. If a class is mostly getters/setters, it is too shallow — push behavior into it or merge it away.
  - Let a method do a whole job, not half a job. Caller should not need 5 calls + orchestration logic that the module could hide. Example: prefer `orderService.checkout(cartId)` over caller doing `validate() + price() + discount() + save() + notify()`.
  - Small interface ≠ few methods only — also few parameters, simple types, no temporal coupling (no `init()` then `run()` if one call suffices).
- When reviewing: ask “what complexity does this interface hide from me?” If answer is “almost none”, make it deeper or inline it.

### 2. Information hiding & leakage
- Hide design decisions that are likely to change (data structures, parsing, retry policy, SQL details) behind the module that owns them.
- Leakage smells: exposing internals in signatures (`Map<String, List<String[]>>`), temporal decomposition (`parse()` + `process()` forcing caller order), excessive config flags leaking implementation choices upward.
- Prefer: opaque domain types + intention-revealing methods. Push defaults and policies **downward** into the module, don’t force every caller to pass them.

### 3. Define errors out of existence
- Best error handling is code where the error case cannot happen or needs no handling: sensible defaults, do-the-right-thing, idempotent operations.
- Prefer: `getOrDefault`, `Optional`/empty-list returns over null/exception for normal absence; mask ignorable low-level failures inside deep modules.
- Only surface exceptions the caller can meaningfully act on. Don’t propagate `SQLException`/`IOException` raw — translate to domain exceptions at the boundary.

### 4. Pull complexity downward
- Never make the caller do work the callee could do. If 10 callers repeat the same 5-line preamble, that logic belongs inside the called method.
- Special-case generously inside implementation so the common caller path stays trivial.

### 5. Comments & intent (Ousterhout Ch. 8)
- Comment the **why and contract**, not the what. Every public class/method needs Javadoc: what it does, preconditions, postconditions, error behavior — things not obvious from the signature.
- Keep comments adjacent to code, update them in the same commit. Delete misleading comments rather than leaving them.
- If a non-obvious hack/workaround is needed, comment must explain why and when it can be removed.

### 6. Other Ousterhout rules to enforce
- **Working code isn’t enough**: strategic programming — invest a little extra to keep design clean vs. tactical quick-fix that adds complexity.
- **General-purpose over special-purpose** (within reason): implement the general mechanism once rather than N special cases.
- **Things should be obvious**: names, control flow, and defaults must make the common case self-evident. Avoid clever code.
- **Fail the complexity test early**: if a design feels hard to explain or needs many words, simplify before coding.

### Java quick checklist (apply before every commit)
- [ ] New class: deep enough? Could it be merged, or should it absorb caller logic?
- [ ] New public method: hides complexity? Minimal params? No temporal coupling?
- [ ] No leakage: no internal collections/DTOs/SQL in signatures unless intentional?
- [ ] Errors defined out where possible; remaining errors translated to domain meaning?
- [ ] Javadoc on public API explains contract + edge cases?

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
