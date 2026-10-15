package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.VirtualServerId;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SessionCommandsCompatibilityIT {
    @ParameterizedTest
    @EnumSource(ServerQueryCompatibilityIT.Target.class)
    @Timeout(120)
    void logoutAndRelogin(ServerQueryCompatibilityIT.Target target) throws Exception {
        try (var server = new TeamSpeakContainer()) {
            server.start();
            try (var query = new TS3Query(server.config(target.protocol))) {
                query.connect();
                var api = query.getApi();
                api.selectVirtualServerById(VirtualServerId.of(1));
                for (String name : List.of("login", "logout", "whoami", "bindinglist"))
                    assertTrue(api.executeRawCommand("help " + name)
                            .getRawResponse()
                            .startsWith("Usage: " + name));
                var bindings = api.instance().bindings();
                assertFalse(bindings.isEmpty());
                assertTrue(bindings.stream().allMatch(binding -> !binding.ip().isBlank()));
                api.logout();
                var loggedOut = api.whoAmI();
                assertEquals(Optional.empty(), loggedOut.getVirtualServerId());
                assertTrue(loggedOut.getLoginName().isEmpty());
                api.login("serveradmin", TeamSpeakContainer.PASSWORD);
                assertEquals(java.util.Optional.of("serveradmin"), api.whoAmI().getLoginName());
                api.selectVirtualServerById(VirtualServerId.of(1));
                assertEquals(Optional.of(VirtualServerId.of(1)), api.whoAmI().getVirtualServerId());
                assertEquals(
                        TeamSpeakContainer.TS3_VERSION, api.instance().version().version());
                query.exit();
            }
            var report = Path.of("build", "compatibility", target + "-session-commands.txt");
            Files.createDirectories(report.getParent());
            Files.writeString(
                    report,
                    "Binding list returned nonblank IP rows; explicit logout cleared selected server/login; same-stream login restored serveradmin; reselected server1 and framing healthy\n");
        }
    }
}
