# Java 25 implementation cleanup (2.0 migration)

Task 17 ([#445](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/445)) applies
Java 25 language and library features where they make the implementation easier to follow.
It adds no new protocol behavior. Encoded command text, responses, deadlines, retries and
admission limits are unchanged. The last section lists the intentional breaks.

## What changed

| Area | Change | Evidence that behavior is kept |
| --- | --- | --- |
| Command parameters | `Parameter` is a sealed interface. `KeyValueParam`, `OptionParam`, `RawParam`, `ValueParam` and `ArrayParameter` are records that return their wire text from `encoded()` | `CommandSyntaxTest`, `FileCommandsTest`, `DomainApiTest` expected command lines, live command ITs |
| Escaping | `CommandEncoding.encode` escapes in one pass with a `switch`, like `decode`, instead of eleven chained `replace` calls | `CommandEncodingTest` round trips; `CommandSyntaxTest` compares the old and new encoders on 2,000 random strings |
| Command building | No capacity hints. Array loops become `ArrayParameter.of(key, values)` or explicit entries. Properties are added directly in map order | `CommandSyntaxTest.propertiesKeepMapOrderAndRejectReadOnlyProperties` |
| Responses | `ResponseBuilder` joins rows with `StringJoiner`. `DefaultArrayResponse` uses `List.of`/`Map.of` and a shorter parser | `DefaultArrayResponseTest`, `ServerQueryProtocolTest` |
| Command queue | One entry per admitted command, holding enqueue/send time, owner, retries and completion work, replaces five parallel `IdentityHashMap`s | `ReconnectSemanticsTest`, `ConnectionLifecycleTest`, `CommandFuturesTest`, `EventResilienceTest`, run repeatedly |
| Events | Immutable `Map.ofEntries` notification registry. Unused bulk listener methods removed | `EventResilienceTest` |
| Reconnect | Constant, linear and exponential backoff share one implementation with the same factories, defaults, messages and attempt limits | `ReconnectBackoffTest.builtInStrategiesKeepTheirDelayProgressionAndAttemptLimit` |
| JDK helpers | `Math.clamp`, stream pipelines (`IntStream.iterate`, `Collectors`), imports instead of fully qualified names, `var` in touched code | Existing unit tests |
| Threads | Reader, writer and keep-alive no longer subclass `Thread`. Daemon status is explicit | `ThreadModelTest`; see [threading model](connection-lifecycle.md#threading-model) |
| Tests | Multi-line fixtures use text blocks; an `instanceof` chain uses a pattern-matching `switch` | — |

## Evaluated and kept

- **Virtual threads.** Blocking work that admission already bounds (initialization, handlers,
  file transfers, command completions, keep-alive) runs on virtual threads. The protocol
  reader/writer, the deadline scheduler and the bounded event pool stay on platform threads.
  The [threading model](connection-lifecycle.md#threading-model) explains why. Cheap threads
  replace neither the single writer nor admission limits.
- **Pattern matching in production code.** `AGENTS.md` prohibits production `instanceof`
  checks, so behavior stays behind polymorphic contracts such as `Parameter.encoded()`.
  Pattern matching is used in tests only.
- **More records.** `TS3Config` is a mutable builder. `QueryConfig` keeps credentials out of
  `toString`, and its 23 getters would all be renamed. `FloodRate` would only rename `getMs()`.
  The response models were settled by #444. Converting these would break callers with no
  readability gain.
- **Bespoke loops kept on purpose.** `ProtocolLineReader` keeps a partial line across socket
  read timeouts, which `BufferedReader.readLine` would lose. File-transfer copy/skip loops
  check abort and deadline state between chunks, which `transferTo`/`skipNBytes` cannot do.
  `Deadline.join` keeps `TimeUnit.timedJoin`, because `Thread.join(Duration)` throws for a
  thread that has not started yet.
- **Formatting.** No formatting-only changes are mixed in; formatting and analysis belong to
  #446. License notices are unchanged.

## Migration

These breaks affect the public `commands` packages (internal command builders, see the
[domain API guide](api-migration.md#verification-and-boundaries)) and two obsolete overloads:

| Before | After |
| --- | --- |
| `TS3Config.setCommandTimeout(int ms)` (deprecated) | `setCommandResponseTimeout(Duration.ofMillis(ms))` |
| `new ReconnectingConnectionHandler(handler, start, cap, addend, multiplier)` | Add `maxAttempts`; the previous default was `10` |
| `Parameter.appendTo(StringBuilder)` / `toString()` | `Parameter.encoded()` |
| `new ArrayParameter(n[, perEntry]).add(p)` | `new ArrayParameter(List<List<Parameter>>)` or `ArrayParameter.of(key, values)` |
| Parameter classes with constructors only | Records with accessors such as `key()`/`value()` and value equality |
| `FileCommands.ftInitDownload(id, path, cid, cpw)` | Pass seek position `0` |
| `FileCommands.ftInitUpload(id, path, cid, cpw, size, overwrite)` | Pass `resume` `false` |
| `PermissionCommands.permIdGetByName(String)` | The varargs overload, same command text |
| `PermissionCommands.clientAddPerm(int, String, boolean, boolean)` | Pass `1`/`0` as the `int` value |
| Reader/writer/event threads inherited daemon status; the deadline scheduler inherited it from the thread that created the query | Reader, writer and event workers are daemon threads. The deadline scheduler is non-daemon, so an open query keeps the JVM alive even when created on a virtual or daemon thread. Close it with `close()` or `exit()` |

Release intent: `refactor!:` for the planned **2.0.0**, with a `BREAKING CHANGE` footer
listing the rows above. No dependency, default, transport or protocol change.
