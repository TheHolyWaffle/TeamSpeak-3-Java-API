package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ServerGroupType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PermissionCommandsTest {

	@Test
	public void permFind_ValidPermissionName() {
		String expected = "permfind permsid=b_virtualserver_create";
		assertEquals(expected, PermissionCommands.permFind("b_virtualserver_create").toString());
	}

	@Test
	public void permFind_NullPermissionNameException() {
		assertThrows(IllegalArgumentException.class, () -> PermissionCommands.permFind(null));
	}

	@Test
	public void permFind_EmptyPermissionNameException() {
		assertThrows(IllegalArgumentException.class, () -> PermissionCommands.permFind(""));
	}

	@Test
	public void permGet_SinglePermission() {
		String expected = "permget permsid=b_virtualserver_create";
		assertEquals(expected, PermissionCommands.permGet("b_virtualserver_create").toString());
	}

	@Test
	public void permGet_MultiplePermissions() {
		String expected = "permget permsid=b_virtualserver_create|permsid=b_client_kick|permsid=b_client_ban";
		assertEquals(expected, PermissionCommands.permGet("b_virtualserver_create", "b_client_kick", "b_client_ban").toString());
	}

	@Test
	public void permGet_NullPermissionArrayException() {
		assertThrows(IllegalArgumentException.class, () -> PermissionCommands.permGet((String[]) null));
	}

	@Test
	public void permGet_EmptyPermissionArrayException() {
		assertThrows(IllegalArgumentException.class, () -> PermissionCommands.permGet());
	}

	@Test
	public void permGet_NullPermissionInArrayException() {
		assertThrows(IllegalArgumentException.class, () -> PermissionCommands.permGet("b_virtualserver_create", null));
	}

	@Test
	public void permGet_EmptyPermissionInArrayException() {
		assertThrows(IllegalArgumentException.class, () -> PermissionCommands.permGet("b_virtualserver_create", ""));
	}

	@Test
	public void permIdGetByName_SinglePermission() {
		String expected = "permidgetbyname permsid=b_virtualserver_create";
		assertEquals(expected, PermissionCommands.permIdGetByName("b_virtualserver_create").toString());
	}

	@Test
	public void permIdGetByName_MultiplePermissions() {
		String expected = "permidgetbyname permsid=b_virtualserver_create|permsid=b_client_kick";
		assertEquals(expected, PermissionCommands.permIdGetByName("b_virtualserver_create", "b_client_kick").toString());
	}

	@Test
	public void permIdGetByName_NullPermissionNameException() {
		assertThrows(IllegalArgumentException.class, () -> PermissionCommands.permIdGetByName((String) null));
	}

	@Test
	public void permIdGetByName_EmptyPermissionNameException() {
		assertThrows(IllegalArgumentException.class, () -> PermissionCommands.permIdGetByName(""));
	}

	@Test
	public void permIdGetByName_NullPermissionArrayException() {
		assertThrows(IllegalArgumentException.class, () -> PermissionCommands.permIdGetByName((String[]) null));
	}

	@Test
	public void permIdGetByName_EmptyPermissionArrayException() {
		assertThrows(IllegalArgumentException.class, () -> PermissionCommands.permIdGetByName(new String[0]));
	}

	@Test
	public void permissionList() {
		String expected = "permissionlist";
		assertEquals(expected, PermissionCommands.permissionList().toString());
	}

	@Test
	public void permOverview_ValidParameters() {
		String expected = "permoverview cid=5 cldbid=100 permid=0";
		assertEquals(expected, PermissionCommands.permOverview(5, 100).toString());
	}

	@Test
	public void permOverview_ZeroChannelId() {
		String expected = "permoverview cid=0 cldbid=100 permid=0";
		assertEquals(expected, PermissionCommands.permOverview(0, 100).toString());
	}

	@Test
	public void permOverview_NegativeChannelId() {
		String expected = "permoverview cid=-1 cldbid=100 permid=0";
		assertEquals(expected, PermissionCommands.permOverview(-1, 100).toString());
	}

	@Test
	public void permReset() {
		String expected = "permreset";
		assertEquals(expected, PermissionCommands.permReset().toString());
	}

	@Test
	public void permFind_ComplexPermissionName() {
		String expected = "permfind permsid=i_channel_needed_subscribe_power";
		assertEquals(expected, PermissionCommands.permFind("i_channel_needed_subscribe_power").toString());
	}

	@Test
	public void permGet_ManyPermissions() {
		String[] permissions = {
			"b_virtualserver_create",
			"b_client_kick",
			"b_client_ban",
			"i_channel_needed_subscribe_power",
			"i_client_talk_power"
		};
		
		String result = PermissionCommands.permGet(permissions).toString();
		assertTrue(result.startsWith("permget"));
		assertTrue(result.contains("permsid=b_virtualserver_create"));
		assertTrue(result.contains("permsid=b_client_kick"));
		assertTrue(result.contains("permsid=b_client_ban"));
		assertTrue(result.contains("permsid=i_channel_needed_subscribe_power"));
		assertTrue(result.contains("permsid=i_client_talk_power"));
		
		// Count the number of permissions (should be 5)
		int count = result.split("\\|").length;
		assertEquals(5, count);
	}

	@Test
	public void permIdGetByName_ManyPermissions() {
		String[] permissions = {
			"b_virtualserver_create",
			"b_client_kick",
			"b_client_ban"
		};
		
		String result = PermissionCommands.permIdGetByName(permissions).toString();
		assertTrue(result.startsWith("permidgetbyname"));
		assertTrue(result.contains("permsid=b_virtualserver_create"));
		assertTrue(result.contains("permsid=b_client_kick"));
		assertTrue(result.contains("permsid=b_client_ban"));
		
		// Count the number of permissions (should be 3)
		int count = result.split("\\|").length;
		assertEquals(3, count);
	}

	@Test
	public void permOverview_LargeIds() {
		String expected = "permoverview cid=999999 cldbid=888888 permid=0";
		assertEquals(expected, PermissionCommands.permOverview(999999, 888888).toString());
	}

	@Test
	public void permFind_PermissionWithUnderscores() {
		String expected = "permfind permsid=i_client_needed_talk_power";
		assertEquals(expected, PermissionCommands.permFind("i_client_needed_talk_power").toString());
	}

	@Test
	public void permGet_SingleLongPermissionName() {
		String longPermName = "i_channel_needed_subscribe_power_with_very_long_name";
		String expected = "permget permsid=" + longPermName;
		assertEquals(expected, PermissionCommands.permGet(longPermName).toString());
	}

	@Test
	public void permIdGetByName_SingleLongPermissionName() {
		String longPermName = "i_channel_needed_subscribe_power_with_very_long_name";
		String expected = "permidgetbyname permsid=" + longPermName;
		assertEquals(expected, PermissionCommands.permIdGetByName(longPermName).toString());
	}
}
