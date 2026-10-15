package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.LogLevel;
import com.github.theholywaffle.teamspeak3.api.VirtualServerId;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LogWriteCompatibilityIT {
    @ParameterizedTest(name = "{0}: custom logging routing, visibility and pagination")
    @EnumSource(ServerQueryCompatibilityIT.Target.class)
    @Timeout(90)
    void writes(ServerQueryCompatibilityIT.Target target) throws Exception {
        var evidence = new ArrayList<String>();
        try (var server = new TeamSpeakContainer()) {
            server.start();
            try (var query = new TS3Query(server.config(target.protocol))) {
                query.connect();
                var api = query.getApi();
                for (int sid : List.of(1, 0)) {
                    if (sid == 0) {
                        api.deselectVirtualServer();
                        assertEquals(Optional.empty(), api.whoAmI().getVirtualServerId());
                    } else {
                        api.selectVirtualServerById(VirtualServerId.of(sid));
                    }
                    var markers = new ArrayList<String>();
                    for (var level : LogLevel.values()) {
                        String marker = "issue441-log-" + sid + "-" + level.getIndex() + " 日本語 | literal \\s";
                        markers.add(marker);
                        if (sid == 0) api.servers().addLogEntry(level, marker);
                        else
                            query.getAsyncApi()
                                    .servers()
                                    .addLogEntry(level, marker)
                                    .get(5, TimeUnit.SECONDS);
                    }
                    String scope = sid == 0 ? " instance=1" : "";
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                    List<String> forward;
                    do {
                        forward = lines(api.executeRawCommand("logview lines=100 reverse=0 begin_pos=0" + scope));
                        var observed = forward;
                        if (markers.stream()
                                .allMatch(marker -> observed.stream().anyMatch(line -> line.endsWith(marker)))) break;
                        assertTrue(
                                System.nanoTime() < deadline,
                                "All custom severities must become query-visible within five seconds");
                        TimeUnit.MILLISECONDS.sleep(25);
                    } while (true);
                    var written = forward.stream()
                            .filter(line -> line.contains("issue441-log-" + sid + "-"))
                            .toList();
                    assertEquals(4, written.size());
                    for (int i = 0; i < markers.size(); i++)
                        assertTrue(written.get(i).endsWith(markers.get(i)));
                    var reverse =
                            lines(api.executeRawCommand("logview lines=100 reverse=1 begin_pos=0" + scope)).stream()
                                    .filter(line -> line.contains("issue441-log-" + sid + "-"))
                                    .toList();
                    assertEquals(written.reversed(), reverse);
                    evidence.add("sid=" + sid
                            + " all four severities visible; exact Unicode/escape suffixes; reverse order matched");
                    long position = 0;
                    var seen = new java.util.HashSet<String>();
                    for (int page = 0; page < 30; page++) {
                        var result = api.executeRawCommand("logview lines=1 reverse=1 begin_pos=" + position + scope);
                        var row = result.getFirstResponse().orElseThrow();
                        long next = row.requireLong("last_pos");
                        assertTrue(row.requireLong("file_size") > 0);
                        assertTrue(next >= 0);
                        for (String line : lines(result))
                            for (String marker : markers) if (line.endsWith(marker)) seen.add(marker);
                        if (next == 0) break;
                        assertTrue(
                                position == 0 || next < position,
                                "Log cursor must progress toward the start of the file");
                        position = next;
                    }
                    assertEquals(new java.util.HashSet<>(markers), seen);
                    evidence.add("one-line byte-position continuation recovered all custom entries");
                }
                query.exit();
            }
        } finally {
            var path = Path.of("build", "compatibility", target + "-log-writes.txt");
            Files.createDirectories(path.getParent());
            Files.write(path, evidence);
        }
    }

    private static List<String> lines(DefaultArrayResponse response) {
        return response.getResponses().stream().map(row -> row.require("l")).toList();
    }
}
