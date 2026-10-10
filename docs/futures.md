# 2.0 asynchronous result migration

`TS3ApiAsync` and `Command.getFuture()` return JDK `CompletableFuture<T>`, which
also implements `CompletionStage<T>` and `Future<T>`. `CommandFuture` and its
listener interfaces have been removed. This is an intentional source and binary
break for 2.0. Recompile consumers and update their asynchronous call sites.
No new runtime dependency or preview feature is needed.

## Results, exceptions and composition

| Previous API | 2.0 replacement |
| --- | --- |
| `onSuccess(listener)` | `thenAccept` for short work; `thenAcceptAsync(listener, executor)` for application work |
| `onFailure(listener)` | `whenCompleteAsync((value, failure) -> ..., executor)` or `exceptionally` for recovery |
| `map(fn)` / `then(fn)` | `thenApply(fn)` / `thenCompose(fn)` |
| `immediate(value)` | `CompletableFuture.completedFuture(value)` |
| `ofAll(requests)` | `CommandFutures.all(requests)` for ordered values and fail-fast failure; JDK `allOf` waits for every input |
| `getUninterruptibly()` | Prefer `TS3Api`; `CommandFutures.join(future)` supplies the same synchronous error translation |
| `set(value)` / `fail(error)` in application-owned futures | `complete(value)` / `completeExceptionally(error)` |
| `isSuccessful()` / `hasFailed()` | `state() == Future.State.SUCCESS` / `state() == Future.State.FAILED` |

The library owns completion of API results. Applications may observe, compose,
cancel or apply JDK timeout methods; do not call `complete`, `completeExceptionally`
or `obtrude` on API-owned results.

Each future has one terminal result: a successful value (including `null`), an
exception or cancellation. Multiple observers and dependent stages are allowed.
`get()` and timed `get` throw `ExecutionException` with the underlying failure as
the cause; `join()` throws `CompletionException`. A cancelled result throws
`CancellationException`. Server error causes remain `TS3CommandFailedException`;
transport/deadline/queue failures retain their existing TS3 exception types.
Exceptions from a mapping or callback fail that dependent stage, leaving the
source result intact; retain and observe the stage returned by your callback.
The library does not mutate an exception's stack trace on retrieval.

Use `thenCompose` for an asynchronous follow-up. Return a completed future with
`null` for a successful no-op; returning `null` instead of a stage now fails with
`NullPointerException`. Independent JDK dependent stages follow JDK cancellation
rules: cancelling a user-created `thenApply` stage does not cancel its source.
Cancel the original API result to cancel its request. `CommandFutures.map`,
`compose`, `link` and `all` are small adapters used by the API and available when
applications deliberately want request cancellation to propagate. `compose`
also cancels a follow-up request created concurrently with cancellation.

`ofAny` has no direct JDK equivalent: `CompletableFuture.anyOf` selects the first
completion, including failure, whereas the removed method selected the first
success. Review such call sites instead of mechanically substituting `anyOf`.
`CommandFutures.all` returns an unmodifiable list in input order, allows null
values, fails promptly on a failed input and does not cancel other inputs on an
ordinary failure. Explicit aggregate cancellation cancels its inputs.

## Callback execution and ordering

Accepted ServerQuery responses are published in receive order on one query-owned
virtual completion worker, away from the protocol reader, writer and deadline
scheduler. Admission is held through completion, including inline callbacks.
A short non-async callback registered before completion runs on this worker;
a callback registered after completion can run on the registering thread. JDK
non-async stages can also execute on a thread completing or cancelling a future.
There is no ordering guarantee among independent observers of the same future.
Immediate admission failures and already-completed no-op results settle on the
submitting thread.

Keep inline callbacks short. Blocking one can delay later result publication beyond
the request deadlines. Do not synchronously wait for another command from
an inline callback: it needs the same completion worker. Use `thenCompose` to
sequence requests, or `thenAcceptAsync`/`whenCompleteAsync` with an application
executor for blocking work. JDK async methods without an executor use the common
pool; applications own the lifetime and capacity of their supplied executor.
File-transfer work uses the query's separate virtual callback executor and retains
its admission slot until that work actually finishes, even after cancellation.

Closing the query stops its completion executor within the existing close budget.
If an inline callback blocks shutdown, queued completions are handed to the
query's callback workers so their results are not abandoned. Terminal completion
order during this forced shutdown is not guaranteed. Callbacks must cooperate
with interruption for all owned workers to terminate; the protocol response is
consumed even when an application callback is slow.

```java
CompletableFuture<ServerQueryInfo> request = query.getAsyncApi().whoAmI();
CompletionStage<Void> observed = request.thenAcceptAsync(info -> process(info), applicationExecutor);
observed.whenCompleteAsync((ignored, failure) -> {
    if (failure != null) report(CommandFutures.unwrap(failure));
}, applicationExecutor);
```

## Waiting, timeouts and cancellation

`get()` is interruptible: it throws `InterruptedException`, clears the interrupt
flag and leaves the request running. `TS3Api` remains a thin uninterruptible
adapter: it waits for the terminal future outcome, preserves/restores the interrupt
flag, rethrows TS3 causes directly and translates other completion failures to
`TS3Exception`. Cancellation remains `CancellationException`.

Timed `get` only limits the caller's wait; `TimeoutException` leaves the request
running. `orTimeout` changes that future's result to a terminal timeout;
`completeOnTimeout` changes it to a successful fallback. Neither operation aborts
transmission or undoes a mutation. They may run non-async callbacks on the JDK
timeout thread. Use `copy().orTimeout(...)` for a separate observation deadline.
The configured queue-wait and command-response deadlines still bound the underlying
request. A response deadline closes the affected connection to avoid misaligning
later responses.

Cancelling the original API result removes an unsent request and immediately
releases its admission slot before cancellation observers run. Aggregate adapters
also cancel all underlying requests before publishing cancellation. If the writer has already claimed it, cancellation
cannot retract the bytes: the request remains in the receive queue, its eventual
response is consumed, and subsequent commands stay aligned. The cancelled request
is not replayed on reconnect. `cancel(true)` and `cancel(false)` have the same
request semantics; JDK cancellation does not interrupt transport or file I/O.
Cancellation cannot undo a server-side mutation, including a file transfer already
in progress. A completion/cancellation race has one winner according to JDK rules.

The contract follows the official [Java 25 CompletableFuture documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/CompletableFuture.html)
and [Executor documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/Executors.html).
