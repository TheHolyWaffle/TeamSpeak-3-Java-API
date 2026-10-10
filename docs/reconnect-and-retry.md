# Safe reconnect and retry (2.0 migration)

A disconnect cannot tell a client whether a command reached the server. In
particular, losing the response to `channelcreate` must not automatically create
another channel. Commands enter the **unknown-outcome** phase when the writer
claims them, before encoding/writing/flushing; partial writes are ambiguous too.
`TS3UnknownOutcomeException` identifies the protocol command name without its
parameters and extends `TS3QueryShutDownException` for existing failure handlers.
Reconcile mutations with server state before deciding what to do next.

Configure the session before constructing a query:

```java
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.reconnect.CommandRetryPolicy;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.reconnect.SessionConfiguration;

TS3Config config = new TS3Config()
    .setLoginCredentials(System.getenv("TS_QUERY_USERNAME"), System.getenv("TS_QUERY_PASSWORD"))
    .setSessionConfiguration(SessionConfiguration.forServer(1)
        .withNickname("admin-bot")
        .withSubscription(TS3EventType.SERVER, -1)
        .withSubscription(TS3EventType.CHANNEL, 0))
    .setReconnectStrategy(ReconnectStrategy.exponentialBackoff().withMaxAttempts(5))
    .setCommandRetryPolicy(CommandRetryPolicy.safeReads(1, "whoami", "version"));
try (TS3Query query = new TS3Query(config)) {
    query.connect();
    query.getApi().getVersion();
}
```

Credentials on the frozen `TS3Config` provide authentication on **every**
connection: automatic RAW login, or SSH authentication using the configured
protocol. Then the immutable session selects its explicit virtual server ID,
sets the nickname, and registers subscriptions in their configured order. The
user `onConnect` callback runs afterward. All these steps must succeed within
the handshake budget before any application command is delivered. Failure does
not fall back to another virtual server or release the application queue.
Application event listeners belong to the query and need registration only once.

| Command at disconnect | Result |
| --- | --- |
| Sent, with a complete response accepted | Completes once; never replayed |
| Sent, no complete response, default policy | Fails with `TS3UnknownOutcomeException`; never replayed |
| Sent, explicitly selected safe read, retries remaining | Returns to the head of the queue; replays after restoration |
| Sent, safe read whose retry allowance is exhausted | Fails with `TS3UnknownOutcomeException`; later unsent commands can continue |
| Queued and unsent, explicit session configured | Keeps its position and original queue deadline; sends after restoration |
| Queued and unsent, no explicit session | Fails; its previous target cannot be guaranteed |
| Command submitted during disconnection without an explicit session | Fails immediately |
| Explicit close, reconnect exhaustion, or cancelled recovery | Outstanding commands settle; closed queries never reconnect |

Replay is **off by default**, even for reads. `safeReads` accepts only explicitly
named commands from this deliberately small read-only set: `whoami`, `version`,
`serverinfo`, `serverlist`, `channellist`, `channelinfo`, `clientlist`, `clientinfo`.
The retry count means additional sends per command, across connection losses;
it is independent of the reconnect attempt limit. A read can observe newer
server state after reconnect; replay does not promise a consistent snapshot.
Mutations, unknown command names, authentication and session changes cannot be
opted in. Complete server error responses never trigger replay. Enabling replay
without explicit session configuration is rejected before allocating resources.

Reconnect-enabled queries keep one application command in flight, preserving
wire order. Eligible sent commands precede all unsent commands; new submissions
append behind them. A fresh reader discards fragments from the old connection.
Acknowledging a complete response and preparing recovery share the queue lock,
so an acknowledged command cannot also be replayed. Each replay starts a fresh
response budget and queue-wait budget. Unsent commands retain their original
queue deadline throughout backoff and initialization. Cancelled command futures
are skipped before dispatch and before replay; already transmitted operations
cannot be retracted. Cancellation of a mapped future propagates to its source;
aggregate/composed futures retain their existing cancellation contract.

Built-in constant, linear and exponential strategies now default to **ten**
connection attempts per disconnect. Use `withMaxAttempts(n)` to change the
positive limit. Delays use equal jitter between half the current upper bound
(rounded up, at least 1 ms) and that upper bound. The first upper bound is the
starting timeout; each later upper bound increases linearly/exponentially up to
the cap. Start and cap must be positive, cap must be at least start, and
multipliers must be finite. Arithmetic saturates at the cap without integer
overflow. Closing interrupts both sleep and initialization. Interrupts remain
set when a reconnect sleep is interrupted. Exhaustion closes the query and
settles all retained work. `userControlled()` keeps application ownership of
reconnection and its attempt limit; the same queue safety rules apply.

## Migrating existing reconnect code

- Move login credentials to `setLoginCredentials`, and move virtual-server
  selection, nickname and subscriptions from `onConnect` to
  `setSessionConfiguration`. Keep observation, such as refreshing the query
  client ID, in `onConnect`.
- With explicit session configuration and a reconnect-enabled strategy,
  `login`, `logout`, `use`, `clientupdate`, `servernotifyregister` and
  `servernotifyunregister` are rejected on the application queue and in the
  post-restoration callback. This prevents queued work from silently running in
  a session different from the one restored. Configure a new query for a new
  target/session; queries using `disconnect()` retain ordinary runtime session
  changes. Query channel moves are not part of this session configuration;
  applications relying on the query's current channel must account for that
  state themselves and should use explicit channel IDs where available.
- Without explicit session configuration, reconnect can still establish a new
  connection and run a legacy `onConnect` callback, but old pending work fails.
  This replaces historical blind replay and #436's blanket request failure with
  a deliberate, documented recovery contract.
- Replace infinite backoff assumptions with an explicit attempt limit and
  observe command failures. Failed mutation futures are not evidence that the
  mutation did not happen; do not automatically resubmit them.

These intentional behavior changes belong to the reviewed 2.0 major release
(`fix!` release intent) and address
[#437](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/437).
Queue capacity/event dispatch, future replacement, broader error taxonomy and
transport architecture remain owned by their later task issues.

Implementation references: [Java 25 bounded random generation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/ThreadLocalRandom.html),
[Java 25 interruption and sleep](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Thread.html),
and [Maven Failsafe integration-test inclusion](https://maven.apache.org/surefire/maven-failsafe-plugin/examples/inclusion-exclusion.html).
