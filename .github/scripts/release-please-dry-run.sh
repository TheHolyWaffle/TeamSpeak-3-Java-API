#!/usr/bin/env bash
# Non-publishing release preparation: installs the release-please version locked by a checkout of the
# pinned Action, runs it in dry-run mode against BRANCH with a read-only token, and checks the release
# PR it would open. Usage: GITHUB_TOKEN=... GITHUB_REPOSITORY=owner/repo release-please-dry-run.sh ACTION_DIR BRANCH
set -euo pipefail
action=$1 branch=$2
log=target/release-please-dry-run.log
fail() { echo "$*" >&2; exit 1; }
npm ci --prefix "$action" --ignore-scripts --omit=dev --no-audit --no-fund
echo "release-please $(jq -r '.packages["node_modules/release-please"].version' "$action/package-lock.json")"
mkdir -p target
"$action/node_modules/.bin/release-please" release-pr --dry-run --token="$GITHUB_TOKEN" \
  --repo-url="$GITHUB_REPOSITORY" --target-branch="$branch" \
  --config-file=release-please-config.json --manifest-file=.release-please-manifest.json > "$log" 2>&1 ||
  { cat "$log" >&2; fail 'release-please dry run failed'; }
if grep -q '✖' "$log"; then cat "$log" >&2; fail 'release-please reported an error'; fi
title=$(sed -n 's/^title: //p' "$log")
if [[ -z $title ]]; then
  echo 'No releasable commits: release preparation would open no PR.'
  exit 0
fi
[[ $title =~ ^chore\(.+\):\ release\ ([0-9]+\.[0-9]+\.[0-9]+)$ ]] || fail "Unexpected release PR title: $title"
release_as=$(jq -r '.packages["."]["release-as"] // empty' release-please-config.json)
[[ -z $release_as || ${BASH_REMATCH[1]} == "$release_as" ]] ||
  fail "Release PR proposes ${BASH_REMATCH[1]}, but release-as is $release_as"
echo "Release preparation would open: $title"

# The proposed changelog entry must pass the same notes gate as the release, without changing source.
version=${BASH_REMATCH[1]}
# The entry runs from its version heading to the footer separator release-please appends.
awk '/^## \[/ { entry = 1 } entry { line[++n] = $0 }
  END { for (i = 1; i < n; i++) if (line[i] == "---" && line[i + 1] ~ /^This PR was generated/) end = i
        for (i = 1; i < end; i++) print line[i] }' "$log" > target/release-please-notes.md
[[ -s target/release-please-notes.md ]] || fail 'The release PR body has no changelog entry'
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
printf '{".": "%s"}\n' "$version" > "$work/manifest.json"
awk -v entry=target/release-please-notes.md '
  !done && /^## \[/ { while ((getline line < entry) > 0) print line; print ""; done = 1 } { print }
' CHANGELOG.md > "$work/CHANGELOG.md"
bash .github/scripts/release-metadata.sh "$version" "v$version" "$work/CHANGELOG.md" "$work/manifest.json" \
  target/release-please-proposed-notes.md || fail 'The proposed release notes fail the release notes gate'
echo "Proposed $version notes: $(LC_ALL=C wc -c < target/release-please-proposed-notes.md | tr -d ' ') of 10000 bytes"
