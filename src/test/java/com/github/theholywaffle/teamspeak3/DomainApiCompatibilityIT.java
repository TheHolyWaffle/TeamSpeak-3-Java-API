package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.BanRequest;
import com.github.theholywaffle.teamspeak3.api.ChannelGroupFilter;
import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.FileLocation;
import com.github.theholywaffle.teamspeak3.api.MessageId;
import com.github.theholywaffle.teamspeak3.api.PermissionTarget;
import com.github.theholywaffle.teamspeak3.api.ServerGroupId;
import com.github.theholywaffle.teamspeak3.api.VirtualServerId;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3QueryShutDownException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3UnsupportedOperationException;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.reconnect.SessionConfiguration;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Live TS3 evidence for the #443 domain API: common workflow, absent results and error categories. */
class DomainApiCompatibilityIT {
    private static final String UNKNOWN_UID = "AAAAAAAAAAAAAAAAAAAAAAAAAAA=";
    private static final byte[] RULES = "Be nice | literal \\s 日本語".getBytes(StandardCharsets.UTF_8);

    @RegisterExtension
    static final TeamSpeakContainer ts3 = new TeamSpeakContainer().withFileTransferPort();

    @ParameterizedTest(name = "{0}: documented common workflow, sync and async")
    @EnumSource(ServerQueryCompatibilityIT.Target.class)
    @Timeout(120)
    void commonWorkflow(ServerQueryCompatibilityIT.Target target) throws Exception {
        var evidence = new ArrayList<String>();
        var messages = new LinkedBlockingQueue<TextMessageEvent>();
        var files = ts3.config(target.protocol)
                .setFileTransferConfig(new FileTransferConfig(
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(15),
                        1024,
                        (_, _, port) -> new InetSocketAddress(ts3.getHost(), ts3.getMappedPort(port))));
        ChannelId lobby = null;
        try (var observer = new TS3Query(ts3.config(target.protocol));
                var actor = new TS3Query(files)) {
            observer.connect();
            observer.getApi().selectVirtualServerById(VirtualServerId.of(1), "observer-" + target);
            observer.getApi().events().register(TS3EventType.TEXT_PRIVATE);
            observer.subscribe(new TS3EventAdapter() {
                @Override
                public void onTextMessage(TextMessageEvent e) {
                    messages.add(e);
                }
            });
            actor.connect();
            var api = actor.getApi();
            api.selectVirtualServerById(VirtualServerId.of(1));
            ts3.record(target + "-domain-api", api.instance().version());
            ClientId observerId = observer.getApi().whoAmI().getId();
            String sharedUid = observer.getApi().whoAmI().getUniqueIdentifier();
            assertEquals(
                    observerId,
                    api.clients().findByUniqueId(sharedUid).orElseThrow().getId(),
                    "Both serveradmin sessions share a UID; the earlier observer session is listed first");
            assertTrue(api.channels().findByName("Lobby", false).isEmpty());

            lobby = CommonWorkflow.prepareLobby(api, sharedUid, RULES);
            var welcome = messages.poll(10, TimeUnit.SECONDS);
            assertNotNull(welcome, "Online branch must message the observer");
            assertEquals("Welcome to the lobby", welcome.getMessage());
            assertEquals(lobby, observer.getApi().whoAmI().getChannelId().orElseThrow());
            evidence.add("sync: created Lobby " + lobby + ", moved and messaged observer " + observerId);

            assertEquals(Optional.empty(), api.clients().findByUniqueId("ServerQuery"));
            assertTrue(
                    api.databaseClients().findByUniqueId("ServerQuery").isPresent(),
                    "The guest identity exists offline");
            ChannelId reused = CommonWorkflow.prepareLobby(actor.getAsyncApi(), "ServerQuery", RULES)
                    .get(30, TimeUnit.SECONDS);
            assertEquals(lobby, reused, "Async form must find the existing channel");
            ChannelId created = lobby;
            evidence.add(
                    "async: reused Lobby " + reused + ", stored an offline message for the offline guest identity");

            CompatibilityAssertions.permission(
                    api.permissions().assigned(PermissionTarget.channel(created)),
                    "i_channel_needed_join_power",
                    10,
                    false,
                    false);
            assertArrayEquals(RULES, api.files().downloadBytes(FileLocation.of(created, "/rules.txt")));
            api.channels().delete(created, true);
            lobby = null;
            assertTrue(api.channels().findByName("Lobby", false).isEmpty());
            observer.exit();
            actor.exit();
        } finally {
            if (lobby != null)
                try (var cleanup = new TS3Query(ts3.config(target.protocol))) {
                    cleanup.connect();
                    cleanup.getApi().selectVirtualServerById(VirtualServerId.of(1));
                    cleanup.getApi().channels().delete(lobby, true);
                }
        }
        write(target, "workflow", evidence);
    }

