#!/usr/bin/env bash
# Runs the project's japicmp api-compatibility execution for two JARs, failing if the API change
# needs a larger release impact than the one given. Usage: api-compare.sh OLD.jar NEW.jar IMPACT REPORT_DIR
set -euo pipefail
old=$1 new=$2 impact=$3 reports=$4
# major: any change; minor: compatible changes only; patch and none: no API change at all.
case $impact in
  major) incompatible=false any=false ;;
  minor) incompatible=true any=false ;;
  patch | none) incompatible=true any=true ;;
  *) echo "Unknown release impact: $impact" >&2; exit 2 ;;
esac
mkdir -p "$reports"
status=0
# japicmp needs this project's artifact from the reactor, so package it in the same run.
./mvnw -B -ntp -q -Papi-quick package japicmp:cmp@api-compatibility \
  -Dapi.compare.old="$old" -Dapi.compare.new="$new" -Dapi.checkVersions=false \
  -Dapi.rejectIncompatible="$incompatible" -Dapi.rejectAnyChange="$any" || status=$?
cp target/japicmp/api-compatibility.* "$reports/"
exit "$status"
