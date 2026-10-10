# Configuration and stream transports (2.0 migration)

`TS3Config` is a reusable mutable builder. `build()` produces an immutable,
validated `QueryConfig`; `new TS3Query(builder)` takes the same snapshot.
Changing the builder afterward affects only future snapshots and queries.
The builder is not thread safe. Share a completed snapshot between queries.
`TS3Config` is now final: replace subclasses with builder composition.

```java
QueryConfig config = new TS3Config()
    .setServerType(ServerType.TS6)
    .setHost("query.example.org")
    .setProtocol(TS3Query.Protocol.SSH)
    .setQueryPort(10022)
    .setLoginCredentials(username, password)
    .setSshHostKeyPolicy(SshHostKeyPolicy.knownHosts(trustFile))
    .setConnectTimeout(Duration.ofSeconds(4))
    .setHandshakeTimeout(Duration.ofSeconds(8))
    .setQueueWaitTimeout(Duration.ofSeconds(4))
    .setCommandResponseTimeout(Duration.ofSeconds(4))
    .setCloseTimeout(Duration.ofSeconds(4))
    .build();
try (TS3Query query = new TS3Query(config)) {
    query.connect();
    query.getApi().selectVirtualServerById(1);
    System.out.println(query.getApi().getVersion().getVersion());
}
```

The API and package names remain `TS3Query`, `TS3Api` and `teamspeak3` for both
server generations. Both use one SSH transport, reader/framing implementation,
codec, command queue and synchronous/asynchronous API. TS3 defaults to raw TCP
on port 10011; SSH defaults to port 10022. A null host resolves to `127.0.0.1`.
Set `ServerType.TS6` explicitly; TS6 with the built-in raw transport fails at
configuration build time. Blank hosts, partial credentials and missing SSH
credentials also fail before connection resources are created. The declared
server generation describes transport compatibility; it does not negotiate
individual command capabilities (task #441).

## SSH host trust

**Breaking security default:** unknown keys are no longer silently accepted or
stored in `known_ts3_hosts`. The default reads standard `~/.ssh/known_hosts`
strictly. Provision keys through a trusted channel before connecting, or use
`SshHostKeyPolicy.knownHosts(path)` to read an application-owned OpenSSH file.
Nonstandard ports use OpenSSH's `[hostname]:port` entries. Existing
`known_ts3_hosts` files can be selected explicitly after reviewing their keys;
there is no automatic import or fallback to the working directory.

`SshHostKeyPolicy.pinnedKey(publicKey)` compares the encoded server public key
and needs no trust file. Obtain that key independently; a pin applies to the
configured connection, so changing the endpoint while keeping a pin trusts
that same key at the new endpoint.

`SshHostKeyPolicy.trustOnFirstUse(path)` is an explicit choice for environments
where the first connection is trusted. Its parent directory must already exist.
It persists the first key for each host/port, reuses that key and rejects
changes, including changes to a different key algorithm. A write failure fails
verification. Updates are serialized among this library's queries in one JVM;
do not share a TOFU file with concurrent writers in other processes. Key
rotation requires a reviewed update to the trust file or pin.

Container readiness bootstraps a key only from the freshly created disposable
fixture, then supplies an in-memory pin to the library. Dedicated TOFU tests
use JUnit temporary files. The development launcher uses the same pin: neither
flow changes `user.home` or writes trust under the developer's home directory.

Configuration `toString()` excludes both username and password, including the
builder. Communications logging remains an explicit opt-in and can expose
protocol data; this change does not make such logs safe for secrets.

## Deadlines, retries and sessions

The snapshot captures all phase deadlines, capacities, flood delay,
reconnection strategy, command retry policy and session restoration settings.
Defaults and behavior remain described in [connection lifecycle](connection-lifecycle.md),
[reconnect and retry](reconnect-and-retry.md) and [event capacity](events-and-capacity.md).
Replay still requires explicit immutable `SessionConfiguration`; it is not
inferred from commands. Application callbacks and custom factories retain their
own implementation state and must follow their thread-safety contracts.

## Adding a transport

Supply a `QueryTransportFactory` through `setTransportFactory`. It creates a
fresh unopened `QueryTransport` for every connection, including reconnects.
The factory declares supported server/protocol settings and whether it handles
authentication. An unauthenticated transport receives the shared ServerQuery
login command when credentials are configured. Built-in factories validate
their server/protocol combinations; custom factories must declare their own
compatibility through `supports`.

A transport opens ordered UTF-8 input/output streams, calls `Connected` once
when underlying connection establishment completes (before handshake/auth),
and closes promptly to abort pending connect/read/write work. It must tolerate
close during partial initialization. If it owns workers, it must implement
`awaitTermination` within the supplied remaining shutdown budget and report
`isTerminated` accurately. Factories allocate without blocking network I/O so
the query can publish resources before starting connection work.

The transport does not parse commands, response frames, notifications or values.
`ProtocolLineReader` and `StreamReader` own framing; command encoding and
response decoding live under `commands`; `CommandQueue` owns admission,
deadlines and completion; public APIs build commands against that same queue.
No copied command executor is needed for a new stream transport. Web Query is
not a stream transport and remains the separately scoped optional task #452.

Implementation references: [SSHJ 0.41.1 known-host verification](https://github.com/hierynomus/sshj/blob/v0.41.1/src/main/java/net/schmizz/sshj/transport/verification/OpenSSHKnownHosts.java),
[SSHJ host verifier contract](https://github.com/hierynomus/sshj/blob/v0.41.1/src/main/java/net/schmizz/sshj/transport/verification/HostKeyVerifier.java),
[Maven release targeting](https://maven.apache.org/plugins/maven-compiler-plugin/examples/set-compiler-release.html),
[Failsafe verify lifecycle](https://maven.apache.org/surefire/maven-failsafe-plugin/usage.html).
