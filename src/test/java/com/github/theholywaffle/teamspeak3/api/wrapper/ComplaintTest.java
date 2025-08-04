package com.github.theholywaffle.teamspeak3.api.wrapper;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ComplaintTest {

	@Test
	public void constructor_ValidMap() {
		Map<String, String> map = new HashMap<>();
		map.put("tcldbid", "100");
		map.put("tname", "TargetUser");
		
		Complaint complaint = new Complaint(map);
		assertNotNull(complaint);
		assertEquals(100, complaint.getTargetClientDatabaseId());
		assertEquals("TargetUser", complaint.getTargetName());
	}

	@Test
	public void getTargetClientDatabaseId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("tcldbid", "200");
		
		Complaint complaint = new Complaint(map);
		assertEquals(200, complaint.getTargetClientDatabaseId());
	}

	@Test
	public void getTargetClientDatabaseId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		map.put("tcldbid", "0");
		
		Complaint complaint = new Complaint(map);
		assertEquals(0, complaint.getTargetClientDatabaseId());
	}

	@Test
	public void getTargetClientDatabaseId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		map.put("tcldbid", "-1");
		
		Complaint complaint = new Complaint(map);
		assertEquals(-1, complaint.getTargetClientDatabaseId());
	}

	@Test
	public void getTargetClientDatabaseId_LargeId() {
		Map<String, String> map = new HashMap<>();
		map.put("tcldbid", String.valueOf(Integer.MAX_VALUE));
		
		Complaint complaint = new Complaint(map);
		assertEquals(Integer.MAX_VALUE, complaint.getTargetClientDatabaseId());
	}

	@Test
	public void getTargetName_ValidName() {
		Map<String, String> map = new HashMap<>();
		map.put("tname", "BadUser");
		
		Complaint complaint = new Complaint(map);
		assertEquals("BadUser", complaint.getTargetName());
	}

	@Test
	public void getTargetName_EmptyName() {
		Map<String, String> map = new HashMap<>();
		map.put("tname", "");
		
		Complaint complaint = new Complaint(map);
		assertEquals("", complaint.getTargetName());
	}

	@Test
	public void getTargetName_NullName() {
		Map<String, String> map = new HashMap<>();
		// tname key not present
		
		Complaint complaint = new Complaint(map);
		assertEquals("", complaint.getTargetName());
	}

	@Test
	public void getTargetName_SpecialCharacters() {
		Map<String, String> map = new HashMap<>();
		map.put("tname", "User [VIP] & Admin");
		
		Complaint complaint = new Complaint(map);
		assertEquals("User [VIP] & Admin", complaint.getTargetName());
	}

	@Test
	public void getSourceClientDatabaseId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("fcldbid", "300");
		
		Complaint complaint = new Complaint(map);
		assertEquals(300, complaint.getSourceClientDatabaseId());
	}

	@Test
	public void getSourceClientDatabaseId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		map.put("fcldbid", "0");
		
		Complaint complaint = new Complaint(map);
		assertEquals(0, complaint.getSourceClientDatabaseId());
	}

	@Test
	public void getSourceClientDatabaseId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		map.put("fcldbid", "-1");
		
		Complaint complaint = new Complaint(map);
		assertEquals(-1, complaint.getSourceClientDatabaseId());
	}

	@Test
	public void getSourceClientDatabaseId_LargeId() {
		Map<String, String> map = new HashMap<>();
		map.put("fcldbid", String.valueOf(Integer.MAX_VALUE));
		
		Complaint complaint = new Complaint(map);
		assertEquals(Integer.MAX_VALUE, complaint.getSourceClientDatabaseId());
	}

	@Test
	public void getSourceName_ValidName() {
		Map<String, String> map = new HashMap<>();
		map.put("fname", "ReporterUser");
		
		Complaint complaint = new Complaint(map);
		assertEquals("ReporterUser", complaint.getSourceName());
	}

	@Test
	public void getSourceName_EmptyName() {
		Map<String, String> map = new HashMap<>();
		map.put("fname", "");
		
		Complaint complaint = new Complaint(map);
		assertEquals("", complaint.getSourceName());
	}

	@Test
	public void getSourceName_NullName() {
		Map<String, String> map = new HashMap<>();
		// fname key not present
		
		Complaint complaint = new Complaint(map);
		assertEquals("", complaint.getSourceName());
	}

	@Test
	public void getSourceName_SpecialCharacters() {
		Map<String, String> map = new HashMap<>();
		map.put("fname", "Moderator [Staff]");
		
		Complaint complaint = new Complaint(map);
		assertEquals("Moderator [Staff]", complaint.getSourceName());
	}

	@Test
	public void getMessage_ValidMessage() {
		Map<String, String> map = new HashMap<>();
		map.put("message", "This user is spamming the chat");
		
		Complaint complaint = new Complaint(map);
		assertEquals("This user is spamming the chat", complaint.getMessage());
	}

	@Test
	public void getMessage_EmptyMessage() {
		Map<String, String> map = new HashMap<>();
		map.put("message", "");
		
		Complaint complaint = new Complaint(map);
		assertEquals("", complaint.getMessage());
	}

	@Test
	public void getMessage_NullMessage() {
		Map<String, String> map = new HashMap<>();
		// message key not present
		
		Complaint complaint = new Complaint(map);
		assertEquals("", complaint.getMessage());
	}

	@Test
	public void getMessage_LongMessage() {
		Map<String, String> map = new HashMap<>();
		String longMessage = "This is a very long complaint message that describes in detail the inappropriate behavior of the user including spamming, trolling, and other violations of server rules.";
		map.put("message", longMessage);
		
		Complaint complaint = new Complaint(map);
		assertEquals(longMessage, complaint.getMessage());
	}

	@Test
	public void getMessage_SpecialCharacters() {
		Map<String, String> map = new HashMap<>();
		map.put("message", "User said: \"Hello & goodbye!\" [offensive]");
		
		Complaint complaint = new Complaint(map);
		assertEquals("User said: \"Hello & goodbye!\" [offensive]", complaint.getMessage());
	}

	@Test
	public void getTimestamp_ValidTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("timestamp", "1609459200"); // 2021-01-01 00:00:00 UTC
		
		Complaint complaint = new Complaint(map);
		Date expectedDate = new Date(1609459200L * 1000);
		assertEquals(expectedDate, complaint.getTimestamp());
	}

	@Test
	public void getTimestamp_ZeroTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("timestamp", "0");
		
		Complaint complaint = new Complaint(map);
		Date expectedDate = new Date(0);
		assertEquals(expectedDate, complaint.getTimestamp());
	}

	@Test
	public void getTimestamp_NegativeTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("timestamp", "-1");
		
		Complaint complaint = new Complaint(map);
		Date expectedDate = new Date(-1000);
		assertEquals(expectedDate, complaint.getTimestamp());
	}

	@Test
	public void getTimestamp_LargeTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("timestamp", "2147483647"); // Max int value
		
		Complaint complaint = new Complaint(map);
		Date expectedDate = new Date(2147483647L * 1000);
		assertEquals(expectedDate, complaint.getTimestamp());
	}

	@Test
	public void complaint_CompleteData() {
		Map<String, String> map = new HashMap<>();
		map.put("tcldbid", "100");
		map.put("tname", "BadUser");
		map.put("fcldbid", "200");
		map.put("fname", "ReporterUser");
		map.put("message", "Inappropriate behavior in chat");
		map.put("timestamp", "1609459200");
		
		Complaint complaint = new Complaint(map);
		
		assertEquals(100, complaint.getTargetClientDatabaseId());
		assertEquals("BadUser", complaint.getTargetName());
		assertEquals(200, complaint.getSourceClientDatabaseId());
		assertEquals("ReporterUser", complaint.getSourceName());
		assertEquals("Inappropriate behavior in chat", complaint.getMessage());
		assertEquals(new Date(1609459200L * 1000), complaint.getTimestamp());
	}

	@Test
	public void complaint_EmptyMap() {
		Map<String, String> map = new HashMap<>();
		
		Complaint complaint = new Complaint(map);
		
		assertEquals(-1, complaint.getTargetClientDatabaseId()); // Default int value
		assertEquals("", complaint.getTargetName());
		assertEquals(-1, complaint.getSourceClientDatabaseId()); // Default int value
		assertEquals("", complaint.getSourceName());
		assertEquals("", complaint.getMessage());
		assertEquals(new Date(-1000), complaint.getTimestamp()); // Default long value * 1000
	}

	@Test
	public void complaint_PartialData() {
		Map<String, String> map = new HashMap<>();
		map.put("tcldbid", "50");
		map.put("message", "Partial complaint");
		// Missing other fields
		
		Complaint complaint = new Complaint(map);
		
		assertEquals(50, complaint.getTargetClientDatabaseId());
		assertEquals("", complaint.getTargetName()); // Default string value
		assertEquals(-1, complaint.getSourceClientDatabaseId()); // Default int value
		assertEquals("", complaint.getSourceName()); // Default string value
		assertEquals("Partial complaint", complaint.getMessage());
		assertEquals(new Date(-1000), complaint.getTimestamp()); // Default long value * 1000
	}

	@Test
	public void complaint_AllZeroValues() {
		Map<String, String> map = new HashMap<>();
		map.put("tcldbid", "0");
		map.put("fcldbid", "0");
		map.put("timestamp", "0");
		
		Complaint complaint = new Complaint(map);
		
		assertEquals(0, complaint.getTargetClientDatabaseId());
		assertEquals(0, complaint.getSourceClientDatabaseId());
		assertEquals(new Date(0), complaint.getTimestamp());
	}

	@Test
	public void complaint_AllNegativeValues() {
		Map<String, String> map = new HashMap<>();
		map.put("tcldbid", "-5");
		map.put("fcldbid", "-10");
		map.put("timestamp", "-1");
		
		Complaint complaint = new Complaint(map);
		
		assertEquals(-5, complaint.getTargetClientDatabaseId());
		assertEquals(-10, complaint.getSourceClientDatabaseId());
		assertEquals(new Date(-1000), complaint.getTimestamp());
	}

	@Test
	public void complaint_AllMaxValues() {
		Map<String, String> map = new HashMap<>();
		map.put("tcldbid", String.valueOf(Integer.MAX_VALUE));
		map.put("fcldbid", String.valueOf(Integer.MAX_VALUE));
		map.put("timestamp", String.valueOf(Long.MAX_VALUE));
		
		Complaint complaint = new Complaint(map);
		
		assertEquals(Integer.MAX_VALUE, complaint.getTargetClientDatabaseId());
		assertEquals(Integer.MAX_VALUE, complaint.getSourceClientDatabaseId());
		assertEquals(new Date(Long.MAX_VALUE * 1000), complaint.getTimestamp());
	}

	@Test
	public void complaint_InheritedWrapperMethods() {
		Map<String, String> map = new HashMap<>();
		map.put("tcldbid", "100");
		map.put("tname", "TestUser");
		map.put("test_field", "test_value");
		
		Complaint complaint = new Complaint(map);
		
		// Test inherited methods from Wrapper
		assertEquals("100", complaint.get("tcldbid"));
		assertEquals("TestUser", complaint.get("tname"));
		assertEquals("test_value", complaint.get("test_field"));
		assertEquals(100, complaint.getInt("tcldbid"));
	}

	@Test
	public void complaint_RealWorldScenario() {
		// Simulate a real-world complaint scenario
		Map<String, String> map = new HashMap<>();
		map.put("tcldbid", "42");
		map.put("tname", "TrollingUser");
		map.put("fcldbid", "1337");
		map.put("fname", "ModeratorUser");
		map.put("message", "User is constantly spamming music bot commands and disrupting conversations");
		map.put("timestamp", "1640995200"); // 2022-01-01 00:00:00 UTC
		
		Complaint complaint = new Complaint(map);
		
		assertEquals(42, complaint.getTargetClientDatabaseId());
		assertEquals("TrollingUser", complaint.getTargetName());
		assertEquals(1337, complaint.getSourceClientDatabaseId());
		assertEquals("ModeratorUser", complaint.getSourceName());
		assertEquals("User is constantly spamming music bot commands and disrupting conversations", complaint.getMessage());
		assertEquals(new Date(1640995200L * 1000), complaint.getTimestamp());
	}
}
