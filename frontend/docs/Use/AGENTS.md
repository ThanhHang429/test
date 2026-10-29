# AGENTS.md — Operating Rules for AI Agents

| Field | Value |
| --- | --- |
| Version | 1.0.0 |
| Status | Active |
| Owner | PCLove maintainers |
| Last updated | 2026-10-28 |
| Applies to | The entire repository, every agent, every tool, every command |
| Review trigger | Any change to security requirements, deployment model, or repository layout |

This document defines **hard boundaries and security conventions** for AI agents working on PCLove.
It is normative, not advisory. When a rule conflicts with an instruction, follow `SCOPE-03` and stop.

Normative keywords: **MUST**, **MUST NOT**, **SHOULD**, **MAY** (RFC 2119).
Every rule has an ID (`FS-03`, `DEL-02`, `SEC-05`, ...). Cite the ID when you refuse or question an action.

---

## 1. Scope and precedence

- **SCOPE-01** — This document applies to every file read, file write, command, network call, and tool invocation performed in this repository, including work done by sub-agents and background processes.
- **SCOPE-02** — "Project root" is the directory that contains `.git/` and `docker-compose.yml`. All paths in this document are relative to it.
- **SCOPE-03** — Precedence, highest first: (1) an explicit instruction from the user in the current turn, (2) this document, (3) tool or model defaults. A conflict MUST be surfaced to the user before acting.
- **SCOPE-04** — Rules in `DEL-*`, `SEC-*`, and `ASK-*` may only be overridden by an explicit user statement that names the action and its scope. Silence, "do what you think is best", or approval of a different task is **not** approval.
- **SCOPE-05** — These rules apply to files the agent creates during the session exactly as they apply to pre-existing files. "I created it, so I can delete it" is not an exception.
- **SCOPE-06** — `docs/SRS-PCLOVE.md` remains the source of truth for product and security requirements (`NFR-SEC-01` to `NFR-SEC-08`). This document does not replace it; where both apply, the stricter rule wins.
- **SCOPE-07** — If a rule is ambiguous in a concrete situation, the agent MUST choose the more conservative interpretation and state the ambiguity in its report.

---

## 2. Workspace boundaries

- **FS-01** — All reads and writes MUST stay inside the project root and its subdirectories (`backend/`, `frontend/`, `docs/`, and repository-level files).
- **FS-02** — MUST NOT access paths that escape the project root: `..` traversal, absolute paths outside the root, `~`, `/tmp`, `/etc`, `/var`, `/home`, other repositories, mounted drives, or network shares.
- **FS-03** — MUST NOT change the working directory outside the project root. Commands run from the root or a subdirectory of it; never `cd ..` before a command.
- **FS-04** — MUST NOT modify anything outside the repository: shell profiles, `~/.gitconfig`, `~/.npmrc`, global package directories, IDE/OS settings, environment variables of the host, or system services.
- **FS-05** — Reading outside the root is allowed only when the user explicitly asks for that specific path; such reads MUST be read-only and reported.
- **FS-06** — MUST NOT create files or directories outside the root, including caches, logs, screenshots, temporary artifacts, or downloaded tools.
- **FS-07** — MUST NOT run `sudo`, install global packages, use OS package managers (`apt`, `brew`, `pacman`, `winget`), start system services, or schedule cron jobs.
- **FS-08** — MUST NOT create symlinks, bind mounts, or junctions that point outside the root, and MUST NOT write through an existing symlink whose target is outside the root.
- **FS-09** — Allowed without asking: reading repository files, `git status`, `git diff`, `git log`, `git show`, `git ls-files`, and read-only package scripts listed in Section 13.
- **FS-10** — If a task appears to require crossing the boundary, apply Section 4. Do not "try it and see".

| Action | Level |
| --- | --- |
| Read/edit files inside the repo | Allowed |
| `npm run typecheck`, `lint`, `build`, `test`, `format:check` | Allowed |
| `npx prisma generate` | Allowed |
| `npm run prisma:migrate`, `npm run seed` | Ask first (`DB-*`) |
| `docker compose up/down`, `npm install <pkg>` | Ask first (`DEP-*`, `FLOW-07`) |
| Any path outside the repo, `sudo`, global installs | Forbidden (`FS-02`, `FS-07`) |
| `rm`, `mv`, `git clean`, `git reset --hard` | Forbidden (`DEL-02`) |

