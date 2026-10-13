package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.BPermissionType;
import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.IPermissionType;
import com.github.theholywaffle.teamspeak3.api.PermissionGrant;
import com.github.theholywaffle.teamspeak3.api.PermissionTarget;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import org.junit.jupiter.api.Test;

class PermissionMetadataTest {
    @Test
    void usesConnectedServerIdsAndNamesIncludingNewPermissions() throws Exception {
        try (var peer = new FakeServerQuery(p -> {
                    p.expect("permissionlist");
                    p.write("""
				TS3
				permid=7001 permname=i_channel_needed_join_power permdesc=Join\\spower|\
				permid=9002 permname=b_channel_create_permanent permdesc=Create|\
				permid=12003 permname=i_future_server_permission permdesc=New\\spermission
				error id=0 msg=ok
				""");
                    p.expect("permidgetbyname permsid=i_channel_needed_join_power");
                    p.write("permid=7001\nerror id=0 msg=ok\n");
                    p.expect("permidgetbyname permsid=b_channel_create_permanent|permsid=i_future_server_permission");
                    p.write("permid=9002|permid=12003\nerror id=0 msg=ok\n");
                    p.expect("clientaddperm cldbid=17 permsid=i_channel_needed_join_power permvalue=23 permskip=0");
                    p.write("error id=0 msg=ok\n");
                    p.expect("clientaddperm cldbid=17 permsid=b_channel_create_permanent permvalue=1 permskip=1");
                    p.write("error id=0 msg=ok\n");
                    p.expect("channeladdperm cid=4 permsid=i_future_server_permission permvalue=41");
                    p.write("error id=0 msg=ok\n");
                    p.expect("permidgetbyname permsid=i_unknown_permission");
                    p.write("error id=2562 msg=invalid\\spermission\\sID\n");
                    p.expect("permfind permsid=i_unknown_permission");
                    p.write("error id=2562 msg=invalid\\spermission\\sID\n");
                    p.expect("permfind permsid=i_valid_unassigned");
                    p.write("error id=1281 msg=database\\sempty\\sresult\\sset\n");
                    p.finishQuit();
                });
                var query = new TS3Query(new TS3Config()
                        .setHost("127.0.0.1")
                        .setQueryPort(peer.port())
                        .setFloodRate(TS3Query.FloodRate.UNLIMITED))) {
            query.connect();
            var api = query.getApi();
            var permissions = api.permissions().list();
            assertEquals(3, permissions.size());
            assertEquals("Join power", permissions.get(0).description());
            assertEquals(7001, api.permissions().idByName(IPermissionType.I_CHANNEL_NEEDED_JOIN_POWER.getName()));
            assertEquals(
                    java.util.List.of(9002, 12003),
                    api.permissions()
                            .idsByName(
                                    BPermissionType.B_CHANNEL_CREATE_PERMANENT.getName(),
                                    permissions.get(2).name()));
            api.permissions()
                    .add(
                            PermissionTarget.client(ClientDatabaseId.of(17)),
                            PermissionGrant.of(IPermissionType.I_CHANNEL_NEEDED_JOIN_POWER, 23));
            api.permissions()
                    .add(
                            PermissionTarget.client(ClientDatabaseId.of(17)),
                            PermissionGrant.of(BPermissionType.B_CHANNEL_CREATE_PERMANENT, true)
                                    .withSkip());
            api.permissions()
                    .add(
                            PermissionTarget.channel(4),
                            PermissionGrant.of(permissions.get(2).name(), 41));
            var error = assertThrows(
                    TS3CommandFailedException.class, () -> api.permissions().idByName("i_unknown_permission"));
            assertEquals(2562, error.getError().getId());
            assertEquals("invalid permission ID", error.getError().getMessage());
            assertEquals(
                    2562,
                    assertThrows(
                                    TS3CommandFailedException.class,
                                    () -> api.permissions().assignments("i_unknown_permission"))
                            .getError()
                            .getId());
            assertTrue(api.permissions().assignments("i_valid_unassigned").isEmpty());
            FutureAssertions.awaitCommandAdmission(query);
            query.exit();
            peer.await();
        }
    }

    @Test
    void resolvesFailedPermissionFromServerMetadata() throws Exception {
        try (var peer = new FakeServerQuery(p -> {
                    p.expect("channeladdperm cid=4 permsid=i_channel_needed_join_power permvalue=41");
                    p.write("TS3\nerror id=2568 msg=insufficient\\sclient\\spermissions failed_permid=7359\n");
                    p.expect("permissionlist");
                    p.write(
                            "permid=7359 permname=i_channel_permission_modify_power permdesc=Modify\\spower\nerror id=0 msg=ok\n");
                    p.finishQuit();
                });
                var query = new TS3Query(new TS3Config()
                        .setHost("127.0.0.1")
                        .setQueryPort(peer.port())
                        .setFloodRate(TS3Query.FloodRate.UNLIMITED))) {
            query.connect();
            var api = query.getApi();
            var error = assertThrows(
                    TS3CommandFailedException.class,
                    () -> api.permissions()
                            .add(
                                    PermissionTarget.channel(4),
                                    PermissionGrant.of(IPermissionType.I_CHANNEL_NEEDED_JOIN_POWER.getName(), 41)));
            assertEquals(2568, error.getError().getId());
            int failedId = error.getError().getFailedPermissionId().orElseThrow();
            assertEquals(7359, failedId);
            assertEquals(
                    "i_channel_permission_modify_power",
                    api.permissions().list().stream()
                            .filter(p -> p.id() == failedId)
                            .findFirst()
                            .orElseThrow()
                            .name());
            FutureAssertions.awaitCommandAdmission(query);
            query.exit();
            peer.await();
        }
    }
}
