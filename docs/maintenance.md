# Dependency and image maintenance

[Renovate](https://docs.renovatebot.com/) keeps every pinned version current, configured in
[`.github/renovate.json5`](../.github/renovate.json5) and run by the Mend Renovate GitHub App. Each
update arrives as an ordinary PR and runs the full **CI required** workflow. Minor, patch and
digest updates, and major updates of test dependencies and build plugins, merge automatically once
it passes; see [auto-merge](#auto-merge). A maintainer merges the other major updates.

## What is updated

| Ecosystem | Files | PR title |
| --- | --- | --- |
| Maven | `pom.xml`, `src/test/consumers/maven/pom.xml`, `.mvn/wrapper/` (Maven and wrapper versions, SHA-256) | `fix(deps):` for the runtime library, `chore(deps):` otherwise |
| GitHub Actions | `.github/workflows/*.yml` (SHA pins with version comments) | `chore(deps):` |
| Docker | [`src/test/resources/teamspeak3/Dockerfile`](../src/test/resources/teamspeak3/Dockerfile) (tag and digest) | `chore(deps):` |
| Gradle | `src/test/consumers/gradle` (Gradle version and consumer dependencies) | `chore(deps):` |

These are the titles of Renovate's `config:recommended` preset, which classifies dependencies by
Maven scope, so a new dependency needs no configuration. Runtime updates appear under "Bug Fixes"
in the changelog. The preset also groups packages released together (JUnit, Testcontainers,
SLF4J, ...) and dependencies sharing a Maven version property; a group mixing both kinds takes
`fix(deps):`.

Version updates wait seven days after a release (`minimumReleaseAge`) so that broken or withdrawn
releases are usually caught upstream first; Renovate opens the PR only after that. Security updates
are not delayed. Renovate opens security PRs from this repository's Dependabot alerts, so keep
alerts enabled and Dependabot security updates disabled in the repository settings. Every tool the
build runs is declared where Renovate reads it: plugins invoked by prefix (`help:evaluate`,
`versions:set`) are in `pluginManagement`, the formatter version is shared with a Spotless plugin
dependency, and the Gradle consumer reads its version from `gradle-wrapper.properties`.

Not updated by Renovate, deliberately:

- The library's own version and the [API baseline](quality-checks.md#baseline): release-please and
  the release process own them. Renovate skips them because their versions are Maven properties
  or script inputs.
- The Java versions in CI (25 and the newest GA, currently 27): a support policy, not a dependency.
- actionlint and yq for the [release workflow check](releases.md#provenance-attestations): Renovate
  cannot update the per-platform checksums of binaries downloaded by a script. To update, set the
  version and the SHA-256 of each platform's archive from the project's published checksums in
  [`workflow-tools.sh`](../.github/scripts/workflow-tools.sh), then run
  `bash .github/scripts/test-check-release-workflow.sh`.

Renovate also proposes updates that are support decisions, and CI fails them: Maven 4 (the
enforcer requires Maven 3) and a new TeamSpeak major version. Close such a PR; Renovate does not
reopen a closed major update. Its **Dependency Dashboard** issue lists pending and ignored updates
and can trigger an update immediately.

## Reviewing an update PR

Each PR runs the normal checks: unit tests on Java 25 and 27, the TS3 RAW and SSH integration
suite, dependency review, API compatibility, the release-intent checks below, the signed
non-publishing release rehearsal and the Maven and Gradle consumers. Review the linked release
notes and merge with **Squash and merge**, keeping the title. The squash commit takes the PR
description as its body, so remove any `BREAKING CHANGE:` line quoted from upstream release notes;
release-please would read it as this project's.

Release-tool updates are exercised without publishing:

| Tool | Validation in the PR |
| --- | --- |
| JReleaser | Signed staging plus `jreleaser:deploy` and `jreleaser:release` in dry-run mode, with planted-defect checks (**Validate publication bundle**) |
| japicmp | `verify` against the 1.3.1 baseline and the controlled examples (**API compatibility**) |
| release-please Action | The release-please version locked by the pinned Action runs `release-pr --dry-run` against the PR branch (**Release preparation (dry run)**) |
| Maven deploy, install, help, versions plugins | Local staging deploy and the synthetic release (**Validate publication bundle**) |
| `actions/attest-build-provenance` | Only the workflow wiring check (**Validate publication bundle**); the action first runs in a real publication, where a failure blocks the upload |

### Auto-merge

Renovate enables GitHub auto-merge on minor, patch and digest updates, and on major updates of
Maven test-scoped dependencies and build plugins. Other major updates (runtime dependencies,
Actions, the Maven and Gradle distributions, the TS3 image) wait for a maintainer. The PR merges
only once **CI required** passes, so a failing update, such as a new TS3 image tag, waits for a
maintainer and merges by itself after the fix. Renovate sets the squash commit to its own title,
not the PR description. Required checks run against the latest `master`; Renovate rebases PRs that
fall behind on its next run. To stop an auto-merge, disable it on the PR.

### Release intent

Bot titles go through the same **Release intent** and **API compatibility** jobs as contributor
titles. The API compatibility job also compares the resolved runtime dependencies
(`dependency:list -DincludeScope=runtime`) with the PR base: a change needs a releasing title
(`deps:`, `fix:`, `feat:` or a breaking change), and `deps:` needs a change. Renovate's
`fix(deps):` and `chore(deps):` titles follow the same split.

### TS3 image updates

The image tag is the server version the integration suite expects, and the README support table
must name it (`DocumentationTest`). A digest-only update (the same tag, rebuilt) passes as is.
A new tag changes the verified support claim: the PR fails until a maintainer updates the README
support table and reviews the compatibility results.

## Scheduled detection

**Upstream update detection** (`.github/workflows/upstream-updates.yml`) runs weekly and on demand.
It resolves the newest official TeamSpeak 3 image (the highest `3.x.y` tag on Docker Hub and its
current manifest digest). If that differs from the pin, it runs the full compatibility suite
against it, applied as its update PR would apply it, and fails if the server is incompatible.
The step summary records both images.

**ServerQuery compatibility** also revalidates the pinned image every week. GitHub runs scheduled
and manually dispatched workflows only when they exist on the default branch, so both start with
the final integration into `master`. Until then, check a candidate locally:

```sh
bash .github/scripts/ts3-image-candidate.sh
bash .github/scripts/use-ts3-image.sh 'teamspeak:X.Y.Z@sha256:...'   # disposable checkout only
./mvnw -B -ntp -Pintegration verify
```
