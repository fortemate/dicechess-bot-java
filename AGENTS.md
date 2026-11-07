# AGENTS.md

Branch naming rules, developer workflows, and agent guidance for the `dicechess-bot-java` repository.

## Definition of Done — before every commit

<!-- dc-shared:definition-of-done-maven v2 — keep identical across Fortemate Maven repositories -->

1. Format: `mise run format` where the repository defines it (Spotless). If `mise` is not on PATH:
   `~/.local/bin/mise exec -- mvn --batch-mode spotless:apply`.
2. Gate: `mise run check` — the same Maven goals CI runs (`verify`, or `spotless:check clean test package`).
   If part of it cannot run in your sandbox, run at least `mise exec -- mvn --batch-mode -DskipTests test-compile`
   plus every suite that can run, and list what you skipped in the pull request.
3. Never publish unformatted Java or code that does not compile: CI rejects both, and every red run costs a
   review cycle.

All agents use the repository-pinned toolchain and documented setup commands. If a required tool
is unavailable, follow the repository's setup documentation and report any gate that cannot run.

<!-- /dc-shared:definition-of-done-maven -->

## Branch Naming Conventions

Branch name pattern: `<type>/<short-description>`, optionally `<type>/<id>-<short-description>` when the branch's PR will fully complete that Issue.

Allowed prefixes:
- Issue-driven: `task` (work items), `feat` (features), `bug` (fixes) — typically carry an `<id>`.
- Issueless: `refactor`, `chore`, `docs`, `ci`, `test`, `perf` — no issue required.

Examples: `bug/42-fix-native-access-warning`, `feat/add-onnx-evaluator`, `chore/bump-deps`.

## Agent Rules (AI Assistance)
- Issue-driven work (`task`/`feat`/`bug`) starts from an issue. Carry its `<id>` only when the PR will fully complete that issue. When the PR targets the default branch, link it with `Closes #<id>`; for partial work or another target branch, use a sub-issue id where appropriate and a non-closing reference. Issueless work (`refactor`/`chore`/`docs`/`ci`/`test`/`perf`) needs no issue. Name the branch per the pattern above.
- Always run `mise run format` on any modified code and ensure `mise run check` passes successfully locally before proposing a PR.
- Releases are human-triggered via GitHub Actions: `gh workflow run release.yaml -f bump=patch|minor|major`. Propose and assist, never execute releases directly.
- Human retains the ultimate authority to review, approve, and merge the PR.
- **GitHub CLI Authentication**: On macOS, credentials are saved in the Keychain. When executing `gh` commands, explicitly set the token to an empty string (e.g., `GH_TOKEN="" gh issue create ...`) to avoid authentication errors.

## Publication boundary

<!-- dc-shared:publication v4 — keep identical across Fortemate repositories -->

- Fortemate is open-core. Public by nature, in the public repositories: their source (engine rules
  and search, feature definitions and extractors, bot templates, the play client and server), serving
  contracts, mechanics, and the programme numbers already published in the project READMEs. Private
  repositories (evaluation service, training pipelines, proprietary evaluators, house bots, analytics,
  infrastructure) stay private in full; this rule governs what may be written into the public ones.
- Always private, wherever it is written: trained weights, opening books, labelled corpora, production
  parameter **values** (search profiles, candidate limits, table sizes, blend weights, time budgets),
  experiment **verdicts** (win rates, feature importance, cost ratios, negative results) and the names
  of private artifacts, hosts and internal paths.
- Before writing to a public repository — code, docs, scaladoc, commit messages, Issues, pull requests,
  review replies — check the text against that list. Values and verdicts go to the private knowledge
  base (`fortemate-internal`, a private repository agents read and write through the owner's access;
  naming it is the address, not a disclosure) and are referenced from public text by page title only;
  examples use placeholders such as `<candidate-limit>` instead of real values.
- The rule is forward-only (ADR 009): nothing already published is retracted and history is never
  rewritten. When unsure whether something is a definition or a verdict, ask the owner before
  publishing.

