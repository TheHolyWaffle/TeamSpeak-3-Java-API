# Verified ban-rule administration

> Current scope: TS3 RAW and SSH only. TS6 functionality and tests are removed.
> Any TS6 observations below are retained historical findings from merged PRs,
> not current support claims or release acceptance requirements.

This focused #441 slice verifies TS3 **3.13.8 RAW** and TS3 **3.13.8 SSH** with the existing pinned fixtures. See the
[overall inventory](command-compatibility.md) and [integration setup](integration-tests.md).
It covers administrative rules, not client disconnection or enforcement.

## Verified operations

`ServerQueryCompatibilityIT.banRuleDomain` captures live `banadd`, `banlist` and
`bandel` help, then creates three disposable rules through the public API:

- A permanent anchored name-pattern rule.
- An IP-pattern rule lasting 3600 seconds, using the six-argument overload with
  null myTeamSpeak ID.
- A permanent UID rule with null reason.

These targets do not identify the connected query session. The IP is from the
documentation address range; the UID is a synthetic test value. Listing must
contain all three new IDs alongside the initial set. The test checks exact
selected patterns/UID, durations, reasons, creator database ID/name/UID,
creation date and zero enforcements. Reasons preserve Japanese text, spaces,
pipes, slashes and literal `\s`, `\p`, `\n` escapes. Actual control characters,
pattern matching and limits are not claimed.

Each new ID is registered with the existing cleanup resource immediately after
creation. All three deletions are attempted even after an assertion failure,
with the original failure preserved through try-with-resources suppression.
Listing returns to exactly the initial ID set; pre-existing rules are preserved.
The test never calls `deleteAllBans` or bans a connected client.

## Output and errors

All two TS3 combinations supply the same observed row fields:
`banid`, `ip`, `name`, `uid`, `mytsid`, `lastnickname`, `created`, `duration`,
`invokername`, `invokercldbid`, `invokeruid`, `reason` and `enforcements`.
Unused targets, myTeamSpeak ID and last nickname are empty in these rows.
Null reason is returned as an empty string. Unknown properties remain accessible
through `getMap()`; live help uses the existing controlled raw-command API.
Numeric rule and creator identities come from the connected server.

`created` is epoch **seconds** on both server pins, and the existing
`Ban.getCreatedDate()` returns an instant within one minute of creation. The
captured help's `banlist` example uses a millisecond-looking value; executed
output, rather than that illustrative example, establishes this timestamp unit.
The TS6 file-directory milliseconds observed in another slice do not apply here.
`duration` retains 0 for permanent rules and 3600 for the temporary rule.

Deleting an already removed ID returns **3328 / `invalid ban id`** on all three
combinations. The library exposes `TS3CommandFailedException` with that metadata;
a subsequent version command succeeds. This is an observed error contract for
the pins, not a universal mapping or restricted-role guarantee.

Evidence is saved to `target/compatibility/<target>-ban-rules.txt`, alongside
version/build/image/transport metadata. Required Docker CI uploads it even on
failure with the test reports. No production rules or credentials are used.

## Release intent, migration and remaining scope

Internal **`test:`** release intent for planned 2.0.0 acceptance. No runtime,
public API, dependency or default change. Consumers should use server-returned
rule IDs, handle actual failed deletion and distinguish rule administration
from evidence that a client was disconnected or denied entry. Timestamp units
must be verified per field; no conversion heuristic or new date API is added.

Ban enforcement, expiry, combined targets, invalid patterns, myTeamSpeak-only
rules, client/bulk bans, mass deletion, pagination/large lists, restricted-role
permissions and other versions remain in #441. No new notification is claimed;
prior independent-session event tests remain regression evidence. Actual voice
clients need a separate fixture or manual verification. TS6 RAW and Web Query
remain unclaimed. Other pending commands remain in the overall inventory;
file-transfer reliability/NAT endpoints are #442 scope.

Official [TS6 beta13.1 release notes](https://github.com/teamspeak/teamspeak6-server/releases/tag/v6.0.0-beta13.1),
[JUnit 6.1.3](https://docs.junit.org/6.1.3/overview.html) and
[Failsafe verify usage](https://maven.apache.org/surefire/maven-failsafe-plugin/usage.html)
were reviewed. The release notes describe server-stop fixes, not ban semantics;
acceptance here comes from actual help/output and executed operations.
