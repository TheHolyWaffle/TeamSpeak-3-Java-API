package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.PermissionGrant;
import com.github.theholywaffle.teamspeak3.api.PermissionTarget;
import com.github.theholywaffle.teamspeak3.api.ServerInstanceProperty;
import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ServerSnapshotCompatibilityIT {
    @ParameterizedTest(name = "{0}: snapshot roundtrip and instance edits")
    @EnumSource(ServerQueryCompatibilityIT.Target.class)
    @Timeout(90)
    void snapshotRoundtrip(ServerQueryCompatibilityIT.Target target) throws Exception {
        var evidence = new ArrayList<String>();
        try (var server = new TeamSpeakContainer()) {
            server.start();
            try (var query = new TS3Query(server.config(target.protocol))) {
                query.connect();
                var api = query.getApi();
                api.selectVirtualServerById(1);
                server.record(target.name(), api.instance().version());
                for (String command : List.of("serversnapshotcreate", "serversnapshotdeploy", "instanceedit")) {
                    var help = api.executeRawCommand("help " + command).getRawResponse();
                    assertTrue(help.startsWith("Usage: " + command));
                    evidence.add("help " + command + "\n" + help);
                }
                var property = ServerInstanceProperty.SERVERINSTANCE_MAX_DOWNLOAD_TOTAL_BANDWIDTH;
                String original = api.instance().info().getMap().get(property.getName());
                assertNotNull(original);
                try {
                    api.instance().edit(Map.of(property, "123456"));
                    assertEquals("123456", api.instance().info().getMap().get(property.getName()));
                } finally {
                    api.instance().edit(Map.of(property, original));
                }
                assertEquals(original, api.instance().info().getMap().get(property.getName()));
                String name = "snapshot 日本語 | literal \\s " + target;
                api.servers()
                        .edit(Map.of(
                                VirtualServerProperty.VIRTUALSERVER_NAME,
                                name,
                                VirtualServerProperty.VIRTUALSERVER_WELCOMEMESSAGE,
                                name,
                                VirtualServerProperty.VIRTUALSERVER_HOSTBANNER_URL,
                                "https://example.invalid/banner"));
                int channel = api.channels()
                        .create(
                                name,
                                Map.of(
                                        ChannelProperty.CHANNEL_FLAG_PERMANENT,
                                        "1",
                                        ChannelProperty.CHANNEL_DESCRIPTION,
                                        name));
                String groupName = "snapshot-" + target;
                int group = api.serverGroups().create(groupName);
                api.permissions()
                        .add(
                                PermissionTarget.serverGroup(group),
                                PermissionGrant.of("i_client_talk_power", 47).withSkip());
                var snapshot = api.servers().createSnapshot();
                assertFalse(snapshot.data().isBlank());
                evidence.add("snapshot contains configuration; contents redacted (identities and keys)");
                api.servers().stop(1);
                api.servers().delete(1);
                assertTrue(api.servers().list().isEmpty());
                api.servers().deploySnapshot(snapshot);
                var restored = api.servers().list();
                assertEquals(1, restored.size());
                api.selectVirtualServerById(restored.getFirst().getId());
                var info = api.servers().info();
                assertEquals(name, info.getName());
                assertEquals(name, info.getMap().get("virtualserver_welcomemessage"));
                assertEquals("https://example.invalid/banner", info.getMap().get("virtualserver_hostbanner_url"));
                var channels = api.channels().list().stream()
                        .filter(row -> row.getName().equals(name))
                        .toList();
                assertEquals(1, channels.size());
                assertEquals(
                        name, api.channels().info(channels.getFirst().getId()).getDescription());
                var groups = api.serverGroups().list().stream()
                        .filter(row -> row.getName().equals(groupName))
                        .toList();
                assertEquals(1, groups.size());
                CompatibilityAssertions.permission(
                        api.permissions()
                                .assigned(PermissionTarget.serverGroup(
                                        groups.getFirst().getId())),
                        "i_client_talk_power",
                        47,
                        false,
                        true);
                evidence.add(
                        "restored server/channel/group names, description, welcome, banner and named permission matched");
                evidence.add("source IDs=" + channel + "/" + group + "; restored IDs are discovered, never assumed");
                query.exit();
            }
        } finally {
            var path = Path.of("target", "compatibility", target + "-snapshots.txt");
            Files.createDirectories(path.getParent());
            Files.write(path, evidence);
        }
    }
}
