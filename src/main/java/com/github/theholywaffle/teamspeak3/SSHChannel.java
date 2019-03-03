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
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import net.schmizz.sshj.userauth.UserAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;

class SSHChannel implements IOChannel {

	private static final Logger log = LoggerFactory.getLogger(SSHChannel.class);
	private static final String KNOWN_HOSTS_FILE_NAME = "known_ts3_hosts";

	private final SSHClient client;
	private final Session session;

	SSHChannel(TS3Config config) throws IOException {
		if (!config.hasLoginCredentials()) {
			throw new TS3ConnectionFailedException("Anonymous queries are not supported when using SSH.\n" +
					"\t\tYou must specify a query username and password using TS3Config#setLoginCredentials.");
		}

		try {
			client = new SSHClient();
			File knownHostsFile = new File(OpenSSHKnownHosts.detectSSHDir(), KNOWN_HOSTS_FILE_NAME);
			client.addHostKeyVerifier(new AutoAddKnownHosts(knownHostsFile));
			client.setConnectTimeout(config.getCommandTimeout());
			client.setTimeout(config.getCommandTimeout());
			client.setRemoteCharset(StandardCharsets.UTF_8);

			client.connect(config.getHost(), config.getQueryPort());
			client.authPassword(config.getUsername(), config.getPassword());
			session = client.startSession();
			session.startShell();
		} catch (UserAuthException uae) {
			close();
			throw new TS3ConnectionFailedException("Invalid query username or password");
		} catch (IOException ioe) {
			close();
			throw ioe;
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
		if (session != null) session.close();
		if (client != null) client.close();
	}

	private static class AutoAddKnownHosts extends OpenSSHKnownHosts {

		public AutoAddKnownHosts(File khFile) throws IOException {
			super(khFile);
		}

		@Override
		protected boolean hostKeyUnverifiableAction(String hostname, PublicKey key) {
			try {
				entries().add(new HostEntry(null, hostname, KeyType.fromKey(key), key));
				write();

				return true;
			} catch (IOException ioe) {
				throw new RuntimeException("Could not write host keys to file", ioe);
			}
		}

		@Override
		protected boolean hostKeyChangedAction(String hostname, PublicKey key) {
			Logger log = SSHChannel.log; // OpenSSHKnownHosts also has a "log" field...
			log.error("The host key for {} has changed!", hostname);
			log.error("This could be because someone is eavesdropping on you (man-in-the-middle attack).");
			log.error("It could also be that the host key has changed, e.g. because the TS3 server was re-installed .");
			log.error("If you trust that the new host key is genuine, correct or remove the entry" +
					" for {} in your known hosts file ({}).", hostname, khFile);
			return false;
		}
	}
}
