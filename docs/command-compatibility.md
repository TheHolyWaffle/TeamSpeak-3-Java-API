# Verified ServerQuery command and event inventory

Task #441 accumulates its remaining acceptance work on one implementation PR.
Previously merged slices verify the core
channel domain, named channel/group permission operations, online text messages,
selected server information/configuration operations, online query-client administration,
file-directory administration, privilege-key administration, ban-rule administration,
global query-login administration, instance/virtual-server log retrieval,
sequential virtual-server lifecycle, and channel configuration.
These slices do not complete the issue or establish whole-API TS6 support.
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
| Servers | Login/version/selection; server info/list/port lookup, host/instance/connection information; name/welcome/host-message edits and independent-session edit notifications. See [server evidence](server-compatibility.md); sequential create/start/stop/delete and lifecycle errors in [lifecycle evidence](server-lifecycle-compatibility.md); message tests document rejected selection and cleared context | Other server/instance edits, snapshots, concurrent/multi-running-server lifecycle, nondefault creation, property combinations and restricted-account probes |
| Channels | Permanent channel create/list/info/find, name/topic/description edit, parent move with first-sibling order, force delete; UTF-8 and literal protocol escapes; deleted channel error 768; semi-permanent/permanent transitions, password flags, limits and Opus settings with independent description/password notifications. See [configuration evidence](channel-configuration-compatibility.md) | Temporary/default channels, restart persistence, password/limit enforcement, other codecs/quality boundaries, banners, other property combinations and restricted-role probes |
| Clients | Query-client list/info/name lookup, self nickname update, description edits, independent-session joins/admin moves/self moves/leaves and error recovery. See [client evidence](client-compatibility.md); existing reconnect probes | Other properties, multiple sessions, bulk/password moves and kicks/bans |
| Permissions/groups | Server metadata and name/ID lookup, effective-value lookup, assignment search; named channel/server-group/channel-group permission add/read/update/delete; group create/list/rename/delete and regular group copy/overwrite. See [permission evidence and migration](permission-compatibility.md) | Template/query group copies, automatic assignments, permission reset and denied-account probes |
| Messages/events | Channel create/edit/move/delete and online server/channel/private text messages from an independent query session; text subscriptions/unregister/re-register, decoded payloads and invalid-target errors. See [message evidence](message-compatibility.md); existing overlapping subscription/reconnect probes | Additional server-edit properties and other events; audio remains outside ServerQuery scope |
| Files | Directory create/list/nested rename/cross-channel move/scalar and bulk delete; list metadata, empty transfer list and path/file-info errors. See [file evidence](file-compatibility.md) | Actual files, positive/bulk file-info, icons, active-transfer metadata and remaining command/error inventory; transfer reliability and container/NAT endpoints belong to #442 |
| Administrative utilities | Privilege-key create/list/delete for both types, metadata and query-client consumption rejection; see [key evidence](privilege-key-compatibility.md). Ban-rule create/list/delete and metadata/errors; see [ban evidence](ban-compatibility.md). Global query-login create/list/authenticate/delete and deleted-credential rejection; see [login evidence](query-login-compatibility.md). Instance/virtual-server log reads, limits/metadata and invalid-severity errors; see [log evidence](log-compatibility.md). Existing version/selection error recovery and controlled `help` | Successful regular-client key consumption/events/custom metadata, invalid references/limits/restricted roles; custom log-write visibility, log pagination/rotation/severities/denied roles, ban enforcement/client bans/bulk deletion, complaints, virtual-server query logins/rotation/pattern limits and remaining utilities |

The [headless regular-client suite](regular-client-compatibility.md) additionally verifies
persistent database/custom-property administration, UID lookup, client/channel-client
permissions and overview, memberships, complaints, offline messages, actual poke and
broadcast receipt, privilege-key consumption, and regular-client joins/leaves on all
three targets. These use a separate full UDP fixture, never desktop automation.

Existing query probes are in `ServerQueryCompatibilityIT`; scripted codec, lifecycle,
queue and event tests remain regression evidence, not live command acceptance.
`RegularClientCompatibilityIT` tests actual regular-client join/leave behavior through
the full UDP protocol. Query-only tests provide no such guarantee; neither suite
claims audio transmission or official desktop-client feature compatibility.

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
