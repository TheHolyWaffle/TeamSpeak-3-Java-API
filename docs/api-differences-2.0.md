# API differences from 1.3.1

Every public and protected type that differs between the published 1.3.1 JAR and 2.0, as
reported by [japicmp](https://siom79.github.io/japicmp/): added, removed, modified, and types whose
own signature is unchanged but whose superclass or class file changed. Each row names the
[migration guide](migration-2.0.md) section that tells users what to do. Type names are relative
to `com.github.theholywaffle.teamspeak3`; `*` matches any characters.

Every 1.3.1 type also changes from class file version 52 (Java 8) to 69 (Java 25), which
japicmp reports as binary incompatible. That is the [Java 25](migration-2.0.md#java-25-and-the-build)
requirement; the first row lists the types for which it is the only difference.

| Types | Difference | Migration |
| --- | --- | --- |
| `commands.response.ResponseBuilder` | Class file version only | [Java 25](migration-2.0.md#java-25-and-the-build) |
| `TS3Api`, `TS3ApiAsync` | Operations removed and moved to domain APIs; async return types changed | [API shape](migration-2.0.md#api-shape) |
| `*Api`, `*ApiAsync` | Added domain APIs | [API shape](migration-2.0.md#api-shape) |
| `api.BanRequest`, `api.ChannelGroupFilter`, `api.ClientBan`, `api.FileLocation`, `api.PermissionGrant`, `api.PermissionTarget`, `api.UploadMode` | Added request and value types for domain APIs | [API shape](migration-2.0.md#api-shape) |
| `api.BPermissionType`, `api.IPermissionType` | `getId()` removed | [API shape](migration-2.0.md#api-shape) |
| `commands.Command`, `commands.CommandEncoding`, `commands.FileCommands`, `commands.PermissionCommands`, `commands.QueryCommands`, `commands.parameter.*`, `commands.response.DefaultArrayResponse` | Command builders: `encoded()`, parameter records, removed overloads, `Optional` first response | [API shape](migration-2.0.md#api-shape) |
| `commands.ProtocolRedactor` | Added redaction of secrets in protocol text | [Logging](migration-2.0.md#logging) |
| `api.CommandFuture`, `api.CommandFuture$*` | Removed | [Futures](migration-2.0.md#futures) |
| `api.CommandFutures` | Added `CompletableFuture` adapters | [Futures](migration-2.0.md#futures) |
| `api.exception.*` | Exception hierarchy: types added and removed, `TS3TransportException` superclass, `isOutcomeUnknown()` | [Errors](migration-2.0.md#errors-and-absent-results) |
| `api.wrapper.*` | Immutable models: typed, `Optional` and record getters; untyped getters removed | [Models](migration-2.0.md#models-and-values) |
| `api.event.*` | Immutable events with typed values; inherited getters changed | [Models](migration-2.0.md#models-and-values) |
| `api.Snapshot`, `api.ClientId`, `api.ClientDatabaseId` | `Snapshot` became a record; typed IDs added | [Models](migration-2.0.md#models-and-values) |
| `api.ChannelBannerMode`, `api.Codec`, `api.CodecEncryptionMode`, `api.HostBannerMode`, `api.HostMessageMode`, `api.PermissionGroupDatabaseType`, `api.PermissionGroupType`, `api.PrivilegeKeyType`, `api.TextMessageTargetMode`, `api.VirtualServerStatus` | Added static lookups such as `fromIndex` | [Models](migration-2.0.md#models-and-values) |
| `TS3Config`, `QueryConfig`, `TS3Query`, `TS3Query$State` | Final builder with an immutable snapshot, `AutoCloseable` query, lifecycle state, `setCommandTimeout(int)` removed | [Configuration and closing](migration-2.0.md#configuration-and-closing) |
| `SshHostKeyPolicy`, `QueryTransport`, `QueryTransport$*`, `QueryTransportFactory`, `QueryTransports` | Added SSH host key policies and the transport extension point | [Transport defaults](migration-2.0.md#transport-defaults) |
| `api.reconnect.*` | Session configuration and retry policy added; backoff attempt limits; final `ReconnectStrategy` | [Reconnect and retry](migration-2.0.md#reconnect-and-retry) |
| `EventStatistics`, `EventSubscription` | Added subscription handles and event counters | [Events](migration-2.0.md#events-and-capacity) |
| `FileTransferConfig`, `FileTransferEndpointResolver` | Added file transfer limits and endpoint mapping | [File transfers](migration-2.0.md#file-transfers) |
| `TS3WebQuery`, `WebQueryConfig`, `WebQueryConfig$Builder`, `api.ApiKeyScope`, `commands.ApiKeyCommands`, `commands.CommandTrait` | Added the optional Web Query transport, API key administration and command traits; nothing to migrate | [What 2.0 covers](migration-2.0.md#what-20-covers) |

Signatures are not the whole story. Behavior, defaults, runtime and dependency changes are listed in
[behavior the compiler cannot find](migration-2.0.md#behavior-the-compiler-cannot-find) and
[changed defaults](migration-2.0.md#changed-defaults).
