# Contributing

Thanks for your interest in `dicechess-bot-java` — the Java 25 baseline house bot and
reference starter template for the Dice Chess platform.

## Contributor License Agreement

Before your first pull request can be accepted, you must sign the project's
[Contributor License Agreement](CLA.md). Signing is self-service: append yourself to the
`signatures` array in [`.github/cla-signatures.json`](.github/cla-signatures.json) in the
same pull request (see [CLA.md](CLA.md), "How to Sign"). The `CI: CLA` status check fails
until the entry is present. Pull requests from the repository owner, organization
members, and bots are exempt.

Why a CLA: the project follows an open-core model. The public repositories are AGPL-3.0,
and the project owner retains the ability to combine the code with closed-source modules
and to offer it under additional licenses. The CLA preserves that option while your
contribution always remains available under AGPL-3.0 — and you keep the copyright to your
work. A plain DCO (`Signed-off-by`) would not grant relicensing rights, which is why a CLA
is used instead.

> **Just building your own bot?** You do not need to sign anything to *use* this
> repository as a template — fork it and go. The CLA applies only to contributions sent
> back here. Note that this repository is AGPL-3.0 and links the AGPL-3.0 engine, so a
> fork you distribute carries those terms; the MIT starter kits
> ([Python](https://github.com/rabestro/dicechess-bot-python),
> [TypeScript](https://github.com/rabestro/dicechess-bot-typescript)) are the permissive
> starting point if that matters to you.

## Development Workflow

**Workflow order for issue-driven work: Issue → Branch → Implementation → PR.** Routine
work (`refactor`, `chore`, `docs`, `ci`, `test`, `perf`) may skip the issue.

- For issue-driven work, create an issue (Context, Objective, Definition of Done) before
  implementation.
- Create a branch: `<type>/<short-desc>`, optionally `<type>/<id>-<short-desc>` to link an
  issue. Types: `task` / `feat` / `bug` (issue-driven), `refactor` / `chore` / `docs` /
  `ci` / `test` / `perf` (issueless).
- Implement changes on that branch only. Do not work directly on `main`.
- Run `mise run format` on modified code, then `mise run check` (Spotless check, unit and
  integration tests, shaded JAR build) before opening a PR.
- When the branch references an issue, include `Closes #<issue-number>` in the PR body and
  a checklist matching the issue's Definition of Done.

See [`AGENTS.md`](AGENTS.md) for branch-name patterns and agent-specific guidance.

## Quality Gates

`main` is protected by a repository **ruleset** (Settings → Rules → Rulesets → `main`).
Direct pushes are refused; every change arrives through a pull request.

Required status checks — a pull request cannot merge until both are green:

- **`check`** (`CI: Java`) — Spotless formatting, unit and integration tests, and the shaded
  JAR build.
- **`cla-check`** (`CI: CLA`) — the signature check described above. Bots, the owner,
  organization members and collaborators are exempt.

Reported on every pull request, but deliberately **not** required — visible, and red when
they fail:

- **SonarQube Cloud** (`SonarCloud Code Analysis`) — the built-in *Sonar way* quality gate,
  which includes coverage on new code; test coverage is reported from the JaCoCo XML produced
  by the build.
- **CodeQL** and secret scanning, applied organization-wide.

Force pushes and branch deletion are refused. An approval count and *do not allow bypassing*
are deliberately **not** enabled — see trap 2 below.

### Why this exact shape — five traps

This configuration is the template for the other Dice Chess repositories, so the reasoning is
recorded rather than left to be rediscovered. Each of the following is a way to make `main`
protected and the repository unusable at the same time. All five were hit or found while
setting this up (#50).

**1. A required check that never runs blocks the merge forever.** `ci.yaml` used to be
path-filtered, so a documentation-only pull request produced no `check` at all — on #44
(README-only) the entire check list was `labeler`. A check that never *starts* is not green,
it is **missing**, and a missing required check is permanent. The filters were removed:
`CI: Java` now runs on every pull request, for about a minute of CI. The same trap is why
`SonarCloud Code Analysis` is *not* required: `ci.yaml` skips the Sonar step for pull requests
from forks, where `SONAR_TOKEN` is unavailable, so requiring it would permanently block
exactly the external contributors the CLA exists for. Require only checks that run on every
pull request from every source.

**2. Do not lock out a solo maintainer.** *Require a pull request before merging* is enabled
with **zero** required approvals. That still forces every change through a pull request, but
does not demand an approval nobody can give — GitHub does not let you approve your own pull
request, so an approval count would block every pull request permanently. For the same
reason, *do not allow bypassing the above settings* stays off and organization admins keep a
bypass: with a single owner it turns a misconfiguration into a locked repository. Tighten both
the day there is a second maintainer, and not before.

**3. Nothing may push to `main` unchecked — including the release.** `release.yaml` used to
push the `chore(release): bump version to <next>-SNAPSHOT` commit straight to `main`. That
commit is created at push time, so no status check has ever run on it, and a protected `main`
rejects it with `GH006: Protected branch update failed` — *after* the tag, the container image
and the GitHub Release are already published, which is the worst place for a release to fail.
Administrator bypass does not rescue it: the push comes from `github-actions[bot]` via
`GITHUB_TOKEN`, and GitHub Actions is not an eligible ruleset bypass actor. The tag push is
unaffected (branch rulesets do not apply to tags), so only the bump moved — it now goes out on
a `chore/bump-to-<version>` branch and arrives as a pull request like everything else.

**4. A pull request opened with `GITHUB_TOKEN` does not get checked.** This is trap 1 again by
another route, and it is the one that makes the naive fix for trap 3 fail silently. Events
triggered by the default `GITHUB_TOKEN` do not create workflow runs. The single relevant
exception is narrow: `pull_request` runs *are* created, but held in an **approval-required**
state. There is no exception for `pull_request_target`, so `cla-check` and `labeler` never
start at all — and `cla-check` is required. The bump pull request is therefore opened with a
**GitHub App installation token**, which triggers events normally. `release.yaml` mints it as
its very first step, before the tag, the image and the Release exist, so a missing or
misconfigured App fails the release immediately and cheaply instead of half-way through.

Releases consequently require two secrets — `RELEASE_APP_ID` and `RELEASE_APP_PRIVATE_KEY` —
from a GitHub App with repository permissions **Contents: Read and write** and **Pull
requests: Read and write**, installed on the repository. Keeping them as organization secrets
makes the rollout to the next repository a no-op.

**5. A step that needs a secret fails the whole required check when the secret is absent.**
Dependabot pull requests receive *Dependabot* secrets, not Actions secrets, so `SONAR_TOKEN`
is empty in them and the Sonar step exits non-zero — taking the required `check` down with it
and making every Dependabot pull request unmergeable. The fork guard already on that step does
not catch it, because Dependabot branches live in this repository and pass the fork test; the
reliable discriminator is `github.actor != 'dependabot[bot]'`. The general shape of the trap:
a *required* check must not contain a step that depends on a secret some legitimate pull
request will never have. Either guard the step or move it out of the required job. Nothing is
lost here — a dependency bump changes no source, so the quality gate has no new code to
measure.

### Two notes for whoever copies this

*Require branches to be up to date before merging* is deliberately off. It would force a
rebase of every open pull request after each merge, which is painful against weekly Dependabot
batches, and it buys little when `check` already runs on the merge result.

Verify protection with `gh api repos/<owner>/<repo>/rules/branches/main`. The classic
`gh api repos/<owner>/<repo>/branches/main/protection` endpoint reports only *classic* branch
protection and answers 404 for a ruleset — that 404 does not mean the branch is unprotected. A
ruleset is used here on purpose: `scorecard.yaml` runs with the default `GITHUB_TOKEN`, and
OpenSSF Scorecard can read rulesets with that token, while seeing classic branch protection
would require issuing a separate fine-grained PAT.

## Reporting Issues

Bug reports and feature requests are welcome via
[GitHub Issues](https://github.com/fortemate/dicechess-bot-java/issues). For questions
about the bot platform and the webhook protocol, see the platform documentation at
[bots.jc.id.lv](https://bots.jc.id.lv).

Security vulnerabilities should be reported privately per our [Security Policy](SECURITY.md)
rather than through public issues.

