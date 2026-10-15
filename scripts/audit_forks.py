#!/usr/bin/env python3
"""Collect GitHub fork-only commits using gh, Git, and Python's standard library."""

import argparse
from collections import deque
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time


DEFAULT_REPO = "TheHolyWaffle/TeamSpeak-3-Java-API"


def run(*args, input=None, timeout=180):
    result = subprocess.run(args, input=input, capture_output=True, text=True,
                            encoding="utf-8", errors="replace", timeout=timeout,
                            env={**os.environ, "GIT_TERMINAL_PROMPT": "0"})
    if result.returncode:
        raise RuntimeError(f"{args[0]} failed: {result.stderr.strip()}")
    return result.stdout


def retry(function):
    for attempt in range(3):
        try:
            return function()
        except (RuntimeError, subprocess.TimeoutExpired):
            if attempt == 2:
                raise
            time.sleep(2 ** attempt)


def api(endpoint, paginate=False):
    args = ["gh", "api", "--hostname", "github.com", endpoint]
    if paginate:
        args += ["--paginate", "--slurp"]
    data = json.loads(retry(lambda: run(*args)))
    return [item for page in data for item in page] if paginate else data


def discover(repo):
    """Traverse nested forks too; IDs deduplicate overlapping API results."""
    root = api(f"repos/{repo}")
    queue = deque([root])
    seen = {root["id"]}
    forks, errors = [], []
    while queue:
        parent = queue.popleft()
        try:
            children = api(f"repos/{parent['full_name']}/forks?per_page=100&sort=oldest", True)
        except (RuntimeError, subprocess.TimeoutExpired) as error:
            if parent["id"] == root["id"]:
                raise
            errors.append({"repo": parent["full_name"], "stage": "discovery", "error": str(error)})
            continue
        for child in children:
            if child["id"] in seen:
                continue
            seen.add(child["id"])
            forks.append(child)
            if child["forks_count"]:
                queue.append(child)
    return root, forks, errors


class Store:
    def __init__(self, path):
        self.path = Path(path).resolve()
        if not self.path.exists():
            run("git", "init", "--bare", str(self.path))
        if self.git("rev-parse", "--is-bare-repository").strip() != "true":
            raise RuntimeError(f"Cache must be a bare repository: {self.path}")

    def git(self, *args, input=None):
        return run("git", "-c", "color.ui=false", "--git-dir", str(self.path), *args, input=input)

    def fetch(self, url, namespace, tags=False):
        specs = [f"+refs/heads/*:refs/{namespace}/heads/*"]
        if tags:
            specs.append(f"+refs/tags/*:refs/{namespace}/tags/*")
        retry(lambda: self.git("fetch", "--quiet", "--prune", "--no-tags", "--no-write-fetch-head",
                               url, *specs))

    def refs(self, namespace):
        rows = self.git("for-each-ref", "--format=%(refname) %(objectname)", f"refs/{namespace}/")
        return dict(row.split(" ", 1) for row in rows.splitlines())

    def unique(self, heads, upstream):
        if not heads:
            return set()
        return set(self.git("rev-list", "--stdin", input="\n".join(
            [*heads, *("^" + ref for ref in upstream)]) + "\n").splitlines())

    def patch_ids(self, revisions, walk=True):
        if not revisions:
            return {}
        args = ["log", "--no-merges", "--root", "--format=commit %H", "--patch",
                "--no-ext-diff", "--no-textconv", "--no-renames", "--binary", "--stdin"]
        if not walk:
            args.append("--no-walk=unsorted")
        patches = self.git(*args, input="\n".join(revisions) + "\n")
        return {sha: patch for patch, sha in (
            row.split() for row in run("git", "patch-id", "--stable", input=patches).splitlines())}

    def describe(self, sha):
        info = self.git("show", "-s", "--format=%an%x00%aI%x00%cI%x00%P%x00%B", sha)
        author, authored, committed, parents, message = info.split("\0", 4)
        files = self.git("diff-tree", "--root", "--no-commit-id", "--name-only", "-r", "-m", "-z", sha)
        return {"sha": sha, "author": author, "authored_at": authored, "committed_at": committed,
                "parents": parents.split(), "message": message.strip(),
                "subject": message.strip().split("\n", 1)[0],
                "files": sorted(set(filter(None, files.split("\0"))))}


