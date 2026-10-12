package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.*;
import com.github.theholywaffle.teamspeak3.api.event.*;
import com.github.theholywaffle.teamspeak3.api.exception.*;
import com.github.theholywaffle.teamspeak3.commands.CommandEncoding;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.containers.Network;
import static org.junit.jupiter.api.Assertions.*;

class RestrictedClientCompatibilityIT {
    @ParameterizedTest @EnumSource(ServerQueryCompatibilityIT.Target.class) @Timeout(120)
    void deniedOperationsAndCredentialRotation(ServerQueryCompatibilityIT.Target target) throws Exception {
        var evidence = new ArrayList<String>();
        try (var network = Network.newNetwork();
             var server = new TeamSpeakContainer().withNetwork(network).withNetworkAliases("server");
             var fixture = new RegularClientContainer(network)) {
            server.start(); fixture.start(); fixture.connect();
            try (var admin = new TS3Query(server.config(target.protocol))) {
                admin.connect(); var api = admin.getApi(); api.selectVirtualServerById(1);
                for (String command : List.of("clientsetserverquerylogin", "queryloginlist", "serveredit", "logview")) {
                    String help = api.executeRawCommand("help " + command).getRawResponse();
                    assertTrue(help.startsWith("Usage: " + command)); evidence.add("help " + command + "\n" + help);
                }
                var client = RegularClientCompatibilityIT.regular(api); int dbid = client.getDatabaseId();
                api.addClientPermission(dbid, BPermissionType.B_SERVERQUERY_LOGIN, true, false);
                api.addClientPermission(dbid, BPermissionType.B_CLIENT_CREATE_MODIFY_SERVERQUERY_LOGIN, true, false);
                String original = "restricted-" + target; String replacement = "rotated-" + target;
                var login = api.addServerQueryLogin(original, dbid);
                try (var cleanup = new Cleanup(() -> api.deleteServerQueryLogin(dbid))) {
                    String password;
                    try (var actor = new TS3Query(server.config(target.protocol).setLoginCredentials(original, login.getQueryClientLoginPassword()))) {
                        actor.connect(); var restricted = actor.getApi(); restricted.selectVirtualServerById(1);
                        assertEquals(dbid, restricted.whoAmI().getDatabaseId());
                        String name = api.getServerInfo().getName();
                        denied(() -> restricted.editServer(Map.of(VirtualServerProperty.VIRTUALSERVER_NAME, "forbidden")), api,
                            BPermissionType.B_VIRTUALSERVER_MODIFY_NAME.getName(), evidence);
                        assertEquals(name, api.getServerInfo().getName());
                        denied(() -> restricted.getVirtualServerLogEntries(), api,
                            BPermissionType.B_VIRTUALSERVER_LOG_VIEW.getName(), evidence);
                        password = restricted.updateServerQueryLogin(replacement);
                        assertFalse(password.isBlank());
                        assertFalse(restricted.getVersion().getVersion().isBlank());
                        evidence.add("credential replacement acknowledged; existing authenticated session remains usable");
                        actor.exit();
                    }
                    assertTrue(api.getServerQueryLoginsByName(original).isEmpty());
                    assertEquals(1, api.getServerQueryLoginsByName(replacement).size());
                    assertTrue(api.getServerQueryLoginsByName("rotated-%").stream().anyMatch(row -> row.getClientDatabaseId() == dbid));
                    assertTrue(api.getServerQueryLoginsByName("does-not-exist-%").isEmpty());
                    var page = api.executeRawCommand("queryloginlist pattern=rotated-% start=0 duration=1");
                    assertEquals(1, page.getResponses().size()); assertEquals(dbid, page.getFirstResponse().getInt("cldbid"));
                    assertTrue(api.executeRawCommand("queryloginlist pattern=rotated-% start=1 duration=1").getResponses().isEmpty());

                    try (var rejected = new TS3Query(server.config(target.protocol).setLoginCredentials(original, login.getQueryClientLoginPassword()))) {
                        var failure = assertThrows(TS3ConnectionFailedException.class, rejected::connect);
                        ServerQueryCompatibilityIT.assertLoginAuthenticationFailure(failure, target, evidence);
                    }
                    try (var rejectedPassword = new TS3Query(server.config(target.protocol).setLoginCredentials(replacement, login.getQueryClientLoginPassword()))) {
                        var failure = assertThrows(TS3ConnectionFailedException.class, rejectedPassword::connect);
                        ServerQueryCompatibilityIT.assertLoginAuthenticationFailure(failure, target, evidence);
                    }
                    try (var replacementQuery = new TS3Query(server.config(target.protocol).setLoginCredentials(replacement, password))) {
                        replacementQuery.connect(); var rotated = replacementQuery.getApi(); rotated.selectVirtualServerById(1);
                        assertEquals(dbid, rotated.whoAmI().getDatabaseId());
                        assertEquals(replacement, rotated.whoAmI().getLoginName()); replacementQuery.exit();
                    }
                    evidence.add("old credentials rejected; new credentials authenticated; exact/wildcard/absent login matching verified");
                }
                admin.exit();
            }
        } finally { report(target, "restricted-credentials", evidence); }
    }

