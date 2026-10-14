#!/usr/bin/env bash
# Derive JReleaser's public-key file without exposing or retaining the private key.
set -euo pipefail
test "$#" -eq 2
test -s "$1"
umask 077
# A short path also works with GnuPG's Unix-domain sockets on macOS.
keyring=$(mktemp -d /tmp/teamspeak-key.XXXXXX)
trap 'gpgconf --homedir "$keyring" --kill all >/dev/null 2>&1 || true; rm -rf "$keyring"' EXIT
gpg --homedir "$keyring" --batch --quiet --import "$1"
gpg --homedir "$keyring" --batch --armor --export > "$2"
test -s "$2"
