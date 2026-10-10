package com.github.theholywaffle.teamspeak3;

import java.time.Duration;
import com.github.theholywaffle.teamspeak3.TS3Query.FloodRate;
import com.github.theholywaffle.teamspeak3.TS3Query.Protocol;
import com.github.theholywaffle.teamspeak3.api.reconnect.CommandRetryPolicy;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.reconnect.SessionConfiguration;

/** Immutable connection and execution settings. Build using {@link TS3Config#build()}.
 * Callback and custom transport contracts must be thread safe; their implementation state is caller owned.
 * Credentials are deliberately excluded from diagnostic output.
 */
public final class QueryConfig {
	private final String host;
	private final ServerType serverType;
	private final SshHostKeyPolicy sshHostKeyPolicy;
	private final QueryTransportFactory transportFactory;
	private final int queryPort;
	private final Protocol protocol;
	private final String username;
	private final String password;
	private final FloodRate floodRate;
	private final boolean enableCommunicationsLogging;
	private final Duration connectTimeout;
	private final Duration handshakeTimeout;
	private final Duration queueWaitTimeout;
	private final Duration commandResponseTimeout;
	private final Duration closeTimeout;
	private final ReconnectStrategy reconnectStrategy;
	private final ConnectionHandler connectionHandler;
	private final CommandRetryPolicy commandRetryPolicy;
	private final SessionConfiguration sessionConfiguration;
	private final int commandCapacity;
	private final int listenerCapacity;
	private final int listenerQueueCapacity;
	private final int eventCallbackThreads;

	QueryConfig(TS3Config builder) {
		host = builder.getHost() == null ? "127.0.0.1" : builder.getHost();
		serverType = builder.getServerType();
		sshHostKeyPolicy = builder.getSshHostKeyPolicy();
		transportFactory = builder.getTransportFactory();
		queryPort = builder.getQueryPort();
		protocol = builder.getProtocol();
		username = builder.getUsername();
		password = builder.getPassword();
		floodRate = builder.getFloodRate();
		enableCommunicationsLogging = builder.getEnableCommunicationsLogging();
		connectTimeout = builder.getConnectTimeout();
		handshakeTimeout = builder.getHandshakeTimeout();
		queueWaitTimeout = builder.getQueueWaitTimeout();
		commandResponseTimeout = builder.getCommandResponseTimeout();
		closeTimeout = builder.getCloseTimeout();
		reconnectStrategy = builder.getReconnectStrategy();
		connectionHandler = builder.getConnectionHandler();
		commandRetryPolicy = builder.getCommandRetryPolicy();
		sessionConfiguration = builder.getSessionConfiguration();
		commandCapacity = builder.getCommandCapacity();
		listenerCapacity = builder.getListenerCapacity();
		listenerQueueCapacity = builder.getListenerQueueCapacity();
		eventCallbackThreads = builder.getEventCallbackThreads();
		if (host.isBlank()) throw new IllegalArgumentException("Host must not be blank");
		if ((username == null) != (password == null) || (username != null && username.isBlank()))
			throw new IllegalArgumentException("Provide both query username and password, or neither");
		if (!transportFactory.supports(serverType, protocol))
			throw new IllegalArgumentException("Transport does not support " + serverType + " with " + protocol);
		if (transportFactory.authenticates() && !hasLoginCredentials())
			throw new IllegalArgumentException("Authenticated transport requires query login credentials");
		if (commandRetryPolicy.getMaxRetries() > 0 && sessionConfiguration == null)
			throw new IllegalStateException("Command replay requires explicit session configuration");
	}
	/** @return configured host */
	public String getHost() { return host; }
	/** @return configured serverType */
	public ServerType getServerType() { return serverType; }
	/** @return configured sshHostKeyPolicy */
	public SshHostKeyPolicy getSshHostKeyPolicy() { return sshHostKeyPolicy; }
	/** @return configured transportFactory */
	public QueryTransportFactory getTransportFactory() { return transportFactory; }
	/** @return configured queryPort */
	public int getQueryPort() { return queryPort; }
	/** @return configured protocol */
	public Protocol getProtocol() { return protocol; }
	/** @return configured username */
	public String getUsername() { return username; }
	/** @return configured password */
	public String getPassword() { return password; }
	/** @return configured floodRate */
	public FloodRate getFloodRate() { return floodRate; }
	/** @return configured enableCommunicationsLogging */
	public boolean getEnableCommunicationsLogging() { return enableCommunicationsLogging; }
	/** @return configured connectTimeout */
	public Duration getConnectTimeout() { return connectTimeout; }
	/** @return configured handshakeTimeout */
	public Duration getHandshakeTimeout() { return handshakeTimeout; }
	/** @return configured queueWaitTimeout */
	public Duration getQueueWaitTimeout() { return queueWaitTimeout; }
	/** @return configured commandResponseTimeout */
	public Duration getCommandResponseTimeout() { return commandResponseTimeout; }
	/** @return configured closeTimeout */
	public Duration getCloseTimeout() { return closeTimeout; }
	/** @return configured reconnectStrategy */
	public ReconnectStrategy getReconnectStrategy() { return reconnectStrategy; }
	/** @return configured connectionHandler */
	public ConnectionHandler getConnectionHandler() { return connectionHandler; }
	/** @return configured commandRetryPolicy */
	public CommandRetryPolicy getCommandRetryPolicy() { return commandRetryPolicy; }
	/** @return configured sessionConfiguration */
	public SessionConfiguration getSessionConfiguration() { return sessionConfiguration; }
	/** @return configured commandCapacity */
	public int getCommandCapacity() { return commandCapacity; }
	/** @return configured listenerCapacity */
	public int getListenerCapacity() { return listenerCapacity; }
	/** @return configured listenerQueueCapacity */
	public int getListenerQueueCapacity() { return listenerQueueCapacity; }
	/** @return configured eventCallbackThreads */
	public int getEventCallbackThreads() { return eventCallbackThreads; }
	/** @return whether a complete login is configured */
	public boolean hasLoginCredentials() { return username != null && password != null; }
	@Override public String toString() {
		return "QueryConfig[server=" + serverType + ", protocol=" + protocol + ", port=" + queryPort + "]";
	}
}
