package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ClientTest {

	private Map<String, String> clientMap;
	private Client client;

	@BeforeEach
	public void setUp() {
		clientMap = new HashMap<>();
		clientMap.put(ClientProperty.CLID.getName(), "5");
		clientMap.put(ClientProperty.CID.getName(), "10");
		clientMap.put(ClientProperty.CLIENT_NICKNAME.getName(), "TestClient");
		clientMap.put(ClientProperty.CLIENT_UNIQUE_IDENTIFIER.getName(), "unique123");
		clientMap.put(ClientProperty.CLIENT_DATABASE_ID.getName(), "100");
		clientMap.put(ClientProperty.CLIENT_CHANNEL_GROUP_ID.getName(), "8");
		clientMap.put(ClientProperty.CLIENT_SERVERGROUPS.getName(), "6,7,8");
		clientMap.put(ClientProperty.CLIENT_TYPE.getName(), "0");
		clientMap.put(ClientProperty.CLIENT_AWAY.getName(), "0");
		clientMap.put(ClientProperty.CLIENT_AWAY_MESSAGE.getName(), "");
		clientMap.put(ClientProperty.CLIENT_IS_TALKER.getName(), "1");
		clientMap.put(ClientProperty.CLIENT_IS_CHANNEL_COMMANDER.getName(), "0");
		clientMap.put(ClientProperty.CLIENT_IS_PRIORITY_SPEAKER.getName(), "1");
		clientMap.put(ClientProperty.CLIENT_IS_RECORDING.getName(), "0");
		clientMap.put(ClientProperty.CLIENT_FLAG_TALKING.getName(), "1");
		clientMap.put(ClientProperty.CLIENT_INPUT_MUTED.getName(), "0");
		clientMap.put(ClientProperty.CLIENT_OUTPUT_MUTED.getName(), "0");
		clientMap.put(ClientProperty.CLIENT_INPUT_HARDWARE.getName(), "1");
		clientMap.put(ClientProperty.CLIENT_OUTPUT_HARDWARE.getName(), "1");
		clientMap.put(ClientProperty.CLIENT_TALK_POWER.getName(), "75");
		clientMap.put(ClientProperty.CLIENT_CREATED.getName(), "1609459200"); // 2021-01-01 00:00:00 UTC
		clientMap.put(ClientProperty.CLIENT_LASTCONNECTED.getName(), "1609545600"); // 2021-01-02 00:00:00 UTC
		clientMap.put(ClientProperty.CLIENT_IDLE_TIME.getName(), "30000");
		clientMap.put(ClientProperty.CLIENT_VERSION.getName(), "3.5.6");
		clientMap.put(ClientProperty.CLIENT_PLATFORM.getName(), "Windows");
		clientMap.put(ClientProperty.CLIENT_COUNTRY.getName(), "US");
		clientMap.put(ClientProperty.CLIENT_ESTIMATED_LOCATION.getName(), "New York");
		clientMap.put(ClientProperty.CONNECTION_CLIENT_IP.getName(), "192.168.1.100");
		clientMap.put(ClientProperty.CLIENT_ICON_ID.getName(), "12345");
		clientMap.put(ClientProperty.CLIENT_BADGES.getName(), "badges=badge1,badge2,badge3 overwolf=1");
		
		client = new Client(clientMap);
	}

	@Test
	public void getId() {
		assertEquals(5, client.getId());
	}

	@Test
	public void getChannelId() {
		assertEquals(10, client.getChannelId());
	}

	@Test
	public void getNickname() {
		assertEquals("TestClient", client.getNickname());
	}

	@Test
	public void getUniqueIdentifier() {
		assertEquals("unique123", client.getUniqueIdentifier());
	}

	@Test
	public void getDatabaseId() {
		assertEquals(100, client.getDatabaseId());
	}

	@Test
	public void getChannelGroupId() {
		assertEquals(8, client.getChannelGroupId());
	}

	@Test
	public void getServerGroups() {
		int[] expected = {6, 7, 8};
		assertArrayEquals(expected, client.getServerGroups());
	}

	@Test
	public void getType() {
		assertEquals(0, client.getType());
	}

	@Test
	public void isRegularClient() {
		assertTrue(client.isRegularClient());
	}

	@Test
	public void isServerQueryClient() {
		assertFalse(client.isServerQueryClient());
	}

	@Test
	public void isAway() {
		assertFalse(client.isAway());
	}

	@Test
	public void getAwayMessage() {
		assertEquals("", client.getAwayMessage());
	}

	@Test
	public void canTalk() {
		assertTrue(client.canTalk());
	}

	@Test
	public void isChannelCommander() {
		assertFalse(client.isChannelCommander());
	}

	@Test
	public void isPrioritySpeaker() {
		assertTrue(client.isPrioritySpeaker());
	}

	@Test
	public void isRecording() {
		assertFalse(client.isRecording());
	}

	@Test
	public void isTalking() {
		assertTrue(client.isTalking());
	}

	@Test
	public void isInputMuted() {
		assertFalse(client.isInputMuted());
	}

	@Test
	public void isOutputMuted() {
		assertFalse(client.isOutputMuted());
	}

	@Test
	public void isInputHardware() {
		assertTrue(client.isInputHardware());
	}

	@Test
	public void isOutputHardware() {
		assertTrue(client.isOutputHardware());
	}

	@Test
	public void getTalkPower() {
		assertEquals(75, client.getTalkPower());
	}

	@Test
	public void getCreatedDate() {
		Date expected = new Date(1609459200L * 1000);
		assertEquals(expected, client.getCreatedDate());
	}

	@Test
	public void getLastConnectedDate() {
		Date expected = new Date(1609545600L * 1000);
		assertEquals(expected, client.getLastConnectedDate());
	}

	@Test
	public void getIdleTime() {
		assertEquals(30000L, client.getIdleTime());
	}

	@Test
	public void getVersion() {
		assertEquals("3.5.6", client.getVersion());
	}

	@Test
	public void getPlatform() {
		assertEquals("Windows", client.getPlatform());
	}

	@Test
	public void getCountry() {
		assertEquals("US", client.getCountry());
	}

	@Test
	public void getEstimatedLocation() {
		assertEquals("New York", client.getEstimatedLocation());
	}

	@Test
	public void getIp() {
		assertEquals("192.168.1.100", client.getIp());
	}

	@Test
	public void getIconId() {
		assertEquals(12345L, client.getIconId());
	}

	@Test
	public void getBadgeGUIDs() {
		String[] expected = {"badge1", "badge2", "badge3"};
		assertArrayEquals(expected, client.getBadgeGUIDs());
	}

	@Test
	public void getBadgeGUIDs_NoBadges() {
		clientMap.put(ClientProperty.CLIENT_BADGES.getName(), "overwolf=1");
		Client clientNoBadges = new Client(clientMap);
		assertEquals(0, clientNoBadges.getBadgeGUIDs().length);
	}

	@Test
	public void hasOverwolf() {
		assertTrue(client.hasOverwolf());
	}

	@Test
	public void hasOverwolf_False() {
		clientMap.put(ClientProperty.CLIENT_BADGES.getName(), "badges=badge1,badge2 overwolf=0");
		Client clientNoOverwolf = new Client(clientMap);
		assertFalse(clientNoOverwolf.hasOverwolf());
	}

	@Test
	public void hasOverwolf_NotPresent() {
		clientMap.put(ClientProperty.CLIENT_BADGES.getName(), "badges=badge1,badge2");
		Client clientNoOverwolf = new Client(clientMap);
		assertFalse(clientNoOverwolf.hasOverwolf());
	}

	@Test
	public void isInServerGroup_True() {
		assertTrue(client.isInServerGroup(7));
	}

	@Test
	public void isInServerGroup_False() {
		assertFalse(client.isInServerGroup(99));
	}

	@Test
	public void getClientURI() {
		String expected = "client://5/unique123~TestClient";
		assertEquals(expected, client.getClientURI());
	}

	@Test
	public void getClientURI_WithSpecialCharacters() {
		clientMap.put(ClientProperty.CLIENT_NICKNAME.getName(), "Test Client With Spaces");
		Client clientWithSpaces = new Client(clientMap);
		String expected = "client://5/unique123~Test+Client+With+Spaces";
		assertEquals(expected, clientWithSpaces.getClientURI());
	}
}
