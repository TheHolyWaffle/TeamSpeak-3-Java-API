package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.IPermissionType;
import com.github.theholywaffle.teamspeak3.api.PermissionGrant;
import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import com.github.theholywaffle.teamspeak3.api.PermissionTarget;
import com.github.theholywaffle.teamspeak3.api.ServerGroupType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PermissionModesCompatibilityIT {
    @ParameterizedTest
    @EnumSource(ServerQueryCompatibilityIT.Target.class)
    @Timeout(90)
    void groupTypesAutomaticAssignmentsAndReset(ServerQueryCompatibilityIT.Target target) throws Exception {
        var evidence = new ArrayList<String>();
        try (var server = new TeamSpeakContainer()) {
            server.start();
            try (var query = new TS3Query(server.config(target.protocol))) {
                query.connect();
                var api = query.getApi();
                api.selectVirtualServerById(1);
                for (String command : List.of(
                        "servergroupautoaddperm",
                        "servergroupautodelperm",
                        "permreset",
                        "servergroupcopy",
                        "channelgroupcopy",
                        "channelgroupadd")) {
                    var help = api.executeRawCommand("help " + command).getRawResponse();
                    assertTrue(help.startsWith("Usage: " + command));
                    evidence.add("help " + command + "\n" + help);
                }
                for (var type : List.of(
                        PermissionGroupDatabaseType.TEMPLATE,
                        PermissionGroupDatabaseType.REGULAR,
                        PermissionGroupDatabaseType.QUERY)) {
                    String name = "mode-source-" + type;
                    int source = api.serverGroups().create(name, type);
                    api.permissions()
                            .add(
                                    PermissionTarget.serverGroup(source),
                                    PermissionGrant.of(IPermissionType.I_CLIENT_TALK_POWER.getName(), 47)
                                            .withSkip());
                    int copy = api.serverGroups().copy(source, "mode-copy-" + type, type);
                    var row = api.serverGroups().list().stream()
                            .filter(group -> group.getId() == copy)
                            .findFirst()
                            .orElseThrow();
                    assertEquals(type, row.getType());
                    CompatibilityAssertions.permission(
                            api.permissions().assigned(PermissionTarget.serverGroup(copy)),
                            IPermissionType.I_CLIENT_TALK_POWER.getName(),
                            47,
                            false,
                            true);
                    api.serverGroups().delete(copy, true);
                    api.serverGroups().delete(source, true);
                    evidence.add("server group " + type + " create/copy/permission/delete passed");
                }
                for (var type : List.of(PermissionGroupDatabaseType.TEMPLATE, PermissionGroupDatabaseType.REGULAR)) {
                    int source = api.channelGroups().create("channel-source-" + type, type);
                    api.permissions()
                            .add(
                                    PermissionTarget.channelGroup(source),
                                    PermissionGrant.of(IPermissionType.I_CLIENT_TALK_POWER.getName(), 48));
                    int copy = api.channelGroups().copy(source, "channel-copy-" + type, type);
                    var row = api.channelGroups().list().stream()
                            .filter(group -> group.getId() == copy)
                            .findFirst()
                            .orElseThrow();
                    assertEquals(type, row.getType());
                    CompatibilityAssertions.permission(
                            api.permissions().assigned(PermissionTarget.channelGroup(copy)),
                            IPermissionType.I_CLIENT_TALK_POWER.getName(),
                            48,
                            false,
                            false);
                    api.channelGroups().delete(copy, true);
                    api.channelGroups().delete(source, true);
                    evidence.add("channel group " + type + " create/copy/permission/delete passed");
                }
                var queryChannelGroup = assertThrows(
                        com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException.class,
                        () -> api.channelGroups()
                                .create("unsupported-query-channel", PermissionGroupDatabaseType.QUERY));
                assertEquals(1538, queryChannelGroup.getError().getId());
                evidence.add("channelgroupadd typeQUERY rejected1538 despite live help listing type2");
                int automatic = api.serverGroups().create("mode-auto");
                int unrelated = api.serverGroups().create("mode-unrelated");
                api.permissions()
                        .add(
                                PermissionTarget.serverGroup(automatic),
                                PermissionGrant.of(
                                        IPermissionType.I_GROUP_AUTO_UPDATE_TYPE.getName(),
                                        ServerGroupType.SERVER_NORMAL.getIndex()));
                api.permissions()
                        .add(
                                PermissionTarget.serverGroup(automatic),
                                PermissionGrant.of(IPermissionType.I_GROUP_AUTO_UPDATE_MAX_VALUE.getName(), 100));
                api.permissions()
                        .addToAllServerGroups(
                                ServerGroupType.SERVER_NORMAL,
                                PermissionGrant.of(IPermissionType.I_CLIENT_TALK_POWER.getName(), 49)
                                        .withSkip());
                CompatibilityAssertions.permission(
                        api.permissions().assigned(PermissionTarget.serverGroup(automatic)),
                        IPermissionType.I_CLIENT_TALK_POWER.getName(),
                        49,
                        false,
                        true);
                assertTrue(api.permissions().assigned(PermissionTarget.serverGroup(unrelated)).stream()
                        .noneMatch(
                                permission -> permission.name().equals(IPermissionType.I_CLIENT_TALK_POWER.getName())));
                evidence.add(
                        "automatic permission targeted configured group classification; unclassified group unchanged");
                api.permissions()
                        .deleteFromAllServerGroups(
                                ServerGroupType.SERVER_NORMAL, IPermissionType.I_CLIENT_TALK_POWER.getName());
                assertTrue(api.permissions().assigned(PermissionTarget.serverGroup(automatic)).stream()
                        .noneMatch(
                                permission -> permission.name().equals(IPermissionType.I_CLIENT_TALK_POWER.getName())));
                evidence.add("automatic permission deletion removed configured assignment");
                String token = api.permissions().reset();
                assertFalse(token.isBlank());
                assertEquals(0, api.whoAmI().getVirtualServerId(), "Permission reset clears query selection");
                api.selectVirtualServerById(1);
                assertTrue(api.privilegeKeys().list().stream()
                        .anyMatch(key -> key.getToken().equals(token)));
                var restoredGroups = api.serverGroups().list();
                assertTrue(restoredGroups.stream()
                        .noneMatch(group -> group.getName().equals("mode-auto")
                                || group.getName().equals("mode-unrelated")));
                assertTrue(restoredGroups.stream()
                        .anyMatch(group -> group.getName().equals("Server Admin")));
                assertFalse(api.permissions().list().isEmpty());
                assertFalse(api.instance().version().version().isBlank());
                evidence.add(
                        "permission reset removed owned custom regular groups, restored defaults and returned an administrator key (redacted)");
                query.exit();
            }
        } finally {
            var path = Path.of("target", "compatibility", target + "-permission-modes.txt");
            Files.createDirectories(path.getParent());
            Files.write(path, evidence);
        }
    }
}
