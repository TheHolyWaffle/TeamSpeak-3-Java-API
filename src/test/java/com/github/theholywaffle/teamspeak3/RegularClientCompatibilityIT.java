package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.*;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import com.github.theholywaffle.teamspeak3.api.wrapper.Permission;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Regular-client administration through an isolated headless UDP fixture. */
class RegularClientCompatibilityIT {
    static Client regular(TS3Api api) {
        var matches = api.getClients().stream().filter(c -> c.isRegularClient() && c.getNickname().equals("issue441-regular-fixture")).toList();
        assertEquals(1, matches.size(), "Connect exactly one real regular client with the documented fixture nickname");
        return matches.getFirst();
    }

    // Full UDP clients send SHA-1/base64 passwords; ServerQuery typed APIs take plaintext.
    private static String passwordHash(String password) throws Exception {
        return java.util.Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-1")
            .digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @ParameterizedTest @EnumSource(ServerQueryCompatibilityIT.Target.class) @Timeout(180)
    void persistentClientAdministration(ServerQueryCompatibilityIT.Target target) throws Exception {
        var evidence = new java.util.ArrayList<String>();
        try (var network = org.testcontainers.containers.Network.newNetwork();
             var server = new TeamSpeakContainer().withNetwork(network).withNetworkAliases("server");
             var fixture = new RegularClientContainer(network)) {
            server.start(); fixture.start(); fixture.connect();
            try (var query = new TS3Query(server.config(target.protocol))) {
            query.connect(); var api = query.getApi(); api.selectVirtualServerById(1);
            for (String command : List.of("clientgetids", "clientgetdbidfromuid", "clientdbfind", "clientdbinfo", "clientdblist", "clientdbedit", "custominfo", "customset", "customsearch", "customdelete", "clientpermlist", "clientaddperm", "clientdelperm", "channelclientpermlist", "channelclientaddperm", "channelclientdelperm", "permoverview", "servergroupsbyclientid", "servergroupaddclient", "servergroupdelclient", "servergroupclientlist", "setclientchannelgroup", "channelgroupclientlist", "complainadd", "complainlist", "complaindel", "complaindelall")) {
                String help = api.executeRawCommand("help " + command).getRawResponse();
                assertTrue(help.startsWith("Usage: " + command)); evidence.add("help " + command + "\n" + help);
            }
            var client = regular(api); int dbid = client.getDatabaseId();
            assertEquals(client.getId(), api.getClientByUId(client.getUniqueIdentifier()).getId());
            assertTrue(api.isClientOnline(client.getId())); assertTrue(api.isClientOnline(client.getUniqueIdentifier()));
            var database = api.getDatabaseClientInfo(dbid);
            assertEquals(dbid, database.getDatabaseId()); assertTrue(client.getUniqueIdentifier().equals(database.getUniqueIdentifier()), "Database identity must match fixture");
            assertEquals(dbid, api.getDatabaseClientByUId(client.getUniqueIdentifier()).getDatabaseId());
            assertTrue(api.getDatabaseClientsByName(client.getNickname()).stream().anyMatch(c -> c.getDatabaseId() == dbid));
            assertTrue(api.getDatabaseClients().stream().anyMatch(c -> c.getDatabaseId() == dbid));
            assertTrue(api.getDatabaseClients(0, 100).stream().anyMatch(c -> c.getDatabaseId() == dbid));
            evidence.add("regular type=" + client.getType() + " clientVersion=" + api.getClientInfo(client.getId()).get("client_version"));
            evidence.add("database fields=" + new java.util.TreeSet<>(database.getMap().keySet()));
            String description = "database 日本語 | literal \\s";
            try (var restoration = new Cleanup(() -> api.editDatabaseClient(dbid, Map.of(ClientProperty.CLIENT_DESCRIPTION, database.getDescription())))) {
                api.editDatabaseClient(dbid, Map.of(ClientProperty.CLIENT_DESCRIPTION, description));
                assertEquals(description, api.getDatabaseClientInfo(dbid).getDescription());
            }
            String key = "issue441_property"; String value = "custom 日本語 | literal \\n";
            assertFalse(api.getCustomClientProperties(dbid).containsKey(key));
            try (var cleanup = new Cleanup(() -> api.deleteCustomClientProperty(dbid, key))) {
                api.setCustomClientProperty(dbid, key, value);
                assertEquals(value, api.getCustomClientProperties(dbid).get(key));
                assertFalse(api.searchCustomClientProperty(key).isEmpty());
                assertFalse(api.searchCustomClientProperty(key, "custom%").isEmpty());
                api.setCustomClientProperties(dbid, Map.of(key, value + " updated"));
                assertEquals(value + " updated", api.getCustomClientProperties(dbid).get(key));
            }
            assertFalse(api.getCustomClientProperties(dbid).containsKey(key));
            String permission = IPermissionType.I_CLIENT_TALK_POWER.getName();
            assertTrue(api.getClientPermissions(dbid).stream().noneMatch(p -> p.getName().equals(permission)));
            try (var cleanup = new Cleanup(() -> api.deleteClientPermission(dbid, permission))) {
                api.addClientPermission(dbid, permission, 42, true);
                assertPermission(api.getClientPermissions(dbid), permission, 42, true);
                api.addClientPermission(dbid, IPermissionType.I_CLIENT_TALK_POWER, 43, false);
                assertPermission(api.getClientPermissions(dbid), permission, 43, false);
                int channel = api.createChannel("regular-permissions-" + target, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
                try (var channelCleanup = new Cleanup(() -> api.deleteChannel(channel, true))) {
                    api.addChannelClientPermission(channel, dbid, permission, 44);
                    assertPermission(api.getChannelClientPermissions(channel, dbid), permission, 44, false);
                    int permissionId = api.getPermissionIdByName(permission);
                    var overview = api.getPermissionOverview(channel, dbid);
                    assertTrue(overview.stream().anyMatch(p -> p.getId() == permissionId && p.getType() == PermissionGroupType.GLOBAL_CLIENT && p.getValue() == 43));
                    assertTrue(overview.stream().anyMatch(p -> p.getId() == permissionId && p.getType() == PermissionGroupType.CHANNEL_CLIENT && p.getValue() == 44));
                    evidence.add("overview entries=" + overview.size() + " permission resolved by name");
                    api.deleteChannelClientPermission(channel, dbid, permission);
                    assertTrue(api.getChannelClientPermissions(channel, dbid).stream().noneMatch(p -> p.getName().equals(permission)));
                    int group = api.addChannelGroup("regular-channel-" + target);
                    try (var groupCleanup = new Cleanup(() -> api.deleteChannelGroup(group, true))) {
                        api.setClientChannelGroup(group, channel, dbid);
                        assertTrue(api.getChannelGroupClients(channel, dbid, group).stream().anyMatch(c -> c.getClientDatabaseId() == dbid && c.getChannelId() == channel && c.getChannelGroupId() == group));
                        assertTrue(api.getChannelGroupClientsByClientDBId(dbid).stream().anyMatch(c -> c.getClientDatabaseId() == dbid && c.getChannelId() == channel && c.getChannelGroupId() == group));
                        assertTrue(api.getChannelGroupClientsByChannelId(channel).stream().anyMatch(c -> c.getClientDatabaseId() == dbid && c.getChannelId() == channel && c.getChannelGroupId() == group));
                        assertTrue(api.getChannelGroupClientsByChannelGroupId(group).stream().anyMatch(c -> c.getClientDatabaseId() == dbid && c.getChannelId() == channel && c.getChannelGroupId() == group));
                    }
                }
            }
            assertTrue(api.getClientPermissions(dbid).stream().noneMatch(p -> p.getName().equals(permission)));
            int group = api.addServerGroup("regular-server-" + target);
            assertTrue(group > 0, "servergroupadd must return its created ID");
            try (var cleanup = new Cleanup(() -> api.deleteServerGroup(group, true))) {
                api.addClientToServerGroup(group, dbid);
                assertTrue(api.getServerGroupsByClientId(dbid).stream().anyMatch(g -> g.getId() == group));
                assertTrue(api.getServerGroupClients(group).stream().anyMatch(c -> c.getClientDatabaseId() == dbid));
                api.removeClientFromServerGroup(group, dbid);
                assertTrue(api.getServerGroupClients(group).isEmpty());
            }
            String complaint = "complaint 日本語 | literal \\s";
            assertTrue(api.getComplaints(dbid).isEmpty());
            try (var cleanup = new Cleanup(() -> api.deleteAllComplaints(dbid))) {
                api.addComplaint(dbid, complaint);
                var row = api.getComplaints(dbid).getFirst();
                assertEquals(dbid, row.getTargetClientDatabaseId()); assertEquals(complaint, row.getMessage());
                assertTrue(row.getTimestamp().getTime() > 0);
                assertTrue(api.getComplaints().stream().anyMatch(c -> c.getTargetClientDatabaseId() == dbid));
                api.deleteComplaint(dbid, row.getSourceClientDatabaseId()); assertTrue(api.getComplaints(dbid).isEmpty());
                api.addComplaint(dbid, complaint); api.deleteAllComplaints(dbid); assertTrue(api.getComplaints(dbid).isEmpty());
            }
            evidence.add("database/custom/client and channel-client permissions/memberships/complaints passed");
            assertFalse(api.getVersion().getVersion().isBlank()); query.exit();
            }
        } finally {
            Path path = Path.of("target", "compatibility", target + "-regular-client.txt");
            Files.createDirectories(path.getParent()); Files.write(path, evidence);
        }
    }


    @ParameterizedTest @EnumSource(ServerQueryCompatibilityIT.Target.class) @Timeout(120)
    void movesKicksAndClientBans(ServerQueryCompatibilityIT.Target target) throws Exception {
        var evidence = new java.util.ArrayList<String>();
        try (var network = org.testcontainers.containers.Network.newNetwork();
             var server = new TeamSpeakContainer().withNetwork(network).withNetworkAliases("server");
             var fixture = new RegularClientContainer(network)) {
            server.start(); fixture.start(); fixture.connect();
            try (var observer = new TS3Query(server.config(target.protocol));
                 var actor = new TS3Query(server.config(target.protocol))) {
                observer.connect(); actor.connect(); var api = observer.getApi(); var actions = actor.getApi();
                api.selectVirtualServerById(1); actions.selectVirtualServerById(1);
                var client = regular(api); int original = client.getChannelId(); int actorId = actions.whoAmI().getId();
                var moves = new EventProbe<com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent>(8);
                var leaves = new EventProbe<com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent>(8);
                api.addTS3Listeners(new com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter() {
                    @Override public void onClientMoved(com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent event) {
                        moves.capture(() -> { if (event.getClientId() == client.getId()) moves.publish(event); });
                    }
                    @Override public void onClientLeave(com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent event) {
                        leaves.capture(() -> { if (event.getClientId() == client.getId()) leaves.publish(event); });
                    }
                });
                api.registerEvent(com.github.theholywaffle.teamspeak3.api.event.TS3EventType.SERVER);
                api.registerEvent(com.github.theholywaffle.teamspeak3.api.event.TS3EventType.CHANNEL, 0);
                int channel = actions.createChannel("moves-" + target, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1", ChannelProperty.CHANNEL_PASSWORD, "fixture-password"));
                try (var cleanup = new Cleanup(() -> actions.deleteChannel(channel, true))) {
                    var wrongPassword = assertThrows(RegularClientContainer.CommandFailure.class, () -> fixture.command("clientmove clid=" + client.getId() + " cid=" + channel + " cpw=" + passwordHash("wrong")));
                    evidence.add("wrong channel password server error=" + wrongPassword.errorId());
                    assertEquals(781, wrongPassword.errorId());
                    assertEquals(original, api.getClientInfo(client.getId()).getChannelId());
                    fixture.command("clientmove clid=" + client.getId() + " cid=" + channel + " cpw=" + passwordHash("fixture-password"));
                    assertEquals(channel, moves.await().getTargetChannelId());
                    actions.moveClient(client.getId(), original); assertEquals(original, moves.await().getTargetChannelId());
                    actions.moveClients(new int[]{client.getId(), actorId}, channel, "fixture-password");
                    var moved = moves.await(); assertEquals(channel, moved.getTargetChannelId()); assertEquals(actorId, moved.getInvokerId());
                    assertEquals(channel, api.getClientInfo(client.getId()).getChannelId()); assertEquals(channel, actions.whoAmI().getChannelId());
                    actions.kickClientFromChannel("channel kick 日本語", client.getId());
                    var kicked = moves.await(); assertEquals(original, kicked.getTargetChannelId()); assertEquals(4, kicked.getReasonId());
                    assertEquals("channel kick 日本語", kicked.getReasonMessage()); assertEquals(actorId, kicked.getInvokerId());
                    actions.moveClient(client.getId(), channel);
                    assertEquals(channel, moves.await().getTargetChannelId());
                    int[] bans = actions.banClient(client.getId(), 60, "client ban 日本語");
                    assertTrue(bans.length > 0);
                    var banned = leaves.await(); assertEquals(6, banned.getReasonId()); assertEquals("client ban 日本語", banned.getReasonMessage());
                    assertEquals(actorId, banned.getInvokerId()); assertFalse(api.isClientOnline(client.getId()));
                    var listed = actions.getBans();
                    for (int ban : bans) assertTrue(listed.stream().anyMatch(row -> row.getId() == ban));
                    actions.deleteAllBans(); assertTrue(actions.getBans().isEmpty());
                    evidence.add("bulk password move, channel kick, regular-client ban leave reason6 and bulk ban deletion passed");
                    int nameBan = actions.addBan(null, "^issue441-regular-fixture$", null, 60, "owned nickname enforcement");
                    try (var banCleanup = new Cleanup(() -> actions.deleteBan(nameBan));
                         var rejected = new RegularClientContainer(network)) {
                        rejected.start();
                        var failure = assertThrows(IllegalStateException.class, rejected::connect);
                        assertTrue(failure.getMessage().startsWith("Regular-client connect failed:"));
                        assertTrue(api.getClients().stream().noneMatch(row -> row.isRegularClient() && row.getNickname().equals("issue441-regular-fixture")));
                        evidence.add("fresh regular-client connection failed while nickname ban exists; after removing it the following fresh connection succeeds (no ban-specific handshake error claimed)");
                    }
                    // Server kick uses a new disposable identity, independent of the banned socket's asynchronous teardown.
                    try (var second = new RegularClientContainer(network)) {
                        second.start(); second.connect(); var victim = regular(api);
                        var kickedServer = new EventProbe<com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent>(2);
                        try (var subscription = observer.subscribe(new com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter() {
                            @Override public void onClientLeave(com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent event) {
                                kickedServer.capture(() -> { if (event.getClientId() == victim.getId()) kickedServer.publish(event); });
                            }
                        })) {
                            actions.kickClientFromServer("server kick 日本語", victim.getId());
                            var left = kickedServer.await(); assertEquals(5, left.getReasonId());
                            assertEquals("server kick 日本語", left.getReasonMessage()); assertEquals(actorId, left.getInvokerId());
                            assertFalse(api.isClientOnline(victim.getId()));
                        }
                    }
                    evidence.add("independent regular-client server kick reason5 passed");
                }
                actor.exit(); observer.exit();
            }
        } finally {
            var path = Path.of("target", "compatibility", target + "-regular-actions.txt");
            Files.createDirectories(path.getParent()); Files.write(path, evidence);
        }
    }

    @ParameterizedTest @EnumSource(ServerQueryCompatibilityIT.Target.class) @Timeout(180)
    void regularEventsPrivilegeKeysAndOfflineMessages(ServerQueryCompatibilityIT.Target target) throws Exception {
        var evidence = new java.util.ArrayList<String>();
        try (var network = org.testcontainers.containers.Network.newNetwork();
             var server = new TeamSpeakContainer().withNetwork(network).withNetworkAliases("server");
             var fixture = new RegularClientContainer(network)) {
            server.start(); fixture.start();
            var joins = new EventProbe<com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent>(8);
            var leaves = new EventProbe<com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent>(8);
            var keys = new EventProbe<com.github.theholywaffle.teamspeak3.api.event.PrivilegeKeyUsedEvent>(8);
            var id = new java.util.concurrent.atomic.AtomicInteger(-1);
            try (var observer = new TS3Query(server.config(target.protocol))) {
                observer.connect(); var api = observer.getApi(); api.selectVirtualServerById(1);
                for (String command : List.of("messageadd", "messagelist", "messageget", "messageupdateflag", "messagedel", "clientpoke", "gm", "clientdbdelete", "tokenuse")) {
                    String help = api.executeRawCommand("help " + command).getRawResponse();
                    assertTrue(help.startsWith("Usage: " + command)); evidence.add("help " + command + "\n" + help);
                }
                api.addTS3Listeners(new com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter() {
                    @Override public void onClientJoin(com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent event) {
                        joins.capture(() -> { if (event.getClientNickname().equals("issue441-regular-fixture")) joins.publish(event); });
                    }
                    @Override public void onClientLeave(com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent event) {
                        leaves.capture(() -> { if (event.getClientId() == id.get()) leaves.publish(event); });
                    }
                    @Override public void onPrivilegeKeyUsed(com.github.theholywaffle.teamspeak3.api.event.PrivilegeKeyUsedEvent event) { keys.publish(event); }
                });
                api.registerEvent(com.github.theholywaffle.teamspeak3.api.event.TS3EventType.SERVER);
                api.registerEvent(com.github.theholywaffle.teamspeak3.api.event.TS3EventType.PRIVILEGE_KEY_USED);
                fixture.connect();
                var join = joins.await(); var client = regular(api); id.set(client.getId());
                assertEquals(0, join.getClientType()); assertEquals(client.getId(), join.getClientId());
                assertTrue(client.getUniqueIdentifier().equals(join.getUniqueClientIdentifier()), "Join identity must match fixture");
                evidence.add("full UDP regular-client join fields=" + new java.util.TreeSet<>(join.getMap().keySet()));
                int source = api.getServerGroups().stream().filter(g -> g.getType() == PermissionGroupDatabaseType.REGULAR && g.getName().equals("Server Admin")).findFirst().orElseThrow().getId();
                int group = api.copyServerGroup(source, "consumed-key-" + target, PermissionGroupDatabaseType.REGULAR);
                try (var groupCleanup = new Cleanup(() -> api.deleteServerGroup(group, true))) {
                    String token = api.addPrivilegeKeyServerGroup(group, "regular client consumption");
                    try (var keyCleanup = new Cleanup(() -> {
                        if (api.getPrivilegeKeys().stream().anyMatch(k -> k.getToken().equals(token))) api.deletePrivilegeKey(token);
                    })) {
                        fixture.command("tokenuse token=" + com.github.theholywaffle.teamspeak3.commands.CommandEncoding.encode(token));
                        var used = keys.await();
                        assertEquals(client.getId(), used.getClientId()); assertEquals(client.getDatabaseId(), used.getClientDatabaseId());
                        assertTrue(client.getUniqueIdentifier().equals(used.getClientUniqueIdentifier()), "Key event identity must match fixture"); assertTrue(token.equals(used.getPrivilegeKey()), "Key event must identify consumed key");
                        assertEquals(PrivilegeKeyType.SERVER_GROUP, used.getPrivilegeKeyType()); assertEquals(group, used.getPrivilegeKeyGroupId());
                        assertTrue(api.getPrivilegeKeys().stream().noneMatch(k -> k.getToken().equals(token)));
                        assertTrue(api.getServerGroupsByClientId(client.getDatabaseId()).stream().anyMatch(g -> g.getId() == group));
                        evidence.add("regular key consumed once; event fields=" + new java.util.TreeSet<>(used.getMap().keySet()));
                    }
                    var login = api.addServerQueryLogin("regular-inbox", client.getDatabaseId());
                    try (var loginCleanup = new Cleanup(() -> api.deleteServerQueryLogin(client.getDatabaseId()));
                         var reader = new TS3Query(server.config(target.protocol).setLoginCredentials("regular-inbox", login.getQueryClientLoginPassword()))) {
                        reader.connect(); var inbox = reader.getApi(); inbox.selectVirtualServerById(1);
                        var inboxInfo = inbox.whoAmI();
                        assertEquals(client.getDatabaseId(), inboxInfo.getDatabaseId());
                        assertEquals(1, inboxInfo.getOriginServerId());
                        assertTrue(inbox.getOfflineMessages().isEmpty());
                        String subject = "offline 日本語 | literal \\s"; String body = "body 日本語 | literal \\n";
                        api.sendOfflineMessage(client.getUniqueIdentifier(), subject, body);
                        var message = inbox.getOfflineMessages().getFirst();
                        assertEquals(subject, message.getSubject()); assertFalse(message.hasBeenRead()); assertTrue(message.getReceivedDate().getTime() > 0);
                        assertEquals(body, inbox.getOfflineMessage(message));
                        inbox.setMessageRead(message);
                        assertTrue(inbox.getOfflineMessages().getFirst().hasBeenRead());
                        inbox.setMessageReadFlag(message, false);
                        assertFalse(inbox.getOfflineMessages().getFirst().hasBeenRead());
                        inbox.deleteOfflineMessage(message.getId()); assertTrue(inbox.getOfflineMessages().isEmpty());
                        assertNull(inbox.getOfflineMessage(message.getId()));
                        evidence.add("offline message CRUD/read flags passed; deleted get returns null (empty successful response)");
                        reader.exit();
                    }
                }
                String poke = "poke 日本語 | literal \\s";
                api.pokeClient(client.getId(), poke); fixture.assertNotification("poke", poke);
                String broadcast = "broadcast 日本語 | literal \\n";
                api.broadcast(broadcast); fixture.assertNotification("text", broadcast);
                evidence.add("poke and instance broadcast received with exact decoded payload by full UDP client");
                fixture.disconnect();
                var leave = leaves.await(); assertEquals(client.getId(), leave.getClientId());
                assertFalse(api.isClientOnline(client.getId()));
                assertEquals("fixture disconnect", leave.getReasonMessage());
                evidence.add("full UDP regular-client leave fields=" + new java.util.TreeSet<>(leave.getMap().keySet()));
                api.deleteDatabaseClientProperties(client.getDatabaseId());
                var missing = assertThrows(com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException.class, () -> api.getDatabaseClientInfo(client.getDatabaseId()));
                assertEquals(512, missing.getError().getId()); assertEquals("invalid clientID", missing.getError().getMessage());
                evidence.add("deleted persistent client error=" + missing.getError().getId());
                assertFalse(api.getVersion().getVersion().isBlank()); observer.exit();
            }
        } finally {
            Path path = Path.of("target", "compatibility", target + "-regular-events-messages.txt");
            Files.createDirectories(path.getParent()); Files.write(path, evidence);
        }
    }

    private static void assertPermission(List<Permission> permissions, String name, int value, boolean skipped) {
        CompatibilityAssertions.permission(permissions, name, value, false, skipped);
    }

    private record Cleanup(Runnable action) implements AutoCloseable {
        @Override public void close() { action.run(); }
    }
}
