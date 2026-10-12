package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.BanRequest;
import com.github.theholywaffle.teamspeak3.api.PermissionGrant;
import com.github.theholywaffle.teamspeak3.api.PermissionTarget;
import com.github.theholywaffle.teamspeak3.api.FileLocation;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.Codec;
import com.github.theholywaffle.teamspeak3.api.event.ChannelDescriptionEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelPasswordChangedEvent;
import com.github.theholywaffle.teamspeak3.api.PrivilegeKeyType;
import com.github.theholywaffle.teamspeak3.api.event.PrivilegeKeyUsedEvent;
import com.github.theholywaffle.teamspeak3.api.wrapper.PrivilegeKey;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent;
import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;
import com.github.theholywaffle.teamspeak3.api.VirtualServerStatus;
import com.github.theholywaffle.teamspeak3.api.event.ServerEditedEvent;
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import com.github.theholywaffle.teamspeak3.api.BPermissionType;
import com.github.theholywaffle.teamspeak3.api.IPermissionType;
import com.github.theholywaffle.teamspeak3.api.PermissionGroupType;
import com.github.theholywaffle.teamspeak3.api.wrapper.Permission;
import com.github.theholywaffle.teamspeak3.api.wrapper.PermissionInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerQueryInfo;
import com.github.theholywaffle.teamspeak3.api.event.ChannelCreateEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelDeletedEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.reconnect.SessionConfiguration;
import com.github.theholywaffle.teamspeak3.commands.CommandEncoding;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
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
		TS3_RAW(TS3Query.Protocol.RAW), TS3_SSH(TS3Query.Protocol.SSH);
		final TS3Query.Protocol protocol;
		Target(TS3Query.Protocol protocol) { this.protocol = protocol; }
	}

	@TempDir static Path home;
	static TeamSpeakContainer ts3;

	@BeforeAll static void start() throws Exception {
		// Deliberately fail, never skip, when the integration profile cannot use Docker.
		assertTrue(DockerClientFactory.instance().isDockerAvailable(), "Required Docker integration tests cannot run: start a Docker daemon");
		ts3 = new TeamSpeakContainer();
		ts3.start();
	}

	@AfterAll static void stop() {
		if (ts3 != null) ts3.close();
	}

	@ParameterizedTest(name = "{0}: login, version, selection, channel CRUD and server errors")
	@EnumSource(Target.class)
	void smoke(Target target) throws Exception {
		TeamSpeakContainer server = ts3;
		TS3Query query = new TS3Query(server.config(target.protocol));
		try {
			query.connect();
			assertTrue(query.isConnected());
			TS3Api api = query.getApi();
			var version = api.instance().version();
			assertEquals("3.13.8", version.version());
			assertFalse(version.build().isBlank());
			server.record(target.name(), version);
			api.selectVirtualServerById(1);
			assertEquals(1, api.whoAmI().getVirtualServerId());
			String name = "compatibility " + target + " | literal \\s";
			int id = api.channels().create(name, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
			try {
				assertTrue(api.channels().list().stream().anyMatch(channel -> channel.getId() == id && channel.getName().equals(name)));
			} finally { api.channels().delete(id, true); }
			assertTrue(api.channels().list().stream().noneMatch(channel -> channel.getId() == id));
			var error = assertThrows(TS3CommandFailedException.class, () -> api.selectVirtualServerById(999));
			assertEquals(2816, error.getError().getId()); // Selection is rejected by this fixture.
			// A server error must leave response framing usable.
			assertEquals(version.version(), api.instance().version().version());
		} finally { exitAndAssertTerminated(query, server); }
	}

	@ParameterizedTest(name = "{0}: reconnect restores authentication, target, nickname and subscriptions")
	@EnumSource(Target.class)
	@Timeout(30)
	void reconnectSession(Target target) throws Exception {
		TeamSpeakContainer server = ts3;
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
				@Override public boolean supports(TS3Query.Protocol protocol) {
					return factory.supports(protocol);
				}
				@Override public boolean authenticates() { return factory.authenticates(); }
			});
			try (var query = new TS3Query(config)) {
				query.subscribe(new TS3EventAdapter() {
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
				int channel = query.getApi().channels().create("reconnect-event-" + target, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
				try { assertTrue(event.await(5, TimeUnit.SECONDS), "Channel notifications must be restored"); }
				finally { query.getApi().channels().delete(channel, true); }
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
		TeamSpeakContainer server = ts3;
		var rows = new java.util.concurrent.CopyOnWriteArrayList<String>();
		var joined = new CountDownLatch(1);
		var moved = new CountDownLatch(1);
		var left = new CountDownLatch(1);
		var actorId = new java.util.concurrent.atomic.AtomicReference<ClientId>();
		var deleted = new CountDownLatch(1);
		try (var observer = new TS3Query(server.config(target.protocol));
			 var actor = new TS3Query(server.config(target.protocol))) {
			observer.connect();
			observer.getApi().selectVirtualServerById(1);
			observer.getApi().events().register(TS3EventType.SERVER);
			observer.getApi().events().register(TS3EventType.CHANNEL, 0);
			try (var subscription = observer.subscribe(new TS3EventAdapter() {
				@Override public void onClientJoin(com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent e) {
					assertTrue(e.getClientId().value() > 0); rows.add("join " + e); joined.countDown();
				}
				@Override public void onClientMoved(com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent e) {
					if (e.getClientId().equals(actorId.get())) { rows.add("move " + e); moved.countDown(); }
				}
				@Override public void onChannelDeleted(ChannelDeletedEvent e) { deleted.countDown(); }
				@Override public void onClientLeave(com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent e) {
					if (e.getClientId().equals(actorId.get())) { rows.add("leave " + e); left.countDown(); }
				}
			})) {
				actor.connect(); actor.getApi().selectVirtualServerById(1);
				actorId.set(actor.getApi().whoAmI().getId());
				assertTrue(joined.await(5, TimeUnit.SECONDS));
				int channel = observer.getApi().channels().create("notification-probe-" + target,
					Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
				try {
					observer.getApi().clients().move(actorId.get(), channel);
					assertTrue(moved.await(5, TimeUnit.SECONDS));
					actor.exit(); assertTrue(left.await(5, TimeUnit.SECONDS));
				} finally { observer.getApi().channels().delete(channel, true); }
				// Deletion callback fences preceding join/move/leave callbacks on this registration.
				assertTrue(deleted.await(5, TimeUnit.SECONDS));
				assertEquals(1, rows.stream().filter(row -> row.startsWith("join ")).count());
				assertEquals(1, rows.stream().filter(row -> row.startsWith("move ")).count());
				assertEquals(1, rows.stream().filter(row -> row.startsWith("leave ")).count());
				assertFalse(observer.getApi().instance().version().version().isBlank());
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
		TeamSpeakContainer server = ts3;
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
			server.record(target.name(), api.instance().version());
			ClientId actorId = api.whoAmI().getId();
			assertNotEquals(actorId, observer.getApi().whoAmI().getId());
			observer.getApi().events().register(TS3EventType.CHANNEL, 0);
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
					int parent = channels.track(api.channels().create("parent-" + target, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1")));
					var parentEvent = created.poll(5, TimeUnit.SECONDS);
					assertNotNull(parentEvent); assertEquals(parent, parentEvent.getChannelId());
					int channel = channels.track(api.channels().create(name, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1", ChannelProperty.CHANNEL_TOPIC, "initial topic")));
					var create = created.poll(5, TimeUnit.SECONDS);
					assertNotNull(create); assertEquals(channel, create.getChannelId());
					assertEquals(name, create.require("channel_name"));
					assertEquals(actorId, create.getInvokerId());
					evidence.add("create event " + new java.util.TreeMap<>(create.getMap()));
					var list = api.channels().list().stream().filter(c -> c.getId() == channel).findFirst().orElseThrow();
					assertEquals(name, list.getName()); assertTrue(list.isPermanent()); assertEquals("initial topic", list.getTopic());
					evidence.add("channellist " + new java.util.TreeMap<>(list.getMap()));
					assertTrue(api.channels().search("domain-" + target).stream().anyMatch(c -> c.getId() == channel));
					String renamed = name + " renamed";
					String topic = "updated | literal \\n";
					api.channels().edit(channel, Map.of(ChannelProperty.CHANNEL_NAME, renamed, ChannelProperty.CHANNEL_TOPIC, topic,
						ChannelProperty.CHANNEL_DESCRIPTION, "description 日本語 | literal \\s"));
					var edit = edited.poll(5, TimeUnit.SECONDS);
					assertNotNull(edit); assertEquals(channel, edit.getChannelId());
					assertEquals(actorId, edit.getInvokerId());
					assertEquals(renamed, edit.require("channel_name")); assertEquals(topic, edit.require("channel_topic"));
					evidence.add("edit event " + new java.util.TreeMap<>(edit.getMap()));
					var info = api.channels().info(channel);
					assertEquals(renamed, info.getName()); assertEquals(topic, info.getTopic());
					assertEquals("description 日本語 | literal \\s", info.getDescription());
					assertNotNull(info.getUniqueIdentifier());
					evidence.add("channelinfo " + new java.util.TreeMap<>(info.getMap()));
					api.channels().move(channel, parent, 0);
					var move = moved.poll(5, TimeUnit.SECONDS);
					assertNotNull(move); evidence.add("move event " + new java.util.TreeMap<>(move.getMap()));
					assertEquals(channel, move.getChannelId());
					assertEquals(actorId, move.getInvokerId());
					assertEquals(parent, move.getChannelParentId()); assertEquals(0, move.getChannelOrder());
					assertEquals(parent, api.channels().info(channel).getParentChannelId());
					channels.delete(channel);
					var delete = deleted.poll(5, TimeUnit.SECONDS);
					assertNotNull(delete); assertEquals(channel, delete.getChannelId());
					assertEquals(actorId, delete.getInvokerId());
					evidence.add("delete event " + new java.util.TreeMap<>(delete.getMap()));
					assertTrue(api.channels().list().stream().noneMatch(c -> c.getId() == channel));
					var error = assertThrows(TS3CommandFailedException.class, () -> api.channels().info(channel));
					assertEquals(768, error.getError().getId());
					assertEquals("invalid channelID", error.getError().getMessage());
					evidence.add("deleted channel error " + new java.util.TreeMap<>(error.getError().getMap()));
					assertEquals(api.instance().version().version(), observer.getApi().instance().version().version());
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


	@ParameterizedTest(name = "{0}: channel modes, passwords, limits and codec settings")
	@EnumSource(Target.class)
	@Timeout(60)
	void channelConfigurationDomain(Target target) throws Exception {
		TeamSpeakContainer server = ts3;
		var evidence = new java.util.ArrayList<String>();
		var descriptions = new EventProbe<ChannelDescriptionEditedEvent>(16);
		var passwords = new EventProbe<ChannelPasswordChangedEvent>(16);
		try (var observer = new TS3Query(server.config(target.protocol));
			 var actor = new TS3Query(server.config(target.protocol))) {
			observer.connect(); actor.connect();
			var api = actor.getApi();
			api.selectVirtualServerById(1); observer.getApi().selectVirtualServerById(1);
			server.record(target.name(), api.instance().version());
			for (String command : java.util.List.of("channelcreate", "channeledit", "channelinfo")) {
				String help = api.executeRawCommand("help " + command).getRawResponse();
				assertTrue(help.startsWith("Usage: " + command)); evidence.add("help " + command + "\n" + help);
			}
			ClientId observerId = observer.getApi().whoAmI().getId();
			assertNotEquals(observerId, api.whoAmI().getId());
			try (var channels = new ChannelCleanup(api)) {
				int channel = channels.track(api.channels().create("configuration-" + target, Map.of(
					ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT, "1", ChannelProperty.CHANNEL_CODEC, Integer.toString(Codec.OPUS_VOICE.getIndex()),
					ChannelProperty.CHANNEL_CODEC_QUALITY, "6", ChannelProperty.CHANNEL_FLAG_MAXCLIENTS_UNLIMITED, "1",
					ChannelProperty.CHANNEL_FLAG_MAXFAMILYCLIENTS_INHERITED, "1")));
				var initial = api.channels().info(channel);
				assertTrue(initial.isSemiPermanent()); assertFalse(initial.isPermanent()); assertFalse(initial.hasPassword());
				assertTrue(initial.hasUnlimitedClients()); assertTrue(initial.hasInheritedMaxFamilyClients());
				assertEquals(Codec.OPUS_VOICE, initial.getCodec()); assertEquals(6, initial.getCodecQuality());
				api.clients().move(observerId, channel);
				observer.getApi().events().register(TS3EventType.CHANNEL, channel);
				try (var subscription = observer.subscribe(new TS3EventAdapter() {
					@Override public void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e) { descriptions.capture(() -> descriptions.publish(e)); }
					@Override public void onChannelPasswordChanged(ChannelPasswordChangedEvent e) { passwords.capture(() -> passwords.publish(e)); }
				})) {
					String description = "configuration 日本語 | literal \\n";
					api.channels().edit(channel, Map.of(ChannelProperty.CHANNEL_DESCRIPTION, description));
					var described = descriptions.await();
					assertEquals(channel, described.getChannelId());
					assertEquals(java.util.Set.of("cid"), described.getMap().keySet());
					evidence.add("description event " + new java.util.TreeMap<>(described.getMap()));
					assertEquals(description, api.channels().info(channel).getDescription());
					String password = "configuration 日本語 | literal \\s";
					api.channels().edit(channel, Map.of(ChannelProperty.CHANNEL_PASSWORD, password));
					var protectedEvent = passwords.await();
					assertEquals(channel, protectedEvent.getChannelId());
					assertEquals(java.util.Set.of("cid"), protectedEvent.getMap().keySet());
					evidence.add("password event fields " + new java.util.TreeSet<>(protectedEvent.getMap().keySet()));
					assertTrue(api.channels().info(channel).hasPassword());
					api.channels().edit(channel, Map.of(ChannelProperty.CHANNEL_PASSWORD, ""));
					var cleared = passwords.await();
					assertEquals(channel, cleared.getChannelId());
					assertEquals(java.util.Set.of("cid"), cleared.getMap().keySet());
					assertFalse(api.channels().info(channel).hasPassword());
				}
				var conflicting = assertThrows(TS3CommandFailedException.class,
					() -> api.channels().edit(channel, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1")));
				assertEquals(775, conflicting.getError().getId()); assertEquals("invalid channel flags", conflicting.getError().getMessage());
				evidence.add("conflicting mode " + new java.util.TreeMap<>(conflicting.getError().getMap()));
				var rejected = api.channels().info(channel);
				assertTrue(rejected.isSemiPermanent()); assertFalse(rejected.isPermanent());
				api.channels().edit(channel, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1", ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT, "0",
					ChannelProperty.CHANNEL_MAXCLIENTS, "2", ChannelProperty.CHANNEL_FLAG_MAXCLIENTS_UNLIMITED, "0",
					ChannelProperty.CHANNEL_MAXFAMILYCLIENTS, "3", ChannelProperty.CHANNEL_FLAG_MAXFAMILYCLIENTS_INHERITED, "0",
					ChannelProperty.CHANNEL_FLAG_MAXFAMILYCLIENTS_UNLIMITED, "0",
					ChannelProperty.CHANNEL_CODEC, Integer.toString(Codec.OPUS_MUSIC.getIndex()), ChannelProperty.CHANNEL_CODEC_QUALITY, "8"));
				var limited = api.channels().info(channel);
				assertTrue(limited.isPermanent()); assertFalse(limited.isSemiPermanent());
				assertFalse(limited.hasUnlimitedClients()); assertEquals(2, limited.getMaxClients());
				assertFalse(limited.hasInheritedMaxFamilyClients()); assertFalse(limited.hasUnlimitedFamilyClients());
				assertEquals(3, limited.getMaxFamilyClients()); assertEquals(Codec.OPUS_MUSIC, limited.getCodec());
				assertEquals(8, limited.getCodecQuality());
				evidence.add("limited fields " + new java.util.TreeSet<>(limited.getMap().keySet()));
				evidence.add("limited values " + Map.of("permanent", limited.isPermanent(), "semiPermanent", limited.isSemiPermanent(),
					"clients", limited.getMaxClients(), "familyClients", limited.getMaxFamilyClients(), "codec", limited.getCodec(), "quality", limited.getCodecQuality()));
				var invalid = assertThrows(TS3CommandFailedException.class,
					() -> api.channels().edit(channel, Map.of(ChannelProperty.CHANNEL_CODEC_QUALITY, "11")));
				evidence.add("invalid quality " + new java.util.TreeMap<>(invalid.getError().getMap()));
				assertEquals(2568, invalid.getError().getId()); assertEquals("insufficient client permissions", invalid.getError().getMessage());
				assertEquals(api.permissions().idByName(IPermissionType.I_CHANNEL_CREATE_MODIFY_WITH_CODEC_MAXQUALITY.getName()),
					invalid.getError().getFailedPermissionId());
				assertEquals(8, api.channels().info(channel).getCodecQuality());
				api.channels().edit(channel, Map.of(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT, "1", ChannelProperty.CHANNEL_FLAG_PERMANENT, "0",
					ChannelProperty.CHANNEL_FLAG_MAXCLIENTS_UNLIMITED, "1", ChannelProperty.CHANNEL_FLAG_MAXFAMILYCLIENTS_INHERITED, "1"));
				var restored = api.channels().info(channel);
				assertTrue(restored.isSemiPermanent()); assertFalse(restored.isPermanent());
				assertTrue(restored.hasUnlimitedClients()); assertTrue(restored.hasInheritedMaxFamilyClients());
			}
			observer.exit(); actor.exit();
			assertEquals(0, observer.getEventStatistics().malformedNotifications());
			assertEquals(0, observer.getEventStatistics().listenerFailures());
			assertEquals(0, observer.getEventStatistics().droppedEvents());
		} finally {
			Path path = Path.of("target", "compatibility", target + "-channel-configuration.txt");
			Files.createDirectories(path.getParent()); Files.write(path, evidence);
		}
	}

	@ParameterizedTest(name = "{0}: ban-rule administration and errors")
	@EnumSource(Target.class)
	@Timeout(60)
	void banRuleDomain(Target target) throws Exception {
		TeamSpeakContainer server = ts3;
		var evidence = new java.util.ArrayList<String>();
		try (var query = new TS3Query(server.config(target.protocol))) {
			query.connect();
			var api = query.getApi(); api.selectVirtualServerById(1);
			var identity = api.whoAmI();
			server.record(target.name(), api.instance().version());
			for (String command : java.util.List.of("banadd", "banlist", "bandel")) {
				String help = api.executeRawCommand("help " + command).getRawResponse();
				assertTrue(help.startsWith("Usage: " + command)); evidence.add("help " + command + "\n" + help);
			}
			var initial = api.bans().list().stream().map(com.github.theholywaffle.teamspeak3.api.wrapper.Ban::getId).collect(java.util.stream.Collectors.toSet());
			String name = "^unconnected-ban-" + target + "$";
			String ip = "198[.]51[.]100[.]44";
			String uid = "AAAAAAAAAAAAAAAAAAAAAAAAAAA=";
			String reason = "ban 日本語 | literal \\s\\p\\n /";
			int deletedId;
			try (var nameBan = new CreatedResource<>(api.bans().add(BanRequest.name(name).withReason(reason)), api.bans()::delete);
				 var ipBan = new CreatedResource<>(api.bans().add(BanRequest.ip(ip).withDuration(Duration.ofHours(1)).withReason(reason)), api.bans()::delete);
				 var uidBan = new CreatedResource<>(api.bans().add(BanRequest.uniqueId(uid)), api.bans()::delete)) {
				deletedId = nameBan.id();
				var bans = api.bans().list(); assertEquals(initial.size() + 3, bans.size());
				var expected = new java.util.HashSet<>(initial);
				expected.addAll(java.util.List.of(nameBan.id(), ipBan.id(), uidBan.id()));
				assertEquals(expected, bans.stream().map(com.github.theholywaffle.teamspeak3.api.wrapper.Ban::getId).collect(java.util.stream.Collectors.toSet()));
				for (var ban : bans) {
					if (initial.contains(ban.getId())) continue;
					evidence.add("ban " + new java.util.TreeMap<>(ban.getMap()));
					assertTrue(Math.abs(System.currentTimeMillis() - ban.getCreatedDate().toEpochMilli()) < 60_000);
					assertEquals(identity.getDatabaseId(), ban.getInvokerClientDBId());
					assertEquals(identity.getUniqueIdentifier(), ban.getInvokerUId());
					assertEquals(identity.getNickname(), ban.getInvokerName());
					assertEquals(0, ban.getEnforcements());
					assertEquals("", ban.getLastNickname());
					assertTrue(ban.getMap().containsKey("mytsid")); assertEquals("", ban.getMap().get("mytsid"));
					if (ban.getId() == nameBan.id()) {
						assertEquals(name, ban.getBannedName()); assertEquals(0, ban.getDuration()); assertEquals(reason, ban.getReason());
					} else if (ban.getId() == ipBan.id()) {
						assertEquals(ip, ban.getBannedIp()); assertEquals(3600, ban.getDuration()); assertEquals(reason, ban.getReason());
					} else {
						assertEquals(uidBan.id(), ban.getId()); assertEquals(uid, ban.getBannedUId());
						assertEquals(0, ban.getDuration()); assertEquals("", ban.getReason());
					}
				}
			}
			assertEquals(initial, api.bans().list().stream().map(com.github.theholywaffle.teamspeak3.api.wrapper.Ban::getId).collect(java.util.stream.Collectors.toSet()));
			var missing = assertThrows(TS3CommandFailedException.class, () -> api.bans().delete(deletedId));
			evidence.add("delete removed ban " + new java.util.TreeMap<>(missing.getError().getMap()));
			assertEquals(3328, missing.getError().getId()); assertEquals("invalid ban id", missing.getError().getMessage());
			assertFalse(api.instance().version().version().isBlank()); query.exit();
		} finally {
			Path path = Path.of("target", "compatibility", target + "-ban-rules.txt");
			Files.createDirectories(path.getParent()); Files.write(path, evidence);
		}
	}

	@ParameterizedTest(name = "{0}: privilege key administration and query-client rejection")
	@EnumSource(Target.class)
	@Timeout(60)
	void privilegeKeyDomain(Target target) throws Exception {
		TeamSpeakContainer server = ts3;
		var evidence = new java.util.ArrayList<String>();
		var used = new java.util.concurrent.LinkedBlockingQueue<PrivilegeKeyUsedEvent>(16);
		var messages = new java.util.concurrent.LinkedBlockingQueue<TextMessageEvent>(16);
		try (var observer = new TS3Query(server.config(target.protocol));
			 var actor = new TS3Query(server.config(target.protocol))) {
			observer.connect(); actor.connect();
			var api = observer.getApi();
			api.selectVirtualServerById(1); actor.getApi().selectVirtualServerById(1);
			actor.getApi().setNickname("key-actor-" + target);
			var identity = actor.getApi().whoAmI();
			ClientId observerId = api.whoAmI().getId();
			assertNotEquals(observerId, identity.getId());
			server.record(target.name(), api.instance().version());
			for (String command : java.util.List.of("privilegekeyadd", "privilegekeylist", "privilegekeydelete", "privilegekeyuse")) {
				String help = api.executeRawCommand("help " + command).getRawResponse();
				assertTrue(help.startsWith("Usage: " + command), "Live help must describe " + command);
				evidence.add("help " + command + "\n" + help);
			}
			var initial = api.privilegeKeys().list().stream().map(PrivilegeKey::getToken).collect(java.util.stream.Collectors.toSet());
			api.events().register(TS3EventType.SERVER); api.events().register(TS3EventType.TEXT_PRIVATE);
			try (var channels = new ChannelCleanup(api);
				 var serverGroup = new CreatedResource<>(api.serverGroups().create("key-server-" + target), id -> api.serverGroups().delete(id, true));
				 var channelGroup = new CreatedResource<>(api.channelGroups().create("key-channel-" + target), id -> api.channelGroups().delete(id, true));
				 var subscription = observer.subscribe(new TS3EventAdapter() {
					@Override public void onPrivilegeKeyUsed(PrivilegeKeyUsedEvent event) { assertTrue(used.offer(event)); }
					@Override public void onTextMessage(TextMessageEvent event) { assertTrue(messages.offer(event)); }
				 })) {
				int channel = channels.track(api.channels().create("key-channel-" + target, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1")));
				String description = "key 日本語 | literal \\s\\p\\n /";
				String deletedToken;
				try (var serverKey = new CreatedPrivilegeKey(api, api.privilegeKeys().createForServerGroup(serverGroup.id(), description));
					 var channelKey = new CreatedPrivilegeKey(api, api.privilegeKeys().createForChannelGroup(channelGroup.id(), channel, description));
					 var genericKey = new CreatedPrivilegeKey(api, api.privilegeKeys().createForServerGroup(serverGroup.id(), null))) {
					deletedToken = serverKey.token();
					var listed = api.privilegeKeys().list();
					assertEquals(initial.size() + 3, listed.size());
					var serverEntry = listed.stream().filter(k -> k.getToken().equals(serverKey.token())).findFirst().orElseThrow();
					assertPrivilegeKey(serverEntry, PrivilegeKeyType.SERVER_GROUP, serverGroup.id(), 0, description, evidence);
					var channelEntry = listed.stream().filter(k -> k.getToken().equals(channelKey.token())).findFirst().orElseThrow();
					assertPrivilegeKey(channelEntry, PrivilegeKeyType.CHANNEL_GROUP, channelGroup.id(), channel, description, evidence);
					var genericEntry = listed.stream().filter(k -> k.getToken().equals(genericKey.token())).findFirst().orElseThrow();
					assertPrivilegeKey(genericEntry, PrivilegeKeyType.SERVER_GROUP, serverGroup.id(), 0, "", evidence);
					var serverUse = assertThrows(TS3CommandFailedException.class, () -> actor.getApi().privilegeKeys().use(serverEntry.getToken()));
					evidence.add("server key query use " + new java.util.TreeMap<>(serverUse.getError().getMap()));
					assertEquals(516, serverUse.getError().getId()); assertEquals("invalid client type", serverUse.getError().getMessage());
					var channelUse = assertThrows(TS3CommandFailedException.class, () -> actor.getApi().privilegeKeys().use(channelKey.token()));
					evidence.add("channel key query use " + new java.util.TreeMap<>(channelUse.getError().getMap()));
					assertEquals(516, channelUse.getError().getId()); assertEquals("invalid client type", channelUse.getError().getMessage());
					// The later actor message fences preceding notification frames on this ordered subscription.
					actor.getApi().messages().sendPrivate(observerId, "rejected-key-use-fence");
					assertMessage(messages, TextMessageTargetMode.CLIENT, "rejected-key-use-fence", identity, observerId, evidence);
					assertTrue(used.isEmpty(), "Rejected consumption must not produce a privilege-key-used event");
					var retained = api.privilegeKeys().list().stream().map(PrivilegeKey::getToken).collect(java.util.stream.Collectors.toSet());
					assertTrue(retained.containsAll(java.util.List.of(serverKey.token(), channelKey.token(), genericKey.token())));
				}
				var restored = api.privilegeKeys().list().stream().map(PrivilegeKey::getToken).collect(java.util.stream.Collectors.toSet());
				assertEquals(initial, restored);
				var missing = assertThrows(TS3CommandFailedException.class, () -> api.privilegeKeys().delete(deletedToken));
				evidence.add("delete removed key " + new java.util.TreeMap<>(missing.getError().getMap()));
				assertEquals(3, missing.getError().getId()); assertEquals("ok", missing.getError().getMessage());
				var deletedUse = assertThrows(TS3CommandFailedException.class, () -> actor.getApi().privilegeKeys().use(deletedToken));
				evidence.add("removed key query use " + new java.util.TreeMap<>(deletedUse.getError().getMap()));
				assertEquals(516, deletedUse.getError().getId()); assertEquals("invalid client type", deletedUse.getError().getMessage());
				assertFalse(api.instance().version().version().isBlank());
			}
			observer.exit(); actor.exit();
			assertEquals(0, observer.getEventStatistics().unknownNotifications());
			assertEquals(0, observer.getEventStatistics().malformedNotifications());
			assertEquals(0, observer.getEventStatistics().listenerFailures());
			assertEquals(0, observer.getEventStatistics().droppedEvents());
		} finally {
			Path path = Path.of("target", "compatibility", target + "-privilege-keys.txt");
			Files.createDirectories(path.getParent()); Files.write(path, evidence);
		}
	}

	private static void assertPrivilegeKey(PrivilegeKey key, PrivilegeKeyType type, int group, int channel,
	                                       String description, java.util.List<String> evidence) {
		evidence.add("key " + new java.util.TreeMap<>(key.getMap()));
		assertFalse(key.getToken().isBlank()); assertEquals(type.getIndex(), key.getType());
		assertEquals(type == PrivilegeKeyType.SERVER_GROUP, key.isServerGroupToken());
		assertEquals(type == PrivilegeKeyType.CHANNEL_GROUP, key.isChannelGroupToken());
		assertEquals(group, key.getGroupId()); assertEquals(channel, key.getChannelId());
		assertTrue(key.getMap().containsKey("token_description")); assertEquals(description, key.getDescription());
		assertTrue(Math.abs(System.currentTimeMillis() - key.getCreated().toEpochMilli()) < 60_000);
	}

	private record CreatedPrivilegeKey(TS3Api api, String token) implements AutoCloseable {
		@Override public void close() { api.privilegeKeys().delete(token); }
	}

	@ParameterizedTest(name = "{0}: file directory commands and metadata")
	@EnumSource(Target.class)
	@Timeout(60)
	void fileDomain(Target target) throws Exception {
		TeamSpeakContainer server = ts3;
		var evidence = new java.util.ArrayList<String>();
		try (var query = new TS3Query(server.config(target.protocol))) {
			query.connect();
			var api = query.getApi();
			api.selectVirtualServerById(1);
			server.record(target.name(), api.instance().version());
			for (String command : java.util.List.of("ftcreatedir", "ftgetfilelist", "ftgetfileinfo", "ftrenamefile", "ftdeletefile", "ftlist")) {
				String help = api.executeRawCommand("help " + command).getRawResponse();
				assertTrue(help.startsWith("Usage: " + command), "Live help must describe " + command);
				evidence.add("help " + command + "\n" + help);
			}
			try (var channels = new ChannelCleanup(api)) {
				int source = channels.track(api.channels().create("file-source-" + target,
					Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1")));
				String password = "file password | \\s";
				int destination = channels.track(api.channels().create("file-destination-" + target,
					Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1", ChannelProperty.CHANNEL_PASSWORD, password)));
				assertTrue(api.channels().info(destination).hasPassword());
				assertEquals(1, api.permissions().ownValue(BPermissionType.B_FT_IGNORE_PASSWORD.getName()));
				evidence.add("effective b_ft_ignore_password=1; destination channel is password protected");
				assertTrue(api.files().list(FileLocation.of(source, "/")).isEmpty());
				assertTrue(api.files().list(FileLocation.of(destination, "/").withChannelPassword(password)).isEmpty());
				assertTrue(api.files().transfers().isEmpty());
				var rejected = assertThrows(TS3CommandFailedException.class, () -> api.files().createDirectory(FileLocation.of(source, "/literal\\s")));
				evidence.add("invalid path " + new java.util.TreeMap<>(rejected.getError().getMap()));
				assertEquals(2054, rejected.getError().getId()); assertEquals("invalid file path", rejected.getError().getMessage());
				assertTrue(api.files().list(FileLocation.of(source, "/")).isEmpty());
				String pipePath = "/directory | pipe";
				api.files().createDirectory(FileLocation.of(source, pipePath));
				var pipe = api.files().list(FileLocation.of(source, "/")).getFirst();
				evidence.add("pipe path " + new java.util.TreeMap<>(pipe.getMap()));
				assertEquals(pipePath, pipe.getPath());
				api.files().delete(FileLocation.of(source, pipePath));
				String unicodePath = "/directory 日本語 spaces";
				api.files().createDirectory(FileLocation.of(source, unicodePath));
				var unicode = api.files().list(FileLocation.of(source, "/")).getFirst();
				evidence.add("Unicode path " + new java.util.TreeMap<>(unicode.getMap()));
				assertEquals(unicodePath, unicode.getPath());
				api.files().delete(FileLocation.of(source, unicodePath));
				assertTrue(api.files().list(FileLocation.of(source, "/")).isEmpty());
				String name = "directory spaces";
				String path = "/" + name;
				api.files().createDirectory(FileLocation.of(source, name));
				var directory = api.files().list(FileLocation.of(source, "/")).getFirst();
				evidence.add("root directory " + new java.util.TreeMap<>(directory.getMap()));
				assertEquals(name, directory.getName()); assertEquals(path, directory.getPath());
				assertEquals("/", directory.getParentPath()); assertEquals(source, directory.getChannelId());
				assertEquals(0, directory.getType()); assertTrue(directory.isDirectory()); assertFalse(directory.isFile());
				long modified = directory.getLastModifiedDate().toEpochMilli();
				assertTrue(Math.abs(System.currentTimeMillis() - modified) < 60_000, "New directory timestamp must represent current time");
				assertFalse(directory.isStillUploading()); assertFalse(directory.getMap().containsKey("incompletesize"));
				assertTrue(api.files().list(FileLocation.of(source, name)).isEmpty());
				api.files().createDirectory(FileLocation.of(source, path + "/child"));
				var nested = api.files().list(FileLocation.of(source, path)).getFirst();
				evidence.add("nested directory " + new java.util.TreeMap<>(nested.getMap()));
				assertEquals("child", nested.getName()); assertEquals(path + "/child", nested.getPath());
				assertEquals(path + "/", nested.getParentPath()); assertTrue(nested.isDirectory());
				var directoryInfo = assertThrows(TS3CommandFailedException.class, () -> api.files().info(FileLocation.of(source, path)));
				evidence.add("directory fileinfo " + new java.util.TreeMap<>(directoryInfo.getError().getMap()));
				assertEquals(2048, directoryInfo.getError().getId()); assertEquals("invalid file name", directoryInfo.getError().getMessage());
				String renamed = "/renamed spaces";
				api.files().move(FileLocation.of(source, path), FileLocation.of(source, renamed));
				assertEquals(renamed, api.files().list(FileLocation.of(source, "/")).getFirst().getPath());
				assertEquals(renamed + "/child", api.files().list(FileLocation.of(source, renamed)).getFirst().getPath());
				api.files().move(FileLocation.of(source, renamed), FileLocation.of(destination, "/moved").withChannelPassword(password));
				assertTrue(api.files().list(FileLocation.of(source, "/")).isEmpty());
				assertEquals("/moved", api.files().list(FileLocation.of(destination, "/").withChannelPassword(password)).getFirst().getPath());
				assertEquals("/moved/child", api.files().list(FileLocation.of(destination, "moved").withChannelPassword(password)).getFirst().getPath());
				api.files().delete(FileLocation.of(destination, "/moved/child").withChannelPassword(password));
				assertTrue(api.files().list(FileLocation.of(destination, "/moved").withChannelPassword(password)).isEmpty());
				api.files().createDirectory(FileLocation.of(destination, "/second").withChannelPassword(password));
				api.files().delete(java.util.List.of(FileLocation.of(destination, "/moved").withChannelPassword(password), FileLocation.of(destination, "/second").withChannelPassword(password)));
				assertTrue(api.files().list(FileLocation.of(destination, "/").withChannelPassword(password)).isEmpty());
				var missing = assertThrows(TS3CommandFailedException.class, () -> api.files().info(FileLocation.of(source, "/absent.txt")));
				evidence.add("missing file " + new java.util.TreeMap<>(missing.getError().getMap()));
				assertEquals(2052, missing.getError().getId());
				assertEquals("file input/output error", missing.getError().getMessage());
				var missingDirectory = assertThrows(TS3CommandFailedException.class, () -> api.files().list(FileLocation.of(source, "/absent")));
				evidence.add("missing directory " + new java.util.TreeMap<>(missingDirectory.getError().getMap()));
				assertEquals(2052, missingDirectory.getError().getId());
				assertEquals("file input/output error", missingDirectory.getError().getMessage());
				assertTrue(api.files().list(FileLocation.of(destination, "/").withChannelPassword("wrong")).isEmpty());
				evidence.add("serveradmin can list the password-protected channel with an incorrect password");
				assertTrue(api.files().list(FileLocation.of(destination, "/").withChannelPassword(password)).isEmpty());
				assertFalse(api.instance().version().version().isBlank());
			}
			query.exit();
		} finally {
			Path path = Path.of("target", "compatibility", target + "-files.txt");
			Files.createDirectories(path.getParent()); Files.write(path, evidence);
		}
	}

	@ParameterizedTest(name = "{0}: online query-client administration and independent notifications")
	@EnumSource(Target.class)
	@Timeout(60)
	void clientDomain(Target target) throws Exception {
		TeamSpeakContainer server = ts3;
		var evidence = new java.util.ArrayList<String>();
		var joins = new java.util.concurrent.LinkedBlockingQueue<ClientJoinEvent>(16);
		var moves = new java.util.concurrent.LinkedBlockingQueue<ClientMovedEvent>(16);
		var leaves = new java.util.concurrent.LinkedBlockingQueue<ClientLeaveEvent>(16);
		try (var observer = new TS3Query(server.config(target.protocol));
			 var controller = new TS3Query(server.config(target.protocol));
			 var subject = new TS3Query(server.config(target.protocol))) {
			observer.connect(); controller.connect();
			var api = controller.getApi();
			api.selectVirtualServerById(1); observer.getApi().selectVirtualServerById(1);
			api.setNickname("client-admin-" + target);
			var invoker = api.whoAmI();
			server.record(target.name(), api.instance().version());
			for (String command : java.util.List.of("clientlist", "clientinfo", "clientfind", "clientupdate", "clientedit", "clientmove")) {
				String help = api.executeRawCommand("help " + command).getRawResponse();
				assertTrue(help.startsWith("Usage: " + command), "Live help must describe " + command);
				evidence.add("help " + command + "\n" + help);
			}
			observer.getApi().events().register(TS3EventType.SERVER);
			observer.getApi().events().register(TS3EventType.CHANNEL, 0);
			try (var channels = new ChannelCleanup(api);
				 var subscription = observer.subscribe(new TS3EventAdapter() {
					@Override public void onClientJoin(ClientJoinEvent event) { assertTrue(joins.offer(event)); }
					@Override public void onClientMoved(ClientMovedEvent event) { assertTrue(moves.offer(event)); }
					@Override public void onClientLeave(ClientLeaveEvent event) { assertTrue(leaves.offer(event)); }
				 })) {
				subject.connect(); subject.getApi().selectVirtualServerById(1);
				var identity = subject.getApi().whoAmI();
				ClientId id = identity.getId();
				assertNotEquals(invoker.getId(), id); assertNotEquals(observer.getApi().whoAmI().getId(), id);
				var join = joins.poll(5, TimeUnit.SECONDS);
				assertNotNull(join); evidence.add("join " + new java.util.TreeMap<>(join.getMap()));
				assertEquals(id, join.getClientId()); assertEquals(identity.getChannelId(), join.getClientTargetId());
				assertEquals(0, join.getClientFromId()); assertEquals(0, join.getReasonId());
				assertEquals(identity.getNickname(), join.getClientNickname());
				assertEquals(identity.getUniqueIdentifier(), join.getUniqueClientIdentifier());
				assertEquals(identity.getDatabaseId(), join.getClientDatabaseId()); assertEquals(1, join.getClientType());
				assertFalse(join.getMap().containsKey("invokerid")); assertEquals(-1, join.getInvokerId());
				String name = "query-" + target + " 日本語 | \\s";
				subject.getApi().updateClient(Map.of(ClientProperty.CLIENT_NICKNAME, name));
				assertEquals(name, subject.getApi().whoAmI().getNickname());
				var info = api.clients().info(id);
				evidence.add("clientinfo " + new java.util.TreeMap<>(info.getMap()));
				assertEquals(id, info.getId()); assertEquals(name, info.getNickname()); assertEquals(1, info.getType());
				assertFalse(info.isRegularClient()); assertEquals(identity.getChannelId(), info.getChannelId());
				assertEquals(identity.getDatabaseId(), info.getDatabaseId());
				assertEquals(identity.getUniqueIdentifier(), info.getUniqueIdentifier());
				var listed = api.clients().list().stream().filter(c -> c.getId().equals(id)).findFirst().orElseThrow();
				evidence.add("clientlist " + new java.util.TreeMap<>(listed.getMap()));
				assertEquals(name, listed.getNickname()); assertEquals(info.getUniqueIdentifier(), listed.getUniqueIdentifier());
				assertEquals(info.getDatabaseId(), listed.getDatabaseId()); assertEquals(1, listed.getType());
				assertEquals(id, api.clients().findByName(name, false).orElseThrow().getId());
				assertEquals(id, api.clients().findByName(name.toUpperCase(java.util.Locale.ROOT), true).orElseThrow().getId());
				assertTrue(api.clients().search(name).stream().anyMatch(c -> c.getId().equals(id)));
				assertTrue(api.clients().findByName("absent-client-" + target, false).isEmpty());
				assertTrue(api.clients().search("absent-client-" + target).isEmpty());
				try (var restoration = new ClientDescription(api, id, info.getDescription())) {
					String description = "description 日本語 | \\s\\p\\n /";
					api.clients().edit(id, Map.of(ClientProperty.CLIENT_DESCRIPTION, description));
					assertEquals(description, api.clients().info(id).getDescription());
					String controls = "before\nmiddle\tend\r tail";
					api.clients().edit(id, Map.of(ClientProperty.CLIENT_DESCRIPTION, controls));
					assertEquals("beforemiddleend tail", api.clients().info(id).getDescription());
					String rawDescription = api.executeRawCommand("clientinfo clid=" + id).getRawResponse();
					evidence.add("description controls raw " + rawDescription);
					assertTrue(rawDescription.contains("client_description=beforemiddleend\\stail"));
					int channel = channels.track(api.channels().create("client-move-" + target,
						Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1")));
					api.clients().move(id, channel);
					assertClientMove(moves, id, channel, 1, invoker, evidence);
					assertEquals(channel, subject.getApi().whoAmI().getChannelId());
					assertEquals(channel, api.clients().info(id).getChannelId());
					var badMove = assertThrows(TS3CommandFailedException.class, () -> api.clients().move(id, Integer.MAX_VALUE));
					evidence.add("invalid channel " + new java.util.TreeMap<>(badMove.getError().getMap()));
					assertEquals(768, badMove.getError().getId()); assertEquals("invalid channelID", badMove.getError().getMessage());
					assertEquals(channel, api.clients().info(id).getChannelId());
					subject.getApi().clients().move(id, identity.getChannelId());
					assertClientMove(moves, id, identity.getChannelId(), 0, null, evidence);
					assertTrue(moves.isEmpty(), "Rejected moves must not add notifications before the valid move fence");
				}
				assertEquals(info.getDescription(), api.clients().info(id).getDescription());
				subject.exit();
				var leave = leaves.poll(5, TimeUnit.SECONDS);
				assertNotNull(leave); evidence.add("leave " + new java.util.TreeMap<>(leave.getMap()));
				assertEquals(id, leave.getClientId()); assertEquals(identity.getChannelId(), leave.getClientFromId());
				assertEquals(0, leave.getClientTargetId()); assertEquals(8, leave.getReasonId());
				assertFalse(leave.getMap().containsKey("invokerid")); assertEquals(-1, leave.getInvokerId());
				var gone = assertThrows(TS3CommandFailedException.class, () -> api.clients().info(id));
				evidence.add("disconnected client " + new java.util.TreeMap<>(gone.getError().getMap()));
				assertEquals(512, gone.getError().getId()); assertEquals("invalid clientID", gone.getError().getMessage());
				assertTrue(api.clients().list().stream().noneMatch(c -> c.getId().equals(id)));
				assertFalse(api.instance().version().version().isBlank());
				assertTrue(joins.isEmpty()); assertTrue(moves.isEmpty()); assertTrue(leaves.isEmpty());
			}
			observer.exit(); controller.exit();
			assertEquals(0, observer.getEventStatistics().malformedNotifications());
			assertEquals(0, observer.getEventStatistics().listenerFailures());
			assertEquals(0, observer.getEventStatistics().droppedEvents());
		} finally {
			Path path = Path.of("target", "compatibility", target + "-clients.txt");
			Files.createDirectories(path.getParent()); Files.write(path, evidence);
		}
	}

	private static void assertClientMove(java.util.concurrent.BlockingQueue<ClientMovedEvent> moves,
	                                     ClientId id, int channel, int reason, ServerQueryInfo invoker, java.util.List<String> evidence) throws InterruptedException {
		var event = moves.poll(5, TimeUnit.SECONDS);
		assertNotNull(event); evidence.add("move " + new java.util.TreeMap<>(event.getMap()));
		assertEquals(id, event.getClientId()); assertEquals(channel, event.getTargetChannelId());
		assertEquals(java.util.OptionalInt.of(reason), event.getReasonId());
		assertEquals(invoker != null, event.getMap().containsKey("invokerid"));
		if (invoker != null) {
			assertEquals(Optional.of(invoker.getId()), event.getInvokerId()); assertEquals(Optional.of(invoker.getNickname()), event.getInvokerName());
			assertEquals(Optional.of(invoker.getUniqueIdentifier()), event.getInvokerUniqueId());
		} else {
			assertEquals(Optional.empty(), event.getInvokerId()); assertEquals(Optional.empty(), event.getInvokerName()); assertEquals(Optional.empty(), event.getInvokerUniqueId());
		}
	}

	private record ClientDescription(TS3Api api, ClientId id, String value) implements AutoCloseable {
		@Override public void close() { api.clients().edit(id, Map.of(ClientProperty.CLIENT_DESCRIPTION, value)); }
	}

	@ParameterizedTest(name = "{0}: server information and edits from an independent query session")
	@EnumSource(Target.class)
	@Timeout(60)
	void serverDomain(Target target) throws Exception {
		TeamSpeakContainer server = ts3;
		var evidence = new java.util.ArrayList<String>();
		var edits = new java.util.concurrent.LinkedBlockingQueue<ServerEditedEvent>(16);
		try (var observer = new TS3Query(server.config(target.protocol));
			 var actor = new TS3Query(server.config(target.protocol))) {
			observer.connect(); actor.connect();
			var api = actor.getApi();
			api.selectVirtualServerById(1); observer.getApi().selectVirtualServerById(1);
			server.record(target.name(), api.instance().version());
			api.setNickname("server-edit-" + target);
			var actorInfo = api.whoAmI();
			assertNotEquals(actorInfo.getId(), observer.getApi().whoAmI().getId());
			for (String command : java.util.List.of("serverinfo", "serverlist", "serveridgetbyport",
				"hostinfo", "instanceinfo", "serverrequestconnectioninfo", "serveredit", "servernotifyregister")) {
				String help = api.executeRawCommand("help " + command).getRawResponse();
				assertTrue(help.startsWith("Usage: " + command), "Live help must describe " + command);
				evidence.add("help " + command + "\n" + help);
			}
			var info = api.servers().info();
			evidence.add("serverinfo " + new java.util.TreeMap<>(info.getMap()));
			assertEquals(1, info.getId()); assertEquals(VirtualServerStatus.ONLINE, info.getStatus());
			assertFalse(info.getName().isBlank()); assertFalse(info.getUniqueIdentifier().isBlank());
			assertTrue(info.getPort() > 0); assertTrue(info.getMaxClients().orElseThrow() > 0);
			assertFalse(info.getVersion().isBlank()); assertFalse(info.getPlatform().isBlank());
			var listed = api.servers().list().stream().filter(v -> v.getId() == 1).findFirst().orElseThrow();
			evidence.add("serverlist " + new java.util.TreeMap<>(listed.getMap()));
			assertEquals(info.getName(), listed.getName()); assertEquals(info.getPort(), listed.getPort());
			assertEquals(info.getUniqueIdentifier(), listed.getUniqueIdentifier());
			assertEquals(info.getStatus(), listed.getStatus()); assertEquals(info.getMaxClients(), listed.getMaxClients());
			assertEquals(Optional.of(1), api.servers().findIdByPort(info.getPort()));
			var host = api.instance().hostInfo();
			evidence.add("hostinfo " + new java.util.TreeMap<>(host.getMap()));
			assertEquals(1, host.getTotalRunningServers()); assertTrue(host.getTotalChannels() > 0);
			assertFalse(host.getUptime().isNegative()); assertTrue(host.getTimeStamp().toEpochMilli() > 0);
			var instance = api.instance().info();
			evidence.add("instanceinfo " + new java.util.TreeMap<>(instance.getMap()));
			assertTrue(instance.getDatabaseVersion() > 0); assertTrue(instance.getFileTransferPort() > 0);
			var connection = api.servers().connectionInfo();
			evidence.add("connectioninfo " + new java.util.TreeMap<>(connection.getMap()));
			assertTrue(connection.getTotalPacketsSent() >= 0); assertTrue(connection.getTotalPacketsReceived() >= 0);
			assertTrue(connection.getTotalBytesSent() >= 0); assertTrue(connection.getTotalBytesReceived() >= 0);
			var original = Map.of(VirtualServerProperty.VIRTUALSERVER_NAME, info.getName(),
				VirtualServerProperty.VIRTUALSERVER_WELCOMEMESSAGE, info.getWelcomeMessage(),
				VirtualServerProperty.VIRTUALSERVER_HOSTMESSAGE, info.getHostMessage());
			observer.getApi().events().register(TS3EventType.SERVER);
			try (var restoration = new ServerSettings(api, original);
				 var subscription = observer.subscribe(new TS3EventAdapter() {
					@Override public void onServerEdit(ServerEditedEvent event) { assertTrue(edits.offer(event)); }
				 })) {
				String name = "server-" + target + " 日本語 | \\s";
				String welcome = "welcome | literal \\s\\p\\n / \n";
				String hostMessage = "host | literal \\t\\r\\\\ / \t\r";
				api.servers().edit(Map.of(VirtualServerProperty.VIRTUALSERVER_NAME, name,
					VirtualServerProperty.VIRTUALSERVER_WELCOMEMESSAGE, welcome,
					VirtualServerProperty.VIRTUALSERVER_HOSTMESSAGE, hostMessage));
				assertServerEdit(edits, name, actorInfo, evidence);
				var changed = api.servers().info();
				assertEquals(name, changed.getName()); assertEquals(welcome, changed.getWelcomeMessage());
				assertEquals(hostMessage, changed.getHostMessage());
				assertEquals(name, api.servers().list().stream().filter(v -> v.getId() == 1).findFirst().orElseThrow().getName());
				var invalidValue = assertThrows(TS3CommandFailedException.class,
					() -> api.servers().edit(Map.of(VirtualServerProperty.VIRTUALSERVER_MAXCLIENTS, "not-an-integer")));
				evidence.add("invalid numeric edit " + new java.util.TreeMap<>(invalidValue.getError().getMap()));
				assertEquals(1540, invalidValue.getError().getId());
				assertEquals("convert error", invalidValue.getError().getMessage());
				assertEquals(name, api.servers().info().getName());
				String secondName = "updated " + name;
				api.servers().edit(Map.of(VirtualServerProperty.VIRTUALSERVER_NAME, secondName));
				assertServerEdit(edits, secondName, actorInfo, evidence);
				assertTrue(edits.isEmpty(), "Rejected edits must not produce an extra server-edit notification");
				assertEquals(secondName, api.servers().info().getName());
				assertEquals(Optional.empty(), api.servers().findIdByPort(65535));
				var missingPort = assertThrows(TS3CommandFailedException.class, () -> api.executeRawCommand("serveridgetbyport virtualserver_port=65535"));
				evidence.add("unknown port " + new java.util.TreeMap<>(missingPort.getError().getMap()));
				assertEquals(1024, missingPort.getError().getId());
				assertEquals("invalid serverID", missingPort.getError().getMessage());
				assertEquals(Optional.of(1), api.servers().findIdByPort(info.getPort()));
			}
			var restored = api.servers().info();
			assertEquals(info.getName(), restored.getName()); assertEquals(info.getWelcomeMessage(), restored.getWelcomeMessage());
			assertEquals(info.getHostMessage(), restored.getHostMessage());
			observer.exit(); actor.exit();
			assertEquals(0, observer.getEventStatistics().malformedNotifications());
			assertEquals(0, observer.getEventStatistics().listenerFailures());
			assertEquals(0, observer.getEventStatistics().droppedEvents());
		} finally {
			Path path = Path.of("target", "compatibility", target + "-servers.txt");
			Files.createDirectories(path.getParent()); Files.write(path, evidence);
		}
	}

	private static void assertServerEdit(java.util.concurrent.BlockingQueue<ServerEditedEvent> edits,
	                                     String name, ServerQueryInfo actorInfo, java.util.List<String> evidence) throws InterruptedException {
		var event = edits.poll(5, TimeUnit.SECONDS);
		assertNotNull(event, "Expected server-edit notification from independent actor");
		evidence.add("server edit " + new java.util.TreeMap<>(event.getMap()));
		assertEquals(name, event.require(VirtualServerProperty.VIRTUALSERVER_NAME));
		assertEquals(Optional.of(actorInfo.getId()), event.getInvokerId()); assertEquals(Optional.of(actorInfo.getNickname()), event.getInvokerName());
		assertEquals(Optional.of(actorInfo.getUniqueIdentifier()), event.getInvokerUniqueId());
		assertTrue(event.getMap().containsKey("reasonid"));
		assertEquals(10, event.getReasonId());
		assertFalse(event.getMap().containsKey(VirtualServerProperty.VIRTUALSERVER_WELCOMEMESSAGE.getName()));
		assertFalse(event.getMap().containsKey(VirtualServerProperty.VIRTUALSERVER_HOSTMESSAGE.getName()));
	}

	private record ServerSettings(TS3Api api, Map<VirtualServerProperty, String> values) implements AutoCloseable {
		@Override public void close() { api.servers().edit(values); }
	}

	@ParameterizedTest(name = "{0}: online text messages from an independent query session")
	@EnumSource(Target.class)
	@Timeout(60)
	void messageDomain(Target target) throws Exception {
		TeamSpeakContainer server = ts3;
		var evidence = new java.util.ArrayList<String>();
		var messages = new java.util.concurrent.LinkedBlockingQueue<TextMessageEvent>(16);
		try (var observer = new TS3Query(server.config(target.protocol));
			 var actor = new TS3Query(server.config(target.protocol))) {
			observer.connect(); actor.connect();
			var api = actor.getApi();
			var recipient = observer.getApi();
			api.selectVirtualServerById(1); recipient.selectVirtualServerById(1);
			server.record(target.name(), api.instance().version());
			String nickname = "msg-" + target + " | \\s";
			api.setNickname(nickname);
			var actorInfo = api.whoAmI();
			assertEquals(nickname, actorInfo.getNickname());
			ClientId observerId = recipient.whoAmI().getId();
			assertNotEquals(actorInfo.getId(), observerId);
			assertFalse(actorInfo.getUniqueIdentifier().isBlank());
			for (String command : java.util.List.of("sendtextmessage", "servernotifyregister", "servernotifyunregister")) {
				String help = api.executeRawCommand("help " + command).getRawResponse();
				assertTrue(help.startsWith("Usage: " + command), "Live help must describe " + command);
				evidence.add("help " + command + "\n" + help);
			}
			try (var channels = new ChannelCleanup(recipient);
				 var subscription = observer.subscribe(new TS3EventAdapter() {
					@Override public void onTextMessage(TextMessageEvent event) { assertTrue(messages.offer(event)); }
				 })) {
				int channel = channels.track(api.channels().create("messages-" + target, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1")));
				recipient.moveQuery(channel);
				recipient.events().register(TS3EventType.TEXT_SERVER, TS3EventType.TEXT_CHANNEL, TS3EventType.TEXT_PRIVATE);
				String payload = "[b]héllo 日本語[/b] | literal \\s\\p\\n\\t\\r\\\\ / \n\t\r";
				api.messages().sendServer(payload);
				assertMessage(messages, TextMessageTargetMode.SERVER, payload, actorInfo, observerId, evidence);
				api.messages().sendChannel(channel, payload);
				assertEquals(channel, api.whoAmI().getChannelId(), "Explicit channel send leaves the query in that channel");
				assertMessage(messages, TextMessageTargetMode.CHANNEL, payload, actorInfo, observerId, evidence);
				api.messages().sendChannel("current " + payload);
				assertMessage(messages, TextMessageTargetMode.CHANNEL, "current " + payload, actorInfo, observerId, evidence);
				// The typed API has no channel/server target: the server ignores it, as these raw commands record.
				api.executeRawCommand("sendtextmessage targetmode=2 target=65535 msg=" + CommandEncoding.encode("ignored target " + payload));
				assertMessage(messages, TextMessageTargetMode.CHANNEL, "ignored target " + payload, actorInfo, observerId, evidence);
				api.executeRawCommand("sendtextmessage targetmode=3 target=65535 msg=" + CommandEncoding.encode("ignored target " + payload));
				assertMessage(messages, TextMessageTargetMode.SERVER, "ignored target " + payload, actorInfo, observerId, evidence);
				api.messages().sendPrivate(observerId, payload);
				assertMessage(messages, TextMessageTargetMode.CLIENT, payload, actorInfo, observerId, evidence);
				api.executeRawCommand("sendtextmessage targetmode=1 target=" + observerId + " msg=" + CommandEncoding.encode("generic " + payload));
				assertMessage(messages, TextMessageTargetMode.CLIENT, "generic " + payload, actorInfo, observerId, evidence);
				api.messages().sendServer(1, "selected " + payload);
				assertEquals(1, api.whoAmI().getVirtualServerId());
				assertMessage(messages, TextMessageTargetMode.SERVER, "selected " + payload, actorInfo, observerId, evidence);
				String boundary = "é".repeat(512);
				api.messages().sendPrivate(observerId, boundary);
				assertMessage(messages, TextMessageTargetMode.CLIENT, boundary, actorInfo, observerId, evidence);
				api.messages().sendPrivate(observerId, boundary + "é");
				assertMessage(messages, TextMessageTargetMode.CLIENT, boundary + "é", actorInfo, observerId, evidence);
				evidence.add("private UTF-8 payloads: 1024 and 1026 bytes received intact");
				var invalidNumber = assertThrows(TS3CommandFailedException.class, () -> api.messages().sendPrivate(ClientId.of(Integer.MAX_VALUE), "out of range"));
				evidence.add("out-of-range recipient " + new java.util.TreeMap<>(invalidNumber.getError().getMap()));
				assertEquals(1540, invalidNumber.getError().getId());
				assertEquals("convert error", invalidNumber.getError().getMessage());
				var invalidClient = assertThrows(TS3CommandFailedException.class, () -> api.messages().sendPrivate(ClientId.of(65535), "missing client"));
				evidence.add("invalid recipient " + new java.util.TreeMap<>(invalidClient.getError().getMap()));
				assertEquals(512, invalidClient.getError().getId());
				assertEquals("invalid clientID", invalidClient.getError().getMessage());
				var invalidChannel = assertThrows(TS3CommandFailedException.class, () -> api.messages().sendChannel(65535, "missing channel"));
				evidence.add("invalid channel " + new java.util.TreeMap<>(invalidChannel.getError().getMap()));
				assertEquals(768, invalidChannel.getError().getId());
				assertEquals("invalid channelID", invalidChannel.getError().getMessage());
				var invalidServer = assertThrows(TS3CommandFailedException.class, () -> api.messages().sendServer(65535, "missing server"));
				evidence.add("invalid server " + new java.util.TreeMap<>(invalidServer.getError().getMap()));
				assertEquals(2816, invalidServer.getError().getId());
				assertEquals("virtualserver limit reached", invalidServer.getError().getMessage());
				assertEquals(0, api.whoAmI().getVirtualServerId(), "Failed selection clears the current server on these targets");
				var unselected = assertThrows(TS3CommandFailedException.class, () -> api.messages().sendServer("no current server"));
				evidence.add("unselected server " + new java.util.TreeMap<>(unselected.getError().getMap()));
				assertEquals(1024, unselected.getError().getId());
				assertEquals("invalid serverID", unselected.getError().getMessage());
				api.selectVirtualServerById(1, nickname);
				actorInfo = api.whoAmI();
				assertEquals(1, actorInfo.getVirtualServerId());
				assertEquals(nickname, actorInfo.getNickname());
				// A private event fences earlier sends on the same actor and listener, without a timing-only absence check.
				recipient.events().unregisterAll();
				recipient.events().register(TS3EventType.TEXT_PRIVATE);
				api.messages().sendServer("unsubscribed server");
				api.messages().sendChannel(channel, "unsubscribed channel");
				api.messages().sendPrivate(observerId, "private fence");
				assertMessage(messages, TextMessageTargetMode.CLIENT, "private fence", actorInfo, observerId, evidence);
				assertTrue(messages.isEmpty(), "No failed-target or unsubscribed message may be dispatched before the fence");
			}
			assertFalse(api.instance().version().version().isBlank());
			observer.exit(); actor.exit();
			assertEquals(0, observer.getEventStatistics().malformedNotifications());
			assertEquals(0, observer.getEventStatistics().listenerFailures());
			assertEquals(0, observer.getEventStatistics().droppedEvents());
		} finally {
			Path path = Path.of("target", "compatibility", target + "-messages.txt");
			Files.createDirectories(path.getParent()); Files.write(path, evidence);
		}
	}

	private static void assertMessage(java.util.concurrent.BlockingQueue<TextMessageEvent> messages,
	                                  TextMessageTargetMode mode, String message, ServerQueryInfo actorInfo,
	                                  ClientId observerId, java.util.List<String> evidence) throws InterruptedException {
		var event = messages.poll(5, TimeUnit.SECONDS);
		assertNotNull(event, "Expected " + mode + " notification from the independent actor");
		evidence.add("message " + new java.util.TreeMap<>(event.getMap()));
		assertEquals(mode, event.getTargetMode()); assertEquals(message, event.getMessage());
		assertEquals(Optional.of(actorInfo.getId()), event.getInvokerId()); assertEquals(Optional.of(actorInfo.getNickname()), event.getInvokerName());
		assertEquals(Optional.of(actorInfo.getUniqueIdentifier()), event.getInvokerUniqueId());
		assertEquals(mode == TextMessageTargetMode.CLIENT, event.getMap().containsKey("target"),
			"Only private text notifications supply the recipient target field");
		assertEquals(mode == TextMessageTargetMode.CLIENT ? Optional.of(observerId) : Optional.empty(), event.getTargetClientId());
	}



	@ParameterizedTest(name = "{0}: virtual-server lifecycle")
	@EnumSource(Target.class)
	@Timeout(120)
	void serverLifecycleDomain(Target target) throws Exception {
		var evidence = new java.util.ArrayList<String>();
		try (var server = new TeamSpeakContainer()) {
			server.start();
			try (var query = new TS3Query(server.config(target.protocol))) {
				query.connect(); var api = query.getApi(); server.record(target.name(), api.instance().version());
				for (String command : java.util.List.of("servercreate", "serverstart", "serverstop", "serverdelete")) {
					String help = api.executeRawCommand("help " + command).getRawResponse();
					assertTrue(help.startsWith("Usage: " + command)); evidence.add("help " + command + "\n" + help);
				}
				var initial = api.servers().list(); assertEquals(1, initial.size());
				int originalId = initial.getFirst().getId();
				api.selectVirtualServerById(originalId);
				api.servers().stop(originalId, "owned fixture replacement 日本語 | literal \\s");
				var stoppedOriginal = api.servers().list().getFirst();
				evidence.add("stopped original " + stoppedOriginal.getStatus());
				assertEquals(VirtualServerStatus.OFFLINE, stoppedOriginal.getStatus());
				api.servers().delete(originalId); assertTrue(api.servers().list().isEmpty());
				String name = "lifecycle 日本語 | literal \\s " + target;
				var created = api.servers().create(name, Map.of(VirtualServerProperty.VIRTUALSERVER_MAXCLIENTS, "8"));
				try (var cleanup = new CreatedResource<>(created.id(), id -> {
					var row = api.servers().list().stream().filter(candidate -> candidate.getId() == id).findFirst().orElseThrow();
					if (row.getStatus() != VirtualServerStatus.OFFLINE) api.servers().stop(row.getId());
					api.servers().delete(id);
				})) {
					assertTrue(created.id() > 0); assertTrue(created.port() > 0);
					assertFalse(created.serverAdminToken().isBlank());
					evidence.add("created id=" + created.id() + " port=" + created.port());
					var running = assertThrows(TS3CommandFailedException.class, () -> api.servers().delete(created.id()));
					evidence.add("delete running " + new java.util.TreeMap<>(running.getError().getMap()));
					assertEquals(1035, running.getError().getId()); assertEquals("server got an invalid status for this operation", running.getError().getMessage());
					for (int cycle = 0; cycle < 3; cycle++) {
						var servers = api.servers().list(); assertEquals(1, servers.size());
						var row = servers.getFirst();
						evidence.add("online " + Map.of("id", row.getId(), "port", row.getPort(), "status", row.getStatus(), "name", row.getName()));
						assertEquals(created.id(), row.getId()); assertEquals(created.port(), row.getPort());
						assertEquals(name, row.getName()); assertEquals(java.util.OptionalInt.of(8), row.getMaxClients());
						assertEquals(VirtualServerStatus.ONLINE, row.getStatus());
						api.selectVirtualServerById(created.id()); assertEquals(name, api.servers().info().getName());
						if (cycle == 2) api.servers().stop(row.getId());
						else api.servers().stop(row.getId(), "cycle " + cycle + " 日本語 | literal \\s");
						var stopped = api.servers().list().getFirst();
						assertEquals(VirtualServerStatus.OFFLINE, stopped.getStatus());
						int context = api.whoAmI().getVirtualServerId(); assertEquals(0, context);
						evidence.add("offline " + created.id() + " query context " + context);
						if (cycle == 0) api.servers().start(stopped.getId());
						else if (cycle == 1) api.servers().start(created.id());
					}
				}
				assertTrue(api.servers().list().isEmpty());
				for (var operation : Map.<String, Runnable>of("serverstart", () -> api.servers().start(created.id()),
					"serverstop", () -> api.servers().stop(created.id()), "serverdelete", () -> api.servers().delete(created.id())).entrySet()) {
					var missing = assertThrows(TS3CommandFailedException.class, operation.getValue()::run);
					evidence.add("missing " + operation.getKey() + " " + new java.util.TreeMap<>(missing.getError().getMap()));
					boolean stop = operation.getKey().equals("serverstop");
					assertEquals(stop ? 1033 : 1024, missing.getError().getId());
					assertEquals(stop ? "server is not running" : "invalid serverID", missing.getError().getMessage());
				}
				assertFalse(api.instance().version().version().isBlank()); query.exit();
			}
		} finally {
			Path path = Path.of("target", "compatibility", target + "-server-lifecycle.txt");
			Files.createDirectories(path.getParent()); Files.write(path, evidence);
		}
	}

	@ParameterizedTest(name = "{0}: instance and virtual-server logs")
	@EnumSource(Target.class)
	@Timeout(60)
	void logDomain(Target target) throws Exception {
		TeamSpeakContainer server = ts3;
		var evidence = new java.util.ArrayList<String>();
		try (var query = new TS3Query(server.config(target.protocol))) {
			query.connect(); var api = query.getApi(); var version = api.instance().version(); server.record(target.name(), version);
			for (String command : java.util.List.of("logadd", "logview")) {
				String help = api.executeRawCommand("help " + command).getRawResponse();
				assertTrue(help.startsWith("Usage: " + command)); evidence.add("help " + command + "\n" + help);
			}
			var instanceRows = api.instance().logEntries(100);
			assertTrue(instanceRows.stream().anyMatch(line -> line.contains(version.version())));
			assertTrue(api.servers().logEntries(100).stream().anyMatch(line -> line.contains("|ServerLibPriv")));
			api.selectVirtualServerById(1);
			var virtualRows = api.servers().logEntries(100);
			assertFalse(virtualRows.isEmpty());
			assertTrue(virtualRows.stream().anyMatch(line -> line.contains("|1  |")));
			assertFalse(virtualRows.stream().anyMatch(line -> line.contains("|ServerLibPriv")));
			assertTrue(api.instance().logEntries(100).stream().anyMatch(line -> line.contains("|ServerLibPriv")));
			evidence.add("instance rows " + instanceRows.size() + " virtual rows " + virtualRows.size());
			for (var rows : java.util.List.of(api.instance().logEntries(1), api.servers().logEntries(1))) {
				assertEquals(1, rows.size());
			}
			for (var rows : java.util.List.of(api.instance().logEntries(100), api.servers().logEntries(100),
				api.instance().logEntries(0), api.servers().logEntries(-1))) {
				assertFalse(rows.isEmpty()); assertTrue(rows.size() <= 100);
			}
			for (String command : java.util.List.of("logview lines=1", "logview lines=1 instance=1")) {
				var raw = api.executeRawCommand(command).getFirstResponse().orElseThrow();
				evidence.add(command + " fields " + new java.util.TreeSet<>(raw.getMap().keySet()));
				assertTrue(raw.getMap().containsKey("l")); assertFalse(raw.require("l").isBlank());
				assertTrue(raw.requireLong("last_pos") >= 0); assertTrue(raw.requireLong("file_size") > 0);
			}
			assertThrows(IllegalArgumentException.class, () -> api.instance().logEntries(101));
			assertThrows(IllegalArgumentException.class, () -> api.servers().logEntries(101));
			assertThrows(IllegalArgumentException.class, () -> query.getAsyncApi().instance().logEntries(101));
			assertThrows(IllegalArgumentException.class, () -> query.getAsyncApi().servers().logEntries(101));
			var invalid = assertThrows(TS3CommandFailedException.class, () -> api.executeRawCommand("logadd loglevel=5 logmsg=invalid-log-level"));
			evidence.add("invalid log level " + new java.util.TreeMap<>(invalid.getError().getMap()));
			assertEquals(1538, invalid.getError().getId()); assertEquals("invalid parameter", invalid.getError().getMessage());
			assertFalse(api.instance().version().version().isBlank()); query.exit();
		} finally {
			Path path = Path.of("target", "compatibility", target + "-logs.txt");
			Files.createDirectories(path.getParent()); Files.write(path, evidence);
		}
	}

	@ParameterizedTest(name = "{0}: global query-login administration")
	@EnumSource(Target.class)
	@Timeout(60)
	void queryLoginDomain(Target target) throws Exception {
		var evidence = new java.util.ArrayList<String>();
		try (var server = new TeamSpeakContainer()) {
			server.start();
			try (var administrator = new TS3Query(server.config(target.protocol))) {
				administrator.connect(); var api = administrator.getApi();
				assertEquals(0, api.whoAmI().getVirtualServerId());
				server.record(target.name(), api.instance().version());
				for (String command : java.util.List.of("queryloginadd", "queryloginlist", "querylogindel")) {
					String help = api.executeRawCommand("help " + command).getRawResponse();
					assertTrue(help.startsWith("Usage: " + command)); evidence.add("help " + command + "\n" + help);
				}
				var initial = api.queryLogins().list().stream().map(com.github.theholywaffle.teamspeak3.api.wrapper.QueryLogin::getMap).toList();
				String name = "global-query-" + target;
				var created = api.queryLogins().create(name);
				try (var cleanup = new CreatedResource<>(created.getClientDatabaseId(), api.queryLogins()::delete)) {
					assertTrue(created.getClientDatabaseId().value() > 0);
					assertFalse(created.getQueryClientLoginPassword().isBlank());
					evidence.add("created fields " + new java.util.TreeSet<>(created.getMap().keySet()));
					assertEquals(0, created.getVirtualServerId()); assertEquals(name, created.getQueryClientLoginName());
					var all = api.queryLogins().list(); assertEquals(initial.size() + 1, all.size());
					assertEquals(1, all.stream().filter(row -> row.getClientDatabaseId().equals(created.getClientDatabaseId()) && row.getVirtualServerId() == 0 && row.getQueryClientLoginName().equals(name)).count());
					var listed = api.queryLogins().search(name); assertEquals(1, listed.size());
					var row = listed.getFirst(); evidence.add("listed fields " + new java.util.TreeSet<>(row.getMap().keySet()));
					evidence.add("listed " + Map.of("cldbid", row.getClientDatabaseId(), "sid", row.getVirtualServerId(), "name", row.getQueryClientLoginName()));
					assertEquals(created.getClientDatabaseId(), row.getClientDatabaseId());
					assertEquals(0, row.getVirtualServerId()); assertEquals(name, row.getQueryClientLoginName());
					try (var actor = new TS3Query(server.config(target.protocol).setLoginCredentials(name, created.getQueryClientLoginPassword()))) {
						actor.connect(); var identity = actor.getApi().whoAmI();
						evidence.add("authenticated " + new java.util.TreeMap<>(identity.getMap()));
						assertEquals(name, identity.getLoginName());
						assertEquals(created.getClientDatabaseId(), identity.getDatabaseId());
						assertEquals(0, identity.getOriginServerId()); assertEquals(0, identity.getVirtualServerId());
						assertFalse(actor.getApi().instance().version().version().isBlank());
						actor.exit();
					}
				}
				assertEquals(initial, api.queryLogins().list().stream().map(com.github.theholywaffle.teamspeak3.api.wrapper.QueryLogin::getMap).toList());
				var missing = assertThrows(TS3CommandFailedException.class, () -> api.queryLogins().delete(created.getClientDatabaseId()));
				evidence.add("delete removed login " + new java.util.TreeMap<>(missing.getError().getMap()));
				assertEquals(512, missing.getError().getId()); assertEquals("invalid clientID", missing.getError().getMessage());
				try (var rejected = new TS3Query(server.config(target.protocol).setLoginCredentials(name, created.getQueryClientLoginPassword()))) {
					var failure = assertThrows(TS3ConnectionFailedException.class, rejected::connect);
					assertLoginAuthenticationFailure(failure, target, evidence);
					assertFalse(rejected.isConnected());
				}
				assertFalse(api.instance().version().version().isBlank()); administrator.exit();
			}
		} finally {
			Path path = Path.of("target", "compatibility", target + "-query-logins.txt");
			Files.createDirectories(path.getParent()); Files.write(path, evidence);
		}
	}

	static void assertLoginAuthenticationFailure(Throwable failure, Target target, java.util.List<String> evidence) {
		for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
			if (target.protocol == TS3Query.Protocol.RAW && cause instanceof TS3CommandFailedException command) {
				evidence.add("credentials rejected " + new java.util.TreeMap<>(command.getError().getMap()));
				assertEquals(520, command.getError().getId());
				assertEquals("invalid loginname or password", command.getError().getMessage());
				return;
			}
			if (target.protocol == TS3Query.Protocol.SSH && cause instanceof net.schmizz.sshj.userauth.UserAuthException) {
				evidence.add("credentials rejected " + cause.getClass().getSimpleName());
				return;
			}
		}
		fail("Expected an authentication-specific failure", failure);
	}

	@ParameterizedTest(name = "{0}: named group copies and overwrites")
	@EnumSource(Target.class)
	@Timeout(60)
	void groupCopyDomain(Target target) throws Exception {
		TeamSpeakContainer server = ts3;
		var evidence = new java.util.ArrayList<String>();
		var regular = com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType.REGULAR;
		try (var query = new TS3Query(server.config(target.protocol))) {
			query.connect(); var api = query.getApi(); api.selectVirtualServerById(1);
			server.record(target.name(), api.instance().version());
			ClientDatabaseId queryDatabaseId = api.whoAmI().getDatabaseId();
			var queryPermissions = assertThrows(TS3CommandFailedException.class, () -> api.permissions().assigned(PermissionTarget.client(queryDatabaseId)));
			evidence.add("query database " + queryDatabaseId + " clientpermlist " + new java.util.TreeMap<>(queryPermissions.getError().getMap()));
			assertEquals(512, queryPermissions.getError().getId()); assertEquals("invalid clientID", queryPermissions.getError().getMessage());
			for (String command : java.util.List.of("servergroupcopy", "channelgroupcopy")) {
				String help = api.executeRawCommand("help " + command).getRawResponse();
				assertTrue(help.startsWith("Usage: " + command)); evidence.add("help " + command + "\n" + help);
			}
			String name = IPermissionType.I_CHANNEL_NEEDED_JOIN_POWER.getName();
			String extra = BPermissionType.B_CHANNEL_CREATE_PERMANENT.getName();
			var initialServerIds = api.serverGroups().list().stream().map(g -> g.getId()).collect(java.util.stream.Collectors.toSet());
			var initialChannelIds = api.channelGroups().list().stream().map(g -> g.getId()).collect(java.util.stream.Collectors.toSet());
			int removedServer, removedChannel;
			try (var sourceServer = new CreatedResource<>(api.serverGroups().create("copy-source-server-" + target), id -> api.serverGroups().delete(id, true));
				 var sourceChannel = new CreatedResource<>(api.channelGroups().create("copy-source-channel-" + target), id -> api.channelGroups().delete(id, true));
				 var targetServer = new CreatedResource<>(api.serverGroups().create("copy-target-server-" + target), id -> api.serverGroups().delete(id, true));
				 var targetChannel = new CreatedResource<>(api.channelGroups().create("copy-target-channel-" + target), id -> api.channelGroups().delete(id, true))) {
				removedServer = sourceServer.id(); removedChannel = sourceChannel.id();
				api.permissions().add(PermissionTarget.serverGroup(sourceServer.id()), PermissionGrant.of(name, 31).withNegated().withSkip());
				api.permissions().add(PermissionTarget.channelGroup(sourceChannel.id()), PermissionGrant.of(name, 41));
				String copiedName = "copy 日本語 | literal \\s " + target;
				try (var copyServer = new CreatedResource<>(api.serverGroups().copy(sourceServer.id(), copiedName, regular), id -> api.serverGroups().delete(id, true));
					 var copyChannel = new CreatedResource<>(api.channelGroups().copy(sourceChannel.id(), copiedName, regular), id -> api.channelGroups().delete(id, true))) {
					assertNotEquals(sourceServer.id(), copyServer.id()); assertNotEquals(sourceChannel.id(), copyChannel.id());
					var serverRow = api.serverGroups().list().stream().filter(g -> g.getId() == copyServer.id()).findFirst().orElseThrow();
					var channelRow = api.channelGroups().list().stream().filter(g -> g.getId() == copyChannel.id()).findFirst().orElseThrow();
					evidence.add("server copy " + new java.util.TreeMap<>(serverRow.getMap()));
					evidence.add("channel copy " + new java.util.TreeMap<>(channelRow.getMap()));
					assertEquals(copiedName, serverRow.getName()); assertEquals(copiedName, channelRow.getName());
					assertEquals(regular, serverRow.getType()); assertEquals(regular, channelRow.getType());
					assertPermission(api.permissions().assigned(PermissionTarget.serverGroup(copyServer.id())), name, 31, true, true, evidence, "server copy permission");
					assertPermission(api.permissions().assigned(PermissionTarget.channelGroup(copyChannel.id())), name, 41, false, false, evidence, "channel copy permission");
					api.permissions().add(PermissionTarget.serverGroup(sourceServer.id()), PermissionGrant.of(name, 33));
					api.permissions().add(PermissionTarget.channelGroup(sourceChannel.id()), PermissionGrant.of(name, 43));
					assertPermission(api.permissions().assigned(PermissionTarget.serverGroup(copyServer.id())), name, 31, true, true, evidence, "independent server copy");
					assertPermission(api.permissions().assigned(PermissionTarget.channelGroup(copyChannel.id())), name, 41, false, false, evidence, "independent channel copy");
					api.permissions().add(PermissionTarget.serverGroup(targetServer.id()), PermissionGrant.of(extra, 1));
					api.permissions().add(PermissionTarget.channelGroup(targetChannel.id()), PermissionGrant.of(extra, 1));
					api.serverGroups().overwrite(sourceServer.id(), targetServer.id(), regular);
					api.channelGroups().overwrite(sourceChannel.id(), targetChannel.id(), regular);
					var serverPermissions = api.permissions().assigned(PermissionTarget.serverGroup(targetServer.id()));
					var channelPermissions = api.permissions().assigned(PermissionTarget.channelGroup(targetChannel.id()));
					assertEquals(permissionStates(api.permissions().assigned(PermissionTarget.serverGroup(sourceServer.id()))), permissionStates(serverPermissions));
					assertEquals(permissionStates(api.permissions().assigned(PermissionTarget.channelGroup(sourceChannel.id()))), permissionStates(channelPermissions));
					assertTrue(serverPermissions.stream().noneMatch(p -> p.name().equals(extra)));
					assertTrue(channelPermissions.stream().noneMatch(p -> p.name().equals(extra)));
					assertPermission(serverPermissions, name, 33, false, false, evidence, "server overwrite");
					assertPermission(channelPermissions, name, 43, false, false, evidence, "channel overwrite");
				}
			}
			assertEquals(initialServerIds, api.serverGroups().list().stream().map(g -> g.getId()).collect(java.util.stream.Collectors.toSet()));
			assertEquals(initialChannelIds, api.channelGroups().list().stream().map(g -> g.getId()).collect(java.util.stream.Collectors.toSet()));
			var serverError = assertThrows(TS3CommandFailedException.class, () -> api.serverGroups().copy(removedServer, "missing server", regular));
			var channelError = assertThrows(TS3CommandFailedException.class, () -> api.channelGroups().copy(removedChannel, "missing channel", regular));
			evidence.add("removed server source " + new java.util.TreeMap<>(serverError.getError().getMap()));
			evidence.add("removed channel source " + new java.util.TreeMap<>(channelError.getError().getMap()));
			for (var error : java.util.List.of(serverError, channelError)) {
				assertEquals(2560, error.getError().getId()); assertEquals("invalid group ID", error.getError().getMessage());
			}
			assertFalse(api.instance().version().version().isBlank()); query.exit();
		} finally {
			Path path = Path.of("target", "compatibility", target + "-group-copy.txt");
			Files.createDirectories(path.getParent()); Files.write(path, evidence);
		}
	}

	private record PermissionState(String name, int value, boolean negated, boolean skipped) { }

	private static java.util.Set<PermissionState> permissionStates(java.util.List<Permission> permissions) {
		return permissions.stream().map(p -> new PermissionState(p.name(), p.value(), p.negated(), p.skipped()))
			.collect(java.util.stream.Collectors.toSet());
	}

	@ParameterizedTest(name = "{0}: permission metadata and named channel/group assignments")
	@EnumSource(Target.class)
	@Timeout(60)
	void permissionDomain(Target target) throws Exception {
		TeamSpeakContainer server = ts3;
		var evidence = new java.util.ArrayList<String>();
		try (var query = new TS3Query(server.config(target.protocol))) {
			query.connect();
			var api = query.getApi();
			api.selectVirtualServerById(1);
			server.record(target.name(), api.instance().version());
			for (String command : java.util.List.of("permissionlist", "permidgetbyname", "permget", "permfind", "channeladdperm",
				"channelpermlist", "channeldelperm", "servergroupadd", "servergrouplist", "servergrouprename", "servergroupdel",
				"servergroupaddperm", "servergrouppermlist", "servergroupdelperm", "channelgroupadd", "channelgrouplist",
				"channelgrouprename", "channelgroupdel", "channelgroupaddperm", "channelgrouppermlist", "channelgroupdelperm")) {
				String help = api.executeRawCommand("help " + command).getRawResponse();
				assertTrue(help.startsWith("Usage: " + command), "Live help must describe " + command);
				evidence.add("help " + command + "\n" + help);
			}
			var metadata = api.permissions().list();
			assertFalse(metadata.isEmpty());
			var byName = new java.util.HashMap<String, PermissionInfo>();
			var ids = new java.util.HashSet<Integer>();
			for (var permission : metadata) {
				assertFalse(permission.name().isBlank()); assertTrue(permission.id() > 0);
				assertNull(byName.put(permission.name(), permission), "Permission names must be unique");
				assertTrue(ids.add(permission.id()), "Permission IDs must be unique on this server");
			}
			metadata.stream().sorted(java.util.Comparator.comparing(PermissionInfo::name))
				.forEach(p -> evidence.add("metadata " + p));
			String integerName = IPermissionType.I_CHANNEL_NEEDED_JOIN_POWER.getName();
			String booleanName = BPermissionType.B_CHANNEL_CREATE_PERMANENT.getName();
			assertTrue(byName.containsKey(integerName)); assertTrue(byName.containsKey(booleanName));
			assertEquals(2562, assertThrows(TS3CommandFailedException.class, () -> api.permissions().idByName("i_issue441_nonexistent")).getError().getId());
			assertEquals(byName.get(integerName).id(), api.permissions().idByName(integerName));
			assertEquals(List.of(byName.get(booleanName).id(), byName.get(integerName).id()),
				api.permissions().idsByName(booleanName, integerName));
			assertEquals(List.of(api.permissions().ownValue(booleanName), api.permissions().ownValue(integerName)),
				api.permissions().ownValues(booleanName, integerName));
			try (var channels = new ChannelCleanup(api);
				 var serverGroup = new CreatedResource<>(api.serverGroups().create("permissions " + target + " | \\s"), id -> api.serverGroups().delete(id, true));
				 var channelGroup = new CreatedResource<>(api.channelGroups().create("permissions " + target + " | \\s"), id -> api.channelGroups().delete(id, true))) {
				int channel = channels.track(api.channels().create("permission-channel-" + target, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1")));
				assertEquals("permissions " + target + " | \\s", api.serverGroups().list().stream().filter(g -> g.getId() == serverGroup.id()).findFirst().orElseThrow().getName());
				assertEquals("permissions " + target + " | \\s", api.channelGroups().list().stream().filter(g -> g.getId() == channelGroup.id()).findFirst().orElseThrow().getName());
				api.permissions().add(PermissionTarget.channel(channel), PermissionGrant.of(integerName, 17));
				assertPermission(api.permissions().assigned(PermissionTarget.channel(channel)), integerName, 17, false, false, evidence, "channel");
				api.permissions().add(PermissionTarget.channel(channel), PermissionGrant.of(integerName, 23));
				assertPermission(api.permissions().assigned(PermissionTarget.channel(channel)), integerName, 23, false, false, evidence, "channel update");
				api.permissions().delete(PermissionTarget.channel(channel), integerName);
				assertTrue(api.permissions().assigned(PermissionTarget.channel(channel)).stream().noneMatch(p -> p.name().equals(integerName)));
				api.permissions().add(PermissionTarget.serverGroup(serverGroup.id()), PermissionGrant.of(integerName, 31).withNegated().withSkip());
				assertPermission(api.permissions().assigned(PermissionTarget.serverGroup(serverGroup.id())), integerName, 31, true, true, evidence, "server group");
				var assignment = api.permissions().assignments(integerName).stream().filter(p ->
					p.getType() == PermissionGroupType.SERVER_GROUP && p.getMajorId() == serverGroup.id()).findFirst().orElseThrow();
				assertEquals(byName.get(integerName).id(), assignment.getId());
				assertFalse(assignment.getMap().containsKey("v"), "permfind returns identities, not assignment values");
				assertFalse(assignment.getMap().containsKey("n"));
				assertFalse(assignment.getMap().containsKey("s"));
				assertEquals(java.util.OptionalInt.empty(), assignment.getValue());
				assertEquals(Optional.empty(), assignment.isNegated()); assertEquals(Optional.empty(), assignment.isSkipped());
				evidence.add("assignment search " + new java.util.TreeMap<>(assignment.getMap()));
				api.permissions().add(PermissionTarget.serverGroup(serverGroup.id()), PermissionGrant.of(integerName, 37));
				assertPermission(api.permissions().assigned(PermissionTarget.serverGroup(serverGroup.id())), integerName, 37, false, false, evidence, "server group update");
				api.permissions().add(PermissionTarget.serverGroup(serverGroup.id()), PermissionGrant.of(booleanName, 1).withSkip());
				assertPermission(api.permissions().assigned(PermissionTarget.serverGroup(serverGroup.id())), booleanName, 1, false, true, evidence, "server group boolean");
				api.permissions().delete(PermissionTarget.serverGroup(serverGroup.id()), booleanName);
				assertTrue(api.permissions().assigned(PermissionTarget.serverGroup(serverGroup.id())).stream().noneMatch(p -> p.name().equals(booleanName)));
				api.permissions().delete(PermissionTarget.serverGroup(serverGroup.id()), integerName);
				assertTrue(api.permissions().assigned(PermissionTarget.serverGroup(serverGroup.id())).stream().noneMatch(p -> p.name().equals(integerName)));
				api.permissions().add(PermissionTarget.channelGroup(channelGroup.id()), PermissionGrant.of(integerName, 41));
				assertPermission(api.permissions().assigned(PermissionTarget.channelGroup(channelGroup.id())), integerName, 41, false, false, evidence, "channel group");
				api.permissions().add(PermissionTarget.channelGroup(channelGroup.id()), PermissionGrant.of(integerName, 43));
				assertPermission(api.permissions().assigned(PermissionTarget.channelGroup(channelGroup.id())), integerName, 43, false, false, evidence, "channel group update");
				api.permissions().delete(PermissionTarget.channelGroup(channelGroup.id()), integerName);
				assertTrue(api.permissions().assigned(PermissionTarget.channelGroup(channelGroup.id())).stream().noneMatch(p -> p.name().equals(integerName)));
				api.serverGroups().rename(serverGroup.id(), "renamed server " + target);
				assertEquals("renamed server " + target, api.serverGroups().list().stream().filter(g -> g.getId() == serverGroup.id()).findFirst().orElseThrow().getName());
				api.channelGroups().rename(channelGroup.id(), "renamed channel " + target);
				assertEquals("renamed channel " + target, api.channelGroups().list().stream().filter(g -> g.getId() == channelGroup.id()).findFirst().orElseThrow().getName());
				var error = assertThrows(TS3CommandFailedException.class, () -> api.permissions().add(PermissionTarget.channel(channel), PermissionGrant.of("i_issue441_missing_permission", 1)));
				evidence.add("unknown permission " + new java.util.TreeMap<>(error.getError().getMap()));
				assertEquals(2562, error.getError().getId());
				assertEquals("invalid permission ID", error.getError().getMessage());
				var lookupError = assertThrows(TS3CommandFailedException.class, () -> api.permissions().idByName("i_issue441_missing_permission"));
				assertEquals(2562, lookupError.getError().getId());
				var findError = assertThrows(TS3CommandFailedException.class, () -> api.permissions().assignments("i_issue441_missing_permission"));
				assertEquals(2562, findError.getError().getId());
				assertEquals(byName.get(integerName).id(), api.permissions().idByName(integerName));
			}
			assertTrue(api.serverGroups().list().stream().noneMatch(g -> g.getName().equals("renamed server " + target)));
			assertTrue(api.channelGroups().list().stream().noneMatch(g -> g.getName().equals("renamed channel " + target)));
			query.exit();
		} finally {
			Path path = Path.of("target", "compatibility", target + "-permissions.txt");
			Files.createDirectories(path.getParent()); Files.write(path, evidence);
		}
	}

	private static void assertPermission(java.util.List<Permission> assignments,
	                                    String name, int value, boolean negated, boolean skipped,
	                                    java.util.List<String> evidence, String label) {
		var permission = CompatibilityAssertions.permission(assignments, name, value, negated, skipped);
		evidence.add(label + " " + permission);
	}

	record CreatedResource<T>(T id, java.util.function.Consumer<T> deletion) implements AutoCloseable {
		@Override public void close() { deletion.accept(id); }
	}

	private static final class ChannelCleanup implements AutoCloseable {
		private final TS3Api api;
		private final java.util.List<Integer> ids = new java.util.ArrayList<>();

		ChannelCleanup(TS3Api api) { this.api = api; }

		int track(int id) { ids.add(id); return id; }

		void delete(int id) {
			api.channels().delete(id, true);
			ids.remove(Integer.valueOf(id));
		}

		@Override public void close() {
			RuntimeException failure = null;
			for (int i = ids.size() - 1; i >= 0; --i) {
				try { api.channels().delete(ids.get(i), true); }
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
			assertFalse(query.getApi().instance().version().version().isBlank());
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
	@EnumSource(value = Target.class, names = {"TS3_SSH"})
	void sshTransportShutdownDoesNotJoinConnectingCaller(Target target) throws Exception {
		TeamSpeakContainer server = ts3;
		var config = server.config(target.protocol).build();
		var transport = config.getTransportFactory().create(config);
		try (transport) { transport.connect(() -> {}); }
		transport.awaitTermination(Duration.ofSeconds(1));
		assertTrue(transport.isTerminated(), "Transport shutdown must exclude the still-running connecting caller");
	}

	@ParameterizedTest(name = "{0}: SSH authentication and host-key regressions")
	@EnumSource(value = Target.class, names = {"TS3_SSH"})
	void sshSecurity(Target target) throws Exception {
		TeamSpeakContainer server = ts3;
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
