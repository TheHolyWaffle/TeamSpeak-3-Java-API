#!/usr/bin/env bash
# JReleaser must have resolved exactly the approved version, tag, commit and branch before it may
# tag or release. Usage: release-consistency.sh VERSION TAG COMMIT [OUTPUT_PROPERTIES]
set -euo pipefail
version=${1:?approved version required}
tag=${2:?approved tag required}
commit=${3:?approved commit required}
output=${4:-target/jreleaser/output.properties}
fail() { echo "Release consistency check failed: $*" >&2; exit 1; }
value() { sed -n "s/^$1=//p" "$output"; }
test -s "$output" || fail "$output is missing"
[[ "$(value projectVersion)" == "$version" ]] || fail "JReleaser resolved version $(value projectVersion), not $version"
[[ "$(value tagName)" == "$tag" ]] || fail "JReleaser resolved tag $(value tagName), not $tag"
[[ "$(value commitFullHash)" == "$commit" ]] || fail "JReleaser resolved commit $(value commitFullHash), not $commit"
[[ "$(value releaseBranch)" == master ]] || fail "JReleaser releases from $(value releaseBranch), not master"
echo "JReleaser resolved $version as $tag at $commit"
