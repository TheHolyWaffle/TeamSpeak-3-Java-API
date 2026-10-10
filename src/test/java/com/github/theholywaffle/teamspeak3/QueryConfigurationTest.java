package com.github.theholywaffle.teamspeak3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class QueryConfigurationTest {
	@Test void builderReuseDoesNotChangeSnapshotsOrQueries() {
		var builder = new TS3Config().setHost("first.example").setQueryPort(1234)
			.setLoginCredentials("private-user", "private-password").setCommandCapacity(7);
		var first = builder.build();
		try (var query = new TS3Query(first)) {
			builder.setHost("second.example").setQueryPort(2345).setLoginCredentials("other-user", "other-password")
				.setCommandCapacity(8).setCommandResponseTimeout(Duration.ofSeconds(9));
			var second = builder.build();
			assertSame(first, query.getConfig());
			assertEquals("first.example", first.getHost());
			assertEquals(1234, first.getQueryPort());
			assertEquals("private-password", first.getPassword());
			assertEquals(7, first.getCommandCapacity());
			assertEquals(Duration.ofSeconds(4), first.getCommandResponseTimeout());
			assertEquals("second.example", second.getHost());
			assertEquals(2345, second.getQueryPort());
			assertEquals(8, second.getCommandCapacity());
			for (String diagnostic : new String[]{first.toString(), second.toString(), builder.toString()}) {
				assertFalse(diagnostic.contains("private"));
				assertFalse(diagnostic.contains("other-user"));
				assertFalse(diagnostic.contains("other-password"));
			}
		}
	}

	@Test void defaultsAndInvalidCombinationsAreExplicit() {
		var defaults = new TS3Config().build();
		assertEquals("127.0.0.1", defaults.getHost());
		assertEquals(10011, defaults.getQueryPort());
		assertSame(QueryTransports.RAW, defaults.getTransportFactory());
		assertThrows(IllegalArgumentException.class, () -> new TS3Config().setHost(" ").build());
		assertThrows(IllegalArgumentException.class, () -> new TS3Config().setLoginCredentials("user", null).build());
		assertThrows(IllegalArgumentException.class, () -> new TS3Config().setLoginCredentials("", "password").build());
		assertThrows(IllegalArgumentException.class, () -> new TS3Config().setProtocol(TS3Query.Protocol.SSH).build());
		var mismatch = assertThrows(IllegalArgumentException.class, () -> new TS3Config().setServerType(ServerType.TS6).build());
		assertTrue(mismatch.getMessage().contains("TS6 with RAW"));
		assertThrows(IllegalArgumentException.class, () -> new TS3Config().setTransportFactory(QueryTransports.SSH).build());
	}

	@ParameterizedTest @EnumSource(ServerType.class)
	void bothGenerationsSelectTheSameSshFactory(ServerType server) {
		var config = new TS3Config().setServerType(server).setProtocol(TS3Query.Protocol.SSH)
			.setLoginCredentials("user", "password").build();
		assertSame(QueryTransports.SSH, config.getTransportFactory());
		assertEquals(10022, config.getQueryPort());
		assertTrue(config.getTransportFactory().authenticates());
	}

	@Test void aNewStreamTransportUsesSharedLoginCodecAndExecution() throws Exception {
		var created = new AtomicInteger();
		try (var peer = new FakeServerQuery(server -> {
			server.write("Welcome to a custom stream transport\n");
			server.expect("login user space\\s\\p\\s\\\\s");
			server.write("error id=0 msg=ok\n");
			server.expect("version");
			server.write("version=6.0.0-test build=42 platform=Linux\nerror id=0 msg=ok\n");
			server.finishQuit();
		})) {
			QueryTransportFactory factory = config -> {
				created.incrementAndGet();
				return new QueryTransport() {
					private final Socket socket = new Socket();
					@Override public void connect(Connected connected) throws IOException {
						socket.connect(new InetSocketAddress(config.getHost(), config.getQueryPort()));
						connected.transportConnected();
					}
					@Override public InputStream getInputStream() throws IOException { return socket.getInputStream(); }
					@Override public OutputStream getOutputStream() throws IOException { return socket.getOutputStream(); }
					@Override public void close() throws IOException { socket.close(); }
				};
			};
			var config = new TS3Config().setHost("127.0.0.1").setQueryPort(peer.port()).setTransportFactory(factory)
				.setLoginCredentials("user", "space | \\s").setFloodRate(TS3Query.FloodRate.UNLIMITED).build();
			try (var query = new TS3Query(config)) {
				query.connect();
				assertEquals("6.0.0-test", query.getApi().getVersion().getVersion());
				query.exit();
				assertTrue(query.resourcesTerminated());
				peer.await();
			}
			assertEquals(1, created.get());
		}
	}
}
