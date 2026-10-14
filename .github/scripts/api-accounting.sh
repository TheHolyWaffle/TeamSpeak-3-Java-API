#!/usr/bin/env bash
# Every type in a japicmp diff report must match a row of the accounting table, and every
# pattern must match a reported type. Patterns are backticked globs in the table's first column,
# relative to the library package. Usage: api-accounting.sh REPORT.diff ACCOUNTING.md
set -euo pipefail
report=$1 accounting=$2
prefix=com.github.theholywaffle.teamspeak3.

types=$(awk -v prefix="$prefix" '/^(\*\*\*|\+\+\+|---|===)/ {
  for (i = 1; i <= NF; i++) if (index($i, prefix) == 1) print substr($i, length(prefix) + 1)
}' "$report" | sort -u)
[[ -n $types ]] || { echo "No types found in $report" >&2; exit 1; }
patterns=$(awk -F'|' '/^\| `/ { print $2 }' "$accounting" | grep -o '`[^`]*`' | tr -d '`')
[[ -n $patterns ]] || { echo "No patterns found in $accounting" >&2; exit 1; }

status=0
while IFS= read -r type; do
  covered=false
  while IFS= read -r pattern; do
    # shellcheck disable=SC2053 # the pattern is a glob on purpose
    if [[ $type == $pattern ]]; then covered=true; break; fi
  done <<< "$patterns"
  $covered || { echo "Unaccounted API difference: $type" >&2; status=1; }
done <<< "$types"
while IFS= read -r pattern; do
  used=false
  while IFS= read -r type; do
    # shellcheck disable=SC2053
    if [[ $type == $pattern ]]; then used=true; break; fi
  done <<< "$types"
  $used || { echo "Accounting row matches no reported type: $pattern" >&2; status=1; }
done <<< "$patterns"
((status == 0)) && echo "Accounted for $(wc -l <<< "$types" | tr -d ' ') types in $report"
exit "$status"
