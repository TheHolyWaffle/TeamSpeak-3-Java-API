package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ServerGroupTest {

	@Test
	public void constructor_ValidMap() {
		Map<String, String> map = new HashMap<>();
		map.put("sgid", "6");
		map.put("name", "Server Admin");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertNotNull(serverGroup);
		assertEquals(6, serverGroup.getId());
		assertEquals("Server Admin", serverGroup.getName());
	}

	@Test
	public void getId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("sgid", "10");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(10, serverGroup.getId());
	}

	@Test
	public void getId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		map.put("sgid", "0");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(0, serverGroup.getId());
	}

	@Test
	public void getId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		map.put("sgid", "-1");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(-1, serverGroup.getId());
	}

	@Test
	public void getId_LargeId() {
		Map<String, String> map = new HashMap<>();
		map.put("sgid", String.valueOf(Integer.MAX_VALUE));
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(Integer.MAX_VALUE, serverGroup.getId());
	}

	@Test
	public void getName_ValidName() {
		Map<String, String> map = new HashMap<>();
		map.put("name", "Moderator");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals("Moderator", serverGroup.getName());
	}

	@Test
	public void getName_EmptyName() {
		Map<String, String> map = new HashMap<>();
		map.put("name", "");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals("", serverGroup.getName());
	}

	@Test
	public void getName_NullName() {
		Map<String, String> map = new HashMap<>();
		// name key not present
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals("", serverGroup.getName());
	}

	@Test
	public void getName_SpecialCharacters() {
		Map<String, String> map = new HashMap<>();
		map.put("name", "Admin [VIP] & Staff");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals("Admin [VIP] & Staff", serverGroup.getName());
	}

	@Test
	public void getType_Template() {
		Map<String, String> map = new HashMap<>();
		map.put("type", "0");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(PermissionGroupDatabaseType.TEMPLATE, serverGroup.getType());
	}

	@Test
	public void getType_Regular() {
		Map<String, String> map = new HashMap<>();
		map.put("type", "1");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(PermissionGroupDatabaseType.REGULAR, serverGroup.getType());
	}

	@Test
	public void getType_Query() {
		Map<String, String> map = new HashMap<>();
		map.put("type", "2");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(PermissionGroupDatabaseType.QUERY, serverGroup.getType());
	}

	@Test
	public void getType_Unknown() {
		Map<String, String> map = new HashMap<>();
		map.put("type", "999");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertNull(serverGroup.getType());
	}

	@Test
	public void getType_NullType() {
		Map<String, String> map = new HashMap<>();
		// type key not present, defaults to -1
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertNull(serverGroup.getType()); // -1 maps to null
	}

	@Test
	public void getIconId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("iconid", "12345");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(12345L, serverGroup.getIconId());
	}

	@Test
	public void getIconId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		map.put("iconid", "0");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(0L, serverGroup.getIconId());
	}

	@Test
	public void getIconId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		map.put("iconid", "-1");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(-1L, serverGroup.getIconId());
	}

	@Test
	public void getIconId_LargeId() {
		Map<String, String> map = new HashMap<>();
		map.put("iconid", String.valueOf(Long.MAX_VALUE));
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(Long.MAX_VALUE, serverGroup.getIconId());
	}

	@Test
	public void getSaveDb_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("savedb", "1");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(1, serverGroup.getSaveDb());
	}

	@Test
	public void getSaveDb_ZeroValue() {
		Map<String, String> map = new HashMap<>();
		map.put("savedb", "0");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(0, serverGroup.getSaveDb());
	}

	@Test
	public void getSaveDb_NegativeValue() {
		Map<String, String> map = new HashMap<>();
		map.put("savedb", "-1");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(-1, serverGroup.getSaveDb());
	}

	@Test
	public void getSortId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("sortid", "100");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(100, serverGroup.getSortId());
	}

	@Test
	public void getSortId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		map.put("sortid", "0");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(0, serverGroup.getSortId());
	}

	@Test
	public void getSortId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		map.put("sortid", "-1");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(-1, serverGroup.getSortId());
	}

	@Test
	public void getNameMode_ValidMode() {
		Map<String, String> map = new HashMap<>();
		map.put("namemode", "2");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(2, serverGroup.getNameMode());
	}

	@Test
	public void getNameMode_ZeroMode() {
		Map<String, String> map = new HashMap<>();
		map.put("namemode", "0");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(0, serverGroup.getNameMode());
	}

	@Test
	public void getModifyPower_ValidPower() {
		Map<String, String> map = new HashMap<>();
		map.put("n_modifyp", "75");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(75, serverGroup.getModifyPower());
	}

	@Test
	public void getModifyPower_ZeroPower() {
		Map<String, String> map = new HashMap<>();
		map.put("n_modifyp", "0");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(0, serverGroup.getModifyPower());
	}

	@Test
	public void getModifyPower_NegativePower() {
		Map<String, String> map = new HashMap<>();
		map.put("n_modifyp", "-1");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(-1, serverGroup.getModifyPower());
	}

	@Test
	public void getMemberAddPower_ValidPower() {
		Map<String, String> map = new HashMap<>();
		map.put("n_member_addp", "50");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(50, serverGroup.getMemberAddPower());
	}

	@Test
	public void getMemberAddPower_ZeroPower() {
		Map<String, String> map = new HashMap<>();
		map.put("n_member_addp", "0");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(0, serverGroup.getMemberAddPower());
	}

	@Test
	public void getMemberRemovePower_ValidPower() {
		Map<String, String> map = new HashMap<>();
		map.put("n_member_removep", "25");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(25, serverGroup.getMemberRemovePower());
	}

	@Test
	public void getMemberRemovePower_ZeroPower() {
		Map<String, String> map = new HashMap<>();
		map.put("n_member_removep", "0");
		
		ServerGroup serverGroup = new ServerGroup(map);
		assertEquals(0, serverGroup.getMemberRemovePower());
	}

	@Test
	public void serverGroup_CompleteData() {
		Map<String, String> map = new HashMap<>();
		map.put("sgid", "15");
		map.put("name", "Server Administrator");
		map.put("type", "1");
		map.put("iconid", "54321");
		map.put("savedb", "1");
		map.put("sortid", "200");
		map.put("namemode", "1");
		map.put("n_modifyp", "100");
		map.put("n_member_addp", "75");
		map.put("n_member_removep", "50");
		
		ServerGroup serverGroup = new ServerGroup(map);
		
		assertEquals(15, serverGroup.getId());
		assertEquals("Server Administrator", serverGroup.getName());
		assertEquals(PermissionGroupDatabaseType.REGULAR, serverGroup.getType());
		assertEquals(54321L, serverGroup.getIconId());
		assertEquals(1, serverGroup.getSaveDb());
		assertEquals(200, serverGroup.getSortId());
		assertEquals(1, serverGroup.getNameMode());
		assertEquals(100, serverGroup.getModifyPower());
		assertEquals(75, serverGroup.getMemberAddPower());
		assertEquals(50, serverGroup.getMemberRemovePower());
	}

	@Test
	public void serverGroup_EmptyMap() {
		Map<String, String> map = new HashMap<>();
		
		ServerGroup serverGroup = new ServerGroup(map);
		
		assertEquals(-1, serverGroup.getId()); // Default int value
		assertEquals("", serverGroup.getName());
		assertNull(serverGroup.getType()); // Default enum value for -1
		assertEquals(-1L, serverGroup.getIconId()); // Default long value
		assertEquals(-1, serverGroup.getSaveDb()); // Default int value
		assertEquals(-1, serverGroup.getSortId()); // Default int value
		assertEquals(-1, serverGroup.getNameMode()); // Default int value
		assertEquals(-1, serverGroup.getModifyPower()); // Default int value
		assertEquals(-1, serverGroup.getMemberAddPower()); // Default int value
		assertEquals(-1, serverGroup.getMemberRemovePower()); // Default int value
	}

	@Test
	public void serverGroup_PartialData() {
		Map<String, String> map = new HashMap<>();
		map.put("sgid", "5");
		map.put("name", "Guest");
		// Missing other fields
		
		ServerGroup serverGroup = new ServerGroup(map);
		
		assertEquals(5, serverGroup.getId());
		assertEquals("Guest", serverGroup.getName());
		assertNull(serverGroup.getType()); // Default enum value for -1
		assertEquals(-1L, serverGroup.getIconId()); // Default long value
		assertEquals(-1, serverGroup.getSaveDb()); // Default int value
		assertEquals(-1, serverGroup.getSortId()); // Default int value
		assertEquals(-1, serverGroup.getNameMode()); // Default int value
		assertEquals(-1, serverGroup.getModifyPower()); // Default int value
		assertEquals(-1, serverGroup.getMemberAddPower()); // Default int value
		assertEquals(-1, serverGroup.getMemberRemovePower()); // Default int value
	}

	@Test
	public void serverGroup_AllZeroValues() {
		Map<String, String> map = new HashMap<>();
		map.put("sgid", "0");
		map.put("type", "0");
		map.put("iconid", "0");
		map.put("savedb", "0");
		map.put("sortid", "0");
		map.put("namemode", "0");
		map.put("n_modifyp", "0");
		map.put("n_member_addp", "0");
		map.put("n_member_removep", "0");
		
		ServerGroup serverGroup = new ServerGroup(map);
		
		assertEquals(0, serverGroup.getId());
		assertEquals(PermissionGroupDatabaseType.TEMPLATE, serverGroup.getType());
		assertEquals(0L, serverGroup.getIconId());
		assertEquals(0, serverGroup.getSaveDb());
		assertEquals(0, serverGroup.getSortId());
		assertEquals(0, serverGroup.getNameMode());
		assertEquals(0, serverGroup.getModifyPower());
		assertEquals(0, serverGroup.getMemberAddPower());
		assertEquals(0, serverGroup.getMemberRemovePower());
	}

	@Test
	public void serverGroup_AllMaxValues() {
		Map<String, String> map = new HashMap<>();
		map.put("sgid", String.valueOf(Integer.MAX_VALUE));
		map.put("iconid", String.valueOf(Long.MAX_VALUE));
		map.put("savedb", String.valueOf(Integer.MAX_VALUE));
		map.put("sortid", String.valueOf(Integer.MAX_VALUE));
		map.put("namemode", String.valueOf(Integer.MAX_VALUE));
		map.put("n_modifyp", String.valueOf(Integer.MAX_VALUE));
		map.put("n_member_addp", String.valueOf(Integer.MAX_VALUE));
		map.put("n_member_removep", String.valueOf(Integer.MAX_VALUE));
		
		ServerGroup serverGroup = new ServerGroup(map);
		
		assertEquals(Integer.MAX_VALUE, serverGroup.getId());
		assertEquals(Long.MAX_VALUE, serverGroup.getIconId());
		assertEquals(Integer.MAX_VALUE, serverGroup.getSaveDb());
		assertEquals(Integer.MAX_VALUE, serverGroup.getSortId());
		assertEquals(Integer.MAX_VALUE, serverGroup.getNameMode());
		assertEquals(Integer.MAX_VALUE, serverGroup.getModifyPower());
		assertEquals(Integer.MAX_VALUE, serverGroup.getMemberAddPower());
		assertEquals(Integer.MAX_VALUE, serverGroup.getMemberRemovePower());
	}

	@Test
	public void serverGroup_InheritedWrapperMethods() {
		Map<String, String> map = new HashMap<>();
		map.put("sgid", "6");
		map.put("name", "Test Group");
		map.put("test_field", "test_value");
		
		ServerGroup serverGroup = new ServerGroup(map);
		
		// Test inherited methods from Wrapper
		assertEquals("6", serverGroup.get("sgid"));
		assertEquals("Test Group", serverGroup.get("name"));
		assertEquals("test_value", serverGroup.get("test_field"));
		assertEquals(6, serverGroup.getInt("sgid"));
	}

	@Test
	public void serverGroup_RealWorldScenarios() {
		// Test common server group scenarios
		
		// Admin group
		Map<String, String> adminMap = new HashMap<>();
		adminMap.put("sgid", "6");
		adminMap.put("name", "Server Admin");
		adminMap.put("type", "1");
		adminMap.put("iconid", "300");
		adminMap.put("savedb", "1");
		adminMap.put("sortid", "0");
		adminMap.put("namemode", "0");
		adminMap.put("n_modifyp", "100");
		adminMap.put("n_member_addp", "100");
		adminMap.put("n_member_removep", "100");
		
		ServerGroup adminGroup = new ServerGroup(adminMap);
		assertEquals(6, adminGroup.getId());
		assertEquals("Server Admin", adminGroup.getName());
		assertEquals(PermissionGroupDatabaseType.REGULAR, adminGroup.getType());
		assertEquals(300L, adminGroup.getIconId());
		assertEquals(1, adminGroup.getSaveDb());
		assertEquals(0, adminGroup.getSortId());
		assertEquals(0, adminGroup.getNameMode());
		assertEquals(100, adminGroup.getModifyPower());
		assertEquals(100, adminGroup.getMemberAddPower());
		assertEquals(100, adminGroup.getMemberRemovePower());
		
		// Guest group
		Map<String, String> guestMap = new HashMap<>();
		guestMap.put("sgid", "8");
		guestMap.put("name", "Guest");
		guestMap.put("type", "1");
		guestMap.put("iconid", "0");
		guestMap.put("savedb", "1");
		guestMap.put("sortid", "0");
		guestMap.put("namemode", "0");
		guestMap.put("n_modifyp", "0");
		guestMap.put("n_member_addp", "0");
		guestMap.put("n_member_removep", "0");
		
		ServerGroup guestGroup = new ServerGroup(guestMap);
		assertEquals(8, guestGroup.getId());
		assertEquals("Guest", guestGroup.getName());
		assertEquals(PermissionGroupDatabaseType.REGULAR, guestGroup.getType());
		assertEquals(0L, guestGroup.getIconId());
		assertEquals(1, guestGroup.getSaveDb());
		assertEquals(0, guestGroup.getSortId());
		assertEquals(0, guestGroup.getNameMode());
		assertEquals(0, guestGroup.getModifyPower());
		assertEquals(0, guestGroup.getMemberAddPower());
		assertEquals(0, guestGroup.getMemberRemovePower());
	}
}
