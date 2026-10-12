package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import com.github.theholywaffle.teamspeak3.api.ServerGroupType;
import com.github.theholywaffle.teamspeak3.api.IPermissionType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class PermissionModesCompatibilityIT {
    @ParameterizedTest @EnumSource(ServerQueryCompatibilityIT.Target.class) @Timeout(90)
    void groupTypesAutomaticAssignmentsAndReset(ServerQueryCompatibilityIT.Target target) throws Exception {
        var evidence = new ArrayList<String>();
        try (var server = new TeamSpeakContainer()) {
            server.start();
            try (var query = new TS3Query(server.config(target.protocol))) {
                query.connect(); var api = query.getApi(); api.selectVirtualServerById(1);
                for (String command : List.of("servergroupautoaddperm", "servergroupautodelperm", "permreset", "servergroupcopy", "channelgroupcopy", "channelgroupadd")) {
                    var help = api.executeRawCommand("help " + command).getRawResponse();
                    assertTrue(help.startsWith("Usage: " + command)); evidence.add("help " + command + "\n" + help);
                }
                for (var type : PermissionGroupDatabaseType.values()) {
                    String name = "mode-source-" + type;
                    int source = api.addServerGroup(name, type);
                    api.addServerGroupPermission(source, IPermissionType.I_CLIENT_TALK_POWER.getName(), 47, false, true);
                    int copy = api.copyServerGroup(source, "mode-copy-" + type, type);
                    var row = api.getServerGroups().stream().filter(group -> group.getId() == copy).findFirst().orElseThrow();
                    assertEquals(type, row.getType());
                    CompatibilityAssertions.permission(api.getServerGroupPermissions(copy), IPermissionType.I_CLIENT_TALK_POWER.getName(), 47, false, true);
                    api.deleteServerGroup(copy, true); api.deleteServerGroup(source, true);
                    evidence.add("server group " + type + " create/copy/permission/delete passed");
                }
                for (var type : List.of(PermissionGroupDatabaseType.TEMPLATE, PermissionGroupDatabaseType.REGULAR)) {
                    int source = api.addChannelGroup("channel-source-" + type, type);
                    api.addChannelGroupPermission(source, IPermissionType.I_CLIENT_TALK_POWER.getName(), 48);
                    int copy = api.copyChannelGroup(source, "channel-copy-" + type, type);
                    var row = api.getChannelGroups().stream().filter(group -> group.getId() == copy).findFirst().orElseThrow();
                    assertEquals(type, row.getType());
                    CompatibilityAssertions.permission(api.getChannelGroupPermissions(copy), IPermissionType.I_CLIENT_TALK_POWER.getName(), 48, false, false);
                    api.deleteChannelGroup(copy, true); api.deleteChannelGroup(source, true);
                    evidence.add("channel group " + type + " create/copy/permission/delete passed");
                }
                var queryChannelGroup = assertThrows(com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException.class,
                    () -> api.addChannelGroup("unsupported-query-channel", PermissionGroupDatabaseType.QUERY));
                assertEquals(1538, queryChannelGroup.getError().getId());
                evidence.add("channelgroupadd typeQUERY rejected1538 despite live help listing type2");
                int automatic = api.addServerGroup("mode-auto"); int unrelated = api.addServerGroup("mode-unrelated");
                api.addServerGroupPermission(automatic, IPermissionType.I_GROUP_AUTO_UPDATE_TYPE.getName(), ServerGroupType.SERVER_NORMAL.getIndex(), false, false);
                api.addServerGroupPermission(automatic, IPermissionType.I_GROUP_AUTO_UPDATE_MAX_VALUE.getName(), 100, false, false);
                api.addPermissionToAllServerGroups(ServerGroupType.SERVER_NORMAL, IPermissionType.I_CLIENT_TALK_POWER.getName(), 49, false, true);
                CompatibilityAssertions.permission(api.getServerGroupPermissions(automatic), IPermissionType.I_CLIENT_TALK_POWER.getName(), 49, false, true);
                assertTrue(api.getServerGroupPermissions(unrelated).stream().noneMatch(permission -> permission.getName().equals(IPermissionType.I_CLIENT_TALK_POWER.getName())));
                evidence.add("automatic permission targeted configured group classification; unclassified group unchanged");
                api.deletePermissionFromAllServerGroups(ServerGroupType.SERVER_NORMAL, IPermissionType.I_CLIENT_TALK_POWER.getName());
                assertTrue(api.getServerGroupPermissions(automatic).stream().noneMatch(permission -> permission.getName().equals(IPermissionType.I_CLIENT_TALK_POWER.getName())));
                evidence.add("automatic permission deletion removed configured assignment");
                String token = api.resetPermissions(); assertFalse(token.isBlank());
                assertEquals(0, api.whoAmI().getVirtualServerId(), "Permission reset clears query selection");
                api.selectVirtualServerById(1);
                assertTrue(api.getPrivilegeKeys().stream().anyMatch(key -> key.getToken().equals(token)));
                assertTrue(api.getServerGroups().stream().noneMatch(group -> group.getName().equals("mode-auto") || group.getName().equals("mode-unrelated")));
                assertTrue(api.getServerGroups().stream().anyMatch(group -> group.getName().equals("Server Admin")));
                assertFalse(api.getPermissions().isEmpty()); assertFalse(api.getVersion().getVersion().isBlank());
                evidence.add("permission reset removed owned custom regular groups, restored defaults and returned an administrator key (redacted)");
                query.exit();
            }
        } finally {
            var path = Path.of("target", "compatibility", target + "-permission-modes.txt");
            Files.createDirectories(path.getParent()); Files.write(path, evidence);
        }
    }
}
