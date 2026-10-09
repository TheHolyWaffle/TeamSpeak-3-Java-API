package com.github.theholywaffle.teamspeak3;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A loopback peer with scripted writes and bounded, observable completion. */
final class FakeServerQuery implements AutoCloseable {
	@FunctionalInterface
	interface Script { void run(FakeServerQuery peer) throws Exception; }

	private final ServerSocket listener;
	private final CompletableFuture<Void> finished = new CompletableFuture<>();
	private final Thread worker;
	private volatile Socket socket;
	private BufferedReader commands;

	FakeServerQuery(Script script) throws Exception {
		listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
		listener.setSoTimeout(5000);
		worker = Thread.ofVirtual().start(() -> {
			try (Socket accepted = listener.accept()) {
				socket = accepted;
				accepted.setSoTimeout(5000);
				accepted.setTcpNoDelay(true);
				commands = new BufferedReader(new InputStreamReader(accepted.getInputStream(), StandardCharsets.UTF_8));
				script.run(this);
				finished.complete(null);
			} catch (Throwable failure) {
				finished.completeExceptionally(failure);
			}
		});
	}

	int port() { return listener.getLocalPort(); }

	void expect(String command) throws Exception {
		assertEquals(command, commands.readLine());
	}

	void write(String text) throws Exception {
		write(text.getBytes(StandardCharsets.UTF_8));
	}

	void write(byte[] bytes) throws Exception {
		socket.getOutputStream().write(bytes);
		socket.getOutputStream().flush();
	}

	void finishQuit() throws Exception {
		expect("quit");
		write("error id=0 msg=ok\n");
	}

	void await() throws Exception { finished.get(5, TimeUnit.SECONDS); }

	@Override
	public void close() throws Exception {
		listener.close();
		if (socket != null) socket.close();
		worker.join(5000);
		if (worker.isAlive()) throw new AssertionError("Fake peer did not stop");
	}
}
