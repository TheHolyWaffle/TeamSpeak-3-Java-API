#!/usr/bin/env bash
set -euo pipefail
fixtures=$(mktemp -d)
trap 'rm -rf "$fixtures"' EXIT
printf '{".":"2.0.0"}\n' > "$fixtures/manifest.json"
printf '## [2.0.0](https://example.invalid/v2.0.0)\n\n### Features\n\n* Approved notes, preserved verbatim.\n\n' > "$fixtures/expected.md"
{ printf '# Changelog\n\n'; cat "$fixtures/expected.md"; printf '## [1.3.1]\n\nOld notes.\n'; } > "$fixtures/changelog.md"
validate() {
  bash .github/scripts/release-metadata.sh "$1" "$2" "$fixtures/changelog.md" "$fixtures/manifest.json" "$fixtures/actual.md"
}
validate 2.0.0 v2.0.0
cmp "$fixtures/expected.md" "$fixtures/actual.md"
for pair in '2.0.0-SNAPSHOT v2.0.0-SNAPSHOT' '2.0.0 v2.0.1' '02.0.0 v02.0.0' '2.0.1 v2.0.1' '2.0.0-rc.1 v2.0.0-rc.1'; do
  read -r version tag <<< "$pair"
  if validate "$version" "$tag"; then echo "Unexpected acceptance: $pair" >&2; exit 1; fi
done
printf '{".":"1.3.1"}\n' > "$fixtures/manifest.json"
if validate 2.0.0 v2.0.0; then exit 1; fi
printf '{".":"2.0.0"}\n' > "$fixtures/manifest.json"
printf '## [2.0.0]\nDuplicate.\n' >> "$fixtures/changelog.md"
if validate 2.0.0 v2.0.0; then exit 1; fi
printf '## [2.0.1]\nNewer.\n## [2.0.0]\nOld.\n' > "$fixtures/changelog.md"
if validate 2.0.0 v2.0.0; then exit 1; fi
printf '## [1.3.1]\nMissing release entry.\n' > "$fixtures/changelog.md"
if validate 2.0.0 v2.0.0; then exit 1; fi
printf '## [2.0.0]\n' > "$fixtures/changelog.md"
header_bytes=$(LC_ALL=C wc -c < "$fixtures/changelog.md")
head -c "$((9999 - header_bytes))" /dev/zero | tr '\0' x >> "$fixtures/changelog.md"
printf '\n' >> "$fixtures/changelog.md"
validate 2.0.0 v2.0.0
cmp "$fixtures/changelog.md" "$fixtures/actual.md"
printf 'x\n' >> "$fixtures/changelog.md"
if validate 2.0.0 v2.0.0; then exit 1; fi
echo 'Release metadata acceptance and rejection tests passed.'

# Recovery must reject a wrong version, namespace, ID, empty bundle, or published state.
id=12345678-1234-1234-1234-123456789abc
jq -n --arg id "$id" '{deploymentId:$id, deploymentState:"VALIDATED",
  purls:["pkg:maven/com.github.theholywaffle/teamspeak3-api@2.0.0"]}' > "$fixtures/status.json"
bash .github/scripts/check-portal-deployment.sh "$id" 2.0.0 "$fixtures/status.json"
for change in '.deploymentState="PUBLISHED"' '.deploymentId="wrong"' '.purls=[]' \
  '.purls=["pkg:maven/com.github.theholywaffle/teamspeak3-api@2.0.1"]' \
  '.purls=["pkg:maven/com.someone.else/teamspeak3-api@2.0.0"]'; do
  jq "$change" "$fixtures/status.json" > "$fixtures/invalid-status.json"
  if bash .github/scripts/check-portal-deployment.sh "$id" 2.0.0 "$fixtures/invalid-status.json"; then exit 1; fi
done
echo 'Portal recovery acceptance and rejection tests passed.'
