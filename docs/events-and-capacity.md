# 2.0 events and capacity migration

Configure finite budgets before constructing `TS3Query`; configuration is then frozen.

| Setting | Default | What consumes capacity | Overflow |
| --- | --- | --- | --- |
| `setCommandCapacity` | 1024 | Accepted waiting commands, in-flight commands and executing completion callbacks | New command future fails with `TS3QueueFullException` before writing |
| `setListenerCapacity` | 64 | Local listener registrations | Registration throws `IllegalStateException` |
| `setListenerQueueCapacity` | 256 | Waiting events per registration, excluding its claimed callback | Drop newest event for that registration |
| `setEventCallbackThreads` | 4 | Query-owned event executor workers | At most listener capacity tasks wait in its executor queue; admission rejection drops that registration's pending events |

The command budget applies independently to the application queue and the transient
startup/session-restoration commands across all connection generations. Blocked
startup callbacks therefore also bound subsequent reconnect initialization. A reconnect retains the budget of recovered
commands. Completion callbacks release admission only after they return, so a
response stream cannot create an unlimited number of blocked callback threads.
Immediate admission failures settle on the submitting thread; listeners attached
to an already completed JDK future can run on the registering thread. See
[the future contract](futures.md) for completion ordering and async executors. Do not busy-loop on rejection; reduce concurrency or arrange application
backpressure. A callback issuing commands also needs spare admission capacity.

Event callbacks run on a separate owned executor from command completions and
connection handlers. Command completions use one ordered virtual worker; lifecycle callbacks use separate owned virtual threads;
accepted command completions are bounded by the command budget. There is no
caller-runs fallback on the reader. The query stops its owned executors. Applications may supply their own executors
to JDK async completion-stage methods and own their lifetime. Event callbacks for one registration
are serialized in received order. Separate registrations of the same listener are
independent. A slow listener can occupy an event worker and delay other listeners
if all workers are occupied, but cannot stall reading or command completions.
Throwing listeners are counted and the next event still runs. Callbacks must
cooperate with interruption for shutdown to terminate their workers.

Use the new local subscription handle instead of keeping a listener reference:

```java
try (TS3Query query = new TS3Query(new TS3Config()
        .setCommandCapacity(512)
        .setListenerQueueCapacity(128))) {
    try (EventSubscription subscription = query.subscribe(listener)) {
        query.connect();
        query.getApi().registerAllEvents();
        // Use the API while the local listener is installed.
    }
}
```

`subscribe` does not register notification types on the server. Closing its handle
is idempotent, removes that registration and discards waiting events without
waiting for user code. A callback already claimed by a worker may finish after
close. The existing `addTS3Listeners` / `removeTS3Listeners` methods remain;
removal now also discards waiting events. Closing the query removes all local
registrations and rejects new ones. Removed blocked callbacks retain at most the
fixed event worker count, even if registrations are repeatedly replaced.

Poll `query.getEventStatistics()` for monotonic dropped-event, unknown-notification,
malformed-notification, listener-failure and executor-rejection counters. Unknown
notification types are ignored with a DEBUG diagnostic and counted once per wire
line; they cannot terminate the reader or consume command response framing.
Missing/blank notification bodies and parse/construction failures are counted as
malformed. Event models still parse most numeric fields lazily: bad fields read by
a listener count as listener failures, isolated from the reader. This task does
not add strict schemas for every server event. These diagnostics do not log raw payloads; opt-in communications logging still does.

The old alternating one-duplicate filter is removed. Identical join, leave and
move frames now reach listeners individually. The pinned-server probe observed one join, move and leave frame per action with
overlapping server/channel registrations, and no malformed frames. This is evidence
for those versions and operations, not a guarantee that all servers never duplicate
notifications. Without a protocol event
identity, byte equality cannot distinguish duplicate delivery from a legitimate
repeat, and the previous filter could discard events even across intervening
traffic. Consumers that require action deduplication must define it for their own
application. The integration probe records join/move/leave payloads for the pinned
TS3 RAW, TS3 SSH and TS6 SSH combinations in `target/compatibility/*-notifications.txt`;
scripted regressions cover malformed/unknown input and identical repeated frames.

Bounds here cover command/event counts and callback concurrency, not payload byte
sizes, application-retained events or application-created threads. The custom
future replacement remains task #439; wider event/command coverage remains #441.
