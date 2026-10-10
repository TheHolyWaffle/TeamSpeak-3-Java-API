# Connection lifecycle and deadlines (2.0 migration)

`TS3Query` implements `AutoCloseable`. Prefer a scoped client:

```java
TS3Config config = new TS3Config()
    .setConnectTimeout(Duration.ofSeconds(4))
    .setHandshakeTimeout(Duration.ofSeconds(10))
    .setQueueWaitTimeout(Duration.ofSeconds(30))
    .setCommandResponseTimeout(Duration.ofSeconds(5))
    .setCloseTimeout(Duration.ofSeconds(2));
try (TS3Query query = new TS3Query(config)) {
    query.connect();
    query.getApi().getVersion();
}
```

Import `java.time.Duration` and the library's `TS3Config`/`TS3Query`. All five
budgets default to four seconds. Values must be positive and fit within a
finite monotonic budget (at most `Long.MAX_VALUE / 4` nanoseconds). Configuration
is frozen when constructing a query.

| Budget | Starts | Ends / expiry behavior |
| --- | --- | --- |
| Connect | Starting initialization, including hostname resolution | TCP established; expiry aborts initialization |
| Handshake | TCP established | SSH negotiation/authentication/shell setup, raw automatic login, `onConnect`, and its command queue fully finish; expiry aborts initialization |
| Queue wait | Enqueuing each command, including before connect | Writer takes the command; expiry removes and fails an unsent command without closing a healthy connection |
| Command response | Writer takes each command, before encoding/writing | The command's terminating `error` line; expiry closes the transport and fails outstanding commands |
| Close | Calling `close()` or `exit()` | Shared budget for draining, closing I/O, joining workers and interrupting callbacks |

Elapsed time uses `System.nanoTime()`. An independent owned scheduler checks
connection/command deadlines every 10 ms, so enforcement includes scheduler and
OS scheduling latency. Incoming notifications, partial response data, and other
commands never renew a pending command's deadline. The response budget includes
blocked writes; a reader blocked on a partial line cannot disable timeout checks.
The raw greeting is informational and has no fixed line count or required wait.

`close()` aborts the transport immediately, interrupts I/O workers and fails
outstanding commands. It works before `connect()` and is idempotent. `exit()`
retains a graceful attempt: it allows at most half the close budget for draining
and `quit`, then follows the same abort path. It never waits indefinitely for a
server response. Both close blocking I/O before joining I/O workers and use one
shared remaining budget for all joins. Closing from a callback never waits for
its own executor task. Commands submitted after closure fail immediately.

`getState()` exposes `NEW`, `CONNECTING`, `CONNECTED`, `DISCONNECTED`, `CLOSING`
and `CLOSED`. `CLOSED` is terminal; `connect()` then throws
`IllegalStateException`. Concurrent or repeated `connect()` while connecting or
connected also throws. A disconnected client using a reconnect strategy can
enter `CONNECTING` again. `isConnected()` describes the current transport state,
not a guarantee that the server will respond to the next command.

The client owns its socket/SSH client, three I/O/keepalive workers, deadline
scheduler and virtual-thread callback executor. Applications own callback code
and streams they supply. Initialization handlers now run on an owned callback
thread, not the caller's thread; they must cooperate with interruption. Closing
shuts down the executor and interrupts unfinished callbacks. Java cannot safely
force termination of application code that ignores interrupts or platform name
resolution that does not respond to interruption. The API wait/close budgets
remain bounded, but such external blocking code can outlive them. Use an IP
address if your resolver cannot provide a bounded lookup. File-transfer endpoint
and stream lifecycle changes remain assigned to #442.

## Intentional behavior changes

- `setCommandTimeout(int)` remains as a deprecated millisecond adapter for
  **command response only**. Set the other four durations explicitly if you
  previously relied on that setting for SSH connection/authentication timeouts.
- A response must finish within its own budget. Slow streaming responses need a
  larger response duration; repeated data no longer keeps them alive forever.
- Queue wait is now finite. Increase it for long flood-rate-limited queues.
- Terminating a connection fails its outstanding requests with
  `TS3QueryShutDownException`, including requests on the initialization queue.
  Expired unsent commands fail with `TS3Exception` describing queue expiry.
  A reconnect does not revive failed requests. Explicit safe retry policy is the
  separate scope of #437; custom futures and public error redesign remain with
  #439 and #443.
- Write failures are surfaced as connection termination rather than being
  suppressed by `PrintWriter`.
- `close()` aborts immediately; use `exit()` when a bounded graceful drain is
  useful. Neither method publishes anything or owns a caller's threads.

These changes belong to the reviewed 2.0 major release (`fix!` release intent)
and address [#436](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/436).

Implementation references: [Java 25 Socket close/connect semantics](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/net/Socket.html),
[Java 25 executor ownership and virtual threads](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/Executors.html),
[SSHJ 0.41.1 socket factory and connection lifecycle](https://github.com/hierynomus/sshj/blob/v0.41.1/src/main/java/net/schmizz/sshj/SocketClient.java),
[SSHJ 0.41.1 disconnect cleanup](https://github.com/hierynomus/sshj/blob/v0.41.1/src/main/java/net/schmizz/sshj/SSHClient.java).
