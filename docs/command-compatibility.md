# Verified ServerQuery command and event inventory

> Current scope: TS3 RAW and SSH only. TS6 functionality and tests are removed.
> Any TS6 observations below are retained historical findings from merged PRs,
> not current support claims or release acceptance requirements.

Task #441 verifies the library's ServerQuery command families and notifications
against TS3. Previously merged #465–#476 and the final accumulated #477 changes
provide evidence; historical TS6 observations are not part of current acceptance.
The inventory distinguishes executed tests, expected rejection boundaries and
remaining coverage gaps.
Help alone or a similar method name does not establish compatibility.

## Exact targets and evidence

Use Java 25 without preview features and `./gradlew build integrationTest`.
The pinned versions, digests, architectures, authenticated readiness and required
CI setup are documented in [integration tests](integration-tests.md). Every tested
operation below runs against TS3 **3.13.8 RAW** and TS3 **3.13.8 SSH**. TS6 RAW is not claimed. Web Query
coverage is listed separately in [Web Query](web-query.md#supported-operations).

`ServerQueryCompatibilityIT.channelDomain` uses a separate actor query session to
create, edit, move and delete channels observed by a subscribed query session.
It captures live `help` for every tested channel command, decoded response fields,
event payloads and errors in `build/compatibility/<target>-channels.txt`.
`<target>.txt` records version/build, image and transport negotiation evidence.
The required Docker CI uploads these reports even after failures. Help establishes
syntax; assertions against actual operations establish the supported subset.
Local Apple Silicon uses emulated amd64 TS3; CI tests amd64.

## Inventory by domain

| Domain | Verified operations on TS3 RAW and SSH | Explicit boundaries / follow-up |
| --- | --- | --- |
| Servers | Info/list/port selection, host/instance/connection/binding metadata; configuration edits and independent edit events; owned create/start/stop/delete, snapshots, instance edits, restart/default-channel persistence and process stop | Bundled license rejects a second concurrently running server with 2816; licensed concurrency is not claimed. [Server](server-compatibility.md), [lifecycle](server-lifecycle-compatibility.md), [snapshot](server-snapshot-compatibility.md), [additional evidence](additional-command-compatibility.md) |
| Channels | Permanent/semi-permanent/default configuration; list/info/find/edit/move/delete; Unicode/escape text, password flags, capacity/Opus metadata, description/password and create/edit/move/delete events; restart persistence | Temporary-channel lifetime and regular-client password enforcement are unverified. Invalid flags 775; invalid quality permission 2568; deleted channel 768. [Configuration](channel-configuration-compatibility.md), [additional evidence](additional-command-compatibility.md) |
| Clients | Query-client CRUD and independent join/move/leave | Regular-client administration, persistent database/UID/custom properties, bulk/password moves, kicks, ban enforcement and complaints are unverified. Query whoami database IDs are not persistent regular identities. [Client](client-compatibility.md), [remaining coverage](additional-command-compatibility.md#remaining-regular-client-coverage) |
| Permissions/groups | Server metadata/name/ID/effective-value lookup and search; channel/server-group/channel-group permission assignments; regular/template/query server groups; regular/template channel groups; copies/overwrite/rename/delete; automatic add/delete/reset | Positive client/channel-client permissions, overview, regular-client memberships and restricted-account behavior are unverified. Query channel-group creation rejected1538 despite help. Permission IDs come from server metadata. Reset clears selection. [Permission](permission-compatibility.md), [copies](group-copy-compatibility.md), [modes](additional-command-compatibility.md) |
| Messages/events | Independent-session server/channel/private messages, overlapping subscriptions and unregister/re-register; privilege-key create/list/delete and rejected query-client consumption | Regular-client poke/broadcast receipt, offline-message CRUD and successful key consumption/events/custom properties are unverified. Scripted tests distinguish absent and empty offline bodies. Query-client key use rejects516. Voice/audio and screen sharing are outside ServerQuery scope. [Message](message-compatibility.md), [privilege keys](privilege-key-compatibility.md) |
| Files | Directory create/list/rename/move/delete; positive scalar/two-file bulk metadata; Unicode content upload/exact download; active-transfer owner/ID/state and stop; icon CRC/list/download/delete | Bounded test sockets explicitly adapt Docker endpoint. Production helper reliability, resume/cancellation/length limits/NAT remain #442. [Directory](file-compatibility.md), [content evidence](additional-command-compatibility.md) |
| Administrative utilities | Global query-login create/list/auth/delete; explicit logout/re-login; ban-rule CRUD; four custom log severities/routing/reverse/cursor continuation | Server-bound login replacement, restricted-account denied operations, client ban enforcement and complaints are unverified. Log windows are not durability, rotation or a full-file guarantee. [Login](query-login-compatibility.md), [ban](ban-compatibility.md), [log](log-compatibility.md), [additional evidence](additional-command-compatibility.md) |

The [protocol-builder mapping](command-coverage-map.md) lists every command family
in production, including expected unsupported outcomes and unverified operations.
Remaining release checks are kept explicitly in PR #477 until green at final HEAD. Future API
facades/models and production transfer reliability keep their own #443/#444/#442 scope.

Existing query probes are in `ServerQueryCompatibilityIT`; scripted codec, lifecycle,
queue and event tests remain regression evidence, not live command acceptance.
There is no regular-client fixture in the automated suite. Behaviors requiring a
regular client need verification with the official TeamSpeak client. Query-only
tests provide no guarantee of those behaviors or audio transmission.

## Observed channel differences and errors

- `notifychannelmoved` uses **`order`**, while channel list/info/create use
  **`channel_order`**, on both TS3 targets. The event accessor now reads `order`.
  A missing order fails with `TS3InvalidValueException` since #444 (it returned `-1`
  before); zero is a valid first-sibling order.
- TS6 channel info includes `channel_storage_quota` (4294967295 in the disposable
  default fixture); TS3 channel info omits it. TS6 channel-create notifications
  include `channel_unique_identifier`; TS3 notifications omit it. Both servers
  provide that identifier in channel info. These observations do not establish
  that those properties can be changed, or their behavior on other versions.
- Unknown/new fields remain available through `Wrapper.getMap()` and the raw
  response API. Check field presence before interpreting an optional value;
  wrapper defaults are not evidence that the server supplied a property.
- A deleted channel's `channelinfo` fails with `TS3CommandFailedException`, server
  ID **768**, message **`invalid channelID`**. The stream remains usable afterwards.
  Caller-visible server error metadata is preserved; permission failures must be
  interpreted using the connected server's metadata, not universal numeric IDs.

The pinned TS6 [beta13 release notes](https://github.com/teamspeak/teamspeak6-server/releases/tag/v6.0.0-beta13)
introduce guest query access; these tests use serveradmin and do not claim guest
permission behavior. The [beta13.1 notes](https://github.com/teamspeak/teamspeak6-server/releases/tag/v6.0.0-beta13.1)
fix stopping virtual servers; the [lifecycle slice](server-lifecycle-compatibility.md)
now exercises sequential create/start/stop/delete independently of these channel tests. Live help is the syntax source for the channel commands. Review release
notes and rerun the inventory when upgrading a pin; do not infer compatibility
from a newer version number alone.

## Controlled command escape hatch and migration

`TS3Api.executeRawCommand(String)` and its asynchronous counterpart accept one
already-encoded command. The result exposes decoded rows and original response
text, useful for commands or properties not yet represented by typed methods:

```java
var response = query.getApi().executeRawCommand("help channelinfo");
String help = response.getRawResponse();
```

Values must be encoded using `CommandEncoding.encode`; never concatenate
untrusted command text. Actual control characters, leading whitespace and
noncanonical command names are rejected synchronously. Protocol array separators
are allowed within the one command. The command name remains separate from its
parameters so session restrictions and opt-in read retry policies still apply.
The escape hatch uses the same bounded queue, deadlines, cancellation, framing,
error translation and shutdown as typed methods. It grants no additional server
permissions. Unknown commands are not automatically retried. Session-changing
raw commands remain restricted when a reconnect session is configured. Prefer
`query.exit()`/`close()` for library shutdown and the typed session configuration
for reconnect restoration.

The channel/raw-command slice has additive `feat:` release intent (minor under normal SemVer, included
in the planned 2.0.0 release), with no new runtime dependencies or breaking API
changes. `ChannelMovedEvent.getChannelOrder()` now returns the supplied protocol
order instead of incorrectly returning `-1`. Consumers that treated every move
order as absent should use the real sibling ID or zero. No other channel model
schema/default changes are introduced. The permission slice intentionally removes embedded enum IDs and corrects unknown-name
errors; see its [breaking migration notes](permission-compatibility.md#migration-for-200).
Scripted `OfflineMessageTest` preserves the distinction between an absent body
(`null`) and an explicit empty body (`""`); this is not live offline-message CRUD
acceptance. `CreatedIdTest` verifies that missing, malformed, zero or negative
creation IDs fail with `TS3Exception`, rather than reaching a subsequent command.
Remaining operations stay within the same #441 implementation PR before it can be closed.

Tool documentation reviewed: [JUnit parameterized tests](https://docs.junit.org/6.1.3/overview.html),
[Testcontainers authenticated waits](https://java.testcontainers.org/features/startup_and_waits/),
[Gradle test tasks](https://docs.gradle.org/current/userguide/java_testing.html),
[Adoptium archive installation](https://adoptium.net/installation/archives/).
