package com.github.theholywaffle.teamspeak3.api.wrapper;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PermissionInfoTest {

	@Test
	public void constructor_ValidMap() {
		Map<String, String> map = new HashMap<>();
		map.put("permid", "100");
		map.put("permname", "b_client_talk_power");
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		assertNotNull(permissionInfo);
		assertEquals(100, permissionInfo.getId());
		assertEquals("b_client_talk_power", permissionInfo.getName());
	}

	@Test
	public void getId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("permid", "42");
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		assertEquals(42, permissionInfo.getId());
	}

	@Test
	public void getId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		map.put("permid", "0");
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		assertEquals(0, permissionInfo.getId());
	}

	@Test
	public void getId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		map.put("permid", "-1");
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		assertEquals(-1, permissionInfo.getId());
	}

	@Test
	public void getId_LargeId() {
		Map<String, String> map = new HashMap<>();
		map.put("permid", String.valueOf(Integer.MAX_VALUE));
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		assertEquals(Integer.MAX_VALUE, permissionInfo.getId());
	}

	@Test
	public void getName_BooleanPermission() {
		Map<String, String> map = new HashMap<>();
		map.put("permname", "b_client_talk_power");
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		assertEquals("b_client_talk_power", permissionInfo.getName());
	}

	@Test
	public void getName_IntegerPermission() {
		Map<String, String> map = new HashMap<>();
		map.put("permname", "i_client_talk_power");
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		assertEquals("i_client_talk_power", permissionInfo.getName());
	}

	@Test
	public void getName_EmptyName() {
		Map<String, String> map = new HashMap<>();
		map.put("permname", "");
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		assertEquals("", permissionInfo.getName());
	}

	@Test
	public void getName_NullName() {
		Map<String, String> map = new HashMap<>();
		// permname key not present
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		assertEquals("", permissionInfo.getName());
	}

	@Test
	public void getDescription_ValidDescription() {
		Map<String, String> map = new HashMap<>();
		map.put("permdesc", "Allows client to talk in channel");
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		assertEquals("Allows client to talk in channel", permissionInfo.getDescription());
	}

	@Test
	public void getDescription_EmptyDescription() {
		Map<String, String> map = new HashMap<>();
		map.put("permdesc", "");
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		assertEquals("", permissionInfo.getDescription());
	}

	@Test
	public void getDescription_NullDescription() {
		Map<String, String> map = new HashMap<>();
		// permdesc key not present
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		assertEquals("", permissionInfo.getDescription());
	}

	@Test
	public void getDescription_LongDescription() {
		Map<String, String> map = new HashMap<>();
		String longDescription = "This permission allows the client to perform various administrative tasks including managing channels, users, and server settings with full control over the server environment.";
		map.put("permdesc", longDescription);
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		assertEquals(longDescription, permissionInfo.getDescription());
	}

	@Test
	public void permissionInfo_CompleteData() {
		Map<String, String> map = new HashMap<>();
		map.put("permid", "150");
		map.put("permname", "b_serverinstance_modify_settings");
		map.put("permdesc", "Modify server instance settings");
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		
		assertEquals(150, permissionInfo.getId());
		assertEquals("b_serverinstance_modify_settings", permissionInfo.getName());
		assertEquals("Modify server instance settings", permissionInfo.getDescription());
	}

	@Test
	public void permissionInfo_EmptyMap() {
		Map<String, String> map = new HashMap<>();
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		
		assertEquals(-1, permissionInfo.getId()); // Default int value
		assertEquals("", permissionInfo.getName());
		assertEquals("", permissionInfo.getDescription());
	}

	@Test
	public void permissionInfo_PartialData() {
		Map<String, String> map = new HashMap<>();
		map.put("permid", "75");
		map.put("permname", "i_channel_needed_talk_power");
		// Missing description
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		
		assertEquals(75, permissionInfo.getId());
		assertEquals("i_channel_needed_talk_power", permissionInfo.getName());
		assertEquals("", permissionInfo.getDescription()); // Default string value
	}

	@Test
	public void permissionInfo_AllZeroValues() {
		Map<String, String> map = new HashMap<>();
		map.put("permid", "0");
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		
		assertEquals(0, permissionInfo.getId());
	}

	@Test
	public void permissionInfo_AllMaxValues() {
		Map<String, String> map = new HashMap<>();
		map.put("permid", String.valueOf(Integer.MAX_VALUE));
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		
		assertEquals(Integer.MAX_VALUE, permissionInfo.getId());
	}

	@Test
	public void permissionInfo_InheritedWrapperMethods() {
		Map<String, String> map = new HashMap<>();
		map.put("permid", "200");
		map.put("permname", "b_client_kick_from_server");
		map.put("test_field", "test_value");
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		
		// Test inherited methods from Wrapper
		assertEquals("200", permissionInfo.get("permid"));
		assertEquals("b_client_kick_from_server", permissionInfo.get("permname"));
		assertEquals("test_value", permissionInfo.get("test_field"));
		assertEquals(200, permissionInfo.getInt("permid"));
	}

	@Test
	public void permissionInfo_RealWorldBooleanPermissions() {
		// Test real-world boolean permissions
		
		// Talk permission
		Map<String, String> talkMap = new HashMap<>();
		talkMap.put("permid", "37");
		talkMap.put("permname", "b_client_talk_power");
		talkMap.put("permdesc", "Talk power");
		
		PermissionInfo talkPerm = new PermissionInfo(talkMap);
		assertEquals(37, talkPerm.getId());
		assertEquals("b_client_talk_power", talkPerm.getName());
		assertEquals("Talk power", talkPerm.getDescription());
		assertTrue(talkPerm.getName().startsWith("b_")); // Boolean permission
		
		// Server admin permission
		Map<String, String> adminMap = new HashMap<>();
		adminMap.put("permid", "1");
		adminMap.put("permname", "b_serverinstance_help_view");
		adminMap.put("permdesc", "Retrieve information about ServerQuery commands");
		
		PermissionInfo adminPerm = new PermissionInfo(adminMap);
		assertEquals(1, adminPerm.getId());
		assertEquals("b_serverinstance_help_view", adminPerm.getName());
		assertEquals("Retrieve information about ServerQuery commands", adminPerm.getDescription());
		assertTrue(adminPerm.getName().startsWith("b_")); // Boolean permission
	}

	@Test
	public void permissionInfo_RealWorldIntegerPermissions() {
		// Test real-world integer permissions
		
		// Talk power permission
		Map<String, String> talkPowerMap = new HashMap<>();
		talkPowerMap.put("permid", "38");
		talkPowerMap.put("permname", "i_client_talk_power");
		talkPowerMap.put("permdesc", "Talk power value");
		
		PermissionInfo talkPowerPerm = new PermissionInfo(talkPowerMap);
		assertEquals(38, talkPowerPerm.getId());
		assertEquals("i_client_talk_power", talkPowerPerm.getName());
		assertEquals("Talk power value", talkPowerPerm.getDescription());
		assertTrue(talkPowerPerm.getName().startsWith("i_")); // Integer permission
		
		// Channel needed talk power
		Map<String, String> neededTalkPowerMap = new HashMap<>();
		neededTalkPowerMap.put("permid", "39");
		neededTalkPowerMap.put("permname", "i_channel_needed_talk_power");
		neededTalkPowerMap.put("permdesc", "Needed talk power");
		
		PermissionInfo neededTalkPowerPerm = new PermissionInfo(neededTalkPowerMap);
		assertEquals(39, neededTalkPowerPerm.getId());
		assertEquals("i_channel_needed_talk_power", neededTalkPowerPerm.getName());
		assertEquals("Needed talk power", neededTalkPowerPerm.getDescription());
		assertTrue(neededTalkPowerPerm.getName().startsWith("i_")); // Integer permission
	}

	@Test
	public void permissionInfo_PermissionsWithoutDescription() {
		// Test permissions that might not have descriptions
		Map<String, String> map = new HashMap<>();
		map.put("permid", "999");
		map.put("permname", "b_custom_permission");
		// No description provided
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		
		assertEquals(999, permissionInfo.getId());
		assertEquals("b_custom_permission", permissionInfo.getName());
		assertEquals("", permissionInfo.getDescription()); // Empty string when no description
	}

	@Test
	public void permissionInfo_SpecialCharactersInDescription() {
		Map<String, String> map = new HashMap<>();
		map.put("permid", "500");
		map.put("permname", "b_special_permission");
		map.put("permdesc", "Permission with [special] characters & symbols!");
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		
		assertEquals(500, permissionInfo.getId());
		assertEquals("b_special_permission", permissionInfo.getName());
		assertEquals("Permission with [special] characters & symbols!", permissionInfo.getDescription());
	}

	@Test
	public void permissionInfo_UnicodeInDescription() {
		Map<String, String> map = new HashMap<>();
		map.put("permid", "600");
		map.put("permname", "b_unicode_permission");
		map.put("permdesc", "Üñíçødé permission description 🔒");
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		
		assertEquals(600, permissionInfo.getId());
		assertEquals("b_unicode_permission", permissionInfo.getName());
		assertEquals("Üñíçødé permission description 🔒", permissionInfo.getDescription());
	}

	@Test
	public void permissionInfo_LongPermissionName() {
		Map<String, String> map = new HashMap<>();
		map.put("permid", "700");
		map.put("permname", "b_very_long_permission_name_that_describes_a_complex_functionality");
		map.put("permdesc", "A very detailed description of what this complex permission allows the user to do in the system");
		
		PermissionInfo permissionInfo = new PermissionInfo(map);
		
		assertEquals(700, permissionInfo.getId());
		assertEquals("b_very_long_permission_name_that_describes_a_complex_functionality", permissionInfo.getName());
		assertEquals("A very detailed description of what this complex permission allows the user to do in the system", permissionInfo.getDescription());
	}

	@Test
	public void permissionInfo_PermissionHierarchy() {
		// Test permissions that might be related in hierarchy
		
		// Parent permission
		Map<String, String> parentMap = new HashMap<>();
		parentMap.put("permid", "100");
		parentMap.put("permname", "b_client_modify");
		parentMap.put("permdesc", "Modify client properties");
		
		PermissionInfo parentPerm = new PermissionInfo(parentMap);
		assertEquals(100, parentPerm.getId());
		assertEquals("b_client_modify", parentPerm.getName());
		assertEquals("Modify client properties", parentPerm.getDescription());
		
		// Child permission
		Map<String, String> childMap = new HashMap<>();
		childMap.put("permid", "101");
		childMap.put("permname", "b_client_modify_own");
		childMap.put("permdesc", "Modify own client properties");
		
		PermissionInfo childPerm = new PermissionInfo(childMap);
		assertEquals(101, childPerm.getId());
		assertEquals("b_client_modify_own", childPerm.getName());
		assertEquals("Modify own client properties", childPerm.getDescription());
		
		// Verify they are different permissions
		assertNotEquals(parentPerm.getId(), childPerm.getId());
		assertNotEquals(parentPerm.getName(), childPerm.getName());
		assertNotEquals(parentPerm.getDescription(), childPerm.getDescription());
	}
}
