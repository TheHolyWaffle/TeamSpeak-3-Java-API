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

import java.util.logging.Level;

public class KeepAliveThread extends Thread {

	private static final int SLEEP = 60_000;

	private final SocketWriter writer;
	private final TS3ApiAsync asyncApi;

	public KeepAliveThread(SocketWriter writer, TS3ApiAsync asyncApi) {
		super("[TeamSpeak-3-Java-API] Keep alive");
		this.writer = writer;
		this.asyncApi = asyncApi;
	}

	@Override
	public void run() {
		try {
			while (!isInterrupted()) {
				final long idleTime = writer.getIdleTime();
				if (idleTime >= SLEEP) {
					// Using the asynchronous API so we get InterruptedExceptions
					asyncApi.whoAmI().get();
				} else {
					Thread.sleep(SLEEP - idleTime);
				}
			}
		} catch (final InterruptedException e) {
			// Thread stopped properly, ignore
		} catch (final Exception e) {
			TS3Query.log.log(Level.WARNING, "KeepAlive thread has stopped!", e);
		}
	}
}
