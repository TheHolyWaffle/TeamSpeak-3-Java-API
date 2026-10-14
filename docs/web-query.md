# TS3 Web Query

`TS3WebQuery` talks to the HTTP(S) Web Query interface of a TeamSpeak 3 server (3.12 or later). It uses
the same domain APIs, models and exceptions as RAW and SSH. Web Query is an **optional** transport: it is
verified separately (below) and is outside the 2.0.0 release acceptance. Only TeamSpeak 3 is
considered; TeamSpeak 6 is not supported.

Use it for stateless administration where an HTTP endpoint suits better than a long-lived query
session. It cannot replace RAW or SSH for events or file transfers.

```java
WebQueryConfig config = WebQueryConfig.builder(URI.create("https://ts.example.org:10443"), apiKey)
        .setRequestTimeout(Duration.ofSeconds(8))
        .build();
try (TS3WebQuery query = new TS3WebQuery(config)) {
    System.out.println(query.getApi().instance().version().version());
    TS3Api server = query.getApi(1);           // virtual server ID 1
    server.channels().create("Lobby", Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
    TS3ApiAsync async = query.getAsyncApiByPort(9987);
    async.clients().list().thenAccept(clients -> System.out.println(clients.size()));
}
```

## How it differs from RAW and SSH

| | RAW / SSH (`TS3Query`) | Web Query (`TS3WebQuery`) |
| --- | --- | --- |
| Connection | One session: `connect()`, keep-alive, reconnect | None: every command is an independent HTTP POST |
| Authentication | `login` or SSH credentials | An API key on every request (`x-api-key` header) |
| Virtual server | Selected once with `use` | In each request path: `getApi()` for the instance, `getApi(id)` or `getApiByPort(port)` for a virtual server |
| Ordering | Commands run one at a time, in order | Asynchronous commands run concurrently and may complete in any order; wait for a result before sending a command that depends on it |
| Events | `events().register(...)` and `subscribe(...)` | None: Web Query has no notification stream |
| File repository and transfers | Supported | None: the server rejects every `ft*` command |
| Deadlines | Connect, handshake, queue wait, response, close | Connect, request (send to complete response), close |
| JVM lifetime | An open query keeps the JVM alive | An open client does not keep the JVM alive |

The server represents Web Query requests with a query client of the key's owner. Its nickname and
channel persist across requests on the server side: `setNickname`, `updateClient`, `moveQuery` and
`messages().sendChannel(channelId, ...)` change that shared client for every later request with the
same key, over HTTP and HTTPS alike. `whoAmI()` describes it; its client ID can change between
requests.

## Enabling Web Query on the server

Web Query is enabled by default from TS3 3.13 on port 10080 (HTTP). The server's
`doc/webquery.md` and `doc/server_quickstart.md` describe it.

- `query_protocols` must contain `http` and/or `https`; the official Docker image takes
  `TS3SERVER_QUERY_PROTOCOLS`, for example `raw,ssh,http,https`.
- HTTPS (port 10443) needs `query_https_certificate_file` and `query_https_private_key_file` (PEM).
  The Docker image passes extra parameters on its command line:
  `ts3server query_https_certificate_file=... query_https_private_key_file=...`.
- `query_http_port`/`query_http_ip` and `query_https_port`/`query_https_ip` change the endpoints.

TeamSpeak recommends HTTPS, or a reverse proxy that terminates TLS, when Web Query is reachable over
a network. A base URI with a path prefix, such as `https://proxy.example.org/teamspeak/`, is kept.

### API keys

The server prints an API key for `serveradmin` when it creates a new database. Create others over any
transport with `api.apiKeys()`, or with ServerQuery `apikeyadd`:

```java
ApiKey key = api.apiKeys().add(ApiKeyScope.WRITE, Duration.ofDays(30));  // Duration.ZERO never expires
String secret = key.getSecret().orElseThrow();  // shown only once
```

A key is bound to the virtual server selected when it was created (`getVirtualServerId()`), or to the
instance when none was selected (`0`), and acts with its owner's permissions within its scope:
`READ`, `WRITE` or `MANAGE`. `list()`, `list(owner)`, `listAll()` and `delete(id)` manage keys;
listings never include secrets. Creating keys needs `b_virtualserver_apikey_add`; keys of other
clients need `b_virtualserver_apikey_manage`. Owners must be database clients of the bound server.

## Configuration

`WebQueryConfig.builder(URI baseUri, String apiKey)` returns a builder; `build()` takes an immutable
snapshot.

