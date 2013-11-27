package com.github.theholywaffle.teamspeak3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.github.theholywaffle.teamspeak3.api.Callback;
import com.github.theholywaffle.teamspeak3.commands.Command;

public class TS3Query {

	public static final int DEFAULT_PORT = 10011;

	private final String host;
	private final int port;

	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	private SocketReader socketReader;
	private SocketWriter socketWriter;

	private CommandList commandList = new CommandList();
	private EventManager eventManager;

	private int floodRate;
	private static boolean debug;
	private TS3Api bot;

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

	public TS3Query(String host) {
		this(host, DEFAULT_PORT, FloodRate.DEFAULT);
	}

	public TS3Query(String host, int port) {
		this(host, port, FloodRate.DEFAULT);
	}

	public TS3Query(String host, FloodRate floodRate) {
		this(host, DEFAULT_PORT, FloodRate.DEFAULT);
	}

	public TS3Query(String host, int port, FloodRate floodRate) {
		this(host, port, floodRate.getMs());
	}

	public TS3Query(String host, int port, int floodRate) {
		this.host = host;
		this.port = port;
		this.floodRate = floodRate;
		this.eventManager = new EventManager();
	}

	public TS3Query connect() {
		exit();
		try {
			socket = new Socket(host, port);
			if (socket.isConnected()) {
				out = new PrintWriter(socket.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				socketReader = new SocketReader(this);
				socketReader.start();
				socketWriter = new SocketWriter(this, floodRate);
				socketWriter.start();
				new KeepAliveThread(this).start();
			}

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
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
		commandList.add(c);
		long start = System.currentTimeMillis();
		while (!c.isAnswered() && System.currentTimeMillis() - start < 5_000) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		commandList.remove(c);
		if (!c.isAnswered()) {
			TS3Query.log("Command " + c.getName() + " is not answered in time.");
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
		commandList.clear();
	}
	
	public static void log(String msg) {
		log(msg, false);	
	}

	public static void log(String msg, boolean forced) {
		if(debug || forced){
			System.out.println("[DEBUG] " + msg);
		}		
	}
	
	public TS3Query debug(){
		debug = true;
		return this;
	}

	public CommandList getCommandList() {
		return commandList;
	}
	
	public EventManager getEventManager(){
		return eventManager;
	}
	
	public TS3Api getApi(){
		if(bot == null){
			bot = new TS3Api(this);
		}
		return bot;
	}

}
