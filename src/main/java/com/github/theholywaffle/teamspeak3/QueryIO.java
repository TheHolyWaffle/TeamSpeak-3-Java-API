package com.github.theholywaffle.teamspeak3;

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

import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.DisconnectingConnectionHandler;
import com.github.theholywaffle.teamspeak3.commands.Command;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class QueryIO {

	private final Socket socket;
	private final SocketReader socketReader;
	private final SocketWriter socketWriter;
	private final KeepAliveThread keepAlive;

	private final BlockingQueue<Command> sendQueue;
	private final BlockingQueue<Command> receiveQueue;

	QueryIO(TS3Query query, TS3Config config) {
		sendQueue = new LinkedBlockingQueue<>();
		ConnectionHandler handler = config.getReconnectStrategy().create(null);
		if (config.getFloodRate() == TS3Query.FloodRate.UNLIMITED && handler instanceof DisconnectingConnectionHandler) {
			// Don't wait for the last response before sending more commands
			receiveQueue = new LinkedBlockingQueue<>();
		} else {
			// Wait for the response to the last command to arrive before sending the next one
			receiveQueue = new ArrayBlockingQueue<>(1);
		}

		Socket tmpSocket = null;
		try {
			tmpSocket = new Socket(config.getHost(), config.getQueryPort());
			socket = tmpSocket;
			socket.setTcpNoDelay(true);

			socketReader = new SocketReader(this, query, config);
			socketWriter = new SocketWriter(this, config);
			keepAlive = new KeepAliveThread(socketWriter, query.getAsyncApi());
		} catch (IOException e) {
			// Clean up resources and fail
			if (tmpSocket != null) {
				try {
					tmpSocket.close();
				} catch (IOException ignored) {
				}
			}

			throw new TS3ConnectionFailedException(e);
		}

		// From here on: all resources have been initialized and are non-null
		socketReader.start();
		socketWriter.start();
		keepAlive.start();
	}

	public void continueFrom(QueryIO io) {
		if (io == null) return;
		if (io.sendQueue.isEmpty() && io.receiveQueue.isEmpty()) return;

		// Resend commands which remained unanswered first
		sendQueue.addAll(io.receiveQueue);
		sendQueue.addAll(io.sendQueue);

		io.receiveQueue.clear();
		io.sendQueue.clear();
	}

	public void disconnect() {
		keepAlive.interrupt();
		socketWriter.interrupt();
		socketReader.interrupt();

		try {
			keepAlive.join();
			socketWriter.join();
			socketReader.join();
		} catch (final InterruptedException e) {
			// Restore the interrupt for the caller
			Thread.currentThread().interrupt();
		}

		try {
			socket.close();
		} catch (IOException ignored) {
		}
	}

	public void enqueueCommand(Command command) {
		if (command == null) throw new IllegalArgumentException("Command cannot be null!");
		sendQueue.add(command);
	}

	// Internals for communication with other IO classes

	Socket getSocket() {
		return socket;
	}

	BlockingQueue<Command> getSendQueue() {
		return sendQueue;
	}

	BlockingQueue<Command> getReceiveQueue() {
		return receiveQueue;
	}
}