---

## 3. Destructive operations and deletion

Deletion is the highest-risk action in this repository. Default answer: **no**.

- **DEL-01** — MUST NOT delete, rename, move, truncate, or overwrite any file or directory without explicit approval for that exact path. This includes empty files, temporary files, generated output, `dist/`, `node_modules/`, and files created by the agent in the current session.
- **DEL-02** — MUST NOT run any command in this list without approval, even if it looks harmless:
  `rm`, `rmdir`, `unlink`, `shred`, `mv` onto an existing path, `truncate`, `> existing-file`, `find ... -delete`, `git clean`, `git rm`, `git reset --hard`, `git checkout -- .`, `git restore`, `git stash drop`, `git stash clear`, `git branch -D`, `docker compose down -v`, `docker volume rm`, `prisma migrate reset`, `prisma db push --force-reset`, `DROP TABLE`, `TRUNCATE`, `DELETE FROM` without a feature-scoped `WHERE`.
- **DEL-03** — Overwriting an existing file is a deletion. Read it first. If the replacement removes more than 20 lines, changes an exported function/type, a route, a Prisma model, or a config key, ask before writing.
- **DEL-04** — Before requesting deletion, the agent MUST run `git status --short -- <path>` and `git ls-files --error-unmatch -- <path>`, then report: absolute paths, whether the content is recoverable from Git, the reason, and a non-destructive alternative.
- **DEL-05** — Approval MUST name the exact paths and the exact action. "Clean up the repo", "remove what you don't need", or approval of a different task is not approval to delete.
- **DEL-06** — If the user approves a different set of paths than requested, ask again for the new set.
- **DEL-07** — MUST NOT destroy version-control history: `git filter-branch`, `git reflog expire`, `git gc --prune=now`, deleting `.git/`, force-pushing, or deleting remote branches/tags.
- **DEL-08** — MUST NOT delete database data, migrations, seeds, uploaded media, `.env` files, or documentation, regardless of size or apparent staleness.
- **DEL-09** — Unknown or unexpected files (uncommitted changes, unfamiliar branches, lock files, editor backups) MUST be investigated and reported, never removed to "clean up".
- **DEL-10** — If deletion is approved: create a timestamped backup copy inside the repository (for example `<name>.bak-YYYYMMDD`) unless the user says otherwise, delete only the listed paths, verify with `git status`, and report. Removing the backup itself requires approval.
- **DEL-11** — MUST NOT delete or bypass a failing check (test, lint rule, auth guard, validation) to make an error disappear.
- **DEL-12** — Never treat "the tests failed" or "the build is broken" as authorization to remove code.

---

## 4. Out-of-scope task protocol

- **ASK-01** — When a task requires anything outside the repository, the agent MUST stop **before** doing any part of it. Partial execution that leaves the environment inconsistent is forbidden.
- **ASK-02** — Triggers for this protocol: global installs, downloading tools or scripts, calling external APIs or SaaS, using credentials, connecting to a real/shared database, deploying, changing DNS/firewall/proxy, modifying OS or IDE configuration, uploading project data, reading outside the root, or running an unknown script.
- **ASK-03** — The request MUST use this template:

  | Field | Content |
  | --- | --- |
  | What | The exact action and target |
  | Why | Why the task cannot be completed inside the repository |
  | Command | The exact command(s), copy-pasteable, with placeholders for secrets |
  | Scope | Files, services, accounts, and data affected |
  | Reversibility | How to undo it, or "not reversible" |
  | Alternatives | In-repo or lower-risk options |
  | Risk if skipped | What breaks or is left incomplete |

