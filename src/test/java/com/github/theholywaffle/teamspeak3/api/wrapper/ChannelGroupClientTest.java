package com.github.theholywaffle.teamspeak3.api.wrapper;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ChannelGroupClientTest {

	@Test
	public void constructor_ValidMap() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", "5");
		map.put("cldbid", "100");
		map.put("cgid", "10");
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		assertNotNull(channelGroupClient);
		assertEquals(5, channelGroupClient.getChannelId());
		assertEquals(100, channelGroupClient.getClientDatabaseId());
		assertEquals(10, channelGroupClient.getChannelGroupId());
	}

	@Test
	public void getChannelId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", "15");
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		assertEquals(15, channelGroupClient.getChannelId());
	}

	@Test
	public void getChannelId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", "0");
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		assertEquals(0, channelGroupClient.getChannelId());
	}

	@Test
	public void getChannelId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", "-1");
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		assertEquals(-1, channelGroupClient.getChannelId());
	}

	@Test
	public void getChannelId_LargeId() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", String.valueOf(Integer.MAX_VALUE));
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		assertEquals(Integer.MAX_VALUE, channelGroupClient.getChannelId());
	}

	@Test
	public void getClientDatabaseId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("cldbid", "200");
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		assertEquals(200, channelGroupClient.getClientDatabaseId());
	}

	@Test
	public void getClientDatabaseId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		map.put("cldbid", "0");
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		assertEquals(0, channelGroupClient.getClientDatabaseId());
	}

	@Test
	public void getClientDatabaseId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		map.put("cldbid", "-1");
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		assertEquals(-1, channelGroupClient.getClientDatabaseId());
	}

	@Test
	public void getClientDatabaseId_LargeId() {
		Map<String, String> map = new HashMap<>();
		map.put("cldbid", String.valueOf(Integer.MAX_VALUE));
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		assertEquals(Integer.MAX_VALUE, channelGroupClient.getClientDatabaseId());
	}

	@Test
	public void getChannelGroupId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("cgid", "25");
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		assertEquals(25, channelGroupClient.getChannelGroupId());
	}

	@Test
	public void getChannelGroupId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		map.put("cgid", "0");
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		assertEquals(0, channelGroupClient.getChannelGroupId());
	}

	@Test
	public void getChannelGroupId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		map.put("cgid", "-1");
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		assertEquals(-1, channelGroupClient.getChannelGroupId());
	}

	@Test
	public void getChannelGroupId_LargeId() {
		Map<String, String> map = new HashMap<>();
		map.put("cgid", String.valueOf(Integer.MAX_VALUE));
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		assertEquals(Integer.MAX_VALUE, channelGroupClient.getChannelGroupId());
	}

	@Test
	public void channelGroupClient_CompleteData() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", "42");
		map.put("cldbid", "1337");
		map.put("cgid", "99");
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		
		assertEquals(42, channelGroupClient.getChannelId());
		assertEquals(1337, channelGroupClient.getClientDatabaseId());
		assertEquals(99, channelGroupClient.getChannelGroupId());
	}

	@Test
	public void channelGroupClient_EmptyMap() {
		Map<String, String> map = new HashMap<>();
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		
		assertEquals(-1, channelGroupClient.getChannelId()); // Default int value
		assertEquals(-1, channelGroupClient.getClientDatabaseId()); // Default int value
		assertEquals(-1, channelGroupClient.getChannelGroupId()); // Default int value
	}

	@Test
	public void channelGroupClient_PartialData() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", "10");
		// Missing cldbid and cgid
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		
		assertEquals(10, channelGroupClient.getChannelId());
		assertEquals(-1, channelGroupClient.getClientDatabaseId()); // Default int value
		assertEquals(-1, channelGroupClient.getChannelGroupId()); // Default int value
	}

	@Test
	public void channelGroupClient_AllZeroValues() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", "0");
		map.put("cldbid", "0");
		map.put("cgid", "0");
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		
		assertEquals(0, channelGroupClient.getChannelId());
		assertEquals(0, channelGroupClient.getClientDatabaseId());
		assertEquals(0, channelGroupClient.getChannelGroupId());
	}

	@Test
	public void channelGroupClient_AllNegativeValues() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", "-5");
		map.put("cldbid", "-10");
		map.put("cgid", "-15");
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		
		assertEquals(-5, channelGroupClient.getChannelId());
		assertEquals(-10, channelGroupClient.getClientDatabaseId());
		assertEquals(-15, channelGroupClient.getChannelGroupId());
	}

	@Test
	public void channelGroupClient_AllMaxValues() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", String.valueOf(Integer.MAX_VALUE));
		map.put("cldbid", String.valueOf(Integer.MAX_VALUE));
		map.put("cgid", String.valueOf(Integer.MAX_VALUE));
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		
		assertEquals(Integer.MAX_VALUE, channelGroupClient.getChannelId());
		assertEquals(Integer.MAX_VALUE, channelGroupClient.getClientDatabaseId());
		assertEquals(Integer.MAX_VALUE, channelGroupClient.getChannelGroupId());
	}

	@Test
	public void channelGroupClient_WithAdditionalFields() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", "1");
		map.put("cldbid", "2");
		map.put("cgid", "3");
		map.put("extra_field", "extra_value");
		map.put("another_field", "another_value");
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		
		assertEquals(1, channelGroupClient.getChannelId());
		assertEquals(2, channelGroupClient.getClientDatabaseId());
		assertEquals(3, channelGroupClient.getChannelGroupId());
		
		// Verify that additional fields don't interfere and can be accessed via inherited methods
		assertEquals("extra_value", channelGroupClient.get("extra_field"));
		assertEquals("another_value", channelGroupClient.get("another_field"));
	}

	@Test
	public void channelGroupClient_InheritedWrapperMethods() {
		Map<String, String> map = new HashMap<>();
		map.put("cid", "5");
		map.put("cldbid", "10");
		map.put("cgid", "15");
		map.put("test_boolean", "1");
		map.put("test_long", "123456789");
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		
		// Test inherited methods from Wrapper
		assertEquals("5", channelGroupClient.get("cid"));
		assertEquals("10", channelGroupClient.get("cldbid"));
		assertEquals("15", channelGroupClient.get("cgid"));
		assertEquals(5, channelGroupClient.getInt("cid"));
		assertEquals(10, channelGroupClient.getInt("cldbid"));
		assertEquals(15, channelGroupClient.getInt("cgid"));
		assertTrue(channelGroupClient.getBoolean("test_boolean"));
		assertEquals(123456789L, channelGroupClient.getLong("test_long"));
	}

	@Test
	public void channelGroupClient_NullMap() {
		// Test that constructor accepts null map (inherited from Wrapper)
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(null);
		assertNotNull(channelGroupClient);
		// Note: calling getter methods would throw NPE, but constructor doesn't
	}

	@Test
	public void channelGroupClient_RealWorldScenario() {
		// Simulate a real-world scenario with typical values
		Map<String, String> map = new HashMap<>();
		map.put("cid", "1"); // Default channel
		map.put("cldbid", "42"); // Client database ID
		map.put("cgid", "8"); // Channel Admin group
		
		ChannelGroupClient channelGroupClient = new ChannelGroupClient(map);
		
		assertEquals(1, channelGroupClient.getChannelId());
		assertEquals(42, channelGroupClient.getClientDatabaseId());
		assertEquals(8, channelGroupClient.getChannelGroupId());
	}
}
