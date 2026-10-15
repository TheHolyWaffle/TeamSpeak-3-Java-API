package com.github.theholywaffle.teamspeak3.api.wrapper;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.ChannelBannerMode;
import com.github.theholywaffle.teamspeak3.api.ChannelGroupId;
import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.ClientFileTransferId;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.Codec;
import com.github.theholywaffle.teamspeak3.api.IconId;
import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import com.github.theholywaffle.teamspeak3.api.PermissionGroupType;
import com.github.theholywaffle.teamspeak3.api.PrivilegeKeyGrant;
import com.github.theholywaffle.teamspeak3.api.PrivilegeKeyType;
import com.github.theholywaffle.teamspeak3.api.ServerFileTransferId;
import com.github.theholywaffle.teamspeak3.api.ServerGroupId;
import com.github.theholywaffle.teamspeak3.api.Snapshot;
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.VirtualServerStatus;
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent;
import com.github.theholywaffle.teamspeak3.api.event.PrivilegeKeyUsedEvent;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import com.github.theholywaffle.teamspeak3.api.exception.TS3InvalidValueException;
import com.github.theholywaffle.teamspeak3.commands.ServerGroupCommands;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class ModelValuesTest {

    private static final String UNSIGNED_MAX = "18446744073709551615";

    @Test
    void receivedPropertiesAreCopiedAndCannotBeMutated() {
        var source = new HashMap<>(
                Map.of("clid", "5", "client_servergroups", "6,8", "client_badges", "overwolf=1:badges=a,b"));
        var client = new Client(source);
        source.put("clid", "6");
        source.put("injected", "1");
        assertEquals(ClientId.of(5), client.getId());
        assertFalse(client.getMap().containsKey("injected"));
        assertUnsupported(() -> client.getMap().put("clid", "7"));
        assertUnsupported(() -> client.getServerGroups().add(ServerGroupId.of(9)));
        assertUnsupported(() -> client.getBadgeGUIDs().clear());
        assertEquals(List.of(ServerGroupId.of(6), ServerGroupId.of(8)), client.getServerGroups());
        assertEquals(List.of("a", "b"), client.getBadgeGUIDs());
        assertTrue(client.hasOverwolf());

        var response = DefaultArrayResponse.parse("cid=1 name=a|name=b");
        assertUnsupported(() -> response.getResponses().clear());
        assertUnsupported(() -> response.getResponses().getLast().getMap().remove("cid"));
        assertEquals("1", response.getResponses().getLast().require("cid"));
        assertThrows(NullPointerException.class, () -> new Wrapper(new HashMap<>(Collections.singletonMap("k", null))));
    }

    @Test
    void missingMalformedAndValuelessPropertiesStayDistinct() {
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
        assertEquals(
                Optional.of(""),
                assertThrows(TS3InvalidValueException.class, () -> row.requireLong("valueless"))
                        .getRawValue());
        assertEquals(
                Optional.of("2"),
                assertThrows(TS3InvalidValueException.class, () -> row.requireBoolean("flag"))
                        .getRawValue());
        assertEquals(
                Optional.of("x"),
                assertThrows(TS3InvalidValueException.class, () -> row.findDouble("text"))
                        .getRawValue());
        assertEquals(Optional.of(""), row.find("valueless"));
        assertEquals("x", row.require("text"));

        // A malformed property only affects the getters that read it.
        var channel = new Channel(Map.of("cid", "3", "channel_name", "a", "channel_codec", "opus"));
        assertEquals(ChannelId.of(3), channel.getId());
        assertEquals("a", channel.getName());
        assertEquals(
                Optional.of("opus"),
                assertThrows(TS3InvalidValueException.class, channel::getCodec).getRawValue());
    }

    @Test
    void unknownCodesMapToUnknownAndKeepTheirRawValue() {
        var channel = new Channel(Map.of("cid", "3", "channel_codec", "99"));
        assertEquals(Codec.UNKNOWN, channel.getCodec());
        assertEquals("99", channel.getMap().get("channel_codec"));
        assertEquals(
                ChannelBannerMode.UNKNOWN,
                new ChannelInfo(ChannelId.of(3), Map.of("channel_banner_mode", "7")).getBannerMode());
        assertEquals(
                VirtualServerStatus.UNKNOWN,
                new VirtualServer(Map.of("virtualserver_status", "hibernating")).getStatus());
        assertEquals(
                VirtualServerStatus.VIRTUAL_ONLINE,
                new VirtualServer(Map.of("virtualserver_status", "virtual online")).getStatus());
        assertEquals(PermissionGroupType.UNKNOWN, new PermissionAssignment(Map.of("t", "9")).getType());
        assertEquals(PermissionGroupDatabaseType.UNKNOWN, new ServerGroup(Map.of("type", "5")).getType());
        assertEquals(PrivilegeKeyType.UNKNOWN, new PrivilegeKey(Map.of("token_type", "4")).getType());
        assertFalse(new PrivilegeKey(Map.of("token_type", "4")).isChannelGroupToken());
        assertEquals(
                TextMessageTargetMode.UNKNOWN,
                event(TextMessageEvent::new, "targetmode=8 msg=hi").getTargetMode());
    }

    @Test
    void unknownCodesCannotBeSent() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ServerGroupCommands.serverGroupAdd("x", PermissionGroupDatabaseType.UNKNOWN));
    }

    @Test
    void unknownPropertiesAndUnsignedLimitsArePreserved() {
        var info = new InstanceInfo(Map.of(
                "serverinstance_max_download_total_bandwidth",
                UNSIGNED_MAX,
                "serverinstance_max_upload_total_bandwidth",
                "1000",
                "serverinstance_future_setting",
                "x"));
        assertEquals(OptionalLong.empty(), info.getMaxDownloadBandwidth());
        assertEquals(OptionalLong.of(1000), info.getMaxUploadBandwidth());
        assertEquals("x", info.getMap().get("serverinstance_future_setting"));
        assertEquals(
                Optional.of(UNSIGNED_MAX),
                assertThrows(
                                TS3InvalidValueException.class,
                                () -> info.requireLong("serverinstance_max_download_total_bandwidth"))
                        .getRawValue());

        var server = new VirtualServerInfo(
                Map.of("virtualserver_download_quota", UNSIGNED_MAX, "virtualserver_upload_quota", "5"));
        assertEquals(OptionalLong.empty(), server.getDownloadQuota());
        assertEquals(OptionalLong.of(5), server.getUploadQuota());
    }

    @Test
    void timesAreInstantsAndDurations() {
        var client = new Client(
                Map.of("client_created", "0", "client_lastconnected", "1791833758", "client_idle_time", "1500"));
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
        assertEquals(
                OptionalInt.empty(), new VirtualServer(Map.of("virtualserver_status", "offline")).getClientsOnline());
        assertEquals(
                Optional.of(Duration.ofSeconds(173)),
                new VirtualServer(Map.of("virtualserver_uptime", "173")).getUptime());
    }

    @Test
    void identifiersAreTypedAndValidated() {
        assertThrows(IllegalArgumentException.class, () -> ClientId.of(-1));
        assertThrows(IllegalArgumentException.class, () -> ClientDatabaseId.of(0));
        assertNotEquals((Object) ClientId.of(2), ClientDatabaseId.of(2));
        assertEquals(
                Optional.of("0"),
                assertThrows(
                                TS3InvalidValueException.class,
                                () -> new DatabaseClient(Map.of("cldbid", "0")).getDatabaseId())
                        .getRawValue());
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

    @Test
    void entityIdsAreLongBackedAndZeroIsNone() {
        assertThrows(IllegalArgumentException.class, () -> ChannelId.of(0));
        assertThrows(IllegalArgumentException.class, () -> ServerGroupId.of(-1));
        assertThrows(IllegalArgumentException.class, () -> ClientFileTransferId.of(-1));
        assertEquals("0", ClientFileTransferId.of(0).toString());
        assertNotEquals((Object) ServerGroupId.of(2), ChannelGroupId.of(2));
        long beyondInt = 1L + Integer.MAX_VALUE;
        assertEquals(ChannelId.of(beyondInt), new Channel(Map.of("cid", Long.toString(beyondInt))).getId());
        assertEquals(
                ClientDatabaseId.of(beyondInt),
                new DatabaseClient(Map.of("cldbid", Long.toString(beyondInt))).getDatabaseId());
        assertEquals(
                Optional.of("0"),
                assertThrows(TS3InvalidValueException.class, () -> new Channel(Map.of("cid", "0")).getId())
                        .getRawValue());

        assertEquals(Optional.empty(), new Channel(Map.of("pid", "0")).getParentChannelId());
        assertEquals(Optional.of(ChannelId.of(2)), new Channel(Map.of("pid", "2")).getParentChannelId());
        assertThrows(TS3InvalidValueException.class, () -> new Channel(Map.of()).getParentChannelId());
        assertThrows(TS3InvalidValueException.class, () -> new Channel(Map.of("pid", "-1")).getParentChannelId());
        var unselected = new ServerQueryInfo(
                Map.of("virtualserver_id", "0", "client_channel_id", "0", "client_origin_server_id", "0"));
        assertEquals(Optional.empty(), unselected.getVirtualServerId());
        assertEquals(Optional.empty(), unselected.getChannelId());
        assertEquals(Optional.empty(), unselected.getOriginServerId());
        assertEquals(Optional.empty(), new QueryLogin(Map.of("sid", "0")).getVirtualServerId());
        assertEquals(Optional.empty(), new FileInfo(Map.of("cid", "0")).getChannelId());

        var serverKey = new PrivilegeKey(Map.of("token_type", "0", "token_id1", "6", "token_id2", "0"));
        assertEquals(PrivilegeKeyGrant.serverGroup(ServerGroupId.of(6)), serverKey.getGrant());
        var channelKey = new PrivilegeKey(Map.of("token_type", "1", "token_id1", "5", "token_id2", "3"));
        assertEquals(PrivilegeKeyGrant.channelGroup(ChannelGroupId.of(5), ChannelId.of(3)), channelKey.getGrant());
        var usedEvent = new PrivilegeKeyUsedEvent(new Wrapper(Map.of("token1", "5", "token2", "3")));
        assertEquals(PrivilegeKeyGrant.channelGroup(ChannelGroupId.of(5), ChannelId.of(3)), usedEvent.getGrant());
        assertEquals(PrivilegeKeyType.CHANNEL_GROUP, usedEvent.getPrivilegeKeyType());
        assertEquals(
                PrivilegeKeyGrant.serverGroup(ServerGroupId.of(6)),
                new PrivilegeKeyUsedEvent(new Wrapper(Map.of("token1", "6", "token2", "0"))).getGrant());

        assertEquals(
                List.of(ServerGroupId.of(2), ServerGroupId.of(beyondInt)),
                new Client(Map.of("client_servergroups", "2," + beyondInt)).getServerGroups());
        assertThrows(
                TS3InvalidValueException.class,
                () -> new Client(Map.of("client_servergroups", "2,0")).getServerGroups());

        var transfer = new FileTransfer(Map.of("clientftfid", "0", "serverftfid", "7"));
        assertEquals(ClientFileTransferId.of(0), transfer.getClientTransferId());
        assertEquals(ServerFileTransferId.of(7), transfer.getServerTransferId());
    }

    @Test
    void clientEventsMapChannelSentinelsAndBanDuration() {
        var join = event(ClientJoinEvent::new, "cfid=0 ctid=1 clid=4");
        assertEquals(Optional.empty(), join.getClientFromId());
        assertEquals(ChannelId.of(1), join.getClientTargetId());

        var banned = event(
                ClientLeaveEvent::new,
                "cfid=1 ctid=0 reasonid=6 invokerid=8 invokername=serveradmin invokeruid=serveradmin"
                        + " reasonmsg=probe bantime=0 clid=10");
        assertEquals(ChannelId.of(1), banned.getClientFromId());
        assertEquals(Optional.empty(), banned.getClientTargetId());
        assertEquals(OptionalInt.of(6), banned.getReasonId());
        assertEquals(Optional.of(Duration.ZERO), banned.getBanDuration());
        assertEquals(
                Optional.of(Duration.ofMinutes(10)),
                event(ClientLeaveEvent::new, "cfid=1 ctid=0 reasonid=6 bantime=600 clid=10")
                        .getBanDuration());

        var kicked = event(
                ClientLeaveEvent::new,
                "cfid=1 ctid=0 reasonid=5 invokerid=4 invokername=serveradmin invokeruid=serveradmin"
                        + " reasonmsg=bye clid=7");
        assertEquals(OptionalInt.of(5), kicked.getReasonId());
        assertEquals(Optional.empty(), kicked.getBanDuration());
    }

    @Test
    void iconIdsAreUnsignedAndZeroIsNone() {
        var icon = Optional.of(IconId.of(3815707113L));
        assertEquals(icon, new ServerGroup(Map.of("iconid", "-479260183")).getIconId());
        assertEquals(icon, new ChannelGroup(Map.of("iconid", "-479260183")).getIconId());
        assertEquals(icon, new ServerGroup(Map.of("iconid", "3815707113")).getIconId());
        assertEquals(Optional.empty(), new ChannelGroup(Map.of("iconid", "0")).getIconId());
        assertEquals(icon, new Channel(Map.of("channel_icon_id", "3815707113")).getIconId());
        assertEquals(icon, new Client(Map.of("client_icon_id", "-479260183")).getIconId());
        assertEquals(icon, new VirtualServerInfo(Map.of("virtualserver_icon_id", "3815707113")).getIconId());
        assertEquals(Optional.empty(), new VirtualServerInfo(Map.of("virtualserver_icon_id", "0")).getIconId());
        assertEquals(icon, new DatabaseClientInfo(Map.of("client_icon_id", "3815707113")).getIconId());
        assertEquals(Optional.empty(), new DatabaseClientInfo(Map.of()).getIconId());
        assertEquals(
                icon,
                event(ClientJoinEvent::new, "clid=4 client_icon_id=-479260183").getClientIconId());
        assertEquals(icon, new IconFile(Map.of("name", "icon_3815707113")).getIconId());
        assertEquals(Optional.empty(), new IconFile(Map.of("name", "icon_0")).getIconId());
        assertEquals(Optional.empty(), new IconFile(Map.of("name", "icon_4294967296")).getIconId());
        assertEquals(
                Optional.empty(),
                assertThrows(TS3InvalidValueException.class, () -> new ServerGroup(Map.of()).getIconId())
                        .getRawValue());
        assertEquals(
                Optional.of("4294967296"),
                assertThrows(
                                TS3InvalidValueException.class,
                                () -> new ServerGroup(Map.of("iconid", "4294967296")).getIconId())
                        .getRawValue());
        assertThrows(
                TS3InvalidValueException.class, () -> new ServerGroup(Map.of("iconid", "-2147483649")).getIconId());
    }

    @Test
    void optionalFieldsDistinguishUnsetValues() {
        var entry =
                new FileListEntry(Map.of("name", "x", "path", "/", "size", "0", "incompletesize", "3", "type", "1"));
        assertTrue(entry.isStillUploading());
        assertEquals(3, entry.getFileSize());
        assertFalse(new FileListEntry(Map.of("size", "4")).isStillUploading());
        assertEquals(Optional.empty(), new IconFile(Map.of("name", "not-an-icon")).getIconId());

        var search = new PermissionAssignment(Map.of("t", "0", "id1", "6", "id2", "0", "p", "221"));
        assertEquals(OptionalInt.empty(), search.getValue());
        assertEquals(Optional.empty(), search.isNegated());
        var overview = new PermissionAssignment(
                Map.of("t", "0", "id1", "6", "id2", "0", "p", "221", "v", "75", "n", "0", "s", "1"));
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
        assertEquals(
                Optional.of(ChannelId.of(5)),
                new ClientInfo(ClientId.of(1), Map.of("client_default_channel", "/5")).getDefaultChannel());
        assertEquals(
                Optional.empty(),
                new ClientInfo(ClientId.of(1), Map.of("client_default_channel", "")).getDefaultChannel());
        assertEquals(Optional.empty(), new Client(Map.of("client_country", "")).getCountry());
    }

    @Test
    void recordsAreValidatedValues() {
        var row = DefaultArrayResponse.parse("version=3.13.8 build=1779874471 platform=Linux future=1")
                .getFirstResponse()
                .orElseThrow();
        assertEquals(new Version("3.13.8", "1779874471", "Linux"), Version.from(row));
        var permission = Permission.from(
                new Wrapper(Map.of("permsid", "i_x", "permvalue", "-1", "permnegated", "1", "permskip", "0")));
        assertEquals(new Permission("i_x", -1, true, false), permission);
        assertEquals(
                Optional.of("yes"),
                assertThrows(
                                TS3InvalidValueException.class,
                                () -> Permission.from(new Wrapper(Map.of(
                                        "permsid", "i_x", "permvalue", "1", "permnegated", "yes", "permskip", "0"))))
                        .getRawValue());
        assertThrows(TS3InvalidValueException.class, () -> ServerGroupClient.from(new Wrapper(Map.of("cldbid", "2"))));
        assertThrows(NullPointerException.class, () -> new Binding(null));
        assertFalse(new Snapshot("secret").toString().contains("secret"));
    }

    @Test
    void mapBackedModelsHaveValueEquality() {
        assertEquals(new Client(Map.of("clid", "1")), new Client(new HashMap<>(Map.of("clid", "1"))));
        assertNotEquals(new Client(Map.of("clid", "1")), new Wrapper(Map.of("clid", "1")));
        assertNotEquals(new ClientInfo(ClientId.of(1), Map.of()), new ClientInfo(ClientId.of(2), Map.of()));
        assertEquals(
                new ChannelInfo(ChannelId.of(4), Map.of()).hashCode(),
                new ChannelInfo(ChannelId.of(4), Map.of()).hashCode());
    }

    private static <T> T event(Function<Wrapper, T> constructor, String raw) {
        return constructor.apply(
                DefaultArrayResponse.parse(raw).getFirstResponse().orElseThrow());
    }

    private static void assertUnsupported(Executable mutation) {
        assertThrows(UnsupportedOperationException.class, mutation);
    }
}
