package com.github.theholywaffle.teamspeak3.api.wrapper;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PermissionTest {

	@Test
	public void constructor_ValidMap() {
		Map<String, String> map = new HashMap<>();
		map.put("permsid", "b_client_talk_power");
		map.put("permvalue", "50");
		
		Permission permission = new Permission(map);
		assertNotNull(permission);
		assertEquals("b_client_talk_power", permission.getName());
		assertEquals(50, permission.getValue());
	}

	@Test
	public void getName_BooleanPermission() {
		Map<String, String> map = new HashMap<>();
		map.put("permsid", "b_client_is_talker");
		
		Permission permission = new Permission(map);
		assertEquals("b_client_is_talker", permission.getName());
	}

	@Test
	public void getName_IntegerPermission() {
		Map<String, String> map = new HashMap<>();
		map.put("permsid", "i_client_talk_power");
		
		Permission permission = new Permission(map);
		assertEquals("i_client_talk_power", permission.getName());
	}

	@Test
	public void getName_EmptyName() {
		Map<String, String> map = new HashMap<>();
		map.put("permsid", "");
		
		Permission permission = new Permission(map);
		assertEquals("", permission.getName());
	}

	@Test
	public void getName_NullName() {
		Map<String, String> map = new HashMap<>();
		// permsid key not present
		
		Permission permission = new Permission(map);
		assertEquals("", permission.getName());
	}

	@Test
	public void getName_ComplexPermissionName() {
		Map<String, String> map = new HashMap<>();
		map.put("permsid", "i_channel_needed_talk_power");
		
		Permission permission = new Permission(map);
		assertEquals("i_channel_needed_talk_power", permission.getName());
	}

	@Test
	public void getValue_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("permvalue", "75");
		
		Permission permission = new Permission(map);
		assertEquals(75, permission.getValue());
	}

	@Test
	public void getValue_ZeroValue() {
		Map<String, String> map = new HashMap<>();
		map.put("permvalue", "0");
		
		Permission permission = new Permission(map);
		assertEquals(0, permission.getValue());
	}

	@Test
	public void getValue_NegativeValue() {
		Map<String, String> map = new HashMap<>();
		map.put("permvalue", "-1");
		
		Permission permission = new Permission(map);
		assertEquals(-1, permission.getValue());
	}

	@Test
	public void getValue_MaxValue() {
		Map<String, String> map = new HashMap<>();
		map.put("permvalue", "100");
		
		Permission permission = new Permission(map);
		assertEquals(100, permission.getValue());
	}

	@Test
	public void getValue_LargeValue() {
		Map<String, String> map = new HashMap<>();
		map.put("permvalue", String.valueOf(Integer.MAX_VALUE));
		
		Permission permission = new Permission(map);
		assertEquals(Integer.MAX_VALUE, permission.getValue());
	}

	@Test
	public void getValue_BooleanTrue() {
		Map<String, String> map = new HashMap<>();
		map.put("permvalue", "1");
		
		Permission permission = new Permission(map);
		assertEquals(1, permission.getValue());
	}

	@Test
	public void getValue_BooleanFalse() {
		Map<String, String> map = new HashMap<>();
		map.put("permvalue", "0");
		
		Permission permission = new Permission(map);
		assertEquals(0, permission.getValue());
	}

	@Test
	public void isNegated_True() {
		Map<String, String> map = new HashMap<>();
		map.put("permnegated", "1");
		
		Permission permission = new Permission(map);
		assertTrue(permission.isNegated());
	}

	@Test
	public void isNegated_False() {
		Map<String, String> map = new HashMap<>();
		map.put("permnegated", "0");
		
		Permission permission = new Permission(map);
		assertFalse(permission.isNegated());
	}

	@Test
	public void isNegated_DefaultValue() {
		Map<String, String> map = new HashMap<>();
		// permnegated key not present
		
		Permission permission = new Permission(map);
		assertFalse(permission.isNegated()); // Default boolean value
	}

	@Test
	public void isSkipped_True() {
		Map<String, String> map = new HashMap<>();
		map.put("permskip", "1");
		
		Permission permission = new Permission(map);
		assertTrue(permission.isSkipped());
	}

	@Test
	public void isSkipped_False() {
		Map<String, String> map = new HashMap<>();
		map.put("permskip", "0");
		
		Permission permission = new Permission(map);
		assertFalse(permission.isSkipped());
	}

	@Test
	public void isSkipped_DefaultValue() {
		Map<String, String> map = new HashMap<>();
		// permskip key not present
		
		Permission permission = new Permission(map);
		assertFalse(permission.isSkipped()); // Default boolean value
	}

	@Test
	public void permission_BooleanPermissionComplete() {
		Map<String, String> map = new HashMap<>();
		map.put("permsid", "b_client_is_channel_commander");
		map.put("permvalue", "1");
		map.put("permnegated", "0");
		map.put("permskip", "1");
		
		Permission permission = new Permission(map);
		
		assertEquals("b_client_is_channel_commander", permission.getName());
		assertEquals(1, permission.getValue());
		assertFalse(permission.isNegated());
		assertTrue(permission.isSkipped());
	}

	@Test
	public void permission_IntegerPermissionComplete() {
		Map<String, String> map = new HashMap<>();
		map.put("permsid", "i_client_talk_power");
		map.put("permvalue", "50");
		map.put("permnegated", "1");
		map.put("permskip", "0");
		
		Permission permission = new Permission(map);
		
		assertEquals("i_client_talk_power", permission.getName());
		assertEquals(50, permission.getValue());
		assertTrue(permission.isNegated());
		assertFalse(permission.isSkipped());
	}

	@Test
	public void permission_EmptyMap() {
		Map<String, String> map = new HashMap<>();
		
		Permission permission = new Permission(map);
		
		assertEquals("", permission.getName());
		assertEquals(-1, permission.getValue()); // Default int value
		assertFalse(permission.isNegated()); // Default boolean value
		assertFalse(permission.isSkipped()); // Default boolean value
	}

	@Test
	public void permission_PartialData() {
		Map<String, String> map = new HashMap<>();
		map.put("permsid", "b_client_use_priority_speaker");
		map.put("permvalue", "1");
		// Missing permnegated and permskip
		
		Permission permission = new Permission(map);
		
		assertEquals("b_client_use_priority_speaker", permission.getName());
		assertEquals(1, permission.getValue());
		assertFalse(permission.isNegated()); // Default boolean value
		assertFalse(permission.isSkipped()); // Default boolean value
	}

	@Test
	public void permission_AllTrueFlags() {
		Map<String, String> map = new HashMap<>();
		map.put("permsid", "i_channel_needed_talk_power");
		map.put("permvalue", "25");
		map.put("permnegated", "1");
		map.put("permskip", "1");
		
		Permission permission = new Permission(map);
		
		assertEquals("i_channel_needed_talk_power", permission.getName());
		assertEquals(25, permission.getValue());
		assertTrue(permission.isNegated());
		assertTrue(permission.isSkipped());
	}

	@Test
	public void permission_AllFalseFlags() {
		Map<String, String> map = new HashMap<>();
		map.put("permsid", "b_client_server_textmessage_send");
		map.put("permvalue", "0");
		map.put("permnegated", "0");
		map.put("permskip", "0");
		
		Permission permission = new Permission(map);
		
		assertEquals("b_client_server_textmessage_send", permission.getName());
		assertEquals(0, permission.getValue());
		assertFalse(permission.isNegated());
		assertFalse(permission.isSkipped());
	}

	@Test
	public void permission_ServerGroupPermission() {
		Map<String, String> map = new HashMap<>();
		map.put("permsid", "i_group_modify_power");
		map.put("permvalue", "75");
		map.put("permnegated", "0");
		map.put("permskip", "1"); // Server group permission can be skipped
		
		Permission permission = new Permission(map);
		
		assertEquals("i_group_modify_power", permission.getName());
		assertEquals(75, permission.getValue());
		assertFalse(permission.isNegated());
		assertTrue(permission.isSkipped());
	}

	@Test
	public void permission_ChannelGroupPermission() {
		Map<String, String> map = new HashMap<>();
		map.put("permsid", "i_channel_needed_modify_power");
		map.put("permvalue", "50");
		map.put("permnegated", "1");
		map.put("permskip", "0"); // Channel group permissions are typically not skipped
		
		Permission permission = new Permission(map);
		
		assertEquals("i_channel_needed_modify_power", permission.getName());
		assertEquals(50, permission.getValue());
		assertTrue(permission.isNegated());
		assertFalse(permission.isSkipped());
	}

	@Test
	public void permission_ClientPermission() {
		Map<String, String> map = new HashMap<>();
		map.put("permsid", "b_client_ignore_antiflood");
		map.put("permvalue", "1");
		map.put("permnegated", "0");
		map.put("permskip", "1"); // Client permission can be skipped
		
		Permission permission = new Permission(map);
		
		assertEquals("b_client_ignore_antiflood", permission.getName());
		assertEquals(1, permission.getValue());
		assertFalse(permission.isNegated());
		assertTrue(permission.isSkipped());
	}

	@Test
	public void permission_InheritedWrapperMethods() {
		Map<String, String> map = new HashMap<>();
		map.put("permsid", "i_client_talk_power");
		map.put("permvalue", "50");
		map.put("test_field", "test_value");
		
		Permission permission = new Permission(map);
		
		// Test inherited methods from Wrapper
		assertEquals("i_client_talk_power", permission.get("permsid"));
		assertEquals("50", permission.get("permvalue"));
		assertEquals("test_value", permission.get("test_field"));
		assertEquals(50, permission.getInt("permvalue"));
	}

	@Test
	public void permission_RealWorldScenarios() {
		// Test common permission scenarios
		
		// Admin permission
		Map<String, String> adminMap = new HashMap<>();
		adminMap.put("permsid", "b_serverinstance_modify_settings");
		adminMap.put("permvalue", "1");
		adminMap.put("permnegated", "0");
		adminMap.put("permskip", "0");
		
		Permission adminPermission = new Permission(adminMap);
		assertEquals("b_serverinstance_modify_settings", adminPermission.getName());
		assertEquals(1, adminPermission.getValue());
		assertFalse(adminPermission.isNegated());
		assertFalse(adminPermission.isSkipped());
		
		// Talk power permission
		Map<String, String> talkMap = new HashMap<>();
		talkMap.put("permsid", "i_client_needed_talk_power");
		talkMap.put("permvalue", "0");
		talkMap.put("permnegated", "0");
		talkMap.put("permskip", "0");
		
		Permission talkPermission = new Permission(talkMap);
		assertEquals("i_client_needed_talk_power", talkPermission.getName());
		assertEquals(0, talkPermission.getValue());
		assertFalse(talkPermission.isNegated());
		assertFalse(talkPermission.isSkipped());
	}
}
