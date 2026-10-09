"""Non-publishing rehearsal of fragments, aggregation, reactor edits and release PRs."""
import contextlib
import importlib.util
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

import yaml

SCRIPT = Path(__file__).resolve().parent / "release.py"
REPO = SCRIPT.parent.parent.parent
spec = importlib.util.spec_from_file_location("release", SCRIPT)
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class ReleaseTests(unittest.TestCase):
    def setUp(self):
        self.original = Path.cwd()
        self.temp = tempfile.TemporaryDirectory()
        os.chdir(self.temp.name)
        Path(".changes/unreleased").mkdir(parents=True)
        Path(".changes/unreleased/.gitkeep").touch()
        shutil.copy(REPO / ".changie.yaml", ".changie.yaml")
        shutil.copy(REPO / ".changes/header.md", ".changes/header.md")
        Path(".changes/v2.0.0.md").write_text("## v2.0.0\n\nPrevious release.\n")
        Path("pom.xml").write_text('''<project xmlns="http://maven.apache.org/POM/4.0.0">
<modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>api</artifactId>
<version>2.0.1-SNAPSHOT</version><dependencies><dependency><groupId>example</groupId>
<artifactId>dependency</artifactId><version>9.8.7</version></dependency></dependencies>
<build><plugins><plugin><artifactId>plugin</artifactId><version>6.5.4</version></plugin></plugins></build>
</project>''')
        self.tool_env = contextlib.ExitStack()
        from unittest.mock import patch
        self.tool_env.enter_context(patch.dict(os.environ, {"CHANGIE": str(REPO / ".tools/changie")}))

    def tearDown(self):
        self.tool_env.close()
        os.chdir(self.original)
        self.temp.cleanup()

    def fragment(self, kind, filename="change"):
        path = Path(f".changes/unreleased/{filename}.yaml")
        path.write_text(yaml.safe_dump({"kind": kind, "body": "Describe the user-facing change", "time": "2026-10-09T00:00:00Z", "custom": {"Issue": "431"}}))
        return path

    def git(self, *args):
        return release.run("git", *args)

    def init_git(self):
        self.git("init", "-q")
        self.git("config", "user.name", "Release rehearsal")
        self.git("config", "user.email", "rehearsal@example.invalid")
        return self.commit()

    def commit(self):
        self.git("add", ".")
        self.git("commit", "-qm", "Rehearsal")
        return self.git("rev-parse", "HEAD")

    def test_auto_aggregation(self):
        for kinds, expected in [(["fixed"], "2.0.1"), (["internal", "added", "fixed"], "2.1.0"), (["breaking", "added", "fixed"], "3.0.0")]:
            with self.subTest(kinds=kinds):
                for path in Path(".changes/unreleased").glob("*.yaml"):
                    path.unlink()
                for i, kind in enumerate(kinds):
                    self.fragment(kind, str(i))
                self.assertEqual(release.changie("next", "auto"), "v" + expected)

    def test_none_does_not_prepare_release(self):
        self.fragment("internal")
        before = Path("pom.xml").read_bytes()
        release.prepare()
        self.assertEqual(Path("pom.xml").read_bytes(), before)
        self.assertEqual(len(release.fragments()), 1)

    def test_prepare_release_pr_and_tag(self):
        self.fragment("fixed")
        base = self.init_git()
        release.prepare()
        pom = Path("pom.xml").read_text()
        self.assertIn("<version>2.0.1</version>", pom)
        self.assertIn("<version>9.8.7</version>", pom)
        self.assertIn("<version>6.5.4</version>", pom)
        self.assertIn("Describe the user-facing change", Path("CHANGELOG.md").read_text())
        self.commit()
        release.validate(base, "release/prepare-modernization/2.0")
        release.check_tag("v2.0.1")
        with self.assertRaises(ValueError):
            release.check_tag("v2.0.2")
        before = Path("CHANGELOG.md").read_bytes()
        release.prepare()
        self.assertEqual(Path("CHANGELOG.md").read_bytes(), before)
        # Generated-PR exemption must not hide dependency or plugin edits.
        original_pom = Path("pom.xml").read_text()
        Path("pom.xml").write_text(original_pom.replace("9.8.7", "9.8.8"))
        self.commit()
        with self.assertRaises(ValueError):
            release.validate(base, "release/prepare-modernization/2.0")
        Path("pom.xml").write_text(original_pom)
        # Generated-PR exemption must not hide source changes.
        Path("source.java").write_text("class Changed {}")
        self.commit()
        with self.assertRaises(ValueError):
            release.validate(base, "release/prepare-modernization/2.0")

    def test_first_modernization_release(self):
        Path(".changes/v2.0.0.md").unlink()
        shutil.copy(REPO / ".changes/v1.3.1.md", ".changes/v1.3.1.md")
        self.fragment("fixed")
        with self.assertRaises(ValueError):
            release.prepare()
        self.fragment("breaking")
        release.prepare()
        self.assertEqual(release.project_version(), "2.0.0")

    def test_missing_valid_and_malformed_intent(self):
        base = self.init_git()
        Path("source.java").write_text("class Changed {}")
        self.commit()
        with self.assertRaises(ValueError):
            release.validate(base)
        path = self.fragment("fixed")
        self.commit()
        release.validate(base)
        for invalid in ["unknown", "", None]:
            value = yaml.safe_load(path.read_text())
            value["kind"] = invalid
            path.write_text(yaml.safe_dump(value))
            with self.assertRaises(ValueError):
                release.validate(base)
        path.write_text("kind: [broken YAML")
        with self.assertRaises(yaml.YAMLError):
            release.validate(base)

    def test_explicit_internal_exemption(self):
        base = self.init_git()
        Path(".changes/exemptions").mkdir()
        path = Path(".changes/exemptions/431.yaml")
        path.write_text('reason: "Internal CI administration, no runtime or API change"\nissue: 431\n')
        self.commit()
        release.validate(base)
        # Exemption cannot conceal malformed fragments.
        self.fragment("wrong")
        with self.assertRaises(ValueError):
            release.validate(base)

    def test_modules_and_parent_versions(self):
        root = Path("pom.xml")
        root.write_text(root.read_text().replace("</project>", "<modules><module>child</module></modules></project>"))
        Path("child").mkdir()
        Path("child/pom.xml").write_text('''<project xmlns="http://maven.apache.org/POM/4.0.0">
<modelVersion>4.0.0</modelVersion><parent><groupId>example</groupId><artifactId>api</artifactId>
<version>2.0.1-SNAPSHOT</version></parent><artifactId>child</artifactId><version>2.0.1-SNAPSHOT</version>
<dependencies><dependency><version>1.2.3</version></dependency></dependencies></project>''')
        release.set_version("2.1.0")
        child = Path("child/pom.xml").read_text()
        self.assertEqual(child.count("<version>2.1.0</version>"), 2)
        self.assertIn("<version>1.2.3</version>", child)

    def test_duplicate_keys_and_bad_exemptions(self):
        path = self.fragment("fixed")
        path.write_text(path.read_text() + "kind: breaking\n")
        with self.assertRaises(ValueError):
            release.validate()
        self.fragment("fixed")
        Path(".changes/exemptions").mkdir()
        Path(".changes/exemptions/bad.yaml").write_text("reason: short\nissue: 431\n")
        with self.assertRaises(ValueError):
            release.validate()

    def test_snapshot_tag_is_rejected(self):
        with self.assertRaises(ValueError):
            release.check_tag("v2.0.1-SNAPSHOT")


if __name__ == "__main__":
    unittest.main()
