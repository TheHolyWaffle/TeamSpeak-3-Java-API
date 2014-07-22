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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Handler;
import java.util.logging.Logger;

import com.github.theholywaffle.teamspeak3.api.Callback;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.commands.Command;
import com.github.theholywaffle.teamspeak3.log.LogHandler;

public class TS3Query {

	private Socket socket;
	private PrintStream out;
	private BufferedReader in;
	private SocketReader socketReader;
	private SocketWriter socketWriter;
	private KeepAliveThread keepAlive;

	private ConcurrentLinkedQueue<Command> commandList = new ConcurrentLinkedQueue<>();
	private final EventManager eventManager = new EventManager();

	private TS3Api api;
	private final TS3Config config;

	public static final Logger log = Logger.getLogger(TS3Query.class.getName());

	public enum FloodRate {
		DEFAULT(350),
		UNLIMITED(0);

		private int ms;

		FloodRate(int ms) {
			this.ms = ms;
		}

		public int getMs() {
			return ms;
		}
	}

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
				in = new BufferedReader(new InputStreamReader(
						socket.getInputStream(), "UTF-8"));
				socketReader = new SocketReader(this);
				socketReader.start();
				socketWriter = new SocketWriter(this, config.getFloodRate()
						.getMs());
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
		commandList.offer(c);
		final long start = System.currentTimeMillis();
		while (!c.isAnswered() && System.currentTimeMillis() - start < 4_000) {
			try {
				Thread.sleep(50);
			} catch (final InterruptedException e) {
				throw new TS3CommandFailedException(e);
			}
		}
		if (!c.isAnswered()) {
			log.severe(("Command " + c.getName() + " is not answered in time."));
			commandList.remove(c);
			return false;
		}
		return true;
	}

	public void doCommandAsync(final Command c) {
		doCommandAsync(c, null);
	}

	public void doCommandAsync(final Command c, final Callback callback) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				doCommand(c);
				if (callback != null) {
					callback.handle();
				}
			}
		}).start();
	}

	/**
	 * Removes and closes all used resources to the teamspeak server.
	 */
	public void exit() {
		if (out != null) {
			out.close();
		}
		if (in != null) {
			try {
				in.close();
			} catch (final IOException ignored) {
			}
		}
		if (socket != null) {
			try {
				socket.close();
			} catch (final IOException ignored) {
			}
		}
		try {
			if (socketReader != null) {
				socketReader.finish();
				socketReader.join();
			}
			if (socketWriter != null) {
				socketWriter.finish();
				socketWriter.join();
			}
			if (keepAlive != null) {
				keepAlive.finish();
				keepAlive.join();
			}
		} catch (final InterruptedException e) {
			e.printStackTrace();
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

}
