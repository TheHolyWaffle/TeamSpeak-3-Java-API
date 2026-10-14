#!/usr/bin/env bash
# Accept the real workflows, then require check-release-workflow.sh to reject one planted defect
# per copy for the intended reason.
set -euo pipefail
scripts=$(dirname "$0")
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
bash "$scripts/workflow-tools.sh" "$work"
export WORKFLOW_TOOLS=$work

bash "$scripts/check-release-workflow.sh" .github/workflows

attest='.jobs.publish.steps[] | select(.uses // "" | test("attest"))'
reject() {
  local file=$1 edit=$2 reason=$3 copy
  copy=$(mktemp -d "$work/case.XXXX")
  cp .github/workflows/*.yml "$copy/"
  "$work/yq" -i "$edit" "$copy/$file"
  if bash "$scripts/check-release-workflow.sh" "$copy" > "$copy/log" 2>&1; then
    echo "Accepted a defect: $edit" >&2; exit 1
  fi
  grep -qF -- "$reason" "$copy/log" || { echo "Rejected for another reason: $edit" >&2; cat "$copy/log" >&2; exit 1; }
}
reject release.yml '.jobs.publish.steps |= (map(select(.uses // "" | test("attest") | not))
  + map(select(.uses // "" | test("attest"))))' 'before any JReleaser step'
reject release.yml '.jobs.publish.steps |= (map(select(.run // "" | test("verify-attestations") | not))
  + map(select(.run // "" | test("verify-attestations"))))' 'before any JReleaser step'
reject release.yml '.jobs.publish.steps |= map(select(.run // "" | test("verify-attestations") | not))' \
  'expected one attestation verification'
reject release.yml "($attest | .uses) = \"actions/attest-build-provenance@v4\"" 'SHA-pinned attest-build-provenance'
reject release.yml "($attest | .if) = \"\${{ inputs.action == 'publish' }}\"" 'deployment condition'
subjects='target/staging-deploy/com/github/theholywaffle/teamspeak3-api/*/teamspeak3-api-*'
reject release.yml "($attest | .with.\"subject-path\") = \"$subjects.pom\"" 'JARs are not attested'
reject release.yml "($attest | .with.\"subject-path\") = \"$subjects.jar\"" 'POM is not attested'
reject release.yml '.jobs.validate.permissions."id-token" = "write"' 'only one job may request'
reject release.yml '.permissions."attestations" = "write"' 'only one job may request'
reject release.yml '.jobs.publish.permissions."packages" = "write"' 'publish needs exactly'
reject verify.yml '.jobs.publication_bundle.steps += [{"uses": "actions/attest-build-provenance@4d101475d8b20a2381f78447822ac1eab6504dd8",
  "with": {"subject-path": "target/staging-deploy/**"}}]' 'only release.yml may use an attestation action'
reject release.yml "($attest | .if) = \"\${{ inputs.unknown != 'finish-github' }}\"" 'property "unknown" is not defined'
echo 'Release workflow wiring acceptance and rejection tests passed.'
