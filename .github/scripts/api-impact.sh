#!/usr/bin/env bash
# Prints the release impact (major, minor, patch or none) that release-please derives from a
# Conventional Commit title and body, as used for squash-merged PRs.
set -euo pipefail
title=$1
body=${2:-}
if [[ $title =~ ^[a-z]+(\([^\)]*\))?!: ]] || grep -qE '^BREAKING[ -]CHANGE: ' <<< "$body"; then
  echo major
elif [[ $title =~ ^feat(\([^\)]*\))?: ]]; then
  echo minor
elif [[ $title =~ ^(fix|perf|deps|revert)(\([^\)]*\))?: ]]; then
  echo patch
else
  echo none
fi
