# Contributing

Thanks for helping. [Getting help](SUPPORT.md) lists where questions, bugs and vulnerabilities go.
For anything larger than a small fix, open or comment on an issue first so the approach can be
agreed before you write code.

## Build and test

Install JDK 25 and set `JAVA_HOME`. The Gradle Wrapper downloads the pinned Gradle, so no Gradle
installation is needed. On Windows, use `gradlew.bat`.

| Command | What it does | Needs |
| --- | --- | --- |
| `./gradlew build` | Compiles the library and examples, runs the unit tests, and checks formatting, static analysis, Javadoc and coverage | JDK 25 |
| `./gradlew spotlessApply` | Fixes formatting | JDK 25 |
| `./gradlew build integrationTest` | Adds the RAW and SSH integration suites, every example and the development server against disposable TS3 servers | Docker |
| `./gradlew devServer` | Starts a disposable TS3 server for manual testing and prints its connection settings | Docker |
| `./gradlew -q runExample -Pexample=ClientInfoExample` | Runs an example against the server in `TS3_*` variables | A server |

The README explains [building](README.md#building-from-source),
[integration tests](README.md#integration-tests) and [examples](README.md#run-an-example);
[quality checks](docs/quality-checks.md) describes each check and its limits. The TS3 server
image is licensed by TeamSpeak; read [integration tests](docs/integration-tests.md) before running
Docker-based checks.

## Branches

`master` is the default branch and carries the 2.0 development line. Create one branch per change
from the latest `master` and open one PR back into it.

Contributors without write access work in a fork. A maintainer approves workflow runs for PRs
from outside contributors before CI starts.

## Pull request titles and releases

Releases are prepared by [release-please](https://github.com/googleapis/release-please) from the
commits on the release branch. Every PR is **squash-merged**, and the repository uses the
**PR title** as the commit subject and the **PR description** as the commit body. The title and
description you get approved are therefore the release record:

- The title must be a [Conventional Commit](https://www.conventionalcommits.org/en/v1.0.0/); the
  **PR title** check rejects anything else and reruns when you edit the title.
- The title becomes the changelog line for releasing types, so write it for library users
  ("fix: decode escaped pipes in client lists", not "fix: review comments").
- Reference issues in the description with `Closes #123` or `Refs #123`; they stay in the commit.
- Individual commits on your branch need no prefix and can be amended or squashed freely.

Each type's SemVer impact is listed in [record intent](docs/releases.md#record-intent): `type!:`
or a `BREAKING CHANGE:` footer is major, `feat:` minor, `fix:`/`perf:`/`deps:`/`revert:` patch,
and internal types such as `docs:`, `test:`, `ci:` and `build:` release nothing on their own.
Breaking changes include changed behavior, defaults and runtime support, and need migration notes
in the description: what users must change, and how. `deps:` is only for changes to the library's
runtime dependencies; build-only updates use `build:`.

A removed or changed public signature is breaking, and so are changed results, exceptions,
threading, timeouts or defaults behind an unchanged one; reviewers check this by hand. See
[API changes](docs/quality-checks.md#api-changes).

### Generated pull requests

- **Release PR.** release-please keeps one PR up to date that sets the version and updates
  `CHANGELOG.md`. A maintainer reviews the generated notes and squash-merges it once
  **CI required** passes; that creates the tag and GitHub release, and publication to Maven
  Central then waits for approval in the protected `release` environment
  ([releases](docs/releases.md#release)).
- **Renovate PRs** follow the same checks. Most merge automatically once they pass; see
  [dependency and image maintenance](docs/maintenance.md#auto-merge).

Do not edit `CHANGELOG.md`, `.release-please-manifest.json` or the project version by hand, and
do not add changelog fragments: release-please owns them.

## Review expectations

A PR is ready for review when:

- **CI required** is green. It covers the build, the Docker-based TS3 RAW
  and SSH suite, dependency review, release intent, the publication rehearsal and reproducible
  builds.
- It does one thing. Unrelated refactoring or reformatting goes in a separate PR.
- New behavior has tests, ideally against a live server in the integration suite when it depends
  on TS3 behavior. Fixes include a test that failed before.
- The README, `docs/`, examples and Javadoc match the change.
- The description follows the [PR template](.github/pull_request_template.md).

Maintainers review every PR, and all review threads must be resolved before merging. Breaking
changes are acceptable for the next major version, recorded with migration notes rather than
compatibility shims. Changes to runtime support, defaults or behavior get an explicit
compatibility review.

## Repository rules

These are enforced by GitHub; contributors do not need to configure anything.

- `master` cannot be pushed to directly, force-pushed or deleted. Changes arrive by squash-merged
  PR with resolved review threads; **CI required** must pass on an up-to-date branch.
- `v*` tags cannot be moved or deleted, and published GitHub releases are immutable. Do not push
  tags: release-please creates them.
- Workflows get a read-only `GITHUB_TOKEN` by default and request more per job.
- Actions are limited to GitHub-owned actions and an allow-list. A new third-party action must be
  added to the allow-list by an administrator, or every workflow using it fails, and it must be
  pinned to a full commit SHA with a version comment.
- Secret scanning with push protection rejects pushes that contain known credential formats.

The release-environment and publishing controls are described in [releases](docs/releases.md).
