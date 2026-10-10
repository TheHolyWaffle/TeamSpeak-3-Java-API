package com.github.theholywaffle.teamspeak3;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2018 Bert De Geyter, Roger Baumgartner
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import com.github.theholywaffle.teamspeak3.api.exception.TS3QueryShutDownException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3Exception;
import com.github.theholywaffle.teamspeak3.api.exception.TS3QueueFullException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3UnknownOutcomeException;
import com.github.theholywaffle.teamspeak3.commands.Command;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class CommandQueue {

	private static final int INITIAL_QUEUE_SIZE = 16;
	private static final CompletionStage<?> NO_ASYNC_WORK = CompletableFuture.completedStage(null);

	private final Queue<Command> sendQueue;
	private final Queue<Command> receiveQueue;
	private final Lock queueLock;
	// Signalled when a command is added to sendQueue or removed from receiveQueue, or when rejectNew is set to true
	private final Condition canTransfer;

	// API objects that insert commands into this queue
	private final TS3Api api;
	private final TS3ApiAsync asyncApi;

	private final boolean unlimitedInFlightCommands;
	private final boolean isGlobal;

	private final Semaphore admission;

	private boolean rejectNew = false;
	private boolean paused;
	private boolean contextSealed;
	private static final Set<String> SESSION_COMMANDS = Set.of(
		"login", "logout", "use", "clientupdate", "servernotifyregister", "servernotifyunregister");
	private final Map<Command, Integer> retries = new IdentityHashMap<>();
	private final TS3Query query;
	private final Map<Command, CompletionStage<?>> completionWork = new IdentityHashMap<>();
	private final Map<Command, Long> enqueued = new IdentityHashMap<>();
	private final Map<Command, Long> sent = new IdentityHashMap<>();
	private final Map<Command, Connection> owners = new IdentityHashMap<>();

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

		sendQueue = new ArrayDeque<>(INITIAL_QUEUE_SIZE);
		receiveQueue = new ArrayDeque<>(unlimited ? INITIAL_QUEUE_SIZE : 1);
		queueLock = new ReentrantLock();
		canTransfer = queueLock.newCondition();

		asyncApi = new TS3ApiAsync(query, this);
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

	void enqueueCommand(Command command) { enqueueCommand(command, NO_ASYNC_WORK); }

	// The work stage settles only after asynchronous library work and its inline callbacks return.
	void enqueueCommand(Command command, CompletionStage<?> work) {
		queueLock.lock();
		try {
			if (rejectNew) {
				command.getFuture().completeExceptionally(new TS3QueryShutDownException());
				return;
			}

			if (paused && query.getConfig().getSessionConfiguration() == null) {
				command.getFuture().completeExceptionally(new TS3Exception("Disconnected query has no restorable session"));
				return;
			}
			if (contextSealed && SESSION_COMMANDS.contains(command.getName())) {
				command.getFuture().completeExceptionally(new TS3Exception("Session changes require SessionConfiguration when reconnect is enabled"));
				return;
			}
			if (!admission.tryAcquire()) {
				command.getFuture().completeExceptionally(new TS3QueueFullException());
				return;
			}
			enqueued.put(command, System.nanoTime());
			completionWork.put(command, work);
			sendQueue.add(command);
			command.getFuture().whenComplete((value, failure) -> {
				if (command.getFuture().isCancelled()) removeUnsent(command);
			});
			canTransfer.signalAll();
		} finally {
			queueLock.unlock();
		}
	}

	private void removeUnsent(Command command) {
		queueLock.lock();
		try {
			if (sendQueue.remove(command)) {
				enqueued.remove(command); retries.remove(command); completionWork.remove(command); admission.release();
				canTransfer.signalAll();
			}
		} finally { queueLock.unlock(); }
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
				Command command = sendQueue.remove();
				if (command.getFuture().isCancelled()) { enqueued.remove(command); retries.remove(command); completionWork.remove(command); admission.release(); continue; }
				if (System.nanoTime() - enqueued.get(command) >= query.getConfig().getQueueWaitTimeout().toNanos()) {
					fail(command, new TS3Exception("Command queue wait deadline exceeded"));
					continue;
				}
				// Conservative boundary: any failure from encoding through flush has an unknown outcome.
				receiveQueue.add(command);
				sent.put(command, System.nanoTime());
				owners.put(command, owner);
				return command;
			}
		} finally { queueLock.unlock(); }
	}

	Command peekReceiveQueue() {
		queueLock.lock();
		try {
			return receiveQueue.peek();
		} finally {
			queueLock.unlock();
		}
	}

	void completeResponse(Command command, Connection owner, Runnable completion) {
		queueLock.lock();
		try {
			if (receiveQueue.peek() != command || owners.get(command) != owner) return;
			receiveQueue.remove(); sent.remove(command); owners.remove(command); enqueued.remove(command); retries.remove(command);
			complete(command, completion);
			canTransfer.signalAll();
		} finally { queueLock.unlock(); }
	}

	void sealSession() {
		queueLock.lock();
		try { contextSealed = true; } finally { queueLock.unlock(); }
	}

	void resume() {
		queueLock.lock();
		try { paused = false; canTransfer.signalAll(); } finally { queueLock.unlock(); }
	}

	void prepareReconnect() {
		queueLock.lock();
		try {
			paused = true;
			boolean restore = query.isReconnectEnabled() && query.getConfig().getSessionConfiguration() != null;
			var policy = query.getConfig().getCommandRetryPolicy();
			Queue<Command> recovered = new ArrayDeque<>();
			for (Command command : receiveQueue) {
				int count = retries.getOrDefault(command, 0);
				if (restore && policy.allows(command.getName()) && count < policy.getMaxRetries()
					&& !command.getFuture().isCancelled()) {
					retries.put(command, count + 1);
					enqueued.put(command, System.nanoTime());
					recovered.add(command);
				} else {
					fail(command, new TS3UnknownOutcomeException(command.getName()));
				}
			}
			for (Command command : sendQueue) {
				if (restore && !command.getFuture().isCancelled()) recovered.add(command);
				else fail(command, new TS3QueryShutDownException());
			}
			sendQueue.clear(); sendQueue.addAll(recovered); receiveQueue.clear(); sent.clear(); owners.clear();
			canTransfer.signalAll();
		} finally { queueLock.unlock(); }
	}

	private void fail(Command command, TS3Exception failure) {
		enqueued.remove(command); retries.remove(command);
		complete(command, () -> command.getFuture().completeExceptionally(failure));
	}

	private void complete(Command command, Runnable completion) {
		CompletionStage<?> work = completionWork.remove(command);
		query.submitCompletion(() -> {
			try { completion.run(); }
			finally { work.whenComplete((value, failure) -> admission.release()); }
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
			long now = System.nanoTime();
			long timeout = query.getConfig().getCommandResponseTimeout().toNanos();
			for (long start : sent.values()) {
				if (now - start >= timeout) return true;
			}
			return false;
		} finally { queueLock.unlock(); }
	}

	void expireWaitingCommands() {
		queueLock.lock();
		try {
			if (sendQueue.isEmpty()) return;
			boolean changed = false;
			long now = System.nanoTime();
			long timeout = query.getConfig().getQueueWaitTimeout().toNanos();
			var iterator = sendQueue.iterator();
			while (iterator.hasNext()) {
				Command command = iterator.next();
				if (now - enqueued.get(command) >= timeout) {
					iterator.remove();
					enqueued.remove(command); retries.remove(command);
					complete(command, () ->
						command.getFuture().completeExceptionally(new TS3Exception("Command queue wait deadline exceeded")));
					changed = true;
				}
			}
			if (changed) canTransfer.signalAll();
		} finally { queueLock.unlock(); }
	}

	void shutDown(Deadline deadline) {
		queueLock.lock();
		try {
			rejectNew = true;
			canTransfer.signalAll();
			while (!isEmpty() && !deadline.expired()) {
				try { canTransfer.awaitNanos(deadline.remaining()); }
				catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
			}
		} finally { queueLock.unlock(); }
	}

	void quit(Deadline deadline) {
		queueLock.lock();
		try {
			if (!rejectNew) asyncApi.quit();
		} finally { queueLock.unlock(); }
		shutDown(deadline);
	}

	void failRemainingCommands() {
		Collection<Command> allCommands;
		queueLock.lock();
		try {
			rejectNew = true;
			allCommands = getAllCommands();
			for (Command command : allCommands) fail(command, sent.containsKey(command)
				? new TS3UnknownOutcomeException(command.getName()) : new TS3QueryShutDownException());
			sendQueue.clear(); receiveQueue.clear(); enqueued.clear(); sent.clear(); owners.clear(); retries.clear();
			canTransfer.signalAll();
		} finally { queueLock.unlock(); }
	}

	// Only call this when holding queueLock
	private Collection<Command> getAllCommands() {
		Collection<Command> allCommands = new ArrayList<>(sendQueue.size() + receiveQueue.size());
		allCommands.addAll(sendQueue);
		allCommands.addAll(receiveQueue);
		return allCommands;
	}
}
