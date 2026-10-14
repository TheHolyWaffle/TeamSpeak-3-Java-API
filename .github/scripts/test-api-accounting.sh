#!/usr/bin/env bash
# The accounting check accepts a fully covered report and rejects uncovered types and stale rows.
set -euo pipefail
fixtures=$(mktemp -d)
trap 'rm -rf "$fixtures"' EXIT
cat > "$fixtures/report.diff" <<'DIFF'
***! MODIFIED CLASS: PUBLIC FINAL (<- NON_FINAL) com.github.theholywaffle.teamspeak3.TS3Query  (not serializable)
	***! CLASS FILE FORMAT VERSION: 69.0 <- 52.0
	+++  NEW METHOD: PUBLIC(+) com.github.theholywaffle.teamspeak3.EventSubscription subscribe(com.github.theholywaffle.teamspeak3.api.event.TS3Listener)
---! REMOVED CLASS: PUBLIC(-) com.github.theholywaffle.teamspeak3.api.CommandFuture$SuccessListener  (class removed)
***! MODIFIED INTERFACE (<- CLASS) PUBLIC com.github.theholywaffle.teamspeak3.commands.parameter.Parameter  (not serializable)
DIFF
check() {
  printf '| Types | Difference | Migration |\n| --- | --- | --- |\n' > "$fixtures/accounting.md"
  printf '%s\n' "$@" >> "$fixtures/accounting.md"
  bash .github/scripts/api-accounting.sh "$fixtures/report.diff" "$fixtures/accounting.md"
}
check '| `TS3Query` | a | b |' '| `api.CommandFuture$*`, `commands.parameter.*` | a | b |'
if check '| `TS3Query` | a | b |' '| `api.CommandFuture$*` | a | b |'; then
  echo 'Accepted an unaccounted type' >&2; exit 1
fi
if check '| `TS3Query`, `TS3Config` | a | b |' '| `api.*`, `commands.*` | a | b |'; then
  echo 'Accepted a row that matches nothing' >&2; exit 1
fi
echo 'API accounting acceptance and rejection tests passed.'
