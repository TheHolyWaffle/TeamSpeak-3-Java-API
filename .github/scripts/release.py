#!/usr/bin/env python3
"""Validate release intent and prepare deterministic, non-publishing releases."""
import argparse
import datetime
import io
import os
from pathlib import Path
import re
import subprocess
import tarfile
import tempfile
import xml.etree.ElementTree as ET
import xml.parsers.expat

import yaml


class UniqueKeyLoader(yaml.SafeLoader):
    """Reject ambiguous YAML rather than silently discarding duplicate fields."""
    def construct_mapping(self, node, deep=False):
        mapping = {}
        for key_node, value_node in node.value:
            key = self.construct_object(key_node, deep=deep)
            if key in mapping:
                raise ValueError(f"Duplicate YAML key: {key}")
            mapping[key] = self.construct_object(value_node, deep=deep)
        return mapping


def load_yaml(path):
    return yaml.load(path.read_text(), Loader=UniqueKeyLoader)


KINDS = {"breaking": "major", "added": "minor", "fixed": "patch", "security": "patch", "internal": "none"}
NS = {"m": "http://maven.apache.org/POM/4.0.0"}
VERSION = re.compile(r"[0-9]+\.[0-9]+\.[0-9]+")


def run(*args):
    return subprocess.check_output(args, text=True).strip()


def changie(*args):
    return run(os.environ.get("CHANGIE", ".tools/changie"), *args)


def fragments():
    directory = Path(".changes/unreleased")
    for path in directory.iterdir():
        if path.name != ".gitkeep" and (path.suffix != ".yaml" or not path.is_file() or path.is_symlink()):
            raise ValueError(f"{path}: fragments must be regular .yaml files")
    configured = load_yaml(Path(".changie.yaml"))
    if {kind["key"]: kind["auto"] for kind in configured["kinds"]} != KINDS:
        raise ValueError("Changie category impacts must agree with the validator")
    files = sorted(directory.glob("*.yaml"))
    for path in files:
        value = load_yaml(path)
        if not isinstance(value, dict) or set(value) != {"kind", "body", "time", "custom"}:
            raise ValueError(f"{path}: expected kind, body, time, custom")
        if not isinstance(value["kind"], str) or value["kind"] not in KINDS:
            raise ValueError(f"{path}: invalid kind")
        if not isinstance(value["body"], str) or len(value["body"].strip()) < 10:
            raise ValueError(f"{path}: provide a user-facing description of at least 10 characters")
        stamp = value["time"]
        if isinstance(stamp, str):
            stamp = datetime.datetime.fromisoformat(stamp.replace("Z", "+00:00"))
        if not isinstance(stamp, datetime.datetime) or stamp.tzinfo is None:
            raise ValueError(f"{path}: time must be an ISO timestamp with timezone")
        if not isinstance(value["custom"], dict) or set(value["custom"]) != {"Issue"}:
            raise ValueError(f"{path}: provide custom.Issue")
        if not re.fullmatch(r"[1-9][0-9]*", str(value["custom"]["Issue"])):
            raise ValueError(f"{path}: Issue must be a positive issue or PR number")
    return files


def poms(path=Path("pom.xml")):
    root = ET.parse(path).getroot()
    yield path, root
    for module in root.findall("m:modules/m:module", NS):
        child = (path.parent / module.text / "pom.xml").resolve()
        if not child.is_relative_to(Path.cwd().resolve()):
            raise ValueError("Maven modules must be inside the repository")
        yield from poms(child.relative_to(Path.cwd().resolve()))


def project_version():
    return ET.parse("pom.xml").getroot().findtext("m:version", namespaces=NS)


def set_version(version):
    """Edit only reactor project versions and reactor parent versions; retain XML formatting."""
    projects = list(poms())
    coordinates = {(root.findtext("m:groupId", namespaces=NS) or root.findtext("m:parent/m:groupId", namespaces=NS),
                    root.findtext("m:artifactId", namespaces=NS)) for _, root in projects}
    for path, root in projects:
        parent = (root.findtext("m:parent/m:groupId", namespaces=NS), root.findtext("m:parent/m:artifactId", namespaces=NS))
        targets = [("project", "version")]
        if parent in coordinates:
            targets.append(("project", "parent", "version"))
        data = path.read_bytes()
        parser = xml.parsers.expat.ParserCreate()
        stack, edits = [], []
        start = None

        def opening(name, _attrs):
            nonlocal start
            stack.append(name)
            if tuple(stack) in targets:
                start = data.index(b">", parser.CurrentByteIndex) + 1

        def closing(_name):
            if tuple(stack) in targets:
                edits.append((start, parser.CurrentByteIndex))
            stack.pop()

        parser.StartElementHandler = opening
        parser.EndElementHandler = closing
        parser.Parse(data, True)
        for begin, end in reversed(edits):
            data = data[:begin] + version.encode() + data[end:]
        path.write_bytes(data)


