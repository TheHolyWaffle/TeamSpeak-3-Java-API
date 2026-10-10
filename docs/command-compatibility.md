# Verified ServerQuery command and event inventory

Task #441 is being delivered in focused PRs. The implemented slices verify the core
channel domain and named channel/group permission operations. It does not complete the issue or establish whole-API TS6 support.
The inventory below is the release acceptance boundary, not a promise based on
similar method names or server help alone.

## Exact targets and evidence

Use Java 25 without preview features and `./mvnw -B -ntp -Pintegration verify`.
The pinned versions, digests, architectures, authenticated readiness and required
CI setup are documented in [integration tests](integration-tests.md). Every tested
operation below runs against TS3 **3.13.8 RAW**, TS3 **3.13.8 SSH**, and TS6
**6.0.0-beta13.1 SSH**. TS6 RAW and Web Query are not claimed.

`ServerQueryCompatibilityIT.channelDomain` uses a separate actor query session to
create, edit, move and delete channels observed by a subscribed query session.
It captures live `help` for every tested channel command, decoded response fields,
event payloads and errors in `target/compatibility/<target>-channels.txt`.
`<target>.txt` records version/build, image and transport negotiation evidence.
The required Docker CI uploads these reports even after failures. Help establishes
syntax; assertions against actual operations establish the supported subset.
Local Apple Silicon uses emulated amd64 TS3 and native arm64 TS6; CI tests amd64.

## Inventory by domain

| Domain | Verified operations across the three targets | Remaining #441 coverage |
| --- | --- | --- |
| Servers | Existing login/version, virtual-server selection, invalid server ID 2816 and post-error framing probes | Server/instance info and edits, virtual-server lifecycle, snapshots and differences |
| Channels | Permanent channel create/list/info/find, name/topic/description edit, parent move with first-sibling order, force delete; UTF-8 and literal protocol escapes; deleted channel error 768 | Other property combinations, temporary/semi-permanent/password/default channels, limits/codecs/banners; permission failures and extra description/password notifications |
| Clients | Existing query-client identity, join/move/leave frames and reconnect probes | Client/database administration and properties, kicks/bans, voice-client fixture/manual verification |
| Permissions/groups | Server metadata and name/ID lookup, effective-value lookup, assignment search; named channel/server-group/channel-group permission add/read/update/delete; group create/list/rename/delete. See [permission evidence and migration](permission-compatibility.md) | Client/channel-client permissions, permission overview, group copy/membership/automatic assignments, permission reset and denied-account probes |
| Messages/events | Channel create/edit/move/delete from an independent query session; existing overlapping subscription and reconnect probes | Text messages, server edits, privilege keys and other event payloads; voice-client behavior requires a separate client fixture or manual evidence |
| Files | No cross-server transfer claim | File command inventory, server output/error differences; transfer reliability and container/NAT endpoints belong to #442 |
| Administrative utilities | Existing version/selection error recovery; `help` through the normal command queue | Logs, bans, complaints, tokens, query-login management and remaining utilities |

Existing probes are in `ServerQueryCompatibilityIT`; scripted codec, lifecycle,
queue and event tests remain regression evidence, not live command acceptance.
No Testcontainers result here guarantees actual voice-client join/leave behavior.

## Observed channel differences and errors

- `notifychannelmoved` uses **`order`**, while channel list/info/create use
  **`channel_order`**, on all three targets. The event accessor now reads `order`.
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
fix stopping virtual servers; those lifecycle operations remain unverified in this
slice. Live help is the syntax source for the channel commands. Review release
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
Remaining operations stay within #441 and require subsequent focused PRs before it can be closed.

Tool documentation reviewed: [JUnit parameterized tests](https://docs.junit.org/6.1.3/overview.html),
[Testcontainers authenticated waits](https://java.testcontainers.org/features/startup_and_waits/),
[Maven Failsafe verify](https://maven.apache.org/surefire/maven-failsafe-plugin/),
[Adoptium archive installation](https://adoptium.net/installation/archives/).