def collect(store, forks, upstream, errors):
    locations, inventory = {}, []
    for index, fork in enumerate(forks, 1):
        name = fork["full_name"]
        print(f"[{index}/{len(forks)}] {name}", file=sys.stderr, flush=True)
        namespace = f"forks/{fork['id']}"
        entry = {"repo": name, "url": fork["html_url"], "archived": fork["archived"],
                 "pushed_at": fork["pushed_at"], "branches": {}, "status": "ok"}
        inventory.append(entry)
        try:
            store.fetch(fork["clone_url"], namespace)
            refs = store.refs(namespace)
            # Group identical branch tips to avoid walking the same history repeatedly.
            tips = {}
            for ref, tip in refs.items():
                branch = ref.split("/heads/", 1)[1]
                entry["branches"][branch] = tip
                tips.setdefault(tip, []).append(branch)
            for tip, branches in tips.items():
                for sha in store.unique([tip], upstream):
                    locations.setdefault(sha, []).append({"repo": name, "branches": sorted(branches),
                                                         "url": f"{fork['html_url']}/commit/{sha}"})
        except (RuntimeError, subprocess.TimeoutExpired) as error:
            entry["status"] = "error"
            errors.append({"repo": name, "stage": "fetch/analyze", "error": str(error)})
    return locations, inventory


def enrich(store, locations, upstream):
    upstream_patches = {}
    for sha, patch in store.patch_ids(upstream).items():
        upstream_patches.setdefault(patch, []).append(sha)
    candidate_patches = store.patch_ids(list(locations), walk=False)
    groups, commits = {}, []
    for sha in sorted(locations):
        commit = store.describe(sha)
        patch = candidate_patches.get(sha)
        commit.update({"locations": sorted(locations[sha], key=lambda item: item["repo"]),
                       "patch_id": patch, "equivalent_upstream": upstream_patches.get(patch, [])})
        if len(commit["parents"]) > 1:
            commit["status"] = "merge_review"
        elif commit["equivalent_upstream"]:
            commit["status"] = "upstream_equivalent"
        elif patch:
            commit["status"] = "candidate"
        else:
            commit["status"] = "empty"
        groups.setdefault(patch or sha, []).append(sha)
        commits.append(commit)
    for commit in commits:
        commit["equivalent_fork_commits"] = [sha for sha in groups[commit["patch_id"] or commit["sha"]]
                                              if sha != commit["sha"]]
    return sorted(commits, key=lambda commit: (commit["committed_at"], commit["sha"]), reverse=True)


def md(value):
    return re.sub(r"([\\`*_{}\[\]<>|])", r"\\\1", str(value)).replace("\n", " ")


