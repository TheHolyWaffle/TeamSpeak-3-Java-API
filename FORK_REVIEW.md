# Fork changes worth incorporating before 2.0.0

Reviewed on 2026-10-15 against upstream and local `master` at
`0316359d8d66a3a8a5abc476f79d399ddd0afa4b`.

**Recommendation: incorporate four changes below, adapting them to the 2.0.0 API.**
Two additional convenience features are worth considering after those. Most of
the other fork changes already have equivalents in this branch or target code
that 2.0.0 has replaced. The four recommended additions have now been implemented in the 2.0.0 API.
The sections below preserve the original rationale and validation plan.

## Incorporate

### 1. Expose server-side file transfer stopping

Source: hammermaps,
[`dbc0e4f8405d`](https://github.com/hammermaps/TeamSpeak-3-Java-API/commit/dbc0e4f8405db5f10c632bad73f9a46853a19f72),
with synchronous/asynchronous entry points in
[`700882219125`](https://github.com/hammermaps/TeamSpeak-3-Java-API/commit/700882219125f5fd5d31965701b5fd874870c452)
and [`df8bd57ec287`](https://github.com/hammermaps/TeamSpeak-3-Java-API/commit/df8bd57ec2876c5d17553181637f19cde1d710f9).

**Gap:** `FileApi` exposes transfer listing and data transfer operations, and
`FileTransfer.getServerTransferId()` returns a typed `ServerFileTransferId`, but
there is no typed `ftstop` command or public stop operation. Cancelling a local
future does not provide an operation to stop an arbitrary listed server transfer.

**Proposed change:** add `files().stop(ServerFileTransferId, boolean delete)` in
both APIs and a corresponding command factory. Follow existing file-command
transport restrictions; WebQuery must continue to reject file commands.

**Why now:** completes an existing administration capability with a small API
addition and reuses IDs already exposed to callers.

**Validation to add:** command encoding; sync/async parity; stopping an active
transfer closes its data socket and removes it from the transfer list; verify both
delete flag values and failure behavior. Existing
[FileContentCompatibilityIT](src/test/java/com/github/theholywaffle/teamspeak3/FileContentCompatibilityIT.java)
already exercises a successful `ftstop` through `executeRawCommand`, so it provides
a concrete integration scenario to convert to the typed API.

### 2. Expose custom server log entries

Source: hammermaps,
[`cb5e4b23307c`](https://github.com/hammermaps/TeamSpeak-3-Java-API/commit/cb5e4b23307cf09d9ea35dfc643ad47c5085b0b5),
plus the same sync/async API commits linked above.

**Gap:** `InstanceApi.logEntries` and `VirtualServerApi.logEntries` can read logs,
and `LogLevel` already models the four supported severities, but writing requires
a raw `logadd` command.

**Proposed change:** expose `addLogEntry(LogLevel, String)` in the shared API
surface and add `ServerCommands.logAdd`. Document that routing depends on the
selected server and permissions: `logadd` has no `instance` selector, so the API
must not promise a forced destination that the protocol does not support.

**Why now:** fills a protocol coverage gap using an existing enum. Port only log
writing from the source commit; its accompanying 500-line log-read change is
incorrect for the supported server (see exclusions).

**Validation to add:** all four severity values, message escaping, null/empty
validation, permissions, routing with/without a selected server, and sync/async
parity. Existing
[LogWriteCompatibilityIT](src/test/java/com/github/theholywaffle/teamspeak3/LogWriteCompatibilityIT.java)
already writes all four levels using raw commands and checks their visibility and
routing. Its tests are implementation evidence, not evidence of a test run during
this review.

### 3. Add direct client identity/name lookup by UID and database ID

Source: hammermaps,
[`273084b2a8af`](https://github.com/hammermaps/TeamSpeak-3-Java-API/commit/273084b2a8af67fa2bec8897a97647a3bc210231),
[`1f14c329a128`](https://github.com/hammermaps/TeamSpeak-3-Java-API/commit/1f14c329a128f8b18033cf22f3a89133242583cc),
and the sync/async API commits linked above.

**Gap:** there are no command factories or typed endpoints for
`clientgetnamefromuid` / `clientgetnamefromdbid`. The current
`databaseClients().findByUniqueId` performs `clientdbfind` followed by `clientdbinfo`
when found; that is useful for full details but unnecessarily expensive when only
the nickname and identity are needed. `clients().findByUniqueId` concerns online
clients instead of database identities.

**Proposed change:** add direct identity lookup operations to `DatabaseClientApi`
and its async counterpart, returning a small immutable model with UID,
`ClientDatabaseId`, and last-known nickname. Choose and test missing-client
semantics explicitly; retain the full-info methods. Both protocol commands also
support batched identifiers, which can be added with the same response model.

**Why now:** adds missing protocol coverage and reduces the UID lookup from two
round trips to one for the lightweight use case.

**Validation to add:** online/offline identities, unknown UID/DBID responses,
Unicode nicknames, typed IDs, sync/async parity, and supported transports. Batch
responses need identifier-based association if batching is included.

### 4. Give TS3Listener default no-op methods

Source: hammermaps,
[`fc6a04544ffb`](https://github.com/hammermaps/TeamSpeak-3-Java-API/commit/fc6a04544ffb12d190ccce0d7d05b090382109bc).

**Gap:** all 12 methods in `TS3Listener` are abstract. A caller interested in one
event must extend `TS3EventAdapter` or implement every method.

**Proposed change:** provide default no-op implementations for the 12 callbacks.
Keep the adapter as a compatibility convenience initially; deprecating it is an
independent choice. Update examples to implement only callbacks they need.

**Why now:** removes the single-inheritance restriction imposed by the adapter
and permits future callbacks to be added without requiring every direct listener
implementation to change.

**Validation to add:** a listener implementing one callback compiles, receives that
event, and ignores other event types; existing adapter subclasses still dispatch
correctly. Existing subscription ordering and backpressure behavior must stay intact.

## Consider after the four above

| Change | Source | Assessment |
| --- | --- | --- |
| Small BBCode formatting utility | Songbird0, [`4b6afa96938c`](https://github.com/Songbird0/TeamSpeak-3-Java-API/commit/4b6afa96938ce2e202362e057ea4a98f5a11abb3) | Missing and useful for bot output: bold, italic, underline, color, URL, and nested formatting. Prefer a smaller utility API to the fork's eight-class design. Define literal text/tag parameter handling and test it; the fork's URL “sanitizing” only rejects null/empty strings, and integer colors are rendered as decimal strings. No need to make this a release blocker. |
| Online clients filtered by server group | astrolamb-gaming, [`76aca04278fb`](https://github.com/astrolamb-gaming/TeamSpeak-3-Java-API/commit/76aca04278fb1c74823dd67f42020095e5473d30), [`d817cecc935e`](https://github.com/astrolamb-gaming/TeamSpeak-3-Java-API/commit/d817cecc935edec760397a48348dc6d76217b77c) | Useful convenience, distinct from database membership returned by `serverGroups().members`. Implement a single `clients().list()` followed by filtering on `Client.isInServerGroup(ServerGroupId)`; the fork makes redundant queries and its name overload confuses group IDs with list indexes and prints diagnostics. A typed-ID overload is sufficient initially. |

Barofioso also added group lookup conveniences in
[`295511afe1fa`](https://github.com/Barofioso/TeamSpeak-3-Java-API/commit/295511afe1fa93d5cdd08dda2a76ea20c72d7a11).
Group `findById` / `findByName` helpers could accompany the filtering utility if
there is demand. They do not unlock new protocol functionality; channel lookup
by ID is already available as `channels().info(ChannelId)`.

## Already covered or superseded in 2.0.0

| Fork proposal | Source | Current equivalent / decision |
| --- | --- | --- |
| Optional wrapper property access | hammermaps, `6eeec77b8a14` | `Wrapper.find`, `findInt`, `findLong`, `findDouble`, and typed getters already cover this, with explicit missing/malformed semantics. Do not add a second accessor naming scheme. |
| Virtual threads for callbacks | hammermaps, `d17fa5811ce3` | `TS3Query` already uses virtual threads for user callbacks and completion dispatch. |
| Typed boolean/integer permission enums and client grants | Eremmmm, `7896b4bc06dd`, `8791457988e8`, `b0b00a33fc4e`, `e441da2dbf5e`, `2bb2125fa0ff` | Current `IPermissionType` contains all 319 names from the fork, and `BPermissionType` all 177. `PermissionGrant.of` supports typed integer/boolean values and skip flags for client targets. |
| Wider channel permission values | KaiAnti, `aecb2c6fb3ec` | The motivating icon use case is covered by `IconId` and `PermissionGrant.icon`. The latter encodes the signed 32-bit permission representation explicitly. A blanket `int` → `long` change would bypass that model. See also upstream [issue #135](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/135). |
| Client URI utility | Scrumplex, `c8ac4a90eff9`, `f43563f94759` | `Client.getClientURI()` already exists. |
| Query port range validation | Bosym, `a07197ea179b` | `TS3Config.setQueryPort` already validates 1–65535; it also rejects zero, unlike the fork. |
| File upload/download/delete and async file methods | WolverinDEV, `96b4922578a8`, `15de9a1d58c0`, `75fcc72538e8` | Current `FileApi` / `FileApiAsync`, `FileTransfers`, and command factories provide these capabilities with typed locations and transfer handling. |
| Connection state and connection failure exceptions | WolverinDEV, `2f38a71f4eee`; Wotre, `832e113fee3d` | `TS3Query.isConnected()` and exceptions on connect failure already exist. No `System.exit` fallback is needed. |
| Public custom-command execution | Csedge, `c2a1cbfab74c` | `executeRawCommand` already provides the extension point through the normal queue and transport rules. Exposing internal response helpers and `CommandBuilder` is unnecessary. |
| Renamed/grouped API operations | somefriggnidiot, `0b01e402d4d0`, `9d67dc14baf9`, `afd641a3c8c7` | Current domain APIs already provide the larger redesign. The fork's names and parameter spelling changes target the old monolithic facade. |
| Client unique ID getter | marcdubs, `bc09d7ef1c3b` | `ClientInfo` inherits `Client.getUniqueIdentifier()`. |
| Channel/event IDs, event invokers, larger icon IDs, channel move result | Barofioso, `1fe811461b0f` | Typed getters, inherited `BaseEvent` invoker accessors, `IconId`, and failure-reporting APIs cover the meaningful changes. Do not port the hardcoded 16-second command wait. |
| Gradle migration, newer Java baseline, dependency/release/build changes | moritzwegner, hammermaps, Flaflo, shiftregister-vg, PrinzKenny1 | The current Gradle/Java 25/release setup supersedes these changes. Fork publication coordinates and release download links are not upstream changes. |
| README formatting, historical CI and issue templates | Kakifrucht and other forks | No missing library capability. Documentation related to accepted APIs should be written against 2.0.0 instead of copying 1.x examples. |

## Do not incorporate as submitted

- **500-line log limit:** `cb5e4b23307c` and its API follow-ups change 100 to 500.
  The official 3.13.8 `serverquerydocs/logview.txt` specifies `lines={1-100}` and a
  default of 100, matching the current implementation. Keep the limit. This is
  separate from accepting `logadd`.
- **Unicode rejection:** Csedge's
  [`2b879be0f9d2`](https://github.com/Csedge/TeamSpeak-3-Java-API/commit/2b879be0f9d202c0bc01cf3f0d3d104f1daef782)
  checks only the first UTF-16 character and rejects backspace, form feed, bell,
  and vertical tab even though `CommandEncoding` explicitly escapes those
  protocol characters. It is not a sound general validator. Any further input
  restrictions need independent protocol evidence and full-string tests.
- **Channel creation followed by name lookup:** Csedge's
  [`1ee4a43437dd`](https://github.com/Csedge/TeamSpeak-3-Java-API/commit/1ee4a43437dd717d44a1f951905d00b843c178d7)
  adds a second, case-insensitive lookup after creating a channel. Current
  `ChannelApiAsync.create` reads the returned `cid` directly. Keep that behavior;
  an extra lookup introduces ambiguity and a race.
- **Client-info caching flag:** P-Kito's
  [`a7c80df90314`](https://github.com/P-Kito/TeamSpeak-3-Java-API/commit/a7c80df90314dd5d362b3332b1902f1845bf48c7)
  adds only configuration storage/accessors. It contains no cache implementation
  or invalidation policy. It does not solve retrieving departed-client details.
- **Sealing the event hierarchy / making every event final:** hammermaps's
  `61a00aa5d76f`, `cc0b9f808ec5`, and follow-ups remove extension points. Exhaustive
  switching is a possible design goal, but the fork demonstrates no missing
  behavior or bug that warrants imposing this restriction. Do not bundle it with
  default listener methods.
- **Adapter deletion, syntax-only modernization, `@Serial` annotations:** adapter
  removal is unnecessary for default callbacks; stylistic modernization and
  annotations can be routine cleanup rather than release functionality.
- **Generated/experimental test branches:** Diffblue-benchmarks and Csedge add
  tests and harness/build setup for earlier architectures. Do not import the
  suites wholesale. Port focused scenarios only when they expose a gap in the
  current tests; this review did not establish a new production bug from them.

## Evidence and coverage

The audit discovered **110 forks**, fetched **401 branch refs** across **107
accessible forks**, and found **161 distinct fork-only commits**: 146 candidate
commits representing 145 patches, eight merges, five upstream-equivalent patches,
and two empty commits. The baseline excluded ancestry on all upstream branches
and tags, not only master. Recommendations above were then checked against the
actual current master source, rather than treating patch-ID absence as a missing
feature.

Production changes and the eight merge combined diffs were examined; the latter
contained no combined conflict-resolution diffs. Remaining historical test,
build, and documentation changes were screened by purpose. Each of the four
recommended features is still present at the scanned hammermaps master tip
`f746e4277255`, so these are not merely commits that disappeared later in that
fork's history. No open upstream PRs were returned when checked during review.

Three fork URLs listed by GitHub could not be fetched and also returned 404 from
the repository metadata API: `akibhightech/TeamSpeak-3-Java-API`,
`ZamirSadekinChowdhury/TeamSpeak-3-Java-API`, and
`Sjenica1/TeamSpeak-3-Java-API`. This review cannot account for their changes.
Deleted branches, unreachable commits, and fork tags are outside the audit's
coverage.

Protocol syntax was checked against text help bundled in the
[official TeamSpeak 3.13.8 server distribution](https://files.teamspeak-services.com/releases/server/3.13.8/teamspeak3-server_linux_amd64-3.13.8.tar.bz2),
the same server version pinned by this repository's integration fixture. The
relevant help files are saved in `build/fork-audit/evidence/`. In particular,
`ftstop`, `logadd`, and both name lookup commands exist, and `logview` specifies
the 100-line limit. This was a source/documentation review; no live server tests
were run for the recommendations.

Raw inventory, branch locations, complete diffs, and scan failures are available
in `build/fork-audit/report.md` and
`build/fork-audit/report.json`. Reproduce the collection with:

```sh
python3 scripts/audit_forks.py
```

The script currently exits 1 because of the three inaccessible forks while still
writing its report. The raw reports and evidence are ignored build artifacts;
this review preserves the decisions and source links in the repository.
