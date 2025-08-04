package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseClientInfoTest {

	@Test
	public void constructor_ValidMap() {
		Map<String, String> map = new HashMap<>();
		map.put("client_database_id", "100");
		map.put("client_nickname", "TestUser");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertNotNull(clientInfo);
		assertEquals(100, clientInfo.getDatabaseId());
		assertEquals("TestUser", clientInfo.getNickname());
	}

	@Test
	public void getDatabaseId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("client_database_id", "200");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals(200, clientInfo.getDatabaseId());
	}

	@Test
	public void getDatabaseId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		map.put("client_database_id", "0");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals(0, clientInfo.getDatabaseId());
	}

	@Test
	public void getDatabaseId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		map.put("client_database_id", "-1");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals(-1, clientInfo.getDatabaseId());
	}

	@Test
	public void getAvatar_ValidAvatar() {
		Map<String, String> map = new HashMap<>();
		map.put("client_flag_avatar", "avatar123");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals("avatar123", clientInfo.getAvatar());
	}

	@Test
	public void getAvatar_EmptyAvatar() {
		Map<String, String> map = new HashMap<>();
		map.put("client_flag_avatar", "");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals("", clientInfo.getAvatar());
	}

	@Test
	public void getAvatar_NullAvatar() {
		Map<String, String> map = new HashMap<>();
		// client_flag_avatar key not present
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals("", clientInfo.getAvatar());
	}

	@Test
	public void getMonthlyBytesUploaded_ValidBytes() {
		Map<String, String> map = new HashMap<>();
		map.put("client_month_bytes_uploaded", "1048576"); // 1 MB
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals(1048576L, clientInfo.getMonthlyBytesUploaded());
	}

	@Test
	public void getMonthlyBytesUploaded_ZeroBytes() {
		Map<String, String> map = new HashMap<>();
		map.put("client_month_bytes_uploaded", "0");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals(0L, clientInfo.getMonthlyBytesUploaded());
	}

	@Test
	public void getMonthlyBytesUploaded_LargeBytes() {
		Map<String, String> map = new HashMap<>();
		map.put("client_month_bytes_uploaded", String.valueOf(Long.MAX_VALUE));
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals(Long.MAX_VALUE, clientInfo.getMonthlyBytesUploaded());
	}

	@Test
	public void getMonthlyBytesDownloaded_ValidBytes() {
		Map<String, String> map = new HashMap<>();
		map.put("client_month_bytes_downloaded", "2097152"); // 2 MB
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals(2097152L, clientInfo.getMonthlyBytesDownloaded());
	}

	@Test
	public void getMonthlyBytesDownloaded_ZeroBytes() {
		Map<String, String> map = new HashMap<>();
		map.put("client_month_bytes_downloaded", "0");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals(0L, clientInfo.getMonthlyBytesDownloaded());
	}

	@Test
	public void getTotalBytesUploaded_ValidBytes() {
		Map<String, String> map = new HashMap<>();
		map.put("client_total_bytes_uploaded", "104857600"); // 100 MB
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals(104857600L, clientInfo.getTotalBytesUploaded());
	}

	@Test
	public void getTotalBytesUploaded_ZeroBytes() {
		Map<String, String> map = new HashMap<>();
		map.put("client_total_bytes_uploaded", "0");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals(0L, clientInfo.getTotalBytesUploaded());
	}

	@Test
	public void getTotalBytesDownloaded_ValidBytes() {
		Map<String, String> map = new HashMap<>();
		map.put("client_total_bytes_downloaded", "209715200"); // 200 MB
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals(209715200L, clientInfo.getTotalBytesDownloaded());
	}

	@Test
	public void getTotalBytesDownloaded_ZeroBytes() {
		Map<String, String> map = new HashMap<>();
		map.put("client_total_bytes_downloaded", "0");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals(0L, clientInfo.getTotalBytesDownloaded());
	}

	@Test
	public void getIconId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("client_icon_id", "12345");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals(12345L, clientInfo.getIconId());
	}

	@Test
	public void getIconId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		map.put("client_icon_id", "0");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals(0L, clientInfo.getIconId());
	}

	@Test
	public void getIconId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		map.put("client_icon_id", "-1");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals(-1L, clientInfo.getIconId());
	}

	@Test
	public void getBase64HashClientUID_ValidHash() {
		Map<String, String> map = new HashMap<>();
		map.put("client_base64HashClientUID", "abcd1234efgh5678");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals("abcd1234efgh5678", clientInfo.getBase64HashClientUID());
	}

	@Test
	public void getBase64HashClientUID_EmptyHash() {
		Map<String, String> map = new HashMap<>();
		map.put("client_base64HashClientUID", "");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals("", clientInfo.getBase64HashClientUID());
	}

	@Test
	public void getBase64HashClientUID_NullHash() {
		Map<String, String> map = new HashMap<>();
		// client_base64HashClientUID key not present
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals("", clientInfo.getBase64HashClientUID());
	}

	// Test inherited methods from DatabaseClient
	@Test
	public void getUniqueIdentifier_ValidUID() {
		Map<String, String> map = new HashMap<>();
		map.put("client_unique_identifier", "unique123456");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals("unique123456", clientInfo.getUniqueIdentifier());
	}

	@Test
	public void getNickname_ValidNickname() {
		Map<String, String> map = new HashMap<>();
		map.put("client_nickname", "TestUser");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals("TestUser", clientInfo.getNickname());
	}

	@Test
	public void getCreatedDate_ValidTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("client_created", "1609459200"); // 2021-01-01 00:00:00 UTC
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		Date expectedDate = new Date(1609459200L * 1000);
		assertEquals(expectedDate, clientInfo.getCreatedDate());
	}

	@Test
	public void getLastConnectedDate_ValidTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("client_lastconnected", "1640995200"); // 2022-01-01 00:00:00 UTC
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		Date expectedDate = new Date(1640995200L * 1000);
		assertEquals(expectedDate, clientInfo.getLastConnectedDate());
	}

	@Test
	public void getTotalConnections_ValidCount() {
		Map<String, String> map = new HashMap<>();
		map.put("client_totalconnections", "50");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals(50, clientInfo.getTotalConnections());
	}

	@Test
	public void getDescription_ValidDescription() {
		Map<String, String> map = new HashMap<>();
		map.put("client_description", "Regular user");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals("Regular user", clientInfo.getDescription());
	}

	@Test
	public void getLastIp_ValidIp() {
		Map<String, String> map = new HashMap<>();
		map.put("client_lastip", "192.168.1.100");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		assertEquals("192.168.1.100", clientInfo.getLastIp());
	}

	@Test
	public void databaseClientInfo_CompleteData() {
		Map<String, String> map = new HashMap<>();
		map.put("client_database_id", "100");
		map.put("client_unique_identifier", "unique123456");
		map.put("client_nickname", "TestUser");
		map.put("client_created", "1609459200");
		map.put("client_lastconnected", "1640995200");
		map.put("client_totalconnections", "25");
		map.put("client_description", "VIP User");
		map.put("client_lastip", "10.0.0.1");
		map.put("client_flag_avatar", "avatar789");
		map.put("client_month_bytes_uploaded", "1048576");
		map.put("client_month_bytes_downloaded", "2097152");
		map.put("client_total_bytes_uploaded", "104857600");
		map.put("client_total_bytes_downloaded", "209715200");
		map.put("client_icon_id", "54321");
		map.put("client_base64HashClientUID", "hash123abc");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		
		assertEquals(100, clientInfo.getDatabaseId());
		assertEquals("unique123456", clientInfo.getUniqueIdentifier());
		assertEquals("TestUser", clientInfo.getNickname());
		assertEquals(new Date(1609459200L * 1000), clientInfo.getCreatedDate());
		assertEquals(new Date(1640995200L * 1000), clientInfo.getLastConnectedDate());
		assertEquals(25, clientInfo.getTotalConnections());
		assertEquals("VIP User", clientInfo.getDescription());
		assertEquals("10.0.0.1", clientInfo.getLastIp());
		assertEquals("avatar789", clientInfo.getAvatar());
		assertEquals(1048576L, clientInfo.getMonthlyBytesUploaded());
		assertEquals(2097152L, clientInfo.getMonthlyBytesDownloaded());
		assertEquals(104857600L, clientInfo.getTotalBytesUploaded());
		assertEquals(209715200L, clientInfo.getTotalBytesDownloaded());
		assertEquals(54321L, clientInfo.getIconId());
		assertEquals("hash123abc", clientInfo.getBase64HashClientUID());
	}

	@Test
	public void databaseClientInfo_EmptyMap() {
		Map<String, String> map = new HashMap<>();
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		
		assertEquals(-1, clientInfo.getDatabaseId()); // Default int value
		assertEquals("", clientInfo.getUniqueIdentifier());
		assertEquals("", clientInfo.getNickname());
		assertEquals(new Date(-1000), clientInfo.getCreatedDate()); // Default long value * 1000
		assertEquals(new Date(-1000), clientInfo.getLastConnectedDate()); // Default long value * 1000
		assertEquals(-1, clientInfo.getTotalConnections()); // Default int value
		assertEquals("", clientInfo.getDescription());
		assertEquals("", clientInfo.getLastIp());
		assertEquals("", clientInfo.getAvatar());
		assertEquals(-1L, clientInfo.getMonthlyBytesUploaded()); // Default long value
		assertEquals(-1L, clientInfo.getMonthlyBytesDownloaded()); // Default long value
		assertEquals(-1L, clientInfo.getTotalBytesUploaded()); // Default long value
		assertEquals(-1L, clientInfo.getTotalBytesDownloaded()); // Default long value
		assertEquals(-1L, clientInfo.getIconId()); // Default long value
		assertEquals("", clientInfo.getBase64HashClientUID());
	}

	@Test
	public void databaseClientInfo_InheritedWrapperMethods() {
		Map<String, String> map = new HashMap<>();
		map.put("client_database_id", "100");
		map.put("client_nickname", "TestUser");
		map.put("test_field", "test_value");
		
		DatabaseClientInfo clientInfo = new DatabaseClientInfo(map);
		
		// Test inherited methods from Wrapper
		assertEquals("100", clientInfo.get("client_database_id"));
		assertEquals("TestUser", clientInfo.get("client_nickname"));
		assertEquals("test_value", clientInfo.get("test_field"));
		assertEquals(100, clientInfo.getInt("client_database_id"));
	}
}
