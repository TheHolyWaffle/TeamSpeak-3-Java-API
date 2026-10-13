#!/usr/bin/env bash
# Prints, as GitHub step outputs, the pinned TS3 image, the newest official TeamSpeak 3 image (the
# highest 3.x.y tag with its current manifest digest) and whether they differ.
set -euo pipefail
pinned=$(sed -n 's/^FROM //p' src/test/resources/teamspeak3/Dockerfile)
url='https://hub.docker.com/v2/namespaces/library/repositories/teamspeak/tags?page_size=100'
tags=
while [[ $url != null ]]; do
  page=$(curl -fsSL --retry 3 "$url")
  tags+=$(jq -r '.results[].name' <<< "$page")$'\n'
  url=$(jq -r '.next' <<< "$page")
done
newest=$(grep -E '^3\.[0-9]+\.[0-9]+$' <<< "$tags" | sort -V | tail -1)
test -n "$newest"
digest=$(docker buildx imagetools inspect "teamspeak:$newest" --format '{{json .Manifest.Digest}}' | tr -d '"')
[[ $digest =~ ^sha256:[0-9a-f]{64}$ ]]
candidate="teamspeak:$newest@$digest"
echo "pinned=$pinned"
echo "candidate=$candidate"
echo "changed=$([[ $candidate != "$pinned" ]] && echo true || echo false)"
