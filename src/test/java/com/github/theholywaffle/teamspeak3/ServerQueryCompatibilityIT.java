package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import net.schmizz.sshj.common.DisconnectReason;
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.DockerClientFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Isolated("Redirects user.home for disposable SSH trust stores")
class ServerQueryCompatibilityIT {
	enum Target {
		TS3_RAW(false, TS3Query.Protocol.RAW), TS3_SSH(false, TS3Query.Protocol.SSH), TS6_SSH(true, TS3Query.Protocol.SSH);
		final boolean ts6;
		final TS3Query.Protocol protocol;
		Target(boolean ts6, TS3Query.Protocol protocol) { this.ts6 = ts6; this.protocol = protocol; }
	}

	@TempDir static Path home;
	static String originalHome;
	static TeamSpeakContainer ts3;
	static TeamSpeakContainer ts6;

	@BeforeAll static void start() throws Exception {
		// Deliberately fail, never skip, when the integration profile cannot use Docker.
		assertTrue(DockerClientFactory.instance().isDockerAvailable(), "Required Docker integration tests cannot run: start a Docker daemon");
		originalHome = System.getProperty("user.home");
		Files.createDirectory(home.resolve(".ssh"));
		System.setProperty("user.home", home.toString());
		ts3 = new TeamSpeakContainer(false);
		ts6 = new TeamSpeakContainer(true);
		ts3.start();
		ts6.start();
	}

	@AfterAll static void stop() {
		try { if (ts6 != null) ts6.close(); } finally {
			try { if (ts3 != null) ts3.close(); } finally {
				if (originalHome != null) System.setProperty("user.home", originalHome);
			}
		}
	}

	@ParameterizedTest(name = "{0}: login, version, selection, channel CRUD and server errors")
	@EnumSource(Target.class)
	void smoke(Target target) throws Exception {
		TeamSpeakContainer server = target.ts6 ? ts6 : ts3;
		TS3Query query = new TS3Query(server.config(target.protocol));
		try {
			query.connect();
			assertTrue(query.isConnected());
			TS3Api api = query.getApi();
			var version = api.getVersion();
			assertEquals(target.ts6 ? "6.0.0-beta13.1" : "3.13.8", version.getVersion());
			assertFalse(version.getBuild().isBlank());
			server.record(target.name(), version);
			api.selectVirtualServerById(1);
			assertEquals(1, api.whoAmI().getVirtualServerId());
			String name = "compatibility " + target + " | literal \\s";
			int id = api.createChannel(name, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
			try {
				assertTrue(api.getChannels().stream().anyMatch(channel -> channel.getId() == id && channel.getName().equals(name)));
			} finally { api.deleteChannel(id, true); }
			assertTrue(api.getChannels().stream().noneMatch(channel -> channel.getId() == id));
			var error = assertThrows(TS3CommandFailedException.class, () -> api.selectVirtualServerById(999));
			assertEquals(2816, error.getError().getId()); // invalid virtual server ID
			// A server error must leave response framing usable.
			assertEquals(version.getVersion(), api.getVersion().getVersion());
		} finally {
			query.exit();
			long started = System.nanoTime();
			while (!query.resourcesTerminated() && System.nanoTime() - started < java.util.concurrent.TimeUnit.SECONDS.toNanos(1)) {
				Thread.sleep(5);
			}
			assertTrue(query.resourcesTerminated(), "Library-owned workers must stop after exit");
			assertTrue(Thread.getAllStackTraces().keySet().stream().noneMatch(thread ->
				thread.getName().startsWith("sshj-Reader-") && thread.getName().contains(":" + server.getMappedPort(10022) + "-")),
				"The SSH transport reader must stop after exit");
		}
	}

	private static void verifyConnection(TeamSpeakContainer server, TS3Query.Protocol protocol) throws Exception {
		TS3Query query = new TS3Query(server.config(protocol));
		try {
			query.connect();
			assertFalse(query.getApi().getVersion().getVersion().isBlank());
		} finally {
			query.exit();
			long started = System.nanoTime();
			while (!query.resourcesTerminated() && System.nanoTime() - started < java.util.concurrent.TimeUnit.SECONDS.toNanos(1)) {
				Thread.sleep(5);
			}
			assertTrue(query.resourcesTerminated(), "Library-owned workers must stop after exit");
			assertTrue(Thread.getAllStackTraces().keySet().stream().noneMatch(thread ->
				thread.getName().startsWith("sshj-Reader-") && thread.getName().contains(":" + server.getMappedPort(10022) + "-")),
				"The SSH transport reader must stop after exit");
		}
	}

	private static void openSSH(TS3Config config) throws Exception {
		try (var query = new TS3Query(config)) {
			var connection = new Connection(query, config, CommandQueue.newConnectQueue(query));
			try { connection.open(); } finally { connection.disconnect(); }
		}
	}

	@ParameterizedTest(name = "{0}: SSH authentication and host-key regressions")
	@EnumSource(value = Target.class, names = {"TS3_SSH", "TS6_SSH"})
	void sshSecurity(Target target) throws Exception {
		TeamSpeakContainer server = target.ts6 ? ts6 : ts3;
		assertTrue(server.sshBanner.startsWith("SSH-2.0-"));
		assertNotNull(server.algorithms);
		Path trust = home.resolve(".ssh/known_ts3_hosts");
		Files.deleteIfExists(trust);
		verifyConnection(server, target.protocol);
		byte[] trusted = Files.readAllBytes(trust);
		assertTrue(trusted.length > 0, "First connection must persist host trust in the temporary home");
		verifyConnection(server, target.protocol);
		assertArrayEquals(trusted, Files.readAllBytes(trust), "Known key must be reused");
		assertTrue(new OpenSSHKnownHosts(trust.toFile()).verify(server.getHost(), server.getMappedPort(10022), server.hostKey));
		var authentication = assertThrows(TS3ConnectionFailedException.class, () -> openSSH(server.config(target.protocol)
				.setLoginCredentials("serveradmin", "incorrect-test-password")));
		assertEquals("Invalid query username or password", authentication.getMessage());
		var generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		var wrongKey = generator.generateKeyPair().getPublic();
		var known = new OpenSSHKnownHosts(trust.toFile());
		known.entries().clear();
		known.entries().add(new OpenSSHKnownHosts.HostEntry(null,
				"[" + server.getHost() + "]:" + server.getMappedPort(10022), KeyType.RSA, wrongKey));
		known.write();
		try {
			var rejection = assertThrows(TransportException.class, () -> openSSH(server.config(target.protocol)));
			assertEquals(DisconnectReason.HOST_KEY_NOT_VERIFIABLE, rejection.getDisconnectReason());
		} finally { Files.delete(trust); }
	}
}
