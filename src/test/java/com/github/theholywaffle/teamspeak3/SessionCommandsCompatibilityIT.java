package com.github.theholywaffle.teamspeak3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class SessionCommandsCompatibilityIT {
    @ParameterizedTest @EnumSource(ServerQueryCompatibilityIT.Target.class) @Timeout(120)
    void logoutAndRelogin(ServerQueryCompatibilityIT.Target target) throws Exception {
        try (var server = new TeamSpeakContainer()) {
            server.start();
            try (var query = new TS3Query(server.config(target.protocol))) {
                query.connect(); var api = query.getApi(); api.selectVirtualServerById(1);
                for (String name : List.of("login", "logout", "whoami", "bindinglist")) assertTrue(api.executeRawCommand("help " + name).getRawResponse().startsWith("Usage: " + name));
                var bindings = api.instance().bindings(); assertFalse(bindings.isEmpty());
                assertTrue(bindings.stream().allMatch(binding -> !binding.getIp().isBlank()));
                api.logout(); var loggedOut = api.whoAmI();
                assertEquals(0, loggedOut.getVirtualServerId());
                assertTrue(loggedOut.getLoginName().isEmpty());
                api.login("serveradmin", TeamSpeakContainer.PASSWORD);
                assertEquals("serveradmin", api.whoAmI().getLoginName());
                api.selectVirtualServerById(1); assertEquals(1, api.whoAmI().getVirtualServerId());
                assertEquals("3.13.8", api.instance().version().getVersion()); query.exit();
            }
            var report = Path.of("target", "compatibility", target + "-session-commands.txt");
            Files.createDirectories(report.getParent());
            Files.writeString(report, "Binding list returned nonblank IP rows; explicit logout cleared selected server/login; same-stream login restored serveradmin; reselected server1 and framing healthy\n");
        }
    }
}
