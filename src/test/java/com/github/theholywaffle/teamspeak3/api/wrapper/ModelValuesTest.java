package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ChannelBannerMode;
import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.Codec;
import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import com.github.theholywaffle.teamspeak3.api.PermissionGroupType;
import com.github.theholywaffle.teamspeak3.api.PermissionTarget;
import com.github.theholywaffle.teamspeak3.api.PrivilegeKeyType;
import com.github.theholywaffle.teamspeak3.api.Snapshot;
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.VirtualServerStatus;
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import com.github.theholywaffle.teamspeak3.api.exception.TS3InvalidValueException;
import com.github.theholywaffle.teamspeak3.commands.ServerGroupCommands;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class ModelValuesTest {

	private static final String UNSIGNED_MAX = "18446744073709551615";

	@Test void receivedPropertiesAreCopiedAndCannotBeMutated() {
		var source = new HashMap<>(Map.of("clid", "5", "client_servergroups", "6,8", "client_badges", "overwolf=1:badges=a,b"));
		var client = new Client(source);
		source.put("clid", "6");
		source.put("injected", "1");
		assertEquals(ClientId.of(5), client.getId());
		assertFalse(client.getMap().containsKey("injected"));
		assertUnsupported(() -> client.getMap().put("clid", "7"));
		assertUnsupported(() -> client.getServerGroups().add(9));
		assertUnsupported(() -> client.getBadgeGUIDs().clear());
		assertEquals(List.of(6, 8), client.getServerGroups());
		assertEquals(List.of("a", "b"), client.getBadgeGUIDs());
		assertTrue(client.hasOverwolf());

		var response = DefaultArrayResponse.parse("cid=1 name=a|name=b");
		assertUnsupported(() -> response.getResponses().clear());
		assertUnsupported(() -> response.getResponses().getLast().getMap().remove("cid"));
		assertEquals("1", response.getResponses().getLast().require("cid"));
		assertThrows(NullPointerException.class, () -> new Wrapper(new HashMap<>(Collections.singletonMap("k", null))));
	}

	@Test void missingMalformedAndValuelessPropertiesStayDistinct() {
		var row = new Wrapper(Map.of("number", "5", "text", "x", "valueless", "", "flag", "2"));
		assertEquals(OptionalInt.of(5), row.findInt("number"));
		assertEquals(OptionalInt.empty(), row.findInt("missing"));
		assertEquals(OptionalInt.empty(), row.findInt("valueless"));

		var missing = assertThrows(TS3InvalidValueException.class, () -> row.requireInt("missing"));
		assertEquals("missing", missing.getProperty());
		assertEquals(Optional.empty(), missing.getRawValue());
		assertFalse(missing.isOutcomeUnknown());

		var malformed = assertThrows(TS3InvalidValueException.class, () -> row.findInt("text"));
		assertEquals(Optional.of("x"), malformed.getRawValue());
		assertEquals(Optional.of(""), assertThrows(TS3InvalidValueException.class, () -> row.requireLong("valueless")).getRawValue());
		assertEquals(Optional.of("2"), assertThrows(TS3InvalidValueException.class, () -> row.requireBoolean("flag")).getRawValue());
		assertEquals(Optional.of("x"), assertThrows(TS3InvalidValueException.class, () -> row.findDouble("text")).getRawValue());
		assertEquals(Optional.of(""), row.find("valueless"));
		assertEquals("x", row.require("text"));

		// A malformed property only affects the getters that read it.
		var channel = new Channel(Map.of("cid", "3", "channel_name", "a", "channel_codec", "opus"));
		assertEquals(3, channel.getId());
		assertEquals("a", channel.getName());
		assertEquals(Optional.of("opus"), assertThrows(TS3InvalidValueException.class, channel::getCodec).getRawValue());
	}

	@Test void unknownCodesMapToUnknownAndKeepTheirRawValue() {
		var channel = new Channel(Map.of("cid", "3", "channel_codec", "99"));
		assertEquals(Codec.UNKNOWN, channel.getCodec());
		assertEquals("99", channel.getMap().get("channel_codec"));
		assertEquals(ChannelBannerMode.UNKNOWN, new ChannelInfo(3, Map.of("channel_banner_mode", "7")).getBannerMode());
		assertEquals(VirtualServerStatus.UNKNOWN, new VirtualServer(Map.of("virtualserver_status", "hibernating")).getStatus());
		assertEquals(VirtualServerStatus.VIRTUAL_ONLINE, new VirtualServer(Map.of("virtualserver_status", "virtual online")).getStatus());
		assertEquals(PermissionGroupType.UNKNOWN, new PermissionAssignment(Map.of("t", "9")).getType());
		assertEquals(PermissionGroupDatabaseType.UNKNOWN, new ServerGroup(Map.of("type", "5")).getType());
		assertEquals(PrivilegeKeyType.UNKNOWN, new PrivilegeKey(Map.of("token_type", "4")).getType());
		assertFalse(new PrivilegeKey(Map.of("token_type", "4")).isChannelGroupToken());
		assertEquals(TextMessageTargetMode.UNKNOWN, event(TextMessageEvent::new, "targetmode=8 msg=hi").getTargetMode());
	}

	@Test void unknownCodesCannotBeSent() {
		assertThrows(IllegalArgumentException.class, () -> new PermissionTarget(PermissionGroupType.UNKNOWN, 1, 0));
		assertThrows(IllegalArgumentException.class, () -> ServerGroupCommands.serverGroupAdd("x", PermissionGroupDatabaseType.UNKNOWN));
	}

	@Test void unknownPropertiesAndUnsignedLimitsArePreserved() {
		var info = new InstanceInfo(Map.of("serverinstance_max_download_total_bandwidth", UNSIGNED_MAX,
			"serverinstance_max_upload_total_bandwidth", "1000", "serverinstance_future_setting", "x"));
		assertEquals(OptionalLong.empty(), info.getMaxDownloadBandwidth());
		assertEquals(OptionalLong.of(1000), info.getMaxUploadBandwidth());
		assertEquals("x", info.getMap().get("serverinstance_future_setting"));
		assertEquals(Optional.of(UNSIGNED_MAX), assertThrows(TS3InvalidValueException.class,
			() -> info.requireLong("serverinstance_max_download_total_bandwidth")).getRawValue());

		var server = new VirtualServerInfo(Map.of("virtualserver_download_quota", UNSIGNED_MAX, "virtualserver_upload_quota", "5"));
		assertEquals(OptionalLong.empty(), server.getDownloadQuota());
		assertEquals(OptionalLong.of(5), server.getUploadQuota());
	}

	@Test void timesAreInstantsAndDurations() {
		var client = new Client(Map.of("client_created", "0", "client_lastconnected", "1791833758", "client_idle_time", "1500"));
		assertEquals(Optional.empty(), client.getCreatedDate());
		assertEquals(Optional.of(Instant.ofEpochSecond(1791833758)), client.getLastConnectedDate());
		assertEquals(Duration.ofMillis(1500), client.getIdleTime());

		var occupied = new Channel(Map.of("seconds_empty", "-1", "total_clients_family", "1"));
		assertEquals(Optional.empty(), occupied.getTimeEmpty());
		assertEquals(Optional.of(Duration.ofSeconds(30)), new Channel(Map.of("seconds_empty", "30")).getTimeEmpty());
		assertEquals(OptionalInt.empty(), new Channel(Map.of("channel_maxclients", "-1")).getMaxClients());

		var permanent = new Ban(Map.of("duration", "0", "created", "1791833952", "invokercldbid", "0"));
		assertTrue(permanent.isPermanent());
		assertEquals(Instant.ofEpochSecond(1791833952), permanent.getCreatedDate());
		assertEquals(Optional.empty(), permanent.getInvokerClientDBId());
		assertEquals(Duration.ofSeconds(60), new Ban(Map.of("duration", "60")).getDuration());

		assertEquals(Duration.ofSeconds(6), new FileTransfer(Map.of("runtime", "6")).getRuntime());
		assertEquals(Optional.empty(), new VirtualServer(Map.of("virtualserver_status", "offline")).getUptime());
		assertEquals(OptionalInt.empty(), new VirtualServer(Map.of("virtualserver_status", "offline")).getClientsOnline());
		assertEquals(Optional.of(Duration.ofSeconds(173)), new VirtualServer(Map.of("virtualserver_uptime", "173")).getUptime());
	}

	@Test void identifiersAreTypedAndValidated() {
		assertThrows(IllegalArgumentException.class, () -> ClientId.of(-1));
		assertThrows(IllegalArgumentException.class, () -> ClientDatabaseId.of(0));
		assertNotEquals((Object) ClientId.of(2), ClientDatabaseId.of(2));
		assertEquals(Optional.of("0"), assertThrows(TS3InvalidValueException.class,
			() -> new DatabaseClient(Map.of("cldbid", "0")).getDatabaseId()).getRawValue());
		assertEquals(ClientDatabaseId.of(7), new QueryLogin(Map.of("cldbid", "7")).getClientDatabaseId());
		assertEquals(Optional.empty(), new ServerQueryInfo(Map.of("client_database_id", "0")).getDatabaseId());

		var join = event(ClientJoinEvent::new, "clid=4 client_database_id=9 client_servergroups=");
		assertEquals(ClientId.of(4), join.getClientId());
		assertEquals(ClientDatabaseId.of(9), join.getClientDatabaseId());
		assertEquals(Optional.empty(), join.getInvokerId());
		assertEquals(OptionalInt.empty(), join.getReasonId());
		assertEquals(List.of(), join.getClientServerGroups());
		assertEquals(0, join.getAmountOfServerGroups());
		var serverInvoked = event(TextMessageEvent::new, "targetmode=3 msg=x invokerid=0 invokername=Server");
		assertEquals(Optional.of(ClientId.of(0)), serverInvoked.getInvokerId());
		assertEquals(Optional.empty(), serverInvoked.getInvokerUniqueId());
		assertEquals(Optional.empty(), serverInvoked.getTargetClientId());
	}

	@Test void optionalFieldsDistinguishUnsetValues() {
		var entry = new FileListEntry(Map.of("name", "x", "path", "/", "size", "0", "incompletesize", "3", "type", "1"));
		assertTrue(entry.isStillUploading());
		assertEquals(3, entry.getFileSize());
		assertFalse(new FileListEntry(Map.of("size", "4")).isStillUploading());
		assertEquals(OptionalLong.empty(), new IconFile(Map.of("name", "not-an-icon")).getIconId());

		var search = new PermissionAssignment(Map.of("t", "0", "id1", "6", "id2", "0", "p", "221"));
		assertEquals(OptionalInt.empty(), search.getValue());
		assertEquals(Optional.empty(), search.isNegated());
		var overview = new PermissionAssignment(Map.of("t", "0", "id1", "6", "id2", "0", "p", "221", "v", "75", "n", "0", "s", "1"));
		assertEquals(OptionalInt.of(75), overview.getValue());
		assertEquals(Optional.of(true), overview.isSkipped());

		var parameters = new FileTransferParameters(Map.of("ftkey", "k", "port", "30033", "seekpos", "0"));
		assertEquals(Optional.empty(), parameters.findFailure());
		assertEquals(Optional.empty(), parameters.getFileServerHost());
		var rejected = new FileTransferParameters(Map.of("status", "2054", "msg", "file not found"));
		assertEquals(2054, rejected.findFailure().orElseThrow().getId());
		assertEquals("file not found", rejected.findFailure().orElseThrow().getMessage());
		assertEquals(Optional.empty(), new FileTransferParameters(Map.of("status", "0", "msg", "ok")).findFailure());
		assertEquals(Optional.empty(), new FileTransferParameters(Map.of("status", "1281")).findFailure());
		assertEquals(OptionalInt.of(5), new ClientInfo(ClientId.of(1), Map.of("client_default_channel", "/5")).getDefaultChannel());
		assertEquals(OptionalInt.empty(), new ClientInfo(ClientId.of(1), Map.of("client_default_channel", "")).getDefaultChannel());
		assertEquals(Optional.empty(), new Client(Map.of("client_country", "")).getCountry());
	}

	@Test void recordsAreValidatedValues() {
		var row = DefaultArrayResponse.parse("version=3.13.8 build=1779874471 platform=Linux future=1").getFirstResponse().orElseThrow();
		assertEquals(new Version("3.13.8", "1779874471", "Linux"), Version.from(row));
		var permission = Permission.from(new Wrapper(Map.of("permsid", "i_x", "permvalue", "-1", "permnegated", "1", "permskip", "0")));
		assertEquals(new Permission("i_x", -1, true, false), permission);
		assertEquals(Optional.of("yes"), assertThrows(TS3InvalidValueException.class,
			() -> Permission.from(new Wrapper(Map.of("permsid", "i_x", "permvalue", "1", "permnegated", "yes", "permskip", "0")))).getRawValue());
		assertThrows(TS3InvalidValueException.class, () -> ServerGroupClient.from(new Wrapper(Map.of("cldbid", "2"))));
		assertThrows(NullPointerException.class, () -> new Binding(null));
		assertFalse(new Snapshot("secret").toString().contains("secret"));
	}

	@Test void mapBackedModelsHaveValueEquality() {
		assertEquals(new Client(Map.of("clid", "1")), new Client(new HashMap<>(Map.of("clid", "1"))));
		assertNotEquals(new Client(Map.of("clid", "1")), new Wrapper(Map.of("clid", "1")));
		assertNotEquals(new ClientInfo(ClientId.of(1), Map.of()), new ClientInfo(ClientId.of(2), Map.of()));
		assertEquals(new ChannelInfo(4, Map.of()).hashCode(), new ChannelInfo(4, Map.of()).hashCode());
	}

	private static <T> T event(Function<Wrapper, T> constructor, String raw) {
		return constructor.apply(DefaultArrayResponse.parse(raw).getFirstResponse().orElseThrow());
	}

	private static void assertUnsupported(Executable mutation) {
		assertThrows(UnsupportedOperationException.class, mutation);
	}
}
