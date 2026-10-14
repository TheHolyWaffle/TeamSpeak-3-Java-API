package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.ApiKeyScope;
import com.github.theholywaffle.teamspeak3.api.BanRequest;
import com.github.theholywaffle.teamspeak3.api.ChannelGroupFilter;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.ClientBan;
import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.PermissionGrant;
import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import com.github.theholywaffle.teamspeak3.api.PermissionTarget;
import com.github.theholywaffle.teamspeak3.api.ServerGroupType;
import com.github.theholywaffle.teamspeak3.api.ServerInstanceProperty;
import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;
import com.github.theholywaffle.teamspeak3.api.VirtualServerStatus;
import com.github.theholywaffle.teamspeak3.api.event.ChannelCreateEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import com.github.theholywaffle.teamspeak3.api.exception.TS3AuthenticationFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.ApiKey;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelGroupClient;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import com.github.theholywaffle.teamspeak3.api.wrapper.CustomPropertyAssignment;
import com.github.theholywaffle.teamspeak3.api.wrapper.Permission;
import com.github.theholywaffle.teamspeak3.api.wrapper.PrivilegeKey;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerGroupClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * TS3 Web Query over HTTP and HTTPS against the pinned server image. A RAW ServerQuery session provisions
 * API keys and independently observes the effects of Web Query requests.
 */
@Timeout(120)
class WebQueryCompatibilityIT {

    enum Target {
        HTTP(false),
        HTTPS(true);
        final boolean https;

        Target(boolean https) {
            this.https = https;
        }
    }

    @RegisterExtension
    static final TeamSpeakContainer ts3 = new TeamSpeakContainer().withWebQuery();

    /** Unlimited instance-wide MANAGE key, as created by an administrator. */
    static ApiKey manageKey;

    static TS3Query admin;

    @BeforeAll
    static void provisionKey() {
        admin = new TS3Query(ts3.config(TS3Query.Protocol.RAW));
        admin.connect();
        manageKey = admin.getApi().apiKeys().add(ApiKeyScope.MANAGE, Duration.ZERO);
        assertEquals(0, manageKey.getVirtualServerId());
        assertTrue(manageKey.isUnlimited());
        admin.getApi().selectVirtualServerById(1);
    }

    @AfterAll
    static void closeAdmin() {
        if (admin != null) admin.close();
    }

    static TS3WebQuery web(Target target) {
        return web(target, manageKey.getSecret().orElseThrow());
    }

    static TS3WebQuery web(Target target, String key) {
        return new TS3WebQuery(ts3.webQueryConfig(target.https, key).build());
    }

    static void record(Target target, String domain, List<String> evidence) throws Exception {
        Path path = Path.of("target", "compatibility", "TS3_WEBQUERY_" + target + "-" + domain + ".txt");
        Files.createDirectories(path.getParent());
        Files.write(path, evidence);
    }

    @ParameterizedTest(name = "{0}: instance, virtual-server and port scopes")
    @EnumSource(Target.class)
    void scopes(Target target) throws Exception {
        var evidence = new ArrayList<String>();
        try (var query = web(target)) {
            var version = query.getApi().instance().version();
            assertEquals(TeamSpeakContainer.TS3_VERSION, version.version());
            ts3.record("TS3_WEBQUERY_" + target, version);
            evidence.add("version " + version);
            var server = query.getApi(1);
            var self = server.whoAmI();
            evidence.add("whoami " + new TreeMap<>(self.getMap()));
            assertEquals(1, self.getVirtualServerId());
            assertEquals(1, query.getApiByPort(9987).whoAmI().getVirtualServerId());
            assertEquals(0, query.getApi().whoAmI().getVirtualServerId());
            assertEquals(Optional.of(1), query.getApi().servers().findIdByPort(9987));
            assertEquals(1, query.getApi().servers().list().size());
            var missing = assertThrows(
                    TS3CommandFailedException.class, () -> query.getApi(99).whoAmI());
            evidence.add("missing server " + new TreeMap<>(missing.getError().getMap()));
            assertTrue(missing.getErrorId() > 0);
            assertEquals(
                    version.version(),
                    CommandFutures.join(query.getAsyncApi(1).instance().version())
                            .version());
        } finally {
            record(target, "scopes", evidence);
        }
    }

