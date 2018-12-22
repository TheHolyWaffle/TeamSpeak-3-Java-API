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
import com.github.theholywaffle.teamspeak3.commands.Command;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class CommandQueue {

	private static final int INITIAL_QUEUE_SIZE = 16;

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

	private boolean rejectNew = false;
	private long firstEnqueueTimeAfterEmpty;

	static CommandQueue newGlobalQueue(TS3Query query, boolean unlimited) {
		return new CommandQueue(query, true, unlimited);
	}

	static CommandQueue newConnectQueue(TS3Query query) {
		return new CommandQueue(query, false, true);
	}

	private CommandQueue(TS3Query query, boolean global, boolean unlimited) {
		isGlobal = global;
		unlimitedInFlightCommands = unlimited;

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

	void enqueueCommand(Command command) {
		queueLock.lock();
		try {
			if (rejectNew) {
				command.getFuture().fail(new TS3QueryShutDownException());
				return;
			}

			if (isEmpty()) {
				firstEnqueueTimeAfterEmpty = System.currentTimeMillis();
			}
			sendQueue.add(command);
			canTransfer.signalAll();
		} finally {
			queueLock.unlock();
		}
	}

	Command transferCommand() throws InterruptedException {
		queueLock.lockInterruptibly();
		try {
			while (sendQueue.isEmpty() || (!receiveQueue.isEmpty() && !unlimitedInFlightCommands)) {
				if (sendQueue.isEmpty() && rejectNew) return null;
				canTransfer.await();
			}

			Command command = sendQueue.remove();
			receiveQueue.add(command);

			return command;
		} finally {
			queueLock.unlock();
		}
	}

	Command peekReceiveQueue() {
		queueLock.lock();
		try {
			return receiveQueue.peek();
		} finally {
			queueLock.unlock();
		}
	}

	void removeFromReceiveQueue() {
		queueLock.lock();
		try {
			if (receiveQueue.isEmpty()) throw new IllegalStateException("Empty receive queue");

			receiveQueue.remove();
			canTransfer.signalAll();
		} finally {
			queueLock.unlock();
		}
	}

	void resetSentCommands() {
		queueLock.lock();
		try {
			Collection<Command> allCommands = getAllCommands();

			sendQueue.clear();
			receiveQueue.clear();
			sendQueue.addAll(allCommands);

			rejectNew = false;
			firstEnqueueTimeAfterEmpty = System.currentTimeMillis();

			canTransfer.signalAll();
		} finally {
			queueLock.unlock();
		}
	}

	boolean isEmpty() {
		queueLock.lock();
		try {
			return receiveQueue.isEmpty() && sendQueue.isEmpty();
		} finally {
			queueLock.unlock();
		}
	}

	long getBusyTime() {
		queueLock.lock();
		try {
			if (isEmpty()) {
				return 0L;
			} else {
				return System.currentTimeMillis() - firstEnqueueTimeAfterEmpty;
			}
		} finally {
			queueLock.unlock();
		}
	}

	void shutDown() {
		queueLock.lock();
		try {
			rejectNew = true;
			canTransfer.signalAll();

			while (!isEmpty()) {
				canTransfer.awaitUninterruptibly();
			}
		} finally {
			queueLock.unlock();
		}
	}

	void quit() {
		queueLock.lock();
		try {
			// Enqueue the last command - don't wait for a response, we'll wait in shutDown
			if (!rejectNew) asyncApi.quit();
			// And wait until all commands have been sent
			shutDown();
		} finally {
			queueLock.unlock();
		}
	}

	void failRemainingCommands() {
		queueLock.lock();
		try {
			rejectNew = true;
			canTransfer.signalAll();

			Collection<Command> allCommands = getAllCommands();
			for (Command command : allCommands) {
				command.getFuture().fail(new TS3QueryShutDownException());
			}
		} finally {
			queueLock.unlock();
		}
	}

	// Only call this when holding queueLock
	private Collection<Command> getAllCommands() {
		Collection<Command> allCommands = new ArrayList<>(sendQueue.size() + receiveQueue.size());
		allCommands.addAll(sendQueue);
		allCommands.addAll(receiveQueue);
		return allCommands;
	}
}
