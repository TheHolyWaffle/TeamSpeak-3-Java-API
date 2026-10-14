#!/usr/bin/env bash
# Download the pinned actionlint and yq release binaries into the given directory and verify their
# SHA-256 against the projects' published checksums. Update versions and checksums together.
set -euo pipefail
dir=${1:?target directory required}
actionlint=1.7.12
yq=4.54.1
os=$(uname -s | tr '[:upper:]' '[:lower:]')
case "$(uname -m)" in x86_64 | amd64) arch=amd64 ;; arm64 | aarch64) arch=arm64 ;; *) arch=unsupported ;; esac
case "$os-$arch" in
  linux-amd64) actionlint_sha=8aca8db96f1b94770f1b0d72b6dddcb1ebb8123cb3712530b08cc387b349a3d8
    yq_sha=8e34fc298390875de416e6a4afcb8cabeceb25d9aa8506c1a2f9353cf702ea5f ;;
  linux-arm64) actionlint_sha=325e971b6ba9bfa504672e29be93c24981eeb1c07576d730e9f7c8805afff0c6
    yq_sha=189088da0c6429ec5178dfaab1a114805f6cab0b61b165ab236efedf1d57a71b ;;
  darwin-amd64) actionlint_sha=5b44c3bc2255115c9b69e30efc0fecdf498fdb63c5d58e17084fd5f16324c644
    yq_sha=3a812fce205a4d67014fb71b7fa297092210e580df3d8f52dddc2cf3a599c22b ;;
  darwin-arm64) actionlint_sha=aba9ced2dee8d27fecca3dc7feb1a7f9a52caefa1eb46f3271ea66b6e0e6953f
    yq_sha=fee511a181bd8b3e6b7da98842b41973bfac8cd3bcd341ed29a584620b5b2844 ;;
  *) echo "No pinned workflow tools for $os-$(uname -m)" >&2; exit 1 ;;
esac
fetch() {
  curl --fail --silent --show-error --location --retry 3 --output "$2" "$1"
  if command -v sha256sum > /dev/null; then sum=$(sha256sum "$2"); else sum=$(shasum -a 256 "$2"); fi
  [[ ${sum%% *} == "$3" ]] || { echo "Checksum mismatch for $1" >&2; exit 1; }
}
mkdir -p "$dir"
fetch "https://github.com/rhysd/actionlint/releases/download/v$actionlint/actionlint_${actionlint}_${os}_$arch.tar.gz" \
  "$dir/actionlint.tar.gz" "$actionlint_sha"
tar -xzf "$dir/actionlint.tar.gz" -C "$dir" actionlint
fetch "https://github.com/mikefarah/yq/releases/download/v$yq/yq_${os}_$arch" "$dir/yq" "$yq_sha"
chmod +x "$dir/yq"
