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
