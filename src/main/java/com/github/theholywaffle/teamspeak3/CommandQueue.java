package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.exception.TS3Exception;
import com.github.theholywaffle.teamspeak3.api.exception.TS3QueryShutDownException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3QueueFullException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3TimeoutException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3UnknownOutcomeException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3UnsupportedOperationException;
import com.github.theholywaffle.teamspeak3.commands.Command;
import com.github.theholywaffle.teamspeak3.commands.CommandTrait;
import com.github.theholywaffle.teamspeak3.commands.QueryCommands;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class CommandQueue {

    /** Bookkeeping for one admitted command, guarded by queueLock. */
    private static final class Entry {
        private final Command command;
        // Settles only after asynchronous library work and its inline callbacks return.
        private final CompletionStage<?> work;
        private long enqueuedAt = System.nanoTime();
        private long sentAt;
        private Connection owner;
        private int retries;

        private Entry(Command command, CompletionStage<?> work) {
            this.command = command;
            this.work = work;
        }

        private boolean isCancelled() {
            return command.getFuture().isCancelled();
        }
    }

    // Unsent commands, then commands written and awaiting their response, both in protocol order
    private final Queue<Entry> sendQueue = new ArrayDeque<>();
    private final Queue<Entry> receiveQueue = new ArrayDeque<>();
    private final Lock queueLock = new ReentrantLock();
    // Signalled when a command is added to sendQueue or removed from receiveQueue, or when rejectNew is set to true
    private final Condition canTransfer = queueLock.newCondition();

    // API objects that insert commands into this queue
    private final TS3Api api;
    private final TS3ApiAsync asyncApi;

    private final boolean unlimitedInFlightCommands;
    private final boolean isGlobal;

    private final Semaphore admission;
    private final TS3Query query;

    private boolean rejectNew = false;
    private boolean paused;
    private boolean contextSealed;

    static CommandQueue newGlobalQueue(TS3Query query, boolean unlimited) {
        return new CommandQueue(query, true, unlimited);
    }

    static CommandQueue newConnectQueue(TS3Query query) {
        return new CommandQueue(query, false, true);
    }

    private CommandQueue(TS3Query query, boolean global, boolean unlimited) {
        this.query = query;
        admission = query.commandAdmission(global);
        isGlobal = global;
        unlimitedInFlightCommands = unlimited;
        contextSealed = global && query.getConfig().getSessionConfiguration() != null && !unlimited;

        asyncApi = new TS3ApiAsync(new ServerQueryBackend(query, this));
        api = new TS3Api(asyncApi);
    }

    TS3Api getApi() {
        return api;
    }

    TS3ApiAsync getAsyncApi() {
        return asyncApi;
    }

    boolean isGlobal() {
        return isGlobal;
    }

    void enqueueCommand(Command command, CompletionStage<?> work) {
        enqueue(command, work, false);
    }

    /** @param admitted whether the caller already holds the command's admission permit */
    private void enqueue(Command command, CompletionStage<?> work, boolean admitted) {
        queueLock.lock();
        try {
            TS3Exception rejection = null;
            if (rejectNew) {
                rejection = new TS3QueryShutDownException();
            } else if (paused && query.getConfig().getSessionConfiguration() == null) {
                rejection = new TS3QueryShutDownException("Disconnected query has no restorable session");
            } else if (contextSealed && command.getTraits().contains(CommandTrait.SESSION_STATE)) {
                rejection = new TS3UnsupportedOperationException(
                        "Session changes require SessionConfiguration when reconnect is enabled");
            } else if (!admitted && !admission.tryAcquire()) {
                rejection = new TS3QueueFullException();
            }
            if (rejection != null) {
                if (admitted) admission.release();
                command.getFuture().completeExceptionally(rejection);
                return;
            }
            var entry = new Entry(command, work);
            sendQueue.add(entry);
            var _ = command.getFuture().whenComplete((_, _) -> {
                if (entry.isCancelled()) removeUnsent(entry);
            });
            canTransfer.signalAll();
        } finally {
            queueLock.unlock();
        }
    }

    private void removeUnsent(Entry entry) {
        queueLock.lock();
        try {
            if (sendQueue.remove(entry)) {
                admission.release();
                canTransfer.signalAll();
            }
        } finally {
            queueLock.unlock();
        }
    }

    Command transferCommand(Connection owner) throws InterruptedException {
        queueLock.lockInterruptibly();
        try {
            while (true) {
                if (owner.isStopped()) return null;
                while (paused || sendQueue.isEmpty() || (!receiveQueue.isEmpty() && !unlimitedInFlightCommands)) {
                    if (owner.isStopped() || (sendQueue.isEmpty() && rejectNew)) return null;
                    canTransfer.await();
                }
                if (owner.isStopped()) return null;
                Entry entry = sendQueue.remove();
                if (entry.isCancelled()) {
                    admission.release();
                    continue;
                }
                if (failIfWaitExpired(entry, System.nanoTime())) continue;
                // Conservative boundary: any failure from encoding through flush has an unknown outcome.
                entry.sentAt = System.nanoTime();
                entry.owner = owner;
                receiveQueue.add(entry);
                return entry.command;
            }
        } finally {
            queueLock.unlock();
        }
    }

    Command peekReceiveQueue() {
        queueLock.lock();
        try {
            Entry head = receiveQueue.peek();
            return head == null ? null : head.command;
        } finally {
            queueLock.unlock();
        }
    }

    // Responses belong to this exact command and connection instance.
    @SuppressWarnings("ReferenceEquality")
    void completeResponse(Command command, Connection owner, Runnable completion) {
        queueLock.lock();
        try {
            Entry head = receiveQueue.peek();
            if (head == null || head.command != command || head.owner != owner) return;
            receiveQueue.remove();
            complete(head, completion);
            canTransfer.signalAll();
        } finally {
            queueLock.unlock();
        }
    }

    void sealSession() {
        queueLock.lock();
        try {
            contextSealed = true;
        } finally {
            queueLock.unlock();
        }
    }

    void resume() {
        queueLock.lock();
        try {
            paused = false;
            canTransfer.signalAll();
        } finally {
            queueLock.unlock();
        }
    }

    void prepareReconnect() {
        queueLock.lock();
        try {
            paused = true;
            boolean restore = query.isReconnectEnabled() && query.getConfig().getSessionConfiguration() != null;
            var policy = query.getConfig().getCommandRetryPolicy();
            Queue<Entry> recovered = new ArrayDeque<>();
            for (Entry entry : receiveQueue) {
                if (restore
                        && policy.allows(entry.command.getName())
                        && entry.retries < policy.getMaxRetries()
                        && !entry.isCancelled()) {
                    entry.retries++;
                    entry.enqueuedAt = System.nanoTime();
                    entry.owner = null;
                    recovered.add(entry);
                } else {
                    fail(entry, unknownOutcome(entry));
                }
            }
            for (Entry entry : sendQueue) {
                if (restore && !entry.isCancelled()) recovered.add(entry);
                else fail(entry, new TS3QueryShutDownException());
            }
            sendQueue.clear();
            sendQueue.addAll(recovered);
            receiveQueue.clear();
            canTransfer.signalAll();
        } finally {
            queueLock.unlock();
        }
    }

    private void fail(Entry entry, TS3Exception failure) {
        complete(entry, () -> entry.command.getFuture().completeExceptionally(failure));
    }

    private void complete(Entry entry, Runnable completion) {
        query.submitCompletion(() -> {
            try {
                completion.run();
            } finally {
                entry.work.whenComplete((_, _) -> admission.release());
            }
        });
    }

    boolean isEmpty() {
        queueLock.lock();
        try {
            return receiveQueue.isEmpty() && sendQueue.isEmpty();
        } finally {
            queueLock.unlock();
        }
    }

    boolean responseExpired() {
        queueLock.lock();
        try {
            // Commands enter receiveQueue in send order, so the head was sent first.
            Entry oldest = receiveQueue.peek();
            return oldest != null && responseExpired(oldest.sentAt, System.nanoTime());
        } finally {
            queueLock.unlock();
        }
    }

    private boolean responseExpired(long sentAt, long now) {
        return now - sentAt >= query.getConfig().getCommandResponseTimeout().toNanos();
    }

    // Only call this when holding queueLock, for a command in receiveQueue
    private TS3Exception unknownOutcome(Entry entry) {
        return responseExpired(entry.sentAt, System.nanoTime())
                ? new TS3TimeoutException(TS3TimeoutException.Phase.RESPONSE, entry.command.getName())
                : new TS3UnknownOutcomeException(entry.command.getName());
    }

    void expireWaitingCommands() {
        queueLock.lock();
        try {
            if (sendQueue.isEmpty()) return;
            long now = System.nanoTime();
            if (sendQueue.removeIf(entry -> failIfWaitExpired(entry, now))) canTransfer.signalAll();
        } finally {
            queueLock.unlock();
        }
    }

    // Only call this when holding queueLock, for a command leaving sendQueue
    private boolean failIfWaitExpired(Entry entry, long now) {
        if (now - entry.enqueuedAt < query.getConfig().getQueueWaitTimeout().toNanos()) return false;
        fail(entry, new TS3TimeoutException(TS3TimeoutException.Phase.QUEUE_WAIT, entry.command.getName()));
        return true;
    }

    void shutDown(Deadline deadline) {
        queueLock.lock();
        try {
            rejectNew = true;
            canTransfer.signalAll();
            while (!isEmpty() && !deadline.expired()) {
                try {
                    canTransfer.awaitNanos(deadline.remaining());
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            queueLock.unlock();
        }
    }

    void quit(Deadline deadline) {
        // A completed command keeps its permit until its completion callbacks return, so wait for one.
        boolean admitted = false;
        try {
            admitted = admission.tryAcquire(deadline.remaining(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
        // Best effort: shutDown settles every remaining command.
        if (admitted) enqueue(QueryCommands.quit(), CompletableFuture.completedStage(null), true);
        shutDown(deadline);
    }

    void failRemainingCommands() {
        queueLock.lock();
        try {
            rejectNew = true;
            for (Entry entry : sendQueue) fail(entry, new TS3QueryShutDownException());
            for (Entry entry : receiveQueue) fail(entry, unknownOutcome(entry));
            sendQueue.clear();
            receiveQueue.clear();
            canTransfer.signalAll();
        } finally {
            queueLock.unlock();
        }
    }
}
