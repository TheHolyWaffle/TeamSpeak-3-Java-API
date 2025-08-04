package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ChannelGroupTest {

	@Test
	public void constructor_ValidMap() {
		Map<String, String> map = new HashMap<>();
		map.put("cgid", "5");
		map.put("name", "Channel Admin");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertNotNull(channelGroup);
		assertEquals(5, channelGroup.getId());
		assertEquals("Channel Admin", channelGroup.getName());
	}

	@Test
	public void getId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("cgid", "10");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(10, channelGroup.getId());
	}

	@Test
	public void getId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		map.put("cgid", "0");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(0, channelGroup.getId());
	}

	@Test
	public void getId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		map.put("cgid", "-1");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(-1, channelGroup.getId());
	}

	@Test
	public void getName_ValidName() {
		Map<String, String> map = new HashMap<>();
		map.put("name", "Moderator");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals("Moderator", channelGroup.getName());
	}

	@Test
	public void getName_EmptyName() {
		Map<String, String> map = new HashMap<>();
		map.put("name", "");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals("", channelGroup.getName());
	}

	@Test
	public void getName_NullName() {
		Map<String, String> map = new HashMap<>();
		// name key not present

		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals("", channelGroup.getName());
	}

	@Test
	public void getType_Template() {
		Map<String, String> map = new HashMap<>();
		map.put("type", "0");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(PermissionGroupDatabaseType.TEMPLATE, channelGroup.getType());
	}

	@Test
	public void getType_Regular() {
		Map<String, String> map = new HashMap<>();
		map.put("type", "1");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(PermissionGroupDatabaseType.REGULAR, channelGroup.getType());
	}

	@Test
	public void getType_Query() {
		Map<String, String> map = new HashMap<>();
		map.put("type", "2");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(PermissionGroupDatabaseType.QUERY, channelGroup.getType());
	}

	@Test
	public void getType_Unknown() {
		Map<String, String> map = new HashMap<>();
		map.put("type", "999");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertNull(channelGroup.getType());
	}

	@Test
	public void getType_NullType() {
		Map<String, String> map = new HashMap<>();
		// type key not present, defaults to -1

		ChannelGroup channelGroup = new ChannelGroup(map);
		assertNull(channelGroup.getType()); // -1 maps to null
	}

	@Test
	public void getIconId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("iconid", "12345");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(12345L, channelGroup.getIconId());
	}

	@Test
	public void getIconId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		map.put("iconid", "0");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(0L, channelGroup.getIconId());
	}

	@Test
	public void getIconId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		map.put("iconid", "-1");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(-1L, channelGroup.getIconId());
	}

	@Test
	public void isSavedInDatabase_True() {
		Map<String, String> map = new HashMap<>();
		map.put("savedb", "1");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertTrue(channelGroup.isSavedInDatabase());
	}

	@Test
	public void isSavedInDatabase_False() {
		Map<String, String> map = new HashMap<>();
		map.put("savedb", "0");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertFalse(channelGroup.isSavedInDatabase());
	}

	@Test
	public void getSortId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("sortid", "100");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(100, channelGroup.getSortId());
	}

	@Test
	public void getSortId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		map.put("sortid", "0");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(0, channelGroup.getSortId());
	}

	@Test
	public void getSortId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		map.put("sortid", "-1");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(-1, channelGroup.getSortId());
	}

	@Test
	public void getNameMode_ValidMode() {
		Map<String, String> map = new HashMap<>();
		map.put("namemode", "2");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(2, channelGroup.getNameMode());
	}

	@Test
	public void getNameMode_ZeroMode() {
		Map<String, String> map = new HashMap<>();
		map.put("namemode", "0");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(0, channelGroup.getNameMode());
	}

	@Test
	public void getModifyPower_ValidPower() {
		Map<String, String> map = new HashMap<>();
		map.put("n_modifyp", "75");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(75, channelGroup.getModifyPower());
	}

	@Test
	public void getModifyPower_ZeroPower() {
		Map<String, String> map = new HashMap<>();
		map.put("n_modifyp", "0");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(0, channelGroup.getModifyPower());
	}

	@Test
	public void getModifyPower_NegativePower() {
		Map<String, String> map = new HashMap<>();
		map.put("n_modifyp", "-1");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(-1, channelGroup.getModifyPower());
	}

	@Test
	public void getMemberAddPower_ValidPower() {
		Map<String, String> map = new HashMap<>();
		map.put("n_member_addp", "50");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(50, channelGroup.getMemberAddPower());
	}

	@Test
	public void getMemberAddPower_ZeroPower() {
		Map<String, String> map = new HashMap<>();
		map.put("n_member_addp", "0");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(0, channelGroup.getMemberAddPower());
	}

	@Test
	public void getMemberRemovePower_ValidPower() {
		Map<String, String> map = new HashMap<>();
		map.put("n_member_removep", "25");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(25, channelGroup.getMemberRemovePower());
	}

	@Test
	public void getMemberRemovePower_ZeroPower() {
		Map<String, String> map = new HashMap<>();
		map.put("n_member_removep", "0");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals(0, channelGroup.getMemberRemovePower());
	}

	@Test
	public void channelGroup_CompleteData() {
		Map<String, String> map = new HashMap<>();
		map.put("cgid", "15");
		map.put("name", "Channel Administrator");
		map.put("type", "1");
		map.put("iconid", "54321");
		map.put("savedb", "1");
		map.put("sortid", "200");
		map.put("namemode", "1");
		map.put("n_modifyp", "100");
		map.put("n_member_addp", "75");
		map.put("n_member_removep", "50");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		
		assertEquals(15, channelGroup.getId());
		assertEquals("Channel Administrator", channelGroup.getName());
		assertEquals(PermissionGroupDatabaseType.REGULAR, channelGroup.getType());
		assertEquals(54321L, channelGroup.getIconId());
		assertTrue(channelGroup.isSavedInDatabase());
		assertEquals(200, channelGroup.getSortId());
		assertEquals(1, channelGroup.getNameMode());
		assertEquals(100, channelGroup.getModifyPower());
		assertEquals(75, channelGroup.getMemberAddPower());
		assertEquals(50, channelGroup.getMemberRemovePower());
	}

	@Test
	public void channelGroup_EmptyMap() {
		Map<String, String> map = new HashMap<>();
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		
		assertEquals(-1, channelGroup.getId()); // Default int value
		assertEquals("", channelGroup.getName());
		assertNull(channelGroup.getType()); // Default enum value for -1
		assertEquals(-1L, channelGroup.getIconId()); // Default long value
		assertFalse(channelGroup.isSavedInDatabase()); // Default boolean value
		assertEquals(-1, channelGroup.getSortId()); // Default int value
		assertEquals(-1, channelGroup.getNameMode()); // Default int value
		assertEquals(-1, channelGroup.getModifyPower()); // Default int value
		assertEquals(-1, channelGroup.getMemberAddPower()); // Default int value
		assertEquals(-1, channelGroup.getMemberRemovePower()); // Default int value
	}

	@Test
	public void channelGroup_SpecialCharactersInName() {
		Map<String, String> map = new HashMap<>();
		map.put("name", "Admin [VIP] & Moderator");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		assertEquals("Admin [VIP] & Moderator", channelGroup.getName());
	}

	@Test
	public void channelGroup_LargeValues() {
		Map<String, String> map = new HashMap<>();
		map.put("cgid", String.valueOf(Integer.MAX_VALUE));
		map.put("iconid", String.valueOf(Long.MAX_VALUE));
		map.put("sortid", String.valueOf(Integer.MAX_VALUE));
		map.put("n_modifyp", String.valueOf(Integer.MAX_VALUE));
		map.put("n_member_addp", String.valueOf(Integer.MAX_VALUE));
		map.put("n_member_removep", String.valueOf(Integer.MAX_VALUE));
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		
		assertEquals(Integer.MAX_VALUE, channelGroup.getId());
		assertEquals(Long.MAX_VALUE, channelGroup.getIconId());
		assertEquals(Integer.MAX_VALUE, channelGroup.getSortId());
		assertEquals(Integer.MAX_VALUE, channelGroup.getModifyPower());
		assertEquals(Integer.MAX_VALUE, channelGroup.getMemberAddPower());
		assertEquals(Integer.MAX_VALUE, channelGroup.getMemberRemovePower());
	}

	@Test
	public void channelGroup_InheritedWrapperMethods() {
		Map<String, String> map = new HashMap<>();
		map.put("cgid", "5");
		map.put("name", "Test Group");
		map.put("test_field", "test_value");
		
		ChannelGroup channelGroup = new ChannelGroup(map);
		
		// Test inherited methods from Wrapper
		assertEquals("5", channelGroup.get("cgid"));
		assertEquals("Test Group", channelGroup.get("name"));
		assertEquals("test_value", channelGroup.get("test_field"));
		assertEquals(5, channelGroup.getInt("cgid"));
	}
}
