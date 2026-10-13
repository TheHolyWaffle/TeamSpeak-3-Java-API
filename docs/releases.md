# CI and releases

`master` remains the default branch and the current published release line.
Modernization task PRs target `modernization/2.0`. Its CI verifies Java 25 and the
newer GA JDK (currently 27), with Java 25 bytecode on both. Maven `verify` runs
tests and creates the normal, sources and Javadoc JARs; separate jobs check that
clean rebuilds are reproducible and that Maven and Gradle consumers run against the
staged artifacts (see [packaging](packaging.md)). Maven errors fail the job, with logs and available test/Javadoc reports
uploaded for seven days. PR builds have read-only permissions and no release
credentials. Linux Docker checks arrived with #434. Formatting, static analysis, Javadoc,
coverage, dependency scanning and japicmp API checks are described in
[quality checks](quality-checks.md).

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

Repository rulesets require squash-merged PRs on `master` and `modernization/2.0`, and
**CI required** on an up-to-date `modernization/2.0` (see
[repository rules](../CONTRIBUTING.md#repository-rules)). `master` keeps requiring its old
build check until the final integration PR introduces this workflow; then switch its
required check to **CI required**. The README badge tracks `modernization/2.0` now; switch
its branch query back to `master` when the final integration PR lands.

## Record intent

Use Conventional Commit PR titles and **Squash and merge**; the squash commit is the PR
title and description (see [contributing](../CONTRIBUTING.md#pull-request-titles-and-releases)),
so release-please sees the reviewed type, breaking-change notes, and issue references.
Individual development commits can be freely amended; the resulting integration
commit must preserve release intent. The **Release intent** job validates PR titles,
including generated release PRs, and reruns when a title changes.

| Title | Impact | Use |
| --- | --- | --- |
| `feat!: ...`, `fix!: ...`, or another type with `!` | major | Breaking change; describe migration in the PR body |
| `feat: ...` | minor | Backward-compatible feature |
| `fix: ...`, `perf: ...`, `deps: ...`, `revert: ...` | patch | Backward-compatible fix, performance change, runtime dependency update or revert |
| `docs:`, `test:`, `ci:`, `build:`, `refactor:`, `style:`, `chore: ...` | none | Internal work; not released on its own and hidden from release notes |

A `BREAKING CHANGE:` footer also records a major impact. Reference the issue in the
PR body and retain the generated PR number when squashing. Release-please derives
notes and versions from merged commits; no separate fragment files, exemptions,
or Python CI scripts are needed. Changes to runtime support, defaults, and
behavior still need human compatibility review. The **API compatibility** job rejects a PR
whose API change needs a larger impact than its title or `BREAKING CHANGE:` footer declares,
and `verify` checks the pending version against the published 1.3.1 API; see
[API compatibility](quality-checks.md#api-compatibility) and its limits. The same job requires a
releasing title when the resolved runtime dependencies change and rejects `deps:` when they do not,
so build-only updates use `build:`. Dependabot PRs pass through these same checks; see
[dependency and image maintenance](maintenance.md#release-intent).

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

The required **Release preparation (dry run)** job is the non-publishing rehearsal: it
installs the release-please version locked by the Action pinned above and runs
`release-pr --dry-run` against the PR or pushed branch with a read-only token. It fails if the
configuration does not load, release-please reports an error, or the proposed release PR
does not carry the configured `release-as` version. It skips fork PRs, whose branch
release-please cannot read. To run it locally against a pushed branch:

```sh
git clone https://github.com/googleapis/release-please-action target/release-please-action
git -C target/release-please-action checkout <SHA pinned in release-prepare.yml>
GITHUB_TOKEN=$(gh auth token) GITHUB_REPOSITORY=TheHolyWaffle/TeamSpeak-3-Java-API \
  bash .github/scripts/release-please-dry-run.sh target/release-please-action <branch>
```

## Modernization migration notes

Tasks #429 and #430 landed before Conventional Commit title checks. Their user-facing
changes must appear in the first release notes and migration guide:

- Build and run on Java 25 or newer; Java 8–24 runtimes are no longer supported.
  Use the pinned Maven Wrapper to build from source.
- SSHJ and its Bouncy Castle dependencies are refreshed with patched crypto modules.
- Logging uses SLF4J 2.0. Replace SLF4J 1.7 bindings with exactly one compatible 2.x
  provider.

Task #439 removes `CommandFuture` in favor of JDK `CompletableFuture` and
`CompletionStage`. Recompile consumers and follow the [future migration guide](futures.md)
for callback execution, exception wrappers, chaining, waiting and cancellation.
The async return-type change is an intentional 2.0 source/binary break.

Task #443 moves `TS3Api`/`TS3ApiAsync` operations into domain APIs such as
`channels()`, `clients()`, `permissions()`, `servers()` and `files()`, returns `Optional`
for absent lookups and defines the `TS3Exception` categories. Every previous operation is
mapped in the [domain API migration guide](api-migration.md), an intentional 2.0
source/binary break.

Task #446 makes `TS3Query` final, makes `TS3CommandFailedException` serializable, returns the
wire text from `Command.encoded()` and redacts secrets in communication logs and `toString()`
diagnostics. See the [quality checks migration table](quality-checks.md#migration-20).

Task #447 stops publishing the standalone `with-dependencies` JAR and removes the `full`
profile. Depend on the regular JAR through Maven or Gradle and choose a logging provider; see
[migration from the standalone JAR](packaging.md#migration-from-the-standalone-jar).

Task #448 changes no library API or behavior. The examples moved from `example/` to the
compiled `src/example/java` source set and read their host and login from `TS3_*`
environment variables (see [run an example](../README.md#run-an-example)); copy code from
there rather than from 1.x examples, which used removed APIs. Javadoc now documents that TS3
assigns channel and client icons through the `i_icon_id` permission rather than the
`channel_icon_id`/`client_icon_id` properties, which the server rejects in edits.

Task #449 changes no library API, behavior or dependency. For contributors: `deps:` titles now
require a runtime dependency change, Maven Enforcer accepts any Maven 3.10 or newer 3.x (the
wrapper still pins the exact version), and the TS3 image is pinned in
`src/test/resources/teamspeak3/Dockerfile`. See [dependency and image maintenance](maintenance.md).

Task #450 changes no library API, behavior or dependency. Questions move from Gitter to
GitHub Discussions; see [getting help](../SUPPORT.md) and the
[supported versions](../SECURITY.md#supported-versions), which end 1.x support at 2.0.0.

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
repository** under `target/staging-deploy`. It does not upload anything. Maven retains license, SCM, description, developer and project
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
using its deployment branch policy. Store the Portal token in that environment:

- `CENTRAL_PORTAL_USERNAME` and `CENTRAL_PORTAL_PASSWORD`: Portal user-token pair.

Reuse the existing `MAVEN_GPG_PRIVATE_KEY` (ASCII-armored private key) and
`MAVEN_GPG_PASSPHRASE` signing secrets. The protected release job references this
pair directly; no key regeneration, renamed copies or separate public-key secret
are required. It derives the public key in an isolated temporary GnuPG keyring,
then passes both key files to JReleaser. Only this job receives the real signing
pair. Repository secrets already used by the legacy `master` publisher remain
supported; storing the pair under the same names in the `release` environment
can restrict future access further, because environment secrets take precedence.

The public key must be published to a supported keyserver and valid for the
release; JReleaser checks publication and expiration. The default GitHub token has
Contents write permission only in the protected publication job, for JReleaser's
version tag and GitHub release. Preparation still uses its separate release PR
token. CI, PRs, metadata validation and non-publishing bundle validation have no
Portal or real signing credentials. Publication writes temporary private-key
files with owner-only permissions and deletes them on exit; recovery artifacts
exclude those files and JReleaser's trace log. Retire legacy OSSRH account secrets once
`master`'s old automatic publisher has been isolated as described above.

### Local validation and CI rehearsal

Use JDK 25 and the pinned Maven Wrapper, without preview features:

```sh
./mvnw -B -ntp verify
bash .github/scripts/test-release-metadata.sh
```

For an approved stable release checkout, extract its notes, then run the signed
non-publishing rehearsal:

```sh
./mvnw -B -ntp help:evaluate \
  -Dexpression=project.version -Doutput=target/release-version.txt
bash .github/scripts/release-metadata.sh "$(cat target/release-version.txt)" v2.0.0
bash .github/scripts/release-rehearsal.sh
bash .github/scripts/verify-staged-artifacts.sh "$(cat target/release-version.txt)"
```

The rehearsal generates an ephemeral test signing key, stages all four normal
artifacts, runs `jreleaser:deploy -Djreleaser.dry.run=true`, and independently
verifies every signature using GPG. It also rehearses JReleaser's tag/GitHub release
step in dry-run mode. Dummy credentials are confined to this explicitly
non-publishing script. Dry-run skips uploads, tags and GitHub releases; JReleaser
may still perform read-only artifact/keyserver checks. The ephemeral key is deleted
on exit and must never be used for a real release.

For development snapshots, use a disposable Git checkout or copy and apply a synthetic
`0.0.0` release-please state (POM version, manifest and changelog entry) only there, as
the **Validate publication bundle (no upload)** CI job does:

```sh
bash .github/scripts/synthetic-release.sh 0.0.0
bash .github/scripts/release-rehearsal.sh
bash .github/scripts/test-verify-staged-artifacts.sh 0.0.0 target/staging-deploy \
  target/jreleaser/deploy/mavenCentral/central/com.github.theholywaffle-teamspeak3-api-0.0.0-bundle.zip
bash .github/scripts/consumer-check.sh 0.0.0
```

The job then inspects the staged artifacts and runs the Maven and Gradle consumers
against them, as described in [packaging](packaging.md). CI uploads the signed
repository, generated bundle and consumer logs as a short-lived Actions artifact for
inspection, without uploading to Portal or publishing a release. Synthetic
version/notes never change committed source or the approved release. Complete
pipeline rehearsal remains #451.

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
validated SHA, stages the normal artifacts, checks them against the approved version
with `verify-staged-artifacts.sh`, then signs and deploys using the
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
