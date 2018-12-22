package com.github.theholywaffle.teamspeak3;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2018 Bert De Geyter, Roger Baumgartner
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

import com.github.theholywaffle.teamspeak3.api.CommandFuture;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.QueryError;
import com.github.theholywaffle.teamspeak3.commands.Command;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;
import com.github.theholywaffle.teamspeak3.commands.response.ResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

class SocketReader extends Thread {

	private static final Logger log = LoggerFactory.getLogger(SocketReader.class);

	private final TS3Query ts3;
	private final Connection con;
	private final BufferedReader in;
	private final boolean logComms;

	private CommandQueue commandQueue = null;
	private ResponseBuilder responseBuilder = null;
	private String lastEvent = "";

	SocketReader(Connection connection, Socket socket, TS3Query query, TS3Config config) throws IOException {
		super("[TeamSpeak-3-Java-API] SocketReader");

		ts3 = query;
		con = connection;
		in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
		logComms = config.getEnableCommunicationsLogging();

		readWelcomeMessage();
	}

	private void readWelcomeMessage() throws IOException {
		for (int i = 0; i < 4 || in.ready(); ++i) {
			String welcomeMessage = in.readLine();
			if (logComms) log.debug("< {}", welcomeMessage);
		}
	}

	@Override
	public void run() {
		while (!isInterrupted()) {
			String line;

			try {
				// Will block until a full line of text could be read.
				line = in.readLine();
			} catch (SocketTimeoutException socketTimeout) {
				// Really disconnected or just no data transferred for <commandTimeout> milliseconds?
				if (con.isTimedOut()) {
					log.error("Connection timed out.", socketTimeout);
					break;
				} else {
					continue;
				}
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
				handleEvent(line);
			} else {
				con.resetIdleTime();
				handleCommandResponse(line);
			}
		}

		try {
			in.close();
		} catch (IOException ignored) {
			// Ignore
		}

		if (!isInterrupted()) {
			con.internalDisconnect();
		}
	}

	private void handleEvent(String event) {
		if (logComms) log.debug("[event] < {}", event);

		// Filter out duplicate events for join, quit and channel move events
		if (isDuplicate(event)) return;

		String arr[] = event.split(" ", 2);
		ts3.getEventManager().fireEvent(arr[0], arr[1]);
	}

	private void handleCommandResponse(String response) {
		if (responseBuilder == null) {
			commandQueue = con.getCommandQueue();
			Command command = commandQueue.peekReceiveQueue();

			if (command == null) {
				log.warn("[UNHANDLED] < {}", response);
				return;
			}

			responseBuilder = new ResponseBuilder(command);
		}

		if (logComms) log.debug("[{}] < {}", responseBuilder.getCommand().getName(), response);

		if (response.startsWith("error ")) {
			handleCommandError(responseBuilder, response);

			commandQueue.removeFromReceiveQueue();
			responseBuilder = null;
		} else {
			responseBuilder.appendResponse(response);
		}
	}

	private void handleCommandError(ResponseBuilder responseBuilder, String error) {
		Command command = responseBuilder.getCommand();
		if (command.getName().equals("quit")) {
			// Response to a quit command received, we're done
			interrupt();
		}

		QueryError queryError = DefaultArrayResponse.parseError(error);
		CommandFuture<DefaultArrayResponse> future = command.getFuture();

		if (queryError.isSuccessful()) {
			DefaultArrayResponse response = responseBuilder.buildResponse();

			ts3.submitUserTask("Future SuccessListener (" + command.getName() + ")",
					() -> future.set(response));
		} else {
			log.debug("TS3 command error: {}", queryError);

			ts3.submitUserTask("Future FailureListener (" + command.getName() + ")",
					() -> future.fail(new TS3CommandFailedException(queryError, command.getName())));
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
