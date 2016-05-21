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

import java.util.logging.Level;

public class TS3Config {

	private String host = null;
	private int queryPort = 10011;
	private FloodRate floodRate = FloodRate.DEFAULT;
	private Level level = Level.WARNING;
	private boolean debugToFile = false;
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
		this.queryPort = queryPort;
		return this;
	}

	int getQueryPort() {
		return queryPort;
	}

	public TS3Config setFloodRate(FloodRate rate) {
		if (rate == null) throw new NullPointerException("rate cannot be null!");
		this.floodRate = rate;
		return this;
	}

	FloodRate getFloodRate() {
		return floodRate;
	}

	public TS3Config setDebugLevel(Level level) {
		if (level == null) throw new NullPointerException("level cannot be null!");
		this.level = level;
		return this;
	}

	Level getDebugLevel() {
		return level;
	}

	public TS3Config setDebugToFile(boolean debugToFile) {
		this.debugToFile = debugToFile;
		return this;
	}

	boolean getDebugToFile() {
		return debugToFile;
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
		if (reconnectStrategy == null) throw new NullPointerException("reconnectStrategy cannot be null!");
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
