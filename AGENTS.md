# AGENTS.md

Branch naming rules, developer workflows, and agent guidance for the `dicechess-bot-java` repository.

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

## Issue management
<!-- dc-shared:issue-management v1 — keep identical across Fortemate repositories -->

- Use the native GitHub Issue Type as the canonical work classification:
  - `Bug` for unexpected or incorrect behavior.
  - `Feature` for a request, idea, or new user-visible capability.
  - `Task` for a specific piece of engineering, research, maintenance, or documentation work.
- Never commit directly to a repository's default branch. Name branches `<type>/<short-description>` or `<type>/<id>-<short-description>` using the canonical types `task|feat|bug|refactor|chore|docs|ci|test|perf`. Include an Issue id only when the pull request is intended to fully complete that Issue; otherwise omit it or use the id of an independently actionable sub-issue. Example: `bug/42-fix-dfen-parser`.
- Do not apply `bug` or `enhancement` labels to Issues merely to repeat their Type. Keep those labels for pull-request release classification. On Issues, labels describe only a technical domain or cross-cutting concern, and only existing repository labels may be used.
- Before creating or updating an Issue, search relevant Fortemate repositories across open and closed Issues for semantic duplicates. Read the live Types, field options, labels, assignees, relationships, and open milestones before mutation; never rely on cached IDs or invent metadata.
- GitHub-facing work items are English-only. Use the appropriate Issue Form when available, or `gh issue create --body-file <file>` for CLI creation; never pass a multiline body inline. Every Issue must contain `Context`, `Objective`, and a testable `Definition of Done`.
- Add every actionable Issue and pull request to the organization Project [Fortemate Engineering](https://github.com/orgs/fortemate/projects/1).
- Use Project `Status` only for workflow state:
  - `Backlog` means triaged but not committed for active work.
  - `Ready` means sufficiently defined and available to start.
  - `In progress` means someone is actively working on it.
  - `In review` means implementation is waiting for review or validation.
  - `Done` means the Issue is closed or the pull request is merged.
- Set the organization `Priority` Issue field by impact and urgency: `Urgent` for an immediate incident, security problem, or release blocker; `High` for important or blocking planned work; `Medium` for normal planned work; and `Low` for opportunistic backlog.
- Set the organization `Effort` Issue field to `Low`, `Medium`, or `High` as a relative implementation-and-verification estimate, not as priority or a time promise. Never replace either organization field with labels or duplicate Project fields; leave a value unset when current evidence does not justify it.
- Triage establishes Type, Priority, Effort, applicable labels, Project membership, Status, milestone, and relationships. Assign an Issue only when a person owns its next action, and assign the active owner before moving it to `In progress`; unassigned means no current owner, not low priority.
- Use parent/sub-issue relationships for independently actionable decomposition, `Blocking`/`Blocked by` for hard ordering dependencies, and `Relates to` for non-blocking associations. If the live UI or API cannot create a relation, add an explicit `Related: owner/repository#<id>` cross-reference. Do not simulate relationships with title prefixes, labels, or duplicate task lists.
- When a pull request targets the repository's default branch and fully completes an Issue, link it with `Closes #<id>` or `Closes owner/repository#<id>`. Use a non-closing reference for partial work or for a pull request targeting any other branch.
- After every Issue, pull-request, or Project mutation, read the item back. For an Issue, verify Type, Issue fields, labels, assignee, milestone, relationships, Project membership, and Status. For a pull request, verify base/head branches, draft and merge state, labels, assignees/reviewers, milestone, linked Issues, Project membership, and Status; Issue Type and Issue fields do not apply. Report any metadata that the available API or UI could not set.
- The human owner reviews, approves, and merges pull requests. Agents never merge pull requests or execute releases.

<!-- /dc-shared:issue-management -->

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
