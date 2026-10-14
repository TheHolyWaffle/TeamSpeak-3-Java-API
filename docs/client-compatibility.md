# Verified online query-client administration

> Current scope: TS3 RAW and SSH only. TS6 functionality and tests are removed.
> Any TS6 observations below are retained historical findings from merged PRs,
> not current support claims or release acceptance requirements.

This focused #441 slice tests TS3 **3.13.8 RAW** and TS3 **3.13.8 SSH** with the existing pinned containers. See the
[overall inventory](command-compatibility.md) and [integration setup](integration-tests.md).
It covers authenticated query clients, not actual voice clients or the entire
client administration API.

## Verified subset

`ServerQueryCompatibilityIT.clientDomain` uses three independent query sessions:
an observer, an administrator and a subject. The observer subscribes to `SERVER`
and `CHANNEL, 0` before the subject selects the server. Channel subscription is
needed for the move notifications exercised here; server subscription alone did
not deliver them in the live probe.

The three target cases verify:

- Subject joins with a distinct live client ID, matching channel/database IDs,
  nickname, UID and query-client type (`1`). The join has reason `0` and no invoker.
- Self nickname update through `updateClient(Map)` preserves Japanese text, pipes
  and literal backslash escapes. `whoAmI`, `clients().info` and `clients().list` agree
  on the subject's identity and nickname.
- Exact case-sensitive and case-insensitive name lookup, and `clients().search`
  for the exercised full name, return the subject. An absent name returns an empty
  `Optional` for exact lookup and an empty list for search. This does not establish every
  search pattern's semantics.
- Administrator description edits through scalar and map overloads preserve
  UTF-8, pipes, slashes and literal protocol escapes. Actual newline, tab and
  carriage return in `before\nmiddle\tend\r tail` are removed: readback is
  `beforemiddleend tail`. Raw `clientinfo` confirms the normalized value is sent
  by the server, rather than produced by wrapper decoding.
- Administrator moves the subject to a permanent channel; the observer receives
  reason `1`, subject/target IDs and the administrator's exact invoker ID/name/UID.
  Subject identity and detailed info agree on the new channel.
- Move to an invalid channel fails and leaves the subject in place. The subject's
  subsequent self move back has reason `0` and no invoker; its event fences the
  preceding rejected move, which must not add a notification.
- Subject exits normally; the observer receives reason `8` and matching subject
  and source-channel IDs. Detailed lookup then fails, client list omits it, and
  subsequent server commands remain usable.
- Description restoration runs through try-with-resources even on failure, with
  successful restoration read back. Created channels, subscriptions and query
  sessions close on failure. Observer malformed/listener-failure/dropped counters
  must all remain zero.

Live help for `clientlist`, `clientinfo`, `clientfind`, `clientupdate`, `clientedit`
and `clientmove`, decoded maps, raw normalized description, events and errors are
saved in `build/compatibility/<target>-clients.txt`. Required Docker CI uploads
these reports even after failures. The existing client-list API requests its
normal property flags; this does not establish all raw option combinations.

## Fields and identity

The pinned TS6 join payload adds `client_is_streaming` and `client_user_tag` to
TS3's fields. Detailed client info adds those two plus `client_public_key_raw`.
Tests explicitly check their presence/absence. The captured query-client list
field names are the same across the pins. These are observed schema differences,
not verification of streaming, key or tag semantics. Unknown properties remain
available through `getMap()` and the controlled raw-command API.

All three sessions use the same serveradmin account. They have distinct online
client IDs but share the account UID and database ID. Do not assume a UID or
client database ID uniquely identifies one online query session. UID-based lookup
and multiple-session selection semantics are not claimed by these tests.
Absent invoker fields are empty `Optional`s since #444 ([models guide](models-and-values.md)).

## Errors and migration

| Operation | Error ID | Message | Recovery |
| --- | --- | --- | --- |
| Move to nonexistent channel | 768 | `invalid channelID` | Subject remains in place; self move succeeds |
| Detailed lookup after subject disconnects | 512 | `invalid clientID` | Listing and subsequent commands succeed |

This slice has internal **`test:`** release intent for 2.0.0 acceptance. No runtime,
public API, dependency or default changes are introduced. Migration guidance:
read back description values when exact control-character retention matters;
these servers remove the exercised control characters. Use live client IDs for
session-specific administration, and inspect event field presence before
interpreting default invoker values. No client-side normalization is added.

## Remaining scope and official sources

These paragraphs record the original slice boundary. The current whole-issue
TS3 evidence and follow-up ownership are in the [acceptance inventory](command-compatibility.md)
and [additional command tests](additional-command-compatibility.md).

Database administration, other client properties, UID lookup, bulk/password moves,
kicks/bans and restricted-account behavior remain in #441. Query-client events
provide no guarantee of voice-client join/leave behavior, media/hardware state or
rendering; those require a separate client fixture or manual verification. Other
versions and TS6 RAW are unclaimed; Web Query evidence is in [Web Query](web-query.md). Transfer reliability/NAT endpoints
remain #442 scope.

Official [TS6 beta13.1 release notes](https://github.com/teamspeak/teamspeak6-server/releases/tag/v6.0.0-beta13.1),
[JUnit 6.1.3](https://docs.junit.org/6.1.3/overview.html) and
[Gradle test tasks](https://docs.gradle.org/current/userguide/java_testing.html)
were reviewed. The release notes describe server-stop fixes, not client-operation
coverage; the acceptance above comes from actual help/output and executed tests.