    /** The library rejects these locally; this records that the server rejects them too. */
    @ParameterizedTest(name = "{0}: server rejects the commands the library blocks")
    @EnumSource(Target.class)
    void serverRejectsBlockedCommands(Target target) throws Exception {
        var evidence = new ArrayList<String>();
        var config = ts3.webQueryConfig(target.https, manageKey.getSecret().orElseThrow())
                .build();
        var client = HttpClient.newBuilder();
        config.getSslContext().ifPresent(client::sslContext);
        try (var http = client.build()) {
            for (String command : List.of(
                    "use",
                    "login",
                    "logout",
                    "quit",
                    "help",
                    "servernotifyregister",
                    "servernotifyunregister",
                    "ftgetfilelist",
                    "ftinitupload",
                    "ftinitdownload",
                    "ftcreatedir",
                    "ftdeletefile",
                    "ftrenamefile",
                    "ftgetfileinfo",
                    "ftlist",
                    "ftstop")) {
                assertTrue(WebQueryProtocol.unsupported(command).isPresent(), command);
                var response = http.send(
                        HttpRequest.newBuilder(config.getBaseUri().resolve("1/" + command))
                                .header("x-api-key", config.apiKey())
                                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
                evidence.add(command + " HTTP " + response.statusCode() + " " + response.body());
                assertEquals(401, response.statusCode(), command);
                assertTrue(response.body().contains("\"code\":5120"), command + ": " + response.body());
            }
        } finally {
            record(target, "blocked-commands", evidence);
        }
    }

    @ParameterizedTest(name = "{0}: API key authentication, scope and revocation")
    @EnumSource(Target.class)
    void authentication(Target target) throws Exception {
        var evidence = new ArrayList<String>();
        try (var anonymous = web(target, "not-a-valid-key")) {
            var invalid = assertThrows(
                    TS3AuthenticationFailedException.class,
                    () -> anonymous.getApi().instance().version());
            evidence.add("invalid " + new TreeMap<>(invalid.getError().getMap()));
            assertEquals(5122, invalid.getErrorId());
            assertFalse(invalid.isOutcomeUnknown());
        }
        try (var manager = web(target)) {
            ApiKey readKey = manager.getApi(1).apiKeys().add(ApiKeyScope.READ, Duration.ofDays(1));
            evidence.add("created " + readKey);
            assertEquals(1, readKey.getVirtualServerId());
            assertEquals(ApiKeyScope.READ, readKey.getScope());
            assertTrue(readKey.getTimeLeft().orElseThrow().compareTo(Duration.ofDays(1)) <= 0);
            var keys = manager.getApi(1).apiKeys();
            assertTrue(keys.list().stream().anyMatch(key -> key.getId() == readKey.getId()));
            assertTrue(keys.list(readKey.getOwner()).stream().anyMatch(key -> key.getId() == readKey.getId()));
            assertTrue(manager.getApi().apiKeys().listAll().stream().anyMatch(key -> key.getId() == manageKey.getId()));
            ClientDatabaseId databaseClient =
                    manager.getApi(1).databaseClients().list().getFirst().getDatabaseId();
            var owned = keys.add(ApiKeyScope.WRITE, Duration.ofDays(2), databaseClient);
            assertEquals(databaseClient, owned.getOwner());
            assertTrue(keys.list(databaseClient).stream().anyMatch(key -> key.getId() == owned.getId()));
            keys.delete(owned.getId());
            try (var reader = web(target, readKey.getSecret().orElseThrow())) {
                assertFalse(reader.getApi(1).channels().list().isEmpty());
                var denied = assertThrows(
                        TS3AuthenticationFailedException.class,
                        () -> reader.getApi(1).channels().create("read-only", Map.of()));
                evidence.add(
                        "write with read key " + new TreeMap<>(denied.getError().getMap()));
                assertEquals(5120, denied.getErrorId());
                manager.getApi(1).apiKeys().delete(readKey.getId());
                var revoked = assertThrows(
                        TS3AuthenticationFailedException.class,
                        () -> reader.getApi(1).channels().list());
                evidence.add("revoked " + new TreeMap<>(revoked.getError().getMap()));
                assertEquals(5122, revoked.getErrorId());
            }
            assertEquals(
                    5123,
                    assertThrows(
                                    TS3CommandFailedException.class,
                                    () -> manager.getApi(1).apiKeys().delete(readKey.getId()))
                            .getErrorId());
        } finally {
            record(target, "authentication", evidence);
        }
    }

    @Test
    void httpsRejectsAnUntrustedCertificateBeforeSendingTheKey() {
        var config = WebQueryConfig.builder(
                        URI.create("https://" + ts3.getHost() + ":" + ts3.getMappedPort(10443)),
                        manageKey.getSecret().orElseThrow())
                .build();
        try (var query = new TS3WebQuery(config)) {
            var error = assertThrows(
                    TS3ConnectionFailedException.class,
                    () -> query.getApi().instance().version());
            assertFalse(error.isOutcomeUnknown());
        }
    }

    @ParameterizedTest(name = "{0}: instance and virtual-server information, edits and logs")
    @EnumSource(Target.class)
    void serverAndInstance(Target target) throws Exception {
        var evidence = new ArrayList<String>();
        try (var query = web(target)) {
            var instance = query.getApi().instance();
            var info = instance.info();
            evidence.add("instanceinfo " + new TreeMap<>(info.getMap()));
            int floodCommands = info.getMaxFloodCommands();
            instance.edit(Map.of(
                    ServerInstanceProperty.SERVERINSTANCE_SERVERQUERY_FLOOD_COMMANDS,
                    String.valueOf(floodCommands + 1)));
            try {
                assertEquals(floodCommands + 1, instance.info().getMaxFloodCommands());
            } finally {
                instance.edit(Map.of(
                        ServerInstanceProperty.SERVERINSTANCE_SERVERQUERY_FLOOD_COMMANDS,
                        String.valueOf(floodCommands)));
            }
            assertTrue(instance.hostInfo().getUptime().compareTo(Duration.ZERO) >= 0);
            assertFalse(instance.bindings().isEmpty());
            var instanceLog = instance.logEntries(5);
            assertFalse(instanceLog.isEmpty());
            assertTrue(instanceLog.size() <= 5);

            var servers = query.getApi(1).servers();
            String name = servers.info().getName();
            String edited = "web " + target + " 日本語 | \\s";
            servers.edit(Map.of(VirtualServerProperty.VIRTUALSERVER_NAME, edited));
            try {
                assertEquals(edited, servers.info().getName());
                assertEquals(edited, admin.getApi().servers().info().getName(), "ServerQuery sees the edit");
            } finally {
                servers.edit(Map.of(VirtualServerProperty.VIRTUALSERVER_NAME, name));
            }
            assertNotNull(servers.connectionInfo().getMap().get("connection_packets_sent_total"));
            assertFalse(servers.logEntries(3).isEmpty());
            assertEquals(1, servers.list().size());
        } finally {
            record(target, "server-instance", evidence);
        }
    }

    @ParameterizedTest(name = "{0}: server and channel groups")
    @EnumSource(Target.class)
    void groups(Target target) throws Exception {
        var evidence = new ArrayList<String>();
        try (var query = web(target)) {
            var api = query.getApi(1);
            ClientDatabaseId member = api.databaseClients().list().getFirst().getDatabaseId();
            var serverGroups = api.serverGroups();
            int group = serverGroups.create("web sg " + target + " | é");
            int copy = serverGroups.copy(group, "web sg copy " + target, PermissionGroupDatabaseType.REGULAR);
            try {
                serverGroups.rename(group, "web sg renamed " + target);
                assertTrue(serverGroups.list().stream()
                        .anyMatch(g -> g.getId() == group && g.getName().equals("web sg renamed " + target)));
                serverGroups.addMember(group, member);
                assertEquals(
                        List.of(member),
                        serverGroups.members(group).stream()
                                .map(ServerGroupClient::clientDatabaseId)
                                .toList());
                assertTrue(serverGroups.ofClient(member).stream().anyMatch(g -> g.getId() == group));
                serverGroups.removeMember(group, member);
                assertEquals(List.of(), serverGroups.members(group));
                serverGroups.overwrite(group, copy, PermissionGroupDatabaseType.REGULAR);
            } finally {
                serverGroups.delete(copy, true);
                serverGroups.delete(group);
            }
            assertTrue(serverGroups.list().stream().noneMatch(g -> g.getId() == group));

            var channelGroups = api.channelGroups();
            int channelGroup = channelGroups.create("web cg " + target);
            int channelCopy =
                    channelGroups.copy(channelGroup, "web cg copy " + target, PermissionGroupDatabaseType.REGULAR);
            int defaultGroup = api.servers().info().getDefaultChannelGroup();
            try {
                channelGroups.rename(channelGroup, "web cg renamed " + target);
                assertTrue(channelGroups.list().stream()
                        .anyMatch(g -> g.getId() == channelGroup && g.getName().equals("web cg renamed " + target)));
                channelGroups.assign(channelGroup, 1, member);
                var members = channelGroups.members(ChannelGroupFilter.all());
                evidence.add("channel group members " + members);
                assertTrue(members.contains(new ChannelGroupClient(1, member, channelGroup)));
                channelGroups.assign(defaultGroup, 1, member);
                channelGroups.overwrite(channelGroup, channelCopy, PermissionGroupDatabaseType.REGULAR);
            } finally {
                channelGroups.delete(channelCopy, true);
                channelGroups.delete(channelGroup, true);
            }
            assertTrue(channelGroups.list().stream().noneMatch(g -> g.getId() == channelGroup));
        } finally {
            record(target, "groups", evidence);
        }
    }

    @ParameterizedTest(name = "{0}: permission metadata and assignments, including pipe-separated entries")
    @EnumSource(Target.class)
    void permissions(Target target) throws Exception {
        var evidence = new ArrayList<String>();
        try (var query = web(target)) {
            var api = query.getApi(1);
            var permissions = api.permissions();
            assertTrue(permissions.list().size() > 100);
            int talkPower = permissions.idByName("i_client_talk_power");
            assertEquals(
                    List.of(talkPower, permissions.idByName("i_icon_id")),
                    permissions.idsByName("i_client_talk_power", "i_icon_id"));
            evidence.add("own values " + permissions.ownValues("b_virtualserver_info_view", "i_client_talk_power"));
            assertEquals(1, permissions.ownValue("b_virtualserver_info_view"));
            assertFalse(permissions.assignments("b_virtualserver_info_view").isEmpty());
            ClientDatabaseId member = api.databaseClients().list().getFirst().getDatabaseId();
            int group = api.serverGroups().create("web perms " + target);
            int channel =
                    api.channels().create("web perms " + target, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
            try {
                for (PermissionTarget holder : List.of(
                        PermissionTarget.serverGroup(group),
                        PermissionTarget.channelGroup(api.servers().info().getDefaultChannelGroup()),
                        PermissionTarget.channel(channel),
                        PermissionTarget.client(member),
                        PermissionTarget.channelClient(channel, member))) {
                    permissions.add(holder, "i_client_talk_power", 42);
                    permissions.add(holder, PermissionGrant.of("i_icon_id", 7));
                    var assigned = permissions.assigned(holder);
                    evidence.add(holder + " " + assigned);
                    assertTrue(
                            assigned.contains(new Permission("i_client_talk_power", 42, false, false)),
                            holder.toString());
                    assertTrue(assigned.contains(new Permission("i_icon_id", 7, false, false)), holder.toString());
                    permissions.delete(holder, "i_client_talk_power");
                    permissions.delete(holder, "i_icon_id");
                    assertTrue(permissions.assigned(holder).stream()
                            .noneMatch(p -> p.name().equals("i_client_talk_power")));
                }
                assertFalse(permissions.overview(channel, member).isEmpty());
                permissions.addToAllServerGroups(ServerGroupType.SERVER_NORMAL, PermissionGrant.of("i_icon_id", 3));
                permissions.deleteFromAllServerGroups(ServerGroupType.SERVER_NORMAL, "i_icon_id");
            } finally {
                api.channels().delete(channel, true);
                api.serverGroups().delete(group, true);
            }
        } finally {
            record(target, "permissions", evidence);
        }
    }

    @ParameterizedTest(name = "{0}: bans, privilege keys and complaints")
    @EnumSource(Target.class)
    void bansKeysAndComplaints(Target target) throws Exception {
        var evidence = new ArrayList<String>();
        try (var query = web(target)) {
            var api = query.getApi(1);
            int ban = api.bans().add(BanRequest.name("^web-" + target + "$").withReason("web | \\s 日本語"));
            assertEquals(
                    Optional.of("web | \\s 日本語"),
                    api.bans().list().stream()
                            .filter(b -> b.getId() == ban)
                            .findFirst()
                            .orElseThrow()
                            .getReason());
            api.bans().delete(ban);
            assertEquals(
                    3328,
                    assertThrows(
                                    TS3CommandFailedException.class,
                                    () -> api.bans().delete(ban))
                            .getErrorId());
            api.bans().add(BanRequest.ip("198[.]51[.]100[.]9").withDuration(Duration.ofHours(1)));
            api.bans().deleteAll();
            assertEquals(List.of(), api.bans().list());

            int defaultGroup = api.servers().info().getDefaultChannelGroup();
            String serverKey = api.privilegeKeys()
                    .createForServerGroup(api.servers().info().getDefaultServerGroup(), "web " + target);
            String channelKey = api.privilegeKeys().createForChannelGroup(defaultGroup, 1, null);
            var keys = api.privilegeKeys().list().stream()
                    .map(PrivilegeKey::getToken)
                    .toList();
            assertTrue(keys.containsAll(List.of(serverKey, channelKey)));
            var used = assertThrows(
                    TS3CommandFailedException.class, () -> api.privilegeKeys().use(serverKey));
            evidence.add("privilegekeyuse " + new TreeMap<>(used.getError().getMap()));
            assertEquals(516, used.getErrorId(), "the Web Query client is a query client");
            api.privilegeKeys().delete(serverKey);
            api.privilegeKeys().delete(channelKey);
            assertTrue(api.privilegeKeys().list().stream()
                    .noneMatch(k -> k.getToken().equals(serverKey)));

            ClientDatabaseId self = api.whoAmI().getDatabaseId().orElseThrow();
            // Query guests cannot be complained about; the API key owner can.
            ClientDatabaseId target1 = self;
            api.complaints().add(target1, "web complaint " + target + " | é");
            var about = api.complaints().about(target1);
            evidence.add("complaints " + about);
            assertEquals(self, about.getFirst().getSourceClientDatabaseId());
            assertTrue(api.complaints().list().stream()
                    .anyMatch(c -> c.getMessage().equals("web complaint " + target + " | é")));
            api.complaints().delete(target1, self);
            api.complaints().add(target1, "again");
            api.complaints().deleteAll(target1);
            assertEquals(List.of(), api.complaints().about(target1));
        } finally {
            record(target, "bans-keys-complaints", evidence);
        }
    }

    @ParameterizedTest(name = "{0}: database clients, custom properties, offline messages and query logins")
    @EnumSource(Target.class)
    void databaseClientsAndLogins(Target target) throws Exception {
        var evidence = new ArrayList<String>();
        try (var query = web(target)) {
            var api = query.getApi(1);
            var clients = api.databaseClients();
            var first = clients.list().getFirst();
            ClientDatabaseId id = first.getDatabaseId();
            assertEquals(
                    List.of(first.getDatabaseId()),
                    clients.list(0, 1).stream().map(c -> c.getDatabaseId()).toList());
            var info = clients.info(id);
            assertEquals(id, info.getDatabaseId());
            assertEquals(
                    Optional.of(id),
                    clients.findByUniqueId(info.getUniqueIdentifier()).map(c -> c.getDatabaseId()));
            assertFalse(clients.search("%").isEmpty());
            String description = info.getDescription();
            clients.edit(id, Map.of(ClientProperty.CLIENT_DESCRIPTION, "web | \\s " + target));
            assertEquals("web | \\s " + target, clients.info(id).getDescription());
            clients.edit(id, Map.of(ClientProperty.CLIENT_DESCRIPTION, description));
            clients.setCustomProperty(id, "web", "value | \\s " + target);
            clients.setCustomProperties(id, Map.of("web2", "two"));
            assertEquals("value | \\s " + target, clients.customProperties(id).get("web"));
            assertEquals(
                    List.of(new CustomPropertyAssignment(id, "web", "value | \\s " + target)),
                    clients.searchCustomProperty("web", "%" + target));
            clients.deleteCustomProperty(id, "web");
            clients.deleteCustomProperty(id, "web2");
            assertFalse(clients.customProperties(id).containsKey("web"));

            var messages = api.messages();
            messages.sendOffline(info.getUniqueIdentifier(), "subject " + target, "body | \\s é");
            // The fixture's only clients are query clients; the Web Query client has no offline inbox entries.
            assertEquals(List.of(), messages.listOffline());
            assertEquals(Optional.empty(), messages.getOffline(99_999));
            assertEquals(
                    512,
                    assertThrows(
                                    TS3CommandFailedException.class,
                                    () -> messages.sendOffline("AAAAAAAAAAAAAAAAAAAAAAAAAAA=", "s", "m"))
                            .getErrorId());

            var instanceLogins = query.getApi().queryLogins();
            String loginName = "web-login-" + target;
            var created = instanceLogins.create(loginName);
            evidence.add("created login cldbid=" + created.getClientDatabaseId());
            assertFalse(created.getQueryClientLoginPassword().isBlank());
            try {
                assertTrue(instanceLogins.list().stream()
                        .anyMatch(l -> l.getQueryClientLoginName().equals(loginName)));
                assertEquals(1, instanceLogins.search(loginName).size());
                try (var login = new TS3Query(ts3.config(TS3Query.Protocol.RAW)
                        .setLoginCredentials(loginName, created.getQueryClientLoginPassword()))) {
                    login.connect();
                    assertEquals(Optional.of(loginName), login.getApi().whoAmI().getLoginName());
                }
                var serverLogin = api.queryLogins().create("web-srv-" + target, id);
                assertEquals(id, serverLogin.getClientDatabaseId());
                api.queryLogins().delete(id);
            } finally {
                instanceLogins.delete(created.getClientDatabaseId());
                var ownLogin = assertThrows(
                        TS3CommandFailedException.class, () -> api.queryLogins().updateOwn("web-own-" + target));
                evidence.add("clientsetserverquerylogin "
                        + new TreeMap<>(ownLogin.getError().getMap()));
                assertEquals(512, ownLogin.getErrorId(), "the Web Query client has no own query login");
            }
            assertTrue(instanceLogins.list().stream()
                    .noneMatch(l -> l.getQueryClientLoginName().equals(loginName)));
            assertEquals(
                    512,
                    assertThrows(TS3CommandFailedException.class, () -> clients.info(ClientDatabaseId.of(99_999)))
                            .getErrorId());
        } finally {
            record(target, "database-clients-logins", evidence);
        }
    }

    /**
     * Web Query has no notification stream: a RAW session registered for events observes what Web
     * Query requests do, which is how applications combine the two transports.
     */
    @ParameterizedTest(name = "{0}: online clients and text messages, observed by a ServerQuery session")
    @EnumSource(Target.class)
    void clientsAndMessages(Target target) throws Exception {
        var evidence = new ArrayList<String>();
        var texts = new LinkedBlockingQueue<TextMessageEvent>();
        var created = new LinkedBlockingQueue<ChannelCreateEvent>();
        try (var query = web(target);
                var observer = new TS3Query(ts3.config(TS3Query.Protocol.RAW))) {
            observer.connect();
            var observed = observer.getApi();
            observed.selectVirtualServerById(1, "observer-" + target);
            observed.events().register(TS3EventType.TEXT_PRIVATE);
            observed.events().register(TS3EventType.TEXT_CHANNEL, 0);
            observed.events().register(TS3EventType.TEXT_SERVER);
            observed.events().register(TS3EventType.CHANNEL, 0);
            observer.subscribe(new TS3EventAdapter() {
                @Override
                public void onTextMessage(TextMessageEvent e) {
                    texts.add(e);
                }

                @Override
                public void onChannelCreate(ChannelCreateEvent e) {
                    created.add(e);
                }
            });
            ClientId observerId = observed.whoAmI().getId();
            var api = query.getApi(1);
            var webClient = api.whoAmI();
            evidence.add("web client " + new TreeMap<>(webClient.getMap()));

            var clients = api.clients();
            var observerRow = clients.list().stream()
                    .filter(c -> c.getId().equals(observerId))
                    .findFirst()
                    .orElseThrow();
            assertEquals("observer-" + target, observerRow.getNickname());
            assertEquals("observer-" + target, clients.info(observerId).getNickname());
            assertTrue(clients.isOnline(observerId));
            assertTrue(clients.isOnline(observerRow.getUniqueIdentifier()));
            assertEquals(
                    Optional.of(observerId),
                    clients.findByName("OBSERVER-" + target, true).map(Client::getId));
            assertEquals(
                    List.of(observerId),
                    clients.search("observer-" + target).stream()
                            .map(Client::getId)
                            .toList());
            assertTrue(clients.findByUniqueId(observerRow.getUniqueIdentifier()).isPresent());
            clients.edit(observerId, Map.of(ClientProperty.CLIENT_DESCRIPTION, "edited by web " + target));
            assertEquals("edited by web " + target, clients.info(observerId).getDescription());

            int channel =
                    api.channels().create("web clients " + target, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
            try {
                var create = created.poll(5, TimeUnit.SECONDS);
                assertNotNull(create, "ServerQuery observes channels created over Web Query");
                assertEquals(channel, create.getChannelId());
                evidence.add("observed channelcreate " + new TreeMap<>(create.getMap()));

                clients.move(observerId, channel);
                assertEquals(channel, observed.whoAmI().getChannelId());
                clients.moveAll(List.of(observerId), 1, null);
                assertEquals(1, observed.whoAmI().getChannelId());
                clients.move(observerId, channel, null);

                api.messages().sendPrivate(observerId, "private | \\s 日本語 " + target);
                assertEquals("private | \\s 日本語 " + target, nextText(texts).getMessage());
                api.messages().sendChannel(channel, "channel " + target);
                assertEquals(channel, api.whoAmI().getChannelId(), "the Web Query client moved");
                assertEquals("channel " + target, nextText(texts).getMessage());
                api.messages().sendChannel("again " + target);
                assertEquals("again " + target, nextText(texts).getMessage());
                api.messages().sendServer("server " + target);
                assertEquals("server " + target, nextText(texts).getMessage());
                api.messages().broadcast("broadcast " + target);
                api.moveQuery(1);
                assertEquals(1, api.whoAmI().getChannelId());
                api.setNickname("web-" + target);
                assertEquals("web-" + target, api.whoAmI().getNickname());
                api.updateClient(Map.of(ClientProperty.CLIENT_NICKNAME, "serveradmin-web"));

                var poke = assertThrows(TS3CommandFailedException.class, () -> clients.poke(observerId, "poke"));
                evidence.add(
                        "poke query client " + new TreeMap<>(poke.getError().getMap()));
                assertEquals(channel, observed.whoAmI().getChannelId());
                clients.kickFromChannel("kick | \\s", observerId);
                assertEquals(1, observed.whoAmI().getChannelId(), "kicked to the default channel");
                // The server rejects these for query clients, the only clients this fixture can connect.
                var serverKick = assertThrows(
                        TS3CommandFailedException.class, () -> clients.kickFromServer("bye " + target, observerId));
                evidence.add("clientkick server "
                        + new TreeMap<>(serverKick.getError().getMap()));
                assertEquals(516, serverKick.getErrorId());
                var ban = assertThrows(
                        TS3CommandFailedException.class, () -> api.bans().banClients(ClientBan.of(observerId)));
                evidence.add("banclient " + new TreeMap<>(ban.getError().getMap()));
                assertEquals(516, ban.getErrorId());
                assertEquals(516, poke.getErrorId());
                assertTrue(clients.isOnline(observerId));
            } finally {
                api.channels().delete(channel, true);
            }
        } finally {
            record(target, "clients-messages", evidence);
        }
    }

    private static TextMessageEvent nextText(LinkedBlockingQueue<TextMessageEvent> texts) throws InterruptedException {
        var event = texts.poll(5, TimeUnit.SECONDS);
        assertNotNull(event, "ServerQuery observes text messages sent over Web Query");
        return event;
    }

    @ParameterizedTest(name = "{0}: raw commands, pipe-separated entries and concurrent requests")
    @EnumSource(Target.class)
    void rawAndConcurrentRequests(Target target) throws Exception {
        try (var query = web(target)) {
            var api = query.getApi(1);
            var raw = api.executeRawCommand("permidgetbyname permsid=i_icon_id|permsid=i_client_talk_power");
            assertEquals(
                    List.of(
                            api.permissions().idByName("i_icon_id"),
                            api.permissions().idByName("i_client_talk_power")),
                    raw.getResponses().stream()
                            .map(row -> row.requireInt("permid"))
                            .toList());
            assertTrue(raw.getRawResponse().startsWith("permid="), "rows keep the ServerQuery encoding");
            assertEquals(
                    "Default Channel",
                    api.executeRawCommand("channelinfo cid=1")
                            .getFirstResponse()
                            .orElseThrow()
                            .require("channel_name"));
            var async = query.getAsyncApi(1);
            var requests = new ArrayList<java.util.concurrent.CompletableFuture<Integer>>();
            for (int i = 0; i < 12; i++) {
                requests.add(CommandFutures.map(async.channels().list(), List::size));
            }
            int expected = api.channels().list().size();
            for (var request : requests) assertEquals(expected, CommandFutures.join(request));
        }
    }

    /** A fresh server: the limited license allows one virtual server, so the fixture's server is replaced. */
    @Test
    void virtualServerLifecycle() throws Exception {
        var evidence = new ArrayList<String>();
        try (var server = new TeamSpeakContainer().withWebQuery()) {
            server.start();
            String key;
            try (var raw = new TS3Query(server.config(TS3Query.Protocol.RAW))) {
                raw.connect();
                key = raw.getApi()
                        .apiKeys()
                        .add(ApiKeyScope.MANAGE, Duration.ZERO)
                        .getSecret()
                        .orElseThrow();
            }
            try (var query = new TS3WebQuery(server.webQueryConfig(true, key).build())) {
                var servers = query.getApi().servers();
                int original = servers.list().getFirst().getId();
                servers.stop(original, "replaced over Web Query | \\s");
                assertEquals(
                        VirtualServerStatus.OFFLINE, servers.list().getFirst().getStatus());
                servers.delete(original);
                assertTrue(servers.list().isEmpty());
                var created = servers.create(
                        "web lifecycle 日本語", Map.of(VirtualServerProperty.VIRTUALSERVER_MAXCLIENTS, "8"));
                evidence.add("created id=" + created.id() + " port=" + created.port());
                assertFalse(created.serverAdminToken().isBlank());
                assertEquals(
                        "web lifecycle 日本語",
                        query.getApi(created.id()).servers().info().getName());
                var serverApi = query.getApiByPort(created.port()).servers();
                assertEquals("web lifecycle 日本語", serverApi.info().getName());
                var snapshot = serverApi.createSnapshot();
                evidence.add("snapshot bytes=" + snapshot.data().length());
                serverApi.edit(Map.of(VirtualServerProperty.VIRTUALSERVER_NAME, "edited | \\s"));
                assertEquals("edited | \\s", serverApi.info().getName());
                serverApi.deploySnapshot(snapshot);
                assertEquals("web lifecycle 日本語", serverApi.info().getName(), "deploying restores the snapshot");
                servers.stop(created.id());
                assertEquals(
                        VirtualServerStatus.OFFLINE, servers.list().getFirst().getStatus());
                servers.start(created.id());
                assertEquals(
                        VirtualServerStatus.ONLINE, servers.list().getFirst().getStatus());
                var running = assertThrows(TS3CommandFailedException.class, () -> servers.delete(created.id()));
                assertEquals(1035, running.getErrorId());
                servers.stop(created.id());
                servers.delete(created.id());
                assertEquals(
                        1024,
                        assertThrows(TS3CommandFailedException.class, () -> servers.start(created.id()))
                                .getErrorId());
                assertEquals(
                        TeamSpeakContainer.TS3_VERSION,
                        query.getApi().instance().version().version());
                var replacement = servers.create("web reset", Map.of());
                String token = query.getApi(replacement.id()).permissions().reset();
                assertFalse(token.isBlank());
                evidence.add("permreset returned a token");
                query.getApi().instance().stopProcess("stopped over Web Query");
                long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
                while (server.isRunning() && System.nanoTime() < deadline) Thread.sleep(200);
                assertFalse(server.isRunning(), "serverprocessstop ends the server process");
            }
        } finally {
            Path path = Path.of("target", "compatibility", "TS3_WEBQUERY-server-lifecycle.txt");
            Files.createDirectories(path.getParent());
            Files.write(path, evidence);
        }
    }

    @ParameterizedTest(name = "{0}: channel administration with protocol escapes")
    @EnumSource(Target.class)
    void channels(Target target) throws Exception {
        var evidence = new ArrayList<String>();
        try (var query = web(target)) {
            var channels = query.getApi(1).channels();
            String name = "web " + target + " 日本語 | literal \\s\\p /";
            int parent = channels.create(name, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
            try {
                int child = channels.create(
                        "child " + target,
                        Map.of(
                                ChannelProperty.CHANNEL_FLAG_PERMANENT, "1",
                                ChannelProperty.CHANNEL_TOPIC, "topic | \\s"));
                var info = channels.info(parent);
                assertEquals(name, info.getName());
                assertEquals(name, admin.getApi().channels().info(parent).getName(), "ServerQuery sees the same value");
                channels.edit(child, Map.of(ChannelProperty.CHANNEL_DESCRIPTION, "described \\ | é\nline"));
                assertEquals("described \\ | é\nline", channels.info(child).getDescription());
                assertEquals("topic | \\s", channels.info(child).getTopic());
                channels.move(child, parent);
                assertEquals(parent, channels.info(child).getParentChannelId());
                channels.move(child, 0, 0);
                assertEquals(
                        Optional.of(parent),
                        channels.findByName(name.toUpperCase(java.util.Locale.ROOT), true)
                                .map(Channel::getId));
                assertEquals(Optional.empty(), channels.findByName(name.toUpperCase(java.util.Locale.ROOT), false));
                assertEquals(
                        List.of(parent),
                        channels.search("日本語").stream().map(Channel::getId).toList());
                assertEquals(List.of(), channels.search("no such channel"));
                assertTrue(channels.list().stream().anyMatch(channel -> channel.getId() == child));
                channels.delete(child);
                var missing = assertThrows(TS3CommandFailedException.class, () -> channels.info(child));
                evidence.add("missing " + new TreeMap<>(missing.getError().getMap()));
                assertEquals(768, missing.getErrorId());
            } finally {
                channels.delete(parent, true);
            }
            assertTrue(channels.list().stream().noneMatch(channel -> channel.getId() == parent));
        } finally {
            record(target, "channels", evidence);
        }
    }
}
