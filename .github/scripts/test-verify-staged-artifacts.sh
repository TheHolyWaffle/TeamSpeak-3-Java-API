#!/usr/bin/env bash
# Each defect in a copy of a valid staged repository must be rejected by verify-staged-artifacts.sh.
set -euo pipefail
version=${1:?staged version required}
staging=${2:-target/staging-deploy}
bundle=${3:-}
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
path=com/github/theholywaffle/teamspeak3-api/$version
printf '{\n  ".": "%s"\n}\n' "$version" > "$work/manifest.json"

bash .github/scripts/verify-staged-artifacts.sh "$version" "$staging" "$work/manifest.json" > /dev/null

# rejects NAME EXPECTED-REASON MUTATION [MANIFEST]
rejects() {
  local name=$1 repo="$work/$1" manifest=${4:-$work/manifest.json}
  cp -R "$staging" "$repo"
  (cd "$repo/$path" && eval "$3")
  if bash .github/scripts/verify-staged-artifacts.sh "$version" "$repo" "$manifest" > "$work/$name.log" 2>&1; then
    echo "verify-staged-artifacts.sh accepted: $name" >&2
    exit 1
  fi
  grep -q "Staged artifact check failed: .*$2" "$work/$name.log" || { cat "$work/$name.log" >&2; exit 1; }
  echo "rejected $name: $2"
}

# Added entries keep the normalized timestamp, so only the intended check can fail.
add() { touch -t 198001010000.02 "$2" && zip -q -X "$1" "$2" && rm -r "${2%%/*}"; }
printf '{\n  ".": "9.9.9"\n}\n' > "$work/other-manifest.json"
rejects unapproved-version 'does not approve' true "$work/other-manifest.json"
rejects shaded-classifier 'unexpected files' "cp teamspeak3-api-$version.jar teamspeak3-api-$version-with-dependencies.jar"
rejects missing-javadoc 'unexpected files' "rm teamspeak3-api-$version-javadoc.jar"
rejects changed-pom 'staged POM differs' "printf '<!-- -->\\n' >> teamspeak3-api-$version.pom"
rejects unnormalized-timestamp 'entries are not all dated' "printf x > extra && zip -q -X teamspeak3-api-$version.jar extra && rm extra"
rejects bundled-logging-config 'library JAR contains foreign entries' "printf 'x=1\\n' > simplelogger.properties && add teamspeak3-api-$version.jar simplelogger.properties"
rejects preview-bytecode 'not plain Java 25 bytecode' "mkdir -p com/github/theholywaffle/teamspeak3 && printf '\\xca\\xfe\\xba\\xbe\\xff\\xff\\x00\\x45' > com/github/theholywaffle/teamspeak3/Preview.class && add teamspeak3-api-$version.jar com/github/theholywaffle/teamspeak3/Preview.class"
rejects incomplete-sources 'sources JAR differs' "zip -q -d teamspeak3-api-$version-sources.jar com/github/theholywaffle/teamspeak3/TS3Query.java"

# The JReleaser bundle must contain exactly the verified, signed artifacts.
if [[ -n "$bundle" ]]; then
  bash .github/scripts/verify-staged-artifacts.sh "$version" "$staging" "$work/manifest.json" "$bundle" > /dev/null
  rejects_bundle() {
    local name=$1 copy="$work/$1.zip"
    mkdir "$work/$name-files"
    cp "$bundle" "$copy"
    (cd "$work/$name-files" && eval "$3")
    if bash .github/scripts/verify-staged-artifacts.sh "$version" "$staging" "$work/manifest.json" "$copy" \
        > "$work/$name.log" 2>&1; then
      echo "verify-staged-artifacts.sh accepted bundle: $name" >&2
      exit 1
    fi
    grep -q "Staged artifact check failed: .*$2" "$work/$name.log" || { cat "$work/$name.log" >&2; exit 1; }
    echo "rejected $name: $2"
  }
  rejects_bundle bundle-missing-signature 'does not hold exactly' "zip -q -d '$work/bundle-missing-signature.zip' '$path/teamspeak3-api-$version.jar.asc'"
  rejects_bundle bundle-other-jar 'bundled .jar differs' "mkdir -p '$path' && printf x > '$path/teamspeak3-api-$version.jar' && zip -q '$work/bundle-other-jar.zip' '$path/teamspeak3-api-$version.jar'"
fi
echo 'verify-staged-artifacts.sh rejected every defect'
