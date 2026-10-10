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

import com.github.theholywaffle.teamspeak3.TS3Query.FloodRate;
import com.github.theholywaffle.teamspeak3.TS3Query.Protocol;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.CommandRetryPolicy;
import com.github.theholywaffle.teamspeak3.api.reconnect.SessionConfiguration;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;

import java.time.Duration;
import java.util.Objects;

/**
 * Reusable mutable builder for immutable {@link QueryConfig} snapshots.
 * Constructing a query never freezes or changes this builder. Not thread safe.
 */
public final class TS3Config {

	private String host = null;
	private ServerType serverType = ServerType.TS3;
	private SshHostKeyPolicy sshHostKeyPolicy = SshHostKeyPolicy.defaultKnownHosts();
	private QueryTransportFactory transportFactory;
	private int queryPort = -1;
	private Protocol protocol = Protocol.RAW;
	private String username = null;
	private String password = null;
	private FloodRate floodRate = FloodRate.DEFAULT;
	private boolean enableCommunicationsLogging = false;
	private Duration connectTimeout = Duration.ofSeconds(4);
	private Duration handshakeTimeout = Duration.ofSeconds(4);
	private Duration queueWaitTimeout = Duration.ofSeconds(4);
	private Duration commandResponseTimeout = Duration.ofSeconds(4);
	private Duration closeTimeout = Duration.ofSeconds(4);
	private ReconnectStrategy reconnectStrategy = ReconnectStrategy.disconnect();
	private ConnectionHandler connectionHandler = null;
	private CommandRetryPolicy commandRetryPolicy = CommandRetryPolicy.none();
	private SessionConfiguration sessionConfiguration;
	private int commandCapacity = 1024;
	private int listenerCapacity = 64;
	private int listenerQueueCapacity = 256;
	private int eventCallbackThreads = 4;

	/**
	 * Bounds accepted commands, including queued, in-flight and completing callbacks.
	 * Excess commands fail immediately on the submitting thread. Default: 1024.
	 * @param capacity positive command capacity, separately applied to startup and application queues
	 * @return this configuration
	 */
	public TS3Config setCommandCapacity(int capacity) {
		commandCapacity = positiveCapacity(capacity); return this;
	}

	/**
	 * Bounds registered listeners. Excess registrations throw IllegalStateException. Default: 64.
	 * @param capacity positive listener capacity
	 * @return this configuration
	 */
	public TS3Config setListenerCapacity(int capacity) {
		listenerCapacity = positiveCapacity(capacity); return this;
	}

	/**
	 * Bounds waiting events per listener, excluding its running callback. Drops newest on overflow.
	 * Default: 256. Dropped events are counted by TS3Query.getEventStatistics().
	 * @param capacity positive waiting-event capacity
	 * @return this configuration
	 */
	public TS3Config setListenerQueueCapacity(int capacity) {
		listenerQueueCapacity = positiveCapacity(capacity); return this;
	}

	/**
	 * Sizes the query-owned event executor, isolated from command completion and lifecycle callbacks.
	 * Its task queue is bounded by listener capacity; rejection drops pending events. Default: 4.
	 * @param threads positive worker count
	 * @return this configuration
	 */
	public TS3Config setEventCallbackThreads(int threads) {
		eventCallbackThreads = positiveCapacity(threads); return this;
	}

	private static int positiveCapacity(int value) {
		if (value <= 0) throw new IllegalArgumentException("Capacity must be positive");
		return value;
	}

	/** @return command capacity */
	public int getCommandCapacity() { return commandCapacity; }
	/** @return listener registration capacity */
	public int getListenerCapacity() { return listenerCapacity; }
	/** @return waiting-event capacity per listener */
	public int getListenerQueueCapacity() { return listenerQueueCapacity; }
	/** @return owned event executor worker count */
	public int getEventCallbackThreads() { return eventCallbackThreads; }


	/**
	 * Sets an opt-in read replay policy. Requires explicit session configuration for replay.
	 * @param policy immutable retry policy
	 * @return this configuration
	 */
	public TS3Config setCommandRetryPolicy(CommandRetryPolicy policy) {
		commandRetryPolicy = Objects.requireNonNull(policy, "policy");
		return this;
	}

