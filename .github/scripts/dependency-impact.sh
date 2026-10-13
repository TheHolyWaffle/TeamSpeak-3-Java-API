#!/usr/bin/env bash
# Rejects a PR title whose release impact disagrees with its resolved runtime dependencies: a change
# must ship in a release (deps:, fix:, feat: or breaking) and deps: needs a change.
# Usage: dependency-impact.sh BASE_LIST HEAD_LIST TITLE [BODY], with lists from dependency:list.
set -euo pipefail
base=$1 head=$2 title=$3 body=${4:-}
impact=$(bash "$(dirname "$0")/api-impact.sh" "$title" "$body")
artifacts() { sed -n 's/^ \{1,\}\([^ ]*\).*/\1/p' "$1" | sort; }
if diff <(artifacts "$base") <(artifacts "$head"); then
  if [[ $title =~ ^deps(\([^\)]*\))?!?: ]]; then
    echo 'No runtime dependency changed: use build:, test: or ci: instead of deps:' >&2
    exit 1
  fi
elif [[ $impact == none ]]; then
  echo 'Runtime dependencies changed: use deps: (or fix:, feat:, a breaking change) so a release ships them' >&2
  exit 1
fi
