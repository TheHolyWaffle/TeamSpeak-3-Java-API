package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.BanRequest;
import com.github.theholywaffle.teamspeak3.api.ChannelGroupFilter;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.FileLocation;
import com.github.theholywaffle.teamspeak3.api.PermissionTarget;
import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;
import com.github.theholywaffle.teamspeak3.api.event.BaseEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelCreateEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelDeletedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelDescriptionEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelPasswordChangedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ServerEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import com.github.theholywaffle.teamspeak3.api.exception.TS3UnsupportedOperationException;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import com.github.theholywaffle.teamspeak3.commands.FileCommands;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.DockerClientFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live TS3 evidence for #444: every typed getter of every model and event the exercised commands
 * return is read on real responses. Required getters must find their properties; optional getters
 * must not fail. Lists must be unmodifiable.
 */
class ModelCompatibilityIT {
	/** Getters that intentionally fail for the producing command. */
	private static final Set<String> UNSUPPORTED = Set.of("ClientInfo#isTalking");

	static TeamSpeakContainer ts3;

	@BeforeAll static void start() {
		assertTrue(DockerClientFactory.instance().isDockerAvailable(), "Required Docker integration tests cannot run: start a Docker daemon");
		ts3 = new TeamSpeakContainer();
		ts3.start();
	}

	@AfterAll static void stop() {
		if (ts3 != null) ts3.close();
	}

