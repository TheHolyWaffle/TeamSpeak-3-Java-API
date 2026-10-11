package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
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
			assertEquals(2816, error.getError().getId()); // Selection is rejected by this fixture.
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


	@ParameterizedTest(name = "{0}: ban-rule administration and errors")
	@EnumSource(Target.class)
	@Timeout(60)
	void banRuleDomain(Target target) throws Exception {
		TeamSpeakContainer server = target.ts6 ? ts6 : ts3;
		var evidence = new java.util.ArrayList<String>();
		try (var query = new TS3Query(server.config(target.protocol))) {
			query.connect();
			var api = query.getApi(); api.selectVirtualServerById(1);
			var identity = api.whoAmI();
			server.record(target.name(), api.getVersion());
			for (String command : java.util.List.of("banadd", "banlist", "bandel")) {
				String help = api.executeRawCommand("help " + command).getRawResponse();
				assertTrue(help.startsWith("Usage: " + command)); evidence.add("help " + command + "\n" + help);
			}
			var initial = api.getBans().stream().map(com.github.theholywaffle.teamspeak3.api.wrapper.Ban::getId).collect(java.util.stream.Collectors.toSet());
			String name = "^unconnected-ban-" + target + "$";
			String ip = "198[.]51[.]100[.]44";
			String uid = "AAAAAAAAAAAAAAAAAAAAAAAAAAA=";
			String reason = "ban 日本語 | literal \\s\\p\\n /";
			int deletedId;
			try (var nameBan = new CreatedResource(api.addBan(null, name, null, 0, reason), api::deleteBan);
				 var ipBan = new CreatedResource(api.addBan(ip, null, null, (String) null, 3600, reason), api::deleteBan);
				 var uidBan = new CreatedResource(api.addBan(null, null, uid, 0, null), api::deleteBan)) {
				deletedId = nameBan.id();
				var bans = api.getBans(); assertEquals(initial.size() + 3, bans.size());
				var expected = new java.util.HashSet<>(initial);
				expected.addAll(java.util.List.of(nameBan.id(), ipBan.id(), uidBan.id()));
				assertEquals(expected, bans.stream().map(com.github.theholywaffle.teamspeak3.api.wrapper.Ban::getId).collect(java.util.stream.Collectors.toSet()));
				for (var ban : bans) {
					if (initial.contains(ban.getId())) continue;
					evidence.add("ban " + new java.util.TreeMap<>(ban.getMap()));
					assertTrue(Math.abs(System.currentTimeMillis() - ban.getCreatedDate().getTime()) < 60_000);
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
			assertEquals(initial, api.getBans().stream().map(com.github.theholywaffle.teamspeak3.api.wrapper.Ban::getId).collect(java.util.stream.Collectors.toSet()));
			var missing = assertThrows(TS3CommandFailedException.class, () -> api.deleteBan(deletedId));
			evidence.add("delete removed ban " + new java.util.TreeMap<>(missing.getError().getMap()));
			assertEquals(3328, missing.getError().getId()); assertEquals("invalid ban id", missing.getError().getMessage());
			assertFalse(api.getVersion().getVersion().isBlank()); query.exit();
		} finally {
			Path path = Path.of("target", "compatibility", target + "-ban-rules.txt");
			Files.createDirectories(path.getParent()); Files.write(path, evidence);
		}
	}

	@ParameterizedTest(name = "{0}: privilege key administration and query-client rejection")
	@EnumSource(Target.class)
	@Timeout(60)
	void privilegeKeyDomain(Target target) throws Exception {
		TeamSpeakContainer server = target.ts6 ? ts6 : ts3;
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
			int observerId = api.whoAmI().getId();
			assertNotEquals(observerId, identity.getId());
			server.record(target.name(), api.getVersion());
			for (String command : java.util.List.of("privilegekeyadd", "privilegekeylist", "privilegekeydelete", "privilegekeyuse")) {
				String help = api.executeRawCommand("help " + command).getRawResponse();
				assertTrue(help.startsWith("Usage: " + command), "Live help must describe " + command);
				evidence.add("help " + command + "\n" + help);
			}
			var initial = api.getPrivilegeKeys().stream().map(PrivilegeKey::getToken).collect(java.util.stream.Collectors.toSet());
			api.registerEvent(TS3EventType.SERVER); api.registerEvent(TS3EventType.TEXT_PRIVATE);
			try (var channels = new ChannelCleanup(api);
				 var serverGroup = new CreatedResource(api.addServerGroup("key-server-" + target), id -> api.deleteServerGroup(id, true));
				 var channelGroup = new CreatedResource(api.addChannelGroup("key-channel-" + target), id -> api.deleteChannelGroup(id, true));
				 var subscription = observer.subscribe(new TS3EventAdapter() {
					@Override public void onPrivilegeKeyUsed(PrivilegeKeyUsedEvent event) { assertTrue(used.offer(event)); }
					@Override public void onTextMessage(TextMessageEvent event) { assertTrue(messages.offer(event)); }
				 })) {
				int channel = channels.track(api.createChannel("key-channel-" + target, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1")));
				String description = "key 日本語 | literal \\s\\p\\n /";
				String deletedToken;
				try (var serverKey = new CreatedPrivilegeKey(api, api.addPrivilegeKeyServerGroup(serverGroup.id(), description));
					 var channelKey = new CreatedPrivilegeKey(api, api.addPrivilegeKeyChannelGroup(channelGroup.id(), channel, description));
					 var genericKey = new CreatedPrivilegeKey(api, api.addPrivilegeKey(PrivilegeKeyType.SERVER_GROUP, serverGroup.id(), 0, null))) {
					deletedToken = serverKey.token();
					var listed = api.getPrivilegeKeys();
					assertEquals(initial.size() + 3, listed.size());
					var serverEntry = listed.stream().filter(k -> k.getToken().equals(serverKey.token())).findFirst().orElseThrow();
					assertPrivilegeKey(serverEntry, PrivilegeKeyType.SERVER_GROUP, serverGroup.id(), 0, description, evidence);
					var channelEntry = listed.stream().filter(k -> k.getToken().equals(channelKey.token())).findFirst().orElseThrow();
					assertPrivilegeKey(channelEntry, PrivilegeKeyType.CHANNEL_GROUP, channelGroup.id(), channel, description, evidence);
					var genericEntry = listed.stream().filter(k -> k.getToken().equals(genericKey.token())).findFirst().orElseThrow();
					assertPrivilegeKey(genericEntry, PrivilegeKeyType.SERVER_GROUP, serverGroup.id(), 0, "", evidence);
					var serverUse = assertThrows(TS3CommandFailedException.class, () -> actor.getApi().usePrivilegeKey(serverEntry));
					evidence.add("server key query use " + new java.util.TreeMap<>(serverUse.getError().getMap()));
					assertEquals(516, serverUse.getError().getId()); assertEquals("invalid client type", serverUse.getError().getMessage());
					var channelUse = assertThrows(TS3CommandFailedException.class, () -> actor.getApi().usePrivilegeKey(channelKey.token()));
					evidence.add("channel key query use " + new java.util.TreeMap<>(channelUse.getError().getMap()));
					assertEquals(516, channelUse.getError().getId()); assertEquals("invalid client type", channelUse.getError().getMessage());
					// The later actor message fences preceding notification frames on this ordered subscription.
					actor.getApi().sendPrivateMessage(observerId, "rejected-key-use-fence");
					assertMessage(messages, TextMessageTargetMode.CLIENT, "rejected-key-use-fence", identity, observerId, evidence);
					assertTrue(used.isEmpty(), "Rejected consumption must not produce a privilege-key-used event");
					var retained = api.getPrivilegeKeys().stream().map(PrivilegeKey::getToken).collect(java.util.stream.Collectors.toSet());
					assertTrue(retained.containsAll(java.util.List.of(serverKey.token(), channelKey.token(), genericKey.token())));
				}
				var restored = api.getPrivilegeKeys().stream().map(PrivilegeKey::getToken).collect(java.util.stream.Collectors.toSet());
				assertEquals(initial, restored);
				var missing = assertThrows(TS3CommandFailedException.class, () -> api.deletePrivilegeKey(deletedToken));
				evidence.add("delete removed key " + new java.util.TreeMap<>(missing.getError().getMap()));
				assertEquals(3, missing.getError().getId()); assertEquals("ok", missing.getError().getMessage());
				var deletedUse = assertThrows(TS3CommandFailedException.class, () -> actor.getApi().usePrivilegeKey(deletedToken));
				evidence.add("removed key query use " + new java.util.TreeMap<>(deletedUse.getError().getMap()));
				assertEquals(516, deletedUse.getError().getId()); assertEquals("invalid client type", deletedUse.getError().getMessage());
				assertFalse(api.getVersion().getVersion().isBlank());
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
		assertTrue(Math.abs(System.currentTimeMillis() - key.getCreated().getTime()) < 60_000);
	}

	private record CreatedPrivilegeKey(TS3Api api, String token) implements AutoCloseable {
		@Override public void close() { api.deletePrivilegeKey(token); }
	}

	@ParameterizedTest(name = "{0}: file directory commands and metadata")
	@EnumSource(Target.class)
	@Timeout(60)
	void fileDomain(Target target) throws Exception {
		TeamSpeakContainer server = target.ts6 ? ts6 : ts3;
		var evidence = new java.util.ArrayList<String>();
		try (var query = new TS3Query(server.config(target.protocol))) {
			query.connect();
			var api = query.getApi();
			api.selectVirtualServerById(1);
			server.record(target.name(), api.getVersion());
			for (String command : java.util.List.of("ftcreatedir", "ftgetfilelist", "ftgetfileinfo", "ftrenamefile", "ftdeletefile", "ftlist")) {
				String help = api.executeRawCommand("help " + command).getRawResponse();
				assertTrue(help.startsWith("Usage: " + command), "Live help must describe " + command);
				evidence.add("help " + command + "\n" + help);
			}
			try (var channels = new ChannelCleanup(api)) {
				int source = channels.track(api.createChannel("file-source-" + target,
					Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1")));
				String password = "file password | \\s";
				int destination = channels.track(api.createChannel("file-destination-" + target,
					Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1", ChannelProperty.CHANNEL_PASSWORD, password)));
				assertTrue(api.getChannelInfo(destination).hasPassword());
				assertEquals(1, api.getPermissionValue(BPermissionType.B_FT_IGNORE_PASSWORD.getName()));
				evidence.add("effective b_ft_ignore_password=1; destination channel is password protected");
				assertTrue(api.getFileList("/", source).isEmpty());
				assertTrue(api.getFileList("/", destination, password).isEmpty());
				assertTrue(api.getFileTransfers().isEmpty());
				var rejected = assertThrows(TS3CommandFailedException.class, () -> api.createFileDirectory("/literal\\s", source));
				evidence.add("invalid path " + new java.util.TreeMap<>(rejected.getError().getMap()));
				assertEquals(2054, rejected.getError().getId()); assertEquals("invalid file path", rejected.getError().getMessage());
				assertTrue(api.getFileList("/", source).isEmpty());
				String pipePath = "/directory | pipe";
				api.createFileDirectory(pipePath, source);
				var pipe = api.getFileList("/", source).getFirst();
				evidence.add("pipe path " + new java.util.TreeMap<>(pipe.getMap()));
				assertEquals(pipePath, pipe.getPath());
				api.deleteFile(pipePath, source);
				String unicodePath = "/directory 日本語 spaces";
				if (target.ts6) {
					var unicode = assertThrows(TS3CommandFailedException.class, () -> api.createFileDirectory(unicodePath, source));
					evidence.add("Unicode path " + new java.util.TreeMap<>(unicode.getError().getMap()));
					assertEquals(2054, unicode.getError().getId()); assertEquals("invalid file path", unicode.getError().getMessage());
				} else {
					api.createFileDirectory(unicodePath, source);
					var unicode = api.getFileList("/", source).getFirst();
					evidence.add("Unicode path " + new java.util.TreeMap<>(unicode.getMap()));
					assertEquals(unicodePath, unicode.getPath());
					api.deleteFile(unicodePath, source);
				}
				assertTrue(api.getFileList("/", source).isEmpty());
				String name = "directory spaces";
				String path = "/" + name;
				api.createFileDirectory(name, source);
				var directory = api.getFileList("/", source).getFirst();
				evidence.add("root directory " + new java.util.TreeMap<>(directory.getMap()));
				assertEquals(name, directory.getName()); assertEquals(path, directory.getPath());
				assertEquals("/", directory.getParentPath()); assertEquals(source, directory.getChannelId());
				assertEquals(0, directory.getType()); assertTrue(directory.isDirectory()); assertFalse(directory.isFile());
				var timestampUnit = target.ts6 ? TimeUnit.MILLISECONDS : TimeUnit.SECONDS;
				long modified = directory.getLastModifiedDate(timestampUnit).getTime();
				assertTrue(Math.abs(System.currentTimeMillis() - modified) < 60_000, "New directory timestamp must represent current time");
				assertFalse(directory.isStillUploading()); assertFalse(directory.getMap().containsKey("incompletesize"));
				assertTrue(api.getFileList(name, source).isEmpty());
				api.createFileDirectory(path + "/child", source, null);
				var nested = api.getFileList(path, source).getFirst();
				evidence.add("nested directory " + new java.util.TreeMap<>(nested.getMap()));
				assertEquals("child", nested.getName()); assertEquals(path + "/child", nested.getPath());
				assertEquals(path + "/", nested.getParentPath()); assertTrue(nested.isDirectory());
				var directoryInfo = assertThrows(TS3CommandFailedException.class, () -> api.getFileInfo(path, source));
				evidence.add("directory fileinfo " + new java.util.TreeMap<>(directoryInfo.getError().getMap()));
				assertEquals(2048, directoryInfo.getError().getId()); assertEquals("invalid file name", directoryInfo.getError().getMessage());
				String renamed = "/renamed spaces";
				api.moveFile(path, renamed, source);
				assertEquals(renamed, api.getFileList("/", source).getFirst().getPath());
				assertEquals(renamed + "/child", api.getFileList(renamed, source).getFirst().getPath());
				api.moveFile(renamed, "/moved", source, null, destination, password);
				assertTrue(api.getFileList("/", source).isEmpty());
				assertEquals("/moved", api.getFileList("/", destination, password).getFirst().getPath());
				assertEquals("/moved/child", api.getFileList("moved", destination, password).getFirst().getPath());
				api.deleteFile("/moved/child", destination, password);
				assertTrue(api.getFileList("/moved", destination, password).isEmpty());
				api.createFileDirectory("/second", destination, password);
				api.deleteFiles(new String[]{"/moved", "/second"}, destination, password);
				assertTrue(api.getFileList("/", destination, password).isEmpty());
				var missing = assertThrows(TS3CommandFailedException.class, () -> api.getFileInfo("/absent.txt", source));
				evidence.add("missing file " + new java.util.TreeMap<>(missing.getError().getMap()));
				assertEquals(target.ts6 ? 2051 : 2052, missing.getError().getId());
				assertEquals(target.ts6 ? "file not found" : "file input/output error", missing.getError().getMessage());
				var missingDirectory = assertThrows(TS3CommandFailedException.class, () -> api.getFileList("/absent", source));
				evidence.add("missing directory " + new java.util.TreeMap<>(missingDirectory.getError().getMap()));
				assertEquals(target.ts6 ? 2051 : 2052, missingDirectory.getError().getId());
				assertEquals(target.ts6 ? "file not found" : "file input/output error", missingDirectory.getError().getMessage());
				assertTrue(api.getFileList("/", destination, "wrong").isEmpty());
				evidence.add("serveradmin can list the password-protected channel with an incorrect password");
				assertTrue(api.getFileList("/", destination, password).isEmpty());
				assertFalse(api.getVersion().getVersion().isBlank());
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
		TeamSpeakContainer server = target.ts6 ? ts6 : ts3;
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
			server.record(target.name(), api.getVersion());
			for (String command : java.util.List.of("clientlist", "clientinfo", "clientfind", "clientupdate", "clientedit", "clientmove")) {
				String help = api.executeRawCommand("help " + command).getRawResponse();
				assertTrue(help.startsWith("Usage: " + command), "Live help must describe " + command);
				evidence.add("help " + command + "\n" + help);
			}
			observer.getApi().registerEvent(TS3EventType.SERVER);
			observer.getApi().registerEvent(TS3EventType.CHANNEL, 0);
			try (var channels = new ChannelCleanup(api);
				 var subscription = observer.subscribe(new TS3EventAdapter() {
					@Override public void onClientJoin(ClientJoinEvent event) { assertTrue(joins.offer(event)); }
					@Override public void onClientMoved(ClientMovedEvent event) { assertTrue(moves.offer(event)); }
					@Override public void onClientLeave(ClientLeaveEvent event) { assertTrue(leaves.offer(event)); }
				 })) {
				subject.connect(); subject.getApi().selectVirtualServerById(1);
				var identity = subject.getApi().whoAmI();
				int id = identity.getId();
				assertNotEquals(invoker.getId(), id); assertNotEquals(observer.getApi().whoAmI().getId(), id);
				var join = joins.poll(5, TimeUnit.SECONDS);
				assertNotNull(join); evidence.add("join " + new java.util.TreeMap<>(join.getMap()));
				assertEquals(id, join.getClientId()); assertEquals(identity.getChannelId(), join.getClientTargetId());
				assertEquals(0, join.getClientFromId()); assertEquals(0, join.getReasonId());
				assertEquals(identity.getNickname(), join.getClientNickname());
				assertEquals(identity.getUniqueIdentifier(), join.getUniqueClientIdentifier());
				assertEquals(identity.getDatabaseId(), join.getClientDatabaseId()); assertEquals(1, join.getClientType());
				assertEquals(target.ts6, join.getMap().containsKey("client_is_streaming"));
				assertEquals(target.ts6, join.getMap().containsKey("client_user_tag"));
				assertFalse(join.getMap().containsKey("invokerid")); assertEquals(-1, join.getInvokerId());
				String name = "query-" + target + " 日本語 | \\s";
				subject.getApi().updateClient(Map.of(ClientProperty.CLIENT_NICKNAME, name));
				assertEquals(name, subject.getApi().whoAmI().getNickname());
				var info = api.getClientInfo(id);
				evidence.add("clientinfo " + new java.util.TreeMap<>(info.getMap()));
				assertEquals(target.ts6, info.getMap().containsKey("client_public_key_raw"));
				assertEquals(target.ts6, info.getMap().containsKey("client_is_streaming"));
				assertEquals(target.ts6, info.getMap().containsKey("client_user_tag"));
				assertEquals(id, info.getId()); assertEquals(name, info.getNickname()); assertEquals(1, info.getType());
				assertFalse(info.isRegularClient()); assertEquals(identity.getChannelId(), info.getChannelId());
				assertEquals(identity.getDatabaseId(), info.getDatabaseId());
				assertEquals(identity.getUniqueIdentifier(), info.getUniqueIdentifier());
				var listed = api.getClients().stream().filter(c -> c.getId() == id).findFirst().orElseThrow();
				evidence.add("clientlist " + new java.util.TreeMap<>(listed.getMap()));
				assertEquals(name, listed.getNickname()); assertEquals(info.getUniqueIdentifier(), listed.getUniqueIdentifier());
				assertEquals(info.getDatabaseId(), listed.getDatabaseId()); assertEquals(1, listed.getType());
				assertEquals(id, api.getClientByNameExact(name, false).getId());
				assertEquals(id, api.getClientByNameExact(name.toUpperCase(java.util.Locale.ROOT), true).getId());
				assertTrue(api.getClientsByName(name).stream().anyMatch(c -> c.getId() == id));
				assertNull(api.getClientByNameExact("absent-client-" + target, false));
				assertTrue(api.getClientsByName("absent-client-" + target).isEmpty());
				try (var restoration = new ClientDescription(api, id, info.getDescription())) {
					String description = "description 日本語 | \\s\\p\\n /";
					api.editClient(id, ClientProperty.CLIENT_DESCRIPTION, description);
					assertEquals(description, api.getClientInfo(id).getDescription());
					String controls = "before\nmiddle\tend\r tail";
					api.editClient(id, Map.of(ClientProperty.CLIENT_DESCRIPTION, controls));
					assertEquals("beforemiddleend tail", api.getClientInfo(id).getDescription());
					String rawDescription = api.executeRawCommand("clientinfo clid=" + id).getRawResponse();
					evidence.add("description controls raw " + rawDescription);
					assertTrue(rawDescription.contains("client_description=beforemiddleend\\stail"));
					int channel = channels.track(api.createChannel("client-move-" + target,
						Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1")));
					api.moveClient(id, channel);
					assertClientMove(moves, id, channel, 1, invoker, evidence);
					assertEquals(channel, subject.getApi().whoAmI().getChannelId());
					assertEquals(channel, api.getClientInfo(id).getChannelId());
					var badMove = assertThrows(TS3CommandFailedException.class, () -> api.moveClient(id, Integer.MAX_VALUE));
					evidence.add("invalid channel " + new java.util.TreeMap<>(badMove.getError().getMap()));
					assertEquals(768, badMove.getError().getId()); assertEquals("invalid channelID", badMove.getError().getMessage());
					assertEquals(channel, api.getClientInfo(id).getChannelId());
					subject.getApi().moveClient(id, identity.getChannelId());
					assertClientMove(moves, id, identity.getChannelId(), 0, null, evidence);
					assertTrue(moves.isEmpty(), "Rejected moves must not add notifications before the valid move fence");
				}
				assertEquals(info.getDescription(), api.getClientInfo(id).getDescription());
				subject.exit();
				var leave = leaves.poll(5, TimeUnit.SECONDS);
				assertNotNull(leave); evidence.add("leave " + new java.util.TreeMap<>(leave.getMap()));
				assertEquals(id, leave.getClientId()); assertEquals(identity.getChannelId(), leave.getClientFromId());
				assertEquals(0, leave.getClientTargetId()); assertEquals(8, leave.getReasonId());
				assertFalse(leave.getMap().containsKey("invokerid")); assertEquals(-1, leave.getInvokerId());
				var gone = assertThrows(TS3CommandFailedException.class, () -> api.getClientInfo(id));
				evidence.add("disconnected client " + new java.util.TreeMap<>(gone.getError().getMap()));
				assertEquals(512, gone.getError().getId()); assertEquals("invalid clientID", gone.getError().getMessage());
				assertTrue(api.getClients().stream().noneMatch(c -> c.getId() == id));
				assertFalse(api.getVersion().getVersion().isBlank());
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
	                                     int id, int channel, int reason, ServerQueryInfo invoker, java.util.List<String> evidence) throws InterruptedException {
		var event = moves.poll(5, TimeUnit.SECONDS);
		assertNotNull(event); evidence.add("move " + new java.util.TreeMap<>(event.getMap()));
		assertEquals(id, event.getClientId()); assertEquals(channel, event.getTargetChannelId());
		assertEquals(reason, event.getReasonId());
		assertEquals(invoker != null, event.getMap().containsKey("invokerid"));
		if (invoker != null) {
			assertEquals(invoker.getId(), event.getInvokerId()); assertEquals(invoker.getNickname(), event.getInvokerName());
			assertEquals(invoker.getUniqueIdentifier(), event.getInvokerUniqueId());
		} else {
			assertEquals(-1, event.getInvokerId()); assertEquals("", event.getInvokerName()); assertEquals("", event.getInvokerUniqueId());
		}
	}

	private record ClientDescription(TS3Api api, int id, String value) implements AutoCloseable {
		@Override public void close() { api.editClient(id, ClientProperty.CLIENT_DESCRIPTION, value); }
	}

	@ParameterizedTest(name = "{0}: server information and edits from an independent query session")
	@EnumSource(Target.class)
	@Timeout(60)
	void serverDomain(Target target) throws Exception {
		TeamSpeakContainer server = target.ts6 ? ts6 : ts3;
		var evidence = new java.util.ArrayList<String>();
		var edits = new java.util.concurrent.LinkedBlockingQueue<ServerEditedEvent>(16);
		try (var observer = new TS3Query(server.config(target.protocol));
			 var actor = new TS3Query(server.config(target.protocol))) {
			observer.connect(); actor.connect();
			var api = actor.getApi();
			api.selectVirtualServerById(1); observer.getApi().selectVirtualServerById(1);
			server.record(target.name(), api.getVersion());
			api.setNickname("server-edit-" + target);
			var actorInfo = api.whoAmI();
			assertNotEquals(actorInfo.getId(), observer.getApi().whoAmI().getId());
			for (String command : java.util.List.of("serverinfo", "serverlist", "serveridgetbyport",
				"hostinfo", "instanceinfo", "serverrequestconnectioninfo", "serveredit", "servernotifyregister")) {
				String help = api.executeRawCommand("help " + command).getRawResponse();
				assertTrue(help.startsWith("Usage: " + command), "Live help must describe " + command);
				evidence.add("help " + command + "\n" + help);
			}
			var info = api.getServerInfo();
			evidence.add("serverinfo " + new java.util.TreeMap<>(info.getMap()));
			assertEquals(target.ts6, info.getMap().containsKey("virtualserver_storage_quota"));
			assertEquals(target.ts6, info.getMap().containsKey("virtualserver_canonical_name"));
			assertEquals(!target.ts6, info.getMap().containsKey("virtualserver_file_storage_class"));
			assertEquals(1, info.getId()); assertEquals(VirtualServerStatus.ONLINE, info.getStatus());
			assertFalse(info.getName().isBlank()); assertFalse(info.getUniqueIdentifier().isBlank());
			assertTrue(info.getPort() > 0); assertTrue(info.getMaxClients() > 0);
			assertFalse(info.getVersion().isBlank()); assertFalse(info.getPlatform().isBlank());
			var listed = api.getVirtualServers().stream().filter(v -> v.getId() == 1).findFirst().orElseThrow();
			evidence.add("serverlist " + new java.util.TreeMap<>(listed.getMap()));
			assertEquals(target.ts6, listed.getMap().containsKey("virtualserver_uuid"));
			assertEquals(info.getName(), listed.getName()); assertEquals(info.getPort(), listed.getPort());
			assertEquals(info.getUniqueIdentifier(), listed.getUniqueIdentifier());
			assertEquals(info.getStatus(), listed.getStatus()); assertEquals(info.getMaxClients(), listed.getMaxClients());
			assertEquals(1, api.getServerIdByPort(info.getPort()));
			var host = api.getHostInfo();
			evidence.add("hostinfo " + new java.util.TreeMap<>(host.getMap()));
			assertEquals(target.ts6, host.getMap().containsKey("virtualservers_total_homebases"));
			assertEquals(1, host.getTotalRunningServers()); assertTrue(host.getTotalChannels() > 0);
			assertTrue(host.getUptime() >= 0); assertTrue(host.getTimeStamp().getTime() > 0);
			var instance = api.getInstanceInfo();
			evidence.add("instanceinfo " + new java.util.TreeMap<>(instance.getMap()));
			assertEquals(target.ts6, instance.getMap().containsKey("serverinstance_max_homebases"));
			assertTrue(instance.getDatabaseVersion() > 0); assertTrue(instance.getFileTransferPort() > 0);
			var connection = api.getConnectionInfo();
			evidence.add("connectioninfo " + new java.util.TreeMap<>(connection.getMap()));
			assertTrue(connection.getTotalPacketsSent() >= 0); assertTrue(connection.getTotalPacketsReceived() >= 0);
			assertTrue(connection.getTotalBytesSent() >= 0); assertTrue(connection.getTotalBytesReceived() >= 0);
			var original = Map.of(VirtualServerProperty.VIRTUALSERVER_NAME, info.getName(),
				VirtualServerProperty.VIRTUALSERVER_WELCOMEMESSAGE, info.getWelcomeMessage(),
				VirtualServerProperty.VIRTUALSERVER_HOSTMESSAGE, info.getHostMessage());
			observer.getApi().registerEvent(TS3EventType.SERVER);
			try (var restoration = new ServerSettings(api, original);
				 var subscription = observer.subscribe(new TS3EventAdapter() {
					@Override public void onServerEdit(ServerEditedEvent event) { assertTrue(edits.offer(event)); }
				 })) {
				String name = "server-" + target + " 日本語 | \\s";
				String welcome = "welcome | literal \\s\\p\\n / \n";
				String hostMessage = "host | literal \\t\\r\\\\ / \t\r";
				api.editServer(Map.of(VirtualServerProperty.VIRTUALSERVER_NAME, name,
					VirtualServerProperty.VIRTUALSERVER_WELCOMEMESSAGE, welcome,
					VirtualServerProperty.VIRTUALSERVER_HOSTMESSAGE, hostMessage));
				assertServerEdit(edits, name, actorInfo, target, evidence);
				var changed = api.getServerInfo();
				assertEquals(name, changed.getName()); assertEquals(welcome, changed.getWelcomeMessage());
				assertEquals(hostMessage, changed.getHostMessage());
				assertEquals(name, api.getVirtualServers().stream().filter(v -> v.getId() == 1).findFirst().orElseThrow().getName());
				var invalidValue = assertThrows(TS3CommandFailedException.class,
					() -> api.editServer(Map.of(VirtualServerProperty.VIRTUALSERVER_MAXCLIENTS, "not-an-integer")));
				evidence.add("invalid numeric edit " + new java.util.TreeMap<>(invalidValue.getError().getMap()));
				assertEquals(1540, invalidValue.getError().getId());
				assertEquals("convert error", invalidValue.getError().getMessage());
				assertEquals(name, api.getServerInfo().getName());
				String secondName = "updated " + name;
				api.editServer(Map.of(VirtualServerProperty.VIRTUALSERVER_NAME, secondName));
				assertServerEdit(edits, secondName, actorInfo, target, evidence);
				assertTrue(edits.isEmpty(), "Rejected edits must not produce an extra server-edit notification");
				assertEquals(secondName, api.getServerInfo().getName());
				var missingPort = assertThrows(TS3CommandFailedException.class, () -> api.getServerIdByPort(65535));
				evidence.add("unknown port " + new java.util.TreeMap<>(missingPort.getError().getMap()));
				assertEquals(1024, missingPort.getError().getId());
				assertEquals("invalid serverID", missingPort.getError().getMessage());
				assertEquals(1, api.getServerIdByPort(info.getPort()));
			}
			var restored = api.getServerInfo();
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
	                                     String name, ServerQueryInfo actorInfo, Target target, java.util.List<String> evidence) throws InterruptedException {
		var event = edits.poll(5, TimeUnit.SECONDS);
		assertNotNull(event, "Expected server-edit notification from independent actor");
		evidence.add("server edit " + new java.util.TreeMap<>(event.getMap()));
		assertEquals(name, event.get(VirtualServerProperty.VIRTUALSERVER_NAME));
		assertEquals(actorInfo.getId(), event.getInvokerId()); assertEquals(actorInfo.getNickname(), event.getInvokerName());
		assertEquals(actorInfo.getUniqueIdentifier(), event.getInvokerUniqueId());
		assertEquals(!target.ts6, event.getMap().containsKey("reasonid"));
		assertEquals(target.ts6 ? -1 : 10, event.getReasonId());
		assertFalse(event.getMap().containsKey(VirtualServerProperty.VIRTUALSERVER_WELCOMEMESSAGE.getName()));
		assertFalse(event.getMap().containsKey(VirtualServerProperty.VIRTUALSERVER_HOSTMESSAGE.getName()));
	}

	private record ServerSettings(TS3Api api, Map<VirtualServerProperty, String> values) implements AutoCloseable {
		@Override public void close() { api.editServer(values); }
	}

	@ParameterizedTest(name = "{0}: online text messages from an independent query session")
	@EnumSource(Target.class)
	@Timeout(60)
	void messageDomain(Target target) throws Exception {
		TeamSpeakContainer server = target.ts6 ? ts6 : ts3;
		var evidence = new java.util.ArrayList<String>();
		var messages = new java.util.concurrent.LinkedBlockingQueue<TextMessageEvent>(16);
		try (var observer = new TS3Query(server.config(target.protocol));
			 var actor = new TS3Query(server.config(target.protocol))) {
			observer.connect(); actor.connect();
			var api = actor.getApi();
			var recipient = observer.getApi();
			api.selectVirtualServerById(1); recipient.selectVirtualServerById(1);
			server.record(target.name(), api.getVersion());
			String nickname = "msg-" + target + " | \\s";
			api.setNickname(nickname);
			var actorInfo = api.whoAmI();
			assertEquals(nickname, actorInfo.getNickname());
			int observerId = recipient.whoAmI().getId();
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
				int channel = channels.track(api.createChannel("messages-" + target, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1")));
				recipient.moveQuery(channel);
				recipient.registerEvents(TS3EventType.TEXT_SERVER, TS3EventType.TEXT_CHANNEL, TS3EventType.TEXT_PRIVATE);
				String payload = "[b]héllo 日本語[/b] | literal \\s\\p\\n\\t\\r\\\\ / \n\t\r";
				api.sendServerMessage(payload);
				assertMessage(messages, TextMessageTargetMode.SERVER, payload, actorInfo, observerId, evidence);
				api.sendChannelMessage(channel, payload);
				assertEquals(channel, api.whoAmI().getChannelId(), "Explicit channel send leaves the query in that channel");
				assertMessage(messages, TextMessageTargetMode.CHANNEL, payload, actorInfo, observerId, evidence);
				api.sendChannelMessage("current " + payload);
				assertMessage(messages, TextMessageTargetMode.CHANNEL, "current " + payload, actorInfo, observerId, evidence);
				api.sendTextMessage(TextMessageTargetMode.CHANNEL, 65535, "ignored target " + payload);
				assertMessage(messages, TextMessageTargetMode.CHANNEL, "ignored target " + payload, actorInfo, observerId, evidence);
				api.sendTextMessage(TextMessageTargetMode.SERVER, 65535, "ignored target " + payload);
				assertMessage(messages, TextMessageTargetMode.SERVER, "ignored target " + payload, actorInfo, observerId, evidence);
				api.sendPrivateMessage(observerId, payload);
				assertMessage(messages, TextMessageTargetMode.CLIENT, payload, actorInfo, observerId, evidence);
				api.sendTextMessage(TextMessageTargetMode.CLIENT, observerId, "generic " + payload);
				assertMessage(messages, TextMessageTargetMode.CLIENT, "generic " + payload, actorInfo, observerId, evidence);
				api.sendServerMessage(1, "selected " + payload);
				assertEquals(1, api.whoAmI().getVirtualServerId());
				assertMessage(messages, TextMessageTargetMode.SERVER, "selected " + payload, actorInfo, observerId, evidence);
				String boundary = "é".repeat(512);
				api.sendPrivateMessage(observerId, boundary);
				assertMessage(messages, TextMessageTargetMode.CLIENT, boundary, actorInfo, observerId, evidence);
				api.sendPrivateMessage(observerId, boundary + "é");
				assertMessage(messages, TextMessageTargetMode.CLIENT, boundary + "é", actorInfo, observerId, evidence);
				evidence.add("private UTF-8 payloads: 1024 and 1026 bytes received intact");
				var invalidNumber = assertThrows(TS3CommandFailedException.class, () -> api.sendPrivateMessage(Integer.MAX_VALUE, "out of range"));
				evidence.add("out-of-range recipient " + new java.util.TreeMap<>(invalidNumber.getError().getMap()));
				assertEquals(1540, invalidNumber.getError().getId());
				assertEquals("convert error", invalidNumber.getError().getMessage());
				var invalidClient = assertThrows(TS3CommandFailedException.class, () -> api.sendPrivateMessage(65535, "missing client"));
				evidence.add("invalid recipient " + new java.util.TreeMap<>(invalidClient.getError().getMap()));
				assertEquals(512, invalidClient.getError().getId());
				assertEquals("invalid clientID", invalidClient.getError().getMessage());
				var invalidChannel = assertThrows(TS3CommandFailedException.class, () -> api.sendChannelMessage(65535, "missing channel"));
				evidence.add("invalid channel " + new java.util.TreeMap<>(invalidChannel.getError().getMap()));
				assertEquals(768, invalidChannel.getError().getId());
				assertEquals("invalid channelID", invalidChannel.getError().getMessage());
				var invalidServer = assertThrows(TS3CommandFailedException.class, () -> api.sendServerMessage(65535, "missing server"));
				evidence.add("invalid server " + new java.util.TreeMap<>(invalidServer.getError().getMap()));
				assertEquals(2816, invalidServer.getError().getId());
				assertEquals("virtualserver limit reached", invalidServer.getError().getMessage());
				assertEquals(0, api.whoAmI().getVirtualServerId(), "Failed selection clears the current server on these targets");
				var unselected = assertThrows(TS3CommandFailedException.class, () -> api.sendServerMessage("no current server"));
				evidence.add("unselected server " + new java.util.TreeMap<>(unselected.getError().getMap()));
				assertEquals(1024, unselected.getError().getId());
				assertEquals("invalid serverID", unselected.getError().getMessage());
				api.selectVirtualServerById(1, nickname);
				actorInfo = api.whoAmI();
				assertEquals(1, actorInfo.getVirtualServerId());
				assertEquals(nickname, actorInfo.getNickname());
				// A private event fences earlier sends on the same actor and listener, without a timing-only absence check.
				recipient.unregisterAllEvents();
				recipient.registerEvent(TS3EventType.TEXT_PRIVATE);
				api.sendServerMessage("unsubscribed server");
				api.sendChannelMessage(channel, "unsubscribed channel");
				api.sendPrivateMessage(observerId, "private fence");
				assertMessage(messages, TextMessageTargetMode.CLIENT, "private fence", actorInfo, observerId, evidence);
				assertTrue(messages.isEmpty(), "No failed-target or unsubscribed message may be dispatched before the fence");
			}
			assertFalse(api.getVersion().getVersion().isBlank());
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
	                                  int observerId, java.util.List<String> evidence) throws InterruptedException {
		var event = messages.poll(5, TimeUnit.SECONDS);
		assertNotNull(event, "Expected " + mode + " notification from the independent actor");
		evidence.add("message " + new java.util.TreeMap<>(event.getMap()));
		assertEquals(mode, event.getTargetMode()); assertEquals(message, event.getMessage());
		assertEquals(actorInfo.getId(), event.getInvokerId()); assertEquals(actorInfo.getNickname(), event.getInvokerName());
		assertEquals(actorInfo.getUniqueIdentifier(), event.getInvokerUniqueId());
		assertEquals(mode == TextMessageTargetMode.CLIENT, event.getMap().containsKey("target"),
			"Only private text notifications supply the recipient target field");
		assertEquals(mode == TextMessageTargetMode.CLIENT ? observerId : -1, event.getTargetClientId());
	}

	@ParameterizedTest(name = "{0}: permission metadata and named channel/group assignments")
	@EnumSource(Target.class)
	@Timeout(60)
	void permissionDomain(Target target) throws Exception {
		TeamSpeakContainer server = target.ts6 ? ts6 : ts3;
		var evidence = new java.util.ArrayList<String>();
		try (var query = new TS3Query(server.config(target.protocol))) {
			query.connect();
			var api = query.getApi();
			api.selectVirtualServerById(1);
			server.record(target.name(), api.getVersion());
			for (String command : java.util.List.of("permissionlist", "permidgetbyname", "permget", "permfind", "channeladdperm",
				"channelpermlist", "channeldelperm", "servergroupadd", "servergrouplist", "servergrouprename", "servergroupdel",
				"servergroupaddperm", "servergrouppermlist", "servergroupdelperm", "channelgroupadd", "channelgrouplist",
				"channelgrouprename", "channelgroupdel", "channelgroupaddperm", "channelgrouppermlist", "channelgroupdelperm")) {
				String help = api.executeRawCommand("help " + command).getRawResponse();
				assertTrue(help.startsWith("Usage: " + command), "Live help must describe " + command);
				evidence.add("help " + command + "\n" + help);
			}
			var metadata = api.getPermissions();
			assertFalse(metadata.isEmpty());
			var byName = new java.util.HashMap<String, PermissionInfo>();
			var ids = new java.util.HashSet<Integer>();
			for (var permission : metadata) {
				assertFalse(permission.getName().isBlank()); assertTrue(permission.getId() > 0);
				assertNull(byName.put(permission.getName(), permission), "Permission names must be unique");
				assertTrue(ids.add(permission.getId()), "Permission IDs must be unique on this server");
			}
			metadata.stream().sorted(java.util.Comparator.comparing(PermissionInfo::getName))
				.forEach(p -> evidence.add("metadata " + new java.util.TreeMap<>(p.getMap())));
			String integerName = IPermissionType.I_CHANNEL_NEEDED_JOIN_POWER.getName();
			String booleanName = BPermissionType.B_CHANNEL_CREATE_PERMANENT.getName();
			assertTrue(byName.containsKey(integerName)); assertTrue(byName.containsKey(booleanName));
			assertEquals(target.ts6, byName.containsKey("i_ft_max_file_size_mb"));
			if (target.ts6) assertEquals(byName.get("i_ft_max_file_size_mb").getId(), api.getPermissionIdByName("i_ft_max_file_size_mb"));
			else assertEquals(2562, assertThrows(TS3CommandFailedException.class, () -> api.getPermissionIdByName("i_ft_max_file_size_mb")).getError().getId());
			assertEquals(byName.get(integerName).getId(), api.getPermissionIdByName(integerName));
			assertArrayEquals(new int[]{byName.get(booleanName).getId(), byName.get(integerName).getId()},
				api.getPermissionIdsByName(booleanName, integerName));
			assertArrayEquals(new int[]{api.getPermissionValue(booleanName), api.getPermissionValue(integerName)},
				api.getPermissionValues(booleanName, integerName));
			try (var channels = new ChannelCleanup(api);
				 var serverGroup = new CreatedResource(api.addServerGroup("permissions " + target + " | \\s"), id -> api.deleteServerGroup(id, true));
				 var channelGroup = new CreatedResource(api.addChannelGroup("permissions " + target + " | \\s"), id -> api.deleteChannelGroup(id, true))) {
				int channel = channels.track(api.createChannel("permission-channel-" + target, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1")));
				assertEquals("permissions " + target + " | \\s", api.getServerGroups().stream().filter(g -> g.getId() == serverGroup.id()).findFirst().orElseThrow().getName());
				assertEquals("permissions " + target + " | \\s", api.getChannelGroups().stream().filter(g -> g.getId() == channelGroup.id()).findFirst().orElseThrow().getName());
				api.addChannelPermission(channel, integerName, 17);
				assertPermission(api.getChannelPermissions(channel), integerName, 17, false, false, evidence, "channel");
				api.addChannelPermission(channel, integerName, 23);
				assertPermission(api.getChannelPermissions(channel), integerName, 23, false, false, evidence, "channel update");
				api.deleteChannelPermission(channel, integerName);
				assertTrue(api.getChannelPermissions(channel).stream().noneMatch(p -> p.getName().equals(integerName)));
				api.addServerGroupPermission(serverGroup.id(), integerName, 31, true, true);
				assertPermission(api.getServerGroupPermissions(serverGroup.id()), integerName, 31, true, true, evidence, "server group");
				var assignment = api.getPermissionAssignments(integerName).stream().filter(p ->
					p.getType() == PermissionGroupType.SERVER_GROUP && p.getMajorId() == serverGroup.id()).findFirst().orElseThrow();
				assertEquals(byName.get(integerName).getId(), assignment.getId());
				assertFalse(assignment.getMap().containsKey("v"), "permfind returns identities, not assignment values");
				assertFalse(assignment.getMap().containsKey("n"));
				assertFalse(assignment.getMap().containsKey("s"));
				evidence.add("assignment search " + new java.util.TreeMap<>(assignment.getMap()));
				api.addServerGroupPermission(serverGroup.id(), integerName, 37, false, false);
				assertPermission(api.getServerGroupPermissions(serverGroup.id()), integerName, 37, false, false, evidence, "server group update");
				api.addServerGroupPermission(serverGroup.id(), booleanName, 1, false, true);
				assertPermission(api.getServerGroupPermissions(serverGroup.id()), booleanName, 1, false, true, evidence, "server group boolean");
				api.deleteServerGroupPermission(serverGroup.id(), booleanName);
				assertTrue(api.getServerGroupPermissions(serverGroup.id()).stream().noneMatch(p -> p.getName().equals(booleanName)));
				api.deleteServerGroupPermission(serverGroup.id(), integerName);
				assertTrue(api.getServerGroupPermissions(serverGroup.id()).stream().noneMatch(p -> p.getName().equals(integerName)));
				api.addChannelGroupPermission(channelGroup.id(), integerName, 41);
				assertPermission(api.getChannelGroupPermissions(channelGroup.id()), integerName, 41, false, false, evidence, "channel group");
				api.addChannelGroupPermission(channelGroup.id(), integerName, 43);
				assertPermission(api.getChannelGroupPermissions(channelGroup.id()), integerName, 43, false, false, evidence, "channel group update");
				api.deleteChannelGroupPermission(channelGroup.id(), integerName);
				assertTrue(api.getChannelGroupPermissions(channelGroup.id()).stream().noneMatch(p -> p.getName().equals(integerName)));
				api.renameServerGroup(serverGroup.id(), "renamed server " + target);
				assertEquals("renamed server " + target, api.getServerGroups().stream().filter(g -> g.getId() == serverGroup.id()).findFirst().orElseThrow().getName());
				api.renameChannelGroup(channelGroup.id(), "renamed channel " + target);
				assertEquals("renamed channel " + target, api.getChannelGroups().stream().filter(g -> g.getId() == channelGroup.id()).findFirst().orElseThrow().getName());
				var error = assertThrows(TS3CommandFailedException.class, () -> api.addChannelPermission(channel, "i_issue441_missing_permission", 1));
				evidence.add("unknown permission " + new java.util.TreeMap<>(error.getError().getMap()));
				assertEquals(2562, error.getError().getId());
				assertEquals("invalid permission ID", error.getError().getMessage());
				var lookupError = assertThrows(TS3CommandFailedException.class, () -> api.getPermissionIdByName("i_issue441_missing_permission"));
				assertEquals(2562, lookupError.getError().getId());
				var findError = assertThrows(TS3CommandFailedException.class, () -> api.getPermissionAssignments("i_issue441_missing_permission"));
				assertEquals(2562, findError.getError().getId());
				assertEquals(byName.get(integerName).getId(), api.getPermissionIdByName(integerName));
			}
			assertTrue(api.getServerGroups().stream().noneMatch(g -> g.getName().equals("renamed server " + target)));
			assertTrue(api.getChannelGroups().stream().noneMatch(g -> g.getName().equals("renamed channel " + target)));
			query.exit();
		} finally {
			Path path = Path.of("target", "compatibility", target + "-permissions.txt");
			Files.createDirectories(path.getParent()); Files.write(path, evidence);
		}
	}

	private static void assertPermission(java.util.List<Permission> assignments,
	                                    String name, int value, boolean negated, boolean skipped,
	                                    java.util.List<String> evidence, String label) {
		var permission = assignments.stream().filter(p -> p.getName().equals(name)).findFirst().orElseThrow();
		assertEquals(value, permission.getValue()); assertEquals(negated, permission.isNegated()); assertEquals(skipped, permission.isSkipped());
		evidence.add(label + " " + new java.util.TreeMap<>(permission.getMap()));
	}

	private record CreatedResource(int id, java.util.function.IntConsumer deletion) implements AutoCloseable {
		@Override public void close() { deletion.accept(id); }
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
