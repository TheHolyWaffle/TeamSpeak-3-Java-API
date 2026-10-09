# CI and releases

`master` remains the default branch and the current published release line.
Modernization task PRs target `modernization/2.0`. Its CI verifies Java 25 and the
newer GA JDK (currently 27), with Java 25 bytecode on both. Maven `verify` runs
tests and creates the normal, sources and Javadoc JARs; `-Pfull verify`
additionally creates the standalone JAR. Maven errors fail the job, with logs and available test/Javadoc reports
uploaded for seven days. PR builds have read-only permissions and no release
credentials. Linux Docker checks arrive with #434; japicmp arrives with #446.

## Before merging modernization into master

The existing `master` workflow still publishes ordinary pushes until a separate
preparatory PR removes its `deploy` job from `.github/workflows/maven.yml`. Land
that small change before merging any modernization work into `master`. A
[reviewable isolation patch](master-publishing-isolation.patch), checked against the
current `master` workflow, removes only that job and its obsolete header comment.
Apply it in a separate checkout of `master` and open its preparatory PR. Preserve
`master`'s current Java/build configuration in that PR; do not cherry-pick the
Java 25 workflow without the corresponding build changes. This branch removes the
publisher as part of its replacement CI, but cannot change the deployed workflow
on `master` before its PR merges. Travis is removed on this modernization branch.

Repository administrators should extend the branch rules to `modernization/2.0`
and require the **CI required** status from GitHub Actions, including requiring
up-to-date branches and PR review. Keep the old `master` build check required until
the final integration PR introduces this workflow, then require **CI required** on
`master` too. The README badge tracks `modernization/2.0` now; switch its branch query
back to `master` when the final integration PR lands.

## Record intent

Use Conventional Commit PR titles and **Squash and merge**. Configure GitHub's
squash-merge message to use the PR title and description so release-please sees the
reviewed type, breaking-change notes, and issue references in the resulting commit.
Individual development commits can be freely amended; the resulting integration
commit must preserve release intent. The **Release intent** job validates PR titles,
including generated release PRs, and reruns when a title changes.

| Title | Impact | Use |
| --- | --- | --- |
| `feat!: ...`, `fix!: ...`, or another type with `!` | major | Breaking change; describe migration in the PR body |
| `feat: ...` | minor | Backward-compatible feature |
| `fix: ...`, `perf: ...`, `deps: ...` | patch | Backward-compatible fix, performance or dependency update |
| `ci: ...`, `test: ...`, `chore: ...`, etc. | none | Internal work; no user-facing release on its own |

A `BREAKING CHANGE:` footer also records a major impact. Reference the issue in the
PR body and retain the generated PR number when squashing. Release-please derives
notes and versions from merged commits; no separate fragment files, exemptions,
or Python CI scripts are needed. Changes to runtime support, defaults, and
behavior still need human compatibility review. Task #446 adds japicmp checks.

## Prepare a release PR

Run **Prepare release** manually on `modernization/2.0`. After final integration,
run it on `master`. The selected branch is the release PR's target; other branches
cannot execute the job. The SHA-pinned release-please Action uses its native
`maven` strategy to update Maven project versions, `CHANGELOG.md`, and
`.release-please-manifest.json`, and opens or updates a single release PR per target
branch. It does not update third-party dependency/plugin versions. Review version
changes when adding modules, particularly inherited parent versions.

The manifest starts from **1.3.1**, the verified latest published version. The
bootstrap SHA is that release's commit. The first modernization version is
explicitly set by `release-as: 2.0.0`, independent of the legacy commit titles.
Release-please still needs a releasable Conventional Commit to generate notes;
this task's breaking PR supplies that intent and carries earlier migration details.
Review the first generated notes for the changes in tasks #429/#430 as well.

Configure `RELEASE_PR_TOKEN` as a dedicated fine-grained PAT for this repository,
with Contents, Pull requests, and Issues read/write (release-please manages PR
labels). It is used only by the trusted, manually dispatched preparation job.
Generated PR events trigger normal CI; the default `GITHUB_TOKEN` would suppress
those events. PR verification uses only a read-only token. Preparation and future
publication share a serialized concurrency group and never cancel running releases.

`skip-github-release: true` prevents preparation from creating tags or GitHub
releases. It does not publish to Maven Central. Review and merge the release PR
only after **CI required** passes. It changes the working `2.0.0-SNAPSHOT` to stable
`2.0.0` and updates the manifest to match. After final integration, use the explicit publication action described below
for the exact approved `master` commit. JReleaser creates `v2.0.0` and the GitHub
release only after Central publication; do not create tags manually.

The Maven strategy's automatic snapshot PRs are disabled with `skip-snapshot: true`
during bootstrap to avoid proposing a legacy 1.3.x snapshot. After 2.0.0 is published,
remove `release-as` and `skip-snapshot` from the config and the bootstrap SHA.
Release-please then derives subsequent versions normally and prepares separate
`autorelease: snapshot` PRs after releases. Those snapshot PRs update development
versions without creating public releases; review and merge them through CI.

