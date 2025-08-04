package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ChannelInfoTest {

	@Test
	public void constructor_ValidData() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_name", "Test Channel");
		map.put("channel_description", "Test Description");
		
		ChannelInfo channelInfo = new ChannelInfo(5, map);
		assertNotNull(channelInfo);
		assertEquals(5, channelInfo.getId());
		assertEquals("Test Channel", channelInfo.getName());
		assertEquals("Test Description", channelInfo.getDescription());
	}

	@Test
	public void getId_ReturnsConstructorValue() {
		Map<String, String> map = new HashMap<>();
		
		ChannelInfo channelInfo = new ChannelInfo(42, map);
		assertEquals(42, channelInfo.getId());
	}

	@Test
	public void getId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		
		ChannelInfo channelInfo = new ChannelInfo(0, map);
		assertEquals(0, channelInfo.getId());
	}

	@Test
	public void getId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		
		ChannelInfo channelInfo = new ChannelInfo(-1, map);
		assertEquals(-1, channelInfo.getId());
	}

	@Test
	public void getDescription_ValidDescription() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_description", "Welcome to our channel!");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertEquals("Welcome to our channel!", channelInfo.getDescription());
	}

	@Test
	public void getDescription_EmptyDescription() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_description", "");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertEquals("", channelInfo.getDescription());
	}

	@Test
	public void getDescription_NullDescription() {
		Map<String, String> map = new HashMap<>();
		// channel_description key not present
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertEquals("", channelInfo.getDescription());
	}

	@Test
	public void getPassword_ValidPassword() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_password", "secret123");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertEquals("secret123", channelInfo.getPassword());
	}

	@Test
	public void getPassword_EmptyPassword() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_password", "");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertEquals("", channelInfo.getPassword());
	}

	@Test
	public void getPassword_NullPassword() {
		Map<String, String> map = new HashMap<>();
		// channel_password key not present
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertEquals("", channelInfo.getPassword());
	}

	@Test
	public void getCodecLatencyFactor_ValidFactor() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_codec_latency_factor", "2");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertEquals(2, channelInfo.getCodecLatencyFactor());
	}

	@Test
	public void getCodecLatencyFactor_ZeroFactor() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_codec_latency_factor", "0");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertEquals(0, channelInfo.getCodecLatencyFactor());
	}

	@Test
	public void getCodecLatencyFactor_NegativeFactor() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_codec_latency_factor", "-1");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertEquals(-1, channelInfo.getCodecLatencyFactor());
	}

	@Test
	public void getUniqueIdentifier_ValidUUID() {
		Map<String, String> map = new HashMap<>();
		String uuidString = "550e8400-e29b-41d4-a716-446655440000";
		map.put("channel_unique_identifier", uuidString);
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		UUID expectedUUID = UUID.fromString(uuidString);
		assertEquals(expectedUUID, channelInfo.getUniqueIdentifier());
	}

	@Test
	public void getUniqueIdentifier_InvalidUUID() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_unique_identifier", "invalid-uuid");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertThrows(IllegalArgumentException.class, () -> channelInfo.getUniqueIdentifier());
	}

	@Test
	public void getUniqueIdentifier_EmptyUUID() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_unique_identifier", "");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertThrows(IllegalArgumentException.class, () -> channelInfo.getUniqueIdentifier());
	}

	@Test
	public void isEncrypted_True() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_codec_is_unencrypted", "0"); // Not unencrypted = encrypted
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertTrue(channelInfo.isEncrypted());
	}

	@Test
	public void isEncrypted_False() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_codec_is_unencrypted", "1"); // Unencrypted = not encrypted
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertFalse(channelInfo.isEncrypted());
	}

	@Test
	public void isEncrypted_DefaultValue() {
		Map<String, String> map = new HashMap<>();
		// channel_codec_is_unencrypted key not present, defaults to false
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertTrue(channelInfo.isEncrypted()); // !false = true
	}

	@Test
	public void hasUnlimitedClients_True() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_flag_maxclients_unlimited", "1");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertTrue(channelInfo.hasUnlimitedClients());
	}

	@Test
	public void hasUnlimitedClients_False() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_flag_maxclients_unlimited", "0");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertFalse(channelInfo.hasUnlimitedClients());
	}

	@Test
	public void hasUnlimitedFamilyClients_True() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_flag_maxfamilyclients_unlimited", "1");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertTrue(channelInfo.hasUnlimitedFamilyClients());
	}

	@Test
	public void hasUnlimitedFamilyClients_False() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_flag_maxfamilyclients_unlimited", "0");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertFalse(channelInfo.hasUnlimitedFamilyClients());
	}

	@Test
	public void hasInheritedMaxFamilyClients_True() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_flag_maxfamilyclients_inherited", "1");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertTrue(channelInfo.hasInheritedMaxFamilyClients());
	}

	@Test
	public void hasInheritedMaxFamilyClients_False() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_flag_maxfamilyclients_inherited", "0");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertFalse(channelInfo.hasInheritedMaxFamilyClients());
	}

	@Test
	public void getFilePath_ValidPath() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_filepath", "/path/to/channel/file");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertEquals("/path/to/channel/file", channelInfo.getFilePath());
	}

	@Test
	public void getFilePath_EmptyPath() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_filepath", "");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertEquals("", channelInfo.getFilePath());
	}

	@Test
	public void getFilePath_NullPath() {
		Map<String, String> map = new HashMap<>();
		// channel_filepath key not present
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertEquals("", channelInfo.getFilePath());
	}

	@Test
	public void isForcedSilence_True() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_forced_silence", "1");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertTrue(channelInfo.isForcedSilence());
	}

	@Test
	public void isForcedSilence_False() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_forced_silence", "0");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertFalse(channelInfo.isForcedSilence());
	}

	@Test
	public void getPhoneticName_ValidName() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_name_phonetic", "Test Phonetic");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertEquals("Test Phonetic", channelInfo.getPhoneticName());
	}

	@Test
	public void getPhoneticName_EmptyName() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_name_phonetic", "");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertEquals("", channelInfo.getPhoneticName());
	}

	@Test
	public void getPhoneticName_NullName() {
		Map<String, String> map = new HashMap<>();
		// channel_name_phonetic key not present
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertEquals("", channelInfo.getPhoneticName());
	}

	@Test
	public void isFamilyEmpty_True() {
		Map<String, String> map = new HashMap<>();
		map.put("seconds_empty", "300"); // >= 0
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertTrue(channelInfo.isFamilyEmpty());
	}

	@Test
	public void isFamilyEmpty_False() {
		Map<String, String> map = new HashMap<>();
		map.put("seconds_empty", "-1"); // < 0
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertFalse(channelInfo.isFamilyEmpty());
	}

	@Test
	public void isFamilyEmpty_Zero() {
		Map<String, String> map = new HashMap<>();
		map.put("seconds_empty", "0"); // >= 0
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		assertTrue(channelInfo.isFamilyEmpty());
	}

	@Test
	public void channelInfo_CompleteData() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_name", "Main Channel");
		map.put("channel_description", "Welcome to our server!");
		map.put("channel_password", "secret123");
		map.put("channel_codec_latency_factor", "3");
		map.put("channel_unique_identifier", "550e8400-e29b-41d4-a716-446655440000");
		map.put("channel_codec_is_unencrypted", "0");
		map.put("channel_flag_maxclients_unlimited", "1");
		map.put("channel_flag_maxfamilyclients_unlimited", "0");
		map.put("channel_flag_maxfamilyclients_inherited", "1");
		map.put("channel_filepath", "/channels/main");
		map.put("channel_forced_silence", "0");
		map.put("channel_name_phonetic", "Main Channel Phonetic");
		map.put("seconds_empty", "600");
		
		ChannelInfo channelInfo = new ChannelInfo(42, map);
		
		assertEquals(42, channelInfo.getId());
		assertEquals("Main Channel", channelInfo.getName());
		assertEquals("Welcome to our server!", channelInfo.getDescription());
		assertEquals("secret123", channelInfo.getPassword());
		assertEquals(3, channelInfo.getCodecLatencyFactor());
		assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), channelInfo.getUniqueIdentifier());
		assertTrue(channelInfo.isEncrypted());
		assertTrue(channelInfo.hasUnlimitedClients());
		assertFalse(channelInfo.hasUnlimitedFamilyClients());
		assertTrue(channelInfo.hasInheritedMaxFamilyClients());
		assertEquals("/channels/main", channelInfo.getFilePath());
		assertFalse(channelInfo.isForcedSilence());
		assertEquals("Main Channel Phonetic", channelInfo.getPhoneticName());
		assertTrue(channelInfo.isFamilyEmpty());
	}

	@Test
	public void channelInfo_EmptyMap() {
		Map<String, String> map = new HashMap<>();
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		
		assertEquals(1, channelInfo.getId());
		assertEquals("", channelInfo.getName()); // From ChannelBase
		assertEquals("", channelInfo.getDescription());
		assertEquals("", channelInfo.getPassword());
		assertEquals(-1, channelInfo.getCodecLatencyFactor()); // Default int value
		assertThrows(IllegalArgumentException.class, () -> channelInfo.getUniqueIdentifier()); // Empty string UUID
		assertTrue(channelInfo.isEncrypted()); // !false = true
		assertFalse(channelInfo.hasUnlimitedClients()); // Default boolean value
		assertFalse(channelInfo.hasUnlimitedFamilyClients()); // Default boolean value
		assertFalse(channelInfo.hasInheritedMaxFamilyClients()); // Default boolean value
		assertEquals("", channelInfo.getFilePath());
		assertFalse(channelInfo.isForcedSilence()); // Default boolean value
		assertEquals("", channelInfo.getPhoneticName());
		assertFalse(channelInfo.isFamilyEmpty()); // -1 < 0
	}

	@Test
	public void channelInfo_InheritedChannelBaseMethods() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_name", "Test Channel");
		map.put("channel_topic", "Test Topic");
		map.put("channel_flag_permanent", "1");
		map.put("channel_maxclients", "50");
		
		ChannelInfo channelInfo = new ChannelInfo(5, map);
		
		// Test inherited methods from ChannelBase
		assertEquals("Test Channel", channelInfo.getName());
		assertEquals("Test Topic", channelInfo.getTopic());
		assertTrue(channelInfo.isPermanent());
		assertEquals(50, channelInfo.getMaxClients());
	}

	@Test
	public void channelInfo_InheritedWrapperMethods() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_name", "Test Channel");
		map.put("test_field", "test_value");
		
		ChannelInfo channelInfo = new ChannelInfo(1, map);
		
		// Test inherited methods from Wrapper
		assertEquals("Test Channel", channelInfo.get("channel_name"));
		assertEquals("test_value", channelInfo.get("test_field"));
		assertEquals(-1, channelInfo.getInt("missing_field")); // Should return -1 for missing key
	}
}
