package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.event.ChannelCreateEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelDeletedEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.reconnect.SessionConfiguration;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import net.schmizz.sshj.common.DisconnectReason;
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.DockerClientFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ServerQueryCompatibilityIT {
	enum Target {
		TS3_RAW(false, TS3Query.Protocol.RAW), TS3_SSH(false, TS3Query.Protocol.SSH), TS6_SSH(true, TS3Query.Protocol.SSH);
		final boolean ts6;
		final TS3Query.Protocol protocol;
		Target(boolean ts6, TS3Query.Protocol protocol) { this.ts6 = ts6; this.protocol = protocol; }
	}

	@TempDir static Path home;
	static TeamSpeakContainer ts3;
	static TeamSpeakContainer ts6;

	@BeforeAll static void start() throws Exception {
		// Deliberately fail, never skip, when the integration profile cannot use Docker.
		assertTrue(DockerClientFactory.instance().isDockerAvailable(), "Required Docker integration tests cannot run: start a Docker daemon");
		ts3 = new TeamSpeakContainer(false);
		ts6 = new TeamSpeakContainer(true);
		ts3.start();
		ts6.start();
	}

	@AfterAll static void stop() {
		try { if (ts6 != null) ts6.close(); } finally {
			if (ts3 != null) ts3.close();
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
		} finally { exitAndAssertTerminated(query, server); }
	}

	@ParameterizedTest(name = "{0}: reconnect restores authentication, target, nickname and subscriptions")
	@EnumSource(Target.class)
	@Timeout(30)
	void reconnectSession(Target target) throws Exception {
		TeamSpeakContainer server = target.ts6 ? ts6 : ts3;
		try (var proxy = new DisconnectingProxy(server.getHost(), server.getMappedPort(target.protocol == TS3Query.Protocol.RAW ? 10011 : 10022))) {
			var connected = new AtomicInteger();
			var restored = new CountDownLatch(1);
			var event = new CountDownLatch(1);
			var session = SessionConfiguration.forServer(1).withNickname("reconnect-" + target)
				.withSubscription(TS3EventType.CHANNEL, 0);
			var config = server.config(target.protocol).setHost("127.0.0.1").setQueryPort(proxy.port())
				.setSessionConfiguration(session).setReconnectStrategy(ReconnectStrategy.constantBackoff(50).withMaxAttempts(3))
				.setHandshakeTimeout(Duration.ofSeconds(10)).setConnectionHandler(new ConnectionHandler() {
					@Override public void onConnect(TS3Api api) { if (connected.incrementAndGet() == 2) restored.countDown(); }
					@Override public void onDisconnect(TS3Query query) { }
				});
			var factory = config.getTransportFactory();
			var transports = new java.util.concurrent.CopyOnWriteArrayList<QueryTransport>();
			config.setTransportFactory(new QueryTransportFactory() {
				@Override public QueryTransport create(QueryConfig settings) {
					var transport = factory.create(settings);
					transports.add(transport);
					return transport;
				}
				@Override public boolean supports(ServerType serverType, TS3Query.Protocol protocol) {
					return factory.supports(serverType, protocol);
				}
				@Override public boolean authenticates() { return factory.authenticates(); }
			});
			try (var query = new TS3Query(config)) {
				query.getApi().addTS3Listeners(new TS3EventAdapter() {
					@Override public void onChannelCreate(ChannelCreateEvent notification) { event.countDown(); }
				});
				query.connect();
				var before = query.getApi().whoAmI();
				assertEquals(1, before.getVirtualServerId()); assertEquals(session.nickname(), before.getNickname());
				proxy.drop();
				assertTrue(restored.await(15, TimeUnit.SECONDS), "Configured session must reconnect");
				assertEquals(2, transports.size());
				assertNotSame(transports.get(0), transports.get(1));
				var after = query.getApi().whoAmI();
				assertEquals(1, after.getVirtualServerId()); assertEquals(session.nickname(), after.getNickname());
				// This privileged operation proves authentication was restored; its event proves the subscription.
				int channel = query.getApi().createChannel("reconnect-event-" + target, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
				try { assertTrue(event.await(5, TimeUnit.SECONDS), "Channel notifications must be restored"); }
				finally { query.getApi().deleteChannel(channel, true); }
				query.exit();
				long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
				while (!query.resourcesTerminated() && System.nanoTime() < until) Thread.sleep(5);
				assertTrue(query.resourcesTerminated());
			}
		}
	}

	@ParameterizedTest(name = "{0}: overlapping server/channel subscriptions preserve real join/move/leave frames")
	@EnumSource(Target.class)
	@Timeout(30)
	void notificationFrames(Target target) throws Exception {
		TeamSpeakContainer server = target.ts6 ? ts6 : ts3;
		var rows = new java.util.concurrent.CopyOnWriteArrayList<String>();
		var joined = new CountDownLatch(1);
		var moved = new CountDownLatch(1);
		var left = new CountDownLatch(1);
		var actorId = new AtomicInteger(-1);
		var deleted = new CountDownLatch(1);
		try (var observer = new TS3Query(server.config(target.protocol));
			 var actor = new TS3Query(server.config(target.protocol))) {
			observer.connect();
			observer.getApi().selectVirtualServerById(1);
			observer.getApi().registerEvent(TS3EventType.SERVER);
			observer.getApi().registerEvent(TS3EventType.CHANNEL, 0);
			try (var subscription = observer.subscribe(new TS3EventAdapter() {
				@Override public void onClientJoin(com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent e) {
					assertTrue(e.getClientId() > 0); rows.add("join " + e); joined.countDown();
				}
				@Override public void onClientMoved(com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent e) {
					if (e.getClientId() == actorId.get()) { rows.add("move " + e); moved.countDown(); }
				}
				@Override public void onChannelDeleted(ChannelDeletedEvent e) { deleted.countDown(); }
				@Override public void onClientLeave(com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent e) {
					if (e.getClientId() == actorId.get()) { rows.add("leave " + e); left.countDown(); }
				}
			})) {
				actor.connect(); actor.getApi().selectVirtualServerById(1);
				actorId.set(actor.getApi().whoAmI().getId());
				assertTrue(joined.await(5, TimeUnit.SECONDS));
				int channel = observer.getApi().createChannel("notification-probe-" + target,
					Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
				try {
					observer.getApi().moveClient(actorId.get(), channel);
					assertTrue(moved.await(5, TimeUnit.SECONDS));
					actor.exit(); assertTrue(left.await(5, TimeUnit.SECONDS));
				} finally { observer.getApi().deleteChannel(channel, true); }
				// Deletion callback fences preceding join/move/leave callbacks on this registration.
				assertTrue(deleted.await(5, TimeUnit.SECONDS));
				assertEquals(1, rows.stream().filter(row -> row.startsWith("join ")).count());
				assertEquals(1, rows.stream().filter(row -> row.startsWith("move ")).count());
				assertEquals(1, rows.stream().filter(row -> row.startsWith("leave ")).count());
				assertFalse(observer.getApi().getVersion().getVersion().isBlank());
				observer.exit();
				assertEquals(0, observer.getEventStatistics().malformedNotifications());
				assertEquals(0, observer.getEventStatistics().listenerFailures());
				assertEquals(0, observer.getEventStatistics().droppedEvents());
				Path evidence = Path.of("target", "compatibility", target + "-notifications.txt");
				Files.createDirectories(evidence.getParent());
				Files.write(evidence, rows);
			}
		}
	}

	@ParameterizedTest(name = "{0}: channel domain with independent actor, live help and field inventory")
	@EnumSource(Target.class)
	@Timeout(60)
	void channelDomain(Target target) throws Exception {
		TeamSpeakContainer server = target.ts6 ? ts6 : ts3;
		var evidence = new java.util.ArrayList<String>();
		var created = new java.util.concurrent.LinkedBlockingQueue<ChannelCreateEvent>();
		var edited = new java.util.concurrent.LinkedBlockingQueue<ChannelEditedEvent>();
		var moved = new java.util.concurrent.LinkedBlockingQueue<ChannelMovedEvent>();
		var deleted = new java.util.concurrent.LinkedBlockingQueue<ChannelDeletedEvent>();
		try (var observer = new TS3Query(server.config(target.protocol));
			 var actor = new TS3Query(server.config(target.protocol))) {
			observer.connect(); actor.connect();
			var api = actor.getApi();
			api.selectVirtualServerById(1); observer.getApi().selectVirtualServerById(1);
			server.record(target.name(), api.getVersion());
			int actorId = api.whoAmI().getId();
			assertNotEquals(actorId, observer.getApi().whoAmI().getId());
			observer.getApi().registerEvent(TS3EventType.CHANNEL, 0);
			try (var subscription = observer.subscribe(new TS3EventAdapter() {
				@Override public void onChannelCreate(ChannelCreateEvent e) { created.add(e); }
				@Override public void onChannelEdit(ChannelEditedEvent e) { edited.add(e); }
				@Override public void onChannelMoved(ChannelMovedEvent e) { moved.add(e); }
				@Override public void onChannelDeleted(ChannelDeletedEvent e) { deleted.add(e); }
			})) {
				for (String command : java.util.List.of("channelcreate", "channellist", "channelinfo", "channelfind", "channeledit", "channelmove", "channeldelete")) {
					String help = api.executeRawCommand("help " + command).getRawResponse();
					assertTrue(help.startsWith("Usage: " + command + " "), "Live help must describe " + command);
					evidence.add("help " + command + "\n" + help);
				}
				try (var channels = new ChannelCleanup(api)) {
					String name = "domain-" + target + " 日本語 | literal \\s";
					int parent = channels.track(api.createChannel("parent-" + target, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1")));
					var parentEvent = created.poll(5, TimeUnit.SECONDS);
					assertNotNull(parentEvent); assertEquals(parent, parentEvent.getChannelId());
					int channel = channels.track(api.createChannel(name, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1", ChannelProperty.CHANNEL_TOPIC, "initial topic")));
					var create = created.poll(5, TimeUnit.SECONDS);
					assertNotNull(create); assertEquals(channel, create.getChannelId());
					assertEquals(name, create.get("channel_name"));
					assertEquals(target.ts6, create.getMap().containsKey("channel_unique_identifier"));
					assertEquals(actorId, create.getInvokerId());
					evidence.add("create event " + new java.util.TreeMap<>(create.getMap()));
					var list = api.getChannels().stream().filter(c -> c.getId() == channel).findFirst().orElseThrow();
					assertEquals(name, list.getName()); assertTrue(list.isPermanent()); assertEquals("initial topic", list.getTopic());
					evidence.add("channellist " + new java.util.TreeMap<>(list.getMap()));
					assertTrue(api.getChannelsByName("domain-" + target).stream().anyMatch(c -> c.getId() == channel));
					String renamed = name + " renamed";
					String topic = "updated | literal \\n";
					api.editChannel(channel, Map.of(ChannelProperty.CHANNEL_NAME, renamed, ChannelProperty.CHANNEL_TOPIC, topic,
						ChannelProperty.CHANNEL_DESCRIPTION, "description 日本語 | literal \\s"));
					var edit = edited.poll(5, TimeUnit.SECONDS);
					assertNotNull(edit); assertEquals(channel, edit.getChannelId());
					assertEquals(actorId, edit.getInvokerId());
					assertEquals(renamed, edit.get("channel_name")); assertEquals(topic, edit.get("channel_topic"));
					evidence.add("edit event " + new java.util.TreeMap<>(edit.getMap()));
					var info = api.getChannelInfo(channel);
					assertEquals(renamed, info.getName()); assertEquals(topic, info.getTopic());
					assertEquals("description 日本語 | literal \\s", info.getDescription());
					assertNotNull(info.getUniqueIdentifier());
					assertEquals(target.ts6, info.getMap().containsKey("channel_storage_quota"));
					if (target.ts6) assertEquals("4294967295", info.get("channel_storage_quota"));
					evidence.add("channelinfo " + new java.util.TreeMap<>(info.getMap()));
					api.moveChannel(channel, parent, 0);
					var move = moved.poll(5, TimeUnit.SECONDS);
					assertNotNull(move); evidence.add("move event " + new java.util.TreeMap<>(move.getMap()));
					assertEquals(channel, move.getChannelId());
					assertEquals(actorId, move.getInvokerId());
					assertEquals(parent, move.getChannelParentId()); assertEquals(0, move.getChannelOrder());
					assertEquals(parent, api.getChannelInfo(channel).getParentChannelId());
					channels.delete(channel);
					var delete = deleted.poll(5, TimeUnit.SECONDS);
					assertNotNull(delete); assertEquals(channel, delete.getChannelId());
					assertEquals(actorId, delete.getInvokerId());
					evidence.add("delete event " + new java.util.TreeMap<>(delete.getMap()));
					assertTrue(api.getChannels().stream().noneMatch(c -> c.getId() == channel));
					var error = assertThrows(TS3CommandFailedException.class, () -> api.getChannelInfo(channel));
					assertEquals(768, error.getError().getId());
					assertEquals("invalid channelID", error.getError().getMessage());
					evidence.add("deleted channel error " + new java.util.TreeMap<>(error.getError().getMap()));
					assertEquals(api.getVersion().getVersion(), observer.getApi().getVersion().getVersion());
				}
			}
			observer.exit(); actor.exit();
			assertEquals(0, observer.getEventStatistics().malformedNotifications());
			assertEquals(0, observer.getEventStatistics().listenerFailures());
			assertEquals(0, observer.getEventStatistics().droppedEvents());
		} finally {
			Path path = Path.of("target", "compatibility", target + "-channels.txt");
			Files.createDirectories(path.getParent()); Files.write(path, evidence);
		}
	}

	private static final class ChannelCleanup implements AutoCloseable {
		private final TS3Api api;
		private final java.util.List<Integer> ids = new java.util.ArrayList<>();

		ChannelCleanup(TS3Api api) { this.api = api; }

		int track(int id) { ids.add(id); return id; }

		void delete(int id) {
			api.deleteChannel(id, true);
			ids.remove(Integer.valueOf(id));
		}

		@Override public void close() {
			RuntimeException failure = null;
			for (int i = ids.size() - 1; i >= 0; --i) {
				try { api.deleteChannel(ids.get(i), true); }
				catch (RuntimeException error) {
					if (failure == null) failure = error;
					else failure.addSuppressed(error);
				}
			}
			if (failure != null) throw failure;
		}
	}

	private static void verifyConnection(TeamSpeakContainer server, TS3Query.Protocol protocol) throws Exception {
		TS3Query query = new TS3Query(server.config(protocol));
		try {
			query.connect();
			assertFalse(query.getApi().getVersion().getVersion().isBlank());
		} finally { exitAndAssertTerminated(query, server); }
	}

	private static void exitAndAssertTerminated(TS3Query query, TeamSpeakContainer server) throws InterruptedException {
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

	private static void openSSH(TS3Config config) throws Exception {
		try (var query = new TS3Query(config)) {
			var connection = new Connection(query, query.getConfig(), CommandQueue.newConnectQueue(query));
			try { connection.open(); } finally { connection.disconnect(); }
		}
	}

	@ParameterizedTest(name = "{0}: transport shutdown excludes the connecting caller")
	@EnumSource(value = Target.class, names = {"TS3_SSH", "TS6_SSH"})
	void sshTransportShutdownDoesNotJoinConnectingCaller(Target target) throws Exception {
		TeamSpeakContainer server = target.ts6 ? ts6 : ts3;
		var config = server.config(target.protocol).build();
		var transport = config.getTransportFactory().create(config);
		try (transport) { transport.connect(() -> {}); }
		transport.awaitTermination(Duration.ofSeconds(1));
		assertTrue(transport.isTerminated(), "Transport shutdown must exclude the still-running connecting caller");
	}

	@ParameterizedTest(name = "{0}: SSH authentication and host-key regressions")
	@EnumSource(value = Target.class, names = {"TS3_SSH", "TS6_SSH"})
	void sshSecurity(Target target) throws Exception {
		TeamSpeakContainer server = target.ts6 ? ts6 : ts3;
		assertTrue(server.sshBanner.startsWith("SSH-2.0-"));
		assertNotNull(server.algorithms);
		Path trust = home.resolve(target + "-known_hosts");
		var tofu = server.config(target.protocol).setSshHostKeyPolicy(SshHostKeyPolicy.trustOnFirstUse(trust));
		Files.deleteIfExists(trust);
		var unknown = assertThrows(TransportException.class, () -> openSSH(server.config(target.protocol)
			.setSshHostKeyPolicy(SshHostKeyPolicy.knownHosts(trust))));
		assertEquals(DisconnectReason.HOST_KEY_NOT_VERIFIABLE, unknown.getDisconnectReason());
		assertFalse(Files.exists(trust), "Strict verification must not persist unknown trust");
		openSSH(tofu);
		byte[] trusted = Files.readAllBytes(trust);
		assertTrue(trusted.length > 0, "First connection must persist host trust in the explicit temporary file");
		openSSH(tofu);
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
			var rejection = assertThrows(TransportException.class, () -> openSSH(server.config(target.protocol).setSshHostKeyPolicy(SshHostKeyPolicy.knownHosts(trust))));
			assertEquals(DisconnectReason.HOST_KEY_NOT_VERIFIABLE, rejection.getDisconnectReason());
		} finally { Files.delete(trust); }
	}
}
