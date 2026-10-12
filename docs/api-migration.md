# 2.0 domain API, errors and absent results

Issue #443 replaces the two 238-method `TS3Api` / `TS3ApiAsync` surfaces with small
domain APIs, defines one exception hierarchy and stops signalling absence with `null`
or `-1`. This is an intentional source and binary break for 2.0.0: recompile and use the
[operation inventory](#operation-inventory) to migrate each call. TeamSpeak 3 RAW and
SSH are the supported transports; scope and support are unchanged otherwise.

## API shape

`query.getApi()` and `query.getAsyncApi()` keep the session operations and expose one
accessor per domain. Each domain exists twice with identical operations: `XApi` returns
values and `XApiAsync` returns `CompletableFuture`s.

| Accessor | Sync / async class | Scope |
| --- | --- | --- |
| `bans()` | `BanApi` / `BanApiAsync` | Ban rules and banning connected clients |
| `channels()` | `ChannelApi` / `ChannelApiAsync` | Channel list, lookup, creation, editing, moving and deletion |
| `channelGroups()` | `ChannelGroupApi` / `ChannelGroupApiAsync` | Channel groups and their memberships |
| `clients()` | `ClientApi` / `ClientApiAsync` | Connected clients: lookup, move, kick, poke, edit |
| `complaints()` | `ComplaintApi` / `ComplaintApiAsync` | Complaints |
| `databaseClients()` | `DatabaseClientApi` / `DatabaseClientApiAsync` | Database clients and custom properties |
| `events()` | `EventApi` / `EventApiAsync` | Server notification registration and local listeners |
| `files()` | `FileApi` / `FileApiAsync` | File repository, transfers and icons |
| `instance()` | `InstanceApi` / `InstanceApiAsync` | Instance info/edit, host info, version, bindings, logs, process stop |
| `messages()` | `MessageApi` / `MessageApiAsync` | Private/channel/server/broadcast text and offline messages |
| `permissions()` | `PermissionApi` / `PermissionApiAsync` | Permission metadata and assignments for every holder |
| `privilegeKeys()` | `PrivilegeKeyApi` / `PrivilegeKeyApiAsync` | Privilege keys |
| `queryLogins()` | `QueryLoginApi` / `QueryLoginApiAsync` | ServerQuery logins |
| `serverGroups()` | `ServerGroupApi` / `ServerGroupApiAsync` | Server groups and their members |
| `servers()` | `VirtualServerApi` / `VirtualServerApiAsync` | Virtual server lifecycle; info, edit, snapshots and logs of the selected server |

The entry points retain `login`, `logout`, `whoAmI`, `selectVirtualServerById`,
`selectVirtualServerByPort`, `setNickname`, `updateClient`, `moveQuery` and
`executeRawCommand`.

**One implementation.** Each operation is implemented once, in the asynchronous
domain class, on one shared command path (bounded admission, deadlines, retry policy,
cancellation and error translation). A synchronous method only waits for that future
with the existing uninterruptible `CommandFutures.join` adapter. Facades are plain
one-line delegations; generating them would add a build step without removing
meaningful maintenance. Instead, `DomainApiParityTest` fails the build when a sync and
async domain differ in any operation, parameter or result type.

**Request objects** replace long or nullable positional argument lists:

| Value | Replaces |
| --- | --- |
| `BanRequest.ip(..)` / `name(..)` / `uniqueId(..)` / `myTeamSpeakId(..)` with `with…` | four nullable patterns, a seconds count and a nullable reason |
| `ClientBan.of(ids...)` with `withDuration`, `withReason`, `continuingOnError` | `banClient`/`banClients` overloads |
| `PermissionTarget.serverGroup/channelGroup/channel/client/channelClient` | five families of add/delete/list methods |
| `PermissionGrant.of(name or enum, value)` with `withNegated()`, `withSkip()` | positional `negated`/`skipped` booleans; unsupported flags are rejected locally |
| `FileLocation.of(channelId, path)` with `withChannelPassword(..)` | path/channel/nullable-password triples and parallel arrays |
| `UploadMode.CREATE` / `OVERWRITE` / `RESUME` | the `overwrite` and `resume` booleans, which could conflict |
| `ChannelGroupFilter.all()` with `inChannel`, `ofClient`, `inGroup` | `-1` "ignore" sentinels |

Lists returned by domain methods are unmodifiable.

## Common workflow

Ensure a permanent "Lobby" channel with a join power, publish its rules file, and
welcome a user by unique identifier: move and message them if they are connected,
otherwise leave an offline message. The 2.0 forms below are the executed code of
`CommonWorkflow`, run by `CommonWorkflowTest` (scripted) and
`DomainApiCompatibilityIT.commonWorkflow` (TS3 3.13.8 RAW and SSH).

Before, synchronous (compiled against `c685be8`, the integration branch before #443):

```java
static int prepareLobby(TS3Api api, String uniqueId, byte[] rules) {
	Channel existing = api.getChannelByNameExact("Lobby", false);
	int lobby = existing != null ? existing.getId()
		: api.createChannel("Lobby", Collections.singletonMap(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
	api.addChannelPermission(lobby, "i_channel_needed_join_power", 10);
	api.uploadFileDirect(rules, "/rules.txt", true, lobby);
	try {
		ClientInfo client = api.getClientByUId(uniqueId);
		api.moveClient(client.getId(), lobby);
		api.sendPrivateMessage(client.getId(), "Welcome to the lobby");
	} catch (TS3Exception offline) {
		if (!offline.matchesQueryError(512)) throw offline;
		api.sendOfflineMessage(uniqueId, "Lobby", "Welcome to the lobby");
	}
	return lobby;
}
```

After, synchronous:

```java
static int prepareLobby(TS3Api api, String uniqueId, byte[] rules) {
	int lobby = api.channels().findByName("Lobby", false).map(Channel::getId)
		.orElseGet(() -> api.channels().create("Lobby", Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1")));
	api.permissions().add(PermissionTarget.channel(lobby), "i_channel_needed_join_power", 10);
	api.files().upload(FileLocation.of(lobby, "/rules.txt"), rules, UploadMode.OVERWRITE);
	api.clients().findByUniqueId(uniqueId).ifPresentOrElse(client -> {
		api.clients().move(client.getId(), lobby);
		api.messages().sendPrivate(client.getId(), "Welcome to the lobby");
	}, () -> api.messages().sendOffline(uniqueId, "Lobby", "Welcome to the lobby"));
	return lobby;
}
```

Before, asynchronous:

```java
static CompletableFuture<Integer> prepareLobby(TS3ApiAsync api, String uniqueId, byte[] rules) {
	return api.getChannelByNameExact("Lobby", false).thenCompose(existing -> existing != null
			? CompletableFuture.completedFuture(existing.getId())
			: api.createChannel("Lobby", Collections.singletonMap(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1")))
		.thenCompose(lobby -> api.addChannelPermission(lobby, "i_channel_needed_join_power", 10)
			.thenCompose(done -> api.uploadFileDirect(rules, "/rules.txt", true, lobby))
			.thenCompose(done -> api.getClientByUId(uniqueId).thenCompose(client -> api.moveClient(client.getId(), lobby)
				.thenCompose(moved -> api.sendPrivateMessage(client.getId(), "Welcome to the lobby"))))
			.exceptionallyCompose(failure -> CommandFutures.unwrap(failure) instanceof TS3Exception offline && offline.matchesQueryError(512)
				? api.sendOfflineMessage(uniqueId, "Lobby", "Welcome to the lobby") : CompletableFuture.failedFuture(failure))
			.thenApply(done -> lobby));
}
```

After, asynchronous:

```java
static CompletableFuture<Integer> prepareLobby(TS3ApiAsync api, String uniqueId, byte[] rules) {
	return api.channels().findByName("Lobby", false).thenCompose(found -> found.map(channel -> CompletableFuture.completedFuture(channel.getId()))
			.orElseGet(() -> api.channels().create("Lobby", Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"))))
		.thenCompose(lobby -> api.permissions().add(PermissionTarget.channel(lobby), "i_channel_needed_join_power", 10)
			.thenCompose(done -> api.files().upload(FileLocation.of(lobby, "/rules.txt"), rules, UploadMode.OVERWRITE))
			.thenCompose(done -> api.clients().findByUniqueId(uniqueId))
			.thenCompose(client -> client.isEmpty() ? api.messages().sendOffline(uniqueId, "Lobby", "Welcome to the lobby")
				: api.clients().move(client.get().getId(), lobby)
					.thenCompose(done -> api.messages().sendPrivate(client.get().getId(), "Welcome to the lobby")))
			.thenApply(done -> lobby));
}
```

| Measure (non-blank lines, characters without indentation) | Before sync | After sync | Before async | After async |
| --- | --- | --- | --- | --- |
| Lines | 16 | 11 | 12 | 11 |
| Characters | 717 | 688 | 965 | 901 |
| `null` checks | 1 | 0 | 1 | 0 |

The old form needs a `null` check and must catch server error 512, because an offline
unique identifier produced `clientinfo clid=-1`. Checking `isClientOnline` first instead
costs an extra command and still races with the client leaving. The new form has no
`null` checks, no `-1` sentinels and no positional `overwrite` boolean; absence is part
of the result type. Both send the same commands on success.

## Errors

Every failure is a `TS3Exception`. Category is the type; whether the command may still
have run is `isOutcomeUnknown()`, so handlers need no type checks for retry decisions.

| Type | Meaning | `isOutcomeUnknown()` |
| --- | --- | --- |
| `TS3CommandFailedException` | The server answered with an error. `getErrorId()`, `getCommandName()`, `getError()` | `false` |
| `TS3TransportException` | Base type for connection failures | `false` unless a subtype says so |
| ↳ `TS3ConnectionFailedException` | Connecting, authentication or initialization failed | `false` |
| ↳ `TS3QueryShutDownException` | Closed or disconnected before the command was written | `false` |
| &nbsp;&nbsp;↳ `TS3UnknownOutcomeException` | The connection ended after the command was written | `true` |
| `TS3TimeoutException` | A deadline expired: `Phase.QUEUE_WAIT` (never written) or `Phase.RESPONSE` (written; the connection is closed) | `true` only for `RESPONSE` |
| `TS3QueueFullException` | Command capacity exhausted; nothing was sent | `false` |
| `TS3UnsupportedOperationException` | The configuration or data does not support the operation | `false` |
| `TS3FileTransferFailedException` | The separate file socket or a caller stream failed; an upload may leave a partial file | `false` (describes the ServerQuery command) |

Changes from the previous integration branch:

- A queue-wait expiry was a plain `TS3Exception`; it is now `TS3TimeoutException`
  with `Phase.QUEUE_WAIT`.
- The command whose own response deadline expired failed with
  `TS3UnknownOutcomeException`; it now fails with `TS3TimeoutException`
  (`Phase.RESPONSE`). Other in-flight commands still use `TS3UnknownOutcomeException`.
  Code that caught `TS3UnknownOutcomeException` to reconcile mutations should test
  `isOutcomeUnknown()` instead.
- Session-changing commands rejected under reconnect without `SessionConfiguration`
  fail with `TS3UnsupportedOperationException` instead of a plain `TS3Exception`;
  commands submitted while a query without a restorable session is disconnected fail with
  `TS3QueryShutDownException`.
- `ClientInfo.isTalking()` throws `TS3UnsupportedOperationException` instead of
  `java.lang.UnsupportedOperationException`.
- `TS3ConnectionFailedException` and `TS3QueryShutDownException` now extend
  `TS3TransportException`; existing catch blocks for them keep working.
- `TS3UnknownErrorException` and `TS3UnknownEventException` were never thrown and
  are removed.

Synchronous methods throw these types directly. Asynchronous results complete with
them as the cause of `ExecutionException` or `CompletionException`;
`CommandFutures.unwrap` returns the cause.

## Absent results and failures

- **Lookups by name, unique identifier or port** return `Optional`: absent is empty,
  never `null`. Other server errors still fail.
- **Searches and lists** return an empty list when nothing matches, including the
  server's "database empty result set" (1281) and the "no match" errors of
  `channelfind` (768) and `clientfind` (512).
- **Reads by ID** fail with the server's error when the entity does not exist
  (for example 768 for `channels().info`, 512 for `clients().info`/`databaseClients().info`,
  2052 for `files().info`, 2560 for `serverGroups().members`).
- **Required results** never fall back to defaults. A read that the server answers
  without a row, or a creation without its ID/token, fails with `TS3Exception`
  instead of returning `-1`, `""` or an object full of defaults.

Evidence from TS3 3.13.8 for each mapped case, asserted on RAW and SSH by
`DomainApiCompatibilityIT.absentResultsAndFailures`:

| Request | Server answer | 2.0 result |
| --- | --- | --- |
| `clients().findByUniqueId` for an unknown UID | `clientgetids`: empty (1281) | `Optional.empty()` |
| `databaseClients().findByUniqueId` for an unknown UID | `clientdbfind -uid`: empty (1281) | `Optional.empty()` |
| `messages().getOffline` for an unknown ID | `messageget`: empty (1281) | `Optional.empty()` |
| `servers().findIdByPort` for an unused port | `serveridgetbyport`: error 1024 | `Optional.empty()` |
| `channels().findByName` / `clients().findByName` without a match | filtered list | `Optional.empty()` |
| `channels().search` / `clients().search` without a match | `channelfind` 768 / `clientfind` 512 | empty list |
| `channels().info` / `clients().info` / `databaseClients().info` for unknown IDs | 768 / 512 / 512 | `TS3CommandFailedException` |
| `files().info` for a missing file | 2052 | `TS3CommandFailedException` |
| `clients().info` for an out-of-range ID such as 999999 | 1540 | `TS3CommandFailedException` |
| `serverGroups().members` for an unknown group | 2560 | `TS3CommandFailedException` |
| `messages().sendOffline` to an unknown UID | 512 | `TS3CommandFailedException` |
| `bans().add(BanRequest.myTeamSpeakId("empty"))` / a malformed myTeamSpeak ID | ban ID / 1538 | rule created / `TS3CommandFailedException` |

`clients().findByUniqueId` also treats a client that disconnects between its two
commands (512 from `clientinfo`) as absent. A myTeamSpeak-only ban rule, documented by `help banadd`
and previously rejected locally, is now accepted.

### Audit of previous null/-1 documentation

| Previous documentation | Actual behavior before #443 | 2.0 |
| --- | --- | --- |
| `TS3Api` class: "many methods in this class will return `null` or `-1` if a command fails" | Inaccurate. Server errors always threw `TS3CommandFailedException` (sync) or failed the future (async). `-1`/`""`/default models appeared when a **successful** response had no row or field: an empty (1281) answer to a single-row read went through `Wrapper.EMPTY`. | Removed. Required rows/fields fail with `TS3Exception`; absent lookups are `Optional`. |
| `getChannelByNameExact`, `getClientByNameExact`: "`null` if no … was found" | Accurate | `Optional` |
| `getDatabaseClientByUId`: "`null` if no client was found" | Accurate (1281) | `Optional` |
| `getOfflineMessage`: "`null` if there was no message with that ID" | Accurate (1281); an explicit empty body was `""` | `Optional`; an empty body is `Optional.of("")` |
| `getClientByUId` (no absence documented) | An offline UID failed with error 512 from `clientinfo clid=-1` | `Optional` |
| `getServerIdByPort` (no absence documented) | An unused port failed with 1024 | `Optional` |
| `getPermissionIdByName`, `getPermissionValue`, token/password results (no absence documented) | An empty answer returned `-1` or `""` | `TS3Exception` |
| `copyServerGroup(int, int, …)`, fixed in #473 | Returned the requested target ID | `overwrite` returns nothing |
| `getChannelGroupClients(int, int, int)`: "set to `-1` to ignore" | Accurate | `ChannelGroupFilter` |

## Operation inventory

Every public operation of the previous `TS3Api` and `TS3ApiAsync` (identical sets of
238 signatures at `c685be8`) has exactly one row. **Retained**: same entry point and
parameters. **Replaced**: moved to a domain or reshaped, same capability.
**Removed**: an intentional convenience overload or variant with the replacement shown.
No operation is deferred. `ApiMigrationInventoryTest` checks that this table covers
238 distinct signatures and that every replacement exists in the 2.0 API.

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
| `addServerQueryLogin(String, int)` | Replaced | `queryLogins().create(loginName, cldbid)` |  |
| `addTS3Listeners(TS3Listener...)` | Removed | `events().subscribe(listener)` | one listener per call; keep the returned `EventSubscription` (same as `TS3Query#subscribe`) |
| `banClient(int, long)` | Replaced | `bans().banClients(ClientBan)` | `ClientBan.of(clid).withDuration(Duration.ofSeconds(s))`; returns `List<Integer>` |
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
| `deleteIcon(long)` | Replaced | `files().deleteIcons(iconIds...)` |  |
| `deleteIcons(long...)` | Replaced | `files().deleteIcons(iconIds...)` |  |
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
| `downloadIcon(OutputStream, long)` | Replaced | `files().downloadIcon(iconId, out)` | icon ID comes first |
| `downloadIconDirect(long)` | Replaced | `files().downloadIcon(iconId)` |  |
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
| `getPermissionAssignments(String)` | Replaced | `permissions().assignments(name)` |  |
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
| `getServerIdByPort(int)` | Replaced | `servers().findIdByPort(port)` | returns `Optional<Integer>`; an unused port (server error 1024) is empty |
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
| `moveChannel(int, int)` | Replaced | `channels().move(channelId, parentId)` |  |
| `moveChannel(int, int, int)` | Replaced | `channels().move(channelId, parentId, order)` |  |
| `moveClient(int, int)` | Replaced | `clients().move(clientId, channelId)` |  |
| `moveClients(int[], int)` | Replaced | `clients().moveAll(clientIds, channelId, password)` | `Collection<Integer>`, `null` password |
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
| `moveQuery(int)` | Retained | `moveQuery(channelId)` |  |
| `moveQuery(ChannelBase)` | Removed | `moveQuery(channelId)` | pass `channel.getId()` |
| `moveQuery(int, String)` | Retained | `moveQuery(channelId, password)` |  |
| `moveQuery(ChannelBase, String)` | Removed | `moveQuery(channelId, password)` | pass `channel.getId()` |
| `pokeClient(int, String)` | Replaced | `clients().poke(clientId, message)` |  |
| `registerAllEvents()` | Replaced | `events().registerAll()` |  |
| `registerEvent(TS3EventType)` | Replaced | `events().register(eventTypes...)` |  |
| `registerEvent(TS3EventType, int)` | Replaced | `events().register(eventType, channelId)` |  |
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
| `selectVirtualServerById(int)` | Retained | `selectVirtualServerById(id)` |  |
| `selectVirtualServerById(int, String)` | Retained | `selectVirtualServerById(id, nickname)` |  |
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
| `uploadIcon(InputStream, long)` | Replaced | `files().uploadIcon(data, length)` |  |
| `uploadIconDirect(byte[])` | Replaced | `files().uploadIcon(data)` |  |
| `usePrivilegeKey(String)` | Replaced | `privilegeKeys().use(token)` |  |
| `usePrivilegeKey(PrivilegeKey)` | Removed | `privilegeKeys().use(token)` | pass `key.getToken()` |
| `whoAmI()` | Retained | `whoAmI()` |  |

## Verification and boundaries

- Live (TS3 3.13.8 RAW and SSH): the common workflow in both forms, every absent/failure
  row above, myTeamSpeak-only ban rules, local unsupported/closed-query failures, and the
  existing compatibility suites, which now call the domain methods.
- Scripted only: response/queue timeouts, permission commands per holder, `ClientBan`
  (banning a client needs a regular voice client) and request-object validation.
  Earlier live boundaries still apply, for example query clients cannot receive
  client permissions (512) or consume privilege keys (516).
- The `commands` packages stay public. Consumers cannot execute their `Command` objects,
  so they are not operations of the API; hiding them needs JPMS exports, which #447
  decides. Model immutability and typed values belong to #444.

## Release intent and migration

`feat!`, planned **2.0.0**. BREAKING CHANGE: `TS3Api`/`TS3ApiAsync` operations move to
domain APIs with request objects; absent lookups return `Optional`; required results fail
instead of returning `-1`/`""`/defaults; queue-wait and response deadlines fail with
`TS3TimeoutException`; sealed session changes fail with `TS3UnsupportedOperationException`;
`TS3UnknownErrorException`/`TS3UnknownEventException` are removed. Migrate with the
inventory above. No dependency, protocol, default or transport change.
