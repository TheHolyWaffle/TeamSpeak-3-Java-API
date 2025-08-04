package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ChannelTest {

	private Map<String, String> channelMap;
	private Channel channel;

	@BeforeEach
	public void setUp() {
		channelMap = new HashMap<>();
		channelMap.put(ChannelProperty.CID.getName(), "5");
		channelMap.put(ChannelProperty.PID.getName(), "0");
		channelMap.put(ChannelProperty.CHANNEL_ORDER.getName(), "0");
		channelMap.put(ChannelProperty.CHANNEL_NAME.getName(), "Test Channel");
		channelMap.put(ChannelProperty.CHANNEL_TOPIC.getName(), "Test Topic");
		channelMap.put(ChannelProperty.CHANNEL_FLAG_DEFAULT.getName(), "0");
		channelMap.put(ChannelProperty.CHANNEL_FLAG_PASSWORD.getName(), "0");
		channelMap.put(ChannelProperty.CHANNEL_FLAG_PERMANENT.getName(), "1");
		channelMap.put(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT.getName(), "0");
		channelMap.put(ChannelProperty.CHANNEL_CODEC.getName(), "4");
		channelMap.put(ChannelProperty.CHANNEL_CODEC_QUALITY.getName(), "7");
		channelMap.put(ChannelProperty.CHANNEL_MAXCLIENTS.getName(), "10");
		channelMap.put(ChannelProperty.CHANNEL_MAXFAMILYCLIENTS.getName(), "100");
		channelMap.put(ChannelProperty.CHANNEL_NEEDED_TALK_POWER.getName(), "0");
		channelMap.put(ChannelProperty.CHANNEL_NEEDED_SUBSCRIBE_POWER.getName(), "0");
		channelMap.put(ChannelProperty.TOTAL_CLIENTS.getName(), "3");
		channelMap.put(ChannelProperty.TOTAL_CLIENTS_FAMILY.getName(), "5");
		channelMap.put(ChannelProperty.CHANNEL_ICON_ID.getName(), "12345");
		
		channel = new Channel(channelMap);
	}

	@Test
	public void constructor_ValidMap() {
		assertNotNull(channel);
		assertEquals(channelMap, channel.getMap());
	}

	@Test
	public void getId() {
		assertEquals(5, channel.getId());
	}

	@Test
	public void getTotalClientsFamily() {
		assertEquals(5, channel.getTotalClientsFamily());
	}

	@Test
	public void getTotalClients() {
		assertEquals(3, channel.getTotalClients());
	}

	@Test
	public void getNeededSubscribePower() {
		assertEquals(0, channel.getNeededSubscribePower());
	}

	@Test
	public void isEmpty_False() {
		assertFalse(channel.isEmpty());
	}

	@Test
	public void isEmpty_True() {
		channelMap.put(ChannelProperty.TOTAL_CLIENTS.getName(), "0");
		Channel emptyChannel = new Channel(channelMap);
		assertTrue(emptyChannel.isEmpty());
	}

	@Test
	public void isFamilyEmpty_False() {
		assertFalse(channel.isFamilyEmpty());
	}

	@Test
	public void isFamilyEmpty_True() {
		channelMap.put(ChannelProperty.TOTAL_CLIENTS_FAMILY.getName(), "0");
		Channel emptyFamilyChannel = new Channel(channelMap);
		assertTrue(emptyFamilyChannel.isFamilyEmpty());
	}

	// Test inherited methods from ChannelBase
	@Test
	public void getName() {
		assertEquals("Test Channel", channel.getName());
	}

	@Test
	public void getTopic() {
		assertEquals("Test Topic", channel.getTopic());
	}

	@Test
	public void getParentChannelId() {
		assertEquals(0, channel.getParentChannelId());
	}

	@Test
	public void getOrder() {
		assertEquals(0, channel.getOrder());
	}

	@Test
	public void isDefault() {
		assertFalse(channel.isDefault());
	}

	@Test
	public void hasPassword() {
		assertFalse(channel.hasPassword());
	}

	@Test
	public void isPermanent() {
		assertTrue(channel.isPermanent());
	}

	@Test
	public void isSemiPermanent() {
		assertFalse(channel.isSemiPermanent());
	}

	@Test
	public void getCodec() {
		// The getCodec() method returns a Codec enum, not an int
		// We need to test the actual codec value
		assertEquals(4, channel.getCodec().getIndex());
	}

	@Test
	public void getCodecQuality() {
		assertEquals(7, channel.getCodecQuality());
	}

	@Test
	public void getMaxClients() {
		assertEquals(10, channel.getMaxClients());
	}

	@Test
	public void getMaxFamilyClients() {
		assertEquals(100, channel.getMaxFamilyClients());
	}

	@Test
	public void getNeededTalkPower() {
		assertEquals(0, channel.getNeededTalkPower());
	}

	@Test
	public void getIconId() {
		assertEquals(12345L, channel.getIconId());
	}

	@Test
	public void isDefault_True() {
		channelMap.put(ChannelProperty.CHANNEL_FLAG_DEFAULT.getName(), "1");
		Channel defaultChannel = new Channel(channelMap);
		assertTrue(defaultChannel.isDefault());
	}

	@Test
	public void hasPassword_True() {
		channelMap.put(ChannelProperty.CHANNEL_FLAG_PASSWORD.getName(), "1");
		Channel passwordChannel = new Channel(channelMap);
		assertTrue(passwordChannel.hasPassword());
	}

	@Test
	public void isPermanent_False() {
		channelMap.put(ChannelProperty.CHANNEL_FLAG_PERMANENT.getName(), "0");
		Channel nonPermanentChannel = new Channel(channelMap);
		assertFalse(nonPermanentChannel.isPermanent());
	}

	@Test
	public void isSemiPermanent_True() {
		channelMap.put(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT.getName(), "1");
		Channel semiPermanentChannel = new Channel(channelMap);
		assertTrue(semiPermanentChannel.isSemiPermanent());
	}

	@Test
	public void getSecondsEmpty() {
		// Add test data for seconds empty
		channelMap.put(ChannelProperty.SECONDS_EMPTY.getName(), "120");
		Channel channelWithSecondsEmpty = new Channel(channelMap);
		assertEquals(120, channelWithSecondsEmpty.getSecondsEmpty());
	}

	@Test
	public void edgeCase_NegativeValues() {
		channelMap.put(ChannelProperty.TOTAL_CLIENTS.getName(), "-1");
		channelMap.put(ChannelProperty.TOTAL_CLIENTS_FAMILY.getName(), "-1");
		Channel negativeChannel = new Channel(channelMap);
		
		assertEquals(-1, negativeChannel.getTotalClients());
		assertEquals(-1, negativeChannel.getTotalClientsFamily());
		assertFalse(negativeChannel.isEmpty()); // -1 != 0
		assertFalse(negativeChannel.isFamilyEmpty()); // -1 != 0
	}

	@Test
	public void edgeCase_MissingProperties() {
		Map<String, String> minimalMap = new HashMap<>();
		minimalMap.put(ChannelProperty.CID.getName(), "1");
		// Missing other properties should return default values

		Channel minimalChannel = new Channel(minimalMap);

		assertEquals(1, minimalChannel.getId());
		assertEquals(-1, minimalChannel.getTotalClients()); // Default value for missing property
		assertEquals(-1, minimalChannel.getTotalClientsFamily()); // Default value for missing property
		assertFalse(minimalChannel.isEmpty()); // -1 != 0, so not empty
		assertFalse(minimalChannel.isFamilyEmpty()); // -1 != 0, so not empty
	}
}
