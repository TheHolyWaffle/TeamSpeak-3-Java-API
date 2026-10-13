#!/usr/bin/env bash
# Inspect the locally staged publication: exactly the normal, sources and Javadoc JARs plus the
# reviewed POM, at the release-please approved version, with reproducible entry timestamps.
# An optional JReleaser Central bundle must hold exactly these files, signed and checksummed.
set -euo pipefail
version=${1:?approved version required}
staging=${2:-target/staging-deploy}
manifest=${3:-.release-please-manifest.json}
bundle=${4:-}
fail() { echo "Staged artifact check failed: $*" >&2; exit 1; }
evaluate() {
  ./mvnw -q -ntp org.apache.maven.plugins:maven-help-plugin:3.5.2:evaluate -Dexpression="$1" -DforceStdout
}

[[ "$(evaluate project.version)" == "$version" ]] || fail "POM version is not $version"
jq -e --arg version "$version" '.["."] == $version' "$manifest" >/dev/null \
  || fail "$manifest does not approve $version"
dir="$staging/com/github/theholywaffle/teamspeak3-api/$version"
base="$dir/teamspeak3-api-$version"
expected=$(printf '%s\n' "$base.pom" "$base.jar" "$base-sources.jar" "$base-javadoc.jar" | sort)
actual=$(find "$dir" -type f ! -name '*.asc' ! -name '*.md5' ! -name '*.sha1' ! -name '*.sha256' \
  ! -name '*.sha512' ! -name 'maven-metadata*' | sort)
[[ "$actual" == "$expected" ]] || fail $'unexpected files:\n'"$actual"
cmp -s pom.xml "$base.pom" || fail 'staged POM differs from the reviewed pom.xml'

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

# Every entry carries project.build.outputTimestamp, as zipinfo prints it (yyyymmdd.hhmmss).
stamp=$(evaluate project.build.outputTimestamp | sed -E 's/^([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})Z$/\1\2\3.\4\5\6/')
[[ "$stamp" =~ ^[0-9]{8}\.[0-9]{6}$ ]] || fail 'project.build.outputTimestamp is not an ISO-8601 UTC instant'
for jar in "$base.jar" "$base-sources.jar" "$base-javadoc.jar"; do
  stamps=$(zipinfo -T "$jar" | awk '/^[-dl]/ { print $7 }' | sort -u)
  [[ "$stamps" == "$stamp" ]] || fail "$jar entries are not all dated $stamp: $stamps"
done

# Library JAR: project classes and Maven descriptors only; no shaded, provider or signature files.
package=com/github/theholywaffle/teamspeak3
others=$(unzip -Z1 "$base.jar" | grep -vE "^($package/.*|com/|com/github/|com/github/theholywaffle/|META-INF/|META-INF/MANIFEST\.MF|META-INF/maven/(com\.github\.theholywaffle/(teamspeak3-api/(pom\.xml|pom\.properties)?)?)?)$" || true)
[[ -z "$others" ]] || fail $'library JAR contains foreign entries:\n'"$others"
unzip -p "$base.jar" META-INF/MANIFEST.MF | tr -d '\r' \
  | grep -qx 'Automatic-Module-Name: com.github.theholywaffle.teamspeak3' || fail 'Automatic-Module-Name missing'
unzip -p "$base.jar" META-INF/maven/com.github.theholywaffle/teamspeak3-api/pom.properties | tr -d '\r' \
  | grep -qx "version=$version" || fail 'embedded pom.properties has another version'
unzip -q "$base.jar" -d "$work/classes"
while IFS= read -r class; do
  # Class file major version 69 is Java 25, without preview minor version 0xFFFF.
  [[ "$(od -An -j4 -N4 -tu1 "$class" | xargs)" == '0 0 0 69' ]] || fail "$class is not plain Java 25 bytecode"
done < <(find "$work/classes" -name '*.class')

# Sources JAR: exactly the main sources.
diff <(cd src/main/java && find . -name '*.java' | sed 's|^\./||' | sort) \
  <(unzip -Z1 "$base-sources.jar" | grep '\.java$' | sort) || fail 'sources JAR differs from src/main/java'

# Javadoc JAR: the automatic module with every source package, without generation dates.
module=com.github.theholywaffle.teamspeak3
unzip -q "$base-javadoc.jar" -d "$work/javadoc"
test -s "$work/javadoc/index.html" || fail 'Javadoc index missing'
test -s "$work/javadoc/$module/$package/TS3Query.html" || fail 'TS3Query Javadoc missing'
[[ "$(head -1 "$work/javadoc/element-list")" == "module:$module" ]] || fail "Javadoc is not documented as module $module"
diff <(cd src/main/java && find . -name '*.java' -exec dirname {} \; | sed 's|^\./||; s|/|.|g' | sort -u) \
  <(tail -n +2 "$work/javadoc/element-list" | sort) || fail 'Javadoc packages differ from source packages'
! grep -rlq 'dc.created' "$work/javadoc" || fail 'Javadoc contains generation dates'

if [[ -n "$bundle" ]]; then
  prefix=com/github/theholywaffle/teamspeak3-api/$version/teamspeak3-api-$version
  expected=$(for file in .pom .jar -sources.jar -javadoc.jar; do
    for suffix in '' .asc .md5 .sha1 .sha256 .sha512; do echo "$prefix$file$suffix"; done
  done | sort)
  [[ "$(unzip -Z1 "$bundle" | sort)" == "$expected" ]] || fail "$bundle does not hold exactly the signed artifacts"
  unzip -q "$bundle" -d "$work/bundle"
  for file in .pom .jar -sources.jar -javadoc.jar; do
    cmp -s "$base$file" "$work/bundle/$prefix$file" || fail "bundled $file differs from the staged artifact"
  done
fi
echo "Verified staged teamspeak3-api $version"