	/**
	 * Sets the session restored before application commands on every connection.
	 * @param session immutable target, nickname and subscriptions
	 * @return this configuration
	 */
	public TS3Config setSessionConfiguration(SessionConfiguration session) {
		sessionConfiguration = Objects.requireNonNull(session, "session");
		return this;
	}

	CommandRetryPolicy getCommandRetryPolicy() { return commandRetryPolicy; }
	SessionConfiguration getSessionConfiguration() { return sessionConfiguration; }

	/**
	 * Sets the hostname or IP address of the TeamSpeak3 server to connect to.
	 * <p>
	 * Note that the query port <strong>is not</strong> part of the hostname -
	 * use {@link #setQueryPort(int)} for that purpose.
	 * </p><p>
	 * If the application is running on the same machine as the TS3 server, you can use
	 * {@code null} as the hostname. You can also use any other loopback address,
	 * such as {@code localhost} or {@code 127.0.0.1}.
	 * </p>
	 *
	 * @param host
	 * 		a valid hostname or IP address of a TeamSpeak3 server, or {@code null}
	 *
	 * @return this TS3Config object for chaining
	 */
	public TS3Config setHost(String host) {
		this.host = host;
		return this;
	}

	String getHost() {
		return host;
	}

	/**
	 * Sets the query port to use when connecting to the TeamSpeak3 server.
	 * <p>
	 * Note that the query uses a different port to connect to a server than the regular
	 * TeamSpeak3 clients. Regular clients use "voice ports", the query uses the "query port".
	 * </p><p>
	 * If you don't set the query port by calling this method, the query will use the default
	 * query port:
	 * </p>
	 * <ul>
	 *     <li>{@code 10011} when connecting using {@link Protocol#RAW}</li>
	 *     <li>{@code 10022} when connecting using {@link Protocol#SSH}</li>
	 * </ul>
	 *
	 * @param queryPort
	 * 		the query port to use, must be between {@code 1} and {@code 65535}
	 *
	 * @return this TS3Config object for chaining
	 *
	 * @throws IllegalArgumentException
	 * 		if the port is out of range
	 */
	public TS3Config setQueryPort(int queryPort) {
		if (queryPort <= 0 || queryPort > 65535) {
			throw new IllegalArgumentException("Port out of range: " + queryPort);
		}
		this.queryPort = queryPort;
		return this;
	}

	int getQueryPort() {
		if (queryPort > 0) {
			return queryPort;
		} else {
			// Query port not set by user, use default for chosen protocol
			return protocol == Protocol.SSH ? 10022 : 10011;
		}
	}

	/**
	 * Defines the protocol used to connect to the TeamSpeak3 server.
	 * By default, {@link Protocol#RAW} is used.
	 *
	 * @param protocol
	 * 		the connection protocol to use
	 *
	 * @return this TS3Config object for chaining
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code protocol} is {@code null}
	 * @see Protocol Protocol
	 */
	public TS3Config setProtocol(Protocol protocol) {
		if (protocol == null) throw new IllegalArgumentException("protocol cannot be null!");
		this.protocol = protocol;
		return this;
	}

	Protocol getProtocol() {
		return protocol;
	}

	/**
	 * Authenticates the query with the TeamSpeak3 server using the given login credentials
	 * immediately after connecting.
	 * <p>
	 * Setting the login credentials is mandatory when using the {@link Protocol#SSH} protocol.
	 * </p><p>
	 * A server query login can be generated by heading over to the TeamSpeak3 Client, Tools,
	 * ServerQuery Login. Note that the server query will have the same permissions as the client who
	 * generated the credentials.
	 * </p>
	 *
	 * @param username
	 * 		the username used to authenticate the query
	 * @param password
	 * 		the password corresponding to {@code username}
	 *
	 * @return this TS3Config object for chaining
	 */
	public TS3Config setLoginCredentials(String username, String password) {
		this.username = username;
		this.password = password;
		return this;
	}

	String getUsername() {
		return username;
	}

	String getPassword() {
		return password;
	}

