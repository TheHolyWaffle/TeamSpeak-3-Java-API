package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.VirtualServerId;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LogLimitsCompatibilityIT {
    @ParameterizedTest
    @EnumSource(ServerQueryCompatibilityIT.Target.class)
    @Timeout(90)
    void rejectsRequestsAboveOneHundredLines(ServerQueryCompatibilityIT.Target target) throws Exception {
        var evidence = new ArrayList<String>();
        try (var server = new TeamSpeakContainer()) {
            server.start();
            try (var query = new TS3Query(server.config(target.protocol))) {
                query.connect();
                evidence.add("version=" + query.getApi().instance().version());
                query.getApi().selectVirtualServerById(VirtualServerId.of(1));
                verifyLogLimits(query.getApi(), evidence);
            }
        } finally {
            var path = Path.of("build", "compatibility", target + "-log-limits.txt");
            Files.createDirectories(path.getParent());
            Files.write(path, evidence);
        }
    }

    private static void verifyLogLimits(TS3Api api, List<String> evidence) {
        for (boolean instance : List.of(false, true)) {
            String scope = instance ? " instance=1" : "";
            for (int lines : List.of(0, 1, 99, 100)) {
                String command = "logview lines=" + lines + scope;
                var rows = api.executeRawCommand(command).getResponses();
                evidence.add(command + " -> success; rows=" + rows.size());
                assertTrue(rows.size() <= lines, command);
                if (lines > 0) assertFalse(rows.isEmpty(), command);
            }
            for (int lines : List.of(101, 499, 500, 501, 1000)) {
                String command = "logview lines=" + lines + scope;
                var error = assertThrows(TS3CommandFailedException.class, () -> api.executeRawCommand(command));
                evidence.add(command + " -> " + error.getError().getMap());
                assertEquals(1541, error.getErrorId(), command);
            }
            var facadeRows =
                    instance ? api.instance().logEntries(100) : api.servers().logEntries(100);
            assertFalse(facadeRows.isEmpty());
            assertTrue(facadeRows.size() <= 100);
        }
    }
}
