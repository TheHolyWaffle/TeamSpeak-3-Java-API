#!/usr/bin/env bash
# Two clean builds must produce byte-identical unsigned artifacts: Maven's reproducible-builds recipe
# (install, then clean verify artifact:compare) in an isolated local repository, so nothing is
# installed into the user's repository. A rebuild with another timestamp must be detected first.
set -euo pipefail
repo=$(mktemp -d)
trap 'rm -rf "$repo"' EXIT
build=(-B -ntp "-Dmaven.repo.local=$repo" "-Dmaven.repo.local.tail=$HOME/.m2/repository"
  -DskipTests -Djacoco.skip -Djapicmp.skip -Dspotless.check.skip -Dbuildinfo.ignoreJavadoc=false)
./mvnw "${build[@]}" clean install

echo "Negative control: a rebuild with another outputTimestamp must be reported as different"
./mvnw "${build[@]}" -q clean verify artifact:compare -Dcompare.fail=false \
  -Dproject.build.outputTimestamp=2000-01-01T00:00:00Z
if grep -qx 'ko=0' target/*.buildcompare; then
  echo 'A different outputTimestamp went undetected' >&2
  exit 1
fi

./mvnw "${build[@]}" clean verify artifact:compare
cat target/*.buildcompare
grep -qx 'ok=4' target/*.buildcompare
grep -qx 'ko=0' target/*.buildcompare
mkdir -p target/reproducibility
cp target/*.buildinfo target/*.buildcompare target/reproducibility/
echo 'Reproducible: a changed timestamp was detected, and two clean builds matched'
