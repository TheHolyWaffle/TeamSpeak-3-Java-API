package com.github.theholywaffle.teamspeak3;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2019 Bert De Geyter, Roger Baumgartner
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
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.userauth.UserAuthException;

import javax.net.SocketFactory;
import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class SSHChannel implements QueryTransport {

	private final SSHClient client = new SSHClient();
	private final Set<Thread> transportReaders = ConcurrentHashMap.newKeySet();
	private final Socket socket = new Socket() {
		@Override
		public InputStream getInputStream() throws IOException {
			return new FilterInputStream(super.getInputStream()) {
				private void trackReader() {
					// Track socket readers by their I/O contract, including SSH negotiation.
					// SSHJ's transport join event can fire before its reader thread exits.
					transportReaders.add(Thread.currentThread());
				}
				@Override public int read() throws IOException { trackReader(); return in.read(); }
				@Override public int read(byte[] bytes, int offset, int length) throws IOException {
					trackReader(); return in.read(bytes, offset, length);
				}
			};
		}
	};
	private final QueryConfig config;
	private volatile Session session;

	SSHChannel(QueryConfig config) { this.config = config; }

	@Override
	public void connect(QueryTransport.Connected connection) throws IOException {
		if (!config.hasLoginCredentials()) {
			throw new TS3ConnectionFailedException("SSH requires query login credentials");
		}
		socket.connect(new InetSocketAddress(config.getHost(),
			config.getQueryPort()), TS3Config.socketTimeout(config.getConnectTimeout()));
		connection.transportConnected();
		socket.setTcpNoDelay(true);
		client.setSocketFactory(new SocketFactory() {
			@Override public Socket createSocket() { return socket; }
			@Override public Socket createSocket(String host, int port) { return socket; }
			@Override public Socket createSocket(String host, int port, InetAddress local, int localPort) { return socket; }
			@Override public Socket createSocket(InetAddress host, int port) { return socket; }
			@Override public Socket createSocket(InetAddress host, int port, InetAddress local, int localPort) { return socket; }
		});
		client.addHostKeyVerifier(config.getSshHostKeyPolicy().verifier());
		client.setTimeout(TS3Config.socketTimeout(config.getHandshakeTimeout()));
		client.setRemoteCharset(StandardCharsets.UTF_8);
		try {
			client.connect(config.getHost(), config.getQueryPort());
			client.authPassword(config.getUsername(), config.getPassword());
			session = client.startSession();
			session.startShell();
		} catch (UserAuthException e) {
			throw new TS3ConnectionFailedException("Invalid query username or password", e);
		}
	}

	@Override
	public InputStream getInputStream() {
		return session.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() {
		return session.getOutputStream();
	}

	@Override
	public void close() throws IOException {
		// Abort the underlying socket first: SSH channel/client close may write to the peer.
		socket.close();
		client.close();
	}

	@Override
	public void awaitTermination(java.time.Duration timeout) {
		Deadline deadline = new Deadline(timeout);
		for (Thread reader : transportReaders) deadline.join(reader);
		deadline.join(client.getConnection().getKeepAlive());
	}

	@Override
	public boolean isTerminated() {
		return transportReaders.stream().noneMatch(Thread::isAlive) && !client.getConnection().getKeepAlive().isAlive();
	}

}