| Setting | Default | Description |
| --- | --- | --- |
| `baseUri` | required | `https://host:10443` or `http://host:10080`, optionally with a path prefix; no user information, query or fragment |
| `apiKey` | required | Visible ASCII; sent only in the `x-api-key` header |
| `setConnectTimeout(Duration)` | 4 s | TCP and TLS connection |
| `setRequestTimeout(Duration)` | 4 s | From sending a request to its complete response |
| `setCloseTimeout(Duration)` | 4 s | Budget for `close()` to abort requests and stop workers |
| `setRequestCapacity(int)` | 16 | Requests in flight; excess fails immediately with `TS3QueueFullException` |
| `setSslContext(SSLContext)` | JDK trust store | Trust for HTTPS, for example a private certificate authority; hostname verification stays on |
| `setPlaintextHttpAllowed(boolean)` | `false` | Required to use an `http` base URI |

There is no client-side flood delay. A Web Query client that is not on the server's query allowlist
should limit its request rate, for example with a small request capacity. `close()` aborts requests in
flight; they fail with an unknown outcome.

Modular applications add `requires java.net.http;`: the library is an automatic module and does not
resolve the JDK HTTP client on its own. Classpath applications need nothing extra.

## Errors

Every failure is a `TS3Exception`. `isOutcomeUnknown()` tells whether the server may have run the
command.

| Situation | Exception | Outcome unknown |
| --- | --- | --- |
| Operation Web Query does not offer (see below) | `TS3UnsupportedOperationException`, before any request | no |
| Missing, invalid or expired key (5124, 5122), or a command outside the key's scope (5120): HTTP 401/403 | `TS3AuthenticationFailedException`, a `TS3CommandFailedException` | no |
| Any other server error, such as 768 (invalid channel ID), with HTTP 200 or 400 | `TS3CommandFailedException` with the server's ID, message and extra message | no |
| An HTTP status without the documented JSON status, for example from a proxy, or an unreadable body | `TS3HttpException` with `getStatusCode()` | for anything but 4xx |
| Connection refused, connect timeout or rejected TLS certificate | `TS3ConnectionFailedException` | no |
| Request deadline expired | `TS3TimeoutException` (`RESPONSE`) | yes |
| Request capacity exhausted | `TS3QueueFullException` | no |
| Client closed before sending | `TS3QueryShutDownException` | no |
| Client closed, request cancelled or connection lost after sending | `TS3UnknownOutcomeException` | yes |

Error 1281 (empty result set) is a successful empty result, as over RAW and SSH.

## Security

- HTTPS is required unless `setPlaintextHttpAllowed(true)` is set. Over plain HTTP the API key and all
  data are readable by anyone on the path; use it only on loopback, a private network or behind a local
  TLS-terminating proxy.
- The key is sent only in the `x-api-key` header, never in a URL. `WebQueryConfig.toString()`,
  `TS3WebQuery.toString()` and exception messages exclude it; models and raw responses redact
  `apikey` values in `toString()`.
- Redirects are not followed, so the key is never forwarded to another host.
- Prefer the narrowest scope and a limited lifetime, and delete keys that are no longer needed. A
  key is effective until deleted or expired.

## Supported operations

