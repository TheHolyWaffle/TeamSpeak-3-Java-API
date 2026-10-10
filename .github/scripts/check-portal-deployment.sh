#!/usr/bin/env bash
# A recovery ID must refer only to this validated, unpublished Maven version.
set -euo pipefail
id=${1:?deployment ID required}
version=${2:?stable Maven version required}
status_file=${3:?Portal status JSON required}
jq -e --arg id "$id" --arg purl "pkg:maven/com.github.theholywaffle/teamspeak3-api@$version" '
  .deploymentId == $id and .deploymentState == "VALIDATED" and
  (.purls | length > 0) and
  all(.purls[]; . == $purl or startswith($purl + "?"))
' "$status_file" >/dev/null
