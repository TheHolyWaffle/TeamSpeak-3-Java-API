#!/usr/bin/env bash
# Validate release-please's stable version and extract only its approved entry.
set -euo pipefail
version=${1:?stable Maven version required}
tag=${2:?version tag required}
changelog=${3:-CHANGELOG.md}
manifest=${4:-.release-please-manifest.json}
output=${5:-target/release-notes.md}
[[ "$version" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]]
[[ "$tag" == "v$version" ]]
jq -e --arg version "$version" '.["."] == $version' "$manifest" >/dev/null
# Require exactly one entry, at the top, then retain its bytes including blank lines.
awk -v version="$version" '
  /^## / {
    if (index($0, "## [" version "]") == 1) matches++
    if (!first++) { if (index($0, "## [" version "]") != 1) exit 1 }
  }
  END { if (matches != 1) exit 1 }
' "$changelog"
mkdir -p "$(dirname "$output")"
awk -v version="$version" '
  /^## / { if (capture) exit; if (index($0, "## [" version "]") == 1) capture=1 }
  capture { print }
' "$changelog" > "$output"
test -s "$output"

# JReleaser 1.26.0 truncates GitHub bodies above 10,000 Java characters.
# A UTF-8 byte bound is conservative and guarantees the approved body stays unchanged.
notes_bytes=$(LC_ALL=C wc -c < "$output" | tr -d '[:space:]')
if (( notes_bytes > 10000 )); then
  echo 'Approved release notes exceed the unchanged GitHub-body limit (10,000 UTF-8 bytes).' >&2
  exit 1
fi
