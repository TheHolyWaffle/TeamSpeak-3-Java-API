# Contributing

Thanks for helping. [Getting help](SUPPORT.md) lists where questions, bugs and vulnerabilities go.
For anything larger than a small fix, open or comment on an issue first so the approach can be
agreed before you write code.

## Build and test

Install JDK 25 or newer (no preview features are used) and set `JAVA_HOME`. The Maven Wrapper
downloads the pinned Maven, so no Maven installation is needed. On Windows, use `mvnw.cmd`.

| Command | What it does | Needs |
| --- | --- | --- |
| `./mvnw -B -ntp verify` | Compiles the library and examples, runs the unit tests, and checks formatting, static analysis, Javadoc, coverage and API compatibility | JDK 25 |
| `./mvnw spotless:apply` | Fixes formatting | JDK 25 |
| `./mvnw -B -ntp -Pintegration verify` | Adds the RAW and SSH integration suites, every example and the development server against disposable TS3 servers | Docker |
| `./mvnw -ntp -Pdev-server test-compile exec:exec` | Starts a disposable TS3 server for manual testing and prints its connection settings | Docker |
| `./mvnw -q -ntp -Pexample test-compile exec:exec -Dexample=ClientInfoExample` | Runs an example against the server in `TS3_*` variables | A server |
| `bash .github/scripts/test-api-compatibility.sh` | Runs the release-impact and japicmp controlled examples | JDK 25 |
| `bash .github/scripts/test-release-metadata.sh` | Tests release-note extraction | Bash |

The README explains [building](README.md#building-from-source),
[integration tests](README.md#integration-tests) and [examples](README.md#run-an-example);
[quality checks](docs/quality-checks.md) describes each check and its limits. The TS3 server
image is licensed by TeamSpeak; read [integration tests](docs/integration-tests.md) before running
Docker-based checks.

## Branches

`master` is the default branch and the published 1.x release line. Until 2.0.0 is integrated:

- 2.0 work happens on `modernization/2.0`. Each task of the
  [modernization plan](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/428) has one
  branch, created from the latest `modernization/2.0`, and one PR back into it.
- Only the completed, validated modernization is merged into `master`, through one final
  integration PR (#451).
- Enhancement work for after 2.0 branches from `master` once that integration has landed.

Contributors without write access work in a fork. A maintainer approves workflow runs for PRs
from outside contributors before CI starts.

## Pull request titles and releases

Releases are prepared by [release-please](https://github.com/googleapis/release-please) from the
commits on the release branch. Every PR is **squash-merged**, and the repository uses the
**PR title** as the commit subject and the **PR description** as the commit body. The title and
description you get approved are therefore the release record:

- The title must be a [Conventional Commit](https://www.conventionalcommits.org/en/v1.0.0/); the
  **Release intent** check rejects anything else and reruns when you edit the title.
- The title becomes the changelog line for releasing types, so write it for library users
  ("fix: decode escaped pipes in client lists", not "fix: review comments").
- Reference issues in the description with `Closes #123` or `Refs #123`; they stay in the commit.
- Individual commits on your branch need no prefix and can be amended or squashed freely.

Each type's SemVer impact is listed in [record intent](docs/releases.md#record-intent): `type!:`
or a `BREAKING CHANGE:` footer is major, `feat:` minor, `fix:`/`perf:`/`deps:`/`revert:` patch,
and internal types such as `docs:`, `test:`, `ci:` and `build:` release nothing on their own.
Breaking changes include changed behavior, defaults and runtime support, and need migration notes
in the description: what users must change, and how. `deps:` is only for changes to the resolved
runtime dependencies; build-only updates use `build:`.

### API compatibility reports

[japicmp](https://siom79.github.io/japicmp/) compares the library's public API:

- `verify` compares it with the last published release and fails if the pending version is too
  small for the changes. Reports go to `target/japicmp/api-compatibility.{md,html,diff,xml}`.
- On each PR, the **API compatibility** job compares your branch with its base and fails if the
  change needs a larger impact than your title declares. The job summary lists the changes, and
  the `api-compatibility` artifact holds the reports.

japicmp sees signatures, not behavior. Changed results, exceptions, threading, timeouts or
defaults behind an unchanged signature still need a breaking title and migration notes; reviewers
check this by hand. See [API compatibility](docs/quality-checks.md#api-compatibility).

### Generated pull requests

- **Release PR.** A maintainer runs the **Prepare release** workflow; release-please opens or
  updates one PR that sets the version and updates `CHANGELOG.md`. Review the generated notes,
  then squash-merge it once **CI required** passes. Merging never publishes: publication is a
  separate, protected action ([releases](docs/releases.md#explicit-release-action)).
- **Snapshot PRs** (`autorelease: snapshot`) are disabled until 2.0.0 is published. Afterwards they
  move the development version on after each release; they publish nothing and are merged like
  other PRs.
- **Dependabot PRs** follow the same checks. Retitle a build-only update from `deps:` to `build:`
  before merging; see [dependency and image maintenance](docs/maintenance.md#release-intent).

Do not edit `CHANGELOG.md`, `.release-please-manifest.json` or the project version by hand, and
do not add changelog fragments: release-please owns them.

## Review expectations

A PR is ready for review when:

- **CI required** is green. It covers Java 25 and the newest GA JDK, the Docker-based TS3 RAW and
  SSH suite, dependency review, API compatibility, release intent, the release dry run, the
  publication rehearsal and reproducible builds.
- It does one thing. Unrelated refactoring or reformatting goes in a separate PR.
- New behavior has tests, ideally against a live server in the integration suite when it depends
  on TS3 behavior. Fixes include a test that failed before.
- The README, `docs/`, examples and Javadoc match the change.
- The description follows the [PR template](.github/pull_request_template.md).

Maintainers review every PR, and all review threads must be resolved before merging. Breaking
changes are acceptable for the next major version, recorded with migration notes rather than
compatibility shims. Changes to runtime support, defaults or behavior get an explicit
compatibility review even when japicmp passes.

## Repository rules

These are enforced by GitHub; contributors do not need to configure anything.

- `master` and `modernization/2.0` cannot be pushed to directly, force-pushed or deleted. Changes
  arrive by squash-merged PR with resolved review threads; **CI required** must pass on an
  up-to-date branch (`master` requires its legacy build check until #451).
- `v*` tags cannot be moved or deleted, and published GitHub releases are immutable. Do not push
  tags: the release workflow creates them.
- Workflows get a read-only `GITHUB_TOKEN` by default and request more per job.
- Actions are limited to GitHub-owned actions and an allow-list. A new third-party action must be
  added to the allow-list by an administrator, or every workflow using it fails, and it must be
  pinned to a full commit SHA with a version comment.
- Secret scanning with push protection rejects pushes that contain known credential formats.

The release-environment and publishing controls are described in [releases](docs/releases.md).
