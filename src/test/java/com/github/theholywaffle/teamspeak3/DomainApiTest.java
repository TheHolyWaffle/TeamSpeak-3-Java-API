package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.BanRequest;
import com.github.theholywaffle.teamspeak3.api.ChannelGroupFilter;
import com.github.theholywaffle.teamspeak3.api.ClientBan;
import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.FileLocation;
import com.github.theholywaffle.teamspeak3.api.PermissionGrant;
import com.github.theholywaffle.teamspeak3.api.PermissionGroupType;
import com.github.theholywaffle.teamspeak3.api.PermissionTarget;
import com.github.theholywaffle.teamspeak3.api.ServerGroupType;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3Exception;
import com.github.theholywaffle.teamspeak3.api.exception.TS3InvalidValueException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
class DomainApiTest {
    private static final String EMPTY = FakeServerQuery.EMPTY;
    private static final String OK = FakeServerQuery.OK;

    private static TS3Query query(FakeServerQuery peer) {
        return new TS3Query(new TS3Config()
                .setHost("127.0.0.1")
                .setQueryPort(peer.port())
                .setFloodRate(TS3Query.FloodRate.UNLIMITED));
    }

    @Test
    void lookupsReturnEmptyForAbsentEntitiesAndPreserveOtherFailures() throws Exception {
        try (var peer = new FakeServerQuery(p -> {
                    p.expect("clientgetids cluid=gone");
                    p.write(EMPTY);
                    p.expect("clientgetids cluid=left");
                    p.write("cluid=left clid=9\n" + OK);
                    p.expect("clientinfo clid=9");
                    p.write("error id=512 msg=invalid\\sclientID\n");
                    p.expect("clientgetids cluid=here");
                    p.write("cluid=here clid=4\n" + OK);
                    p.expect("clientinfo clid=4");
                    p.write("client_nickname=Here\n" + OK);
                    p.expect("clientdbfind pattern=gone -uid");
                    p.write(EMPTY);
                    p.expect("serveridgetbyport virtualserver_port=1");
                    p.write("error id=1024 msg=invalid\\sserverID\n");
                    p.expect("serveridgetbyport virtualserver_port=2");
                    p.write("error id=2568 msg=insufficient\\sclient\\spermissions\n");
                    p.expect("serveridgetbyport virtualserver_port=9987");
                    p.write("server_id=1\n" + OK);
                    p.expect("channelfind pattern=none");
                    p.expect("channellist -topic -flags -voice -limits -icon -secondsempty -banners");
                    p.write("error id=768 msg=invalid\\schannelID\n");
                    p.write("cid=1 channel_name=Default\n" + OK);
                    p.finishQuit();
                });
                var query = query(peer)) {
            query.connect();
            var api = query.getApi();
            assertEquals(Optional.empty(), api.clients().findByUniqueId("gone"));
            assertEquals(
                    Optional.empty(),
                    api.clients().findByUniqueId("left"),
                    "A client leaving between lookups is absent");
            assertEquals(
                    "Here", api.clients().findByUniqueId("here").orElseThrow().getNickname());
            assertEquals(Optional.empty(), api.databaseClients().findByUniqueId("gone"));
            assertEquals(Optional.empty(), api.servers().findIdByPort(1));
            assertEquals(
                    2568,
                    assertThrows(
                                    TS3CommandFailedException.class,
                                    () -> api.servers().findIdByPort(2))
                            .getErrorId());
            assertEquals(Optional.of(1), api.servers().findIdByPort(9987));
            assertEquals(List.of(), api.channels().search("none"));
            query.exit();
            peer.await();
        }
    }

    @Test
    void serverMessageByIdSelectsThatServerFirst() throws Exception {
        try (var peer = new FakeServerQuery(p -> {
                    p.expect("use sid=2 -virtual");
                    p.write(OK);
                    p.expect("sendtextmessage targetmode=3 target=0 msg=hi");
                    p.write(OK);
                    p.finishQuit();
                });
                var query = query(peer)) {
            query.connect();
            query.getApi().messages().sendServer(2, "hi");
            query.exit();
            peer.await();
        }
    }

    @Test
    void emptyClientDatabaseListsNoClients() throws Exception {
        try (var peer = new FakeServerQuery(p -> {
                    // Recorded from TS3 3.13.8 after deleting a virtual server's only database client.
                    p.expect("clientdblist start=0 duration=1 -count");
                    p.write(EMPTY);
                    p.finishQuit();
                });
                var query = query(peer)) {
            query.connect();
            assertEquals(List.of(), query.getApi().databaseClients().list());
            query.exit();
            peer.await();
        }
    }