<!-- /dc-shared:publication -->

## Issue management
<!-- dc-shared:issue-management v8 — keep identical across Fortemate repositories -->

- Classify work with the native GitHub Issue Type: `Bug` (unexpected or incorrect behavior), `Feature` (request, idea, new user-visible capability), `Task` (a specific piece of engineering, research, maintenance or documentation work). Labels on Issues name a technical domain or cross-cutting concern only, never repeat the Type, and must already exist in the repository.
- Never commit to a repository's default branch. Name branches you control `<type>/<short-description>` or `<type>/<issue-id>-<short-description>` with a type from `task|feat|bug|refactor|chore|docs|ci|test|perf`. A branch that carries an Issue id must be closed by its pull request (`Closes #<id>`, or `Closes owner/repository#<id>` across repositories); partial work uses a non-closing reference. Before dispatching an external tool, read the repository's live PR-policy workflow: a tool-managed branch name is acceptable only when that policy allows it and the pull request closes the delegated leaf Issue — never edit a workflow to make a generated branch pass. A delegated pull request and its commits close only their leaf Issue, never a parent or sibling.
- GitHub-facing text is English-only. Every Issue has `Context`, `Objective` and a testable `Definition of Done`; create it with `gh issue create --body-file <file>`, never with an inline multi-line body, and search open and closed Issues across Fortemate repositories for duplicates first. Every actionable Issue (never a pull request) belongs to the organization Project [Fortemate Engineering](https://github.com/orgs/fortemate/projects/1); triage (Type, `Execution tier`, `Status`, `Priority`, labels, relationships, assignee) and the mandatory read-back after every mutation follow the `github-issue-workflow` skill in `fortemate-internal/skills/`.
- Google Jules is retired from new Fortemate work. Never start a Jules session or apply or reapply the `jules` label. Existing sessions and pull requests are completed or recovered under the human owner's direction; preserve closed Issues and their execution metadata as history. Route new work through `Execution tier` and the `github-issue-workflow` skill.
- The human owner reviews, approves and merges pull requests. Agents never merge pull requests or execute releases.

<!-- /dc-shared:issue-management -->

## Contributor routing

If you cannot access the private triage instructions, use the public contribution and Issue templates
and ask a maintainer to set the Issue's `Execution tier` and other organization Project fields.
Maintainers own that routing step; private-document access is not a prerequisite for contributing.
This does not authorize external agent dispatch or change the issueless branch rules above.

## Developer Workflows
- **Core Runner**: Use `mise run <task>` from the root of the repository for all development tasks.
- **Local Validation**: `mise run check` compiles code, runs unit & integration tests, and builds the shaded JAR.
- **Code Formatting**: `mise run format` applies standard Maven/Java formatting.
- **Local Service Control**:
  - `mise run build`: Compiles and packages the application via Maven.
  - `mise run test`: Runs all JUnit 5 unit and integration tests.
  - `mise run run`: Launches the bot server locally on port 8080.
  - `docker build -t dicechess-bot-java .`: Builds the local Docker image.
  - `docker-compose up`: Starts the container stack via `docker-compose.yaml`.

## Approved GitHub Labels

Use ONLY these labels when generating `gh` commands:

* **Shared core** (identical across all Dice Chess repositories):
  * **bug** — Pull-request release classification for a defect fix.
  * **enhancement** — Pull-request release classification for a new or expanded capability.
  * **refactoring** — Code restructuring without behavioral changes.
  * **documentation** — Improvements or additions to documentation.
  * **testing** — Adding unit or integration tests.
  * **performance** — Strategy optimizations and speedups.
  * **ci-cd** — GitHub Actions, build scripts, or mise configuration.
  * **dependencies** — Dependency updates (applied by Dependabot).

* **Domains** (this repository only):
  * **bot-engine** — ONNX strategy, move generation, and Scala interop.
  * **infrastructure** — Docker, Koyeb, and container runtime.