def write_report(output, report, store):
    output.mkdir(parents=True, exist_ok=True)
    (output / "report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    commits = report["commits"]
    candidates = [c for c in commits if c["status"] == "candidate"]
    lines = [f"# Fork audit: {report['repo']}", "", f"Generated: {report['generated_at']}", "",
             f"Forks discovered: {len(report['forks'])}; errors: {len(report['errors'])}.",
             f"Unique fork-only commits: {len(commits)}; candidate commits: {len(candidates)}; "
             f"distinct candidate patches: {len({c['patch_id'] for c in candidates})}.", "",
             "Baseline: all upstream branches and tags, pinned in report.json. "
             "Candidate means absent by ancestry and stable patch ID; it is not a recommendation.", "",
             "Patch equivalence does not prove the feature is present in 2.0.0. Squashes, rewrites, "
             "and reverts require manual checking. Merge commits need separate conflict-resolution review.", ""]
    lines += ["## Forks with candidate patches", "", "Counts overlap when forks share work.", "",
              "| Fork | Distinct candidate patches |", "| --- | ---: |"]
    fork_patches = {}
    for commit in candidates:
        for location in commit["locations"]:
            fork_patches.setdefault(location["repo"], set()).add(commit["patch_id"])
    for name, patches in sorted(fork_patches.items(), key=lambda item: (-len(item[1]), item[0])):
        lines.append(f"| [{md(name)}](https://github.com/{name}) | {len(patches)} |")
    lines += [""]
    for status, title in [("candidate", "Candidates for 2.0.0 review"), ("merge_review", "Merge commits to review"),
                          ("upstream_equivalent", "Patch already found upstream"), ("empty", "Empty commits")]:
        lines += [f"## {title}", ""]
        for commit in (c for c in commits if c["status"] == status):
            sha = commit["sha"]
            lines += [f"### [{sha[:12]}]({commit['locations'][0]['url']}) {md(commit['subject'])}", "",
                      f"{md(commit['author'])} — {commit['authored_at']}", "",
                      "Files: " + (", ".join(md(f) for f in commit["files"]) or "(none)"), ""]
            for location in commit["locations"]:
                lines.append(f"- [{md(location['repo'])}]({location['url']}): "
                             + ", ".join(md(b) for b in location["branches"]))
            if commit["equivalent_upstream"]:
                lines += ["", "Equivalent upstream: " + ", ".join(
                    f"[{s[:12]}](https://github.com/{report['repo']}/commit/{s})"
                    for s in commit["equivalent_upstream"])]
            if commit["equivalent_fork_commits"]:
                lines += ["", "Same patch in: " + ", ".join(s[:12] for s in commit["equivalent_fork_commits"])]
            lines += [""]
    lines += ["## Coverage", "", "| Fork | Branches | Status |", "| --- | ---: | --- |"]
    for fork in report["forks"]:
        lines.append(f"| [{md(fork['repo'])}]({fork['url']}) | {len(fork['branches'])} | {fork['status']} |")
    if report["errors"]:
        lines += ["", "## Errors (incomplete scan)", ""]
        lines += [f"- {md(e['repo'])}: {md(e['stage'])}: {md(e['error'])}" for e in report["errors"]]
    (output / "report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    patches = output / "patches"
    patches.mkdir(exist_ok=True)
    for commit in commits:
        # git show preserves merge evidence with combined diffs; these are review artifacts.
        patch = store.git("show", "--root", "--format=fuller", "--binary", "--no-ext-diff",
                          "--no-textconv", "--cc", commit["sha"])
        (patches / f"{commit['sha']}.patch").write_text(patch, encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", default=DEFAULT_REPO, help="GitHub OWNER/REPO")
    parser.add_argument("--output", type=Path, default=Path("build/fork-audit"))
    parser.add_argument("--cache", type=Path, help="Reusable bare Git repository (default: OUTPUT/objects.git)")
    args = parser.parse_args()
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", args.repo):
        parser.error("--repo must be OWNER/REPO")
    args.output = args.output.resolve()
    args.output.mkdir(parents=True, exist_ok=True)
    store = Store(args.cache or args.output / "objects.git")
    # A cache is tied to one upstream; fail rather than mixing repositories.
    identity = store.path / "audit-repository"
    if identity.exists() and identity.read_text().strip().lower() != args.repo.lower():
        raise RuntimeError("Cache belongs to another repository; choose a different --cache")
    identity.write_text(args.repo + "\n", encoding="utf-8")
    print(f"Discovering forks of {args.repo}...", file=sys.stderr, flush=True)
    root, forks, errors = discover(args.repo)
    store.fetch(root["clone_url"], "upstream", tags=True)
    upstream = store.refs("upstream")
    if not upstream:
        raise RuntimeError("No upstream refs found")
    locations, inventory = collect(store, forks, list(upstream), errors)
    print(f"Analyzing {len(locations)} unique fork-only commits...", file=sys.stderr, flush=True)
    report = {"repo": root["full_name"], "generated_at": datetime.now(timezone.utc).isoformat(),
              "upstream_refs": upstream, "forks": inventory, "errors": errors,
              "commits": enrich(store, locations, list(upstream))}
    write_report(args.output, report, store)
    print(f"Report: {args.output / 'report.md'}", flush=True)
    return 1 if errors else 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (RuntimeError, OSError, subprocess.TimeoutExpired) as error:
        print(f"error: {error}", file=sys.stderr)
        sys.exit(1)