	/**
	 * Sets the delay between sending commands.
	 * <p>
	 * If the query's hostname / IP has not been added to the server's {@code query_ip_whitelist.txt},
	 * you need to use {@link FloodRate#DEFAULT} to prevent the query from being flood-banned.
	 * </p><p>
	 * Calling {@link FloodRate#custom} allows you to use a custom command delay if neither
	 * {@link FloodRate#UNLIMITED} nor {@link FloodRate#DEFAULT} fit your needs.
	 * </p>
	 *
	 * @param rate
	 * 		a {@link FloodRate} object that defines the delay between commands
	 *
	 * @return this TS3Config object for chaining
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code rate} is {@code null}
	 * @see FloodRate FloodRate
	 */
	public TS3Config setFloodRate(FloodRate rate) {
		if (rate == null) throw new IllegalArgumentException("rate cannot be null!");
		this.floodRate = rate;
		return this;
	}

	FloodRate getFloodRate() {
		return floodRate;
	}

	/**
	 * Setting this value to {@code true} will log the communication between the
	 * query client and the TS3 server at the {@code DEBUG} level.
	 * <p>
	 * By default, this is turned off to prevent leaking IPs, tokens, passwords, etc.
	 * into the console and / or log files.
	 * </p>
	 *
	 * @param enable
	 * 		whether to log query commands
	 *
	 * @return this TS3Config object for chaining
	 */
	public TS3Config setEnableCommunicationsLogging(boolean enable) {
		enableCommunicationsLogging = enable;
		return this;
	}

	boolean getEnableCommunicationsLogging() {
		return enableCommunicationsLogging;
	}

	/**
	 * Sets the command response deadline in milliseconds. Unrelated traffic does not extend it.
	 * @param commandTimeout positive timeout in milliseconds
	 * @return this configuration
	 * @deprecated use {@link #setCommandResponseTimeout(Duration)}; other phases have separate deadlines
	 */
	@Deprecated
	public TS3Config setCommandTimeout(int commandTimeout) {
		return setCommandResponseTimeout(Duration.ofMillis(commandTimeout));
	}

	private Duration timeout(Duration value) {
		if (value == null || value.isZero() || value.isNegative()) {
			throw new IllegalArgumentException("Deadline must be positive");
		}
		try {
			if (value.toNanos() > Long.MAX_VALUE / 4) throw new ArithmeticException();
		} catch (ArithmeticException e) {
			throw new IllegalArgumentException("Deadline is too large", e);
		}
		return value;
	}

	/**
	 * Sets the TCP connection deadline, including name resolution. Defaults to four seconds.
	 * @param value positive, finite duration
	 * @return this configuration
	 */
	public TS3Config setConnectTimeout(Duration value) {
		connectTimeout = timeout(value);
		return this;
	}

	/**
	 * Sets the total SSH negotiation, authentication and onConnect deadline after TCP connection. Defaults to four seconds.
	 * @param value positive, finite duration
	 * @return this configuration
	 */
	public TS3Config setHandshakeTimeout(Duration value) {
		handshakeTimeout = timeout(value);
		return this;
	}

	/**
	 * Sets the maximum wait before a command starts being written. Defaults to four seconds.
	 * @param value positive, finite duration
	 * @return this configuration
	 */
	public TS3Config setQueueWaitTimeout(Duration value) {
		queueWaitTimeout = timeout(value);
		return this;
	}

	/**
	 * Sets the deadline from starting a command write through its terminating error response. Defaults to four seconds.
	 * @param value positive, finite duration
	 * @return this configuration
	 */
	public TS3Config setCommandResponseTimeout(Duration value) {
		commandResponseTimeout = timeout(value);
		return this;
	}

	/**
	 * Sets the total shutdown budget, including optional exit draining and thread joins. Defaults to four seconds.
	 * @param value positive, finite duration
	 * @return this configuration
	 */
	public TS3Config setCloseTimeout(Duration value) {
		closeTimeout = timeout(value);
		return this;
	}

	/** @return the configured TCP connection deadline, including name resolution */
	public Duration getConnectTimeout() { return connectTimeout; }

	/** @return the configured total SSH negotiation, authentication and onConnect deadline after TCP connection */
	public Duration getHandshakeTimeout() { return handshakeTimeout; }

	/** @return the configured maximum wait before a command starts being written */
	public Duration getQueueWaitTimeout() { return queueWaitTimeout; }

