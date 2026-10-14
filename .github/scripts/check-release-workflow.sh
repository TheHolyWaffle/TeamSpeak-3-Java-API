#!/usr/bin/env bash
# Lint every workflow, then require the release provenance wiring without running it: only the
# publish job of release.yml may attest, with least privilege, after the staged artifacts are
# verified and before anything is signed or deployed.
set -euo pipefail
workflows=${1:-.github/workflows}
fail() { echo "Release workflow check failed: $*" >&2; exit 1; }
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
# WORKFLOW_TOOLS may name a directory already filled by workflow-tools.sh.
tools=${WORKFLOW_TOOLS:-$work}
[[ -n ${WORKFLOW_TOOLS:-} ]] || bash "$(dirname "$0")/workflow-tools.sh" "$tools"

"$tools/actionlint" "$workflows"/*.yml

json=$work/workflows.json
for file in "$workflows"/*.yml; do
  "$tools/yq" -o=json '{"file": filename, "workflow": .}' "$file"
done | jq -s 'map({(.file | split("/") | last): .workflow}) | add' > "$json"
check() { jq -e "$1" "$json" > /dev/null || fail "$2"; }

check '[to_entries[] | .value | (.permissions, (.jobs[] | .permissions))
    | select(. != null) | select(. == "write-all" or (type == "object"
      and (has("id-token") or has("attestations") or has("artifact-metadata"))))] | length == 1' \
  'only one job may request id-token, attestations or artifact-metadata'
check '[to_entries[] | select(.key != "release.yml") | .value | .. | strings
    | select(test("actions/attest"))] == []' 'only release.yml may use an attestation action'
check '."release.yml".permissions == {"contents": "read"}' 'release.yml must default to contents: read'
check '."release.yml".jobs.validate.permissions
    == {"contents": "read", "pull-requests": "read", "checks": "read"}' 'validate permissions changed'
check '."release.yml".jobs.publish.permissions
    == {"contents": "write", "id-token": "write", "attestations": "write"}' \
  'publish needs exactly contents, id-token and attestations write'
check '."release.yml".jobs.publish.environment == "release"' 'publish must run in the release environment'

steps='."release.yml".jobs.publish.steps'
# Index of the single publish step matching the filter.
index() { jq -e "[$steps | to_entries[] | select(.value | $1) | .key] | select(length == 1)[0]" "$json"; }
stage=$(index '(.run // "") | test("-Pcentral-staging deploy")') || fail 'expected one staging step'
verify=$(index '(.run // "") | test("verify-staged-artifacts\\.sh")') || fail 'expected one staged-artifact check'
attest=$(index '(.uses // "") | test("^actions/attest-build-provenance@[0-9a-f]{40}$")') \
  || fail 'expected one SHA-pinned attest-build-provenance step'
proof=$(index '(.run // "") | test("verify-attestations\\.sh")') || fail 'expected one attestation verification'
jreleaser=$(jq "[$steps | to_entries[] | select(.value.run // \"\" | test(\"jreleaser:\")) | .key] | min" "$json")
[[ $stage -lt $verify && $verify -lt $attest && $attest -lt $proof && $proof -lt $jreleaser ]] \
  || fail 'publish must stage, check, attest and verify provenance before any JReleaser step'

# Attestation runs whenever the upload step runs.
deploy_if=$(jq -r "${steps}[] | select(.run // \"\" | test(\"jreleaser:deploy\")) | .if" "$json")
for i in "$stage" "$verify" "$attest" "$proof"; do
  [[ $(jq -r "${steps}[$i].if" "$json") == "$deploy_if" ]] \
    || fail "publish step $i does not run under the deployment condition $deploy_if"
done
subjects=$(jq -r "${steps}[$attest].with.\"subject-path\"" "$json")
grep -qF 'teamspeak3-api/*/teamspeak3-api-*.pom' <<< "$subjects" || fail 'POM is not attested'
grep -qF 'teamspeak3-api/*/teamspeak3-api-*.jar' <<< "$subjects" || fail 'JARs are not attested'
# shellcheck disable=SC2016 # The literal argument in that script.
grep -qF -- '--signer-workflow "$repo/.github/workflows/release.yml"' \
  "$(dirname "$0")/verify-attestations.sh" || fail 'verification does not pin the signer workflow'
echo "Release workflow wiring verified in $workflows"
