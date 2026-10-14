# Dependency and image maintenance

[Dependabot version updates](https://docs.github.com/en/code-security/dependabot/working-with-dependabot/dependabot-options-reference)
keep every pinned version current, configured in [`.github/dependabot.yml`](../.github/dependabot.yml).
Dependabot is built into GitHub, so it needs no app installation, token or extra Action, and it
already provides this repository's vulnerability alerts and security updates. Each update arrives as
an ordinary PR, runs the full **CI required** workflow with a read-only token, and is merged by a
maintainer like any contributor PR. Nothing merges automatically.

## What is updated

| Ecosystem | Files | PR title |
| --- | --- | --- |
| Maven | `pom.xml`, `src/test/consumers/maven/pom.xml`, `.mvn/wrapper/maven-wrapper.properties` | `deps:` for the runtime library, `build:` otherwise |
| GitHub Actions | `.github/workflows/*.yml` (SHA pins with version comments) | `ci:` |
| Docker | [`src/test/resources/teamspeak3/Dockerfile`](../src/test/resources/teamspeak3/Dockerfile) (tag and digest) | `test:` |
| Gradle | `src/test/consumers/gradle` (Gradle version and consumer dependencies) | `test:` |

Related updates share one PR per group (see the config): the build toolchain, release tooling
(JReleaser, japicmp), SLF4J, test libraries, other build plugins, Actions other than
release-please, and the Gradle consumer.

Runtime dependencies (SSHJ, the Bouncy Castle BOM) and major updates arrive one per PR.
Version updates wait seven days after a release (`cooldown`) so that broken or withdrawn releases
are usually caught upstream first; security updates are not delayed. Every tool the build runs is
declared where Dependabot reads it: plugins invoked by prefix (`help:evaluate`, `versions:set`)
are in `pluginManagement`, the formatter version is shared with a Spotless plugin dependency, and
the Gradle consumer reads its version from `gradle-wrapper.properties`.

Not updated by Dependabot, deliberately:

- The library's own version and the [API baseline](quality-checks.md#baseline): release-please and
  the release process own them.
- The Java versions in CI (25 and the newest GA, currently 27): a support policy, not a dependency.
- Another TeamSpeak major version: TS3 only is supported; major `teamspeak` updates are ignored.
- The Maven distribution while Dependabot's Maven Wrapper support stays behind a feature flag
  (`maven_wrapper_updater` in dependabot-core). Its group is configured already, and the
  scheduled check below reports new Maven 3 releases until then. To update by hand, set the
  version in `distributionUrl` and its SHA-256 in `distributionSha256Sum` of
  `.mvn/wrapper/maven-wrapper.properties`, then run `./mvnw -v`.

- actionlint and yq for the [release workflow check](releases.md#provenance-attestations): Dependabot
  cannot track binaries downloaded by a script. To update, set the version and the SHA-256 of each
  platform's archive from the project's published checksums in
  [`workflow-tools.sh`](../.github/scripts/workflow-tools.sh), then run
  `bash .github/scripts/test-check-release-workflow.sh`.

Dependabot reads its configuration from the default branch only. The configuration lands on
`modernization/2.0` and takes effect when the final integration (#451) merges it into `master`,
the branch its PRs then target.

## Reviewing an update PR

Each PR runs the normal checks: unit tests on Java 25 and 27, the TS3 RAW and SSH integration
suite, dependency review, API compatibility, the release-intent checks below, the signed
non-publishing release rehearsal and the Maven and Gradle consumers. Review the linked release
notes and merge with **Squash and merge**, keeping the title.

Release-tool updates are exercised without publishing:

| Tool | Validation in the PR |
| --- | --- |
| JReleaser | Signed staging plus `jreleaser:deploy` and `jreleaser:release` in dry-run mode, with planted-defect checks (**Validate publication bundle**) |
| japicmp | `verify` against the 1.3.1 baseline and the controlled examples (**API compatibility**) |
| release-please Action | The release-please version locked by the pinned Action runs `release-pr --dry-run` against the PR branch (**Release preparation (dry run)**) |
| Maven deploy, install, help, versions plugins | Local staging deploy and the synthetic release (**Validate publication bundle**) |
| `actions/attest-build-provenance` | Only the workflow wiring check (**Validate publication bundle**); the action first runs in a real publication, where a failure blocks the upload |

### Release intent

Bot titles go through the same **Release intent** and **API compatibility** jobs as contributor
titles. The API compatibility job also compares the resolved runtime dependencies
(`dependency:list -DincludeScope=runtime`) with the PR base: a change needs a releasing title
(`deps:`, `fix:`, `feat:` or a breaking change), and `deps:` needs a change.

Dependabot classifies the Maven distribution, annotation processors and plugin dependencies as
production dependencies, so it titles `build-toolchain` PRs `deps:`. That check then fails; edit the
title to `build:` (the job reruns on edits) before merging, so the update does not cause a release.

### TS3 image updates

The image tag is the server version the integration suite expects, and the README support table
must name it (`DocumentationTest`). A digest-only update (the same tag, rebuilt) passes as is.
A new tag changes the verified support claim: the PR fails until a maintainer updates the README
support table and reviews the compatibility results.

## Scheduled detection

**Upstream update detection** (`.github/workflows/upstream-updates.yml`) runs weekly and on demand:

- It resolves the newest official TeamSpeak 3 image (the highest `3.x.y` tag on Docker Hub and its
  current manifest digest). If that differs from the pin, it runs the full compatibility suite
  against it, applied as its update PR would apply it, and fails if the server is incompatible.
  The step summary records both images.
- It fails while the Maven Wrapper is older than the newest Maven 3 release. Remove this job once
  Dependabot's Maven Wrapper updates are generally available.

**ServerQuery compatibility** also revalidates the pinned image every week. GitHub runs scheduled
and manually dispatched workflows only when they exist on the default branch, so both start with
the final integration into `master`. Until then, check a candidate locally:

```sh
bash .github/scripts/ts3-image-candidate.sh
bash .github/scripts/use-ts3-image.sh 'teamspeak:X.Y.Z@sha256:...'   # disposable checkout only
./mvnw -B -ntp -Pintegration verify
```
