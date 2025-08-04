package com.github.theholywaffle.teamspeak3.api.wrapper;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MessageTest {

	@Test
	public void constructor_ValidMap() {
		Map<String, String> map = new HashMap<>();
		map.put("msgid", "1");
		map.put("subject", "Test Message");
		
		Message message = new Message(map);
		assertNotNull(message);
		assertEquals(1, message.getId());
		assertEquals("Test Message", message.getSubject());
	}

	@Test
	public void getId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("msgid", "42");
		
		Message message = new Message(map);
		assertEquals(42, message.getId());
	}

	@Test
	public void getId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		map.put("msgid", "0");
		
		Message message = new Message(map);
		assertEquals(0, message.getId());
	}

	@Test
	public void getId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		map.put("msgid", "-1");
		
		Message message = new Message(map);
		assertEquals(-1, message.getId());
	}

	@Test
	public void getId_LargeId() {
		Map<String, String> map = new HashMap<>();
		map.put("msgid", String.valueOf(Integer.MAX_VALUE));
		
		Message message = new Message(map);
		assertEquals(Integer.MAX_VALUE, message.getId());
	}

	@Test
	public void getSenderUniqueIdentifier_ValidUID() {
		Map<String, String> map = new HashMap<>();
		map.put("cluid", "sender123456");
		
		Message message = new Message(map);
		assertEquals("sender123456", message.getSenderUniqueIdentifier());
	}

	@Test
	public void getSenderUniqueIdentifier_EmptyUID() {
		Map<String, String> map = new HashMap<>();
		map.put("cluid", "");
		
		Message message = new Message(map);
		assertEquals("", message.getSenderUniqueIdentifier());
	}

	@Test
	public void getSenderUniqueIdentifier_NullUID() {
		Map<String, String> map = new HashMap<>();
		// cluid key not present
		
		Message message = new Message(map);
		assertEquals("", message.getSenderUniqueIdentifier());
	}

	@Test
	public void getSubject_ValidSubject() {
		Map<String, String> map = new HashMap<>();
		map.put("subject", "Important Message");
		
		Message message = new Message(map);
		assertEquals("Important Message", message.getSubject());
	}

	@Test
	public void getSubject_EmptySubject() {
		Map<String, String> map = new HashMap<>();
		map.put("subject", "");
		
		Message message = new Message(map);
		assertEquals("", message.getSubject());
	}

	@Test
	public void getSubject_NullSubject() {
		Map<String, String> map = new HashMap<>();
		// subject key not present
		
		Message message = new Message(map);
		assertEquals("", message.getSubject());
	}

	@Test
	public void getSubject_SpecialCharacters() {
		Map<String, String> map = new HashMap<>();
		map.put("subject", "Message [URGENT] & Important!");
		
		Message message = new Message(map);
		assertEquals("Message [URGENT] & Important!", message.getSubject());
	}

	@Test
	public void getReceivedDate_ValidTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("timestamp", "1609459200"); // 2021-01-01 00:00:00 UTC
		
		Message message = new Message(map);
		Date expectedDate = new Date(1609459200L * 1000);
		assertEquals(expectedDate, message.getReceivedDate());
	}

	@Test
	public void getReceivedDate_ZeroTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("timestamp", "0");
		
		Message message = new Message(map);
		Date expectedDate = new Date(0);
		assertEquals(expectedDate, message.getReceivedDate());
	}

	@Test
	public void getReceivedDate_NegativeTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("timestamp", "-1");
		
		Message message = new Message(map);
		Date expectedDate = new Date(-1000);
		assertEquals(expectedDate, message.getReceivedDate());
	}

	@Test
	public void getReceivedDate_LargeTimestamp() {
		Map<String, String> map = new HashMap<>();
		map.put("timestamp", "2147483647"); // Max int value
		
		Message message = new Message(map);
		Date expectedDate = new Date(2147483647L * 1000);
		assertEquals(expectedDate, message.getReceivedDate());
	}

	@Test
	public void hasBeenRead_True() {
		Map<String, String> map = new HashMap<>();
		map.put("flag_read", "1");
		
		Message message = new Message(map);
		assertTrue(message.hasBeenRead());
	}

	@Test
	public void hasBeenRead_False() {
		Map<String, String> map = new HashMap<>();
		map.put("flag_read", "0");
		
		Message message = new Message(map);
		assertFalse(message.hasBeenRead());
	}

	@Test
	public void hasBeenRead_DefaultValue() {
		Map<String, String> map = new HashMap<>();
		// flag_read key not present
		
		Message message = new Message(map);
		assertFalse(message.hasBeenRead()); // Default boolean value
	}

	@Test
	public void message_CompleteData() {
		Map<String, String> map = new HashMap<>();
		map.put("msgid", "123");
		map.put("cluid", "sender789");
		map.put("subject", "Welcome Message");
		map.put("timestamp", "1640995200"); // 2022-01-01 00:00:00 UTC
		map.put("flag_read", "0");
		
		Message message = new Message(map);
		
		assertEquals(123, message.getId());
		assertEquals("sender789", message.getSenderUniqueIdentifier());
		assertEquals("Welcome Message", message.getSubject());
		assertEquals(new Date(1640995200L * 1000), message.getReceivedDate());
		assertFalse(message.hasBeenRead());
	}

	@Test
	public void message_EmptyMap() {
		Map<String, String> map = new HashMap<>();
		
		Message message = new Message(map);
		
		assertEquals(-1, message.getId()); // Default int value
		assertEquals("", message.getSenderUniqueIdentifier());
		assertEquals("", message.getSubject());
		assertEquals(new Date(-1000), message.getReceivedDate()); // Default long value * 1000
		assertFalse(message.hasBeenRead()); // Default boolean value
	}

	@Test
	public void message_PartialData() {
		Map<String, String> map = new HashMap<>();
		map.put("msgid", "456");
		map.put("subject", "Partial Message");
		// Missing other fields
		
		Message message = new Message(map);
		
		assertEquals(456, message.getId());
		assertEquals("", message.getSenderUniqueIdentifier()); // Default string value
		assertEquals("Partial Message", message.getSubject());
		assertEquals(new Date(-1000), message.getReceivedDate()); // Default long value * 1000
		assertFalse(message.hasBeenRead()); // Default boolean value
	}

	@Test
	public void message_AllZeroValues() {
		Map<String, String> map = new HashMap<>();
		map.put("msgid", "0");
		map.put("timestamp", "0");
		map.put("flag_read", "0");
		
		Message message = new Message(map);
		
		assertEquals(0, message.getId());
		assertEquals(new Date(0), message.getReceivedDate());
		assertFalse(message.hasBeenRead());
	}

	@Test
	public void message_AllMaxValues() {
		Map<String, String> map = new HashMap<>();
		map.put("msgid", String.valueOf(Integer.MAX_VALUE));
		map.put("timestamp", String.valueOf(Long.MAX_VALUE));
		map.put("flag_read", "1");
		
		Message message = new Message(map);
		
		assertEquals(Integer.MAX_VALUE, message.getId());
		assertEquals(new Date(Long.MAX_VALUE * 1000), message.getReceivedDate());
		assertTrue(message.hasBeenRead());
	}

	@Test
	public void message_InheritedWrapperMethods() {
		Map<String, String> map = new HashMap<>();
		map.put("msgid", "789");
		map.put("subject", "Test Message");
		map.put("test_field", "test_value");
		
		Message message = new Message(map);
		
		// Test inherited methods from Wrapper
		assertEquals("789", message.get("msgid"));
		assertEquals("Test Message", message.get("subject"));
		assertEquals("test_value", message.get("test_field"));
		assertEquals(789, message.getInt("msgid"));
	}

	@Test
	public void message_RealWorldScenario() {
		// Simulate a real-world message scenario
		Map<String, String> map = new HashMap<>();
		map.put("msgid", "1001");
		map.put("cluid", "ServerAdmin=");
		map.put("subject", "Server Maintenance Notice");
		map.put("timestamp", "1672531200"); // 2023-01-01 00:00:00 UTC
		map.put("flag_read", "0");
		
		Message message = new Message(map);
		
		assertEquals(1001, message.getId());
		assertEquals("ServerAdmin=", message.getSenderUniqueIdentifier());
		assertEquals("Server Maintenance Notice", message.getSubject());
		assertEquals(new Date(1672531200L * 1000), message.getReceivedDate());
		assertFalse(message.hasBeenRead());
	}

	@Test
	public void message_ReadMessage() {
		Map<String, String> map = new HashMap<>();
		map.put("msgid", "2001");
		map.put("cluid", "user123");
		map.put("subject", "Read Message");
		map.put("timestamp", "1672617600"); // 2023-01-02 00:00:00 UTC
		map.put("flag_read", "1");
		
		Message message = new Message(map);
		
		assertEquals(2001, message.getId());
		assertEquals("user123", message.getSenderUniqueIdentifier());
		assertEquals("Read Message", message.getSubject());
		assertEquals(new Date(1672617600L * 1000), message.getReceivedDate());
		assertTrue(message.hasBeenRead());
	}

	@Test
	public void message_LongSubject() {
		Map<String, String> map = new HashMap<>();
		String longSubject = "This is a very long message subject that contains detailed information about the content of the message and may span multiple lines when displayed in the client interface.";
		map.put("msgid", "3001");
		map.put("subject", longSubject);
		
		Message message = new Message(map);
		
		assertEquals(3001, message.getId());
		assertEquals(longSubject, message.getSubject());
	}

	@Test
	public void message_UnicodeSubject() {
		Map<String, String> map = new HashMap<>();
		map.put("msgid", "4001");
		map.put("subject", "Üñíçødé Mëssågé 🎉");
		
		Message message = new Message(map);
		
		assertEquals(4001, message.getId());
		assertEquals("Üñíçødé Mëssågé 🎉", message.getSubject());
	}
}