For a non-publishing rehearsal, use the pinned release-please CLI version bundled
with the Action and its dry-run mode; do not run preparation against a live
repository with publication enabled. Native Maven verification remains:

```sh
./mvnw -B -ntp verify
./mvnw -B -ntp -Pfull verify
```

## Modernization migration notes

Tasks #429 and #430 landed before Conventional Commit title checks. Their user-facing
changes must appear in the first release notes and migration guide:

- Build and run on Java 25 or newer; Java 8–24 runtimes are no longer supported.
  Use the pinned Maven Wrapper to build from source.
- SSHJ and its Bouncy Castle dependencies are refreshed with patched crypto modules.
- Logging uses SLF4J 2.0. Replace SLF4J 1.7 bindings with exactly one compatible 2.x
  provider. The standalone JAR includes SimpleLogger.

## Central Portal publication (#433)

JReleaser **1.26.0**, pinned in Maven, is the sole owner of signing, Central Portal
release deployment, version tags, and GitHub releases. Release-please remains the
sole owner of the reviewed stable POM version, manifest version and release notes;
its preparation workflow continues to use `skip-github-release: true`. JReleaser
uses `target/release-notes.md` as an external changelog, with formatting disabled.
The metadata script extracts exactly the newest matching `## [version]` section,
including its heading, internal Markdown headings and whitespace, and excludes
all older releases. Only version headings delimit release entries. It rejects
snapshots, prereleases, leading-zero versions, tag/manifest mismatches, duplicate
entries, and entries which are not newest. Notes above 10,000 UTF-8 bytes are
rejected before publication: JReleaser truncates GitHub bodies above 10,000
characters, so this conservative bound guarantees unchanged notes. If needed,
shorten the release-please entry through a reviewed PR; the workflow never rewrites
it. No second changelog generator runs.

The old `ossrh` profile, Nexus staging extension and retired endpoints are removed.
Do not use `-Possrh` or remote Maven `deploy`. `-Pcentral-staging deploy` runs tests
and stages the normal JAR, POM, source JAR and Javadoc JAR in a **local file
repository** under `target/staging-deploy`. It does not upload anything. Keep `full`
separate: its standalone JAR and logging-provider dependency are not the Central
library publication. Maven retains license, SCM, description, developer and project
metadata; JReleaser validates those metadata and the required artifacts, signs them,
and constructs the Central bundle. No second Central publishing plugin is used.

### Credentials and namespace

