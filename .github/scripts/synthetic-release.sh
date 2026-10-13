#!/usr/bin/env bash
# For disposable checkouts only: apply what a release-please PR changes (POM version, manifest and a
# newest changelog entry), then extract the notes through the same gate as a real release.
set -euo pipefail
version=${1:?synthetic stable version required}
./mvnw -B -ntp org.codehaus.mojo:versions-maven-plugin:2.22.0:set \
  -DnewVersion="$version" -DgenerateBackupPoms=false -DupdateBuildOutputTimestampPolicy=never
printf '{\n  ".": "%s"\n}\n' "$version" > .release-please-manifest.json
mkdir -p target
# Release-please inserts the newest entry above the first version heading.
awk -v entry="## [$version]\n\nNon-publishing rehearsal.\n" '
  !done && /^## \[/ { print entry; done = 1 } { print }
' CHANGELOG.md > target/CHANGELOG.synthetic
mv target/CHANGELOG.synthetic CHANGELOG.md
bash .github/scripts/release-metadata.sh "$version" "v$version"