    @ParameterizedTest(name = "{0}: absent lookups are empty, reads by ID fail")
    @EnumSource(ServerQueryCompatibilityIT.Target.class)
    @Timeout(120)
    void absentResultsAndFailures(ServerQueryCompatibilityIT.Target target) throws Exception {
        var evidence = new ArrayList<String>();
        try (var query = new TS3Query(ts3.config(target.protocol))) {
            query.connect();
            var api = query.getApi();
            api.selectVirtualServerById(VirtualServerId.of(1));
            String absent = "absent-" + target;
            assertEquals(Optional.empty(), api.clients().findByUniqueId(UNKNOWN_UID));
            assertEquals(Optional.empty(), api.databaseClients().findByUniqueId(UNKNOWN_UID));
            assertEquals(Optional.empty(), api.messages().getOffline(MessageId.of(999_999)));
            assertEquals(Optional.empty(), api.servers().findIdByPort(1));
            assertEquals(
                    Optional.of(VirtualServerId.of(1)),
                    api.servers().findIdByPort(api.servers().info().getPort()));
            assertEquals(Optional.empty(), api.channels().findByName(absent, true));
            assertEquals(Optional.empty(), api.clients().findByName(absent, true));
            assertEquals(List.of(), api.channels().search(absent));
            assertEquals(List.of(), api.clients().search(absent));
            assertEquals(List.of(), api.databaseClients().searchCustomProperty("absent-key", "%"));
            assertNotNull(api.channelGroups().members(ChannelGroupFilter.all().inChannel(ChannelId.of(1))));
            evidence.add(
                    "Optional.empty: unknown UID (online/database), message 999999, port 1, absent names; empty lists: channel/client search, custom search");

            failsWith(768, "channelinfo", () -> api.channels().info(ChannelId.of(9_999)), evidence);
            failsWith(512, "clientinfo", () -> api.clients().info(ClientId.of(9_999)), evidence);
            failsWith(1540, "clientinfo", () -> api.clients().info(ClientId.of(999_999)), evidence);
            failsWith(512, "clientdbinfo", () -> api.databaseClients().info(ClientDatabaseId.of(9_999)), evidence);
            failsWith(
                    2052,
                    "ftgetfileinfo",
                    () -> api.files().info(FileLocation.of(ChannelId.of(1), "/" + absent)),
                    evidence);
            failsWith(
                    2560, "servergroupclientlist", () -> api.serverGroups().members(ServerGroupId.of(9_999)), evidence);
            failsWith(1538, "banadd", () -> api.bans().add(BanRequest.myTeamSpeakId("not-a-valid-id")), evidence);
            failsWith(512, "messageadd", () -> api.messages().sendOffline(UNKNOWN_UID, "absent", "absent"), evidence);

            try (var ban = new ServerQueryCompatibilityIT.CreatedResource<>(
                    api.bans().add(BanRequest.myTeamSpeakId("empty").withReason("domain " + target)),
                    api.bans()::delete)) {
                assertTrue(api.bans().list().stream()
                        .anyMatch(row -> row.getId().equals(ban.id())
                                && row.getBannedMyTeamSpeakId().equals(java.util.Optional.of("empty"))));
            }
            evidence.add("myTeamSpeak-only ban rule mytsid=empty accepted and deleted");
            query.exit();
        }
        write(target, "absent-results", evidence);
    }

    @ParameterizedTest(name = "{0}: unsupported session change and closed-query failure")
    @EnumSource(ServerQueryCompatibilityIT.Target.class)
    @Timeout(120)
    void localFailureCategories(ServerQueryCompatibilityIT.Target target) throws Exception {
        var config = ts3.config(target.protocol)
                .setSessionConfiguration(SessionConfiguration.forServer(VirtualServerId.of(1)))
                .setReconnectStrategy(ReconnectStrategy.constantBackoff(1000));
        var query = new TS3Query(config);
        try {
            query.connect();
            var unsupported = assertThrows(
                    TS3UnsupportedOperationException.class, () -> query.getApi().setNickname("other"));
            assertFalse(unsupported.isOutcomeUnknown());
            assertEquals(
                    Optional.of(VirtualServerId.of(1)),
                    query.getApi().whoAmI().getVirtualServerId(),
                    "Rejected session change sends nothing");
            query.exit();
        } finally {
            query.close();
        }
        var closed = assertThrows(
                TS3QueryShutDownException.class, () -> query.getApi().whoAmI());
        assertFalse(closed.isOutcomeUnknown());
        write(
                target,
                "local-failures",
                List.of(
                        "sealed session change: TS3UnsupportedOperationException; closed query: TS3QueryShutDownException"));
    }

    private static void failsWith(int errorId, String command, Executable call, List<String> evidence) {
        var failure = assertThrows(TS3CommandFailedException.class, call);
        assertEquals(errorId, failure.getErrorId(), command);
        assertEquals(command, failure.getCommandName());
        assertFalse(failure.isOutcomeUnknown());
        evidence.add(command + " -> " + errorId + " " + failure.getError().getMessage());
    }

    private static void write(ServerQueryCompatibilityIT.Target target, String name, List<String> evidence)
            throws Exception {
        var report = Path.of("build", "compatibility", target + "-domain-" + name + ".txt");
        Files.createDirectories(report.getParent());
        Files.writeString(report, String.join("\n", evidence) + "\n");
    }
}
