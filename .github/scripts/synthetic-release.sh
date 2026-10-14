#!/usr/bin/env bash
# For disposable checkouts only: apply what a release-please PR changes (POM version, manifest and a
# newest changelog entry), then extract the notes through the same gate as a real release.
# NOTES optionally holds a complete entry, such as release-please-dry-run.sh's proposed notes.
# Usage: synthetic-release.sh VERSION [NOTES]
set -euo pipefail
version=${1:?synthetic stable version required}
notes=${2:-}
./mvnw -B -ntp versions:set \
  -DnewVersion="$version" -DgenerateBackupPoms=false -DupdateBuildOutputTimestampPolicy=never
printf '{\n  ".": "%s"\n}\n' "$version" > .release-please-manifest.json
mkdir -p target
if [[ -z $notes ]]; then
  notes=target/synthetic-notes.md
  printf '## [%s]\n\nNon-publishing rehearsal.\n' "$version" > "$notes"
fi
# Release-please inserts the newest entry above the first version heading.
awk -v entry="$notes" '
  !done && /^## \[/ { while ((getline line < entry) > 0) print line; print ""; done = 1 } { print }
' CHANGELOG.md > target/CHANGELOG.synthetic
mv target/CHANGELOG.synthetic CHANGELOG.md
bash .github/scripts/release-metadata.sh "$version" "v$version"
