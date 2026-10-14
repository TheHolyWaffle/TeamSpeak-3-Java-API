# CI and releases

Two workflows run everything:

- [`ci.yml`](../.github/workflows/ci.yml) on every PR, push to `master` and weekly: the Gradle
  build on Java 25 ([quality checks](quality-checks.md)), the TS3 integration suite in Docker
  ([integration tests](integration-tests.md)), a signed publication rehearsal with the Maven and
  Gradle consumers and a reproducibility check ([packaging](packaging.md)), actionlint, dependency
  submission and review, and the PR title check. **CI required** aggregates them and is the only
  required check on `master`. PR jobs have read-only tokens and no release credentials.
- [`release.yml`](../.github/workflows/release.yml) on every push to `master`: release-please, and
  the publication of a release it has just created.

## Record intent

Use Conventional Commit PR titles and **Squash and merge**; the squash commit is the PR title and
description (see [contributing](../CONTRIBUTING.md#pull-request-titles-and-releases)), so
release-please sees the reviewed type, breaking-change notes and issue references. The
**PR title** job validates PR titles, including generated release PRs, and reruns when a
title changes.

| Title | Impact | Use |
| --- | --- | --- |
| `feat!: ...`, `fix!: ...`, or another type with `!` | major | Breaking change; describe migration in the PR body |
| `feat: ...` | minor | Backward-compatible feature |
| `fix: ...`, `perf: ...`, `deps: ...`, `revert: ...` | patch | Backward-compatible fix, performance change, runtime dependency update or revert |
| `docs:`, `test:`, `ci:`, `build:`, `refactor:`, `style:`, `chore: ...` | none | Internal work; not released on its own and hidden from release notes |

A `BREAKING CHANGE:` footer also records a major impact. The release PR carries the version
release-please derived, and its build checks that version against the published API with japicmp
(see [API compatibility](quality-checks.md#api-compatibility)): if a merged change needs a larger
version than the commits declare, the release PR fails. Fix that with a `feat!:` or
`BREAKING CHANGE:` commit, or a [commit override](#correct-release-notes), never by publishing.

## Release

1. **Release PR.** After every releasable push to `master`, release-please updates one release PR
   (`chore(master): release X.Y.Z`) that sets the version in `gradle.properties`, adds the
   `CHANGELOG.md` entry and updates `.release-please-manifest.json`. It uses `RELEASE_PR_TOKEN`, a
   fine-grained PAT for this repository with Contents, Pull requests and Issues read/write, so the
   PR receives normal CI events.
2. **Review and merge** it with **Squash and merge** once **CI required** passes. Read the entry:
   every `!` commit must appear under **BREAKING CHANGES** with usable migration notes.
3. **Tag and GitHub release.** On that merge, release-please creates `vX.Y.Z` and the GitHub
   release with exactly the changelog entry.
4. **Approve publication.** The **gradle jreleaserDeploy** job waits for a reviewer of the protected `release`
   environment. It checks out the tag, requires `gradle.properties` to hold the released version,
   runs the full build and stages the artifacts under `build/staging-deploy`, attests their
   [provenance](#verify-a-published-release), then signs, validates and deploys them to the Central
   Portal with [JReleaser](https://jreleaser.org/guide/latest/reference/deploy/maven/maven-central.html).
   JReleaser's `applyMavenCentralRules` rejects a bundle without sources, Javadoc, signatures or
   the required POM metadata before anything is uploaded.

Snapshots are never published: JReleaser's deployer and signing are `active: RELEASE`, and the
Central token stays behind the approval. To try an unreleased commit, download the
`snapshot-repository` artifact of its **CI** run (kept 14 days). It is an unsigned Maven repository
holding the current `-SNAPSHOT` version; unzip it and declare the directory as a repository, for
example `maven { url = uri("/path/to/snapshot-repository") }` in Gradle.

### Correct release notes

Squash commits are immutable. To fix a merged PR's entry, add a `BEGIN_COMMIT_OVERRIDE` …
`END_COMMIT_OVERRIDE` block with the corrected Conventional Commit message to that PR's
description, as the [release-please documentation](https://github.com/googleapis/release-please#how-can-i-fix-release-notes)
describes; release-please rewrites the release PR on its next run.

### If publication fails

Published Maven versions are immutable; never create another version merely to recover.

- **Failed before the upload** (build, attestation, JReleaser validation): fix the cause on
  `master` if needed and re-run the **gradle jreleaserDeploy** job; it rebuilds the same tag.
- **Upload succeeded but publishing failed or timed out:** check the deployment in the
  [Central Portal](https://central.sonatype.com/publishing/deployments). Publish or drop it there;
  re-run the job only after dropping it. The job's `release-X.Y.Z` artifact keeps the staged files
  and JReleaser's output, including the deployment ID.

### Credentials

The `release` environment is restricted to `master` and requires a reviewer. It holds:

- `CENTRAL_PORTAL_USERNAME` and `CENTRAL_PORTAL_PASSWORD`: a Central Portal user token with access
  to the `com.github.theholywaffle` namespace. Give it an expiry and rotate it after any suspected
  exposure. Maven Central does not support OIDC trusted publishing; recheck when Sonatype announces
  it.
- `MAVEN_GPG_PRIVATE_KEY` (ASCII-armored) and `MAVEN_GPG_PASSPHRASE`: the release signing key,
  which exists only in this environment. The job derives the public key from the private key. It
  must be on a keyserver and valid for the release; JReleaser checks both. Before it expires,
  extend it or replace it with a new key, publish that to keyserver.ubuntu.com and
  keys.openpgp.org, and update the fingerprint under
  [verify a published release](#verify-a-published-release).

## Verify a published release

Maven Central does not show GitHub attestations. Download an artifact and verify it with the
[GitHub CLI](https://cli.github.com/):

```sh
version=2.0.0
curl -fLO "https://repo.maven.apache.org/maven2/com/github/theholywaffle/teamspeak3-api/$version/teamspeak3-api-$version.jar"
gh attestation verify "teamspeak3-api-$version.jar" --repo TheHolyWaffle/TeamSpeak-3-Java-API \
  --signer-workflow TheHolyWaffle/TeamSpeak-3-Java-API/.github/workflows/release.yml
```

The same command works for the `.pom`, `.module`, `-sources.jar` and `-javadoc.jar`. Success proves
the file was built by this repository's `release.yml` on a GitHub-hosted runner. To require the
release commit, add `--source-ref refs/heads/master --source-digest "$(git rev-parse "v$version^{commit}")"`:
the release runs on the push to `master` that merged the release PR, which is the tagged commit.
The release job verifies every file with this policy before it uploads anything. Attestations trust that run; a
[reproducible rebuild](packaging.md#reproducible-artifacts) checks the build itself: check out
`v$version`, run `./gradlew publish` with JDK 25 and compare the SHA-256 of the files under
`build/staging-deploy` with the downloaded ones. The `.asc` PGP signatures, which Central requires,
remain the signer check that Maven tooling understands. Releases from 2.0.0 on are signed with
`336C 77A1 6224 E1E6 90AE B70B 3B5F 5764 861E F0BA` (expires 2029-10-13); 1.x releases were signed
with `72BF 8ED7 AA59 66B2 F5CA AA83 BEE1 8CF6 E79F 7B17`.

## Repository administration

These settings live in the repository, not in files. Recheck them after changing settings or before
a release:

| Setting | Configuration |
| --- | --- |
| Rulesets on `master` | Changes by PR only, squash merge only, stale approvals dismissed, review threads resolved, no deletion or force-push; **CI required** must pass on an up-to-date branch. No bypass actors |
| Ruleset **Immutable version tags** (`refs/tags/v*`) | No deletion or update; creation stays allowed for release-please |
| Immutable releases | A published GitHub release's assets and tag cannot change |
| Actions | Default `GITHUB_TOKEN` read-only; fork PR workflows need approval. Every action is pinned to a full commit SHA by convention |
| Actions allow-list | GitHub-owned actions plus `amannn/action-semantic-pull-request`, `googleapis/release-please-action` and `gradle/actions` |
| `release` environment | Deployment branch policy `master` only, required reviewer @TheHolyWaffle, the secrets above |

A new third-party action must be added to the allow-list (Settings → Actions → General) and pinned
to a full commit SHA with a version comment.

```sh
repo=repos/TheHolyWaffle/TeamSpeak-3-Java-API
for id in $(gh api $repo/rulesets --jq '.[].id'); do
  gh api $repo/rulesets/$id --jq '{name, enforcement, conditions, rules, bypass_actors}'
done
gh api $repo/actions/permissions/selected-actions
gh api $repo/environments/release --jq '{protection_rules, deployment_branch_policy}'
gh api $repo/environments/release/secrets --jq '.secrets[].name'
gh api $repo/actions/secrets --jq '.secrets[].name'
```

## After 2.0.0 is published

In one reviewed `build:` PR:

- set `apiBaselineVersion` to `2.0.0` and `apiBaselineSha256` to the SHA-256 of the published JAR
  in `build.gradle.kts`, after comparing it with Central's `.sha256`;
- remove the **Account for every API difference from 1.3.1** step from `ci.yml` and
  `api-accounting.sh`; keep [API differences from 1.3.1](api-differences-2.0.md) as the record;
- remove `release-as` and `bootstrap-sha` from `release-please-config.json`;
- update the README's 2.0 development-line note and [SECURITY.md](../SECURITY.md#supported-versions),
  where 1.x reaches end of life.
