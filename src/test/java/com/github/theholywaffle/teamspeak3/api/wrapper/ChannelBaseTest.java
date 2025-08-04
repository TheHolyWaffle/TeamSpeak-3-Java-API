package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ChannelBannerMode;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.Codec;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ChannelBaseTest {

	// Concrete implementation for testing abstract ChannelBase
	private static class TestChannelBase extends ChannelBase {
		private final int id;
		private final boolean familyEmpty;

		public TestChannelBase(Map<String, String> map, int id, boolean familyEmpty) {
			super(map);
			this.id = id;
			this.familyEmpty = familyEmpty;
		}

		@Override
		public int getId() {
			return id;
		}

		@Override
		public boolean isFamilyEmpty() {
			return familyEmpty;
		}
	}

	@Test
	public void constructor_ValidMap() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_name", "Test Channel");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertNotNull(channel);
		assertEquals(1, channel.getId());
		assertEquals("Test Channel", channel.getName());
	}

	@Test
	public void getParentChannelId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("pid", "5");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(5, channel.getParentChannelId());
	}

	@Test
	public void getParentChannelId_ZeroId() {
		Map<String, String> map = new HashMap<>();
		map.put("pid", "0");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(0, channel.getParentChannelId());
	}

	@Test
	public void getOrder_ValidOrder() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_order", "10");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(10, channel.getOrder());
	}

	@Test
	public void getOrder_ZeroOrder() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_order", "0");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(0, channel.getOrder());
	}

	@Test
	public void getName_ValidName() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_name", "General Chat");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals("General Chat", channel.getName());
	}

	@Test
	public void getName_EmptyName() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_name", "");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals("", channel.getName());
	}

	@Test
	public void getName_NullName() {
		Map<String, String> map = new HashMap<>();
		// channel_name key not present

		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals("", channel.getName());
	}

	@Test
	public void getTopic_ValidTopic() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_topic", "Welcome to our server!");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals("Welcome to our server!", channel.getTopic());
	}

	@Test
	public void getTopic_EmptyTopic() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_topic", "");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals("", channel.getTopic());
	}

	@Test
	public void getTopic_NullTopic() {
		Map<String, String> map = new HashMap<>();
		// channel_topic key not present

		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals("", channel.getTopic());
	}

	@Test
	public void isDefault_True() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_flag_default", "1");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertTrue(channel.isDefault());
	}

	@Test
	public void isDefault_False() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_flag_default", "0");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertFalse(channel.isDefault());
	}

	@Test
	public void hasPassword_True() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_flag_password", "1");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertTrue(channel.hasPassword());
	}

	@Test
	public void hasPassword_False() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_flag_password", "0");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertFalse(channel.hasPassword());
	}

	@Test
	public void isPermanent_True() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_flag_permanent", "1");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertTrue(channel.isPermanent());
	}

	@Test
	public void isPermanent_False() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_flag_permanent", "0");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertFalse(channel.isPermanent());
	}

	@Test
	public void isSemiPermanent_True() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_flag_semi_permanent", "1");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertTrue(channel.isSemiPermanent());
	}

	@Test
	public void isSemiPermanent_False() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_flag_semi_permanent", "0");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertFalse(channel.isSemiPermanent());
	}

	@Test
	public void getBannerMode_NoAdjustment() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_banner_mode", "0");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(ChannelBannerMode.NO_ADJUST, channel.getBannerMode());
	}

	@Test
	public void getBannerMode_IgnoreAspect() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_banner_mode", "1");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(ChannelBannerMode.IGNORE_ASPECT, channel.getBannerMode());
	}

	@Test
	public void getBannerMode_KeepAspect() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_banner_mode", "2");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(ChannelBannerMode.KEEP_ASPECT, channel.getBannerMode());
	}

	@Test
	public void getBannerMode_Unknown() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_banner_mode", "999");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(ChannelBannerMode.UNKNOWN, channel.getBannerMode());
	}

	@Test
	public void getBannerGraphicsUrl_ValidUrl() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_banner_gfx_url", "http://example.com/banner.png");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals("http://example.com/banner.png", channel.getBannerGraphicsUrl());
	}

	@Test
	public void getBannerGraphicsUrl_NullUrl() {
		Map<String, String> map = new HashMap<>();
		// channel_banner_gfx_url key not present

		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals("", channel.getBannerGraphicsUrl());
	}

	@Test
	public void getCodec_SpeexNarrowband() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_codec", "0");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(Codec.SPEEX_NARROWBAND, channel.getCodec());
	}

	@Test
	public void getCodec_SpeexWideband() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_codec", "1");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(Codec.SPEEX_WIDEBAND, channel.getCodec());
	}

	@Test
	public void getCodec_SpeexUltrawideband() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_codec", "2");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(Codec.SPEEX_ULTRAWIDEBAND, channel.getCodec());
	}

	@Test
	public void getCodec_CeltMono() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_codec", "3");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(Codec.CELT_MONO, channel.getCodec());
	}

	@Test
	public void getCodec_OpusVoice() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_codec", "4");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(Codec.OPUS_VOICE, channel.getCodec());
	}

	@Test
	public void getCodec_OpusMusic() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_codec", "5");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(Codec.OPUS_MUSIC, channel.getCodec());
	}

	@Test
	public void getCodec_Unknown() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_codec", "999");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(Codec.UNKNOWN, channel.getCodec());
	}

	@Test
	public void getCodecQuality_ValidQuality() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_codec_quality", "7");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(7, channel.getCodecQuality());
	}

	@Test
	public void getNeededTalkPower_ValidPower() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_needed_talk_power", "50");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(50, channel.getNeededTalkPower());
	}

	@Test
	public void getIconId_ValidId() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_icon_id", "12345");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(12345L, channel.getIconId());
	}

	@Test
	public void getMaxClients_ValidMax() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_maxclients", "100");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(100, channel.getMaxClients());
	}

	@Test
	public void getMaxFamilyClients_ValidMax() {
		Map<String, String> map = new HashMap<>();
		map.put("channel_maxfamilyclients", "200");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(200, channel.getMaxFamilyClients());
	}

	@Test
	public void getSecondsEmpty_ValidSeconds() {
		Map<String, String> map = new HashMap<>();
		map.put("seconds_empty", "300");
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertEquals(300, channel.getSecondsEmpty());
	}

	@Test
	public void isFamilyEmpty_True() {
		Map<String, String> map = new HashMap<>();
		
		TestChannelBase channel = new TestChannelBase(map, 1, true);
		assertTrue(channel.isFamilyEmpty());
	}

	@Test
	public void isFamilyEmpty_False() {
		Map<String, String> map = new HashMap<>();
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		assertFalse(channel.isFamilyEmpty());
	}

	@Test
	public void channelBase_CompleteData() {
		Map<String, String> map = new HashMap<>();
		map.put("pid", "0");
		map.put("channel_order", "1");
		map.put("channel_name", "Main Channel");
		map.put("channel_topic", "Welcome!");
		map.put("channel_flag_default", "1");
		map.put("channel_flag_password", "0");
		map.put("channel_flag_permanent", "1");
		map.put("channel_flag_semi_permanent", "0");
		map.put("channel_banner_mode", "2");
		map.put("channel_banner_gfx_url", "http://example.com/banner.png");
		map.put("channel_codec", "5");
		map.put("channel_codec_quality", "10");
		map.put("channel_needed_talk_power", "25");
		map.put("channel_icon_id", "54321");
		map.put("channel_maxclients", "50");
		map.put("channel_maxfamilyclients", "100");
		map.put("seconds_empty", "600");
		
		TestChannelBase channel = new TestChannelBase(map, 42, true);
		
		assertEquals(42, channel.getId());
		assertEquals(0, channel.getParentChannelId());
		assertEquals(1, channel.getOrder());
		assertEquals("Main Channel", channel.getName());
		assertEquals("Welcome!", channel.getTopic());
		assertTrue(channel.isDefault());
		assertFalse(channel.hasPassword());
		assertTrue(channel.isPermanent());
		assertFalse(channel.isSemiPermanent());
		assertEquals(ChannelBannerMode.KEEP_ASPECT, channel.getBannerMode());
		assertEquals("http://example.com/banner.png", channel.getBannerGraphicsUrl());
		assertEquals(Codec.OPUS_MUSIC, channel.getCodec());
		assertEquals(10, channel.getCodecQuality());
		assertEquals(25, channel.getNeededTalkPower());
		assertEquals(54321L, channel.getIconId());
		assertEquals(50, channel.getMaxClients());
		assertEquals(100, channel.getMaxFamilyClients());
		assertEquals(600, channel.getSecondsEmpty());
		assertTrue(channel.isFamilyEmpty());
	}

	@Test
	public void channelBase_EmptyMap() {
		Map<String, String> map = new HashMap<>();
		
		TestChannelBase channel = new TestChannelBase(map, 1, false);
		
		assertEquals(1, channel.getId());
		assertEquals(-1, channel.getParentChannelId()); // Default int value
		assertEquals(-1, channel.getOrder()); // Default int value
		assertEquals("", channel.getName());
		assertEquals("", channel.getTopic());
		assertFalse(channel.isDefault()); // Default boolean value
		assertFalse(channel.hasPassword()); // Default boolean value
		assertFalse(channel.isPermanent()); // Default boolean value
		assertFalse(channel.isSemiPermanent()); // Default boolean value
		assertEquals(ChannelBannerMode.UNKNOWN, channel.getBannerMode()); // Default enum value for -1
		assertEquals("", channel.getBannerGraphicsUrl());
		assertEquals(Codec.UNKNOWN, channel.getCodec()); // Default enum value for -1
		assertEquals(-1, channel.getCodecQuality()); // Default int value
		assertEquals(-1, channel.getNeededTalkPower()); // Default int value
		assertEquals(-1L, channel.getIconId()); // Default long value
		assertEquals(-1, channel.getMaxClients()); // Default int value
		assertEquals(-1, channel.getMaxFamilyClients()); // Default int value
		assertEquals(-1, channel.getSecondsEmpty()); // Default int value
		assertFalse(channel.isFamilyEmpty());
	}
}