	/** @return the configured deadline from starting a command write through its terminating error response */
	public Duration getCommandResponseTimeout() { return commandResponseTimeout; }

	/** @return the configured total shutdown budget, including optional exit draining and thread joins */
	public Duration getCloseTimeout() { return closeTimeout; }


	static int socketTimeout(Duration value) {
		return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, value.toMillis()));
	}

	/**
	 * Sets what strategy the query uses to reconnect after having been disconnected.
	 * <p>
	 * The different reconnect strategies let you control whether and after which delay the
	 * query will try to reconnect. By default, {@link ReconnectStrategy#disconnect()} is used,
	 * which doesn't try to reconnect and simply stops the query.
	 * </p><p>
	 * Note that when using a reconnect strategy, you probably also want to set the
	 * {@link ConnectionHandler} using {@link TS3Config#setConnectionHandler(ConnectionHandler)}.
	 *
	 * @param reconnectStrategy
	 * 		the reconnect strategy used when the query loses connection
	 *
	 * @return this TS3Config object for chaining
	 *
	 * @see ReconnectStrategy The reconnect strategies
	 * @see ConnectionHandler The connection handler
	 */
	public TS3Config setReconnectStrategy(ReconnectStrategy reconnectStrategy) {
		if (reconnectStrategy == null) throw new IllegalArgumentException("reconnectStrategy cannot be null!");
		this.reconnectStrategy = reconnectStrategy;
		return this;
	}

	ReconnectStrategy getReconnectStrategy() {
		return reconnectStrategy;
	}

	/**
	 * Sets the {@link ConnectionHandler} that defines the query's behaviour
	 * when connecting or disconnecting.
	 * <p>
	 * The following sample code illustrates how a reconnect strategy and connection handler can be
	 * used to print a message to the console every time the query connects or disconnects:
	 * </p>
	 *
	 * <pre>
	 * config.setReconnectStrategy(ReconnectStrategy.exponentialBackoff());
	 * config.setConnectionHandler(new ConnectionHandler() {
	 * 	&#64;Override
	 * 	public void onConnect(TS3Api api) {
	 * 		System.out.println("Successfully connected!");
	 * 	}
	 *
	 * 	&#64;Override
	 * 	public void onDisconnect(TS3Query query) {
	 * 		System.out.println("The query was disconnected!");
	 * 	}
	 * });
	 * </pre>
	 *
	 * @param connectionHandler
	 * 		the {@link ConnectionHandler} object
	 *
	 * @return this TS3Config object for chaining
	 *
	 * @see TS3Config#setReconnectStrategy(ReconnectStrategy)
	 */
	public TS3Config setConnectionHandler(ConnectionHandler connectionHandler) {
		this.connectionHandler = connectionHandler;
		return this;
	}

	ConnectionHandler getConnectionHandler() {
		return connectionHandler;
	}

	/** Builds an immutable validated snapshot; this builder remains reusable.
	 * @return configuration snapshot
	 */
	public QueryConfig build() { return new QueryConfig(this); }

	/** @param type server generation @return this builder */
	public TS3Config setServerType(ServerType type) { serverType = Objects.requireNonNull(type); return this; }
	/** @return configured server generation */
	public ServerType getServerType() { return serverType; }
	/** @param policy explicit SSH trust policy @return this builder */
	public TS3Config setSshHostKeyPolicy(SshHostKeyPolicy policy) { sshHostKeyPolicy = Objects.requireNonNull(policy); return this; }
	/** @return SSH trust policy */
	public SshHostKeyPolicy getSshHostKeyPolicy() { return sshHostKeyPolicy; }
	/** @param factory stream transport factory, invoked separately for every connection @return this builder */
	public TS3Config setTransportFactory(QueryTransportFactory factory) { transportFactory = Objects.requireNonNull(factory); return this; }
	/** @return selected transport factory */
	public QueryTransportFactory getTransportFactory() {
		return transportFactory == null ? (protocol == Protocol.SSH ? QueryTransports.SSH : QueryTransports.RAW) : transportFactory;
	}
	@Override public String toString() { return "TS3Config[server=" + serverType + ", protocol=" + protocol + "]"; }
}
