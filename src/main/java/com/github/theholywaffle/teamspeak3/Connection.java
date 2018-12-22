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

import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class Connection {

	private final TS3Query ts3Query;
	private final Socket socket;
	private final SocketReader socketReader;
	private final SocketWriter socketWriter;
	private final KeepAlive keepAlive;

	private final AtomicReference<CommandQueue> commandQueue;
	private final AtomicLong lastCommandSent;
	private final long commandTimeout;

	Connection(TS3Query query, TS3Config config, CommandQueue initialQueue) {
		ts3Query = query;
		commandQueue = new AtomicReference<>(initialQueue);
		lastCommandSent = new AtomicLong(System.currentTimeMillis());
		commandTimeout = config.getCommandTimeout();

		try {
			socket = new Socket(config.getHost(), config.getQueryPort());
			socket.setTcpNoDelay(true);
			socket.setSoTimeout(config.getCommandTimeout());

			socketReader = new SocketReader(this, socket, query, config);
			socketWriter = new SocketWriter(this, socket, config);
			keepAlive = new KeepAlive(this);
		} catch (IOException ioe) {
			closeSocket();
			throw new TS3ConnectionFailedException(ioe);
		}

		socketReader.start();
		socketWriter.start();
		keepAlive.start();
	}

	void internalDisconnect() {
		disconnect();

		CommandQueue queue = getCommandQueue();
		if (queue.isGlobal()) {
			ts3Query.fireDisconnect();
		} else {
			queue.failRemainingCommands();
		}
	}

	void disconnect() {
		keepAlive.interrupt();
		socketWriter.interrupt();
		socketReader.interrupt();

		boolean wasInterrupted = joinThread(keepAlive);
		wasInterrupted |= joinThread(socketWriter);
		wasInterrupted |= joinThread(socketReader);

		if (wasInterrupted) {
			// Restore the interrupt for the caller
			Thread.currentThread().interrupt();
		}

		closeSocket();
	}

	private static boolean joinThread(Thread thread) {
		try {
			thread.join();
			return false;
		} catch (InterruptedException e) {
			return true;
		}
	}

	private void closeSocket() {
		if (socket == null) return;
		try {
			socket.close();
		} catch (IOException ignored) {
		}
	}

	CommandQueue getCommandQueue() {
		return commandQueue.get();
	}

	void setCommandQueue(CommandQueue newQueue) {
		newQueue.resetSentCommands();
		CommandQueue oldQueue = commandQueue.getAndSet(newQueue);

		if (!oldQueue.isEmpty()) {
			// shutDown was not called on the old queue, but that's
			// a programming error that we can't recover from here
			throw new IllegalStateException("Old queue not empty");
		}
	}

	long getIdleTime() {
		return System.currentTimeMillis() - lastCommandSent.get();
	}

	void resetIdleTime() {
		lastCommandSent.set(System.currentTimeMillis());
	}

	boolean isTimedOut() {
		/*
		 * Rationale: The connection has only timed out if we haven't sent a command for some time
		 * and the command queue has had pending commands for at least that amount of time.
		 * CommandQueue#getBusyTime is reset when the command queue is switched, so even if a user
		 * blocks the onConnect handler for too long without sending a command, this won't return true
		 * when the queue is switched over in #setCommandTime.
		 */

		return getIdleTime() > commandTimeout && getCommandQueue().getBusyTime() > commandTimeout;
	}
}
