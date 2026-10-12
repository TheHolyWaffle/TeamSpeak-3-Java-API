package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
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
import static org.junit.jupiter.api.Assertions.*;

class ServerSnapshotCompatibilityIT {
    @ParameterizedTest(name = "{0}: snapshot roundtrip and instance edits")
    @EnumSource(ServerQueryCompatibilityIT.Target.class)
    @Timeout(90)
    void snapshotRoundtrip(ServerQueryCompatibilityIT.Target target) throws Exception {
        var evidence = new ArrayList<String>();
        try (var server = new TeamSpeakContainer(target.ts6)) {
            server.start();
            try (var query = new TS3Query(server.config(target.protocol))) {
                query.connect(); var api = query.getApi(); api.selectVirtualServerById(1);
                server.record(target.name(), api.getVersion());
                for (String command : List.of("serversnapshotcreate", "serversnapshotdeploy", "instanceedit")) {
                    var help = api.executeRawCommand("help " + command).getRawResponse();
                    assertTrue(help.startsWith("Usage: " + command)); evidence.add("help " + command + "\n" + help);
                }
                var property = ServerInstanceProperty.SERVERINSTANCE_MAX_DOWNLOAD_TOTAL_BANDWIDTH;
                String original = api.getInstanceInfo().getMap().get(property.getName());
                assertNotNull(original);
                try {
                    api.editInstance(property, "123456");
                    assertEquals("123456", api.getInstanceInfo().getMap().get(property.getName()));
                } finally { api.editInstance(property, original); }
                assertEquals(original, api.getInstanceInfo().getMap().get(property.getName()));
                String name = "snapshot 日本語 | literal \\s " + target;
                api.editServer(Map.of(VirtualServerProperty.VIRTUALSERVER_NAME, name,
                    VirtualServerProperty.VIRTUALSERVER_WELCOMEMESSAGE, name,
                    VirtualServerProperty.VIRTUALSERVER_HOSTBANNER_URL, "https://example.invalid/banner"));
                int channel = api.createChannel(name, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1",
                    ChannelProperty.CHANNEL_DESCRIPTION, name));
                String groupName = "snapshot-" + target;
                int group = api.addServerGroup(groupName);
                api.addServerGroupPermission(group, "i_client_talk_power", 47, false, true);
                var snapshot = api.createServerSnapshot(); assertFalse(snapshot.get().isBlank());
                evidence.add("snapshot contains configuration; contents redacted (identities and keys)");
                api.stopServer(1); api.deleteServer(1); assertTrue(api.getVirtualServers().isEmpty());
                api.deployServerSnapshot(snapshot);
                var restored = api.getVirtualServers(); assertEquals(1, restored.size());
                api.selectVirtualServerById(restored.getFirst().getId());
                var info = api.getServerInfo(); assertEquals(name, info.getName());
                assertEquals(name, info.getMap().get("virtualserver_welcomemessage"));
                assertEquals("https://example.invalid/banner", info.getMap().get("virtualserver_hostbanner_url"));
                var channels = api.getChannels().stream().filter(row -> row.getName().equals(name)).toList();
                assertEquals(1, channels.size());
                assertEquals(name, api.getChannelInfo(channels.getFirst().getId()).getDescription());
                var groups = api.getServerGroups().stream().filter(row -> row.getName().equals(groupName)).toList();
                assertEquals(1, groups.size());
                CompatibilityAssertions.permission(api.getServerGroupPermissions(groups.getFirst().getId()),
                    "i_client_talk_power", 47, false, true);
                evidence.add("restored server/channel/group names, description, welcome, banner and named permission matched");
                evidence.add("source IDs=" + channel + "/" + group + "; restored IDs are discovered, never assumed");
                query.exit();
            }
        } finally {
            var path = Path.of("target", "compatibility", target + "-snapshots.txt");
            Files.createDirectories(path.getParent()); Files.write(path, evidence);
        }
    }
}