    @ParameterizedTest @EnumSource(ServerQueryCompatibilityIT.Target.class) @Timeout(120)
    void channelKeyCustomMetadataAndTemporaryLifetime(ServerQueryCompatibilityIT.Target target) throws Exception {
        var evidence = new ArrayList<String>();
        try (var network = Network.newNetwork();
             var server = new TeamSpeakContainer().withNetwork(network).withNetworkAliases("server");
             var fixture = new RegularClientContainer(network)) {
            server.start(); fixture.start(); fixture.connect();
            try (var observer = new TS3Query(server.config(target.protocol));
                 var actor = new TS3Query(server.config(target.protocol))) {
                observer.connect(); actor.connect(); var api = observer.getApi(); var actions = actor.getApi();
                api.selectVirtualServerById(1); actions.selectVirtualServerById(1);
                var client = RegularClientCompatibilityIT.regular(api);
                var keys = new EventProbe<PrivilegeKeyUsedEvent>(8);
                var deleted = new EventProbe<ChannelDeletedEvent>(8);
                api.addTS3Listeners(new TS3EventAdapter() {
                    @Override public void onPrivilegeKeyUsed(PrivilegeKeyUsedEvent event) { keys.publish(event); }
                    @Override public void onChannelDeleted(ChannelDeletedEvent event) { deleted.publish(event); }
                });
                api.registerEvent(TS3EventType.PRIVILEGE_KEY_USED); api.registerEvent(TS3EventType.SERVER);
                api.registerEvent(TS3EventType.CHANNEL, 0);
                int source = actions.getChannelGroups().stream().filter(group -> group.getType() == PermissionGroupDatabaseType.REGULAR && group.getName().equals("Channel Admin")).findFirst().orElseThrow().getId();
                int group = actions.copyChannelGroup(source, "owned-channel-key", PermissionGroupDatabaseType.REGULAR);
                int channel = actions.createChannel("owned-key-channel", Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
                try (var groupCleanup = new Cleanup(() -> actions.deleteChannelGroup(group, true));
                     var channelCleanup = new Cleanup(() -> actions.deleteChannel(channel, true))) {
                    String value = "key 日本語 | literal \\s"; String property = "issue441_key";
                    String custom = "ident=" + property + " value=" + CommandEncoding.encode(value);
                    var created = actions.executeRawCommand("privilegekeyadd tokentype=1 tokenid1=" + group + " tokenid2=" + channel + " tokencustomset=" + CommandEncoding.encode(custom));
                    String token = created.getFirstResponse().get("token"); assertFalse(token.isBlank());
                    try (var keyCleanup = new Cleanup(() -> {
                        if (actions.getPrivilegeKeys().stream().anyMatch(key -> key.getToken().equals(token))) actions.deletePrivilegeKey(token);
                    })) {
                        fixture.command("tokenuse token=" + CommandEncoding.encode(token)); var used = keys.await();
                        assertEquals(PrivilegeKeyType.CHANNEL_GROUP, used.getPrivilegeKeyType());
                        assertEquals(group, used.getPrivilegeKeyGroupId()); assertEquals(channel, used.getPrivilegeKeyChannelId());
                        assertEquals(client.getDatabaseId(), used.getClientDatabaseId());
                        assertTrue(token.equals(used.getPrivilegeKey()), "Event identifies consumed key without exposing it");
                        assertTrue(actions.getPrivilegeKeys().stream().noneMatch(key -> key.getToken().equals(token)));
                        assertEquals(value, actions.getCustomClientProperties(client.getDatabaseId()).get(property));
                        assertTrue(actions.getChannelGroupClients(channel, client.getDatabaseId(), group).stream().anyMatch(row -> row.getChannelGroupId() == group));
                        var reused = assertThrows(RegularClientContainer.CommandFailure.class, () -> fixture.command("tokenuse token=" + CommandEncoding.encode(token)));
                        assertEquals(3840, reused.errorId());
                        evidence.add("channel key consumed once; custom property decoded exactly; event fields=" + new java.util.TreeSet<>(used.getMap().keySet()));
                    }
                }
                // Consume the expected permanent-channel deletion before testing automatic deletion.
                assertEquals(channel, deleted.await().getChannelId());
                int temporary = actions.createChannel("owned-temporary", Map.of());
                actions.moveClient(client.getId(), temporary);
                assertEquals(temporary, actions.getClientInfo(client.getId()).getChannelId());
                assertFalse(actions.getChannelInfo(temporary).isPermanent()); assertFalse(actions.getChannelInfo(temporary).isSemiPermanent());
                actions.moveQuery(client.getChannelId());
                assertEquals(1, actions.getClients().stream().filter(row -> row.getChannelId() == temporary).count(), "Only the regular fixture remains in the temporary channel");
                actions.moveClient(client.getId(), client.getChannelId());
                assertEquals(temporary, deleted.await().getChannelId());
                var missing = assertThrows(TS3CommandFailedException.class, () -> actions.getChannelInfo(temporary)); assertEquals(768, missing.getError().getId());
                evidence.add("temporary channel deleted after last regular client leaves; independent observer receives deletion; subsequent info error768");
                assertFalse(actions.getVersion().getVersion().isBlank()); observer.exit(); actor.exit();
            }
        } finally { report(target, "channel-key-temporary", evidence); }
    }

    private static void denied(Runnable action, TS3Api administrator, String name, List<String> evidence) {
        var failure = assertThrows(TS3CommandFailedException.class, action::run);
        assertEquals(2568, failure.getError().getId());
        assertEquals(administrator.getPermissionIdByName(name), failure.getError().getFailedPermissionId());
        evidence.add("denied error2568 failed permission resolved by name=" + name);
    }
    private static void report(ServerQueryCompatibilityIT.Target target, String domain, List<String> evidence) throws Exception {
        var path = Path.of("target", "compatibility", target + "-" + domain + ".txt");
        Files.createDirectories(path.getParent()); Files.write(path, evidence);
    }
    private record Cleanup(Runnable action) implements AutoCloseable { @Override public void close() { action.run(); } }
}
