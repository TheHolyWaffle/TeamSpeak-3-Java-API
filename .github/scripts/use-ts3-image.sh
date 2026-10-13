#!/usr/bin/env bash
# Applies a TS3 image to a disposable checkout as its update PR would: the Dockerfile pin and the
# README support table. Usage: use-ts3-image.sh teamspeak:X.Y.Z@sha256:DIGEST
set -euo pipefail
image=$1
[[ $image =~ ^teamspeak:(3\.[0-9]+\.[0-9]+)@sha256:[0-9a-f]{64}$ ]] ||
  { echo "Not a digest-pinned TeamSpeak 3 image: $image" >&2; exit 1; }
version=${BASH_REMATCH[1]}
dockerfile=src/test/resources/teamspeak3/Dockerfile
old=$(sed -n 's/^FROM teamspeak:\([^@]*\)@.*/\1/p' "$dockerfile")
sed -i.bak "s|^FROM .*|FROM $image|" "$dockerfile"
sed -i.bak "s/| TeamSpeak 3 server ${old//./\\.} (official/| TeamSpeak 3 server $version (official/" README.md
rm "$dockerfile.bak" README.md.bak
echo "Using $image instead of TS3 $old"
