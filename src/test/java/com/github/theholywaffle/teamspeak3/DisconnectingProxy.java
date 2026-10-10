package com.github.theholywaffle.teamspeak3;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/** Test-only TCP tunnel: drops the active RAW/SSH connection without interpreting encrypted traffic. */
final class DisconnectingProxy implements AutoCloseable {
	private final ServerSocket listener;
	private final Thread acceptor;
	private final List<Thread> workers = new ArrayList<>();
	private final List<Socket> sockets = new ArrayList<>();
	private volatile Socket downstream;
	private volatile Socket upstream;
	private volatile IOException failure;

	DisconnectingProxy(String host, int port) throws IOException {
		listener = new ServerSocket(0, 4, InetAddress.getLoopbackAddress());
		acceptor = Thread.ofVirtual().start(() -> {
			try {
				while (!listener.isClosed()) {
					Socket client = listener.accept();
					Socket server = new Socket();
					server.connect(new InetSocketAddress(host, port), 5000);
					synchronized (this) {
						sockets.add(client); sockets.add(server);
						downstream = client; upstream = server;
						workers.add(pipe(client, server)); workers.add(pipe(server, client));
					}
				}
			} catch (IOException e) { if (!listener.isClosed()) failure = e; }
		});
	}

	private Thread pipe(Socket from, Socket to) {
		return Thread.ofVirtual().start(() -> {
			try { from.getInputStream().transferTo(to.getOutputStream()); }
			catch (IOException expectedWhenDropped) { }
			finally { closeSocket(from); closeSocket(to); }
		});
	}

	int port() { return listener.getLocalPort(); }

	void drop() {
		if (failure != null) throw new AssertionError("TCP proxy failed", failure);
		closeSocket(downstream); closeSocket(upstream);
	}

	private static void closeSocket(Socket socket) {
		if (socket != null) try { socket.close(); } catch (IOException ignored) { }
	}

	@Override public void close() throws Exception {
		listener.close(); acceptor.join(5000);
		for (Socket socket : sockets) closeSocket(socket);
		for (Thread worker : workers) { worker.join(5000); if (worker.isAlive()) throw new AssertionError("Proxy worker did not stop"); }
		if (acceptor.isAlive()) throw new AssertionError("Proxy acceptor did not stop");
		if (failure != null) throw new AssertionError("TCP proxy failed", failure);
	}
}
