#!/usr/bin/env bash
# Verify the staged repository (and optional JReleaser bundle), then require each defect in a copy
# to be rejected by verify-staged-artifacts.sh for the intended reason.
set -euo pipefail
version=${1:?staged version required}
staging=${2:-target/staging-deploy}
bundle=${3:-}
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
rel=com/github/theholywaffle/teamspeak3-api/$version/teamspeak3-api-$version

bash .github/scripts/verify-staged-artifacts.sh "$version" "$staging" "$bundle"

# rejects NAME EXPECTED-REASON MUTATION: MUTATION edits $repo (and $copy, the bundle copy) in place.
rejects() {
  local name=$1 repo="$work/$1/repo" copy="$work/$1/bundle.zip"
  mkdir -p "$work/$name/files"
  cp -R "$staging" "$repo"
  [[ -z "$bundle" ]] || cp "$bundle" "$copy"
  (cd "$work/$name/files" && "$3" "$repo/$rel" "$copy")
  if bash .github/scripts/verify-staged-artifacts.sh "$version" "$repo" "${bundle:+$copy}" > "$work/$name.log" 2>&1; then
    echo "verify-staged-artifacts.sh accepted: $name" >&2
    exit 1
  fi
  grep -q "Staged artifact check failed: .*$2" "$work/$name.log" || { cat "$work/$name.log" >&2; exit 1; }
  echo "rejected $name: $2"
}

# Added entries keep the normalized timestamp, so only the intended check can fail.
add() { mkdir -p "$(dirname "$2")" && printf "$3" > "$2" && touch -t 198001010000.02 "$2" && zip -q -X "$1" "$2"; }
shaded_classifier() { cp "$1.jar" "$1-with-dependencies.jar"; }
missing_javadoc() { rm "$1-javadoc.jar"; }
changed_pom() { printf '<!-- -->\n' >> "$1.pom"; }
unnormalized_timestamp() { printf x > extra && zip -q -X "$1.jar" extra; }
bundled_logging_config() { add "$1.jar" simplelogger.properties 'x=1\n'; }
preview_bytecode() { add "$1.jar" com/github/theholywaffle/teamspeak3/Preview.class '\xca\xfe\xba\xbe\xff\xff\x00\x45'; }
incomplete_sources() { zip -q -d "$1-sources.jar" com/github/theholywaffle/teamspeak3/TS3Query.java; }
bundle_missing_signature() { zip -q -d "$2" "$rel.jar.asc"; }
bundle_other_jar() { add "$2" "$rel.jar" x; }

rejects shaded-classifier 'unexpected files' shaded_classifier
rejects missing-javadoc 'unexpected files' missing_javadoc
rejects changed-pom 'staged POM differs' changed_pom
rejects unnormalized-timestamp 'entries are not all dated' unnormalized_timestamp
rejects bundled-logging-config 'library JAR contains foreign entries' bundled_logging_config
rejects preview-bytecode 'not plain Java 25 bytecode' preview_bytecode
rejects incomplete-sources 'sources JAR differs' incomplete_sources
if [[ -n "$bundle" ]]; then
  rejects bundle-missing-signature 'does not hold exactly' bundle_missing_signature
  rejects bundle-other-jar 'bundled .jar differs' bundle_other_jar
fi
echo 'verify-staged-artifacts.sh rejected every defect'
