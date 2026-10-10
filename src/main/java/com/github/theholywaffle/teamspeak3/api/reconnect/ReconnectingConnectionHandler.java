package com.github.theholywaffle.teamspeak3.api.reconnect;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2015 Bert De Geyter
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

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

public class ReconnectingConnectionHandler implements ConnectionHandler {

	private static final Logger log = LoggerFactory.getLogger(ReconnectingConnectionHandler.class);

	private final ConnectionHandler userConnectionHandler;
	private final int startTimeout;
	private final int timeoutCap;
	private final int addend;
	private final double multiplier;
	private final int maxAttempts;

	public ReconnectingConnectionHandler(ConnectionHandler userConnectionHandler, int startTimeout,
	                                     int timeoutCap, int addend, double multiplier) {
		this(userConnectionHandler, startTimeout, timeoutCap, addend, multiplier, 10);
	}

	/** Creates a bounded reconnect handler; delays use equal jitter within the cap.
	 * @param userConnectionHandler optional callback
	 * @param startTimeout first delay upper bound in milliseconds
	 * @param timeoutCap maximum delay upper bound in milliseconds
	 * @param addend linear increase in milliseconds
	 * @param multiplier exponential increase
	 * @param maxAttempts maximum connection attempts per disconnect
	 */
	public ReconnectingConnectionHandler(ConnectionHandler userConnectionHandler, int startTimeout,
	                                     int timeoutCap, int addend, double multiplier, int maxAttempts) {
		if (startTimeout <= 0 || timeoutCap < startTimeout || addend < 0
			|| !Double.isFinite(multiplier) || multiplier < 1 || maxAttempts <= 0) {
			throw new IllegalArgumentException("Reconnect requires positive attempts/delays, cap >= start, and finite multiplier >= 1");
		}
		this.maxAttempts = maxAttempts;
		this.userConnectionHandler = userConnectionHandler;
		this.startTimeout = startTimeout;
		this.timeoutCap = timeoutCap;
		this.addend = addend;
		this.multiplier = multiplier;
	}

	@Override
	public void onConnect(TS3Api api) {
		if (userConnectionHandler != null) {
			userConnectionHandler.onConnect(api);
		}
	}

	@Override
	public void onDisconnect(TS3Query ts3Query) {
		// Announce disconnect and run user connection handler
		log.info("[Connection] Disconnected from TS3 server - reconnect delay capped at {}ms", startTimeout);
		if (userConnectionHandler != null) {
			userConnectionHandler.onDisconnect(ts3Query);
		}

		long timeout = startTimeout;
		for (int attempt = 0; attempt < maxAttempts; attempt++) {
			if (Thread.currentThread().isInterrupted() || ts3Query.getState() != TS3Query.State.DISCONNECTED) return;
			try {
				Thread.sleep(jitterDelay(timeout));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
			if (ts3Query.getState() != TS3Query.State.DISCONNECTED) return;
			try {
				ts3Query.connect();
				return;
			} catch (TS3ConnectionFailedException conFailed) {
				log.debug("[Connection] Reconnect attempt {} failed", attempt + 1);
			} catch (IllegalStateException stateChanged) {
				if (ts3Query.getState() == TS3Query.State.CLOSED || ts3Query.getState() == TS3Query.State.CLOSING
					|| ts3Query.getState() == TS3Query.State.CONNECTED) return;
				throw stateChanged;
			}
			timeout = nextDelay(timeout);
		}
		log.warn("[Connection] Reconnect attempts exhausted ({})", maxAttempts);
		ts3Query.close();
	}

	long nextDelay(long delay) {
		return (long) Math.min(timeoutCap, Math.ceil(delay * multiplier) + addend);
	}

	static long jitterDelay(long upperBound) {
		return ThreadLocalRandom.current().nextLong(Math.max(1, (upperBound + 1) / 2), upperBound + 1);
	}

}
