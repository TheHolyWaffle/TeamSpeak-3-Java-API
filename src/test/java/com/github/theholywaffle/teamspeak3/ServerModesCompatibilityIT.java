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
                int originalDefault = api.channels().list().stream().filter(row -> row.isDefault()).findFirst().orElseThrow().getId();
                int permanent = api.channels().create("restart-permanent", Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
                int semi = api.channels().create("restart-semi", Map.of(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT, "1"));
                api.channels().edit(permanent, Map.of(ChannelProperty.CHANNEL_FLAG_DEFAULT, "1"));
                assertTrue(api.channels().info(permanent).isDefault()); assertFalse(api.channels().info(originalDefault).isDefault());
                api.servers().stop(1); assertEquals(VirtualServerStatus.OFFLINE, api.servers().list().getFirst().getStatus());
                api.servers().start(1); api.selectVirtualServerByPort(api.servers().list().getFirst().getPort(), "restart-query");
                var restartedQuery = api.whoAmI();
                assertEquals(1, restartedQuery.getVirtualServerId()); assertEquals("restart-query", restartedQuery.getNickname());
                var restartedChannel = api.channels().info(permanent);
                assertTrue(restartedChannel.isPermanent()); assertTrue(restartedChannel.isDefault());
                assertTrue(api.channels().list().stream().noneMatch(row -> row.getId() == semi));
                api.channels().edit(originalDefault, Map.of(ChannelProperty.CHANNEL_FLAG_DEFAULT, "1"));
                assertTrue(api.channels().info(originalDefault).isDefault()); api.channels().delete(permanent, true);
                evidence.add("permanent/default channel survived restart; semi-permanent channel removed; default restored");
                var limit = assertThrows(TS3CommandFailedException.class, () -> api.servers().create("second-running-server", Map.of()));
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
                query.getApi().instance().stopProcess("owned compatibility fixture stop 日本語");
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