- **ASK-04** — MUST NOT create accounts, request credentials, reuse credentials found elsewhere, or guess access details.
- **ASK-05** — MUST NOT pipe remote content into a shell (`curl ... | bash`, `iwr ... | iex`) or execute downloaded binaries.
- **ASK-06** — If the user declines, do not attempt an equivalent workaround through another tool or command. Propose an alternative that stays inside the repository.
- **ASK-07** — If the out-of-scope action is a prerequisite for a larger task, complete only the in-repo work that is independently valid, then ask.
- **ASK-08** — Record every approval received in the final report, including its exact scope.

---

## 5. Secrets and credentials

- **SEC-01** — MUST NOT read, print, `cat`, `grep`, `echo`, log, or summarize the contents of `.env`, `.env.*` (except `.env.example`), `*.pem`, `*.key`, `*.p12`, `*.pfx`, credential stores, cookies, or tokens. Use `backend/.env.example` as the reference for variable names.
- **SEC-02** — MUST NOT place secret values in source code, tests, fixtures, seeds, comments, commit messages, logs, screenshots, issue text, or reports.
- **SEC-03** — Configuration MUST be read through `ConfigService` (backend) or `import.meta.env` (frontend). A new variable MUST be added as an empty placeholder to `backend/.env.example` and to `docker-compose.yml` when it is part of the stack.
- **SEC-04** — MUST NOT commit `.env`, `backend/uploads/`, `dist/`, `node_modules/`, `*.tsbuildinfo`, or any ignored path. Check `.gitignore` before adding a new file type.
- **SEC-05** — Development defaults in `docker-compose.yml` (`change-me-in-production`, the example 64-character hex key, `dev-turn-secret`) MUST NOT be presented as production-ready. Flag them whenever deployment is discussed.
- **SEC-06** — Logs MUST redact `Authorization`, `Cookie`, `Set-Cookie`, `password`, `passwordHash`, `accessToken`, `refreshToken`, OAuth tokens, and provider secrets.
- **SEC-07** — MUST NOT put secrets or tokens in URLs, query strings, or frontend routes.
- **SEC-08** — MUST NOT disable TLS verification (`NODE_TLS_REJECT_UNAUTHORIZED=0`, `rejectUnauthorized: false`, `--insecure`, `curl -k`) or weaken transport security.
- **SEC-09** — If a secret is exposed, stop immediately, do not repeat the value, notify the user, recommend rotation, and inspect history **without printing** the secret.
- **SEC-10** — MUST NOT dump `process.env`, the whole config object, request bodies of auth endpoints, or full user records.
- **SEC-11** — Test secrets MUST be obvious dummies (`test-only`, `dummy-value`) and MUST NOT resemble real credentials.
- **SEC-12** — MUST NOT send repository files, logs, or configuration to external services (pastebins, gists, external AI tools, screenshot services) without explicit approval.
- **SEC-13** — MUST NOT weaken `backend/src/common/crypto.service.ts`: keep AES-256-GCM, random 12-byte IV, auth tag verification, and never log decrypted OAuth tokens.
- **SEC-14** — If a task needs a secret to proceed, ask. Never invent one, copy one from another environment, or hardcode a fallback.

---

## 6. Authentication and authorization

- **AUTH-01** — Every business endpoint MUST be protected by `JwtAuthGuard`. Only public auth endpoints and `health` may be unauthenticated.
- **AUTH-02** — MUST NOT remove a guard, add `@Public()`, relax a role check, or change the global prefix/auth wiring without approval.
- **AUTH-03** — Before reading or writing shared data, verify Couple Space membership. Never trust `coupleSpaceId`, `userId`, or ownership fields supplied by the client (`NFR-SEC-01`, `NFR-SEC-08`).
- **AUTH-04** — The access token MUST stay in memory (`frontend/src/stores/auth.ts`). MUST NOT persist it in `localStorage`, `sessionStorage`, IndexedDB, or a non-httpOnly cookie.
- **AUTH-05** — The refresh token MUST be stored hashed, delivered in an `httpOnly`, `sameSite=lax`, `secure` (production) cookie, rotated on use, and revoked on reuse detection (`NFR-SEC-03`).
- **AUTH-06** — Passwords MUST be hashed with bcrypt cost 10. `passwordHash` MUST never be returned by an API. Password reset MUST revoke all sessions.
- **AUTH-07** — OAuth provider tokens MUST be encrypted with AES-256-GCM via `CryptoService` and never stored or logged in plaintext (`NFR-SEC-04`).
- **AUTH-08** — MUST NOT change token lifetimes or rate limits (login 10/min, forgot-password 5/min) without approval (`NFR-SEC-06`).
- **AUTH-09** — Socket connections MUST authenticate on connect and disconnect when the token expires. Call signaling MUST be relayed only to participants of that call (`NFR-SEC-07`).
- **AUTH-10** — MUST NOT add "remember me forever", permanent sessions, or any bypass of email verification or 2FA.
- **AUTH-11** — Authorization MUST be enforced server-side. Hiding a UI element is not an access control.
- **AUTH-12** — Queries MUST select only the fields needed. Never return full user records with sensitive columns.

