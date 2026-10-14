# 2.0 response models and typed values

Issue #444 makes every received response and event an immutable value, replaces
default sentinels (`-1`, `""`, `false`, `null`, `java.util.Date`) with explicit types and
keeps every received property. This is an intentional source and binary break for 2.0.0;
recompile and use the [migration table](#migration-table). Scope is unchanged: TeamSpeak 3
RAW and SSH ServerQuery.

## Immutability

- Models (`api.wrapper`) and events (`api.event`) copy the received properties on
  construction. Changing the map passed to a constructor does not change the model, and
  `getMap()` returns an unmodifiable map in received order. `Wrapper.EMPTY` is removed.
- Lists returned by the domain APIs, `DefaultArrayResponse.getResponses()`,
  `Client.getServerGroups()`, `Client.getBadgeGUIDs()` and
  `ClientJoinEvent.getClientServerGroups()` are unmodifiable. Arrays (`int[]`, `String[]`)
  are no longer returned by models.
- Map-backed models have value equality: the same class with the same properties is equal.
  `ClientInfo` and `ChannelInfo` also compare the ID the caller supplied.
- `ModelImmutabilityTest` fails the build if a model or event gains a non-final field, a
  setter, or an array or `Date` result.

## Missing, malformed and unknown values

Typed getters keep three cases apart. Values are interpreted when a getter is called, so a
new or changed property only affects the getters that read it.

| Case | Required getter (`T`) | Optional getter (`Optional`/`OptionalInt`/…) |
| --- | --- | --- |
| Missing property | throws `TS3InvalidValueException`, `getRawValue()` empty | empty |
| Malformed value, for example `clid=x` | throws `TS3InvalidValueException` with the raw value | throws the same |
| Unknown enum code, for example codec `99` | `UNKNOWN` constant; the raw code stays in `getMap()` | — |

`TS3InvalidValueException` extends `TS3Exception`; `getProperty()` names the property and
`isOutcomeUnknown()` is `false` because the command itself succeeded. A property sent
without a value (`key` rather than `key=value`) is an empty string; numeric and boolean
accessors treat it as missing. Text that TeamSpeak leaves empty when unset (IP addresses,
country, avatar, myTeamSpeak ID, password hashes, ban criteria) is an empty `Optional`;
free text such as descriptions and away messages stays a `String`.

Every model exposes the same accessors for properties without a typed getter, so a
property added by a new server version is usable before the library changes:

```java
OptionalInt slots = info.findInt("virtualserver_new_setting");      // empty when absent
String raw = info.getMap().get("virtualserver_new_setting");        // exact received text
int required = info.requireInt("virtualserver_reserved_slots");     // fails when absent or malformed
```

Required means the property is part of the producing command's TS3 3.13.8 output for the
options the library sends. `ModelCompatibilityIT` reads every typed getter of every model
and event below on live TS3 3.13.8 RAW and SSH responses; none may fail except
`ClientInfo.isTalking()`, which `clientinfo` does not report.

Optional because TeamSpeak genuinely omits or blanks the value (live-observed on 3.13.8):

| Value | Absent when |
| --- | --- |
| `VirtualServer.getClientsOnline()`, `getQueryClientsOnline()`, `getMaxClients()`, `getUptime()` | the server is not running |
| `PermissionAssignment.getValue()`, `isNegated()`, `isSkipped()` | the row comes from `permissions().assignments(..)` (`permfind`) |
| `FileListEntry` incomplete size (`isStillUploading()`) | the file is complete |
| `BaseEvent.getInvokerId()`, `getInvokerName()`, `getInvokerUniqueId()`, `getReasonId()`, `getReasonMessage()` | the event has no invoker (for example a self-caused join or move) or no reason |
| `TextMessageEvent.getTargetClientId()` | the message is not private |
| `InstanceInfo`/`VirtualServerInfo` bandwidth limits and quotas | unlimited (TeamSpeak sends 2<sup>64</sup>-1, which `long` cannot hold) |
| `ChannelBase.getMaxClients()`, `getMaxFamilyClients()` | unlimited (`-1`) |
| `ChannelBase.getTimeEmpty()` | clients are in the channel (`-1`) |
| `Client`/`DatabaseClient` created and last-connected times | not recorded (`0`, for example query clients) |
| `ClientInfo.getTalkRequestCreatedDate()`, `getDefaultChannel()` | no talk request / no default channel |
| `ClientInfo.getUnreadMessages()` | the client is not the query's own client |
| `Ban.getInvokerClientDBId()` | invoker database ID `0` |
| `ServerQueryInfo.getDatabaseId()` | before login (`client_database_id=0`) |
| `DatabaseClientInfo.getIconId()` | always on 3.13.8, which does not send it in `clientdbinfo` |
| `FileTransferParameters.findFailure()`, `getFileServerHost()` | the transfer started (no status, or `0`/`1281`) / the server advertises no address |
| `QueryError.getExtraMessage()`, `getFailedPermissionId()` | not part of the error |

## Time

`java.util.Date` and raw seconds/milliseconds are replaced; units were checked against the
live server (for example `ftlist runtime` counts seconds, not milliseconds as documented).

| Getter | Before | 2.0 |
| --- | --- | --- |
| created, last connected, timestamps, received, last modified | `Date` | `Instant`, or `Optional<Instant>` where `0` means not recorded |
| `Client.getIdleTime()`, `ClientInfo.getTimeConnected()` | `long` ms | `Duration` |
| `HostInfo.getUptime()`, `ConnectionInfo.getConnectedTime()`, `FileTransfer.getRuntime()` | `long` s (runtime documented as ms) | `Duration` |
| `VirtualServer.getUptime()` | `long` s | `Optional<Duration>` |
| `Ban.getDuration()` | `long` s, `0` permanent | `Duration` (`ZERO` permanent) plus `isPermanent()` |
| `ChannelBase.getSecondsEmpty()` | `int`, `-1` occupied | `getTimeEmpty()`: `Optional<Duration>` |
| `InstanceInfo` flood time/ban time, `VirtualServerInfo` complaint times and banner interval | `int`/`long` s | `Duration` |

## Identifier types

Only the two client identifiers that are routinely confused get types: `ClientId` (the
session ID, `clid`, which changes on every connection) and `ClientDatabaseId` (the
persistent `cldbid`). Both are records whose `toString()` is the plain number, so they
can be concatenated into raw commands. `ClientId` accepts `0` (the server as event invoker,
or a query that has not selected a server); `ClientDatabaseId` must be positive. Channel,
group, server and permission IDs stay `int`: they are not interchangeable with each other in
the domain APIs' parameter lists and typing every integer would add conversion noise
without preventing a realistic mistake.

Domain API parameters follow the model types:

| Domain | Typed parameters |
| --- | --- |
| `clients()` | `info`, `isOnline`, `move`, `moveAll`, `kickFromChannel`, `kickFromServer`, `poke`, `edit` take `ClientId` |
| `messages()` | `sendPrivate(ClientId, ..)` |
| `bans()` | `ClientBan.of(ClientId...)` |
| `databaseClients()` | `info`, `edit`, `delete`, custom properties take `ClientDatabaseId` |
| `complaints()` | `about`, `add`, `delete`, `deleteAll` take `ClientDatabaseId` |
| `serverGroups()` / `channelGroups()` | `addMember`, `removeMember`, `ofClient` / `assign`, `ChannelGroupFilter.ofClient` take `ClientDatabaseId` |
| `permissions()` | `overview(int, ClientDatabaseId)`, `PermissionTarget.client(..)`, `PermissionTarget.channelClient(int, ..)` |
| `queryLogins()` | `create(String, ClientDatabaseId)`, `delete(ClientDatabaseId)`; the former `cldbid 0` sentinel is the new `create(String)` (no virtual server selected) |

## Records and map-backed models

Closed protocol tuples are records with validating `from(Wrapper)` factories:
`Version`, `Binding`, `CreatedVirtualServer`, `ChannelGroupClient`, `ServerGroupClient`,
`CustomPropertyAssignment`, `Permission`, `PermissionInfo` and `Snapshot` (whose
`toString()` omits the snapshot data). A malformed or missing component fails the whole
response with `TS3InvalidValueException`. Record accessors drop the `get` prefix:
`version.version()`, `binding.ip()`, `created.serverAdminToken()`, `permission.negated()`.

Rows whose property set varies by command option, server state or server version stay
immutable map-backed classes so unknown properties remain available: clients, channels,
servers, groups, bans, privilege keys, complaints, messages, query logins, files,
transfers, permission assignments, instance/host/connection info and events. They were
not converted mechanically.

## Enum fallbacks

`Codec`, `CodecEncryptionMode`, `HostBannerMode`, `HostMessageMode`, `ChannelBannerMode` and
`VirtualServerStatus` keep their `UNKNOWN` fallback and gain shared `fromIndex`/`fromName`
decoders. `PermissionGroupType`, `PermissionGroupDatabaseType`, `PrivilegeKeyType` and
`TextMessageTargetMode`, whose getters returned `null` for unknown codes, now return a new
`UNKNOWN` constant. `UNKNOWN` describes received values only: `PermissionTarget` and the
group/privilege-key commands reject it as input. `PrivilegeKey.getType()` returns
`PrivilegeKeyType` instead of `int`.

## Migration table

| Before | 2.0 |
| --- | --- |
| `wrapper.get/getInt/getLong/getDouble/getBoolean(name)` (defaults `""`/`-1`/`false`) | `find*` (optional) or `require*` (throwing) |
| `DefaultArrayResponse.getFirstResponse()` returning `Wrapper.EMPTY` | `Optional<Wrapper>` |
| `int` client / database IDs in models, events and parameters | `ClientId` / `ClientDatabaseId` (see above) |
| `Client.getServerGroups()` `int[]`, `getBadgeGUIDs()` `String[]` | `List<Integer>`, `List<String>` |
| `ClientJoinEvent.getClientServerGroups()` comma-separated `String` | `List<Integer>` |
| `ClientJoinEvent.isClientRequestingToTalk()` read a timestamp as a flag (failed for real requests) | compares the request time with `0` |
| `ChannelBase.getBannerMode()`, `getBannerGraphicsUrl()` | only on `ChannelInfo` (`channellist` does not send them) |
| `VirtualServerInfo.getBandwidthReceivedLastMinute()` read as `int` | `long`, like the other counters |
| `ServerGroup.getSaveDb()` `int` | `isSavedInDatabase()` |
| `FileTransferParameters.getStatus()` `0` default, `getQueryError()` (always present, check `isSuccessful()`) | `OptionalInt`, `findFailure()` (`Optional<QueryError>`, empty unless the status reports a failure); `getSeekPosition()` added |
| `IconFile.getIconId()` `-1` | `OptionalLong` |
| `ClientInfo(int, Map)`, `ClientInfo.getDefaultChannel()` `-1` | `ClientInfo(ClientId, Map)`, `OptionalInt` |
| `Snapshot.get()` | `Snapshot.data()` |
| "required field" failures as plain `TS3Exception` ("response lacks …") | `TS3InvalidValueException` |

## Verification and boundaries

- Live, TS3 3.13.8 RAW and SSH: `ModelCompatibilityIT` (every typed getter of whoami,
  server/instance/host/connection info, version, bindings, clients, channels, servers,
  server/channel groups and members, database clients, custom properties, bans, privilege
  keys, permission metadata/assignments/overview/search, query logins, offline messages,
  file list/info/transfers, and eleven event types; returned lists are unmodifiable), plus
  the existing compatibility suites migrated to the typed models. Field presence and units
  above were probed on the same pinned image.
- Offline-server rows are asserted live in `ServerQueryCompatibilityIT.serverLifecycleDomain`
  (the limited license allows one running server, so the model IT cannot add one).
- Scripted only: complaints (need a voice client), privilege-key-used events (query clients
  cannot use keys, 516), and malformed or unknown values (a real server does not send them).
- Out of scope: Web Query (#452), JPMS exports for the `commands` packages (#447).

## Release intent

`feat!`, planned **2.0.0**. BREAKING CHANGE: response models and events are immutable and
use typed values; getters that returned `-1`, `""`, `false`, `null` or `Date` now return
`Optional*`, `Instant`, `Duration`, `ClientId`/`ClientDatabaseId`, records or `UNKNOWN`,
and required values fail with `TS3InvalidValueException`. Domain APIs take the typed client
IDs. Migrate with the table above. No dependency, protocol, default or transport change.
