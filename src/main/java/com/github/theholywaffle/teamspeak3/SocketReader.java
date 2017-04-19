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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Queue;

public class SocketReader extends Thread {

	private static final Logger log = LoggerFactory.getLogger(SocketReader.class);

	private final TS3Query ts3;
	private final Queue<Command> receiveQueue;
	private final BufferedReader in;

	private String lastEvent = "";

	public SocketReader(QueryIO io, TS3Query ts3Query) throws IOException {
		super("[TeamSpeak-3-Java-API] SocketReader");
		this.receiveQueue = io.getReceiveQueue();
		this.ts3 = ts3Query;

		// Connect
		this.in = new BufferedReader(new InputStreamReader(io.getSocket().getInputStream(), "UTF-8"));
		int i = 0;
		while (i < 4 || in.ready()) {
			log.debug("< {}", in.readLine());
			i++;
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
					log.error("Connection error occurred.", io);
				}
				break;
			}

			if (line == null) {
				// End of stream: connection terminated by server
				log.error("Connection closed by the server.");
				break;
			} else if (line.isEmpty()) {
				continue; // The server is sending garbage
			}

			if (line.startsWith("notify")) {
				// Handle event
				log.debug("< [event] {}", line);

				// Filter out duplicate events for join, quit and channel move events
				if (isDuplicate(line)) continue;

				ts3.submitUserTask(new Runnable() {
					@Override
					public void run() {
						final String arr[] = line.split(" ", 2);
						ts3.getEventManager().fireEvent(arr[0], arr[1]);
					}
				});
			} else {
				// Handle response to a command
				final Command c = receiveQueue.peek();
				if (c == null) {
					log.warn("[UNHANDLED] < {}", line);
					return;
				}

				log.debug("[{}] < {}", c.getName(), line);
				if (line.startsWith("error")) {
					if (c instanceof CQuit) {
						// Response to a quit command received, we're done
						interrupt();
					}

					c.feedError(line.substring("error ".length()));
					if (c.getError().getId() != 0) {
						log.warn("TS3 command error: {}", c.getError());
					}
					receiveQueue.remove();

					final Callback callback = c.getCallback();
					if (callback != null) {
						ts3.submitUserTask(new Runnable() {
							@Override
							public void run() {
								try {
									callback.handle();
								} catch (Throwable t) {
									log.warn("User callback threw exception", t);
								}
							}
						});
					}
				} else {
					c.feed(line);
				}
			}
		}

		try {
			in.close();
		} catch (IOException ignored) {
			// Ignore
		}

		if (!isInterrupted()) {
			log.warn("SocketReader has stopped!");
			ts3.fireDisconnect();
		}
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
