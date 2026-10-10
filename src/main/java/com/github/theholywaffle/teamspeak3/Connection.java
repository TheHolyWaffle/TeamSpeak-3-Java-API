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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class Connection {
	private final TS3Query query;
	private final TS3Config config;
	private final IOChannel channel;
	private final AtomicReference<CommandQueue> queue;
	private final AtomicBoolean stopped = new AtomicBoolean();
	private final AtomicLong lastSent = new AtomicLong(System.nanoTime());
	private volatile Deadline initialization;
	private volatile StreamReader reader;
	private volatile StreamWriter writer;
	private volatile KeepAlive keepAlive;

	Connection(TS3Query query, TS3Config config, CommandQueue initialQueue) {
		this.query = query;
		this.config = config;
		queue = new AtomicReference<>(initialQueue);
		channel = config.getProtocol() == TS3Query.Protocol.SSH ? new SSHChannel(config) : new SocketChannel(config);
		initialization = new Deadline(config.getConnectTimeout());
	}

	void open() throws IOException {
		channel.connect(this);
		synchronized (this) {
			if (stopped.get()) throw new IOException("Connection closed during initialization");
			reader = new StreamReader(this, channel.getInputStream(), query, config);
			writer = new StreamWriter(this, channel.getOutputStream(), config);
			keepAlive = new KeepAlive(this);
			reader.start(); writer.start(); keepAlive.start();
		}
	}

	void transportConnected() throws IOException {
		if (stopped.get()) throw new IOException("Connection closed during connect");
		initialization = new Deadline(config.getHandshakeTimeout());
	}

	void initialized() { initialization = null; }
	boolean isStopped() { return stopped.get(); }

	void checkDeadlines() {
		Deadline init = initialization;
		if (!stopped.get() && ((init != null && init.expired()) || queue.get().responseExpired())) {
			internalDisconnect();
		}
		queue.get().expireWaitingCommands();
	}

	void internalDisconnect() {
		if (!stop(new Deadline(config.getCloseTimeout()))) return;
		query.fireDisconnect(this);
	}

	void disconnect() { disconnect(new Deadline(config.getCloseTimeout())); }
	void disconnect(Deadline deadline) {
		if (!stop(deadline)) {
			queue.get().failRemainingCommands();
			deadline.join(reader); deadline.join(writer); deadline.join(keepAlive);
		}
	}

	private boolean stop(Deadline deadline) {
		if (!stopped.compareAndSet(false, true)) return false;
		try { channel.close(); } catch (IOException ignored) { }
		Thread[] threads;
		synchronized (this) { threads = new Thread[] {reader, writer, keepAlive}; }
		for (Thread thread : threads) if (thread != null) thread.interrupt();
		queue.get().failRemainingCommands();
		for (Thread thread : threads) deadline.join(thread);
		return true;
	}

	boolean threadsTerminated() {
		return (reader == null || !reader.isAlive()) && (writer == null || !writer.isAlive())
			&& (keepAlive == null || !keepAlive.isAlive());
	}

	CommandQueue getCommandQueue() { return queue.get(); }
	void setCommandQueue(CommandQueue newQueue) {
		synchronized (this) {
			if (stopped.get()) throw new TS3ConnectionFailedException("Connection terminated during initialization");
			newQueue.resetSentCommands();
			if (!queue.get().isEmpty()) throw new IllegalStateException("Old queue not empty");
			queue.set(newQueue);
		}
	}
	long getIdleTime() { return (System.nanoTime() - lastSent.get()) / 1_000_000L; }
	void resetIdleTime() { lastSent.set(System.nanoTime()); }
	boolean isTimedOut() { return queue.get().responseExpired(); }
}