def validate(base=None, head_branch=""):
    files = fragments()  # Always validate all fragments, including exempt PRs.
    exemptions = set()
    for path in Path(".changes/exemptions").glob("*.yaml"):
        value = load_yaml(path)
        if (path.is_symlink() or not isinstance(value, dict) or set(value) != {"reason", "issue"}
                or not isinstance(value["reason"], str) or len(value["reason"].strip()) < 20
                or not re.fullmatch(r"[1-9][0-9]*", str(value["issue"]))):
            raise ValueError(f"{path}: exemption needs a reason (20+ characters) and issue/PR number")
        exemptions.add(str(path))
    if not base:
        return
    changes = run("git", "diff", "--name-status", "--no-renames", f"{base}...HEAD").splitlines()
    added = {line[2:] for line in changes if line.startswith("A\t")}
    if added.intersection(str(path) for path in files):
        return
    if added.intersection(exemptions):
        return
    if head_branch.startswith("release/prepare-"):
        version = project_version()
        note = f".changes/v{version}.md"
        allowed = {"CHANGELOG.md", note} | {str(path) for path, _ in poms()}
        if (version and VERSION.fullmatch(version) and changie("latest") == f"v{version}"
                and note in added and all(line[2:] in allowed or
                    (line.startswith("D\t.changes/unreleased/") and line.endswith(".yaml")) for line in changes)):
            validate_generated(base)
            return
    raise ValueError("Add a new valid Changie fragment, or a documented internal-only exemption")


def validate_generated(base):
    """A generated PR must match preparation from its base, including dependency versions."""
    original = Path.cwd()
    binary = Path(os.environ.get("CHANGIE", ".tools/changie")).resolve()
    prior_binary = os.environ.get("CHANGIE")
    archive = subprocess.check_output(["git", "archive", base])
    with tempfile.TemporaryDirectory() as temporary:
        with tarfile.open(fileobj=io.BytesIO(archive)) as source:
            source.extractall(temporary, filter="data")
        try:
            os.chdir(temporary)
            os.environ["CHANGIE"] = str(binary)
            prepare()
            expected = {str(path): path.read_bytes() for path in Path(".changes").rglob("*") if path.is_file()}
            expected.update({str(path): path.read_bytes() for path, _ in poms()})
            expected["CHANGELOG.md"] = Path("CHANGELOG.md").read_bytes()
        finally:
            os.chdir(original)
            if prior_binary is None:
                os.environ.pop("CHANGIE", None)
            else:
                os.environ["CHANGIE"] = prior_binary
    actual = {str(path): path.read_bytes() for path in Path(".changes").rglob("*") if path.is_file()}
    actual.update({str(path): path.read_bytes() for path, _ in poms()})
    actual["CHANGELOG.md"] = Path("CHANGELOG.md").read_bytes()
    if actual != expected:
        raise ValueError("Generated release PR must exactly match preparation from its base; rerun preparation")


def prepare():
    files = fragments()
    if not files or all(KINDS[load_yaml(path)["kind"]] == "none" for path in files):
        print("No version-impacting changes; no release PR needed")
        return
    previous = changie("latest")
    next_version = changie("next", "auto").removeprefix("v")
    if previous == "v1.3.1" and next_version != "2.0.0":
        raise ValueError("The first modernization release must be 2.0.0; include a breaking fragment")
    changie("batch", "auto")
    changie("merge")
    version = changie("latest").removeprefix("v")
    if not VERSION.fullmatch(version):
        raise ValueError("Release version must be stable SemVer")
    set_version(version)
    print(f"Prepared {version}; review and merge the release PR before tagging v{version}")


def check_tag(tag):
    version = project_version()
    if not version or not VERSION.fullmatch(version) or tag != f"v{version}":
        raise ValueError("Tag must be v<stable POM version>; snapshots cannot be published")
    if changie("latest") != tag or not Path(f".changes/{tag}.md").is_file():
        raise ValueError("Tag must match the approved Changie release")
    if fragments():
        raise ValueError("Release commit must have no unbatched fragments")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=["validate", "prepare", "check-tag"])
    parser.add_argument("--base")
    parser.add_argument("--head-branch", default="")
    parser.add_argument("--tag")
    args = parser.parse_args()
    if args.command == "validate":
        validate(args.base, args.head_branch)
    elif args.command == "prepare":
        prepare()
    else:
        check_tag(args.tag)

if __name__ == "__main__":
    main()
