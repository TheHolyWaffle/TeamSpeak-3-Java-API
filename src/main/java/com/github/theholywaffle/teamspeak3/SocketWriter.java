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
import com.github.theholywaffle.teamspeak3.commands.response.ResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;

public class SocketWriter extends Thread {

	private static final Logger log = LoggerFactory.getLogger(SocketWriter.class);

	private final BlockingQueue<Command> sendQueue;
	private final BlockingQueue<ResponseBuilder> receiveQueue;
	private final int floodRate;
	private final boolean logComms;
	private final PrintStream out;

	private volatile long lastCommandTime = System.currentTimeMillis();

	private Command interruptedCommand = null;

	public SocketWriter(QueryIO io, TS3Config config) throws IOException {
		super("[TeamSpeak-3-Java-API] SocketWriter");
		this.sendQueue = io.getSendQueue();
		this.receiveQueue = io.getReceiveQueue();
		this.floodRate = config.getFloodRate().getMs();
		this.logComms = config.getEnableCommunicationsLogging();
		this.out = new PrintStream(io.getSocket().getOutputStream(), true, "UTF-8");
	}

	@Override
	public void run() {
		try {
			// Initial sleep to prevent flood ban shortly after connecting
			if (floodRate > 0) Thread.sleep(floodRate);

			while (!isInterrupted()) {
				final Command c = sendQueue.take();
				final String msg = c.toString();

				lastCommandTime = System.currentTimeMillis();

				try {
					receiveQueue.put(new ResponseBuilder(c));
				} catch (InterruptedException e) {
					// Properly handle commands removed from the sendQueue
					// but not inserted into the receiveQueue
					interruptedCommand = c;
					interrupt();
					break;
				}

				if (logComms) log.debug("[{}] > {}", c.getName(), msg);
				out.println(msg);

				if (floodRate > 0) Thread.sleep(floodRate);
			}
		} catch (InterruptedException e) {
			// Regular shutdown
			interrupt();
		}

		out.close();

		if (!isInterrupted()) {
			log.warn("SocketWriter has stopped!");
		}
	}

	long getIdleTime() {
		return System.currentTimeMillis() - lastCommandTime;
	}

	void drainCommandsTo(Collection<Command> commands) {
		if (interruptedCommand != null) {
			commands.add(interruptedCommand);
		}
		sendQueue.drainTo(commands);
	}
}
