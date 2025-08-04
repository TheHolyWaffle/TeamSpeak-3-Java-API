package com.github.theholywaffle.teamspeak3.api.wrapper;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseClientTest {

	@Test
	public void constructor_ValidMap() {
		Map<String, String> map = new HashMap<>();
		map.put("cldbid", "100");
		map.put("client_nickname", "TestUser");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertNotNull(databaseClient);
		assertEquals(100, databaseClient.getDatabaseId());
		assertEquals("TestUser", databaseClient.getNickname());
	}

	@Test
	public void getDatabaseId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("cldbid", "200");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals(200, databaseClient.getDatabaseId());
	}

	@Test
	public void getDatabaseId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		map.put("cldbid", "0");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals(0, databaseClient.getDatabaseId());
	}

	@Test
	public void getDatabaseId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		map.put("cldbid", "-1");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals(-1, databaseClient.getDatabaseId());
	}

	@Test
	public void getDatabaseId_LargeId() {
		Map<String, String> map = new HashMap<>();
		map.put("cldbid", String.valueOf(Integer.MAX_VALUE));
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals(Integer.MAX_VALUE, databaseClient.getDatabaseId());
	}

	@Test
	public void getUniqueIdentifier_ValidUID() {
		Map<String, String> map = new HashMap<>();
		map.put("client_unique_identifier", "unique123456");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals("unique123456", databaseClient.getUniqueIdentifier());
	}

	@Test
	public void getUniqueIdentifier_EmptyUID() {
		Map<String, String> map = new HashMap<>();
		map.put("client_unique_identifier", "");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals("", databaseClient.getUniqueIdentifier());
	}

	@Test
	public void getUniqueIdentifier_NullUID() {
		Map<String, String> map = new HashMap<>();
		// client_unique_identifier key not present
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals("", databaseClient.getUniqueIdentifier());
	}

	@Test
	public void getNickname_ValidNickname() {
		Map<String, String> map = new HashMap<>();
		map.put("client_nickname", "TestUser");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals("TestUser", databaseClient.getNickname());
	}

	@Test
	public void getNickname_EmptyNickname() {
		Map<String, String> map = new HashMap<>();
		map.put("client_nickname", "");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals("", databaseClient.getNickname());
	}

	@Test
	public void getNickname_NullNickname() {
		Map<String, String> map = new HashMap<>();
		// client_nickname key not present
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals("", databaseClient.getNickname());
	}

	@Test
	public void getNickname_SpecialCharacters() {
		Map<String, String> map = new HashMap<>();
		map.put("client_nickname", "User [VIP] & Admin");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals("User [VIP] & Admin", databaseClient.getNickname());
	}

	@Test
	public void getCreatedDate_ValidTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("client_created", "1609459200"); // 2021-01-01 00:00:00 UTC
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		Date expectedDate = new Date(1609459200L * 1000);
		assertEquals(expectedDate, databaseClient.getCreatedDate());
	}

	@Test
	public void getCreatedDate_ZeroTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("client_created", "0");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		Date expectedDate = new Date(0);
		assertEquals(expectedDate, databaseClient.getCreatedDate());
	}

	@Test
	public void getCreatedDate_NegativeTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("client_created", "-1");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		Date expectedDate = new Date(-1000);
		assertEquals(expectedDate, databaseClient.getCreatedDate());
	}

	@Test
	public void getCreatedDate_LargeTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("client_created", "2147483647"); // Max int value
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		Date expectedDate = new Date(2147483647L * 1000);
		assertEquals(expectedDate, databaseClient.getCreatedDate());
	}

	@Test
	public void getLastConnectedDate_ValidTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("client_lastconnected", "1640995200"); // 2022-01-01 00:00:00 UTC
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		Date expectedDate = new Date(1640995200L * 1000);
		assertEquals(expectedDate, databaseClient.getLastConnectedDate());
	}

	@Test
	public void getLastConnectedDate_ZeroTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("client_lastconnected", "0");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		Date expectedDate = new Date(0);
		assertEquals(expectedDate, databaseClient.getLastConnectedDate());
	}

	@Test
	public void getLastConnectedDate_NegativeTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("client_lastconnected", "-1");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		Date expectedDate = new Date(-1000);
		assertEquals(expectedDate, databaseClient.getLastConnectedDate());
	}

	@Test
	public void getTotalConnections_ValidCount() {
		Map<String, String> map = new HashMap<>();
		map.put("client_totalconnections", "50");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals(50, databaseClient.getTotalConnections());
	}

	@Test
	public void getTotalConnections_ZeroCount() {
		Map<String, String> map = new HashMap<>();
		map.put("client_totalconnections", "0");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals(0, databaseClient.getTotalConnections());
	}

	@Test
	public void getTotalConnections_NegativeCount() {
		Map<String, String> map = new HashMap<>();
		map.put("client_totalconnections", "-1");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals(-1, databaseClient.getTotalConnections());
	}

	@Test
	public void getTotalConnections_LargeCount() {
		Map<String, String> map = new HashMap<>();
		map.put("client_totalconnections", String.valueOf(Integer.MAX_VALUE));
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals(Integer.MAX_VALUE, databaseClient.getTotalConnections());
	}

	@Test
	public void getDescription_ValidDescription() {
		Map<String, String> map = new HashMap<>();
		map.put("client_description", "Regular user");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals("Regular user", databaseClient.getDescription());
	}

	@Test
	public void getDescription_EmptyDescription() {
		Map<String, String> map = new HashMap<>();
		map.put("client_description", "");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals("", databaseClient.getDescription());
	}

	@Test
	public void getDescription_NullDescription() {
		Map<String, String> map = new HashMap<>();
		// client_description key not present
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals("", databaseClient.getDescription());
	}

	@Test
	public void getDescription_LongDescription() {
		Map<String, String> map = new HashMap<>();
		String longDescription = "This is a very long description that contains detailed information about the user including their role, permissions, and other relevant details.";
		map.put("client_description", longDescription);
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals(longDescription, databaseClient.getDescription());
	}

	@Test
	public void getLastIp_ValidIp() {
		Map<String, String> map = new HashMap<>();
		map.put("client_lastip", "192.168.1.100");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals("192.168.1.100", databaseClient.getLastIp());
	}

	@Test
	public void getLastIp_IPv6Address() {
		Map<String, String> map = new HashMap<>();
		map.put("client_lastip", "2001:0db8:85a3:0000:0000:8a2e:0370:7334");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", databaseClient.getLastIp());
	}

	@Test
	public void getLastIp_LocalhostIp() {
		Map<String, String> map = new HashMap<>();
		map.put("client_lastip", "127.0.0.1");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals("127.0.0.1", databaseClient.getLastIp());
	}

	@Test
	public void getLastIp_EmptyIp() {
		Map<String, String> map = new HashMap<>();
		map.put("client_lastip", "");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals("", databaseClient.getLastIp());
	}

	@Test
	public void getLastIp_NullIp() {
		Map<String, String> map = new HashMap<>();
		// client_lastip key not present
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		assertEquals("", databaseClient.getLastIp());
	}

	@Test
	public void databaseClient_CompleteData() {
		Map<String, String> map = new HashMap<>();
		map.put("cldbid", "100");
		map.put("client_unique_identifier", "unique123456");
		map.put("client_nickname", "TestUser");
		map.put("client_created", "1609459200");
		map.put("client_lastconnected", "1640995200");
		map.put("client_totalconnections", "25");
		map.put("client_description", "VIP User");
		map.put("client_lastip", "10.0.0.1");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		
		assertEquals(100, databaseClient.getDatabaseId());
		assertEquals("unique123456", databaseClient.getUniqueIdentifier());
		assertEquals("TestUser", databaseClient.getNickname());
		assertEquals(new Date(1609459200L * 1000), databaseClient.getCreatedDate());
		assertEquals(new Date(1640995200L * 1000), databaseClient.getLastConnectedDate());
		assertEquals(25, databaseClient.getTotalConnections());
		assertEquals("VIP User", databaseClient.getDescription());
		assertEquals("10.0.0.1", databaseClient.getLastIp());
	}

	@Test
	public void databaseClient_EmptyMap() {
		Map<String, String> map = new HashMap<>();
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		
		assertEquals(-1, databaseClient.getDatabaseId()); // Default int value
		assertEquals("", databaseClient.getUniqueIdentifier());
		assertEquals("", databaseClient.getNickname());
		assertEquals(new Date(-1000), databaseClient.getCreatedDate()); // Default long value * 1000
		assertEquals(new Date(-1000), databaseClient.getLastConnectedDate()); // Default long value * 1000
		assertEquals(-1, databaseClient.getTotalConnections()); // Default int value
		assertEquals("", databaseClient.getDescription());
		assertEquals("", databaseClient.getLastIp());
	}

	@Test
	public void databaseClient_PartialData() {
		Map<String, String> map = new HashMap<>();
		map.put("cldbid", "50");
		map.put("client_nickname", "PartialUser");
		// Missing other fields
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		
		assertEquals(50, databaseClient.getDatabaseId());
		assertEquals("", databaseClient.getUniqueIdentifier()); // Default string value
		assertEquals("PartialUser", databaseClient.getNickname());
		assertEquals(new Date(-1000), databaseClient.getCreatedDate()); // Default long value * 1000
		assertEquals(new Date(-1000), databaseClient.getLastConnectedDate()); // Default long value * 1000
		assertEquals(-1, databaseClient.getTotalConnections()); // Default int value
		assertEquals("", databaseClient.getDescription()); // Default string value
		assertEquals("", databaseClient.getLastIp()); // Default string value
	}

	@Test
	public void databaseClient_AllZeroValues() {
		Map<String, String> map = new HashMap<>();
		map.put("cldbid", "0");
		map.put("client_created", "0");
		map.put("client_lastconnected", "0");
		map.put("client_totalconnections", "0");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		
		assertEquals(0, databaseClient.getDatabaseId());
		assertEquals(new Date(0), databaseClient.getCreatedDate());
		assertEquals(new Date(0), databaseClient.getLastConnectedDate());
		assertEquals(0, databaseClient.getTotalConnections());
	}

	@Test
	public void databaseClient_AllNegativeValues() {
		Map<String, String> map = new HashMap<>();
		map.put("cldbid", "-5");
		map.put("client_created", "-1");
		map.put("client_lastconnected", "-1");
		map.put("client_totalconnections", "-10");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		
		assertEquals(-5, databaseClient.getDatabaseId());
		assertEquals(new Date(-1000), databaseClient.getCreatedDate());
		assertEquals(new Date(-1000), databaseClient.getLastConnectedDate());
		assertEquals(-10, databaseClient.getTotalConnections());
	}

	@Test
	public void databaseClient_AllMaxValues() {
		Map<String, String> map = new HashMap<>();
		map.put("cldbid", String.valueOf(Integer.MAX_VALUE));
		map.put("client_created", String.valueOf(Long.MAX_VALUE));
		map.put("client_lastconnected", String.valueOf(Long.MAX_VALUE));
		map.put("client_totalconnections", String.valueOf(Integer.MAX_VALUE));
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		
		assertEquals(Integer.MAX_VALUE, databaseClient.getDatabaseId());
		assertEquals(new Date(Long.MAX_VALUE * 1000), databaseClient.getCreatedDate());
		assertEquals(new Date(Long.MAX_VALUE * 1000), databaseClient.getLastConnectedDate());
		assertEquals(Integer.MAX_VALUE, databaseClient.getTotalConnections());
	}

	@Test
	public void databaseClient_InheritedWrapperMethods() {
		Map<String, String> map = new HashMap<>();
		map.put("cldbid", "100");
		map.put("client_nickname", "TestUser");
		map.put("test_field", "test_value");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		
		// Test inherited methods from Wrapper
		assertEquals("100", databaseClient.get("cldbid"));
		assertEquals("TestUser", databaseClient.get("client_nickname"));
		assertEquals("test_value", databaseClient.get("test_field"));
		assertEquals(100, databaseClient.getInt("cldbid"));
	}

	@Test
	public void databaseClient_RealWorldScenario() {
		// Simulate a real-world database client scenario
		Map<String, String> map = new HashMap<>();
		map.put("cldbid", "42");
		map.put("client_unique_identifier", "ServerAdmin=");
		map.put("client_nickname", "ServerAdmin");
		map.put("client_created", "1577836800"); // 2020-01-01 00:00:00 UTC
		map.put("client_lastconnected", "1640995200"); // 2022-01-01 00:00:00 UTC
		map.put("client_totalconnections", "150");
		map.put("client_description", "Server Administrator - Full Access");
		map.put("client_lastip", "192.168.1.10");
		
		DatabaseClient databaseClient = new DatabaseClient(map);
		
		assertEquals(42, databaseClient.getDatabaseId());
		assertEquals("ServerAdmin=", databaseClient.getUniqueIdentifier());
		assertEquals("ServerAdmin", databaseClient.getNickname());
		assertEquals(new Date(1577836800L * 1000), databaseClient.getCreatedDate());
		assertEquals(new Date(1640995200L * 1000), databaseClient.getLastConnectedDate());
		assertEquals(150, databaseClient.getTotalConnections());
		assertEquals("Server Administrator - Full Access", databaseClient.getDescription());
		assertEquals("192.168.1.10", databaseClient.getLastIp());
	}
}
