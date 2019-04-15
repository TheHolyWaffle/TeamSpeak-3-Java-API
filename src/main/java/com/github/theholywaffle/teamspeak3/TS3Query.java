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

import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3QueryShutDownException;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.DisconnectingConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class TS3Query {

	private static final Logger log = LoggerFactory.getLogger(TS3Query.class);

	/**
	 * Artificial delay between sending commands, measured in milliseconds.
	 * <p>
	 * If the query's hostname / IP has not been added to the server's {@code query_ip_whitelist.txt},
	 * you need to use {@link FloodRate#DEFAULT} to prevent the query from being flood-banned.
	 * </p><p>
	 * Calling {@link FloodRate#custom} allows you to use a custom command delay if neither
	 * {@link FloodRate#UNLIMITED} nor {@link FloodRate#DEFAULT} fit your needs.
	 * </p>
	 */
	public static class FloodRate {

		/**
		 * Default delay of 350 milliseconds between commands for queries that are not whitelisted.
		 */
		public static final FloodRate DEFAULT = new FloodRate(350);

		/**
		 * No delay between commands. If a query uses this without being whitelisted, it will likely be flood-banned.
		 */
		public static final FloodRate UNLIMITED = new FloodRate(0);

		/**
		 * Creates a FloodRate object that represents a custom command delay.
		 *
		 * @param milliseconds
		 * 		the delay between sending commands in milliseconds
		 *
		 * @return a new {@code FloodRate} object representing a custom delay
		 */
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

	/**
	 * The protocol used to communicate with the TeamSpeak3 server.
	 */
	public enum Protocol {
		RAW, SSH
	}

	private final ConnectionHandler connectionHandler;
	private final EventManager eventManager;
	private final ExecutorService userThreadPool;
	private final FileTransferHelper fileTransferHelper;
	private final CommandQueue globalQueue;
	private final TS3Config config;

	private final AtomicBoolean connected = new AtomicBoolean(false);

	private Connection connection;

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
		this.config = config.freeze();
		this.eventManager = new EventManager(this);
		this.userThreadPool = Executors.newCachedThreadPool();
		this.fileTransferHelper = new FileTransferHelper(config.getHost());
		this.connectionHandler = config.getReconnectStrategy().create(config.getConnectionHandler());
		this.globalQueue = CommandQueue.newGlobalQueue(this, connectionHandler instanceof DisconnectingConnectionHandler);
	}

	// PUBLIC

	/**
	 * Tries to establish a connection to the TeamSpeak3 server.
	 *
	 * @throws IllegalStateException
	 * 		if this method was called from {@link ConnectionHandler#onConnect}
	 * @throws TS3ConnectionFailedException
	 * 		if the query can't connect to the server or the {@link ConnectionHandler} throws an exception
	 */
	public void connect() {
		if (Thread.holdsLock(this)) {
			// Check that connect is not called from onConnect
			throw new IllegalStateException("Cannot call connect from onConnect handler");
		}

		doConnect();
	}

	private synchronized void doConnect() {
		if (userThreadPool.isShutdown()) {
			throw new IllegalStateException("The query has already been shut down");
		}

		disconnect(); // If we're already connected

		try {
			CommandQueue queue = CommandQueue.newConnectQueue(this);
			Connection con = new Connection(this, config, queue);

			try {
				TS3Api api = queue.getApi();
				if (config.getProtocol() == Protocol.RAW && config.hasLoginCredentials()) {
					api.login(config.getUsername(), config.getPassword());
				}
				connectionHandler.onConnect(api);
			} catch (TS3QueryShutDownException e) {
				// Disconnected during onConnect, re-throw as a TS3ConnectionFailedException
				queue.failRemainingCommands();
				throw new TS3ConnectionFailedException(e);
			} catch (Exception e) {
				con.disconnect();
				queue.failRemainingCommands();
				throw new TS3ConnectionFailedException("ConnectionHandler threw exception in connect handler", e);
			}

			// Reject new commands and wait until the onConnect queue is empty
			queue.shutDown();

			connection = con;
			con.setCommandQueue(globalQueue);
			connected.set(true);
		} catch (TS3ConnectionFailedException conFailed) {
			// If this is the first connection attempt, we won't run the handleDisconnect method,
			// so we need to call shutDown from this method instead.
			if (connection == null) shutDown();
			throw conFailed;
		}
	}

	/**
	 * Disconnects the query and closes all open resources.
	 * <p>
	 * If the command queue still contains commands when this method is called,
	 * the query will first process these commands, causing this method to block.
	 * However, the query will reject any new commands as soon as this method is called.
	 * </p>
	 *
	 * @throws IllegalStateException
	 * 		if this method was called from {@link ConnectionHandler#onConnect}
	 */
	public void exit() {
		if (Thread.holdsLock(this)) {
			// Check that exit is not called from onConnect
			throw new IllegalStateException("Cannot call exit from onConnect handler");
		}

		try {
			globalQueue.quit();
		} finally {
			shutDown();
		}
	}

	private synchronized void shutDown() {
		if (userThreadPool.isShutdown()) return;

		disconnect();
		globalQueue.failRemainingCommands();
		userThreadPool.shutdown();
	}

	private synchronized void disconnect() {
		if (connection == null) return;

		connection.disconnect();
		connected.set(false);
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

	/**
	 * Gets the API object that can be used to send commands to the TS3 server.
	 *
	 * @return a {@code TS3Api} object
	 */
	public TS3Api getApi() {
		return globalQueue.getApi();
	}

	/**
	 * Gets the asynchronous API object that can be used to send commands to the TS3 server
	 * in a non-blocking manner.
	 * <p>
	 * Please only use the asynchronous API if it is really necessary and if you understand
	 * the implications of having multiple threads interact with your program.
	 * </p>
	 *
	 * @return a {@code TS3ApiAsync} object
	 */
	public TS3ApiAsync getAsyncApi() {
		return globalQueue.getAsyncApi();
	}

	// INTERNAL

	void submitUserTask(final String name, final Runnable task) {
		userThreadPool.submit(() -> {
			try {
				task.run();
			} catch (Throwable throwable) {
				log.error(name + " threw an exception", throwable);
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

		submitUserTask("ConnectionHandler disconnect task", this::handleDisconnect);
	}

	private void handleDisconnect() {
		try {
			connectionHandler.onDisconnect(this);
		} finally {
			synchronized (this) {
				if (!connected.get()) {
					shutDown();
				}
			}
		}
	}
}
