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
on `master` before its PR merges. Remove Travis on this modernization branch.

Repository administrators should extend the branch rules to `modernization/2.0`
and require the **CI required** status from GitHub Actions, including requiring
up-to-date branches and PR review. The current default-branch rules only forbid
deletion and force pushes. Keep the old `master` build check required until the
final integration PR introduces this workflow, then require **CI required** on
`master` too. Do not rename `master` or require checks absent from the legacy
workflow. The README badge tracks `modernization/2.0` now; switch its branch query
back to `master` when the final integration PR lands.

## Record intent

Use the installation and `changie new` commands in the README. Changie 1.26.0
archives are pinned by SHA-256; Python validation uses pinned PyYAML 6.0.3.
The release scripts use Python 3.12 or newer on Linux or macOS.
The categories explicitly map to impact:

| Kind | Impact | Use |
| --- | --- | --- |
| `breaking` | major | Runtime/API incompatibility, with migration instructions |
| `added` | minor | Backward-compatible feature |
| `fixed` | patch | Backward-compatible fix |
| `security` | patch | Backward-compatible security fix |
| `internal` | none | Internal work included in the next release notes |

Include a description, timezone-aware timestamp, and positive `custom.Issue`
(issue or PR number). Highest impact wins when batching; `none` fragments alone
produce no release. Commit-message prefixes are not checked. The initial
published baseline is **1.3.1**, verified using Maven Central metadata and the
GitHub release. Backfilled #429/#430 fragments record the Java and logging breaks,
so the first modernization batch produces **2.0.0**.

Every task PR must add a **new** valid `.changes/unreleased/*.yaml` fragment.
Editing an existing fragment alone does not satisfy the check. Internal-only work
may instead add a unique `.changes/exemptions/<issue-or-pr>-<description>.yaml`:

```yaml
reason: Internal test or CI maintenance with no user-visible behavior change.
issue: 431
```

Reviewers must confirm that the exemption fits the change. A reason needs at least
20 characters. Exemptions do not suppress validation of malformed fragments and
are not release notes. Generated release PRs on `release/prepare-*` are exempt
only when they add a version note matching the stable POM/Changie version and
change only changelog/version files and removed fragments; source changes still
need a new fragment. CI regenerates release files from the PR base and requires an
exact match, preventing dependency/plugin edits from using this exemption.
CI validates all fragments and rehearses the release tooling.

## Prepare a release PR

Run **Prepare release** manually on `modernization/2.0`. After the final integration,
run it on `master`. The selected branch is the PR base; other branches cannot
execute the job. It checks out the selected commit, runs `changie batch auto` and
`changie merge`, and edits only project versions and parent versions within the
Maven reactor. Dependency/plugin versions and XML formatting are preserved.
It opens or updates one `release/prepare-<base>` branch and PR. No version-impacting
fragments means no new release; a repeated run from the same base regenerates the
same files and updates the same PR. Preparation and publication share a serialized
concurrency group and never cancel running releases.

Configure `RELEASE_PR_TOKEN` as a dedicated fine-grained PAT for this repository,
with Contents and Pull requests read/write. This token is used only by the trusted,
manually dispatched preparation job. Its generated PR events trigger normal CI;
the default `GITHUB_TOKEN` would suppress those events. Do not substitute that
token, bypass required checks, or expose publishing credentials to PR workflows.
All third-party Actions are pinned to full commit SHAs with version comments.

Rehearse locally without opening a PR or publishing, in a disposable checkout:

```sh
.tools/venv/bin/python -m unittest discover -s .github/scripts -p 'test_*.py'
.tools/venv/bin/python .github/scripts/release.py prepare
./mvnw -B -ntp verify
./mvnw -B -ntp -Pfull verify
```

Development stays at a `-SNAPSHOT` project version (currently `2.0.0-SNAPSHOT`).
Preparation changes the release PR to a stable version; it never publishes a
snapshot. Review and merge that PR after **CI required** passes. Tag its **exact
approved commit** as `v<project version>`, e.g. `v2.0.0`; never tag a later snapshot
commit. After tagging, start the next development cycle with the intended next
`-SNAPSHOT` version. New task fragments arriving while a release PR is open require
rerunning preparation and CI before merging it.

## Publication handoff (#433)

The release workflow checks the exact tag's stable POM and Changie versions and
requires an empty fragment queue, then invokes the same Java verification matrix.
The publication job has an unconditional false gate and contains no publishing
command or credential references. Ordinary pushes and PRs cannot reach it.
Task 05 must supply JReleaser, retain dependencies on successful validation and
verification, check that the tag commit belongs to the approved integration line,
and use an appropriately protected release environment for signing/Central
credentials. It must publish artifacts from that exact verified commit and keep
release serialization. The obsolete OSSRH POM profile is retained only for task
05's migration; these workflows never activate it.
