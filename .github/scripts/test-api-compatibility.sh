#!/usr/bin/env bash
# Controlled examples for the japicmp release-impact gate.
set -euo pipefail
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
fail() { echo "FAILED: $*" >&2; exit 1; }

# Release impact derived from Conventional Commit titles and bodies.
expect_impact() {
  local actual
  actual=$(bash .github/scripts/api-impact.sh "$2" "${3:-}")
  [[ $actual == "$1" ]] || fail "'$2' has impact $actual, expected $1"
}
expect_impact major 'feat!: drop TS3Api'
expect_impact major 'fix(commands)!: rename parameter'
expect_impact major 'refactor: move package' $'Body.\n\nBREAKING CHANGE: Api moved.'
expect_impact minor 'feat: add channel lookup'
expect_impact minor 'feat(files): resumable uploads'
expect_impact patch 'fix: decode escaped pipes'
expect_impact patch 'perf: avoid copies'
expect_impact patch 'deps: bump sshj'
expect_impact none 'refactor: simplify queue'
expect_impact none 'ci: pin actions'
expect_impact none 'docs: mention BREAKING CHANGE: in prose' 'Not a footer: BREAKING CHANGE: inline'

javac_bin=${JAVA_HOME:+$JAVA_HOME/bin/}javac
jar_bin=${JAVA_HOME:+$JAVA_HOME/bin/}jar
for variant in base removed changed added extended internal; do
  "$javac_bin" --release 25 -d "$work/classes/$variant" src/test/api-fixtures/"$variant"/fixture/*.java
  "$jar_bin" --create --file "$work/$variant.jar" --date 1980-01-01T00:00:02Z -C "$work/classes/$variant" .
done

rejected_by_gate() {
  grep -q 'japicmp-maven-plugin:[^:]*:cmp (api-compatibility)' "$1"
}

# compare VARIANT IMPACT pass|fail [text the report must contain]
compare() {
  local reports="$work/reports/$1-$2" status=0
  bash .github/scripts/api-compare.sh "$work/base.jar" "$work/$1.jar" "$2" "$reports" > "$work/log" 2>&1 || status=$?
  if [[ $3 == pass && $status -ne 0 ]]; then cat "$work/log" >&2; fail "$1 with $2 impact was rejected"; fi
  if [[ $3 == fail && $status -eq 0 ]]; then fail "$1 with $2 impact was accepted"; fi
  if [[ $3 == fail ]] && ! rejected_by_gate "$work/log"; then cat "$work/log" >&2; fail "$1 with $2 failed for another reason"; fi
  if [[ $# -gt 3 ]] && ! grep -qF "$4" "$reports/api-compatibility.diff"; then fail "$1 report lacks $4"; fi
  echo "ok: $1 with $2 impact: $3"
}
# Removed and changed API is reported, and needs a major release.
compare removed major pass 'REMOVED METHOD: PUBLIC(-) void reset()'
compare removed minor fail 'REMOVED METHOD: PUBLIC(-) void reset()'
compare changed major pass 'MODIFIED METHOD: PUBLIC long (<-int) count()'
compare changed patch fail 'MODIFIED METHOD: PUBLIC long (<-int) count()'
# A method added to an interface is binary compatible but breaks implementations at compile time.
compare extended major pass 'NEW METHOD: PUBLIC(+) ABSTRACT(+) void onClose()'
compare extended minor fail
# A compatible addition needs a feature release.
compare added minor pass 'NEW METHOD: PUBLIC(+) boolean isEmpty()'
compare added patch fail 'NEW METHOD: PUBLIC(+) boolean isEmpty()'
compare added none fail
# Implementation-only changes pass at any impact.
compare internal patch pass
compare internal none pass
# End to end: a fix-titled PR that removes API is rejected.
impact=$(bash .github/scripts/api-impact.sh 'fix: remove reset' '')
compare removed "$impact" fail

# The real library against its verified published baseline: 2.0 is a major release of 1.3.1.
./mvnw -B -ntp -q -Papi-quick package japicmp:cmp@api-compatibility || fail "the pending version was rejected against 1.3.1"
echo "ok: library as the pending version against 1.3.1: pass"
library=$(ls target/teamspeak3-api-*.jar | grep -vE -- '-(sources|javadoc)\.jar$')
cp "$library" "$work/teamspeak3-api-1.4.0.jar"
if ./mvnw -B -ntp -q -Papi-quick package japicmp:cmp@api-compatibility \
  -Dapi.compare.new="$work/teamspeak3-api-1.4.0.jar" > "$work/log" 2>&1; then
  fail "a 1.4.0 release of the 2.0 API was accepted against 1.3.1"
fi
rejected_by_gate "$work/log" || { cat "$work/log" >&2; fail "1.4.0 failed for another reason"; }
echo "ok: library as 1.4.0 against 1.3.1: fail"
echo 'API compatibility examples passed.'
