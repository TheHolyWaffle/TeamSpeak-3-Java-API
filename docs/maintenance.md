# Dependency and image maintenance

[Renovate](https://docs.renovatebot.com/) keeps every pinned version current, configured in
[`.github/renovate.json5`](../.github/renovate.json5) and run by the Mend Renovate GitHub App. Each
update arrives as an ordinary PR and runs the full **CI required** workflow. Minor, patch and
digest updates, and major updates of test dependencies and build tools, merge automatically once
it passes; see [auto-merge](#auto-merge). A maintainer merges the other major updates.

## What is updated

| Ecosystem | Files | PR title |
| --- | --- | --- |
| Gradle | [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) (libraries, build tools, plugins), `gradle/wrapper/gradle-wrapper.properties` (Gradle version and SHA-256), `src/test/consumers/gradle` | `fix(deps):` for the library's runtime dependencies, `chore(deps):` otherwise |
| Maven | `src/test/consumers/maven/pom.xml` | `chore(deps):` |
| GitHub Actions | `.github/workflows/*.yml` (SHA pins with version comments) | `chore(deps):` |
| Docker | [`src/test/resources/teamspeak3/Dockerfile`](../src/test/resources/teamspeak3/Dockerfile) (tag and digest) | `chore(deps):` |

Only updates of SSHJ, the SLF4J API, Gson and Bouncy Castle are `fix(deps):`, so they appear
under "Bug Fixes" and ship in a release; everything else the build uses is a chore. The rules are
in `renovate.json5`; a new runtime dependency must be added to the `fix` rule there. Every tool the
build runs is a catalog entry, so Renovate sees it: the formatter, Error Prone, JaCoCo, japicmp and
the JReleaser CLI.

Version updates wait seven days after a release (`minimumReleaseAge`) so that broken or withdrawn
releases are usually caught upstream first. Security updates are not delayed. Renovate opens
security PRs from this repository's Dependabot alerts, so keep alerts enabled and Dependabot
security updates disabled in the repository settings.

Not updated by Renovate, deliberately:

- The library's own version and the [API baseline](quality-checks.md#baseline): release-please and
  the release process own them.
- The Java version (25): a support policy, not a dependency.
- The actionlint image in `ci.yml`: update the tag and its digest together.

Renovate also proposes updates that are support decisions, and CI fails them, such as a new
TeamSpeak major version. Close such a PR; Renovate does not reopen a closed major update. Its
**Dependency Dashboard** issue lists pending and ignored updates and can trigger an update
immediately.

## Reviewing an update PR

Each PR runs the normal checks: the build, the TS3 RAW and SSH integration suite, dependency
review, the signed publication rehearsal and the Maven and Gradle consumers. Review the linked
release notes and merge with **Squash and merge**, keeping the title. The squash commit takes the
PR description as its body, so remove any `BREAKING CHANGE:` line quoted from upstream release
notes; release-please would read it as this project's.

Release tools are exercised without publishing: a JReleaser update runs the signed dry-run
deployment, and a japicmp update runs the API check against the baseline. A release-please Action
update first runs on the next push to `master`, and `actions/attest-build-provenance` first runs in
a real publication, where a failure stops the release before anything is uploaded.

### Auto-merge

Renovate enables GitHub auto-merge on minor, patch and digest updates, and on major updates of test
dependencies and build tools. Other major updates (runtime dependencies, Actions, Gradle, the TS3
image) wait for a maintainer. The PR merges only once **CI required** passes, so a failing update,
such as a new TS3 image tag, waits for a maintainer and merges by itself after the fix. Required
checks run against the latest `master`; Renovate rebases PRs that fall behind on its next run. To
stop an auto-merge, disable it on the PR.

### TS3 image updates

The image tag is the server version the integration suite expects, and the README support table
must name it (`DocumentationTest`). A digest-only update (the same tag, rebuilt) passes as is. A new
tag changes the verified support claim: the PR fails until a maintainer updates the README support
table and reviews the compatibility results. CI also revalidates the pinned image every week.
