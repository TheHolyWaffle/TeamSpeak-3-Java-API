#!/usr/bin/env bash
# Build and run the Maven (module path) and Gradle (classpath) consumers against the staged library
# and require that both loaded exactly the staged JAR. Gradle comes from PATH or $GRADLE.
set -euo pipefail
version=${1:?staged version required}
staging=$(cd "${2:-target/staging-deploy}" && pwd)
gradle=${GRADLE:-gradle}
jar="$staging/com/github/theholywaffle/teamspeak3-api/$version/teamspeak3-api-$version.jar"
test -s "$jar"
work=target/consumers
rm -rf "$work/maven" "$work/gradle"
mkdir -p "$work"
cp -R src/test/consumers/maven src/test/consumers/gradle "$work"/

sha() { shasum -a 256 "$1" | cut -d' ' -f1; }
loaded() {
  local log=$1 library
  grep -qx 'CONSUMER OK' "$log"
  grep -qx 'module=com.github.theholywaffle.teamspeak3' "$log" || [[ $2 == classpath ]]
  library=$(sed -n 's/^library=//p' "$log")
  [[ "$(sha "$library")" == "$(sha "$jar")" ]] || { echo "$2 consumer loaded $library, not the staged JAR" >&2; exit 1; }
}

# A dedicated local repository: no cached or previously installed library can be resolved.
./mvnw -B -ntp -f "$work/maven/pom.xml" -Dmaven.repo.local="$PWD/$work/m2" \
  -Dteamspeak3.version="$version" -Dstaging.repository="file://$staging" \
  package exec:exec | tee "$work/maven.log"
loaded "$work/maven.log" module

"$gradle" --version
"$gradle" --no-daemon -p "$work/gradle" -Pteamspeak3Version="$version" \
  -PstagingRepository="file://$staging" run | tee "$work/gradle.log"
loaded "$work/gradle.log" classpath
echo "Maven and Gradle consumers ran against staged teamspeak3-api $version"
