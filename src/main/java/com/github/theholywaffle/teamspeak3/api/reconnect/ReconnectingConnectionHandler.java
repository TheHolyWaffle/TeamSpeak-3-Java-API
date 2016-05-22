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

import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;

public class ReconnectingConnectionHandler implements ConnectionHandler {

	private final ConnectionHandler userConnectionHandler;
	private final int startTimeout;
	private final int timeoutCap;
	private final int addend;
	private final double multiplier;

	private int timeout = -1;

	public ReconnectingConnectionHandler(ConnectionHandler userConnectionHandler, int startTimeout,
										 int timeoutCap, int addend, double multiplier) {
		this.userConnectionHandler = userConnectionHandler;
		this.startTimeout = startTimeout;
		this.timeoutCap = timeoutCap;
		this.addend = addend;
		this.multiplier = multiplier;
	}

	@Override
	public void onConnect(TS3Query ts3Query) {
		timeout = startTimeout;
		if (userConnectionHandler != null) {
			userConnectionHandler.onConnect(ts3Query);
		}
	}

	@Override
	public void onDisconnect(TS3Query ts3Query) {
		if (timeout < 0) {
			// Special case: We never connected
			// --> Something is probably not set up correctly
			// Do not attempt to reconnect
			return;
		} else if (timeout == startTimeout) {
			// First run, announce disconnect
			TS3Query.log.info("[Connection] Disconnected from TS3 server");

			if (userConnectionHandler != null) {
				userConnectionHandler.onDisconnect(ts3Query);
			}
		}

		timeout = (int) Math.ceil(timeout * multiplier) + addend;
		if (timeoutCap > 0) timeout = Math.min(timeout, timeoutCap);

		try {
			Thread.sleep(timeout);
		} catch (InterruptedException e) {
			return;
		}

		try {
			ts3Query.connect();
		} catch (TS3ConnectionFailedException conFailed) {
			// Ignore exception, announce reconnect failure
			TS3Query.log.fine("[Connection] Failed to reconnect - waiting " + timeout + "ms until next attempt");
		}
	}
}
