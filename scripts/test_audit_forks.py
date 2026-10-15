"""Offline integration tests: real Git histories, no GitHub credentials needed."""

import contextlib
import io
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import audit_forks as audit


class ForkAuditTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.upstream = self.root / "upstream"
        audit.run("git", "init", "-b", "main", str(self.upstream))
        self.git(self.upstream, "config", "user.email", "test@example.invalid")
        self.git(self.upstream, "config", "user.name", "Test Author")
        self.base = self.commit(self.upstream, "base.txt", "base\n", "Initial commit")
        self.fork = self.root / "fork"
        audit.run("git", "clone", "--quiet", str(self.upstream), str(self.fork))
        self.git(self.fork, "config", "user.email", "test@example.invalid")
        self.git(self.fork, "config", "user.name", "Test Author")
        self.store = audit.Store(self.root / "cache.git")

    def git(self, repo, *args):
        return audit.run("git", "-C", str(repo), *args).strip()

    def commit(self, repo, file, contents, subject):
        (repo / file).write_text(contents)
        self.git(repo, "add", file)
        self.git(repo, "commit", "--quiet", "-m", subject)
        return self.git(repo, "rev-parse", "HEAD")

    def fork_data(self, repo_id=1):
        return {"id": repo_id, "full_name": f"test/fork{repo_id}", "clone_url": str(self.fork),
                "html_url": f"https://github.com/test/fork{repo_id}", "archived": False,
                "pushed_at": "2026-01-01T00:00:00Z"}

    def test_all_branches_dedup_patch_equivalence_merges_and_export(self):
        upstream_patch = self.commit(self.upstream, "fix.txt", "fix\n", "Upstream fix")
        self.git(self.fork, "switch", "-c", "feature/old")
        equivalent = self.commit(self.fork, "fix.txt", "fix\n", "Same fix different hash")
        self.git(self.fork, "switch", "-c", "feature/new", self.base)
        candidate = self.commit(self.fork, "feature.txt", "feature\n", "New API")
        self.git(self.fork, "commit", "--quiet", "--allow-empty", "-m", "Empty metadata")
        empty = self.git(self.fork, "rev-parse", "HEAD")
        self.git(self.fork, "merge", "--quiet", "--no-ff", "feature/old", "-m", "Merge feature")
        merge = self.git(self.fork, "rev-parse", "HEAD")
        self.git(self.fork, "switch", "-c", "duplicate", self.base)
        duplicate = self.commit(self.fork, "feature.txt", "feature\n", "Same API different hash")
        self.store.fetch(str(self.upstream), "upstream", tags=True)
        upstream = self.store.refs("upstream")
        errors = []
        with contextlib.redirect_stderr(io.StringIO()):
            locations, inventory = audit.collect(self.store, [self.fork_data(), self.fork_data(2)],
                                                  list(upstream), errors)
        self.assertEqual(errors, [])
        self.assertNotIn(self.base, locations)
        self.assertEqual(len(locations[candidate]), 2)
        self.assertEqual(inventory[0]["branches"]["feature/new"], merge)
        commits = audit.enrich(self.store, locations, list(upstream))
        by_sha = {c["sha"]: c for c in commits}
        self.assertEqual(by_sha[equivalent]["status"], "upstream_equivalent")
        self.assertIn(upstream_patch, by_sha[equivalent]["equivalent_upstream"])
        self.assertEqual(by_sha[candidate]["status"], "candidate")
        self.assertIn(duplicate, by_sha[candidate]["equivalent_fork_commits"])
        self.assertEqual(by_sha[empty]["status"], "empty")
        self.assertEqual(by_sha[merge]["status"], "merge_review")
        self.assertEqual(by_sha[candidate]["files"], ["feature.txt"])
        report = {"repo": "test/upstream", "generated_at": "now", "forks": inventory,
                  "errors": errors, "commits": commits}
        output = self.root / "report"
        audit.write_report(output, report, self.store)
        self.assertTrue((output / "report.json").exists())
        self.assertIn("New API", (output / "report.md").read_text())
        # A non-merge exported patch is usable by Git, not just a message summary.
        self.git(self.fork, "switch", "--detach", self.base)
        self.git(self.fork, "apply", "--check", str(output / "patches" / f"{candidate}.patch"))

    def test_deleted_branch_pruned_and_failed_fork_reported(self):
        self.git(self.fork, "branch", "temporary")
        self.store.fetch(str(self.fork), "forks/1")
        self.git(self.fork, "branch", "-D", "temporary")
        self.store.fetch(str(self.fork), "forks/1")
        self.assertFalse(any(ref.endswith("/temporary") for ref in self.store.refs("forks/1")))
        with patch.object(self.store, "fetch", side_effect=RuntimeError("unavailable")):
            errors = []
            with contextlib.redirect_stderr(io.StringIO()):
                locations, inventory = audit.collect(self.store, [self.fork_data()], [], errors)
        self.assertEqual(locations, {})
        self.assertEqual(inventory[0]["status"], "error")
        self.assertEqual(len(errors), 1)

    def test_nested_forks_and_pagination(self):
        root = {"id": 0, "full_name": "test/upstream"}
        first = {"id": 1, "full_name": "test/first", "forks_count": 1}
        nested = {"id": 2, "full_name": "test/nested", "forks_count": 0}
        with patch.object(audit, "api", side_effect=[root, [first], [first, nested]]):
            _, forks, errors = audit.discover("test/upstream")
        self.assertEqual([f["id"] for f in forks], [1, 2])
        self.assertEqual(errors, [])
        with patch.object(audit, "run", return_value='[[{"id":1}],[{"id":2}]]') as command:
            self.assertEqual(audit.api("repos/test/upstream/forks", True), [{"id": 1}, {"id": 2}])
            self.assertIn("--paginate", command.call_args.args)


if __name__ == "__main__":
    unittest.main()
