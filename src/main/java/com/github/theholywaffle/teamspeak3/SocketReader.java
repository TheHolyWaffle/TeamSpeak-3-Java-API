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
import com.github.theholywaffle.teamspeak3.commands.CQuit;
import com.github.theholywaffle.teamspeak3.commands.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class SocketReader extends Thread {

	private final TS3Query ts3;
	private final QueryIO io;
	private final BufferedReader in;

	private String lastEvent = "";

	public SocketReader(TS3Query ts3Query, QueryIO io) throws IOException {
		super("[TeamSpeak-3-Java-API] SocketReader");
		this.io = io;
		this.ts3 = ts3Query;

		// Connect
		this.in = new BufferedReader(new InputStreamReader(io.getSocket().getInputStream(), "UTF-8"));
		try {
			int i = 0;
			while (i < 4 || in.ready()) {
				TS3Query.log.info("< " + in.readLine());
				i++;
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (!isInterrupted()) {
			final String line;

			try {
				// Will block until a full line of text could be read.
				line = in.readLine();
			} catch (IOException io) {
				if (!isInterrupted()) {
					TS3Query.log.log(Level.WARNING, "Connection error occurred.", io);
				}
				break;
			}

			if (line == null) {
				// End of stream: connection terminated by server
				TS3Query.log.warning("Connection closed by the server.");
				break;
			} else if (line.isEmpty()) {
				continue; // The server is sending garbage
			}

			final Command c = io.getCommandQueue().peek();

			if (line.startsWith("notify")) {
				TS3Query.log.info("< [event] " + line);

				// Filter out duplicate events for join, quit and channel move events
				if (isDuplicate(line)) continue;

				ts3.submitUserTask(new Runnable() {
					@Override
					public void run() {
						final String arr[] = line.split(" ", 2);
						ts3.getEventManager().fireEvent(arr[0], arr[1]);
					}
				});
			} else if (c != null && c.isSent()) {
				TS3Query.log.info("[" + c.getName() + "] < " + line);
				if (line.startsWith("error")) {
					if (c instanceof CQuit) {
						// Response to a quit command received, we're done
						interrupt();
					}

					c.feedError(line.substring("error ".length()));
					if (c.getError().getId() != 0) {
						TS3Query.log.severe("TS3 command error: " + c.getError());
					}
					c.setAnswered();
					io.getCommandQueue().remove(c);
					answerCallback(c);
				} else {
					c.feed(line);
				}
			} else {
				TS3Query.log.warning("[UNHANDLED] < " + line);
			}
		}

		try {
			in.close();
		} catch (IOException ignored) {
			// Ignore
		}

		if (!isInterrupted()) {
			TS3Query.log.warning("SocketReader has stopped!");
			ts3.fireDisconnect();
		}
	}

	private void answerCallback(Command c) {
		final Map<Command, Callback> callbackMap = io.getCallbackMap();
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

		ts3.submitUserTask(new Runnable() {
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
