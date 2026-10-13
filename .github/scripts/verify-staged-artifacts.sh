#!/usr/bin/env bash
# Inspect the locally staged publication: exactly the normal, sources and Javadoc JARs plus the
# reviewed POM, with reproducible entry timestamps. An optional JReleaser Central bundle must hold
# exactly these files, signed and checksummed. Run release-metadata.sh first: it approves the version.
set -euo pipefail
version=${1:?approved version required}
staging=${2:-target/staging-deploy}
bundle=${3:-}
fail() { echo "Staged artifact check failed: $*" >&2; exit 1; }

files=(.pom .jar -sources.jar -javadoc.jar)
rel=com/github/theholywaffle/teamspeak3-api/$version/teamspeak3-api-$version
base=$staging/$rel
expected=$(for file in "${files[@]}"; do echo "$base$file"; done | sort)
actual=$(find "$staging/${rel%/*}" -type f ! -name '*.asc' ! -name '*.md5' ! -name '*.sha1' \
  ! -name '*.sha256' ! -name '*.sha512' ! -name 'maven-metadata*' | sort)
[[ "$actual" == "$expected" ]] || fail $'unexpected files:\n'"$actual"
cmp -s pom.xml "$base.pom" || fail 'staged POM differs from the reviewed pom.xml'

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

# Every entry carries project.build.outputTimestamp, as zipinfo prints it (yyyymmdd.hhmmss).
stamp=$(./mvnw -q -ntp help:evaluate \
  -Dexpression=project.build.outputTimestamp -DforceStdout | tr -d -- '-:Z' | tr T .)
[[ "$stamp" =~ ^[0-9]{8}\.[0-9]{6}$ ]] || fail 'project.build.outputTimestamp is not an ISO-8601 UTC instant'
for jar in "$base.jar" "$base-sources.jar" "$base-javadoc.jar"; do
  stamps=$(zipinfo -T "$jar" | awk '/^[-dl]/ { print $7 }' | sort -u)
  [[ "$stamps" == "$stamp" ]] || fail "$jar entries are not all dated $stamp: $stamps"
done

# Library JAR: project classes and Maven descriptors only; no shaded, provider or signature files.
package=com/github/theholywaffle/teamspeak3
others=$(unzip -Z1 "$base.jar" | grep -v '/$' \
  | grep -vE "^($package/.*|META-INF/MANIFEST\.MF|META-INF/maven/com\.github\.theholywaffle/teamspeak3-api/pom\.(xml|properties))$" || true)
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
  expected=$(for file in "${files[@]}"; do
    for suffix in '' .asc .md5 .sha1 .sha256 .sha512; do echo "$rel$file$suffix"; done
  done | sort)
  [[ "$(unzip -Z1 "$bundle" | sort)" == "$expected" ]] || fail "$bundle does not hold exactly the signed artifacts"
  unzip -q "$bundle" -d "$work/bundle"
  for file in "${files[@]}"; do
    cmp -s "$base$file" "$work/bundle/$rel$file" || fail "bundled $file differs from the staged artifact"
  done
fi
echo "Verified staged teamspeak3-api $version"
