package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.VirtualServerStatus;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class ServerModesCompatibilityIT {
    @ParameterizedTest @EnumSource(ServerQueryCompatibilityIT.Target.class) @Timeout(90)
    void restartAndDefaultChannels(ServerQueryCompatibilityIT.Target target) throws Exception {
        var evidence = new ArrayList<String>();
        try (var server = new TeamSpeakContainer()) {
            server.start();
            try (var query = new TS3Query(server.config(target.protocol))) {
                query.connect(); var api = query.getApi(); api.selectVirtualServerById(1);
                int originalDefault = api.getChannels().stream().filter(row -> row.isDefault()).findFirst().orElseThrow().getId();
                int permanent = api.createChannel("restart-permanent", Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
                int semi = api.createChannel("restart-semi", Map.of(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT, "1"));
                api.editChannel(permanent, Map.of(ChannelProperty.CHANNEL_FLAG_DEFAULT, "1"));
                assertTrue(api.getChannelInfo(permanent).isDefault()); assertFalse(api.getChannelInfo(originalDefault).isDefault());
                api.stopServer(1); assertEquals(VirtualServerStatus.OFFLINE, api.getVirtualServers().getFirst().getStatus());
                api.startServer(1); api.selectVirtualServerByPort(api.getVirtualServers().getFirst().getPort(), "restart-query");
                var restartedQuery = api.whoAmI();
                assertEquals(1, restartedQuery.getVirtualServerId()); assertEquals("restart-query", restartedQuery.getNickname());
                var restartedChannel = api.getChannelInfo(permanent);
                assertTrue(restartedChannel.isPermanent()); assertTrue(restartedChannel.isDefault());
                assertTrue(api.getChannels().stream().noneMatch(row -> row.getId() == semi));
                api.editChannel(originalDefault, Map.of(ChannelProperty.CHANNEL_FLAG_DEFAULT, "1"));
                assertTrue(api.getChannelInfo(originalDefault).isDefault()); api.deleteChannel(permanent, true);
                evidence.add("permanent/default channel survived restart; semi-permanent channel removed; default restored");
                var limit = assertThrows(TS3CommandFailedException.class, () -> api.createServer("second-running-server", Map.of()));
                assertEquals(2816, limit.getError().getId());
                evidence.add("second running server license boundary=" + limit.getError().getId() + " " + limit.getError().getMessage());
                query.exit();
            }
        } finally {
            var path = Path.of("target", "compatibility", target + "-server-modes.txt");
            Files.createDirectories(path.getParent()); Files.write(path, evidence);
        }
    }

    @ParameterizedTest @EnumSource(ServerQueryCompatibilityIT.Target.class) @Timeout(90)
    void processStop(ServerQueryCompatibilityIT.Target target) throws Exception {
        try (var server = new TeamSpeakContainer()) {
            server.start();
            try (var query = new TS3Query(server.config(target.protocol))) {
                query.connect();
                query.getApi().stopServerProcess("owned compatibility fixture stop 日本語");
                long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
                while (server.isRunning() || query.isConnected()) {
                    assertTrue(System.nanoTime() < deadline, "Process stop must terminate the owned server and connection");
                    java.util.concurrent.TimeUnit.MILLISECONDS.sleep(25);
                }
                var path = Path.of("target", "compatibility", target + "-process-stop.txt");
                Files.createDirectories(path.getParent()); Files.writeString(path, "typed process stop acknowledged; owned process and query terminated\n");
            }
        }
    }
}