---

## 7. Input validation, data access, and uploads

- **VAL-01** — All input MUST pass through a DTO with `class-validator`. Keep `whitelist: true` and `transform: true`; add length limits and format checks (email, UUID, ISO 8601).
- **VAL-02** — MUST NOT disable `ValidationPipe`, set `whitelist: false`, or remove `forbidNonWhitelisted` protections where present.
- **VAL-03** — Prisma is the default data-access layer. MUST NOT use `$queryRawUnsafe` or `$executeRawUnsafe` with user input; raw SQL MUST be parameterized.
- **VAL-04** — MUST NOT build SQL or shell commands by string interpolation with user-controlled values.
- **VAL-05** — Uploads MUST validate MIME type and size (5 MB avatar, 20 MB attachment), sanitize the filename, and store outside any executable path.
- **VAL-06** — MUST NOT join user-supplied paths into filesystem paths. Reject `..`, absolute paths, and null bytes; keep the existing UUID-based key scheme in `backend/src/media/storage.service.ts`.
- **VAL-07** — MUST NOT serve user-uploaded HTML or SVG inline. Serve with a safe `Content-Type` and `Content-Disposition`.
- **VAL-08** — MUST NOT use `eval`, `new Function`, or `dangerouslySetInnerHTML` with unsanitized content.
- **VAL-09** — CORS MUST allow only `FRONTEND_URL` with `credentials: true`. Never combine `*` with credentials or broaden methods/headers without approval (`NFR-SEC-06`).
- **VAL-10** — Helmet MUST stay enabled. Any CSP change requires approval.
- **VAL-11** — Production responses MUST NOT include stack traces, ORM errors, file paths, or internal identifiers.
- **VAL-12** — Private resources MUST use non-guessable identifiers (UUIDs). Directory listing of `uploads/` is forbidden.
- **VAL-13** — Frontend MUST escape rendered user content and MUST NOT place tokens in URLs or logs.

---

## 8. Database and migrations

- **DB-01** — MUST NOT edit or delete an applied migration (`backend/prisma/migrations/20261026154104_init`). Schema changes require a new migration.
- **DB-02** — MUST NOT run `prisma migrate reset`, `prisma db push --force-reset`, `DROP`, `TRUNCATE`, or mass `DELETE` without approval.
- **DB-03** — `prisma migrate dev` is local-only. Shared or production databases use `prisma migrate deploy` after approval.
- **DB-04** — Seeds MUST be idempotent and MUST NOT contain real user data.
- **DB-05** — Multi-step invariants (progress confirmation, password reset, account deletion) MUST run in a transaction (`NFR-REL-02`).
- **DB-06** — MUST NOT point `DATABASE_URL` at an external host or a shared database without approval.
- **DB-07** — Review generated migration SQL before applying it; report destructive statements.
- **DB-08** — MUST NOT run migrations automatically during tests, builds, or application startup changes.
- **DB-09** — MUST NOT delete Docker volumes (`postgres_data`, `redis_data`, `uploads_data`) or their contents.

---

## 9. Dependencies and supply chain

