package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ClientInfoTest {

	@Test
	public void constructor_ValidData() {
		Map<String, String> map = new HashMap<>();
		map.put("client_nickname", "TestUser");
		map.put("client_description", "Test Description");
		
		ClientInfo clientInfo = new ClientInfo(5, map);
		assertNotNull(clientInfo);
		assertEquals(5, clientInfo.getId());
		assertEquals("TestUser", clientInfo.getNickname());
		assertEquals("Test Description", clientInfo.getDescription());
	}

	@Test
	public void getId_ReturnsConstructorValue() {
		Map<String, String> map = new HashMap<>();
		
		ClientInfo clientInfo = new ClientInfo(42, map);
		assertEquals(42, clientInfo.getId());
	}

	@Test
	public void getId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		
		ClientInfo clientInfo = new ClientInfo(0, map);
		assertEquals(0, clientInfo.getId());
	}

	@Test
	public void getId_NegativeId() {
		Map<String, String> map = new HashMap<>();
		
		ClientInfo clientInfo = new ClientInfo(-1, map);
		assertEquals(-1, clientInfo.getId());
	}

	@Test
	public void getAvatar_ValidAvatar() {
		Map<String, String> map = new HashMap<>();
		map.put("client_flag_avatar", "avatar123");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals("avatar123", clientInfo.getAvatar());
	}

	@Test
	public void getAvatar_EmptyAvatar() {
		Map<String, String> map = new HashMap<>();
		map.put("client_flag_avatar", "");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals("", clientInfo.getAvatar());
	}

	@Test
	public void getAvatar_NullAvatar() {
		Map<String, String> map = new HashMap<>();
		// client_flag_avatar key not present
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals("", clientInfo.getAvatar());
	}

	@Test
	public void getBandwidthReceivedLastMinute_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_bandwidth_received_last_minute_total", "1048576");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(1048576L, clientInfo.getBandwidthReceivedLastMinute());
	}

	@Test
	public void getBandwidthReceivedLastMinute_ZeroValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_bandwidth_received_last_minute_total", "0");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(0L, clientInfo.getBandwidthReceivedLastMinute());
	}

	@Test
	public void getBandwidthReceivedLastSecond_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_bandwidth_received_last_second_total", "2048");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(2048L, clientInfo.getBandwidthReceivedLastSecond());
	}

	@Test
	public void getBandwidthSentlastMinute_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_bandwidth_sent_last_minute_total", "512000");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(512000L, clientInfo.getBandwidthSentlastMinute());
	}

	@Test
	public void getBandwidthSentLastSecond_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_bandwidth_sent_last_second_total", "1024");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(1024L, clientInfo.getBandwidthSentLastSecond());
	}

	@Test
	public void getBase64ClientUId_ValidUID() {
		Map<String, String> map = new HashMap<>();
		map.put("client_base64HashClientUID", "abcd1234efgh5678");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals("abcd1234efgh5678", clientInfo.getBase64ClientUId());
	}

	@Test
	public void getBase64ClientUId_EmptyUID() {
		Map<String, String> map = new HashMap<>();
		map.put("client_base64HashClientUID", "");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals("", clientInfo.getBase64ClientUId());
	}

	@Test
	public void getDefaultChannel_ValidChannel() {
		Map<String, String> map = new HashMap<>();
		map.put("client_default_channel", "/5"); // TeamSpeak prefixes with /
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(5, clientInfo.getDefaultChannel());
	}

	@Test
	public void getDefaultChannel_ZeroChannel() {
		Map<String, String> map = new HashMap<>();
		map.put("client_default_channel", "/0");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(0, clientInfo.getDefaultChannel());
	}

	@Test
	public void getDefaultChannel_EmptyChannel() {
		Map<String, String> map = new HashMap<>();
		map.put("client_default_channel", "");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(-1, clientInfo.getDefaultChannel());
	}

	@Test
	public void getDefaultChannel_NullChannel() {
		Map<String, String> map = new HashMap<>();
		// client_default_channel key not present
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(-1, clientInfo.getDefaultChannel());
	}

	@Test
	public void getDefaultToken_ValidToken() {
		Map<String, String> map = new HashMap<>();
		map.put("client_default_token", "token123456");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals("token123456", clientInfo.getDefaultToken());
	}

	@Test
	public void getDefaultToken_EmptyToken() {
		Map<String, String> map = new HashMap<>();
		map.put("client_default_token", "");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals("", clientInfo.getDefaultToken());
	}

	@Test
	public void getDescription_ValidDescription() {
		Map<String, String> map = new HashMap<>();
		map.put("client_description", "VIP User");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals("VIP User", clientInfo.getDescription());
	}

	@Test
	public void getDescription_EmptyDescription() {
		Map<String, String> map = new HashMap<>();
		map.put("client_description", "");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals("", clientInfo.getDescription());
	}

	@Test
	public void getFiletransferBandwidthReceived_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_filetransfer_bandwidth_received", "204800");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(204800L, clientInfo.getFiletransferBandwidthReceived());
	}

	@Test
	public void getFiletransferBandwidthSent_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_filetransfer_bandwidth_sent", "102400");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(102400L, clientInfo.getFiletransferBandwidthSent());
	}

	@Test
	public void getLoginName_ValidName() {
		Map<String, String> map = new HashMap<>();
		map.put("client_login_name", "admin");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals("admin", clientInfo.getLoginName());
	}

	@Test
	public void getLoginName_EmptyName() {
		Map<String, String> map = new HashMap<>();
		map.put("client_login_name", "");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals("", clientInfo.getLoginName());
	}

	@Test
	public void getMetaData_ValidData() {
		Map<String, String> map = new HashMap<>();
		map.put("client_meta_data", "metadata123");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals("metadata123", clientInfo.getMetaData());
	}

	@Test
	public void getMonthlyBytesDownloaded_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("client_month_bytes_downloaded", "10485760"); // 10 MB
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(10485760L, clientInfo.getMonthlyBytesDownloaded());
	}

	@Test
	public void getMonthlyBytesUploaded_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("client_month_bytes_uploaded", "5242880"); // 5 MB
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(5242880L, clientInfo.getMonthlyBytesUploaded());
	}

	@Test
	public void getMyTeamSpeakId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("client_myteamspeak_id", "myts123456");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals("myts123456", clientInfo.getMyTeamSpeakId());
	}

	@Test
	public void getNeededServerQueryViewPower_ValidPower() {
		Map<String, String> map = new HashMap<>();
		map.put("client_needed_serverquery_view_power", "75");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(75, clientInfo.getNeededServerQueryViewPower());
	}

	@Test
	public void getPhoneticNickname_ValidNickname() {
		Map<String, String> map = new HashMap<>();
		map.put("client_nickname_phonetic", "Test User Phonetic");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals("Test User Phonetic", clientInfo.getPhoneticNickname());
	}

	@Test
	public void getTalkRequestCreatedDate_ValidDate() {
		Map<String, String> map = new HashMap<>();
		map.put("client_talk_request", "1609459200"); // 2021-01-01 00:00:00 UTC
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		Date expectedDate = new Date(1609459200L * 1000);
		assertEquals(expectedDate, clientInfo.getTalkRequestCreatedDate());
	}

	@Test
	public void getTalkRequestCreatedDate_ZeroDate() {
		Map<String, String> map = new HashMap<>();
		map.put("client_talk_request", "0");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertNull(clientInfo.getTalkRequestCreatedDate());
	}

	@Test
	public void getTalkRequestMessage_ValidMessage() {
		Map<String, String> map = new HashMap<>();
		map.put("client_talk_request_msg", "Please allow me to speak");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals("Please allow me to speak", clientInfo.getTalkRequestMessage());
	}

	@Test
	public void getTimeConnected_ValidTime() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_connected_time", "300000"); // 5 minutes in milliseconds
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(300000L, clientInfo.getTimeConnected());
	}

	@Test
	public void getTotalBytesDownloaded_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("client_total_bytes_downloaded", "104857600"); // 100 MB
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(104857600L, clientInfo.getTotalBytesDownloaded());
	}

	@Test
	public void getTotalBytesReceived_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_bytes_received_total", "52428800"); // 50 MB
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(52428800L, clientInfo.getTotalBytesReceived());
	}

	@Test
	public void getTotalBytesSent_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_bytes_sent_total", "26214400"); // 25 MB
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(26214400L, clientInfo.getTotalBytesSent());
	}

	@Test
	public void getTotalBytesUploaded_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("client_total_bytes_uploaded", "209715200"); // 200 MB
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(209715200L, clientInfo.getTotalBytesUploaded());
	}

	@Test
	public void getTotalConnections_ValidCount() {
		Map<String, String> map = new HashMap<>();
		map.put("client_totalconnections", "25");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(25, clientInfo.getTotalConnections());
	}

	@Test
	public void getTotalPacketsReceived_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_packets_received_total", "10000");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(10000L, clientInfo.getTotalPacketsReceived());
	}

	@Test
	public void getTotalPacketsSent_ValidValue() {
		Map<String, String> map = new HashMap<>();
		map.put("connection_packets_sent_total", "8000");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(8000L, clientInfo.getTotalPacketsSent());
	}

	@Test
	public void getUnreadMessages_ValidCount() {
		Map<String, String> map = new HashMap<>();
		map.put("client_unread_messages", "3");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertEquals(3, clientInfo.getUnreadMessages());
	}

	@Test
	public void isOutputOnlyMuted_True() {
		Map<String, String> map = new HashMap<>();
		map.put("client_outputonly_muted", "1");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertTrue(clientInfo.isOutputOnlyMuted());
	}

	@Test
	public void isOutputOnlyMuted_False() {
		Map<String, String> map = new HashMap<>();
		map.put("client_outputonly_muted", "0");
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertFalse(clientInfo.isOutputOnlyMuted());
	}

	@Test
	public void isRequestingToTalk_True() {
		Map<String, String> map = new HashMap<>();
		map.put("client_talk_request", "1609459200"); // Non-zero value
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertTrue(clientInfo.isRequestingToTalk());
	}

	@Test
	public void isRequestingToTalk_False() {
		Map<String, String> map = new HashMap<>();
		map.put("client_talk_request", "0"); // Zero value
		
		ClientInfo clientInfo = new ClientInfo(1, map);
		assertFalse(clientInfo.isRequestingToTalk());
	}

	@Test
	public void isTalking_ThrowsUnsupportedOperationException() {
		Map<String, String> map = new HashMap<>();

		ClientInfo clientInfo = new ClientInfo(1, map);
		assertThrows(UnsupportedOperationException.class, () -> clientInfo.isTalking());
	}

	@Test
	public void clientInfo_CompleteData() {
		Map<String, String> map = new HashMap<>();
		map.put("client_nickname", "TestUser");
		map.put("client_flag_avatar", "avatar123");
		map.put("connection_bandwidth_received_last_minute_total", "1048576");
		map.put("connection_bandwidth_received_last_second_total", "2048");
		map.put("connection_bandwidth_sent_last_minute_total", "512000");
		map.put("connection_bandwidth_sent_last_second_total", "1024");
		map.put("client_base64HashClientUID", "abcd1234efgh5678");
		map.put("client_default_channel", "/5");
		map.put("client_default_token", "token123456");
		map.put("client_description", "VIP User");
		map.put("connection_filetransfer_bandwidth_received", "204800");
		map.put("connection_filetransfer_bandwidth_sent", "102400");
		map.put("client_login_name", "admin");
		map.put("client_meta_data", "metadata123");
		map.put("client_month_bytes_downloaded", "10485760");
		map.put("client_month_bytes_uploaded", "5242880");
		map.put("client_myteamspeak_id", "myts123456");
		map.put("client_needed_serverquery_view_power", "75");
		map.put("client_nickname_phonetic", "Test User Phonetic");
		map.put("client_talk_request", "1609459200");
		map.put("client_talk_request_msg", "Please allow me to speak");
		map.put("connection_connected_time", "300000");
		map.put("client_total_bytes_downloaded", "104857600");
		map.put("connection_bytes_received_total", "52428800");
		map.put("connection_bytes_sent_total", "26214400");
		map.put("client_total_bytes_uploaded", "209715200");
		map.put("client_totalconnections", "25");
		map.put("connection_packets_received_total", "10000");
		map.put("connection_packets_sent_total", "8000");
		map.put("client_unread_messages", "3");
		map.put("client_outputonly_muted", "1");

		ClientInfo clientInfo = new ClientInfo(42, map);

		assertEquals(42, clientInfo.getId());
		assertEquals("TestUser", clientInfo.getNickname());
		assertEquals("avatar123", clientInfo.getAvatar());
		assertEquals(1048576L, clientInfo.getBandwidthReceivedLastMinute());
		assertEquals(2048L, clientInfo.getBandwidthReceivedLastSecond());
		assertEquals(512000L, clientInfo.getBandwidthSentlastMinute());
		assertEquals(1024L, clientInfo.getBandwidthSentLastSecond());
		assertEquals("abcd1234efgh5678", clientInfo.getBase64ClientUId());
		assertEquals(5, clientInfo.getDefaultChannel());
		assertEquals("token123456", clientInfo.getDefaultToken());
		assertEquals("VIP User", clientInfo.getDescription());
		assertEquals(204800L, clientInfo.getFiletransferBandwidthReceived());
		assertEquals(102400L, clientInfo.getFiletransferBandwidthSent());
		assertEquals("admin", clientInfo.getLoginName());
		assertEquals("metadata123", clientInfo.getMetaData());
		assertEquals(10485760L, clientInfo.getMonthlyBytesDownloaded());
		assertEquals(5242880L, clientInfo.getMonthlyBytesUploaded());
		assertEquals("myts123456", clientInfo.getMyTeamSpeakId());
		assertEquals(75, clientInfo.getNeededServerQueryViewPower());
		assertEquals("Test User Phonetic", clientInfo.getPhoneticNickname());
		assertEquals(new Date(1609459200L * 1000), clientInfo.getTalkRequestCreatedDate());
		assertEquals("Please allow me to speak", clientInfo.getTalkRequestMessage());
		assertEquals(300000L, clientInfo.getTimeConnected());
		assertEquals(104857600L, clientInfo.getTotalBytesDownloaded());
		assertEquals(52428800L, clientInfo.getTotalBytesReceived());
		assertEquals(26214400L, clientInfo.getTotalBytesSent());
		assertEquals(209715200L, clientInfo.getTotalBytesUploaded());
		assertEquals(25, clientInfo.getTotalConnections());
		assertEquals(10000L, clientInfo.getTotalPacketsReceived());
		assertEquals(8000L, clientInfo.getTotalPacketsSent());
		assertEquals(3, clientInfo.getUnreadMessages());
		assertTrue(clientInfo.isOutputOnlyMuted());
		assertTrue(clientInfo.isRequestingToTalk());
	}

	@Test
	public void clientInfo_EmptyMap() {
		Map<String, String> map = new HashMap<>();

		ClientInfo clientInfo = new ClientInfo(1, map);

		assertEquals(1, clientInfo.getId());
		assertEquals("", clientInfo.getNickname()); // From Client
		assertEquals("", clientInfo.getAvatar());
		assertEquals(-1L, clientInfo.getBandwidthReceivedLastMinute()); // Default long value
		assertEquals(-1L, clientInfo.getBandwidthReceivedLastSecond()); // Default long value
		assertEquals(-1L, clientInfo.getBandwidthSentlastMinute()); // Default long value
		assertEquals(-1L, clientInfo.getBandwidthSentLastSecond()); // Default long value
		assertEquals("", clientInfo.getBase64ClientUId());
		assertEquals(-1, clientInfo.getDefaultChannel()); // Empty string case
		assertEquals("", clientInfo.getDefaultToken());
		assertEquals("", clientInfo.getDescription());
		assertEquals(-1L, clientInfo.getFiletransferBandwidthReceived()); // Default long value
		assertEquals(-1L, clientInfo.getFiletransferBandwidthSent()); // Default long value
		assertEquals("", clientInfo.getLoginName());
		assertEquals("", clientInfo.getMetaData());
		assertEquals(-1L, clientInfo.getMonthlyBytesDownloaded()); // Default long value
		assertEquals(-1L, clientInfo.getMonthlyBytesUploaded()); // Default long value
		assertEquals("", clientInfo.getMyTeamSpeakId());
		assertEquals(-1, clientInfo.getNeededServerQueryViewPower()); // Default int value
		assertEquals("", clientInfo.getPhoneticNickname());
		assertEquals(new Date(-1000), clientInfo.getTalkRequestCreatedDate()); // -1 * 1000 returns Date(-1000)
		assertEquals("", clientInfo.getTalkRequestMessage());
		assertEquals(-1L, clientInfo.getTimeConnected()); // Default long value
		assertEquals(-1L, clientInfo.getTotalBytesDownloaded()); // Default long value
		assertEquals(-1L, clientInfo.getTotalBytesReceived()); // Default long value
		assertEquals(-1L, clientInfo.getTotalBytesSent()); // Default long value
		assertEquals(-1L, clientInfo.getTotalBytesUploaded()); // Default long value
		assertEquals(-1, clientInfo.getTotalConnections()); // Default int value
		assertEquals(-1L, clientInfo.getTotalPacketsReceived()); // Default long value
		assertEquals(-1L, clientInfo.getTotalPacketsSent()); // Default long value
		assertEquals(-1, clientInfo.getUnreadMessages()); // Default int value
		assertFalse(clientInfo.isOutputOnlyMuted()); // Default boolean value
		assertTrue(clientInfo.isRequestingToTalk()); // -1L != 0L is true
	}

	@Test
	public void clientInfo_InheritedClientMethods() {
		Map<String, String> map = new HashMap<>();
		map.put("client_nickname", "TestUser");
		map.put("client_type", "0");
		map.put("client_away", "1");
		map.put("cid", "5"); // CID property maps to "cid" key

		ClientInfo clientInfo = new ClientInfo(1, map);

		// Test inherited methods from Client
		assertEquals("TestUser", clientInfo.getNickname());
		assertEquals(0, clientInfo.getType());
		assertTrue(clientInfo.isAway());
		assertEquals(5, clientInfo.getChannelId());
	}

	@Test
	public void clientInfo_InheritedWrapperMethods() {
		Map<String, String> map = new HashMap<>();
		map.put("client_nickname", "TestUser");
		map.put("test_field", "test_value");

		ClientInfo clientInfo = new ClientInfo(1, map);

		// Test inherited methods from Wrapper
		assertEquals("TestUser", clientInfo.get("client_nickname"));
		assertEquals("test_value", clientInfo.get("test_field"));
		assertEquals(-1, clientInfo.getInt("missing_field")); // Should return -1 for missing key
	}
}
