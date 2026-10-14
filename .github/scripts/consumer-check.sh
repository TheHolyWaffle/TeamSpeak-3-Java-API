#!/usr/bin/env bash
# Build and run the Maven (module path) and Gradle (classpath) consumers against the staged library
# and require that both loaded exactly the staged JAR. Run ./gradlew publish -Pversion=VERSION first.
# Usage: consumer-check.sh VERSION
set -euo pipefail
version=${1:?staged version required}
staging=$PWD/build/staging-deploy
jar=$staging/com/github/theholywaffle/teamspeak3-api/$version/teamspeak3-api-$version.jar
test -s "$jar"
work=build/consumers
rm -rf "$work"
mkdir -p "$work"
cp -R src/test/consumers/maven src/test/consumers/gradle "$work"/

sha() { shasum -a 256 "$1" | cut -d' ' -f1; }
# loaded LOG MODULE: the consumer succeeded, saw the library as MODULE and loaded the staged JAR.
loaded() {
  local library
  grep -qx 'CONSUMER OK' "$1"
  grep -qx "module=$2" "$1"
  library=$(sed -n 's/^library=//p' "$1")
  [[ "$(sha "$library")" == "$(sha "$jar")" ]] || { echo "$1: loaded $library, not the staged JAR" >&2; exit 1; }
}

# A dedicated local repository: no cached or previously installed library can be resolved.
mvn -B -ntp -f "$work/maven/pom.xml" -Dmaven.repo.local="$PWD/$work/m2" \
  -Dteamspeak3.version="$version" -Dstaging.repository="file://$staging" \
  compile exec:exec | tee "$work/maven.log"
loaded "$work/maven.log" com.github.theholywaffle.teamspeak3

./gradlew -q -p "$work/gradle" -Pteamspeak3Version="$version" -PstagingRepository="file://$staging" run \
  | tee "$work/gradle.log"
loaded "$work/gradle.log" null
echo "Maven and Gradle consumers ran against staged teamspeak3-api $version"
