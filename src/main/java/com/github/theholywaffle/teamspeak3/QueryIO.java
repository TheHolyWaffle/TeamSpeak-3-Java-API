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

import com.github.theholywaffle.teamspeak3.api.Callback;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.commands.Command;

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueryIO {

	private final Socket socket;
	private final SocketReader socketReader;
	private final SocketWriter socketWriter;
	private final KeepAliveThread keepAlive;

	private final Queue<Command> commandQueue;
	private final Map<Command, Callback> callbackMap;

	QueryIO(TS3Query query, TS3Config config) {
		commandQueue = new ConcurrentLinkedQueue<>();
		callbackMap = Collections.synchronizedMap(new LinkedHashMap<Command, Callback>(16));

		Socket tmpSocket = null;
		try {
			tmpSocket = new Socket(config.getHost(), config.getQueryPort());
			socket = tmpSocket;
			socketReader = new SocketReader(query, this);
			socketWriter = new SocketWriter(this, config.getFloodRate().getMs());
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
		if (io == null || io.commandQueue.isEmpty()) return;

		callbackMap.putAll(io.callbackMap);
		Command lastSent = io.commandQueue.poll();
		commandQueue.add(lastSent.reset());
		commandQueue.addAll(io.commandQueue);

		io.commandQueue.clear();
		io.callbackMap.clear();
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

	public void queueCommand(Command command, Callback callback) {
		if (command == null) throw new NullPointerException("Command cannot be null!");

		if (callback != null) {
			callbackMap.put(command, callback);
		}
		commandQueue.add(command);
	}

	public Socket getSocket() {
		return socket;
	}

	public Map<Command, Callback> getCallbackMap() {
		return callbackMap;
	}

	public Queue<Command> getCommandQueue() {
		return commandQueue;
	}
}