	@ParameterizedTest(name = "{0}: typed getters of received models and events")
	@EnumSource(ServerQueryCompatibilityIT.Target.class) @Timeout(120)
	void typedGettersReadLiveResponses(ServerQueryCompatibilityIT.Target target) throws Exception {
		var evidence = new TreeSet<String>();
		var failures = new ArrayList<String>();
		var events = new LinkedBlockingQueue<BaseEvent>();
		try (var observer = new TS3Query(ts3.config(target.protocol)); var actor = new TS3Query(ts3.config(target.protocol))) {
			observer.connect();
			var api = observer.getApi();
			inspect(api.whoAmI(), evidence, failures);
			api.selectVirtualServerById(1);
			api.events().register(TS3EventType.SERVER, TS3EventType.TEXT_PRIVATE);
			api.events().register(TS3EventType.CHANNEL, 0);
			try (var subscription = observer.subscribe(new TS3EventAdapter() {
				@Override public void onClientJoin(ClientJoinEvent e) { events.add(e); }
				@Override public void onClientLeave(ClientLeaveEvent e) { events.add(e); }
				@Override public void onClientMoved(ClientMovedEvent e) { events.add(e); }
				@Override public void onChannelCreate(ChannelCreateEvent e) { events.add(e); }
				@Override public void onChannelEdit(ChannelEditedEvent e) { events.add(e); }
				@Override public void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e) { events.add(e); }
				@Override public void onChannelPasswordChanged(ChannelPasswordChangedEvent e) { events.add(e); }
				@Override public void onChannelMoved(ChannelMovedEvent e) { events.add(e); }
				@Override public void onChannelDeleted(ChannelDeletedEvent e) { events.add(e); }
				@Override public void onServerEdit(ServerEditedEvent e) { events.add(e); }
				@Override public void onTextMessage(TextMessageEvent e) { events.add(e); }
			})) {
				actor.connect();
				var actorApi = actor.getApi();
				actorApi.selectVirtualServerById(1);
				var actorId = actorApi.whoAmI().getId();
				int parent = actorApi.channels().create("models-parent-" + target, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
				int child = actorApi.channels().create("models-child-" + target, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
				String serverName = api.servers().info().getName();
				var databaseClient = api.databaseClients().list().getFirst().getDatabaseId();
				int banId = api.bans().add(BanRequest.ip("10.44.4.4").withReason("models " + target));
				String key = api.privilegeKeys().createForServerGroup(6, "models " + target);
				var login = api.queryLogins().create("models-" + target, databaseClient);
				String upload = "/models-" + target + ".bin";
				try {
					actorApi.channels().edit(child, Map.of(ChannelProperty.CHANNEL_TOPIC, "topic"));
					actorApi.channels().edit(child, Map.of(ChannelProperty.CHANNEL_DESCRIPTION, "description"));
					actorApi.channels().edit(child, Map.of(ChannelProperty.CHANNEL_PASSWORD, "secret"));
					actorApi.channels().move(child, parent);
					api.clients().move(actorId, parent);
					actorApi.messages().sendPrivate(api.whoAmI().getId(), "models " + target);
					actorApi.servers().edit(Map.of(VirtualServerProperty.VIRTUALSERVER_NAME, serverName + " models"));
					api.servers().edit(Map.of(VirtualServerProperty.VIRTUALSERVER_NAME, serverName));
					api.serverGroups().addMember(7, databaseClient);
					api.channelGroups().assign(5, 1, databaseClient);
					api.databaseClients().setCustomProperty(databaseClient, "models", target.name());
					api.messages().sendOffline(api.databaseClients().info(databaseClient).getUniqueIdentifier(), "models", "offline " + target);
					api.executeRawCommand(FileCommands.ftInitUpload(41, upload, 1, "", 8, true).toString());

					for (Object model : List.of(api.servers().info(), api.servers().connectionInfo(), api.instance().info(),
						api.instance().hostInfo(), api.instance().version(), api.whoAmI(), api.clients().info(actorId),
						api.channels().info(child), api.databaseClients().info(databaseClient))) {
						inspect(model, evidence, failures);
					}
					for (List<?> rows : List.of(api.servers().list(), api.clients().list(), api.channels().list(),
						api.instance().bindings(), api.serverGroups().list(), api.serverGroups().members(7),
						api.serverGroups().ofClient(databaseClient), api.channelGroups().list(),
						api.channelGroups().members(ChannelGroupFilter.all()), api.databaseClients().list(),
						api.databaseClients().searchCustomProperty("models", "%"), api.bans().list(), api.privilegeKeys().list(),
						api.permissions().list(), api.permissions().assigned(PermissionTarget.serverGroup(6)),
						api.permissions().overview(1, databaseClient), api.permissions().assignments("i_client_talk_power"),
						api.queryLogins().list(), api.files().list(FileLocation.of(1, "/")),
						api.files().info(List.of(FileLocation.of(1, upload))), api.files().transfers())) {
						assertFalse(rows.isEmpty(), "Every exercised listing returns rows");
						assertThrows(UnsupportedOperationException.class, () -> rows.remove(0));
						for (Object row : rows) inspect(row, evidence, failures);
					}
					// The offline inbox belongs to the logged-in identity, so read it as the database client.
					try (var recipient = new TS3Query(ts3.config(target.protocol)
						.setLoginCredentials("models-" + target, login.getQueryClientLoginPassword()))) {
						recipient.connect();
						recipient.getApi().selectVirtualServerById(1);
						var inbox = recipient.getApi().messages().listOffline();
						assertFalse(inbox.isEmpty());
						for (var message : inbox) {
							inspect(message, evidence, failures);
							recipient.getApi().messages().deleteOffline(message.getId());
						}
						recipient.exit();
					}
					var customProperties = api.databaseClients().customProperties(databaseClient);
					assertTrue(customProperties.containsKey("models"));
					assertThrows(UnsupportedOperationException.class, customProperties::clear);
				} finally {
					api.queryLogins().delete(login.getClientDatabaseId());
					api.privilegeKeys().delete(key);
					api.bans().delete(banId);
					api.databaseClients().deleteCustomProperty(databaseClient, "models");
					api.serverGroups().removeMember(7, databaseClient);
					actorApi.channels().delete(parent, true);
					actor.exit();
				}
				var seen = new TreeSet<String>();
				while (seen.size() < 11) {
					BaseEvent event = events.poll(5, TimeUnit.SECONDS);
					assertNotNull(event, "Missing events; received " + seen);
					seen.add(event.getClass().getSimpleName());
					inspect(event, evidence, failures);
				}
				assertEquals(databaseClient, login.getClientDatabaseId());
			}
			observer.exit();
			assertEquals(0, observer.getEventStatistics().listenerFailures());
		}
		Path report = Path.of("target", "compatibility", target + "-models.txt");
		Files.createDirectories(report.getParent());
		Files.write(report, evidence);
		assertEquals(List.of(), failures);
	}

	/** Calls every public no-argument getter declared by the model's own classes. */
	private static void inspect(Object model, Set<String> evidence, List<String> failures) {
		var getters = Arrays.stream(model.getClass().getMethods())
			.filter(method -> method.getParameterCount() == 0 && !Modifier.isStatic(method.getModifiers()))
			.filter(method -> method.getDeclaringClass().getPackageName().startsWith("com.github.theholywaffle.teamspeak3.api"))
			.filter(method -> method.getDeclaringClass() != Wrapper.class)
			.filter(method -> !Set.of("hashCode", "toString", "getMap", "fire").contains(method.getName()))
			.sorted(Comparator.comparing(Method::getName)).toList();
		for (Method getter : getters) {
			String name = model.getClass().getSimpleName() + "#" + getter.getName();
			try {
				Object value = getter.invoke(model);
				evidence.add(name + " -> " + describe(value));
			} catch (InvocationTargetException failure) {
				Throwable cause = failure.getCause();
				if (UNSUPPORTED.contains(name) && cause instanceof TS3UnsupportedOperationException) {
					evidence.add(name + " -> unsupported");
				} else {
					failures.add(name + ": " + cause + " in " + new TreeMap<>(rawOf(model)));
				}
			} catch (IllegalAccessException e) {
				throw new AssertionError(e);
			}
		}
	}

	private static Map<String, String> rawOf(Object model) {
		return model instanceof Wrapper wrapper ? wrapper.getMap() : Map.of("record", model.toString());
	}

	/** Records presence and type, not values, so generated secrets stay out of the report. */
	private static String describe(Object value) {
		if (value instanceof Optional<?> optional) return optional.isPresent() ? "present " + optional.get().getClass().getSimpleName() : "empty";
		if (value instanceof OptionalInt optional) return optional.isPresent() ? "present int" : "empty";
		if (value instanceof OptionalLong optional) return optional.isPresent() ? "present long" : "empty";
		if (value instanceof Enum<?> constant) return constant.getDeclaringClass().getSimpleName() + "." + constant.name();
		return value == null ? "null" : value.getClass().getSimpleName();
	}
}
