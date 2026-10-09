# CI and releases

`master` remains the default branch and the current published release line.
Modernization task PRs target `modernization/2.0`. Its CI verifies Java 25 and the
newer GA JDK (currently 27), with Java 25 bytecode on both. Maven `verify` runs
tests; `-Pfull verify` additionally creates the standalone, sources and Javadoc
JARs. Maven errors fail the job, with logs and available test/Javadoc reports
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
`2.0.0` and updates the manifest to match. After final integration, tag the exact
approved `master` commit as `v2.0.0`; never tag a later development snapshot.
Task #433 owns the explicit publication action and the single tag/GitHub release owner.

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

## Publication handoff (#433)

The release workflow validates the tag against the stable Maven project version,
release manifest, and generated changelog entry, then invokes the same Java
verification matrix. Maven reads the version natively; validation uses Bash and jq.
The publication job has an unconditional false gate and contains no publishing
command or credential references. Ordinary pushes and PRs cannot reach it.
Task #433 must supply JReleaser, keep dependencies on successful validation and
verification, and check that the tag commit belongs to the approved `master` line.
Use release-please's approved release-specific notes as JReleaser's external
changelog rather than regenerating them or uploading the entire changelog.
Define a single owner for tags/GitHub releases before enabling either tool to create
them. Keep signing/Central credentials in a protected release environment and
publish from that exact verified commit. The obsolete OSSRH POM profile is retained
only for task #433's migration; these workflows never activate it.
