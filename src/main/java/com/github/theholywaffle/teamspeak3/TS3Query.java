package com.github.theholywaffle.teamspeak3;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2014 Bert De Geyter
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

import com.github.theholywaffle.teamspeak3.api.exception.TS3Exception;
import com.github.theholywaffle.teamspeak3.api.exception.TS3QueryShutDownException;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.commands.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class TS3Query {

	private static final Logger log = LoggerFactory.getLogger(TS3Query.class);

	public static class FloodRate {

		public static final FloodRate DEFAULT = new FloodRate(350);
		public static final FloodRate UNLIMITED = new FloodRate(0);

		public static FloodRate custom(int milliseconds) {
			if (milliseconds < 0) throw new IllegalArgumentException("Timeout must be positive");
			return new FloodRate(milliseconds);
		}

		private final int ms;

		private FloodRate(int ms) {
			this.ms = ms;
		}

		public int getMs() {
			return ms;
		}
	}

	private final ConnectionHandler connectionHandler;
	private final EventManager eventManager;
	private final ExecutorService userThreadPool;
	private final FileTransferHelper fileTransferHelper;
	private final TS3Api api;
	private final TS3ApiAsync asyncApi;
	private final TS3Config config;

	private final AtomicBoolean connected = new AtomicBoolean(false);
	private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

	private QueryIO io;

	/**
	 * Creates a TS3Query that connects to a TS3 server at
	 * {@code localhost:10011} using default settings.
	 */
	public TS3Query() {
		this(new TS3Config());
	}

	/**
	 * Creates a customized TS3Query that connects to a server
	 * specified by {@code config}.
	 *
	 * @param config
	 * 		configuration for this TS3Query
	 */
	public TS3Query(TS3Config config) {
		this.config = config;
		this.eventManager = new EventManager(this);
		this.userThreadPool = Executors.newCachedThreadPool();
		this.fileTransferHelper = new FileTransferHelper(config.getHost());
		this.connectionHandler = config.getReconnectStrategy().create(config.getConnectionHandler());

		this.asyncApi = new TS3ApiAsync(this);
		this.api = new TS3Api(asyncApi);
	}

	/*
	 * Copy constructor only used for ReconnectQuery
	 */
	private TS3Query(TS3Query query) {
		this.config = query.config;
		this.eventManager = query.eventManager;
		this.userThreadPool = query.userThreadPool;
		this.fileTransferHelper = query.fileTransferHelper;
		this.connectionHandler = null;

		this.asyncApi = new TS3ApiAsync(this);
		this.api = new TS3Api(asyncApi);
	}

	// PUBLIC

	public void connect() {
		synchronized (this) {
			if (userThreadPool.isShutdown()) {
				throw new IllegalStateException("The query has already been shut down");
			}
		}

		QueryIO newIO = new QueryIO(this, config);

		try {
			connectionHandler.onConnect(new ReconnectQuery(this, newIO));
		} catch (Exception e) {
			log.error("ConnectionHandler threw exception in connect handler", e);
		}

		synchronized (this) {
			QueryIO oldIO = io;
			io = newIO;
			if (oldIO != null) {
				oldIO.disconnect();
				newIO.continueFrom(io);
			}
			connected.set(true);
		}
	}

	/**
	 * Removes and closes all used resources to the TeamSpeak server.
	 */
	public void exit() {
		if (shuttingDown.compareAndSet(false, true)) {
			try {
				// Sending this command will guarantee that all previously sent commands have been processed
				api.quit();
			} catch (TS3Exception e) {
				log.warn("Could not send a quit command to terminate the connection", e);
			} finally {
				shutDown();
			}
		} else {
			// Only 1 thread shall ever send quit, the rest of the threads wait for the first thread to finish
			try {
				synchronized (this) {
					while (connected.get()) {
						wait();
					}
				}
			} catch (InterruptedException e) {
				// Restore interrupt, then bail out
				Thread.currentThread().interrupt();
			}
		}
	}

	private synchronized void shutDown() {
		if (userThreadPool.isShutdown()) return;

		if (io != null) {
			io.disconnect();
			io.failRemainingCommands();
		}

		userThreadPool.shutdown();
		connected.set(false);
		notifyAll();
	}

	/**
	 * Returns {@code true} if the query is likely connected,
	 * {@code false} if the query is disconnected or currently trying to reconnect.
	 * <p>
	 * Note that the only way to really determine whether the query is connected or not
	 * is to send a command and check whether it succeeds.
	 * Thus this method could return {@code true} almost a minute after the connection
	 * has been lost, when the last keep-alive command was sent.
	 * </p><p>
	 * Please do not use this method to write your own connection handler.
	 * Instead, use the built-in classes in the {@code api.reconnect} package.
	 * </p>
	 *
	 * @return whether the query is connected or not
	 *
	 * @see TS3Config#setReconnectStrategy(ReconnectStrategy)
	 * @see TS3Config#setConnectionHandler(ConnectionHandler)
	 */
	public boolean isConnected() {
		return connected.get();
	}

	public TS3Api getApi() {
		return api;
	}

	public TS3ApiAsync getAsyncApi() {
		return asyncApi;
	}

	// INTERNAL

	synchronized void doCommandAsync(Command c) {
		if (userThreadPool.isShutdown()) {
			c.getFuture().fail(new TS3QueryShutDownException());
			return;
		}

		io.enqueueCommand(c);
	}

	void submitUserTask(final String name, final Runnable task) {
		userThreadPool.submit(new Runnable() {
			@Override
			public void run() {
				try {
					task.run();
				} catch (Throwable throwable) {
					log.error(name + " threw an exception", throwable);
				}
			}
		});
	}

	EventManager getEventManager() {
		return eventManager;
	}

	FileTransferHelper getFileTransferHelper() {
		return fileTransferHelper;
	}

	void fireDisconnect() {
		connected.set(false);

		submitUserTask("ConnectionHandler disconnect task", new Runnable() {
			@Override
			public void run() {
				handleDisconnect();
			}
		});
	}

	private synchronized void handleDisconnect() {
		try {
			connectionHandler.onDisconnect(this);
		} finally {
			if (!connected.get()) {
				shuttingDown.set(true); // Try to prevent extraneous exit commands
				shutDown();
			}
		}
	}

	private static class ReconnectQuery extends TS3Query {

		private final TS3Query parent;

		private ReconnectQuery(TS3Query query, QueryIO io) {
			super(query);
			super.io = io;
			this.parent = query;
		}

		@Override
		public void connect() {
			throw new UnsupportedOperationException("Can't call connect from onConnect");
		}

		@Override
		public void exit() {
			parent.exit();
		}
	}
}
