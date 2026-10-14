# Migrating from 1.x to 2.0

2.0.0 is a major release with intentional source, binary and behavior breaks. Nothing is kept
for compatibility: recompile, then work through the checklist below. Each section links the
detailed guide for its area. Every API difference that japicmp reports against 1.3.1 is
mapped to a section of this guide in the [API difference accounting](api-differences-2.0.md).

## What 2.0 covers

| | 2.0 |
| --- | --- |
| Coordinates | `com.github.theholywaffle:teamspeak3-api`, unchanged |
| Packages | `com.github.theholywaffle.teamspeak3` and its subpackages, unchanged; automatic module `com.github.theholywaffle.teamspeak3` |
| Java | 25 or newer, build and runtime, no preview features |
| Server | TeamSpeak 3 ServerQuery. Verified: TS3 server **3.13.8** (official Docker image, Linux amd64) over raw TCP and SSH. Older and newer 3.x servers are not verified |
| Scope | Server administration, notifications and file transfers |
| Not supported | TeamSpeak 6, the TS3 WebQuery (HTTP) transport, voice, audio and screen sharing |

1.x supported Java 8 and did not state a server version. The 2.0 support statement is the
README's [compatibility table](../README.md#requirements-and-compatibility); see also the
[security policy](../SECURITY.md#supported-versions) for the end of 1.x support. Web Query is a
possible later addition ([#452](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/452)),
not part of 2.0.

## Checklist

1. [Move to Java 25](#java-25-and-the-build) and replace the standalone JAR with the regular
   dependency.
2. [Add one SLF4J 2.0 provider](#logging).
3. [Call the domain APIs](#api-shape) instead of the flat `TS3Api`/`TS3ApiAsync` methods.
4. [Replace `CommandFuture`](#futures) with `CompletableFuture`.
5. [Handle `Optional` results and the new exception types](#errors-and-absent-results).
6. [Update model getters](#models-and-values) to the typed values.
7. [Review configuration, deadlines and closing](#configuration-and-closing).
8. [Provision SSH host keys](#transport-defaults) and check the [changed defaults](#changed-defaults).
9. [Move reconnect session state into the configuration](#reconnect-and-retry).
10. [Use subscription handles for events](#events-and-capacity) and size the queues.
11. [Check file transfer limits](#file-transfers).
12. Review the [behavior changes the compiler cannot find](#behavior-the-compiler-cannot-find).

## Java 25 and the build

- Java 8–24 runtimes are not supported; classes are Java 25 bytecode (class file version 69).
  Build from source with the Maven Wrapper (`./mvnw`), which needs JDK 25 and pins Maven.
- The `with-dependencies` JAR and the `full` Maven profile are gone. Depend on the regular JAR
  with Maven or Gradle: [migration from the standalone JAR](packaging.md#migration-from-the-standalone-jar).
- Runtime dependencies are SSHJ 0.41, Bouncy Castle 1.86 and the SLF4J 2.0 API. On the module
  path, `requires com.github.theholywaffle.teamspeak3;` ([Java modules](packaging.md#java-modules)).
- Copy examples from [`src/example/java`](../src/example/java), not from 1.x; they use the 2.0 API.

## Logging

The library uses the SLF4J **2.0** API. SLF4J 1.7 bindings (such as `slf4j-simple` 1.7 or
`slf4j-log4j12`) are ignored: add exactly one 2.0 provider, or SLF4J prints a warning and discards
the output ([logging](../README.md#logging)). Communication logs and the `toString()` of commands,
responses and configuration redact passwords, privilege keys, API keys and file transfer keys
([quality checks migration](quality-checks.md#migration-20)).

## API shape

`TS3Api` and `TS3ApiAsync` keep only the session operations (`login`, `logout`,
`selectVirtualServerById`, `setNickname`, `whoAmI`, `moveQuery`, `executeRawCommand`).
Everything else moved to one domain API per area, such as `api.channels().create(...)` or
`api.clients().kickFromServer(...)`, often with a request object instead of overloads. The
[operation inventory](api-migration.md#operation-inventory) maps every 1.3.1 method to its
replacement.

Smaller API changes:

- `TS3Query`, `TS3Config`, `ReconnectStrategy` and many models are `final` or records; wrap or
  compose them instead of subclassing.
- `BPermissionType.getId()` and `IPermissionType.getId()` are removed; resolve IDs with
  `permissions().idByName(...)` ([permissions](permission-compatibility.md#migration-for-200)).
- The public `commands` packages are command builders used by the domain APIs. Their parameter
  classes are records, `Command.encoded()` returns the wire text and some obsolete overloads are
  removed ([implementation cleanup](implementation-cleanup.md#migration)). Prefer the domain APIs or
  the controlled escape hatch in [command compatibility](command-compatibility.md#controlled-command-escape-hatch-and-migration).

## Futures

`CommandFuture`, `CommandFuture.SuccessListener` and `CommandFuture.FailureListener` are removed.
Async methods and `Command.getFuture()` return `CompletableFuture`. `onSuccess` becomes
`thenAccept`, `onFailure` becomes `whenComplete`/`exceptionally`, `getUninterruptibly()` becomes the
synchronous API or `CommandFutures.join`, and `ofAll` becomes `CommandFutures.all`. Callbacks run on
one ordered, query-owned worker: move slow work to your own executor with the `*Async` methods.
Details and the full table: [futures](futures.md).

## Errors and absent results

- Failures are `TS3Exception` subtypes: `TS3CommandFailedException`, `TS3TransportException` (with
  `TS3ConnectionFailedException`, `TS3QueryShutDownException` and `TS3UnknownOutcomeException`),
  `TS3TimeoutException`, `TS3QueueFullException`, `TS3UnsupportedOperationException`,
  `TS3InvalidValueException` and `TS3FileTransferFailedException`.
- `isOutcomeUnknown()` says whether a failed command may still have run. Reconcile such mutations
  with the server before retrying.
- `TS3UnknownErrorException` and `TS3UnknownEventException` were never thrown and are removed.
- Lookups that can find nothing return `Optional` or an empty list; nothing returns `null` or `-1`.
  Required values that are missing fail instead of defaulting.

Details: [errors](api-migration.md#errors) and [absent results](api-migration.md#absent-results-and-failures).

## Models and values

Response models and events are immutable. Getters return typed values: `ClientId`,
`ClientDatabaseId`, `Instant`, `Duration`, `Optional*`, lists instead of arrays, records such as
`Binding` and `Snapshot`, and `UNKNOWN` enum constants for values the library does not know.
`Wrapper.get*` accessors become `find*` (optional) or `require*` (throwing). Enums gain
static lookups such as `fromIndex`. Details: [models and values](models-and-values.md#migration-table).

## Configuration and closing

- `TS3Config` is a reusable builder; `new TS3Query(config)` or `config.build()` takes an immutable
  `QueryConfig` snapshot. Later builder changes do not affect existing queries.
- `TS3Query` is `AutoCloseable`: use try-with-resources. `close()` aborts immediately; `exit()` drains
  and sends `quit` within half the close budget. `getState()` reports the lifecycle; a closed query
  cannot connect again, and `connect()` while connected throws.
- `setCommandTimeout(int)` is removed: use `setCommandResponseTimeout(Duration.ofMillis(ms))`. Connect,
  handshake, queue wait, command response and close each have a 4-second deadline; traffic no
  longer extends a pending command's deadline.
- An open query keeps the JVM alive until it is closed.

Details: [connection lifecycle](connection-lifecycle.md) and
[configuration and transports](configuration-and-transports.md).

## Transport defaults

- **SSH host keys are verified.** 1.x silently accepted and stored unknown keys in
  `~/.ssh/known_ts3_hosts`. 2.0 reads `~/.ssh/known_hosts` strictly; choose
  `SshHostKeyPolicy.knownHosts(path)`, `pinnedKey(key)` or `trustOnFirstUse(path)` with
  `setSshHostKeyPolicy`. To keep using reviewed keys from the old file, pass its path to `knownHosts`
  ([SSH host trust](configuration-and-transports.md#ssh-host-trust)).
- A null host means `127.0.0.1`; blank hosts, partial credentials and SSH without credentials are
  rejected before connecting.
- Custom transports implement `QueryTransportFactory`
  ([adding a transport](configuration-and-transports.md#adding-a-transport)).

### Changed defaults

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

- Put credentials in `setLoginCredentials` and the virtual server, nickname and event
  registrations in `setSessionConfiguration`, not in `onConnect`. They are restored in order before
  queued commands continue; session-changing commands are rejected while a restorable session is
  configured.
- A command that was sent but not answered is never resent unless you name it in
  `CommandRetryPolicy.safeReads(...)`.
- Built-in backoff gives up after 10 attempts; `ReconnectingConnectionHandler` constructors take the
  attempt limit explicitly.

Details: [safe reconnect and retry](reconnect-and-retry.md#migrating-existing-reconnect-code).

## Events and capacity

`api.events().subscribe(listener)` (or `query.subscribe`) returns an `EventSubscription`; close it
to remove the listener. `addTS3Listeners`/`removeTS3Listeners` are removed. Registering
notification types with the server is a separate `api.events().register(...)` call. Overflow,
listener failures and unknown notifications are counted in `query.getEventStatistics()`.
Details: [events and capacity](events-and-capacity.md).

## File transfers

File transfers have finite deadlines and a 16 MiB limit for in-memory downloads and icons; stream
larger files to your own `OutputStream`. Cancelling the original future aborts the transfer. Streams
you pass are never closed by the library. Behind NAT or Docker, configure a
`FileTransferEndpointResolver`. File data still uses a separate, unencrypted socket, even with SSH.
Details: [file transfers](file-transfers.md#evidence-and-20-migration).

## Behavior the compiler cannot find

These changes keep their signatures, so recompiling does not reveal them:

- deadlines on every phase, timeouts closing the connection and the finite command queue;
- no automatic resend of sent commands after a reconnect, and a bounded number of reconnect attempts;
- strict SSH host key verification;
- event delivery on several threads, with overflow dropping the newest events;
- `Optional`/empty results instead of `null` or `-1` behind APIs that kept their name;
- secrets redacted in logs and `toString()`;
- an SLF4J 1.7 binding no longer producing output.

## Release notes and support

The [2.0.0 release notes](../CHANGELOG.md) list each breaking change with its pull request. After
2.0.0 is published, 1.x receives no further releases; questions go to
[GitHub Discussions](../SUPPORT.md).
