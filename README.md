TeamSpeak 3 Java API
====================
[![Build Status](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/actions/workflows/maven.yml/badge.svg?branch=master)](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/actions/workflows/maven.yml) [![Maven Central](https://img.shields.io/maven-central/v/com.github.theholywaffle/teamspeak3-api.svg)](https://central.sonatype.com/artifact/com.github.theholywaffle/teamspeak3-api) [![Javadocs](https://www.javadoc.io/badge/com.github.theholywaffle/teamspeak3-api.svg)](https://www.javadoc.io/doc/com.github.theholywaffle/teamspeak3-api)

A Java library for the TeamSpeak 3 ServerQuery interface: server administration, notifications
and file transfers over raw TCP or SSH, and optionally administration over HTTP(S) Web Query.

> This branch is the 2.0 development line. It needs Java 25 and contains intentional breaking
> changes; see [migrating from 1.x](#migrating-from-1x). The Javadoc badge shows the latest
> published release.

- [Requirements and compatibility](#requirements-and-compatibility)
- [Installation](#installation)
- [Quick start](#quick-start)
- [Run an example](#run-an-example)
- [Using the API](#using-the-api): [domain APIs](#domain-apis), [futures](#asynchronous-api-and-futures),
  [events](#events), [deadlines](#deadlines-and-closing), [reconnects](#reconnect-guarantees),
  [file transfers](#file-transfers), [SSH trust](#ssh-host-key-trust), [logging](#logging),
  [flood rate](#flood-rate), [Web Query](#web-query)
- [Configuration reference](#configuration-reference)
- [Development](#development)
- [Getting help and contributing](#getting-help-and-contributing)

## Requirements and compatibility

- **Java 25 or newer** to build and run; no preview features. Java 8–24 are not supported by 2.0.
- **A TeamSpeak 3 server** with ServerQuery enabled, and a query login (`serveradmin` or one created
  in the TeamSpeak client under *Tools → ServerQuery Login*).
- **Docker** only for the [local development server](#run-an-example) and the
  [integration tests](#integration-tests). Using the library needs no Docker.

| Server | Transport | Status |
| --- | --- | --- |
| TeamSpeak 3 server 3.13.8 (official Docker image, Linux amd64) | Raw TCP (port 10011) | Verified by the integration suite in CI |
| TeamSpeak 3 server 3.13.8 (official Docker image, Linux amd64) | SSH (port 10022) | Verified by the integration suite in CI |
| TeamSpeak 3 server 3.13.8 (official Docker image, Linux amd64) | Web Query over HTTP (10080) and HTTPS (10443) | Optional, new in 2.0; operations listed in [Web Query](docs/web-query.md#supported-operations) verified by the integration suite in CI |
| Other TeamSpeak 3 server versions | Raw TCP, SSH, Web Query | Not verified |
| TeamSpeak 6 | — | Not supported |

The exact image digest and per-command results are listed in the
[command compatibility inventory](docs/command-compatibility.md) and
[integration tests](docs/integration-tests.md). On Apple Silicon the TS3 image runs under Docker's
amd64 emulation; that is not native arm64 support. The ServerQuery commands themselves are
described in the `doc/` and `serverquerydocs/` folders shipped with every TS3 server
(an unofficial copy of the [ServerQuery manual](https://www.teamspeak-info.de/downloads/ts3_serverquery_manual.pdf)
is also available). Voice, audio and screen sharing are out of scope.

## Installation

The library is published to [Maven Central](https://central.sonatype.com/artifact/com.github.theholywaffle/teamspeak3-api)
as one regular JAR with sources and Javadoc. Its POM brings in SSHJ, the patched Bouncy Castle
modules, the SLF4J API and Gson (JSON for Web Query). 2.0 no longer publishes a standalone `with-dependencies` JAR; see
[packaging](docs/packaging.md#migration-from-the-standalone-jar).

```xml
<dependency>
  <groupId>com.github.theholywaffle</groupId>
  <artifactId>teamspeak3-api</artifactId>
  <version>...</version>
</dependency>
```

```kotlin
dependencies {
    implementation("com.github.theholywaffle:teamspeak3-api:...")
}
```

On the module path, the JAR is the automatic module `com.github.theholywaffle.teamspeak3`:
add `requires com.github.theholywaffle.teamspeak3;` to your `module-info.java`
(see [Java modules](docs/packaging.md#java-modules)). Add an SLF4J provider as described under
[logging](#logging).

### Verify a release

Every release from 2.0.0 on is built and published by this repository's
[`release.yml`](.github/workflows/release.yml) with a signed GitHub build provenance attestation.
Maven Central does not show it, so verify a downloaded artifact with the
[GitHub CLI](https://cli.github.com/):

```sh
gh attestation verify teamspeak3-api-<version>.jar --repo TheHolyWaffle/TeamSpeak-3-Java-API \
  --signer-workflow TheHolyWaffle/TeamSpeak-3-Java-API/.github/workflows/release.yml
```

This proves which workflow and commit produced the JAR. Reproducible builds confirm it
independently: rebuilding the tagged commit yields byte-identical artifacts. See
[verify a published release](docs/releases.md#verify-a-published-release).

## Quick start

```java
import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;

TS3Config config = new TS3Config()
        .setHost("ts.example.net")
        .setLoginCredentials(System.getenv("TS3_USERNAME"), System.getenv("TS3_PASSWORD"));

try (TS3Query query = new TS3Query(config)) {
    query.connect();                       // connects and logs in
    TS3Api api = query.getApi();
    api.selectVirtualServerById(1);
    api.setNickname("PutPutBot");
    api.messages().sendChannel("PutPutBot is online!");
    query.exit();                          // optional graceful quit; close() follows
}
```

`connect()` throws `TS3ConnectionFailedException` if the server is unreachable or the login is
rejected. Every API call either returns its result or throws a `TS3Exception` subtype. Use SSH
with `.setProtocol(TS3Query.Protocol.SSH)`; it requires the login and a trusted
[host key](#ssh-host-key-trust).

## Run an example

The [examples](src/example/java/com/github/theholywaffle/teamspeak3/example) are compiled by every
build, so they cannot drift from the API. They contain no hosts or passwords: everything comes from
environment variables.

| Example | Shows |
| --- | --- |
| `ClientInfoExample` | Listing channels and clients, then disconnecting |
| `CreateChannelExample` | Creating, reading and deleting a channel |
| `FileTransferExample` | Uploading and downloading files and icons with byte arrays and streams |
| `ChatBotExample` | Event subscription and replying to `!ping` / `hello` in a channel |
| `EventListenerExample` | Every event type and the query's event statistics |
| `ReconnectExample` | Reconnect strategy, session restoration, retry policy and asynchronous commands |
| `TrollExample` | The asynchronous API driven by a scheduler |
| `WebQueryExample` | [Web Query](#web-query) with an API key: listing clients, creating and deleting a channel |

Long-running examples stop when you press Enter (or close standard input).

### Against a disposable local server

Needs JDK 25 in `JAVA_HOME` and a running Docker daemon. Read the
[TeamSpeak 3 server license](https://hub.docker.com/_/teamspeak) first; the development server
accepts it on your behalf.

1. In a first terminal, start the [development server](docs/development-server.md):

   ```sh
   ./mvnw -ntp -Pdev-server test-compile exec:exec
   ```

   When it is ready, it prints `export` lines with the host, the mapped ports, a disposable
   `serveradmin` password, an SSH trust file, and a Web Query API key and certificate for this
   session, then waits. Keep it running.

2. In a second terminal in the same directory, paste the first `export` line and run an example:

   ```sh
   export TS3_HOST='127.0.0.1' TS3_USERNAME='serveradmin' TS3_PASSWORD='…' …   # copied from terminal 1
   ./mvnw -q -ntp -Pexample test-compile exec:exec -Dexample=ClientInfoExample
   ```

   Choose any example with `-Dexample=<name>`. To use SSH instead of raw TCP, also paste the
   second `export` line. `WebQueryExample` needs only the third `export` line. On Windows PowerShell, set each variable as `$env:TS3_HOST = '127.0.0.1'`
   and use `mvnw.cmd`.

3. Press Enter in the first terminal to stop the server. All data, ports, keys and trust files are
   discarded; the next start is a clean server.

### Against your own server

Set the variables yourself and run the same Maven command:

| Variable | Required | Meaning |
| --- | :---: | --- |
| `TS3_HOST` | yes | Server host name or IP address |
| `TS3_USERNAME`, `TS3_PASSWORD` | yes | ServerQuery login |
| `TS3_PROTOCOL` | no | `raw` (default) or `ssh` |
| `TS3_QUERY_PORT` | no | Defaults to 10011 for raw and 10022 for SSH |
| `TS3_VIRTUAL_SERVER_ID` | no | Virtual server to select, default 1 |
| `TS3_NICKNAME` | no | Query nickname, default `Java API example` |
| `TS3_FLOOD_RATE` | no | `default` (350 ms between commands) or `unlimited` for an allowlisted IP; see [flood rate](#flood-rate) |
| `TS3_SSH_KNOWN_HOSTS` | no | OpenSSH trust file for SSH, default `~/.ssh/known_hosts` |
| `TS3_FILE_TRANSFER_HOST`, `TS3_FILE_TRANSFER_PORT` | no | Reachable file transfer endpoint when the server's advertised one is not (NAT, Docker) |
| `TS3_COMMUNICATIONS_LOGGING` | no | `true` to log the (redacted) protocol at DEBUG |
| `TS3_WEBQUERY_URI`, `TS3_API_KEY` | `WebQueryExample` only | Web Query endpoint, such as `https://host:10443`, and API key; with the optional variables below and `TS3_VIRTUAL_SERVER_ID`, the only ones `WebQueryExample` reads |
| `TS3_WEBQUERY_CERTIFICATE` | no | PEM certificate(s) to trust for HTTPS instead of the JDK trust store |
| `TS3_WEBQUERY_PLAINTEXT` | no | `true` to allow an `http://` endpoint, which sends the key unencrypted |

Examples change server state (channels, files, icons, messages) and clean up after themselves where
possible. `TrollExample` pokes, kicks and moves real clients: only run it on a test server.

## Using the API

### Domain APIs

`TS3Api` holds the session operations (`login`, `selectVirtualServerById`, `setNickname`, `whoAmI`)
and one domain API per area: `channels()`, `clients()`, `databaseClients()`, `serverGroups()`,
`channelGroups()`, `permissions()`, `bans()`, `complaints()`, `messages()`, `files()`,
`privilegeKeys()`, `queryLogins()`, `apiKeys()`, `servers()`, `instance()` and `events()`.

- Calls block until the server answers and throw a `TS3Exception` subtype on failure:
  `TS3CommandFailedException` (server error, with its ID), `TS3TransportException` (connection
  unavailable), `TS3TimeoutException`, `TS3QueueFullException`, `TS3UnsupportedOperationException`
  and `TS3FileTransferFailedException`. `isOutcomeUnknown()` tells whether a failed command may still
  have run on the server.
- Lookups that can find nothing return an empty `Optional` or list; no method returns `null` or `-1`.
- Responses are immutable models with typed values such as `ClientId` and `ClientDatabaseId`.

See the [API and error guide](docs/api-migration.md), the [model guide](docs/models-and-values.md) and
the [Javadoc](https://www.javadoc.io/doc/com.github.theholywaffle/teamspeak3-api).

### Asynchronous API and futures

`query.getAsyncApi()` offers the same operations returning JDK `CompletableFuture`s. Responses
complete in the order they were received on one query-owned worker; keep callbacks short or pass your
own executor to `thenAcceptAsync` and similar methods. Failures complete the future with the same
`TS3Exception` subtypes. Waiting with a timeout or `orTimeout` only stops waiting: the command still
runs. Cancelling the original future removes a command that has not been sent yet. Details:
[futures](docs/futures.md).

### Events

Receiving events needs two steps: register the notification types with the server, and subscribe a
local listener.

```java
EventSubscription subscription = api.events().subscribe(new TS3EventAdapter() {
    @Override
    public void onClientJoin(ClientJoinEvent e) {
        System.out.println(e.getClientNickname() + " joined");
    }
});
api.events().register(TS3EventType.SERVER);
```

Listeners run on a bounded pool of query-owned threads (`setEventCallbackThreads`, default 4), one
event at a time per subscription, in the order received. A slow listener never blocks reading or
command responses; if its queue (`setListenerQueueCapacity`, default 256) is full, the newest events
are dropped and counted in `query.getEventStatistics()`. Exceptions thrown by listeners are counted and
do not stop delivery. Close the subscription to remove the listener. The library does not deduplicate
notifications. Details: [events and capacity](docs/events-and-capacity.md).

### Deadlines and closing

Every phase has a finite deadline, four seconds by default: connect, handshake (SSH negotiation, login,
session restoration and `onConnect`), queue wait, command response and close. A command that is not
answered in time fails with `TS3TimeoutException` and, because later responses could otherwise be
mismatched, the connection is closed. `close()` aborts immediately and fails outstanding commands;
`exit()` first tries to drain and send `quit` within half the close budget. An open query keeps the JVM
alive until it is closed. A keep-alive `whoami` is sent after 60 seconds without commands. Details:
[connection lifecycle and deadlines](docs/connection-lifecycle.md).

### Reconnect guarantees

Reconnecting is off by default: a lost connection closes the query. With a reconnect strategy,
the library guarantees:

- The login, virtual server, nickname and event registrations from `setSessionConfiguration` are
  restored, in order, before any queued command is sent. Listeners stay subscribed.
- Commands not yet sent keep their order and are sent after restoration.
- A command that was sent but not answered is **never resent** by default: it fails with
  `TS3UnknownOutcomeException`. Only reads you name in `CommandRetryPolicy.safeReads(...)` are replayed.
- Built-in backoff strategies stop after ten attempts (`withMaxAttempts` changes this) and then close
  the query.

```java
config.setLoginCredentials(user, password)
      .setSessionConfiguration(SessionConfiguration.forServer(1)
              .withNickname("PutPutBot")
              .withSubscription(TS3EventType.TEXT_CHANNEL, -1))
      .setReconnectStrategy(ReconnectStrategy.exponentialBackoff())
      .setCommandRetryPolicy(CommandRetryPolicy.safeReads(1, "whoami"));
```

Reconcile mutations that failed with an unknown outcome before retrying them yourself. See
[ReconnectExample](src/example/java/com/github/theholywaffle/teamspeak3/example/ReconnectExample.java)
and [safe reconnect and retry](docs/reconnect-and-retry.md).

### File transfers

`files()` uploads and downloads with byte arrays or caller-owned streams (never closed or flushed by
the library) and supports resume. Limitations:

- File data uses a **separate, unencrypted TCP connection** to the server's file transfer port
  (30033 by default), even when the query uses SSH.
- The server advertises the file endpoint; behind NAT or Docker port mapping, configure a
  `FileTransferEndpointResolver` in `FileTransferConfig`.
- Byte-array downloads and icon buffering are limited to 16 MiB by default; stream larger files.
  Socket reads time out after 4 seconds and a whole transfer after 5 minutes by default.
- Only the TS3 file protocol is supported. A cancelled or failed upload can leave a partial file.
- Channel and client icons are assigned with the `i_icon_id` permission using the ID's signed
  32-bit value; TS3 rejects the `channel_icon_id` and `client_icon_id` properties.

Details: [file transfers](docs/file-transfers.md).

### SSH host key trust

SSH connections verify the server's host key; unknown and changed keys are rejected. By default the
key must be in `~/.ssh/known_hosts` (as `[host]:port` for non-standard ports). Choose another policy
with `setSshHostKeyPolicy`:

- `SshHostKeyPolicy.knownHosts(path)`: a dedicated OpenSSH trust file, read strictly.
- `SshHostKeyPolicy.pinnedKey(publicKey)`: one independently verified key.
- `SshHostKeyPolicy.trustOnFirstUse(path)`: stores the first key seen per host and port in an existing
  application-owned file and rejects later changes. Only use it where the first connection is trusted.

The client authenticates with password, then keyboard-interactive. It uses SSHJ's default algorithms,
which still include legacy ones; compression is off. Details:
[configuration and transports](docs/configuration-and-transports.md#ssh-host-trust).

### Logging

The library logs through the [SLF4J](https://www.slf4j.org/) 2.0 API and ships no provider. Add exactly
one SLF4J 2.0 provider, for example:

```xml
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-simple</artifactId>
  <version>2.0.20</version>
  <scope>runtime</scope>
</dependency>
```

Without a provider, SLF4J prints one warning and discards log output; SLF4J 1.7 bindings are ignored.
See the [SLF4J manual](https://www.slf4j.org/manual.html). `setEnableCommunicationsLogging(true)` logs
every command and response at DEBUG with passwords, privilege keys, API keys and file transfer keys
redacted; other data such as IP addresses and messages is logged unchanged. This
[SimpleLogger configuration](src/example/simplelogger.properties) shows that output.

### Flood rate

TS3 temporarily bans query clients that send commands too fast. The default `FloodRate.DEFAULT` waits
350 ms between commands. Only use `FloodRate.UNLIMITED` when the client's IP address is in the
server's `query_ip_allowlist.txt` (`query_ip_whitelist.txt` before TS3 3.13).

### Web Query

`TS3WebQuery` runs the same domain APIs over a TS3 server's HTTP(S) Web Query interface, authenticated
by an API key (create one with `api.apiKeys()`). Each command is an independent request addressed to
the instance (`getApi()`), a virtual server ID (`getApi(1)`) or a voice port (`getApiByPort(9987)`).
There is no session, so `login`, `selectVirtualServerById`, events and file operations fail with
`TS3UnsupportedOperationException`; keep a RAW or SSH query for those. HTTPS is required unless
plaintext is explicitly allowed. Rejected keys raise `TS3AuthenticationFailedException`; responses
without a usable status, for example from a proxy, raise `TS3HttpException`. Configuration, errors,
security and the verified operations: [Web Query](docs/web-query.md).

```java
var config = WebQueryConfig.builder(URI.create("https://ts.example.org:10443"), apiKey).build();
try (TS3WebQuery query = new TS3WebQuery(config)) {
    query.getApi(1).channels().list().forEach(channel -> System.out.println(channel.getName()));
}
```

### Migrating from 1.x

2.0 changes the build requirements, the API shape, futures, models, defaults and security behavior.
Follow the [2.0 migration guide](docs/migration-2.0.md): it has an upgrade checklist, the changed
defaults and links to the detailed guide for each area. Every API difference from 1.3.1 is
[accounted for](docs/api-differences-2.0.md).

## Configuration reference

`TS3Config` is a reusable builder; `new TS3Query(config)` or `config.build()` takes an immutable,
validated snapshot. Invalid values throw `IllegalArgumentException` (or `IllegalStateException` for an
inconsistent retry configuration) before any connection is opened.

| Setting | Default | Description |
| --- | --- | --- |
| `setHost(String)` | `127.0.0.1` | Server host name or IP address; must not be blank |
| `setQueryPort(int)` | 10011 raw, 10022 SSH | ServerQuery port, 1–65535 |
| `setProtocol(Protocol)` | `RAW` | `RAW` (plain TCP) or `SSH` |
| `setLoginCredentials(String, String)` | none | Login used on every (re)connection; required for SSH; both or neither |
| `setSshHostKeyPolicy(SshHostKeyPolicy)` | `~/.ssh/known_hosts`, strict | SSH host key verification; see [SSH trust](#ssh-host-key-trust) |
| `setFloodRate(FloodRate)` | `FloodRate.DEFAULT` (350 ms) | Delay between commands; see [flood rate](#flood-rate) |
| `setEnableCommunicationsLogging(boolean)` | `false` | Log redacted protocol traffic at DEBUG |
| `setConnectTimeout(Duration)` | 4 s | TCP connection, including name resolution |
| `setHandshakeTimeout(Duration)` | 4 s | SSH negotiation or raw login, session restoration and `onConnect` |
| `setQueueWaitTimeout(Duration)` | 4 s | Wait before a command starts being written |
| `setCommandResponseTimeout(Duration)` | 4 s | From writing a command to its complete response |
| `setCloseTimeout(Duration)` | 4 s | Total budget for `exit()`/`close()` |
| `setCommandCapacity(int)` | 1024 | Accepted, in-flight and completing commands; excess fails with `TS3QueueFullException` |
| `setListenerCapacity(int)` | 64 | Event subscriptions; excess throws `IllegalStateException` |
| `setListenerQueueCapacity(int)` | 256 | Waiting events per subscription; overflow drops the newest |
| `setEventCallbackThreads(int)` | 4 | Threads running event listeners |
| `setReconnectStrategy(ReconnectStrategy)` | `disconnect()` | Reconnect behavior; see [reconnect guarantees](#reconnect-guarantees) |
| `setSessionConfiguration(SessionConfiguration)` | none | Virtual server, nickname and registrations restored on every connection |
| `setCommandRetryPolicy(CommandRetryPolicy)` | `none()` | Reads that may be replayed after a reconnect; requires a session configuration |
| `setConnectionHandler(ConnectionHandler)` | none | Callbacks after each connection and connection loss |
| `setFileTransferConfig(FileTransferConfig)` | 4 s connect/read, 5 min total, 16 MiB, advertised endpoint | File transfer limits and endpoint mapping |
| `setTransportFactory(QueryTransportFactory)` | built-in raw/SSH | Custom stream transport; see [adding a transport](docs/configuration-and-transports.md#adding-a-transport) |

## Development

### Building from source

Install a JDK 25 distribution (for example [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=25))
and set `JAVA_HOME` to it. On macOS: `export JAVA_HOME=$(/usr/libexec/java_home -v 25)`. On Windows, use
`mvnw.cmd` instead of `./mvnw`. From a fresh checkout:

```sh
./mvnw -v                 # must report Java 25 or newer
./mvnw -B -ntp verify
```

The Maven Wrapper downloads and verifies the pinned Maven version, so no Maven installation is needed;
the first build needs internet access. Maven Enforcer rejects JDKs older than 25 and Maven older than 3.10.
`verify` compiles the library and the examples, runs the unit tests, and applies the formatting,
static analysis, Javadoc, coverage and API compatibility checks described in
[quality checks](docs/quality-checks.md). It needs no Docker and publishes nothing. Run
`./mvnw spotless:apply` to fix formatting. CI verifies Java 25 and Java 27.

### Integration tests

```sh
./mvnw -B -ntp -Pintegration verify
```

Starts disposable TS3 servers in Docker and runs the RAW, SSH and Web Query compatibility suites, every
example and the development server launcher. A missing Docker daemon fails the build instead of skipping
tests. See [integration tests](docs/integration-tests.md) for images, licenses and Apple Silicon notes.

### Local development server

`./mvnw -ntp -Pdev-server test-compile exec:exec` starts a disposable server for manual testing, as
shown in [run an example](#run-an-example). Add `-Ddev.once=true` to exit after its own smoke test.
See [development server](docs/development-server.md).

### Documentation

- [Migrating from 1.x](docs/migration-2.0.md) · [API differences from 1.3.1](docs/api-differences-2.0.md)
- [Domain API, errors and absent results](docs/api-migration.md) · [Response models](docs/models-and-values.md) · [Implementation cleanup](docs/implementation-cleanup.md)
- [Futures](docs/futures.md) · [Events and capacity](docs/events-and-capacity.md) · [Connection lifecycle](docs/connection-lifecycle.md) · [Reconnect and retry](docs/reconnect-and-retry.md)
- [Configuration and transports](docs/configuration-and-transports.md) · [File transfers](docs/file-transfers.md) · [Web Query](docs/web-query.md)
- [Command compatibility](docs/command-compatibility.md) · [Integration tests](docs/integration-tests.md) · [Development server](docs/development-server.md)
- [Packaging](docs/packaging.md) · [Quality checks](docs/quality-checks.md) · [CI and releases](docs/releases.md)

## Getting help and contributing

See [getting help](SUPPORT.md) for questions, bug reports and private vulnerability reports, the
[security policy](SECURITY.md) for supported versions, and the [contributing guide](CONTRIBUTING.md)
for building, PR titles, releases and review.
