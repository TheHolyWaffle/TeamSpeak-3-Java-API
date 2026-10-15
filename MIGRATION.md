# Migrating from 1.x to 2.0

2.0.0 is a major release with intentional source, binary and behavior breaks. Nothing is kept for
compatibility: recompile against 2.0, then work through the checklist below. The
[Javadoc](https://www.javadoc.io/doc/com.github.theholywaffle/teamspeak3-api) describes every type in
detail; this guide only covers what changed.

## Checklist

1. [Move to Java 25](#java-25-and-dependencies) and replace the standalone JAR with the regular
   dependency.
2. [Add one SLF4J 2.0 provider](#logging).
3. [Call the domain APIs](#api-shape) instead of the flat `TS3Api`/`TS3ApiAsync` methods; the
   [operation inventory](#operation-inventory) maps every 1.3.1 method.
4. [Replace `CommandFuture`](#futures) with `CompletableFuture`.
5. [Handle `Optional` results and the new exception types](#errors-and-absent-results).
6. [Update model getters](#models-and-values) to the typed values.
7. [Review configuration, deadlines and closing](#configuration-and-closing).
8. [Provision SSH host keys](#ssh-host-keys-and-transports) and check the
   [changed defaults](#changed-defaults).
9. [Move reconnect session state into the configuration](#reconnect-and-retry).
10. [Use subscription handles for events](#events) and size the queues.
11. [Check file transfer limits](#file-transfers).
12. Review the [behavior changes the compiler cannot find](#behavior-the-compiler-cannot-find).

## Java 25 and dependencies

- Java 8–24 are not supported; classes are Java 25 bytecode (class file version 69). The Maven
  coordinates `com.github.theholywaffle:teamspeak3-api` and the package names are unchanged.
- The `with-dependencies` JAR and the `full` Maven profile are gone. Depend on the regular JAR with
  Maven or Gradle; its POM brings in SSHJ, Bouncy Castle, the SLF4J 2.0 API and Gson. Without a build
  tool, collect the runtime dependencies once, for example with
  `mvn dependency:copy-dependencies -DincludeScope=runtime`, and run with `java -cp "app.jar:lib/*"`.
- On the module path the JAR is the automatic module `com.github.theholywaffle.teamspeak3`:
  `requires com.github.theholywaffle.teamspeak3;`.
- Copy examples from [`src/example/java`](src/example/java), not from 1.x.

## Logging

The library uses the SLF4J **2.0** API and ships no provider. SLF4J 1.7 bindings (such as
`slf4j-simple` 1.7 or `slf4j-log4j12`) are ignored: add exactly one 2.0 provider, or SLF4J prints a
warning and discards the output. The standalone JAR's bundled SimpleLogger is gone;
[`src/example/simplelogger.properties`](src/example/simplelogger.properties) has the same
configuration. Communication logs and the `toString()` of commands, responses and configuration
redact passwords, privilege keys, API keys and file transfer keys.

Disconnects no longer log at ERROR. The cause (timeout, I/O error, server close) logs at DEBUG; the
disconnect itself logs once, at WARN without reconnecting or INFO with a reconnect strategy. ERROR is
reserved for failures the application cannot otherwise observe, such as exceptions thrown by
listeners and callbacks.

## API shape

`TS3Api` and `TS3ApiAsync` keep only the session operations (`login`, `logout`, `whoAmI`,
`selectVirtualServerById`, `selectVirtualServerByPort`, `deselectVirtualServer`, `setNickname`, `updateClient`, `moveQuery`,
`executeRawCommand`). Everything else moved to one domain API per area, each with a sync and an
async variant:

| Accessor | Scope |
| --- | --- |
| `bans()` | Ban rules and banning connected clients |
| `channels()` | Channel list, lookup, creation, editing, moving and deletion |
| `channelGroups()` | Channel groups and their memberships |
| `clients()` | Connected clients: lookup, move, kick, poke, edit |
| `complaints()` | Complaints |
| `databaseClients()` | Database clients and custom properties |
| `events()` | Server notification registration and local listeners |
| `files()` | File repository, transfers and icons |
| `instance()` | Instance info/edit, host info, version, bindings, logs, process stop |
| `messages()` | Private/channel/server/broadcast text and offline messages |
| `permissions()` | Permission metadata and assignments for every holder |
| `privilegeKeys()` | Privilege keys |
| `queryLogins()` | ServerQuery logins |
| `serverGroups()` | Server groups and their members |
| `servers()` | Virtual server lifecycle; info, edit, snapshots and logs of the selected server |

Request objects replace long or nullable positional argument lists:

| Value | Replaces |
| --- | --- |
| `BanRequest.ip(..)` / `name(..)` / `uniqueId(..)` / `myTeamSpeakId(..)` with `with…` | four nullable patterns, a seconds count and a nullable reason |
| `ClientBan.of(ids...)` with `withDuration`, `withReason`, `continuingOnError` | `banClient`/`banClients` overloads |
| `PermissionTarget.serverGroup/channelGroup/channel/client/channelClient`, a sealed interface with one record per holder (`ServerGroupTarget`, `ChannelGroupTarget`, `ChannelTarget`, `ClientTarget`, `ChannelClientTarget`) | five families of add/delete/list methods |
| `PermissionGrant.of(name or enum, value)` with `withNegated()`, `withSkip()` | positional `negated`/`skipped` booleans |
| `FileLocation.of(channelId, path)` or `FileLocation.ofServer(path)` with `withChannelPassword(..)` | path/channel/nullable-password triples and parallel arrays; `ofServer` replaces channel `0` |
| `UploadMode.CREATE` / `OVERWRITE` / `RESUME` | the `overwrite` and `resume` booleans |
| `ChannelGroupFilter.all()` with `inChannel`, `ofClient`, `inGroup` | `-1` "ignore" sentinels |

Other API changes:

- `TS3Query`, `TS3Config`, `ReconnectStrategy` and many models are `final` or records; wrap or compose
  them instead of subclassing.
- `BPermissionType.getId()` and `IPermissionType.getId()` are removed; resolve IDs with
  `permissions().idByName(...)`.
- The public `commands` packages are command builders used by the domain APIs. Their parameter classes
  are records, factories take typed IDs and `FileLocation`, `Command.encoded()` returns the wire text and
  some obsolete overloads are removed. Use
  the domain APIs, or `executeRawCommand` for commands they do not cover.

### Example

Ensure a permanent "Lobby" channel, publish its rules file, and welcome a user by unique identifier.

1.3.1:

```java
static int prepareLobby(TS3Api api, String uniqueId, byte[] rules) {
    Channel existing = api.getChannelByNameExact("Lobby", false);
    int lobby = existing != null
            ? existing.getId()
            : api.createChannel("Lobby", Collections.singletonMap(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
    api.addChannelPermission(lobby, "i_channel_needed_join_power", 10);
    api.uploadFileDirect(rules, "/rules.txt", true, lobby);
    try {
        ClientInfo client = api.getClientByUId(uniqueId);
        api.moveClient(client.getId(), lobby);
        api.sendPrivateMessage(client.getId(), "Welcome to the lobby");
    } catch (TS3CommandFailedException offline) {
        if (offline.getError().getId() != 512) throw offline;
        api.sendOfflineMessage(uniqueId, "Lobby", "Welcome to the lobby");
    }
    return lobby;
}
```

2.0, synchronous:

```java
static ChannelId prepareLobby(TS3Api api, String uniqueId, byte[] rules) {
    ChannelId lobby = api.channels()
            .findByName("Lobby", false)
            .map(Channel::getId)
            .orElseGet(() -> api.channels().create("Lobby", Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1")));
    api.permissions().add(PermissionTarget.channel(lobby), "i_channel_needed_join_power", 10);
    api.files().upload(FileLocation.of(lobby, "/rules.txt"), rules, UploadMode.OVERWRITE);
    api.clients()
            .findByUniqueId(uniqueId)
            .ifPresentOrElse(
                    client -> {
                        api.clients().move(client.getId(), lobby);
                        api.messages().sendPrivate(client.getId(), "Welcome to the lobby");
                    },
                    () -> api.messages().sendOffline(uniqueId, "Lobby", "Welcome to the lobby"));
    return lobby;
}
```

2.0, asynchronous:

```java
static CompletableFuture<ChannelId> prepareLobby(TS3ApiAsync api, String uniqueId, byte[] rules) {
    return api.channels()
            .findByName("Lobby", false)
            .thenCompose(found -> found.map(channel -> CompletableFuture.completedFuture(channel.getId()))
                    .orElseGet(() ->
                            api.channels().create("Lobby", Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"))))
            .thenCompose(lobby -> api.permissions()
                    .add(PermissionTarget.channel(lobby), "i_channel_needed_join_power", 10)
                    .thenCompose(done ->
                            api.files().upload(FileLocation.of(lobby, "/rules.txt"), rules, UploadMode.OVERWRITE))
                    .thenCompose(_ -> api.clients().findByUniqueId(uniqueId))
                    .thenCompose(client -> client.isEmpty()
                            ? api.messages().sendOffline(uniqueId, "Lobby", "Welcome to the lobby")
                            : api.clients()
                                    .move(client.get().getId(), lobby)
                                    .thenCompose(_ -> api.messages()
                                            .sendPrivate(client.get().getId(), "Welcome to the lobby")))
                    .thenApply(_ -> lobby));
}
```

## Futures

`CommandFuture` and its listener interfaces are removed. Async methods and `Command.getFuture()`
return `CompletableFuture`.

| 1.x | 2.0 |
| --- | --- |
| `onSuccess(listener)` | `thenAccept` for short work; `thenAcceptAsync(listener, executor)` for application work |
| `onFailure(listener)` | `whenCompleteAsync((value, failure) -> ..., executor)` or `exceptionally` for recovery |
| `map(fn)` / `then(fn)` | `thenApply(fn)` / `thenCompose(fn)` |
| `immediate(value)` | `CompletableFuture.completedFuture(value)` |
| `ofAll(requests)` | `CommandFutures.all(requests)` |
| `ofAny(requests)` | No direct equivalent: `CompletableFuture.anyOf` also completes on the first failure |
| `getUninterruptibly()` | The synchronous `TS3Api`, or `CommandFutures.join(future)` |
| `set(value)` / `fail(error)` | `complete(value)` / `completeExceptionally(error)` on your own futures only |
| `isSuccessful()` / `hasFailed()` | `state() == Future.State.SUCCESS` / `state() == Future.State.FAILED` |

Callbacks run on one ordered, query-owned worker. Keep inline callbacks short, never wait
synchronously for another command inside one, and move blocking work to your own executor with the
`*Async` methods. Timed `get` and `orTimeout` only stop waiting; the command still runs. Cancelling the
original API future removes a command that was not sent yet.

## Errors and absent results

Every failure is a `TS3Exception` subtype. `isOutcomeUnknown()` tells whether a failed command may still
have run; reconcile such mutations with the server before retrying.

| Type | Meaning |
| --- | --- |
| `TS3CommandFailedException` | The server answered with an error (`getErrorId()`) |
| `TS3TransportException` | Base type for connection failures |
| ↳ `TS3ConnectionFailedException` | Connecting, authentication or initialization failed |
| ↳ `TS3QueryShutDownException` | Closed or disconnected before the command was written |
| &nbsp;&nbsp;↳ `TS3UnknownOutcomeException` | The connection ended after the command was written |
| `TS3TimeoutException` | A queue-wait or response deadline expired |
| `TS3QueueFullException` | Command capacity exhausted; nothing was sent |
| `TS3UnsupportedOperationException` | The configuration or transport does not support the operation |
| `TS3InvalidValueException` | A required response value is missing or malformed |
| `TS3FileTransferFailedException` | The file socket or a caller stream failed |

`TS3UnknownErrorException` and `TS3UnknownEventException` were never thrown and are removed.
`TS3ConnectionFailedException` and `TS3QueryShutDownException` now extend `TS3TransportException`, so
existing catch blocks keep working. Async results complete with these types as the cause;
`CommandFutures.unwrap` returns it.

Nothing returns `null` or `-1` any more:

- Lookups by name, unique identifier or port return `Optional`. For example `getClientByUId` failed
  with error 512 for an offline client; `clients().findByUniqueId` returns an empty `Optional`.
- Searches and lists return an empty list when nothing matches.
- Reads by ID still fail with the server's error when the entity does not exist.
- Required values that are missing fail with `TS3InvalidValueException` instead of defaulting to `-1`,
  `""` or an object full of defaults.

## Models and values

Response models and events are immutable. Getters return typed values: IDs such as `ClientId`,
`ChannelId` or `ServerGroupId`, `IconId`, `Instant`, `Duration`, `Optional*`, unmodifiable lists
instead of arrays, and records such as `Binding` and `Snapshot`. Parameters take the same ID types; create
them with `of(..)`, and `value()` returns the number. ID types are not interchangeable even when their
values are equal. Where TeamSpeak sends `0` for "none", such as the parent of a top-level channel, the
getter returns an empty `Optional` and an ID type never holds `0` (except `ClientId`, whose `0` is the
server as event invoker, and the file transfer IDs). Enums gain static lookups such as `fromIndex`/`fromName`;
`PermissionGroupType`, `PermissionGroupDatabaseType`, `PrivilegeKeyType` and `TextMessageTargetMode`
return a new `UNKNOWN` constant where they returned `null`.

| 1.x | 2.0 |
| --- | --- |
| `wrapper.get/getInt/getLong/getDouble/getBoolean(name)` (defaults `""`/`-1`/`false`) | `find*` (optional) or `require*` (throwing) |
| `DefaultArrayResponse.getFirstResponse()` returning `Wrapper.EMPTY` | `Optional<Wrapper>` |
| `int` client IDs (`clid`) in models, events and parameters | `ClientId` (`int`-backed) |
| `int` client database IDs (`cldbid`) | `ClientDatabaseId`; `value()` is a `long` |
| `int` channel IDs (`cid`, `pid`/`cpid`, `cfid`/`ctid`, channel order, privilege key channel) | `ChannelId` (`long`-backed); `0` is an empty `Optional` |
| `int` server group IDs (`sgid`, default and template groups) | `ServerGroupId` (`long`-backed) |
| `int` channel group IDs (`cgid`, `client_channel_group_id`, default and template groups) | `ChannelGroupId` (`long`-backed) |
| `int` virtual server IDs (`sid`, `whoAmI`, query logins) | `VirtualServerId` (`long`-backed); no selected server or an instance-wide login (`0`) is an empty `Optional` |
| `int` ban and offline message IDs | `BanId`, `MessageId` (`long`-backed) |
| `int` file transfer IDs (`clientftfid`, `serverftfid`) | `ClientFileTransferId`, `ServerFileTransferId` (`int`-backed) |
| `PrivilegeKey.getGroupId()`/`getChannelId()`, `PrivilegeKeyUsedEvent.getPrivilegeKeyGroupId()`/`getPrivilegeKeyChannelId()` | `getGrant()` on both: a sealed `PrivilegeKeyGrant`, either `ServerGroupGrant(serverGroupId)` or `ChannelGroupGrant(channelGroupId, channelId)` |
| `PermissionAssignment.getMajorId()`/`getMinorId()` `int` | `long` |
| `PermissionAssignment` from `getPermissionAssignments` (`permfind`), whose value and flags defaulted | `PermissionSource`, without value and flags; `permissions().overview(..)` still returns `PermissionAssignment` |
| `ServerQueryInfo.getId()`/`getVirtualServerPort()` `0` before a virtual server is selected | `Optional<ClientId>`, `OptionalInt` |
| `Date` timestamps, `int`/`long` durations | `Instant`, `Duration` |
| `Client.getServerGroups()` `int[]`, `getBadgeGUIDs()` `String[]` | `List<ServerGroupId>`, `List<String>` |
| `ClientJoinEvent.getClientServerGroups()` comma-separated `String` | `List<ServerGroupId>` |
| `ChannelBase.getBannerMode()`, `getBannerGraphicsUrl()` | only on `ChannelInfo` |
| `VirtualServerInfo.getBandwidthReceivedLastMinute()` `int` | `long` |
| `ServerGroup.getSaveDb()` `int` | `isSavedInDatabase()` |
| `PrivilegeKey.getType()` `int` | `PrivilegeKeyType` |
| `FileTransferParameters.getStatus()`, `getQueryError()` | `OptionalInt`, `findFailure()` |
| `long` icon IDs in models, events and `files()` icon methods; `0` or `-1` for none | `IconId` (unsigned, also for group lists that send it signed); getters return `Optional<IconId>` |
| signed `int` values of the `i_icon_id` permission | `PermissionGrant.icon(iconId)`; `IconId.fromPermissionValue(value)` reads an `i_icon_id` value |
| `ClientInfo(int, Map)`, `ChannelInfo(int, Map)`, `ClientInfo.getDefaultChannel()` `-1` | `ClientInfo(ClientId, Map)`, `ChannelInfo(ChannelId, Map)`, `Optional<ChannelId>` |
| `Snapshot.get()` | `Snapshot.data()` |

## Configuration and closing

- `TS3Config` is a reusable builder; `new TS3Query(config)` or `config.build()` takes an immutable
  `QueryConfig` snapshot. Later builder changes do not affect existing queries. Invalid values fail
  before connecting: a null host means `127.0.0.1`; blank hosts, partial credentials and SSH without
  credentials are rejected.
- `TS3Query` is `AutoCloseable`: use try-with-resources. `close()` aborts immediately; `exit()` drains
  and sends `quit` within half the close budget. `getState()` reports the lifecycle; a closed query
  cannot connect again, and `connect()` while connected throws.
- `setCommandTimeout(int)` is removed: use `setCommandResponseTimeout(Duration)`. Connect, handshake,
  queue wait, command response and close each have their own deadline; traffic no longer extends a
  pending command's deadline, and a response timeout closes the connection.
- An open query keeps the JVM alive until it is closed.

## SSH host keys and transports

1.x silently accepted unknown SSH host keys and stored them in `~/.ssh/known_ts3_hosts`. 2.0 verifies
them against `~/.ssh/known_hosts` (as `[host]:port` for non-standard ports) and rejects unknown and
changed keys. Choose another policy with `setSshHostKeyPolicy`:

- `SshHostKeyPolicy.knownHosts(path)`: an OpenSSH trust file. Pass `~/.ssh/known_ts3_hosts` to keep
  using keys you reviewed under 1.x.
- `SshHostKeyPolicy.pinnedKey(publicKey)`: one independently verified key.
- `SshHostKeyPolicy.trustOnFirstUse(path)`: stores the first key per host and port and rejects later
  changes.

Custom transports implement `QueryTransportFactory` and are set with `setTransportFactory`.

## Changed defaults

| Setting | 1.3.1 | 2.0 |
| --- | --- | --- |
| SSH host keys | Accept and store unknown keys | Strict `~/.ssh/known_hosts` |
| Command response deadline | 4 s, renewed by any traffic | 4 s, fixed per command; timeout closes the connection |
| Connect, handshake, queue wait, close | Raw TCP: no connect deadline; SSH: the command timeout; queue wait and close unbounded | 4 s each |
| Command queue | Unbounded | 1024 commands, then `TS3QueueFullException` |
| Event delivery | Unbounded queue per listener on an unbounded thread pool | 4 threads, 256 waiting events per subscription, newest dropped on overflow |
| Reconnect backoff attempts | Unlimited | 10 (`withMaxAttempts`) |
| Commands sent before a disconnect | Resent after reconnect | Fail with an unknown outcome; only named safe reads are replayed |
| File transfers | No limits | 4 s connect/read, 5 min per transfer, 16 MiB in memory |
| Logging API | SLF4J 1.7 | SLF4J 2.0 |

Unchanged: raw TCP on port 10011 and SSH on 10022, `FloodRate.DEFAULT` (350 ms), reconnect
`disconnect()` by default and communications logging off.

## Reconnect and retry

Reconnecting stays off by default. With a reconnect strategy:

- Put credentials in `setLoginCredentials` and the virtual server, nickname and event registrations in
  `setSessionConfiguration` instead of `onConnect`. They are restored in order before queued commands
  continue. Keep observation, such as refreshing the query client ID, in `onConnect`.
- While a restorable session is configured, session-changing commands (`login`, `logout`, `use`,
  `clientupdate`, `servernotifyregister`, `servernotifyunregister`) fail with
  `TS3UnsupportedOperationException`. Configure a new query for a different session.
- A command that was sent but not answered is never resent unless you name it in
  `CommandRetryPolicy.safeReads(...)`; it fails with `TS3UnknownOutcomeException`. Do not resubmit
  failed mutations blindly.
- Built-in backoff gives up after 10 attempts and closes the query; `ReconnectingConnectionHandler`
  constructors take the attempt limit explicitly.

## Events

`addTS3Listeners`/`removeTS3Listeners` are removed. `api.events().subscribe(listener)` (or
`query.subscribe`) returns an `EventSubscription`; close it to remove the listener. Registering
notification types with the server is a separate `api.events().register(...)` call. Listeners run on
`setEventCallbackThreads` threads, one event at a time per subscription; when a subscription's queue
(`setListenerQueueCapacity`) is full the newest events are dropped. Overflow, listener failures and
unknown notifications are counted in `query.getEventStatistics()`.

`TS3Listener` now supplies default no-op callbacks. Implement it directly and override only the
events you need. `TS3EventAdapter` has been removed: replace `extends TS3EventAdapter` with
`implements TS3Listener`, or `new TS3EventAdapter()` with `new TS3Listener()` for anonymous listeners.

## File transfers

File transfers have finite deadlines and a 16 MiB limit for in-memory downloads and icons; stream
larger files to your own `OutputStream`. Streams you pass are never closed by the library. Cancelling
the original future aborts the transfer. Behind NAT or Docker, configure a
`FileTransferEndpointResolver` in `FileTransferConfig`. File data still uses a separate, unencrypted
socket, even with SSH.

## Behavior the compiler cannot find

These changes keep their signatures, so recompiling does not reveal them:

- deadlines on every phase, timeouts closing the connection and the finite command queue;
- no automatic resend of sent commands after a reconnect, and a bounded number of reconnect attempts;
- strict SSH host key verification;
- event delivery on several threads, with overflow dropping the newest events;
- `Optional`/empty results instead of `null` or `-1` behind APIs that kept their name;
- secrets redacted in logs and `toString()`;
- an SLF4J 1.7 binding no longer producing output.

## Operation inventory

Every public method of the 1.3.1 `TS3Api` and `TS3ApiAsync` (identical sets of 238 signatures) has one
row. **Retained**: same entry point and parameters. **Replaced**: moved to a domain API or reshaped,
same capability. **Removed**: a convenience overload or variant, with its replacement. ID parameters and
results use the [typed IDs](#models-and-values): `ClientId`, `ClientDatabaseId`, `ChannelId`,
`ServerGroupId`, `ChannelGroupId`, `VirtualServerId`, `BanId` and `MessageId`.

| Previous operation | Status | 2.0 replacement | Notes |
| --- | --- | --- | --- |
| `executeRawCommand(String)` | Retained | `executeRawCommand(line)` |  |
| `addBan(String, String, String, long, String)` | Replaced | `bans().add(BanRequest)` | `BanRequest.ip(..)`/`name(..)`/`uniqueId(..)` with `withDuration`/`withReason` |
| `addBan(String, String, String, String, long, String)` | Replaced | `bans().add(BanRequest)` | `BanRequest.myTeamSpeakId(..)` or `withMyTeamSpeakId(..)`; a myTeamSpeak-only rule is now accepted |
| `addChannelClientPermission(int, int, String, int)` | Replaced | `permissions().add(PermissionTarget, name, value)` | `PermissionTarget.channelClient(cid, cldbid)` |
| `addChannelGroup(String)` | Replaced | `channelGroups().create(name)` |  |
| `addChannelGroup(String, PermissionGroupDatabaseType)` | Replaced | `channelGroups().create(name, type)` |  |
| `addChannelGroupPermission(int, String, int)` | Replaced | `permissions().add(PermissionTarget, name, value)` | `PermissionTarget.channelGroup(id)` |
| `addChannelPermission(int, String, int)` | Replaced | `permissions().add(PermissionTarget, name, value)` | `PermissionTarget.channel(id)` |
| `addClientPermission(int, String, int, boolean)` | Replaced | `permissions().add(PermissionTarget, PermissionGrant)` | `PermissionTarget.client(cldbid)`, `PermissionGrant.of(name, value).withSkip()`; no longer deprecated |
| `addClientPermission(int, IPermissionType, int, boolean)` | Replaced | `permissions().add(PermissionTarget, PermissionGrant)` | `PermissionGrant.of(IPermissionType, int)` |
| `addClientPermission(int, BPermissionType, boolean, boolean)` | Replaced | `permissions().add(PermissionTarget, PermissionGrant)` | `PermissionGrant.of(BPermissionType, boolean)` |
| `addClientToServerGroup(int, int)` | Replaced | `serverGroups().addMember(groupId, cldbid)` |  |
| `addComplaint(int, String)` | Replaced | `complaints().add(cldbid, message)` |  |
| `addPermissionToAllServerGroups(ServerGroupType, String, int, boolean, boolean)` | Replaced | `permissions().addToAllServerGroups(type, PermissionGrant)` | flags via `withNegated()`/`withSkip()` |
| `addPrivilegeKey(PrivilegeKeyType, int, int, String)` | Removed | `privilegeKeys().createForServerGroup(id, description)` | or `createForChannelGroup(id, channelId, description)`; the type-switched form is gone |
| `addPrivilegeKeyChannelGroup(int, int, String)` | Replaced | `privilegeKeys().createForChannelGroup(groupId, channelId, description)` |  |
| `addPrivilegeKeyServerGroup(int, String)` | Replaced | `privilegeKeys().createForServerGroup(groupId, description)` |  |
| `addServerGroup(String)` | Replaced | `serverGroups().create(name)` |  |
| `addServerGroup(String, PermissionGroupDatabaseType)` | Replaced | `serverGroups().create(name, type)` |  |
| `addServerGroupPermission(int, String, int, boolean, boolean)` | Replaced | `permissions().add(PermissionTarget, PermissionGrant)` | `PermissionTarget.serverGroup(id)`; flags via `withNegated()`/`withSkip()` |
| `addServerQueryLogin(String, int)` | Replaced | `queryLogins().create(loginName, cldbid)` | `cldbid` 0 (new identity, no server selected) is `create(loginName)` since #444 |
| `addTS3Listeners(TS3Listener...)` | Removed | `events().subscribe(listener)` | one listener per call; keep the returned `EventSubscription` (same as `TS3Query#subscribe`) |
| `banClient(int, long)` | Replaced | `bans().banClients(ClientBan)` | `ClientBan.of(clid).withDuration(Duration.ofSeconds(s))`; returns `List<BanId>` |
| `banClient(int, long, String)` | Replaced | `bans().banClients(ClientBan)` | add `withReason(..)` |
| `banClient(int, String)` | Replaced | `bans().banClients(ClientBan)` | `ClientBan.of(clid).withReason(..)` |
| `banClients(int[], long, String, boolean)` | Replaced | `bans().banClients(ClientBan)` | `ClientBan.of(ids...)`, `continuingOnError()`; an empty ID list is rejected |
| `broadcast(String)` | Replaced | `messages().broadcast(message)` |  |
| `copyChannelGroup(int, int, PermissionGroupDatabaseType)` | Replaced | `channelGroups().overwrite(sourceId, targetId, type)` |  |
| `copyChannelGroup(int, String, PermissionGroupDatabaseType)` | Replaced | `channelGroups().copy(sourceId, name, type)` |  |
| `copyServerGroup(int, int, PermissionGroupDatabaseType)` | Replaced | `serverGroups().overwrite(sourceId, targetId, type)` | returns nothing instead of echoing the target ID |
| `copyServerGroup(int, String, PermissionGroupDatabaseType)` | Replaced | `serverGroups().copy(sourceId, name, type)` |  |
| `createChannel(String, Map<ChannelProperty, String>)` | Replaced | `channels().create(name, properties)` |  |
| `createFileDirectory(String, int)` | Replaced | `files().createDirectory(FileLocation)` | `FileLocation.of(cid, path)` |
| `createFileDirectory(String, int, String)` | Replaced | `files().createDirectory(FileLocation)` | `withChannelPassword(..)` |
| `createServer(String, Map<VirtualServerProperty, String>)` | Replaced | `servers().create(name, properties)` |  |
| `createServerSnapshot()` | Replaced | `servers().createSnapshot()` |  |
| `deleteAllBans()` | Replaced | `bans().deleteAll()` |  |
| `deleteAllComplaints(int)` | Replaced | `complaints().deleteAll(cldbid)` |  |
| `deleteBan(int)` | Replaced | `bans().delete(banId)` |  |
| `deleteChannel(int)` | Replaced | `channels().delete(channelId)` |  |
| `deleteChannel(int, boolean)` | Replaced | `channels().delete(channelId, force)` |  |
| `deleteChannelClientPermission(int, int, String)` | Replaced | `permissions().delete(PermissionTarget, name)` | `PermissionTarget.channelClient(cid, cldbid)` |
| `deleteChannelGroup(int)` | Replaced | `channelGroups().delete(groupId)` |  |
| `deleteChannelGroup(int, boolean)` | Replaced | `channelGroups().delete(groupId, force)` |  |
| `deleteChannelGroupPermission(int, String)` | Replaced | `permissions().delete(PermissionTarget, name)` | `PermissionTarget.channelGroup(id)` |
| `deleteChannelPermission(int, String)` | Replaced | `permissions().delete(PermissionTarget, name)` | `PermissionTarget.channel(id)` |
| `deleteClientPermission(int, String)` | Replaced | `permissions().delete(PermissionTarget, name)` | `PermissionTarget.client(cldbid)`; no longer deprecated |
| `deleteClientPermission(int, IPermissionType)` | Replaced | `permissions().delete(PermissionTarget, name)` | pass `permission.getName()` |
| `deleteClientPermission(int, BPermissionType)` | Replaced | `permissions().delete(PermissionTarget, name)` | pass `permission.getName()` |
| `deleteComplaint(int, int)` | Replaced | `complaints().delete(targetCldbid, fromCldbid)` |  |
| `deleteCustomClientProperty(int, String)` | Replaced | `databaseClients().deleteCustomProperty(cldbid, key)` |  |
| `deleteDatabaseClientProperties(int)` | Replaced | `databaseClients().delete(cldbid)` |  |
| `deleteFile(String, int)` | Replaced | `files().delete(FileLocation)` |  |
| `deleteFile(String, int, String)` | Replaced | `files().delete(FileLocation)` | `withChannelPassword(..)` |
| `deleteFiles(String[], int)` | Replaced | `files().delete(List<FileLocation>)` | all locations must share channel and password |
| `deleteFiles(String[], int, String)` | Replaced | `files().delete(List<FileLocation>)` | all locations must share channel and password |
| `deleteIcon(long)` | Replaced | `files().deleteIcons(iconIds...)` | `IconId` |
| `deleteIcons(long...)` | Replaced | `files().deleteIcons(iconIds...)` | `IconId` |
| `deleteOfflineMessage(int)` | Replaced | `messages().deleteOffline(messageId)` |  |
| `deletePermissionFromAllServerGroups(ServerGroupType, String)` | Replaced | `permissions().deleteFromAllServerGroups(type, name)` |  |
| `deletePrivilegeKey(String)` | Replaced | `privilegeKeys().delete(token)` |  |
| `deleteServer(int)` | Replaced | `servers().delete(serverId)` |  |
| `deleteServerGroup(int)` | Replaced | `serverGroups().delete(groupId)` |  |
| `deleteServerGroup(int, boolean)` | Replaced | `serverGroups().delete(groupId, force)` |  |
| `deleteServerGroupPermission(int, String)` | Replaced | `permissions().delete(PermissionTarget, name)` | `PermissionTarget.serverGroup(id)` |
| `deleteServerQueryLogin(int)` | Replaced | `queryLogins().delete(cldbid)` |  |
| `deployServerSnapshot(Snapshot)` | Replaced | `servers().deploySnapshot(snapshot)` |  |
| `deployServerSnapshot(String)` | Removed | `servers().deploySnapshot(snapshot)` | wrap text with `new Snapshot(text)` |
| `downloadFile(OutputStream, String, int)` | Replaced | `files().download(FileLocation, out)` | location comes first |
| `downloadFile(OutputStream, String, int, String)` | Replaced | `files().download(FileLocation, out)` | `withChannelPassword(..)` |
| `downloadFile(OutputStream, String, int, String, long)` | Replaced | `files().download(FileLocation, out, offset)` |  |
| `downloadFileDirect(String, int)` | Replaced | `files().downloadBytes(FileLocation)` |  |
| `downloadFileDirect(String, int, String)` | Replaced | `files().downloadBytes(FileLocation)` | `withChannelPassword(..)` |
| `downloadIcon(OutputStream, long)` | Replaced | `files().downloadIcon(iconId, out)` | `IconId`, which comes first |
| `downloadIconDirect(long)` | Replaced | `files().downloadIcon(iconId)` | `IconId` |
| `editChannel(int, Map<ChannelProperty, String>)` | Replaced | `channels().edit(channelId, properties)` |  |
| `editChannel(int, ChannelProperty, String)` | Removed | `channels().edit(channelId, properties)` | use `Map.of(property, value)` |
| `editClient(int, Map<ClientProperty, String>)` | Replaced | `clients().edit(clientId, properties)` |  |
| `editClient(int, ClientProperty, String)` | Removed | `clients().edit(clientId, properties)` | use `Map.of(property, value)` |
| `editDatabaseClient(int, Map<ClientProperty, String>)` | Replaced | `databaseClients().edit(cldbid, properties)` |  |
| `editInstance(ServerInstanceProperty, String)` | Replaced | `instance().edit(properties)` | use `Map.of(property, value)`; several properties per command now possible |
| `editServer(Map<VirtualServerProperty, String>)` | Replaced | `servers().edit(properties)` |  |
| `getBans()` | Replaced | `bans().list()` |  |
| `getBindings()` | Replaced | `instance().bindings()` |  |
| `getChannelByNameExact(String, boolean)` | Replaced | `channels().findByName(name, ignoreCase)` | returns `Optional<Channel>` instead of `null` |
| `getChannelsByName(String)` | Replaced | `channels().search(namePart)` |  |
| `getChannelClientPermissions(int, int)` | Replaced | `permissions().assigned(PermissionTarget)` | `PermissionTarget.channelClient(cid, cldbid)` |
| `getChannelGroupClients(int, int, int)` | Replaced | `channelGroups().members(ChannelGroupFilter)` | `ChannelGroupFilter.all()` with `inChannel`/`ofClient`/`inGroup` instead of -1 sentinels |
| `getChannelGroupClientsByChannelGroupId(int)` | Replaced | `channelGroups().members(ChannelGroupFilter)` | `ChannelGroupFilter.all().inGroup(id)` |
| `getChannelGroupClientsByChannelId(int)` | Replaced | `channelGroups().members(ChannelGroupFilter)` | `ChannelGroupFilter.all().inChannel(id)` |
| `getChannelGroupClientsByClientDBId(int)` | Replaced | `channelGroups().members(ChannelGroupFilter)` | `ChannelGroupFilter.all().ofClient(cldbid)` |
| `getChannelGroupPermissions(int)` | Replaced | `permissions().assigned(PermissionTarget)` | `PermissionTarget.channelGroup(id)` |
| `getChannelGroups()` | Replaced | `channelGroups().list()` |  |
| `getChannelInfo(int)` | Replaced | `channels().info(channelId)` |  |
| `getChannelPermissions(int)` | Replaced | `permissions().assigned(PermissionTarget)` | `PermissionTarget.channel(id)` |
| `getChannels()` | Replaced | `channels().list()` |  |
| `getClientByNameExact(String, boolean)` | Replaced | `clients().findByName(nickname, ignoreCase)` | returns `Optional<Client>` instead of `null` |
| `getClientsByName(String)` | Replaced | `clients().search(nicknamePart)` |  |
| `getClientByUId(String)` | Replaced | `clients().findByUniqueId(uniqueId)` | returns `Optional<ClientInfo>`; previously an offline UID failed with error 512 from `clientinfo clid=-1` |
| `getClientInfo(int)` | Replaced | `clients().info(clientId)` |  |
| `getClientPermissions(int)` | Replaced | `permissions().assigned(PermissionTarget)` | `PermissionTarget.client(cldbid)` |
| `getClients()` | Replaced | `clients().list()` |  |
| `getComplaints()` | Replaced | `complaints().list()` |  |
| `getComplaints(int)` | Replaced | `complaints().about(cldbid)` |  |
| `getConnectionInfo()` | Replaced | `servers().connectionInfo()` |  |
| `getCustomClientProperties(int)` | Replaced | `databaseClients().customProperties(cldbid)` | unmodifiable map |
| `getDatabaseClientsByName(String)` | Replaced | `databaseClients().search(nicknamePattern)` |  |
| `getDatabaseClientByUId(String)` | Replaced | `databaseClients().findByUniqueId(uniqueId)` | returns `Optional<DatabaseClientInfo>` instead of `null` |
| `getDatabaseClientInfo(int)` | Replaced | `databaseClients().info(cldbid)` |  |
| `getDatabaseClients()` | Replaced | `databaseClients().list()` |  |
| `getDatabaseClients(int, int)` | Replaced | `databaseClients().list(offset, count)` |  |
| `getFileInfo(String, int)` | Replaced | `files().info(FileLocation)` |  |
| `getFileInfo(String, int, String)` | Replaced | `files().info(FileLocation)` | `withChannelPassword(..)` |
| `getFileInfos(String[], int)` | Replaced | `files().info(List<FileLocation>)` |  |
| `getFileInfos(String[], int, String)` | Replaced | `files().info(List<FileLocation>)` |  |
| `getFileInfos(String[], int[], String[])` | Replaced | `files().info(List<FileLocation>)` | one location per file replaces three parallel arrays |
| `getFileList(String, int)` | Replaced | `files().list(FileLocation)` |  |
| `getFileList(String, int, String)` | Replaced | `files().list(FileLocation)` | `withChannelPassword(..)` |
| `getFileTransfers()` | Replaced | `files().transfers()` |  |
| `getHostInfo()` | Replaced | `instance().hostInfo()` |  |
| `getIconList()` | Replaced | `files().icons()` |  |
| `getInstanceInfo()` | Replaced | `instance().info()` |  |
| `getInstanceLogEntries(int)` | Replaced | `instance().logEntries(lines)` |  |
| `getInstanceLogEntries()` | Removed | `instance().logEntries(lines)` | pass 100 for the old default |
| `getOfflineMessage(int)` | Replaced | `messages().getOffline(messageId)` | returns `Optional<String>` instead of `null` for a missing message |
| `getOfflineMessage(Message)` | Removed | `messages().getOffline(messageId)` | pass `message.getId()` |
| `getOfflineMessages()` | Replaced | `messages().listOffline()` |  |
| `getPermissionAssignments(String)` | Replaced | `permissions().assignments(name)` | returns `PermissionSource`, which has no value or flags |
| `getPermissionIdByName(String)` | Replaced | `permissions().idByName(name)` |  |
| `getPermissionIdsByName(String...)` | Replaced | `permissions().idsByName(names...)` | returns `List<Integer>` instead of `int[]` |
| `getPermissionOverview(int, int)` | Replaced | `permissions().overview(channelId, cldbid)` |  |
| `getPermissions()` | Replaced | `permissions().list()` |  |
| `getPermissionValue(String)` | Replaced | `permissions().ownValue(name)` |  |
| `getPermissionValues(String...)` | Replaced | `permissions().ownValues(names...)` | returns `List<Integer>` instead of `int[]` |
| `getPrivilegeKeys()` | Replaced | `privilegeKeys().list()` |  |
| `getServerGroupClients(int)` | Replaced | `serverGroups().members(groupId)` |  |
| `getServerGroupClients(ServerGroup)` | Removed | `serverGroups().members(groupId)` | pass `group.getId()` |
| `getServerGroupPermissions(int)` | Replaced | `permissions().assigned(PermissionTarget)` | `PermissionTarget.serverGroup(id)` |
| `getServerGroupPermissions(ServerGroup)` | Removed | `permissions().assigned(PermissionTarget)` | `PermissionTarget.serverGroup(group.getId())` |
| `getServerGroups()` | Replaced | `serverGroups().list()` |  |
| `getServerGroupsByClientId(int)` | Replaced | `serverGroups().ofClient(cldbid)` |  |
| `getServerGroupsByClient(Client)` | Removed | `serverGroups().ofClient(cldbid)` | pass `client.getDatabaseId()` |
| `getServerIdByPort(int)` | Replaced | `servers().findIdByPort(port)` | returns `Optional<VirtualServerId>`; an unused port (server error 1024) is empty |
| `getServerInfo()` | Replaced | `servers().info()` |  |
| `getServerQueryLogins()` | Replaced | `queryLogins().list()` |  |
| `getServerQueryLoginsByName(String)` | Replaced | `queryLogins().search(pattern)` |  |
| `getVersion()` | Replaced | `instance().version()` |  |
| `getVirtualServers()` | Replaced | `servers().list()` |  |
| `getVirtualServerLogEntries(int)` | Replaced | `servers().logEntries(lines)` |  |
| `getVirtualServerLogEntries()` | Removed | `servers().logEntries(lines)` | pass 100 for the old default |
| `isClientOnline(int)` | Replaced | `clients().isOnline(clientId)` |  |
| `isClientOnline(String)` | Replaced | `clients().isOnline(uniqueId)` |  |
| `kickClientFromChannel(int...)` | Replaced | `clients().kickFromChannel(reason, clientIds...)` | pass `null` as reason |
| `kickClientFromChannel(Client...)` | Removed | `clients().kickFromChannel(reason, clientIds...)` | pass client IDs |
| `kickClientFromChannel(String, int...)` | Replaced | `clients().kickFromChannel(reason, clientIds...)` |  |
| `kickClientFromChannel(String, Client...)` | Removed | `clients().kickFromChannel(reason, clientIds...)` | pass client IDs |
| `kickClientFromServer(int...)` | Replaced | `clients().kickFromServer(reason, clientIds...)` | pass `null` as reason |
| `kickClientFromServer(Client...)` | Removed | `clients().kickFromServer(reason, clientIds...)` | pass client IDs |
| `kickClientFromServer(String, int...)` | Replaced | `clients().kickFromServer(reason, clientIds...)` |  |
| `kickClientFromServer(String, Client...)` | Removed | `clients().kickFromServer(reason, clientIds...)` | pass client IDs |
| `login(String, String)` | Retained | `login(username, password)` |  |
| `logout()` | Retained | `logout()` |  |
| `moveChannel(int, int)` | Replaced | `channels().move(channelId, parentId)` | `null` parent instead of `0` for the top level |
| `moveChannel(int, int, int)` | Replaced | `channels().move(channelId, parentId, afterId)` | the sibling to sort below, or `null` instead of `0` for the first position |
| `moveClient(int, int)` | Replaced | `clients().move(clientId, channelId)` |  |
| `moveClients(int[], int)` | Replaced | `clients().moveAll(clientIds, channelId, password)` | `Collection<ClientId>`, `null` password |
| `moveClient(Client, ChannelBase)` | Removed | `clients().move(clientId, channelId)` | pass IDs |
| `moveClients(Client[], ChannelBase)` | Removed | `clients().moveAll(clientIds, channelId, password)` | pass IDs |
| `moveClient(int, int, String)` | Replaced | `clients().move(clientId, channelId, password)` |  |
| `moveClients(int[], int, String)` | Replaced | `clients().moveAll(clientIds, channelId, password)` |  |
| `moveClient(Client, ChannelBase, String)` | Removed | `clients().move(clientId, channelId, password)` | pass IDs |
| `moveClients(Client[], ChannelBase, String)` | Removed | `clients().moveAll(clientIds, channelId, password)` | pass IDs |
| `moveFile(String, String, int)` | Replaced | `files().move(FileLocation, FileLocation)` |  |
| `moveFile(String, String, int, int)` | Replaced | `files().move(FileLocation, FileLocation)` | locations in different channels |
| `moveFile(String, String, int, String)` | Replaced | `files().move(FileLocation, FileLocation)` | `withChannelPassword(..)` |
| `moveFile(String, String, int, String, int, String)` | Replaced | `files().move(FileLocation, FileLocation)` |  |
| `moveQuery(int)` | Replaced | `moveQuery(channelId)` | takes a `ChannelId` |
| `moveQuery(ChannelBase)` | Removed | `moveQuery(channelId)` | pass `channel.getId()` |
| `moveQuery(int, String)` | Replaced | `moveQuery(channelId, password)` | takes a `ChannelId` |
| `moveQuery(ChannelBase, String)` | Removed | `moveQuery(channelId, password)` | pass `channel.getId()` |
| `pokeClient(int, String)` | Replaced | `clients().poke(clientId, message)` |  |
| `registerAllEvents()` | Replaced | `events().registerAll()` |  |
| `registerEvent(TS3EventType)` | Replaced | `events().register(eventTypes...)` |  |
| `registerEvent(TS3EventType, int)` | Replaced | `events().register(eventType, channelId)` | one `ChannelId`; `register(eventTypes...)` instead of `0` or `-1` |
| `registerEvents(TS3EventType...)` | Replaced | `events().register(eventTypes...)` |  |
| `removeClientFromServerGroup(int, int)` | Replaced | `serverGroups().removeMember(groupId, cldbid)` |  |
| `removeClientFromServerGroup(ServerGroup, Client)` | Removed | `serverGroups().removeMember(groupId, cldbid)` | pass `group.getId()`, `client.getDatabaseId()` |
| `removeTS3Listeners(TS3Listener...)` | Removed | `EventSubscription#close()` | close the handle returned by `events().subscribe(..)` |
| `renameChannelGroup(int, String)` | Replaced | `channelGroups().rename(groupId, name)` |  |
| `renameChannelGroup(ChannelGroup, String)` | Removed | `channelGroups().rename(groupId, name)` | pass `group.getId()` |
| `renameServerGroup(int, String)` | Replaced | `serverGroups().rename(groupId, name)` |  |
| `renameServerGroup(ServerGroup, String)` | Removed | `serverGroups().rename(groupId, name)` | pass `group.getId()` |
| `resetPermissions()` | Replaced | `permissions().reset()` |  |
| `searchCustomClientProperty(String)` | Removed | `databaseClients().searchCustomProperty(key, valuePattern)` | pass `"%"` |
| `searchCustomClientProperty(String, String)` | Replaced | `databaseClients().searchCustomProperty(key, valuePattern)` |  |
| `selectVirtualServerById(int)` | Replaced | `selectVirtualServerById(id)` | takes a `VirtualServerId`; to deselect (`0`), call `deselectVirtualServer()` |
| `selectVirtualServerById(int, String)` | Replaced | `selectVirtualServerById(id, nickname)` | takes a `VirtualServerId`; to deselect (`0`), call `deselectVirtualServer()` |
| `selectVirtualServerByPort(int)` | Retained | `selectVirtualServerByPort(port)` |  |
| `selectVirtualServerByPort(int, String)` | Retained | `selectVirtualServerByPort(port, nickname)` |  |
| `selectVirtualServer(VirtualServer)` | Removed | `selectVirtualServerById(id)` | pass `server.getId()` |
| `selectVirtualServer(VirtualServer, String)` | Removed | `selectVirtualServerById(id, nickname)` | pass `server.getId()` |
| `sendOfflineMessage(String, String, String)` | Replaced | `messages().sendOffline(uniqueId, subject, message)` |  |
| `sendTextMessage(TextMessageTargetMode, int, String)` | Removed | `messages().sendPrivate(clientId, message)` | or `sendChannel(message)`/`sendServer(message)`; the server ignores channel/server target IDs (live evidence in `ServerQueryCompatibilityIT.messageDomain`) |
| `sendChannelMessage(int, String)` | Replaced | `messages().sendChannel(channelId, message)` |  |
| `sendChannelMessage(String)` | Replaced | `messages().sendChannel(message)` |  |
| `sendServerMessage(int, String)` | Replaced | `messages().sendServer(serverId, message)` |  |
| `sendServerMessage(String)` | Replaced | `messages().sendServer(message)` |  |
| `sendPrivateMessage(int, String)` | Replaced | `messages().sendPrivate(clientId, message)` |  |
| `setClientChannelGroup(int, int, int)` | Replaced | `channelGroups().assign(groupId, channelId, cldbid)` |  |
| `setCustomClientProperties(int, Map<String, String>)` | Replaced | `databaseClients().setCustomProperties(cldbid, properties)` | null keys are rejected instead of skipped |
| `setCustomClientProperty(int, String, String)` | Replaced | `databaseClients().setCustomProperty(cldbid, key, value)` |  |
| `setMessageRead(int)` | Removed | `messages().setOfflineRead(messageId, read)` | pass `true` |
| `setMessageRead(Message)` | Removed | `messages().setOfflineRead(messageId, read)` | pass `message.getId(), true` |
| `setMessageReadFlag(int, boolean)` | Replaced | `messages().setOfflineRead(messageId, read)` |  |
| `setMessageReadFlag(Message, boolean)` | Removed | `messages().setOfflineRead(messageId, read)` | pass `message.getId()` |
| `setNickname(String)` | Retained | `setNickname(nickname)` |  |
| `startServer(int)` | Replaced | `servers().start(serverId)` |  |
| `startServer(VirtualServer)` | Removed | `servers().start(serverId)` | pass `server.getId()` |
| `stopServer(int)` | Replaced | `servers().stop(serverId)` |  |
| `stopServer(int, String)` | Replaced | `servers().stop(serverId, reason)` |  |
| `stopServer(VirtualServer)` | Removed | `servers().stop(serverId)` | pass `server.getId()` |
| `stopServer(VirtualServer, String)` | Removed | `servers().stop(serverId, reason)` | pass `server.getId()` |
| `stopServerProcess()` | Removed | `instance().stopProcess(reason)` | pass `null` |
| `stopServerProcess(String)` | Replaced | `instance().stopProcess(reason)` |  |
| `unregisterAllEvents()` | Replaced | `events().unregisterAll()` |  |
| `updateClient(Map<ClientProperty, String>)` | Retained | `updateClient(properties)` |  |
| `updateClient(ClientProperty, String)` | Removed | `updateClient(properties)` | use `Map.of(property, value)` |
| `updateServerQueryLogin(String)` | Replaced | `queryLogins().updateOwn(loginName)` |  |
| `uploadFile(InputStream, long, String, boolean, int)` | Replaced | `files().upload(FileLocation, data, length, UploadMode)` | `overwrite` becomes `UploadMode.OVERWRITE`/`CREATE` |
| `uploadFile(InputStream, long, String, boolean, int, String)` | Replaced | `files().upload(FileLocation, data, length, UploadMode)` |  |
| `uploadFile(InputStream, long, String, boolean, int, String, boolean)` | Replaced | `files().upload(FileLocation, data, length, UploadMode)` | `resume` becomes `UploadMode.RESUME` |
| `uploadFileDirect(byte[], String, boolean, int)` | Replaced | `files().upload(FileLocation, data, UploadMode)` |  |
| `uploadFileDirect(byte[], String, boolean, int, String)` | Replaced | `files().upload(FileLocation, data, UploadMode)` |  |
| `uploadIcon(InputStream, long)` | Replaced | `files().uploadIcon(data, length)` | returns `IconId` |
| `uploadIconDirect(byte[])` | Replaced | `files().uploadIcon(data)` | returns `IconId` |
| `usePrivilegeKey(String)` | Replaced | `privilegeKeys().use(token)` |  |
| `usePrivilegeKey(PrivilegeKey)` | Removed | `privilegeKeys().use(token)` | pass `key.getToken()` |
| `whoAmI()` | Retained | `whoAmI()` | `getId()` and `getVirtualServerPort()` are empty before a virtual server is selected |
