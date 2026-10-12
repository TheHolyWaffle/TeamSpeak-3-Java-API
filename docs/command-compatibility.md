# Verified ServerQuery command and event inventory

> Current scope: TS3 RAW and SSH only. TS6 functionality and tests are removed.
> Any TS6 observations below are retained historical findings from merged PRs,
> not current support claims or release acceptance requirements.

Task #441 verifies the library's ServerQuery command families and notifications
against TS3. Previously merged #465–#476 and the final accumulated #477 changes
provide evidence; historical TS6 observations are not part of current acceptance.
Each operation below has executed tests, including expected rejection boundaries.
Help alone or a similar method name does not establish compatibility.

## Exact targets and evidence

Use Java 25 without preview features and `./mvnw -B -ntp -Pintegration verify`.
The pinned versions, digests, architectures, authenticated readiness and required
CI setup are documented in [integration tests](integration-tests.md). Every tested
operation below runs against TS3 **3.13.8 RAW** and TS3 **3.13.8 SSH**. TS6 RAW and Web Query are not claimed.

`ServerQueryCompatibilityIT.channelDomain` uses a separate actor query session to
create, edit, move and delete channels observed by a subscribed query session.
It captures live `help` for every tested channel command, decoded response fields,
event payloads and errors in `target/compatibility/<target>-channels.txt`.
`<target>.txt` records version/build, image and transport negotiation evidence.
The required Docker CI uploads these reports even after failures. Help establishes
syntax; assertions against actual operations establish the supported subset.
Local Apple Silicon uses emulated amd64 TS3; CI tests amd64.

## Inventory by domain

| Domain | Verified operations on TS3 RAW and SSH | Explicit boundaries / follow-up |
| --- | --- | --- |
| Servers | Info/list/port selection, host/instance/connection/binding metadata; configuration edits and independent edit events; owned create/start/stop/delete, snapshots, instance edits, restart/default-channel persistence and process stop | Bundled license rejects a second concurrently running server with 2816; licensed concurrency is not claimed. [Server](server-compatibility.md), [lifecycle](server-lifecycle-compatibility.md), [snapshot](server-snapshot-compatibility.md), [additional evidence](additional-command-compatibility.md) |
| Channels | Permanent/semi-permanent/temporary/default creation and lifetime; list/info/find/edit/move/delete; Unicode/escape text, password flags and actual password moves, capacity/Opus metadata, description/password and create/edit/move/delete events | Generic property editing does not imply every combination or desktop rendering. Invalid flags 775; invalid quality permission 2568; deleted channel 768. [Configuration](channel-configuration-compatibility.md), [temporary lifetime](additional-command-compatibility.md) |
| Clients | Query-client CRUD and independent join/move/leave; full UDP regular-client join/leave; persistent database/UID/custom properties; bulk/password moves, channel/server kicks, bans and complaints | Query whoami database IDs are not persistent regular identities. Full UDP fixture has no audio source and is not desktop-client feature parity. [Client](client-compatibility.md), [regular client](regular-client-compatibility.md), [moderation](additional-command-compatibility.md) |
| Permissions/groups | Server metadata/name/ID/effective-value lookup and search; all five permission scopes; overview/memberships; regular/template/query server groups; regular/template channel groups; copies/overwrite/rename/delete; automatic add/delete/reset; denied-account metadata | Query channel-group creation rejected1538 despite help. Permission IDs come from server metadata, never embedded universal constants. Reset clears selection; reselect before server-specific commands. [Permission](permission-compatibility.md), [copies](group-copy-compatibility.md), [modes](additional-command-compatibility.md) |
| Messages/events | Independent-session server/channel/private messages, overlapping subscriptions and unregister/re-register; regular-client actual poke/broadcast receipts; offline message CRUD/read flags; successful single-use server/channel keys and events/custom properties | Absent offline body is null; empty body remains empty. Used regular-client key returns3840. Query-client key use rejects516. Voice/audio and screen sharing are outside ServerQuery scope. [Message](message-compatibility.md), [regular client](regular-client-compatibility.md), [channel keys](additional-command-compatibility.md) |
| Files | Directory create/list/rename/move/delete; positive scalar/two-file bulk metadata; Unicode content upload/exact download; active-transfer owner/ID/state and stop; icon CRC/list/download/delete | Bounded test sockets explicitly adapt Docker endpoint. Production helper reliability, resume/cancellation/length limits/NAT remain #442. [Directory](file-compatibility.md), [content evidence](additional-command-compatibility.md) |
| Administrative utilities | Query-login create/list/auth/delete/replacement, SQL pattern matching; explicit logout/re-login; ban CRUD/client enforcement condition/bulk delete, complaints; four custom log severities/routing/reverse/cursor continuation and denied log reads | Nickname-ban test observes connection failure with rule present and success after removal; no ban-specific handshake code or same-UID reconnect guarantee. Log windows are not durability, rotation or a full-file guarantee. [Login](query-login-compatibility.md), [ban](ban-compatibility.md), [log](log-compatibility.md), [additional evidence](additional-command-compatibility.md) |

The [complete protocol-builder mapping](command-coverage-map.md) accounts for every
command family in production, including expected unsupported outcomes. Remaining
release checks are kept explicitly in PR #477 until green at final HEAD. Future API
facades/models and production transfer reliability keep their own #443/#444/#442 scope.

Existing query probes are in `ServerQueryCompatibilityIT`; scripted codec, lifecycle,
queue and event tests remain regression evidence, not live command acceptance.
`RegularClientCompatibilityIT` tests actual regular-client join/leave behavior through
the full UDP protocol. Query-only tests provide no such guarantee; neither suite
claims audio transmission or official desktop-client feature compatibility.

## Observed channel differences and errors

- `notifychannelmoved` uses **`order`**, while channel list/info/create use
  **`channel_order`**, on both TS3 targets. The event accessor now reads `order`.
  A missing order still returns `-1`; zero is a valid first-sibling order.
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
Remaining operations stay within the same #441 implementation PR before it can be closed.

Tool documentation reviewed: [JUnit parameterized tests](https://docs.junit.org/6.1.3/overview.html),
[Testcontainers authenticated waits](https://java.testcontainers.org/features/startup_and_waits/),
[Maven Failsafe verify](https://maven.apache.org/surefire/maven-failsafe-plugin/),
[Adoptium archive installation](https://adoptium.net/installation/archives/).
