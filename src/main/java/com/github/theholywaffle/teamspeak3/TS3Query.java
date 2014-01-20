/*******************************************************************************
 * Copyright (c) 2014 Bert De Geyter (https://github.com/TheHolyWaffle).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Bert De Geyter (https://github.com/TheHolyWaffle) - initial API and implementation
 ******************************************************************************/
package com.github.theholywaffle.teamspeak3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import com.github.theholywaffle.teamspeak3.api.Callback;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.commands.Command;
import com.github.theholywaffle.teamspeak3.log.LogHandler;

public class TS3Query {

	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	private SocketReader socketReader;
	private SocketWriter socketWriter;

	private ConcurrentLinkedQueue<Command> commandList = new ConcurrentLinkedQueue<>();
	private EventManager eventManager = new EventManager();

	private TS3Api bot;
	private TS3Config config;

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
		exit();
		try {
			socket = new Socket(config.getHost(), config.getQueryPort());
			if (socket.isConnected()) {
				out = new PrintWriter(socket.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				socketReader = new SocketReader(this);
				socketReader.start();
				socketWriter = new SocketWriter(this, config.getFloodRate()
						.getMs());
				socketWriter.start();
				new KeepAliveThread(this, socketWriter).start();
			}

		} catch (IOException e) {
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

	public PrintWriter getOut() {
		return out;
	}

	public BufferedReader getIn() {
		return in;
	}

	public boolean doCommand(Command c) {
		commandList.offer(c);
		long start = System.currentTimeMillis();
		while (!c.isAnswered() && System.currentTimeMillis() - start < 4_000) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
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

	public void exit() {
		if (out != null) {
			out.close();
			out = null;
		}
		if (in != null) {
			try {
				in.close();
				in = null;
			} catch (IOException ignored) {
			}
		}
		if (socket != null) {
			try {
				socket.shutdownInput();
				socket.shutdownOutput();
				socket.close();
				socket = null;
			} catch (IOException ignored) {
			}
		}
		if (socketReader != null) {
			socketReader.finish();
			socketReader = null;
		}
		if (socketWriter != null) {
			socketWriter.finish();
			socketWriter = null;
		}
		commandList.clear();
	}

	public ConcurrentLinkedQueue<Command> getCommandList() {
		return commandList;
	}

	public EventManager getEventManager() {
		return eventManager;
	}

	public TS3Api getApi() {
		if (bot == null) {
			bot = new TS3Api(this);
		}
		return bot;
	}

}
