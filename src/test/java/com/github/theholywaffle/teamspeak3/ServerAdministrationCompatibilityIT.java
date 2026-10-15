package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.ApiKeyScope;
import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.VirtualServerId;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@Timeout(90)
class ServerAdministrationCompatibilityIT {
    enum Target {
        RAW,
        SSH,
        HTTP,
        HTTPS
    }

    @RegisterExtension
    static final TeamSpeakContainer server = new TeamSpeakContainer().withWebQuery();

    static TS3Query observer;
    static String key;

    @BeforeAll
    static void connectObserver() {
        observer = new TS3Query(server.config(TS3Query.Protocol.RAW));
        observer.connect();
        key = observer.getApi()
                .apiKeys()
                .add(ApiKeyScope.MANAGE, Duration.ZERO)
                .getSecret()
                .orElseThrow();
        observer.getApi().selectVirtualServerById(VirtualServerId.of(1));
    }

    @AfterAll
    static void closeObserver() {
        if (observer != null) observer.close();
    }

    @ParameterizedTest
    @EnumSource(Target.class)
    void clientUidAndTemporaryPasswords(Target target) throws Exception {
        var evidence = new ArrayList<String>();
        evidence.add("version=" + observer.getApi().instance().version());
        try {
            if (target == Target.RAW || target == Target.SSH) {
                try (var query = new TS3Query(server.config(TS3Query.Protocol.valueOf(target.name())))) {
                    query.connect();
                    query.getApi().selectVirtualServerById(VirtualServerId.of(1));
                    verify(query.getApi(), query.getAsyncApi(), target, evidence);
                }
            } else {
                try (var query = new TS3WebQuery(
                        server.webQueryConfig(target == Target.HTTPS, key).build())) {
                    verify(
                            query.getApi(VirtualServerId.of(1)),
                            query.getAsyncApi(VirtualServerId.of(1)),
                            target,
                            evidence);
                }
            }
        } finally {
            var path = Path.of("build", "compatibility", target + "-server-administration.txt");
            Files.createDirectories(path.getParent());
            Files.write(path, evidence);
        }
    }

    private static void verify(TS3BaseApi api, TS3BaseApiAsync async, Target target, List<String> evidence) {
        var self = observer.getApi().whoAmI();
        var clientId = self.getId().orElseThrow();
        assertEquals(self.getUniqueIdentifier(), api.clients().uniqueId(clientId));
        assertEquals(
                self.getUniqueIdentifier(), CommandFutures.join(async.clients().uniqueId(clientId)));
        var unknown = assertThrows(
                TS3CommandFailedException.class, () -> api.clients().uniqueId(ClientId.of(65535)));
        assertEquals(512, unknown.getErrorId());
        evidence.add("client UID: sync/async match independent observer; unknown client -> " + unknown.getErrorId());

        var servers = api.servers();
        assertTrue(servers.temporaryPasswords().isEmpty());
        String password = "temporary " + target + " | 日本語 \\s";
        String description = "description | 日本語 \\s";
        Instant before = Instant.now().minusSeconds(1);
        servers.addTemporaryPassword(password, description, Duration.ofMinutes(10));
        try {
            var rows = CommandFutures.join(async.servers().temporaryPasswords());
            assertEquals(1, rows.size());
            var row = rows.getFirst();
            assertEquals(password, row.getPassword());
            assertEquals(description, row.getDescription());
            assertEquals(self.getUniqueIdentifier(), row.getCreatorUniqueId());
            assertFalse(row.getCreatorNickname().isBlank());
            assertEquals(Optional.empty(), row.getTargetChannelId());
            assertEquals(Optional.empty(), row.getTargetChannelPassword());
            assertFalse(row.getStartsAt().isBefore(before));
            assertFalse(row.getStartsAt().isAfter(Instant.now()));
            assertEquals(Duration.ofMinutes(10), Duration.between(row.getStartsAt(), row.getExpiresAt()));
            assertFalse(row.toString().contains(password));
            evidence.add(
                    "temporary password: exact Unicode/escapes; creator; epoch-second validity; default channel; redacted toString");
        } finally {
            CommandFutures.join(async.servers().deleteTemporaryPassword(password));
        }
        assertTrue(servers.temporaryPasswords().isEmpty());

        String channelPassword = "channel secret | 日本語 \\s";
        ChannelId channel = observer.getApi()
                .channels()
                .create(
                        "temporary-target-" + target,
                        Map.of(
                                ChannelProperty.CHANNEL_FLAG_PERMANENT,
                                "1",
                                ChannelProperty.CHANNEL_PASSWORD,
                                channelPassword));
        try {
            CommandFutures.join(async.servers()
                    .addTemporaryPassword(
                            password, "", Duration.ofMinutes(10), Optional.of(channel), Optional.of(channelPassword)));
            try {
                var row = servers.temporaryPasswords().getFirst();
                assertEquals(Optional.of(channel), row.getTargetChannelId());
                assertEquals(Optional.of(channelPassword), row.getTargetChannelPassword());
                assertEquals("", row.getDescription());
                assertFalse(row.toString().contains(password));
                assertFalse(row.toString().contains(channelPassword));
                evidence.add(
                        "temporary password: explicit protected channel and channel password persisted; both secrets redacted");
            } finally {
                servers.deleteTemporaryPassword(password);
            }
        } finally {
            observer.getApi().channels().delete(channel, true);
        }
        assertTrue(servers.temporaryPasswords().isEmpty());
        servers.deleteTemporaryPassword(password);
        assertTrue(servers.temporaryPasswords().isEmpty());
        evidence.add("delete unknown temporary password -> success; deletion is idempotent");
    }
}
