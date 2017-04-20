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
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;

public class TS3Config {

	private String host = null;
	private int queryPort = 10011;
	private FloodRate floodRate = FloodRate.DEFAULT;
	private boolean enableCommunicationsLogging = false;
	private int commandTimeout = 4000;
	private ReconnectStrategy reconnectStrategy = ReconnectStrategy.disconnect();
	private ConnectionHandler connectionHandler = null;

	public TS3Config setHost(String host) {
		this.host = host;
		return this;
	}

	String getHost() {
		return host;
	}

	public TS3Config setQueryPort(int queryPort) {
		if (queryPort <= 0 || queryPort > 65535) {
			throw new IllegalArgumentException("Port out of range: " + queryPort);
		}
		this.queryPort = queryPort;
		return this;
	}

	int getQueryPort() {
		return queryPort;
	}

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
	 * Sets how many milliseconds a call in {@link TS3Api} should block at most until a command
	 * without response fails. By default, this timeout is 4000 milliseconds.
	 *
	 * @param commandTimeout
	 * 		the maximum time to wait for a response until a synchronous command call fails
	 *
	 * @return this TS3Config object for chaining
	 *
	 * @throws IllegalArgumentException
	 * 		if the timeout value is smaller than or equal to {@code 0}
	 */
	public TS3Config setCommandTimeout(int commandTimeout) {
		if (commandTimeout <= 0) {
			throw new IllegalArgumentException("Timeout value must be greater than 0");
		}

		this.commandTimeout = commandTimeout;
		return this;
	}

	int getCommandTimeout() {
		return commandTimeout;
	}

	public TS3Config setReconnectStrategy(ReconnectStrategy reconnectStrategy) {
		if (reconnectStrategy == null) throw new IllegalArgumentException("reconnectStrategy cannot be null!");
		this.reconnectStrategy = reconnectStrategy;
		return this;
	}

	ReconnectStrategy getReconnectStrategy() {
		return reconnectStrategy;
	}

	public TS3Config setConnectionHandler(ConnectionHandler connectionHandler) {
		this.connectionHandler = connectionHandler;
		return this;
	}

	ConnectionHandler getConnectionHandler() {
		return connectionHandler;
	}
}
