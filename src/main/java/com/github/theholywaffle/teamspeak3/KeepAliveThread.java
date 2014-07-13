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

import com.github.theholywaffle.teamspeak3.commands.CWhoAmI;

public class KeepAliveThread extends Thread {

	private static final int SLEEP = 60_000;

	private final TS3Query ts3;
	private final SocketWriter writer;

	private volatile boolean stop;

	public KeepAliveThread(TS3Query ts3, SocketWriter socketWriter) {
		super("Keep alive");
		this.ts3 = ts3;
		this.writer = socketWriter;
	}

	@Override
	public void run() {
		while (ts3.getSocket() != null && ts3.getSocket().isConnected()
				&& ts3.getIn() != null && !stop) {
			final long idleTime = writer.getIdleTime();
			if (idleTime >= SLEEP) {
				ts3.doCommand(new CWhoAmI());
			}
			try {
				Thread.sleep(50);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
		TS3Query.log.warning("KeepAlive thread has stopped!");
	}

	public void finish() {
		stop = true;
	}

}
