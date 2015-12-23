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

public abstract class ReconnectingConnectionHandler implements ConnectionHandler {

	// Exponential backoff strategy
	private static final long STARTING_TIMEOUT = 1000;
	private static final double MULTIPLIER = 1.5;
	private static final long TIMEOUT_CAP = 30000;

	private long timeout = -1;

	@Override
	public void onConnect(TS3Query ts3Query) {
		timeout = STARTING_TIMEOUT;
		setUpQuery(ts3Query);
	}

	public abstract void setUpQuery(TS3Query ts3Query);

	@Override
	public void onDisconnect(TS3Query ts3Query) {
		if (timeout < 0) {
			// Special case: We never connected
			// --> Something is probably not set up correctly
			// Do not attempt to reconnect
			return;
		} else if (timeout == STARTING_TIMEOUT) {
			// First run, announce disconnect
			TS3Query.log.info("[Connection] Disconnected from TS3 server");
		}

		try {
			Thread.sleep(timeout);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		timeout *= MULTIPLIER;
		if (timeout > TIMEOUT_CAP) timeout = TIMEOUT_CAP;

		try {
			ts3Query.connect();
		} catch (TS3ConnectionFailedException conFailed) {
			// Ignore exception, announce reconnect failure
			TS3Query.log.fine("[Connection] Failed to reconnect - waiting " + timeout + "ms until next attempt");
		}
	}
}