`TS3_WEBQUERY_*` evidence from [`WebQueryCompatibilityIT`](#verification) backs this table: TS3
**3.13.8** over HTTP and over HTTPS. "Verified" means the operation succeeded and its result or effect
was checked, including from an independent ServerQuery session where noted. Errors are TS3 error IDs.

| Domain | Verified over HTTP and HTTPS | Rejected by the server, verified | Fails locally (unsupported) | Not verified |
| --- | --- | --- | --- | --- |
| `TS3Api` | `whoAmI`, `setNickname`, `updateClient`, `moveQuery`, `executeRawCommand` (including pipe-separated entries) | | `login`, `logout`, `selectVirtualServerById`, `selectVirtualServerByPort`; raw `use`, `login`, `logout`, `quit`, `help`, `servernotify*`, `ft*` | |
| `apiKeys()` | all | `delete` of an unknown ID (5123); an owner that is not a database client of the bound server (512) | | |
| `instance()` | `info`, `edit`, `hostInfo`, `version`, `bindings`, `logEntries`, `stopProcess` | | | |
| `servers()` | `list`, `findIdByPort`, `create`, `delete`, `start`, both `stop`, `info`, `edit` (seen by ServerQuery), `connectionInfo`, `createSnapshot`, `deploySnapshot`, `logEntries` | `delete` of a running server (1035), `start` of a deleted server (1024) | | |
| `channels()` | all, with protocol escapes and Unicode (seen by ServerQuery) | `info` of a deleted channel (768) | | |
| `clients()` | `list`, `info`, `findByName`, `search`, `findByUniqueId`, both `isOnline`, both `move`, `moveAll`, `kickFromChannel`, `edit` | `kickFromServer` and `poke` of query clients (516) | | success for voice clients |
| `databaseClients()` | `list` (both), `info`, `search`, `findByUniqueId`, `edit`, custom properties (all) | `info` of an unknown ID (512) | | `delete` |
| `serverGroups()` | all | | | |
| `channelGroups()` | all | | | |
| `permissions()` | all, including pipe-separated multi-permission requests for every target type | | | |
| `bans()` | `list`, `add`, `delete`, `deleteAll` | `banClients` of a query client (516), `delete` of a deleted ban (3328) | | `banClients` for voice clients |
| `complaints()` | all | | | |
| `privilegeKeys()` | `list`, both `create...`, `delete` | `use` by the Web Query client (516) | | |
| `queryLogins()` | `list`, `search`, both `create` (the instance login then authenticates over RAW), `delete` | `updateOwn`: the Web Query client has no own login (512) | | |
| `messages()` | `sendPrivate`, both `sendChannel`, `sendServer(String)`, `broadcast`, `sendOffline`, `listOffline`, `getOffline` of an unknown ID (empty) | `sendOffline` to an unknown unique ID (512) | `sendServer(int, String)` (needs `use`; call `getApi(serverId).messages().sendServer(...)`) | reading, marking and deleting an existing offline message |
| `events()` | | | all | |
| `files()` | | | all | |

The server's own documentation lists `ft*`, `help`, `login`, `logout`, `quit`,
`servernotifyregister`, `servernotifyunregister` and `use` as unsupported, and the suite records that
3.13.8 answers each of them with 5120 (HTTP 401). The library rejects them locally with a message
that names the alternative, so a configuration problem is not mistaken for an authentication
failure. Other commands sent through `executeRawCommand` are passed to the server unchanged.

### Events

Web Query has no subscription mechanism, so `events().register(...)`, `unregisterAll()` and
`subscribe(...)` fail with `TS3UnsupportedOperationException`. Applications that need notifications
keep a RAW or SSH `TS3Query` for them. The suite verifies that such a session receives the
notifications caused by Web Query requests, such as channel creation and text messages; the Web Query
client is their invoker. Polling list commands over Web Query is an alternative for coarse state.

### File transfers

The server rejects every `ft*` command over Web Query, including directory listings, so the whole
`files()` domain fails locally. Use a RAW or SSH query for files and icons.

## Request mapping

Each command is a POST to `<base>/<command>`, `<base>/<sid>/<command>` or
`<base>/byport/<port>/<command>`, with `Content-Type: application/json`. Parameters form one JSON
object whose values are strings; options such as `-topic` become members with empty values. A command
with pipe-separated entries, such as several permissions or client IDs, becomes an array of objects
whose first object also holds the shared parameters, as described in the server's
`doc/webquery.md`. Values are sent as text, without ServerQuery escaping.

The `status` object (`code`, `message`, `extra_message`) becomes the shared `QueryError`, and each
`body` row becomes a response row. `DefaultArrayResponse.getRawResponse()` is the ServerQuery encoding
of those rows, the same for every transport.

## Verification

```sh
./mvnw -B -ntp -Pintegration verify
```

`WebQueryCompatibilityIT` runs in the required integration profile. Its fixture starts the pinned TS3
image with `raw,ssh,http,https`, and a certificate generated for that run and trusted only by the test
client. A RAW session provisions an instance `MANAGE` key through `apiKeys()`. Each domain test runs
over HTTP and HTTPS. A separate disposable server covers virtual-server lifecycle, snapshots,
permission reset and process shutdown. The suite also checks rejected, out-of-scope and revoked keys,
an untrusted HTTPS certificate, and that the server rejects every command the library blocks.
Evidence is written to `target/compatibility/TS3_WEBQUERY*`.

Unit tests cover JSON parsing and quoting, request and response mapping, configuration validation, and
client behavior against an in-process HTTP server: paths, headers, timeouts, capacity, cancellation,
closing and locally rejected operations. The packaging consumer check makes a Web Query request from
the staged JAR on the module path and on the classpath.

Only TS3 3.13.8 on Linux amd64 is verified. Other server versions, voice-client targets and the
operations marked "not verified" above are not claimed.

Implementation references: the TS3 server's `doc/webquery.md` and `doc/server_quickstart.md`
(shipped in the pinned image under `/opt/ts3server/doc/`) and its `serverquerydocs/apikeyadd.txt`,
[Java 25 `HttpClient`](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpClient.html),
[`HttpRequest` timeouts](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpRequest.Builder.html#timeout(java.time.Duration)),
[RFC 8259 (JSON)](https://www.rfc-editor.org/rfc/rfc8259).
