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
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.commands.CQuit;
import com.github.theholywaffle.teamspeak3.commands.Command;
import com.github.theholywaffle.teamspeak3.log.LogHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Handler;
import java.util.logging.Level;
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

	private final ExecutorService userThreadPool = Executors.newCachedThreadPool();
	private final EventManager eventManager = new EventManager();
	private final TS3Config config;
	private final ConnectionHandler connectionHandler;

	private QueryIO io;
	private TS3Api api;
	private TS3ApiAsync asyncApi;

	/**
	 * Creates a TS3Query that connects to a TS3 server at
	 * {@code localhost:10011} using default settings.
	 */
	public TS3Query() {
		this(new TS3Config());
	}

	/**
	 * Creates a customized TS3Query that connects to a server
	 * specified by {@code config}.
	 *
	 * @param config
	 * 		configuration for this TS3Query
	 */
	public TS3Query(TS3Config config) {
		log.setUseParentHandlers(false);
		log.addHandler(new LogHandler(config.getDebugToFile()));
		log.setLevel(config.getDebugLevel());
		this.config = config;
		this.connectionHandler = config.getReconnectStrategy().create(config.getConnectionHandler());
	}

	// PUBLIC

	public TS3Query connect() {
		QueryIO oldIO = io;
		if (oldIO != null) {
			oldIO.disconnect();
		}

		try {
			io = new QueryIO(this, config);
		} catch (TS3ConnectionFailedException conFailed) {
			fireDisconnect();
			throw conFailed;
		}

		try {
			connectionHandler.onConnect(this);
		} catch (Throwable t) {
			log.log(Level.SEVERE, "ConnectionHandler threw exception in connect handler", t);
		}
		io.continueFrom(oldIO);

		return this;
	}

	/**
	 * Removes and closes all used resources to the teamspeak server.
	 */
	public void exit() {
		// Send a quit command synchronously
		// This will guarantee that all previously sent commands have been processed
		doCommand(new CQuit());

		io.disconnect();
		userThreadPool.shutdown();
		for (final Handler lh : log.getHandlers()) {
			log.removeHandler(lh);
		}
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

	// INTERNAL

	boolean doCommand(Command c) {
		final long end = System.currentTimeMillis() + config.getCommandTimeout();
		final Object signal = new Object();
		final Callback callback = new Callback() {
			@Override
			public void handle() {
				synchronized (signal) {
					signal.notifyAll();
				}
			}
		};

		io.queueCommand(c, callback);

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

	void doCommandAsync(Command c) {
		doCommandAsync(c, null);
	}

	void doCommandAsync(Command c, Callback callback) {
		io.queueCommand(c, callback);
	}

	void submitUserTask(Runnable task) {
		userThreadPool.submit(task);
	}

	EventManager getEventManager() {
		return eventManager;
	}

	void fireDisconnect() {
		userThreadPool.submit(new Runnable() {
			@Override
			public void run() {
				try {
					connectionHandler.onDisconnect(TS3Query.this);
				} catch (Throwable t) {
					log.log(Level.SEVERE, "ConnectionHandler threw exception in disconnect handler", t);
				}
			}
		});
	}
}
