# Verified privilege-key administration

> Current scope: TS3 RAW and SSH only. TS6 functionality and tests are removed.
> Any TS6 observations below are retained historical findings from merged PRs,
> not current support claims or release acceptance requirements.

This focused #441 slice covers TS3 **3.13.8 RAW** and TS3 **3.13.8 SSH** using the existing pinned fixtures. See the
[overall inventory](command-compatibility.md) and [integration setup](integration-tests.md).
It verifies administrative create/list/delete operations and serveradmin query-client
rejection of key consumption. Successful regular-client consumption and its
notification remain unverified.

## Verified subset and evidence

`ServerQueryCompatibilityIT.privilegeKeyDomain` uses separate administrator/observer
and actor query sessions with distinct online client IDs. It creates disposable
server and channel groups plus a permanent channel, then verifies:

- Server-group and channel-group key creation through both convenience methods,
  plus generic enum-based creation with a null description.
- Listing returns each generated token, exact type/group/channel references,
  description and a creation date within one minute of creation. Descriptions
  preserve Japanese text, pipes, slashes and literal `\s`, `\p`, `\n` escapes.
  Actual control-character retention and length limits are not claimed.
- Null description is omitted by the command builder; the server supplies an
  explicitly present empty `token_description` field. Both pins provide
  `token_created` in epoch seconds; do not extrapolate the milliseconds observed
  in TS6 file-directory rows to every timestamp field.
- Three newly generated keys are listed alongside the original key set. Valid
  server/channel keys remain listed after rejected consumption attempts.
- All three keys are deleted through cleanup resources, and listing returns to
  exactly the original token set. Groups, channel, subscription and both query
  sessions also close on failure. Try-with-resources preserves original failures
  while still attempting cleanup of every registered resource.
- Delete an already removed key and attempt query consumption of that removed
  token; actual server errors remain caller-visible and later commands succeed.

Live help for `privilegekeyadd`, `privilegekeylist`, `privilegekeydelete` and
`privilegekeyuse`, decoded key maps, error metadata and a private-message event
fence are recorded in `target/compatibility/<target>-privilege-keys.txt`. Required
Docker CI uploads these reports even after failures alongside version/build/image/
transport evidence and test reports. Generated tokens belong to disposable
fixtures and are deleted by the test. No production key is used.

## Query-client rejection and event boundary

Both known-valid key types fail with **`516 / invalid client type`** when the
actor's serveradmin query session calls `usePrivilegeKey`. The test exercises
both the wrapper and string overloads. No key is consumed by these failures.
The same session also returns 516 for a removed token, so this error does not
prove that a supplied token exists or is valid.

The observer subscribes to `SERVER` and private text events. After the two known-key
rejections, the actor sends a private message to the observer. That later event
fences earlier notification frames on the same ordered registration; the
privilege-key-used queue must remain empty. The message's invoker/recipient and
payload are checked, and observer unknown/malformed/listener-failure/dropped counters
must all be zero. This demonstrates rejected query-client requests do not emit
key-use notifications; it does not verify successful consumption payloads.

Do not infer regular/voice-client behavior from serveradmin query sessions.
Successful key consumption, one-time deletion, resulting membership, custom client
metadata and `PrivilegeKeyUsedEvent` field decoding require evidence from the
official TeamSpeak client. No Testcontainers guarantee is made
for those operations.

## Fields, errors and migration

Both pins' captured key rows contain `token`, `token_type`, `token_id1`,
`token_id2`, `token_created`, `token_description` and `token_customset`. Type `0`
is server group (channel ID `0`); type `1` is channel group with the selected
channel ID. IDs come from groups/channels created on the same server, rather
than assumed cross-server identities. Unknown fields remain available through
`getMap()` and the controlled raw-command API. Custom-set contents and other
property combinations are not verified.

| Operation | Error ID | Message | Observed result |
| --- | --- | --- | --- |
| Query-client use of a valid server/channel key | 516 | `invalid client type` | Key remains listed; no use event before the message fence |
| Delete an already removed key | 3 | `ok` | Throws `TS3CommandFailedException`; later operations remain usable |
| Query-client use of a removed key | 516 | `invalid client type` | Does not establish token validity |

The removed-key deletion returns a **nonzero** error ID despite message `ok` on
all three pins. Test the error ID/status, not the text alone. The library preserves
this metadata rather than silently treating the operation as successful. These
observations are not a universal error mapping or a restricted-account guarantee.

This slice has internal **`test:`** release intent for planned 2.0.0 acceptance.
No runtime, public API, dependency or default changes are introduced. Migration
guidance: query clients can administer keys but the verified serveradmin sessions
cannot consume them; handle the actual command failure and distinguish it from
missing keys. Sync/async Javadocs document that boundary. No automatic retry,
consumption emulation or group assignment is added.

## Remaining scope and official sources

These paragraphs record the original slice boundary. The current whole-issue
TS3 evidence and follow-up ownership are in the [acceptance inventory](command-compatibility.md)
and [additional command tests](additional-command-compatibility.md).

Successful regular-client consumption/events, automatic membership/deletion,
custom metadata, invalid group/channel references, description limits,
restricted accounts and other versions remain in #441. Other administrative
utilities are also still pending. TS6 RAW, Web Query and actual voice-client
join/leave behavior are unclaimed; file-transfer reliability/NAT endpoints remain
#442 scope.

Official [TS6 beta13.1 release notes](https://github.com/teamspeak/teamspeak6-server/releases/tag/v6.0.0-beta13.1),
[JUnit 6.1.3](https://docs.junit.org/6.1.3/overview.html) and
[Failsafe verify usage](https://maven.apache.org/surefire/maven-failsafe-plugin/usage.html)
were reviewed. The release notes concern server-stop fixes; the privilege-key
acceptance here comes from actual help/output and executed probes.
