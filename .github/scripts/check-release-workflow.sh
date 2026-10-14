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
tools=${WORKFLOW_TOOLS:-}
[[ -n $tools ]] || { tools=$work; bash "$(dirname "$0")/workflow-tools.sh" "$tools"; }

"$tools/actionlint" "$workflows"/*.yml

json=$work/workflows.json
"$tools/yq" -o=json '{"file": filename, "workflow": .}' "$workflows"/*.yml \
  | jq -s 'map({(.file | split("/") | last): .workflow}) | add' > "$json"
check() { jq -e "$1" "$json" > /dev/null || fail "$2"; }

check '[.[] | (.permissions, (.jobs[] | .permissions)) | select(. == "write-all" or (type == "object"
    and (has("id-token") or has("attestations") or has("artifact-metadata"))))] | length == 1' \
  'only one job may request id-token, attestations or artifact-metadata'
check '[to_entries[] | select(.key != "release.yml") | .value | .. | strings
    | select(test("actions/attest"))] == []' 'only release.yml may use an attestation action'
check '."release.yml".jobs.publish.permissions
    == {"contents": "write", "id-token": "write", "attestations": "write"}' \
  'publish needs exactly contents, id-token and attestations write'
check '."release.yml".jobs.publish.environment == "release"' 'publish must run in the release environment'

publish='."release.yml".jobs.publish.steps | to_entries | map({id: .value.id, index: .key, step: .value})'
check "$publish | map(select(.id == \"attest\").step.uses)
    | . == [.[] | select(test(\"^actions/attest-build-provenance@[0-9a-f]{40}$\"))] and length == 1" \
  'expected one SHA-pinned attest-build-provenance step with id attest'
check "$publish | map(select(.id | IN(\"stage\", \"staged\", \"attest\", \"provenance\", \"deploy\")))
    | map(.id) == [\"stage\", \"staged\", \"attest\", \"provenance\", \"deploy\"]" \
  'publish must stage, check, attest and verify provenance, then deploy (step ids in this order)'
# Provenance is checked whenever the upload runs.
check "$publish | map(select(.id | IN(\"stage\", \"staged\", \"attest\", \"provenance\", \"deploy\")).step.if)
    | unique | length == 1" 'provenance steps must share the deployment condition'
echo "Release workflow wiring verified in $workflows"