    @Test
    void requiredResultsFailInsteadOfReturningDefaults() throws Exception {
        try (var peer = new FakeServerQuery(p -> {
                    p.expect("whoami");
                    p.write(EMPTY);
                    p.expect("permidgetbyname permsid=i_empty");
                    p.write(EMPTY);
                    p.expect("privilegekeyadd tokentype=0 tokenid1=6 tokenid2=0");
                    p.write("unexpected=1\n" + OK);
                    p.expect("channelinfo cid=99");
                    p.write("error id=768 msg=invalid\\schannelID\n");
                    p.expect("clientgetids cluid=malformed");
                    p.write("cluid=malformed clid=x\n" + OK);
                    p.finishQuit();
                });
                var query = query(peer)) {
            query.connect();
            var api = query.getApi();
            assertEquals(
                    "whoami returned no result",
                    assertThrows(TS3Exception.class, api::whoAmI).getMessage());
            assertEquals(
                    "permidgetbyname returned no result",
                    assertThrows(TS3Exception.class, () -> api.permissions().idByName("i_empty"))
                            .getMessage());
            var missing = assertThrows(
                    TS3InvalidValueException.class, () -> api.privilegeKeys().createForServerGroup(6, null));
            assertEquals("token", missing.getProperty());
            assertEquals(Optional.empty(), missing.getRawValue());
            assertEquals(
                    768,
                    assertThrows(
                                    TS3CommandFailedException.class,
                                    () -> api.channels().info(99))
                            .getErrorId());
            var malformed = assertThrows(
                    TS3InvalidValueException.class, () -> api.clients().findByUniqueId("malformed"));
            assertEquals("clid", malformed.getProperty());
            assertEquals(Optional.of("x"), malformed.getRawValue());
            assertFalse(malformed.isOutcomeUnknown());
            query.exit();
            peer.await();
        }
    }

