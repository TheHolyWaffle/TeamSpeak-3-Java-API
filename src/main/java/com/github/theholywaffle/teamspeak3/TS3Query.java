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
import com.github.theholywaffle.teamspeak3.api.event.TS3Listener;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ServerQuery client owning its transport, I/O workers, deadline scheduler and callback executors.
 * Event callbacks use a fixed worker pool with bounded queues; command completions use virtual
 * threads bounded by command admission and run separately from event callbacks.
 * Closing interrupts callback tasks; application callbacks must cooperate with interruption.
 */
public class TS3Query implements AutoCloseable {

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
	private final Semaphore commandAdmission;
	private final Semaphore startupAdmission;

	/** Observable lifecycle. CLOSED is terminal; DISCONNECTED may reconnect. */
	public enum State { NEW, CONNECTING, CONNECTED, DISCONNECTED, CLOSING, CLOSED }
	private final AtomicReference<State> state = new AtomicReference<>(State.NEW);
	private final ScheduledExecutorService deadlines;
	private final ThreadLocal<Boolean> userTask = ThreadLocal.withInitial(() -> false);
	private final ThreadLocal<Boolean> initializing = ThreadLocal.withInitial(() -> false);
	private volatile Connection connection;
	private volatile Future<?> initializationTask;

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
		this.commandAdmission = new Semaphore(config.getCommandCapacity());
		this.startupAdmission = new Semaphore(config.getCommandCapacity());
		this.eventManager = new EventManager(this);
		this.userThreadPool = Executors.newThreadPerTaskExecutor(
			Thread.ofVirtual().name("[TeamSpeak-3-Java-API] Callback-", 0).factory());
		this.deadlines = Executors.newSingleThreadScheduledExecutor(
			Thread.ofPlatform().name("[TeamSpeak-3-Java-API] Deadlines").factory());
		this.fileTransferHelper = new FileTransferHelper(config.getHost());
		this.connectionHandler = config.getReconnectStrategy().create(config.getConnectionHandler());
		this.globalQueue = CommandQueue.newGlobalQueue(this, !isReconnectEnabled());
		deadlines.scheduleWithFixedDelay(() -> {
			try {
				globalQueue.expireWaitingCommands();
				Connection con = connection;
				if (con != null) con.checkDeadlines();
			} catch (RuntimeException e) { log.error("Deadline check failed", e); }
		}, 1, 10, TimeUnit.MILLISECONDS);
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
		if (initializing.get()) throw new IllegalStateException("Cannot call connect from onConnect handler");
		Connection con;
		Future<?> task;
		synchronized (this) {
			State current = state.get();
			if (current == State.CLOSING || current == State.CLOSED) {
				throw new IllegalStateException("The query has already been shut down");
			}
			if (current == State.CONNECTING || current == State.CONNECTED) {
				throw new IllegalStateException("The query is already connecting or connected");
			}
			state.set(State.CONNECTING);
			CommandQueue queue = CommandQueue.newConnectQueue(this);
			try { con = new Connection(this, config, queue); }
			catch (RuntimeException e) {
				close();
				throw new TS3ConnectionFailedException("Could not create connection resources", e);
			}
			connection = con; // Publish before opening any blocking I/O.
			task = userThreadPool.submit(() -> {
				userTask.set(true);
				initializing.set(true);
				try {
					con.open();
					TS3Api api = queue.getApi();
					if (config.getProtocol() == Protocol.RAW && config.hasLoginCredentials()) {
						api.login(config.getUsername(), config.getPassword());
					}
					if (config.getSessionConfiguration() != null) {
						config.getSessionConfiguration().restore(api);
						if (isReconnectEnabled()) queue.sealSession();
					}
					connectionHandler.onConnect(api);
					queue.shutDown(new Deadline(config.getHandshakeTimeout()));
					synchronized (TS3Query.this) {
						if (state.get() != State.CONNECTING || con.isStopped()) {
							throw new TS3ConnectionFailedException("Connection closed during initialization");
						}
						con.setCommandQueue(globalQueue);
						con.initialized();
						state.set(State.CONNECTED);
					}
				} catch (Exception e) {
					con.disconnect();
					throw new TS3ConnectionFailedException("Connection initialization failed", e);
				} finally { initializing.remove(); userTask.remove(); }
			});
			initializationTask = task;
		}
		try {
			// Independent phase watchdogs enforce connect and handshake separately.
			long budget = Math.addExact(config.getConnectTimeout().toNanos(), config.getHandshakeTimeout().toNanos());
			task.get(budget, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			close();
			throw new TS3ConnectionFailedException("Connect interrupted", e);
		} catch (ExecutionException | CancellationException | TimeoutException e) {
			con.disconnect();
			task.cancel(true);
			if (!isReconnectEnabled()) close();
			else state.compareAndSet(State.CONNECTING, State.DISCONNECTED);
			throw new TS3ConnectionFailedException("Could not initialize connection", e);
		}
	}

