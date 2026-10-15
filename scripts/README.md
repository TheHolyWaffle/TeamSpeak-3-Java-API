# Fork audit

Find work in GitHub forks that may be useful for the 2.0.0 release. Requires Python
3.9+, Git, and the GitHub CLI (`gh`) authenticated with read access to the repository
(`gh auth login`). No Python packages are needed.

From the repository root:

```sh
python3 scripts/audit_forks.py
```

The default repository is `TheHolyWaffle/TeamSpeak-3-Java-API`. To audit another
repository or choose output/cache locations:

```sh
python3 scripts/audit_forks.py --repo OWNER/REPO --output build/fork-audit \
    --cache build/fork-audit/objects.git
```

The script reads GitHub and fetches into a separate bare Git repository. It leaves
the working tree and its remotes alone. Rerun the same command to refresh the
reports; the object cache makes subsequent fetches incremental. Use a separate
output directory for each upstream repository. Run only one audit per cache at a
time.

## Outputs

- `build/fork-audit/report.md`: candidates with commit links, authors, changed
  files, source forks/branches, equivalent patches, and a coverage table.
- `build/fork-audit/report.json`: full commit messages, parent hashes, pinned
  upstream and fork branch tips, patch IDs, locations, and scan errors.
- `build/fork-audit/patches/<sha>.patch`: review diffs for every reported commit.
  Normal commits can be applied with `git apply`; merge artifacts use combined
  diffs for conflict-resolution review.
- `build/fork-audit/objects.git`: reusable Git objects for inspecting full diffs
  and parent histories without downloading again.

These outputs live under the already ignored `build/` directory. The JSON and
Markdown reports describe the latest run; patch files from previous runs can
remain in `patches/`. Use the JSON commit list as the current inventory.

Inspect a commit locally:

```sh
git --git-dir=build/fork-audit/objects.git show COMMIT_SHA
```

To bring a selected commit into a development branch after review:

```sh
git fetch ./build/fork-audit/objects.git COMMIT_SHA
git cherry-pick COMMIT_SHA
```

## Coverage and interpretation

Discovery paginates GitHub's forks endpoint and follows nested forks, deduplicating
by repository ID. Archived forks are included. Every advertised branch is fetched,
with full history: a feature branch matters even when its default branch has no
changes. No date or activity cutoff is used. Fork tags, deleted branches, deleted
or inaccessible forks, and commits unreachable from any current branch are not
covered.

All upstream branches and tags form the exclusion baseline. Commits already
reachable upstream disappear from the report. Fork-only commit hashes are listed
once, with every source fork/branch attached. Stable patch IDs identify equivalent
non-merge changes under different hashes, both upstream and in other forks.
Upstream history is walked once for patch IDs, rather than making API calls for
individual commits. Git shares common objects between all forks in the cache.

The report separates:

- **Candidates:** nonempty patches absent from upstream ancestry and patch IDs.
  Multiple candidate commits can represent the same patch; the report links them
  and counts distinct patches separately.
- **Merge commits:** retained for manual review of conflict resolutions.
- **Upstream-equivalent patches:** changes found somewhere in upstream history.
- **Empty commits:** no patch to evaluate.

This is a discovery report, not a compatibility or PR-status check. It retains
commits even if they already have an open/closed PR. Patch IDs cannot reliably
recognize squashed or rewritten implementations. A patch found in history may
have been reverted, or exist only on another upstream branch; verify its behavior
against the current 2.0.0 code before ruling it out. Candidate patches may also be
obsolete, incompatible, or already implemented differently. Review related
commits together and test any port against the current API.

Transient failures are retried. Failed fork fetches or nested discovery calls are
recorded, other forks continue, and the process exits with status 1 for an
incomplete scan. Failure to discover the root forks or fetch upstream aborts the
run. GitHub rate-limit errors are surfaced; rerun after the limit resets. Reports
are replaced only once discovery, fetching, and analysis have finished.

The paginated API is documented in [GitHub's forks REST API reference](https://docs.github.com/en/rest/repos/forks).
Patch matching uses [`git patch-id --stable`](https://git-scm.com/docs/git-patch-id).

## Offline tests

```sh
python3 -m unittest discover -s scripts -p 'test_*.py' -v
```

The tests construct local Git repositories and exercise branch coverage, merge
and empty commits, patch equivalence, deduplication, patch application, pruning,
failed fetches, nested discovery, and API pagination without network access.