    @Test
    void permissionTargetsSelectTheMatchingCommand() throws Exception {
        try (var peer = new FakeServerQuery(p -> {
                    for (String command : List.of(
                            "servergroupaddperm sgid=6 permsid=b_x permvalue=1 permnegated=1 permskip=1",
                            "channelgroupaddperm cgid=5 permsid=b_x permvalue=1",
                            "channeladdperm cid=4 permsid=i_x permvalue=7",
                            "clientaddperm cldbid=3 permsid=i_x permvalue=7 permskip=1",
                            "channelclientaddperm cid=4 cldbid=3 permsid=i_x permvalue=7",
                            "servergroupautoaddperm sgtype=10 permsid=i_x permvalue=7 permnegated=0 permskip=0",
                            "servergroupdelperm sgid=6 permsid=b_x",
                            "channelgroupdelperm cgid=5 permsid=b_x",
                            "channeldelperm cid=4 permsid=i_x",
                            "clientdelperm cldbid=3 permsid=i_x",
                            "channelclientdelperm cid=4 cldbid=3 permsid=i_x",
                            "servergroupautodelperm sgtype=10 permsid=i_x")) {
                        p.expect(command);
                        p.write(OK);
                    }
                    p.expect("servergrouppermlist sgid=6 -permsid");
                    p.write("permsid=b_x permvalue=1 permnegated=1 permskip=1\n" + OK);
                    p.expect("channelclientpermlist cid=4 cldbid=3 -permsid");
                    p.write(EMPTY);
                    p.finishQuit();
                });
                var query = query(peer)) {
            query.connect();
            var permissions = query.getApi().permissions();
            permissions.add(
                    PermissionTarget.serverGroup(6),
                    PermissionGrant.of("b_x", 1).withNegated().withSkip());
            permissions.add(PermissionTarget.channelGroup(5), PermissionGrant.of("b_x", 1));
            permissions.add(PermissionTarget.channel(4), "i_x", 7);
            permissions.add(
                    PermissionTarget.client(ClientDatabaseId.of(3)),
                    PermissionGrant.of("i_x", 7).withSkip());
            permissions.add(PermissionTarget.channelClient(4, ClientDatabaseId.of(3)), "i_x", 7);
            permissions.addToAllServerGroups(ServerGroupType.CHANNEL_GUEST, PermissionGrant.of("i_x", 7));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> permissions.add(
                            PermissionTarget.channel(4),
                            PermissionGrant.of("i_x", 7).withSkip()));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> permissions.add(
                            PermissionTarget.client(ClientDatabaseId.of(3)),
                            PermissionGrant.of("i_x", 7).withNegated()));
            permissions.delete(PermissionTarget.serverGroup(6), "b_x");
            permissions.delete(PermissionTarget.channelGroup(5), "b_x");
            permissions.delete(PermissionTarget.channel(4), "i_x");
            permissions.delete(PermissionTarget.client(ClientDatabaseId.of(3)), "i_x");
            permissions.delete(PermissionTarget.channelClient(4, ClientDatabaseId.of(3)), "i_x");
            permissions.deleteFromAllServerGroups(ServerGroupType.CHANNEL_GUEST, "i_x");
            var assigned = permissions.assigned(PermissionTarget.serverGroup(6));
            assertTrue(assigned.getFirst().negated() && assigned.getFirst().skipped());
            assertThrows(UnsupportedOperationException.class, () -> assigned.add(assigned.getFirst()));
            assertEquals(List.of(), permissions.assigned(PermissionTarget.channelClient(4, ClientDatabaseId.of(3))));
            query.exit();
            peer.await();
        }
    }

    @Test
    void requestObjectsReplacePositionalArguments() throws Exception {
        try (var peer = new FakeServerQuery(p -> {
                    p.expect("banadd mytsid=empty banreason=no\\saccount");
                    p.write("banid=11\n" + OK);
                    p.expect("banadd ip=1.2.3.4 name=spam.* time=3600");
                    p.write("banid=12\n" + OK);
                    p.expect("banclient banreason=bye -continueonerror clid=3|clid=4");
                    p.write("banid=13|banid=14\n" + OK);
                    p.expect("ftdeletefile cid=2 cpw=pw name=\\/a|name=\\/b");
                    p.write(OK);
                    p.expect("ftrenamefile cid=2 cpw=pw tcid=3 tcpw= oldname=\\/a newname=\\/c");
                    p.write(OK);
                    p.expect("channelgroupclientlist cid=2 cgid=8");
                    p.write(EMPTY);
                    p.expect("servernotifyregister event=channel id=0");
                    p.write(OK);
                    p.expect("servernotifyregister event=textprivate");
                    p.write(OK);
                    p.finishQuit();
                });
                var query = query(peer)) {
            query.connect();
            var api = query.getApi();
            assertEquals(11, api.bans().add(BanRequest.myTeamSpeakId("empty").withReason("no account")));
            assertEquals(
                    12,
                    api.bans().add(BanRequest.ip("1.2.3.4").withName("spam.*").withDuration(Duration.ofHours(1))));
            assertEquals(
                    List.of(13, 14),
                    api.bans()
                            .banClients(ClientBan.of(ClientId.of(3), ClientId.of(4))
                                    .withReason("bye")
                                    .continuingOnError()));
            var a = FileLocation.of(2, "/a").withChannelPassword("pw");
            api.files().delete(List.of(a, a.withPath("/b")));
            assertThrows(
                    IllegalArgumentException.class, () -> api.files().delete(List.of(a, FileLocation.of(3, "/b"))));
            api.files().move(a, FileLocation.of(3, "/c").withChannelPassword(""));
            assertEquals(
                    List.of(),
                    api.channelGroups()
                            .members(ChannelGroupFilter.all().inChannel(2).inGroup(8)));
            api.events().register(TS3EventType.CHANNEL, TS3EventType.TEXT_PRIVATE);
            assertNull(
                    query.getAsyncApi().clients().moveAll(List.of(), 2, null).getNow(null),
                    "An empty move sends nothing");
            query.exit();
            peer.await();
        }
    }

    @Test
    void valueObjectsRejectInvalidCombinations() {
        assertThrows(IllegalArgumentException.class, () -> new BanRequest(null, null, null, null, Duration.ZERO, null));
        assertThrows(IllegalArgumentException.class, () -> BanRequest.ip("x").withDuration(Duration.ofSeconds(-1)));
        assertThrows(IllegalArgumentException.class, ClientBan::of);
        assertThrows(IllegalArgumentException.class, () -> PermissionTarget.channel(0));
        assertThrows(IllegalArgumentException.class, () -> new PermissionTarget(PermissionGroupType.CHANNEL, 1, 2));
        assertThrows(
                IllegalArgumentException.class, () -> new PermissionTarget(PermissionGroupType.CHANNEL_CLIENT, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> PermissionGrant.of("", 1));
        assertThrows(IllegalArgumentException.class, () -> FileLocation.of(-1, "/a"));
        assertThrows(IllegalArgumentException.class, () -> FileLocation.of(1, ""));
        assertThrows(
                IllegalArgumentException.class, () -> ChannelGroupFilter.all().inGroup(0));
        assertEquals(
                ChannelGroupFilter.all().inChannel(2), ChannelGroupFilter.all().inChannel(2));
        assertTrue(ChannelGroupFilter.all().groupId().isEmpty());
        assertEquals(
                1,
                PermissionGrant.of(
                                com.github.theholywaffle.teamspeak3.api.BPermissionType.B_CHANNEL_CREATE_PERMANENT,
                                true)
                        .value());
    }
}
