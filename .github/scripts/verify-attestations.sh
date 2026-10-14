#!/usr/bin/env bash
# Require a GitHub build provenance attestation, signed by release.yml from the given master commit,
# for each staged release artifact. Run verify-staged-artifacts.sh first: it fixes the file set.
set -euo pipefail
version=${1:?approved version required}
staging=${2:-target/staging-deploy}
commit=${3:?release commit required}
repo=${GITHUB_REPOSITORY:-TheHolyWaffle/TeamSpeak-3-Java-API}
base=$staging/com/github/theholywaffle/teamspeak3-api/$version/teamspeak3-api-$version

for file in "$base.pom" "$base.jar" "$base-sources.jar" "$base-javadoc.jar"; do
  test -s "$file" || { echo "Missing staged artifact $file" >&2; exit 1; }
  # A just-created attestation can take a moment to become readable through the API.
  for attempt in 1 2 3 4 5; do
    if gh attestation verify "$file" --repo "$repo" \
      --signer-workflow "$repo/.github/workflows/release.yml" \
      --source-ref refs/heads/master --source-digest "$commit" --deny-self-hosted-runners; then
      continue 2
    fi
    sleep $((attempt * 5))
  done
  echo "No verifiable provenance for $file" >&2
  exit 1
done
echo "Verified provenance of teamspeak3-api $version from $commit"
