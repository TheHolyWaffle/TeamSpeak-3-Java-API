#!/usr/bin/env bash
# Runs the project's japicmp api-compatibility execution for two JARs, failing if the API change
# needs a larger release impact than the one given. Usage: api-compare.sh OLD.jar NEW.jar IMPACT REPORT_DIR
set -euo pipefail
old=$1 new=$2 impact=$3 reports=$4
# major: any change; minor: compatible changes only; patch and none: no API change at all.
case $impact in
  major) version=2.0.0 incompatible=false any=false ;;
  minor) version=1.1.0 incompatible=true any=false ;;
  patch) version=1.0.1 incompatible=true any=true ;;
  none) version=1.0.0 incompatible=true any=true ;;
  *) echo "Unknown release impact: $impact" >&2; exit 2 ;;
esac
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
# japicmp takes each version from the file name.
mkdir -p "$work/old" "$work/new" "$reports"
cp "$old" "$work/old/api-1.0.0.jar"
cp "$new" "$work/new/api-$version.jar"
status=0
# japicmp needs this project's artifact from the reactor, so package it (without tests) in the same run.
./mvnw -B -ntp -q -DskipTests -Djacoco.skip -Dspotless.check.skip -Dmaven.javadoc.skip -Dmaven.source.skip \
  package japicmp:cmp@api-compatibility \
  -Dapi.compare.old="$work/old/api-1.0.0.jar" -Dapi.compare.new="$work/new/api-$version.jar" \
  -Dapi.rejectIncompatible="$incompatible" -Dapi.rejectAnyChange="$any" || status=$?
cp target/japicmp/api-compatibility.* "$reports/"
exit "$status"