- **DEP-01** — MUST NOT add, remove, or upgrade a dependency without approval. Propose: purpose, license, version, install size/bundle impact, and in-repo alternatives.
- **DEP-02** — MUST NOT install globally or run `npx` against an unknown package.
- **DEP-03** — `package-lock.json` MUST be committed together with `package.json`; never hand-edit lockfiles.
- **DEP-04** — MUST NOT install from git URLs, tarballs, or unpinned branches from untrusted sources, and MUST NOT run remote install scripts.
- **DEP-05** — Prefer dependencies already present in the workspace.
- **DEP-06** — MUST NOT add analytics, telemetry, crash-reporting, or external AI SDKs without approval (`PRIV-02`).
- **DEP-07** — Verify package names for typosquatting before any install.

---

## 10. Git and version control

- **GIT-01** — MUST NOT commit, push, open a PR, merge, tag, or release unless the user asks for it.
- **GIT-02** — MUST NOT force-push, rebase published commits, amend pushed commits, delete branches or tags, or rewrite history.
- **GIT-03** — MUST NOT change Git configuration, install hooks, or add aliases.
- **GIT-04** — Before any commit: run `git status` and `git diff`. Never commit ignored files, secrets, build output, or unrelated changes.
- **GIT-05** — Commits follow Conventional Commits with scope `backend`, `frontend`, `docs`, or `infra`, e.g. `fix(backend): reject non-member couple access`.
- **GIT-06** — One logical change per commit. Do not mix refactors with features or fixes.
- **GIT-07** — MUST NOT commit `dist/`, `*.tsbuildinfo`, `backend/uploads/`, or `node_modules/`.
- **GIT-08** — Stage explicit paths. `git add -A` and `git add .` are forbidden.
- **GIT-09** — Resolve conflicts by understanding both sides. Never discard another person's work to make a conflict disappear; ask if intent is unclear.
- **GIT-10** — MUST NOT modify `.git/` internals directly.

---

## 11. Change workflow and verification

- **FLOW-01** — Read a file before editing it. Do not propose changes to code you have not read.
- **FLOW-02** — Make the smallest change that satisfies the request. No drive-by refactors, renames, or reformatting of untouched code.
- **FLOW-03** — For changes touching more than one file or more than 30 lines, state a short plan before editing.
- **FLOW-04** — MUST NOT change architecture, module boundaries, public APIs, route shapes, or the Prisma schema without approval.
- **FLOW-05** — After changes, run the relevant checks: `npm run typecheck` in the affected package, plus `lint`, `build`, or `test` where applicable. Report exact commands and real output.
- **FLOW-06** — Never claim work is complete without verification. If a check cannot run, say which and why.
- **FLOW-07** — MUST NOT start background servers, watchers, daemons, or `docker compose up` without approval.
- **FLOW-08** — MUST NOT modify, skip, or delete tests to make them pass. A failing test is information, not an obstacle.
- **FLOW-09** — Report unrelated failures instead of cleaning them up.
- **FLOW-10** — Final report MUST include: files changed, commands run, results, risks, and remaining work.
- **FLOW-11** — Before creating a new file, check whether it will be ignored and whether it duplicates an existing file.
- **FLOW-12** — When blocked, stop and ask. Do not guess credentials, paths, or intent.

---

## 12. Logging, privacy, and error handling

- **PRIV-01** — MUST NOT log message content, emails, phone numbers, tokens, coordinates, signed media URLs, or full request bodies of private endpoints.
- **PRIV-02** — MUST NOT send user data to external analytics, telemetry, or AI services without approval.
- **PRIV-03** — MUST NOT use production data in tests, seeds, or fixtures; anonymize any sample data.
- **PRIV-04** — User-facing messages are Vietnamese (see `apiErrorMessage` in `frontend/src/lib/api.ts`); technical logs and code comments are English. Neither may expose internal details.
- **PRIV-05** — MUST NOT create log files inside the repository; use the existing Nest logger or stdout.
- **PRIV-06** — Auth error responses MUST NOT reveal whether an email or account exists.
- **PRIV-07** — MUST NOT include user data, tokens, or internal URLs in commit messages, issue text, or PR descriptions.

---

## 13. Command allowlist

