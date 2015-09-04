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

import com.github.theholywaffle.teamspeak3.api.Callback;
import com.github.theholywaffle.teamspeak3.commands.Command;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class SocketReader extends Thread {

	private final TS3Query ts3;
	private final ExecutorService userThreadPool;
	private final Map<Command, Callback> callbackMap;

	private String lastEvent;

	public SocketReader(TS3Query ts3) {
		super("[TeamSpeak-3-Java-API] SocketReader");
		this.ts3 = ts3;
		this.userThreadPool = Executors.newCachedThreadPool();
		this.callbackMap = Collections.synchronizedMap(new LinkedHashMap<Command, Callback>());
		this.lastEvent = "";

		try {
			int i = 0;
			while (i < 4 || ts3.getIn().ready()) {
				TS3Query.log.info("< " + ts3.getIn().readLine());
				i++;
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (ts3.getSocket() != null && ts3.getSocket().isConnected()
				&& ts3.getIn() != null && !isInterrupted()) {
			final String line;

			try {
				// Will block until a full line of text could be read.
				line = ts3.getIn().readLine();
			} catch (IOException io) {
				if (!isInterrupted()) {
					io.printStackTrace();
				}
				break;
			}

			if (line == null) {
				break; // The underlying socket was closed
			} else if (line.isEmpty()) {
				continue; // The server is sending garbage
			}

			final Command c = ts3.getCommandList().peek();

			if (line.startsWith("notify")) {
				TS3Query.log.info("< [event] " + line);

				// Filter out duplicate events for join, quit and channel move events
				if (isDuplicate(line)) continue;

				userThreadPool.execute(new Runnable() {
					@Override
					public void run() {
						final String arr[] = line.split(" ", 2);
						ts3.getEventManager().fireEvent(arr[0], arr[1]);
					}
				});
			} else if (c != null && c.isSent()) {
				TS3Query.log.info("[" + c.getName() + "] < " + line);
				if (line.startsWith("error")) {
					c.feedError(line.substring("error ".length()));
					if (c.getError().getId() != 0) {
						TS3Query.log.severe("TS3 command error: " + c.getError());
					}
					c.setAnswered();
					ts3.getCommandList().remove(c);
					answerCallback(c);
				} else {
					c.feed(line);
				}
			} else {
				TS3Query.log.warning("[UNHANDLED] < " + line);
			}
		}

		userThreadPool.shutdown();
		if (!isInterrupted()) {
			TS3Query.log.warning("SocketReader has stopped!");
		}
	}

	private void answerCallback(Command c) {
		final Callback callback = callbackMap.get(c);

		// Command had no callback registered
		if (callback == null) return;

		// To avoid the possibility of clogging the map with callbacks for
		// inexistent commands, remove all entries before the current one
		// Typically, this will exit without removing a single entry
		Set<Command> keySet = callbackMap.keySet();
		synchronized (callbackMap) {
			Iterator<Command> iterator = keySet.iterator();
			while (iterator.hasNext() && !c.equals(iterator.next())) {
				iterator.remove();
			}
		}

		userThreadPool.execute(new Runnable() {
			@Override
			public void run() {
				try {
					callback.handle();
				} catch (Throwable t) {
					TS3Query.log.log(Level.WARNING, "User callback threw exception", t);
				}
			}
		});
	}

	void registerCallback(Command command, Callback callback) {
		callbackMap.put(command, callback);
	}

	private boolean isDuplicate(String eventMessage) {
		if (!(eventMessage.startsWith("notifyclientmoved")
				|| eventMessage.startsWith("notifycliententerview")
				|| eventMessage.startsWith("notifyclientleftview"))) {

			// Event that will never cause duplicates
			return false;
		}

		if (eventMessage.equals(lastEvent)) {
			// Duplicate event!
			lastEvent = ""; // Let's only ever filter one duplicate
			return true;
		}

		lastEvent = eventMessage;
		return false;
	}
}