	/**
	 * Attempts to drain commands and send quit for at most half the close budget,
	 * then closes the transport and terminates owned workers within the remaining budget.
	 */
	public void exit() { terminate(true); }

	/**
	 * Immediately aborts blocking I/O and fails outstanding commands. Idempotent,
	 * including before connect. User callbacks must cooperate with interruption.
	 */
	@Override
	public void close() { terminate(false); }

	private void terminate(boolean drain) {
		Deadline deadline = new Deadline(config.getCloseTimeout());
		Connection con;
		synchronized (this) {
			if (state.get() == State.CLOSED || state.get() == State.CLOSING) return;
			state.set(State.CLOSING);
			con = connection;
		}
		if (drain && con != null && !con.isStopped() && !initializing.get()) {
			globalQueue.quit(new Deadline(config.getCloseTimeout().dividedBy(2)));
		}
		if (con != null) con.disconnect(deadline);
		Future<?> task = initializationTask;
		if (task != null && !task.isDone()) task.cancel(true);
		eventManager.close();
		globalQueue.failRemainingCommands();
		deadlines.shutdownNow();
		userThreadPool.shutdown();
		// Never await our own callback executor from one of its workers.
		if (!userTask.get()) {
			try {
				userThreadPool.awaitTermination(deadline.remaining() / 2, TimeUnit.NANOSECONDS);
			} catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		}
		userThreadPool.shutdownNow();
		if (!userTask.get()) {
			try { userThreadPool.awaitTermination(deadline.remaining(), TimeUnit.NANOSECONDS); }
			catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		}
		if (!userTask.get()) eventManager.awaitTermination(deadline);
		state.set(State.CLOSED);
	}

	/** @return the current lifecycle state */
	public State getState() { return state.get(); }

	TS3Config getConfig() { return config; }
	Semaphore commandAdmission(boolean global) { return global ? commandAdmission : startupAdmission; }
	boolean isReconnectEnabled() { return config.getReconnectStrategy().isReconnectEnabled(); }

	boolean resourcesTerminated() {
		Connection con = connection;
		return eventManager.isTerminated() && userThreadPool.isTerminated() && deadlines.isTerminated()
			&& (con == null || con.threadsTerminated());
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
		return state.get() == State.CONNECTED;
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

	/**
	 * Registers one local event listener with an idempotent close handle.
	 * @param listener listener receiving ordered callbacks on the owned event executor
	 * @return local subscription; server event registration remains explicit
	 */
	public EventSubscription subscribe(TS3Listener listener) {
		return eventManager.subscribe(listener);
	}

	/** @return observable event overflow, unknown/malformed input and callback failure counters */
	public EventStatistics getEventStatistics() { return eventManager.statistics(); }

	// INTERNAL

	void runUserTask(Runnable task) {
		userTask.set(true);
		try { task.run(); } finally { userTask.remove(); }
	}

	void submitUserTask(final String name, final Runnable task) {
		try {
			userThreadPool.submit(() -> {
				try { runUserTask(task); }
				catch (Throwable e) { log.error(name + " threw an exception", e); }
			});
		} catch (RejectedExecutionException ignored) {
			// Shutdown has already settled pending commands and rejects new callbacks.
		}
	}

	EventManager getEventManager() {
		return eventManager;
	}

	FileTransferHelper getFileTransferHelper() {
		return fileTransferHelper;
	}

	void fireDisconnect(Connection source) {
		synchronized (this) {
			if (source != connection || state.get() == State.CLOSING || state.get() == State.CLOSED) return;
			if (state.get() == State.CONNECTING) {
				Future<?> task = initializationTask;
				if (task != null) task.cancel(true);
				return;
			}
			state.set(State.DISCONNECTED);
		}
		submitUserTask("ConnectionHandler disconnect task", () -> {
			try { connectionHandler.onDisconnect(this); }
			finally { if (state.get() == State.DISCONNECTED) close(); }
		});
		if (!isReconnectEnabled()) {
			submitUserTask("Disconnected query cleanup", this::close);
		}
	}
}
