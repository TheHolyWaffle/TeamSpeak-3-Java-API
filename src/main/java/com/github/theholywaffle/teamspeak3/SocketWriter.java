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

import com.github.theholywaffle.teamspeak3.commands.Command;

import java.io.IOException;
import java.io.PrintStream;

public class SocketWriter extends Thread {

	private final QueryIO io;
	private final int floodRate;
	private final PrintStream out;
	private volatile long lastCommand = System.currentTimeMillis();

	public SocketWriter(QueryIO io, int floodRate) throws IOException {
		super("[TeamSpeak-3-Java-API] SocketWriter");
		this.io = io;
		if (floodRate > 50) {
			this.floodRate = floodRate;
		} else {
			this.floodRate = 50;
		}

		out = new PrintStream(io.getSocket().getOutputStream(), true, "UTF-8");
	}

	@Override
	public void run() {
		while (!isInterrupted()) {
			final Command c = io.getCommandQueue().peek();
			if (c != null && !c.isSent()) {
				final String msg = c.toString();
				TS3Query.log.info("> " + msg);

				c.setSent();
				out.println(msg);
				lastCommand = System.currentTimeMillis();
			}
			try {
				Thread.sleep(floodRate);
			} catch (final InterruptedException e) {
				interrupt();
				break;
			}
		}

		out.close();

		if (!isInterrupted()) {
			TS3Query.log.warning("SocketWriter has stopped!");
		}
	}

	public long getIdleTime() {
		return System.currentTimeMillis() - lastCommand;
	}
}
