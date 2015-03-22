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
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.commands.CQuit;
import com.github.theholywaffle.teamspeak3.commands.Command;
import com.github.theholywaffle.teamspeak3.log.LogHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Handler;
import java.util.logging.Logger;

public class TS3Query {

	public enum FloodRate {
		DEFAULT(350),
		UNLIMITED(0);

		private final int ms;

		FloodRate(int ms) {
			this.ms = ms;
		}

		public int getMs() {
			return ms;
		}
	}

	public static final Logger log = Logger.getLogger(TS3Query.class.getName());
	private final EventManager eventManager = new EventManager();
	private final TS3Config config;
	private Socket socket;
	private PrintStream out;
	private BufferedReader in;
	private SocketReader socketReader;
	private SocketWriter socketWriter;
	private KeepAliveThread keepAlive;
	private ConcurrentLinkedQueue<Command> commandList = new ConcurrentLinkedQueue<>();
	private TS3Api api;
	private TS3ApiAsync asyncApi;

	public TS3Query(TS3Config config) {
		log.setUseParentHandlers(false);
		log.addHandler(new LogHandler(config.getDebugToFile()));
		log.setLevel(config.getDebugLevel());
		this.config = config;
	}

	public TS3Query connect() {
		// exit();
		try {
			socket = new Socket(config.getHost(), config.getQueryPort());
			if (socket.isConnected()) {
				out = new PrintStream(socket.getOutputStream(), true, "UTF-8");
				in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
				socketReader = new SocketReader(this);
				socketReader.start();
				socketWriter = new SocketWriter(this, config.getFloodRate().getMs());
				socketWriter.start();
				keepAlive = new KeepAliveThread(this, socketWriter);
				keepAlive.start();
			}
		} catch (final IOException e) {
			throw new TS3ConnectionFailedException(e);
		}

		// Executing config object
		final TS3Api api = getApi();
		if (config.getUsername() != null && config.getPassword() != null) {
			api.login(config.getUsername(), config.getPassword());
		}
		return this;
	}

	public Socket getSocket() {
		return socket;
	}

	public PrintStream getOut() {
		return out;
	}

	public BufferedReader getIn() {
		return in;
	}

	public boolean doCommand(Command c) {
		final Object signal = new Object();
		final Callback callback = new Callback() {
			@Override
			public void handle() {
				synchronized (signal) {
					signal.notifyAll();
				}
			}
		};
		socketReader.registerCallback(c, callback);

		final long end = System.currentTimeMillis() + config.getCommandTimeout();
		commandList.offer(c);

		boolean interrupted = false;
		while (!c.isAnswered() && System.currentTimeMillis() < end) {
			try {
				synchronized (signal) {
					signal.wait(end - System.currentTimeMillis());
				}
			} catch (final InterruptedException e) {
				interrupted = true;
			}
		}
		if (interrupted) {
			// Restore the interrupt
			Thread.currentThread().interrupt();
		}

		if (!c.isAnswered()) {
			log.severe("Command " + c.getName() + " was not answered in time.");
			return false;
		}

		return c.getError().isSuccessful();
	}

	public void doCommandAsync(final Command c) {
		doCommandAsync(c, null);
	}

	public void doCommandAsync(final Command c, final Callback callback) {
		if (callback != null) {
			socketReader.registerCallback(c, callback);
		}
		commandList.offer(c);
	}

	/**
	 * Removes and closes all used resources to the teamspeak server.
	 */
	public void exit() {
		// Send a quit command synchronously
		// This will guarantee that all previously sent commands have been processed
		doCommand(new CQuit());

		if (keepAlive != null) {
			keepAlive.interrupt();
		}
		if (socketWriter != null) {
			socketWriter.interrupt();
		}
		if (socketReader != null) {
			socketReader.interrupt();
		}

		if (out != null) {
			out.close();
		}
		if (in != null) {
			try {
				in.close();
			} catch (IOException ignored) {
			}
		}
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException ignored) {
			}
		}

		try {
			if (keepAlive != null) {
				keepAlive.join();
			}
			if (socketWriter != null) {
				socketWriter.join();
			}
			if (socketReader != null) {
				socketReader.join();
			}
		} catch (final InterruptedException e) {
			// Restore the interrupt for the caller
			Thread.currentThread().interrupt();
		}

		commandList.clear();
		commandList = null;
		for (final Handler lh : log.getHandlers()) {
			log.removeHandler(lh);
		}
	}

	public ConcurrentLinkedQueue<Command> getCommandList() {
		return commandList;
	}

	public EventManager getEventManager() {
		return eventManager;
	}

	public TS3Api getApi() {
		if (api == null) {
			api = new TS3Api(this);
		}
		return api;
	}

	public TS3ApiAsync getAsyncApi() {
		if (asyncApi == null) {
			asyncApi = new TS3ApiAsync(this);
		}
		return asyncApi;
	}
}
