# Verified virtual-server lifecycle

> Current scope: TS3 RAW and SSH only. TS6 functionality and tests are removed.
> Any TS6 observations below are retained historical findings from merged PRs,
> not current support claims or release acceptance requirements.

This focused #441 slice verifies TS3 **3.13.8 RAW** and TS3 **3.13.8 SSH** with the existing pinned images. See the
[overall inventory](command-compatibility.md), [server read/edit evidence](server-compatibility.md)
and [integration setup](integration-tests.md).

## Verified operations and ownership

`ServerQueryCompatibilityIT.serverLifecycleDomain` starts a separate disposable
container for each target. It captures live `servercreate`, `serverstart`,
`serverstop` and `serverdelete` help through the existing controlled raw API.
No shared compatibility fixture, user server or persistent workspace data is
stopped or deleted.

- The test obtains the original server ID from `serverlist`, selects it, stops
  it with a Unicode/literal-escape reason, verifies its OFFLINE listing, deletes
  it and checks an empty list. This makes room within the fixture's one-server
  license before creating the test server.
- `servers().create` returns a positive server-assigned ID and voice port plus a
  nonempty admin token. Only creation field names are recorded, never the token.
  The test sets eight slots and a name with Unicode, pipe and literal backslash
  escape text. It accepts the port chosen by the server, without a fixed host
  port or voice connection.
- Creation starts the server on the pinned defaults. Running deletion returns
  **1035 / `server got an invalid status for this operation`**; the server still
  lists as ONLINE and can be selected for information reads.
- Three ONLINE → OFFLINE transitions and two restarts verify the same ID, port,
  name and slot count. Both ID and wrapper start overloads are exercised, as are
  reason-bearing and reasonless stops. Selected-server info preserves the name
  after each restart. OFFLINE remains visible in the `-all` listing, and stopping
  the selected server clears query context to ID 0.
- Cleanup is registered immediately after creation. It inspects state, stops a
  still-running created server and deletes it. The list is empty afterwards.
  The enclosing container disposes all fixture data even if an assertion or
  command prevents query-level cleanup; resource failures preserve the original
  assertion through try-with-resources suppression.
- Start and deletion of the removed ID return **1024 / `invalid serverID`**.
  Stop instead returns **1033 / `server is not running`**, even for the removed
  ID. A later version command succeeds on the same connection.

Lifecycle commands retain five-second deadlines, including the TS6 stop calls;
each target has a 120-second outer timeout that includes its container startup.
Assertions check state directly after command completion, without sleeps,
retries or acceptance based solely on a successful response. Reports at
`target/compatibility/<target>-server-lifecycle.txt` retain help, allowlisted
identity/state metadata, creation field names, query context observations and
actual errors. Required Docker CI uploads reports alongside pinned version,
image/architecture and transport evidence.

## Release intent and migration

Internal **`test:`** intent for planned 2.0.0 acceptance. No runtime, public API,
dependency or default changes. Use the server-assigned ID/port, stop before
removal, and handle actual errors rather than interpreting an invalid status or
missing identity as success. A stop error saying "not running" does not prove
that the ID still exists; inspect the server list when existence matters. Reselect
a running server after stopping a selected one. Account for licensing before creating or starting
additional servers; this test replaces the owned default and runs one server at
a time, without changing license limits.

Creation returns an admin credential, so keep it out of diagnostic maps and
reports. Names and reasons pass through the existing command encoder; reason
acceptance does not establish an event payload or voice-client disconnect reason.
A voice port in a response is not proof of host/container reachability.

## Remaining scope and official sources

These paragraphs record the original slice boundary. The current whole-issue
TS3 evidence and follow-up ownership are in the [acceptance inventory](command-compatibility.md)
and [additional command tests](additional-command-compatibility.md).

This covers serveradmin and sequential lifecycle operations on the pinned
fixtures. Concurrent or multi-running-server operations, repeated create/delete
stress and thread-budget accounting, autostart/nondefault creation combinations,
port conflicts, denied roles, snapshots, process shutdown, reboot persistence,
voice-client disconnect/rejoin behavior and other versions remain unverified.
TS6 RAW and Web Query remain unclaimed. Other #441 domain gaps stay in the
inventory; file-transfer reliability/NAT endpoints remain #442 scope.

Official [TS6 beta13.1 release notes](https://github.com/teamspeak/teamspeak6-server/releases/tag/v6.0.0-beta13.1)
report a ServerQuery stop hang and thread-budget cleanup fixes. These tests execute
stop/start transitions with deadlines; they do not validate internal accounting,
Windows socket cleanup or all server fixes from that release. Reviewed
[server configuration](https://github.com/teamspeak/teamspeak6-server/blob/main/CONFIG.md),
[JUnit 6.1.3](https://docs.junit.org/6.1.3/overview.html) and
[Failsafe verify](https://maven.apache.org/surefire/maven-failsafe-plugin/usage.html).
Live help and executed assertions establish the lifecycle subset above.