Before publication, an administrator must verify the Central Portal user token
against the Portal account and verify access to `com.github.theholywaffle`. Legacy
OSSRH passwords are not Portal user tokens. Generate a Portal user token under
Account, then verify the account's namespace access in Portal; record only the
verification date, account identity and namespace, never the token values. Token
verification must not upload a test release. See the official
[Portal token/API documentation](https://central.sonatype.org/publish/publish-portal-api/)
and [namespace documentation](https://central.sonatype.org/register/namespace/).

Configure a protected GitHub environment named **release**, restricted to `master`,
using its deployment branch policy. Store these secrets only in that environment:

- `CENTRAL_PORTAL_USERNAME` and `CENTRAL_PORTAL_PASSWORD`: Portal user-token pair.
- `RELEASE_GPG_PUBLIC_KEY` and `RELEASE_GPG_SECRET_KEY`: ASCII-armored release keys.
- `RELEASE_GPG_PASSPHRASE`: the signing-key passphrase.

The public key must be published to a supported keyserver and valid for the
release; JReleaser checks publication and expiration. The default GitHub token has
Contents write permission only in the protected publication job, for JReleaser's
version tag and GitHub release. Preparation still uses its separate release PR
token. CI, PRs, metadata validation and non-publishing bundle validation have no
Portal or real signing credentials. Publication writes temporary private-key
files with owner-only permissions and deletes them on exit; recovery artifacts
exclude those files and JReleaser's trace log. Retire legacy OSSRH secrets once
`master`'s old automatic publisher has been isolated as described above.

### Local validation and CI rehearsal

Use JDK 25 and the pinned Maven Wrapper, without preview features:

```sh
./mvnw -B -ntp verify
./mvnw -B -ntp -Pfull verify
bash .github/scripts/test-release-metadata.sh
```

For an approved stable release checkout, extract its notes, then run the signed
non-publishing rehearsal:

```sh
./mvnw -B -ntp org.apache.maven.plugins:maven-help-plugin:3.5.2:evaluate \
  -Dexpression=project.version -Doutput=target/release-version.txt
bash .github/scripts/release-metadata.sh "$(cat target/release-version.txt)" v2.0.0
bash .github/scripts/release-rehearsal.sh
```

The rehearsal generates an ephemeral test signing key, stages all four normal
artifacts, runs `jreleaser:deploy -Djreleaser.dry.run=true`, and independently
verifies every signature using GPG. It also rehearses JReleaser's tag/GitHub release
step in dry-run mode. Dummy credentials are confined to this explicitly
non-publishing script. Dry-run skips uploads, tags and GitHub releases; JReleaser
may still perform read-only artifact/keyserver checks. The ephemeral key is deleted
on exit and must never be used for a real release.

For development snapshots, use a disposable checkout/copy and set only that copy
to synthetic `0.0.0`, as the **Validate publication bundle (no upload)** CI job does:

```sh
./mvnw -B -ntp org.codehaus.mojo:versions-maven-plugin:2.22.0:set \
  -DnewVersion=0.0.0 -DgenerateBackupPoms=false -DupdateBuildOutputTimestampPolicy=never
mkdir -p target
printf '## [0.0.0]\n\nNon-publishing CI rehearsal.\n' > target/release-notes.md
bash .github/scripts/release-rehearsal.sh
```

CI uploads the signed repository and generated bundle as a short-lived Actions
artifact for inspection, without uploading to Portal or publishing a release.
Synthetic version/notes never change committed source or the approved release.
Full consumer acceptance and complete pipeline rehearsal remain #447 and #451.

### Explicit release action

After final integration, manually dispatch **Explicit release publication** on
`master`. Supply the full reviewed release-please merge SHA and exact `vX.Y.Z`
tag. The default action **validate** verifies without publication. Other branch
refs are rejected; ordinary pushes, tag pushes, PRs and integration merges have no
publication trigger. The commit must still be `master` HEAD, correspond to a merged
release-please PR targeting `master` with `autorelease: pending`, and have successful
**CI required** checks. Maven version, manifest, latest changelog entry and tag must
agree. Existing tags must identify that exact commit; an existing GitHub release
blocks reruns. The same Java matrix and signed bundle rehearsal must succeed before
the protected publication job can begin. Additional environment reviewers may be
configured under repository policy; they are not required by this task.

Select **publish** only as the explicit release action after those checks. The job
rechecks `master` HEAD when the release environment job begins, checks out the
validated SHA, stages the normal artifacts, then signs and deploys using the
[JReleaser Central Portal deployer](https://jreleaser.org/guide/latest/reference/deploy/maven/maven-central.html).
After Central succeeds, it waits for the release POM to be available from Central
and uses the release-only configuration to create the version tag and GitHub release
with exactly the extracted notes. The job rechecks the approved `master` SHA again
immediately before that tag/release step, after deployment and Central propagation.
Both JReleaser configurations disable overwrite
and release updates. No tag or GitHub release is created by release-please.

### Partial failure and recovery

Keep the same approved version and commit throughout recovery. Inspect the Portal
deployment state and the retained **release-state** artifact first; do not blindly
rerun **publish** after an upload timeout. `target/jreleaser/deployment-output.properties` (preserved before the GitHub step)
records `deployMavenCentralCentralDeploymentId` when Portal returns an ID. Preserve
the original bundle and commit evidence. A missing ID after a timeout requires
checking Portal for a matching deployment before any retry. Never create another
version merely to recover and never overwrite released artifacts.

- If validation failed before any upload, fix the cause and repeat validation.
- If the existing Portal deployment is validated and still unpublished, select
  **resume-central** with its deployment UUID. This sets JReleaser's stage to
  `PUBLISH` and supplies that deployment ID, without a second upload. A read-only
  Portal status request must confirm `VALIDATED` and only the exact namespace,
  artifact and version PURLs. Check the original bundle evidence as well.
- If Central has already published but the GitHub step failed, select
  **finish-github**. It requires the same commit/version gates and Central POM
  availability, then runs only the JReleaser release step with the GitHub token;
  it never redeploys and needs no signing or Portal secrets.
- If Portal rejected a bundle, review the failure and the existing deployment in
  Portal before deleting an unpublished deployment or deciding whether corrected
  artifacts can be uploaded. Published Maven versions are immutable. If a GitHub
  release already exists, the workflow refuses to overwrite it; inspect and resolve
  state manually rather than deleting a completed release.

Recovery actions repeat validation and use the restricted release environment. If `master`
has advanced, the workflow intentionally blocks; a maintainer must review a recovery
change for that exact approved commit rather than bypassing checks. JReleaser's
[staged deployment documentation](https://jreleaser.org/guide/latest/reference/deploy/maven/maven-central.html#_staged_deployments)
explains `UPLOAD`, `PUBLISH` and retained deployment IDs.

### Snapshot policy

Remote snapshot publication is disabled. There is no snapshot distribution endpoint,
workflow or Portal deployer; the deployer's `active: RELEASE` excludes snapshots.
`-Pcentral-staging` can stage snapshots locally for development but does not publish
them. A future snapshot policy requires a separately reviewed explicit workflow and
repository configuration; it must never create public SemVer tags/GitHub releases.
