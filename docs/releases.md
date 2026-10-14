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

## Final integration into master

`modernization/2.0` reaches `master` through one reviewed integration PR, after every core task,
including #451, has merged into `modernization/2.0` and the rehearsal below has passed. The
integration itself publishes nothing; publication remains the [explicit release action](#explicit-release-action).

**History is merged, not squashed.** Release-please builds the 2.0.0 notes from the individual
Conventional Commits and their PRs (including [commit overrides](#200-release-rehearsal)). A squash
would leave one commit on `master` and lose them, so the integration PR alone uses a merge commit.
GitHub's merge commit subject, `Merge pull request #N from ...`, is not a Conventional Commit, so
release-please ignores it and adds no entry of its own. The PR description must not contain a
`BEGIN_COMMIT_OVERRIDE` block.

**No separate publishing-isolation PR.** The legacy `master` workflow publishes on pushes to
`master`, but a push runs the workflow file of the pushed commit. The integration merge commit
carries this branch's workflows, which have no deploy job, and no other change reaches `master`
before it. A preparatory PR removing the legacy job would therefore protect nothing.

Steps, in order:

1. **Reconcile `master`.** `git log origin/modernization/2.0..origin/master` must be empty; on
   2026-10-14 `master` (`f458e72`) is an ancestor of `modernization/2.0`. Bring any later `master`
   fix into `modernization/2.0` through a normal PR first.
2. **Open the integration PR** from `modernization/2.0` to `master`, titled
   `chore!: integrate the 2.0 modernization into master`. The `!` satisfies the API and dependency
   impact checks against 1.3.1's `master`; the title does not reach the release notes. Its
   **CI required** run is the complete suite on the final merge result: build, unit and TS3
   RAW/SSH integration tests, packaging, consumers, reproducibility, API compatibility, dependency
   review and the release-please dry run.
3. **Prepare the rules** once that CI is green. In the **Legacy CI required on master** ruleset,
   replace the required check `Build and run tests`, which the 2.0 workflows do not produce, with
   `CI required` (rename the ruleset to match). In **Pull requests for release branches**, add the
   `merge` method next to `squash`, and enable merge commits in the repository settings.
4. **Merge** with **Create a merge commit** and the default message.
5. **Restore** squash-only merging in the ruleset and repository settings. The README build badge
   already tracks `master`, so it shows the new CI from this merge on.
6. **Pin actions.** Once no branch still runs a legacy workflow, enable **Require actions to be
   pinned to a full-length commit SHA** (Settings → Actions). On 2026-10-14 the branches whose
   workflows still use tag-pinned actions are `actionplan`, `modernization/01-reproducible-java-25-build`
   and `TheHolyWaffle/modernization-02-dependency-security-refresh`; delete them or accept that
   their workflows fail.
7. **Retire** the legacy `SONATYPE_USERNAME` and `SONATYPE_PASSWORD` repository secrets, which only
   the removed OSSRH publisher used. Once `MAVEN_GPG_PRIVATE_KEY` and `MAVEN_GPG_PASSPHRASE` also
   exist as `release` environment secrets (see
   [move the signing secrets](#maintainer-action-move-the-signing-secrets)), delete their
   repository-level copies, which only the legacy publisher still read.
8. **Release.** Configure `RELEASE_PR_TOKEN` (see [prepare a release PR](#prepare-a-release-pr)),
   run **Prepare release** on `master`, review the generated 2.0.0 PR against the rehearsed notes,
   squash-merge it after **CI required** passes, then run the explicit release action with
   `validate` and, after review, `publish`. Finish with [after 2.0.0 is published](#after-200-is-published).

Contributor-facing rules are summarized in [repository rules](../CONTRIBUTING.md#repository-rules).

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
so build-only updates use `build:`. Renovate PRs pass through these same checks; see
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

Users follow the [2.0 migration guide](migration-2.0.md), which consolidates these notes and
the task guides; this section records which task introduced each change.

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

Task #484 changes no library API, behavior or dependency. Releases from 2.0.0 on carry GitHub
build provenance attestations; see [verify a published release](#verify-a-published-release).
For contributors: the publication rehearsal now lints every workflow with actionlint and
shellcheck, so a `run:` script with a shellcheck warning fails CI.

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
pair. They are still repository secrets, which any workflow on a pushed branch could read;
moving them into the `release` environment is a
[maintainer action](#maintainer-action-move-the-signing-secrets).

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
version/notes never change committed source or the approved release. The complete
2.0.0 rehearsal is described [below](#200-release-rehearsal).

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
the protected publication job can begin, and a required reviewer of the `release`
environment must approve it (see [repository administration](#repository-administration)).

Select **publish** only as the explicit release action after those checks. The job
rechecks `master` HEAD when the release environment job begins, checks out the
validated SHA, stages the normal artifacts, checks them against the approved version
with `verify-staged-artifacts.sh`, [attests and verifies their provenance](#provenance-attestations),
then signs and deploys using the
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

### Provenance attestations

After `verify-staged-artifacts.sh` accepts the staged POM and library, sources and Javadoc JARs,
the publish job attests them with the SHA-pinned
[`actions/attest-build-provenance`](https://github.com/actions/attest-build-provenance). This signs
a [SLSA build provenance](https://slsa.dev/spec/v1.0/provenance) statement for each file's SHA-256
with a short-lived Sigstore certificate bound to this repository, `release.yml`, the `master` ref
and the commit. For this public repository the signature is recorded in Sigstore's public
transparency log, and GitHub stores the attestation under
[the repository's attestations](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/attestations).

[`verify-attestations.sh`](../.github/scripts/verify-attestations.sh) then fetches the stored
attestations and runs `gh attestation verify` on every staged file with `--signer-workflow`,
`--source-ref refs/heads/master`, the approved commit as `--source-digest` and
`--deny-self-hosted-runners`. JReleaser signs and deploys only after this passes, so a missing or
failed attestation stops the release before anything is uploaded. JReleaser deploys exactly the
attested files: its bundle is the same staged repository plus signatures and checksums.

Only the publish job has `id-token: write` (for the Sigstore certificate) and
`attestations: write`, beside the `contents: write` JReleaser needs; every other job keeps its
previous permissions. **finish-github** skips staging and attestation, like the deployment.
The original **publish** run attested the uploaded files before
uploading them; **resume-central** stages, attests and verifies its rebuild again before
publishing that deployment.

The non-publishing rehearsal never attests the synthetic `0.0.0`. Instead,
[`test-check-release-workflow.sh`](../.github/scripts/test-check-release-workflow.sh) runs in
**Validate publication bundle (no upload)**. It lints every workflow with
[actionlint](https://github.com/rhysd/actionlint), including shellcheck on `run:` scripts where
installed, and [`check-release-workflow.sh`](../.github/scripts/check-release-workflow.sh) reads
the workflows as data with [yq](https://github.com/mikefarah/yq) to require:

- that only the release `publish` job requests `id-token`, `attestations` or `artifact-metadata`
  permissions, with exactly `contents`, `id-token` and `attestations` write, in the `release`
  environment, and that no other workflow uses an attestation action;
- that the steps with ids `stage`, `staged`, `attest` (the SHA-pinned attestation action),
  `provenance` and `deploy` run in this order;
- that these steps share one condition, so the upload never runs without them.

The test then plants one defect per copy of the workflows (attesting or verifying after
deployment, removing verification, a tag-pinned action, a narrower condition, extra permissions,
an attestation in the rehearsal, an undefined input) and requires each to be rejected for that
reason. Which files are attested is checked at release time, when `verify-attestations.sh`
requires provenance for each of the four staged files. [`workflow-tools.sh`](../.github/scripts/workflow-tools.sh) downloads the pinned
actionlint and yq release binaries and checks their published SHA-256. To run it locally:

```sh
bash .github/scripts/test-check-release-workflow.sh
```

The attestation action itself first runs in a real publication. Its failure, or any verification
failure, stops the release before anything is uploaded. Rerun **publish** for the same commit
after a transient failure; a cause that needs a workflow change moves `master` and is handled as
described under [partial failure and recovery](#partial-failure-and-recovery).

### Verify a published release

Maven Central does not show GitHub attestations. Download the artifact and verify it with the
current [GitHub CLI](https://cli.github.com/):

```sh
version=2.0.0
curl -fLO "https://repo.maven.apache.org/maven2/com/github/theholywaffle/teamspeak3-api/$version/teamspeak3-api-$version.jar"
gh attestation verify "teamspeak3-api-$version.jar" --repo TheHolyWaffle/TeamSpeak-3-Java-API \
  --signer-workflow TheHolyWaffle/TeamSpeak-3-Java-API/.github/workflows/release.yml
```

The same command works for the `.pom`, `-sources.jar` and `-javadoc.jar`. Success proves the file
was built by this repository's `release.yml` on a GitHub-hosted runner; the output and
`--format json` name the source commit. Add `--source-ref refs/heads/master` and
`--source-digest <commit>` to require the release commit, which is the commit tagged
`v<version>`. `gh attestation verify` needs a GitHub login (`gh auth login`); `gh attestation
download` saves the bundles for offline verification with `--bundle`.

Attestations prove which workflow run produced the file, but trust that run. A reproducible
rebuild checks the build itself: check out the tagged commit and run
[`reproducibility-check.sh`](../.github/scripts/reproducibility-check.sh) (see
[reproducible artifacts](packaging.md#reproducible-artifacts)), or build it with
`./mvnw -B -ntp -DskipTests package` and compare the SHA-256 of the JARs with the downloaded ones.
Use the JDK 25 build that the release run's `setup-java` step reports, because compiler and
Javadoc output can differ between JDK builds. Matching digests mean the published bytes follow
from the source at that commit. The `.asc` PGP signatures, which Central requires, remain the signer-identity check that Maven
tooling understands.

### Snapshot policy

Remote snapshot publication is disabled. There is no snapshot distribution endpoint,
workflow or Portal deployer; the deployer's `active: RELEASE` excludes snapshots.
`-Pcentral-staging` can stage snapshots locally for development but does not publish
them. A future snapshot policy requires a separately reviewed explicit workflow and
repository configuration; it must never create public SemVer tags/GitHub releases.

## Repository administration

These settings are applied in the repository settings, not in files. They were last checked
against the live configuration with `gh api` on 2026-10-14 (#484); recheck them after changing
settings or before a release with the commands below.

| Setting | Live configuration |
| --- | --- |
| Ruleset **Pull requests for release branches** (`master`, `modernization/2.0`) | No deletion or force-push; changes by PR only, squash merge only, stale approvals dismissed on push, review threads resolved. Required approvals: 0, because there is a single maintainer. No bypass actors |
| Ruleset **CI required on modernization/2.0** | **CI required** must pass on an up-to-date branch |
| Ruleset **Legacy CI required on master** | **Build and run tests** must pass on an up-to-date branch; #451 switches it to **CI required** |
| Ruleset **Protect default branch** (`master`) | No deletion or force-push (older ruleset, overlapping the first) |
| Ruleset **Immutable version tags** (`refs/tags/v*`) | No deletion or update. Creation stays allowed because JReleaser creates the tag |
| Immutable releases | Enabled: a published GitHub release's assets and tag cannot change |
| Actions workflow permissions | Default `GITHUB_TOKEN` read-only; Actions cannot create or approve PRs |
| Actions allow-list | GitHub-owned actions plus `advanced-security/maven-dependency-submission-action`, `amannn/action-semantic-pull-request`, `googleapis/release-please-action` and `gradle/actions/setup-gradle`. Verified-creator actions are not allowed |
| Fork PR workflows | Approval required for all outside contributors |
| `release` environment | Deployment branch policy `master` only, and a required reviewer (@TheHolyWaffle) |
| Secret scanning | Secret scanning and push protection enabled |

**A new third-party action must be added to the allow-list** by an administrator (Settings →
Actions → General), otherwise every workflow that uses it fails. Pin it to a full commit SHA with
a version comment, like every action here; GitHub-owned actions such as
`actions/attest-build-provenance` need no allow-list change.

```sh
repo=repos/TheHolyWaffle/TeamSpeak-3-Java-API
for id in $(gh api $repo/rulesets --jq '.[].id'); do
  gh api $repo/rulesets/$id --jq '{name, enforcement, conditions, rules, bypass_actors}'
done
gh api $repo/immutable-releases
gh api $repo/actions/permissions
gh api $repo/actions/permissions/selected-actions
gh api $repo/actions/permissions/workflow
gh api $repo/actions/permissions/fork-pr-contributor-approval
gh api $repo/environments/release --jq '{protection_rules, deployment_branch_policy}'
gh api $repo/environments/release/deployment-branch-policies --jq '.branch_policies[].name'
gh api $repo --jq .security_and_analysis
gh api $repo/environments/release/secrets --jq '.secrets[].name'
gh api $repo/actions/secrets --jq '.secrets[].name'
```

Secret names (never values) on 2026-10-14: the `release` environment holds
`CENTRAL_PORTAL_USERNAME` and `CENTRAL_PORTAL_PASSWORD`. The repository holds
`MAVEN_GPG_PRIVATE_KEY`, `MAVEN_GPG_PASSPHRASE` and the legacy `SONATYPE_USERNAME` and
`SONATYPE_PASSWORD`.

Owned by the final integration, because `master` still runs its legacy workflow until then:
requiring actions pinned to a full-length commit SHA (`sha_pinning_required` is currently
`false`), switching `master`'s required check to **CI required** and retiring the `SONATYPE_*`
secrets. See [final integration into master](#final-integration-into-master), which also explains
why no separate publishing-isolation PR is needed.

### Maintainer action: move the signing secrets

Not blocking any task. Secret values cannot be read back, so automation cannot move them. Create
`MAVEN_GPG_PRIVATE_KEY` and `MAVEN_GPG_PASSPHRASE` with the same values under Settings →
Environments → **release** → Environment secrets, from the key's original source. Once #451 has
retired `master`'s legacy publisher, which still reads the repository secrets, delete the
repository-level copies. Environment secrets reach only jobs that the environment's branch policy
and reviewer have admitted. The Central Portal token is already an environment secret.

### Trusted publishing (OIDC)

Checked on 2026-10-14: Maven Central does not support OIDC trusted publishing. The
[Portal publisher API](https://central.sonatype.org/publish/publish-portal-api/) and
[token documentation](https://central.sonatype.org/publish/generate-portal-token/) authenticate
only with a user token generated in the Portal, and the
[Central news](https://central.sonatype.org/news/archive/) announce no tokenless publishing. The
long-lived Portal token therefore stays, restricted to the `release` environment. Give it an
expiry when generating it and rotate it after any suspected exposure. Recheck when Sonatype
announces trusted publishing; then replace the token with the job's OIDC identity, which the
publish job already has for attestations.

### OpenSSF Scorecard and SBOM

Decided for 2.0 (#484): neither is adopted now.

- **OpenSSF Scorecard.** Scorecard analyses the default branch only, which is `master` and its
  legacy workflow until #451. Its action, `ossf/scorecard-action`, would also have to join the
  allow-list and receive `security-events` and `id-token` write. The controls it scores, such as
  pinned actions, token permissions, branch protection and dependency updates, are already enforced
  and listed above. Reconsider after the final integration.
- **CycloneDX SBOM.** The published POM already pins the library's direct runtime dependencies
  (SSHJ, the Bouncy Castle provider and PKIX modules, the SLF4J API and Gson), and GitHub's dependency
  graph, fed by the dependency submission job, exports an SPDX SBOM for the repository. Attaching
  an SBOM would add a published file that the staged-artifact checks, the attestation and the
  Central bundle would all have to include. Reconsider if consumers ask for one.

## 2.0.0 release rehearsal

The rehearsal runs every release step for the exact 2.0.0 the release PR would produce, without
uploading, tagging or creating a release. **Prepare release** can only be dispatched once its
workflow is on `master`, so before the final integration the release PR is rehearsed by its dry
run, which reads the merged history exactly as `master` will after a merge-commit integration.

1. **Release PR.** With the pinned release-please (see [prepare a release PR](#prepare-a-release-pr)),
   `release-please-dry-run.sh` against `modernization/2.0` must propose
   `chore(modernization/2.0): release 2.0.0`, updating `pom.xml`, the Maven consumer POM,
   `CHANGELOG.md` and the manifest. It writes the proposed changelog entry to
   `target/release-please-notes.md` and passes it through `release-metadata.sh`, so notes that
   publication would reject (for example above 10,000 bytes) fail CI first. The rehearsed 2.0.0
   notes use 9,189 bytes; keep further releasing PRs before 2.0.0 short. Read the entry: every
   `!` commit must appear under **BREAKING CHANGES** with usable migration notes. Release-please
   logs `commit could not be parsed` for messages it skips; only the pre-Conventional-Commit
   commits from 1.3.1 and #429/#430 may appear there.
2. **Correct notes without rewriting history.** Squash commits are immutable. To fix a merged PR's
   entry, add a `BEGIN_COMMIT_OVERRIDE` … `END_COMMIT_OVERRIDE` block with the corrected
   Conventional Commit message to that PR's description, as the
   [release-please documentation](https://github.com/googleapis/release-please#how-can-i-fix-release-notes)
   describes, and dry-run again. Release-please reads the text after the *first* occurrence of the
   start marker, so the description must not mention the marker anywhere else.
3. **Bundle, consumers and API.** In a disposable worktree, apply that entry and version, then
   build, compare, sign, stage and consume:

   ```sh
   bash .github/scripts/synthetic-release.sh 2.0.0 /path/to/release-please-notes.md
   ./mvnw -B -ntp verify
   bash .github/scripts/api-accounting.sh target/japicmp/api-compatibility.diff docs/api-differences-2.0.md
   bash .github/scripts/release-rehearsal.sh
   bash .github/scripts/verify-staged-artifacts.sh 2.0.0 target/staging-deploy \
     target/jreleaser/deploy/mavenCentral/central/com.github.theholywaffle-teamspeak3-api-2.0.0-bundle.zip
   bash .github/scripts/consumer-check.sh 2.0.0
   bash .github/scripts/reproducibility-check.sh
   ```

   `verify` checks the 2.0.0 API against 1.3.1 with the version check enabled, and the
   accounting check requires a [migration row](api-differences-2.0.md) for every reported type.
4. **Publication gates.** `test-release-workflow.sh` (CI, with `yq`) asserts the gates in
   `release.yml` and the JReleaser configurations listed below.

### What stops a bad release

| Failure | Gate | Tested by |
| --- | --- | --- |
| Not a reviewed release-please merge at `master` HEAD, or **CI required** not green | `validate` job; `publish` needs `validate` and `verify` to succeed | `test-release-workflow.sh` |
| Version, manifest, tag or newest changelog entry disagree, a snapshot or prerelease, notes too long | `release-metadata.sh` in `validate` and again in `publish` | `test-release-metadata.sh` |
| Tag exists on another commit, or the GitHub release exists | `validate` | `test-release-workflow.sh` |
| JReleaser resolved another version, tag, commit or branch than approved | `release-consistency.sh` after deployment, before the tag and GitHub release | `release-rehearsal.sh`; `test-release-workflow.sh` |
| `master` moved during the release | Rechecked when `publish` starts and before the GitHub release | `test-release-workflow.sh` |
| Missing, extra, non-reproducible or wrong-version artifacts | `verify-staged-artifacts.sh` before JReleaser signs | `test-verify-staged-artifacts.sh` (planted defects) |
| Bundle without sources, unsigned or failing Central rules | JReleaser with `applyMavenCentralRules` before upload | `release-rehearsal.sh` (missing sources) |
| Build, test, reproducibility or consumer failure | `verify` reusable workflow | Every CI run |
| `validate` selected | `publish` skipped | `test-release-workflow.sh` |

### Resuming a partial publication

Published Maven versions are immutable and nothing in the pipeline can replace them: the Portal
rejects a second upload of a published version, both JReleaser configurations set
`overwrite: false` with updates disabled, and `validate` refuses a tag on another commit or an
existing GitHub release. A release interrupted after upload resumes with **resume-central**, which
checks that the deployment is `VALIDATED` and holds only this version
(`check-portal-deployment.sh`, tested in `test-release-metadata.sh`) and then publishes that
deployment without uploading again (`JRELEASER_MAVENCENTRAL_CENTRAL_STAGE=PUBLISH`). A release
published to Central whose GitHub step failed finishes with **finish-github**, which neither signs
nor deploys. See [partial failure and recovery](#partial-failure-and-recovery).

## After 2.0.0 is published

Once the release workflow has published 2.0.0 and created `v2.0.0`:

1. **Check the published release.** Download `teamspeak3-api-2.0.0.jar` from
   `https://repo.maven.apache.org/maven2/com/github/theholywaffle/teamspeak3-api/2.0.0/` and run
   `gh attestation verify teamspeak3-api-2.0.0.jar --repo TheHolyWaffle/TeamSpeak-3-Java-API
   --signer-workflow TheHolyWaffle/TeamSpeak-3-Java-API/.github/workflows/release.yml`. Check its
   `.asc` signature with the release key, that `v2.0.0` points to the approved commit, and that the
   GitHub release body equals the `CHANGELOG.md` entry. `reproducibility-check.sh` on a checkout of
   `v2.0.0` must reproduce the unsigned JARs byte for byte.
2. **Move the baselines** in one reviewed PR to `master` (`build:` title):
   - set `api.baseline.version` to `2.0.0` and `api.baseline.sha256` to the SHA-256 of the
     downloaded JAR, after comparing it with Central's `.sha256`;
   - update `test-api-compatibility.sh`, which pins the 1.3.1 comparison, to 2.0.0;
   - remove the **Account for every API difference from 1.3.1** step from `verify.yml`; keep
     [API differences from 1.3.1](api-differences-2.0.md) as the 2.0.0 record;
   - remove `release-as`, `skip-snapshot` and `bootstrap-sha` from `release-please-config.json`,
     so release-please derives later versions and opens `autorelease: snapshot` PRs.
3. **Update support statements:** the README's 2.0 development-line note, and
   [SECURITY.md](../SECURITY.md#supported-versions), where 1.x reaches end of life.

From then on japicmp enforces SemVer against 2.0.0 and every release repeats the explicit release
action. Corrections need a new version; published artifacts are never replaced.
