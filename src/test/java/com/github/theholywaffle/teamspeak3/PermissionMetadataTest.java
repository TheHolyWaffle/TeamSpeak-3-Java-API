package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.BPermissionType;
import com.github.theholywaffle.teamspeak3.api.IPermissionType;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PermissionMetadataTest {
	@Test void usesConnectedServerIdsAndNamesIncludingNewPermissions() throws Exception {
		try (var peer = new FakeServerQuery(p -> {
			p.expect("permissionlist");
			p.write("TS3\npermid=7001 permname=i_channel_needed_join_power permdesc=Join\\spower|permid=9002 permname=b_channel_create_permanent permdesc=Create|permid=12003 permname=i_future_server_permission permdesc=New\\spermission\nerror id=0 msg=ok\n");
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
		}); var query = new TS3Query(new TS3Config().setHost("127.0.0.1").setQueryPort(peer.port()).setFloodRate(TS3Query.FloodRate.UNLIMITED))) {
			query.connect();
			var api = query.getApi();
			var permissions = api.getPermissions();
			assertEquals(3, permissions.size());
			assertEquals("Join power", permissions.get(0).getDescription());
			assertEquals(7001, api.getPermissionIdByName(IPermissionType.I_CHANNEL_NEEDED_JOIN_POWER.getName()));
			assertArrayEquals(new int[]{9002, 12003}, api.getPermissionIdsByName(BPermissionType.B_CHANNEL_CREATE_PERMANENT.getName(), permissions.get(2).getName()));
			api.addClientPermission(17, IPermissionType.I_CHANNEL_NEEDED_JOIN_POWER, 23, false);
			api.addClientPermission(17, BPermissionType.B_CHANNEL_CREATE_PERMANENT, true, true);
			api.addChannelPermission(4, permissions.get(2).getName(), 41);
			var error = assertThrows(TS3CommandFailedException.class, () -> api.getPermissionIdByName("i_unknown_permission"));
			assertEquals(2562, error.getError().getId());
			assertEquals("invalid permission ID", error.getError().getMessage());
			assertEquals(2562, assertThrows(TS3CommandFailedException.class, () -> api.getPermissionAssignments("i_unknown_permission")).getError().getId());
			assertTrue(api.getPermissionAssignments("i_valid_unassigned").isEmpty());
			FutureAssertions.awaitCommandAdmission(query);
			query.exit(); peer.await();
		}
	}

	@Test void resolvesFailedPermissionFromServerMetadata() throws Exception {
		try (var peer = new FakeServerQuery(p -> {
			p.expect("channeladdperm cid=4 permsid=i_channel_needed_join_power permvalue=41");
			p.write("TS3\nerror id=2568 msg=insufficient\\sclient\\spermissions failed_permid=7359\n");
			p.expect("permissionlist");
			p.write("permid=7359 permname=i_channel_permission_modify_power permdesc=Modify\\spower\nerror id=0 msg=ok\n");
			p.finishQuit();
		}); var query = new TS3Query(new TS3Config().setHost("127.0.0.1").setQueryPort(peer.port()).setFloodRate(TS3Query.FloodRate.UNLIMITED))) {
			query.connect();
			var api = query.getApi();
			var error = assertThrows(TS3CommandFailedException.class, () -> api.addChannelPermission(4, IPermissionType.I_CHANNEL_NEEDED_JOIN_POWER.getName(), 41));
			assertEquals(2568, error.getError().getId());
			int failedId = error.getError().getFailedPermissionId();
			assertEquals(7359, failedId);
			assertEquals("i_channel_permission_modify_power", api.getPermissions().stream().filter(p -> p.getId() == failedId).findFirst().orElseThrow().getName());
			FutureAssertions.awaitCommandAdmission(query);
			query.exit(); peer.await();
		}
	}
}