| Command | Level | Notes |
| --- | --- | --- |
| `npm run typecheck` (backend/frontend) | Allowed | Read-only |
| `npm run lint` (frontend) | Allowed | Read-only |
| `npm run format:check` | Allowed | Read-only |
| `npm run build` | Allowed | Writes `dist/`; never commit it |
| `npm test` (backend) | Allowed | Do not modify tests to pass |
| `npx prisma generate` | Allowed | Regenerates client |
| `npm run prisma:migrate` | Ask | Changes schema and database (`DB-02`, `DB-03`) |
| `npm run seed` | Ask | Writes data (`DB-04`) |
| `docker compose up` / `down` | Ask | Shared local state (`FLOW-07`) |
| `npm install <pkg>` / `npm uninstall` | Ask | Supply chain (`DEP-01`) |
| `docker compose down -v`, `docker volume rm` | Forbidden | Deletes volumes (`DEL-02`, `DB-09`) |
| `rm`, `mv`, `truncate`, `git clean`, `git reset --hard` | Forbidden | See Section 3 |
| `sudo`, `npm i -g`, `apt`, `brew` | Forbidden | See `FS-07` |

---

## 14. Stop conditions

Stop and ask before continuing when:

- **STOP-01** — The task requires anything outside the project root (Section 4).
- **STOP-02** — The task requires deleting, moving, or overwriting an existing file (Section 3).
- **STOP-03** — The task requires reading or using a secret, credential, or real environment value (Section 5).
- **STOP-04** — Instructions conflict with each other or with this document (`SCOPE-03`).
- **STOP-05** — Tests fail for reasons unrelated to the current change, or the cause is unclear.
- **STOP-06** — Unexpected uncommitted changes, unknown branches, or unfamiliar files are found (`DEL-09`).
- **STOP-07** — The only way forward is a forbidden command; propose an alternative instead.

---

## 15. Pre-flight checklist

Before any write or destructive command:

1. Is every path inside the project root? (`FS-01`)
2. Is the file already tracked, and is it recoverable? (`DEL-04`)
3. Do I have explicit approval naming these paths? (`DEL-05`)
4. Does the command appear in the forbidden list? (`DEL-02`)
5. Am I about to read or print a secret? (`SEC-01`)
6. Does this change weaken auth, validation, CORS, or TLS? (`AUTH-*`, `VAL-*`, `SEC-08`)
7. Is the change limited to the requested scope? (`FLOW-02`)
8. Have I read every file I am about to edit? (`FLOW-01`)

Before finishing a task:

1. Which files changed?
2. Which commands ran, and what was the real result?
3. What could not be verified, and why?
4. Did I leave the working tree in an unexpected state?
5. Is there anything the user must decide or approve?

---

## 16. Maintenance

- **MAINT-01** — Only maintainers may change this document. Changes require review like code.
- **MAINT-02** — New rules get a new ID in the correct prefix; retired IDs are never reused.
- **MAINT-03** — When a project convention changes, update the relevant section and the `Last updated` field in the same change.
- **MAINT-04** — If an agent repeatedly violates a rule, treat it as a documentation defect first: clarify the rule, add a concrete example, or add it to the command allowlist.

---

## Appendix A — Forbidden command patterns (quick reference)

```text
rm, rmdir, unlink, shred          DEL-02
mv <path> <existing-path>         DEL-01
truncate, > existing-file         DEL-02
find . -delete                    DEL-02
git clean -[fdx]                  DEL-02
git reset --hard                  DEL-02
git checkout -- .                 DEL-02
git restore                       DEL-02
git push --force                  GIT-02
git filter-branch                 DEL-07
docker compose down -v            DB-09
docker volume rm                  DB-09
prisma migrate reset              DB-02
DROP / TRUNCATE / DELETE (mass)   DB-02
sudo, npm i -g, apt, brew         FS-07
curl ... | bash                   ASK-05
```

## Appendix B — Reporting template

```text
Changed:   <files, one per line>
Commands:  <exact command> -> <result>
Verified:  <checks passed> / <checks not run and why>
Risks:     <security or behavior risks introduced>
Approvals: <actions approved by the user, with scope>
Next:      <remaining work or decisions needed>
```
